package com.revolution.bluetooth.file

import android.net.Uri
import java.io.FileInputStream
import java.security.MessageDigest

class MD5Checker {

    private companion object {
        const val BUFFER_SIZE = 1024
    }

    fun calculateMD5Hash(file: Uri): ByteArray {
        val md5 = MessageDigest.getInstance("MD5")
        val fileInputStream = FileInputStream(file.path)
        val buffer = ByteArray(BUFFER_SIZE)
        var numRead: Int

        do {
            numRead = fileInputStream.read(buffer)
            if (numRead > 0) {
                md5.update(buffer, 0, numRead)
            }
        } while (numRead != -1)

        fileInputStream.close()
        return md5.digest()
    }
}
