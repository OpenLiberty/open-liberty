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


// Generated from class javax.faces.component.html._HtmlGraphicImage.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlGraphicImage extends javax.faces.component.UIGraphic
    implements javax.faces.component.behavior.ClientBehaviorHolder
{

    static public final String COMPONENT_FAMILY =
        "javax.faces.Graphic";
    static public final String COMPONENT_TYPE =
        "javax.faces.HtmlGraphicImage";


    public HtmlGraphicImage()
    {
        setRendererType("javax.faces.Image");
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

    
    // Property: height

    public String getHeight()
    {
        return (String) getStateHelper().eval(PropertyKeys.height);
    }
    
    public void setHeight(String height)
    {
        getStateHelper().put(PropertyKeys.height, height ); 
    }
    // Property: ismap

    public boolean isIsmap()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.ismap, false);
    }
    
    public void setIsmap(boolean ismap)
    {
        getStateHelper().put(PropertyKeys.ismap, ismap ); 
    }
    // Property: longdesc

    public String getLongdesc()
    {
        return (String) getStateHelper().eval(PropertyKeys.longdesc);
    }
    
    public void setLongdesc(String longdesc)
    {
        getStateHelper().put(PropertyKeys.longdesc, longdesc ); 
    }
    // Property: usemap

    public String getUsemap()
    {
        return (String) getStateHelper().eval(PropertyKeys.usemap);
    }
    
    public void setUsemap(String usemap)
    {
        getStateHelper().put(PropertyKeys.usemap, usemap ); 
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
    // Property: alt

    public String getAlt()
    {
        return (String) getStateHelper().eval(PropertyKeys.alt);
    }
    
    public void setAlt(String alt)
    {
        getStateHelper().put(PropertyKeys.alt, alt ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ALT_PROP);
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
         height
        , ismap
        , longdesc
        , usemap
        , width
        , style
        , styleClass
        , alt
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
