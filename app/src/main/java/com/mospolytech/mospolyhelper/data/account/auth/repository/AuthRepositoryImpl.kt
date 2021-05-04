package com.mospolytech.mospolyhelper.data.account.auth.repository

import com.auth0.android.jwt.JWT
import com.mospolytech.mospolyhelper.data.account.auth.local.AuthJwtLocalDataSource
import com.mospolytech.mospolyhelper.data.account.auth.remote.AuthJwtRemoteDataSource
import com.mospolytech.mospolyhelper.data.account.auth.remote.AuthRemoteDataSource
import com.mospolytech.mospolyhelper.data.core.local.SharedPreferencesDataSource
import com.mospolytech.mospolyhelper.domain.account.auth.model.JwtModel
import com.mospolytech.mospolyhelper.domain.account.auth.repository.AuthRepository
import com.mospolytech.mospolyhelper.utils.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.contracts.ExperimentalContracts

class AuthRepositoryImpl(
    private val dataSourceJWT: AuthJwtRemoteDataSource,
    private val authJwtLocalDataSource: AuthJwtLocalDataSource,
    private val prefDataSource: SharedPreferencesDataSource
) : AuthRepository {

    @ExperimentalContracts
    override suspend fun logIn(login: String, password: String) = flow {
        val token = dataSourceJWT.authJwt(login, password)
        emit(token.map {
            authJwtLocalDataSource.set(it.accessToken)
            val sessionId = authJwtLocalDataSource.get()?.getSessionId()!!
            prefDataSource.setString(PreferenceKeys.SessionId, sessionId)
            prefDataSource.setString(PreferenceKeys.RefreshToken, it.refreshToken)
            return@map sessionId
        })
    }

    @ExperimentalContracts
    override suspend fun refresh(): Flow<Result<String>> = flow {
        if (authJwtLocalDataSource.get()?.isExpired() == true) {
            val oldToken = prefDataSource.getString(PreferenceKeys.AccessToken, "")
            val refresh = prefDataSource.getString(PreferenceKeys.RefreshToken, "")
            val newToken = dataSourceJWT.refresh(oldToken, refresh)
            newToken.onSuccess {
                authJwtLocalDataSource.set(it.replace("\"", ""))
                val sessionId = authJwtLocalDataSource.get()?.getSessionId()!!
                prefDataSource.setString(PreferenceKeys.SessionId, sessionId)
            }
//            val sessionId = authJwtLocalDataSource.get()?.getSessionId()!!
//            prefDataSource.setString(PreferenceKeys.SessionId, sessionId)
//            emit(newToken.map {
//                authJwtLocalDataSource.set(it)
//                val sessionId = authJwtLocalDataSource.get()?.getSessionId()!!
//                prefDataSource.setString(PreferenceKeys.SessionId, sessionId)
//                return@map sessionId
//            })
        }
    }

    override fun getAvatar() = authJwtLocalDataSource.get()?.getAvatar() ?: ""

    override fun getPermissions() = authJwtLocalDataSource.get()?.getPermissions() ?: emptyList()

    override fun getFio() = authJwtLocalDataSource.get()?.getName() ?: ""

    override fun logOut() {
        prefDataSource.setString(PreferenceKeys.SessionId, PreferenceDefaults.SessionId)
        prefDataSource.setString(PreferenceKeys.Info, "")
        authJwtLocalDataSource.clear()
    }

    override fun getLogin(): String {
        return prefDataSource.getString(PreferenceKeys.Login, PreferenceDefaults.Login)
    }

    override fun setLogin(value: String) {
        prefDataSource.setString(PreferenceKeys.Login, value)
    }

    override fun getPassword(): String {
        return prefDataSource.getString(PreferenceKeys.Password, PreferenceDefaults.Password)
    }

    override fun setPassword(value: String) {
        prefDataSource.setString(PreferenceKeys.Password, value)
    }

    override fun getSaveLogin(): Boolean {
        return prefDataSource.getBoolean(PreferenceKeys.SaveLogin, PreferenceDefaults.SaveLogin)
    }

    override fun setSaveLogin(value: Boolean) {
        prefDataSource.setBoolean(PreferenceKeys.SaveLogin, value)
    }

    override fun getSavePassword(): Boolean {
        return prefDataSource.getBoolean(PreferenceKeys.SavePassword, PreferenceDefaults.SavePassword)
    }

    override fun setSavePassword(value: Boolean) {
        prefDataSource.setBoolean(PreferenceKeys.SavePassword, value)
    }
}