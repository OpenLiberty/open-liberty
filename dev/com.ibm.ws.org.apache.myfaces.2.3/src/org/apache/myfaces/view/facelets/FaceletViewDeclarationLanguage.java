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

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.application.Resource;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewVisitOption;
import javax.faces.component.ActionSource2;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIPanel;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitHint;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.event.MethodExpressionActionListener;
import javax.faces.event.MethodExpressionValueChangeListener;
import javax.faces.event.PhaseId;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PostRestoreStateEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import javax.faces.render.RenderKit;
import javax.faces.render.ResponseStateManager;
import javax.faces.validator.MethodExpressionValidator;
import javax.faces.validator.Validator;
import javax.faces.view.ActionSource2AttachedObjectHandler;
import javax.faces.view.ActionSource2AttachedObjectTarget;
import javax.faces.view.AttachedObjectHandler;
import javax.faces.view.AttachedObjectTarget;
import javax.faces.view.BehaviorHolderAttachedObjectHandler;
import javax.faces.view.BehaviorHolderAttachedObjectTarget;
import javax.faces.view.EditableValueHolderAttachedObjectHandler;
import javax.faces.view.EditableValueHolderAttachedObjectTarget;
import javax.faces.view.StateManagementStrategy;
import javax.faces.view.ValueHolderAttachedObjectHandler;
import javax.faces.view.ValueHolderAttachedObjectTarget;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewMetadata;
import javax.faces.view.facelets.Facelet;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.ResourceResolver;
import javax.servlet.http.HttpServletResponse;
import org.apache.myfaces.application.StateManagerImpl;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFWebConfigParam;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.shared.application.DefaultViewHandlerSupport;
import org.apache.myfaces.shared.application.ViewHandlerSupport;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.shared.util.StringUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.view.ViewDeclarationLanguageStrategy;
import org.apache.myfaces.view.ViewMetadataBase;
import org.apache.myfaces.view.facelets.compiler.Compiler;
import org.apache.myfaces.view.facelets.compiler.SAXCompiler;
import org.apache.myfaces.view.facelets.el.CompositeComponentELUtils;
import org.apache.myfaces.view.facelets.el.LocationMethodExpression;
import org.apache.myfaces.view.facelets.el.LocationValueExpression;
import org.apache.myfaces.view.facelets.el.MethodExpressionMethodExpression;
import org.apache.myfaces.view.facelets.el.RedirectMethodExpressionValueExpressionActionListener;
import org.apache.myfaces.view.facelets.el.RedirectMethodExpressionValueExpressionValidator;
import org.apache.myfaces.view.facelets.el.RedirectMethodExpressionValueExpressionValueChangeListener;
import org.apache.myfaces.view.facelets.el.ValueExpressionMethodExpression;
import org.apache.myfaces.view.facelets.el.VariableMapperWrapper;
import org.apache.myfaces.view.facelets.impl.DefaultFaceletFactory;
import org.apache.myfaces.view.facelets.impl.DefaultResourceResolver;
import org.apache.myfaces.view.facelets.tag.composite.ClientBehaviorAttachedObjectTarget;
import org.apache.myfaces.view.facelets.tag.composite.ClientBehaviorRedirectBehaviorAttachedObjectHandlerWrapper;
import org.apache.myfaces.view.facelets.tag.composite.ClientBehaviorRedirectEventComponentWrapper;
import org.apache.myfaces.view.facelets.tag.jsf.ComponentSupport;
import org.apache.myfaces.view.facelets.tag.jsf.core.AjaxHandler;
import org.apache.myfaces.view.facelets.tag.ui.UIDebug;

import static org.apache.myfaces.view.facelets.DefaultFaceletsStateManagementStrategy.*;
import org.apache.myfaces.view.facelets.compiler.FaceletsCompilerSupport;
import org.apache.myfaces.view.facelets.compiler.RefreshDynamicComponentListener;
import org.apache.myfaces.view.facelets.impl.SectionUniqueIdCounter;
import org.apache.myfaces.view.facelets.pool.RestoreViewFromPoolResult;
import org.apache.myfaces.view.facelets.pool.ViewEntry;
import org.apache.myfaces.view.facelets.pool.ViewPool;
import org.apache.myfaces.view.facelets.pool.ViewStructureMetadata;
import org.apache.myfaces.view.facelets.tag.composite.CreateDynamicCompositeComponentListener;
import org.apache.myfaces.view.facelets.tag.jsf.PartialMethodExpressionActionListener;
import org.apache.myfaces.view.facelets.tag.jsf.PartialMethodExpressionValidator;
import org.apache.myfaces.view.facelets.tag.jsf.PartialMethodExpressionValueChangeListener;
import org.apache.myfaces.view.facelets.util.FaceletsTemplateMappingUtils;
import org.apache.myfaces.view.facelets.util.FaceletsViewDeclarationLanguageUtils;

/**
 * This class represents the abstraction of Facelets as a ViewDeclarationLanguage.
 *
 * @author Simon Lessard (latest modification by $Author$)
 * @version $Revision$ $Date$
 *
 * @since 2.0
 */
public class FaceletViewDeclarationLanguage extends FaceletViewDeclarationLanguageBase
{
    private static final Logger log = Logger.getLogger(FaceletViewDeclarationLanguage.class.getName());

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    private static final Class<?>[] VALUE_CHANGE_LISTENER_SIGNATURE = new Class[]{ValueChangeEvent.class};

    private static final Class<?>[] ACTION_LISTENER_SIGNATURE = new Class[]{ActionEvent.class};

    private static final Class<?>[] VALIDATOR_SIGNATURE
            = new Class[]{FacesContext.class, UIComponent.class, Object.class};

    public static final String CHARACTER_ENCODING_KEY = "javax.faces.request.charset";

    public final static long DEFAULT_REFRESH_PERIOD = 0;
    public final static long DEFAULT_REFRESH_PERIOD_PRODUCTION = -1;

    public final static String DEFAULT_CHARACTER_ENCODING = "UTF-8";

    /**
     * Define the default buffer size value passed to ExternalContext.setResponseBufferResponse() and in a
     * servlet environment to HttpServletResponse.setBufferSize().
     */
    @JSFWebConfigParam(since = "2.0", deprecated = true, classType = "java.lang.Integer")
    private final static String PARAM_BUFFER_SIZE_DEPRECATED = "facelets.BUFFER_SIZE";

    private final static String[] PARAMS_BUFFER_SIZE = {ViewHandler.FACELETS_BUFFER_SIZE_PARAM_NAME,
        PARAM_BUFFER_SIZE_DEPRECATED};

    //private final static String PARAM_BUILD_BEFORE_RESTORE = "facelets.BUILD_BEFORE_RESTORE";

    /**
     * Constant used by EncodingHandler to indicate the current encoding of the page being built,
     * and indicate which one is the response encoding on getResponseEncoding(FacesContext, String) method.
     */
    public final static String PARAM_ENCODING = "facelets.Encoding";

    /**
     * Define the period used to refresh the facelet abstract syntax tree from the view definition file. 
     *
     * <p>By default is infinite (no active).</p>
     */
    @JSFWebConfigParam(since = "2.0", defaultValue = "-1", deprecated = true)
    public final static String PARAM_REFRESH_PERIOD_DEPRECATED = "facelets.REFRESH_PERIOD";

    public final static String[] PARAMS_REFRESH_PERIOD = {ViewHandler.FACELETS_REFRESH_PERIOD_PARAM_NAME,
        PARAM_REFRESH_PERIOD_DEPRECATED};

    /**
     * Class implementing ResourceResolver interface used to locate facelet resources. 
     */
    @JSFWebConfigParam(since = "2.0", alias = "facelets.RESOURCE_RESOLVER")
    public final static String PARAM_RESOURCE_RESOLVER = "javax.faces.FACELETS_RESOURCE_RESOLVER";

    /**
     * Class implementing ResourceResolver interface used to locate facelet resources.
     */
    @JSFWebConfigParam(since = "2.0", deprecated = true)
    private final static String PARAM_RESOURCE_RESOLVER_DEPRECATED = "facelets.RESOURCE_RESOLVER";

    private final static String[] PARAMS_RESOURCE_RESOLVER
            = {PARAM_RESOURCE_RESOLVER, PARAM_RESOURCE_RESOLVER_DEPRECATED};

    @JSFWebConfigParam(since = "2.1", defaultValue = "false", expectedValues = "true, false", tags = "performance")
    private final static String PARAM_MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW
            = "org.apache.myfaces.MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW";

    public final static String FILLED_VIEW = "org.apache.myfaces.FILLED_VIEW";

    //BEGIN CONSTANTS SET ON BUILD VIEW

    public final static String BUILDING_VIEW_METADATA = "org.apache.myfaces.BUILDING_VIEW_METADATA";

    public final static String REFRESHING_TRANSIENT_BUILD = "org.apache.myfaces.REFRESHING_TRANSIENT_BUILD";

    public final static String REFRESH_TRANSIENT_BUILD_ON_PSS = "org.apache.myfaces.REFRESH_TRANSIENT_BUILD_ON_PSS";

    public final static String USING_PSS_ON_THIS_VIEW = "org.apache.myfaces.USING_PSS_ON_THIS_VIEW";

    public final static String REMOVING_COMPONENTS_BUILD = "org.apache.myfaces.REMOVING_COMPONENTS_BUILD";
    //END CONSTANTS SET ON BUILD VIEW

    /**
     * Marker to indicate tag handlers the view currently being built is using
     * partial state saving and it is necessary to call UIComponent.markInitialState
     * after component instances are populated. 
     */
    public final static String MARK_INITIAL_STATE_KEY = "org.apache.myfaces.MARK_INITIAL_STATE";
    
    public final static String IS_BUILDING_INITIAL_STATE_KEY_ALIAS
            = "javax.faces.view.ViewDeclarationLanguage.IS_BUILDING_INITIAL_STATE";

    public final static String CLEAN_TRANSIENT_BUILD_ON_RESTORE
            = "org.apache.myfaces.CLEAN_TRANSIENT_BUILD_ON_RESTORE";

    private final static String STATE_KEY = "<!--@@JSF_FORM_STATE_MARKER@@-->";

    private final static int STATE_KEY_LEN = STATE_KEY.length();
    
    private static final Set<VisitHint> VISIT_HINTS_DYN_REFRESH = Collections.unmodifiableSet( 
            EnumSet.of(VisitHint.SKIP_ITERATION));
    
    /**
     * Key used to cache component ids for the counter
     */
    public final static String CACHED_COMPONENT_IDS = "oam.CACHED_COMPONENT_IDS"; 
    
    private static final String ASTERISK = "*";

    private int _bufferSize;

    // This param evolve in jsf 2.0 to partial state saving
    //private boolean _buildBeforeRestore = false;

    private ViewHandlerSupport _cachedViewHandlerSupport;

    private String _defaultSuffix;

    private FaceletFactory _faceletFactory;

    private StateManagementStrategy _stateMgmtStrategy;
    
    private boolean _partialStateSaving;

    private boolean _refreshTransientBuildOnPSS;

    private boolean _refreshTransientBuildOnPSSAuto;

    private Set<String> _viewIds;

    private boolean _markInitialStateWhenApplyBuildView;

    private final ViewDeclarationLanguageStrategy _strategy;

    private ResourceResolver _resourceResolver;
    
    private Map<String, List<String>> _contractMappings;
    private List<String> _prefixWildcardKeys;
    
    private FaceletsCompilerSupport _faceletsCompilerSupport;
    
    private MyfacesConfig _config;
    
    private ViewPoolProcessor _viewPoolProcessor;

    /**
     *
     */
    public FaceletViewDeclarationLanguage(FacesContext context)
    {
        _config = MyfacesConfig.getCurrentInstance(context.getExternalContext());
        initialize(context);
        _strategy = new FaceletViewDeclarationLanguageStrategy();
    }

    public FaceletViewDeclarationLanguage(FacesContext context, ViewDeclarationLanguageStrategy strategy)
    {
        _config = MyfacesConfig.getCurrentInstance(context.getExternalContext());
        initialize(context);
        _strategy = strategy;
    }


    @Override
    public String getId()
    {
        return ViewDeclarationLanguage.FACELETS_VIEW_DECLARATION_LANGUAGE_ID;
    }

    @Override
    public boolean viewExists(FacesContext facesContext, String viewId)
    {
        if (_strategy.handles(viewId))
        {
            if (_resourceResolver instanceof DefaultResourceResolver)
            {
                return ((DefaultResourceResolver)_resourceResolver).resolveUrl(facesContext, viewId) != null;
            }
            else
            {
                return _resourceResolver.resolveUrl(viewId) != null;
            }
        }
        return false;
    }

    private RestoreViewFromPoolResult tryRestoreViewFromCache(FacesContext context, UIViewRoot view)
    {
        if (_viewPoolProcessor != null)
        {
            ViewPool viewPool = _viewPoolProcessor.getViewPool(context, view);
            if (viewPool != null)
            {
                ViewStructureMetadata metadata = viewPool.retrieveStaticViewStructureMetadata(context, view);
                if (metadata != null)
                {
                    ViewEntry entry = viewPool.popStaticOrPartialStructureView(context, view);
                    if (entry != null)
                    {
                        _viewPoolProcessor.cloneAndRestoreView(context, view, entry, metadata);
                        return entry.getResult();
                    }
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildView(FacesContext context, UIViewRoot view) throws IOException
    {
        if (isFilledView(context, view))
        {
            if (view != null && 
                FaceletViewDeclarationLanguageBase.isDynamicComponentRefreshTransientBuildActive(context, view))
            {
                // don't return
            }
            else
            {
                return;
            }
        }

        // setup our viewId
        String previousViewId = view.getViewId();
        String renderedViewId = getRenderedViewId(context, previousViewId);

        if (renderedViewId == null)
        {
            view.setViewId(renderedViewId);
        }
        else if (!renderedViewId.equals(previousViewId))
        {
            view.setViewId(renderedViewId);
        }

        if (log.isLoggable(Level.FINEST))
        {
            log.finest("Building View: " + renderedViewId);
        }

        boolean usePartialStateSavingOnThisView = _usePartialStateSavingOnThisView(renderedViewId);
        boolean refreshTransientBuild = (view.getChildCount() > 0);
        boolean refreshTransientBuildOnPSS = (usePartialStateSavingOnThisView && _refreshTransientBuildOnPSS);
        boolean refreshPartialView = false;

        if (_viewPoolProcessor != null && !refreshTransientBuild)
        {
            RestoreViewFromPoolResult result = tryRestoreViewFromCache(context, view);
            if (result != null)
            {
                // Since all transient stuff has been removed, add listeners that keep
                // track of tree updates.
                if (RestoreViewFromPoolResult.COMPLETE.equals(result))
                {
                    if (!PhaseId.RESTORE_VIEW.equals(context.getCurrentPhaseId()))
                    {
                        ((DefaultFaceletsStateManagementStrategy) 
                                getStateManagementStrategy(context, view.getViewId())).
                                suscribeListeners(view);
                    }
                    // If the result is complete, the view restored here is static. 
                    // static views can be marked as filled.
                    if (!refreshTransientBuildOnPSS)
                    {
                        // This option will be true on this cases:
                        // -pss is true and refresh is not active
                        setFilledView(context, view);
                    }
                    //At this point refreshTransientBuild = false && refreshTransientBuildOnPSS is true
                    else if (_refreshTransientBuildOnPSSAuto &&
                             !context.getAttributes().containsKey(CLEAN_TRANSIENT_BUILD_ON_RESTORE))
                    {
                        setFilledView(context, view);
                    }
                    return;
                }
                else
                {
                    // We need to refresh a partial view.
                    refreshTransientBuild = true;
                    refreshPartialView = true;
                }
            }
        }
        
        if (usePartialStateSavingOnThisView)
        {
            // Before apply we need to make sure the current view has
            // a clientId that will be used as a key to save and restore
            // the current view. Note that getClientId is never called (or used)
            // from UIViewRoot.
            if (view.getId() == null)
            {
                view.setId(view.createUniqueId(context, null));
            }

            context.getAttributes().put(USING_PSS_ON_THIS_VIEW, Boolean.TRUE);
            //Add a key to indicate ComponentTagHandlerDelegate to 
            //call UIComponent.markInitialState after it is populated
            if (!refreshTransientBuild || refreshPartialView)
            {
                context.getAttributes().put(StateManager.IS_BUILDING_INITIAL_STATE, Boolean.TRUE);
                context.getAttributes().put(IS_BUILDING_INITIAL_STATE_KEY_ALIAS, Boolean.TRUE);
            }
            if (!refreshTransientBuild && _markInitialStateWhenApplyBuildView)
            {
                context.getAttributes().put(MARK_INITIAL_STATE_KEY, Boolean.TRUE);
            }
            if (refreshTransientBuildOnPSS)
            {
                //This value is only set when _refreshTransientBuildOnPSSMode is "auto" or "true" 
                context.getAttributes().put(REFRESH_TRANSIENT_BUILD_ON_PSS,
                                            _refreshTransientBuildOnPSSAuto ? "auto" : "true");
            }
        }

        try
        {
            if (refreshTransientBuild)
            {
                context.getAttributes().put(REFRESHING_TRANSIENT_BUILD, Boolean.TRUE);

                // In theory, this should be disabled on ComponentTagHandlerDelegate,
                // otherwise we could lost PostAddToViewEvent / PreRemoveFromViewEvent
                // caused by c:if effect or facelets cleanup algorithm
                //context.setProcessingEvents(false);
            }
            // populate UIViewRoot
            _getFacelet(context, renderedViewId).apply(context, view);
        }
        finally
        {
            if (refreshTransientBuildOnPSS)
            {
                context.getAttributes().remove(REFRESH_TRANSIENT_BUILD_ON_PSS);
            }
            if (refreshTransientBuild)
            {
                //context.setProcessingEvents(true);
                if (FaceletViewDeclarationLanguageBase.isDynamicComponentRefreshTransientBuildActive(context))
                {
                    VisitContext visitContext = (VisitContext) getVisitContextFactory().
                        getVisitContext(context, null, VISIT_HINTS_DYN_REFRESH);
                    view.visitTree(visitContext, new PublishDynamicComponentRefreshTransientBuildCallback());
                }
                if (!usePartialStateSavingOnThisView || refreshTransientBuildOnPSS)
                {
                    // When the facelet is applied, all components are removed and added from view,
                    // but the difference resides in the ordering. Since the view is
                    // being refreshed, if we don't do this manually, some tags like
                    // cc:insertChildren or cc:insertFacet will not work correctly, because
                    // we expect PostAddToViewEvent will be propagated from parent to child, and
                    // facelets refreshing algorithm do the opposite.
                    //FaceletViewDeclarationLanguage._publishPreRemoveFromViewEvent(context, view);
                    //FaceletViewDeclarationLanguage._publishPostAddToViewEvent(context, view);
                    FaceletViewDeclarationLanguage._publishPostBuildComponentTreeOnRestoreViewEvent(context, view);
                }

                context.getAttributes().remove(REFRESHING_TRANSIENT_BUILD);
            }
            else
            {
                // Publish PostAddToView over UIViewRoot, because this is not done automatically.
                context.getApplication().publishEvent(context, PostAddToViewEvent.class, UIViewRoot.class, view);
            }
        }

        // set this view as filled
        if (refreshTransientBuild)
        {
            //This option will be true on this cases:
            //- pss is false, but we are refreshing
            //- pss is true, and we are refreshing a view already filled
            setFilledView(context, view);
        }
        else if (!refreshTransientBuildOnPSS)
        {
            // This option will be true on this cases:
            // -pss is true and refresh is not active
            setFilledView(context, view);
        }
        //At this point refreshTransientBuild = false && refreshTransientBuildOnPSS is true
        else if (_refreshTransientBuildOnPSSAuto &&
                 !context.getAttributes().containsKey(CLEAN_TRANSIENT_BUILD_ON_RESTORE))
        {
            setFilledView(context, view);
        }

        // Suscribe listeners if we are using partialStateSaving
        if (usePartialStateSavingOnThisView)
        {
            // UIViewRoot.markInitialState() is not called because it does
            // not have a facelet tag handler class that create it, instead
            // new instances are created programatically.
            if (!refreshTransientBuild || refreshPartialView)
            {
                // Save the state
                if (_viewPoolProcessor != null &&
                    _viewPoolProcessor.isViewPoolEnabledForThisView(context, view))
                {
                    _viewPoolProcessor.storeViewStructureMetadata(context, view);
                }
                if (_markInitialStateWhenApplyBuildView)
                {
                    if (!refreshTransientBuildOnPSS ||
                        !view.getAttributes().containsKey(COMPONENT_ADDED_AFTER_BUILD_VIEW))
                    {
                        view.markInitialState();
                    }

                    //Remove the key that indicate we need to call UIComponent.markInitialState
                    //on the current tree
                    context.getAttributes().remove(MARK_INITIAL_STATE_KEY);
                }
                else
                {
                    context.getAttributes().put(MARK_INITIAL_STATE_KEY, Boolean.TRUE);
                    _markInitialStateOnView(view, refreshTransientBuildOnPSS);
                    context.getAttributes().remove(MARK_INITIAL_STATE_KEY);
                }
                context.getAttributes().remove(StateManager.IS_BUILDING_INITIAL_STATE);
                context.getAttributes().remove(IS_BUILDING_INITIAL_STATE_KEY_ALIAS);
            }

            // We need to suscribe the listeners of changes in the component tree
            // only the first time here. Later we suscribe this listeners on
            // DefaultFaceletsStateManagement.restoreView after calling 
            // _publishPostBuildComponentTreeOnRestoreViewEvent(), to ensure 
            // relocated components are not retrieved later on getClientIdsRemoved().
            if (!(refreshTransientBuild && PhaseId.RESTORE_VIEW.equals(context.getCurrentPhaseId())) &&
                !view.isTransient())
            {
                ((DefaultFaceletsStateManagementStrategy) getStateManagementStrategy(context, view.getViewId())).
                        suscribeListeners(view);
            }

            context.getAttributes().remove(USING_PSS_ON_THIS_VIEW);
        }

        // Remove this var from faces context because this one prevent AjaxHandler
        // register the standard script library on Post-Redirect-Get pattern or
        // in the next view
        context.getAttributes().remove(AjaxHandler.STANDARD_JSF_AJAX_LIBRARY_LOADED);
    }

    private void _markInitialStateOnView(final UIViewRoot view, final boolean refreshTransientBuildOnPSS)
    {
        if (!refreshTransientBuildOnPSS ||
                !view.getAttributes().containsKey(COMPONENT_ADDED_AFTER_BUILD_VIEW))
        {
            if (!view.isTransient())
            {
                view.markInitialState();
            }
        }

        int childCount = view.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                UIComponent child = view.getChildren().get(i);
                if (!child.isTransient())
                {
                    _markInitialState(child);
                }
            }
        }
        if (view.getFacetCount() > 0)
        {
            Map<String, UIComponent> facetMap = view.getFacets();
            for (Map.Entry<String, UIComponent> entry : facetMap.entrySet())
            {
                UIComponent child = entry.getValue();
                if (!child.isTransient())
                {
                    _markInitialState(child);
                }
            }

        }
    }

    private void _markInitialState(final UIComponent component)
    {
        component.markInitialState();

        final int childCount = component.getChildCount();
        if (childCount > 0)
        {
            for (int i = 0; i < childCount; i++)
            {
                UIComponent child = component.getChildren().get(i);
                if (!child.isTransient())
                {
                    _markInitialState(child);
                }
            }
        }
        if (component.getFacetCount() > 0)
        {
            Map<String, UIComponent> facetMap = component.getFacets();
            for (Map.Entry<String, UIComponent> entry : facetMap.entrySet())
            {
                UIComponent child = entry.getValue();
                if (!child.isTransient())
                {
                    _markInitialState(child);
                }
            }

        }
    }

    public static void _publishPostBuildComponentTreeOnRestoreViewEvent(FacesContext context, UIComponent component)
    {
        context.getApplication().publishEvent(context, PostBuildComponentTreeOnRestoreViewEvent.class,
                                              component.getClass(), component);

        if (component.getChildCount() > 0)
        {
            // PostAddToViewEvent could cause component relocation
            // (h:outputScript, h:outputStylesheet, composite:insertChildren, composite:insertFacet)
            // so we need to check if the component was relocated or not
            List<UIComponent> children = component.getChildren();
            UIComponent child = null;
            UIComponent currentChild = null;
            int i = 0;
            while (i < children.size())
            {
                child = children.get(i);
                // Iterate over the same index if the component was removed
                // This prevents skip components when processing
                do
                {
                    _publishPostBuildComponentTreeOnRestoreViewEvent(context, child);
                    currentChild = child;
                    child = children.get(i);
                }
                while ((i < children.size()) && child != currentChild);
                i++;
            }
        }
        if (component.getFacetCount() > 0)
        {
            for (UIComponent child : component.getFacets().values())
            {
                _publishPostBuildComponentTreeOnRestoreViewEvent(context, child);
            }
        }
    }

    private boolean isFilledView(FacesContext context, UIViewRoot view)
    {
        // The view is only built on restoreView or renderView, but if
        // we are not using partial state saving, we need to mark the current
        // view as filled, otherwise it will be filled again on renderView.
        return context.getAttributes().containsKey(view);
        // -= Leonardo Uribe =- save this key on view cause render fail, because the view
        // is built before render view to "restore" the transient components that has
        // facelet markup (facelets UIInstructions ...) This effect is only notice when
        // partial state saving is not used. 
        //return view.getAttributes().containsKey(FILLED_VIEW);
    }

    private void setFilledView(FacesContext context, UIViewRoot view)
    {
        context.getAttributes().put(view, Boolean.TRUE);
        // -= Leonardo Uribe =- save this key on view cause render fail, because the view
        // is built before render view to "restore" the transient components that has
        // facelet markup (facelets UIInstructions ...) This effect is only notice when
        // partial state saving is not used. 
        // view.getAttributes().put(FILLED_VIEW, Boolean.TRUE);
    }

    /**
     * retargetMethodExpressions(FacesContext, UIComponent) has some clues about the behavior of this method
     *
     * {@inheritDoc}
     */
    @Override
    public BeanInfo getComponentMetadata(FacesContext context, Resource componentResource)
    {
        BeanInfo beanInfo = null;

        checkNull(context, "context");

        try
        {
            Facelet compositeComponentFacelet;
            FaceletFactory.setInstance(_faceletFactory);
            try
            {
                compositeComponentFacelet
                        = _faceletFactory.getCompositeComponentMetadataFacelet(componentResource.getURL());
            }
            finally
            {
                FaceletFactory.setInstance(null);
            }
            //context.getAttributes().put(BUILDING_COMPOSITE_COMPONENT_METADATA, Boolean.TRUE);

            // Create a temporal tree where all components will be put, but we are only
            // interested in metadata.
            UINamingContainer compositeComponentBase
                    = (UINamingContainer) context.getApplication().createComponent(
                    context, UINamingContainer.COMPONENT_TYPE, null);

            // Fill the component resource key, because this information should be available
            // on metadata to recognize which is the component used as composite component base.
            // Since this method is called from Application.createComponent(FacesContext,Resource),
            // and in that specific method this key is updated, this is the best option we
            // have for recognize it (also this key is used by UIComponent.isCompositeComponent)
            compositeComponentBase.getAttributes().put(Resource.COMPONENT_RESOURCE_KEY, componentResource);

            // According to UserTagHandler, in this point we need to wrap the facelet
            // VariableMapper, so local changes are applied on "page context", but
            // data is retrieved from full context
            FaceletContext faceletContext = (FaceletContext) context.
                    getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
            VariableMapper orig = faceletContext.getVariableMapper();
            try
            {
                faceletContext.setVariableMapper(new VariableMapperWrapper(orig));

                compositeComponentBase.pushComponentToEL(context, compositeComponentBase);

                compositeComponentFacelet.apply(context, compositeComponentBase);

                compositeComponentBase.popComponentFromEL(context);
            }
            finally
            {
                faceletContext.setVariableMapper(orig);
            }

            beanInfo = (BeanInfo) compositeComponentBase.getAttributes().get(UIComponent.BEANINFO_KEY);
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
        //finally
        //{
        //context.getAttributes().remove(BUILDING_COMPOSITE_COMPONENT_METADATA);
        //}

        return beanInfo;
    }

    /**
     * Check if the current facelet applied is used to build composite component metadata.
     *
     * @param context
     * @return
     */
    //public static boolean isBuildingCompositeComponentMetadata(FacesContext context)
    //{
    //    return context.getAttributes().containsKey(BUILDING_COMPOSITE_COMPONENT_METADATA);
    //}

    /**
     * Check if the current facelet applied is used to build view metadata.
     *
     * @param context
     * @return
     */
    public static boolean isBuildingViewMetadata(FacesContext context)
    {
        return context.getAttributes().containsKey(BUILDING_VIEW_METADATA);
    }

    public static boolean isRefreshingTransientBuild(FacesContext context)
    {
        return context.getAttributes().containsKey(REFRESHING_TRANSIENT_BUILD);
    }

    public static boolean isRemovingComponentBuild(FacesContext context)
    {
        return context.getAttributes().containsKey(REMOVING_COMPONENTS_BUILD);
    }

    public static boolean isMarkInitialState(FacesContext context)
    {
        return Boolean.TRUE.equals(context.getAttributes().get(MARK_INITIAL_STATE_KEY));
    }

    public static boolean isRefreshTransientBuildOnPSS(FacesContext context)
    {
        //this include both "true" and "auto"
        return context.getAttributes().containsKey(REFRESH_TRANSIENT_BUILD_ON_PSS);
    }

    public static boolean isRefreshTransientBuildOnPSSAuto(FacesContext context)
    {
        return "auto".equalsIgnoreCase((String) context.getAttributes().get(REFRESH_TRANSIENT_BUILD_ON_PSS));
    }

    public static boolean isCleanTransientBuildOnRestore(FacesContext context)
    {
        return context.getAttributes().containsKey(CLEAN_TRANSIENT_BUILD_ON_RESTORE);
    }

    public static void cleanTransientBuildOnRestore(FacesContext context)
    {
        context.getAttributes().put(CLEAN_TRANSIENT_BUILD_ON_RESTORE, Boolean.TRUE);
    }

    public static boolean isUsingPSSOnThisView(FacesContext context)
    {
        return context.getAttributes().containsKey(USING_PSS_ON_THIS_VIEW);
    }

    /**
     * In short words, this method take care of "target" an "attached object".
     * <ul>
     * <li>The "attached object" is instantiated by a tag handler.</li> 
     * <li>The "target" is an object used as "marker", that exposes a List&lt;UIComponent&gt;</li>
     * </ul>
     * This method should be called from some composite component tag handler, after
     * all children of composite component has been applied.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void retargetAttachedObjects(FacesContext context,
                                        UIComponent topLevelComponent, List<AttachedObjectHandler> handlerList)
    {
        checkNull(context, "context");
        checkNull(topLevelComponent, "topLevelComponent");
        checkNull(handlerList, "handlerList");

        BeanInfo compositeComponentMetadata
                = (BeanInfo) topLevelComponent.getAttributes().get(UIComponent.BEANINFO_KEY);

        if (compositeComponentMetadata == null)
        {
            log.severe("Composite component metadata not found for: " + topLevelComponent.getClientId(context));
            return;
        }

        BeanDescriptor compositeComponentDescriptor = compositeComponentMetadata.getBeanDescriptor();

        List<AttachedObjectTarget> targetList = (List<AttachedObjectTarget>)
                compositeComponentDescriptor.getValue(AttachedObjectTarget.ATTACHED_OBJECT_TARGETS_KEY);

        if (targetList == null || targetList.isEmpty())
        {
            return;
        }

        for (int i = 0, size = handlerList.size(); i < size; i++)
        {
            AttachedObjectHandler currentHandler = handlerList.get(i);
            // In the spec javadoc this variable is referred as forAttributeValue, but
            // note it is also called curTargetName
            String forValue = currentHandler.getFor();
            
            // perf: targetList is always arrayList: see AttachedObjectTargetHandler.apply 
            // and ClientBehaviorHandler.apply 
            for (int k = 0, targetsSize = targetList.size(); k < targetsSize; k++)
            {
                AttachedObjectTarget currentTarget = targetList.get(k);
                FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance();

                if ((forValue != null && forValue.equals(currentTarget.getName())) &&
                        ((currentTarget instanceof ActionSource2AttachedObjectTarget &&
                                currentHandler instanceof ActionSource2AttachedObjectHandler) ||
                                (currentTarget instanceof EditableValueHolderAttachedObjectTarget &&
                                        currentHandler instanceof EditableValueHolderAttachedObjectHandler) ||
                                (currentTarget instanceof ValueHolderAttachedObjectTarget &&
                                        currentHandler instanceof ValueHolderAttachedObjectHandler)))
                {
                    // perf: getTargets return ArrayList - see getTargets implementations
                    List<UIComponent> targets = currentTarget.getTargets(topLevelComponent);
                    for (int l = 0, targetsCount = targets.size(); l < targetsCount; l++)
                    {
                        UIComponent component = targets.get(l);
                        // If we found composite components when traverse the tree
                        // we have to call this one recursively, because each composite component
                        // should have its own AttachedObjectHandler list, filled earlier when
                        // its tag handler is applied.
                        if (UIComponent.isCompositeComponent(component))
                        {
                            // How we obtain the list of AttachedObjectHandler for
                            // the current composite component? It should be a component
                            // attribute or retrieved by a key inside component.getAttributes
                            // map. Since api does not specify any attribute, we suppose
                            // this is an implementation detail and it should be retrieved
                            // from component attribute map.
                            // But this is only the point of the iceberg, because we should
                            // define how we register attached object handlers in this list.
                            // ANS: see CompositeComponentResourceTagHandler.
                            // The current handler should be added to the list, to be chained.
                            // Note that the inner component should have a target with the same name
                            // as "for" attribute
                            mctx.addAttachedObjectHandler(component, currentHandler);

                            List<AttachedObjectHandler> handlers = mctx.getAttachedObjectHandlers(component);

                            retargetAttachedObjects(context, component, handlers);

                            handlers.remove(currentHandler);
                        }
                        else
                        {
                            currentHandler.applyAttachedObject(context, component);
                        }
                        if (mctx.isUsingPSSOnThisView() && mctx.isMarkInitialState())
                        {
                            component.markInitialState();
                        }
                    }
                }
                else if ((currentTarget instanceof BehaviorHolderAttachedObjectTarget &&
                        currentHandler instanceof BehaviorHolderAttachedObjectHandler))
                {
                    String eventName = ((BehaviorHolderAttachedObjectHandler) currentHandler).getEventName();
                    boolean isDefaultEvent = ((BehaviorHolderAttachedObjectTarget) currentTarget).isDefaultEvent();

                    if ((eventName != null && eventName.equals(currentTarget.getName())) ||
                            (eventName == null && isDefaultEvent))
                    {
                        List<UIComponent> targets = currentTarget.getTargets(topLevelComponent);
                        for (int j = 0, targetssize = targets.size(); j < targetssize; j++)
                        {
                            UIComponent component = targets.get(j);
                            // If we found composite components when traverse the tree
                            // we have to call this one recursively, because each composite component
                            // should have its own AttachedObjectHandler list, filled earlier when
                            // its tag handler is applied.
                            if (UIComponent.isCompositeComponent(component))
                            {
                                if (currentTarget instanceof ClientBehaviorAttachedObjectTarget)
                                {
                                    mctx.addAttachedObjectHandler(component,
                                            new ClientBehaviorRedirectBehaviorAttachedObjectHandlerWrapper(
                                                    (BehaviorHolderAttachedObjectHandler) currentHandler,
                                                    ((ClientBehaviorAttachedObjectTarget) currentTarget).getEvent()));
                                }
                                else
                                {
                                    mctx.addAttachedObjectHandler(component, currentHandler);
                                }

                                List<AttachedObjectHandler> handlers = mctx.getAttachedObjectHandlers(component);

                                retargetAttachedObjects(context, component, handlers);

                                handlers.remove(currentHandler);
                            }
                            else
                            {
                                if (currentHandler instanceof
                                        ClientBehaviorRedirectBehaviorAttachedObjectHandlerWrapper)
                                {
                                    currentHandler.applyAttachedObject(context,
                                            new ClientBehaviorRedirectEventComponentWrapper(component,
                                            ((ClientBehaviorRedirectBehaviorAttachedObjectHandlerWrapper)
                                                    currentHandler).getWrappedEventName(), eventName));
                                }
                                else
                                {
                                    currentHandler.applyAttachedObject(context, component);
                                }
                            }
                            if (mctx.isUsingPSSOnThisView() && mctx.isMarkInitialState())
                            {
                                component.markInitialState();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void retargetMethodExpressions(FacesContext context, UIComponent topLevelComponent)
    {
        checkNull(context, "context");

        BeanInfo compositeComponentMetadata
                = (BeanInfo) topLevelComponent.getAttributes().get(UIComponent.BEANINFO_KEY);

        if (compositeComponentMetadata == null)
        {
            log.severe("Composite component metadata not found for: " + topLevelComponent.getClientId(context));
            return;
        }

        // "...For each attribute that is a MethodExpression..." This means we have to scan
        // all attributes with "method-signature" attribute and no "type" attribute
        // javax.faces.component._ComponentAttributesMap uses BeanInfo.getPropertyDescriptors to
        // traverse over it, but here the metadata returned by UIComponent.BEANINFO_KEY is available
        // only for composite components.
        // That means somewhere we need to create a custom BeanInfo object for composite components
        // that will be filled somewhere (theorically in ViewDeclarationLanguage.getComponentMetadata())

        PropertyDescriptor[] propertyDescriptors = compositeComponentMetadata.getPropertyDescriptors();

        ELContext elContext = (ELContext) context.getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);

        for (PropertyDescriptor propertyDescriptor : propertyDescriptors)
        {
            if (propertyDescriptor.getValue("type") != null)
            {
                // This check is necessary if we have both "type" and "method-signature" set.
                // In that case, "method-signature" is ignored
                continue;
            }

            String attributeName = propertyDescriptor.getName();
            //boolean isKnownMethod = "action".equals(attributeName) || "actionListener".equals(attributeName)  
            //        || "validator".equals(attributeName) || "valueChangeListener".equals(attributeName);

            // <composite:attribute> method-signature attribute is 
            // ValueExpression that must evaluate to String
            ValueExpression methodSignatureExpression
                    = (ValueExpression) propertyDescriptor.getValue("method-signature");
            String methodSignature = null;
            if (methodSignatureExpression != null)
            {
                // Check if the value expression holds a method signature
                // Note that it could be null, so in that case we don't have to do anything
                methodSignature = (String) methodSignatureExpression.getValue(elContext);
            }

            String targetAttributeName = null;
            ValueExpression targetAttributeNameVE
                    = (ValueExpression) propertyDescriptor.getValue("targetAttributeName");
            if (targetAttributeNameVE != null)
            {
                targetAttributeName = (String) targetAttributeNameVE.getValue(context.getELContext());
                if (targetAttributeName == null)
                {
                    targetAttributeName = attributeName;
                }
            }
            else
            {
                targetAttributeName = attributeName;
            }

            boolean isKnownTargetAttributeMethod
                    = "action".equals(targetAttributeName) || "actionListener".equals(targetAttributeName)
                      || "validator".equals(targetAttributeName) || "valueChangeListener".equals(targetAttributeName);

            // either the attributeName has to be a knownMethod or there has to be a method-signature
            if (isKnownTargetAttributeMethod || methodSignature != null)
            {
                ValueExpression targetsExpression =
                        (ValueExpression) propertyDescriptor.getValue("targets");

                String targets = null;
                // <composite:attribute> targets attribute is 
                // ValueExpression that must evaluate to String
                if (targetsExpression != null)
                {
                    targets = (String) targetsExpression.getValue(elContext);
                }

                if (targets == null)
                {
                    // "...let the name of the metadata element be the 
                    // evaluated value of the targets attribute..."
                    targets = attributeName;
                }

                FaceletCompositionContext mctx = FaceletCompositionContext.getCurrentInstance();

                // If the MethodExpression attribute has been already applied, there is no need to
                // handle it and it is probably a MethodExpression instance is on attribute map, so the
                // inner code will cause a ClassCastException.
                if (!mctx.isMethodExpressionAttributeApplied(topLevelComponent, attributeName))
                {

                    ValueExpression attributeNameValueExpression =
                            (ValueExpression) topLevelComponent.getAttributes().get(attributeName);

                    if (attributeNameValueExpression == null)
                    {
                        // composite:attribute has a default property, so if we can't found on the
                        // component attribute map, we should get the default as CompositeComponentELResolver
                        // does.
                        attributeNameValueExpression = (ValueExpression) propertyDescriptor.getValue("default");
                        if (attributeNameValueExpression == null)
                        {
                            // It is only valid to log an error if the attribute is required
                            ValueExpression ve = (ValueExpression) propertyDescriptor.getValue("required");
                            if (ve != null)
                            {
                                Object requiredValue = ve.getValue(elContext);
                                Boolean required = null;
                                if (requiredValue instanceof Boolean)
                                {
                                    required = (Boolean) requiredValue;
                                }
                                else
                                {
                                    required = Boolean.valueOf(requiredValue.toString());
                                }

                                if (required != null && required.booleanValue())
                                {
                                    if (log.isLoggable(Level.SEVERE))
                                    {
                                        log.severe("attributeValueExpression not found under the key \""
                                                   + attributeName
                                                   + "\". Looking for the next attribute");
                                    }
                                }
                            }
                            continue;
                        }
                    }

                    String[] targetsArray = StringUtils.splitShortString(targets, ' ');
                    String attributeExpressionString = attributeNameValueExpression.getExpressionString();

                    //Check if the stored valueExpression is a ccRedirection, to handle it properly later.
                    boolean ccAttrMeRedirection =
                            attributeNameValueExpression instanceof LocationValueExpression &&
                                    CompositeComponentELUtils.isCompositeComponentAttrsMethodExpression(
                                            attributeNameValueExpression.getExpressionString());

                    if (isKnownTargetAttributeMethod)
                    {
                        // To add support to #{cc.attrs.action}, #{cc.attrs.actionListener}, #{cc.attrs.validator} or
                        // #{cc.attrs.valueChangeListener} it is necessary to put a MethodExpression or a 
                        // ValueExpression pointing to the associated java method in the component attribute map.
                        // org.apache.myfaces.view.facelets.tag.composite.RetargetMethodExpressionRule already put
                        // a ValueExpression, so we only need to put a MethodExpression when a non redirecting
                        // expression is used (for example when a nested #{cc.attrs.xxx} is used).
                        if ("action".equals(targetAttributeName))
                        {
                            applyActionMethodExpressionEL(context, elContext,
                                    topLevelComponent, attributeName,
                                    attributeExpressionString, attributeNameValueExpression,
                                    ccAttrMeRedirection);
                        }
                        else if ("actionListener".equals(targetAttributeName))
                        {
                            applyActionListenerMethodExpressionEL(context, elContext,
                                    topLevelComponent, attributeName, 
                                    attributeExpressionString, attributeNameValueExpression, 
                                    ccAttrMeRedirection);
                        }
                        else if ("validator".equals(targetAttributeName))
                        {
                            applyValidatorMethodExpressionEL(context, elContext,
                                    topLevelComponent, attributeName,
                                    attributeExpressionString, attributeNameValueExpression, 
                                    ccAttrMeRedirection);
                        }
                        else if ("valueChangeListener".equals(targetAttributeName))
                        {
                            applyValueChangeListenerMethodExpressionEL(context, elContext,
                                    topLevelComponent, attributeName, 
                                    attributeExpressionString, attributeNameValueExpression,
                                    ccAttrMeRedirection);
                        }

                        UIComponent topLevelComponentBase = 
                            topLevelComponent.getFacet(UIComponent.COMPOSITE_FACET_NAME);

                        for (String target : targetsArray)
                        {
                            UIComponent innerComponent
                                    = ComponentSupport.findComponentChildOrFacetFrom(context, topLevelComponentBase,
                                                                                     target);

                            if (innerComponent == null)
                            {
                                continue;
                            }

                            if (isCompositeComponentRetarget(context, innerComponent, targetAttributeName))
                            {
                                innerComponent.getAttributes().put(targetAttributeName, attributeNameValueExpression);

                                mctx.clearMethodExpressionAttribute(innerComponent, targetAttributeName);

                                retargetMethodExpressions(context, innerComponent);
                                if (mctx.isUsingPSSOnThisView() && mctx.isMarkInitialState())
                                {
                                    innerComponent.markInitialState();
                                }
                            }
                            else
                            {
                                if ("action".equals(targetAttributeName))
                                {
                                    applyActionMethodExpressionTarget(context, mctx, elContext,
                                            topLevelComponentBase, innerComponent, 
                                            attributeName, targetAttributeName, 
                                            attributeExpressionString, attributeNameValueExpression, 
                                            ccAttrMeRedirection);
                                    if (mctx.isUsingPSSOnThisView() && mctx.isMarkInitialState())
                                    {
                                        innerComponent.markInitialState();
                                    }
                                }
                                else if ("actionListener".equals(targetAttributeName))
                                {
                                    applyActionListenerMethodExpressionTarget(context, mctx, elContext, 
                                            topLevelComponentBase, innerComponent, 
                                            attributeName, targetAttributeName, 
                                            attributeExpressionString, attributeNameValueExpression, 
                                            ccAttrMeRedirection);
                                    if (mctx.isUsingPSSOnThisView() && mctx.isMarkInitialState())
                                    {
                                        innerComponent.markInitialState();
                                    }
                                }
                                else if ("validator".equals(targetAttributeName))
                                {
                                    applyValidatorMethodExpressionTarget(context, mctx, elContext,
                                            topLevelComponentBase, innerComponent, 
                                            attributeName, targetAttributeName, 
                                            attributeExpressionString, attributeNameValueExpression, 
                                            ccAttrMeRedirection);
                                    if (mctx.isUsingPSSOnThisView() && mctx.isMarkInitialState())
                                    {
                                        innerComponent.markInitialState();
                                    }
                                }
                                else if ("valueChangeListener".equals(targetAttributeName))
                                {
                                    applyValueChangeListenerMethodExpressionTarget(context, mctx, elContext,
                                            topLevelComponentBase, innerComponent, 
                                            attributeName, targetAttributeName,
                                            attributeExpressionString, attributeNameValueExpression,
                                            ccAttrMeRedirection);
                                    if (mctx.isUsingPSSOnThisView() && mctx.isMarkInitialState())
                                    {
                                        innerComponent.markInitialState();
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        MethodExpression methodExpression = null;
                        // composite:attribute targets property only has sense for action, actionListener,
                        // validator or valueChangeListener. This means we have to retarget the method expression
                        // to the topLevelComponent.

                        // Since a MethodExpression has no state, we can use it multiple times without problem, so
                        // first create it here.
                        methodSignature = methodSignature.trim();
                        methodExpression = context.getApplication().getExpressionFactory().
                                createMethodExpression(elContext,
                                        attributeExpressionString, 
                                        FaceletsViewDeclarationLanguageUtils.getReturnType(methodSignature),
                                        FaceletsViewDeclarationLanguageUtils.getParameters(methodSignature));

                        methodExpression = reWrapMethodExpression(methodExpression, attributeNameValueExpression);

                        applyMethodExpression(context, mctx, topLevelComponent, attributeName, 
                                targetAttributeName, attributeNameValueExpression, methodExpression, 
                                ccAttrMeRedirection, targetsArray);
                    }
                    mctx.markMethodExpressionAttribute(topLevelComponent, attributeName);
                }

                // We need to remove the previous ValueExpression, to prevent some possible
                // confusion when the same value is retrieved from the attribute map.
                topLevelComponent.setValueExpression(attributeName, null);
            }
        }
    }
    
    private void applyActionMethodExpressionEL(FacesContext context, 
                                               ELContext elContext, 
                                               UIComponent topLevelComponent, 
                                               String attributeName,
                                               String attributeExpressionString, 
                                               ValueExpression attributeNameValueExpression, 
                                               boolean ccAttrMeRedirection)
    {
        // target is ActionSource2
        MethodExpression methodExpression = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                createMethodExpression(elContext,
                        attributeExpressionString, null,
                        EMPTY_CLASS_ARRAY), attributeNameValueExpression);

        //Store the method expression to the topLevelComponent to allow reference it through EL
        if (!ccAttrMeRedirection)
        {
            //Replace it with a method expression
            topLevelComponent.getAttributes().put(attributeName, methodExpression);
        }
        // Otherwise keep the current ValueExpression,
        // because it will be used chain other value expressions
    }

    private void applyActionListenerMethodExpressionEL(FacesContext context, 
                                                       ELContext elContext, 
                                                       UIComponent topLevelComponent, 
                                                       String attributeName,
                                                       String attributeExpressionString, 
                                                       ValueExpression attributeNameValueExpression, 
                                                       boolean ccAttrMeRedirection)
    {
        // target is ActionSource2
        MethodExpression methodExpression = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                createMethodExpression(elContext,
                        attributeExpressionString, Void.TYPE, ACTION_LISTENER_SIGNATURE),
                        attributeNameValueExpression);

        MethodExpression methodExpression2 = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                createMethodExpression(elContext,
                        attributeExpressionString, Void.TYPE, EMPTY_CLASS_ARRAY),
                        attributeNameValueExpression);

        //Store the method expression to the topLevelComponent to allow reference it through EL
        if (!ccAttrMeRedirection)
        {
            //Replace it with a method expression
            topLevelComponent.getAttributes().put(attributeName,
                    new MethodExpressionMethodExpression(methodExpression, methodExpression2));
        }
        // Otherwise keep the current ValueExpression,
        // because it will be used chain other value expressions
    }
    
    private void applyValidatorMethodExpressionEL(FacesContext context, 
                                                  ELContext elContext, 
                                                  UIComponent topLevelComponent, 
                                                  String attributeName,
                                                  String attributeExpressionString, 
                                                  ValueExpression attributeNameValueExpression, 
                                                  boolean ccAttrMeRedirection)
    {
        // target is EditableValueHolder
        MethodExpression methodExpression = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                createMethodExpression(elContext,
                        attributeExpressionString, Void.TYPE,
                        VALIDATOR_SIGNATURE), attributeNameValueExpression);

        //Store the method expression to the topLevelComponent to allow reference it through EL
        if (!ccAttrMeRedirection)
        {
            //Replace it with a method expression
            topLevelComponent.getAttributes().put(attributeName, methodExpression);
        }
        // Otherwise keep the current ValueExpression,
        // because it will be used chain other value expressions
    }
    
    private void applyValueChangeListenerMethodExpressionEL(FacesContext context, 
                                                            ELContext elContext, 
                                                            UIComponent topLevelComponent, 
                                                            String attributeName,
                                                            String attributeExpressionString, 
                                                            ValueExpression attributeNameValueExpression, 
                                                            boolean ccAttrMeRedirection)
    {
        // target is EditableValueHolder
        MethodExpression methodExpression = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                createMethodExpression(elContext,
                        attributeExpressionString, Void.TYPE,
                        VALUE_CHANGE_LISTENER_SIGNATURE), attributeNameValueExpression);

        MethodExpression methodExpression2 = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                createMethodExpression(elContext,
                        attributeExpressionString, Void.TYPE,
                        EMPTY_CLASS_ARRAY), attributeNameValueExpression);

        //Store the method expression to the topLevelComponent to allow reference it through EL
        if (!ccAttrMeRedirection)
        {
            //Replace it with a method expression
            topLevelComponent.getAttributes().put(attributeName,
                    new MethodExpressionMethodExpression(methodExpression, methodExpression2));
        }
        // Otherwise keep the current ValueExpression, because it will be used chain other value expressions
    }
    
    private void applyActionMethodExpressionTarget(FacesContext context, FaceletCompositionContext mctx,
                                                   ELContext elContext, 
                                                   UIComponent topLevelComponent,
                                                   UIComponent innerComponent,
                                                   String attributeName,
                                                   String targetAttributeName,
                                                   String attributeExpressionString,
                                                   ValueExpression attributeNameValueExpression,
                                                   boolean ccAttrMeRedirection)
    {
        // target is ActionSource2
        MethodExpression methodExpression
                = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                createMethodExpression(elContext,
                        attributeExpressionString, null, EMPTY_CLASS_ARRAY),
                        attributeNameValueExpression);

        // If it is a redirection, a wrapper is used to
        // locate the right instance and call it properly.
        if (ccAttrMeRedirection)
        {
            ((ActionSource2) innerComponent).setActionExpression(
                    new ValueExpressionMethodExpression(attributeNameValueExpression));
        }
        else
        {
            ((ActionSource2) innerComponent).setActionExpression(methodExpression);
        }
    }

    private void applyActionListenerMethodExpressionTarget(FacesContext context, FaceletCompositionContext mctx,
            ELContext elContext, 
            UIComponent topLevelComponent,
            UIComponent innerComponent,
            String attributeName,
            String targetAttributeName,
            String attributeExpressionString,
            ValueExpression attributeNameValueExpression,
            boolean ccAttrMeRedirection)
    {
        //First try to remove any prevous target if any
        ActionListener o = (ActionListener)
                mctx.removeMethodExpressionTargeted(innerComponent, targetAttributeName);
        if (o != null)
        {
            ((ActionSource2) innerComponent).removeActionListener(o);
        }

        // target is ActionSource2
        ActionListener actionListener = null;
        // If it is a redirection, a wrapper is used to locate the right instance and call
        //it properly.
        if (ccAttrMeRedirection)
        {
            actionListener = new RedirectMethodExpressionValueExpressionActionListener(
                                         attributeNameValueExpression);
        }
        else
        {
            MethodExpression methodExpression = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                    createMethodExpression(elContext,
                       attributeExpressionString, Void.TYPE, ACTION_LISTENER_SIGNATURE), attributeNameValueExpression);

            MethodExpression methodExpression2 = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                    createMethodExpression(elContext,
                            attributeExpressionString, Void.TYPE, EMPTY_CLASS_ARRAY), attributeNameValueExpression);

            if (mctx.isUsingPSSOnThisView())
            {
                actionListener = new PartialMethodExpressionActionListener(methodExpression, methodExpression2);
            }
            else
            {
                actionListener = new MethodExpressionActionListener(methodExpression, methodExpression2);
            }
        }
        ((ActionSource2) innerComponent).addActionListener(actionListener);
        mctx.addMethodExpressionTargeted(innerComponent, targetAttributeName, actionListener);
    }
    
    private void applyValidatorMethodExpressionTarget(FacesContext context, FaceletCompositionContext mctx,
            ELContext elContext, 
            UIComponent topLevelComponent,
            UIComponent innerComponent,
            String attributeName,
            String targetAttributeName,
            String attributeExpressionString,
            ValueExpression attributeNameValueExpression,
            boolean ccAttrMeRedirection)
    {
        //First try to remove any prevous target if any
        Validator o = (Validator) mctx.removeMethodExpressionTargeted(innerComponent, targetAttributeName);
        if (o != null)
        {
            ((EditableValueHolder) innerComponent).removeValidator(o);
        }

        // target is EditableValueHolder
        Validator validator = null;
        // If it is a redirection, a wrapper is used to locate the right instance and call it properly.
        if (ccAttrMeRedirection)
        {
            validator = new RedirectMethodExpressionValueExpressionValidator(attributeNameValueExpression);
        }
        else
        {
            MethodExpression methodExpression = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                    createMethodExpression(elContext,
                            attributeExpressionString, Void.TYPE,
                            VALIDATOR_SIGNATURE), attributeNameValueExpression);

            if (mctx.isUsingPSSOnThisView())
            {
                validator = new PartialMethodExpressionValidator(methodExpression);
            }
            else
            {
                validator = new MethodExpressionValidator(methodExpression);
            }
        }
        ((EditableValueHolder) innerComponent).addValidator(validator);
        mctx.addMethodExpressionTargeted(innerComponent, targetAttributeName, validator);
    }
    
    private void applyValueChangeListenerMethodExpressionTarget(FacesContext context, FaceletCompositionContext mctx,
            ELContext elContext, 
            UIComponent topLevelComponent,
            UIComponent innerComponent,
            String attributeName,
            String targetAttributeName,
            String attributeExpressionString,
            ValueExpression attributeNameValueExpression,
            boolean ccAttrMeRedirection)
    {
        ValueChangeListener o = (ValueChangeListener) mctx.removeMethodExpressionTargeted(
                innerComponent, targetAttributeName);
        if (o != null)
        {
            ((EditableValueHolder) innerComponent).removeValueChangeListener(o);
        }

        // target is EditableValueHolder
        ValueChangeListener valueChangeListener = null;
        // If it is a redirection, a wrapper is used to locate the right instance and call it properly.
        if (ccAttrMeRedirection)
        {
            valueChangeListener = new RedirectMethodExpressionValueExpressionValueChangeListener(
                    attributeNameValueExpression);
        }
        else
        {
            MethodExpression methodExpression = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                    createMethodExpression(elContext,
                            attributeExpressionString, Void.TYPE,
                            VALUE_CHANGE_LISTENER_SIGNATURE), attributeNameValueExpression);

            MethodExpression methodExpression2 = reWrapMethodExpression(context.getApplication().getExpressionFactory().
                    createMethodExpression(elContext,
                            attributeExpressionString, Void.TYPE,
                            EMPTY_CLASS_ARRAY), attributeNameValueExpression);

            if (mctx.isUsingPSSOnThisView())
            {
                valueChangeListener = new PartialMethodExpressionValueChangeListener(
                        methodExpression, methodExpression2);
            }
            else
            {
                valueChangeListener = new MethodExpressionValueChangeListener(methodExpression, methodExpression2);
            }
        }
        ((EditableValueHolder) innerComponent).addValueChangeListener(valueChangeListener);
        mctx.addMethodExpressionTargeted(innerComponent, targetAttributeName, valueChangeListener);
    }
    
    private void applyMethodExpression(FacesContext context, FaceletCompositionContext mctx, 
            UIComponent topLevelComponent,
            String attributeName,
            String targetAttributeName,
            ValueExpression attributeNameValueExpression,
            MethodExpression methodExpression,
            boolean ccAttrMeRedirection,
            String[] targetsArray)
    {
        UIComponent topLevelComponentBase = topLevelComponent.getFacet(
                UIComponent.COMPOSITE_FACET_NAME);

        for (String target : targetsArray)
        {
            UIComponent innerComponent = ComponentSupport.findComponentChildOrFacetFrom(context, 
                    topLevelComponentBase, target);

            if (innerComponent == null)
            {
                continue;
            }

            // If a component is found, that means the expression should be retarget to the
            // components related
            if (isCompositeComponentRetarget(context, innerComponent, targetAttributeName))
            {
                innerComponent.getAttributes().put(targetAttributeName, attributeNameValueExpression);

                mctx.clearMethodExpressionAttribute(innerComponent, targetAttributeName);

                retargetMethodExpressions(context, innerComponent);
                
                if (mctx.isUsingPSSOnThisView() && mctx.isMarkInitialState())
                {
                    //retargetMethodExpression occur on build view time, so it is safe to call markInitiaState here
                    innerComponent.markInitialState();
                }
            }
            else
            {
                //Put the retarget
                if (ccAttrMeRedirection)
                {
                    // Since we require here a method expression, it is necessary to wrap 
                    // the ValueExpression into a MethodExpression that handles redirection.
                    innerComponent.getAttributes().put(targetAttributeName, 
                            new ValueExpressionMethodExpression(attributeNameValueExpression));
                }
                else
                {
                    innerComponent.getAttributes().put(targetAttributeName, methodExpression);
                }
                if (mctx.isUsingPSSOnThisView() && mctx.isMarkInitialState())
                {
                    innerComponent.markInitialState();
                }
            }
        }
        //Store the method expression to the topLevelComponent to allow reference it through EL
        if (!ccAttrMeRedirection)
        {
            //Replace it with a method expression
            topLevelComponent.getAttributes().put(attributeName, methodExpression);
        }
        // Othewise keep the current ValueExpression, because it will be used chain other value 
        // expressions
    }


    private boolean isCompositeComponentRetarget(FacesContext context, UIComponent component, String attributeName)
    {
        if (UIComponent.isCompositeComponent(component))
        {
            BeanInfo compositeComponentMetadata = (BeanInfo) component.getAttributes().get(UIComponent.BEANINFO_KEY);

            PropertyDescriptor[] propertyDescriptors = compositeComponentMetadata.getPropertyDescriptors();

            ELContext elContext = (ELContext) context.getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);

            for (PropertyDescriptor propertyDescriptor : propertyDescriptors)
            {
                if (propertyDescriptor.getValue("type") != null)
                {
                    // This check is necessary if we have both "type" and "method-signature" set.
                    // In that case, "method-signature" is ignored
                    continue;
                }

                if (attributeName.equals(propertyDescriptor.getName()))
                {
                    //boolean isKnownMethod = "action".equals(attributeName) || "actionListener".equals(attributeName)  
                    //|| "validator".equals(attributeName) || "valueChangeListener".equals(attributeName);

                    // <composite:attribute> method-signature attribute is 
                    // ValueExpression that must evaluate to String
                    ValueExpression methodSignatureExpression
                            = (ValueExpression) propertyDescriptor.getValue("method-signature");
                    String methodSignature = null;
                    if (methodSignatureExpression != null)
                    {
                        // Check if the value expression holds a method signature
                        // Note that it could be null, so in that case we don't have to do anything
                        methodSignature = (String) methodSignatureExpression.getValue(elContext);
                    }

                    String targetAttributeName = null;
                    ValueExpression targetAttributeNameVE = (ValueExpression) 
                        propertyDescriptor.getValue("targetAttributeName");
                    if (targetAttributeNameVE != null)
                    {
                        targetAttributeName = (String) targetAttributeNameVE.getValue(context.getELContext());
                        if (targetAttributeName == null)
                        {
                            targetAttributeName = attributeName;
                        }
                    }
                    else
                    {
                        targetAttributeName = attributeName;
                    }

                    boolean isKnownTargetAttributeMethod = "action".equals(targetAttributeName)
                            || "actionListener".equals(targetAttributeName)
                            || "validator".equals(targetAttributeName)
                            || "valueChangeListener".equals(targetAttributeName);

                    // either the attributeName has to be a knownMethod or there has to be a method-signature
                    if (isKnownTargetAttributeMethod || methodSignature != null)
                    {
                        if ("action".equals(targetAttributeName))
                        {
                            return !(component instanceof ActionSource2);
                        }
                        else if ("actionListener".equals(targetAttributeName))
                        {
                            return !(component instanceof ActionSource2);
                        }
                        else if ("validator".equals(targetAttributeName))
                        {
                            return !(component instanceof EditableValueHolder);
                        }
                        else if ("valueChangeListener".equals(targetAttributeName))
                        {
                            return !(component instanceof EditableValueHolder);
                        }
                        else
                        {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        else
        {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private MethodExpression reWrapMethodExpression(MethodExpression createdMethodExpression,
                                                    ValueExpression originalValueExpression)
    {
        if (originalValueExpression instanceof LocationValueExpression)
        {
            return new LocationMethodExpression(
                    ((LocationValueExpression) originalValueExpression).getLocation(),
                    reWrapMethodExpression(createdMethodExpression,
                            ((LocationValueExpression) originalValueExpression).getWrapped()),
                    ((LocationValueExpression) originalValueExpression).getCCLevel());
        }
        else if (originalValueExpression instanceof FacesWrapper &&
                ((FacesWrapper) originalValueExpression).getWrapped() instanceof ValueExpression)
        {
            return reWrapMethodExpression(createdMethodExpression,
                    (ValueExpression) ((FacesWrapper) originalValueExpression).getWrapped());
        }
        else
        {
            return createdMethodExpression;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource getScriptComponentResource(FacesContext context, Resource componentResource)
    {
        checkNull(context, "context");
        checkNull(componentResource, "componentResource");
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StateManagementStrategy getStateManagementStrategy(FacesContext context, String viewId)
    {
        // Use partial state saving strategy only if javax.faces.PARTIAL_STATE_SAVING is "true" and
        // the current view is not on javax.faces.FULL_STATE_SAVING_VIEW_IDS.
        if (_partialStateSaving && _stateMgmtStrategy == null)
        {
            _stateMgmtStrategy = new DefaultFaceletsStateManagementStrategy(context);
        }

        return _usePartialStateSavingOnThisView(viewId) ? _stateMgmtStrategy : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewMetadata getViewMetadata(FacesContext context, String viewId)
    {
        checkNull(context, "facesContext");
        checkNull(viewId, "viewId");
        return new FaceletViewMetadata(viewId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderView(FacesContext context, UIViewRoot view) throws IOException
    {
        if (!view.isRendered())
        {
            return;
        }

        // log request
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Rendering View: " + view.getViewId());
        }

        try
        {
            // build view - but not if we're in "buildBeforeRestore"
            // land and we've already got a populated view. Note
            // that this optimizations breaks if there's a "c:if" in
            // the page that toggles as a result of request processing -
            // should that be handled? Or
            // is this optimization simply so minor that it should just
            // be trimmed altogether?
            // See JSF 2.0 spec section 2.2.6, buildView is called before
            // Render Response
            //if (!isFilledView(context, view))
            //{
            //    buildView(context, view);
            //}

            // setup writer and assign it to the context
            ResponseWriter origWriter = createResponseWriter(context);

            ExternalContext extContext = context.getExternalContext();
            Writer outputWriter = extContext.getResponseOutputWriter();

            StateWriter stateWriter = new StateWriter(outputWriter, 1024, context);
            try
            {
                ResponseWriter writer = origWriter.cloneWithWriter(stateWriter);
                try
                {
                    context.setResponseWriter(writer);

                    StateManager stateMgr = context.getApplication().getStateManager();
                    // force creation of session if saving state there
                    // -= Leonardo Uribe =- Do this does not have any sense!. The only reference
                    // about these lines are on http://java.net/projects/facelets/sources/svn/revision/376
                    // and it says: "fixed lazy session instantiation with eager response commit"
                    // This code is obviously to prevent this exception:
                    // java.lang.IllegalStateException: Cannot create a session after the response has been committed
                    // But in theory if that so, StateManager.saveState must happen before writer.close() is called,
                    // which can be done very easily.
                    //if (!stateMgr.isSavingStateInClient(context))
                    //{
                    //    extContext.getSession(true);
                    //}

                    // render the view to the response
                    writer.startDocument();

                    view.encodeAll(context);

                    writer.endDocument();

                    // finish writing
                    // -= Leonardo Uribe =- This does not has sense too, because that's the reason
                    // of the try/finally block. In practice, it only forces the close of the tag 
                    // in HtmlResponseWriter if necessary, but according to the spec, this should
                    // be done using writer.flush() instead.
                    // writer.close();

                    // flush to origWriter
                    if (stateWriter.isStateWritten())
                    {
                        // Call this method to force close the tag if necessary.
                        // The spec javadoc says this: 
                        // "... Flush any ouput buffered by the output method to the underlying 
                        // Writer or OutputStream. This method will not flush the underlying 
                        // Writer or OutputStream; it simply clears any values buffered by this 
                        // ResponseWriter. ..."
                        writer.flush();

                        // =-= markoc: STATE_KEY is in output ONLY if 
                        // stateManager.isSavingStateInClient(context)is true - see
                        // org.apache.myfaces.application.ViewHandlerImpl.writeState(FacesContext)
                        // TODO this class and ViewHandlerImpl contain same constant <!--@@JSF_FORM_STATE_MARKER@@-->
                        Object stateObj = stateMgr.saveView(context);
                        String content = stateWriter.getAndResetBuffer();
                        int end = content.indexOf(STATE_KEY);
                        // See if we can find any trace of the saved state.
                        // If so, we need to perform token replacement
                        if (end >= 0)
                        {
                            // save state
                            int start = 0;

                            while (end != -1)
                            {
                                origWriter.write(content, start, end - start);
                                
                                String stateStr;
                                if (view.isTransient())
                                {
                                    // Force state saving
                                    stateMgr.writeState(context, stateObj);
                                    stateStr = stateWriter.getAndResetBuffer();
                                }
                                else if (stateObj == null)
                                {
                                    stateStr = null;
                                }
                                else
                                {
                                    stateMgr.writeState(context, stateObj);
                                    stateStr = stateWriter.getAndResetBuffer();
                                }                                
                                
                                if (stateStr != null)
                                {
                                    origWriter.write(stateStr);
                                }
                                start = end + STATE_KEY_LEN;
                                end = content.indexOf(STATE_KEY, start);
                            }

                            origWriter.write(content, start, content.length() - start);
                            // No trace of any saved state, so we just need to flush
                            // the buffer
                        }
                        else
                        {
                            origWriter.write(content);
                        }
                    }
                    else if (stateWriter.isStateWrittenWithoutWrapper())
                    {
                        // The state token has been written but the state has not been
                        // saved yet.
                        stateMgr.saveView(context);
                    }
                    else
                    {
                        // GET case without any form that trigger state saving.
                        // Try to store it into cache.
                        if (_viewPoolProcessor != null && 
                            _viewPoolProcessor.isViewPoolEnabledForThisView(context, view))
                        {
                            ViewDeclarationLanguage vdl = context.getApplication().
                                    getViewHandler().getViewDeclarationLanguage(
                                        context, view.getViewId());

                            if (ViewDeclarationLanguage.FACELETS_VIEW_DECLARATION_LANGUAGE_ID.equals(
                                    vdl.getId()))
                            {
                                StateManagementStrategy sms = vdl.getStateManagementStrategy(
                                        context, view.getId());
                                if (sms != null)
                                {
                                    context.getAttributes().put(ViewPoolProcessor.FORCE_HARD_RESET, Boolean.TRUE);

                                    // Force indirectly to store the map in the cache
                                    try
                                    {
                                        Object state = sms.saveView(context);
                                    }
                                    finally
                                    {
                                        context.getAttributes().remove(ViewPoolProcessor.FORCE_HARD_RESET);
                                    }

                                    // Clear the calculated value from the application map
                                    context.getAttributes().remove(SERIALIZED_VIEW_REQUEST_ATTR);
                                }
                            }
                        }
                    }
                }
                finally
                {
                    // The Facelets implementation must close the writer used to write the response
                    writer.close();
                }
            }
            finally
            {
                stateWriter.release(context);
            }
        }
        catch (FileNotFoundException fnfe)
        {
            handleFaceletNotFound(context, view.getViewId());
        }
        catch (Exception e)
        {
            handleRenderException(context, e);
        }
    }
    
    private static final String SERIALIZED_VIEW_REQUEST_ATTR = 
        StateManagerImpl.class.getName() + ".SERIALIZED_VIEW";

    /**
     * {@inheritDoc}
     */
    @Override
    public UIViewRoot createView(FacesContext context, String viewId)
    {
        checkNull(viewId, "viewId");
        // we have to check for a possible debug request
        if (UIDebug.debugRequest(context))
        {
            // the current request is a debug request, so we don't need
            // to create a view, since the output has already been written
            // in UIDebug.debugRequest() and facesContext.responseComplete()
            // has been called.
            return null;
        }
        else
        {
            UIViewRoot root = super.createView(context, viewId);
            if (root != null)
            {
                //Ensure calculateResourceLibraryContracts() can be decorated
                ViewDeclarationLanguage vdl = context.getApplication().getViewHandler().
                    getViewDeclarationLanguage(context, viewId);
                List<String> contracts = vdl.calculateResourceLibraryContracts(
                    context, root.getViewId() != null ? root.getViewId() : viewId);
                context.setResourceLibraryContracts(contracts);
            }
            return root;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UIViewRoot restoreView(FacesContext context, String viewId)
    {
        checkNull(viewId, "viewId");
        // Currently there is no way, in which UIDebug.debugRequest(context)
        // can create debug information and return true at this point,
        // because this method is only accessed if the current request
        // is a postback, which will never be true for a debug page.
        // The only point where valid debug output can be produced by now
        // is in createView() -= Jakob Korherr =-
        //if (UIDebug.debugRequest(context))
        //{
        //    return new UIViewRoot();
        //}

        //else if (!_buildBeforeRestore)
        //{
        
        // When partial state saving is not used, createView() is not called. To ensure
        // everything is in place it is necessary to calculate the resource library 
        // contracts and set them into facesContext.
        ViewDeclarationLanguage vdl = context.getApplication().getViewHandler().
            getViewDeclarationLanguage(context, viewId);
        List<String> contracts = vdl.calculateResourceLibraryContracts(
            context, viewId);
        context.setResourceLibraryContracts(contracts);
        
        // JSF 2.2 stateless views
        // We need to check if the incoming view is stateless or not and if that so rebuild it here
        // note we cannot do this in DefaultFaceletsStateManagementStrategy because it is only used
        // when PSS is enabled, but stateless views can be used without PSS. If the view is stateless,
        // there is no need to ask to the StateManager.
        Application application = context.getApplication();
        ViewHandler applicationViewHandler = application.getViewHandler();
        String renderKitId = applicationViewHandler.calculateRenderKitId(context);

        ResponseStateManager manager = getRenderKitFactory().getRenderKit(
            context, renderKitId).getResponseStateManager();
        
        if (manager.isStateless(context, viewId))
        {
            // Per the spec: build the view.
            UIViewRoot view = null;
            try
            {
                ViewMetadata metadata = vdl.getViewMetadata (context, viewId);
                if (metadata != null)
                {
                    view = metadata.createMetadataView(context);
                }
                if (view == null)
                {
                    view = context.getApplication().getViewHandler().createView(context, viewId);
                }
                context.setViewRoot (view); 
                boolean oldContextEventState = context.isProcessingEvents();
                try 
                {
                    context.setProcessingEvents (true);
                    vdl.buildView (context, view);
                    // If the view is not transient, then something is wrong. Throw an exception.
                    if (!view.isTransient())
                    {
                        throw new FacesException ("unable to create view \"" + viewId + "\"");
                    } 
    
                }
                finally
                {
                    context.setProcessingEvents (oldContextEventState);
                } 
            }
            catch (Throwable e)
            {
                throw new FacesException ("unable to create view \"" + viewId + "\"", e);
            }
            FaceletsViewDeclarationLanguageUtils.markRenderedResources(context, view);
            return view;
        }
        else
        {
            UIViewRoot root = super.restoreView(context, viewId);
            FaceletsViewDeclarationLanguageUtils.markRenderedResources(context, root);
            return root;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected String calculateViewId(FacesContext context, String viewId)
    {
        if (_cachedViewHandlerSupport == null)
        {
            _cachedViewHandlerSupport = new DefaultViewHandlerSupport(context);
        }

        return _cachedViewHandlerSupport.calculateViewId(context, viewId);
    }

    /**
     * Creates the Facelet page compiler.
     *
     * @param context
     *            the current FacesContext
     *
     * @return the application's Facelet page compiler
     */
    protected Compiler createCompiler(FacesContext context)
    {
        Compiler compiler = new SAXCompiler();

        compiler.setDevelopmentProjectStage(context.isProjectStage(ProjectStage.Development));

        loadLibraries(context, compiler);
        loadDecorators(context, compiler);
        loadOptions(context, compiler);

        return compiler;
    }

    /**
     * Creates a FaceletFactory instance using the specified compiler.
     *
     * @param context
     *            the current FacesContext
     * @param compiler
     *            the compiler to be used by the factory
     *
     * @return the factory used by this VDL to load pages
     */
    protected FaceletFactory createFaceletFactory(FacesContext context, Compiler compiler)
    {
        ExternalContext eContext = context.getExternalContext();

        // refresh period
        long refreshPeriod;
        if (context.isProjectStage(ProjectStage.Production))
        {
            refreshPeriod = WebConfigParamUtils.getLongInitParameter(eContext, PARAMS_REFRESH_PERIOD,
                    DEFAULT_REFRESH_PERIOD_PRODUCTION);
        }
        else
        {
            refreshPeriod = WebConfigParamUtils.getLongInitParameter(eContext, PARAMS_REFRESH_PERIOD,
                    DEFAULT_REFRESH_PERIOD);
        }

        // resource resolver
        ResourceResolver resolver = new DefaultResourceResolver();
        ArrayList<String> classNames = new ArrayList<String>();
        String faceletsResourceResolverClassName = WebConfigParamUtils.getStringInitParameter(eContext,
                PARAMS_RESOURCE_RESOLVER, null);
        List<String> resourceResolversFromAnnotations = RuntimeConfig.getCurrentInstance(
            context.getExternalContext()).getResourceResolvers();
        if (faceletsResourceResolverClassName != null)
        {
            classNames.add(faceletsResourceResolverClassName);
        }
        if (!resourceResolversFromAnnotations.isEmpty())
        {
            classNames.addAll(resourceResolversFromAnnotations);
        }
        if (!classNames.isEmpty())
        {
            resolver = ClassUtils.buildApplicationObject(ResourceResolver.class, classNames, resolver);
        }

        _resourceResolver = resolver;

        return new DefaultFaceletFactory(compiler, resolver, refreshPeriod);
    }


    protected ResponseWriter createResponseWriter(FacesContext context) throws IOException, FacesException
    {
        ExternalContext extContext = context.getExternalContext();
        RenderKit renderKit = context.getRenderKit();
        // Avoid a cryptic NullPointerException when the renderkit ID
        // is incorrectly set
        if (renderKit == null)
        {
            String id = context.getViewRoot().getRenderKitId();
            throw new IllegalStateException("No render kit was available for id \"" + id + "\"");
        }

        // set the buffer for content
        if (_bufferSize != -1)
        {
            extContext.setResponseBufferSize(_bufferSize);
        }

        // get our content type
        String contentType = (String) context.getAttributes().get("facelets.ContentType");

        // get the encoding
        String encoding = (String) context.getAttributes().get("facelets.Encoding");

        // -= Leonardo Uribe =- Add */* to the contentType is a fix done from FaceletViewHandler
        // to make old RI versions work, but since this is for JSF 2.0 it is not necessary that code.
        ResponseWriter writer = renderKit.createResponseWriter(FaceletsVDLUtils.NullWriter.INSTANCE,
            contentType, encoding);

        //ResponseWriter writer;
        // append */* to the contentType so createResponseWriter will succeed no matter
        // the requested contentType.
        //if (contentType != null && !contentType.equals("*/*"))
        //{
        //    contentType += ",*/*";
        //}
        // Create a dummy ResponseWriter with a bogus writer,
        // so we can figure out what content type the ReponseWriter
        // is really going to ask for
        //try
        //{
        //    writer = renderKit.createResponseWriter(NullWriter.Instance, contentType, encoding);
        //}
        // catch (IllegalArgumentException e)
        //{
        // Added because of an RI bug prior to 1.2_05-b3. Might as well leave it in case other
        // impls have the same problem. https://javaserverfaces.dev.java.net/issues/show_bug.cgi?id=613
        //log.finest("The impl didn't correctly handled '*/*' in the content type list.  Trying '*/*' directly.");
        //writer = renderKit.createResponseWriter(NullWriter.Instance, "*/*", encoding);
        //}

        // Override the JSF provided content type if necessary
        contentType = getResponseContentType(context, writer.getContentType());
        encoding = getResponseEncoding(context, writer.getCharacterEncoding());

        // apply them to the response
        extContext.setResponseContentType(contentType + "; charset=" + encoding);

        // removed 2005.8.23 to comply with J2EE 1.3
        // response.setCharacterEncoding(encoding);

        // Now, clone with the real writer
        writer = writer.cloneWithWriter(extContext.getResponseOutputWriter());

        return writer;
    }

    /**
     * @deprecated this code is not used anymore
     */
    @Deprecated
    protected String getDefaultSuffix(FacesContext context) throws FacesException
    {
        if (_defaultSuffix == null)
        {
            ExternalContext eContext = context.getExternalContext();

            String viewSuffix = eContext.getInitParameter(ViewHandler.DEFAULT_SUFFIX_PARAM_NAME);

            _defaultSuffix = viewSuffix == null ? ViewHandler.DEFAULT_FACELETS_SUFFIX : viewSuffix;
        }

        return _defaultSuffix;
    }

    /**
     * @deprecated
     */
    @Deprecated
    protected String getRenderedViewId(FacesContext context, String actionId)
    {
        // The previous code comes from Facelets 1.1.x but now it becomes invalid. In JSF 2.1, it is possible
        // to have multiple file extensions, and to make work correctly viewExists(), it is necessary to return
        // the viewId without changes
        return actionId;
    }

    /**
     * Generate the content type
     *
     * @param context
     * @param orig
     * @return
     */
    protected String getResponseContentType(FacesContext context, String orig)
    {
        String contentType = orig;

        // see if we need to override the contentType
        Map<Object, Object> m = context.getAttributes();
        if (m.containsKey("facelets.ContentType"))
        {
            contentType = (String) m.get("facelets.ContentType");
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("Facelet specified alternate contentType '" + contentType + "'");
            }
        }

        // safety check
        if (contentType == null)
        {
            contentType = "text/html";
            log.finest("ResponseWriter created had a null ContentType, defaulting to text/html");
        }

        return contentType;
    }

    /**
     * Generate the encoding
     *
     * @param context
     * @param orig
     * @return
     */
    protected String getResponseEncoding(FacesContext context, String orig)
    {
        String encoding = orig;

        // see if we need to override the encoding
        Map<Object, Object> m = context.getAttributes();
        
        Object session = context.getExternalContext().getSession(false);

        // 1. check the request attribute
        if (m.containsKey(PARAM_ENCODING))
        {
            encoding = (String) m.get(PARAM_ENCODING);
            if (encoding != null && log.isLoggable(Level.FINEST))
            {
                log.finest("Facelet specified alternate encoding '" + encoding + "'");
            }

            if (session != null)
            {
                context.getExternalContext().getSessionMap().put(CHARACTER_ENCODING_KEY, encoding);
            }
        }

        // 2. get it from request
        if (encoding == null)
        {
            encoding = context.getExternalContext().getRequestCharacterEncoding();
        }

        // 3. get it from the session
        if (encoding == null)
        {
            if (session != null)
            {
                encoding = (String) context.getExternalContext().getSessionMap().get(CHARACTER_ENCODING_KEY);
                if (encoding != null && log.isLoggable(Level.FINEST))
                {
                    log.finest("Session specified alternate encoding '" + encoding + '\'');
                }
            }
        }

        // 4. default it
        if (encoding == null)
        {
            encoding = DEFAULT_CHARACTER_ENCODING;
            if (log.isLoggable(Level.FINEST))
            {
                log.finest("ResponseWriter created had a null CharacterEncoding, defaulting to " + encoding);
            }
        }

        return encoding;
    }

    protected void handleFaceletNotFound(FacesContext context, String viewId) throws FacesException, IOException
    {
        String actualId = context.getApplication().getViewHandler().getActionURL(context, viewId);
        context.getExternalContext().responseSendError(HttpServletResponse.SC_NOT_FOUND, actualId);
        context.responseComplete();

    }

    protected void handleRenderException(FacesContext context, Exception e)
            throws IOException, ELException, FacesException
    {
        /*
        UIViewRoot root = context.getViewRoot();
        StringBuffer sb = new StringBuffer(64);
        sb.append("Error Rendering View");
        if (root != null)
        {
            sb.append('[');
            sb.append(root.getViewId());
            sb.append(']');
        }
        
        log.log(Level.SEVERE, sb.toString(), e);
        */

        // rethrow the Exception to be handled by the ExceptionHandler
        if (e instanceof RuntimeException)
        {
            throw (RuntimeException) e;
        }
        else if (e instanceof IOException)
        {
            throw (IOException) e;
        }
        else
        {
            throw new FacesException(e.getMessage(), e);
        }
    }

    /**
     * Initialize the ViewHandler during its first request.
     */
    protected void initialize(FacesContext context)
    {
        log.finest("Initializing");

        Compiler compiler = createCompiler(context);

        _faceletFactory = createFaceletFactory(context, compiler);

        ExternalContext eContext = context.getExternalContext();
        _initializeBuffer(eContext);
        _initializeMode(eContext);
        _initializeContractMappings(eContext);
        
        // Create a component ids cache and store it on application map to
        // reduce the overhead associated with create such ids over and over.
        MyfacesConfig mfConfig = MyfacesConfig.getCurrentInstance(eContext);
        if (mfConfig.getComponentUniqueIdsCacheSize() > 0)
        {
            String[] componentIdsCached = SectionUniqueIdCounter.generateUniqueIdCache("_", 
                    mfConfig.getComponentUniqueIdsCacheSize());
            eContext.getApplicationMap().put(
                    CACHED_COMPONENT_IDS, componentIdsCached);
        }
        
        _viewPoolProcessor = ViewPoolProcessor.getInstance(context);

        log.finest("Initialization Successful");
    }

    /**
     * Load the various decorators for Facelets.
     *
     * @param context
     *            the current FacesContext
     * @param compiler
     *            the page compiler
     */
    protected void loadDecorators(FacesContext context, Compiler compiler)
    {
        getFaceletsCompilerSupport().loadDecorators(context, compiler);
    }

    protected FaceletsCompilerSupport getFaceletsCompilerSupport()
    {
        if (_faceletsCompilerSupport == null)
        {
            _faceletsCompilerSupport = new FaceletsCompilerSupport();
        }
        return _faceletsCompilerSupport;
    }
    
    public void setFaceletsCompilerSupport(FaceletsCompilerSupport support)
    {
        _faceletsCompilerSupport = support;
    }

    /**
     * Load the various tag libraries for Facelets.
     *
     * @param context
     *            the current FacesContext
     * @param compiler
     *            the page compiler
     */
    protected void loadLibraries(FacesContext context, Compiler compiler)
    {
        getFaceletsCompilerSupport().loadLibraries(context, compiler);
    }

    /**
     * Load the various options for Facelets compiler. Currently only comment skipping is supported.
     *
     * @param context
     *            the current FacesContext
     * @param compiler
     *            the page compiler
     */
    protected void loadOptions(FacesContext context, Compiler compiler)
    {
        getFaceletsCompilerSupport().loadOptions(context, compiler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void sendSourceNotFound(FacesContext context, String message)
    {
        try
        {
            context.responseComplete();
            context.getExternalContext().responseSendError(HttpServletResponse.SC_NOT_FOUND, message);
        }
        catch (IOException ioe)
        {
            throw new FacesException(ioe);
        }
    }

    /**
     * Gets the Facelet representing the specified view identifier.
     *
     * @param viewId
     *            the view identifier
     *
     * @return the Facelet representing the specified view identifier
     *
     * @throws IOException
     *             if a read or parsing error occurs
     */
    private Facelet _getFacelet(FacesContext context, String viewId) throws IOException
    {
        // grab our FaceletFactory and create a Facelet
        FaceletFactory.setInstance(_faceletFactory);
        try
        {
            return _faceletFactory.getFacelet(context, viewId);
        }
        finally
        {
            FaceletFactory.setInstance(null);
        }
    }

    private Facelet _getViewMetadataFacelet(FacesContext context, String viewId) throws IOException
    {
        // grab our FaceletFactory and create a Facelet used to create view metadata
        FaceletFactory.setInstance(_faceletFactory);
        try
        {
            return _faceletFactory.getViewMetadataFacelet(context, viewId);
        }
        finally
        {
            FaceletFactory.setInstance(null);
        }
    }

    private void _initializeBuffer(ExternalContext context)
    {
        _bufferSize = WebConfigParamUtils.getIntegerInitParameter(context, PARAMS_BUFFER_SIZE, 1024);
    }

    private void _initializeMode(ExternalContext context)
    {
        String facesVersion = RuntimeConfig.getCurrentInstance(context).getFacesVersion();
        boolean partialStateSavingDefault;

        // Per spec section 11.1.3, the default value for the partial state saving feature needs
        // to be true if 2.0, false otherwise.

        partialStateSavingDefault = "2.0".equals(facesVersion) || "2.1".equals(facesVersion) || 
            "2.2".equals(facesVersion) || (facesVersion == null);

        // In jsf 2.0 this code evolve as PartialStateSaving feature
        //_buildBeforeRestore = _getBooleanParameter(context, PARAM_BUILD_BEFORE_RESTORE, false);
        _partialStateSaving = WebConfigParamUtils.getBooleanInitParameter(context,
                StateManager.PARTIAL_STATE_SAVING_PARAM_NAME, partialStateSavingDefault);

        String[] viewIds = StringUtils.splitShortString(WebConfigParamUtils.getStringInitParameter(context,
                StateManager.FULL_STATE_SAVING_VIEW_IDS_PARAM_NAME), ',');

        if (viewIds.length > 0)
        {
            _viewIds = new HashSet<String>(viewIds.length, 1.0f);
            Collections.addAll(_viewIds, viewIds);
        }
        else
        {
            _viewIds = null;
        }

        if (_partialStateSaving)
        {
            _refreshTransientBuildOnPSS = MyfacesConfig.getCurrentInstance(context).isRefreshTransientBuildOnPSS();

            _refreshTransientBuildOnPSSAuto
                    = MyfacesConfig.getCurrentInstance(context).isRefreshTransientBuildOnPSSAuto();

            _markInitialStateWhenApplyBuildView = WebConfigParamUtils.getBooleanInitParameter(context,
                    PARAM_MARK_INITIAL_STATE_WHEN_APPLY_BUILD_VIEW, false);
        }
    }
    
    private void _initializeContractMappings(ExternalContext context)
    {
        RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(context);
        List<String> prefixWildcardKeys = new ArrayList<String>();
        Map<String, List<String>> contractMappings = new HashMap<String, List<String>>();
        
        for (Map.Entry<String, List<String>> entry : runtimeConfig.getContractMappings().entrySet())
        {
            String urlPattern = entry.getKey().trim();
            if (urlPattern.endsWith(ASTERISK))
            {
                prefixWildcardKeys.add(urlPattern);
            }
            contractMappings.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
        
        Collections.sort(prefixWildcardKeys, new FaceletsVDLUtils.KeyComparator());
        
        this._prefixWildcardKeys = prefixWildcardKeys;
        this._contractMappings = contractMappings;
    }

    private boolean _usePartialStateSavingOnThisView(String viewId)
    {
        return _partialStateSaving && !(_viewIds != null && _viewIds.contains(viewId));
    }

    @Override
    public List<String> calculateResourceLibraryContracts(FacesContext context, String viewId)
    {
        List<String> contracts = this._contractMappings.get(viewId);
        if (contracts == null)
        {
            //Check prefix mapping
            for (String prefix : this._prefixWildcardKeys)
            {
                if (FaceletsVDLUtils.matchPattern(viewId, prefix))
                {
                    contracts =  this._contractMappings.get(prefix);
                    break;
                }
            }
        }
        return contracts;
    }

    private class FaceletViewMetadata extends ViewMetadataBase
    {
        /**
         * Constructor
         *
         * Note that this viewId is not the one after calculateViewId() method
         */
        public FaceletViewMetadata(String viewId)
        {
            super(viewId);
        }

        @Override
        public UIViewRoot createMetadataView(FacesContext context)
        {
            try
            {
                context.setProcessingEvents(false);

                // spec doesn't say that this is necessary, but we blow up later if
                // the viewroot isn't available from the FacesContext.
                // -= Leonardo Uribe =- since it is supposed when we apply view metadata
                // facelet we don't apply components with renderers and we don't call getRenderKit()
                // it is safe to let this one commented
                // context.setViewRoot(view);

                // -= Leonardo Uribe =- This part is related to section 2.5.5 of jsf 2.0 spec.
                // In theory what we need here is fill UIViewRoot.METADATA_FACET_NAME facet
                // with UIViewParameter instances. Later, ViewHandlerImpl.getBookmarkableURL(),
                // ViewHandlerImpl.getRedirectURL() and UIViewRoot.encodeEnd uses them. 
                // For now, the only way to do this is call buildView(context,view) method, but 
                // this is a waste of resources. We need to find another way to handle facelets view metadata.
                // Call to buildView causes the view is not created on Render Response phase,
                // if buildView is called from here all components pass through current lifecycle and only
                // UIViewParameter instances should be taken into account.
                // It should be an additional call to buildView on Render Response phase.
                // buildView(context, view);

                context.getAttributes().put(BUILDING_VIEW_METADATA, Boolean.TRUE);

                // we have to invoke createView() on the application's ViewHandler
                // here instead of invoking it directly in FaceletVDL, because
                // the ViewHandler might be wrapped and wants to do some work
                // in createView() (e.g. in Trinidad - see MYFACES-2641)
                UIViewRoot view = context.getApplication().getViewHandler().createView(context, getViewId());

                if (view != null)
                {
                    // inside createView(context,viewId), calculateViewId() is called and
                    // the result is stored inside created UIViewRoot, so we can safely take the derived
                    // viewId from there.
                    Facelet facelet = null;
                    try
                    {
                        facelet = _getViewMetadataFacelet(context, view.getViewId());
                    }
                    catch (FileNotFoundException e)
                    {
                        sendSourceNotFound(context, getViewId());
                        return null;
                    }

                    facelet.apply(context, view);
                }

                return view;
            }
            catch (IOException ioe)
            {
                throw new FacesException(ioe);
            }
            finally
            {
                context.getAttributes().remove(BUILDING_VIEW_METADATA);

                context.setProcessingEvents(true);
            }
        }
    }
    
    public FaceletFactory getFaceletFactory()
    {
        return _faceletFactory;
    }

    @Override
    public UIComponent createComponent(FacesContext context, 
        String taglibURI, String tagName, Map<String, Object> attributes)
    {
        checkNull(context, "context");
        UIComponent createdComponent = null;
        try
        {
            Facelet componentFacelet;
            FaceletFactory.setInstance(_faceletFactory);
            try
            {
                componentFacelet
                        = _faceletFactory.compileComponentFacelet(taglibURI, tagName, attributes);
            }
            finally
            {
                FaceletFactory.setInstance(null);
            }
            if (componentFacelet == null)
            {
                return null;
            }
            // Create a temporal component base class where all components will be put, but we are only
            // interested in the inner UIComponent and if multiple are created, return this one.
            boolean requiresDynamicRefresh = false;
            boolean requiresFaceletDynamicRefresh = false;
            UIPanel tempParent
                    = (UIPanel) context.getApplication().createComponent(
                    context, UIPanel.COMPONENT_TYPE, null);
            tempParent.setId(context.getViewRoot().createUniqueId(context, null));
            String baseKey = tempParent.getId();
            baseKey = baseKey.startsWith(UIViewRoot.UNIQUE_ID_PREFIX) ? baseKey.substring(4) : baseKey;

            try
            {
                tempParent.pushComponentToEL(context, tempParent);
                ((AbstractFacelet)componentFacelet).applyDynamicComponentHandler(
                    context, tempParent, baseKey);
            }
            finally
            {
                tempParent.popComponentFromEL(context);
                // There are two cases:
                // 1. If we are under facelets algorithm control (binding case), the refreshing logic will be done
                // outside this block. We can check that condition easily with FaceletCompositionContext
                // 2. If we are not under facelets algorithm control, check if the dynamic component requires refresh,
                // if that so, mark the view to be refreshed and reset the flag, otherwise continue. This check
                // allows us to decide if we add a third listener to refresh on transient build.
                    // Check if the current component requires dynamic refresh and if that so,
                FaceletCompositionContext fcc = FaceletCompositionContext.getCurrentInstance(context);
                if (fcc != null)
                {
                    requiresFaceletDynamicRefresh = true;
                }
                else if (FaceletViewDeclarationLanguageBase.isDynamicComponentNeedsRefresh(context))
                {
                    FaceletViewDeclarationLanguageBase.activateDynamicComponentRefreshTransientBuild(context);
                    FaceletViewDeclarationLanguageBase.resetDynamicComponentNeedsRefreshFlag(context);
                    requiresDynamicRefresh = true;
                }
            }
            if (tempParent.getChildCount() > 1)
            {
                // Multiple child. The tempParent will be returned. No need to
                // save MARK_CREATED.
                createdComponent = tempParent;
                tempParent.getAttributes().put("oam.vf.DYN_WRAPPER", baseKey);
                tempParent.subscribeToEvent(PostRestoreStateEvent.class, new 
                    RefreshDynamicComponentListener(taglibURI, tagName, attributes, baseKey));
                if (requiresFaceletDynamicRefresh)
                {
                    FaceletViewDeclarationLanguageBase.dynamicComponentNeedsRefresh(context);
                }
            }
            else if (tempParent.getChildCount() == 1)
            {
                createdComponent = tempParent.getChildren().get(0);
                boolean requiresRefresh = false;
                // One child. In that case there are three choices:
                if (UIComponent.isCompositeComponent(createdComponent))
                {
                    // 1. Composite component. Needs special handling because
                    // facets will be added programatically. The algorithm that
                    // process the composite component content should occur
                    // after the component is added to the view (PostAddToViewEvent).
                    // Requires refresh. To do that, we need to save the MARK_CREATED
                    // value and set it only when the full component is refreshed after 
                    // restore it.
                    createdComponent.getAttributes().put("oam.vf.GEN_MARK_ID",
                        createdComponent.getAttributes().get(ComponentSupport.MARK_CREATED));
                    createdComponent.getAttributes().put(ComponentSupport.MARK_CREATED, null);
                    createdComponent.subscribeToEvent(PostAddToViewEvent.class, new 
                        CreateDynamicCompositeComponentListener(taglibURI, tagName, attributes, baseKey));
                    requiresRefresh = true;
                    if (requiresFaceletDynamicRefresh)
                    {
                        FaceletViewDeclarationLanguageBase.dynamicComponentNeedsRefresh(context);
                    }
                }
                else if (createdComponent.getChildCount() > 0)
                {
                    // 2. Single component with children inside.
                    // Requires refresh. To do that, we need to save the MARK_CREATED
                    // value and set it only when the full component is refreshed after 
                    // restore it.
                    createdComponent.getAttributes().put("oam.vf.GEN_MARK_ID",
                        createdComponent.getAttributes().get(ComponentSupport.MARK_CREATED));
                    createdComponent.getAttributes().put(ComponentSupport.MARK_CREATED, null);
                    requiresRefresh = true;
                    if (requiresFaceletDynamicRefresh)
                    {
                        FaceletViewDeclarationLanguageBase.dynamicComponentNeedsRefresh(context);
                    }
                }
                else if (createdComponent.isTransient())
                {
                    // Just transient markup inside. It is necessary to wrap
                    // that content into a component. Requires refresh. No need to
                    // save MARK_CREATED. No requires dynamic refresh.
                    createdComponent = tempParent;
                    tempParent.getAttributes().put("oam.vf.DYN_WRAPPER", baseKey);
                    requiresRefresh = true;
                }
                else
                {
                    // 4. Single component without children: 
                    // Remove MARK_CREATED because it does not requires
                    // refresh on restore. When it is added to the component
                    // tree, it will be saved and restored as if was a programatically
                    // added component.
                    createdComponent.getAttributes().put(ComponentSupport.MARK_CREATED, null);
                }
                if (requiresRefresh)
                {
                    createdComponent.subscribeToEvent(PostRestoreStateEvent.class, new 
                        RefreshDynamicComponentListener(taglibURI, tagName, attributes, baseKey));
                }
                if (requiresDynamicRefresh)
                {
                    createdComponent.subscribeToEvent(DynamicComponentRefreshTransientBuildEvent.class, new 
                        RefreshDynamicComponentListener(taglibURI, tagName, attributes, baseKey));
                    createdComponent.getAttributes().put(
                            DynamicComponentRefreshTransientBuildEvent.DYN_COMP_REFRESH_FLAG, Boolean.TRUE);
                }
                if (requiresFaceletDynamicRefresh)
                {
                    createdComponent.subscribeToEvent(FaceletDynamicComponentRefreshTransientBuildEvent.class, new 
                        RefreshDynamicComponentListener(taglibURI, tagName, attributes, baseKey));
                }
            }
        }
        catch (IOException e)
        {
            throw new FacesException(e);
        }
        return createdComponent;
    }
    
    @Override
    public Stream<String> getViews(FacesContext facesContext, String path, int maxDepth, ViewVisitOption... options)
    {
        Stream<String> stream = super.getViews(facesContext, path, maxDepth, options);
        RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(facesContext.getExternalContext());
            stream = stream.filter(f -> (_strategy.handles(f) && 
                    !FaceletsTemplateMappingUtils.matchTemplate(runtimeConfig, f) ) );
        if (options != null &&
            Arrays.binarySearch(options, ViewVisitOption.RETURN_AS_MINIMAL_IMPLICIT_OUTCOME) >= 0)
        {
            stream = stream.map(f -> _strategy.getMinimalImplicitOutcome(f));
        }
        return stream;
    }
    
}
