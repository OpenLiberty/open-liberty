/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.jsonb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.json.Json;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Provider
public class MyJsonBProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    private final Jsonb jsonb = JsonbBuilder.create();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean readable = false;
        if (isJsonType(mediaType)) {
            readable = true;
        }
        System.out.println("MyJsonBProvider:isReadable=" + readable);
        verifyJohnzon();
        return readable;
    }

    @Override
    public Object readFrom(Class<Object> clazz, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        Object obj = null;
        obj = this.jsonb.fromJson(entityStream, clazz);
        System.out.println("MyJsonBProvider:readFrom=" + obj);
        verifyJohnzon();
        return obj;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean writeable = false;
        if (isJsonType(mediaType)) {
            writeable = true;
        }
        System.out.println("MyJsonBProvider:isWriteable=" + writeable);
        verifyJohnzon();
        return writeable;
    }

    @Override
    public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        System.out.println("MyJsonBProvider:writeTo=" + obj);
        verifyJohnzon();
        this.jsonb.toJson(obj, entityStream);
    }

    private boolean isJsonType(MediaType mediaType) {
        return mediaType.getSubtype().toLowerCase().startsWith("json")
               || mediaType.getSubtype().toLowerCase().contains("+json");
    }

    private void verifyJohnzon() {
        String jsonpProvider = Json.createObjectBuilder().getClass().getCanonicalName();
        String jsonbProvider = jsonb.getClass().getCanonicalName();
        if (!jsonpProvider.contains("johnzon"))
            throw new IllegalStateException("Test error! Expected to be using user-defined JSON-P provider (Johnzon) but instead was: " + jsonpProvider);
        if (!jsonbProvider.contains("johnzon"))
            throw new IllegalStateException("Test error! Expected to be using user-defined JSON-B provider (Johnzon) but instead was: " + jsonbProvider);
    }
}
