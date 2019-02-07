/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.jackson1x;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;

@javax.ws.rs.ext.Provider
@javax.ws.rs.Consumes(value = { "application/json", "text/json" })
@javax.ws.rs.Produces(value = { "application/json", "text/json" })
public class ChildJacksonJaxbJsonProvider extends JacksonJaxbJsonProvider {

    @Override
    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        System.out.println("User's Jackson provider: " + this.getClass() + "->isReadable");
        return super.isReadable(arg0, arg1, arg2, arg3);
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        System.out.println("User's Jackson provider: " + this.getClass() + "->isWriteable");
        return super.isWriteable(arg0, arg1, arg2, arg3);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        System.out.println("User's Jackson provider: " + this.getClass() + "->readFrom");
        return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }

    @Override
    public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException {
        System.out.println("User's Jackson provider: " + this.getClass() + "->writeTo");
        super.writeTo(value, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }

}