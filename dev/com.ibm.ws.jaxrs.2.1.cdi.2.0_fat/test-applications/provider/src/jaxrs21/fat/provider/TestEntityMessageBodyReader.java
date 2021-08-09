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
package jaxrs21.fat.provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.Resource;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Dependent
public class TestEntityMessageBodyReader implements MessageBodyReader<TestEntity> {
    
    @Inject
    InjectableObject injectableObject;
    @Resource(description = "Application Data Source", name = "jdbc/TestDataSource")
    private DataSource datasource;
    @Context
    private Application application;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                              MediaType mediaType) {
        if (injectableObject == null) {
            System.out.println("isReadable NULL");
        } else {
            System.out.println("isReadable " + injectableObject.getSomething() + " " + datasource.toString() + " " + application.toString());
        }
        
        return type == TestEntity.class;
    }

    @Override
    public TestEntity readFrom(Class<TestEntity> type, Type genericType, Annotation[] annotations,
                               MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                    throws IOException, WebApplicationException {
        if (injectableObject == null) {
            System.out.println("readFrom NULL");
        } else {
            System.out.println("readFrom "+ injectableObject.getSomething() + " " + datasource.toString() + " " + application.toString());
        }
        
        TestEntity entity =  new TestEntity();
        entity.setData1("data1");
        entity.setData2(1);
        entity.setData3(true);
     
        System.out.println("readFrom entity: " + entity.toString());
        return entity;
    }
}
