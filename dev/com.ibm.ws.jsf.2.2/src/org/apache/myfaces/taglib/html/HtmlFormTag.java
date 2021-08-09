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


// Generated from class javax.faces.component.html._HtmlForm.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlFormTag
    extends javax.faces.webapp.UIComponentELTag
{
    public HtmlFormTag()
    {    
    }
    
    @Override
    public String getComponentType()
    {
        return "javax.faces.HtmlForm";
    }

    public String getRendererType()
    {
        return "javax.faces.Form";
    }

    private ValueExpression _accept;
    
    public void setAccept(ValueExpression accept)
    {
        _accept = accept;
    }
    private ValueExpression _acceptcharset;
    
    public void setAcceptcharset(ValueExpression acceptcharset)
    {
        _acceptcharset = acceptcharset;
    }
    private ValueExpression _enctype;
    
    public void setEnctype(ValueExpression enctype)
    {
        _enctype = enctype;
    }
    private ValueExpression _onreset;
    
    public void setOnreset(ValueExpression onreset)
    {
        _onreset = onreset;
    }
    private ValueExpression _onsubmit;
    
    public void setOnsubmit(ValueExpression onsubmit)
    {
        _onsubmit = onsubmit;
    }
    private ValueExpression _target;
    
    public void setTarget(ValueExpression target)
    {
        _target = target;
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
    private ValueExpression _role;
    
    public void setRole(ValueExpression role)
    {
        _role = role;
    }
    private ValueExpression _onclick;
    
    public void setOnclick(ValueExpression onclick)
    {
        _onclick = onclick;
    }
    private ValueExpression _ondblclick;
    
    public void setOndblclick(ValueExpression ondblclick)
    {
        _ondblclick = ondblclick;
    }
    private ValueExpression _onkeydown;
    
    public void setOnkeydown(ValueExpression onkeydown)
    {
        _onkeydown = onkeydown;
    }
    private ValueExpression _onkeypress;
    
    public void setOnkeypress(ValueExpression onkeypress)
    {
        _onkeypress = onkeypress;
    }
    private ValueExpression _onkeyup;
    
    public void setOnkeyup(ValueExpression onkeyup)
    {
        _onkeyup = onkeyup;
    }
    private ValueExpression _onmousedown;
    
    public void setOnmousedown(ValueExpression onmousedown)
    {
        _onmousedown = onmousedown;
    }
    private ValueExpression _onmousemove;
    
    public void setOnmousemove(ValueExpression onmousemove)
    {
        _onmousemove = onmousemove;
    }
    private ValueExpression _onmouseout;
    
    public void setOnmouseout(ValueExpression onmouseout)
    {
        _onmouseout = onmouseout;
    }
    private ValueExpression _onmouseover;
    
    public void setOnmouseover(ValueExpression onmouseover)
    {
        _onmouseover = onmouseover;
    }
    private ValueExpression _onmouseup;
    
    public void setOnmouseup(ValueExpression onmouseup)
    {
        _onmouseup = onmouseup;
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
    private ValueExpression _prependId;
    
    public void setPrependId(ValueExpression prependId)
    {
        _prependId = prependId;
    }

    @Override
    protected void setProperties(UIComponent component)
    {
        if (!(component instanceof javax.faces.component.html.HtmlForm ))
        {
            throw new IllegalArgumentException("Component "+
                component.getClass().getName() +" is no javax.faces.component.html.HtmlForm");
        }
        
        javax.faces.component.html.HtmlForm comp = (javax.faces.component.html.HtmlForm) component;
        
        super.setProperties(component);
        

        if (_accept != null)
        {
            comp.setValueExpression("accept", _accept);
        } 
        if (_acceptcharset != null)
        {
            comp.setValueExpression("acceptcharset", _acceptcharset);
        } 
        if (_enctype != null)
        {
            comp.setValueExpression("enctype", _enctype);
        } 
        if (_onreset != null)
        {
            comp.setValueExpression("onreset", _onreset);
        } 
        if (_onsubmit != null)
        {
            comp.setValueExpression("onsubmit", _onsubmit);
        } 
        if (_target != null)
        {
            comp.setValueExpression("target", _target);
        } 
        if (_style != null)
        {
            comp.setValueExpression("style", _style);
        } 
        if (_styleClass != null)
        {
            comp.setValueExpression("styleClass", _styleClass);
        } 
        if (_role != null)
        {
            comp.setValueExpression("role", _role);
        } 
        if (_onclick != null)
        {
            comp.setValueExpression("onclick", _onclick);
        } 
        if (_ondblclick != null)
        {
            comp.setValueExpression("ondblclick", _ondblclick);
        } 
        if (_onkeydown != null)
        {
            comp.setValueExpression("onkeydown", _onkeydown);
        } 
        if (_onkeypress != null)
        {
            comp.setValueExpression("onkeypress", _onkeypress);
        } 
        if (_onkeyup != null)
        {
            comp.setValueExpression("onkeyup", _onkeyup);
        } 
        if (_onmousedown != null)
        {
            comp.setValueExpression("onmousedown", _onmousedown);
        } 
        if (_onmousemove != null)
        {
            comp.setValueExpression("onmousemove", _onmousemove);
        } 
        if (_onmouseout != null)
        {
            comp.setValueExpression("onmouseout", _onmouseout);
        } 
        if (_onmouseover != null)
        {
            comp.setValueExpression("onmouseover", _onmouseover);
        } 
        if (_onmouseup != null)
        {
            comp.setValueExpression("onmouseup", _onmouseup);
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
        if (_prependId != null)
        {
            comp.setValueExpression("prependId", _prependId);
        } 
    }

    @Override
    public void release()
    {
        super.release();
        _accept = null;
        _acceptcharset = null;
        _enctype = null;
        _onreset = null;
        _onsubmit = null;
        _target = null;
        _style = null;
        _styleClass = null;
        _role = null;
        _onclick = null;
        _ondblclick = null;
        _onkeydown = null;
        _onkeypress = null;
        _onkeyup = null;
        _onmousedown = null;
        _onmousemove = null;
        _onmouseout = null;
        _onmouseover = null;
        _onmouseup = null;
        _dir = null;
        _lang = null;
        _title = null;
        _prependId = null;
    }
}
