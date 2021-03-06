package com.mospolytech.mospolyhelper.data.schedule.remote

import com.mospolytech.mospolyhelper.data.schedule.api.GroupInfoApi
import com.mospolytech.mospolyhelper.domain.schedule.model.info.GroupsInfo
import com.mospolytech.mospolyhelper.utils.Result0
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class GroupInfoRemoteDataSource(
    private val client: GroupInfoApi
) {
    suspend fun get(): Result0<GroupsInfo> {
        return try {
            val res = client.get()
            Result0.Success(Json.decodeFromString(res))
        } catch (e: Exception) {
            Result0.Failure(e)
        }
    }
}