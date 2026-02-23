package com.example.arthguard.core.util.sms

import android.content.Context
import ai.onnxruntime.*
import com.example.arthguard.core.util.sms.tokenizer.HFTokenizer
import java.nio.LongBuffer

class SmsClassifier(context: Context) {

    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val tokenizer = HFTokenizer(context)

    init {
        val modelFile = java.io.File(context.filesDir, "model.onnx")
        if (!modelFile.exists()) {
            context.assets.open("distil_sms_classifier_quantized.onnx").use { input ->
                modelFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
    }

    fun classify(text: String): String {

        val (inputIds, attentionMask) = tokenizer.tokenize(text)

        val shape = longArrayOf(1, inputIds.size.toLong())

        val inputTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(inputIds),
            shape
        )

        val maskTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(attentionMask),
            shape
        )

        val inputs = mapOf(
            "input_ids" to inputTensor,
            "attention_mask" to maskTensor
        )

        session.run(inputs).use { results ->
            val output = results[0].value as Array<FloatArray>
            val logits = output[0]

            val maxIndex = logits.indices.maxByOrNull { logits[it] } ?: 0

            return when (maxIndex) {
                0 -> "CREDIT"
                1 -> "DEBIT"
                else -> "NON_TRANSACTION"
            }
        }
    }
}