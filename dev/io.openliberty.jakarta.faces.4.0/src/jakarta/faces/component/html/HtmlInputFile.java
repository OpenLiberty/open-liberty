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

import jakarta.el.ValueExpression;
import jakarta.faces.context.FacesContext;
import java.util.Collections;
import org.apache.myfaces.core.api.shared.MessageUtils;
import org.apache.myfaces.core.api.shared.CommonHtmlEvents;
import org.apache.myfaces.core.api.shared.CommonHtmlAttributes;
import jakarta.faces.component.UIComponent;
import jakarta.faces.convert.Converter;


// Generated from class jakarta.faces.component.html._HtmlInputFile.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlInputFile extends jakarta.faces.component.UIInput
    implements jakarta.faces.component.behavior.ClientBehaviorHolder
{

    static public final String COMPONENT_FAMILY =
        "jakarta.faces.Input";
    static public final String COMPONENT_TYPE =
        "jakarta.faces.HtmlInputFile";

    //BEGIN CODE COPIED FROM jakarta.faces.component.html._HtmlInputFile
    protected void validateValue(jakarta.faces.context.FacesContext context, java.lang.Object convertedValue)
    {
        if (!isValid())
        {
            return;
        }

        // If our value is empty, check the required property
        boolean isEmpty = isEmptyValue(convertedValue); 

        if (isRequired() && isEmpty)
        {
            if (getRequiredMessage() != null)
            {
                String requiredMessage = getRequiredMessage();
                context.addMessage(this.getClientId(context), new jakarta.faces.application.FacesMessage(
                        jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    requiredMessage, requiredMessage));
            }
            else
            {
                MessageUtils.addErrorMessage(context, this, REQUIRED_MESSAGE_ID,
                    new Object[] { MessageUtils.getLabel(context, this) });
            }
            setValid(false);
            return;
        }

        if (!isEmpty)
        {
            super.validateValue(context, convertedValue);
        }
    }

    private static boolean isEmptyValue(java.lang.Object value)
    {
        if (value == null)
        {
            return true;
        }
        else if (value instanceof String)
        {
            if ( ((String)value).trim().length() <= 0 )
            {
                return true;
            }
        }
        else if (value instanceof java.util.Collection)
        {
            if ( ((java.util.Collection)value).isEmpty())
            {
                return true;
            }
        }
        else if (value.getClass().isArray())
        {
            if (java.lang.reflect.Array.getLength(value) <= 0)
            {
                return true;
            }
        }
        else if (value instanceof java.util.Map)
        {
            if ( ((java.util.Map)value).isEmpty())
            {
                return true;
            }
        }
        else if (value instanceof jakarta.servlet.http.Part) 
        {
            if (((jakarta.servlet.http.Part)value).getSize() <= 0) 
            {
                return true;
            }
        }
        return false;
    }


    //END CODE COPIED FROM jakarta.faces.component.html._HtmlInputFile

    public HtmlInputFile()
    {
        setRendererType("jakarta.faces.File");
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }


    static private final java.util.Collection<String> CLIENT_EVENTS_LIST =
        java.util.Collections.unmodifiableCollection(
            java.util.Arrays.asList(
             "select"
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
            , "change"
            , "valueChange"
        ));

    public java.util.Collection<String> getEventNames()
    {
        return CLIENT_EVENTS_LIST;
    }

    @Override
    public void addClientBehavior(String eventName, jakarta.faces.component.behavior.ClientBehavior behavior)
    {
        super.addClientBehavior(eventName, behavior);
        CommonHtmlEvents.markEvent(this, eventName);
    }



    //ClientBehaviorHolder default: valueChange
    public String getDefaultEventName()
    {
        return "valueChange";
    }


    // Property: maxlength

    @Deprecated(since = "4.0", forRemoval = true)
    public int getMaxlength()
    {
        return (Integer) getStateHelper().eval(PropertyKeys.maxlength, Integer.MIN_VALUE);
    }

    @Deprecated(since = "4.0", forRemoval = true)
    public void setMaxlength(int maxlength)
    {
        getStateHelper().put(PropertyKeys.maxlength, maxlength );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.MAXLENGTH);
    }
    // Property: size

    @Deprecated(since = "4.0", forRemoval = true)
    public int getSize()
    {
        return (Integer) getStateHelper().eval(PropertyKeys.size, Integer.MIN_VALUE);
    }

    @Deprecated(since = "4.0", forRemoval = true)
    public void setSize(int size)
    {
        getStateHelper().put(PropertyKeys.size, size );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.SIZE);
    }
    // Property: autocomplete

    @Deprecated(since = "4.0", forRemoval = true)
    public String getAutocomplete()
    {
        return (String) getStateHelper().eval(PropertyKeys.autocomplete);
    }

    @Deprecated(since = "4.0", forRemoval = true)
    public void setAutocomplete(String autocomplete)
    {
        getStateHelper().put(PropertyKeys.autocomplete, autocomplete );
    }
    // Property: accept

    public String getAccept()
    {
        return (String) getStateHelper().eval(PropertyKeys.accept);
    }

    public void setAccept(String accept)
    {
        getStateHelper().put(PropertyKeys.accept, accept );
    }
    // Property: multiple

    public boolean isMultiple()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.multiple, false);
    }

    public void setMultiple(boolean multiple)
    {
        getStateHelper().put(PropertyKeys.multiple, multiple );
    }
    // Property: accesskey

    public String getAccesskey()
    {
        return (String) getStateHelper().eval(PropertyKeys.accesskey);
    }

    public void setAccesskey(String accesskey)
    {
        getStateHelper().put(PropertyKeys.accesskey, accesskey );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ACCESSKEY);
    }
    // Property: onselect

    public String getOnselect()
    {
        return (String) getStateHelper().eval(PropertyKeys.onselect);
    }

    public void setOnselect(String onselect)
    {
        getStateHelper().put(PropertyKeys.onselect, onselect );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONSELECT);
    }
    // Property: disabled

    public boolean isDisabled()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.disabled, false);
    }

    public void setDisabled(boolean disabled)
    {
        getStateHelper().put(PropertyKeys.disabled, disabled );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.DISABLED);
    }
    // Property: readonly

    @Deprecated(since = "4.0", forRemoval = true)
    public boolean isReadonly()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.readonly, false);
    }

    @Deprecated(since = "4.0", forRemoval = true)
    public void setReadonly(boolean readonly)
    {
        getStateHelper().put(PropertyKeys.readonly, readonly );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.READONLY);
    }
    // Property: onclick

    public String getOnclick()
    {
        return (String) getStateHelper().eval(PropertyKeys.onclick);
    }

    public void setOnclick(String onclick)
    {
        getStateHelper().put(PropertyKeys.onclick, onclick );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONCLICK);
    }
    // Property: ondblclick

    public String getOndblclick()
    {
        return (String) getStateHelper().eval(PropertyKeys.ondblclick);
    }

    public void setOndblclick(String ondblclick)
    {
        getStateHelper().put(PropertyKeys.ondblclick, ondblclick );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONDBLCLICK);
    }
    // Property: onkeydown

    public String getOnkeydown()
    {
        return (String) getStateHelper().eval(PropertyKeys.onkeydown);
    }

    public void setOnkeydown(String onkeydown)
    {
        getStateHelper().put(PropertyKeys.onkeydown, onkeydown );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONKEYDOWN);
    }
    // Property: onkeypress

    public String getOnkeypress()
    {
        return (String) getStateHelper().eval(PropertyKeys.onkeypress);
    }

    public void setOnkeypress(String onkeypress)
    {
        getStateHelper().put(PropertyKeys.onkeypress, onkeypress );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONKEYPRESS);
    }
    // Property: onkeyup

    public String getOnkeyup()
    {
        return (String) getStateHelper().eval(PropertyKeys.onkeyup);
    }

    public void setOnkeyup(String onkeyup)
    {
        getStateHelper().put(PropertyKeys.onkeyup, onkeyup );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONKEYUP);
    }
    // Property: onmousedown

    public String getOnmousedown()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmousedown);
    }

    public void setOnmousedown(String onmousedown)
    {
        getStateHelper().put(PropertyKeys.onmousedown, onmousedown );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONMOUSEDOWN);
    }
    // Property: onmousemove

    public String getOnmousemove()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmousemove);
    }

    public void setOnmousemove(String onmousemove)
    {
        getStateHelper().put(PropertyKeys.onmousemove, onmousemove );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONMOUSEMOVE);
    }
    // Property: onmouseout

    public String getOnmouseout()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmouseout);
    }

    public void setOnmouseout(String onmouseout)
    {
        getStateHelper().put(PropertyKeys.onmouseout, onmouseout );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONMOUSEOUT);
    }
    // Property: onmouseover

    public String getOnmouseover()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmouseover);
    }

    public void setOnmouseover(String onmouseover)
    {
        getStateHelper().put(PropertyKeys.onmouseover, onmouseover );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONMOUSEOVER);
    }
    // Property: onmouseup

    public String getOnmouseup()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmouseup);
    }

    public void setOnmouseup(String onmouseup)
    {
        getStateHelper().put(PropertyKeys.onmouseup, onmouseup );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONMOUSEUP);
    }
    // Property: style

    public String getStyle()
    {
        return (String) getStateHelper().eval(PropertyKeys.style);
    }

    public void setStyle(String style)
    {
        getStateHelper().put(PropertyKeys.style, style );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.STYLE);
    }
    // Property: styleClass

    public String getStyleClass()
    {
        return (String) getStateHelper().eval(PropertyKeys.styleClass);
    }

    public void setStyleClass(String styleClass)
    {
        getStateHelper().put(PropertyKeys.styleClass, styleClass );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.STYLECLASS);
    }
    // Property: role

    public String getRole()
    {
        return (String) getStateHelper().eval(PropertyKeys.role);
    }

    public void setRole(String role)
    {
        getStateHelper().put(PropertyKeys.role, role );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ROLE);
    }
    // Property: label

    public String getLabel()
    {
        return (String) getStateHelper().eval(PropertyKeys.label);
    }

    public void setLabel(String label)
    {
        getStateHelper().put(PropertyKeys.label, label );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.LABEL);
    }
    // Property: alt

    @Deprecated(since = "4.0", forRemoval = true)
    public String getAlt()
    {
        return (String) getStateHelper().eval(PropertyKeys.alt);
    }

    @Deprecated(since = "4.0", forRemoval = true)
    public void setAlt(String alt)
    {
        getStateHelper().put(PropertyKeys.alt, alt );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ALT);
    }
    // Property: dir

    public String getDir()
    {
        return (String) getStateHelper().eval(PropertyKeys.dir);
    }

    public void setDir(String dir)
    {
        getStateHelper().put(PropertyKeys.dir, dir );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.DIR);
    }
    // Property: lang

    public String getLang()
    {
        return (String) getStateHelper().eval(PropertyKeys.lang);
    }

    public void setLang(String lang)
    {
        getStateHelper().put(PropertyKeys.lang, lang );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.LANG);
    }
    // Property: title

    public String getTitle()
    {
        return (String) getStateHelper().eval(PropertyKeys.title);
    }

    public void setTitle(String title)
    {
        getStateHelper().put(PropertyKeys.title, title );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.TITLE);
    }
    // Property: onblur

    public String getOnblur()
    {
        return (String) getStateHelper().eval(PropertyKeys.onblur);
    }

    public void setOnblur(String onblur)
    {
        getStateHelper().put(PropertyKeys.onblur, onblur );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONBLUR);
    }
    // Property: onfocus

    public String getOnfocus()
    {
        return (String) getStateHelper().eval(PropertyKeys.onfocus);
    }

    public void setOnfocus(String onfocus)
    {
        getStateHelper().put(PropertyKeys.onfocus, onfocus );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONFOCUS);
    }
    // Property: tabindex

    public String getTabindex()
    {
        return (String) getStateHelper().eval(PropertyKeys.tabindex);
    }

    public void setTabindex(String tabindex)
    {
        getStateHelper().put(PropertyKeys.tabindex, tabindex );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.TABINDEX);
    }
    // Property: onchange

    public String getOnchange()
    {
        return (String) getStateHelper().eval(PropertyKeys.onchange);
    }

    public void setOnchange(String onchange)
    {
        getStateHelper().put(PropertyKeys.onchange, onchange );
        CommonHtmlAttributes.markAttribute(this, CommonHtmlAttributes.ONCHANGE);
    }

    public void setValueExpression(String name, ValueExpression expression)
    {
        super.setValueExpression(name, expression);
        CommonHtmlAttributes.markAttribute(this, name);
    }

    protected enum PropertyKeys
    {
         @Deprecated(since = "4.0", forRemoval = true) maxlength
        , @Deprecated(since = "4.0", forRemoval = true) size
        , @Deprecated(since = "4.0", forRemoval = true) autocomplete
        , accept
        , multiple
        , accesskey
        , onselect
        , disabled
        , @Deprecated(since = "4.0", forRemoval = true) readonly
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
        , role
        , label
        , @Deprecated(since = "4.0", forRemoval = true) alt
        , dir
        , lang
        , title
        , onblur
        , onfocus
        , tabindex
        , onchange
    }

 }
