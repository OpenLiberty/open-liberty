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
package com.ibm.ws.jaxrs20.fat.bookstore;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.provider.JAXBElementProvider;

public class GenericHandlerWriter implements MessageBodyWriter<GenericHandler<Book>> {
    JAXBElementProvider<Object> jaxb = new JAXBElementProvider<Object>();

    public GenericHandlerWriter() {

    }

    @Override
    public long getSize(GenericHandler<Book> t, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
                               Annotation[] annotations, MediaType mediaType) {
        return type == GenericHandler.class && InjectionUtils.getActualType(genericType) == Book.class;
    }

    @Override
    public void writeTo(GenericHandler<Book> o, Class<?> c, Type t, Annotation[] anns, MediaType m,
                        MultivaluedMap<String, Object> headers, OutputStream os)
                    throws IOException, WebApplicationException {
        jaxb.writeTo(o.getEntity(), o.getEntity().getClass(), InjectionUtils.getActualType(t),
                     anns, m, headers, os);
    }

}
