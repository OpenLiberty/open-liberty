/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.providers.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;

import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.model.ProviderInfo;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@Produces({ "*/*" })
@Consumes({ "*/*" })
@Provider
public class JsonBProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    private final static TraceComponent tc = Tr.register(JsonBProvider.class);
    private final Jsonb jsonb;
    private final Iterable<ProviderInfo<ContextResolver<?>>> contextResolvers;

    public JsonBProvider(JsonbProvider jsonbProvider, Iterable<ProviderInfo<ContextResolver<?>>> contextResolvers) {
        this.contextResolvers = contextResolvers;

        if(jsonbProvider != null) {
            this.jsonb = jsonbProvider.create().build();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "<init> called with null provider - looking up via META-INF/services/"
                                + JsonbProvider.class.getName());
            }
            try {
                JsonbProvider provider = AccessController.doPrivileged(new PrivilegedExceptionAction<JsonbProvider>(){

                    @Override
                    public JsonbProvider run() throws Exception {
                        // first try thread context classloader
                        Iterator<JsonbProvider> providers = ServiceLoader.load(JsonbProvider.class).iterator();
                        if (providers.hasNext()) {
                            return providers.next();
                        }
                        // next try this classloader
                        providers = ServiceLoader.load(JsonbProvider.class, JsonBProvider.class.getClassLoader()).iterator();
                        if (providers.hasNext()) {
                            return providers.next();
                        }

                        // not good - but maybe we're in a test environment where there will be no
                        // need for a JSON-B provider...
                        if (Boolean.getBoolean("com.ibm.ws.jaxrs.testing")) {
                            return null;
                        }

                        throw new IllegalArgumentException("jsonbProvider can't be null");
                    }});
                if (provider != null) {
                    this.jsonb = provider.create().build();
                } else {
                    this.jsonb = null;
                }
            } catch (PrivilegedActionException ex) {
                Throwable t = ex.getCause();
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                throw new IllegalArgumentException(t);
            }
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean readable = false;
        if (!isUntouchable(type) && isJsonType(mediaType)) {
            readable = true;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "readable=" + readable);
        }
        return readable;
    }

    @Override
    public Object readFrom(Class<Object> clazz, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        Object obj = null;
        // For most generic return types, we want to use the genericType so as to ensure
        // that the generic value is not lost on conversion - specifically in collections.
        // But for CompletionStage<SomeType> we want to use clazz to pull the right value
        // - and then client code will handle the result, storing it in the CompletionStage.
        if ((genericType instanceof ParameterizedType) &&
            CompletionStage.class.equals( ((ParameterizedType)genericType).getRawType())) {
            obj = getJsonb().fromJson(entityStream, clazz);
        } else {
            obj = getJsonb().fromJson(entityStream, genericType);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "object=" + obj);
        }
        return obj;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean writeable = false;
        if (!isUntouchable(type) && isJsonType(mediaType)) {
            writeable = true;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "writeable=" + writeable);
        }
        return writeable;
    }

    private boolean isUntouchable(Class<?> clazz) {
        final String[] jsonpClasses = new String[] { "javax.json.JsonArray", "javax.json.JsonObject", "javax.json.JsonStructure" };

        boolean untouchable = false;
        for (String c : jsonpClasses) {
            if(clazz.toString().equals(c)) {
                untouchable = true;
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "untouchable=" + untouchable);
        }
        return untouchable;
    }

    private boolean isJsonType(MediaType mediaType) {
        return mediaType.getSubtype().toLowerCase().startsWith("json")
                        || mediaType.getSubtype().toLowerCase().contains("+json");
    }

    @Override
    public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        getJsonb().toJson(obj, entityStream);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "object=" + obj);
        }
    }

    private Jsonb getJsonb() {
        for (ProviderInfo<ContextResolver<?>> crPi : contextResolvers) {
            ContextResolver<?> cr = crPi.getProvider();
            Object o = cr.getContext(null);
            if (o instanceof Jsonb) {
                return (Jsonb) o;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Context-injected Providers is null");
        }
        return this.jsonb;
    }
}
