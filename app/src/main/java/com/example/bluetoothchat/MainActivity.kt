package com.example.bluetoothchat

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ViewAnimator
import com.example.common.activities.SampleActivityBase
import com.example.common.logger.Log
import com.example.common.logger.LogFragment
import com.example.common.logger.LogWrapper
import com.example.common.logger.MessageOnlyLogFilter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : SampleActivityBase() {

    private var mLogShown: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolBar)

        if (savedInstanceState == null) {
            val transition = supportFragmentManager.beginTransaction()
            val fragment = BluetoothChatFragment()
            transition.replace(R.id.sample_content_fragment, fragment)
            transition.commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val logToggle = menu.findItem(R.id.menu_toggle_log)
        logToggle.isVisible = findViewById<ViewAnimator>(R.id.sample_output) is ViewAnimator
        logToggle.title =
            if (mLogShown) getString(R.string.sample_hide_log) else getString(R.string.sample_show_log)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_toggle_log -> {
                mLogShown = !mLogShown
                val output = findViewById<View>(R.id.sample_output) as ViewAnimator
                if (mLogShown) {
                    output.displayedChild = 1
                } else {
                    output.displayedChild = 0
                }
                supportInvalidateOptionsMenu()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun initializeLogging() {
        val logWrapper = LogWrapper()
        Log.setLogNode(logWrapper)
        val msgFilter = MessageOnlyLogFilter()
        logWrapper.next = msgFilter
        val logFragment: LogFragment =
            supportFragmentManager.findFragmentById(R.id.log_fragment) as LogFragment
        logWrapper.next = logFragment.logView
        Log.i(TAG, "Ready")
    }

    companion object {

        const val TAG = "MainActivity"
    }
}