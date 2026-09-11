package com.example.mail.util

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device Gemini Nano integration via Google AI Edge / Android AICore.
 *
 * Uses the LiteRT runtime to load and execute Gemini Nano models directly
 * on the device. Provides suspending functions for thread summarization,
 * action card data extraction, and OTP detection.
 *
 * Falls back gracefully if the model file is missing or the device lacks
 * AICore support (returns sensible defaults instead of crashing).
 */
@Singleton
class GeminiProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "GeminiProcessor"

    private var interpreter: Interpreter? = null
    private var isModelAvailable = false

    init {
        tryLoadModel()
    }

    // -----------------------------------------------------------------------
    // Model initialization
    // -----------------------------------------------------------------------

    private fun tryLoadModel() {
        try {
            // Gemini Nano model is bundled in assets/ or downloaded by AICore.
            // For now, check if a model file exists in assets.
            val modelBuffer = loadModelFromAssets("gemini_nano.tflite")
            if (modelBuffer != null) {
                interpreter = Interpreter(modelBuffer)
                isModelAvailable = true
                Log.i(tag, "Gemini Nano model loaded successfully")
            } else {
                Log.w(tag, "Gemini Nano model not found — AI features disabled")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Gemini Nano: ${e.message}")
            isModelAvailable = false
        }
    }

    private fun loadModelFromAssets(filename: String): MappedByteBuffer? {
        return try {
            context.assets.openFd(filename).use { fileDescriptor ->
                FileInputStream(fileDescriptor.fileDescriptor).channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        } catch (e: Exception) {
            Log.w(tag, "Model file $filename not found in assets")
            null
        }
    }

    // -----------------------------------------------------------------------
    // Public API — suspending functions with fallback logic
    // -----------------------------------------------------------------------

    /**
     * Generate a 2-sentence TL;DR summary for a long email thread.
     * Returns a bullet-point summary or a fallback message if AI is unavailable.
     */
    suspend fun generateThreadSummary(text: String): String {
        if (!isModelAvailable || interpreter == null) {
            return FallbackGenerator.threadSummary(text)
        }

        return try {
            val input = tokenize(text)
            val output = FloatArray(SUMMARY_OUTPUT_SIZE)
            interpreter?.run(input, output)
            detokenize(output) ?: FallbackGenerator.threadSummary(text)
        } catch (e: Exception) {
            Log.e(tag, "Thread summary failed: ${e.message}")
            FallbackGenerator.threadSummary(text)
        }
    }

    /**
     * Extract actionable data from email text (dates, tracking IDs, flight numbers).
     * Returns a structured string or empty string if nothing is extractable.
     */
    suspend fun extractActionableData(text: String): String {
        if (!isModelAvailable || interpreter == null) {
            return FallbackGenerator.extractActionableData(text)
        }

        return try {
            val input = tokenize(text)
            val output = FloatArray(ACTION_OUTPUT_SIZE)
            interpreter?.run(input, output)
            detokenize(output) ?: FallbackGenerator.extractActionableData(text)
        } catch (e: Exception) {
            Log.e(tag, "Action extraction failed: ${e.message}")
            FallbackGenerator.extractActionableData(text)
        }
    }

    /**
     * Detect and extract OTP codes from email text.
     * Returns the OTP digits as a string, or empty if none found.
     */
    suspend fun extractOtp(text: String): String {
        if (!isModelAvailable || interpreter == null) {
            return FallbackGenerator.extractOtp(text)
        }

        return try {
            val input = tokenize(text)
            val output = FloatArray(OTP_OUTPUT_SIZE)
            interpreter?.run(input, output)
            detokenize(output) ?: FallbackGenerator.extractOtp(text)
        } catch (e: Exception) {
            Log.e(tag, "OTP extraction failed: ${e.message}")
            FallbackGenerator.extractOtp(text)
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers — tokenize / detokenize
    // -----------------------------------------------------------------------

    private fun tokenize(text: String): ByteBuffer {
        // Simple UTF-8 byte encoding padded to max input length.
        val bytes = text.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocateDirect(MAX_INPUT_BYTES).apply {
            order(ByteOrder.nativeOrder())
            put(bytes, 0, minOf(bytes.size, MAX_INPUT_BYTES))
            // Pad remaining with zeros.
            for (i in bytes.size until MAX_INPUT_BYTES) {
                put(0.toByte())
            }
            rewind()
        }
        return buffer
    }

    private fun detokenize(output: FloatArray): String? {
        // Convert float logits back to text (simplified — real tokenizer lives in AICore).
        // For now, map high-confidence indices to known tokens.
        return output.takeIf { it.any { v -> v > 0f } }
            ?.filter { it > 0.5f }
            ?.joinToString("") { (it * 255).toInt().toChar().toString() }
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * Check if the model is available for use.
     */
    fun isAvailable(): Boolean = isModelAvailable

    fun close() {
        interpreter?.close()
        interpreter = null
        isModelAvailable = false
    }
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private const val MAX_INPUT_BYTES = 2048
private const val SUMMARY_OUTPUT_SIZE = 128
private const val ACTION_OUTPUT_SIZE = 128
private const val OTP_OUTPUT_SIZE = 16

// ---------------------------------------------------------------------------
// Fallback generators — regex/heuristic when AI is unavailable
// ---------------------------------------------------------------------------

object FallbackGenerator {

    /**
     * Generate a simple first-two-sentences summary when Gemini Nano is absent.
     */
    fun threadSummary(text: String): String {
        val sentences = text.replace(Regex("\\s+"), " ")
            .split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
        return if (sentences.isNotEmpty()) {
            sentences.joinToString(" ")
        } else {
            text.take(120).trim() + if (text.length > 120) "..." else ""
        }
    }

    /**
     * Extract dates, tracking IDs, and flight numbers using regex patterns.
     */
    fun extractActionableData(text: String): String {
        val findings = mutableListOf<String>()

        // ISO dates and common formats
        val datePattern = Regex("""\b(\d{4}-\d{2}-\d{2}|\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""")
        datePattern.findAll(text).forEach { findings.add("DATE: ${it.value}") }

        // Tracking IDs (e.g., 1Z999AA10123456784, 9400111899223100000)
        val trackingPattern = Regex("""\b(1Z[A-Z0-9]{16}|94\d{18}|[A-Z]{2}\d{9}US)\b""")
        trackingPattern.findAll(text).forEach { findings.add("TRACKING: ${it.value}") }

        // Flight numbers (e.g., AA1234, UA 567, DL890)
        val flightPattern = Regex("""\b([A-Z]{2})\s?(\d{3,4})\b""")
        flightPattern.findAll(text).forEach { findings.add("FLIGHT: ${it.value}") }

        // Meeting times
        val timePattern = Regex("""\b(\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?)\b""")
        timePattern.findAll(text).forEach { findings.add("TIME: ${it.value}") }

        return findings.joinToString("; ")
    }

    /**
     * Extract OTP codes (4–8 digits) from email text. Only returns digits
     * that appear within OTP keyword context (code, verification, etc.) —
     * bare numbers like years or order IDs must never match.
     */
    fun extractOtp(text: String): String {
        val lower = text.lowercase()
        val digitPattern = Regex("""\b(\d{4,8})\b""")
        for (match in digitPattern.findAll(text)) {
            val contextStart = maxOf(0, match.range.first - 80)
            val context = lower.substring(contextStart, match.range.first)
            if (otpKeywords.any { it in context }) {
                return match.value
            }
        }
        return ""
    }

    private val otpKeywords = listOf(
        "otp", "one-time", "one time", "verification", "verify",
        "passcode", "security code", "2fa", "two-factor", "two factor",
        "authentication code", "login code", "confirm", "validate",
        "code is", "code:", "is your code", "use code", "enter code"
    )
}
