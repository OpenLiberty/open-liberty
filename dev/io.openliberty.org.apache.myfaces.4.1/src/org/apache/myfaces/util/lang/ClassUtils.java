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
package org.apache.myfaces.util.lang;

import jakarta.faces.FacesException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.myfaces.core.api.shared.lang.Assert;



public final class ClassUtils extends org.apache.myfaces.core.api.shared.lang.ClassUtils
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger log                  = Logger.getLogger(ClassUtils.class.getName());

    /**
     * Creates ApplicationObjects like NavigationHandler or StateManager and creates 
     * the right wrapping chain of the ApplicationObjects known as the decorator pattern. 
     * @param <T>
     * @param interfaceClass The class from which the implementation has to inherit from.
     * @param classNamesIterator All the class names of the actual ApplicationObject implementations
     *                           from the faces-config.xml.
     * @param defaultObject The default implementation for the given ApplicationObject.
     * @return
     */    
    public static <T> T buildApplicationObject(Class<T> interfaceClass, 
            Collection<String> classNamesIterator, T defaultObject)
    {
        return buildApplicationObject(interfaceClass, null, null, classNamesIterator, defaultObject);
    }

    /**
     * Creates ApplicationObjects like NavigationHandler or StateManager and creates 
     * the right wrapping chain of the ApplicationObjects known as the decorator pattern. 
     * @param <T>
     * @param interfaceClass The class from which the implementation has to inherit from.
     * @param extendedInterfaceClass A subclass of interfaceClass which specifies a more
     *                               detailed implementation.
     * @param extendedInterfaceWrapperClass A wrapper class for the case that you have an ApplicationObject
     *                                      which only implements the interfaceClass but not the 
     *                                      extendedInterfaceClass.
     * @param classNamesIterator All the class names of the actual ApplicationObject implementations
     *                           from the faces-config.xml.
     * @param defaultObject The default implementation for the given ApplicationObject.
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T buildApplicationObject(Class<T> interfaceClass, Class<? extends T> extendedInterfaceClass,
            Class<? extends T> extendedInterfaceWrapperClass,
            Collection<String> classNamesIterator, T defaultObject)
    {
        T current = defaultObject;
        

        for (String implClassName : classNamesIterator)
        {
            Class<? extends T> implClass = ClassUtils.simpleClassForName(implClassName);

            // check, if class is of expected interface type
            if (!interfaceClass.isAssignableFrom(implClass))
            {
                throw new IllegalArgumentException("Class " + implClassName + " is no " + interfaceClass.getName());
            }

            if (current == null)
            {
                // nothing to decorate
                current = ClassUtils.newInstance(implClass);
            }
            else
            {
                // let's check if class supports the decorator pattern
                T newCurrent = null;
                try
                {
                    Constructor<? extends T> delegationConstructor = null;
                    
                    // first, if there is a extendedInterfaceClass,
                    // try to find a constructor that uses that
                    if (extendedInterfaceClass != null 
                            && extendedInterfaceClass.isAssignableFrom(current.getClass()))
                    {
                        try
                        {
                            delegationConstructor = 
                                    implClass.getConstructor(extendedInterfaceClass);
                        }
                        catch (NoSuchMethodException mnfe)
                        {
                            // just eat it
                        }
                    }
                    if (delegationConstructor == null)
                    {
                        // try to find the constructor with the "normal" interfaceClass
                        delegationConstructor = 
                                implClass.getConstructor(interfaceClass);
                    }
                    // impl class supports decorator pattern at this point
                    try
                    {
                        // create new decorator wrapping current
                        newCurrent = delegationConstructor.newInstance(current);
                    }
                    catch (InstantiationException | IllegalAccessException | InvocationTargetException e)
                    {
                        log.log(Level.SEVERE, e.getMessage(), e);
                        throw new FacesException(e);
                    }
                }
                catch (NoSuchMethodException e)
                {
                    // no decorator pattern support
                    newCurrent = ClassUtils.newInstance(implClass);
                }
                
                current = wrapBackwardCompatible(interfaceClass, extendedInterfaceClass, 
                                                    extendedInterfaceWrapperClass, current, newCurrent);
            }
        }

        return current;
    }
    
    
    /**
     * Wrap an object using a backwards compatible wrapper if available
     * @param interfaceClass The class from which the implementation has to inherit from.
     * @param extendedInterfaceClass A subclass of interfaceClass which specifies a more
     *                               detailed implementation.
     * @param extendedInterfaceWrapperClass A wrapper class for the case that you have an ApplicationObject
     *                                      which only implements the interfaceClass but not the 
     *                                      extendedInterfaceClass.
     * @param defaultObject The default implementation for the given ApplicationObject.
     * @param newCurrent The new current object
     * @return
     */
    public static <T> T wrapBackwardCompatible(Class<T> interfaceClass, Class<? extends T> extendedInterfaceClass,
                                               Class<? extends T> extendedInterfaceWrapperClass, 
                                               T defaultObject, T newCurrent)
    {
        
        T current = newCurrent;
        
        // now we have a new current object (newCurrent)
        // --> find out if it is assignable from extendedInterfaceClass
        // and if not, wrap it in a backwards compatible wrapper (if available)
        if (extendedInterfaceWrapperClass != null
                && !extendedInterfaceClass.isAssignableFrom(current.getClass()))
        {
            try
            {
                Constructor<? extends T> wrapperConstructor
                        = extendedInterfaceWrapperClass.getConstructor(
                        interfaceClass, extendedInterfaceClass);
                current = wrapperConstructor.newInstance(newCurrent, defaultObject);
            }
            catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                    | InvocationTargetException e)
            {
                log.log(Level.SEVERE, e.getMessage(), e);
                throw new FacesException(e);
            }
        }
        
        return current;
    }

    /**
     * Tries a Class.loadClass with the context class loader of the current thread first and automatically falls back to
     * the ClassUtils class loader (i.e. the loader of the myfaces.jar lib) if necessary.
     *
     * @param type
     *            fully qualified name of a non-primitive non-array class
     * @return the corresponding Class
     * @throws NullPointerException
     *             if type is null
     * @throws ClassNotFoundException
     */
    // @Override MYFACES-4449: Methods that call ClassUtils.class.getClassLoader() need to be here
    //           as well as in the API ClassUtils so that the correct ClassLoader is used.
    public static <T> Class<T> classForName(String type) throws ClassNotFoundException
    {
        Assert.notNull(type, "type");

        try
        {
            // Try WebApp ClassLoader first
            return (Class<T>) Class.forName(type,
                    false, // do not initialize for faster startup
                    getContextClassLoader());
        }
        catch (ClassNotFoundException ignore)
        {
            // fallback: Try ClassLoader for ClassUtils (i.e. the myfaces.jar lib)
            return (Class<T>) Class.forName(type,
                    false, // do not initialize for faster startup
                    ClassUtils.class.getClassLoader());
        }
    }

    /**
     * Same as {@link #classForName(String)}, but throws a RuntimeException (FacesException) instead of a
     * ClassNotFoundException.
     *
     * @return the corresponding Class
     * @throws NullPointerException
     *             if type is null
     * @throws FacesException
     *             if class not found
     */
    // @Override MYFACES-4449: Methods that call ClassUtils.class.getClassLoader() need to be here
    //           as well as in the API ClassUtils so that the correct ClassLoader is used.
    public static Class simpleClassForName(String type)
    {
        return simpleClassForName(type, true);
    }

    /**
     * Same as {link {@link #simpleClassForName(String)}, but will only
     * log the exception and rethrow a RunTimeException if logException is true.
     *
     * @param type
     * @param logException - true to log/throw FacesException, false to avoid logging/throwing FacesException
     * @return the corresponding Class
     * @throws FacesException if class not found and logException is true
     */
    // @Override MYFACES-4449: Methods that call ClassUtils.class.getClassLoader() need to be here
    //           as well as in the API ClassUtils so that the correct ClassLoader is used.
    public static Class simpleClassForName(String type, boolean logException)
    {
        Class returnClass = null;
        try
        {
            returnClass = classForName(type);
        }
        catch (ClassNotFoundException e)
        {
            if (logException)
            {
                log.log(Level.SEVERE, "Class " + type + " not found", e);
                throw new FacesException(e);
            }
        }
        return returnClass;
    }

    // @Override MYFACES-4449: Methods that call ClassUtils.class.getClassLoader() need to be here
    //           as well as in the API ClassUtils so that the correct ClassLoader is used.
    public static URL getResource(String resource)
    {
        URL url = getContextClassLoader().getResource(resource);
        if (url == null)
        {
            url = ClassUtils.class.getClassLoader().getResource(resource);
        }
        return url;
    }

    // @Override MYFACES-4449: Methods that call ClassUtils.class.getClassLoader() need to be here
    //           as well as in the API ClassUtils so that the correct ClassLoader is used.
    public static InputStream getResourceAsStream(String resource)
    {
        InputStream stream = getContextClassLoader().getResourceAsStream(resource);
        if (stream == null)
        {
            // fallback
            stream = ClassUtils.class.getClassLoader().getResourceAsStream(resource);
        }
        return stream;
    }

    protected static final String paramString(Class<?>... types)
    {
        if (types != null)
        {
            StringBuilder sb = new StringBuilder();
            for (Class<?> type : types)
            {
                sb.append(type.getName()).append(", ");
            }
            
            if (sb.length() > 2)
            {
                sb.setLength(sb.length() - 2);
            }
            
            return sb.toString();
        }
        
        return null;
    }

}
