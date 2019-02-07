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
package com.ibm.ws.jaxrs.fat.providercache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.enterprise.context.Dependent;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Dependent
public class TestEntityMessageBodyReaderWriter
                implements MessageBodyReader<TestEntity>, MessageBodyWriter<TestEntity> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                              MediaType mediaType) {
        System.out.println("TestEntityMessageBodyReaderWriter#isReadable");
        return false;
    }

    @Override
    public TestEntity readFrom(Class<TestEntity> type, Type genericType, Annotation[] annotations,
                               MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                    throws IOException, WebApplicationException {
        return null;
    }

    @Override
    public long getSize(TestEntity arg0, Class<?> arg1, Type arg2, Annotation[] arg3,
                        MediaType arg4) {
        return 0;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        System.out.println("TestEntityMessageBodyReaderWriter#isWriteable");
        return false;
    }

    @Override
    public void writeTo(TestEntity arg0, Class<?> arg1, Type arg2, Annotation[] arg3,
                        MediaType arg4, MultivaluedMap<String, Object> arg5, OutputStream arg6)
                    throws IOException, WebApplicationException {}

}
