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

import java.io.IOException;
import java.util.Map;
import javax.faces.FacesException;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.view.facelets.Facelet;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.view.facelets.AbstractFacelet;
import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage;
import org.apache.myfaces.view.facelets.tag.composite.CompositeResourceLibrary;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;

/**
 * This listener must be attached to PostRestoreStateEvent so when the view is restored, 
 * the algorithm refresh the component that was created using facelets, restoring the
 * transient markup.
 *
 * @author lu4242
 */
public final class RefreshDynamicComponentListener implements 
    ComponentSystemEventListener, StateHolder
{
    private String taglibURI;
    private String tagName;
    private Map<String,Object> attributes;
    private String baseKey;

    public RefreshDynamicComponentListener(String taglibURI, String tagName, 
        Map<String, Object> attributes, String baseKey)
    {
        this.taglibURI = taglibURI;
        this.tagName = tagName;
        this.attributes = attributes;
        this.baseKey = baseKey;
    }

    public RefreshDynamicComponentListener()
    {
    }
    
    public void processEvent(ComponentSystemEvent event)
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        //if (!PhaseId.RESTORE_VIEW.equals(facesContext.getCurrentPhaseId()))
        //{
            // This listener is only active when PostAddToViewEvent occur
            // on restore view phase. 
            //return;
        //}
        
        FaceletViewDeclarationLanguage vdl = (FaceletViewDeclarationLanguage) 
            facesContext.getApplication().getViewHandler().getViewDeclarationLanguage(
                facesContext, facesContext.getViewRoot().getViewId());
        
        Facelet componentFacelet;
        FaceletFactory faceletFactory = vdl.getFaceletFactory();
            
        FaceletFactory.setInstance(faceletFactory);
        try
        {
            componentFacelet
                    = faceletFactory.compileComponentFacelet(taglibURI, tagName, attributes);
        }
        finally
        {
            FaceletFactory.setInstance(null);
        }
        try
        {
            UIComponent component = event.getComponent();
            
            facesContext.getAttributes().put(FaceletViewDeclarationLanguage.REFRESHING_TRANSIENT_BUILD,
                Boolean.TRUE);
            
            
            // Detect the relationship between parent and child, to ensure the component is properly created
            // and refreshed. In facelets this is usually done by core.FacetHandler, but since it is a 
            // dynamic component, we need to do it here before apply the handler
            UIComponent parent = component.getParent();
            String facetName = null;
            if (parent.getFacetCount() > 0 && !parent.getChildren().contains(component))
            {
                facetName = ComponentSupport.findFacetNameByComponentInstance(parent, component);
            }

            try
            {
                if (facetName != null)
                {
                    parent.getAttributes().put(org.apache.myfaces.view.facelets.tag.jsf.core.FacetHandler.KEY, 
                            facetName);
                }            
                // The trick here is restore MARK_CREATED, just to allow ComponentTagHandlerDelegate to
                // find the component. Then we reset it to exclude it from facelets refresh algorithm.
                // Note oam.vf.GEN_MARK_ID helps to identify when there is a wrapper component or not,
                // and in that way identify which component is the parent.
                String markId = (String) component.getAttributes().get("oam.vf.GEN_MARK_ID");
                if (markId == null)
                {
                    ((AbstractFacelet)componentFacelet).applyDynamicComponentHandler(
                        facesContext, component, baseKey);
                }
                else
                {
                    try
                    {
                        component.getAttributes().put(ComponentSupport.MARK_CREATED, markId);
                        ((AbstractFacelet)componentFacelet).applyDynamicComponentHandler(
                            facesContext, component.getParent(), baseKey);
                    }
                    finally
                    {
                        component.getAttributes().put(ComponentSupport.MARK_CREATED, null);
                    }
                }
            }
            finally
            {
                if (facetName != null)
                {
                    parent.getAttributes().remove(org.apache.myfaces.view.facelets.tag.jsf.core.FacetHandler.KEY);
                }
            }
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
        finally
        {
            facesContext.getAttributes().remove(
                FaceletViewDeclarationLanguage.REFRESHING_TRANSIENT_BUILD);
        }
    }

    public Object saveState(FacesContext context)
    {
        RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(
            context.getExternalContext());
        Object[] values = new Object[4];
        Integer tagId = runtimeConfig.getIdByNamespace().get(taglibURI);
        if (tagId != null)
        {
            values[0] = tagId;
        }
        else if (taglibURI.startsWith(CompositeResourceLibrary.NAMESPACE_PREFIX))
        {
            values[0] = new Object[]{0, taglibURI.substring(35)};
        }
        else if(taglibURI.startsWith(CompositeResourceLibrary.ALIAS_NAMESPACE_PREFIX))
        {
            values[0] = new Object[]{1, taglibURI.substring(34)};
        }
        else
        {
            values[0] = taglibURI;
        }
        values[1] = tagName;
        values[2] = attributes;
        values[3] = baseKey;
        return values;
    }

    public void restoreState(FacesContext context, Object state)
    {
        Object[] values = (Object[]) state;
        if (values[0] instanceof String)
        {
            taglibURI = (String) values[0];
        }
        else if (values[0] instanceof Integer)
        {
            RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(
                context.getExternalContext());
            taglibURI = runtimeConfig.getNamespaceById().get((Integer)values[0]);
        }
        else if (values[0] instanceof Object[])
        {
            Object[] def = (Object[])values[0];
            String ns = ( ((Integer)def[0]).intValue() == 0) ? 
                CompositeResourceLibrary.NAMESPACE_PREFIX :
                CompositeResourceLibrary.ALIAS_NAMESPACE_PREFIX;
            taglibURI = ns + (String)(((Object[])values[0])[1]);
        }
        tagName = (String)values[1];
        attributes = (Map<String,Object>) values[2];
        baseKey = (String)values[3];
    }

    public boolean isTransient()
    {
        return false;
    }

    public void setTransient(boolean newTransientValue)
    {
    }

}
