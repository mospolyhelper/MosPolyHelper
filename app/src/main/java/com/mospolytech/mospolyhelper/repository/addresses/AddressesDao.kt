package com.mospolytech.mospolyhelper.repository.addresses

import android.util.Log
import com.beust.klaxon.Klaxon
import com.mospolytech.mospolyhelper.TAG
import com.mospolytech.mospolyhelper.utils.AssetProvider
import com.mospolytech.mospolyhelper.utils.ContextProvider
import java.io.File
import java.lang.Exception
import java.net.URL

class AddressesDao {
    companion object {
        const val ADDRESSES_FOLDER = "addresses"
        const val ADDRESSES_FILE = "cached_addresses"
        const val ADDRESSES_URL =
            "https://gist.githubusercontent.com/tipapro/f19b581ea759cacde6ff674b516c552a/raw/mospolyhelper-addresses.json"
    }

    private fun readAddresses(): Addresses? {
        val filePath = ContextProvider.getFilesDir().resolve(ADDRESSES_FOLDER).resolve(
            ADDRESSES_FILE
        )  // TODO: Add directory
        return if (!filePath.exists()) {
            null
        } else {
            val serBuildings = filePath.readText()
            Klaxon().parse<Addresses>(serBuildings)
        }
    }

    private fun downloadAddresses(): Addresses? {
        return try {
            val serBuildings = URL(ADDRESSES_URL).readText()
            Klaxon().parse<Addresses>(serBuildings)
        } catch(e: Exception) {
            Log.e(TAG, "Addresses downloading and parsing error", e)
            null
        }
    }

    private fun saveAddresses(buildings: Addresses) {
        val filePath = File(ContextProvider.getFilesDir().resolve(ADDRESSES_FOLDER).resolve(
            ADDRESSES_FILE
        ),
            ADDRESSES_FILE
        )
        filePath.delete()
        val str = Klaxon().toJsonString(buildings)
        filePath.parentFile?.mkdirs()
        filePath.createNewFile()
        filePath.writeText(str)
    }

    private fun getAddressesFromAssets(): Addresses? {
        return Klaxon().parse<Addresses>(AssetProvider.getAsset("addresses.json")!!)
    }

    fun getAddresses(downloadNew: Boolean): Addresses? {
        var addresses: Addresses? = null
        if (!downloadNew) {
            try {
                addresses = readAddresses()
            } catch (ex: Exception) {
                val a =1
            }
        }
        if (addresses == null) {
            addresses = downloadAddresses()
            if (addresses == null) {
                if (downloadNew) {
                    try {
                        addresses = readAddresses()
                        if (addresses != null) {
                            return addresses
                        }
                    } catch (ex: Exception) {

                    }
                }
                addresses = getAddressesFromAssets()
            } else {
                saveAddresses(addresses)
            }
        }
        return addresses
    }
}