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


// Generated from class javax.faces.component.html._HtmlDataTable.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlDataTable extends javax.faces.component.UIData
    implements javax.faces.component.behavior.ClientBehaviorHolder
{

    static public final String COMPONENT_FAMILY =
        "javax.faces.Data";
    static public final String COMPONENT_TYPE =
        "javax.faces.HtmlDataTable";


    public HtmlDataTable()
    {
        setRendererType("javax.faces.Table");
    }

    @Override    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }


    static private final java.util.Collection<String> CLIENT_EVENTS_LIST = 
        java.util.Collections.unmodifiableCollection(
            java.util.Arrays.asList(
             "click"
            , "dblclick"
            , "keydown"
            , "keypress"
            , "keyup"
            , "mousedown"
            , "mousemove"
            , "mouseout"
            , "mouseover"
            , "mouseup"
        ));

    public java.util.Collection<String> getEventNames()
    {
        return CLIENT_EVENTS_LIST;
    }

    @Override
    public void addClientBehavior(String eventName, javax.faces.component.behavior.ClientBehavior behavior)
    {
        super.addClientBehavior(eventName, behavior);
        _CommonEventConstants.markEvent(this, eventName);
    }

    
    // Property: bgcolor

    public String getBgcolor()
    {
        return (String) getStateHelper().eval(PropertyKeys.bgcolor);
    }
    
    public void setBgcolor(String bgcolor)
    {
        getStateHelper().put(PropertyKeys.bgcolor, bgcolor ); 
    }
    // Property: bodyrows

    public String getBodyrows()
    {
        return (String) getStateHelper().eval(PropertyKeys.bodyrows);
    }
    
    public void setBodyrows(String bodyrows)
    {
        getStateHelper().put(PropertyKeys.bodyrows, bodyrows ); 
    }
    // Property: border

    public int getBorder()
    {
        return (Integer) getStateHelper().eval(PropertyKeys.border, Integer.MIN_VALUE);
    }
    
    public void setBorder(int border)
    {
        getStateHelper().put(PropertyKeys.border, border ); 
    }
    // Property: cellpadding

    public String getCellpadding()
    {
        return (String) getStateHelper().eval(PropertyKeys.cellpadding);
    }
    
    public void setCellpadding(String cellpadding)
    {
        getStateHelper().put(PropertyKeys.cellpadding, cellpadding ); 
    }
    // Property: cellspacing

    public String getCellspacing()
    {
        return (String) getStateHelper().eval(PropertyKeys.cellspacing);
    }
    
    public void setCellspacing(String cellspacing)
    {
        getStateHelper().put(PropertyKeys.cellspacing, cellspacing ); 
    }
    // Property: columnClasses

    public String getColumnClasses()
    {
        return (String) getStateHelper().eval(PropertyKeys.columnClasses);
    }
    
    public void setColumnClasses(String columnClasses)
    {
        getStateHelper().put(PropertyKeys.columnClasses, columnClasses ); 
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
    // Property: frame

    public String getFrame()
    {
        return (String) getStateHelper().eval(PropertyKeys.frame);
    }
    
    public void setFrame(String frame)
    {
        getStateHelper().put(PropertyKeys.frame, frame ); 
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
    // Property: rowClasses

    public String getRowClasses()
    {
        return (String) getStateHelper().eval(PropertyKeys.rowClasses);
    }
    
    public void setRowClasses(String rowClasses)
    {
        getStateHelper().put(PropertyKeys.rowClasses, rowClasses ); 
    }
    // Property: rules

    public String getRules()
    {
        return (String) getStateHelper().eval(PropertyKeys.rules);
    }
    
    public void setRules(String rules)
    {
        getStateHelper().put(PropertyKeys.rules, rules ); 
    }
    // Property: summary

    public String getSummary()
    {
        return (String) getStateHelper().eval(PropertyKeys.summary);
    }
    
    public void setSummary(String summary)
    {
        getStateHelper().put(PropertyKeys.summary, summary ); 
    }
    // Property: width

    public String getWidth()
    {
        return (String) getStateHelper().eval(PropertyKeys.width);
    }
    
    public void setWidth(String width)
    {
        getStateHelper().put(PropertyKeys.width, width ); 
    }
    // Property: captionClass

    public String getCaptionClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.captionClass);
    }
    
    public void setCaptionClass(String captionClass)
    {
        getStateHelper().put(PropertyKeys.captionClass, captionClass ); 
    }
    // Property: captionStyle

    public String getCaptionStyle()
    {
        return (String) getStateHelper().eval(PropertyKeys.captionStyle);
    }
    
    public void setCaptionStyle(String captionStyle)
    {
        getStateHelper().put(PropertyKeys.captionStyle, captionStyle ); 
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
    // Property: onclick

    public String getOnclick()
    {
        return (String) getStateHelper().eval(PropertyKeys.onclick);
    }
    
    public void setOnclick(String onclick)
    {
        getStateHelper().put(PropertyKeys.onclick, onclick ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONCLICK_PROP);
    }
    // Property: ondblclick

    public String getOndblclick()
    {
        return (String) getStateHelper().eval(PropertyKeys.ondblclick);
    }
    
    public void setOndblclick(String ondblclick)
    {
        getStateHelper().put(PropertyKeys.ondblclick, ondblclick ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONDBLCLICK_PROP);
    }
    // Property: onkeydown

    public String getOnkeydown()
    {
        return (String) getStateHelper().eval(PropertyKeys.onkeydown);
    }
    
    public void setOnkeydown(String onkeydown)
    {
        getStateHelper().put(PropertyKeys.onkeydown, onkeydown ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONKEYDOWN_PROP);
    }
    // Property: onkeypress

    public String getOnkeypress()
    {
        return (String) getStateHelper().eval(PropertyKeys.onkeypress);
    }
    
    public void setOnkeypress(String onkeypress)
    {
        getStateHelper().put(PropertyKeys.onkeypress, onkeypress ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONKEYPRESS_PROP);
    }
    // Property: onkeyup

    public String getOnkeyup()
    {
        return (String) getStateHelper().eval(PropertyKeys.onkeyup);
    }
    
    public void setOnkeyup(String onkeyup)
    {
        getStateHelper().put(PropertyKeys.onkeyup, onkeyup ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONKEYUP_PROP);
    }
    // Property: onmousedown

    public String getOnmousedown()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmousedown);
    }
    
    public void setOnmousedown(String onmousedown)
    {
        getStateHelper().put(PropertyKeys.onmousedown, onmousedown ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONMOUSEDOWN_PROP);
    }
    // Property: onmousemove

    public String getOnmousemove()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmousemove);
    }
    
    public void setOnmousemove(String onmousemove)
    {
        getStateHelper().put(PropertyKeys.onmousemove, onmousemove ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONMOUSEMOVE_PROP);
    }
    // Property: onmouseout

    public String getOnmouseout()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmouseout);
    }
    
    public void setOnmouseout(String onmouseout)
    {
        getStateHelper().put(PropertyKeys.onmouseout, onmouseout ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONMOUSEOUT_PROP);
    }
    // Property: onmouseover

    public String getOnmouseover()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmouseover);
    }
    
    public void setOnmouseover(String onmouseover)
    {
        getStateHelper().put(PropertyKeys.onmouseover, onmouseover ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONMOUSEOVER_PROP);
    }
    // Property: onmouseup

    public String getOnmouseup()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmouseup);
    }
    
    public void setOnmouseup(String onmouseup)
    {
        getStateHelper().put(PropertyKeys.onmouseup, onmouseup ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONMOUSEUP_PROP);
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
         bgcolor
        , bodyrows
        , border
        , cellpadding
        , cellspacing
        , columnClasses
        , footerClass
        , frame
        , headerClass
        , rowClasses
        , rules
        , summary
        , width
        , captionClass
        , captionStyle
        , style
        , styleClass
        , role
        , onclick
        , ondblclick
        , onkeydown
        , onkeypress
        , onkeyup
        , onmousedown
        , onmousemove
        , onmouseout
        , onmouseover
        , onmouseup
        , dir
        , lang
        , title
    }

 }
