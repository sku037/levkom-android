package com.example.levkomandroid.viewmodel

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
            // Обработка полученного ID нового маршрута
        }
    }

    fun deleteRoute(routeId: Int) = viewModelScope.launch {
        repository.deleteRoute(routeId).collect {
            // Обновление UI или списка маршрутов после удаления
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
            // Обновление списка адресов в UI
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