package com.antigravity.cryptowallet.data.repository

import android.content.SharedPreferences
import com.antigravity.cryptowallet.ui.theme.FontType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FontRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    private val _currentFont = MutableStateFlow(getSavedFont())
    val currentFont = _currentFont.asStateFlow()

    fun setFont(font: FontType) {
        sharedPreferences.edit().putString("app_font", font.name).apply()
        _currentFont.value = font
    }

    private fun getSavedFont(): FontType {
        val name = sharedPreferences.getString("app_font", FontType.MONOSPACE.name)
        return try {
            FontType.valueOf(name ?: FontType.MONOSPACE.name)
        } catch (e: Exception) {
            FontType.MONOSPACE
        }
    }
}
