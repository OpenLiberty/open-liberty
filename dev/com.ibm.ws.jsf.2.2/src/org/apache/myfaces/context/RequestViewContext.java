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
package org.apache.myfaces.context;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.faces.application.ResourceDependency;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;

/**
 *
 * @author  Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1550609 $ $Date: 2013-12-13 01:18:08 +0000 (Fri, 13 Dec 2013) $
 * @since 2.0.2
 */
public class RequestViewContext
{

    public static final String VIEW_CONTEXT_KEY = "oam.VIEW_CONTEXT";
    
    public static final String RESOURCE_DEPENDENCY_INSPECTED_CLASS = "oam.RDClass";
    
    private static final String SKIP_ITERATION_HINT = "javax.faces.visit.SKIP_ITERATION";
    
    private static final Set<VisitHint> VISIT_HINTS = Collections.unmodifiableSet( 
            EnumSet.of(VisitHint.SKIP_ITERATION));

    private RequestViewMetadata requestViewMetadata;
    
    private Map<String, Boolean> renderTargetMap = null;
    
    public RequestViewContext()
    {
        this.requestViewMetadata = new RequestViewMetadata();
    }
    
    public RequestViewContext(RequestViewMetadata rvm)
    {
        this.requestViewMetadata = new RequestViewMetadata();
    }

    static public RequestViewContext getCurrentInstance()
    {
        FacesContext ctx = FacesContext.getCurrentInstance();
        return getCurrentInstance(ctx);
    }
    
    static public RequestViewContext getCurrentInstance(FacesContext ctx)
    {
        return getCurrentInstance(ctx, ctx.getViewRoot());
    }
    
    @SuppressWarnings("unchecked")
    static public RequestViewContext getCurrentInstance(FacesContext ctx, UIViewRoot root)
    {
        Map<UIViewRoot, RequestViewContext> map
                = (Map<UIViewRoot, RequestViewContext>) ctx.getAttributes().get(VIEW_CONTEXT_KEY);
        RequestViewContext rvc = null;        
        if (map == null)
        {
            map = new HashMap<UIViewRoot, RequestViewContext>();
            rvc = new RequestViewContext();
            map.put(root, rvc);
            ctx.getAttributes().put(VIEW_CONTEXT_KEY, map);
            return rvc;
        }
        else
        {
            rvc = map.get(root); 
            if (rvc == null)
            {
                rvc = new RequestViewContext();
                map.put(root, rvc);
            }
            return rvc;
        }
    }
    
    static public RequestViewContext getCurrentInstance(FacesContext ctx, UIViewRoot root, boolean create)
    {
        if (create)
        {
            return getCurrentInstance(ctx, root);
        }
        Map<UIViewRoot, RequestViewContext> map
                = (Map<UIViewRoot, RequestViewContext>) ctx.getAttributes().get(VIEW_CONTEXT_KEY);
        if (map != null)
        {
            return map.get(root);
        }
        return null;
    }
    
    static public RequestViewContext newInstance(RequestViewMetadata rvm)
    {
        RequestViewContext clone = new RequestViewContext(rvm.cloneInstance());
        return clone;
    }
    
    static public void setCurrentInstance(FacesContext ctx, UIViewRoot root, RequestViewContext rvc)
    {
        Map<UIViewRoot, RequestViewContext> map
                = (Map<UIViewRoot, RequestViewContext>) ctx.getAttributes().get(VIEW_CONTEXT_KEY);
        if (map == null)
        {
            map = new HashMap<UIViewRoot, RequestViewContext>();
            rvc = new RequestViewContext();
            map.put(root, rvc);
            ctx.getAttributes().put(VIEW_CONTEXT_KEY, map);
        }
        else
        {
            map.put(root, rvc);
        }
    }

    public boolean isResourceDependencyAlreadyProcessed(ResourceDependency dependency)
    {
        return requestViewMetadata.isResourceDependencyAlreadyProcessed(dependency);
    }
    
    public void setResourceDependencyAsProcessed(ResourceDependency dependency)
    {
        requestViewMetadata.setResourceDependencyAsProcessed(dependency);
    }

    public boolean isClassAlreadyProcessed(Class<?> inspectedClass)
    {
        return requestViewMetadata.isClassAlreadyProcessed(inspectedClass);
    }

    public void setClassProcessed(Class<?> inspectedClass)
    {
        requestViewMetadata.setClassProcessed(inspectedClass);
    }
    
    public boolean isRenderTarget(String target)
    {
        if (renderTargetMap != null)
        {
            return Boolean.TRUE.equals(renderTargetMap.get(target));
        }
        return false;
    }
    
    public void setRenderTarget(String target, boolean value)
    {
        if (renderTargetMap == null)
        {
            renderTargetMap = new HashMap<String, Boolean>(8);
        }
        renderTargetMap.put(target, value);
    }
    
    /**
     * Scans UIViewRoot facets with added component resources by the effect of
     * @ResourceDependency annotation, and register the associated inspected classes
     * so new component resources will not be added to the component tree again and again.
     * 
     * @param facesContext
     * @param root 
     */
    public void refreshRequestViewContext(FacesContext facesContext, UIViewRoot root)
    {
        for (Map.Entry<String, UIComponent> entry : root.getFacets().entrySet())
        {
            UIComponent facet = entry.getValue();
            if (facet.getId() != null && facet.getId().startsWith("javax_faces_location_"))
            {
                try
                {
                    facesContext.getAttributes().put(SKIP_ITERATION_HINT, Boolean.TRUE);

                    VisitContext visitContext = VisitContext.createVisitContext(facesContext, null, VISIT_HINTS);
                    facet.visitTree(visitContext, new RefreshViewContext());
                }
                finally
                {
                    // We must remove hint in finally, because an exception can break this phase,
                    // but lifecycle can continue, if custom exception handler swallows the exception
                    facesContext.getAttributes().remove(SKIP_ITERATION_HINT);
                }
            }
        }
    }
    
    private class RefreshViewContext implements VisitCallback
    {

        public VisitResult visit(VisitContext context, UIComponent target)
        {
            Class<?> inspectedClass = (Class<?>)target.getAttributes().get(RESOURCE_DEPENDENCY_INSPECTED_CLASS);
            if (inspectedClass != null)
            {
                setClassProcessed(inspectedClass);
            }            
            return VisitResult.ACCEPT;
        }
    }
    
    public RequestViewMetadata getRequestViewMetadata()
    {
        return requestViewMetadata;
    }

    /**
     * @param requestViewMetadata the requestViewMetadata to set
     */
    public void setRequestViewMetadata(RequestViewMetadata requestViewMetadata)
    {
        this.requestViewMetadata = requestViewMetadata;
    }
}
