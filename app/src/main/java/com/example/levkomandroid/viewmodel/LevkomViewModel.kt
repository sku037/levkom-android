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

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _routeGeometry = MutableLiveData<String>()
    val routeGeometry: LiveData<String> = _routeGeometry

    private val _failedAddresses = MutableLiveData<List<Address>>()
    val failedAddresses: LiveData<List<Address>> = _failedAddresses

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

    fun loadAddressesForRoute(routeId: Int) = viewModelScope.launch {
        repository.getAddressesByRouteIdWithOrder(routeId).collect { addresses ->
            _addresses.postValue(addresses)
        }
    }

    fun importAddresses(addresses: List<Address>, routeId: Int) = viewModelScope.launch {
        repository.importAddresses(addresses, routeId).collect { result ->
            if (!result.failed.isNullOrEmpty()) {
                _message.postValue(result.failed)
            } else {
                _message.postValue("Success: ${result.success}")
            }
            _failedAddresses.postValue(result.failedAddresses ?: emptyList())
        }
    }

    fun deleteAddress(addressId: Int, routeId: Int) = viewModelScope.launch {
        repository.deleteAddress(addressId, routeId).collect { isSuccess ->
            if (isSuccess) {
                loadAddressesForRoute(routeId)
            } else {
                // Обработка ошибок
                _error.postValue("Error deleting address.")
            }
        }
    }

    fun calculateRoute(routeId: Int) = viewModelScope.launch {
        repository.calculateRoute(routeId).collect { isSuccess ->
        if (isSuccess) {
            loadAddressesAndRoute(routeId)
        } else {
            _error.postValue("Error calculating route.")
        }
        }
    }

    private suspend fun loadAddressesAndRoute(routeId: Int) {
        repository.getAddressesByRouteIdWithOrder(routeId).collect { addresses ->
            _addresses.postValue(addresses)
            val geometry = repository.getRouteGeometryByRouteId(routeId)
            if (!geometry.isNullOrEmpty()) {
                _routeGeometry.postValue(geometry)
            }
        }
    }

    fun loadAddressesAndRoutePublic(routeId: Int) {
        viewModelScope.launch {
            loadAddressesAndRoute(routeId)
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