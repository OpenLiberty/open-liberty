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

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import javax.json.bind.annotation.JsonbProperty;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.leangen.graphql.metadata.Resolver;
import io.leangen.graphql.metadata.execution.Executable;
import io.leangen.graphql.metadata.execution.MethodInvoker;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.metadata.strategy.query.DefaultOperationBuilder;

import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;


/**
 * Overrides the default operation builder by allowing the operation name to be specified
 * using JSON-B's <code>{@literal @}JsonbProperty</code> annotation.
 */
public class MPOperationBuilder extends DefaultOperationBuilder {

    public MPOperationBuilder() {
        super(DefaultOperationBuilder.TypeInference.NONE);
    }
    @Override
    protected String resolveName(List<Resolver> resolvers) {
        Resolver resolver = resolvers.get(0);
        Executable exec = resolver.getExecutable();
        if (exec instanceof MethodInvoker) {
            Method method = (Method) exec.getDelegate();
            // first check for Query annotation
            Query queryAnno = method.getAnnotation(Query.class);
            if (queryAnno != null) {
                String value = queryAnno.value();
                if (!isEmpty(value)) {
                    return value;
                }
            }

            // if no Query annotation, next check for @Name
            Name nameAnno = method.getAnnotation(Name.class);
            if (nameAnno != null) {
                String value = nameAnno.value();
                if (!isEmpty(value)) {
                    return value;
                }
            }

            // still no name, try JsonbProperty annotation
            JsonbProperty jsonbPropAnno = method.getAnnotation(JsonbProperty.class);
            if (jsonbPropAnno == null) {
                Field field = getFieldFromGetter(method);
                jsonbPropAnno = field == null ? null : field.getAnnotation(JsonbProperty.class);
            }
            if (jsonbPropAnno != null) {
                String value = jsonbPropAnno.value();
                if (!isEmpty(value)) {
                    return value;
                }
            }
        }
        return resolvers.get(0).getOperationName();
    }

    @FFDCIgnore(Exception.class)
    private static Field getFieldFromGetter(Method method) {
        try {
            return method.getDeclaringClass().getDeclaredField(MethodUtils.getPropertyName(method));
        } catch (Exception ex) {
            return null;
        }
    }
    
    private static boolean isEmpty(String value) {
        return value == null || "".contentEquals(value);
    }
}
