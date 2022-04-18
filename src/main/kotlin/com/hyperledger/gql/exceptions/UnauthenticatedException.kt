package com.hyperledger.gql.exceptions

import com.netflix.graphql.types.errors.ErrorType
import com.netflix.graphql.types.errors.TypedGraphQLError
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult

class UnauthenticatedException(): RuntimeException(), CustomExceptionInterface {
    override fun dataFetchingExceptionHandler(handlerParameters: DataFetcherExceptionHandlerParameters): DataFetcherExceptionHandlerResult {
        return DataFetcherExceptionHandlerResult.newResult().errors(
            listOf(TypedGraphQLError
                .newBuilder()
                .errorType(ErrorType.PERMISSION_DENIED)
                .path(handlerParameters.path)
                .message("Failed to get username! Authentication Failed!")
                .build()
            )
        ).build()
    }
}
