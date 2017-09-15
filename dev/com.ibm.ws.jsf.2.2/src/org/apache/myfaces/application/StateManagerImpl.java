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
package org.apache.myfaces.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FactoryFinder;
import javax.faces.application.StateManager;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.ResponseStateManager;
import javax.faces.view.StateManagementStrategy;
import javax.faces.view.ViewDeclarationLanguage;

import org.apache.myfaces.application.viewstate.StateCacheUtils;
import org.apache.myfaces.context.RequestViewContext;

public class StateManagerImpl extends StateManager
{
    private static final Logger log = Logger.getLogger(StateManagerImpl.class.getName());
    
    private static final String SERIALIZED_VIEW_REQUEST_ATTR = 
        StateManagerImpl.class.getName() + ".SERIALIZED_VIEW";
    
    private RenderKitFactory _renderKitFactory = null;
    
    public StateManagerImpl()
    {
    }

    @Override
    protected Object getComponentStateToSave(FacesContext facesContext)
    {
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Entering getComponentStateToSave");
        }

        UIViewRoot viewRoot = facesContext.getViewRoot();
        if (viewRoot.isTransient())
        {
            return null;
        }

        Object serializedComponentStates = viewRoot.processSaveState(facesContext);
        //Locale is a state attribute of UIViewRoot and need not be saved explicitly
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Exiting getComponentStateToSave");
        }
        return serializedComponentStates;
    }

    /**
     * Return an object which contains info about the UIComponent type
     * of each node in the view tree. This allows an identical UIComponent
     * tree to be recreated later, though all the components will have
     * just default values for their members.
     */
    @Override
    protected Object getTreeStructureToSave(FacesContext facesContext)
    {
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Entering getTreeStructureToSave");
        }
        UIViewRoot viewRoot = facesContext.getViewRoot();
        if (viewRoot.isTransient())
        {
            return null;
        }
        TreeStructureManager tsm = new TreeStructureManager();
        Object retVal = tsm.buildTreeStructureToSave(viewRoot);
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Exiting getTreeStructureToSave");
        }
        return retVal;
    }

    @Override
    public UIViewRoot restoreView(FacesContext facesContext, String viewId, String renderKitId)
    {
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Entering restoreView - viewId: " + viewId + " ; renderKitId: " + renderKitId);
        }

        UIViewRoot uiViewRoot = null;
        
        ViewDeclarationLanguage vdl = facesContext.getApplication().
            getViewHandler().getViewDeclarationLanguage(facesContext,viewId);
        StateManagementStrategy sms = null; 
        if (vdl != null)
        {
            sms = vdl.getStateManagementStrategy(facesContext, viewId);
        }
        
        if (sms != null)
        {
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("Redirect to StateManagementStrategy: " + sms.getClass().getName());
            }
            
            uiViewRoot = sms.restoreView(facesContext, viewId, renderKitId);
        }
        else
        {
            RenderKit renderKit = getRenderKitFactory().getRenderKit(facesContext, renderKitId);
            ResponseStateManager responseStateManager = renderKit.getResponseStateManager();

            Object state = responseStateManager.getState(facesContext, viewId);

            if (state != null)
            {
                Object[] stateArray = (Object[])state;
                TreeStructureManager tsm = new TreeStructureManager();
                
                uiViewRoot = tsm.restoreTreeStructure(((Object[])stateArray[0])[0]);

                if (uiViewRoot != null)
                {
                    facesContext.setViewRoot (uiViewRoot);
                    uiViewRoot.processRestoreState(facesContext, stateArray[1]);
                    
                    RequestViewContext.getCurrentInstance(facesContext).refreshRequestViewContext(
                            facesContext, uiViewRoot);
                    
                    // If state is saved fully, there outer f:view tag handler will not be executed,
                    // so "contracts" attribute will not be set properly. We need to save it and
                    // restore it from here. With PSS, the view will always be built so it is not
                    // necessary to save it on the state.
                    Object rlc = ((Object[])stateArray[0])[1];
                    if (rlc != null)
                    {
                        facesContext.setResourceLibraryContracts((List) UIComponentBase.
                            restoreAttachedState(facesContext, rlc));
                    }
                }
            }            
        }
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Exiting restoreView - " + viewId);
        }

        return uiViewRoot;
    }

    /**
     * Wrap the original method and redirect to VDL StateManagementStrategy when
     * necessary
     */
    @Override
    public Object saveView(FacesContext facesContext)
    {
        UIViewRoot uiViewRoot = facesContext.getViewRoot();
        
        if (uiViewRoot.isTransient())
        {
            return null;
        }
        
        Object serializedView = null;
        ResponseStateManager responseStateManager = facesContext.getRenderKit().getResponseStateManager();
        
        String viewId = uiViewRoot.getViewId();
        ViewDeclarationLanguage vdl = facesContext.getApplication().
            getViewHandler().getViewDeclarationLanguage(facesContext,viewId);
        
        try
        {
            facesContext.getAttributes().put(StateManager.IS_SAVING_STATE, Boolean.TRUE);
            if (vdl != null)
            {
                StateManagementStrategy sms = vdl.getStateManagementStrategy(facesContext, viewId);
                
                if (sms != null)
                {
                    if (log.isLoggable(Level.FINEST))
                    {
                        log.finest("Calling saveView of StateManagementStrategy: " + sms.getClass().getName());
                    }
                    
                    serializedView = sms.saveView(facesContext);
                    
                    // If MyfacesResponseStateManager is used, give the option to do
                    // additional operations for save the state if is necessary.
                    if (StateCacheUtils.isMyFacesResponseStateManager(responseStateManager))
                    {
                        StateCacheUtils.getMyFacesResponseStateManager(responseStateManager).
                                saveState(facesContext, serializedView);
                    }
                    
                    return serializedView; 
                }
            }
    
            // In StateManagementStrategy.saveView there is a check for transient at
            // start, but the same applies for VDL without StateManagementStrategy,
            // so this should be checked before call parent (note that parent method
            // does not do this check).
            if (uiViewRoot.isTransient())
            {
                return null;
            }
    
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("Entering saveSerializedView");
            }
    
            checkForDuplicateIds(facesContext, facesContext.getViewRoot(), new HashSet<String>());
    
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("Processing saveSerializedView - Checked for duplicate Ids");
            }
    
            // SerializedView already created before within this request?
            serializedView = facesContext.getAttributes().get(SERIALIZED_VIEW_REQUEST_ATTR);
            if (serializedView == null)
            {
                if (log.isLoggable(Level.FINEST))
                {
                    log.finest("Processing saveSerializedView - create new serialized view");
                }
    
                // first call to saveSerializedView --> create SerializedView
                Object treeStruct = getTreeStructureToSave(facesContext);
                Object compStates = getComponentStateToSave(facesContext);
                Object rlcStates = !facesContext.getResourceLibraryContracts().isEmpty() ? 
                    UIComponentBase.saveAttachedState(facesContext, 
                                new ArrayList<String>(facesContext.getResourceLibraryContracts())) : null;
                serializedView = new Object[] {
                        new Object[]{treeStruct, rlcStates} ,
                        compStates};
                facesContext.getAttributes().put(SERIALIZED_VIEW_REQUEST_ATTR,
                                                    serializedView);
    
                if (log.isLoggable(Level.FINEST))
                {
                    log.finest("Processing saveSerializedView - new serialized view created");
                }
            }
            
            // If MyfacesResponseStateManager is used, give the option to do
            // additional operations for save the state if is necessary.
            if (StateCacheUtils.isMyFacesResponseStateManager(responseStateManager))
            {
                StateCacheUtils.getMyFacesResponseStateManager(responseStateManager).
                        saveState(facesContext, serializedView);
            }
    
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("Exiting saveView");
            }
        }
        finally
        {
            facesContext.getAttributes().remove(StateManager.IS_SAVING_STATE);
        }

        return serializedView;
    }

    private static void checkForDuplicateIds(FacesContext context,
                                             UIComponent component,
                                             Set<String> ids)
    {
        String id = component.getId();
        if (id != null && !ids.add(id))
        {
            throw new IllegalStateException("Client-id : " + id +
                                            " is duplicated in the faces tree. Component : " + 
                                            component.getClientId(context)+", path: " +
                                            getPathToComponent(component));
        }
        
        if (component instanceof NamingContainer)
        {
            ids = new HashSet<String>();
        }
        
        int facetCount = component.getFacetCount();
        if (facetCount > 0)
        {
            for (UIComponent facet : component.getFacets().values())
            {
                checkForDuplicateIds (context, facet, ids);
            }
        }
        for (int i = 0, childCount = component.getChildCount(); i < childCount; i++)
        {
            UIComponent child = component.getChildren().get(i);
            checkForDuplicateIds (context, child, ids);
        }
    }

    private static String getPathToComponent(UIComponent component)
    {
        StringBuffer buf = new StringBuffer();

        if(component == null)
        {
            buf.append("{Component-Path : ");
            buf.append("[null]}");
            return buf.toString();
        }

        getPathToComponent(component,buf);

        buf.insert(0,"{Component-Path : ");
        buf.append("}");

        return buf.toString();
    }

    private static void getPathToComponent(UIComponent component, StringBuffer buf)
    {
        if(component == null)
        {
            return;
        }

        StringBuffer intBuf = new StringBuffer();

        intBuf.append("[Class: ");
        intBuf.append(component.getClass().getName());
        if(component instanceof UIViewRoot)
        {
            intBuf.append(",ViewId: ");
            intBuf.append(((UIViewRoot) component).getViewId());
        }
        else
        {
            intBuf.append(",Id: ");
            intBuf.append(component.getId());
        }
        intBuf.append("]");

        buf.insert(0,intBuf.toString());

        getPathToComponent(component.getParent(),buf);
    }

    @Override
    public void writeState(FacesContext facesContext,
                           Object state) throws IOException
    {
        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Entering writeState");
        }

        //UIViewRoot uiViewRoot = facesContext.getViewRoot();
        //save state in response (client)
        RenderKit renderKit = facesContext.getRenderKit();
        ResponseStateManager responseStateManager = renderKit.getResponseStateManager();

        responseStateManager.writeState(facesContext, state);

        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Exiting writeState");
        }

    }

    //helpers

    protected RenderKitFactory getRenderKitFactory()
    {
        if (_renderKitFactory == null)
        {
            _renderKitFactory = (RenderKitFactory)FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
        }
        return _renderKitFactory;
    }

}
