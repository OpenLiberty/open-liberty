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
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.facelets.MetadataTarget;
import org.apache.myfaces.core.api.shared.lang.LambdaPropertyDescriptor;
import org.apache.myfaces.core.api.shared.lang.PropertyDescriptorUtils;
import org.apache.myfaces.core.api.shared.lang.PropertyDescriptorWrapper;

public class LambdaMetadataTargetImpl extends MetadataTarget
{
    private final Map<String, ? extends PropertyDescriptorWrapper> propertyDescriptors;
    private final Class<?> type;

    public LambdaMetadataTargetImpl(Class<?> type) throws IntrospectionException
    {
        this.type = type;
        this.propertyDescriptors = PropertyDescriptorUtils.getCachedPropertyDescriptors(
                FacesContext.getCurrentInstance().getExternalContext(),
                type);
    }

    @Override
    public PropertyDescriptor getProperty(String name)
    {
        LambdaPropertyDescriptor lpd = getLambdaProperty(name);
        if (lpd == null)
        {
            return null;
        }

        return lpd.getWrapped();
    }

    @Override
    public Class<?> getPropertyType(String name)
    {
        LambdaPropertyDescriptor lpd = getLambdaProperty(name);
        if (lpd == null)
        {
            return null;
        }

        return lpd.getPropertyType();
    }

    @Override
    public Method getReadMethod(String name)
    {
        LambdaPropertyDescriptor lpd = getLambdaProperty(name);
        if (lpd == null)
        {
            return null;
        }
        
        return lpd.getReadMethod();
    }

    @Override
    public Class<?> getTargetClass()
    {
        return type;
    }

    @Override
    public Method getWriteMethod(String name)
    {
        LambdaPropertyDescriptor lpd = getLambdaProperty(name);
        if (lpd == null)
        {
            return null;
        }
        
        return lpd.getWriteMethod();
    }

    @Override
    public boolean isTargetInstanceOf(Class type)
    {
        return type.isAssignableFrom(type);
    }
 
    public LambdaPropertyDescriptor getLambdaProperty(String name)
    {
        PropertyDescriptorWrapper pdw = propertyDescriptors.get(name);
        return pdw instanceof LambdaPropertyDescriptor
                ? (LambdaPropertyDescriptor) pdw
                : null;
    }

    public Function<Object, Object> getReadFunction(String name)
    {
        LambdaPropertyDescriptor lpd = getLambdaProperty(name);
        if (lpd == null)
        {
            return null;
        }
        
        return lpd.getReadFunction();
    }

    public BiConsumer<Object, Object> getWriteFunction(String name)
    {
        LambdaPropertyDescriptor lpd = getLambdaProperty(name);
        if (lpd == null)
        {
            return null;
        }
        
        return lpd.getWriteFunction();
    }
}
