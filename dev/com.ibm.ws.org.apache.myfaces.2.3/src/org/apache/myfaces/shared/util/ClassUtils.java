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
package org.apache.myfaces.shared.util;

import javax.el.ExpressionFactory;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class ClassUtils
{
    //~ Static fields/initializers -----------------------------------------------------------------

    //private static final Log log                  = LogFactory.getLog(ClassUtils.class);
    private static final Logger log                  = Logger.getLogger(ClassUtils.class.getName());

    public static final Class BOOLEAN_ARRAY_CLASS = boolean[].class;
    public static final Class BYTE_ARRAY_CLASS    = byte[].class;
    public static final Class CHAR_ARRAY_CLASS    = char[].class;
    public static final Class SHORT_ARRAY_CLASS   = short[].class;
    public static final Class INT_ARRAY_CLASS     = int[].class;
    public static final Class LONG_ARRAY_CLASS    = long[].class;
    public static final Class FLOAT_ARRAY_CLASS   = float[].class;
    public static final Class DOUBLE_ARRAY_CLASS  = double[].class;
    public static final Class OBJECT_ARRAY_CLASS  = Object[].class;
    public static final Class BOOLEAN_OBJECT_ARRAY_CLASS = Boolean[].class;
    public static final Class BYTE_OBJECT_ARRAY_CLASS = Byte[].class;
    public static final Class CHARACTER_OBJECT_ARRAY_CLASS = Character[].class;
    public static final Class SHORT_OBJECT_ARRAY_CLASS = Short[].class;
    public static final Class INTEGER_OBJECT_ARRAY_CLASS = Integer[].class;
    public static final Class LONG_OBJECT_ARRAY_CLASS = Long[].class;
    public static final Class FLOAT_OBJECT_ARRAY_CLASS = Float[].class;
    public static final Class DOUBLE_OBJECT_ARRAY_CLASS = Double[].class;
    public static final Class STRING_OBJECT_ARRAY_CLASS = String[].class;

    public static ClassLoaderExtension [] classLoadingExtensions = new ClassLoaderExtension[0];



    public static final Map COMMON_TYPES = new HashMap(64);
    static
    {
        COMMON_TYPES.put("byte", Byte.TYPE);
        COMMON_TYPES.put("char", Character.TYPE);
        COMMON_TYPES.put("double", Double.TYPE);
        COMMON_TYPES.put("float", Float.TYPE);
        COMMON_TYPES.put("int", Integer.TYPE);
        COMMON_TYPES.put("long", Long.TYPE);
        COMMON_TYPES.put("short", Short.TYPE);
        COMMON_TYPES.put("boolean", Boolean.TYPE);
        COMMON_TYPES.put("void", Void.TYPE);
        COMMON_TYPES.put("java.lang.Object", Object.class);
        COMMON_TYPES.put("java.lang.Boolean", Boolean.class);
        COMMON_TYPES.put("java.lang.Byte", Byte.class);
        COMMON_TYPES.put("java.lang.Character", Character.class);
        COMMON_TYPES.put("java.lang.Short", Short.class);
        COMMON_TYPES.put("java.lang.Integer", Integer.class);
        COMMON_TYPES.put("java.lang.Long", Long.class);
        COMMON_TYPES.put("java.lang.Float", Float.class);
        COMMON_TYPES.put("java.lang.Double", Double.class);
        COMMON_TYPES.put("java.lang.String", String.class);

        COMMON_TYPES.put("byte[]", BYTE_ARRAY_CLASS);
        COMMON_TYPES.put("char[]", CHAR_ARRAY_CLASS);
        COMMON_TYPES.put("double[]", DOUBLE_ARRAY_CLASS);
        COMMON_TYPES.put("float[]", FLOAT_ARRAY_CLASS);
        COMMON_TYPES.put("int[]", INT_ARRAY_CLASS);
        COMMON_TYPES.put("long[]", LONG_ARRAY_CLASS);
        COMMON_TYPES.put("short[]", SHORT_ARRAY_CLASS);
        COMMON_TYPES.put("boolean[]", BOOLEAN_ARRAY_CLASS);
        COMMON_TYPES.put("java.lang.Object[]", OBJECT_ARRAY_CLASS);
        COMMON_TYPES.put("java.lang.Boolean[]", BOOLEAN_OBJECT_ARRAY_CLASS);
        COMMON_TYPES.put("java.lang.Byte[]", BYTE_OBJECT_ARRAY_CLASS);
        COMMON_TYPES.put("java.lang.Character[]", CHARACTER_OBJECT_ARRAY_CLASS);
        COMMON_TYPES.put("java.lang.Short[]", SHORT_OBJECT_ARRAY_CLASS);
        COMMON_TYPES.put("java.lang.Integer[]", INTEGER_OBJECT_ARRAY_CLASS);
        COMMON_TYPES.put("java.lang.Long[]", LONG_OBJECT_ARRAY_CLASS);
        COMMON_TYPES.put("java.lang.Float[]", FLOAT_OBJECT_ARRAY_CLASS);
        COMMON_TYPES.put("java.lang.Double[]", DOUBLE_OBJECT_ARRAY_CLASS);
        COMMON_TYPES.put("java.lang.String[]", STRING_OBJECT_ARRAY_CLASS);
        // array of void is not a valid type
    }

    /** utility class, do not instantiate */
    private ClassUtils()
    {
        // utility class, disable instantiation
    }

    //~ Methods ------------------------------------------------------------------------------------

    public synchronized static void addClassLoadingExtension(ClassLoaderExtension extension, boolean top)
    {
      /**
       * now at the first look this looks somewhat strange
       * but to get the best performance access we assign new native
       * arrays to our static variable
       * 
       * we have to synchronized nevertheless because if two threads try to register
       * loaders at the same time none of them should get lost
       */
        ClassLoaderExtension [] retVal = new ClassLoaderExtension[classLoadingExtensions.length+1];
        ArrayList extensions = new ArrayList(classLoadingExtensions.length+1);

        if(!top)
        {
            extensions.addAll(Arrays.asList(classLoadingExtensions));
        }
        extensions.add(extension);
        if(top)
        {
            extensions.addAll(Arrays.asList(classLoadingExtensions));
        }    

        classLoadingExtensions = (ClassLoaderExtension []) extensions.toArray(retVal);
    }

    /**
     * Tries a Class.loadClass with the context class loader of the current thread first and
     * automatically falls back to the ClassUtils class loader (i.e. the loader of the
     * myfaces.jar lib) if necessary.
     *
     * @param type fully qualified name of a non-primitive non-array class
     * @return the corresponding Class
     * @throws NullPointerException if type is null
     * @throws ClassNotFoundException
     */
    public static Class classForName(String type)
        throws ClassNotFoundException
    {
        //we now assign the array to safekeep the reference on
        // the local variable stack, that way
        //we can avoid synchronisation calls
        ClassLoaderExtension [] loaderPlugins = classLoadingExtensions;
        for (int cnt = 0; cnt < loaderPlugins.length; cnt ++)
        {
            ClassLoaderExtension extension = loaderPlugins[cnt];
            Class retVal = extension.forName(type);
            if(retVal != null)
            {
                return retVal;
            }
        }

        if (type == null)
        {
            throw new NullPointerException("type");
        }
        try
        {
            // Try WebApp ClassLoader first
            return Class.forName(type,
                                 false, // do not initialize for faster startup
                                 getContextClassLoader());
        }
        catch (ClassNotFoundException ignore)
        {
            // fallback: Try ClassLoader for ClassUtils (i.e. the myfaces.jar lib)
            return Class.forName(type,
                                 false, // do not initialize for faster startup
                                 ClassUtils.class.getClassLoader());
        }
    }

    /**
     * Same as {@link #classForName(String)}, but throws a RuntimeException
     * (FacesException) instead of a ClassNotFoundException.
     *
     * @return the corresponding Class
     * @throws NullPointerException if type is null
     * @throws FacesException if class not found
     */
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


    /**
     * Similar as {@link #classForName(String)}, but also supports primitive types
     * and arrays as specified for the JavaType element in the JavaServer Faces Config DTD.
     *
     * @param type fully qualified class name or name of a primitive type, both optionally
     *             followed by "[]" to indicate an array type
     * @return the corresponding Class
     * @throws NullPointerException if type is null
     * @throws ClassNotFoundException
     */
    public static Class javaTypeToClass(String type)
        throws ClassNotFoundException
    {
        if (type == null)
        {
            throw new NullPointerException("type");
        }

        // try common types and arrays of common types first
        Class clazz = (Class) COMMON_TYPES.get(type);
        if (clazz != null)
        {
            return clazz;
        }

        int len = type.length();
        if (len > 2 && type.charAt(len - 1) == ']' && type.charAt(len - 2) == '[')
        {
            String componentType = type.substring(0, len - 2);
            Class componentTypeClass = classForName(componentType);
            return Array.newInstance(componentTypeClass, 0).getClass();
        }

        return classForName(type);
        
    }

    /**
     * This method is similar to shared ClassUtils.javaTypeToClass,
     * but the default package for the type is java.lang
     *
     * @param type
     * @return
     * @throws ClassNotFoundException
     */
    public static Class javaDefaultTypeToClass(String type)
            throws ClassNotFoundException
    {
        if (type == null)
        {
            throw new NullPointerException("type");
        }

        // try common types and arrays of common types first
        Class clazz = (Class) ClassUtils.COMMON_TYPES.get(type);
        if (clazz != null)
        {
            return clazz;
        }

        int len = type.length();
        if (len > 2 && type.charAt(len - 1) == ']' && type.charAt(len - 2) == '[')
        {
            String componentType = type.substring(0, len - 2);
            Class componentTypeClass = ClassUtils.classForName(componentType);
            return Array.newInstance(componentTypeClass, 0).getClass();
        }

        if (type.indexOf('.') == -1)
        {
            type = "java.lang." + type;
        }
        return ClassUtils.classForName(type);
    }

    /**
     * Same as {@link #javaTypeToClass(String)}, but throws a RuntimeException
     * (FacesException) instead of a ClassNotFoundException.
     *
     * @return the corresponding Class
     * @throws NullPointerException if type is null
     * @throws FacesException if class not found
     */
    public static Class simpleJavaTypeToClass(String type)
    {
        try
        {
            return javaTypeToClass(type);
        }
        catch (ClassNotFoundException e)
        {
            log.log(Level.SEVERE, "Class " + type + " not found", e);
            throw new FacesException(e);
        }
    }

    public static URL getResource(String resource)
    {
        URL url = getContextClassLoader().getResource(resource);
        if (url == null)
        {
            url = ClassUtils.class.getClassLoader().getResource(resource);
        }
        return url;
    }

    public static InputStream getResourceAsStream(String resource)
    {
        InputStream stream = getContextClassLoader()
                                .getResourceAsStream(resource);
        if (stream == null)
        {
            // fallback
            stream = ClassUtils.class.getClassLoader().getResourceAsStream(resource);
        }
        return stream;
    }

    /**
     * @param resource       Name of resource(s) to find in classpath
     * @param defaultObject  The default object to use to determine the class loader 
     *                       (if none associated with current thread.)
     * @return Iterator over URL Objects
     */
    public static Iterator getResources(String resource, Object defaultObject)
    {
        try
        {
            Enumeration resources = getCurrentLoader(defaultObject).getResources(resource);
            List lst = new ArrayList();
            while (resources.hasMoreElements())
            {
                lst.add(resources.nextElement());
            }
            return lst.iterator();
        }
        catch (IOException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new FacesException(e);
        }
    }


    public static Object newInstance(String type)
        throws FacesException
    {
        if (type == null)
        {
            return null;
        }
        return newInstance(simpleClassForName(type));
    }

    public static Object newInstance(String type, Class expectedType) throws FacesException
    {
        return newInstance(type, expectedType == null ? null : new Class[] {expectedType});
    }

    public static Object newInstance(String type, Class[] expectedTypes)
    {
        if (type == null)
        {
            return null;
        }
        
        Class clazzForName = simpleClassForName(type);
        
        if(expectedTypes != null)
        {
            for (int i = 0, size = expectedTypes.length; i < size; i++)
            {
                if (!expectedTypes[i].isAssignableFrom(clazzForName))
                {
                    throw new FacesException("'" + type + "' does not implement expected type '" + expectedTypes[i]
                            + "'");
                }
            }
        }
        
        return newInstance(clazzForName);
    }

    public static <T> T newInstance(Class<T> clazz)
        throws FacesException
    {
        try
        {
            return clazz.newInstance();
        }
        catch(NoClassDefFoundError e)
        {
            log.log(Level.SEVERE, "Class : "+clazz.getName()+" not found.",e);
            throw new FacesException(e);
        }
        catch (InstantiationException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new FacesException(e);
        }
        catch (IllegalAccessException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new FacesException(e);
        }
    }

    public static <T> T newInstance(Class<T> clazz,
                                    Class<?>[] constructorArgClasses,
                                    Object... constructorArgs) throws NoSuchMethodException
    {
        if (constructorArgs.length == 0)
        {
            // no args given - use normal newInstance()
            return newInstance(clazz);
        }

        // try to get a fitting constructor (throws NoSuchMethodException)
        Constructor constructor = clazz.getConstructor(constructorArgClasses);

        try
        {
            // actually create instance
            return (T) constructor.newInstance(constructorArgs);
        }
        catch (Exception e)
        {
            throw new FacesException(e);
        }
    }

    public static Object convertToType(Object value, Class desiredClass)
    {
        if (value == null)
        {
            return null;
        }

        try
        {
            ExpressionFactory expFactory = FacesContext.getCurrentInstance().getApplication().getExpressionFactory();
            return expFactory.coerceToType(value, desiredClass);
        }
        catch (Exception e)
        {
            String message = "Cannot coerce " + value.getClass().getName()
                             + " to " + desiredClass.getName();
            log.log(Level.SEVERE, message, e);
            throw new FacesException(message, e);
        }
    }

    /**
     * Gets the ClassLoader associated with the current thread.  Returns the class loader associated with
     * the specified default object if no context loader is associated with the current thread.
     *
     * @param defaultObject The default object to use to determine the class loader 
     *        (if none associated with current thread.)
     * @return ClassLoader
     */
    protected static ClassLoader getCurrentLoader(Object defaultObject)
    {
        ClassLoader loader = getContextClassLoader();
        if(loader == null)
        {
            loader = defaultObject.getClass().getClassLoader();
        }
        return loader;
    }
    
    /**
     * Gets the ClassLoader associated with the current thread.  Includes a check for priviledges 
     * against java2 security to ensure no security related exceptions are encountered. 
     *
     * @since 3.0.6
     * @return ClassLoader
     */
    public static ClassLoader getContextClassLoader()
    {
        // call into the same method on ClassLoaderUtils.  no need for duplicate code maintenance. 
        return ClassLoaderUtils.getContextClassLoader();
    }
    
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
                current = (T) ClassUtils.newInstance(implClass);
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
                                    implClass.getConstructor(new Class[] {extendedInterfaceClass});
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
                                implClass.getConstructor(new Class[] {interfaceClass});
                    }
                    // impl class supports decorator pattern at this point
                    try
                    {
                        // create new decorator wrapping current
                        newCurrent = delegationConstructor.newInstance(new Object[] { current });
                    }
                    catch (InstantiationException e)
                    {
                        log.log(Level.SEVERE, e.getMessage(), e);
                        throw new FacesException(e);
                    }
                    catch (IllegalAccessException e)
                    {
                        log.log(Level.SEVERE, e.getMessage(), e);
                        throw new FacesException(e);
                    }
                    catch (InvocationTargetException e)
                    {
                        log.log(Level.SEVERE, e.getMessage(), e);
                        throw new FacesException(e);
                    }
                }
                catch (NoSuchMethodException e)
                {
                    // no decorator pattern support
                    newCurrent = (T) ClassUtils.newInstance(implClass);
                }
                
                // now we have a new current object (newCurrent)
                // --> find out if it is assignable from extendedInterfaceClass
                // and if not, wrap it in a backwards compatible wrapper (if available)
                if (extendedInterfaceWrapperClass != null
                        && !extendedInterfaceClass.isAssignableFrom(newCurrent.getClass()))
                {
                    try
                    {
                        Constructor<? extends T> wrapperConstructor
                                = extendedInterfaceWrapperClass.getConstructor(
                                        new Class[] {interfaceClass, extendedInterfaceClass});
                        newCurrent = wrapperConstructor.newInstance(new Object[] {newCurrent, current});
                    }
                    catch (NoSuchMethodException e)
                    {
                        log.log(Level.SEVERE, e.getMessage(), e);
                        throw new FacesException(e);
                    }
                    catch (InstantiationException e)
                    {
                        log.log(Level.SEVERE, e.getMessage(), e);
                        throw new FacesException(e);
                    }
                    catch (IllegalAccessException e)
                    {
                        log.log(Level.SEVERE, e.getMessage(), e);
                        throw new FacesException(e);
                    }
                    catch (InvocationTargetException e)
                    {
                        log.log(Level.SEVERE, e.getMessage(), e);
                        throw new FacesException(e);
                    }
                }
                
                current = newCurrent;
            }
        }

        return current;
    }
}
