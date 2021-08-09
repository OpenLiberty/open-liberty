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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a>
 */
public class ConverterImpl extends org.apache.myfaces.config.element.Converter implements Serializable
{

    private String converterId;
    private String forClass;
    private String converterClass;
    private List<org.apache.myfaces.config.element.Property> _properties = null;
    private List<org.apache.myfaces.config.element.Attribute> _attributes = null;


    public String getConverterId()
    {
        return converterId;
    }


    public void setConverterId(String converterId)
    {
        this.converterId = converterId;
    }


    public String getForClass()
    {
        return forClass;
    }


    public void setForClass(String forClass)
    {
        this.forClass = forClass;
    }


    public String getConverterClass()
    {
        return converterClass;
    }


    public void setConverterClass(String converterClass)
    {
        this.converterClass = converterClass;
    }

    public void addProperty(org.apache.myfaces.config.element.Property value)
    {
        if(_properties == null)
        {
            _properties = new ArrayList<org.apache.myfaces.config.element.Property>();
        }

        _properties.add(value);
    }

    public Collection<? extends org.apache.myfaces.config.element.Property> getProperties()
    {
        if(_properties == null)
        {
            return Collections.emptyList();
        }

        return _properties;
    }
    
    public void addAttribute(org.apache.myfaces.config.element.Attribute value)
    {
        if(_attributes == null)
        {
            _attributes = new ArrayList<org.apache.myfaces.config.element.Attribute>();
        }

        _attributes.add(value);
    }

    public Collection<? extends org.apache.myfaces.config.element.Attribute> getAttributes()
    {
        if(_attributes == null)
        {
            return Collections.emptyList();
        }

        return _attributes;
    }
}
