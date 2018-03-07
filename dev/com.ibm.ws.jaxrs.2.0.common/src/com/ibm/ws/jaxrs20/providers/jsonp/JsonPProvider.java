/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.providers.jsonp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.utils.ExceptionUtils;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.component.LibertyJaxRsThreadPoolAdapter;
import com.ibm.ws.jaxrs20.utils.ReflectUtil;
import com.ibm.wsspi.classloading.ClassLoadingService;

@SuppressWarnings("rawtypes")
@Produces({ "application/json", "application/*+json" })
@Consumes({ "application/json", "application/*+json" })
@Provider
public class JsonPProvider implements MessageBodyReader, MessageBodyWriter {

    private final static String[] jsonpClasses = new String[] { "javax.json.Json", "javax.json.JsonArray", "javax.json.JsonException", "javax.json.JsonObject",
                                                               "javax.json.JsonReader",
                                                               "javax.json.JsonStructure", "javax.json.JsonWriter" };

    private final static Map<String, Class<?>> jsonpClsMaps = new HashMap<String, Class<?>>();
    private final static Map<String, Method> jsonpMethodMaps = new HashMap<String, Method>();

    static {
        try {
            final ClassLoadingService clSvc = LibertyJaxRsThreadPoolAdapter.getClassLoadingServiceref().getService();
            final ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

                @Override
                public ClassLoader run() {
                    return clSvc == null ? Thread.currentThread().getContextClassLoader() : clSvc.createThreadContextClassLoader(JsonPProvider.class.getClassLoader());
                }
            });

            loadClass(cl);

            if (clSvc != null) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {

                    @Override
                    public Void run() {
                        clSvc.destroyThreadContextClassLoader(cl);
                        return null;
                    }
                });
            }
        } catch (NoClassDefFoundError e) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            loadClass(cl);
        }
    }

    private static void loadClass(ClassLoader cl) {
        for (String clsName : jsonpClasses) {
            Class<?> c = ReflectUtil.loadClass(cl, clsName);
            if (c != null) {
                jsonpClsMaps.put(clsName, c);
            }
        }
    }

    @Override
    public long getSize(Object arg0, Class arg1, Type arg2, Annotation[] arg3, MediaType arg4) {

        return -1;
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {

        return isAssignableFrom(type, "javax.json.JsonStructure") ||
               isAssignableFrom(type, "javax.json.JsonArray") ||
               isAssignableFrom(type, "javax.json.JsonObject");
    }

    /**
     * @param type
     * @param isWriteable
     * @return
     */
    private boolean isAssignableFrom(Class type, String parentClass) {
        if (jsonpClsMaps.containsKey(parentClass)) {
            return jsonpClsMaps.get(parentClass).isAssignableFrom(type);
        }
        return false;
    }

    private Method getMethod(String className, String methodName, Class[] paramTypes) {

        if (!jsonpClsMaps.containsKey(className)) {
            return null;
        }

        Class<?> c = jsonpClsMaps.get(className);

        Method m = null;

        String cachekey = className + "." + methodName;

        if (jsonpMethodMaps.containsKey(cachekey)) {
            m = jsonpMethodMaps.get(cachekey);
        }
        else {
            m = ReflectUtil.getMethod(c, methodName, paramTypes);
            jsonpMethodMaps.put(cachekey, m);
        }

        return m;
    }

    @Override
    public void writeTo(Object t, Class type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        if (entityStream == null) {
            throw new IOException("Initialized OutputStream should be provided");
        }

        if (jsonpClsMaps.containsKey("javax.json.JsonWriter") && jsonpClsMaps.containsKey("javax.json.Json") && jsonpClsMaps.containsKey("javax.json.JsonStructure")) {

            Class<?> jsonStructureClass = jsonpClsMaps.get("javax.json.JsonStructure");

            Method m = getMethod("javax.json.Json", "createWriter", new Class[] { OutputStream.class });
            Method m2 = getMethod("javax.json.JsonWriter", "write", new Class[] { jsonStructureClass });
            Method m3 = getMethod("javax.json.JsonWriter", "close", null);

            if (m != null) {
                Object writer = null;
                try {
                    writer = ReflectUtil.invoke(m, null, new Object[] { entityStream });
                    if (writer != null) {
                        ReflectUtil.invoke(m2, writer, new Object[] { t });
                    }
                } catch (Throwable e) {
                    //ignore
                } finally {
                    if (writer != null) {
                        try {
                            ReflectUtil.invoke(m3, writer, null);
                        } catch (Throwable e) {
                            //ignore
                        }
                    }
                }
            }

        }
    }

    @Override
    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isAssignableFrom(type, "javax.json.JsonStructure") ||
               isAssignableFrom(type, "javax.json.JsonArray") ||
               isAssignableFrom(type, "javax.json.JsonObject");
    }

    @FFDCIgnore(value = { Throwable.class })
    @Override
    public Object readFrom(Class arg0, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        if (entityStream == null) {
            throw new IOException("Initialized InputStream should be provided");
        }

        if (jsonpClsMaps.containsKey("javax.json.JsonReader") && jsonpClsMaps.containsKey("javax.json.Json") && jsonpClsMaps.containsKey("javax.json.JsonException")) {

            Class<?> jsonException = jsonpClsMaps.get("javax.json.JsonException");

            Method m = getMethod("javax.json.Json", "createReader", new Class[] { InputStream.class });
            Method m2 = getMethod("javax.json.JsonReader", "read", null);
            Method m3 = getMethod("javax.json.JsonReader", "close", null);

            if (m != null) {
                Object reader = null;
                try {
                    reader = ReflectUtil.invoke(m, null, new Object[] { entityStream });
                    if (reader != null) {
                        return ReflectUtil.invoke(m2, reader, null);
                    }
                } catch (Throwable e) {

                    if (jsonException.isAssignableFrom(e.getClass())) {
                        throw ExceptionUtils.toBadRequestException(e, null);
                    }

                } finally {
                    if (reader != null) {
                        try {
                            ReflectUtil.invoke(m3, reader, null);
                        } catch (Throwable e) {
                            //ignore
                        }
                    }
                }
            }
        }

        return null;
    }
}
