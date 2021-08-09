/*******************************************************************************
 * Copyright (c) 2012,2021 IBM Corporation and others.
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
import java.lang.reflect.Type;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
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

@SuppressWarnings("rawtypes")
@Produces({ "application/json", "application/*+json" })
@Consumes({ "application/json", "application/*+json" })
@Provider
public class JsonPProvider implements MessageBodyReader, MessageBodyWriter {

    private static final JsonWriterFactory writerFactory = Json.createWriterFactory(null);

    private static final JsonReaderFactory readerFactory = Json.createReaderFactory(null);

    @Override
    public long getSize(Object obj, Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonStructure.class.isAssignableFrom(type) ||
               JsonArray.class.isAssignableFrom(type) ||
               JsonObject.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(Object obj, Class type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        if (entityStream == null) {
            throw new IOException("Initialized OutputStream should be provided");
        }

        JsonWriter writer = null;
        try {
            writer = writerFactory.createWriter(entityStream);
            if (writer != null) {
                writer.write((JsonStructure) obj);
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
    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonStructure.class.isAssignableFrom(type) ||
               JsonArray.class.isAssignableFrom(type) ||
               JsonObject.class.isAssignableFrom(type);
    }

    @FFDCIgnore(value = { Throwable.class })
    @Override
    public Object readFrom(Class type, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        if (entityStream == null) {
            throw new IOException("Initialized InputStream should be provided");
        }

        JsonReader reader = null;
        try {
            reader = readerFactory.createReader(entityStream);
            if (reader != null) {
                return reader.read();
            }
        } catch (Throwable e) {
            if (JsonException.class.isAssignableFrom(e.getClass())) {
                throw ExceptionUtils.toBadRequestException(e, null);
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
