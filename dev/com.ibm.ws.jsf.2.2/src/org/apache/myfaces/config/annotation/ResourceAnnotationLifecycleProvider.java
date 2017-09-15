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
package org.apache.myfaces.config.annotation;

import javax.naming.NamingException;
import javax.naming.Context;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.myfaces.shared.util.ClassUtils;

// TODO @Resources
public class ResourceAnnotationLifecycleProvider extends NoInjectionAnnotationLifecycleProvider
{
    /**
     * Cache the Method instances per ClassLoader using the Class-Name.
     * NOTE that we do it this way, because the only other valid way in order to support a shared
     * classloader scenario would be to use a WeakHashMap<Class<?>, Method[]>, but this
     * creates a cyclic reference between the key and the value of the WeakHashMap which will
     * most certainly cause a memory leak! Furthermore we can manually cleanup the Map when
     * the webapp is undeployed just by removing the Map for the current ClassLoader. 
     */
    private volatile static WeakHashMap<ClassLoader, Map<Class,Field[]> > declaredFieldBeans = 
            new WeakHashMap<ClassLoader, Map<Class, Field[]>>();

    protected Context context;
    private static final String JAVA_COMP_ENV = "java:comp/env/";

    public ResourceAnnotationLifecycleProvider(Context context)
    {
        this.context = context;
    }

    private static Map<Class,Field[]> getDeclaredFieldBeansMap()
    {
        ClassLoader cl = ClassUtils.getContextClassLoader();
        
        Map<Class,Field[]> metadata = (Map<Class,Field[]>)
                declaredFieldBeans.get(cl);

        if (metadata == null)
        {
            // Ensure thread-safe put over _metadata, and only create one map
            // per classloader to hold metadata.
            synchronized (declaredFieldBeans)
            {
                metadata = createDeclaredFieldBeansMap(cl, metadata);
            }
        }

        return metadata;
    }
    
    private static Map<Class,Field[]> createDeclaredFieldBeansMap(
            ClassLoader cl, Map<Class,Field[]> metadata)
    {
        metadata = (Map<Class,Field[]>) declaredFieldBeans.get(cl);
        if (metadata == null)
        {
            metadata = new HashMap<Class,Field[]>();
            declaredFieldBeans.put(cl, metadata);
        }
        return metadata;
    }

    /**
     * Inject resources in specified instance.
     */
    @Override
    protected void processAnnotations(Object instance)
            throws IllegalAccessException, InvocationTargetException, NamingException
    {

        if (context == null)
        {
            // No resource injection
            return;
        }

        checkAnnotation(instance.getClass(), instance);

        /* 
         * May be only check non private fields and methods
         * for @Resource (JSR 250), if used all superclasses MUST be examined
         * to discover all uses of this annotation.
         */

        Class superclass = instance.getClass().getSuperclass();
        while (superclass != null && (!superclass.equals(Object.class)))
        {
            checkAnnotation(superclass, instance);
            superclass = superclass.getSuperclass();
        } 
    }
    
    Field[] getDeclaredFieldBeans(Class clazz)
    {
        Map<Class,Field[]> declaredFieldBeansMap = getDeclaredFieldBeansMap();
        Field[] fields = declaredFieldBeansMap.get(clazz);
        if (fields == null)
        {
            fields = clazz.getDeclaredFields();
            synchronized(declaredFieldBeansMap)
            {
                declaredFieldBeansMap.put(clazz, fields);
            }
        }
        return fields;
    }

    private void checkAnnotation(Class<?> clazz, Object instance)
            throws NamingException, IllegalAccessException, InvocationTargetException
    {
        // Initialize fields annotations
        Field[] fields = getDeclaredFieldBeans(clazz);
        for (int i = 0; i < fields.length; i++)
        {
            Field field = fields[i];
            checkFieldAnnotation(field, instance);
        }

        // Initialize methods annotations
        Method[] methods = getDeclaredMethods(clazz);
        for (int i = 0; i < methods.length; i++)
        {
            Method method = methods[i];
            checkMethodAnnotation(method, instance);
        }
    }

    protected void checkMethodAnnotation(Method method, Object instance)
            throws NamingException, IllegalAccessException, InvocationTargetException
    {
        if (method.isAnnotationPresent(Resource.class))
        {
            Resource annotation = method.getAnnotation(Resource.class);
            lookupMethodResource(context, instance, method, annotation.name());
        }
    }

    protected void checkFieldAnnotation(Field field, Object instance)
            throws NamingException, IllegalAccessException
    {
        if (field.isAnnotationPresent(Resource.class))
        {
            Resource annotation = field.getAnnotation(Resource.class);
            lookupFieldResource(context, instance, field, annotation.name());
        }
    }

    /**
     * Inject resources in specified field.
     */
    protected static void lookupFieldResource(javax.naming.Context context,
            Object instance, Field field, String name)
            throws NamingException, IllegalAccessException
    {

        Object lookedupResource;

        if ((name != null) && (name.length() > 0))
        {
            // TODO local or global JNDI
            lookedupResource = context.lookup(JAVA_COMP_ENV + name);
        }
        else
        {
            // TODO local or global JNDI 
            lookedupResource = context.lookup(JAVA_COMP_ENV + instance.getClass().getName() + "/" + field.getName());
        }

        boolean accessibility = field.isAccessible();
        field.setAccessible(true);
        field.set(instance, lookedupResource);
        field.setAccessible(accessibility);
    }


    /**
     * Inject resources in specified method.
     */
    protected static void lookupMethodResource(javax.naming.Context context,
            Object instance, Method method, String name)
            throws NamingException, IllegalAccessException, InvocationTargetException
    {

        if (!method.getName().startsWith("set")
                || method.getParameterTypes().length != 1
                || !method.getReturnType().getName().equals("void"))
        {
            throw new IllegalArgumentException("Invalid method resource injection annotation");
        }

        Object lookedupResource;

        if ((name != null) && (name.length() > 0))
        {
            // TODO local or global JNDI
            lookedupResource = context.lookup(JAVA_COMP_ENV + name);
        }
        else
        {
            // TODO local or global JNDI
            lookedupResource =
                    context.lookup(JAVA_COMP_ENV + instance.getClass().getName() + "/" + getFieldName(method));
        }

        boolean accessibility = method.isAccessible();
        method.setAccessible(true);
        method.invoke(instance, lookedupResource);
        method.setAccessible(accessibility);
    }

    /**
     * Returns the field name for the given Method.
     * E.g. setName() will be "name". 
     *
     * @param setter the setter method
     * @return the field name of the given setter method
     */
    protected static String getFieldName(Method setter)
    {
        StringBuilder name = new StringBuilder(setter.getName());

        // remove 'set'
        name.delete(0, 3);

        // lowercase first char
        name.setCharAt(0, Character.toLowerCase(name.charAt(0)));

        return name.toString();
    }

}
