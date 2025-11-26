package com.example.ekycsimulate.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkModule {
    private const val BASE_URL = "http://10.0.2.2:8001" // Android Emulator to Localhost

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        expectSuccess = false // Allow handling 4xx/5xx manually
        
        install(Logging) {
            level = LogLevel.ALL
        }

        defaultRequest {
            url(BASE_URL)
        }
    }
}
