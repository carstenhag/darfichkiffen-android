package de.chagemann.darfichkiffen

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    val json: Json
) : ViewModel() {

    private val _viewState = MutableStateFlow(ViewState(Any()))
    val viewState = _viewState.asStateFlow()


    data class ViewState(
        val x: Any
    )
}