package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(val text: String)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(val contents: List<Content>)

@JsonClass(generateAdapter = true)
data class PartResponse(val text: String?)

@JsonClass(generateAdapter = true)
data class ContentResponse(val parts: List<PartResponse>)

@JsonClass(generateAdapter = true)
data class Candidate(val content: ContentResponse)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(val candidates: List<Candidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiRepository {

    suspend fun getSongRecommendation(songTitle: String, artist: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key not configured. Please add your GEMINI_API_KEY securely in the AI Studio Secrets panel."
        }

        val prompt = """
            The user is playing the song called '$songTitle' by '$artist' in 'Amar Gaan'—a beautiful, culturally inspired Bangladeshi music player.
            Based on this track, please suggest 3 similar real Bangladeshi songs (or suitable international acoustics/fusion). 
            For each suggested song, provide a very brief, colorful 1-sentence reason why they will love it, capturing the rhythmic beats, traditional instruments (like Dotara or Bansuri), or emotional feel.
            Format your output beautifully and concisely with bullets of song name and 1-sentence description. Keep it short (max 100 words), warm, and inspirational.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Could not load suggestions right now. Keep listening!"
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error fetching recommendation", e)
            "Error: ${e.message}. Connect to WiFi and make sure your AI Studio Secrets are saved."
        }
    }

    suspend fun getLyricsSnippet(songTitle: String, artist: String, songDescription: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Traditional Bangla Lyrics: Enjoy the soothing acoustic beats of $songTitle."
        }

        val prompt = """
            The song '$songTitle' by artist '$artist' is selected. Description: $songDescription.
            Provide a beautiful, poetic 4-line quote (English translation or inspirational lyrical description in Bengal aesthetic style) that fits the theme of this track perfectly.
            Output ONLY the 4 poetic lines, separated by line breaks. No titles, no greetings, no introductory text, no credits. Keep it highly lyrical, motivational, or fun.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Warm breeze of music, flowing like the rivers of Meghna,\nBringing solace to my heart,\nMay this song accompany your quiet moments,\nAnd wash away your fatigue."
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error fetching lyrics", e)
            "A warm breeze of music, flowing like the rivers of Bengal,\nBringing solace to the wandering heart,\nMay this melody accompany your offline hours,\nAnd bright memories surround you."
        }
    }
}
