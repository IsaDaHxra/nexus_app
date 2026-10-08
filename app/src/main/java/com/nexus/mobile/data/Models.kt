package com.nexus.mobile.data

data class Character(
    val id: String = "", val name: String = "", val role: String = "",
    val description: String = "", val image: String = "", val portrait: String = image,
    val powers: List<String> = emptyList(), val appearances: List<String> = emptyList(),
    val comicVineId: Int? = null, val source: String = "Elenco local", val sourceUrl: String = ""
)
data class Layer(
    val id: String, val kind: String, val x: Float, val y: Float, val w: Float, val h: Float,
    val rot: Float = 0f, val a: Float = 1f, val asset: String = "", val text: String = "",
    val font: String = "", val size: Float = 0f, val center: Boolean = false
)
data class Choice(val label: String, val next: String)
data class Scene(val id: String, val title: String, val next: String?, val choices: List<Choice>, val layers: List<Layer>)
data class Decision(val sceneId: String, val label: String)
data class Progress(
    val sceneId: String = "83:29", val history: List<String> = emptyList(),
    val decisions: List<Decision> = emptyList(), val completed: Boolean = false
)
data class Preferences(val reducedMotion: Boolean = false, val textDelayMs: Long = 22L)
data class SavedState(
    val progress: Progress = Progress(), val favorites: List<Character> = emptyList(),
    val recentSearches: List<String> = emptyList(), val preferences: Preferences = Preferences()
)

/** Branching is explicit in episode_1.json. No scene number arithmetic. */
class StoryEngine(scenes: List<Scene>) {
    val scenes = scenes.associateBy { it.id }
    init {
        require(scenes.size == this.scenes.size) { "Duplicate scene ID" }
        require("83:29" in this.scenes)
        scenes.forEach { s -> (s.choices.map { it.next } + listOfNotNull(s.next)).forEach { require(it in this.scenes) } }
    }
    fun scene(p: Progress) = scenes.getValue(p.sceneId)
    fun advance(p: Progress, choice: Int? = null): Progress {
        if (p.completed) return p
        val s = scene(p)
        val selected = choice?.let { s.choices.getOrNull(it) }
        if (s.choices.isNotEmpty() && selected == null) return p
        val next = selected?.next ?: s.next ?: return p.copy(completed = true)
        return p.copy(sceneId = next, history = p.history + s.id,
            decisions = if (selected == null) p.decisions else p.decisions.filterNot { it.sceneId == s.id } + Decision(s.id, selected.label))
    }
    fun back(p: Progress): Progress {
        if (p.completed) return p.copy(completed = false)
        val previous = p.history.lastOrNull() ?: return p
        return p.copy(sceneId = previous, history = p.history.dropLast(1), decisions = p.decisions.filterNot { it.sceneId == previous })
    }
    private fun remaining(id: String, seen: Set<String> = emptySet()): Int {
        if (id in seen) return 0
        val s = scenes.getValue(id)
        return 1 + (s.choices.map { it.next }.plus(listOfNotNull(s.next)).maxOfOrNull { remaining(it, seen + id) } ?: 0)
    }
    fun fraction(p: Progress): Float = if (p.completed) 1f else p.history.size.toFloat() / (p.history.size + remaining(p.sceneId))
    fun valid(p: Progress) = p.sceneId in scenes && p.history.all { it in scenes }
}
