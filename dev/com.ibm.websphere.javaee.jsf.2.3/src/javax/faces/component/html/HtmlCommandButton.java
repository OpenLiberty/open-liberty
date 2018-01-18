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
import javax.faces.context.FacesContext;
import javax.faces.el.MethodBinding;
import javax.el.MethodExpression;
import javax.faces.component.UIComponent;


// Generated from class javax.faces.component.html._HtmlCommandButton.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlCommandButton extends javax.faces.component.UICommand
    implements javax.faces.component.behavior.ClientBehaviorHolder
{

    static public final String COMPONENT_FAMILY =
        "javax.faces.Command";
    static public final String COMPONENT_TYPE =
        "javax.faces.HtmlCommandButton";


    public HtmlCommandButton()
    {
        setRendererType("javax.faces.Button");
    }

    @Override    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }


    static private final java.util.Collection<String> CLIENT_EVENTS_LIST = 
        java.util.Collections.unmodifiableCollection(
            java.util.Arrays.asList(
             "change"
            , "select"
            , "click"
            , "dblclick"
            , "keydown"
            , "keypress"
            , "keyup"
            , "mousedown"
            , "mousemove"
            , "mouseout"
            , "mouseover"
            , "mouseup"
            , "blur"
            , "focus"
            , "action"
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

    //ClientBehaviorHolder default: action
    public String getDefaultEventName()
    {
        return "action";
    }

    
    // Property: image

    public String getImage()
    {
        return (String) getStateHelper().eval(PropertyKeys.image);
    }
    
    public void setImage(String image)
    {
        getStateHelper().put(PropertyKeys.image, image ); 
    }
    // Property: type

    public String getType()
    {
        return (String) getStateHelper().eval(PropertyKeys.type, "submit");
    }
    
    public void setType(String type)
    {
        getStateHelper().put(PropertyKeys.type, type ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.TYPE_PROP);
    }
    // Property: accesskey

    public String getAccesskey()
    {
        return (String) getStateHelper().eval(PropertyKeys.accesskey);
    }
    
    public void setAccesskey(String accesskey)
    {
        getStateHelper().put(PropertyKeys.accesskey, accesskey ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ACCESSKEY_PROP);
    }
    // Property: onchange

    public String getOnchange()
    {
        return (String) getStateHelper().eval(PropertyKeys.onchange);
    }
    
    public void setOnchange(String onchange)
    {
        getStateHelper().put(PropertyKeys.onchange, onchange ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONCHANGE_PROP);
    }
    // Property: onselect

    public String getOnselect()
    {
        return (String) getStateHelper().eval(PropertyKeys.onselect);
    }
    
    public void setOnselect(String onselect)
    {
        getStateHelper().put(PropertyKeys.onselect, onselect ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONSELECT_PROP);
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
    // Property: disabled

    public boolean isDisabled()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.disabled, false);
    }
    
    public void setDisabled(boolean disabled)
    {
        getStateHelper().put(PropertyKeys.disabled, disabled ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.DISABLED_PROP);
    }
    // Property: readonly

    public boolean isReadonly()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.readonly, false);
    }
    
    public void setReadonly(boolean readonly)
    {
        getStateHelper().put(PropertyKeys.readonly, readonly ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.READONLY_PROP);
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
    // Property: label

    public String getLabel()
    {
        return (String) getStateHelper().eval(PropertyKeys.label);
    }
    
    public void setLabel(String label)
    {
        getStateHelper().put(PropertyKeys.label, label ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.LABEL_PROP);
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
    // Property: onblur

    public String getOnblur()
    {
        return (String) getStateHelper().eval(PropertyKeys.onblur);
    }
    
    public void setOnblur(String onblur)
    {
        getStateHelper().put(PropertyKeys.onblur, onblur ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONBLUR_PROP);
    }
    // Property: onfocus

    public String getOnfocus()
    {
        return (String) getStateHelper().eval(PropertyKeys.onfocus);
    }
    
    public void setOnfocus(String onfocus)
    {
        getStateHelper().put(PropertyKeys.onfocus, onfocus ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.ONFOCUS_PROP);
    }
    // Property: tabindex

    public String getTabindex()
    {
        return (String) getStateHelper().eval(PropertyKeys.tabindex);
    }
    
    public void setTabindex(String tabindex)
    {
        getStateHelper().put(PropertyKeys.tabindex, tabindex ); 
        _CommonPropertyConstants.markProperty(this, _CommonPropertyConstants.TABINDEX_PROP);
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
         image
        , type
        , accesskey
        , onchange
        , onselect
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
        , style
        , styleClass
        , disabled
        , readonly
        , role
        , label
        , alt
        , dir
        , lang
        , title
        , onblur
        , onfocus
        , tabindex
    }

 }
