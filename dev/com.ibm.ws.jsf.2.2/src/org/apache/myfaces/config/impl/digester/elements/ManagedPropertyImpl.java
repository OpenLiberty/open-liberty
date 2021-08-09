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
package org.apache.myfaces.config.impl.digester.elements;

import java.io.Serializable;

import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.apache.myfaces.util.ContainerUtils;


/**
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a> (latest modification by $Author: lu4242 $)
 * @author Anton Koinov
 *
 * @version $Revision: 1537800 $ $Date: 2013-11-01 01:55:08 +0000 (Fri, 01 Nov 2013) $
 */
public class ManagedPropertyImpl extends org.apache.myfaces.config.element.ManagedProperty implements Serializable
{
    private static final ValueBinding DUMMY_VB = new DummyValueBinding();

    private int                       _type    = TYPE_UNKNOWN;
    private String                    _propertyName;
    private String                    _propertyClass;
    private transient ValueBinding    _valueBinding;
    private String                    _value;
    private org.apache.myfaces.config.element.MapEntries                _mapEntries;
    private org.apache.myfaces.config.element.ListEntries               _listEntries;

    public int getType()
    {
        return _type;
    }
    
    public org.apache.myfaces.config.element.MapEntries getMapEntries()
    {
        return _mapEntries;
    }
    
    public void setMapEntries(org.apache.myfaces.config.element.MapEntries mapEntries)
    {
        _mapEntries = mapEntries;
        _type = TYPE_MAP;
    }
    
    public org.apache.myfaces.config.element.ListEntries getListEntries()
    {
        return _listEntries;
    }
    
    public void setListEntries(ListEntriesImpl listEntries)
    {
        _listEntries = listEntries;
        _type = TYPE_LIST;
    }
    
    public String getPropertyName()
    {
        return _propertyName;
    }
    
    public void setPropertyName(String propertyName)
    {
        _propertyName = propertyName;
    }
    
    public String getPropertyClass()
    {
        return _propertyClass;
    }
    
    public void setPropertyClass(String propertyClass)
    {
        _propertyClass = propertyClass;
    }
    
    public boolean isNullValue()
    {
        return _type == TYPE_NULL;
    }
    
    public void setNullValue()
    {
        _type = TYPE_NULL;
    }
    
    public void setValue(String value)
    {
        _value = value;
        _type = TYPE_VALUE;
    }

    public String getValue()
    {
        return _value;
    }
    
    public Object getRuntimeValue(FacesContext facesContext)
    {
        getValueBinding(facesContext);

        return (_valueBinding == DUMMY_VB)
            ? _value : _valueBinding.getValue(facesContext);
    }
    
    public ValueBinding getValueBinding(FacesContext facesContext)
    {
        if (_valueBinding == null)
        {
            _valueBinding =
                isValueReference()
                ? facesContext.getApplication().createValueBinding(_value)
                : DUMMY_VB;
        }
        return _valueBinding;
    }
    
    public boolean isValueReference()
    {
        return ContainerUtils.isValueReference(_value);
    }
    
    private static class DummyValueBinding extends ValueBinding
    {
        @Override
        public String getExpressionString()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<?> getType(FacesContext facesContext)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getValue(FacesContext facesContext)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isReadOnly(FacesContext facesContext)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setValue(FacesContext facesContext, Object value)
        {
            throw new UnsupportedOperationException();
        }
    }
}
