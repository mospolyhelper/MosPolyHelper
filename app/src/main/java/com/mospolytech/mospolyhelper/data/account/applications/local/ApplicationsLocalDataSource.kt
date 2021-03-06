package com.mospolytech.mospolyhelper.data.account.applications.local

import com.mospolytech.mospolyhelper.data.core.local.SharedPreferencesDataSource
import com.mospolytech.mospolyhelper.domain.account.applications.model.Application
import com.mospolytech.mospolyhelper.utils.PreferenceKeys
import com.mospolytech.mospolyhelper.utils.Result2
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ApplicationsLocalDataSource(private val prefDataSource: SharedPreferencesDataSource) {

    fun get(applications: String): Result2<List<Application>> {
        return try {
            Result2.success(Json.decodeFromString(applications))
        } catch (e: Exception) {
            Result2.failure(e)
        }
    }

    fun set(applications: List<Application>) {
        val currentInfo = Json.encodeToString(applications)
        if (getJson() != currentInfo)
            prefDataSource.set(PreferenceKeys.Applications, currentInfo)
    }

    fun getJson(): String {
        return prefDataSource.get(PreferenceKeys.Applications, "")
    }
}