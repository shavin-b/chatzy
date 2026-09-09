package com.orderflow.admin.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "N/A"
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "N/A"
        return dateFormat.format(Date(timestamp))
    }

    fun calculateDaysRemaining(expiryTimestamp: Long?): Long {
        if (expiryTimestamp == null || expiryTimestamp == 0L) return 0L
        val diffMillis = expiryTimestamp - System.currentTimeMillis()
        if (diffMillis <= 0) return 0L
        return TimeUnit.MILLISECONDS.toDays(diffMillis) + 1
    }

    fun addDaysToTimestamp(currentTimestamp: Long, days: Int): Long {
        val calendar = Calendar.getInstance()
        val baseTime = if (currentTimestamp > System.currentTimeMillis()) currentTimestamp else System.currentTimeMillis()
        calendar.timeInMillis = baseTime
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.timeInMillis
    }

    fun subtractDaysFromTimestamp(currentTimestamp: Long, days: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimestamp
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val now = System.currentTimeMillis()
        return if (calendar.timeInMillis < now) now else calendar.timeInMillis
    }

    fun getLifetimeTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(2099, Calendar.DECEMBER, 31, 23, 59, 59)
        return calendar.timeInMillis
    }

    fun isOnline(lastSeenTimestamp: Long?): Boolean {
        if (lastSeenTimestamp == null) return false
        // Heartbeat within last 15 minutes + 2 min grace period
        val threshold = 17 * 60 * 1000L
        return (System.currentTimeMillis() - lastSeenTimestamp) < threshold
    }
}
