package com.nooan.cardpaypasspass

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.io.File


class NfcService : HostApduService() {

    override fun onDeactivated(reason: Int) {
        Log.e("LOG onDEACTIVATED", reason.toString());
    }

    fun getData(context: Context?): List<Command> {
        var list: List<Command> = arrayListOf()
        filePath?.let {
            if (it.isNotBlank()) {
                Log.e("", "")
                list = getCommands(Uri.fromFile(File(it)).readTextFromUri(context), this::showError)
            } else {
                Toast.makeText(applicationContext, "Not found file path", Toast.LENGTH_SHORT).show()
            }
        }
        return list
    }

    private fun showError(text: String, line: String) {
        Log.e("LOG", "OnError in line: $line")
    }

    override fun processCommandApdu(apdu: ByteArray?, bundle: Bundle?): ByteArray {
        Log.i("LOG", "Received APDU: $apdu - String Representation: ${if (apdu != null) String(apdu) else "null"}")
        var index = 0

        val pref = applicationContext!!.getSharedPreferences("EMV", Context.MODE_PRIVATE)
        filePath = pref.getString("path", "EMV/")

        val commands = getData(applicationContext)
        commands.forEachIndexed { i, command ->
            if (apdu?.toHex() == command.getHexString()) {
                index = i + 1
                Log.e("LOG", "Found bytes: ${apdu.toHex()}")
                return@forEachIndexed
            }
        }
        return commands[index].split()
    }

    private var filePath: String? = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check if intent has extras
        val pref = applicationContext!!.getSharedPreferences("EMV", Context.MODE_PRIVATE)
        filePath = pref.getString("path", "EMV/")
        if (intent?.getExtras() != null) {
            // Get message
            filePath = if (intent?.getExtras().getString("path") == null) return START_STICKY else intent?.getExtras().getString("path")
        }
        return START_STICKY
    }
}

private val HEX_CHARS = "0123456789ABCDEF"

fun String.hexToByteArray(): ByteArray {

    val hexbytes = this.replace(" ", "")
    val result = ByteArray(hexbytes.length / 2)

    for (i in 0 until hexbytes.length step 2) {
        val firstIndex = HEX_CHARS.indexOf(hexbytes[i]);
        val secondIndex = HEX_CHARS.indexOf(hexbytes[i + 1]);

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }
    return result
}

val HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray()

fun ByteArray.toHex(): String {
    val result = StringBuffer()

    this.forEach {
        val octet = it.toInt()
        val firstIndex = (octet and 0xF0).ushr(4)
        val secondIndex = octet and 0x0F
        result.append(HEX_CHARS_ARRAY[firstIndex])
        result.append(HEX_CHARS_ARRAY[secondIndex])
    }

    return result.toString()
}