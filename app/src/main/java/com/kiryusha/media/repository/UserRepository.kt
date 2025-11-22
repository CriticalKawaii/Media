package com.kiryusha.media.repository

import com.kiryusha.media.database.daos.UserDao
import com.kiryusha.media.database.entities.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {
    suspend fun registerUser(user: User): Boolean = withContext(Dispatchers.IO){
        try {
            userDao.insertAll(user)
            true
        } catch (e: Exception){
            false
        }
    }

    suspend fun loginUser(login: String, password: String) : User? = withContext(Dispatchers.IO){
        try {
            val user = userDao.findByLogin(login)
            if(user.password == password) user else null
        } catch (e: Exception){
            null
        }
    }

    suspend fun isLoginTaken(login: String): Boolean = withContext(Dispatchers.IO){
        try {
            userDao.findByLogin(login)
            true
        } catch (e: Exception){
            false
        }
    }
}