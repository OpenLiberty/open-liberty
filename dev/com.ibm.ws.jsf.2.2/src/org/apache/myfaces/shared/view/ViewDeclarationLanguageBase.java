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
package org.apache.myfaces.shared.view;

import javax.faces.application.Application;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;

import org.apache.myfaces.shared.application.InvalidViewIdException;

/**
 * @since 2.0
 */
public abstract class ViewDeclarationLanguageBase extends ViewDeclarationLanguage
{
    
    /**
     * Process the specification required algorithm that is generic to all PDL.
     * 
     * @param context
     * @param viewId
     */
    public UIViewRoot createView(FacesContext context, String viewId)
    {
        checkNull(context, "context");
        //checkNull(viewId, "viewId");

        try
        {
            viewId = calculateViewId(context, viewId);
            
            Application application = context.getApplication();

            // Create a new UIViewRoot object instance using Application.createComponent(UIViewRoot.COMPONENT_TYPE).
            UIViewRoot newViewRoot = (UIViewRoot) application.createComponent(
                context, UIViewRoot.COMPONENT_TYPE, null);
            UIViewRoot oldViewRoot = context.getViewRoot();
            if (oldViewRoot == null)
            {
                // If not, this method must call calculateLocale() and calculateRenderKitId(), and store the results
                // as the values of the locale and renderKitId, proeprties, respectively, of the newly created
                // UIViewRoot.
                ViewHandler handler = application.getViewHandler();
                newViewRoot.setLocale(handler.calculateLocale(context));
                newViewRoot.setRenderKitId(handler.calculateRenderKitId(context));
            }
            else
            {
                // If there is an existing UIViewRoot available on the FacesContext, this method must copy its locale
                // and renderKitId to this new view root
                newViewRoot.setLocale(oldViewRoot.getLocale());
                newViewRoot.setRenderKitId(oldViewRoot.getRenderKitId());
            }
            
            // TODO: VALIDATE - The spec is silent on the following line, but I feel bad if I don't set it
            newViewRoot.setViewId(viewId);

            return newViewRoot;
        }
        catch (InvalidViewIdException e)
        {
            // If no viewId could be identified, or the viewId is exactly equal to the servlet mapping, 
            // send the response error code SC_NOT_FOUND with a suitable message to the client.
            sendSourceNotFound(context, e.getMessage());
            
            // TODO: VALIDATE - Spec is silent on the return value when an error was sent
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UIViewRoot restoreView(FacesContext context, String viewId)
    {
        checkNull(context, "context");
        //checkNull(viewId, "viewId");

        Application application = context.getApplication();
        
        ViewHandler applicationViewHandler = application.getViewHandler();
        
        String renderKitId = applicationViewHandler.calculateRenderKitId(context);

        UIViewRoot viewRoot = application.getStateManager().restoreView(context, viewId, renderKitId);

        return viewRoot;
    }

    /**
     * Calculates the effective view identifier for the specified raw view identifier.
     * 
     * @param context le current FacesContext
     * @param viewId the raw view identifier
     * 
     * @return the effective view identifier
     */
    protected abstract String calculateViewId(FacesContext context, String viewId);
    
    /**
     * Send a source not found to the client. Although it can be considered ok in JSP mode,
     * I think it's pretty lame to have this kind of requirement at VDL level considering VDL 
     * represents the page --> JSF tree link, not the transport layer required to send a 
     * SC_NOT_FOUND.
     * 
     * @param context le current FacesContext
     * @param message the message associated with the error
     */
    protected abstract void sendSourceNotFound(FacesContext context, String message);
    
    /**
     * Check if the specified value of a param is <code>null</code>.
     * 
     * @param o the parameter's value
     * @param param the parameter's name
     * 
     * @throws NullPointerException if the value is <code>null</code>
     */
    protected void checkNull(final Object o, final String param)
    {
        if (o == null)
        {
            throw new NullPointerException(param + " can not be null.");
        }
    }
}
