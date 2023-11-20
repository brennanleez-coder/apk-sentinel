package com.example.apksentinel.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apksentinel.ApkSentinel
import com.example.apksentinel.repository.AppRepository
import com.example.apksentinel.viewmodel.DashboardViewModel

class DashboardViewModelFactory(private val application: ApkSentinel,private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
