package com.example.criminalintent.database

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Dao
interface CrimeDao {
    @Query("SELECT * FROM crime")
    fun getCrimes(): LiveData<List<Crime>>

    @Query("SELECT * FROM crime WHERE id=(:id)")
    fun getCrimes(id: UUID): LiveData<Crime?>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateCrime(crime: Crime)

    @Insert
    fun addCrime(crime: Crime)
}