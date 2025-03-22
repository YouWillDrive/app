package ru.gd_alt.youwilldrive.models

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://supa.gd-alt.ru/",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ewogICJyb2xlIjogImFub24iLAogICJpc3MiOiAic3VwYWJhc2UiLAogICJpYXQiOiAxNzQxNTU0MDAwLAogICJleHAiOiAxODk5MzIwNDAwCn0.mMAbh7mV3DKFNXdV8-EkdYGDQNh2AsWdd8d4W2HPH6g"
    ) {
        install(Postgrest)
        install(Auth)
        install(Realtime)
    }
}