// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.myfaces.taglib.core;

import javax.faces.component.UIComponent;
import javax.el.ValueExpression;


// Generated from class javax.faces.component._UISelectItems.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class SelectItemsTag
    extends javax.faces.webapp.UIComponentELTag
{
    public SelectItemsTag()
    {    
    }
    
    @Override
    public String getComponentType()
    {
        return "javax.faces.SelectItems";
    }

    public String getRendererType()
    {
        return null;
    }

    private ValueExpression _value;
    
    public void setValue(ValueExpression value)
    {
        _value = value;
    }
    private java.lang.String _var;
    
    public void setVar(java.lang.String var)
    {
        _var = var;
    }
    private ValueExpression _itemValue;
    
    public void setItemValue(ValueExpression itemValue)
    {
        _itemValue = itemValue;
    }
    private ValueExpression _itemLabel;
    
    public void setItemLabel(ValueExpression itemLabel)
    {
        _itemLabel = itemLabel;
    }
    private ValueExpression _itemDescription;
    
    public void setItemDescription(ValueExpression itemDescription)
    {
        _itemDescription = itemDescription;
    }
    private ValueExpression _itemDisabled;
    
    public void setItemDisabled(ValueExpression itemDisabled)
    {
        _itemDisabled = itemDisabled;
    }
    private ValueExpression _itemLabelEscaped;
    
    public void setItemLabelEscaped(ValueExpression itemLabelEscaped)
    {
        _itemLabelEscaped = itemLabelEscaped;
    }

    @Override
    protected void setProperties(UIComponent component)
    {
        if (!(component instanceof javax.faces.component.UISelectItems ))
        {
            throw new IllegalArgumentException("Component "+
                component.getClass().getName() +" is no javax.faces.component.UISelectItems");
        }
        
        javax.faces.component.UISelectItems comp = (javax.faces.component.UISelectItems) component;
        
        super.setProperties(component);
        

        if (_value != null)
        {
            comp.setValueExpression("value", _value);
        } 
        if (_var != null)
        {
            comp.getAttributes().put("var", _var);
        } 
        if (_itemValue != null)
        {
            comp.setValueExpression("itemValue", _itemValue);
        } 
        if (_itemLabel != null)
        {
            comp.setValueExpression("itemLabel", _itemLabel);
        } 
        if (_itemDescription != null)
        {
            comp.setValueExpression("itemDescription", _itemDescription);
        } 
        if (_itemDisabled != null)
        {
            comp.setValueExpression("itemDisabled", _itemDisabled);
        } 
        if (_itemLabelEscaped != null)
        {
            comp.setValueExpression("itemLabelEscaped", _itemLabelEscaped);
        } 
    }

    @Override
    public void release()
    {
        super.release();
        _value = null;
        _var = null;
        _itemValue = null;
        _itemLabel = null;
        _itemDescription = null;
        _itemDisabled = null;
        _itemLabelEscaped = null;
    }
}
