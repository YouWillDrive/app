package ru.gd_alt.youwilldrive.models

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://supa.gd-alt.ru/",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ewogICJyb2xlIjogInNlcnZpY2Vfcm9sZSIsCiAgImlzcyI6ICJzdXBhYmFzZSIsCiAgImlhdCI6IDE3NDE1NTQwMDAsCiAgImV4cCI6IDE4OTkzMjA0MDAKfQ.Nyktw3Oi9XruSSl4defHz5WF0dtrIpsDt3L3V90WXMA"
    ) {
        install(Postgrest)
        install(Auth)
        install(Realtime)
    }
}