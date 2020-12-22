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

import jakarta.faces.el.ValueBinding;
import jakarta.el.ValueExpression;
import jakarta.faces.context.FacesContext;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.convert.Converter;


// Generated from class jakarta.faces.component.html._HtmlHead.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlHead extends jakarta.faces.component.UIOutput
{

    static public final String COMPONENT_FAMILY =
        "jakarta.faces.Output";
    static public final String COMPONENT_TYPE =
        "jakarta.faces.OutputHead";


    public HtmlHead()
    {
        setRendererType("jakarta.faces.Head");
    }

    @Override    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
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
    // Property: xmlns

    public String getXmlns()
    {
        return (String) getStateHelper().eval(PropertyKeys.xmlns);
    }
    
    public void setXmlns(String xmlns)
    {
        getStateHelper().put(PropertyKeys.xmlns, xmlns ); 
    }

    @Deprecated
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
         dir
        , lang
        , xmlns
    }

 }
