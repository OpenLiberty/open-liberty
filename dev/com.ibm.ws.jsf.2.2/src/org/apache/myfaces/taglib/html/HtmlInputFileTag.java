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
import javax.faces.event.MethodExpressionValueChangeListener;
import javax.faces.validator.MethodExpressionValidator;


// Generated from class javax.faces.component.html._HtmlInputFile.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlInputFileTag
    extends javax.faces.webapp.UIComponentELTag
{
    public HtmlInputFileTag()
    {    
    }
    
    @Override
    public String getComponentType()
    {
        return "javax.faces.HtmlInputFile";
    }

    public String getRendererType()
    {
        return "javax.faces.File";
    }

    private ValueExpression _maxlength;
    
    public void setMaxlength(ValueExpression maxlength)
    {
        _maxlength = maxlength;
    }
    private ValueExpression _size;
    
    public void setSize(ValueExpression size)
    {
        _size = size;
    }
    private ValueExpression _autocomplete;
    
    public void setAutocomplete(ValueExpression autocomplete)
    {
        _autocomplete = autocomplete;
    }
    private ValueExpression _label;
    
    public void setLabel(ValueExpression label)
    {
        _label = label;
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
    private ValueExpression _onchange;
    
    public void setOnchange(ValueExpression onchange)
    {
        _onchange = onchange;
    }
    private ValueExpression _onselect;
    
    public void setOnselect(ValueExpression onselect)
    {
        _onselect = onselect;
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
    private ValueExpression _disabled;
    
    public void setDisabled(ValueExpression disabled)
    {
        _disabled = disabled;
    }
    private ValueExpression _readonly;
    
    public void setReadonly(ValueExpression readonly)
    {
        _readonly = readonly;
    }
    private ValueExpression _immediate;
    
    public void setImmediate(ValueExpression immediate)
    {
        _immediate = immediate;
    }
    private ValueExpression _required;
    
    public void setRequired(ValueExpression required)
    {
        _required = required;
    }
    private ValueExpression _converterMessage;
    
    public void setConverterMessage(ValueExpression converterMessage)
    {
        _converterMessage = converterMessage;
    }
    private ValueExpression _requiredMessage;
    
    public void setRequiredMessage(ValueExpression requiredMessage)
    {
        _requiredMessage = requiredMessage;
    }
    private javax.el.MethodExpression _validator;
    
    public void setValidator(javax.el.MethodExpression validator)
    {
        _validator = validator;
    }
    private ValueExpression _validatorMessage;
    
    public void setValidatorMessage(ValueExpression validatorMessage)
    {
        _validatorMessage = validatorMessage;
    }
    private javax.el.MethodExpression _valueChangeListener;
    
    public void setValueChangeListener(javax.el.MethodExpression valueChangeListener)
    {
        _valueChangeListener = valueChangeListener;
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
        if (!(component instanceof javax.faces.component.html.HtmlInputFile ))
        {
            throw new IllegalArgumentException("Component "+
                component.getClass().getName() +" is no javax.faces.component.html.HtmlInputFile");
        }
        
        javax.faces.component.html.HtmlInputFile comp = (javax.faces.component.html.HtmlInputFile) component;
        
        super.setProperties(component);
        

        if (_maxlength != null)
        {
            comp.setValueExpression("maxlength", _maxlength);
        } 
        if (_size != null)
        {
            comp.setValueExpression("size", _size);
        } 
        if (_autocomplete != null)
        {
            comp.setValueExpression("autocomplete", _autocomplete);
        } 
        if (_label != null)
        {
            comp.setValueExpression("label", _label);
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
        if (_onchange != null)
        {
            comp.setValueExpression("onchange", _onchange);
        } 
        if (_onselect != null)
        {
            comp.setValueExpression("onselect", _onselect);
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
        if (_disabled != null)
        {
            comp.setValueExpression("disabled", _disabled);
        } 
        if (_readonly != null)
        {
            comp.setValueExpression("readonly", _readonly);
        } 
        if (_immediate != null)
        {
            comp.setValueExpression("immediate", _immediate);
        } 
        if (_required != null)
        {
            comp.setValueExpression("required", _required);
        } 
        if (_converterMessage != null)
        {
            comp.setValueExpression("converterMessage", _converterMessage);
        } 
        if (_requiredMessage != null)
        {
            comp.setValueExpression("requiredMessage", _requiredMessage);
        } 
        if (_validator != null)
        {
            comp.addValidator(new MethodExpressionValidator(_validator));
        }
        if (_validatorMessage != null)
        {
            comp.setValueExpression("validatorMessage", _validatorMessage);
        } 
        if (_valueChangeListener != null)
        {
            comp.addValueChangeListener(new MethodExpressionValueChangeListener(_valueChangeListener));
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
        _maxlength = null;
        _size = null;
        _autocomplete = null;
        _label = null;
        _style = null;
        _styleClass = null;
        _alt = null;
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
        _onchange = null;
        _onselect = null;
        _dir = null;
        _lang = null;
        _title = null;
        _disabled = null;
        _readonly = null;
        _immediate = null;
        _required = null;
        _converterMessage = null;
        _requiredMessage = null;
        _validator = null;
        _validatorMessage = null;
        _valueChangeListener = null;
        _value = null;
        _converter = null;
    }
}
