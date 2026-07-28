package com.toy.controller

import android.bluetooth.*
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var advertiser: BluetoothLeAdvertiser
    private var advertisingSet: AdvertisingSet? = null
    private val REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bm = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bm.adapter
        if (!adapter.isEnabled) {
            // 简单提示，实际上需要请求开启蓝牙，这里假设已开启
        }
        advertiser = adapter.bluetoothLeAdvertiser

        checkPermissions()

        findViewById<Button>(R.id.btn_low).setOnClickListener { sendCommand(0x33) } // 51
        findViewById<Button>(R.id.btn_mid).setOnClickListener { sendCommand(0x4D) } // 77
        findViewById<Button>(R.id.btn_high).setOnClickListener { sendCommand(0x64) } // 100
        findViewById<Button>(R.id.btn_stop).setOnClickListener { sendStop() }
    }

    private fun sendCommand(strength: Int) {
        val random = Random()
        val payload = byteArrayOf(
            random.nextInt(256).toByte(), random.nextInt(256).toByte(), // 随机2字节
            0x02, strength.toByte(), 0x00, 0x00, 0x64, 0x00, // 固定格式
            0x01, 0x53, 0x23, 0x00 // 尾部
        )
        startAdvertising(payload)
    }

    private fun sendStop() {
        val random = Random()
        // 停止：随机2字节 + 7个0x00 + 01 53 23 00
        val payload = byteArrayOf(
            random.nextInt(256).toByte(), random.nextInt(256).toByte(),
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x01, 0x53, 0x23, 0x00
        )
        startAdvertising(payload)
    }

    private fun startAdvertising(payload: ByteArray) {
        stopAdvertising()

        val params = AdvertisingSetParameters.Builder()
            .setLegacyMode(true)
            .setConnectable(false)
            .setInterval(160)
            .setTxPowerLevel(BluetoothAdapter.TX_POWER_MEDIUM)
            .build()

        // 厂商ID 0xAC04（从你的日志里提取的）
        val data = AdvertiseData.Builder()
            .addManufacturerData(0xAC04, payload)
            .build()

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        advertiser.startAdvertisingSet(params, data, null, null, null, object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
                this@MainActivity.advertisingSet = advertisingSet
                if (status == AdvertisingSetCallback.ADVERTISE_SUCCESS) {
                    runOnUiThread { /* 可以弹个Toast，这里忽略 */ }
                }
            }
        })
    }

    private fun stopAdvertising() {
        advertisingSet?.let {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
                advertiser.stopAdvertisingSet(it)
            }
        }
        advertisingSet = null
    }

    private fun checkPermissions() {
        val permissions = listOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_CODE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAdvertising()
    }
}
