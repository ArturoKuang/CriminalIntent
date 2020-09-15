package com.example.criminalintent

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.lang.IllegalStateException
import java.util.*

private const val ARG_BITMAP = "bitmap"

class DetailDisplayFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return  activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.detail_image_dialog, null)
            val imageView = view.findViewById(R.id.crime_photo) as ImageView
            val imageBitmap = arguments?.getParcelable(ARG_BITMAP) as Bitmap?

            imageView.setImageBitmap(imageBitmap)
            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity can not be null")
    }

    companion object {
        fun newInstance(bitmap: Bitmap): DetailDisplayFragment {
            val args = Bundle().apply {
                putParcelable(ARG_BITMAP, bitmap)
            }
            return DetailDisplayFragment().apply {
                arguments = args
            }
        }
    }
}