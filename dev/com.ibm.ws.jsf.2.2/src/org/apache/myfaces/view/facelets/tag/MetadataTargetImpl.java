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
package org.apache.myfaces.view.facelets.tag;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.faces.view.facelets.MetadataTarget;

/**
 * 
 * @author Jacob Hookom
 * @version $Id: MetadataTargetImpl.java 1187701 2011-10-22 12:21:54Z bommel $
 */
public final class MetadataTargetImpl extends MetadataTarget
{
    private final Map<String, PropertyDescriptor> _pd;
    
    private final Class<?> _type;

    public MetadataTargetImpl(Class<?> type) throws IntrospectionException
    {
        _type = type;
        
        _pd = new HashMap<String, PropertyDescriptor>();
        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors())
        {
            _pd.put(descriptor.getName(), descriptor);
        }
    }

    public PropertyDescriptor getProperty(String name)
    {
        return _pd.get(name);
    }

    public Class<?> getPropertyType(String name)
    {
        PropertyDescriptor pd = getProperty(name);
        if (pd != null)
        {
            return pd.getPropertyType();
        }
        
        return null;
    }

    public Method getReadMethod(String name)
    {
        PropertyDescriptor pd = getProperty(name);
        if (pd != null)
        {
            return pd.getReadMethod();
        }
        
        return null;
    }

    public Class<?> getTargetClass()
    {
        return _type;
    }

    public Method getWriteMethod(String name)
    {
        PropertyDescriptor pd = getProperty(name);
        if (pd != null)
        {
            return pd.getWriteMethod();
        }
        
        return null;
    }

    public boolean isTargetInstanceOf(Class type)
    {
        return type.isAssignableFrom(_type);
    }
}
