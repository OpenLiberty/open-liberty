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

import java.beans.PropertyDescriptor;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import jakarta.faces.FacesWrapper;

public class PropertyDescriptorWrapper implements FacesWrapper<PropertyDescriptor>
{
    private final PropertyDescriptor wrapped;
    private Reference<Method> readMethodRef;
    private Reference<Method> writeMethodRef;

    public PropertyDescriptorWrapper(Class<?> beanClass, PropertyDescriptor wrapped)
    {
        this.wrapped = wrapped;
        this.readMethodRef = new SoftReference<>(wrapped.getReadMethod());
    }

    public PropertyDescriptorWrapper(Class<?> beanClass, PropertyDescriptor wrapped, Method readMethod)
    {
        this.wrapped = wrapped;
        this.readMethodRef = new SoftReference<>(readMethod);
    }

    public Class<?> getPropertyType()
    {
        return wrapped.getPropertyType();
    }    
    
    public String getName()
    {
        return wrapped.getName();
    }
    
    @Override
    public PropertyDescriptor getWrapped()
    {
        return wrapped;
    }

    public Method getReadMethod()
    {
        Method readMethod = readMethodRef.get();
        if (readMethod == null)
        {
            readMethod = wrapped.getReadMethod();
            readMethodRef = new SoftReference<>(readMethod);
        }
        return readMethod;
    }

    public Method getWriteMethod()
    {
        if (writeMethodRef == null || writeMethodRef.get() == null)
        {
            // In facelets, the Method instance used to write the variable is stored
            // in a variable (see org.apache.myfaces.view.facelets.tag.BeanPropertyTagRule),
            // so the impact of this synchronized call at the end is minimal compared with 
            // getReadMethod. That's the reason why cache it here in a lazy way is enough
            // instead retrieve it as soon as this holder is created.
            Method writeMethod = wrapped.getWriteMethod();
            writeMethodRef = new SoftReference<>(writeMethod);
        }
        return writeMethodRef.get();
    }
}
