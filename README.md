# ZTinker
关于热更新热修复的例子


主要有以下几步：
一  添加依赖：
  首先添加插件到project的build.gradle:
  buildscript {
    dependencies {
        classpath ('com.tencent.tinker:tinker-patch-gradle-plugin:1.9.1')
    }
}
然后在app的gradle里面添加
    //optional, help to generate the final application
    compileOnly('com.tencent.tinker:tinker-android-anno:1.9.1')
    //tinker's main Android lib
    implementation('com.tencent.tinker:tinker-android-lib:1.9.1')
    
    到这里不要着急sync now接下来是第二步（不然会报错Error:Execution failed for task ':tinkerdemo:tinkerProcessDebugManifest'.
> tinkerId is not set!!!）
    
 二.在app的gradle中配置tinker如下：
 //-----------------------tinker配置区-----------------------------
def bakPath = file("${buildDir}/bakApk/")

//def gitSha() {//该方法需要安装git，并将项目与git建立连接，本例中不使用git，故注释
//    try {
//        String gitRev = 'git rev-parse --short HEAD'.execute(null, project.rootDir).text.trim()
//        if (gitRev == null) {
//            throw new GradleException("can't get git rev, you should add git to system path or just input test value, such as 'testTinkerId'")
//        }
//        return gitRev
//    } catch (Exception e) {
//        throw new GradleException("can't get git rev, you should add git to system path or just input test value, such as 'testTinkerId'")
//    }
//}

/**
 * you can use assembleRelease to build you base apk
 * use tinkerPatchRelease -POLD_APK=  -PAPPLY_MAPPING=  -PAPPLY_RESOURCE= to build patch
 * add apk from the build/bakApk
 */
ext {
    //for some reason, you may want to ignore tinkerBuild, such as instant run debug build?
    tinkerEnabled = true

    //for normal build
    //old apk file to build patch apk
    tinkerOldApkPath = "${bakPath}/app-debug-0228-18-53-39.apk"
    //proguard mapping file to build patch apk
    tinkerApplyMappingPath = "${bakPath}/app-debug-0228-18-53-39.txt"
    //resource R.txt to build patch apk, must input if there is resource changed
    tinkerApplyResourcePath = "${bakPath}/app-debug-0228-18-53-39-R.txt"

    //only use for build all flavor, if not, just ignore this field
    tinkerBuildFlavorDirectory = "${bakPath}/app-1018-17-32-47"
}


def getOldApkPath() {
    return hasProperty("OLD_APK") ? OLD_APK : ext.tinkerOldApkPath
}

def getApplyMappingPath() {
    return hasProperty("APPLY_MAPPING") ? APPLY_MAPPING : ext.tinkerApplyMappingPath
}

def getApplyResourceMappingPath() {
    return hasProperty("APPLY_RESOURCE") ? APPLY_RESOURCE : ext.tinkerApplyResourcePath
}

def getTinkerIdValue() {
//    return hasProperty("TINKER_ID") ? TINKER_ID : gitSha()
    return TINKER_ID //需要保证TINKER_ID有设置（在gradle.properties中）
}

def buildWithTinker() {
    return hasProperty("TINKER_ENABLE") ? TINKER_ENABLE : ext.tinkerEnabled
}

def getTinkerBuildFlavorDirectory() {
    return ext.tinkerBuildFlavorDirectory
}

if (buildWithTinker()) {
    apply plugin: 'com.tencent.tinker.patch'

    tinkerPatch {
        /**
         * necessary，default 'null'
         * the old apk path, use to diff with the new apk to build
         * add apk from the build/bakApk
         * 必须，默认为null
         * 基准apk包的路径
         */
        oldApk = getOldApkPath()
        /**
         *
         * optional，default 'false'
         * there are some cases we may get some warnings
         * if ignoreWarning is true, we would just assert the patch process
         * case 1: minSdkVersion is below 14, but you are using dexMode with raw.
         *         it must be crash when load.
         * case 2: newly added Android Component in AndroidManifest.xml,
         *         it must be crash when load.
         * case 3: loader classes in dex.loader{} are not keep in the main dex,
         *         it must be let tinker not work.
         * case 4: loader classes in dex.loader{} changes,
         *         loader classes is ues to load patch dex. it is useless to change them.
         *         it won't crash, but these changes can't effect. you may ignore it
         * case 5: resources.arsc has changed, but we don't use applyResourceMapping to build
         *
         * 可选，默认为false
         * 当设置false，可能会出现以下警告：
         * 1.minSdkVersion小于14，但你使用的是dexMode为"raw",加载时会崩溃
         * 2.AndroidManifest.xml中新增的Android组件，加载时会崩溃。
         * 3.dex.loader {}中的加载器类不保留在主dex中，会导致tinker无效
         * 4.加载器类在dex.loader {}中发生变化，加载器类用于加载补丁dex。改变它们是没有用的。它不会崩溃，但这些更改不会生效。你可以忽略它
         * 5.resources.arsc已更改，但我们不使用applyResourceMapping来构建
         */
        ignoreWarning = true

        /**
         * optional，default 'true'
         * whether sign the patch file
         * if not, you must do yourself. otherwise it can't check success during the patch loading
         * we will use the sign config with your build type
         * 可选，默认为true
         * 是否为你签名补丁文件
         * 如果false，则需要自己签名
         */
        useSign = true

        /**
         * Warning, applyMapping will affect the normal android build!
         */
        buildConfig {
            /**
             * optional，default 'null'
             * if we use tinkerPatch to build the patch apk, you'd better to apply the old
             * apk mapping file if minifyEnabled is enable!
             * Warning:
             * you must be careful that it will affect the normal assemble build!
             * 如果使用tinkerPatch构建补丁的apk，那么如果启用了minifyEnabled,则最好使用旧的apk mapping文件
             */
            applyMapping = getApplyMappingPath()
            /**
             * optional，default 'null'
             * It is nice to keep the resource id from R.txt file to reduce java changes
             * 可以保留R.txt文件中的资源来减少java的更改
             */
            applyResourceMapping = getApplyResourceMappingPath()

            /**
             * necessary，default 'null'
             * because we don't want to check the base apk with md5 in the runtime(it is slow)
             * tinkerId is use to identify the unique base apk when the patch is tried to apply.
             * we can use git rev, svn rev or simply versionCode.
             * we will gen the tinkerId in your manifest automatic
             * 这里就是我们需要设置的tinkerId
             */
            tinkerId = getTinkerIdValue()

            /**
             * if keepDexApply is true, class in which dex refer to the old apk.
             * open this can reduce the dex diff file size.
             * 如果为true，则dex指旧的apk，打开可以减少dex diff的文件大小
             */
            keepDexApply = false

            /**
             * optional, default 'false'
             * Whether tinker should treat the base apk as the one being protected by app
             * protection tools.
             * If this attribute is true, the generated patch package will contain a
             * dex including all changed classes instead of any dexdiff patch-info files.
             * 是否修补程序应该将基本apk视为受应用程序保护工具保护的那个。 如果此属性为true，
             * 则生成的修补程序包将包含一个dex，其中包含所有已更改的类，而不是任何dexdiff patch-info文件。
             */
            isProtectedApp = false

            /**
             * optional, default 'false'
             * Whether tinker should support component hotplug (add new component dynamically).
             * If this attribute is true, the component added in new apk will be available after
             * patch is successfully loaded. Otherwise an error would be announced when generating patch
             * on compile-time.
             *
             * <b>Notice that currently this feature is incubating and only support NON-EXPORTED Activity</b>
             * 如果此属性为true，则新补丁程序中添加的组件将在补丁程序成功加载后可用。 否则在编译时生成补丁时会报错。
             */
            supportHotplugComponent = false
        }

        dex {
            /**
             * optional，default 'jar'
             * only can be 'raw' or 'jar'. for raw, we would keep its original format
             * for jar, we would repack dexes with zip format.
             * if you want to support below 14, you must use jar
             * or you want to save rom or check quicker, you can use raw mode also
             * 对于raw，会保留原来的格式，对于jar，会用zip格式重新打包dex，如果要支持14以下，必须使用jar，如果想保存rom或更快检查，则可使用raw
             */
            dexMode = "jar"

            /**
             * necessary，default '[]'
             * what dexes in apk are expected to deal with tinkerPatch
             * it support * or ? pattern.
             * 需要处理dex路径，支持*、?通配符，路径是相对安装包的
             */
            pattern = ["classes*.dex",
                       "assets/secondary-dex-?.jar"]
            /**
             * necessary，default '[]'
             * Warning, it is very very important, loader classes can't change with patch.
             * thus, they will be removed from patch dexes.
             * you must put the following class into main dex.
             * Simply, you should add your own application {@code tinker.sample.android.SampleApplication}
             * own tinkerLoader, and the classes you use in them
             * 这一项非常重要，它定义了哪些类在加载补丁包的时候会用到。这些类是通过Tinker无法修改的类，也是一定要放在main dex的类。
             * 这里需要定义的类有：
             * 1. 你自己定义的Application类；
             * 2. Tinker库中用于加载补丁包的部分类，即com.tencent.tinker.loader.*；
             * 3. 如果你自定义了TinkerLoader，需要将它以及它引用的所有类也加入loader中；
             * 4. 其他一些你不希望被更改的类，例如Sample中的BaseBuildInfo类。这里需要注意的是，这些类的直接引用类也需要加入到loader中。或者你需要将这个类变成非preverify。
             * 5. 使用1.7.6版本之后版本，参数1、2会自动填写。
             *
             */
            loader = [
                    //use sample, let BaseBuildInfo unchangeable with tinker
                    "tinker.sample.android.app.BaseBuildInfo"
            ]
        }

        lib {
            /**
             * optional，default '[]'
             * what library in apk are expected to deal with tinkerPatch
             * it support * or ? pattern.
             * for library in assets, we would just recover them in the patch directory
             * you can get them in TinkerLoadResult with Tinker
             * 库匹配
             */
            pattern = ["lib/*/*.so"]
        }

        res {
            /**
             * optional，default '[]'
             * what resource in apk are expected to deal with tinkerPatch
             * it support * or ? pattern.
             * you must include all your resources in apk here,
             * otherwise, they won't repack in the new apk resources.
             * 资源文件匹配
             */
            pattern = ["res/*", "assets/*", "resources.arsc", "AndroidManifest.xml"]

            /**
             * optional，default '[]'
             * the resource file exclude patterns, ignore add, delete or modify resource change
             * it support * or ? pattern.
             * Warning, we can only use for files no relative with resources.arsc
             * 满足ignoreChange的pattern，在编译时会忽略该文件的新增、删除与修改。
             */
            ignoreChange = ["assets/sample_meta.txt"]

            /**
             * default 100kb
             * for modify resource, if it is larger than 'largeModSize'
             * we would like to use bsdiff algorithm to reduce patch file size
             * 对于修改的资源，如果大于largeModSize，将使用bsdiff算法。
             * 这可以降低补丁包的大小，但是会增加合成时的复杂度。
             */
            largeModSize = 100
        }

        packageConfig {//用于生成补丁包中的’package_meta.txt’文件
            /**
             * optional，default 'TINKER_ID, TINKER_ID_VALUE' 'NEW_TINKER_ID, NEW_TINKER_ID_VALUE'
             * package meta file gen. path is assets/package_meta.txt in patch file
             * you can use securityCheck.getPackageProperties() in your ownPackageCheck method
             * or TinkerLoadResult.getPackageConfigByName
             * we will get the TINKER_ID from the old apk manifest for you automatic,
             * other config files (such as patchMessage below)is not necessary
             * configField(“key”, “value”), 默认我们自动从基准安装包与新安装包的Manifest中读取tinkerId,并自动写入configField。
             * 在这里，你可以定义其他的信息，在运行时可以通过TinkerLoadResult.getPackageConfigByName得到
             */
            configField("patchMessage", "tinker is sample to use")
            /**
             * just a sample case, you can use such as sdkVersion, brand, channel...
             * you can parse it in the SamplePatchListener.
             * Then you can use patch conditional!
             */
            configField("platform", "all")
            /**
             * patch version via packageConfig
             */
            configField("patchVersion", "1.0")
        }
        //or you can add config filed outside, or get meta value from old apk
        //project.tinkerPatch.packageConfig.configField("test1", project.tinkerPatch.packageConfig.getMetaDataFromOldApk("Test"))
        //project.tinkerPatch.packageConfig.configField("test2", "sample")

        /**
         * if you don't use zipArtifact or path, we just use 7za to try
         */
        sevenZip {
            /**
             * optional，default '7za'
             * the 7zip artifact path, it will use the right 7za with your platform
             */
            zipArtifact = "com.tencent.mm:SevenZip:1.1.10"
            /**
             * optional，default '7za'
             * you can specify the 7za path yourself, it will overwrite the zipArtifact value
             */
//        path = "/usr/local/bin/7za"
        }
    }

    List<String> flavors = new ArrayList<>();
    project.android.productFlavors.each { flavor ->
        flavors.add(flavor.name)
    }
    boolean hasFlavors = flavors.size() > 0
    def date = new Date().format("MMdd-HH-mm-ss")

    /**
     * bak apk and mapping
     */
    android.applicationVariants.all { variant ->
        /**
         * task type, you want to bak
         */
        def taskName = variant.name

        tasks.all {
            if ("assemble${taskName.capitalize()}".equalsIgnoreCase(it.name)) {

                it.doLast {
                    copy {
                        def fileNamePrefix = "${project.name}-${variant.baseName}"
                        def newFileNamePrefix = hasFlavors ? "${fileNamePrefix}" : "${fileNamePrefix}-${date}"

                        def destPath = hasFlavors ? file("${bakPath}/${project.name}-${date}/${variant.flavorName}") : bakPath
                        from variant.outputs.first().outputFile
                        into destPath
                        rename { String fileName ->
                            fileName.replace("${fileNamePrefix}.apk", "${newFileNamePrefix}.apk")
                        }

                        from "${buildDir}/outputs/mapping/${variant.dirName}/mapping.txt"
                        into destPath
                        rename { String fileName ->
                            fileName.replace("mapping.txt", "${newFileNamePrefix}-mapping.txt")
                        }

                        from "${buildDir}/intermediates/symbols/${variant.dirName}/R.txt"
                        into destPath
                        rename { String fileName ->
                            fileName.replace("R.txt", "${newFileNamePrefix}-R.txt")
                        }
                    }
                }
            }
        }
    }
    project.afterEvaluate {
        //sample use for build all flavor for one time
        if (hasFlavors) {
            task(tinkerPatchAllFlavorRelease) {
                group = 'tinker'
                def originOldPath = getTinkerBuildFlavorDirectory()
                for (String flavor : flavors) {
                    def tinkerTask = tasks.getByName("tinkerPatch${flavor.capitalize()}Release")
                    dependsOn tinkerTask
                    def preAssembleTask = tasks.getByName("process${flavor.capitalize()}ReleaseManifest")
                    preAssembleTask.doFirst {
                        String flavorName = preAssembleTask.name.substring(7, 8).toLowerCase() + preAssembleTask.name.substring(8, preAssembleTask.name.length() - 15)
                        project.tinkerPatch.oldApk = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release.apk"
                        project.tinkerPatch.buildConfig.applyMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release-mapping.txt"
                        project.tinkerPatch.buildConfig.applyResourceMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-release-R.txt"

                    }

                }
            }

            task(tinkerPatchAllFlavorDebug) {
                group = 'tinker'
                def originOldPath = getTinkerBuildFlavorDirectory()
                for (String flavor : flavors) {
                    def tinkerTask = tasks.getByName("tinkerPatch${flavor.capitalize()}Debug")
                    dependsOn tinkerTask
                    def preAssembleTask = tasks.getByName("process${flavor.capitalize()}DebugManifest")
                    preAssembleTask.doFirst {
                        String flavorName = preAssembleTask.name.substring(7, 8).toLowerCase() + preAssembleTask.name.substring(8, preAssembleTask.name.length() - 13)
                        project.tinkerPatch.oldApk = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug.apk"
                        project.tinkerPatch.buildConfig.applyMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug-mapping.txt"
                        project.tinkerPatch.buildConfig.applyResourceMapping = "${originOldPath}/${flavorName}/${project.name}-${flavorName}-debug-R.txt"
                    }

                }
            }
        }
    }
}
//---------------------tinker配置结束----------------------------------
然后在gradle.properties中加入配置
TINKER_VERSION=1.0
TINKER_ID=1.0
TINKER_ENABLE=true

三.现在可以sync now  接下来就是继承DefaultApplicationLike，并生成自己的application  详见项目中的SampleApplicationLike
四就是编译了 
