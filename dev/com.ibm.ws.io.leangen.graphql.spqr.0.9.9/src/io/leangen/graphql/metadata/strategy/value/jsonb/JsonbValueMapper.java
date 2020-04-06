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
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.config.PropertyNamingStrategy;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.metadata.strategy.value.InputParsingException;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Name;

public class JsonbValueMapper implements ValueMapper, InputFieldBuilder {

    private static final Logger LOG = Logger.getLogger(JsonbValueMapper.class.getName());

    private final Map<Type, Jsonb> inputFieldsJsonbMap = new ConcurrentHashMap<>();
    private final Jsonb jsonb = JsonbBuilder.create();
    
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
            T t = getJsonb(outputTypeActual).fromJson(json, outputTypeActual);
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
            T t = getJsonb(type).fromJson(json, type);
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
            AnnotatedType declaringClassType = params.getType();
            Class<?> declaringClass = (Class<?>) declaringClassType.getType();
            BeanInfo beanInfo = Introspector.getBeanInfo(declaringClass);
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "getInputFields introspecting " + declaringClass + " found " + beanInfo.getPropertyDescriptors().length + " properties");
            }
            for (PropertyDescriptor propDesc : beanInfo.getPropertyDescriptors()) {
                String propName = propDesc.getName();
                Method setterMethod = propDesc.getWriteMethod();
                Field field = getDeclaredField(declaringClass, propName);
                if (field == null && setterMethod == null) {
                    continue;
                }

                AnnotatedType fieldType = field != null ? GenericTypeReflector.getExactFieldType(field, declaringClassType) : 
                                                          GenericTypeReflector.getExactParameterTypes(setterMethod, declaringClassType)[0];

                // no need to proceed if the field or setter has an @Ignore annotation on it:
                if ((field != null && !params.getEnvironment().inclusionStrategy.includeInputField(declaringClass, field, fieldType)) ||
                    (setterMethod != null && !params.getEnvironment().inclusionStrategy.includeInputField(declaringClass, setterMethod, fieldType))) {
                    continue;
                }

                String fieldName = propName;
                Name schemaNameAnno = null;
                if (setterMethod != null) {
                    schemaNameAnno = setterMethod.getAnnotation(Name.class);
                }
                if (schemaNameAnno == null && field != null) {
                    schemaNameAnno = field.getAnnotation(Name.class);
                }
                if (schemaNameAnno != null) {
                    fieldName = useDefaultIfEmpty(schemaNameAnno.value(), fieldName);
                } else {
                    // try JSON-B annotations
                    JsonbProperty jsonbPropAnno = null;
                    if (setterMethod != null) {
                        jsonbPropAnno = setterMethod.getAnnotation(JsonbProperty.class);
                    }
                    if (jsonbPropAnno == null && field != null) {
                        jsonbPropAnno = field.getAnnotation(JsonbProperty.class);
                    }
                    if (jsonbPropAnno != null) {
                        fieldName = useDefaultIfEmpty(jsonbPropAnno.value(), fieldName);
                    }
                }

                String fieldDesc = "";
                Description descAnno = null;
                if (setterMethod != null) {
                    descAnno = setterMethod.getAnnotation(Description.class);
                }
                if (descAnno == null && field != null) {
                    descAnno = field.getAnnotation(Description.class);
                }
                if (descAnno != null) {
                    fieldDesc = descAnno.value();
                }
    
                // if no description is provided by other annotations, check @JsonbDateFormat and use that as the description
                if ("".equals(fieldDesc)) {
                    JsonbDateFormat jsonbDateFormatAnno = setterMethod.getAnnotation(JsonbDateFormat.class);
                    if (jsonbDateFormatAnno == null) {
                        jsonbDateFormatAnno = field.getAnnotation(JsonbDateFormat.class);
                    }
                    if (jsonbDateFormatAnno != null) {
                        fieldDesc = useDefaultIfEmpty(jsonbDateFormatAnno.value(), getDefaultDateDescriptionFor(fieldType));
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
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, "adding field {0} with description {1}", new Object[] {inputField, fieldDesc});
                }
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

    private Jsonb getJsonb(Type type) {
        return inputFieldsJsonbMap.computeIfAbsent(type, t -> {
            return JsonbBuilder.create(new JsonbConfig()
                                       .withPropertyNamingStrategy(new InputFieldPropertyNamingStrategy(t)));
        });
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

    public static String getDefaultDateDescriptionFor(AnnotatedType annoType) {
        Type t = annoType.getType();
        if (t instanceof Class) {
            Class<?> cls = (Class<?>) t;
            if (LocalDate.class.isAssignableFrom(cls)) {
                return "yyyy-MM-dd";
            }
            if (LocalTime.class.isAssignableFrom(cls)) {
                return "HH:mm:ss";
            }
            if (LocalDateTime.class.isAssignableFrom(cls)) {
                return "yyyy-MM-dd'T'HH:mm:ss'Z'";
            }
        }
        return "";
    }

    private static class InputFieldPropertyNamingStrategy implements PropertyNamingStrategy {

        final Map<String, String> propertyMap = new ConcurrentHashMap<>();

        InputFieldPropertyNamingStrategy(Type type) {
            Class<?> cls = ClassUtils.getRawType(type);
            Set<Method> setters = new HashSet<>();
            Collections.addAll(setters, cls.getMethods());
            setters.stream()
                   .filter(m -> m.getName().startsWith("set"))
                   .forEach(m -> {
                       String propName = ClassUtils.getFieldNameFromSetter(m);
                       if (LOG.isLoggable(Level.FINEST)) {
                           LOG.finest("<init> checking " + propName);
                       }
                       if (addMapping(propName, m.getAnnotation(Name.class))) {
                           return;
                       }
                       ClassUtils.findFieldBySetter(m).ifPresent(f -> {
                           addMapping(propName, f.getAnnotation(Name.class));
                       });
                   });
        }

        private boolean addMapping(String propName, Name schemaNameAnno) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("addMapping " + propName + " -> " + (schemaNameAnno == null ? "null" : schemaNameAnno.value()));
            }
            if (schemaNameAnno != null) {
                propertyMap.put(propName, schemaNameAnno.value());
                return true;
            }
            return false;
        }

        @Override
        public String translateName(String propertyName) {
            String translated = propertyMap.getOrDefault(propertyName, propertyName);
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("translateName " + propertyName + " -> " + translated);
            }
            return translated;
        }
    }
}
