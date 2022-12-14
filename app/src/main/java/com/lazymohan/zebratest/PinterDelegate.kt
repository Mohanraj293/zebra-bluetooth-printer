package com.lazymohan.zebratest

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.zebra.sdk.comm.BluetoothConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.printer.PrinterStatus
import com.zebra.sdk.printer.SGD
import com.zebra.sdk.printer.ZebraPrinter
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException
import com.zebra.sdk.printer.ZebraPrinterLinkOs

class PinterDelegate(
  private val itemData: MutableList<PrintContentModel>,
  private val macAddress: String,
  private val context: View,
  private val listener: EnablePrintButton
) {
  private lateinit var connection: Connection
  private var printer: ZebraPrinter? = null
  private fun connect(): ZebraPrinter? {
    listener.printBtnEnabled(false)
    setStatus("Connecting...")
    try {
      connection = BluetoothConnection(macAddress)
      connection.open()
      setStatus("Connected..!")
    } catch (E: ConnectionException) {
      setStatus("Select the desired device and try again..!")
      disconnect()
    }

    var zebraPrinter: ZebraPrinter? = null

    if (connection.isConnected) {
      try {
        zebraPrinter = ZebraPrinterFactory.getInstance(connection)
        setStatus("Determining Printer Language")
        val pl = SGD.GET("device.languages", connection)
        setStatus("Printer Language $pl")
      } catch (ex: Exception) {
        when (ex) {
          is ConnectionException, is ZebraPrinterLanguageUnknownException -> {
            setStatus("Unknown Printer Language")
            zebraPrinter = null
            disconnect()
          }
        }
      }
    }
    return zebraPrinter
  }

  private fun getConfigLabel(
    itemNum: String,
    description: String,
    noOfCopies: Int
  ): String {
    SGD.SET("device.language", "zpl", connection)
    return """
      ^XA
      ^FX^CF0,20^PW400^LL300
      ^FS^FO0,50^FB400,1,0,C^FD#$itemNum
      ^FS^FO0,90^FB400,5,,,C^FD$description
      ^FS^FO0,150^FB400,1,0,C^GB420,1,1
      ^FS^FO140,160^BY2,2^BQN,2,5^FD000$itemNum
      ^FS^PQ$noOfCopies^XZ
      """.trimIndent()
  }

  private fun disconnect() {
    try {
      connection.close()
    } catch (e: ConnectionException) {
      setStatus("COMM Error! Disconnected")
    } finally {
      listener.printBtnEnabled(true)
    }
  }

  private fun sendTestLabel() {
    try {
      val linkOsPrinter: ZebraPrinterLinkOs = ZebraPrinterFactory.createLinkOsPrinter(printer)
      val printerStatus: PrinterStatus = linkOsPrinter.currentStatus

      if (printerStatus.isReadyToPrint) {
        itemData.forEach {
          val configLabel = getConfigLabel(it.itemNum,it.description,it.noOfCopies)
          connection.write(configLabel.toByteArray())
        }
      } else if (printerStatus.isHeadOpen) {
        setStatus("Printer Head Open")
      } else if (printerStatus.isPaused) {
        setStatus("Printer is Paused")
      } else if (printerStatus.isPaperOut) {
        setStatus("Printer Media Out")
      }
      if (connection is BluetoothConnection) {
        val friendlyName = (connection as BluetoothConnection).friendlyName
        setStatus(friendlyName)
      }
    } catch (e: ConnectionException) {
      setStatus(e.message.toString())
    } finally {
      disconnect()
    }
  }

  fun doConnectionTest() {
    printer = connect()
    if (printer != null) {
      sendTestLabel()
    } else {
      disconnect()
    }
  }

  private fun setStatus(
    statusMessage: String
  ) {
    Handler(Looper.getMainLooper()).post {
      Snackbar.make(context, statusMessage, Toast.LENGTH_SHORT).show()
    }
  }

  interface EnablePrintButton {
    fun printBtnEnabled(enabled: Boolean)
  }
}