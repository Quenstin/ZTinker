package com.example.zhuguoqing.ztinker

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import com.tencent.tinker.lib.tinker.TinkerInstaller
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btTinker.setOnClickListener {
            Toast.makeText(this, "修复", Toast.LENGTH_SHORT).show()
            TinkerInstaller.onReceiveUpgradePatch(application.applicationContext, Environment.getExternalStorageDirectory().absolutePath + "/patch.patch")
        }


    }
}
