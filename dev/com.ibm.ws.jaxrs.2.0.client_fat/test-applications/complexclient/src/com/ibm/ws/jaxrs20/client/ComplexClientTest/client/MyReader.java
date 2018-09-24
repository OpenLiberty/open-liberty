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
package com.ibm.ws.jaxrs20.client.ComplexClientTest.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.ibm.ws.jaxrs20.client.ComplexClientTest.service.MyObject;

@Provider
@Consumes(MyObject.MIME_TYPE)
public class MyReader implements MessageBodyReader<MyObject> {

    @Override
    public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return MyObject.class.isAssignableFrom(type);
    }

    @Override
    public MyObject readFrom(Class<MyObject> type,
                             Type type1,
                             Annotation[] antns,
                             MediaType mt, MultivaluedMap<String, String> mm,
                             InputStream in) throws IOException, WebApplicationException {
        try {
            ObjectInputStream ois = new ObjectInputStream(in);
            return (MyObject) ois.readObject();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
