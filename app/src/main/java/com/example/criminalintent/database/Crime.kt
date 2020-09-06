package com.example.criminalintent.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import java.util.Date

@Entity
data class Crime(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    var title: String = "",
    var date: Date = Date(),
    var isSolved: Boolean = false,
    var suspect: String = "",
    var phone: String = "",
    var test: String = "")
