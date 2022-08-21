package com.lazymohan.zebratest

import android.Manifest.permission
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.lazymohan.zebratest.PairedDeviceAdapter.DeviceItemListener
import com.lazymohan.zebratest.databinding.ActivityPrinterBinding

class PrinterActivity : AppCompatActivity(), DeviceItemListener {
  private lateinit var binding: ActivityPrinterBinding
  private var pairedArrayList = mutableListOf<PairedDeviceModel>()
  private var noOfCopies = 1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityPrinterBinding.inflate(layoutInflater)
    setContentView(binding.root)
    val bluetoothManager: BluetoothManager =
      applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    noOfCopies = intent.getIntExtra("copies",1)
    val pairedDevices = bluetoothAdapter.bondedDevices
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (ActivityCompat.checkSelfPermission(
              this, permission.BLUETOOTH_CONNECT
          ) == PackageManager.PERMISSION_GRANTED
      ) {
        pairedDevices.map { device ->
          val pairedDeviceModel = PairedDeviceModel(device.name, device.address)
          pairedArrayList.add(pairedDeviceModel)
        }
      }
    } else if (ActivityCompat.checkSelfPermission(
            this, permission.BLUETOOTH
        ) == PackageManager.PERMISSION_GRANTED
    ) {
      pairedDevices.map { device ->
        val pairedDeviceModel = PairedDeviceModel(device.name, device.address)
        pairedArrayList.add(pairedDeviceModel)
      }
    }
    binding.recyclerView.layoutManager = LinearLayoutManager(this)
    binding.recyclerView.adapter = PairedDeviceAdapter(pairedArrayList, this)
    binding.recyclerView.visibility = if (pairedArrayList.isNotEmpty()) View.VISIBLE else View.GONE
  }

  override fun getDevice(model: PairedDeviceModel) {
    val intent = Intent(this, MainActivity::class.java)
    intent.putExtra("friendlyName", model.friendlyName)
    intent.putExtra("macAddress", model.macAddress)
    intent.putExtra("copies", noOfCopies)
    setResult(RESULT_OK, intent)
    finish()
  }
}
