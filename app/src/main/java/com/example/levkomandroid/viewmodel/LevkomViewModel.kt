package com.example.levkomandroid.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.levkomandroid.network.Address
import com.example.levkomandroid.network.DeliveryAddr
import com.example.levkomandroid.network.RouteDto
import com.example.levkomandroid.repository.LevkomRepository
import kotlinx.coroutines.launch

class LevkomViewModel(private val repository: LevkomRepository) : ViewModel() {

    private val _routes = MutableLiveData<List<RouteDto>>()
    val routes: LiveData<List<RouteDto>> = _routes

    private val _newRouteId = MutableLiveData<Int>()
    val newRouteId: LiveData<Int> = _newRouteId

    private val _addresses = MutableLiveData<List<DeliveryAddr>>()
    val addresses: LiveData<List<DeliveryAddr>> = _addresses

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    init {
        loadRoutes()
    }

    private fun loadRoutes() = viewModelScope.launch {
        repository.getRoutes().collect { routeList ->
            _routes.postValue(routeList)
        }
    }

    fun createNewRoute() = viewModelScope.launch {
        repository.createRoute().collect { newRouteId ->
            if (newRouteId > 0) {
                _newRouteId.postValue(newRouteId)
                loadAddressesForRoute(newRouteId)
            } else {
                _newRouteId.postValue(-1)
            }
        }
    }

    fun deleteRoute(routeId: Int) = viewModelScope.launch {
        repository.deleteRoute(routeId).collect { isSuccess ->
            if (isSuccess) {
                // Reload routelist for update UI
                loadRoutes()
            } else {
                // Error handling
                _error.postValue("Error deleting route.")
            }
        }
    }

    fun importAddresses(addresses: List<Address>, routeId: Int) = viewModelScope.launch {
        addresses.forEach { address ->
            repository.importAddress(address, routeId).collect {
                // Обработка результатов импорта адреса
            }
        }
    }

    fun loadAddressesForRoute(routeId: Int) = viewModelScope.launch {
        repository.getAddressesByRouteIdWithOrder(routeId).collect { addresses ->
            _addresses.postValue(addresses)
        }
    }
}

class LevkomViewModelFactory(
    private val repository: LevkomRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LevkomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LevkomViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}