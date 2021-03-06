package com.mospolytech.mospolyhelper.data.account.dialogs.local

import android.util.Log
import com.mospolytech.mospolyhelper.data.core.local.SharedPreferencesDataSource
import com.mospolytech.mospolyhelper.data.utils.getFromJson
import com.mospolytech.mospolyhelper.data.utils.setAsJson
import com.mospolytech.mospolyhelper.domain.account.dialogs.model.DialogModel
import com.mospolytech.mospolyhelper.utils.PreferenceKeys
import com.mospolytech.mospolyhelper.utils.Result2

class DialogsLocalDataSource(private val prefDataSource: SharedPreferencesDataSource) {

    fun get(): Result2<List<DialogModel>> {
        return prefDataSource.getFromJson<List<DialogModel>>(PreferenceKeys.Dialogs)?.let {
                Result2.success(it)
            } ?: let {
                Log.e("JSON","Can't get local dialogs")
                Result2.success(emptyList())
            }
    }

    fun set(dialogs: List<DialogModel>) {
        if (prefDataSource.getFromJson<List<DialogModel>>(PreferenceKeys.Dialogs) != dialogs) {
            prefDataSource.setAsJson(PreferenceKeys.Dialogs, dialogs)
        }
    }
}