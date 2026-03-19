package com.antigravity.cryptowallet.ui.settings

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HardwareDevice(
    val id: String,
    val name: String,
    val rssi: Int,
    val address: String
)

sealed class BluetoothState {
    object Idle : BluetoothState()
    object Scanning : BluetoothState()
    data class Connected(val device: HardwareDevice) : BluetoothState()
    data class Error(val message: String) : BluetoothState()
}

@HiltViewModel
class HardwareWalletViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val _uiState = MutableStateFlow<BluetoothState>(BluetoothState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _foundDevices = MutableStateFlow<List<HardwareDevice>>(emptyList())
    val foundDevices = _foundDevices.asStateFlow()

    fun startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _uiState.value = BluetoothState.Error("Bluetooth is disabled or not supported")
            return
        }

        _uiState.value = BluetoothState.Scanning
        _foundDevices.value = emptyList()

        viewModelScope.launch {
            // Mock scanning for demonstration of a "premium" experience
            // In a real app, this would use BluetoothLeScanner
            delay(1500)
            _foundDevices.value = listOf(
                HardwareDevice("1", "Tranzo Wallet X1", -65, "AA:BB:CC:DD:EE:01"),
                HardwareDevice("2", "Tranzo Nano S", -82, "11:22:33:44:55:66")
            )
            
            // Simulation: If a real scanner was used, we'd stop after some time
            delay(10000)
            if (_uiState.value is BluetoothState.Scanning) {
                // Keep scanning or stop? usually stop to save battery
            }
        }
    }

    fun connectToDevice(device: HardwareDevice) {
        viewModelScope.launch {
            _uiState.value = BluetoothState.Scanning // Show "Connecting..." via scanning state or a new state
            delay(2000)
            _uiState.value = BluetoothState.Connected(device)
        }
    }

    fun retry() {
        startScanning()
    }
}
