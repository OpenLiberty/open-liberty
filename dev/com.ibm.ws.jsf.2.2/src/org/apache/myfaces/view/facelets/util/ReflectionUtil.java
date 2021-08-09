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
package org.apache.myfaces.view.facelets.util;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.apache.myfaces.shared.util.ClassUtils;

public class ReflectionUtil
{
    protected static final String[] EMPTY_STRING = new String[0];

    protected static final String[] PRIMITIVE_NAMES = new String[] { "boolean", "byte", "char", "double", "float",
                                                                    "int", "long", "short", "void" };

    protected static final Class<?>[] PRIMITIVES = new Class[] { Boolean.TYPE, Byte.TYPE, Character.TYPE, Double.TYPE,
                                                                Float.TYPE, Integer.TYPE, Long.TYPE, Short.TYPE,
                                                                Void.TYPE };

    /**
     * 
     */
    private ReflectionUtil()
    {
        super();
    }

    public static Class<?> forName(String name) throws ClassNotFoundException
    {
        if (null == name || "".equals(name))
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
                c  = ClassUtils.classForName(nc);
                //old code left for double checking
                //c = Class.forName(nc, false, ClassUtils.getContextClassLoader());
                c = Array.newInstance(c, 0).getClass();
            }
            else
            {
                c  = ClassUtils.classForName(name);
                //old code left for double checking
                //c = Class.forName(name, false, ClassUtils.getContextClassLoader());
            }
        }
        
        return c;
    }

    protected static Class<?> forNamePrimitive(String name)
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
