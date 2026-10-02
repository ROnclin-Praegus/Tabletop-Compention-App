package com.homebrew.tabletopcompanion.server

import fi.iki.elonen.NanoHTTPD
import java.util.UUID

data class UploadRecord(val timestamp: Long, val jsonData: String, val id: String = UUID.randomUUID().toString())

class LocalGameServer(port: Int, private val onDataReceived: (String) -> Unit) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val method = session.method
        val uri = session.uri

        // Handle CORS
        if (method == Method.OPTIONS) {
            val response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "")
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS")
            response.addHeader("Access-Control-Allow-Headers", "Authorization, Content-Type")
            return response
        }

        if (uri == "/api/sync" && method == Method.POST) {
            val headers = session.headers
            val authHeader = headers["authorization"]
            
            val currentKey = GameServerManager.apiKey
            if (authHeader == null || !authHeader.equals("Bearer $currentKey", ignoreCase = true)) {
                println("Auth failed. Expected: Bearer $currentKey, Got: $authHeader")
                return corsResponse(newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Unauthorized"))
            }

            val map = HashMap<String, String>()
            try {
                session.parseBody(map)
                // NanoHTTPD places parsed application/x-www-form-urlencoded fields into session.parms, not the map
                val bodyData = session.parms["postData"] ?: map["postData"]
                if (bodyData != null) {
                    onDataReceived(bodyData)
                    return corsResponse(newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"success\"}"))
                } else {
                    return corsResponse(newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing body data"))
                }
            } catch (e: Exception) {
                return corsResponse(newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}"))
            }
        }

        return corsResponse(newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found"))
    }

    private fun corsResponse(response: Response): Response {
        response.addHeader("Access-Control-Allow-Origin", "*")
        return response
    }
}
