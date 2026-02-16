package com.catechism.platform.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}") private val jwtSecret: String,
    @Value("\${app.jwt.expiration}") private val jwtExpirationMs: Long
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Generate JWT token for a user
     */
    fun generateToken(userId: UUID, email: String, role: String): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    /**
     * Get user ID from JWT token
     */
    fun getUserIdFromToken(token: String): UUID {
        val claims = getClaims(token)
        return UUID.fromString(claims.subject)
    }

    /**
     * Get email from JWT token
     */
    fun getEmailFromToken(token: String): String {
        val claims = getClaims(token)
        return claims["email"] as String
    }

    /**
     * Get role from JWT token
     */
    fun getRoleFromToken(token: String): String {
        val claims = getClaims(token)
        return claims["role"] as String
    }

    /**
     * Validate JWT token
     */
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get all claims from token
     */
    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}