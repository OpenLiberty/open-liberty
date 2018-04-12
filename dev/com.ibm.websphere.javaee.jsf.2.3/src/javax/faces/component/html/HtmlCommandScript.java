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


// Generated from class javax.faces.component.html._HtmlCommandScript.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlCommandScript extends javax.faces.component.UICommand
{

    static public final String COMPONENT_FAMILY =
        "javax.faces.Command";
    static public final String COMPONENT_TYPE =
        "javax.faces.HtmlCommandScript";


    public HtmlCommandScript()
    {
        setRendererType("javax.faces.Script");
    }

    @Override    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }



    //ClientBehaviorHolder default: action
    public String getDefaultEventName()
    {
        return "action";
    }

    
    // Property: autorun

    public boolean isAutorun()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.autorun, false);
    }
    
    public void setAutorun(boolean autorun)
    {
        getStateHelper().put(PropertyKeys.autorun, autorun ); 
    }
    // Property: execute

    public String getExecute()
    {
        return (String) getStateHelper().eval(PropertyKeys.execute);
    }
    
    public void setExecute(String execute)
    {
        getStateHelper().put(PropertyKeys.execute, execute ); 
    }
    // Property: name

    public String getName()
    {
        return (String) getStateHelper().eval(PropertyKeys.name);
    }
    
    public void setName(String name)
    {
        getStateHelper().put(PropertyKeys.name, name ); 
    }
    // Property: onerror

    public String getOnerror()
    {
        return (String) getStateHelper().eval(PropertyKeys.onerror);
    }
    
    public void setOnerror(String onerror)
    {
        getStateHelper().put(PropertyKeys.onerror, onerror ); 
    }
    // Property: onevent

    public String getOnevent()
    {
        return (String) getStateHelper().eval(PropertyKeys.onevent);
    }
    
    public void setOnevent(String onevent)
    {
        getStateHelper().put(PropertyKeys.onevent, onevent ); 
    }
    // Property: render

    public String getRender()
    {
        return (String) getStateHelper().eval(PropertyKeys.render);
    }
    
    public void setRender(String render)
    {
        getStateHelper().put(PropertyKeys.render, render ); 
    }
    // Property: resetValues

    public Boolean getResetValues()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.resetValues);
    }
    
    public void setResetValues(Boolean resetValues)
    {
        getStateHelper().put(PropertyKeys.resetValues, resetValues ); 
    }


    protected enum PropertyKeys
    {
         autorun
        , execute
        , name
        , onerror
        , onevent
        , render
        , resetValues
    }

 }
