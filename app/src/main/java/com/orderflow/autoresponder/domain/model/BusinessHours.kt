package com.orderflow.autoresponder.domain.model

data class BusinessHours(
    val isEnabled: Boolean = false,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 18,
    val endMinute: Int = 0,
    val activeDaysCsv: String = "MON,TUE,WED,THU,FRI"
)
