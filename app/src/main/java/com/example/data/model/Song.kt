package com.example.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mediaUri: String = "",
    val coverColorStart: Long = 0xFF10B981, // Default Bengal Green
    val coverColorEnd: Long = 0xFF0D9488,   // Default Bengal Teal
    val isLocal: Boolean = false,
    val description: String = "" // Summary of its style or meaning (e.g. "rabindra_classic", "lalon_mystic", "folk_wedding")
)
