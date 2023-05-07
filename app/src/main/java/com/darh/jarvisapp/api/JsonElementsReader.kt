package com.darh.jarvisapp.api

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import timber.log.Timber

internal class JsonElementsReader {

    private val jsonFactory: JsonFactory = JsonFactory()

    private var buffer = ""

    private val fields: MutableMap<String, Any> = mutableMapOf()

    var stream: String? = null
        private set

    private val fieldIndicationChars = """[\[}:"]""".toRegex()

    fun feedChunk(chunk: String) {
        buffer += chunk
        if (fieldIndicationChars.containsMatchIn(chunk)) {
            processJson()
        }
    }

    fun getField(name: String): Any? {
        return fields[name]
    }

    private fun processJson() {
        runCatching {
            jsonFactory.createParser(buffer).use { jsonParser ->
                while (jsonParser.nextToken() != null) {
                    val currentToken = jsonParser.currentToken
                    if (currentToken == JsonToken.FIELD_NAME) {
                        val fieldName = jsonParser.currentName
                        when (jsonParser.nextToken()) {
                            JsonToken.VALUE_STRING -> processStringValue(fieldName, jsonParser)
                            JsonToken.VALUE_NUMBER_INT,
                            JsonToken.VALUE_NUMBER_FLOAT -> fields[fieldName] =
                                jsonParser.numberValue
                            JsonToken.START_ARRAY -> processArrayValue(jsonParser, fieldName)
                            else -> { // Ignore
                            }
                        }
                    }
                }
            }
        }.onFailure {
        } // No need to report, it will fail every time there is incomplete value. values are saved.
//        Timber.tag(OPEN_AI)
//            .d("processJson finished. fields: [${fields.map { "${it.key}: ${it.value}" }}]")
    }

    private fun processStringValue(fieldName: String, jsonParser: JsonParser) {
        // Setting the field to an empty string by default, allows to display loading for this field.
        fields.putIfAbsent(fieldName, "")
        // Save the value if it is ready, throws exception otherwise.
        fields[fieldName] = jsonParser.valueAsString
    }

    private fun processArrayValue(jsonParser: JsonParser, fieldName: String) {
        // Setting the field to an empty list by default, allows to display loading for this field.
        fields.putIfAbsent(fieldName, emptyList<String>())

        val stringList = mutableListOf<String>()
        var isCompleteList = false
        var token = jsonParser.nextToken()
        while (!isCompleteList && token != null) {
            stringList.add(jsonParser.valueAsString)
            token = jsonParser.nextToken()
            isCompleteList = token == JsonToken.END_ARRAY
        }
        if (isCompleteList) {
            fields[fieldName] = stringList
        }
    }
}
