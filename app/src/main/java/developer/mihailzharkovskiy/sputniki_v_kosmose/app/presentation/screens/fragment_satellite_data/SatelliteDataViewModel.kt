package developer.mihailzharkovskiy.sputniki_v_kosmose.app.presentation.screens.fragment_satellite_data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.Coordinates
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.domain.core_calculations.predict.Satellite
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.domain.usecase.satellite_above_the_user.SatAboveTheUserDomainModel
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.domain.usecase.satellite_position.SatellitePositionUseCase
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.framework.compas.CompassEvent
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.presentation.common.resource.Resource
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.presentation.data_state.DataState
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.presentation.framework.Compass
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.presentation.framework.UserLocationSource
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.presentation.screens.fragment_satellite_data.mapper.toInitUiModel
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.presentation.screens.fragment_satellite_data.mapper.toSatDataUiModel
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.presentation.screens.fragment_satellite_data.model.SatDataUiModel
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.presentation.screens.fragment_satellite_data.model.SatelliteDataInitUiModel
import developer.mihailzharkovskiy.sputniki_v_kosmose.app.presentation.screens.fragment_satellite_data.state.SatDataUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SatelliteDataViewModel @Inject constructor(
    private val userLocation: UserLocationSource,
    private val satPositionUseCase: SatellitePositionUseCase,
    private val resource: Resource,
) : ViewModel(), Compass.AzimuthListener {

    private val userPos = userLocation.getUserLocation()

    private val _progress = MutableStateFlow<SatDataUiState>(SatDataUiState.NoData)
    val progress: StateFlow<SatDataUiState> get() = _progress.asStateFlow()

    private val _compassEvent = MutableStateFlow<CompassEvent>(CompassEvent.NoData)
    val compassEvent: StateFlow<CompassEvent> = _compassEvent.asStateFlow()

    fun initScreen(satellite: SatAboveTheUserDomainModel): SatelliteDataInitUiModel {
        sendPassData(satellite)
        return satellite.toInitUiModel(resource)
    }

    private fun sendPassData(satPass: SatAboveTheUserDomainModel) = viewModelScope.launch {
        while (isActive) {
            val data = getSatelliteData(satPass.satellite, userPos, Date())
            val progress = calculateProgress(satPass.startTime, satPass.endTime)
            val state =
                if (progress.toInt() in 1 until 100) SatDataUiState.SatVisible(progress,
                    DataState.success(data))
                else SatDataUiState.SatInvisible(DataState.success(data))
            _progress.value = state
            delay(1000)
        }
    }

    private suspend fun getSatelliteData(
        sat: Satellite,
        userPos: Coordinates,
        date: Date,
    ): SatDataUiModel {
        val satelliteData = satPositionUseCase.getPositionSat(sat, userPos, date.time)
        return satelliteData.toSatDataUiModel(resource)
    }

    private fun calculateProgress(timeStart: Long, losTime: Long): Float {
        val timeNow = System.currentTimeMillis()
        val deltaNow = timeNow.minus(timeStart).toFloat()
        val deltaTotal = losTime.minus(timeStart).toFloat()
        return ((deltaNow / deltaTotal) * 100)
    }

    override fun onOrientationChanged(compassEvent: CompassEvent) {
        _compassEvent.value = compassEvent
    }
}

