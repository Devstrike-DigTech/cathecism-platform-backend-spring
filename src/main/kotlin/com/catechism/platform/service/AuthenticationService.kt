package com.catechism.platform.service

import com.catechism.platform.domain.AppUser
import com.catechism.platform.domain.UserRole
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class AuthenticationService(
    private val userRepository: AppUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    /**
     * Register a new user
     */
    fun register(
        email: String,
        password: String,
        name: String,
        diocese: String? = null
    ): AuthResult {
        // Check if user already exists
        if (userRepository.findByEmail(email) != null) {
            throw ValidationException("User with email $email already exists")
        }

        // Validate email format
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            throw ValidationException("Invalid email format")
        }

        // Validate password strength (at least 8 characters)
        if (password.length < 8) {
            throw ValidationException("Password must be at least 8 characters long")
        }

        // Create user
        val user = AppUser(
            email = email,
            passwordHash = passwordEncoder.encode(password),
            name = name,
            role = UserRole.PUBLIC_USER,
            verified = false,
            diocese = diocese
        )

        val savedUser = userRepository.save(user)

        // Generate token
        val token = jwtTokenProvider.generateToken(
            userId = savedUser.id,
            email = savedUser.email,
            role = savedUser.role.name
        )

        return AuthResult(
            token = token,
            user = savedUser
        )
    }

    /**
     * Login user
     */
    fun login(email: String, password: String): AuthResult {
        // Find user
        val user = userRepository.findByEmail(email)

        if (user == null) {
            // Don't reveal if email exists or not (security best practice)
            throw AuthenticationException("Invalid email or password")
        }

        // Check password
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw AuthenticationException("Invalid email or password")
        }

        // Generate token
        val token = jwtTokenProvider.generateToken(
            userId = user.id,
            email = user.email,
            role = user.role.name
        )

        return AuthResult(
            token = token,
            user = user
        )
    }

    /**
     * Get user from token
     */
    fun getUserFromToken(token: String): AppUser? {
        if (!jwtTokenProvider.validateToken(token)) {
            return null
        }

        val userId = jwtTokenProvider.getUserIdFromToken(token)
        return userRepository.findById(userId).orElse(null)
    }

    /**
     * Validate token and return user
     */
    fun validateTokenAndGetUser(token: String): AppUser? {
        return try {
            if (jwtTokenProvider.validateToken(token)) {
                val userId = jwtTokenProvider.getUserIdFromToken(token)
                userRepository.findById(userId).orElse(null)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Result of authentication (login/register)
 */
data class AuthResult(
    val token: String,
    val user: AppUser
)

/**
 * Custom exception for authentication errors
 */
class AuthenticationException(message: String) : RuntimeException(message)

/**
 * Custom exception for validation errors
 */
class ValidationException(message: String) : RuntimeException(message)