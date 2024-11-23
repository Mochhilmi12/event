package com.example.keydates

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.keydates.databinding.DialogEventBinding
import java.util.*

class EventDialogFragment(
    private val event: Event? = null,
    private val onSave: (Event) -> Unit
) : DialogFragment() {

    private var _binding: DialogEventBinding? = null
    private val binding get() = _binding!!

    private var selectedDay = 0
    private var selectedMonth = 0
    private var selectedYear = 0
    private var selectedHour = 0
    private var selectedMinute = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        _binding = DialogEventBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        val calendar = Calendar.getInstance()

        // Set existing event details if editing
        event?.let {
            binding.titleInput.setText(it.title)
            binding.descriptionInput.setText(it.description)
            selectedDay = it.day
            selectedMonth = it.month - 1 // Calendar months are 0-based
            selectedYear = it.year
            selectedHour = it.hour
            selectedMinute = it.minute
            updateDateText()
            updateTimeText()
        }

        // Date picker button
        binding.datePickerButton.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month
                    selectedDay = dayOfMonth
                    updateDateText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Time picker button
        binding.timePickerButton.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    updateTimeText()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        binding.saveButton.setOnClickListener {
            if (binding.titleInput.text.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.descriptionInput.text.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Description cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedYear == 0 || selectedMonth < 0 || selectedDay == 0) {
                Toast.makeText(requireContext(), "Please select a valid date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = event?.id ?: UUID.randomUUID().toString()
            val newEvent = Event(
                id,
                binding.titleInput.text.toString(),
                binding.descriptionInput.text.toString(),
                selectedDay,
                selectedMonth + 1, // Adjust month to 1-based
                selectedYear,
                selectedHour,
                selectedMinute
            )
            onSave(newEvent)
            dismiss()
            Log.d("EventDialogFragment", "Saving event: $newEvent")
        }


        // Cancel button
        binding.cancelButton.setOnClickListener { dismiss() }

        return dialog
    }

    @SuppressLint("SetTextI18n")
    private fun updateDateText() {
        binding.selectedDate.text = "$selectedDay/${selectedMonth + 1}/$selectedYear"
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun updateTimeText() {
        binding.selectedTime.text = "${String.format("%02d", selectedHour)}:${String.format("%02d", selectedMinute)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
