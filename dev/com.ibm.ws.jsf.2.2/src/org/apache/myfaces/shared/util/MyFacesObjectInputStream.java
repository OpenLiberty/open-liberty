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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

/**
 * Tried to deploy v0.4.2 on JBoss 3.2.1 and had a classloading problem again.
 * The problem seemed to be with JspInfo, line 98. We are using an
 * ObjectInputStream Class, which then cannot find the classes to deserialize
 * the input stream.  The solution appears to be to subclass ObjectInputStream
 * (eg. CustomInputStream), and specify a different class-loading mechanism.
 */
public class MyFacesObjectInputStream
    extends ObjectInputStream
{
    public MyFacesObjectInputStream(InputStream in) throws IOException
    {
        super(in);
    }

    protected Class resolveClass(ObjectStreamClass desc)
        throws ClassNotFoundException, IOException
    {
        try
        {
            return ClassUtils.classForName(desc.getName());
        }
        catch (ClassNotFoundException e)
        {
            return super.resolveClass(desc);
        }
    }

    protected Class resolveProxyClass(String[] interfaces) 
            throws IOException, ClassNotFoundException
    {
        // Only option that would match the current code would be to
        // expand ClassLoaderExtension to handle 'getProxyClass', which
        // would break all existing ClassLoaderExtension implementations
        Class[] cinterfaces = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++)
        {
            cinterfaces[i] = ClassUtils.classForName(interfaces[i]);
        }

        try
        {
            // Try WebApp ClassLoader first
            return Proxy.getProxyClass(ClassUtils.getContextClassLoader(), cinterfaces);
        }
        catch (Exception ex)
        {
            // fallback: Try ClassLoader for MyFacesObjectInputStream (i.e. the myfaces.jar lib)
            try
            {
                return Proxy.getProxyClass(
                        MyFacesObjectInputStream.class.getClassLoader(), cinterfaces);
            }
            catch (IllegalArgumentException e)
            {
                throw new ClassNotFoundException(e.toString(), e);
            }
        }
    }
}
