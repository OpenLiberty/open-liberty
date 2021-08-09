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
package org.apache.myfaces.view.facelets.tag.composite;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.context.FacesContext;
import org.apache.myfaces.shared.util.ClassUtils;


/**
 * Like MetadataTargetImpl but integrate composite component bean info
 * with it.
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1525898 $ $Date: 2013-09-24 14:32:45 +0000 (Tue, 24 Sep 2013) $
 */
final class CompositeMetadataTargetImpl extends MetadataTarget
{
    private final Map<String, PropertyDescriptor> _pd;
    
    private final MetadataTarget _delegate;
    
    private final BeanInfo _beanInfo;

    public CompositeMetadataTargetImpl(MetadataTarget delegate, BeanInfo beanInfo) throws IntrospectionException
    {
        _delegate = delegate;
        _beanInfo = beanInfo;
        
        _pd = new HashMap<String, PropertyDescriptor>();
        
        for (PropertyDescriptor descriptor : _beanInfo.getPropertyDescriptors())
        {
            _pd.put(descriptor.getName(), descriptor);
        }
    }

    public PropertyDescriptor getProperty(String name)
    {
        PropertyDescriptor pd = _delegate.getProperty(name); 
        if (pd == null)
        {
            pd = _pd.get(name);
        }
        return pd;
    }

    public Class<?> getPropertyType(String name)
    {
        PropertyDescriptor pd = getProperty(name);
        if (pd != null)
        {
            Object type = pd.getValue("type");
            if (type != null)
            {
                type = ((ValueExpression)type).getValue(FacesContext.getCurrentInstance().getELContext());
                if (type instanceof String)
                {
                    try
                    {
                        type = ClassUtils.javaDefaultTypeToClass((String)type);
                    }
                    catch (ClassNotFoundException e)
                    {
                        type = Object.class;
                    }
                }
                return (Class<?>) type;
            }
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
        return _delegate.getTargetClass();
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
        return _delegate.isTargetInstanceOf(type);
    }
}
