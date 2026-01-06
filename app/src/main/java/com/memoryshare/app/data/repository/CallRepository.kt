package com.memoryshare.app.data.repository

import com.memoryshare.app.data.local.dao.CallDao
import com.memoryshare.app.data.model.Call
import com.memoryshare.app.data.model.CallStatus
import com.memoryshare.app.data.model.CallType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CallRepository(
    private val callDao: CallDao
) {

    fun getAllCalls(): Flow<List<Call>> = callDao.getAllCalls()

    fun getCallsByUser(userId: String): Flow<List<Call>> = callDao.getCallsByUser(userId)

    fun getCallById(callId: String): Flow<Call?> = callDao.getCallById(callId)

    suspend fun createCall(
        userId: String,
        type: CallType,
        isIncoming: Boolean
    ): Call {
        val call = Call(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = type,
            status = if (isIncoming) CallStatus.INCOMING else CallStatus.OUTGOING,
            isIncoming = isIncoming
        )
        callDao.insertCall(call)
        return call
    }

    suspend fun endCall(callId: String, duration: Long) {
        val call = callDao.getCallById(callId)
        call.collect { currentCall ->
            currentCall?.let {
                val updatedCall = it.copy(
                    status = CallStatus.ENDED,
                    duration = duration
                )
                callDao.updateCall(updatedCall)
            }
        }
    }

    suspend fun updateCallStatus(callId: String, status: CallStatus, duration: Long? = null) {
        val call = callDao.getCallById(callId)
        call.collect { currentCall ->
            currentCall?.let {
                val updatedCall = it.copy(
                    status = status,
                    duration = duration
                )
                callDao.updateCall(updatedCall)
            }
        }
    }

    suspend fun deleteCall(call: Call) {
        callDao.deleteCall(call)
    }

    suspend fun deleteCallsByUser(userId: String) {
        callDao.deleteCallsByUser(userId)
    }

    suspend fun deleteAllCalls() {
        callDao.deleteAllCalls()
    }
}
