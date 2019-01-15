/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces.component;

import javax.el.ValueExpression;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 *
 */
@JSFComponent
(clazz = "javax.faces.component.UIImportConstants",
 name = "f:importConstants", bodyContent = "empty")
public class UIImportConstants extends UIComponentBase
{
    
    static public final String COMPONENT_FAMILY = "javax.faces.ImportConstants";
    static public final String COMPONENT_TYPE = "javax.faces.ImportConstants";


    public UIImportConstants()
    {
        setRendererType(null);
    }

    @Override    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }
    
    @JSFProperty
    public String getType()
    {
        return (String) getStateHelper().eval(PropertyKeys.type);
    }
    
    public void setType(String type)
    {
        getStateHelper().put(PropertyKeys.type, type ); 
    }

    @JSFProperty
    public String getVar()
    {
        return (String) getStateHelper().eval(PropertyKeys.var);
    }
    
    public void setVar(String var)
    {
        getStateHelper().put(PropertyKeys.var, var ); 
    }

    @Override
    public void setValueExpression(String name, ValueExpression binding) 
    {
        if (PropertyKeys.var.toString().equals(name)) 
        {
            throw new IllegalArgumentException(name);
        }

        super.setValueExpression(name, binding);
    }

    enum PropertyKeys
    {
         type
        , var
    }
}
