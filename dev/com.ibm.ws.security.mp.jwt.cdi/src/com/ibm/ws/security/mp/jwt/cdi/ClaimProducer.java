/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.cdi;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class ClaimProducer {

    private static final TraceComponent tc = Tr.register(ClaimProducer.class);

    @Inject
    private JsonWebToken jsonWebToken;

    @Produces
    @Dependent
    @Claim
    public <T> ClaimValue<T> getClaimValue(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getClaimValue", injectionPoint);
        }

        String claimName = getClaimName(injectionPoint);
        boolean isOptional = false;
        Class<?> returnClass = null;
        Class<?> wrappedClass = null;
        Type beanType = injectionPoint.getType();

        if (beanType instanceof ParameterizedType) {
            ParameterizedType parameterizedBeanType = (ParameterizedType) beanType;
            returnClass = getTypeClass(parameterizedBeanType.getActualTypeArguments()[0]);
            isOptional = Optional.class.isAssignableFrom(returnClass);
            if (isOptional) {
                wrappedClass = getTypeClass(((ParameterizedType) parameterizedBeanType.getActualTypeArguments()[0]).getActualTypeArguments()[0]);
            }
        }

        final boolean isOptionalFinal = isOptional;
        final Class<?> returnClassFinal = returnClass;
        final Class<?> wrappedClassFinal = wrappedClass;
        ClaimValue<T> instance = new ClaimValue<T>() {

            @Override
            public String getName() {
                return claimName;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T getValue() {
                T value = null;
                if (isOptionalFinal) {
                    value = (T) Optional.ofNullable(getPlainValue(wrappedClassFinal, claimName));
                } else {
                    if (returnClassFinal != null && JsonValue.class.isAssignableFrom(returnClassFinal)) {
                        value = (T) getAsJsonValue(claimName);
                    } else {
                        value = jsonWebToken.getClaim(claimName);
                    }
                }
                return value;
            }

        };

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getClaimValue", instance);
        }
        return instance;
    }

    private String getClaimName(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getClaimName", injectionPoint);
        }

        Claim claim = getClaimAnnotation(injectionPoint);
        String claimName = claim.value();
        if (claimName == null || claimName.trim().isEmpty()) {
            claimName = claim.standard().name();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getClaimName", claimName);
        }
        return claimName;
    }

    private Claim getClaimAnnotation(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getClaimAnnotation", injectionPoint);
        }
        Claim claim = null;

        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        if (qualifiers != null) {
            for (Annotation qualifier : qualifiers) {
                if (qualifier.annotationType().equals(Claim.class)) {
                    claim = (Claim) qualifier;
                    break;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getClaimAnnotation", claim);
        }
        return claim;
    }

    private Class<?> getTypeClass(Type type) {
        if (type instanceof ParameterizedType) {
            type = ((ParameterizedType) type).getRawType();
        }
        return (Class<?>) type;
    }

    @SuppressWarnings("unchecked")
    private <U> U getPlainValue(Class<U> returnClass, String claimName) {
        U value = null;
        if (JsonValue.class.isAssignableFrom(returnClass)) {
            value = (U) getAsJsonValue(claimName);
        } else {
            value = (U) jsonWebToken.getClaim(claimName);
        }
        return value;
    }

    // TODO: Determine how to match JsonValue.TRUE/FALSE

    private JsonValue getAsJsonValue(String claimName) {
        JsonReader reader = Json.createReader(new StringReader(jsonWebToken.toString()));
        JsonObject jsonObject = reader.readObject();
        return jsonObject.get(claimName);
    }

    @Produces
    @Dependent
    @Claim
    public String getString(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getString", injectionPoint);
        }

        String instance = jsonWebToken.getClaim(getClaimName(injectionPoint));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getString", instance);
        }
        return instance;
    }

    @Produces
    @Dependent
    @Claim
    public Set<String> getSetString(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getSetString", injectionPoint);
        }

        Set<String> instance = jsonWebToken.getClaim(getClaimName(injectionPoint));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getSetString", instance);
        }
        return instance;
    }

    @Produces
    @Dependent
    @Claim
    public Long getLong(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getLong", injectionPoint);
        }

        Long instance = jsonWebToken.getClaim(getClaimName(injectionPoint));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getLong", instance);
        }
        return instance;
    }

    @Produces
    @Dependent
    @Claim
    public Boolean getBoolean(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getBoolean", injectionPoint);
        }

        Boolean instance = jsonWebToken.getClaim(getClaimName(injectionPoint));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getBoolean", instance);
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    @Produces
    @Dependent
    @Claim
    public <T> Optional<T> getOptional(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getOptional", injectionPoint);
        }

        ParameterizedType parameterizedBeanType = (ParameterizedType) injectionPoint.getType();
        final Class<?> wrappedClassFinal = getTypeClass((parameterizedBeanType.getActualTypeArguments()[0]));
        Optional<T> instance = (Optional<T>) Optional.ofNullable(getPlainValue(wrappedClassFinal, getClaimName(injectionPoint)));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getOptional", instance);
        }
        return instance;
    }

    // TODO: Determine how to inject a JsonValue.TRUE/FALSE
//    @Produces
//    @Dependent
//    @Claim
//    public JsonValue getJsonValue(InjectionPoint injectionPoint) {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            Tr.entry(tc, "getJsonValue", injectionPoint);
//        }
//
//        JsonNumber instance = (JsonNumber) getAsJsonValue(getClaimName(injectionPoint));
//
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//            Tr.exit(tc, "getJsonValue", instance);
//        }
//        return instance;
//    }

    @Produces
    @Dependent
    @Claim
    public JsonNumber getJsonNumber(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getJsonNumber", injectionPoint);
        }

        JsonNumber instance = (JsonNumber) getAsJsonValue(getClaimName(injectionPoint));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getJsonNumber", instance);
        }
        return instance;
    }

    @Produces
    @Dependent
    @Claim
    public JsonString getJsonString(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getJsonString", injectionPoint);
        }

        JsonString instance = (JsonString) getAsJsonValue(getClaimName(injectionPoint));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getJsonNumber", instance);
        }
        return instance;
    }

    // TODO: Confirm if this is a valid claim type
    @Produces
    @Dependent
    @Claim
    public JsonStructure getJsonStructure(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getJsonStructure", injectionPoint);
        }

        JsonStructure instance = (JsonStructure) getAsJsonValue(getClaimName(injectionPoint));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getJsonStructure", instance);
        }
        return instance;
    }

    @Produces
    @Dependent
    @Claim
    public JsonArray getJsonArray(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getJsonArray", injectionPoint);
        }

        JsonArray instance = (JsonArray) getAsJsonValue(getClaimName(injectionPoint));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getJsonArray", instance);
        }
        return instance;
    }

    @Produces
    @Dependent
    @Claim
    public JsonObject getJsonObject(InjectionPoint injectionPoint) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getJsonObject", injectionPoint);
        }

        JsonObject instance = (JsonObject) getAsJsonValue(getClaimName(injectionPoint));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getJsonObject", instance);
        }
        return instance;
    }

}
