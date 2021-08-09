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
package com.ibm.websphere.jaxrs.providers.json4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import com.ibm.ws.jaxrs20.providers.json4j.utils.ProviderUtils;
import com.ibm.ws.jaxrs20.utils.ReflectUtil;
//import com.ibm.json.xml.XMLToJSONTransformer;
//import com.ibm.json.xml.XMLToJSONTransformer;

//import com.ibm.json.java.JSONObject;

@SuppressWarnings("rawtypes")
@Provider
@Consumes({ "application/json", "application/javascript" })
@Produces({ "application/json", "application/javascript" })
public class JSON4JJAXBProvider implements MessageBodyWriter {

    @Context
    private Providers providers;
    private volatile MessageBodyWriter bodyWriter;

    @Override
    public long getSize(Object t, Class type, Type genericType,
                        Annotation[] annotations, MediaType mediaType) {
        return -1L;
    }

    @Override
    public boolean isWriteable(Class type, Type genericType,
                               Annotation[] annotations, MediaType mediaType) {
        if ((!ProviderUtils.isJAXBObject(type, genericType))
            && (!ProviderUtils.isJAXBElement(type, genericType))) {
            return false;
        }
        if (this.bodyWriter == null) {
            Class<?> c = ProviderUtils.getJSON4JClass("com.ibm.json.java.JSONObject");
            if (c == null)
                return false;
            this.bodyWriter = this.providers.getMessageBodyWriter(c, c, annotations, mediaType);

            if (this.bodyWriter == null) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeTo(Object t, Class type, Type genericType,
                        Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap httpHeaders,
                        OutputStream entityStream) throws IOException,
                    WebApplicationException {
        MessageBodyWriter jaxbWriter = this.providers.getMessageBodyWriter(type, genericType, annotations, MediaType.APPLICATION_XML_TYPE);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        // mediaType =
        // MediaTypeUtils.setDefaultCharsetOnMediaTypeHeader(httpHeaders,
        // mediaType);
        jaxbWriter.writeTo(t, type, genericType, annotations, MediaType.APPLICATION_XML_TYPE, httpHeaders, os);
        try {

            Method m = ProviderUtils.getMethod("com.ibm.json.xml.XMLToJSONTransformer", "transform", new Class<?>[] { InputStream.class, OutputStream.class });

            ReflectUtil.invoke(m, null, new Object[] { new ByteArrayInputStream(os.toByteArray()), entityStream });

        } catch (Throwable e) {
            throw new WebApplicationException(e);
        }
    }
}