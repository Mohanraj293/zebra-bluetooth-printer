package com.lazymohan.zebratest

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.lazymohan.zebratest.PinterDelegate.EnablePrintButton
import com.lazymohan.zebratest.databinding.ActivityMainBinding
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.PrinterStatus
import com.zebra.sdk.printer.SGD
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException
import com.zebra.sdk.printer.ZebraPrinterLinkOs

class MainActivity : AppCompatActivity(), EnablePrintButton {

  private lateinit var binding: ActivityMainBinding
  private val permissionCode = 1

  @SuppressLint("HardwareIds")
  @RequiresApi(VERSION_CODES.S)
  override fun onCreate(
    savedInstanceState: Bundle?,
  ) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    if (ContextCompat.checkSelfPermission(
            this, permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
    ) {
      requestBluetoothPermission()
    }

    binding.testButton.setOnClickListener {
      val bluetoothManager: BluetoothManager =
        applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
      val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
      if (!bluetoothAdapter.isEnabled) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        resultLauncher.launch(enableBtIntent)
      } else {
        showPrintDialog()
      }
    }
  }

  private fun showPrintDialog() {
    val alertDialog = AlertDialog.Builder(this)
    val view = layoutInflater.inflate(R.layout.edit_text_printer, null)
    val et = view.findViewById<EditText>(R.id.no_of_copies)
    alertDialog
        .setView(view)
        .setTitle("No of Copies")
        .setPositiveButton("Done") { _, _ ->
          if (et.text.isNullOrEmpty() || et.text.toString().toInt() == 0) {
            Toast.makeText(this, "Please provide valid copies", Toast.LENGTH_SHORT).show()
          } else {
            val noOfCopies = et.text.toString().toInt()
            val intent = Intent(applicationContext, PrinterActivity::class.java)
            intent.putExtra("copies", noOfCopies)
            resultLauncher.launch(intent)
          }
        }
        .setNegativeButton("Cancel") { dialog, _ ->
          dialog.dismiss()
        }
        .setCancelable(false)
        .show()
  }

  private var resultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val address = result.data?.getStringExtra("macAddress")
      val copies: Int = result.data!!.getIntExtra("copies", 1)
      Thread {
        kotlin.run {
          val itemData = mutableListOf<PrintContentModel>()
          itemData.add(
              PrintContentModel("# 0-003145", "Tubing, copper - 11-6 in dx - in wall", copies)
          )
          itemData.add(
              PrintContentModel("# 0-003146", "Tubing, copper - 11-6 in dx - in wall123", copies)
          )
          val printerDelegate = address?.let {
            PinterDelegate(
                itemData, it,
                context = binding.root, this
            )
          }
          printerDelegate?.doConnectionTest()
        }
      }.start()
    }
  }

  @RequiresApi(VERSION_CODES.S)
  private fun requestBluetoothPermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission.BLUETOOTH_CONNECT)) {
      AlertDialog.Builder(this)
          .setTitle("Permission Needed")
          .setMessage("Zebra Requires Bluetooth Connectivity")
          .setPositiveButton("Ok") { _, _ ->
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission.BLUETOOTH_CONNECT),
                permissionCode
            )
          }
          .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
          }
          .create()
          .show()
    } else {
      ActivityCompat.requestPermissions(
          this, arrayOf(permission.BLUETOOTH_CONNECT),
          permissionCode
      )
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == permissionCode) {
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun printBtnEnabled(enabled: Boolean) {
    runOnUiThread {
      binding.testButton.isEnabled = enabled
    }
  }
}