package com.catechism.platform.graphql

import com.catechism.platform.service.AuthenticationException
import com.catechism.platform.service.ValidationException
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class GraphQLExceptionHandler : DataFetcherExceptionResolverAdapter() {

    override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
        return when (ex) {
            is AuthenticationException -> {
                GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.UNAUTHORIZED)
                    .message(ex.message ?: "Authentication failed")
                    .path(env.executionStepInfo.path)
                    .location(env.field.sourceLocation)
                    .build()
            }
            is ValidationException -> {
                GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.message ?: "Validation failed")
                    .path(env.executionStepInfo.path)
                    .location(env.field.sourceLocation)
                    .build()
            }
            is IllegalArgumentException -> {
                GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(ex.message ?: "Invalid input")
                    .path(env.executionStepInfo.path)
                    .location(env.field.sourceLocation)
                    .build()
            }
            is SecurityException -> {
                GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.UNAUTHORIZED)
                    .message(ex.message ?: "Access denied")
                    .path(env.executionStepInfo.path)
                    .location(env.field.sourceLocation)
                    .build()
            }
            else -> {
                // For unexpected errors, log them but don't expose internals
                logger.error("Unexpected error in GraphQL operation", ex)
                GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.INTERNAL_ERROR)
                    .message("An unexpected error occurred")
                    .path(env.executionStepInfo.path)
                    .location(env.field.sourceLocation)
                    .build()
            }
        }
    }
}