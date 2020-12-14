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
package jakarta.faces.component.html;

import jakarta.faces.el.ValueBinding;
import jakarta.el.ValueExpression;
import jakarta.faces.context.FacesContext;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;


// Generated from class jakarta.faces.component.html._HtmlColumn.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlColumn extends jakarta.faces.component.UIColumn
{

    static public final String COMPONENT_FAMILY =
        "jakarta.faces.Column";
    static public final String COMPONENT_TYPE =
        "jakarta.faces.HtmlColumn";


    public HtmlColumn()
    {
        setRendererType(null);
    }

    @Override    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }





    
    // Property: headerClass

    public String getHeaderClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.headerClass);
    }
    
    public void setHeaderClass(String headerClass)
    {
        getStateHelper().put(PropertyKeys.headerClass, headerClass ); 
    }
    // Property: footerClass

    public String getFooterClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.footerClass);
    }
    
    public void setFooterClass(String footerClass)
    {
        getStateHelper().put(PropertyKeys.footerClass, footerClass ); 
    }
    // Property: rowHeader

    public boolean isRowHeader()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.rowHeader, false);
    }
    
    public void setRowHeader(boolean rowHeader)
    {
        getStateHelper().put(PropertyKeys.rowHeader, rowHeader ); 
    }
    // Property: styleClass

    public String getStyleClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.styleClass);
    }
    
    public void setStyleClass(String styleClass)
    {
        getStateHelper().put(PropertyKeys.styleClass, styleClass ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.STYLECLASS_PROP);
    }

    @Deprecated
    public void setValueBinding(String name, ValueBinding binding)
    {
        super.setValueBinding(name, binding);
        _CommonPropertyConstants.markProperty(this, name);
    }

    public void setValueExpression(String name, ValueExpression expression)
    {
        super.setValueExpression(name, expression);
        _CommonPropertyConstants.markProperty(this, name);
    }

    protected enum PropertyKeys
    {
         headerClass
        , footerClass
        , rowHeader
        , styleClass
    }

 }
