package com.example.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.database.Crime
import java.sql.Time
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CrimeFragment"
private const val DATE_FORMAT = "EEE, MMM, dd"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val REQUEST_DATE = 0
private const val REQUEST_TIME = 2
private const val REQUEST_CONTACT = 3


class CrimeFragment : Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {
    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var solvedCheckBox: CheckBox

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)

        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(android.Manifest.permission.READ_CONTACTS),
            1
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //return super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        timeButton = view.findViewById(R.id.crime_time) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        callButton = view.findViewById(R.id.crime_call) as Button
        return view
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {

            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // This space intentionally left blank
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // This one too
            }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        timeButton.setOnClickListener {
            val t = Time(crime.date.time)
            TimePickerFragment.newInstance(t).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_TIME)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_TIME)
            }
        }

        reportButton.setOnClickListener() {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject)
                )
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }

            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(
                    pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            if (resolvedActivity == null) {
                isEnabled = false
            }
        }

        callButton.apply {
            if (crime.phone.isEmpty()) {
                isEnabled = false
                return
            }

            isEnabled = true
            Toast.makeText(context, crime.phone, Toast.LENGTH_SHORT)
        }
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    override fun onTimeSelected(time: Time) {
        crime.date.time = time.time
        updateUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveDate.observe(
            viewLifecycleOwner,
            { crime ->
                crime?.let {
                    this.crime = crime
                    updateUI()
                }
            }
        )
    }

    private fun updateUI() {
        val timeFormat: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        timeButton.text = timeFormat.format(crime.date.time).toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }

        if (crime.phone.isNotEmpty()) {
            callButton.text = crime.phone
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                var contactId: String? = null

                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID)
                val cursor = contactUri?.let { requireActivity().contentResolver.query(it, queryFields, null, null, null) }
                cursor?.use {
                    if(it.count == 0) return

                    it.moveToFirst()

                    val suspect = it.getString(0)
                    //get the contact ID
                    contactId = it.getString(1)

                    crime.suspect = suspect
                    suspectButton.text = suspect
                }

                val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val phoneQueryFields = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val phoneWhereClause =
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
                val phoneQueryParams = arrayOf(contactId)
                val phoneCursor = requireActivity().contentResolver.query(
                    phoneUri,
                    phoneQueryFields,
                    phoneWhereClause,
                    phoneQueryParams,
                    null
                )

                var phoneNumber: String = ""
                val allNumbers: ArrayList<String> = arrayListOf<String>()
                phoneCursor?.use {cursorPhone ->
                    cursorPhone.moveToFirst()
                    while (!cursorPhone.isAfterLast)
                    {
                        phoneNumber = cursorPhone.getString(0)
                        allNumbers.add(phoneNumber)
                        cursorPhone.moveToNext()
                    }
                    callButton.isEnabled = true
                    callButton.text = phoneNumber
                }

                if(allNumbers.size > 0) {
                    crime.phone = allNumbers[0]
                }
                crimeDetailViewModel.saveCrime(crime)
            }
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = with(DateFormat.getInstance()) {
            (this as SimpleDateFormat).applyPattern(DATE_FORMAT)
            this.format(crime.date).toString()
        }
        Log.d(TAG, "Date string: $dateString")

        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(
            R.string.crime_report,
            crime.title, dateString, solvedString, suspect
        )
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }

}