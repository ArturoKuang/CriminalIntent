package com.example.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.nfc.Tag
import android.os.Bundle
import android.text.format.DateFormat.is24HourFormat
import android.util.Log
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.sql.Time
import java.text.DateFormat
import java.time.LocalTime
import java.util.*
import java.util.concurrent.TimeUnit

private const val ARG_TIME = "time"

class TimePickerFragment : DialogFragment() {
    interface Callbacks {
        fun onTimeSelected(time: Time)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val timeListener = TimePickerDialog.OnTimeSetListener {
                _: TimePicker, hourOfDay:Int, minute: Int ->

            val resultTime: Time = Time(hourOfDay, minute, 0)

            targetFragment?.let { fragment ->
                (fragment as Callbacks).onTimeSelected(resultTime)
            }
        }

        val time = arguments?.getSerializable(ARG_TIME) as Time
        val calender = Calendar.getInstance()
        calender.time = time
        val hours = calender.get(Calendar.HOUR_OF_DAY)
        val minute = calender.get(Calendar.MINUTE)

        Log.d("TIMEPICKER", "{$hours} : {$minute}")

        return TimePickerDialog(
            requireContext(),
            timeListener,
            hours,
            minute,
            false
        )

    }

  
    companion object {
        fun newInstance(time: Time): TimePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_TIME, time)
            }
            return TimePickerFragment().apply {
                arguments = args
            }
        }
    }
}