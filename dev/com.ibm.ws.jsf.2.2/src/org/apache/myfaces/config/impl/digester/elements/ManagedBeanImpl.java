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
import java.util.List;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;

import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.view.facelets.el.ELText;


/**
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a>
 */
public class ManagedBeanImpl extends org.apache.myfaces.config.element.ManagedBean implements Serializable
{

    private String description;
    private String name;
    private String beanClassName;
    private Class<?> beanClass;
    private String scope;
    private List<org.apache.myfaces.config.element.ManagedProperty> property
            = new ArrayList<org.apache.myfaces.config.element.ManagedProperty>();
    private org.apache.myfaces.config.element.MapEntries mapEntries;
    private org.apache.myfaces.config.element.ListEntries listEntries;
    private ValueExpression scopeValueExpression;
    private String eager;

    public int getInitMode()
    {
        if (mapEntries != null)
        {
            return INIT_MODE_MAP;
        }
        if (listEntries != null)
        {
            return INIT_MODE_LIST;
        }
        if (! property.isEmpty())
        {
            return INIT_MODE_PROPERTIES;
        }
        return INIT_MODE_NO_INIT;
    }



    public org.apache.myfaces.config.element.MapEntries getMapEntries()
    {
        return mapEntries;
    }


    public void setMapEntries(org.apache.myfaces.config.element.MapEntries mapEntries)
    {
        this.mapEntries = mapEntries;
    }


    public org.apache.myfaces.config.element.ListEntries getListEntries()
    {
        return listEntries;
    }


    public void setListEntries(org.apache.myfaces.config.element.ListEntries listEntries)
    {
        this.listEntries = listEntries;
    }


    public String getDescription()
    {
        return description;
    }
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
    public String getManagedBeanName()
    {
        return name;
    }


    public void setName(String name)
    {
        this.name = name;
    }


    public String getManagedBeanClassName()
    {
        return beanClassName;
    }


    public Class<?> getManagedBeanClass()
    {
        if (beanClassName == null)
        {
            return null;
        }
        
        if (beanClass == null)
        {
            beanClass = ClassUtils.simpleClassForName(beanClassName);
        }
        
        return beanClass;
    }


    public void setBeanClass(String beanClass)
    {
        this.beanClassName = beanClass;
    }


    public String getManagedBeanScope()
    {
        return scope;
    }


    public void setScope(String scope)
    {
        this.scope = scope;
    }


    public void addProperty(org.apache.myfaces.config.element.ManagedProperty value)
    {
        property.add(value);
    }


    public Collection<? extends org.apache.myfaces.config.element.ManagedProperty> getManagedProperties()
    {
        return property;
    }
    
    public boolean isManagedBeanScopeValueExpression()
    {
        return (scope != null) 
                   && (scopeValueExpression != null || !ELText.isLiteral(scope));
    }
    
    public ValueExpression getManagedBeanScopeValueExpression(FacesContext facesContext)
    {
        if (scopeValueExpression == null)
        {
            // we need to set the expected type to Object, because we have to generate a 
            // Exception text with the actual value and the actual type of the expression,
            // if the expression does not resolve to java.util.Map
            scopeValueExpression = 
                isManagedBeanScopeValueExpression()
                ? facesContext.getApplication().getExpressionFactory()
                        .createValueExpression(facesContext.getELContext(), scope, Object.class)
                : null;
        }
        return scopeValueExpression;
    }

    public String getEager()
    {
        return eager;
    }
    
    public void setEager(String eager)
    {
        this.eager = eager;
    }
    
}
