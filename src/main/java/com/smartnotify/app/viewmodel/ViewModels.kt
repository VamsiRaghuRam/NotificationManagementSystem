package com.smartnotify.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartnotify.app.data.api.ModelInfoResponse
import com.smartnotify.app.data.model.*
import com.smartnotify.app.data.repository.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    val notificationRepository: NotificationRepository = NotificationRepository.instance,
    val focusRepository: FocusRepository = FocusRepository.instance,
    val callRequestRepository: CallRequestRepository = CallRequestRepository.instance
) : ViewModel() {

    val focusState: StateFlow<FocusModeState> = focusRepository.focusState
    val notifications: StateFlow<List<NotificationItem>> = notificationRepository.notifications
    val activeSessionId: StateFlow<String> = notificationRepository.activeSessionId
    val callRequests: StateFlow<List<CallRequest>> = callRequestRepository.callRequests
    val backendConnected: StateFlow<Boolean> = notificationRepository.backendConnected
    val modelInfo: StateFlow<ModelInfoResponse?> = notificationRepository.modelInfo

    init {
        checkBackendHealth()
    }

    fun checkBackendHealth() {
        viewModelScope.launch {
            notificationRepository.checkBackendHealth()
        }
    }

    fun checkPermissions(context: android.content.Context) {
        focusRepository.checkPermissions(context)
    }

    fun toggleFocusMode(context: android.content.Context): Boolean {
        return focusRepository.toggleFocusMode(context)
    }

    fun submitFeedback(notificationId: String, rating: UserFeedbackRating) {
        notificationRepository.submitUserFeedback(notificationId, rating)
    }

    fun dialCaller(context: android.content.Context, phoneNumber: String, requestId: String) {
        callRequestRepository.dialCaller(context, phoneNumber, requestId)
    }

    fun dismissCallRequest(id: String) {
        callRequestRepository.dismissRequest(id)
    }
}

class FocusViewModel(
    val focusRepository: FocusRepository = FocusRepository.instance
) : ViewModel() {

    val focusState: StateFlow<FocusModeState> = focusRepository.focusState

    fun checkPermissions(context: android.content.Context) {
        focusRepository.checkPermissions(context)
    }

    fun toggleFocus(context: android.content.Context): Boolean {
        return focusRepository.toggleFocusMode(context)
    }

    fun updateCallOptions(enableCallHandling: Boolean, enableAutoSms: Boolean) {
        focusRepository.updateCallOptions(enableCallHandling, enableAutoSms)
    }
}

class HistoryViewModel(
    val notificationRepository: NotificationRepository = NotificationRepository.instance,
    val callRequestRepository: CallRequestRepository = CallRequestRepository.instance
) : ViewModel() {

    val notifications: StateFlow<List<NotificationItem>> = notificationRepository.notifications
    val activeSessionId: StateFlow<String> = notificationRepository.activeSessionId
    val callRequests: StateFlow<List<CallRequest>> = callRequestRepository.callRequests

    private val _selectedFilter = kotlinx.coroutines.flow.MutableStateFlow<PriorityLevel?>(null)
    val selectedFilter: StateFlow<PriorityLevel?> = _selectedFilter.asStateFlow()

    fun setFilter(priority: PriorityLevel?) {
        _selectedFilter.value = priority
    }

    fun submitFeedback(notificationId: String, rating: UserFeedbackRating) {
        notificationRepository.submitUserFeedback(notificationId, rating)
    }
}

class StatisticsViewModel(
    val notificationRepository: NotificationRepository = NotificationRepository.instance
) : ViewModel() {

    val notifications: StateFlow<List<NotificationItem>> = notificationRepository.notifications

    fun getStats(): AppStatistics {
        return notificationRepository.getStatistics()
    }
}

class SettingsViewModel(
    val preferencesRepository: PreferencesRepository = PreferencesRepository.instance,
    val notificationRepository: NotificationRepository = NotificationRepository.instance
) : ViewModel() {

    val alertPreferences: StateFlow<AlertPreferences> = preferencesRepository.prefs
    val backendConnected: StateFlow<Boolean> = notificationRepository.backendConnected
    val modelInfo: StateFlow<ModelInfoResponse?> = notificationRepository.modelInfo

    init {
        checkHealth()
    }

    fun checkHealth() {
        viewModelScope.launch {
            notificationRepository.checkBackendHealth()
        }
    }

    fun updatePreferences(vibrate: Boolean, screenOn: Boolean, sound: Boolean, ring: Boolean) {
        preferencesRepository.updatePreferences(vibrate, screenOn, sound, ring)
    }
}
