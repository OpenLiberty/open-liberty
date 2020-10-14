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
package io.openliberty.restfulWS.providers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;


import javax.json.JsonException;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.spi.JsonProvider;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Produces({ "application/json", "application/*+json" })
@Consumes({ "application/json", "application/*+json" })
@Provider
public class JsonPProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

    JsonProvider jsonProvider = null;

    public JsonPProvider() {
        jsonProvider = AccessController.doPrivileged(new PrivilegedAction<JsonProvider>(){

            @Override
            public JsonProvider run() {
                try {
                Bundle b = FrameworkUtil.getBundle(JsonPProvider.class);
                if(b != null) {
                    BundleContext bc = b.getBundleContext();
                    ServiceReference<JsonProvider> sr = bc.getServiceReference(JsonProvider.class);
                    if (sr != null) {
                        return (JsonProvider)bc.getService(sr);
                    }
                }
                } catch (NoClassDefFoundError ncdfe) {
                    // ignore - return null
                }
                return JsonProvider.provider();
            }});
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonValue.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        if (entityStream == null) {
            throw new IOException("Initialized OutputStream should be provided");
        }

        JsonWriter writer = null;
        try {
            writer = this.jsonProvider.createWriter(entityStream);
            if (writer != null) {
                writer.write((JsonValue) obj);
            }
        } catch (Throwable e) {
            //ignore
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable e) {
                    //ignore
                }
            }
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonValue.class.isAssignableFrom(type);
    }

    @FFDCIgnore(value = { Throwable.class })
    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        if (entityStream == null) {
            throw new IOException("Initialized InputStream should be provided");
        }

        JsonReader reader = null;
        try {
            reader = this.jsonProvider.createReader(entityStream);
            if (reader != null) {
                return reader.read();
            }
        } catch (Throwable e) {
            if (JsonException.class.isAssignableFrom(e.getClass())) {
                throw new BadRequestException(e);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable e) {
                    //ignore
                }
            }
        }

        return null;
    }
}

