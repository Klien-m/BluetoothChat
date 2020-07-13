package com.example.bluetoothchat

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.common.logger.Log
import kotlinx.android.synthetic.main.fragment_bluetooth_chat.*

/**
 *
 *
 * @author Klien
 * @since 2020/7/12 21:56
 */
class BluetoothChatFragment : Fragment() {

    /**
     * 连接设备地名字
     */
    private var mConnectedDeviceName: String? = null

    /**
     * 用于对话线程的数组适配器
     */
    private lateinit var mConversationArrayAdapter: ArrayAdapter<String>

    /**
     * 输出消息的 String buffer
     */
    private lateinit var mOutStringBuffer: StringBuffer

    /**
     * 本地蓝牙适配器
     */
    private var mBluetoothAdapter: BluetoothAdapter? = null

    /**
     * 聊天服务的成员对象
     */
    private var mChatService: BluetoothChatService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (mBluetoothAdapter == null) {
            val activity = activity
            Toast.makeText(activity, "蓝牙不可用", Toast.LENGTH_LONG).show()
            activity!!.finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else if (mChatService == null) {
            setupChat()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mChatService != null) {
            mChatService!!.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mChatService != null) {
            if (mChatService!!.state == BluetoothChatService.STATE_NONE) {
                mChatService!!.start()
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false)
    }

    private fun setupChat() {
        Log.d(TAG, "setupChat()")
        mConversationArrayAdapter = ArrayAdapter(activity!!, R.layout.message)
        `in`.adapter = mConversationArrayAdapter
        edit_text_out.setOnEditorActionListener(mWriterListener)
        button_send.setOnClickListener {
            val view = view
            if (view != null) {
                val textView = view.findViewById<TextView>(R.id.edit_text_out)
                val message = textView.text.toString()
                sendMessage(message)
            }
        }
        mChatService = BluetoothChatService(activity, mHandler)

        mOutStringBuffer = StringBuffer("")
    }

    private fun ensureDiscoverable() {
        if (mBluetoothAdapter!!.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)
        }
    }

    private fun sendMessage(message: String) {
        if (mChatService!!.state != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(activity, R.string.not_connected, Toast.LENGTH_SHORT).show()
            return
        }

        if (message.isNotEmpty()) {
            val send = message.toByteArray()
            mChatService!!.write(send)

            mOutStringBuffer.setLength(0)
            edit_text_out.setText(mOutStringBuffer)
        }
    }

    private val mWriterListener =
        TextView.OnEditorActionListener { view, actionId, event ->
            if (actionId == EditorInfo.IME_NULL && event.action == KeyEvent.ACTION_UP) {
                val message = view.text.toString()
                sendMessage(message)
            }
            true
        }

    private fun setStatus(resId: Int) {
        val activity = activity ?: return
        val actionBar = activity.actionBar ?: return
        actionBar.setSubtitle(resId)
    }

    private fun setStatus(subTitle: CharSequence) {
        val activity = activity ?: return
        val actionBar = activity.actionBar ?: return
        actionBar.subtitle = subTitle
    }

    private val mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            val activity = activity
            when (msg.what) {
                Constants.MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    BluetoothChatService.STATE_CONNECTED -> {
                        setStatus(getString(R.string.title_connected_to, mConnectedDeviceName))
                        mConversationArrayAdapter.clear()
                    }
                    BluetoothChatService.STATE_CONNECTING -> setStatus(R.string.title_connecting)
                    BluetoothChatService.STATE_LISTEN, BluetoothChatService.STATE_NONE -> setStatus(
                        R.string.title_not_connected
                    )

                }
                Constants.MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    val writeMessage = String(writeBuf)
                    mConversationArrayAdapter.add("Me: $writeMessage")
                }
                Constants.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    val readMessage = String(readBuf, 0, msg.arg1)
                    mConversationArrayAdapter!!.add("$mConnectedDeviceName: $readMessage")
                }
                Constants.MESSAGE_DEVICE_NAME -> {
                    mConnectedDeviceName = msg.data.getString(Constants.DEVICE_NAME)
                    if (activity != null) {
                        Toast.makeText(
                            activity,
                            "连接到 $mConnectedDeviceName",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                Constants.MESSAGE_TOAST -> if (activity != null) {
                    Toast.makeText(
                        activity,
                        msg.data.getString(Constants.TOAST),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CONNECT_DEVICE_SECURE ->
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true)
                }
            REQUEST_CONNECT_DEVICE_INSECURE ->                 // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false)
                }
            REQUEST_ENABLE_BT ->
                if (resultCode == Activity.RESULT_OK) {
                    setupChat()
                } else {
                    Log.d(TAG, "蓝牙未启用")
                    Toast.makeText(activity, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT)
                        .show()
                    activity!!.finish()
                }
        }
    }

    private fun connectDevice(data: Intent?, secure: Boolean) {
        val address = data!!.extras?.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        mChatService!!.connect(device, secure)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.secure_connect_scan -> {
                val serverIntent = Intent(activity, DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE)
                return true
            }
            R.id.insecure_connect_scan -> {
                val serverIntent = Intent(activity, DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE)
                return true
            }
            R.id.discoverable -> {
                ensureDiscoverable()
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "BluetoothChatFragment"
        private const val REQUEST_CONNECT_DEVICE_SECURE = 1
        private const val REQUEST_CONNECT_DEVICE_INSECURE = 2
        private const val REQUEST_ENABLE_BT = 3
    }
}