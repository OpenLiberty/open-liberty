/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.language.SourceLocation;

import io.leangen.graphql.execution.InvocationContext;
import io.leangen.graphql.execution.ResolverInterceptor;

import org.eclipse.microprofile.graphql.GraphQLException;
/**
 * Used to handle instances of <code>GraphQLException</code> that contain partial results. 
 */
public class PartialResultsResolverInterceptor implements ResolverInterceptor {
    

    @Override
    public Object aroundInvoke(InvocationContext context, Continuation continuation) throws Exception {
        try {
            return continuation.proceed(context);
        } catch (Throwable ex) {
            Throwable t = ex.getCause();
            if (t == null || !(t instanceof GraphQLException)) {
                throw ex;
            }
            GraphQLException gex = (GraphQLException) t;
            Object partialResults = gex.getPartialResults();
            if (partialResults == null) {
                throw ex;
            }
            GraphQLError error = createNewGraphQLError(gex.getMessage(),
                                                       Collections.emptyList(),
                                                       gex.getExceptionType());
            DataFetcherResult result = new DataFetcherResult(partialResults, Collections.singletonList(error));
            // once upgrade to graphql-java to latest, replace the above line with this:
//            DataFetcherResult result = DataFetcherResult.Builder(partialResults)
//                                                        .error(error)
//                                                        .build();
            return result;
        }
    }
    
    private static GraphQLError createNewGraphQLError(String message,
                                              List<SourceLocation> locations, 
                                              GraphQLException.ExceptionType exType) {
        return new GraphQLError() {
            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public List<SourceLocation> getLocations() {
                return locations;
            }

            @Override
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public ErrorType getErrorType() {
                return fromExceptionType(exType);
            }
            
            @Override
            @JsonIgnore
            public Map<String, Object> getExtensions() {
                return null;
            }
        };
    }

    private static ErrorType fromExceptionType(GraphQLException.ExceptionType exType) {
        if (exType != null) {
            switch (exType) {
                case DataFetchingException: return ErrorType.DataFetchingException;
                case OperationNotSupported: return ErrorType.OperationNotSupported;
                case ExecutionAborted:      return ErrorType.ExecutionAborted;
            }
        }
        return null;
    }
}
