package com.example.apksentinel.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.apksentinel.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.liveData
import com.example.apksentinel.ApkSentinel

class DashboardViewModel(private val application: ApkSentinel, private val repository: AppRepository) : ViewModel() {

    private val app: ApkSentinel = application

    val isInitialized: LiveData<Boolean> = app.isInitialized

    val systemAppsCount = liveData(Dispatchers.IO) {
        emit(repository.getSystemAppsCount())
    }

    val nonSystemAppsCount = liveData(Dispatchers.IO) {
        emit(repository.getNonSystemAppsCount())
    }
}
