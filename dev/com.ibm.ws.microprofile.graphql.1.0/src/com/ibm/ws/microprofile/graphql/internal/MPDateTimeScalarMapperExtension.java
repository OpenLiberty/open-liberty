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
package com.ibm.ws.microprofile.graphql.internal;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import io.leangen.graphql.generator.mapping.ScalarMapperExtension;
import io.leangen.graphql.util.Scalars;
import io.leangen.graphql.util.Scalars.ThrowingFunction;

public class MPDateTimeScalarMapperExtension implements ScalarMapperExtension {

    public static final GraphQLScalarType GraphQLLocalDate = temporalScalar(LocalDate.class, "Date", "String used to define a Date");

    public static final GraphQLScalarType GraphQLLocalTime = temporalScalar(LocalTime.class, "Time", "String used to define a Time");

    public static final GraphQLScalarType GraphQLLocalDateTime = temporalScalar(LocalDateTime.class, "DateTime", "String used to define a Date and Time");

    @Override
    public Map<Type, GraphQLScalarType> updateScalarMappings(Map<Type, GraphQLScalarType> scalarMapping) {
        scalarMapping.put(LocalDate.class, GraphQLLocalDate);
        scalarMapping.put(LocalTime.class, GraphQLLocalTime);
        scalarMapping.put(LocalDateTime.class, GraphQLLocalDateTime);
        return scalarMapping;
    }

    private static <T> GraphQLScalarType temporalScalar(Class<?> type, String name, String description) {
        return new GraphQLScalarType(name, "Built-in scalar representing " + description, new Coercing<String, String>() {

            @Override
            @SuppressWarnings("unchecked")
            public String serialize(Object dataFetcherResult) {
                if (dataFetcherResult instanceof String) {
                    return (String) dataFetcherResult;
                }
                // if unformatted by JSON-B, use default ISO formatter
                if (dataFetcherResult instanceof LocalDate) {
                    return DateTimeFormatter.ISO_DATE.format((LocalDate)dataFetcherResult);
                }
                if (dataFetcherResult instanceof LocalTime) {
                    return DateTimeFormatter.ISO_TIME.format((LocalTime)dataFetcherResult);
                }
                if (dataFetcherResult instanceof LocalDateTime) {
                    return DateTimeFormatter.ISO_DATE_TIME.format((LocalDateTime)dataFetcherResult);
                }
                throw Scalars.serializationException(dataFetcherResult, type);
            }

            @Override
            public String parseValue(Object input) {
                if (input instanceof StringValue) {
                    return ((StringValue)input).getValue();
                }
                return input.toString();
            }

            @Override
            public String parseLiteral(Object input) {
                if (input instanceof StringValue) {
                    return ((StringValue)input).getValue();
                }
                return input.toString();
            }
        });
    }
}
