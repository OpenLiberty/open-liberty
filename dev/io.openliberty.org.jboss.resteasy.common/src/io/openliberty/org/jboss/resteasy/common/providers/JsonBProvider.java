/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.providers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Produces({ "*/*" })
@Consumes({ "*/*" })
@Provider
public class JsonBProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object>, UnaryOperator<Jsonb> {

    private final static TraceComponent tc = Tr.register(JsonBProvider.class);
    private final static Charset DEFAULT_CHARSET = getDefaultCharset();
    private final JsonbProvider jsonbProvider;
    private final AtomicReference<Jsonb> jsonb = new AtomicReference<>();
    
    @Context
    protected Providers providers;
    
    @FFDCIgnore(Exception.class)
    private static Charset getDefaultCharset() {
        Charset cs = null;
        String csStr = null;
        try {
            csStr = AccessController.doPrivileged((PrivilegedAction<String>)() -> {
                return System.getProperty("com.ibm.ws.jaxrs.jsonbprovider.defaultCharset");
            });
            if (csStr != null) {
                cs = Charset.forName(csStr);
            }
        } catch (Exception ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not load specified default charset: " + csStr);
            }
        }
        if (cs == null) {
            cs = StandardCharsets.UTF_8;
        }
        return cs;
    }
    

    JsonBProvider(Providers providers) {

        this.providers = providers;

        JsonbProvider jsonbProvider = AccessController.doPrivileged(new PrivilegedAction<JsonbProvider>(){

            @Override
            public JsonbProvider run() {
                try {
                Bundle b = FrameworkUtil.getBundle(JsonBProvider.class);
                if(b != null) {
                    BundleContext bc = b.getBundleContext();
                    ServiceReference<JsonbProvider> sr = bc.getServiceReference(JsonbProvider.class);
                    return (JsonbProvider)bc.getService(sr);
                }
                } catch (NoClassDefFoundError ncdfe) {
                    // ignore - return null
                }
                return null;
            }});
        if(jsonbProvider == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "<init> provider not found in OSGi services - looking up via META-INF/services/"
                                + JsonbProvider.class.getName());
            }
            try {
                jsonbProvider = AccessController.doPrivileged(new PrivilegedExceptionAction<JsonbProvider>(){

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
            } catch (PrivilegedActionException ex) {
                Throwable t = ex.getCause();
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                throw new IllegalArgumentException(t);
            }
        }
        this.jsonbProvider = jsonbProvider;
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
        
        // For most generic return types, we want to use the genericType so as to ensure
        // that the generic value is not lost on conversion - specifically in collections.
        // But for CompletionStage<SomeType> we want to use clazz to pull the right value
        // - and then client code will handle the result, storing it in the CompletionStage.
        if ((genericType instanceof ParameterizedType) &&
            CompletionStage.class.equals( ((ParameterizedType)genericType).getRawType())) {
            genericType = ((ParameterizedType)genericType).getActualTypeArguments()[0];
        }
        Object obj = getJsonb(mediaType).fromJson(entityStream, genericType);

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
        String json = getJsonb(mediaType).toJson(obj);
        entityStream.write(json.getBytes(charset(httpHeaders))); // do not close entityStream

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "object=" + obj);
        }
    }

    private Jsonb getJsonb(MediaType mediaType) {
        ContextResolver<Jsonb> cr = providers.getContextResolver(Jsonb.class, mediaType);
        if (cr != null) {
            return cr.getContext(Jsonb.class);
        }
        /*
        for (ProviderInfo<ContextResolver<?>> crPi : contextResolvers) {
            ContextResolver<?> cr = crPi.getProvider();
            InjectionUtils.injectContexts(cr, crPi, JAXRSUtils.getCurrentMessage());
            Object o = cr.getContext(null);
            if (o instanceof Jsonb) {
                return (Jsonb) o;
            }
        }
        */

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Context-injected Providers is null");
        }
        if (jsonbProvider == null) {
            return null;
        }

        Jsonb json = jsonb.get();
        if (json == null) {
            return jsonb.updateAndGet(this);
        }

        return json;
    }

    /**
     * @see java.util.function.Function#apply(java.lang.Object)
     */
    @Override
    public Jsonb apply(Jsonb t) {
        if (t != null) {
            return t;
        }
        return jsonbProvider.create().build();
    }

    private static Charset charset(MultivaluedMap<String, Object> httpHeaders) {
        if (httpHeaders == null) {
            return DEFAULT_CHARSET;
        }
        List<?> charsets = httpHeaders.get(HttpHeaders.ACCEPT_CHARSET);
        return sortCharsets(charsets).stream()
                                     .findFirst()
                                     .orElseGet(() -> {
                                         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                             Tr.debug(tc, "No matching charsets, using " + DEFAULT_CHARSET.name() + 
                                                  ", client requested " + charsets);
                                         }
                                         return DEFAULT_CHARSET;
                                     });
    }
    
    public static List<Charset> sortCharsets(List<?> charsetHeaderValues) {
        if (charsetHeaderValues == null || charsetHeaderValues.size() < 1) {
            return Collections.emptyList();
        }
        return charsetHeaderValues.stream()
                                  .map(CharsetQualityTuple::parseTuple)
                                  .sorted((t1, t2) -> { return Float.compare(t1.quality, t2.quality) * -1; })
                                  .filter(t -> { return t.charset != null && t.quality > 0; })
                                  .map(t -> { return t.charset; })
                                  .collect(Collectors.toList());
    }

    private static class CharsetQualityTuple {
        Charset charset;
        float quality = 1; // aka weight

        @FFDCIgnore(IllegalCharsetNameException.class)
        static CharsetQualityTuple parseTuple(Object o) {
            String s;
            if (o instanceof String) {
                s = (String) o;
            } else {
                s = o.toString();
            }
            CharsetQualityTuple tuple = new CharsetQualityTuple();
            String[] sArr = s.split(";[qQ]=");
            if (sArr.length > 1) {
                try {
                    float f = Float.parseFloat(sArr[1]);
                    tuple.quality = Float.min(1.0f, Float.max(0f, f));
                } catch (NumberFormatException ex) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid charset weight (" + s + ") - defaulting to 0.");
                    }
                    tuple.quality = 0;
                }
            }
            try {
                if (Charset.isSupported(sArr[0])) {
                    tuple.charset = Charset.forName(sArr[0]);
                } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unsupported charset, " + sArr[0]);
                }
            } catch (IllegalCharsetNameException ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Illegal charset name, " + sArr[0]);
                }
            }
            return tuple;
        }
    }
}

