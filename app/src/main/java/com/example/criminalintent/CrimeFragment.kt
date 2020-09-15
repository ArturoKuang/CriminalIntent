package com.example.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.database.Crime
import com.example.criminalintent.database.CrimeDetailViewModel
import java.io.File
import java.sql.Time
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val DIALOG_PHOTO = "DialogPhoto"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACTS = 1
private const val REQUEST_TIME = 2
private const val REQUEST_CONTACT = 3
private const val REQUEST_PHOTO = 4
private const val REQUEST_ENLARGE_PHOTO = 5


class CrimeFragment : Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {
    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var solvedCheckBox: CheckBox
    private var photoViewWidth: Int = 0
    private var photoViewHeight: Int = 0

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
            REQUEST_CONTACTS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        initTitleFieldListener()
        initSolvedCheckBoxListener()
        initDateButtonListener()
        initTimeButtonListener()
        initReportButtonListener()
        initSuspectButtonListener()
        initCallButtonListener()
        initPhotoButtonListener()
        initPhotoViewListener()
        photoView.afterMeasured {
            photoViewWidth = photoView.width
            photoViewHeight = photoView.height
        }
    }

    private inline fun ImageView.afterMeasured(crossinline f: ImageView.() -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    f()
                }
            }
        })
    }

    private fun initTitleFieldListener() {
        val titleWatcher = object : TextWatcher {
            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()
            }

            override fun beforeTextChanged(
                sequence: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun afterTextChanged(sequence: Editable?) { }
        }

        titleField.addTextChangedListener(titleWatcher)
    }

    private fun initSolvedCheckBoxListener() {
        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }
    }

    private fun initDateButtonListener() {
        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }
    }

    private fun initTimeButtonListener() {
        timeButton.setOnClickListener {
            val t = Time(crime.date.time)
            TimePickerFragment.newInstance(t).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_TIME)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_TIME)
            }
        }
    }

    private fun initReportButtonListener() {
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
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.getDateInstance(DateFormat.SHORT).format(crime.date).toString()
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

    private fun initSuspectButtonListener() {
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
    }

    private fun initCallButtonListener() {
        callButton.apply {
            setOnClickListener {
                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${crime.phone}")
                }
                startActivity(callIntent)
            }

            if (crime.phone.isEmpty()) {
                isEnabled = false
            }
        }
    }

    private fun initPhotoButtonListener() {
        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)

            if(resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY)

                for(cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }
    }

    private fun initPhotoViewListener() {
        photoView.apply {
            setOnClickListener {
                val bitmap = getScaledBitmap(photoFile.path, requireActivity())
                DetailDisplayFragment.newInstance(bitmap).apply {
                    setTargetFragment(this@CrimeFragment, REQUEST_ENLARGE_PHOTO)
                    show(this@CrimeFragment.parentFragmentManager, DIALOG_PHOTO)
                }
            }
        }

    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
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
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(requireActivity(), "com.example.criminalintent.fileprovider", photoFile)
                    updateUI()
                }
            }
        )
    }

    private fun updateUI() {
        setTitleText()
        setDateText()
        setTimeText()
        setSolvedCheckBox()
        setSuspectButtonText()
        setPhoneButtonText()
        updatePhotoView()
    }

    private fun setTitleText() {
        titleField.setText(crime.title)
    }

    private fun setDateText() {
        val dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
        dateButton.text = dateFormat.format(crime.date).toString()
    }

    private fun setTimeText() {
        val timeFormat: DateFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
        timeButton.text = timeFormat.format(crime.date.time).toString()
    }

    private fun setSolvedCheckBox() {
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
    }

    private fun setSuspectButtonText() {
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }
    }

    private fun setPhoneButtonText() {
        if (crime.phone.isNotEmpty()) {
            callButton.text = getString(R.string.call_report, crime.phone)
            callButton.isEnabled = true
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                resolveContactRequest(data)
            }

            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }

    private fun updatePhotoView() {
        if(photoFile.exists()) {
            changePhotoView()
        } else {
            photoView.setImageBitmap(null)
            photoView.contentDescription = getString(R.string.crime_photo_no_image_description)
        }
    }

    private fun changePhotoView() {
        val bitmap = getScaledBitmap(photoFile.path, photoViewWidth, photoViewHeight)
        photoView.setImageBitmap(bitmap)
        photoView.contentDescription = getString(R.string.crime_photo_image_description)
    }

    private fun resolveContactRequest(data: Intent) {
        var contactId: String? = null
        val contactCursor = createContactCursor(data)
        moveCursorToFirst(contactCursor)?.use {cursor ->
            val suspect = cursor.getString(0)
            contactId = cursor.getString(1)

            updateSuspectCrime(suspect)
            updateSuspectText(suspect)
        }

        val phoneCursor = createPhoneCursor(contactId)
        moveCursorToFirst(phoneCursor)?.use { cursor ->
            val allNumbers = getAllPhoneNumbers(cursor)
            val phoneNumber = allNumbers.first()

            setPhoneNumber(phoneNumber)
            crime.phone = phoneNumber
        }

        crimeDetailViewModel.saveCrime(crime)
    }

    private fun updateSuspectCrime(suspect: String) {
        crime.suspect = suspect
    }

    private fun updateSuspectText(suspect: String) {
        suspectButton.text = suspect
    }

    private fun moveCursorToFirst(cursor: Cursor?): Cursor? {
        if(cursor?.count == 0) return null
        cursor?.moveToFirst()
        return cursor
    }

    private fun createContactCursor(data: Intent) : Cursor? {
        val contactUri: Uri? = data.data
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID)
        return contactUri?.let<Uri, Cursor?> {
            requireActivity().contentResolver.query(
                it,
                queryFields,
                null,
                null,
                null
            )
        }
    }

    private fun createPhoneCursor(contactId: String?): Cursor? {
        val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val phoneQueryFields = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val phoneWhereClause =
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
        val phoneQueryParams = arrayOf(contactId)
        return requireActivity().contentResolver.query(
            phoneUri,
            phoneQueryFields,
            phoneWhereClause,
            phoneQueryParams,
            null
        )
    }

    private fun getAllPhoneNumbers(cursor: Cursor): ArrayList<String> {
        val allNumbers: ArrayList<String> = arrayListOf<String>()
        var phoneNumber: String = ""

        while (!cursor.isAfterLast)
        {
            phoneNumber = cursor.getString(0)
            allNumbers.add(phoneNumber)
            cursor.moveToNext()
        }
        return allNumbers
    }

    private fun setPhoneNumber(phoneNumber: String) {
        callButton.isEnabled = true
        callButton.text = getString(R.string.call_report, phoneNumber)
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