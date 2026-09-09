package com.orderflow.admin.data.model

import com.orderflow.admin.domain.model.LogEntry

data class LogDto(
    val logId: String = "",
    val timestamp: Long = 0L,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val performedBy: String = "",
    val deviceId: String? = null
)

fun LogDto.toDomain(): LogEntry {
    return LogEntry(
        logId = logId,
        timestamp = timestamp,
        title = title,
        description = description,
        category = category,
        performedBy = performedBy,
        deviceId = deviceId
    )
}
