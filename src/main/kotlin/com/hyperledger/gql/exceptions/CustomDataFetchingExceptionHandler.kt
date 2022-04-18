package com.hyperledger.gql.exceptions

import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import org.springframework.stereotype.Component

@Component
class CustomDataFetchingExceptionHandler : DataFetcherExceptionHandler {
    private val defaultHandler = DefaultDataFetcherExceptionHandler()

    override fun onException(handlerParameters: DataFetcherExceptionHandlerParameters): DataFetcherExceptionHandlerResult {
        return when (handlerParameters.exception) {
            is CustomExceptionInterface -> (handlerParameters.exception as CustomExceptionInterface).dataFetchingExceptionHandler(handlerParameters)
            else -> defaultHandler.onException(handlerParameters)
        }
    }
}