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
package org.apache.myfaces.view.facelets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.application.Application;
import jakarta.faces.application.NavigationHandler;
import jakarta.faces.application.ProjectStage;
import jakarta.faces.application.ResourceDependency;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseId;
import jakarta.faces.view.StateManagementStrategy;
import jakarta.faces.view.ViewDeclarationLanguage;
import org.apache.myfaces.application.StateManagerImpl;
import org.apache.myfaces.component.ComponentResourceContainer;
import org.apache.myfaces.context.RequestViewContext;
import org.apache.myfaces.context.RequestViewMetadata;
import org.apache.myfaces.lifecycle.DefaultRestoreViewSupport;
import org.apache.myfaces.lifecycle.RestoreViewSupport;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.view.facelets.impl.FaceletCompositionContextImpl;
import org.apache.myfaces.view.facelets.pool.ViewPool;
import org.apache.myfaces.view.facelets.pool.ViewPoolFactory;
import org.apache.myfaces.view.facelets.pool.ViewEntry;
import org.apache.myfaces.view.facelets.pool.ViewStructureMetadata;
import org.apache.myfaces.view.facelets.pool.impl.ViewPoolFactoryImpl;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;
import org.apache.myfaces.view.facelets.tag.jsf.FaceletState;

/**
 * This class is reponsible for all processing tasks related to the view pool.
 * 
 * For enable the pool only for a subset of your views, you can
 * add an entry inside faces-config.xml file like this:
 * <pre>
 * {@code 
 * <faces-config-extension>
 *   <view-pool-mapping>
 *      <url-pattern>/*</url-pattern>
 *      <parameter>
 *          <name>org.apache.myfaces.VIEW_POOL_MAX_POOL_SIZE</name>
 *          <value>5</value>
 *      </parameter>
 *  </view-pool-mapping>
 * </faces-config-extension>
 * }
 * </pre>
 * 
 * @author Leonardo Uribe
 */
public class ViewPoolProcessor
{
    /**
     * Used to hold the view pool processor instance on the application map. 
     * A ViewPoolProcessor is only accessible if the view pool has been
     * enabled for the whole application (using a web config parameter) or 
     * partially (an &lt;view-pool-mapping&gt; entry inside &lt;faces-config-extension&gt;).
     */
    private final static String INSTANCE = "oam.ViewPoolProcessor";
    
    /**
     * UIViewRoot attribute to enable/disable the view for use pooling.
     */
    public final static String ENABLE_VIEW_POOL = "oamEnableViewPool";
    
    /**
     * Flag that indicates to the StateManagementStrategy that no state needs 
     * to be stored and we are using saveState() to dispose the view and store it
     * into the pool directly.
     */
    public final static String FORCE_HARD_RESET = "oam.ViewPool.forceHardReset";
    
    /**
     * Flag to indicate that dispose this view on navigation is valid.
     */
    public final static String DISPOSE_VIEW_NAVIGATION = "oam.ViewPool.disposeViewOnNavigation";
    
    /**
     * Attribute of UIViewRoot that indicates if a soft (1) or hard(2) 
     * (reset and check) reset is required in the call to saveState().
     */
    public final static String RESET_SAVE_STATE_MODE_KEY ="oam.view.resetSaveStateMode";
    
    /**
     * Indicates no reset should be done on this state saving
     */
    public static final int RESET_MODE_OFF = 0;
    
    /**
     * Indicates a soft reset should be done when saveState(...) is performed,
     * which means all transient state should be cleared but the delta state 
     * should not be destroyed in the process.
     */
    public static final int RESET_MODE_SOFT = 1;
    
    /**
     * Indicates a hard reset should be done when saveState(...) is performed,
     * which means all transient and delta state should be cleared, destroying
     * all existing state in the process. If something cannot be reseted, the 
     * state should return non null, so the algorithm can remove the component
     * from the tree and mark the tree as partial (requires refresh before
     * reuse).
     */
    public static final int RESET_MODE_HARD = 2;
    
    /**
     * Param used to indicate a "deferred navigation" needs to be done. To allow the view pool to
     * dispose the view properly (and reuse it later), it is necessary to ensure the view is not
     * being used at the moment. If the navigation call occur inside an action listener, the current
     * view is being used and cannot be disposed (because it conflicts with hard/soft reset). This
     * extension allows to call handleNavigation() before end invoke application phase, but after
     * traverse the component tree. 
     */
    public static final String INVOKE_DEFERRED_NAVIGATION = "oam.invoke.navigation";

    private ViewPoolFactory viewPoolFactory;
    private RestoreViewSupport restoreViewSupport;
    
    public ViewPoolProcessor(FacesContext context)
    {
        viewPoolFactory = new ViewPoolFactoryImpl(context);
        restoreViewSupport = new DefaultRestoreViewSupport(context);
    }
    
    public static ViewPoolProcessor getInstance(FacesContext context)
    {
        return (ViewPoolProcessor) context.getExternalContext().
                getApplicationMap().get(INSTANCE);
    }
    
    /**
     * This method should be called at startup to decide if a view processor should be
     * provided or not to the runtime.
     * 
     * @param context 
     */
    public static void initialize(FacesContext context)
    {
        if (context.isProjectStage(ProjectStage.Production))
        {
            boolean initialize = true;
            String elMode = WebConfigParamUtils.getStringInitParameter(
                        context.getExternalContext(),
                        FaceletCompositionContextImpl.INIT_PARAM_CACHE_EL_EXPRESSIONS, 
                            ELExpressionCacheMode.noCache.name());
            if (!elMode.equals(ELExpressionCacheMode.alwaysRecompile.name()))
            {
                Logger.getLogger(ViewPoolProcessor.class.getName()).log(
                    Level.INFO, FaceletCompositionContextImpl.INIT_PARAM_CACHE_EL_EXPRESSIONS +
                    " web config parameter is set to \"" + ( (elMode == null) ? "none" : elMode) +
                    "\". To enable view pooling this param"+
                    " must be set to \"alwaysRecompile\". View Pooling disabled.");
                initialize = false;
            }
            
            long refreshPeriod = WebConfigParamUtils.getLongInitParameter(context.getExternalContext(),
                    FaceletViewDeclarationLanguage.PARAMS_REFRESH_PERIOD,
                    FaceletViewDeclarationLanguage.DEFAULT_REFRESH_PERIOD_PRODUCTION);

            if (refreshPeriod != -1)
            {
                Logger.getLogger(ViewPoolProcessor.class.getName()).log(
                    Level.INFO, ViewHandler.FACELETS_REFRESH_PERIOD_PARAM_NAME +
                    " web config parameter is set to \"" + Long.toString(refreshPeriod) +
                    "\". To enable view pooling this param"+
                    " must be set to \"-1\". View Pooling disabled.");
                initialize = false;
            }
            
            if (MyfacesConfig.getCurrentInstance(context.getExternalContext()).isStrictJsf2FaceletsCompatibility())
            {
                Logger.getLogger(ViewPoolProcessor.class.getName()).log(
                    Level.INFO, MyfacesConfig.INIT_PARAM_STRICT_JSF_2_FACELETS_COMPATIBILITY +
                    " web config parameter is set to \"" + "true" +
                    "\". To enable view pooling this param "+
                    " must be set to \"false\". View Pooling disabled.");
                initialize = false;
            }
            
            if (initialize)
            {
                ViewPoolProcessor processor = new ViewPoolProcessor(context);
                context.getExternalContext().
                    getApplicationMap().put(INSTANCE, processor);
            }
        }
    }

    public ViewPool getViewPool(FacesContext context, UIViewRoot root)
    {
        if (root.isTransient())
        {
            // Stateless views cannot be pooled, because we are reusing
            // state saving algorithm for that.
            return null;
        }
        Boolean enableViewPool = (Boolean) root.getAttributes().get(ViewPoolProcessor.ENABLE_VIEW_POOL);
        if (enableViewPool != null && !Boolean.TRUE.equals(enableViewPool))
        {
            // view pool not enabled for this view.
            return null;
        }
        else
        {
            return viewPoolFactory.getViewPool(context, root);
        }
    }
    
    public boolean isViewPoolEnabledForThisView(FacesContext context, UIViewRoot root)
    {
        if (root == null || root.isTransient())
        {
            // Stateless views cannot be pooled, because we are reusing
            // state saving algorithm for that.
            return false;
        }

        Boolean enableViewPool = (Boolean) root.getAttributes().get(ViewPoolProcessor.ENABLE_VIEW_POOL);
        if (enableViewPool != null)
        {
            if (Boolean.TRUE.equals(enableViewPool))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            ViewPool viewPool = getViewPool(context, root);
            if (viewPool != null)
            {
                return true;
            }
        }

        return false;
    }

    public boolean isViewPoolStrategyAllowedForThisView(FacesContext context, UIViewRoot root)
    {
        // Check if the viewId is not null.
        if (root.getViewId() == null)
        {
            return false;
        }
        if (root.isTransient())
        {
            // Stateless views cannot be pooled, because we are reusing
            // state saving algorithm for that.
            return false;
        }
        
        // Check if the view is enabled or not for use pooling
        if (!isViewPoolEnabledForThisView(context, root))
        {
            return false;
        }

        // Check if the vdl is using PSS on this view (has a vdl and that vdl is facelets)
        ViewDeclarationLanguage vdl = context.getApplication().getViewHandler().
            getViewDeclarationLanguage(context, root.getViewId());
        if (vdl == null)
        {
            return false;
        }
        else if (!ViewDeclarationLanguage.FACELETS_VIEW_DECLARATION_LANGUAGE_ID.equals(vdl.getId()))
        {
            return false;
        }
        
        if (vdl.getStateManagementStrategy(context, root.getViewId()) == null)
        {
            return false;
        }
        return true;
    }

    public void setViewPoolDisabledOnThisView(FacesContext context, UIViewRoot root, boolean value)
    {
        root.getAttributes().put(ViewPoolProcessor.ENABLE_VIEW_POOL, !value);
    }
    
    /**
     * Takes the newView and restore the state taken as base the provided ViewEntry,
     * and then move all child components from oldView to newView, to finally obtain
     * a clean component tree.
     * 
     * @param context
     * @param newView
     * @param entry 
     */
    public void cloneAndRestoreView(FacesContext context, UIViewRoot newView, 
            ViewEntry entry, ViewStructureMetadata metadata)
    {
        UIViewRoot oldView = entry.getViewRoot();
        // retrieveViewRootInitialState(context, oldView)
        Object viewState = metadata.getViewRootState();
        Object newViewState;
        UIComponent metadataFacet = newView.getFacet(UIViewRoot.METADATA_FACET_NAME);
        if (viewState == null)
        {
            // (Optional, it should be always metadata)
            oldView.clearInitialState();
            viewState = oldView.saveState(context);
        }
        Map<String, Object> viewScopeMap = newView.getViewMap(false);
        if (viewScopeMap != null && !viewScopeMap.isEmpty())
        {
            newViewState = newView.saveState(context);
        }
        else
        {
            newViewState = null;
        }
        
        boolean oldProcessingEvents = context.isProcessingEvents();
        context.setProcessingEvents(false);
        try
        {
            if (oldView.getFacetCount() > 0)
            {
                List<String> facetKeys = new ArrayList<String>();
                facetKeys.addAll(oldView.getFacets().keySet());
                for (String facetKey : facetKeys)
                {
                    //context.setProcessingEvents(false);
                    if (metadataFacet != null && UIViewRoot.METADATA_FACET_NAME.equals(facetKey) &&
                        !PhaseId.RESTORE_VIEW.equals(context.getCurrentPhaseId()))
                    {
                        // Metadata facet is special, it is created when ViewHandler.createView(...) is
                        // called, so it shouldn't be taken from the oldView, otherwise the state
                        // will be lost. Instead reuse it and discard the one in oldView.
                        // But on restore view phase, use the old one, because the new one does not
                        // have initial state marked.
                        newView.getFacets().put(facetKey, metadataFacet);
                    }
                    else
                    {
                        UIComponent facet = oldView.getFacets().remove(facetKey);
                        //context.setProcessingEvents(true);
                        newView.getFacets().put(facetKey, facet);
                    }
                }
            }
            if (oldView.getChildCount() > 0)
            {
                for (Iterator<UIComponent> it = oldView.getChildren().iterator(); it.hasNext();)
                {
                    //context.setProcessingEvents(false);
                    UIComponent c = it.next();
                    it.remove();
                    //context.setProcessingEvents(true);
                    newView.getChildren().add(c);
                }
            }
            
            // Restore the newView as saved just before markInitialState() call
            newView.restoreState(context, viewState);
            newView.markInitialState();
            
            if (!PhaseId.RESTORE_VIEW.equals(context.getCurrentPhaseId()))
            {
                // Restore bindings like in restore view phase, because in this case,
                // bindings needs to be set (Application.createComponent is not called!).
                restoreViewSupport.processComponentBinding(context, newView);
                
                // Restore view scope map if necessary
                if (viewScopeMap != null && !viewScopeMap.isEmpty())
                {
                    Map<String, Object> newViewScopeMap = newView.getViewMap(false);
                    if (newViewScopeMap == null)
                    {
                        newView.restoreViewScopeState(context, newViewState);
                    }
                    else
                    {
                        // Should theoretically not happen, because when a pooled static view 
                        // is saved, the view scope map is skipped, otherwise it could be a
                        // leak. Anyway, we let this code here that overrides the values from
                        // the original map.
                        for (Map.Entry<String, Object> entry2 : viewScopeMap.entrySet())
                        {
                            newViewScopeMap.put(entry2.getKey(), entry2.getValue());
                        }
                    }
                }
            }
            
            // Update request view metadata to ensure resource list is restored as when the
            // view was built on the first time. This ensures correct calculation of added 
            // resources by dynamic behavior. 
            RequestViewContext rcv = RequestViewContext.getCurrentInstance(context, newView, false);
            if (rcv != null)
            {
                rcv.setRequestViewMetadata(metadata.getRequestViewMetadata().cloneInstance());
            }
            else
            {
                RequestViewContext.setCurrentInstance(context, newView,
                        RequestViewContext.newInstance(metadata.getRequestViewMetadata().cloneInstance()));
            }
        }
        finally
        {
            context.setProcessingEvents(oldProcessingEvents);
        }
    }
    
    public void storeViewStructureMetadata(FacesContext context, UIViewRoot root)
    {
        ViewPool viewPool = getViewPool(context, root);
        if (viewPool != null)
        {
            FaceletState faceletState = (FaceletState) root.getAttributes().get(
                    ComponentSupport.FACELET_STATE_INSTANCE);
            boolean isDynamic = faceletState != null ? faceletState.isDynamic() : false;
            if (!isDynamic)
            {
                viewPool.storeStaticViewStructureMetadata(context, root, faceletState);            
            }
            else
            {
                viewPool.storeDynamicViewStructureMetadata(context, root, faceletState);
            }
        }
    }
    
    public ViewStructureMetadata retrieveViewStructureMetadata(FacesContext context,
            UIViewRoot root)
    {
        ViewPool viewPool = getViewPool(context, root);
        if (viewPool != null)
        {
            FaceletState faceletState = (FaceletState) root.getAttributes().get(
                    ComponentSupport.FACELET_STATE_INSTANCE);
            boolean isDynamic = faceletState != null ? faceletState.isDynamic() : false;
            if (!isDynamic)
            {
                return viewPool.retrieveStaticViewStructureMetadata(context, root);
            }
            else
            {
                return viewPool.retrieveDynamicViewStructureMetadata(context, root, faceletState);
            }
        }
        return null;
    }
    
    public void pushResetableView(FacesContext context, UIViewRoot view, FaceletState faceletViewState)
    {
        ViewPool viewPool = getViewPool(context, view);
        if (viewPool != null)
        {
            boolean isDynamic = faceletViewState != null ? faceletViewState.isDynamic() : false;
            if (!isDynamic)
            {
                clearTransientAndNonFaceletComponentsForStaticView(context, view);
                viewPool.pushStaticStructureView(context, view);
            }
            else
            {
                ViewStructureMetadata viewStructureMetadata = viewPool.retrieveDynamicViewStructureMetadata(
                    context, view, faceletViewState);
                if (viewStructureMetadata != null)
                {
                    clearTransientAndNonFaceletComponentsForDynamicView(context, view, viewStructureMetadata);
                    viewPool.pushDynamicStructureView(context, view, faceletViewState);
                }
            }
        }
    }
    
    public void pushPartialView(FacesContext context, UIViewRoot view, FaceletState faceletViewState, int count)
    {
        ViewPool viewPool = getViewPool(context, view);
        
        if (viewPool != null && viewPool.isWorthToRecycleThisView(context, view))
        {
            ViewStructureMetadata viewStructureMetadata = null;
            if (faceletViewState == null)
            {
                viewStructureMetadata = viewPool.retrieveStaticViewStructureMetadata(context, view);
            }
            else
            {
                viewStructureMetadata = viewPool.retrieveDynamicViewStructureMetadata(
                    context, view, faceletViewState);
            }
            if (viewStructureMetadata != null)
            {
                ClearPartialTreeContext ptc = new ClearPartialTreeContext();
                // add partial structure view to the map.
                clearTransientAndRemoveNonResetableComponents(context, ptc, view, viewStructureMetadata);
                int reusableCount = ptc.getCount();
                float factor = ((float)reusableCount) / ((float)count);
                if (factor > 0.3f)
                {
                    viewPool.pushPartialStructureView(context, view);
                }
            }
        }        
    }
    
    protected void clearTransientAndNonFaceletComponentsForStaticView(final FacesContext context, 
            final UIViewRoot root)
    {
        // In a static view, clear components that are both transient and non bound to any facelet tag handler
        // is quite simple. Since the structure of the view is static, there is no need to check component resources.
        clearTransientAndNonFaceletComponents(context, root);
    }
    
    public void clearTransientAndNonFaceletComponentsForDynamicView(final FacesContext context, 
            final UIViewRoot root, final ViewStructureMetadata viewStructureMetadata)
    {
        //Scan children
        int childCount = root.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                UIComponent child = root.getChildren().get(i);
                if (child != null && child.isTransient() &&
                    child.getAttributes().get(ComponentSupport.MARK_CREATED) == null)
                {
                    root.getChildren().remove(i);
                    i--;
                    childCount--;
                }
                else
                {
                    if (child.getChildCount() > 0 || !child.getFacets().isEmpty())
                    {
                        clearTransientAndNonFaceletComponents(context, child);
                    }
                }
            }
        }

        clearTransientAndNonFaceletComponentsForDynamicViewUIViewRootFacets(
            context, root, viewStructureMetadata);
    }
    
    private void clearTransientAndNonFaceletComponentsForDynamicViewUIViewRootFacets(final FacesContext context, 
            final UIViewRoot root, ViewStructureMetadata viewStructureMetadata)
    {
        
        //Scan facets
        if (root.getFacetCount() > 0)
        {
            Map<String, UIComponent> facets = root.getFacets();
            for (Iterator<UIComponent> itr = facets.values().iterator(); itr.hasNext();)
            {
                UIComponent fc = itr.next();
                if (fc != null && !(fc instanceof ComponentResourceContainer))
                {
                    if ( fc.isTransient() &&
                        fc.getAttributes().get(ComponentSupport.MARK_CREATED) == null)
                    {
                        itr.remove();
                    }
                    else
                    {
                        if (fc.getChildCount() > 0 || !fc.getFacets().isEmpty())
                        {
                            clearTransientAndNonFaceletComponents(context, fc);
                        }
                    }
                }
                else if (fc != null)
                {
                    // In a facet which is a ComponentResourceContainer instance,
                    // we need to check these two cases:
                    // 1. Resources relocated by facelets
                    // 2. Resources created by effect of a @ResourceDependency annotation
                    if (fc.getId() != null && fc.getId().startsWith("jakarta_faces_location_"))
                    {
                        String target = fc.getId().substring("jakarta_faces_location_".length());
                        Map<String, List<ResourceDependency>> addedResources = 
                            viewStructureMetadata.getRequestViewMetadata().
                                getResourceDependencyAnnotations(context);
                        List<ResourceDependency> resourceDependencyList = (addedResources != null) ?
                            addedResources.get(target) : null;

                        clearComponentResourceContainer(context, fc, resourceDependencyList);
                    }
                }
            }
        }
    }
    
    private void clearComponentResourceContainer(final FacesContext context, UIComponent component,
        List<ResourceDependency> resourceDependencyList)
    {
        //Scan children
        int childCount = component.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                UIComponent child = component.getChildren().get(i);
                String id = (String) child.getAttributes().get(ComponentSupport.MARK_CREATED);
                if (child != null && child.isTransient() &&
                    id == null)
                {
                    //Remove both transient not facelets bound components
                    component.getChildren().remove(i);
                    i--;
                    childCount--;
                }
                else if (id != null)
                {
                    // If it has an id set, it is a facelet component resource.
                    // The refresh algorithm take care of the cleanup.
                }
                /*
                else if (!child.isTransient() && id != null && !faceletResources.contains(id))
                {
                    // check if the resource has a facelet tag in this "dynamic state", if not
                    // remove it. Really leave a component resource does not harm, but 
                    // the objective is make this view as close as when if it is built as new.
                    component.getChildren().remove(i);
                    i--;
                    childCount--;
                }*/
                else
                {
                    // Check if the component instance was created using a @ResourceDependency annotation
                    Object[] rdk = (Object[]) child.getAttributes().get(
                                RequestViewMetadata.RESOURCE_DEPENDENCY_KEY);
                    if (rdk != null)
                    {
                        boolean found = false;
                        String library = (String) rdk[0];
                        String name = (String) rdk[1];
                        if (resourceDependencyList != null)
                        {
                            for (ResourceDependency resource : resourceDependencyList)
                            {
                                if (library == null && resource.library() == null)
                                {
                                    if (name != null && name.equals(resource.name()))
                                    {
                                        found = true;
                                        break;
                                    }
                                }
                                else
                                {
                                    if (library != null && library.equals(resource.library()) &&
                                        name != null && name.equals(resource.name()) )
                                    {
                                        found = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!found)
                        {
                            //Remove it, because for this dynamic state it it does not exists.
                            component.getChildren().remove(i);
                            i--;
                            childCount--;
                        }
                        // If found just leave it.
                    }
                    else
                    {
                        if (child.getChildCount() > 0 || !child.getFacets().isEmpty())
                        {
                            clearTransientAndNonFaceletComponents(context, child);
                        }
                    }
                }
            }
        }
    }

    /**
     * Clear all transient components not created by facelets algorithm. In this way,
     * we ensure the component tree does not have any changes done after markInitialState.
     * 
     * @param context
     * @param component 
     */
    private void clearTransientAndNonFaceletComponents(final FacesContext context, final UIComponent component)
    {
        //Scan children
        int childCount = component.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                UIComponent child = component.getChildren().get(i);
                if (child != null && child.isTransient() &&
                    child.getAttributes().get(ComponentSupport.MARK_CREATED) == null)
                {
                    component.getChildren().remove(i);
                    i--;
                    childCount--;
                }
                else
                {
                    if (child.getChildCount() > 0 || !child.getFacets().isEmpty())
                    {
                        clearTransientAndNonFaceletComponents(context, child);
                    }
                }
            }
        }

        //Scan facets
        if (component.getFacetCount() > 0)
        {
            Map<String, UIComponent> facets = component.getFacets();
            for (Iterator<UIComponent> itr = facets.values().iterator(); itr.hasNext();)
            {
                UIComponent fc = itr.next();
                if (fc != null && fc.isTransient() &&
                    fc.getAttributes().get(ComponentSupport.MARK_CREATED) == null)
                {
                    itr.remove();
                }
                else
                {
                    if (fc.getChildCount() > 0 || !fc.getFacets().isEmpty())
                    {
                        clearTransientAndNonFaceletComponents(context, fc);
                    }
                }
            }
        }
    }

    private void clearTransientAndRemoveNonResetableComponents(final FacesContext context, 
        final ClearPartialTreeContext ptc, final UIViewRoot root, 
        ViewStructureMetadata viewStructureMetadata)
    {
        //Scan children
        int childCount = root.getChildCount();

        try
        {
            root.getAttributes().put(ViewPoolProcessor.RESET_SAVE_STATE_MODE_KEY, 
                        ViewPoolProcessor.RESET_MODE_HARD);
            if (childCount > 0)
            {
                for (int i = 0; i < childCount; i++)
                {
                    UIComponent child = root.getChildren().get(i);
                    boolean containsFaceletId = child.getAttributes().containsKey(ComponentSupport.MARK_CREATED);
                    if (child != null && child.isTransient() && !containsFaceletId)
                    {
                        //Transient and not bound to facelets tag, remove it!.
                        root.getChildren().remove(i);
                        i--;
                        childCount--;
                    }
                    else
                    {
                        if (child.getAttributes().containsKey(ComponentSupport.COMPONENT_ADDED_BY_HANDLER_MARKER))
                        {
                            //Dynamically added or moved, remove it!
                            root.getChildren().remove(i);
                            i--;
                            childCount--;

                        }
                        else if (containsFaceletId ||
                            child.getAttributes().containsKey(ComponentSupport.COMPONENT_ADDED_BY_HANDLER_MARKER))
                        {
                            // Bound to a facelet tag or created by facelets, we have two options:
                            // 1. If is not transient, check its state and try to clear it, if fails remove it
                            // 2. If is transient, assume stateless, continue.
                            if (!child.isTransient())
                            {
                                // Remember that hard reset is already enabled.
                                Object state = child.saveState(context);
                                if (state == null)
                                {
                                    if (child.getChildCount() > 0 || !child.getFacets().isEmpty())
                                    {
                                        clearTransientAndRemoveNonResetableComponents(context, ptc, child);
                                    }
                                    ptc.incrementCount();
                                }
                                else
                                {
                                    root.getChildren().remove(i);
                                    i--;
                                    childCount--;
                                }
                            }
                            else
                            {
                                ptc.incrementCount();
                            }
                        }
                        else
                        {
                            // Non facelets component, remove it!.
                            root.getChildren().remove(i);
                            i--;
                            childCount--;
                        }
                    }
                }
            }

            clearTransientAndNonFaceletComponentsForDynamicViewUIViewRootFacets(
                context, root, viewStructureMetadata);
        }
        finally
        {
            root.getAttributes().put(ViewPoolProcessor.RESET_SAVE_STATE_MODE_KEY, 
                        ViewPoolProcessor.RESET_MODE_OFF);
        }
    }
    
    private void clearTransientAndRemoveNonResetableComponents(final FacesContext context,
        final ClearPartialTreeContext ptc, final UIComponent component)
    {
        //Scan children
        int childCount = component.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                UIComponent child = component.getChildren().get(i);
                boolean containsFaceletId = child.getAttributes().containsKey(ComponentSupport.MARK_CREATED);
                if (child != null && child.isTransient() && !containsFaceletId)
                {
                    //Transient and not bound to facelets tag, remove it!.
                    component.getChildren().remove(i);
                    i--;
                    childCount--;
                }
                else
                {
                    if (child.getAttributes().containsKey(ComponentSupport.COMPONENT_ADDED_BY_HANDLER_MARKER))
                    {
                        //Dynamically added or moved, remove it!
                        component.getChildren().remove(i);
                        i--;
                        childCount--;

                    }
                    else if (containsFaceletId ||
                        child.getAttributes().containsKey(ComponentSupport.COMPONENT_ADDED_BY_HANDLER_MARKER))
                    {
                        // Bound to a facelet tag or created by facelets, we have two options:
                        // 1. If is not transient, check its state and try to clear it, if fails remove it
                        // 2. If is transient, assume stateless, continue.
                        if (!child.isTransient())
                        {
                            // Remember that hard reset is already enabled.
                            Object state = child.saveState(context);
                            if (state == null)
                            {
                                if (child.getChildCount() > 0 || !child.getFacets().isEmpty())
                                {
                                    clearTransientAndRemoveNonResetableComponents(context, ptc, child);
                                }
                                ptc.incrementCount();
                            }
                            else
                            {
                                component.getChildren().remove(i);
                                i--;
                                childCount--;
                            }
                        }
                        else
                        {
                            ptc.incrementCount();
                        }
                    }
                    else
                    {
                        // Non facelets component, remove it!.
                        component.getChildren().remove(i);
                        i--;
                        childCount--;
                    }
                }
            }
        }

        //Scan facets
        if (component.getFacetCount() > 0)
        {
            Map<String, UIComponent> facets = component.getFacets();
            for (Iterator<UIComponent> itr = facets.values().iterator(); itr.hasNext();)
            {
                UIComponent fc = itr.next();
                boolean containsFaceletId = fc.getAttributes().containsKey(ComponentSupport.MARK_CREATED);
                if (fc != null && fc.isTransient() && !containsFaceletId)
                {
                    //Transient and not bound to facelets tag, remove it!.
                    itr.remove();
                }
                else
                {
                    if (fc.getAttributes().containsKey(ComponentSupport.COMPONENT_ADDED_BY_HANDLER_MARKER))
                    {
                        //Dynamically added or moved, remove it!
                        itr.remove();

                    }
                    else if (containsFaceletId ||
                        fc.getAttributes().containsKey(ComponentSupport.COMPONENT_ADDED_BY_HANDLER_MARKER))
                    {
                        // Bound to a facelet tag or created by facelets, we have two options:
                        // 1. If is not transient, check its state and try to clear it, if fails remove it
                        // 2. If is transient, assume stateless, continue.
                        if (!fc.isTransient())
                        {
                            // Remember that hard reset is already enabled.
                            Object state = fc.saveState(context);
                            if (state == null)
                            {
                                if (fc.getChildCount() > 0 || !fc.getFacets().isEmpty())
                                {
                                    clearTransientAndRemoveNonResetableComponents(context, ptc, fc);
                                }
                                ptc.incrementCount();
                            }
                            else
                            {
                                itr.remove();
                            }
                        }
                        else
                        {
                            ptc.incrementCount();
                        }
                    }
                    else
                    {
                        // Non facelets component, remove it!.
                        itr.remove();
                    }
                }
            }
        }
    }
    
    public void processDeferredNavigation(FacesContext facesContext)
    {
            Object[] command = (Object[]) facesContext.getAttributes().get(
                ViewPoolProcessor.INVOKE_DEFERRED_NAVIGATION);
        if (command != null)
        {
            try
            {
                facesContext.getAttributes().put(ViewPoolProcessor.DISPOSE_VIEW_NAVIGATION, Boolean.TRUE);
                NavigationHandler navigationHandler = facesContext.getApplication().getNavigationHandler();
                if (command.length == 3)
                {
                    navigationHandler.handleNavigation(facesContext, (String) command[0], (String) command[1], 
                        (String) command[2]);
                }
                else
                {
                    navigationHandler.handleNavigation(facesContext, (String) command[0], (String) command[1]);
                }
                //Render Response if needed
                facesContext.renderResponse();
                facesContext.getAttributes().remove(ViewPoolProcessor.INVOKE_DEFERRED_NAVIGATION);
            }
            finally
            {
                facesContext.getAttributes().remove(ViewPoolProcessor.DISPOSE_VIEW_NAVIGATION);
            }
        }
    }
    
    private static final String SERIALIZED_VIEW_REQUEST_ATTR = 
        StateManagerImpl.class.getName() + ".SERIALIZED_VIEW";

    public void disposeView(FacesContext facesContext, UIViewRoot root)
    {
        if (root == null)
        {
            return;
        }

        String viewId = root.getViewId();
        if (viewId == null)
        {
            return;
        }
        Application app = facesContext.getApplication();
        if (app == null)
        {
            return;
        }
        ViewHandler viewHandler = app.getViewHandler();
        if (viewHandler == null)
        {
            return;
        }

        if (Boolean.TRUE.equals(facesContext.getAttributes().get(ViewPoolProcessor.DISPOSE_VIEW_NAVIGATION)))
        {
            ViewDeclarationLanguage vdl = facesContext.getApplication().
                    getViewHandler().getViewDeclarationLanguage(
                        facesContext, root.getViewId());

            if (vdl != null && ViewDeclarationLanguage.FACELETS_VIEW_DECLARATION_LANGUAGE_ID.equals(vdl.getId()))
            {
                StateManagementStrategy sms = vdl.getStateManagementStrategy(facesContext, root.getId());
                if (sms != null)
                {
                    // Force indirectly to store the map in the pool
                    facesContext.getAttributes().put(ViewPoolProcessor.FORCE_HARD_RESET, Boolean.TRUE);

                    try
                    {
                        Object state = sms.saveView(facesContext);
                    }
                    finally
                    {
                        facesContext.getAttributes().remove(ViewPoolProcessor.FORCE_HARD_RESET);
                    }

                    // Clear the calculated value from the application map
                    facesContext.getAttributes().remove(SERIALIZED_VIEW_REQUEST_ATTR);
                }
            }
        }
    }
    
    private static class ClearPartialTreeContext 
    {
        private int count;
        
        public ClearPartialTreeContext()
        {
            count = 0;
        }

        /**
         * @return the count
         */
        public int getCount()
        {
            return count;
        }

        public int incrementCount()
        {
            return count++;
        }
        /**
         * @param count the count to set
         */
        public void setCount(int count)
        {
            this.count = count;
        }
    }
}
