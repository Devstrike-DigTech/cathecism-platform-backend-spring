package com.catechism.platform.graphql

import com.catechism.platform.domain.AppUser
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.service.AuthenticationService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class AuthResolver(
    private val authenticationService: AuthenticationService,
    private val userRepository: AppUserRepository
) {

    @MutationMapping
    fun login(
        @Argument email: String,
        @Argument password: String
    ): AuthPayloadDTO {
        val result = authenticationService.login(email, password)
        return AuthPayloadDTO(
            token = result.token,
            user = result.user.toDTO()
        )
    }

    @MutationMapping
    fun register(@Argument input: RegisterInput): AuthPayloadDTO {
        val result = authenticationService.register(
            email = input.email,
            password = input.password,
            name = input.name,
            diocese = input.diocese
        )
        return AuthPayloadDTO(
            token = result.token,
            user = result.user.toDTO()
        )
    }

    @QueryMapping
    fun me(): UserDTO? {
        // Get the authenticated user from Spring Security context
        val authentication = SecurityContextHolder.getContext().authentication

        // If not authenticated or anonymous, return null
        if (authentication == null || !authentication.isAuthenticated || authentication.principal == "anonymousUser") {
            return null
        }

        // The principal is the email (set in JwtAuthenticationFilter)
        val email = authentication.principal as? String ?: return null

        // Find user by email
        val user = userRepository.findByEmail(email) ?: return null

        return user.toDTO()
    }

    // Extension function to convert AppUser to DTO
    private fun AppUser.toDTO(): UserDTO {
        return UserDTO(
            id = this.id,
            email = this.email,
            name = this.name,
            role = this.role.name,
            verified = this.verified,
            diocese = this.diocese
        )
    }
}

// DTOs
data class AuthPayloadDTO(
    val token: String,
    val user: UserDTO
)

data class UserDTO(
    val id: UUID,
    val email: String,
    val name: String,
    val role: String,
    val verified: Boolean,
    val diocese: String?
)

data class RegisterInput(
    val email: String,
    val password: String,
    val name: String,
    val diocese: String?
)