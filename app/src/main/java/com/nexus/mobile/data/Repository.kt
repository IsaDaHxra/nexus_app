package com.nexus.mobile.data

import android.content.Context
import android.text.Html
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexus.mobile.BuildConfig
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private val Context.nexusStore by preferencesDataStore("nexus")
class LocalRepository(private val context: Context) {
    private val gson = Gson()
    private val key = stringPreferencesKey("state_v1")
    val engine: StoryEngine by lazy {
        val type = object : TypeToken<List<Scene>>() {}.type
        StoryEngine(gson.fromJson(context.assets.open("episode_1.json").bufferedReader().use { it.readText() }, type))
    }
    val cast: List<Character> by lazy {
        val type = object : TypeToken<List<Character>>() {}.type
        gson.fromJson(context.assets.open("characters.json").bufferedReader().use { it.readText() }, type)
    }
    suspend fun load(): SavedState {
        val raw = context.nexusStore.data.first()[key] ?: return SavedState()
        return runCatching { gson.fromJson(raw, SavedState::class.java) }.getOrDefault(SavedState()).let {
            if (engine.valid(it.progress)) it else it.copy(progress = Progress())
        }
    }
    suspend fun save(state: SavedState) { context.nexusStore.edit { it[key] = gson.toJson(state) } }
}

data class CvImage(val super_url: String?, val medium_url: String?)
data class CvRef(val id: Int?, val name: String?)
data class CvCharacter(
    val id: Int, val name: String?, val deck: String?, val description: String?,
    val image: CvImage?, val powers: List<CvRef>?, val issue_credits: List<CvRef>?, val site_detail_url: String?
)
data class CvResponse<T>(val status_code: Int, val error: String?, val results: T?, val number_of_total_results: Int)
interface ComicVineApi {
    @GET("search/") suspend fun search(
        @Query("query") query: String, @Query("offset") offset: Int = 0,
        @Query("resources") resources: String = "character", @Query("limit") limit: Int = 20,
        @Query("field_list") fields: String = "id,name,deck,image,site_detail_url"
    ): CvResponse<List<CvCharacter>>
    @GET("character/4005-{id}/") suspend fun detail(
        @Path("id") id: Int,
        @Query("field_list") fields: String = "id,name,deck,description,image,powers,issue_credits,site_detail_url"
    ): CvResponse<CvCharacter>
}
data class SearchPage(val characters: List<Character>, val total: Int)
class ComicVineRepository {
    val configured = BuildConfig.COMIC_VINE_API_KEY.isNotBlank()
    private val api = Retrofit.Builder().baseUrl("https://comicvine.gamespot.com/api/")
        .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val url = chain.request().url.newBuilder().addQueryParameter("api_key", BuildConfig.COMIC_VINE_API_KEY)
                    .addQueryParameter("format", "json").build()
                chain.proceed(chain.request().newBuilder().url(url).header("User-Agent", "Nexus-Android/1.0").build())
            }.build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(ComicVineApi::class.java)
    private val searchCache = linkedMapOf<String, SearchPage>()
    private val detailCache = linkedMapOf<Int, Character>()
    private fun CvCharacter.toCharacter(): Character = Character(
        id = "cv_$id", name = name ?: "Personagem", role = "Personagem • Comic Vine",
        description = Html.fromHtml(description ?: deck.orEmpty(), Html.FROM_HTML_MODE_LEGACY).toString().trim(),
        image = image?.super_url ?: image?.medium_url.orEmpty(), portrait = image?.super_url ?: image?.medium_url.orEmpty(),
        powers = powers.orEmpty().mapNotNull { it.name }, appearances = issue_credits.orEmpty().mapNotNull { it.name }.take(30),
        comicVineId = id, source = "Comic Vine", sourceUrl = site_detail_url.orEmpty()
    )
    suspend fun search(query: String, offset: Int): SearchPage {
        check(configured) { "Configure COMIC_VINE_API_KEY para pesquisar online." }
        val key = "${query.trim().lowercase()}:$offset"
        searchCache[key]?.let { return it }
        val response = api.search(query.trim(), offset)
        check(response.status_code == 1) { response.error ?: "A Comic Vine não retornou resultados." }
        return SearchPage(response.results.orEmpty().map { it.toCharacter() }, response.number_of_total_results).also {
            if (searchCache.size >= 30) searchCache.remove(searchCache.keys.first())
            searchCache[key] = it
        }
    }
    suspend fun detail(id: Int): Character {
        detailCache[id]?.let { return it }
        check(configured) { "Configure a chave para carregar informações online." }
        val response = api.detail(id)
        check(response.status_code == 1) { response.error ?: "Falha ao buscar personagem." }
        return checkNotNull(response.results).toCharacter().also { detailCache[id] = it }
    }
}
