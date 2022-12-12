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
package org.apache.myfaces.el.resolver;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.el.BeanELResolver;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.PropertyNotWritableException;
import jakarta.faces.context.FacesContext;
import org.apache.myfaces.core.api.shared.lang.LambdaPropertyDescriptor;
import org.apache.myfaces.core.api.shared.lang.PropertyDescriptorUtils;
import org.apache.myfaces.core.api.shared.lang.PropertyDescriptorWrapper;

public class LambdaBeanELResolver extends BeanELResolver
{
    private final ConcurrentHashMap<String, Map<String, ? extends PropertyDescriptorWrapper>> cache;

    public LambdaBeanELResolver()
    {
        this.cache = new ConcurrentHashMap<>(1000);
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property)
    {
        Objects.requireNonNull(context);
        if (base == null || property == null)
        {
            return null;
        }

        context.setPropertyResolved(base, property);

        return getPropertyDescriptor(base, property).getPropertyType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getValue(ELContext context, Object base, Object property)
    {        
        Objects.requireNonNull(context);
        if (base == null || property == null)
        {
            return null;
        }

        context.setPropertyResolved(base, property);

        try
        {
            PropertyDescriptorWrapper pd = getPropertyDescriptor(base, property);
            if (pd instanceof LambdaPropertyDescriptor)
            {
                return ((LambdaPropertyDescriptor) pd).getReadFunction().apply(base);
            }

            return pd.getWrapped().getReadMethod().invoke(base);
        }
        catch (Exception e)
        {
            throw new ELException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setValue(ELContext context, Object base, Object property, Object value)
    {
        Objects.requireNonNull(context);
        if (base == null || property == null)
        {
            return;
        }

        context.setPropertyResolved(base, property);

        PropertyDescriptorWrapper pd = getPropertyDescriptor(base, property);
        if (pd.getWrapped().getWriteMethod()== null)
        {
            throw new PropertyNotWritableException("Property \"" + (String) property
                    + "\" in \"" + base.getClass().getName() + "\" is not writable!");
        }

        try
        {
            if (pd instanceof LambdaPropertyDescriptor)
            {
                ((LambdaPropertyDescriptor) pd).getWriteFunction().accept(base, value);
            }
            else
            {
                pd.getWrapped().getWriteMethod().invoke(base, value);
            }
        }
        catch (Exception e)
        {
            throw new ELException(e);
        }
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property)
    {
        Objects.requireNonNull(context);
        if (base == null || property == null)
        {
            return false;
        }

        context.setPropertyResolved(base, property);

        return getPropertyDescriptor(base, property).getWrapped().getWriteMethod() == null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base)
    {
        if (base != null)
        {
            return Object.class;
        }

        return null;
    }

    protected PropertyDescriptorWrapper getPropertyDescriptor(Object base, Object property)
    {
        Map<String, ? extends PropertyDescriptorWrapper> beanCache = cache.get(base.getClass().getName());
        if (beanCache == null)
        {
            beanCache = PropertyDescriptorUtils.getCachedPropertyDescriptors(
                    FacesContext.getCurrentInstance().getExternalContext(),
                    base.getClass());
            cache.put(base.getClass().getName(), beanCache);
        }

        PropertyDescriptorWrapper pd = beanCache.get((String) property);
        if (pd == null)
        {
            throw new PropertyNotFoundException("Property [" + property
                    + "] not found on type [" + base.getClass().getName() + "]");
        }
        return pd;
    }
}
