package com.mospolytech.mospolyhelper.domain.schedule.repository

import com.mospolytech.mospolyhelper.domain.schedule.model.tag.LessonTag
import com.mospolytech.mospolyhelper.domain.schedule.model.tag.LessonTagKey
import kotlinx.coroutines.flow.Flow

interface LessonTagsRepository {
    fun getAll(): Flow<List<LessonTag>>

    suspend fun addTag(tag: LessonTag)

    suspend fun editTag(tagTitle: String, newTitle: String, newColor: Int)

    suspend fun removeTag(tagTitle: String)

    suspend fun removeTagFromLesson(tagTitle: String, lesson: LessonTagKey)
}