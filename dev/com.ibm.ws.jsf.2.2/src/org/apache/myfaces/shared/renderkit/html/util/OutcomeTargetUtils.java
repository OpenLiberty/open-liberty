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
package org.apache.myfaces.shared.renderkit.html.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.FacesException;
import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.NavigationCase;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ProjectStage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutcomeTarget;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.flow.FlowHandler;
import javax.faces.lifecycle.ClientWindow;
import org.apache.myfaces.shared.application.NavigationUtils;
import org.apache.myfaces.shared.renderkit.JSFAttr;
import org.apache.myfaces.shared.renderkit.RendererUtils;
import org.apache.myfaces.shared.renderkit.html.HtmlRendererUtils;

/**
 * Utility methods for OutcomeTarget components.
 */
public class OutcomeTargetUtils
{
    
    private static final Logger log = Logger.getLogger(OutcomeTargetUtils.class
            .getName());
    
    public static String getOutcomeTargetHref(FacesContext facesContext,
            UIOutcomeTarget component) throws IOException
    {
        String outcome = component.getOutcome();
        outcome = (outcome == null) ? facesContext.getViewRoot().getViewId()
                : outcome;
        outcome = ((outcome == null) ? HtmlRendererUtils.STR_EMPTY : outcome.trim());
        // Get the correct URL for the outcome.
        NavigationHandler nh = facesContext.getApplication().getNavigationHandler();
        if (!(nh instanceof ConfigurableNavigationHandler))
        {
            throw new FacesException(
                    "Navigation handler must be an instance of "
                            + "ConfigurableNavigationHandler for using h:link or h:button");
        }
        ConfigurableNavigationHandler navigationHandler = (ConfigurableNavigationHandler) nh;
        
        // handle faces flow 
        // 1. check to-flow-document-id
        String toFlowDocumentId = (String) component.getAttributes().get(
            JSFAttr.TO_FLOW_DOCUMENT_ID_ATTR);
        
        // fromAction is null because there is no action method that was called to get the outcome
        NavigationCase navigationCase = null;
        if (toFlowDocumentId == null)
        {
            navigationCase = navigationHandler.getNavigationCase(
                facesContext, null, outcome);
        }
        else
        {
            navigationCase = navigationHandler.getNavigationCase(
                facesContext, null, outcome, toFlowDocumentId);            
        }
        
        // when navigation case is null, force the link or button to be disabled and log a warning
        if (navigationCase == null)
        {
            // log a warning
            log.warning("Could not determine NavigationCase for UIOutcomeTarget component "
                    + RendererUtils.getPathToComponent(component) + " with outcome " + outcome);

            return null;
        }
        Map<String, List<String>> parameters = null;
        // handle URL parameters
        if (component.getChildCount() > 0)
        {
            List<UIParameter> validParams = getValidUIParameterChildren(
                    facesContext, component.getChildren(), true, false);
            if (validParams.size() > 0)
            {
                parameters = new HashMap<String, List<String>>();
            }
            for (int i = 0, size = validParams.size(); i < size; i++)
            {
                UIParameter param = validParams.get(i);
                String name = param.getName();
                Object value = param.getValue();
                if (parameters.containsKey(name))
                {
                    parameters.get(name).add(value.toString());
                }
                else
                {
                    List<String> list = new ArrayList<String>(1);
                    list.add(value.toString());
                    parameters.put(name, list);
                }
            }
        }
        
        // From the navigation case, use getToFlowDocumentId() to identify when
        // a navigation case is a flow call or a flow return.
        if (navigationCase.getToFlowDocumentId() != null)
        {
            if (parameters == null)
            {
                parameters = new HashMap<String, List<String>>();
            }
            if (!parameters.containsKey(FlowHandler.TO_FLOW_DOCUMENT_ID_REQUEST_PARAM_NAME))
            {
                List<String> list = new ArrayList<String>(1);
                list.add(navigationCase.getToFlowDocumentId());
                parameters.put(FlowHandler.TO_FLOW_DOCUMENT_ID_REQUEST_PARAM_NAME, list);
            }
            if (!parameters.containsKey(FlowHandler.FLOW_ID_REQUEST_PARAM_NAME))
            {
                List<String> list2 = new ArrayList<String>(1);
                list2.add(navigationCase.getFromOutcome());
                parameters.put(FlowHandler.FLOW_ID_REQUEST_PARAM_NAME, list2);
            }
        }
        
        // handle NavigationCase parameters
        Map<String, List<String>> navigationCaseParams = 
            NavigationUtils.getEvaluatedNavigationParameters(facesContext,
                navigationCase.getParameters());
        if (navigationCaseParams != null)
        {
            if (parameters == null)
            {
                parameters = new HashMap<String, List<String>>();
            }
            //parameters.putAll(navigationCaseParams);
            for (Map.Entry<String, List<String>> entry : navigationCaseParams
                    .entrySet())
            {
                if (!parameters.containsKey(entry.getKey()))
                {
                    parameters.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (parameters == null)
        {
            parameters = Collections.emptyMap();
        }
        boolean disableClientWindow = component.isDisableClientWindow();
        ClientWindow clientWindow = facesContext.getExternalContext().getClientWindow();
        String href;
        try
        {
            if (clientWindow != null && disableClientWindow)
            {
                clientWindow.disableClientWindowRenderMode(facesContext);
            }
            // In theory the precedence order to deal with params is this:
            // component parameters, navigation-case parameters, view parameters
            // getBookmarkableURL deal with this details.
            ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
            href = viewHandler.getBookmarkableURL(facesContext,
                    navigationCase.getToViewId(facesContext),
                    parameters, navigationCase.isIncludeViewParams() || component.isIncludeViewParams());
        }
        finally
        {
            if (clientWindow != null && disableClientWindow)
            {
                clientWindow.enableClientWindowRenderMode(facesContext);
            }
        }
        // handle fragment (viewId#fragment)
        String fragment = (String) component.getAttributes().get("fragment");
        if (fragment != null)
        {
            fragment = fragment.trim();

            if (fragment.length() > 0)
            {
                href += "#" + fragment;
            }
        }
        return href;
    }

    /**
     * Calls getValidUIParameterChildren(facesContext, children, skipNullValue, skipUnrendered, true);
     *
     * @param facesContext
     * @param children
     * @param skipNullValue
     * @param skipUnrendered
     * @return ArrayList size > 0 if any parameter found
     */
    public static List<UIParameter> getValidUIParameterChildren(
            FacesContext facesContext, List<UIComponent> children,
            boolean skipNullValue, boolean skipUnrendered)
    {
        return getValidUIParameterChildren(facesContext, children,
                skipNullValue, skipUnrendered, true);
    }
    
    
    /**
     * Returns a List of all valid UIParameter children from the given children.
     * Valid means that the UIParameter is not disabled, its name is not null
     * (if skipNullName is true), its value is not null (if skipNullValue is true)
     * and it is rendered (if skipUnrendered is true). This method also creates a
     * warning for every UIParameter with a null-name (again, if skipNullName is true)
     * and, if ProjectStage is Development and skipNullValue is true, it informs the
     * user about every null-value.
     *
     * @param facesContext
     * @param children
     * @param skipNullValue  should UIParameters with a null value be skipped
     * @param skipUnrendered should UIParameters with isRendered() returning false be skipped
     * @param skipNullName   should UIParameters with a null name be skipped
     *                       (normally true, but in the case of h:outputFormat false)
     * @return ArrayList size > 0 if any parameter found 
     */
    public static List<UIParameter> getValidUIParameterChildren(
            FacesContext facesContext, List<UIComponent> children,
            boolean skipNullValue, boolean skipUnrendered, boolean skipNullName)
    {
        List<UIParameter> params = null;
        for (int i = 0, size = children.size(); i < size; i++)
        {
            UIComponent child = children.get(i);
            if (child instanceof UIParameter)
            {
                UIParameter param = (UIParameter) child;
                // check for the disable attribute (since 2.0)
                // and the render attribute (only if skipUnrendered is true)
                if (param.isDisable() || (skipUnrendered && !param.isRendered()))
                {
                    // ignore this UIParameter and continue
                    continue;
                }
                // check the name
                String name = param.getName();
                if (skipNullName && (name == null || HtmlRendererUtils.STR_EMPTY.equals(name)))
                {
                    // warn for a null-name
                    log.log(Level.WARNING, "The UIParameter " + RendererUtils.getPathToComponent(param)
                                    + " has a name of null or empty string and thus will not be added to the URL.");
                    // and skip it
                    continue;
                }
                // check the value
                if (skipNullValue && param.getValue() == null)
                {
                    if (facesContext.isProjectStage(ProjectStage.Development))
                    {
                        // inform the user about the null value when in Development stage
                        log.log(Level.INFO, "The UIParameter " + RendererUtils.getPathToComponent(param)
                                        + " has a value of null and thus will not be added to the URL.");
                    }
                    // skip a null-value
                    continue;
                }
                // add the param
                if (params == null)
                {
                    params = new ArrayList<UIParameter>();
                }
                params.add(param);
            }
        }
        if (params == null)
        {
            params = Collections.emptyList();
        }
        return params;
    }

}
