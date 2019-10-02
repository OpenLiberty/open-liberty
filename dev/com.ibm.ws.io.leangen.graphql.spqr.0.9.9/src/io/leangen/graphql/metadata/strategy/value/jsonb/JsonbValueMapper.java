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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbProperty;

import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.metadata.strategy.value.InputParsingException;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

import org.eclipse.microprofile.graphql.DefaultValue;

public class JsonbValueMapper implements ValueMapper, InputFieldBuilder {

    private static final Logger LOG = Logger.getLogger(JsonbValueMapper.class.getName());

    private Jsonb jsonb = JsonbBuilder.create();
    
    JsonbValueMapper() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromInput(Object graphQLInput, Type sourceType, AnnotatedType outputType) throws InputParsingException {
        
        Type outputTypeActual = outputType.getType();
        if (outputTypeActual.equals(sourceType)) {
            return (T) graphQLInput;
        }
        String json = null;
        try {
            json = jsonb.toJson(graphQLInput, sourceType);
            T t = jsonb.fromJson(json, outputTypeActual);
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("fromInput " + graphQLInput + " | " + sourceType + " | " + outputType + " -> " + t);
            }
            return t;
        } catch (Exception ex) {
            throw new InputParsingException(json != null ? json : graphQLInput, outputTypeActual, ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(String json, AnnotatedType annotatedType) throws InputParsingException {
        Type type = annotatedType.getType();
        if (json == null || String.class.equals(type)) {
            return (T) json;
        }
        
        try {
            T t = jsonb.fromJson(json, type);
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("fromString " + json + " | " + annotatedType + " -> " + t);
            }
            return t;
        } catch (Exception ex) {
            throw new InputParsingException(json, type, ex);
        }
    }

    @Override
    public String toString(Object output) {
        String s;
        if (output == null || output instanceof String) {
            s = (String) output;
        } else {
            s = jsonb.toJson(output);
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("toString " + output + " -> " + s);
        }
        return s; 
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }

    @Override
    public Set<InputField> getInputFields(InputFieldBuilderParams params) {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.entering(JsonbValueMapper.class.getName(), "getInputFields", params);
        }

        Set<InputField> inputFields = new HashSet<>();

        try {
            //NOTE: JSON-B does not currently have an introspection API, so for now, we're manually introspecting...
            Class<?> declaringClass = (Class<?>) params.getType().getType();
            BeanInfo beanInfo = Introspector.getBeanInfo(declaringClass);
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "getInputFields introspecting " + declaringClass + " found " + beanInfo.getPropertyDescriptors().length + " properties");
            }
            for (PropertyDescriptor propDesc : beanInfo.getPropertyDescriptors()) {
                String propName = propDesc.getName();
                Field field = getDeclaredField(declaringClass, propName);
                if (field == null) {
                    continue;
                }
                Method setterMethod = propDesc.getWriteMethod();
                AnnotatedType fieldType = field.getAnnotatedType();
                if (!params.getEnvironment().inclusionStrategy.includeInputField(declaringClass, setterMethod, fieldType)) {
                    continue;
                }

                String fieldName = propName;
                String fieldDesc = "";
                org.eclipse.microprofile.graphql.InputField inputFieldAnno = setterMethod.getAnnotation(org.eclipse.microprofile.graphql.InputField.class);
                if (inputFieldAnno == null) {
                    inputFieldAnno = field.getAnnotation(org.eclipse.microprofile.graphql.InputField.class);
                }
                if (inputFieldAnno != null) {
                    fieldName = useDefaultIfEmpty(inputFieldAnno.value(), fieldName);
                    fieldDesc = inputFieldAnno.description();
                } else {
                    // try JSON-B annotations
                    JsonbProperty jsonbPropAnno = setterMethod.getAnnotation(JsonbProperty.class);
                    if (jsonbPropAnno == null) {
                        jsonbPropAnno = field.getAnnotation(JsonbProperty.class);
                    }
                    if (jsonbPropAnno != null) {
                        fieldName = useDefaultIfEmpty(jsonbPropAnno.value(), fieldName);
                    }
                }

                DefaultValue defaultValueAnno = setterMethod.getAnnotation(DefaultValue.class);
                if (defaultValueAnno == null) {
                    defaultValueAnno = field.getAnnotation(DefaultValue.class);
                }
                if (defaultValueAnno == null) {
                    Method getterMethod = propDesc.getReadMethod();
                    if (getterMethod != null) {
                        defaultValueAnno = getterMethod.getAnnotation(DefaultValue.class);
                    }
                }
                Object defaultValue = defaultValueAnno == null ? null : defaultValueAnno.value();
                InputField inputField = new InputField(fieldName, fieldDesc, fieldType, null, defaultValue, setterMethod);
                inputFields.add(inputField);
            }
        } catch (Exception ex) {
            //Auto FFDC
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "getInputFields caught exception", ex);
            }
        }
        if (LOG.isLoggable(Level.FINER)) {
            LOG.exiting(JsonbValueMapper.class.getName(), "getInputFields", inputFields);
        }
        return inputFields;
    }
    
    private static Field getDeclaredField(Class<?> declaringClass, String fieldName) {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Field>) () -> {
                return declaringClass.getDeclaredField(fieldName);
                });
        } catch (PrivilegedActionException pae) {
            //Auto-FFDC
            return null;
        }
    }
    
    private static String useDefaultIfEmpty(String s, String defaultValue) {
        return (s == null || "".equals(s.trim())) ? defaultValue : s;
    }
}
