package com.mardous.booming.viewmodels.info

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.mardous.booming.R
import com.mardous.booming.database.toPlayCount
import com.mardous.booming.extensions.files.asReadableFileSize
import com.mardous.booming.extensions.files.getHumanReadableSize
import com.mardous.booming.extensions.files.getPrettyAbsolutePath
import com.mardous.booming.extensions.files.toAudioFile
import com.mardous.booming.extensions.media.replayGainStr
import com.mardous.booming.extensions.media.songDurationStr
import com.mardous.booming.extensions.media.timesStr
import com.mardous.booming.extensions.utilities.dateStr
import com.mardous.booming.extensions.utilities.format
import com.mardous.booming.model.Album
import com.mardous.booming.model.Artist
import com.mardous.booming.model.Song
import com.mardous.booming.repository.Repository
import com.mardous.booming.taglib.MetadataReader
import com.mardous.booming.viewmodels.info.model.PlayInfoResult
import com.mardous.booming.viewmodels.info.model.SongInfoResult
import kotlinx.coroutines.Dispatchers
import org.jaudiotagger.audio.AudioHeader
import java.io.File

class InfoViewModel(private val repository: Repository) : ViewModel() {

    fun loadAlbum(id: Long): LiveData<Album> = liveData(Dispatchers.IO) {
        if (id != -1L) {
            emit(repository.albumById(id))
        } else {
            emit(Album.Companion.empty)
        }
    }

    fun loadArtist(id: Long, name: String?): LiveData<Artist> = liveData(Dispatchers.IO) {
        if (name.isNullOrEmpty()) {
            emit(repository.artistById(id))
        } else if (id == -1L) {
            emit(repository.albumArtistByName(name))
        } else {
            emit(Artist.Companion.empty)
        }
    }

    fun playInfo(songs: List<Song>): LiveData<PlayInfoResult> = liveData(Dispatchers.IO) {
        val playCountEntities =
            repository.playCountSongsFrom(songs).sortedByDescending { it.playCount }
        val totalPlayCount = playCountEntities.sumOf { it.playCount }
        val totalSkipCount = playCountEntities.sumOf { it.skipCount }
        val lastPlayDate = playCountEntities.maxOf { it.timePlayed }
        emit(PlayInfoResult(totalPlayCount, totalSkipCount, lastPlayDate, playCountEntities))
    }

    fun songDetail(context: Context, song: Song): LiveData<SongInfoResult> =
        liveData(Dispatchers.IO) {
            // Play count
            val result = runCatching {
                val playCountEntity = repository.findSongInPlayCount(song.id) ?: song.toPlayCount()
                val playCount = playCountEntity.playCount.timesStr(context)
                val skipCount = playCountEntity.skipCount.timesStr(context)
                val lastPlayed = context.dateStr(playCountEntity.timePlayed)

                val dateModified = song.getModifiedDate().format(context)
                val year = if (song.year > 0) song.year.toString() else null
                val trackLength = song.songDurationStr()
                val replayGain = song.replayGainStr(context)

                val metadataReader = MetadataReader(song.mediaStoreUri)
                if (!metadataReader.hasMetadata) {
                    SongInfoResult(
                        playCount = playCount,
                        skipCount = skipCount,
                        lastPlayedDate = lastPlayed,
                        filePath = File(song.data).getPrettyAbsolutePath(),
                        fileSize = song.size.asReadableFileSize(),
                        trackLength = trackLength,
                        dateModified = dateModified,
                        title = song.title,
                        albumYear = year,
                        replayGain = replayGain
                    )
                } else {
                    // FILE
                    val file = File(song.data)
                    val filePath = file.getPrettyAbsolutePath()
                    val fileSize = file.getHumanReadableSize()

                    val audioHeader = getAudioHeader(
                        context,
                        file.toAudioFile()?.audioHeader,
                        metadataReader
                    )

                    // MEDIA
                    val title = metadataReader.first(MetadataReader.TITLE)
                    val album = metadataReader.first(MetadataReader.ALBUM)
                    val artist = metadataReader.merge(MetadataReader.ARTIST)
                    val albumArtist = metadataReader.first(MetadataReader.ALBUM_ARTIST)

                    val trackNumber = getNumberAndTotal(
                        metadataReader.value(MetadataReader.TRACK_NUMBER),
                        metadataReader.value(MetadataReader.TRACK_TOTAL)
                    )
                    val discNumber = getNumberAndTotal(
                        metadataReader.value(MetadataReader.DISC_NUMBER),
                        metadataReader.value(MetadataReader.DISC_TOTAL)
                    )

                    val composer = metadataReader.merge(MetadataReader.COMPOSER)
                    val conductor = metadataReader.merge(MetadataReader.PRODUCER)
                    val publisher = metadataReader.merge(MetadataReader.COPYRIGHT)
                    val genre = metadataReader.merge(MetadataReader.GENRE)
                    val comment = metadataReader.value(MetadataReader.COMMENT)

                    SongInfoResult(
                        playCount,
                        skipCount,
                        lastPlayed,
                        filePath,
                        fileSize,
                        trackLength,
                        dateModified,
                        audioHeader,
                        title,
                        album,
                        artist,
                        albumArtist,
                        year,
                        trackNumber,
                        discNumber,
                        composer,
                        conductor,
                        publisher,
                        genre,
                        replayGain,
                        comment
                    )
                }
            }
            if (result.isSuccess) {
                emit(result.getOrThrow())
            } else {
                emit(SongInfoResult.Companion.Empty)
            }
        }

    private fun getNumberAndTotal(number: String?, total: String?): String? {
        val numberInt = number?.toIntOrNull() ?: return null
        val totalInt = total?.toIntOrNull()
        return if (totalInt == null || totalInt == 0) {
            numberInt.toString().padStart(2, '0')
        } else {
            "%02d/%02d".format(numberInt, totalInt)
        }
    }

    private fun getAudioHeader(context: Context, header: AudioHeader?, metadataReader: MetadataReader): String {
        val properties = arrayOf(
            header?.format,
            metadataReader.bitrate(),
            metadataReader.sampleRate(),
            metadataReader.channelName(),
            header?.let {
                if (header.isVariableBitRate)
                    context.getString(R.string.label_variable_bitrate).lowercase()
                else null
            },
            header?.let {
                if (header.isLossless)
                    context.getString(R.string.label_loss_less).lowercase()
                else null
            }
        )
        return properties.filterNotNull().joinToString(separator = " - ")
    }
}