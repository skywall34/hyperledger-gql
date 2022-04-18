package com.hyperledger.gql.exceptions

import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult

interface CustomExceptionInterface {
    fun dataFetchingExceptionHandler(
        handlerParameters: DataFetcherExceptionHandlerParameters
    ): DataFetcherExceptionHandlerResult
}