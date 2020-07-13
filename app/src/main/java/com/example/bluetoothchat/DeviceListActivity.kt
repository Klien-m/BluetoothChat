package com.example.bluetoothchat

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.example.common.logger.Log
import kotlinx.android.synthetic.main.activity_device_list.*

/**
 *
 *
 * @author Klien
 * @since 2020/7/12 21:57
 */
class DeviceListActivity : Activity(), AdapterView.OnItemClickListener {

    private lateinit var mbtAdapter: BluetoothAdapter

    private lateinit var mNewDevicesArrayAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        setContentView(R.layout.activity_device_list)

        setResult(RESULT_CANCELED)

        buttonScan.setOnClickListener {
            doDiscovery()
            it.visibility = View.GONE
        }

        val pairedDeviceArrayAdapter: ArrayAdapter<String> =
            ArrayAdapter(this, R.layout.device_name)
        mNewDevicesArrayAdapter = ArrayAdapter(this, R.layout.device_name)

        pairedDevices.adapter = pairedDeviceArrayAdapter
        pairedDevices.onItemClickListener = this

        newDevices.adapter = mNewDevicesArrayAdapter
        newDevices.onItemClickListener = this

        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)

        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)

        mbtAdapter = BluetoothAdapter.getDefaultAdapter()

        val pairedDevices = mbtAdapter.bondedDevices

        if (pairedDevices.size > 0) {
            titlePairedDevices.visibility = View.VISIBLE
            for (device: BluetoothDevice in pairedDevices) {
                pairedDeviceArrayAdapter.add("${device.name}\n${device.address}")
            }
        } else {
            pairedDeviceArrayAdapter.add(resources.getString(R.string.none_paired))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mbtAdapter.cancelDiscovery()
        this.unregisterReceiver(mReceiver)
    }

    private fun doDiscovery() {
        Log.d(TAG, "doDiscovery")
        setProgressBarIndeterminateVisibility(true)
        setTitle(R.string.scanning)
        titleNewDevices.visibility = View.VISIBLE
        if (mbtAdapter.isDiscovering) {
            mbtAdapter.cancelDiscovery()
        }
        mbtAdapter.startDiscovery()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        mbtAdapter.cancelDiscovery()
        val info = (view as TextView).text.toString()
        val address = info.substring(info.length - 17)
        val intent = Intent()
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action as String
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add("${device.name}\n${device.address}")
                    Toast.makeText(
                        this@DeviceListActivity,
                        "${device.name}\n${device.address}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                setProgressBarIndeterminateVisibility(false)
                setTitle(R.string.select_device)
                if (mNewDevicesArrayAdapter.count == 0) {
                    mNewDevicesArrayAdapter.add(context.getString(R.string.none_found))
                }
            }
        }
    }

    companion object {

        private const val TAG = "DeviceListActivity"

        const val EXTRA_DEVICE_ADDRESS = "device_address"

    }
}