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
package com.ibm.ws.jaxrs.fat.jackson2x;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

@javax.ws.rs.ext.Provider
public class WrapJacksonProvider implements javax.ws.rs.ext.MessageBodyReader<Object>, javax.ws.rs.ext.MessageBodyWriter<Object> {

    private final JacksonJaxbJsonProvider p;

    public WrapJacksonProvider() {
        p = new JacksonJaxbJsonProvider();
    }

    @Override
    public long getSize(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {

        return p.getSize(arg0, arg1, arg2, arg3, arg4);
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {

        System.out.println("User's Jackson provider: " + this.getClass() + "->isWriteable");
        return p.isWriteable(arg0, arg1, arg2, arg3);
    }

    @Override
    public void writeTo(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4, MultivaluedMap<String, Object> arg5,
                        OutputStream arg6) throws IOException, WebApplicationException {
        System.out.println("User's Jackson provider: " + this.getClass() + "->writeTo");
        p.writeTo(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        System.out.println("User's Jackson provider: " + this.getClass() + "->isReadable");
        return p.isReadable(arg0, arg1, arg2, arg3);
    }

    @Override
    public Object readFrom(Class<Object> arg0, Type arg1, Annotation[] arg2, MediaType arg3, MultivaluedMap<String, String> arg4,
                           InputStream arg5) throws IOException, WebApplicationException {
        System.out.println("User's Jackson provider: " + this.getClass() + "->readFrom");
        return p.readFrom(arg0, arg1, arg2, arg3, arg4, arg5);
    }

}
