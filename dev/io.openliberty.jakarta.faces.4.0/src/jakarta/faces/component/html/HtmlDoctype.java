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


// Generated from class jakarta.faces.component.html._HtmlDoctype.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class HtmlDoctype extends jakarta.faces.component.UIOutput
    implements jakarta.faces.component.Doctype
{

    static public final String COMPONENT_FAMILY =
        "jakarta.faces.Output";
    static public final String COMPONENT_TYPE =
        "jakarta.faces.OutputDoctype";


    public HtmlDoctype()
    {
        setRendererType("jakarta.faces.Doctype");
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }






    // Property: public

    public String getPublic()
    {
        return (String) getStateHelper().eval(PropertyKeys.publicVal);
    }

    public void setPublic(String publicParam)
    {
        getStateHelper().put(PropertyKeys.publicVal, publicParam );
    }
    // Property: rootElement

    public String getRootElement()
    {
        return (String) getStateHelper().eval(PropertyKeys.rootElement);
    }

    public void setRootElement(String rootElement)
    {
        getStateHelper().put(PropertyKeys.rootElement, rootElement );
    }
    // Property: system

    public String getSystem()
    {
        return (String) getStateHelper().eval(PropertyKeys.system);
    }

    public void setSystem(String system)
    {
        getStateHelper().put(PropertyKeys.system, system );
    }


    protected enum PropertyKeys
    {
         publicVal("public")
        , rootElement
        , system
        ;
        String c;

        PropertyKeys()
        {
        }

        //Constructor needed by "for" property
        PropertyKeys(String c)
        {
            this.c = c;
        }

        public String toString()
        {
            return ((this.c != null) ? this.c : super.toString());
        }
    }

 }
