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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.naming.NamingException;

import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.InjectionProvider;
import org.apache.myfaces.spi.InjectionProviderException;

/**
 * See SRV.14.5 Servlet Specification Version 2.5 JSR 154
 * and Common Annotations for the Java Platform JSR 250

 */

public class NoInjectionAnnotationInjectionProvider extends InjectionProvider
{
     /**
     * Cache the Method instances per ClassLoader using the Class-Name.
     * NOTE that we do it this way, because the only other valid way in order to support a shared
     * classloader scenario would be to use a WeakHashMap<Class<?>, Method[]>, but this
     * creates a cyclic reference between the key and the value of the WeakHashMap which will
     * most certainly cause a memory leak! Furthermore we can manually cleanup the Map when
     * the webapp is undeployed just by removing the Map for the current ClassLoader. 
     */
    private volatile static WeakHashMap<ClassLoader, Map<Class,Method[]> > declaredMethodBeans = 
            new WeakHashMap<ClassLoader, Map<Class, Method[]>>();

    private static Map<Class,Method[]> getDeclaredMethodBeansMap()
    {
        ClassLoader cl = ClassUtils.getContextClassLoader();
        
        Map<Class,Method[]> metadata = (Map<Class,Method[]>)
                declaredMethodBeans.get(cl);

        if (metadata == null)
        {
            // Ensure thread-safe put over _metadata, and only create one map
            // per classloader to hold metadata.
            synchronized (declaredMethodBeans)
            {
                metadata = createDeclaredMethodBeansMap(cl, metadata);
            }
        }

        return metadata;
    }
    
    private static Map<Class,Method[]> createDeclaredMethodBeansMap(
            ClassLoader cl, Map<Class,Method[]> metadata)
    {
        metadata = (Map<Class,Method[]>) declaredMethodBeans.get(cl);
        if (metadata == null)
        {
            metadata = new HashMap<Class,Method[]>();
            declaredMethodBeans.put(cl, metadata);
        }
        return metadata;
    }

    @Override
    public Object inject(Object instance) throws InjectionProviderException
    {
        try
        {
            processAnnotations(instance);
        }
        catch (IllegalAccessException ex)
        {
            throw new InjectionProviderException(ex);
        }
        catch (InvocationTargetException ex)
        {
            throw new InjectionProviderException(ex);
        }
        catch (NamingException ex)
        {
            throw new InjectionProviderException(ex);
        }
        return null;
    }
    
    
    Method[] getDeclaredMethods(Class clazz)
    {
        Map<Class,Method[]> declaredMethodBeansMap = getDeclaredMethodBeansMap();
        Method[] methods = declaredMethodBeansMap.get(clazz);
        if (methods == null)
        {
            methods = clazz.getDeclaredMethods();
            synchronized(declaredMethodBeansMap)
            {
                declaredMethodBeansMap.put(clazz, methods);
            }
        }
        return methods;
    }

    /**
     * Call postConstruct method on the specified instance.
     */
    @Override
    public void postConstruct(Object instance, Object creationMetaData) throws InjectionProviderException
    {
        // TODO the servlet spec is not clear about searching in superclass??
        Class clazz = instance.getClass();
        Method[] methods = getDeclaredMethods(clazz);
        if (methods == null)
        {
            methods = clazz.getDeclaredMethods();
            Map<Class,Method[]> declaredMethodBeansMap = getDeclaredMethodBeansMap();
            synchronized(declaredMethodBeansMap)
            {
                declaredMethodBeansMap.put(clazz, methods);
            }
        }
        Method postConstruct = null;
        for (int i = 0; i < methods.length; i++)
        {
            Method method = methods[i];
            if (method.isAnnotationPresent(PostConstruct.class))
            {
                // a method that does not take any arguments
                // the method must not be static
                // must not throw any checked expections
                // the return value must be void
                // the method may be public, protected, package private or private

                if ((postConstruct != null)
                        || (method.getParameterTypes().length != 0)
                        || (Modifier.isStatic(method.getModifiers()))
                        || (method.getExceptionTypes().length > 0)
                        || (!method.getReturnType().getName().equals("void")))
                {
                    throw new IllegalArgumentException("Invalid PostConstruct annotation");
                }
                postConstruct = method;
            }
        }
        try
        {
            invokeAnnotatedMethod(postConstruct, instance);
        }
        catch (IllegalAccessException ex)
        {
            throw new InjectionProviderException(ex);
        }
        catch (InvocationTargetException ex)
        {
            throw new InjectionProviderException(ex);
        }
    }

    @Override
    public void preDestroy(Object instance, Object creationMetaData) throws InjectionProviderException
    {

        // TODO the servlet spec is not clear about searching in superclass??
        // May be only check non private fields and methods
        Class clazz = instance.getClass();
        Method[] methods = getDeclaredMethods(clazz);
        Method preDestroy = null;
        for (int i = 0; i < methods.length; i++)
        {
            Method method = methods[i];
            if (method.isAnnotationPresent(PreDestroy.class))
            {
                // must not throw any checked expections
                // the method must not be static
                // must not throw any checked expections
                // the return value must be void
                // the method may be public, protected, package private or private

                if ((preDestroy != null)
                        || (method.getParameterTypes().length != 0)
                        || (Modifier.isStatic(method.getModifiers()))
                        || (method.getExceptionTypes().length > 0)
                        || (!method.getReturnType().getName().equals("void")))
                {
                    throw new IllegalArgumentException("Invalid PreDestroy annotation");
                }
                preDestroy = method;
            }
        }

        try
        {
            invokeAnnotatedMethod(preDestroy, instance);
        }
        catch (IllegalAccessException ex)
        {
            throw new InjectionProviderException(ex);
        }
        catch (InvocationTargetException ex)
        {
            throw new InjectionProviderException(ex);
        }
    }

    private void invokeAnnotatedMethod(Method method, Object instance)
                throws IllegalAccessException, InvocationTargetException
    {
        // At the end the annotated
        // method is invoked
        if (method != null)
        {
            boolean accessibility = method.isAccessible();
            method.setAccessible(true);
            method.invoke(instance);
            method.setAccessible(accessibility);
        }
    }

     /**
     * Inject resources in specified instance.
     */
    protected void processAnnotations(Object instance)
            throws IllegalAccessException, InvocationTargetException, NamingException
    {

    }
}
