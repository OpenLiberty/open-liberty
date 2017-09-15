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
package javax.faces.component.html;

import javax.faces.el.ValueBinding;
import javax.el.ValueExpression;


// Generated from class javax.faces.component.html._HtmlMessages.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlMessages extends javax.faces.component.UIMessages
{

    static public final String COMPONENT_FAMILY =
        "javax.faces.Messages";
    static public final String COMPONENT_TYPE =
        "javax.faces.HtmlMessages";


    public HtmlMessages()
    {
        setRendererType("javax.faces.Messages");
    }

    @Override    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }



    
    // Property: layout

    public String getLayout()
    {
        return (String) getStateHelper().eval(PropertyKeys.layout, "list");
    }
    
    public void setLayout(String layout)
    {
        getStateHelper().put(PropertyKeys.layout, layout ); 
    }
    // Property: style

    public String getStyle()
    {
        return (String) getStateHelper().eval(PropertyKeys.style);
    }
    
    public void setStyle(String style)
    {
        getStateHelper().put(PropertyKeys.style, style ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.STYLE_PROP);
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
    // Property: errorClass

    public String getErrorClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.errorClass);
    }
    
    public void setErrorClass(String errorClass)
    {
        getStateHelper().put(PropertyKeys.errorClass, errorClass ); 
    }
    // Property: errorStyle

    public String getErrorStyle()
    {
        return (String) getStateHelper().eval(PropertyKeys.errorStyle);
    }
    
    public void setErrorStyle(String errorStyle)
    {
        getStateHelper().put(PropertyKeys.errorStyle, errorStyle ); 
    }
    // Property: fatalClass

    public String getFatalClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.fatalClass);
    }
    
    public void setFatalClass(String fatalClass)
    {
        getStateHelper().put(PropertyKeys.fatalClass, fatalClass ); 
    }
    // Property: fatalStyle

    public String getFatalStyle()
    {
        return (String) getStateHelper().eval(PropertyKeys.fatalStyle);
    }
    
    public void setFatalStyle(String fatalStyle)
    {
        getStateHelper().put(PropertyKeys.fatalStyle, fatalStyle ); 
    }
    // Property: infoClass

    public String getInfoClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.infoClass);
    }
    
    public void setInfoClass(String infoClass)
    {
        getStateHelper().put(PropertyKeys.infoClass, infoClass ); 
    }
    // Property: infoStyle

    public String getInfoStyle()
    {
        return (String) getStateHelper().eval(PropertyKeys.infoStyle);
    }
    
    public void setInfoStyle(String infoStyle)
    {
        getStateHelper().put(PropertyKeys.infoStyle, infoStyle ); 
    }
    // Property: tooltip

    public boolean isTooltip()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.tooltip, false);
    }
    
    public void setTooltip(boolean tooltip)
    {
        getStateHelper().put(PropertyKeys.tooltip, tooltip ); 
    }
    // Property: warnClass

    public String getWarnClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.warnClass);
    }
    
    public void setWarnClass(String warnClass)
    {
        getStateHelper().put(PropertyKeys.warnClass, warnClass ); 
    }
    // Property: warnStyle

    public String getWarnStyle()
    {
        return (String) getStateHelper().eval(PropertyKeys.warnStyle);
    }
    
    public void setWarnStyle(String warnStyle)
    {
        getStateHelper().put(PropertyKeys.warnStyle, warnStyle ); 
    }
    // Property: role

    public String getRole()
    {
        return (String) getStateHelper().eval(PropertyKeys.role);
    }
    
    public void setRole(String role)
    {
        getStateHelper().put(PropertyKeys.role, role ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ROLE_PROP);
    }
    // Property: dir

    public String getDir()
    {
        return (String) getStateHelper().eval(PropertyKeys.dir);
    }
    
    public void setDir(String dir)
    {
        getStateHelper().put(PropertyKeys.dir, dir ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.DIR_PROP);
    }
    // Property: lang

    public String getLang()
    {
        return (String) getStateHelper().eval(PropertyKeys.lang);
    }
    
    public void setLang(String lang)
    {
        getStateHelper().put(PropertyKeys.lang, lang ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.LANG_PROP);
    }
    // Property: title

    public String getTitle()
    {
        return (String) getStateHelper().eval(PropertyKeys.title);
    }
    
    public void setTitle(String title)
    {
        getStateHelper().put(PropertyKeys.title, title ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.TITLE_PROP);
    }

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
         layout
        , style
        , styleClass
        , errorClass
        , errorStyle
        , fatalClass
        , fatalStyle
        , infoClass
        , infoStyle
        , tooltip
        , warnClass
        , warnStyle
        , role
        , dir
        , lang
        , title
    }

 }
