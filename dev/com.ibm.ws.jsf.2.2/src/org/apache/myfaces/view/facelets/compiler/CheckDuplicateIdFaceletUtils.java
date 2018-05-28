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
package org.apache.myfaces.view.facelets.compiler;

import java.util.HashSet;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.view.Location;

import org.apache.myfaces.shared.renderkit.RendererUtils;

/**
 *
 * @author Leonardo Uribe
 */
public final class CheckDuplicateIdFaceletUtils
{
    
    public static void checkIdsStatefulComponents (FacesContext context, UIComponent view)
    {
        checkIdsStatefulComponents (context, view, new HashSet<String>());
    }

    private static void checkIdsStatefulComponents (FacesContext context, 
            UIComponent component, Set<String> existingIds)
    {
        String id;
        
        if (component == null)
        {
            return;
        }
        
        // Need to use this form of the client ID method so we generate the client-side ID.
        
        id = component.getClientId (context);
        
        if (!existingIds.add (id))
        {
            DuplicateIdException duplicateIdException = createAndQueueException(context, component, id);
            throw duplicateIdException;
        }
        
        int facetCount = component.getFacetCount();
        if (facetCount > 0)
        {
            for (UIComponent facet : component.getFacets().values())
            {
                if (!(facet instanceof UILeaf))
                {
                    checkIdsStatefulComponents (context, facet, existingIds);
                }
            }
        }
        for (int i = 0, childCount = component.getChildCount(); i < childCount; i++)
        {
            UIComponent child = component.getChildren().get(i);
            if (!(child instanceof UILeaf))
            {
                checkIdsStatefulComponents (context, child, existingIds);
            }
        }
    }

    public static void checkIds (FacesContext context, UIComponent view)
    {
        checkIds (context, view, new HashSet<String>());
    }
    
    private static void checkIds (FacesContext context, UIComponent component, Set<String> existingIds)
    {
        String id;
        
        if (component == null)
        {
            return;
        }
        
        // Need to use this form of the client ID method so we generate the client-side ID.
        
        id = component.getClientId (context);
        
        if (!existingIds.add (id))
        {
            DuplicateIdException duplicateIdException = createAndQueueException(context, component, id);
            throw duplicateIdException;
        }
        
        int facetCount = component.getFacetCount();
        if (facetCount > 0)
        {
            for (UIComponent facet : component.getFacets().values())
            {
                checkIds (context, facet, existingIds);
            }
        }
        for (int i = 0, childCount = component.getChildCount(); i < childCount; i++)
        {
            UIComponent child = component.getChildren().get(i);
            checkIds (context, child, existingIds);
        }
    }

    private static DuplicateIdException createAndQueueException(FacesContext context, UIComponent component, String id)
    {
        String message = "Component with duplicate id \"" + id + "\" found. The first component is ";

        
        // We report as problematic the second component. The client (an exception handler mostly)
        // has the possibility to report all about the second component, 
        // but the first component is hard to find, especially in large view with tons of naming containers
        // So we do here two things:
        // 1) provide an info about the first component in exception message 
        UIComponent firstComponent = context.getViewRoot().findComponent(id);
        Location location = (Location) firstComponent.getAttributes().get(UIComponent.VIEW_LOCATION_KEY);
        if (location != null)
        {
            message += location.toString();
        }
        else
        {
            // location is not available in production mode or if the component
            // doesn't come from Facelets VDL.
            message += RendererUtils.getPathToComponent(firstComponent);
        }
        
        // 2) we store the first commponent in exception attributes
        DuplicateIdException duplicateIdException = new DuplicateIdException 
                (message, firstComponent, component);
        
        ExceptionQueuedEventContext exceptionContext 
        = new ExceptionQueuedEventContext(context, duplicateIdException,
                component, context.getCurrentPhaseId());

        
        context.getApplication().publishEvent(context, ExceptionQueuedEvent.class, exceptionContext);
        return duplicateIdException;
    }
}
