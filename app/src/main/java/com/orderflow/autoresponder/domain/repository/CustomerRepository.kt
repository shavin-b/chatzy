package com.orderflow.autoresponder.domain.repository

import com.orderflow.autoresponder.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun getAllCustomers(): Flow<List<Customer>>
    suspend fun getCustomerByPhone(phone: String): Customer?
    suspend fun saveOrUpdateCustomer(phone: String, name: String)
    suspend fun updateNotes(phone: String, notes: String)
    suspend fun getCustomerCount(): Int
}
