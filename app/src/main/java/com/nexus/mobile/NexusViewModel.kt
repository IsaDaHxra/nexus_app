package com.nexus.mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.mobile.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val ready: Boolean = false, val saved: SavedState = SavedState(), val searchResults: List<Character> = emptyList(),
    val query: String = "", val total: Int = 0, val searching: Boolean = false, val searchError: String? = null,
    val selected: Character? = null, val detailLoading: Boolean = false, val detailError: String? = null,
    val storageError: String? = null
)
class NexusViewModel(app: Application) : AndroidViewModel(app) {
    private val local = LocalRepository(app)
    private val remote = ComicVineRepository()
    val engine = local.engine
    val cast = local.cast
    val online get() = remote.configured
    private val mutable = MutableStateFlow(UiState())
    val state = mutable.asStateFlow()
    private val writes = Channel<SavedState>(Channel.UNLIMITED)
    private var searchJob: Job? = null
    private var detailJob: Job? = null
    init {
        viewModelScope.launch {
            try { mutable.value = mutable.value.copy(saved = local.load(), ready = true) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = mutable.value.copy(ready = true, storageError = "Não foi possível restaurar o progresso.") }
        }
        viewModelScope.launch {
            for (value in writes) {
                try { local.save(value); mutable.value = mutable.value.copy(storageError = null) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { mutable.value = mutable.value.copy(storageError = "Não foi possível salvar. Tente novamente antes de sair.") }
            }
        }
    }
    private fun save(value: SavedState) { mutable.value = mutable.value.copy(saved = value); writes.trySend(value) }
    fun retrySave() { writes.trySend(mutable.value.saved) }
    fun advance(choice: Int? = null) = save(mutable.value.saved.let { it.copy(progress = engine.advance(it.progress, choice)) })
    fun backScene() = save(mutable.value.saved.let { it.copy(progress = engine.back(it.progress)) })
    fun restart() = save(mutable.value.saved.copy(progress = Progress()))
    fun preferences(p: Preferences) = save(mutable.value.saved.copy(preferences = p))
    fun favorite(character: Character) {
        val s = mutable.value.saved
        save(s.copy(favorites = if (s.favorites.any { it.id == character.id }) s.favorites.filterNot { it.id == character.id } else s.favorites + character))
    }
    fun clearRecents() = save(mutable.value.saved.copy(recentSearches = emptyList()))
    fun select(character: Character) {
        detailJob?.cancel()
        mutable.value = mutable.value.copy(selected = character, detailError = null, detailLoading = false)
        val id = character.comicVineId ?: return
        if (!online) return
        detailJob = viewModelScope.launch {
            mutable.value = mutable.value.copy(detailLoading = true)
            try { mutable.value = mutable.value.copy(selected = remote.detail(id), detailLoading = false) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = mutable.value.copy(detailLoading = false, detailError = "Não foi possível carregar a ficha. Verifique a conexão e a chave da API.") }
        }
    }
    fun selectId(id: String) {
        if (mutable.value.selected?.id == id) return
        (cast + mutable.value.saved.favorites + mutable.value.searchResults).firstOrNull { it.id == id }?.let { select(it); return }
        id.removePrefix("cv_").toIntOrNull()?.let { select(Character(id = id, name = "Personagem", comicVineId = it)) }
    }
    fun search(query: String, more: Boolean = false) {
        val q = query.trim()
        if (q.length < 2 || mutable.value.searching && more) return
        searchJob?.cancel()
        val old = if (more && q == mutable.value.query) mutable.value.searchResults else emptyList()
        save(mutable.value.saved.let { it.copy(recentSearches = (listOf(q) + it.recentSearches.filterNot { t -> t.equals(q, true) }).take(8)) })
        mutable.value = mutable.value.copy(query = q, searchResults = old, searching = true, searchError = null)
        searchJob = viewModelScope.launch {
            try {
                val page = if (online) remote.search(q, old.size) else cast.filter { it.name.contains(q, true) }.let { SearchPage(it, it.size) }
                mutable.value = mutable.value.copy(searchResults = (old + page.characters).distinctBy { it.id }, total = page.total, searching = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = mutable.value.copy(searching = false, searchError = "A busca não foi concluída. Confira sua conexão e a configuração da Comic Vine.") }
        }
    }
}
