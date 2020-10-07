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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.Resource;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
@Dependent
public class TestEntityMessageBodyWriter implements MessageBodyWriter<Object> {

    @Inject
    InjectableObject injectableObject;
    @Resource(description = "Application Data Source", name = "jdbc/TestDataSource")
    private DataSource datasource;
    @Context
    private Application application;
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                               MediaType mediaType) {
        if (injectableObject == null) {
            System.out.println("isWriteable NULL");
        } else {
            System.out.println("isWriteable "  + injectableObject.getSomething() + " " + datasource.toString() + " " + application.toString());
        }        
        return type == TestEntity.class;
    }

    @Override
    public long getSize(Object entity, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        System.out.println("getSize");
        return -1;
    }

    @Override
    public void writeTo(Object entity, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                    throws IOException, WebApplicationException {
        if (injectableObject == null) {
            System.out.println("writeTo NULL");
        } else {
            System.out.println("writeTo "  + injectableObject.getSomething() + " " + datasource.toString() + " " + application.toString());
        }
        
        if (entity == null) {
            System.out.println("writeTo entity is NULL");
        } else {
            System.out.println("writeTo entity is "  + ((TestEntity)entity).toString() );
        }        
    }
}
