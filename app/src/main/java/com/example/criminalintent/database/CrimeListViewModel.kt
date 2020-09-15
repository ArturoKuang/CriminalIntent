package com.example.criminalintent.database

import androidx.lifecycle.ViewModel
import com.example.criminalintent.database.Crime
import com.example.criminalintent.database.CrimeRepository

class CrimeListViewModel() : ViewModel() {
    private val crimeRepository = CrimeRepository.get()
    val crimesListLiveData = crimeRepository.getCrimes()

    fun addCrime(crime: Crime) {
        crimeRepository.addCrime(crime)
    }
}