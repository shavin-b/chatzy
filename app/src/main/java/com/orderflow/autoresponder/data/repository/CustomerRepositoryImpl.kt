package com.orderflow.autoresponder.data.repository

import com.orderflow.autoresponder.data.local.dao.CustomerDao
import com.orderflow.autoresponder.data.local.entity.CustomerEntity
import com.orderflow.autoresponder.data.local.entity.toDomainModel
import com.orderflow.autoresponder.domain.model.Customer
import com.orderflow.autoresponder.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao
) : CustomerRepository {

    override fun getAllCustomers(): Flow<List<Customer>> {
        return customerDao.getAllCustomers().map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getCustomerByPhone(phone: String): Customer? {
        return customerDao.getCustomerByPhone(phone)?.toDomainModel()
    }

    override suspend fun saveOrUpdateCustomer(phone: String, name: String) {
        val existing = customerDao.getCustomerByPhone(phone)
        val now = System.currentTimeMillis()
        if (existing == null) {
            val newCustomer = CustomerEntity(
                phone = phone,
                name = if (name.isNotBlank()) name else phone,
                totalMessages = 1,
                lastInteractionTime = now,
                notes = ""
            )
            customerDao.insertCustomer(newCustomer)
        } else {
            val updated = existing.copy(
                name = if (name.isNotBlank()) name else existing.name,
                totalMessages = existing.totalMessages + 1,
                lastInteractionTime = now
            )
            customerDao.updateCustomer(updated)
        }
    }

    override suspend fun updateNotes(phone: String, notes: String) {
        customerDao.updateNotes(phone, notes)
    }

    override suspend fun getCustomerCount(): Int {
        return customerDao.getCustomerCount()
    }
}
