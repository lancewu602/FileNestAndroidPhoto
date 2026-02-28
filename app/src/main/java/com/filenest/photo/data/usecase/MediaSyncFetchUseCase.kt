package com.filenest.photo.data.usecase

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import com.filenest.photo.data.AppPrefKeys
import com.filenest.photo.data.model.MediaSyncItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSyncFetchUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        const val TAG = "MediaSyncFetchUseCase"
    }

    private val imageQueryColumns = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.GENERATION_ADDED,
        MediaStore.Images.Media.GENERATION_MODIFIED,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_MODIFIED,
        MediaStore.Images.Media.RELATIVE_PATH,
    )

    private val videoQueryColumns = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.GENERATION_ADDED,
        MediaStore.Video.Media.GENERATION_MODIFIED,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_TAKEN,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.RELATIVE_PATH,
    )

    suspend fun fetchMedias(): List<MediaSyncItem> {
        val albumBucketIds = AppPrefKeys.getSelectedAlbums(context).first()
        val lastGen = AppPrefKeys.getMediaStoreLastGen(context).first()
        Log.i(TAG, "albumBucketIds: $albumBucketIds, lastGen: $lastGen")

        val result = mutableListOf<MediaSyncItem>()
        if (albumBucketIds.isEmpty()) {
            return result
        }

        val bucketIdsPlaceholder = albumBucketIds.joinToString(separator = ",", prefix = "(", postfix = ")") { "?" }
        val selectionArgs = albumBucketIds.map { it.toString() }.toTypedArray()

        val imageSelection = """
                ${MediaStore.Images.Media.BUCKET_ID} IN $bucketIdsPlaceholder
                AND (${MediaStore.Images.Media.GENERATION_ADDED} > ? OR ${MediaStore.Images.Media.GENERATION_MODIFIED} > ?)
            """.trimIndent()
        val imageSelectionArgs = selectionArgs + lastGen.toString() + lastGen.toString()
        val imageCursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageQueryColumns, imageSelection, imageSelectionArgs,
            "${MediaStore.Images.Media.GENERATION_MODIFIED} ASC"
        )

        imageCursor?.use {
            while (it.moveToNext()) {
                result.add(imageCursorConvertToItem(it, false))
            }
        }

        val videoSelection = """
                ${MediaStore.Video.Media.BUCKET_ID} IN $bucketIdsPlaceholder
                AND (${MediaStore.Video.Media.GENERATION_ADDED} > ? OR ${MediaStore.Video.Media.GENERATION_MODIFIED} > ?)
            """.trimIndent()
        val videoSelectionArgs = selectionArgs + lastGen.toString() + lastGen.toString()
        val videoCursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoQueryColumns, videoSelection, videoSelectionArgs,
            "${MediaStore.Video.Media.GENERATION_MODIFIED} ASC"
        )

        videoCursor?.use {
            while (it.moveToNext()) {
                result.add(videoCursorConvertToItem(it, false))
            }
        }

        result.sortBy { it.generationModified }

        return result
    }

    private fun imageCursorConvertToItem(cursor: Cursor, enabledSyncFavorite: Boolean): MediaSyncItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
        val favorite = if (enabledSyncFavorite) {
            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_FAVORITE))
        } else {
            0
        }
        return MediaSyncItem(
            contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString(),
            type = "image",
            name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)),
            size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)),
            dateToken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)),
            dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)),
            dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)),
            duration = 0,
            favorite = favorite,
            generationAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.GENERATION_ADDED)),
            generationModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.GENERATION_MODIFIED)),
        )
    }

    private fun videoCursorConvertToItem(cursor: Cursor, enabledSyncFavorite: Boolean): MediaSyncItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
        val favorite = if (enabledSyncFavorite) {
            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.IS_FAVORITE))
        } else {
            0
        }
        return MediaSyncItem(
            contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString(),
            type = "video",
            name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)),
            size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)),
            dateToken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)),
            dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)),
            dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)),
            duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)),
            favorite = favorite,
            generationAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.GENERATION_ADDED)),
            generationModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.GENERATION_MODIFIED)),
        )
    }
}