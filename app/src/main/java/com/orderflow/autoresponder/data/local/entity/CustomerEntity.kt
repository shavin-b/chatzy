package com.orderflow.autoresponder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.orderflow.autoresponder.domain.model.Customer

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val phone: String,
    val name: String,
    val totalMessages: Int,
    val lastInteractionTime: Long,
    val notes: String
)

fun CustomerEntity.toDomainModel(): Customer {
    return Customer(
        phone = phone,
        name = name,
        totalMessages = totalMessages,
        lastInteractionTime = lastInteractionTime,
        notes = notes
    )
}

fun Customer.toEntity(): CustomerEntity {
    return CustomerEntity(
        phone = phone,
        name = name,
        totalMessages = totalMessages,
        lastInteractionTime = lastInteractionTime,
        notes = notes
    )
}
