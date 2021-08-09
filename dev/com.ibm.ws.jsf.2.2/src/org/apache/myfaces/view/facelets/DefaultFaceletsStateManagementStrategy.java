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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.ProjectStage;
import javax.faces.application.StateManager;
import javax.faces.component.ContextCallback;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitContextFactory;
import javax.faces.component.visit.VisitHint;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PreRemoveFromViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.ResponseStateManager;
import javax.faces.view.StateManagementStrategy;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewDeclarationLanguageFactory;
import javax.faces.view.ViewMetadata;

import org.apache.myfaces.application.StateManagerImpl;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.context.RequestViewContext;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.shared.util.HashMapUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.view.facelets.compiler.CheckDuplicateIdFaceletUtils;
import org.apache.myfaces.view.facelets.pool.ViewEntry;
import org.apache.myfaces.view.facelets.pool.ViewPool;
import org.apache.myfaces.view.facelets.pool.ViewStructureMetadata;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;
import org.apache.myfaces.view.facelets.tag.jsf.FaceletState;

/**
 * This class implements partial state saving feature when facelets
 * is used to render pages. (Theorically it could be applied on jsp case too,
 * but all considerations below should be true before apply it).
 * 
 * The following considerations apply for this class:
 * 
 * 1. This StateManagementStrategy should only be active if javax.faces.PARTIAL_STATE_SAVING
 *    config param is active(true). See javadoc on StateManager for details.
 * 2. A map using component clientId as keys are used to hold the state.
 * 3. Each component has a valid id after ViewDeclarationLanguage.buildView().
 *    This implies that somewhere, every TagHandler that create an UIComponent 
 *    instance should call setId and assign it.
 * 4. Every TagHandler that create an UIComponent instance should call markInitialState
 *    after the component is populated. Otherwise, full state is always saved.
 * 5. A SystemEventListener is used to keep track for added and removed components, listen
 *    PostAddToViewEvent and PreRemoveFromViewEvent event triggered by UIComponent.setParent()
 *    method.
 * 6. It is not possible to use javax.faces.component.visit API to traverse the component
 *    tree during save/restore, because UIData.visitTree traverse all rows and we only need
 *    to restore state per component (not per row).
 * 7. It is necessary to preserve the order of the children added/removed between requests.
 * 8. Added and removed components could be seen as subtrees. This imply that we need to save
 *    the structure of the added components subtree and remove one component could be remove
 *    all its children and facets from view inclusive.
 * 9. It is necessary to save and restore the list of added/removed components between several
 *    requests.
 * 10.All components ids removed in any moment of time must be preserved.
 * 11.Each component must be restored only once.
 * 11.The order is important for ids added when it is traversed the tree, otherwise the algorithm 
 *    could change the order in which components will be restored.  
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1622093 $ $Date: 2014-09-02 19:03:52 +0000 (Tue, 02 Sep 2014) $
 * @since 2.0
 *
 */
public class DefaultFaceletsStateManagementStrategy extends StateManagementStrategy
{
    public static final String CLIENTIDS_ADDED = "oam.CLIENTIDS_ADDED";
    
    public static final String CLIENTIDS_REMOVED = "oam.CLIENTIDS_REMOVED";
    
    /**
     * Key used on component attribute map to indicate if a component was added
     * after build view, so itself and all descendants should not use partial
     * state saving. There are two possible values:
     * 
     * Key not present: The component uses pss.
     * ComponentState.ADD: The component was added to the view after build view.
     * ComponentState.REMOVE_ADD: The component was removed/added to the view. Itself and all
     * descendants should be saved and restored, but we have to unregister/register
     * from CLIENTIDS_ADDED and CLIENTIDS_REMOVED lists. See ComponentSupport.markComponentToRestoreFully
     * for details.
     * ComponentState.ADDED: The component has been added or removed/added, but it has
     * been already processed.
     */
    public  static final String COMPONENT_ADDED_AFTER_BUILD_VIEW = "oam.COMPONENT_ADDED_AFTER_BUILD_VIEW"; 
    
    /**
     * If this param is set to true (by default), when pss algorithm is executed to save state, a visit tree
     * traversal is done, instead a plain traversal like previous versions (2.0.7/2.1.1 and earlier) of MyFaces Core.
     * 
     * This param is just provided to preserve backwards behavior. 
     * @deprecated JSF 2.2 change enforces tree visiting as the preferred way. Performance tests shows that plain
     * visit is faster but the difference is negligible.
     */
    @Deprecated
    @JSFWebConfigParam(since="2.0.8, 2.1.2", defaultValue="true", expectedValues="true, false",
                       group="state", tags="performance", deprecated=true)
    public static final String SAVE_STATE_WITH_VISIT_TREE_ON_PSS
            = "org.apache.myfaces.SAVE_STATE_WITH_VISIT_TREE_ON_PSS";
    
    /**
     * Define how duplicate ids are checked when ProjectStage is Production, by default (auto) it only check ids of
     * components that does not encapsulate markup (like facelets UILeaf).
     *  
     * <ul>
     * <li>true: check all ids, including ids for components that are transient and encapsulate markup.</li>
     * <li>auto: (default) check ids of components that does not encapsulate markup (like facelets UILeaf). 
     * Note ids of UILeaf instances are generated by facelets vdl, start with "j_id", are never rendered 
     * into the response and UILeaf instances are never used as a target for listeners, so in practice 
     * there is no need to check such ids. This reduce the overhead associated with generate client ids.</li>
     * <li>false: do not do any check when ProjectStage is Production</li>
     * </ul>
     * <p> According to specification, identifiers must be unique within the scope of the nearest ancestor to 
     * the component that is a naming container.</p>
     */
    @JSFWebConfigParam(since="2.0.12, 2.1.6", defaultValue="auto", expectedValues="true, auto, false",
                       group="state", tags="performance")
    public static final String CHECK_ID_PRODUCTION_MODE
            = "org.apache.myfaces.CHECK_ID_PRODUCTION_MODE";
    
    private static final String CHECK_ID_PRODUCTION_MODE_DEFAULT = "auto";
    private static final String CHECK_ID_PRODUCTION_MODE_TRUE = "true";
    private static final String CHECK_ID_PRODUCTION_MODE_AUTO = "auto";
    
    private static final String SKIP_ITERATION_HINT = "javax.faces.visit.SKIP_ITERATION";
    
    private static final String SERIALIZED_VIEW_REQUEST_ATTR = 
        StateManagerImpl.class.getName() + ".SERIALIZED_VIEW";
    
    private static final Object[] EMPTY_STATES = new Object[]{null, null};
    
    private static final Set<VisitHint> VISIT_HINTS = Collections.unmodifiableSet( 
            EnumSet.of(VisitHint.SKIP_ITERATION));
    
    private static final String UNIQUE_ID_COUNTER_KEY =
              "oam.view.uniqueIdCounter";
    
    private ViewDeclarationLanguageFactory _vdlFactory;
    
    private RenderKitFactory _renderKitFactory = null;
    
    private VisitContextFactory _visitContextFactory = null;
    
    private String _checkIdsProductionMode;
    
    private MyfacesConfig _config;
    
    private ViewPoolProcessor _viewPoolProcessor;
    
    public DefaultFaceletsStateManagementStrategy ()
    {
        this(FacesContext.getCurrentInstance());
    }
    
    public DefaultFaceletsStateManagementStrategy (FacesContext context)
    {
        _vdlFactory = (ViewDeclarationLanguageFactory)
                FactoryFinder.getFactory(FactoryFinder.VIEW_DECLARATION_LANGUAGE_FACTORY);
        _config = MyfacesConfig.getCurrentInstance(context.getExternalContext());
        _viewPoolProcessor = ViewPoolProcessor.getInstance(context);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public UIViewRoot restoreView (FacesContext context, String viewId, String renderKitId)
    {
        ResponseStateManager manager;
        Object state[];
        Map<String, Object> states;
        UIViewRoot view = null;
 
        // The value returned here is expected to be false (set by RestoreViewExecutor), but
        //we don't know if some ViewHandler wrapper could change it, so it is better to save the value.
        final boolean oldContextEventState = context.isProcessingEvents();
        // Get previous state from ResponseStateManager.
        manager = getRenderKitFactory().getRenderKit(context, renderKitId).getResponseStateManager();
        
        state = (Object[]) manager.getState(context, viewId);
        
        if (state == null)
        {
            //No state could be restored, return null causing ViewExpiredException
            return null;
        }
        
        if (state[1] instanceof Object[])
        {
            Object[] fullState = (Object[]) state[1]; 
            view = (UIViewRoot) internalRestoreTreeStructure((TreeStructComponent)fullState[0]);

            if (view != null)
            {
                context.setViewRoot (view);
                view.processRestoreState(context, fullState[1]);
                
                // If the view is restored fully, it is necessary to refresh RequestViewContext, otherwise at
                // each ajax request new components associated with @ResourceDependency annotation will be added
                // to the tree, making the state bigger without real need.
                RequestViewContext.getCurrentInstance(context).
                        refreshRequestViewContext(context, view);
                
                if (fullState.length == 3 && fullState[2] != null)
                {
                    context.setResourceLibraryContracts((List) UIComponentBase.
                        restoreAttachedState(context, fullState[2]));
                }
            }
        }
        else
        {
            // Per the spec: build the view.
            ViewDeclarationLanguage vdl = _vdlFactory.getViewDeclarationLanguage(viewId);
            Object faceletViewState = null;
            try
            {
                ViewMetadata metadata = vdl.getViewMetadata (context, viewId);
                
                if (metadata != null)
                {
                    view = metadata.createMetadataView(context);
                    
                    // If no view and response complete there is no need to continue
                    if (view == null && context.getResponseComplete())
                    {
                        return null;
                    }
                }
                if (view == null)
                {
                    view = context.getApplication().getViewHandler().createView(context, viewId);
                }
                
                context.setViewRoot (view); 
                boolean skipBuildView = false;
                if (state != null && state[1] != null)
                {
                    // Since JSF 2.2, UIViewRoot.restoreViewScopeState() must be called, but
                    // to get the state of the root, it is necessary to force calculate the
                    // id from this location. Remember in this point, PSS is enabled, so the
                    // code match with the assigment done in 
                    // FaceletViewDeclarationLanguage.buildView()
                    states = (Map<String, Object>) state[1];
                    faceletViewState = UIComponentBase.restoreAttachedState(
                            context,states.get(ComponentSupport.FACELET_STATE_INSTANCE));
                    if (faceletViewState != null && _viewPoolProcessor != null)
                    {
                        ViewPool viewPool = _viewPoolProcessor.getViewPool(context, view);
                        if (viewPool != null)
                        {
                            ViewStructureMetadata viewMetadata = viewPool.retrieveDynamicViewStructureMetadata(
                                context, view, (FaceletState) faceletViewState);
                            if (viewMetadata != null)
                            {
                                ViewEntry entry = viewPool.popDynamicStructureView(context, view,
                                        (FaceletState) faceletViewState);
                                if (entry != null)
                                {
                                    skipBuildView = true;
                                    _viewPoolProcessor.cloneAndRestoreView(context, view, entry, viewMetadata);
                                }
                            }
                        }
                    }
                    if (view.getId() == null)
                    {
                        view.setId(view.createUniqueId(context, null));
                    }
                    if (faceletViewState != null)
                    {
                        view.getAttributes().put(ComponentSupport.FACELET_STATE_INSTANCE,  faceletViewState);
                    }
                    if (state.length == 3)
                    {
                        //Jump to where the count is
                        view.getAttributes().put(UNIQUE_ID_COUNTER_KEY, state[2]);
                    }
                    Object viewRootState = states.get(view.getClientId(context));
                    if (viewRootState != null)
                    {
                        try
                        {
                            view.pushComponentToEL(context, view);
                            view.restoreViewScopeState(context, viewRootState);
                        }
                        finally
                        {
                            view.popComponentFromEL(context);
                        }
                    }
                }
                // On RestoreViewExecutor, setProcessingEvents is called first to false
                // and then to true when postback. Since we need listeners registered to PostAddToViewEvent
                // event to be handled, we should enable it again. For partial state saving we need this listeners
                // be called from here and relocate components properly.
                if (!skipBuildView)
                {
                    try 
                    {
                        context.setProcessingEvents (true);
                        vdl.buildView (context, view);
                        // In the latest code related to PostAddToView, it is
                        // triggered no matter if it is applied on postback. It seems that MYFACES-2389, 
                        // TRINIDAD-1670 and TRINIDAD-1671 are related.
                        suscribeListeners(view);
                    }
                    finally
                    {
                        context.setProcessingEvents (oldContextEventState);
                    }
                }
            }
            catch (Throwable e)
            {
                throw new FacesException ("unable to create view \"" + viewId + "\"", e);
            }

            // Stateless mode only for transient views and non stateless mode for
            // stateful views. This check avoid apply state over a stateless view.
            boolean statelessMode = manager.isStateless(context, viewId);
            if (statelessMode && !view.isTransient())
            {
                throw new IllegalStateException("View is not transient");
            }
            if (!statelessMode && view.isTransient())
            {
                throw new IllegalStateException("Cannot apply state over stateless view");
            }
            
            if (state != null && state[1] != null)
            {
                states = (Map<String, Object>) state[1];
                //Save the last unique id counter key in UIViewRoot
                Integer lastUniqueIdCounter = (Integer) view.getAttributes().get(UNIQUE_ID_COUNTER_KEY);
                
                // Visit the children and restore their state.
                boolean emptyState = false;
                boolean containsFaceletState = states.containsKey(
                        ComponentSupport.FACELET_STATE_INSTANCE);
                if (states.isEmpty())
                {
                    emptyState = true; 
                }
                else if (states.size() == 1 && 
                        containsFaceletState)
                {
                    emptyState = true; 
                }
                //Restore state of current components
                if (!emptyState)
                {
                    // Check if there is only one component state
                    // and that state is UIViewRoot instance (for example
                    // when using ViewScope)
                    if ((states.size() == 1 && !containsFaceletState) || 
                        (states.size() == 2 && containsFaceletState))
                    {
                        Object viewState = states.get(view.getClientId(context));
                        if (viewState != null)
                        {
                            restoreViewRootOnlyFromMap(context,viewState, view);
                        }
                        else
                        {
                            //The component is not viewRoot, restore as usual.
                            restoreStateFromMap(context, states, view);
                        }
                    }
                    else
                    {
                        restoreStateFromMap(context, states, view);
                    }
                }
                if (faceletViewState != null)
                {
                    view.getAttributes().put(ComponentSupport.FACELET_STATE_INSTANCE,  faceletViewState);
                }
                if (lastUniqueIdCounter != null)
                {
                    Integer newUniqueIdCounter = (Integer) view.getAttributes().get(UNIQUE_ID_COUNTER_KEY);
                    if (newUniqueIdCounter != null && 
                        lastUniqueIdCounter.intValue() > newUniqueIdCounter.intValue())
                    {
                        // The unique counter was restored by a side effect of 
                        // restoreState() over UIViewRoot with a lower count,
                        // to avoid a component duplicate id exception we need to fix the count.
                        view.getAttributes().put(UNIQUE_ID_COUNTER_KEY, lastUniqueIdCounter);
                    }
                }
                handleDynamicAddedRemovedComponents(context, view, states);
            }
        }
        return view;
    }
    
    public void handleDynamicAddedRemovedComponents(FacesContext context, UIViewRoot view, Map<String, Object> states)
    {
        List<String> clientIdsRemoved = getClientIdsRemoved(view);

        if (clientIdsRemoved != null)
        {
            Set<String> idsRemovedSet = new HashSet<String>(HashMapUtils.calcCapacity(clientIdsRemoved.size()));
            context.getAttributes().put(FaceletViewDeclarationLanguage.REMOVING_COMPONENTS_BUILD, Boolean.TRUE);
            try
            {
                // perf: clientIds are ArrayList: see method registerOnAddRemoveList(String)
                for (int i = 0, size = clientIdsRemoved.size(); i < size; i++)
                {
                    String clientId = clientIdsRemoved.get(i);
                    if (!idsRemovedSet.contains(clientId))
                    {
                        RemoveComponentCallback callback = new RemoveComponentCallback();
                        view.invokeOnComponent(context, clientId, callback);
                        if (callback.isComponentFound())
                        {
                            //Add only if component found
                            idsRemovedSet.add(clientId);
                        }
                    }
                }
                clientIdsRemoved.clear();
                clientIdsRemoved.addAll(idsRemovedSet);
            }
            finally
            {
                context.getAttributes().remove(FaceletViewDeclarationLanguage.REMOVING_COMPONENTS_BUILD);
            }
        }
        List<String> clientIdsAdded = getClientIdsAdded(view);
        if (clientIdsAdded != null)
        {
            Set<String> idsAddedSet = new HashSet<String>(HashMapUtils.calcCapacity(clientIdsAdded.size()));
            // perf: clientIds are ArrayList: see method setClientsIdsAdded(String)
            for (int i = 0, size = clientIdsAdded.size(); i < size; i++)
            {
                String clientId = clientIdsAdded.get(i);
                if (!idsAddedSet.contains(clientId))
                {
                    final AttachedFullStateWrapper wrapper = (AttachedFullStateWrapper) states.get(clientId);
                    if (wrapper != null)
                    {
                        final Object[] addedState = (Object[]) wrapper.getWrappedStateObject(); 
                        if (addedState != null)
                        {
                            if (addedState.length == 2)
                            {
                                view = (UIViewRoot)
                                        internalRestoreTreeStructure((TreeStructComponent) addedState[0]);
                                view.processRestoreState(context, addedState[1]);
                                break;
                            }
                            else
                            {
                                final String parentClientId = (String) addedState[0];
                                view.invokeOnComponent(context, parentClientId, 
                                    new AddComponentCallback(addedState));
                            }
                        }
                    }
                    idsAddedSet.add(clientId);
                }
            }
            // Reset this list, because it will be calculated later when the view is being saved
            // in the right order, preventing duplicates (see COMPONENT_ADDED_AFTER_BUILD_VIEW for details).
            clientIdsAdded.clear();
            
            // This call only has sense when components has been added programatically, because if facelets has control
            // over all components in the component tree, build the initial state and apply the state will have the
            // same effect.
            RequestViewContext.getCurrentInstance(context).
                    refreshRequestViewContext(context, view);
        }
    }

    public static class RemoveComponentCallback implements ContextCallback
    {
        private boolean componentFound;
        
        public RemoveComponentCallback()
        {
            this.componentFound = false;
        }
        
        public void invokeContextCallback(FacesContext context,
                UIComponent target)
        {
            if (target.getParent() != null && 
                !target.getParent().getChildren().remove(target))
            {
                String key = null;
                if (target.getParent().getFacetCount() > 0)
                {
                    for (Map.Entry<String, UIComponent> entry :
                            target.getParent().getFacets().entrySet())
                    {
                        if (entry.getValue()==target)
                        {
                            key = entry.getKey();
                            break;
                        }
                    }
                }
                if (key != null)
                {
                    UIComponent removedTarget = target.getParent().getFacets().remove(key);
                    if (removedTarget != null)
                    {
                        this.componentFound = true;
                    }
                }
            }
            else
            {
                this.componentFound = true;
            }
        }
        
        public boolean isComponentFound()
        {
            return this.componentFound;
        }
    }

    public static class AddComponentCallback implements ContextCallback
    {
        private final Object[] addedState;
        
        public AddComponentCallback(Object[] addedState)
        {
            this.addedState = addedState;
        }
        
        public void invokeContextCallback(FacesContext context,
                UIComponent target)
        {
            if (addedState[1] != null)
            {
                String facetName = (String) addedState[1];
                UIComponent child
                        = internalRestoreTreeStructure((TreeStructComponent)
                                                       addedState[3]);
                child.processRestoreState(context, addedState[4]);
                target.getFacets().put(facetName,child);
            }
            else
            {
                Integer childIndex = (Integer) addedState[2];
                UIComponent child
                        = internalRestoreTreeStructure((TreeStructComponent)
                                                       addedState[3]);
                child.processRestoreState(context, addedState[4]);
                
                boolean done = false;
                // Is the child a facelet controlled component?
                if (child.getAttributes().containsKey(ComponentSupport.MARK_CREATED))
                {
                    // By effect of c:forEach it is possible that the component can be duplicated
                    // in the component tree, so what we need to do as a fallback is replace the
                    // component in the spot with the restored version.
                    UIComponent parent = target;
                    if (parent.getChildCount() > 0)
                    {
                        String tagId = (String) child.getAttributes().get(ComponentSupport.MARK_CREATED);
                        if (childIndex < parent.getChildCount())
                        {
                            // Try to find the component quickly 
                            UIComponent dup = parent.getChildren().get(childIndex);
                            if (tagId.equals(dup.getAttributes().get(ComponentSupport.MARK_CREATED)))
                            {
                                // Replace
                                parent.getChildren().remove(childIndex.intValue());
                                parent.getChildren().add(childIndex, child);
                                done = true;
                            }
                        }
                        if (!done)
                        {
                            // Fallback to iteration
                            for (int i = 0, childCount = parent.getChildCount(); i < childCount; i ++)
                            {
                                UIComponent dup = parent.getChildren().get(i);
                                if (tagId.equals(dup.getAttributes().get(ComponentSupport.MARK_CREATED)))
                                {
                                    // Replace
                                    parent.getChildren().remove(i);
                                    parent.getChildren().add(i, child);
                                    done = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!done)
                {
                    try
                    {
                        target.getChildren().add(childIndex, child);
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        // We can't be sure about where should be this 
                        // item, so just add it. 
                        target.getChildren().add(child);
                    }
                }
            }
        }
    }

    @Override
    public Object saveView (FacesContext context)
    {
        UIViewRoot view = context.getViewRoot();
        Object states;
        
        if (view == null)
        {
            // Not much that can be done.
            
            return null;
        }
        
        Object serializedView = context.getAttributes()
            .get(SERIALIZED_VIEW_REQUEST_ATTR);
        
        //Note on ajax case the method saveState could be called twice: once before start
        //document rendering and the other one when it is called StateManager.getViewState method.
        if (serializedView == null)
        {
                    
            // Make sure the client IDs are unique per the spec.
            
            if (context.isProjectStage(ProjectStage.Production))
            {
                if (CHECK_ID_PRODUCTION_MODE_AUTO.equals(getCheckIdProductionMode(context)))
                {
                    CheckDuplicateIdFaceletUtils.checkIdsStatefulComponents(context, view);
                }
                else if (CHECK_ID_PRODUCTION_MODE_TRUE.equals(getCheckIdProductionMode(context)))
                {
                    CheckDuplicateIdFaceletUtils.checkIds(context, view);
                }
            }
            else
            {
                CheckDuplicateIdFaceletUtils.checkIds(context, view);
            }
            
            // Create save state objects for every component.
            
            boolean viewResetable = false;
            int count = 0;
            Object faceletViewState = null;
            boolean saveViewFully = view.getAttributes().containsKey(COMPONENT_ADDED_AFTER_BUILD_VIEW);
            if (saveViewFully)
            {
                ensureClearInitialState(view);
                Object rlcStates = !context.getResourceLibraryContracts().isEmpty() ? 
                    UIComponentBase.saveAttachedState(context, 
                                new ArrayList<String>(context.getResourceLibraryContracts())) : null;
                states = new Object[]{
                            internalBuildTreeStructureToSave(view),
                            view.processSaveState(context), rlcStates};
            }
            else
            {
                states = new HashMap<String, Object>();

                faceletViewState = view.getAttributes().get(ComponentSupport.FACELET_STATE_INSTANCE);
                if (faceletViewState != null)
                {
                    ((Map<String, Object>)states).put(ComponentSupport.FACELET_STATE_INSTANCE,
                            UIComponentBase.saveAttachedState(context, faceletViewState));
                    //Do not save on UIViewRoot
                    view.getAttributes().remove(ComponentSupport.FACELET_STATE_INSTANCE);
                }
                if (_viewPoolProcessor != null && 
                    _viewPoolProcessor.isViewPoolEnabledForThisView(context, view))
                {
                    SaveStateAndResetViewCallback cb = saveStateOnMapVisitTreeAndReset(
                            context,(Map<String,Object>) states, view,
                            Boolean.TRUE.equals(
                        context.getAttributes().get(ViewPoolProcessor.FORCE_HARD_RESET)));
                    viewResetable = cb.isViewResetable();
                    count = cb.getCount();
                }
                else
                {
                    saveStateOnMapVisitTree(context,(Map<String,Object>) states, view);
                }
                
                if ( ((Map<String,Object>)states).isEmpty())
                {
                    states = null;
                }
            }
            
            Integer uniqueIdCount = (Integer) view.getAttributes().get(UNIQUE_ID_COUNTER_KEY);
            if (uniqueIdCount != null && !uniqueIdCount.equals(1))
            {
                serializedView = new Object[] { null, states, uniqueIdCount };
            }
            else if (states == null)
            {
                serializedView = EMPTY_STATES;
            }
            else
            {
                serializedView = new Object[] { null, states };
            }
            
            //If view cache enabled store the view state into the pool
            if (!saveViewFully && _viewPoolProcessor != null)
            {
                if (viewResetable)
                {
                    _viewPoolProcessor.pushResetableView(
                        context, view, (FaceletState) faceletViewState);
                }
                else
                {
                    _viewPoolProcessor.pushPartialView(
                        context, view, (FaceletState) faceletViewState, count);
                }
            }
            
            context.getAttributes().put(SERIALIZED_VIEW_REQUEST_ATTR, serializedView);

        }
        
        return serializedView;
    }
    
    private void restoreViewRootOnlyFromMap(
            final FacesContext context, final Object viewState,
            final UIComponent view)
    {
        // Only viewState found, process it but skip tree
        // traversal, saving some time.
        try
        {
            //Restore view
            view.pushComponentToEL(context, view);
            if (viewState != null && !(viewState instanceof AttachedFullStateWrapper))
            {
                try
                {
                    view.restoreState(context, viewState);
                }
                catch(Exception e)
                {
                    throw new IllegalStateException(
                            "Error restoring component: "+
                            view.getClientId(context), e);
                }
            }
        }
        finally
        {
             view.popComponentFromEL(context);
        }
    }
    
    private void restoreStateFromMap(final FacesContext context, final Map<String,Object> states,
            final UIComponent component)
    {
        if (states == null)
        {
            return;
        }
        
        try
        {
            //Restore view
            component.pushComponentToEL(context, component);
            Object state = states.get(component.getClientId(context));
            if (state != null)
            {
                if (state instanceof AttachedFullStateWrapper)
                {
                    //Don't restore this one! It will be restored when the algorithm remove and add it.
                    return;
                }
                try
                {
                    component.restoreState(context, state);
                }
                catch(Exception e)
                {
                    throw new IllegalStateException("Error restoring component: "+component.getClientId(context), e);
                }
            }
    
            //Scan children
            if (component.getChildCount() > 0)
            {
                //String currentClientId = component.getClientId();
                
                List<UIComponent> children  = component.getChildren();
                for (int i = 0; i < children.size(); i++)
                {
                    UIComponent child = children.get(i);
                    if (child != null && !child.isTransient())
                    {
                        restoreStateFromMap( context, states, child);
                    }
                }
            }
    
            //Scan facets
            if (component.getFacetCount() > 0)
            {
                Map<String, UIComponent> facetMap = component.getFacets();
                
                for (Map.Entry<String, UIComponent> entry : facetMap.entrySet())
                {
                    UIComponent child = entry.getValue();
                    if (child != null && !child.isTransient())
                    {
                        //String facetName = entry.getKey();
                        restoreStateFromMap( context, states, child);
                    }
                }
            }
        }
        finally
        {
            component.popComponentFromEL(context);
        }
    }

    static List<String> getClientIdsAdded(UIViewRoot root)
    {
        return (List<String>) root.getAttributes().get(CLIENTIDS_ADDED);
    }
    
    static void setClientsIdsAdded(UIViewRoot root, List<String> clientIdsList)
    {
        root.getAttributes().put(CLIENTIDS_ADDED, clientIdsList);
    }
    
    static List<String> getClientIdsRemoved(UIViewRoot root)
    {
        return (List<String>) root.getAttributes().get(CLIENTIDS_REMOVED);
    }
    
    static void setClientsIdsRemoved(UIViewRoot root, List<String> clientIdsList)
    {
        root.getAttributes().put(CLIENTIDS_REMOVED, clientIdsList);
    }
    
    @SuppressWarnings("unchecked")
    private void registerOnAddRemoveList(FacesContext facesContext, String clientId)
    {
        UIViewRoot uiViewRoot = facesContext.getViewRoot();

        List<String> clientIdsAdded = (List<String>) getClientIdsAdded(uiViewRoot);
        if (clientIdsAdded == null)
        {
            //Create a set that preserve insertion order
            clientIdsAdded = new ArrayList<String>();
        }
        clientIdsAdded.add(clientId);

        setClientsIdsAdded(uiViewRoot, clientIdsAdded);

        List<String> clientIdsRemoved = (List<String>) getClientIdsRemoved(uiViewRoot);
        if (clientIdsRemoved == null)
        {
            //Create a set that preserve insertion order
            clientIdsRemoved = new ArrayList<String>();
        }

        clientIdsRemoved.add(clientId);

        setClientsIdsRemoved(uiViewRoot, clientIdsRemoved);
    }
    
    @SuppressWarnings("unchecked")
    private void registerOnAddList(FacesContext facesContext, String clientId)
    {
        UIViewRoot uiViewRoot = facesContext.getViewRoot();

        List<String> clientIdsAdded = (List<String>) getClientIdsAdded(uiViewRoot);
        if (clientIdsAdded == null)
        {
            //Create a set that preserve insertion order
            clientIdsAdded = new ArrayList<String>();
        }
        clientIdsAdded.add(clientId);

        setClientsIdsAdded(uiViewRoot, clientIdsAdded);
    }

    private void saveStateOnMapVisitTree(final FacesContext facesContext, final Map<String,Object> states,
            final UIViewRoot uiViewRoot)
    {
        facesContext.getAttributes().put(SKIP_ITERATION_HINT, Boolean.TRUE);
        try
        {
            uiViewRoot.visitTree( getVisitContextFactory().getVisitContext(
                    facesContext, null, VISIT_HINTS), new VisitCallback()
            {
                public VisitResult visit(VisitContext context, UIComponent target)
                {
                    FacesContext facesContext = context.getFacesContext();
                    Object state;
                    
                    if ((target == null) || target.isTransient())
                    {
                        // No need to bother with these components or their children.
                        
                        return VisitResult.REJECT;
                    }
                    
                    ComponentState componentAddedAfterBuildView
                            = (ComponentState) target.getAttributes().get(COMPONENT_ADDED_AFTER_BUILD_VIEW);
                    
                    //Note if UIViewRoot has this marker, JSF 1.2 like state saving is used.
                    if (componentAddedAfterBuildView != null && (target.getParent() != null))
                    {
                        if (ComponentState.REMOVE_ADD.equals(componentAddedAfterBuildView))
                        {
                            registerOnAddRemoveList(facesContext, target.getClientId(facesContext));
                            target.getAttributes().put(COMPONENT_ADDED_AFTER_BUILD_VIEW, ComponentState.ADDED);
                        }
                        else if (ComponentState.ADD.equals(componentAddedAfterBuildView))
                        {
                            registerOnAddList(facesContext, target.getClientId(facesContext));
                            target.getAttributes().put(COMPONENT_ADDED_AFTER_BUILD_VIEW, ComponentState.ADDED);
                        }
                        else if (ComponentState.ADDED.equals(componentAddedAfterBuildView))
                        {
                            registerOnAddList(facesContext, target.getClientId(facesContext));
                        }
                        ensureClearInitialState(target);
                        //Save all required info to restore the subtree.
                        //This includes position, structure and state of subtree
                        
                        int childIndex = target.getParent().getChildren().indexOf(target);
                        if (childIndex >= 0)
                        {
                            states.put(target.getClientId(facesContext), new AttachedFullStateWrapper( 
                                    new Object[]{
                                        target.getParent().getClientId(facesContext),
                                        null,
                                        childIndex,
                                        internalBuildTreeStructureToSave(target),
                                        target.processSaveState(facesContext)}));
                        }
                        else
                        {
                            String facetName = null;
                            if (target.getParent().getFacetCount() > 0)
                            {
                                for (Map.Entry<String, UIComponent> entry : target.getParent().getFacets().entrySet()) 
                                {
                                    if (target.equals(entry.getValue()))
                                    {
                                        facetName = entry.getKey();
                                        break;
                                    }
                                }
                            }
                            states.put(target.getClientId(facesContext),new AttachedFullStateWrapper(new Object[]{
                                    target.getParent().getClientId(facesContext),
                                    facetName,
                                    null,
                                    internalBuildTreeStructureToSave(target),
                                    target.processSaveState(facesContext)}));
                        }
                        return VisitResult.REJECT;
                    }
                    else if (target.getParent() != null)
                    {
                        state = target.saveState (facesContext);
                        
                        if (state != null)
                        {
                            // Save by client ID into our map.
                            
                            states.put (target.getClientId (facesContext), state);
                        }
                        
                        return VisitResult.ACCEPT;
                    }
                    else
                    {
                        //Only UIViewRoot has no parent in a component tree.
                        return VisitResult.ACCEPT;
                    }
                }
            });
        }
        finally
        {
            facesContext.getAttributes().remove(SKIP_ITERATION_HINT);
        }
        if (!uiViewRoot.isTransient())
        {
            Object state = uiViewRoot.saveState (facesContext);
            if (state != null)
            {
                // Save by client ID into our map.

                states.put (uiViewRoot.getClientId (facesContext), state);
            }
        }
    }
    
    
    private SaveStateAndResetViewCallback saveStateOnMapVisitTreeAndReset(final FacesContext facesContext,
            final Map<String,Object> states, final UIViewRoot uiViewRoot, boolean forceHardReset)
    {
        facesContext.getAttributes().put(SKIP_ITERATION_HINT, Boolean.TRUE);
        SaveStateAndResetViewCallback callback = new SaveStateAndResetViewCallback(
                facesContext.getViewRoot(), states, forceHardReset);
        if (forceHardReset)
        {
            uiViewRoot.getAttributes().put(ViewPoolProcessor.RESET_SAVE_STATE_MODE_KEY, 
                    ViewPoolProcessor.RESET_MODE_HARD);
        }
        else
        {
            uiViewRoot.getAttributes().put(ViewPoolProcessor.RESET_SAVE_STATE_MODE_KEY, 
                    ViewPoolProcessor.RESET_MODE_SOFT);
        }
        try
        {
            if (_viewPoolProcessor != null && 
                !_viewPoolProcessor.isViewPoolEnabledForThisView(facesContext, uiViewRoot))
            {
                callback.setViewResetable(false);
            }
            
            // Check if the view has removed components. If that so, it
            // means there is some manipulation over the component tree that
            // can be rollback, so it is ok to set the view as resetable.
            if (callback.isViewResetable())
            {
                List<String> removedIds = getClientIdsRemoved(uiViewRoot);
                if (removedIds != null && !removedIds.isEmpty())
                {
                    callback.setViewResetable(false);
                }
            }

            try
            {
                uiViewRoot.visitTree( getVisitContextFactory().getVisitContext(
                        facesContext, null, VISIT_HINTS), callback);
            }
            finally
            {
                facesContext.getAttributes().remove(SKIP_ITERATION_HINT);
            }
            
            if (callback.isViewResetable() && callback.isRemoveAddedComponents())
            {
                List<String> clientIdsToRemove = getClientIdsAdded(uiViewRoot);

                if (clientIdsToRemove != null)
                {
                    // perf: clientIds are ArrayList: see method registerOnAddRemoveList(String)
                    for (int i = 0, size = clientIdsToRemove.size(); i < size; i++)
                    {
                        String clientId = clientIdsToRemove.get(i);
                        uiViewRoot.invokeOnComponent(facesContext, clientId, new RemoveComponentCallback());
                    }
                }
            }

            Object state = uiViewRoot.saveState (facesContext);
            if (state != null)
            {
                // Save by client ID into our map.
                states.put (uiViewRoot.getClientId (facesContext), state);

                //Hard reset (or reset and check state again)
                Integer oldResetMode = (Integer) uiViewRoot.getAttributes().put(
                        ViewPoolProcessor.RESET_SAVE_STATE_MODE_KEY, ViewPoolProcessor.RESET_MODE_HARD);
                state = uiViewRoot.saveState (facesContext);
                uiViewRoot.getAttributes().put(ViewPoolProcessor.RESET_SAVE_STATE_MODE_KEY, oldResetMode);
                if (state != null)
                {
                    callback.setViewResetable(false);
                }
            }
        }
        finally
        {
            uiViewRoot.getAttributes().put(ViewPoolProcessor.RESET_SAVE_STATE_MODE_KEY, 
                    ViewPoolProcessor.RESET_MODE_OFF);
        }
        return callback;
    }
    
    private class SaveStateAndResetViewCallback implements VisitCallback
    {
        private final Map<String, Object> states;
        
        private final UIViewRoot view;
        
        private boolean viewResetable;
        
        private boolean skipRoot;
        
        private int count;
        
        private boolean forceHardReset;
        
        private boolean removeAddedComponents;
        
        public SaveStateAndResetViewCallback(UIViewRoot view, Map<String, Object> states,
                boolean forceHardReset)
        {
            this.states = states;
            this.view = view;
            this.viewResetable = true;
            this.skipRoot = true;
            this.count = 0;
            this.forceHardReset = forceHardReset;
            this.removeAddedComponents = false;
        }
        
        public VisitResult visit(VisitContext context, UIComponent target)
        {
            FacesContext facesContext = context.getFacesContext();
            Object state;
            this.count++;

            if ((target == null) || target.isTransient())
            {
                // No need to bother with these components or their children.

                return VisitResult.REJECT;
            }
            
            if (skipRoot && target instanceof UIViewRoot)
            {
                //UIViewRoot should be scanned at last.
                skipRoot = false;
                return VisitResult.ACCEPT;
            }

            ComponentState componentAddedAfterBuildView
                    = (ComponentState) target.getAttributes().get(COMPONENT_ADDED_AFTER_BUILD_VIEW);

            //Note if UIViewRoot has this marker, JSF 1.2 like state saving is used.
            if (componentAddedAfterBuildView != null && (target.getParent() != null))
            {
                //Set this view as not resetable.
                //setViewResetable(false);
                // Enable flag to remove added components later
                setRemoveAddedComponents(true);
                if (forceHardReset)
                {
                    // The ideal is remove the added component here but visitTree does not support that
                    // kind of tree manipulation.
                    if (isViewResetable() &&
                        ComponentState.REMOVE_ADD.equals(componentAddedAfterBuildView))
                    {
                        setViewResetable(false);
                    }
                    // it is not important to save anything, skip
                    return VisitResult.REJECT;
                }
                if (ComponentState.REMOVE_ADD.equals(componentAddedAfterBuildView))
                {
                    //If the view has removed components, set the view as non resetable
                    setViewResetable(false);
                    registerOnAddRemoveList(facesContext, target.getClientId(facesContext));
                    target.getAttributes().put(COMPONENT_ADDED_AFTER_BUILD_VIEW, ComponentState.ADDED);
                }
                else if (ComponentState.ADD.equals(componentAddedAfterBuildView))
                {
                    registerOnAddList(facesContext, target.getClientId(facesContext));
                    target.getAttributes().put(COMPONENT_ADDED_AFTER_BUILD_VIEW, ComponentState.ADDED);
                }
                else if (ComponentState.ADDED.equals(componentAddedAfterBuildView))
                {
                    // Later on the check of removed components we'll see if the view
                    // is resetable or not.
                    registerOnAddList(facesContext, target.getClientId(facesContext));
                }
                ensureClearInitialState(target);
                //Save all required info to restore the subtree.
                //This includes position, structure and state of subtree

                int childIndex = target.getParent().getChildren().indexOf(target);
                if (childIndex >= 0)
                {
                    states.put(target.getClientId(facesContext), new AttachedFullStateWrapper( 
                            new Object[]{
                                target.getParent().getClientId(facesContext),
                                null,
                                childIndex,
                                internalBuildTreeStructureToSave(target),
                                target.processSaveState(facesContext)}));
                }
                else
                {
                    String facetName = null;
                    if (target.getParent().getFacetCount() > 0)
                    {
                        for (Map.Entry<String, UIComponent> entry : target.getParent().getFacets().entrySet()) 
                        {
                            if (target.equals(entry.getValue()))
                            {
                                facetName = entry.getKey();
                                break;
                            }
                        }
                    }
                    states.put(target.getClientId(facesContext),new AttachedFullStateWrapper(new Object[]{
                            target.getParent().getClientId(facesContext),
                            facetName,
                            null,
                            internalBuildTreeStructureToSave(target),
                            target.processSaveState(facesContext)}));
                }
                return VisitResult.REJECT;
            }
            else if (target.getParent() != null)
            {
                if (forceHardReset)
                {
                    // force hard reset set reset move on top
                    state = target.saveState (facesContext);
                    if (state != null)
                    {
                        setViewResetable(false);
                        return VisitResult.REJECT;
                    }
                }
                else
                {
                    state = target.saveState (facesContext);

                    if (state != null)
                    {
                        // Save by client ID into our map.
                        states.put (target.getClientId (facesContext), state);

                        if (isViewResetable())
                        {
                            //Hard reset (or reset and check state again)
                            Integer oldResetMode = (Integer) view.getAttributes().put(
                                    ViewPoolProcessor.RESET_SAVE_STATE_MODE_KEY, 
                                    ViewPoolProcessor.RESET_MODE_HARD);
                            state = target.saveState (facesContext);
                            view.getAttributes().put(ViewPoolProcessor.RESET_SAVE_STATE_MODE_KEY, 
                                    oldResetMode);
                            if (state != null)
                            {
                                setViewResetable(false);
                            }
                        }
                    }
                }

                return VisitResult.ACCEPT;
            }
            else
            {
                //Only UIViewRoot has no parent in a component tree.
                return VisitResult.ACCEPT;
            }
        }
        
        /**
         * @return the viewResetable
         */
        public boolean isViewResetable()
        {
            return viewResetable;
        }

        /**
         * @param viewResetable the viewResetable to set
         */
        public void setViewResetable(boolean viewResetable)
        {
            this.viewResetable = viewResetable;
        }
        
        public int getCount()
        {
            return count;
        }

        /**
         * @return the removeAddedComponents
         */
        public boolean isRemoveAddedComponents()
        {
            return removeAddedComponents;
        }

        /**
         * @param removeAddedComponents the removeAddedComponents to set
         */
        public void setRemoveAddedComponents(boolean removeAddedComponents)
        {
            this.removeAddedComponents = removeAddedComponents;
        }
    }
    
    protected void ensureClearInitialState(UIComponent c)
    {
        c.clearInitialState();
        if (c.getChildCount() > 0)
        {
            for (int i = 0, childCount = c.getChildCount(); i < childCount; i++)
            {
                UIComponent child = c.getChildren().get(i);
                ensureClearInitialState(child);
            }
        }
        if (c.getFacetCount() > 0)
        {
            for (UIComponent child : c.getFacets().values())
            {
                ensureClearInitialState(child);
            }
        }
    }
    
    public void suscribeListeners(UIViewRoot uiViewRoot)
    {
        boolean listenerSubscribed = false;
        List<SystemEventListener> pavList = uiViewRoot.getViewListenersForEventClass(PostAddToViewEvent.class);
        if (pavList != null)
        {
            for (SystemEventListener listener : pavList)
            {
                if (listener instanceof PostAddPreRemoveFromViewListener)
                {
                    listenerSubscribed = true;
                    break;
                }
            }
        }
        if (!listenerSubscribed)
        {
            PostAddPreRemoveFromViewListener componentListener = new PostAddPreRemoveFromViewListener();
            uiViewRoot.subscribeToViewEvent(PostAddToViewEvent.class, componentListener);
            uiViewRoot.subscribeToViewEvent(PreRemoveFromViewEvent.class, componentListener);
        }
    }
    
    protected RenderKitFactory getRenderKitFactory()
    {
        if (_renderKitFactory == null)
        {
            _renderKitFactory = (RenderKitFactory)FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
        }
        return _renderKitFactory;
    }
    
    protected VisitContextFactory getVisitContextFactory()
    {
        if (_visitContextFactory == null)
        {
            _visitContextFactory = (VisitContextFactory)FactoryFinder.getFactory(FactoryFinder.VISIT_CONTEXT_FACTORY);
        }
        return _visitContextFactory;
    }

    protected String getCheckIdProductionMode(FacesContext facesContext)
    {
        if (_checkIdsProductionMode == null)
        {
            _checkIdsProductionMode
                    = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(),
                    CHECK_ID_PRODUCTION_MODE, CHECK_ID_PRODUCTION_MODE_DEFAULT); //default (auto)
        }
        return _checkIdsProductionMode;
    }

    
    public static class PostAddPreRemoveFromViewListener implements SystemEventListener
    {
        private transient FacesContext _facesContext;
        
        private transient Boolean _isRefreshOnTransientBuildPreserveState;

        public boolean isListenerForSource(Object source)
        {
            // PostAddToViewEvent and PreRemoveFromViewEvent are
            // called from UIComponentBase.setParent
            return (source instanceof UIComponent);
        }
        
        private boolean isRefreshOnTransientBuildPreserveState()
        {
            if (_isRefreshOnTransientBuildPreserveState == null)
            {
                _isRefreshOnTransientBuildPreserveState = MyfacesConfig.getCurrentInstance(
                        _facesContext.getExternalContext()).isRefreshTransientBuildOnPSSPreserveState();
            }
            return _isRefreshOnTransientBuildPreserveState;
        }

        public void processEvent(SystemEvent event)
        {
            UIComponent component = (UIComponent) event.getSource();
            
            if (component.isTransient())
            {
                return;
            }
            
            // This is a view listener. It is not saved on the state and this listener
            // is suscribed each time the view is restored, so we can cache facesContext
            // here
            if (_facesContext == null)
            {
                _facesContext = FacesContext.getCurrentInstance();
            }
            
            if (event instanceof PostAddToViewEvent)
            {
                if (!isRefreshOnTransientBuildPreserveState() &&
                    Boolean.TRUE.equals(_facesContext.getAttributes().get(StateManager.IS_BUILDING_INITIAL_STATE)))
                {
                    return;
                }

                //PostAddToViewEvent
                component.getAttributes().put(COMPONENT_ADDED_AFTER_BUILD_VIEW, ComponentState.ADD);
            }
            else
            {
                //FacesContext facesContext = FacesContext.getCurrentInstance();
                // In this case if we are removing components on build, it is not necessary to register
                // again the current id, and its more, it could cause a concurrent exception. But note
                // we need to propagate PreRemoveFromViewEvent, otherwise the view will not be restored
                // correctly.
                if (FaceletViewDeclarationLanguage.isRemovingComponentBuild(_facesContext))
                {
                    return;
                }

                if (!isRefreshOnTransientBuildPreserveState() &&
                    FaceletCompositionContext.getCurrentInstance(_facesContext) != null &&
                    (component.getAttributes().containsKey(ComponentSupport.MARK_CREATED) ||
                     component.getAttributes().containsKey(ComponentSupport.COMPONENT_ADDED_BY_HANDLER_MARKER))
                    )
                {
                    // Components removed by facelets algorithm does not need to be registered
                    // unless preserve state mode is used, because PSS initial state is changed
                    // to restore delta properly.
                    // MYFACES-3554 It is possible to find use cases where a component
                    // created by a facelet tag is changed dynamically in some way in render
                    // response time, so we need to check here also when facelets algorithm
                    // is running or not. 
                    return;
                }
                
                //PreRemoveFromViewEvent
                UIViewRoot uiViewRoot = _facesContext.getViewRoot();
                
                List<String> clientIdsRemoved = getClientIdsRemoved(uiViewRoot);
                if (clientIdsRemoved == null)
                {
                    //Create a set that preserve insertion order
                    clientIdsRemoved = new ArrayList<String>();
                }
                clientIdsRemoved.add(component.getClientId(_facesContext));
                setClientsIdsRemoved(uiViewRoot, clientIdsRemoved);
            }
        }
    }
    
    private static TreeStructComponent internalBuildTreeStructureToSave(UIComponent component)
    {
        TreeStructComponent structComp = new TreeStructComponent(component.getClass().getName(),
                                                                 component.getId());

        //children
        if (component.getChildCount() > 0)
        {
            List<TreeStructComponent> structChildList = new ArrayList<TreeStructComponent>();
            for (int i = 0, childCount = component.getChildCount(); i < childCount; i++)
            {
                UIComponent child = component.getChildren().get(i);     
                if (!child.isTransient())
                {
                    TreeStructComponent structChild = internalBuildTreeStructureToSave(child);
                    structChildList.add(structChild);
                }
            }
            
            TreeStructComponent[] childArray = structChildList.toArray(new TreeStructComponent[structChildList.size()]);
            structComp.setChildren(childArray);
        }

        //facets
        
        if (component.getFacetCount() > 0)
        {
            Map<String, UIComponent> facetMap = component.getFacets();
            List<Object[]> structFacetList = new ArrayList<Object[]>();
            for (Map.Entry<String, UIComponent> entry : facetMap.entrySet())
            {
                UIComponent child = entry.getValue();
                if (!child.isTransient())
                {
                    String facetName = entry.getKey();
                    TreeStructComponent structChild = internalBuildTreeStructureToSave(child);
                    structFacetList.add(new Object[] {facetName, structChild});
                }
            }
            
            Object[] facetArray = structFacetList.toArray(new Object[structFacetList.size()]);
            structComp.setFacets(facetArray);
        }

        return structComp;
    }
    
    private static UIComponent internalRestoreTreeStructure(TreeStructComponent treeStructComp)
    {
        String compClass = treeStructComp.getComponentClass();
        String compId = treeStructComp.getComponentId();
        UIComponent component = (UIComponent)ClassUtils.newInstance(compClass);
        component.setId(compId);

        //children
        TreeStructComponent[] childArray = treeStructComp.getChildren();
        if (childArray != null)
        {
            List<UIComponent> childList = component.getChildren();
            for (int i = 0, len = childArray.length; i < len; i++)
            {
                UIComponent child = internalRestoreTreeStructure(childArray[i]);
                childList.add(child);
            }
        }

        //facets
        Object[] facetArray = treeStructComp.getFacets();
        if (facetArray != null)
        {
            Map<String, UIComponent> facetMap = component.getFacets();
            for (int i = 0, len = facetArray.length; i < len; i++)
            {
                Object[] tuple = (Object[])facetArray[i];
                String facetName = (String)tuple[0];
                TreeStructComponent structChild = (TreeStructComponent)tuple[1];
                UIComponent child = internalRestoreTreeStructure(structChild);
                facetMap.put(facetName, child);
            }
        }

        return component;
    }

    public static class TreeStructComponent implements Serializable
    {
        private static final long serialVersionUID = 5069109074684737231L;
        private String _componentClass;
        private String _componentId;
        private TreeStructComponent[] _children = null; // Array of children
        private Object[] _facets = null; // Array of Array-tuples with Facetname and TreeStructComponent

        TreeStructComponent(String componentClass, String componentId)
        {
            _componentClass = componentClass;
            _componentId = componentId;
        }

        public String getComponentClass()
        {
            return _componentClass;
        }

        public String getComponentId()
        {
            return _componentId;
        }

        void setChildren(TreeStructComponent[] children)
        {
            _children = children;
        }

        TreeStructComponent[] getChildren()
        {
            return _children;
        }

        Object[] getFacets()
        {
            return _facets;
        }

        void setFacets(Object[] facets)
        {
            _facets = facets;
        }
    }
    
}
