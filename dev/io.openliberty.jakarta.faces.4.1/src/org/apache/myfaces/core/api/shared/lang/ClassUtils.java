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
package org.apache.myfaces.core.api.shared.lang;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.el.ExpressionFactory;
import jakarta.faces.FacesException;
import jakarta.faces.context.FacesContext;

public class ClassUtils
{
    // ~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger log = Logger.getLogger(ClassUtils.class.getName());

    public static final Class<boolean[]> BOOLEAN_ARRAY_CLASS = boolean[].class;
    public static final Class<byte[]> BYTE_ARRAY_CLASS = byte[].class;
    public static final Class<char[]> CHAR_ARRAY_CLASS = char[].class;
    public static final Class<short[]> SHORT_ARRAY_CLASS = short[].class;
    public static final Class<int[]> INT_ARRAY_CLASS = int[].class;
    public static final Class<long[]> LONG_ARRAY_CLASS = long[].class;
    public static final Class<float[]> FLOAT_ARRAY_CLASS = float[].class;
    public static final Class<double[]> DOUBLE_ARRAY_CLASS = double[].class;
    public static final Class<Object[]> OBJECT_ARRAY_CLASS = Object[].class;
    public static final Class<Boolean[]> BOOLEAN_OBJECT_ARRAY_CLASS = Boolean[].class;
    public static final Class<Byte[]> BYTE_OBJECT_ARRAY_CLASS = Byte[].class;
    public static final Class<Character[]> CHARACTER_OBJECT_ARRAY_CLASS = Character[].class;
    public static final Class<Short[]> SHORT_OBJECT_ARRAY_CLASS = Short[].class;
    public static final Class<Integer[]> INTEGER_OBJECT_ARRAY_CLASS = Integer[].class;
    public static final Class<Long[]> LONG_OBJECT_ARRAY_CLASS = Long[].class;
    public static final Class<Float[]> FLOAT_OBJECT_ARRAY_CLASS = Float[].class;
    public static final Class<Double[]> DOUBLE_OBJECT_ARRAY_CLASS = Double[].class;
    public static final Class<String[]> STRING_OBJECT_ARRAY_CLASS = String[].class;

    protected static final String[] EMPTY_STRING = new String[0];

    protected static final String[] PRIMITIVE_NAMES = new String[] { "boolean", "byte", "char", "double", "float",
                                                                    "int", "long", "short", "void" };

    protected static final Class<?>[] PRIMITIVES = new Class[] { Boolean.TYPE, Byte.TYPE, Character.TYPE, Double.TYPE,
                                                                Float.TYPE, Integer.TYPE, Long.TYPE, Short.TYPE,
                                                                Void.TYPE };
    
    public static final Map<String, Class<?>> COMMON_TYPES = new HashMap<String, Class<?>>(64);
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
    public static Class simpleClassForName(String type)
    {
        return simpleClassForName(type, true);
    }

    /**
     * Same as {link {@link #simpleClassForName(String)}, but will only
     * log the exception and rethrow a RunTimeException if logAndThrowException is true.
     *
     * @param type
     * @param logAndThrowException - true to log and throw FacesException, false to avoid logging and throwing 
     *  the FacesException
     * @return the corresponding Class
     * @throws FacesException if class not found and logAndThrowException is true
     */
    public static Class simpleClassForName(String type, boolean logAndThrowException)
    {
        Class returnClass = null;
        try
        {
            returnClass = classForName(type);
        }
        catch (ClassNotFoundException e)
        {
            if (logAndThrowException)
            {
                log.log(Level.SEVERE, "Class " + type + " not found", e);
                throw new FacesException(e);
            }
        }
        return returnClass;
    }

    /**
     * Same as {link {@link #simpleClassForName(String)}, but accepts two booleans
     * One to log an exception and another to rethrow a FacesExceptions
     *
     * @param type
     * @param logException - true to log the ClassNotFoundException, false to avoid logging
     * @param throwException - true to throw a FacesException, false to avoid throwing a FacesException
     * @return the corresponding Class
     * @throws FacesException if class not found and throwException is true
     */
    public static Class simpleClassForName(String type, boolean throwException, boolean logException)
    {
        Class returnClass = null;
        try
        {
            returnClass = classForName(type);
        }
        catch (ClassNotFoundException e)
        {
            if(logException)
            {
                log.log(Level.SEVERE, "Class " + type + " not found", e);
            }
            if (throwException)
            {
                throw new FacesException(e);
            }
        }
        return returnClass;
    }


    /**
     * Similar as {@link #classForName(String)}, but also supports primitive types and arrays as specified for the
     * JavaType element in the JavaServer Faces Config DTD.
     * 
     * @param type
     *            fully qualified class name or name of a primitive type, both optionally followed by "[]" to indicate
     *            an array type
     * @return the corresponding Class
     * @throws NullPointerException
     *             if type is null
     * @throws ClassNotFoundException
     */
    public static Class<?> javaTypeToClass(String type) throws ClassNotFoundException
    {
        Assert.notNull(type, "type");

        // try common types and arrays of common types first
        Class<?> clazz = COMMON_TYPES.get(type);
        if (clazz != null)
        {
            return clazz;
        }

        int len = type.length();
        if (len > 2 && type.charAt(len - 1) == ']' && type.charAt(len - 2) == '[')
        {
            String componentType = type.substring(0, len - 2);
            Class<?> componentTypeClass = classForName(componentType);
            return Array.newInstance(componentTypeClass, 0).getClass();
        }

        return classForName(type);

    }

    /**
     * Same as {@link #javaTypeToClass(String)}, but throws a RuntimeException (FacesException) instead of a
     * ClassNotFoundException.
     * 
     * @return the corresponding Class
     * @throws NullPointerException
     *             if type is null
     * @throws FacesException
     *             if class not found
     */
    public static Class<?> simpleJavaTypeToClass(String type)
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
        Assert.notNull(type, "type");

        // try common types and arrays of common types first
        Class clazz = COMMON_TYPES.get(type);
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

        if (type.indexOf('.') == -1)
        {
            type = "java.lang." + type;
        }
        return classForName(type);
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
        InputStream stream = getContextClassLoader().getResourceAsStream(resource);
        if (stream == null)
        {
            // fallback
            stream = ClassUtils.class.getClassLoader().getResourceAsStream(resource);
        }
        return stream;
    }
    
    /**
     * @param resource
     *            Name of resource(s) to find in classpath
     * @param defaultObject
     *            The default object to use to determine the class loader (if none associated with current thread.)
     * @return Iterator over URL Objects
     */
    public static Collection<URL> getResources(String resource, Object defaultObject)
    {
        try
        {
            Enumeration<URL> resources = getCurrentLoader(defaultObject).getResources(resource);
            List<URL> lst = new ArrayList<>();
            while (resources.hasMoreElements())
            {
                lst.add(resources.nextElement());
            }
            return lst;
        }
        catch (IOException e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new FacesException(e);
        }
    }

    public static Object newInstance(String type) throws FacesException
    {
        if (type == null)
        {
            return null;
        }
        return newInstance(simpleClassForName(type));
    }

    public static Object newInstance(String type, Class<?> expectedType) throws FacesException
    {
        return newInstance(type, expectedType == null ? null : new Class[] { expectedType });
    }

    public static Object newInstance(String type, Class<?>[] expectedTypes)
    {
        if (type == null)
        {
            return null;
        }

        Class<?> clazzForName = simpleClassForName(type);

        if (expectedTypes != null)
        {
            for (int i = 0, size = expectedTypes.length; i < size; i++)
            {
                if (!expectedTypes[i].isAssignableFrom(clazzForName))
                {
                    throw new FacesException('\'' + type + "' does not implement expected type '" + expectedTypes[i]
                            + '\'');
                }
            }
        }

        return newInstance(clazzForName);
    }

    public static <T> T newInstance(Class<T> clazz) throws FacesException
    {
        try
        {
            return clazz.newInstance();
        }
        catch (NoClassDefFoundError | InstantiationException | IllegalAccessException e)
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
    
    public static Object convertToType(Object value, Class<?> desiredClass)
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
            String message = "Cannot coerce " + value.getClass().getName() + " to " + desiredClass.getName();
            log.log(Level.SEVERE, message, e);
            throw new FacesException(message, e);
        }
    }

    public static Object convertToTypeNoLogging(FacesContext facesContext, Object value, Class<?> desiredClass)
        throws Exception
    {
        if (value == null)
        {
            return null;
        }

        ExpressionFactory expFactory = facesContext.getApplication().getExpressionFactory();
        return expFactory.coerceToType(value, desiredClass);
    }    

    /**
     * Gets the ClassLoader associated with the current thread. Returns the class loader associated with the specified
     * default object if no context loader is associated with the current thread.
     * 
     * @param defaultObject
     *            The default object to use to determine the class loader (if none associated with current thread.)
     * @return ClassLoader
     */
    public static ClassLoader getCurrentLoader(Object defaultObject)
    {
        ClassLoader loader = getContextClassLoader();
        if (loader == null)
        {
            loader = defaultObject.getClass().getClassLoader();
        }
        return loader;
    }
    
    public static ClassLoader getCurrentLoader(Class<?> clazz)
    {
        ClassLoader loader = getContextClassLoader();
        if (loader == null && clazz != null)
        {
            loader = clazz.getClassLoader();
        }
        return loader;
    }
    
    /**
     * Gets the ClassLoader associated with the current thread. Returns the class loader associated with the specified
     * default object if no context loader is associated with the current thread.
     * 
     * @return ClassLoader
     */
    public static ClassLoader getContextClassLoader()
    {
        return Thread.currentThread().getContextClassLoader();
    }
    
    public static Class<?> forNamePrimitive(String name)
    {
        if (name.length() <= 8)
        {
            int p = Arrays.binarySearch(PRIMITIVE_NAMES, name);
            if (p >= 0)
            {
                return PRIMITIVES[p];
            }
        }
        
        return null;
    }
    
    /**
     * Converts an array of Class names to Class types
     * 
     * @param s
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?>[] toTypeArray(String[] s) throws ClassNotFoundException
    {
        if (s == null)
        {
            return null;
        }
        
        Class<?>[] c = new Class[s.length];
        for (int i = 0; i < s.length; i++)
        {
            c[i] = forName(s[i]);
        }
        
        return c;
    }
    
    /**
     * Converts an array of Class types to Class names
     * 
     * @param c
     * @return
     */
    public static String[] toTypeNameArray(Class<?>[] c)
    {
        if (c == null)
        {
            return null;
        }
        
        String[] s = new String[c.length];
        for (int i = 0; i < c.length; i++)
        {
            s[i] = c[i].getName();
        }
        
        return s;
    }
    
    public static Class<?> forName(String name) throws ClassNotFoundException
    {
        if (name == null || name.isEmpty())
        {
            return null;
        }
        
        Class<?> c = forNamePrimitive(name);
        if (c == null)
        {
            if (name.endsWith("[]"))
            {
                String nc = name.substring(0, name.length() - 2);
                //we should route through our shared forName, due to plugins and due to better classloader resolution
                c  = classForName(nc);
                c = Array.newInstance(c, 0).getClass();
            }
            else
            {
                c  = classForName(name);
            }
        }
        
        return c;
    }

}
