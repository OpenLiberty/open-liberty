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
package com.ibm.ws.jaxrs.fat.jerseywithinjection;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.Resource;
import javax.enterprise.context.Dependent;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Dependent
public class TestEntityMessageBodyReader2 implements MessageBodyReader<TestEntity2> {

    @Resource(description = "Application Data Source", name = "jdbc/TestDataSource")
    private DataSource datasource;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                              MediaType mediaType) {
        System.out.println("TestEntityMessageBodyReader2(isReadable) - datasource " + datasource.toString());
        return type == TestEntity2.class;
    }

    @Override
    public TestEntity2 readFrom(Class<TestEntity2> type, Type genericType, Annotation[] annotations,
                               MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                    throws IOException, WebApplicationException {
        System.out.println("TestEntityMessageBodyReader2(readFrom)");
        TestEntity2 entity = new TestEntity2();
        return entity;
    }

}
