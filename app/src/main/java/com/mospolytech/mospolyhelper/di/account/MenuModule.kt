package com.mospolytech.mospolyhelper.di.account

import com.mospolytech.mospolyhelper.data.account.auth.api.AuthJwtHerokuClient
import com.mospolytech.mospolyhelper.data.account.auth.local.AuthJwtLocalDataSource
import com.mospolytech.mospolyhelper.data.account.auth.remote.AuthJwtRemoteDataSource
import com.mospolytech.mospolyhelper.data.account.auth.remote.AuthRemoteDataSource
import com.mospolytech.mospolyhelper.data.account.auth.repository.AuthRepositoryImpl
import com.mospolytech.mospolyhelper.domain.account.auth.repository.AuthRepository
import com.mospolytech.mospolyhelper.domain.account.auth.usecase.AuthUseCase
import com.mospolytech.mospolyhelper.features.ui.account.menu.MenuViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val menuModule = module {
    single { AuthJwtHerokuClient(get(named("accountHerokuClient"))) }
    single { AuthJwtRemoteDataSource(get()) }
    single { AuthJwtLocalDataSource(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single { AuthUseCase(get()) }
    viewModel { MenuViewModel(get()) }
}