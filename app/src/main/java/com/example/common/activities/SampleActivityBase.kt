package com.example.common.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.example.bluetoothchat.R
import com.example.common.logger.Log
import com.example.common.logger.LogWrapper
import kotlinx.android.synthetic.main.activity_main.*

/**
 * @author Klien
 * @since 2020/7/12 20:48
 */
open class SampleActivityBase : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        initializeLogging()
    }

    open fun initializeLogging() {
        val logWrapper: LogWrapper = LogWrapper()
        Log.setLogNode(logWrapper)
        Log.i(TAG, "Ready")
    }

    companion object {
        const val TAG = "SampleActivityBase"
    }
}