package com.mospolytech.mospolyhelper.data.account.info.local

import com.mospolytech.mospolyhelper.data.core.local.SharedPreferencesDataSource
import com.mospolytech.mospolyhelper.domain.account.info.model.Info
import com.mospolytech.mospolyhelper.utils.PreferenceKeys
import com.mospolytech.mospolyhelper.utils.Result2
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class InfoLocalDataSource(private val prefDataSource: SharedPreferencesDataSource) {
    fun get(info: String): Result2<Info> {
        return try {
                Result2.success(Json.decodeFromString(info))
        } catch (e: Exception) {
            Result2.failure(e)
        }
    }

    fun set(info: Info) {
        val currentInfo = Json.encodeToString(info)
        if (getJson() != currentInfo)
            prefDataSource.set(PreferenceKeys.Info, currentInfo)
    }

    fun getJson(): String {
        return prefDataSource.get(PreferenceKeys.Info, "")
    }
}