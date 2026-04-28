package com.wagmilabs.sigil.core.utilities

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object CanonicalJSON {

    fun canonicalize(json: String): String {
        val element = JsonParser.parseString(json)
        val sorted = sortKeys(element)
        return Gson().toJson(sorted)
            .replace(" ", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("\t", "")
    }

    fun canonicalize(obj: Any): String {
        val json = Gson().toJson(obj)
        return canonicalize(json)
    }

    private fun sortKeys(element: JsonElement): JsonElement {
        return when {
            element.isJsonObject -> {
                val sorted = JsonObject()
                val keys = element.asJsonObject.keySet().sorted()
                keys.forEach { key ->
                    sorted.add(key, sortKeys(element.asJsonObject.get(key)))
                }
                sorted
            }
            element.isJsonArray -> {
                val sorted = JsonArray()
                element.asJsonArray.forEach { sorted.add(sortKeys(it)) }
                sorted
            }
            else -> element
        }
    }
}
