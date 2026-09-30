package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "qr_history")
data class QrItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val qrType: String, // from QrType.name
    val title: String,
    val isGenerated: Boolean, // true = generated, false = scanned
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
