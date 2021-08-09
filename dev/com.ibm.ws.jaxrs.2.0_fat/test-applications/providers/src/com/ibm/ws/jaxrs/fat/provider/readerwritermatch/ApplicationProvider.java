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
package com.ibm.ws.jaxrs.fat.provider.readerwritermatch;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
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
public class ApplicationProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

    @Override
    public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return Boolean.class.isAssignableFrom(type);
    }

    @Override
    public Boolean readFrom(Class<Object> type,
                            Type type1,
                            Annotation[] antns,
                            MediaType mt, MultivaluedMap<String, String> mm,
                            InputStream in) {

        return Boolean.TRUE;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return Boolean.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Object t, Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        // As of JAX-RS 2.0, the method has been deprecated and the
        // value returned by the method is ignored by a JAX-RS runtime.
        // All MessageBodyWriter implementations are advised to return -1 from
        // the method.

        return -1;
    }

    @Override
    public void writeTo(Object t,
                        Class<?> type,
                        Type type1,
                        Annotation[] antns,
                        MediaType mt,
                        MultivaluedMap<String, Object> mm,
                        OutputStream out) throws IOException, WebApplicationException {
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(t);
    }
}
