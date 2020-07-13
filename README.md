
# 一个蓝牙聊天的例子

copy自以下[项目][1]

## 学习到的新东西

+ ViewAnimator
+ Handler，传递消息
+ 与蓝牙连接相关的一些东西

## BUG

+ 点击连接后只是单纯的弹出一条消息显示连接成功，但未实际连接
+ 写入时遇到 `java.io.IOException: Broken pipe`，由于所学浅薄，未曾找到解决办法，因此此项目只是一个花样子，无实际功能

## 关于蓝牙的使用

### 蓝牙权限

```xml
<manifest ... >
  <uses-permission android:name="android.permission.BLUETOOTH" />
  <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

  <!-- If your app targets Android 9 or lower, you can declare
       ACCESS_COARSE_LOCATION instead. -->
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
  ...
</manifest>
```

### 设置蓝牙

#### 1. 获取 `BluetoothAdapter`

```kotlin
val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
if (bluetoothAdapter == null) {
    // Device doesn't support Bluetooth
}
```

#### 2. 启用蓝牙

使用  `isEnabled()` 检查是否已启用蓝牙，若未启用，可使用以下方法启用蓝牙：

```kotlin
if (bluetoothAdapter?.isEnabled == false) {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    // REQUEST_ENABLE_BT为自定义常量
    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
}
```

> 启用可检测性即可自动启用蓝牙。如果计划在执行蓝牙 Activity 之前一直启用设备的可检测性，则可以跳过上述步骤 2。

#### 侦听蓝牙状态更改广播

每当蓝牙状态发生变化时，系统会发出 `ACTION_STATE_CHANGED` 广播Intent。此广播包含额外字段 `EXTRA_STATE` 和 `EXTRA_PREVIOUS_STATE`，二者分别包含新的和旧的蓝牙状态。这些额外字段可能为以下值：`STATE_TURNING_ON`、`STATE_ON`、`STATE_TURNING_OFF`和 `STATE_OFF`。

### 查找设备

#### 查找已配对设备

**`getBondedDevices()`**

```kotlin
val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
pairedDevices?.forEach { device ->
    val deviceName = device.name
    val deviceHardwareAddress = device.address // MAC address
}
```

#### 发现设备

**`startDiscovery()`**
为了发现设备，必须针对`ACTION_FOUND` Intent 注册一个 BroadcastReceiver，以便接收每台发现的设备的相关信息。系统会为每台设备广播此 Intent。Intent 包含额外字段 `EXTRA_DEVICE` 和 `EXTRA_CLASS`，二者又分别包含 `BluetoothDevice` 和 `BluetoothClass`。以下代码段展示如何在发现设备时通过注册来处理广播：

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    ...

    // Register for broadcasts when a device is discovered.
    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
    registerReceiver(receiver, filter)
}

// Create a BroadcastReceiver for ACTION_FOUND.
private val receiver = object : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action: String = intent.action
        when(action) {
            BluetoothDevice.ACTION_FOUND -> {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
            }
        }
    }
}

override fun onDestroy() {
    super.onDestroy()
    ...

    // Don't forget to unregister the ACTION_FOUND receiver.
    unregisterReceiver(receiver)
}
```

#### 启用可检测性

使用 `ACTION_REQUEST_DISCOVERABLE` Intent 调用 `startActivityForResult(Intent, int)`。

```kotlin
val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
}
startActivity(discoverableIntent)
```

此后设备将在分配的时间内以静默方式保持可检测到模式。可以为 `ACTION_SCAN_MODE_CHANGED` Intent 注册 BroadcastReceiver。此 Intent 将包含额外字段 `EXTRA_SCAN_MODE` 和 `EXTRA_PREVIOUS_SCAN_MODE`，二者分别提供新的和旧的扫描模式。每个 Extra 属性可能拥有以下值：

+ `SCAN_MODE_CONNECTABLE_DISCOVERABLE` 设备处于可检测到模式。
+ `SCAN_MODE_CONNECTABLE` 设备未处于可检测到模式，但仍能收到连接。
+ `SCAN_MODE_NONE` 设备未处于可检测到模式，且无法收到连接。

> 发起对远程设备的连接无需启用设备可检测性，只有当需要应用对接受传入连接的服务器套接字进行托管时，才有必要启用可检测性。

### 连接设备

#### 作为服务器连接

1. 通过调用 `listenUsingRfcommWithServiceRecord()` 获取 `BluetoothServerSocket`。
2. 通过调用 `accept()` 开始侦听连接请求。
3. 使用完成后，调用 `close()`。

#### 作为客户端连接

1. 使用 `BluetoothDevice`，通过调用 `createRfcommSocketToServiceRecord(UUID)` 获取 `BluetoothSocket`。
2. 通过调用 `connect()` 发起连接。

#### 管理连接

1. 使用 `getInputStream()` 和 `getOutputStream()`，分别获取通过套接字处理数据传输的 `InputStream` 和 `OutputStream`。
2. 使用 `read(byte[])` 和 `write(byte[])` 读取数据以及将其写入数据流。

[1]: https://github.com/loipn1804/android-BluetoothChat-master
