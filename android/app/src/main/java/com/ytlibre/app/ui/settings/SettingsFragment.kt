package com.ytlibre.app.ui.settings

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ytlibre.app.R
import com.ytlibre.app.YouTubeLibreApp
import com.ytlibre.app.data.local.SecurePreferences
import com.ytlibre.app.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

/**
 * Réglages de l'application : configuration de l'API, thème et données locales.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        (requireActivity().application as YouTubeLibreApp).container.viewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.inputApiKey.setText(viewModel.apiKey)
        binding.inputApiKey.transformationMethod = PasswordTransformationMethod.getInstance()
        binding.inputProxy.setText(viewModel.proxyUrl)
        binding.inputRegion.setText(viewModel.regionCode)
        binding.inputHistoryLimit.setText(viewModel.historyLimit.toString())

        when (viewModel.themeMode) {
            SecurePreferences.ThemeMode.SYSTEM -> binding.radioSystem.isChecked = true
            SecurePreferences.ThemeMode.LIGHT -> binding.radioLight.isChecked = true
            SecurePreferences.ThemeMode.DARK -> binding.radioDark.isChecked = true
        }

        binding.btnSaveApiKey.setOnClickListener {
            viewModel.saveApiKey(binding.inputApiKey.text?.toString().orEmpty())
            binding.tvApiKeyStatus.text = if (viewModel.apiKey.isBlank()) {
                getString(R.string.settings_key_cleared)
            } else {
                getString(R.string.settings_key_saved)
            }
        }

        binding.btnSaveProxy.setOnClickListener {
            viewModel.saveProxyUrl(binding.inputProxy.text?.toString().orEmpty())
            binding.tvProxyStatus.text = if (viewModel.isProxyMode.value) {
                getString(R.string.settings_proxy_saved)
            } else {
                getString(R.string.settings_proxy_cleared)
            }
        }

        binding.btnSaveRegion.setOnClickListener {
            viewModel.saveRegion(binding.inputRegion.text?.toString().orEmpty().ifBlank { "FR" })
            binding.tvRegionStatus.text = getString(R.string.settings_region_saved)
        }

        binding.btnSaveHistoryLimit.setOnClickListener {
            val limit = binding.inputHistoryLimit.text?.toString()?.toIntOrNull() ?: 200
            viewModel.saveHistoryLimit(limit.coerceIn(20, 1000))
            binding.tvHistoryStatus.text = getString(R.string.settings_history_saved)
        }

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioLight -> SecurePreferences.ThemeMode.LIGHT
                R.id.radioDark -> SecurePreferences.ThemeMode.DARK
                else -> SecurePreferences.ThemeMode.SYSTEM
            }
            viewModel.saveTheme(mode)
            YouTubeLibreApp.applyTheme(mode)
        }

        binding.btnTestConnection.setOnClickListener { viewModel.testConnection() }
        binding.btnClearFavorites.setOnClickListener { viewModel.clearFavorites() }
        binding.btnClearHistory.setOnClickListener { viewModel.clearHistory() }
        binding.btnClearRecent.setOnClickListener { viewModel.clearRecentQueries() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.testResult.collect { result ->
                    when (result) {
                        is SettingsViewModel.TestResult.Idle -> {
                            binding.tvTestResult.text = ""
                        }

                        is SettingsViewModel.TestResult.Running -> {
                            binding.tvTestResult.text = getString(R.string.settings_test_running)
                            binding.progressTest.visibility = View.VISIBLE
                        }

                        is SettingsViewModel.TestResult.Success -> {
                            binding.tvTestResult.text = result.message
                            binding.progressTest.visibility = View.GONE
                        }

                        is SettingsViewModel.TestResult.Failure -> {
                            binding.tvTestResult.text = result.message
                            binding.progressTest.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
