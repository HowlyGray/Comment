package com.memoryshare.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoryshare.app.data.model.Media
import com.memoryshare.app.data.model.MediaType
import com.memoryshare.app.data.model.PermissionLevel
import com.memoryshare.app.data.model.SharedSpace
import com.memoryshare.app.data.model.SharedSpacePermission
import com.memoryshare.app.data.repository.SharedSpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SharedSpaceViewModel(
    private val repository: SharedSpaceRepository
) : ViewModel() {

    private val _spaces = MutableStateFlow<List<SharedSpace>>(emptyList())
    val spaces: StateFlow<List<SharedSpace>> = _spaces.asStateFlow()

    private val _currentSpace = MutableStateFlow<SharedSpace?>(null)
    val currentSpace: StateFlow<SharedSpace?> = _currentSpace.asStateFlow()

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    val media: StateFlow<List<Media>> = _media.asStateFlow()

    private val _permissions = MutableStateFlow<List<SharedSpacePermission>>(emptyList())
    val permissions: StateFlow<List<SharedSpacePermission>> = _permissions.asStateFlow()

    init {
        loadSpaces()
    }

    private fun loadSpaces() {
        viewModelScope.launch {
            repository.getAllSharedSpaces().collect { spaces ->
                _spaces.value = spaces
            }
        }
    }

    fun loadSpace(spaceId: String) {
        viewModelScope.launch {
            repository.getSharedSpaceById(spaceId).collect { space ->
                _currentSpace.value = space
            }
        }
    }

    fun loadMedia(spaceId: String, type: MediaType? = null) {
        viewModelScope.launch {
            if (type != null) {
                repository.getMediaBySpaceAndType(spaceId, type).collect { media ->
                    _media.value = media
                }
            } else {
                repository.getMediaBySpace(spaceId).collect { media ->
                    _media.value = media
                }
            }
        }
    }

    fun createSpace(
        name: String,
        creatorId: String,
        description: String? = null,
        memberIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            repository.createSharedSpace(name, creatorId, description, memberIds)
        }
    }

    fun addMedia(
        spaceId: String,
        uploaderId: String,
        url: String,
        type: MediaType,
        title: String? = null,
        description: String? = null,
        thumbnailUrl: String? = null,
        duration: Long? = null
    ) {
        viewModelScope.launch {
            repository.addMedia(spaceId, uploaderId, url, type, title, description, thumbnailUrl, duration)
        }
    }

    fun addMember(spaceId: String, userId: String) {
        viewModelScope.launch {
            repository.addMemberToSpace(spaceId, userId)
        }
    }

    fun removeMember(spaceId: String, userId: String) {
        viewModelScope.launch {
            repository.removeMemberFromSpace(spaceId, userId)
        }
    }

    fun deleteMedia(media: Media) {
        viewModelScope.launch {
            repository.deleteMedia(media)
        }
    }

    fun deleteSpace(space: SharedSpace) {
        viewModelScope.launch {
            repository.deleteSharedSpace(space)
        }
    }

    fun loadPermissions(spaceId: String) {
        viewModelScope.launch {
            repository.getSpacePermissions(spaceId).collect { permissions ->
                _permissions.value = permissions
            }
        }
    }

    fun updatePermission(spaceId: String, userId: String, permissionLevel: PermissionLevel) {
        viewModelScope.launch {
            val permission = SharedSpacePermission(
                id = UUID.randomUUID().toString(),
                spaceId = spaceId,
                userId = userId,
                permissionLevel = permissionLevel
            )
            repository.updatePermission(permission)
        }
    }
}
