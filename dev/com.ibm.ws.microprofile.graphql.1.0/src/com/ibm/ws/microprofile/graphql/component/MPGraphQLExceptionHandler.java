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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        //TODO: internationalize
        if (error instanceof Throwable) {
            log.log(Level.SEVERE, "Error executing query!", (Throwable) error);
        } else if (error instanceof ExceptionWhileDataFetching) {
            log.log(Level.SEVERE, "Error executing query {}" + error.getMessage(), ((ExceptionWhileDataFetching) error).getException());
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
            final GraphQLException.ExceptionType exType;
            if (t instanceof GraphQLException) {
                message = t.getMessage();
                exType = ((GraphQLException)t).getExceptionType();
            } else {
                message = DEFAULT_MESSAGE;
                exType = null;
            }
            return createNewGraphQLError(message, error.getLocations(), exType);
            
        }
        return error;
    }

    static GraphQLError createNewGraphQLError(String message, List<SourceLocation> locations, GraphQLException.ExceptionType exType) {
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
    
    static ErrorType fromExceptionType(GraphQLException.ExceptionType exType) {
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
