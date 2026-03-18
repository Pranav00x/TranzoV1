package com.antigravity.cryptowallet.ui.settings

import androidx.lifecycle.ViewModel
import com.antigravity.cryptowallet.data.repository.FontRepository
import com.antigravity.cryptowallet.data.repository.ThemeRepository
import com.antigravity.cryptowallet.ui.theme.FontType
import com.antigravity.cryptowallet.ui.theme.ThemeType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val fontRepository: FontRepository,
    private val walletRepository: com.antigravity.cryptowallet.data.wallet.WalletRepository
) : ViewModel() {

    val currentTheme = themeRepository.currentTheme
    val currentFont = fontRepository.currentFont

    fun setTheme(theme: ThemeType) = themeRepository.setTheme(theme)
    fun setFont(font: FontType) = fontRepository.setFont(font)

    fun hasMnemonic(): Boolean = walletRepository.hasMnemonic()
}
