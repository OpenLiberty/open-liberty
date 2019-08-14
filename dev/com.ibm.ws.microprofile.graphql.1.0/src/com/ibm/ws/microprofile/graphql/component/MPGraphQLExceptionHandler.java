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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import graphql.ErrorType;
import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import graphql.servlet.DefaultGraphQLErrorHandler;
import graphql.servlet.GenericGraphQLError;

import org.eclipse.microprofile.graphql.GraphQLException;

public class MPGraphQLExceptionHandler extends DefaultGraphQLErrorHandler {

    private static final Logger log = Logger.getLogger(MPGraphQLExceptionHandler.class.getName());
    private static final String DEFAULT_MESSAGE = ConfigFacade.getOptionalValue("mp.graphql.defaultErrorMessage", String.class).orElse("Internal Server Error"); //TODO: lookup msg via MP Config
    private static final boolean LOG_APPLICATION_EXCEPTION = true;

    @Override
    public List<GraphQLError> processErrors(List<GraphQLError> errors) {
        return errors.stream()
                      .filter(this::logError)
                      .map(this::replaceUserExceptions)
                      .collect(Collectors.toList());
    }


    protected boolean logError(GraphQLError error) {
        if (error instanceof Throwable) {
            log.log(Level.SEVERE, "Error executing query!", (Throwable) error);
            ((Throwable)error).printStackTrace();
        } else if (error instanceof ExceptionWhileDataFetching) {
            log.log(Level.SEVERE, "Error executing query {}" + error.getMessage(), ((ExceptionWhileDataFetching) error).getException());
            ((ExceptionWhileDataFetching) error).getException().printStackTrace();
        } else {
            log.log(Level.SEVERE, "Error executing query ({}): {}" + error.getClass().getSimpleName(), error.getMessage());
        }
        return true;
    }

    private GraphQLError replaceUserExceptions(GraphQLError error) {
        final Throwable t;
        if (error instanceof Throwable) {
            t = (Throwable) error;
        } else if (error instanceof ExceptionWhileDataFetching) {
            t = ((ExceptionWhileDataFetching) error).getException();
        } else {
            t = null;
        }
        
        if (t != null) {
            final String message;
            final ErrorType errorType;
            if (t instanceof GraphQLException) {
                message = t.getMessage();
                switch ( ((GraphQLException)t).getExceptionType() ) {
                    case DataFetchingException: errorType = ErrorType.DataFetchingException; break;
                    case OperationNotSupported: errorType = ErrorType.OperationNotSupported; break;
                    case ExecutionAborted: errorType = ErrorType.ExecutionAborted; break;
                    default: errorType = null;
                }
            } else {
                message = DEFAULT_MESSAGE;
                errorType = null;
            }
            return new GraphQLError() {
                @Override
                public String getMessage() {
                    return message;
                }

                @Override
                public List<SourceLocation> getLocations() {
                    return error.getLocations();
                }

                @Override
                @JsonInclude(JsonInclude.Include.NON_NULL)
                public ErrorType getErrorType() {
                    return errorType;
                }
            };
        }
        return error;
    }
}
