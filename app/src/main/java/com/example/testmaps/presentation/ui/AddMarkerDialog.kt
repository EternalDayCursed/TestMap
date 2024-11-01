package com.example.testmaps.presentation.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.testMaps.databinding.DialogAddMarkerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddMarkerDialog(
    private val onMarkerAdded: (String) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddMarkerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddMarkerBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить метку")
            .setView(binding.root)
            .setPositiveButton("Добавить") { _, _ ->
                val markerName = binding.etMarkerName.text.toString()
                if (markerName.isNotBlank()) {
                    onMarkerAdded(markerName)
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
