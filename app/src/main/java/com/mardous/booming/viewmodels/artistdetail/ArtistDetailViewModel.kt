package com.mardous.booming.viewmodels.artistdetail

import androidx.lifecycle.*
import com.mardous.booming.http.Result
import com.mardous.booming.http.lastfm.LastFmArtist
import com.mardous.booming.model.Artist
import com.mardous.booming.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArtistDetailViewModel(
    private val repository: Repository,
    private val artistId: Long,
    private val artistName: String?
) : ViewModel() {

    private val _artistDetail = MutableLiveData<Artist>()

    fun getArtist() = getArtistDetail().value ?: Artist.Companion.empty

    fun getArtistDetail(): LiveData<Artist> = _artistDetail

    fun loadArtistDetail() = viewModelScope.launch(Dispatchers.IO) {
        if (!artistName.isNullOrEmpty()) {
            _artistDetail.postValue(repository.albumArtistByName(artistName))
        } else if (artistId != -1L) {
            _artistDetail.postValue(repository.artistById(artistId))
        } else {
            _artistDetail.postValue(Artist.Companion.empty)
        }
    }

    fun getSimilarArtists(artist: Artist): LiveData<List<Artist>> = liveData(Dispatchers.IO) {
        emit(repository.similarAlbumArtists(artist).sortedBy { it.name })
    }

    fun getArtistBio(
        name: String,
        lang: String?,
        cache: String?
    ): LiveData<Result<LastFmArtist>> = liveData(Dispatchers.IO) {
        emit(Result.Loading)
        val info = repository.artistInfo(name, lang, cache)
        emit(info)
    }
}