package com.kiryusha.media.repository

import android.util.Log
import com.kiryusha.media.database.daos.UserDao
import com.kiryusha.media.database.entities.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {

    companion object{
        private const val TAG = "UserRepository"
    }

    suspend fun registerUser(user: User): Boolean = withContext(Dispatchers.IO){
        try {
            Log.d(TAG, "User registration attempt: ${user.login}")
            userDao.insertAll(user)
            Log.d(TAG, "User registered: ${user.login}")
            true
        } catch (e: Exception){
            Log.e(TAG, "User registration error: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    suspend fun loginUser(login: String, password: String) : User? = withContext(Dispatchers.IO){
        try {
            Log.d(TAG, "Attempting to login user: $login")
            val user = userDao.findByLogin(login)
            if (user.password == password) {
                Log.d(TAG, "Login successful for: $login")
                user
            } else {
                Log.d(TAG, "Login failed - incorrect password for: $login")
                null
            }
        } catch (e: Exception){
            Log.e(TAG, "Error during login: ${e.message}", e)
            null
        }
    }

    suspend fun isLoginTaken(login: String): Boolean = withContext(Dispatchers.IO){
        try {
            Log.d(TAG, "Checking if login exists: $login")
            userDao.findByLogin(login)
            Log.d(TAG, "Login exists: $login")
            true
        } catch (e: Exception){
            Log.d(TAG, "Login does not exist: $login")
            false
        }
    }
}