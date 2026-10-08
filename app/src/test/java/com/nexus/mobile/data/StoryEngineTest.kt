package com.nexus.mobile.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class StoryEngineTest {
    private val scenes: List<Scene> = Gson().fromJson(File("src/main/assets/episode_1.json").readText(), object : TypeToken<List<Scene>>() {}.type)
    private val engine = StoryEngine(scenes)
    @Test fun everySceneIsReachableAndBothEndingsExist() {
        val visited = mutableSetOf<String>(); val endings = mutableSetOf<String>()
        fun visit(id: String, ancestors: Set<String>) {
            assertFalse("Cycle at $id", id in ancestors)
            visited += id
            val s = engine.scenes.getValue(id)
            val edges = s.choices.map { it.next } + listOfNotNull(s.next)
            if (edges.isEmpty()) endings += id
            edges.forEach { visit(it, ancestors + id) }
        }
        visit("83:29", emptySet())
        assertEquals(53, visited.size)
        assertEquals(setOf("102:837", "104:974"), endings)
    }
    @Test fun choicesCannotBeSkippedAndBackAllowsChangingBranch() {
        val initial = Progress(sceneId = "96:451")
        assertEquals(initial, engine.advance(initial))
        val branchA = engine.advance(initial, 0)
        assertEquals("96:460", branchA.sceneId)
        val back = engine.back(branchA)
        assertEquals(initial, back)
        val branchB = engine.advance(back, 1)
        assertEquals("96:474", branchB.sceneId)
        assertEquals(1, branchB.decisions.size)
    }
    @Test fun everyChoiceCombinationTerminatesAndProgressNeverDecreases() {
        var finishes = 0
        fun traverse(p: Progress) {
            val scene = engine.scene(p)
            val options: List<Int?> = if (scene.choices.isEmpty()) listOf(null) else scene.choices.indices.toList()
            options.forEach { choice ->
                val next = engine.advance(p, choice)
                assertTrue(engine.fraction(next) >= engine.fraction(p))
                if (next.completed) { finishes++; assertEquals(1f, engine.fraction(next)) }
                else traverse(next)
            }
        }
        traverse(Progress()); assertEquals(16, finishes)
    }
    @Test fun progressSurvivesJsonRoundTripAndKeepsFavorites() {
        val state = SavedState(progress = engine.advance(Progress("100:655"), 1), favorites = listOf(Character(id = "miles", name = "Miles")))
        val gson = Gson()
        assertEquals(state, gson.fromJson(gson.toJson(state), SavedState::class.java))
    }
    @Test fun finalSceneRequiresExplicitCompletionAndCanBeRevisited() {
        val p = Progress("104:974")
        assertFalse(p.completed)
        val complete = engine.advance(p)
        assertTrue(complete.completed)
        assertEquals(p, engine.back(complete))
    }
}
