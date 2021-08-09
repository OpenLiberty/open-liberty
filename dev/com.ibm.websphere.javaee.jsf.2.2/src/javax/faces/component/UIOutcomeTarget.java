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

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * 
 * @since 2.0
 */
@JSFComponent
public class UIOutcomeTarget extends UIOutput
{
    public static final String COMPONENT_TYPE = "javax.faces.OutcomeTarget";
    public static final String COMPONENT_FAMILY = "javax.faces.OutcomeTarget";
    
    private static final boolean DEFAULT_INCLUDEVIEWPARAMS = false;
    private static final boolean DEFAULT_DISABLE_CLIENT_WINDOW = false;
    
    public UIOutcomeTarget()
    {
        super();
        setRendererType("javax.faces.Link");
    }
    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }

    @JSFProperty
    public String getOutcome()
    {
        String outcome = (String) getStateHelper().eval(PropertyKeys.outcome);
        
        if(outcome == null && isInView())  //default to the view id
        {
            return getFacesContext().getViewRoot().getViewId();
        }
        
        return outcome;
    }

    public void setOutcome(String outcome)
    {
        getStateHelper().put(PropertyKeys.outcome, outcome);
    }

    @JSFProperty(defaultValue="false")
    public boolean isIncludeViewParams()
    {        
        return (Boolean) getStateHelper().eval(PropertyKeys.includeViewParams, DEFAULT_INCLUDEVIEWPARAMS);
    }

    public void setIncludeViewParams(boolean includeViewParams)
    {
        getStateHelper().put(PropertyKeys.includeViewParams, includeViewParams);
    }

    /**
     * @since 2.2
     * @return 
     */
    @JSFProperty(defaultValue="false")
    public boolean isDisableClientWindow()
    {        
        return (Boolean) getStateHelper().eval(PropertyKeys.disableClientWindow, DEFAULT_DISABLE_CLIENT_WINDOW);
    }

    /**
     * @since 2.2
     * @param disableClientWindow 
     */
    public void setDisableClientWindow(boolean disableClientWindow)
    {
        getStateHelper().put(PropertyKeys.disableClientWindow, disableClientWindow);
    }

    enum PropertyKeys
    {
        includeViewParams,
        outcome,
        disableClientWindow,
    }
}
