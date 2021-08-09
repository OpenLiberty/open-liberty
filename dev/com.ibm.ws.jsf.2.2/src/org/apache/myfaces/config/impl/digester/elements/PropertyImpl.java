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
 * @author Martin Marinschek
 * @version $Revision: 1537800 $ $Date: 2013-11-01 01:55:08 +0000 (Fri, 01 Nov 2013) $
 *
 * The "property" element represents a JavaBean property of the Java class
 * represented by our parent element.
 *
 * Property names must be unique within the scope of the Java class
 * that is represented by the parent element, and must correspond to
 * property names that will be recognized when performing introspection
 * against that class via java.beans.Introspector.
 *
 * <!ELEMENT property        (description*, display-name*, icon*, property-name, property-class,
 * default-value?, suggested-value?, property-extension*)>
 *
 *          <p/>
 */
public class PropertyImpl extends org.apache.myfaces.config.element.Property implements Serializable
{
    private List<String> _description;
    private List<String> _displayName;
    private List<String> _icon;
    private String _propertyName;
    private String _propertyClass;
    private String _defaultValue;
    private String _suggestedValue;
    private List<String> _propertyExtension;


    public void addDescription(String value)
    {
        if(_description == null)
        {
            _description = new ArrayList<String>();
        }

        _description.add(value);
    }

    public Collection<? extends String> getDescriptions()
    {
        if(_description == null)
        {
            return Collections.emptyList();
        }

        return _description;
    }

    public void addDisplayName(String value)
    {
        if(_displayName == null)
        {
            _displayName = new ArrayList<String>();
        }

        _displayName.add(value);
    }

    public Collection<? extends String> getDisplayNames()
    {
        if(_displayName==null)
        {
            return Collections.emptyList();
        }

        return _displayName;
    }

    public void addIcon(String value)
    {
        if(_icon == null)
        {
            _icon = new ArrayList<String>();
        }

        _icon.add(value);
    }

    public Collection<? extends String> getIcons()
    {
        if(_icon == null)
        {
            return Collections.emptyList();
        }

        return _icon;
    }

    public void setPropertyName(String propertyName)
    {
        _propertyName = propertyName;
    }

    public String getPropertyName()
    {
        return _propertyName;
    }

    public void setPropertyClass(String propertyClass)
    {
        _propertyClass = propertyClass;
    }

    public String getPropertyClass()
    {
        return _propertyClass;
    }

    public void setDefaultValue(String defaultValue)
    {
        _defaultValue = defaultValue;
    }

    public String getDefaultValue()
    {
        return _defaultValue;
    }

    public void setSuggestedValue(String suggestedValue)
    {
        _suggestedValue = suggestedValue;
    }

    public String getSuggestedValue()
    {
        return _suggestedValue;
    }

    public void addPropertyExtension(String propertyExtension)
    {
        if(_propertyExtension == null)
        {
            _propertyExtension = new ArrayList<String>();
        }

        _propertyExtension.add(propertyExtension);
    }

    public Collection<? extends String> getPropertyExtensions()
    {
        if(_propertyExtension == null)
        {
            return Collections.emptyList();
        }

        return _propertyExtension;
    }

}
