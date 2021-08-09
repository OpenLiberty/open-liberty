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
package org.apache.myfaces.taglib.html;

import javax.faces.component.UIComponent;
import javax.el.ValueExpression;


// Generated from class javax.faces.component.html._HtmlMessages.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlMessagesTag
    extends javax.faces.webapp.UIComponentELTag
{
    public HtmlMessagesTag()
    {    
    }
    
    @Override
    public String getComponentType()
    {
        return "javax.faces.HtmlMessages";
    }

    public String getRendererType()
    {
        return "javax.faces.Messages";
    }

    private ValueExpression _layout;
    
    public void setLayout(ValueExpression layout)
    {
        _layout = layout;
    }
    private ValueExpression _style;
    
    public void setStyle(ValueExpression style)
    {
        _style = style;
    }
    private ValueExpression _styleClass;
    
    public void setStyleClass(ValueExpression styleClass)
    {
        _styleClass = styleClass;
    }
    private ValueExpression _errorClass;
    
    public void setErrorClass(ValueExpression errorClass)
    {
        _errorClass = errorClass;
    }
    private ValueExpression _errorStyle;
    
    public void setErrorStyle(ValueExpression errorStyle)
    {
        _errorStyle = errorStyle;
    }
    private ValueExpression _fatalClass;
    
    public void setFatalClass(ValueExpression fatalClass)
    {
        _fatalClass = fatalClass;
    }
    private ValueExpression _fatalStyle;
    
    public void setFatalStyle(ValueExpression fatalStyle)
    {
        _fatalStyle = fatalStyle;
    }
    private ValueExpression _infoClass;
    
    public void setInfoClass(ValueExpression infoClass)
    {
        _infoClass = infoClass;
    }
    private ValueExpression _infoStyle;
    
    public void setInfoStyle(ValueExpression infoStyle)
    {
        _infoStyle = infoStyle;
    }
    private ValueExpression _tooltip;
    
    public void setTooltip(ValueExpression tooltip)
    {
        _tooltip = tooltip;
    }
    private ValueExpression _warnClass;
    
    public void setWarnClass(ValueExpression warnClass)
    {
        _warnClass = warnClass;
    }
    private ValueExpression _warnStyle;
    
    public void setWarnStyle(ValueExpression warnStyle)
    {
        _warnStyle = warnStyle;
    }
    private ValueExpression _role;
    
    public void setRole(ValueExpression role)
    {
        _role = role;
    }
    private ValueExpression _dir;
    
    public void setDir(ValueExpression dir)
    {
        _dir = dir;
    }
    private ValueExpression _lang;
    
    public void setLang(ValueExpression lang)
    {
        _lang = lang;
    }
    private ValueExpression _title;
    
    public void setTitle(ValueExpression title)
    {
        _title = title;
    }
    private ValueExpression _globalOnly;
    
    public void setGlobalOnly(ValueExpression globalOnly)
    {
        _globalOnly = globalOnly;
    }
    private ValueExpression _showDetail;
    
    public void setShowDetail(ValueExpression showDetail)
    {
        _showDetail = showDetail;
    }
    private ValueExpression _showSummary;
    
    public void setShowSummary(ValueExpression showSummary)
    {
        _showSummary = showSummary;
    }
    private ValueExpression _for;
    
    public void setFor(ValueExpression forParam)
    {
        _for = forParam;
    }

    @Override
    protected void setProperties(UIComponent component)
    {
        if (!(component instanceof javax.faces.component.html.HtmlMessages ))
        {
            throw new IllegalArgumentException("Component "+
                component.getClass().getName() +" is no javax.faces.component.html.HtmlMessages");
        }
        
        javax.faces.component.html.HtmlMessages comp = (javax.faces.component.html.HtmlMessages) component;
        
        super.setProperties(component);
        

        if (_layout != null)
        {
            comp.setValueExpression("layout", _layout);
        } 
        if (_style != null)
        {
            comp.setValueExpression("style", _style);
        } 
        if (_styleClass != null)
        {
            comp.setValueExpression("styleClass", _styleClass);
        } 
        if (_errorClass != null)
        {
            comp.setValueExpression("errorClass", _errorClass);
        } 
        if (_errorStyle != null)
        {
            comp.setValueExpression("errorStyle", _errorStyle);
        } 
        if (_fatalClass != null)
        {
            comp.setValueExpression("fatalClass", _fatalClass);
        } 
        if (_fatalStyle != null)
        {
            comp.setValueExpression("fatalStyle", _fatalStyle);
        } 
        if (_infoClass != null)
        {
            comp.setValueExpression("infoClass", _infoClass);
        } 
        if (_infoStyle != null)
        {
            comp.setValueExpression("infoStyle", _infoStyle);
        } 
        if (_tooltip != null)
        {
            comp.setValueExpression("tooltip", _tooltip);
        } 
        if (_warnClass != null)
        {
            comp.setValueExpression("warnClass", _warnClass);
        } 
        if (_warnStyle != null)
        {
            comp.setValueExpression("warnStyle", _warnStyle);
        } 
        if (_role != null)
        {
            comp.setValueExpression("role", _role);
        } 
        if (_dir != null)
        {
            comp.setValueExpression("dir", _dir);
        } 
        if (_lang != null)
        {
            comp.setValueExpression("lang", _lang);
        } 
        if (_title != null)
        {
            comp.setValueExpression("title", _title);
        } 
        if (_globalOnly != null)
        {
            comp.setValueExpression("globalOnly", _globalOnly);
        } 
        if (_showDetail != null)
        {
            comp.setValueExpression("showDetail", _showDetail);
        } 
        if (_showSummary != null)
        {
            comp.setValueExpression("showSummary", _showSummary);
        } 
        if (_for != null)
        {
            comp.setValueExpression("for", _for);
        } 
    }

    @Override
    public void release()
    {
        super.release();
        _layout = null;
        _style = null;
        _styleClass = null;
        _errorClass = null;
        _errorStyle = null;
        _fatalClass = null;
        _fatalStyle = null;
        _infoClass = null;
        _infoStyle = null;
        _tooltip = null;
        _warnClass = null;
        _warnStyle = null;
        _role = null;
        _dir = null;
        _lang = null;
        _title = null;
        _globalOnly = null;
        _showDetail = null;
        _showSummary = null;
        _for = null;
    }
}
