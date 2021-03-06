package com.mospolytech.mospolyhelper.features.ui.account.marks

import androidx.lifecycle.ViewModel
import com.mospolytech.mospolyhelper.domain.account.auth.usecase.AuthUseCase
import com.mospolytech.mospolyhelper.domain.account.marks.model.Marks
import com.mospolytech.mospolyhelper.domain.account.marks.usecase.MarksUseCase
import com.mospolytech.mospolyhelper.utils.Result2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import org.koin.core.component.KoinComponent

class MarksViewModel(
    private val useCase: MarksUseCase,
    private val authUseCase: AuthUseCase
    ) : ViewModel(), KoinComponent {

    val marks = MutableStateFlow<Result2<Marks>>(Result2.loading())
    val auth = MutableStateFlow<Result2<String>?>(null)

    suspend fun refresh() {
        authUseCase.refresh().collect {
            auth.value = it
        }
    }

    suspend fun downloadInfo() {
        useCase.getInfo().collect {
            marks.value = it
        }
    }

    suspend fun getInfo() {
        useCase.getLocalInfo().collect {
            marks.value = it
        }
        //marks.value = Result.loading()
        useCase.getInfo().collect {
            marks.value = it
        }
    }

}