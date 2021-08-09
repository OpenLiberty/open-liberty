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


// Generated from class javax.faces.component.html._HtmlGraphicImage.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlGraphicImageTag
    extends javax.faces.webapp.UIComponentELTag
{
    public HtmlGraphicImageTag()
    {    
    }
    
    @Override
    public String getComponentType()
    {
        return "javax.faces.HtmlGraphicImage";
    }

    public String getRendererType()
    {
        return "javax.faces.Image";
    }

    private ValueExpression _height;
    
    public void setHeight(ValueExpression height)
    {
        _height = height;
    }
    private ValueExpression _ismap;
    
    public void setIsmap(ValueExpression ismap)
    {
        _ismap = ismap;
    }
    private ValueExpression _longdesc;
    
    public void setLongdesc(ValueExpression longdesc)
    {
        _longdesc = longdesc;
    }
    private ValueExpression _usemap;
    
    public void setUsemap(ValueExpression usemap)
    {
        _usemap = usemap;
    }
    private ValueExpression _width;
    
    public void setWidth(ValueExpression width)
    {
        _width = width;
    }
    private ValueExpression _library;
    
    public void setLibrary(ValueExpression library)
    {
        _library = library;
    }
    private ValueExpression _name;
    
    public void setName(ValueExpression name)
    {
        _name = name;
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
    private ValueExpression _alt;
    
    public void setAlt(ValueExpression alt)
    {
        _alt = alt;
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
    private ValueExpression _url;
    
    public void setUrl(ValueExpression url)
    {
        _url = url;
    }
    private ValueExpression _value;
    
    public void setValue(ValueExpression value)
    {
        _value = value;
    }

    @Override
    protected void setProperties(UIComponent component)
    {
        if (!(component instanceof javax.faces.component.html.HtmlGraphicImage ))
        {
            throw new IllegalArgumentException("Component "+
                component.getClass().getName() +" is no javax.faces.component.html.HtmlGraphicImage");
        }
        
        javax.faces.component.html.HtmlGraphicImage comp = (javax.faces.component.html.HtmlGraphicImage) component;
        
        super.setProperties(component);
        

        if (_height != null)
        {
            comp.setValueExpression("height", _height);
        } 
        if (_ismap != null)
        {
            comp.setValueExpression("ismap", _ismap);
        } 
        if (_longdesc != null)
        {
            comp.setValueExpression("longdesc", _longdesc);
        } 
        if (_usemap != null)
        {
            comp.setValueExpression("usemap", _usemap);
        } 
        if (_width != null)
        {
            comp.setValueExpression("width", _width);
        } 
        if (_library != null)
        {
            comp.setValueExpression("library", _library);
        } 
        if (_name != null)
        {
            comp.setValueExpression("name", _name);
        } 
        if (_style != null)
        {
            comp.setValueExpression("style", _style);
        } 
        if (_styleClass != null)
        {
            comp.setValueExpression("styleClass", _styleClass);
        } 
        if (_alt != null)
        {
            comp.setValueExpression("alt", _alt);
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
        if (_url != null)
        {
            comp.setValueExpression("url", _url);
        } 
        if (_value != null)
        {
            comp.setValueExpression("value", _value);
        } 
    }

    @Override
    public void release()
    {
        super.release();
        _height = null;
        _ismap = null;
        _longdesc = null;
        _usemap = null;
        _width = null;
        _library = null;
        _name = null;
        _style = null;
        _styleClass = null;
        _alt = null;
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
        _url = null;
        _value = null;
    }
}
