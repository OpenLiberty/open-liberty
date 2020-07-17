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
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Produces({ "*/*" })
@Consumes({ "*/*" })
@Provider
public class JsonBProviderDelegator implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    private final boolean canLoadJsonbApis;
    private Object delegate;

    @FFDCIgnore(Throwable.class)
    public JsonBProviderDelegator() {
        boolean b = true;
        try {
            Class.forName("javax.json.bind.Jsonb");
        } catch (Throwable t) {
            b = false;
        }
        canLoadJsonbApis = b;
        
    }
    
    @Context
    public void setProviders(Providers providers) {
        delegate = canLoadJsonbApis ? new JsonBProvider(providers) : null;
    }

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return canLoadJsonbApis && ((JsonBProvider)delegate).isReadable(type, genericType, annotations, mediaType);
    }

    @Override
    public Object readFrom(Class<Object> clazz, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return ((JsonBProvider)delegate).readFrom(clazz, genericType, annotations, mediaType, httpHeaders, entityStream);
        
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return canLoadJsonbApis && ((JsonBProvider)delegate).isWriteable(type, genericType, annotations, mediaType);
    }

    @Override
    public void writeTo(Object obj, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        ((JsonBProvider)delegate).writeTo(obj, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }
}
