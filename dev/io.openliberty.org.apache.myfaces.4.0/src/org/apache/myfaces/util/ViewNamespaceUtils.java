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
package org.apache.myfaces.util;

import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.render.ResponseStateManager;
import jakarta.servlet.ServletRequest;
import java.util.Map;

public class ViewNamespaceUtils
{
    protected static final String CACHE_ATTR = "myfaces.viewnamespace";
    
    public static String getViewNamespace(FacesContext context)
    {
        return getViewNamespace(context, (ServletRequest) context.getExternalContext().getRequest());
    }
    
    /**
     * Gets the view namespace of the current viewroot.
     * This is a porlet feature, where multiple faces view exists on one portlet view.
     *
     * @param context
     * @param request
     * @return 
     */
    public static String getViewNamespace(FacesContext context, ServletRequest request)
    {
        UIViewRoot viewRoot = context.getViewRoot();

        // not yet present, we are in a postback phase, without a ViewRoot yet present
        // the state that we have to determine a naming container before view root buildup
        // can only happen during postback and before a ViewRoot buildup, not during a page get request.
        // We always will have a ViewState being sent with the request in this case.
        // The cases, where we trigger this from outside, are always before ViewRoot buildup
        // we omit this code after we have a ViewRoot, because theoretically the naming container can change.
        // Practically it won´t. But during postback before the RestoreViewRoot we always
        // work on the postback naming container name.
        if (viewRoot == null)
        {
            if (context.getAttributes().containsKey(CACHE_ATTR))
            {
                return (String) context.getAttributes().get(CACHE_ATTR);
            }

            // actually we use the ExternalContext here but this contains already the viewNamespace wrapping logic
            // so must use the native request here
            Map<String, String[]> requestParameterMap = request.getParameterMap();

            String viewNamespace = "";
            if (!requestParameterMap.containsKey(ResponseStateManager.VIEW_STATE_PARAM))
            {
                viewNamespace = resolvePrefixFromRequest(context, requestParameterMap);
            }

            context.getAttributes().put(CACHE_ATTR, viewNamespace);
            return viewNamespace;
        }

        if (viewRoot instanceof NamingContainer)
        {
            return viewRoot.getContainerClientId(context) + UINamingContainer.getSeparatorChar(context);
        }
        return "";
    }

    protected static String resolvePrefixFromRequest(FacesContext facesContext,
            Map<String, String[]> requestParameterMap)
    {
        String firstViewStateKey = requestParameterMap.keySet().stream()
                .filter(item -> item.contains(ResponseStateManager.VIEW_STATE_PARAM))
                .findFirst().orElse("");
        if (!firstViewStateKey.isEmpty())
        {
            char sep = facesContext.getNamingContainerSeparatorChar();
            firstViewStateKey = firstViewStateKey.split(String.valueOf(sep))[0] + sep;
        }
        return firstViewStateKey;
    }
}
