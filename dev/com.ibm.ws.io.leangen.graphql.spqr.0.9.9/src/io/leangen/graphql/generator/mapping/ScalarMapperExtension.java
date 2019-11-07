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
package io.leangen.graphql.generator.mapping;

import graphql.schema.GraphQLScalarType;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Map;

/**
 * Used to update the scalar mappings - for example, to add a new custom scalar:
 * <pre>
 * public Map<Type, GraphQLScalarType> updateScalarMappings(Map<Type, GraphQLScalarType> scalarMapping) {
 *     scalarMapping.put(MyCustomType.class, new GraphQLScalarType("MyScalar", "My custom scalar", new Coercing() {

            public Object serialize(Object dataFetcherResult) {
                return convertResult(dataFetcherResult);
            }

            public Object parseValue(Object input) {
                return convertFromValue(input);
            }

            public T parseLiteral(Object input) {
                return convertFromValueLiteral(input);
            }
        });
 * }
 * </pre>
 */
public interface ScalarMapperExtension {

    Map<Type, GraphQLScalarType> updateScalarMappings(Map<Type, GraphQLScalarType> scalarMapping);
}
