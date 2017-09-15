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
package javax.faces.component;

import java.beans.PropertyDescriptor;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;

class _PropertyDescriptorHolder
{
    private final PropertyDescriptor _descriptor;
    private Reference<Method> _readMethodRef;
    private Reference<Method> _writeMethodRef;

    public _PropertyDescriptorHolder(PropertyDescriptor descriptor)
    {
        _descriptor = descriptor;
        _readMethodRef = new SoftReference<Method>(_descriptor.getReadMethod());
    }

    public _PropertyDescriptorHolder(PropertyDescriptor descriptor, Method readMethod)
    {
        _descriptor = descriptor;
        _readMethodRef = new SoftReference<Method>(readMethod);
    }
    
    public String getName()
    {
        return _descriptor.getName();
    }
    
    public Method getReadMethod()
    {
        Method readMethod = _readMethodRef.get();
        if (readMethod == null)
        {
            readMethod = _descriptor.getReadMethod();
            _readMethodRef = new SoftReference<Method>(readMethod);
        }
        return readMethod;
    }
    
    public Method getWriteMethod()
    {
        if (_writeMethodRef == null || _writeMethodRef.get() == null)
        {
            // In facelets, the Method instance used to write the variable is stored
            // in a variable (see org.apache.myfaces.view.facelets.tag.BeanPropertyTagRule),
            // so the impact of this synchronized call at the end is minimal compared with 
            // getReadMethod. That's the reason why cache it here in a lazy way is enough
            // instead retrieve it as soon as this holder is created.
            Method writeMethod = _descriptor.getWriteMethod();
            _writeMethodRef = new SoftReference<Method>(writeMethod);
        }
        return _writeMethodRef.get();
    }
    
    public PropertyDescriptor getPropertyDescriptor()
    {
        return _descriptor;
    }
}
