package com.editlyai.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.editlyai.app.data.model.UserState
import com.editlyai.app.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)

    val userState: StateFlow<UserState> = userRepository.userStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserState())

    init {
        viewModelScope.launch { userRepository.resetDailyCreditsIfNeeded() }
    }

    fun onRewardedAdEarned() {
        viewModelScope.launch { userRepository.addRewardedCredit() }
    }
}
