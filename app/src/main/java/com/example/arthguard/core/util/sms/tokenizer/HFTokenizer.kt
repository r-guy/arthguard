package com.example.arthguard.core.util.sms.tokenizer

import android.content.Context
import org.json.JSONObject

class HFTokenizer(context: Context) {

    private val maxLength = 96
    private val vocab: Map<String, Long>
    private val unkId: Long
    private val clsId: Long
    private val sepId: Long
    private val padId: Long

    init {
        val json = context.assets.open("tokenizer.json").bufferedReader().readText()
        val root = JSONObject(json)
        val model = root.getJSONObject("model")
        val vocabObj = model.getJSONObject("vocab")

        vocab = buildMap {
            vocabObj.keys().forEach { key ->
                put(key, vocabObj.getLong(key))
            }
        }

        unkId = vocab["[UNK]"] ?: 100L
        clsId = vocab["[CLS]"] ?: 101L
        sepId = vocab["[SEP]"] ?: 102L
        padId = vocab["[PAD]"] ?: 0L
    }

    fun tokenize(text: String): Pair<LongArray, LongArray> {
        val tokens = wordPieceTokenize(text.lowercase())
        
        val inputIds = LongArray(maxLength) { padId }
        val attentionMask = LongArray(maxLength) { 0L }

        inputIds[0] = clsId
        attentionMask[0] = 1L

        val maxTokens = minOf(tokens.size, maxLength - 2)
        for (i in 0 until maxTokens) {
            inputIds[i + 1] = tokens[i]
            attentionMask[i + 1] = 1L
        }

        inputIds[maxTokens + 1] = sepId
        attentionMask[maxTokens + 1] = 1L

        return Pair(inputIds, attentionMask)
    }

    private fun wordPieceTokenize(text: String): List<Long> {
        val tokens = mutableListOf<Long>()
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }

        for (word in words) {
            var start = 0
            while (start < word.length) {
                var end = word.length
                var found = false

                while (start < end) {
                    val subword = if (start == 0) word.substring(start, end) else "##" + word.substring(start, end)
                    val id = vocab[subword]
                    if (id != null) {
                        tokens.add(id)
                        start = end
                        found = true
                        break
                    }
                    end--
                }

                if (!found) {
                    tokens.add(unkId)
                    start++
                }
            }
        }
        return tokens
    }
}
