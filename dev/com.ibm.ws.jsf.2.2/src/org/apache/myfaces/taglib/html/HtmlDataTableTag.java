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


// Generated from class javax.faces.component.html._HtmlDataTable.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlDataTableTag
    extends javax.faces.webapp.UIComponentELTag
{
    public HtmlDataTableTag()
    {    
    }
    
    @Override
    public String getComponentType()
    {
        return "javax.faces.HtmlDataTable";
    }

    public String getRendererType()
    {
        return "javax.faces.Table";
    }

    private ValueExpression _bgcolor;
    
    public void setBgcolor(ValueExpression bgcolor)
    {
        _bgcolor = bgcolor;
    }
    private ValueExpression _bodyrows;
    
    public void setBodyrows(ValueExpression bodyrows)
    {
        _bodyrows = bodyrows;
    }
    private ValueExpression _border;
    
    public void setBorder(ValueExpression border)
    {
        _border = border;
    }
    private ValueExpression _cellpadding;
    
    public void setCellpadding(ValueExpression cellpadding)
    {
        _cellpadding = cellpadding;
    }
    private ValueExpression _cellspacing;
    
    public void setCellspacing(ValueExpression cellspacing)
    {
        _cellspacing = cellspacing;
    }
    private ValueExpression _columnClasses;
    
    public void setColumnClasses(ValueExpression columnClasses)
    {
        _columnClasses = columnClasses;
    }
    private ValueExpression _footerClass;
    
    public void setFooterClass(ValueExpression footerClass)
    {
        _footerClass = footerClass;
    }
    private ValueExpression _frame;
    
    public void setFrame(ValueExpression frame)
    {
        _frame = frame;
    }
    private ValueExpression _headerClass;
    
    public void setHeaderClass(ValueExpression headerClass)
    {
        _headerClass = headerClass;
    }
    private ValueExpression _rowClasses;
    
    public void setRowClasses(ValueExpression rowClasses)
    {
        _rowClasses = rowClasses;
    }
    private ValueExpression _rules;
    
    public void setRules(ValueExpression rules)
    {
        _rules = rules;
    }
    private ValueExpression _summary;
    
    public void setSummary(ValueExpression summary)
    {
        _summary = summary;
    }
    private ValueExpression _width;
    
    public void setWidth(ValueExpression width)
    {
        _width = width;
    }
    private ValueExpression _captionClass;
    
    public void setCaptionClass(ValueExpression captionClass)
    {
        _captionClass = captionClass;
    }
    private ValueExpression _captionStyle;
    
    public void setCaptionStyle(ValueExpression captionStyle)
    {
        _captionStyle = captionStyle;
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
    private ValueExpression _value;
    
    public void setValue(ValueExpression value)
    {
        _value = value;
    }
    private ValueExpression _first;
    
    public void setFirst(ValueExpression first)
    {
        _first = first;
    }
    private ValueExpression _rows;
    
    public void setRows(ValueExpression rows)
    {
        _rows = rows;
    }
    private java.lang.String _var;
    
    public void setVar(java.lang.String var)
    {
        _var = var;
    }
    private String _rowStatePreserved;
    
    public void setRowStatePreserved(String rowStatePreserved)
    {
        _rowStatePreserved = rowStatePreserved;
    }

    @Override
    protected void setProperties(UIComponent component)
    {
        if (!(component instanceof javax.faces.component.html.HtmlDataTable ))
        {
            throw new IllegalArgumentException("Component "+
                component.getClass().getName() +" is no javax.faces.component.html.HtmlDataTable");
        }
        
        javax.faces.component.html.HtmlDataTable comp = (javax.faces.component.html.HtmlDataTable) component;
        
        super.setProperties(component);
        

        if (_bgcolor != null)
        {
            comp.setValueExpression("bgcolor", _bgcolor);
        } 
        if (_bodyrows != null)
        {
            comp.setValueExpression("bodyrows", _bodyrows);
        } 
        if (_border != null)
        {
            comp.setValueExpression("border", _border);
        } 
        if (_cellpadding != null)
        {
            comp.setValueExpression("cellpadding", _cellpadding);
        } 
        if (_cellspacing != null)
        {
            comp.setValueExpression("cellspacing", _cellspacing);
        } 
        if (_columnClasses != null)
        {
            comp.setValueExpression("columnClasses", _columnClasses);
        } 
        if (_footerClass != null)
        {
            comp.setValueExpression("footerClass", _footerClass);
        } 
        if (_frame != null)
        {
            comp.setValueExpression("frame", _frame);
        } 
        if (_headerClass != null)
        {
            comp.setValueExpression("headerClass", _headerClass);
        } 
        if (_rowClasses != null)
        {
            comp.setValueExpression("rowClasses", _rowClasses);
        } 
        if (_rules != null)
        {
            comp.setValueExpression("rules", _rules);
        } 
        if (_summary != null)
        {
            comp.setValueExpression("summary", _summary);
        } 
        if (_width != null)
        {
            comp.setValueExpression("width", _width);
        } 
        if (_captionClass != null)
        {
            comp.setValueExpression("captionClass", _captionClass);
        } 
        if (_captionStyle != null)
        {
            comp.setValueExpression("captionStyle", _captionStyle);
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
        if (_value != null)
        {
            comp.setValueExpression("value", _value);
        } 
        if (_first != null)
        {
            comp.setValueExpression("first", _first);
        } 
        if (_rows != null)
        {
            comp.setValueExpression("rows", _rows);
        } 
        if (_var != null)
        {
            comp.getAttributes().put("var", _var);
        } 
        if (_rowStatePreserved != null)
        {
            comp.getAttributes().put("rowStatePreserved", Boolean.valueOf(_rowStatePreserved));
        } 
    }

    @Override
    public void release()
    {
        super.release();
        _bgcolor = null;
        _bodyrows = null;
        _border = null;
        _cellpadding = null;
        _cellspacing = null;
        _columnClasses = null;
        _footerClass = null;
        _frame = null;
        _headerClass = null;
        _rowClasses = null;
        _rules = null;
        _summary = null;
        _width = null;
        _captionClass = null;
        _captionStyle = null;
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
        _value = null;
        _first = null;
        _rows = null;
        _var = null;
        _rowStatePreserved = null;
    }
}
