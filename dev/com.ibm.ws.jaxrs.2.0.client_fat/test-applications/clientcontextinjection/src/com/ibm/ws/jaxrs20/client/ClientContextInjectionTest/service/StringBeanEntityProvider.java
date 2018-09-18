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
package com.ibm.ws.jaxrs20.client.ClientContextInjectionTest.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
public class StringBeanEntityProvider implements MessageBodyReader<StringBean>,
                MessageBodyWriter<StringBean> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
                               Annotation[] annotations, MediaType mediaType) {
        return StringBean.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(StringBean t, Class<?> type, Type genericType,
                        Annotation[] annotations, MediaType mediaType) {
        return t.get().length();
    }

    @Override
    public void writeTo(StringBean t, Class<?> type, Type genericType,
                        Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException,
                    WebApplicationException {
        entityStream.write(t.get().getBytes());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
                              Annotation[] annotations, MediaType mediaType) {
        return isWriteable(type, genericType, annotations, mediaType);
    }

    @Override
    public StringBean readFrom(Class<StringBean> type, Type genericType,
                               Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                    throws IOException, WebApplicationException {

        String stream = readFromStream(entityStream);
        StringBean bean = new StringBean(stream);
        return bean;
    }

    public static String readFromStream(InputStream in)
                    throws IOException {
        StringBuilder sb = new StringBuilder(1024);

        for (int i = in.read(); i != -1; i = in.read()) {
            sb.append((char) i);
        }

        in.close();

        return sb.toString();
    }
}
