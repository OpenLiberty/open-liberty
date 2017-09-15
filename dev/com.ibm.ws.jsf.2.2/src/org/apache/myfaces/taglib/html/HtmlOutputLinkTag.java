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
import javax.faces.convert.Converter;


// Generated from class javax.faces.component.html._HtmlOutputLink.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlOutputLinkTag
    extends javax.faces.webapp.UIComponentELTag
{
    public HtmlOutputLinkTag()
    {    
    }
    
    @Override
    public String getComponentType()
    {
        return "javax.faces.HtmlOutputLink";
    }

    public String getRendererType()
    {
        return "javax.faces.Link";
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
    private ValueExpression _tabindex;
    
    public void setTabindex(ValueExpression tabindex)
    {
        _tabindex = tabindex;
    }
    private ValueExpression _onblur;
    
    public void setOnblur(ValueExpression onblur)
    {
        _onblur = onblur;
    }
    private ValueExpression _onfocus;
    
    public void setOnfocus(ValueExpression onfocus)
    {
        _onfocus = onfocus;
    }
    private ValueExpression _accesskey;
    
    public void setAccesskey(ValueExpression accesskey)
    {
        _accesskey = accesskey;
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
    private ValueExpression _charset;
    
    public void setCharset(ValueExpression charset)
    {
        _charset = charset;
    }
    private ValueExpression _coords;
    
    public void setCoords(ValueExpression coords)
    {
        _coords = coords;
    }
    private ValueExpression _hreflang;
    
    public void setHreflang(ValueExpression hreflang)
    {
        _hreflang = hreflang;
    }
    private ValueExpression _rel;
    
    public void setRel(ValueExpression rel)
    {
        _rel = rel;
    }
    private ValueExpression _rev;
    
    public void setRev(ValueExpression rev)
    {
        _rev = rev;
    }
    private ValueExpression _shape;
    
    public void setShape(ValueExpression shape)
    {
        _shape = shape;
    }
    private ValueExpression _target;
    
    public void setTarget(ValueExpression target)
    {
        _target = target;
    }
    private ValueExpression _type;
    
    public void setType(ValueExpression type)
    {
        _type = type;
    }
    private ValueExpression _disabled;
    
    public void setDisabled(ValueExpression disabled)
    {
        _disabled = disabled;
    }
    private ValueExpression _value;
    
    public void setValue(ValueExpression value)
    {
        _value = value;
    }
    private ValueExpression _converter;
    
    public void setConverter(ValueExpression converter)
    {
        _converter = converter;
    }

    @Override
    protected void setProperties(UIComponent component)
    {
        if (!(component instanceof javax.faces.component.html.HtmlOutputLink ))
        {
            throw new IllegalArgumentException("Component "+
                component.getClass().getName() +" is no javax.faces.component.html.HtmlOutputLink");
        }
        
        javax.faces.component.html.HtmlOutputLink comp = (javax.faces.component.html.HtmlOutputLink) component;
        
        super.setProperties(component);
        

        if (_style != null)
        {
            comp.setValueExpression("style", _style);
        } 
        if (_styleClass != null)
        {
            comp.setValueExpression("styleClass", _styleClass);
        } 
        if (_tabindex != null)
        {
            comp.setValueExpression("tabindex", _tabindex);
        } 
        if (_onblur != null)
        {
            comp.setValueExpression("onblur", _onblur);
        } 
        if (_onfocus != null)
        {
            comp.setValueExpression("onfocus", _onfocus);
        } 
        if (_accesskey != null)
        {
            comp.setValueExpression("accesskey", _accesskey);
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
        if (_charset != null)
        {
            comp.setValueExpression("charset", _charset);
        } 
        if (_coords != null)
        {
            comp.setValueExpression("coords", _coords);
        } 
        if (_hreflang != null)
        {
            comp.setValueExpression("hreflang", _hreflang);
        } 
        if (_rel != null)
        {
            comp.setValueExpression("rel", _rel);
        } 
        if (_rev != null)
        {
            comp.setValueExpression("rev", _rev);
        } 
        if (_shape != null)
        {
            comp.setValueExpression("shape", _shape);
        } 
        if (_target != null)
        {
            comp.setValueExpression("target", _target);
        } 
        if (_type != null)
        {
            comp.setValueExpression("type", _type);
        } 
        if (_disabled != null)
        {
            comp.setValueExpression("disabled", _disabled);
        } 
        if (_value != null)
        {
            comp.setValueExpression("value", _value);
        } 
        if (_converter != null)
        {
            if (!_converter.isLiteralText())
            {
                comp.setValueExpression("converter", _converter);
            }
            else
            {
                String s = _converter.getExpressionString();
                if (s != null)
                {            
                    Converter converter = getFacesContext().getApplication().createConverter(s);
                    comp.setConverter(converter);
                }
            }
        }
    }

    @Override
    public void release()
    {
        super.release();
        _style = null;
        _styleClass = null;
        _tabindex = null;
        _onblur = null;
        _onfocus = null;
        _accesskey = null;
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
        _charset = null;
        _coords = null;
        _hreflang = null;
        _rel = null;
        _rev = null;
        _shape = null;
        _target = null;
        _type = null;
        _disabled = null;
        _value = null;
        _converter = null;
    }
}
