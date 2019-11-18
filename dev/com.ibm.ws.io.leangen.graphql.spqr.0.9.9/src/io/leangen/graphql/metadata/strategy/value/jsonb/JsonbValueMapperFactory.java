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
package io.leangen.graphql.metadata.strategy.value.jsonb;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Map;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.value.ScalarDeserializationStrategy;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.metadata.strategy.value.ValueMapperFactory;

public class JsonbValueMapperFactory implements ValueMapperFactory, ScalarDeserializationStrategy {
    private static JsonbValueMapperFactory INSTANCE = new JsonbValueMapperFactory();

    private JsonbValueMapper mapper = new JsonbValueMapper();

    public static JsonbValueMapperFactory instance() {
        return INSTANCE;
    }

    private JsonbValueMapperFactory() {}

    @Override
    public ValueMapper getValueMapper(Map<Class, List<Class<?>>> concreteSubTypes, GlobalEnvironment environment) {
        return mapper;
    }
    
    public JsonbValueMapper getMapper() {
        return mapper;
    }

    @Override
    public boolean isDirectlyDeserializable(AnnotatedType type) {
        return false;
    }
}
