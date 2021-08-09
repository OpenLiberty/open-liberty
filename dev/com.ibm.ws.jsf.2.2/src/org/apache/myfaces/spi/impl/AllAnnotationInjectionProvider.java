/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.spi.impl;

import javax.naming.NamingException;
import javax.naming.Context;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;


import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;

// TODO @EJBs
public class AllAnnotationInjectionProvider extends ResourceAnnotationInjectionProvider
{

    public AllAnnotationInjectionProvider(Context context)
    {
        super(context);
    }

    @Override
    protected void checkMethodAnnotation(Method method, Object instance)
            throws NamingException, IllegalAccessException, InvocationTargetException
    {
        super.checkMethodAnnotation(method, instance);
        if (method.isAnnotationPresent(Resource.class))
        {
            Resource annotation =  method.getAnnotation(Resource.class);
            lookupMethodResource(context, instance, method, annotation.name());
        }
        if (method.isAnnotationPresent(EJB.class))
        {
            EJB annotation =  method.getAnnotation(EJB.class);
            lookupMethodResource(context, instance, method, annotation.name());
        }
        // TODO where i find WebServiceRef?
        /*if (method.isAnnotationPresent(WebServiceRef.class)) {
            WebServiceRef annotation =
                (WebServiceRef) method.getAnnotation(WebServiceRef.class);
            lookupMethodResource(context, instance, methods, annotation.name());
        }*/
        if (method.isAnnotationPresent(PersistenceContext.class))
        {
            PersistenceContext annotation = method.getAnnotation(PersistenceContext.class);
            lookupMethodResource(context, instance, method, annotation.name());
        }
        if (method.isAnnotationPresent(PersistenceUnit.class))
        {
            PersistenceUnit annotation = method.getAnnotation(PersistenceUnit.class);
            lookupMethodResource(context, instance, method, annotation.name());
        }
    }

    @Override
    protected void checkFieldAnnotation(Field field, Object instance)
            throws NamingException, IllegalAccessException
    {
        super.checkFieldAnnotation(field, instance);
        if (field.isAnnotationPresent(EJB.class))
        {
            EJB annotation = field.getAnnotation(EJB.class);
            lookupFieldResource(context, instance, field, annotation.name());
        }
        /*if (field.isAnnotationPresent(WebServiceRef.class)) {
            WebServiceRef annotation =
                (WebServiceRef) field.getAnnotation(WebServiceRef.class);
            lookupFieldResource(context, instance, field, annotation.name());
        }*/
        if (field.isAnnotationPresent(PersistenceContext.class))
        {
            PersistenceContext annotation = field.getAnnotation(PersistenceContext.class);
            lookupFieldResource(context, instance, field, annotation.name());
        }
        if (field.isAnnotationPresent(PersistenceUnit.class))
        {
            PersistenceUnit annotation = field.getAnnotation(PersistenceUnit.class);
            lookupFieldResource(context, instance, field, annotation.name());
        }
    }
}
