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
package javax.faces.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FactoryFinder;
import javax.faces.application.ProjectStage;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.event.PostConstructViewMapEvent;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.ViewMetadata;
import javax.faces.webapp.FacesServlet;

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 * Creates a JSF View, which is a container that holds all of the components that are part of the view.
 * <p>
 * Unless otherwise specified, all attributes accept static values or EL expressions.
 * </p>
 * <p>
 * See the javadoc for this class in the <a href="http://java.sun.com/j2ee/javaserverfaces/1.2/docs/api/index.html">JSF
 * Specification</a> for further details.
 * </p>
 */
@JSFComponent(name = "f:view", bodyContent = "JSP", tagClass = "org.apache.myfaces.taglib.core.ViewTag")
@JSFJspProperty(name = "binding", returnType = "java.lang.String", tagExcluded = true)
public class UIViewRoot extends UIComponentBase implements UniqueIdVendor
{
    public static final String COMPONENT_FAMILY = "javax.faces.ViewRoot";
    public static final String COMPONENT_TYPE = "javax.faces.ViewRoot";
    public static final String METADATA_FACET_NAME = "javax_faces_metadata";
    public static final String UNIQUE_ID_PREFIX = "j_id";
    public static final String VIEW_PARAMETERS_KEY = "javax.faces.component.VIEW_PARAMETERS_KEY";

    private transient Logger logger = null;

    private static final PhaseProcessor APPLY_REQUEST_VALUES_PROCESSOR = new ApplyRequestValuesPhaseProcessor();
    private static final PhaseProcessor PROCESS_VALIDATORS_PROCESSOR = new ProcessValidatorPhaseProcessor();
    private static final PhaseProcessor UPDATE_MODEL_PROCESSOR = new UpdateModelPhaseProcessor();

    private static final VisitCallback RESET_VALUES_CALLBACK = new ResetValuesCallback();
    
    /**
     * Class that is used to create the view scope map. This strategy
     * allows change the implementation of view scope map to use cdi or
     * whatever without change UIViewRoot implementation.
     */
    private static final Class<?> VIEW_SCOPE_PROXY_MAP_CLASS;

    static
    {
        Class<?> viewMapClass = null;
        try
        {
            viewMapClass = _ClassUtils.classForName(
                "org.apache.myfaces.view.ViewScopeProxyMap");
        }
        catch (Exception e)
        {
            // no op
        }
        VIEW_SCOPE_PROXY_MAP_CLASS = viewMapClass;
    }

    /**
     * The counter which will ensure a unique component id for every component instance in the tree that doesn't have an
     * id attribute set.
     */
    //private long _uniqueIdCounter = 0;

    // todo: is it right to save the state of _events and _phaseListeners?
    private List<FacesEvent> _events;

    /**
     * Map containing view scope objects. 
     * 
     * It is not expected this map hold PartialStateHolder instances,
     * so we can use saveAttachedState and restoreAttachedState methods.
     */
    private Map<String, Object> _viewScope;
    private transient boolean _restoreViewScopeStateCalled = false;

    private transient Lifecycle _lifecycle = null;
    
    private HashMap<Class<? extends SystemEvent>, List<SystemEventListener>> _systemEventListeners;
    
    // Tracks success in the beforePhase. Listeners that threw an exception
    // in beforePhase or were never called, because a previous listener threw
    // an exception, should not have their afterPhase method called
    private transient Map<PhaseId, boolean[]> listenerSuccessMap;
    
    private static final String JAVAX_FACES_LOCATION_PREFIX = "javax_faces_location_";
    private static final String JAVAX_FACES_LOCATION_HEAD = "javax_faces_location_head";
    private static final String JAVAX_FACES_LOCATION_BODY = "javax_faces_location_body";
    private static final String JAVAX_FACES_LOCATION_FORM = "javax_faces_location_form";
    
    private static final String SKIP_VIEW_MAP_SAVE_STATE = "oam.viewPool.SKIP_VIEW_MAP_SAVE_STATE";
    
    private transient int _resetSaveStateMode = 0;
    private transient boolean _resourceDependencyUniqueId;
    private transient Map<String,Object> _attributesMap;
    
    /**
     * Construct an instance of the UIViewRoot.
     */
    public UIViewRoot()
    {
        setRendererType(null);
    }

    /**
     * @since 2.0
     */
    public void addComponentResource(FacesContext context, UIComponent componentResource)
    {
        addComponentResource(context, componentResource, null);
    }

    /**
     * @since 2.0
     */
    public void addComponentResource(FacesContext context, UIComponent componentResource, String target)
    {
        // If the target argument is null
        if (target == null)
        {
            // Look for a target attribute on the component
            target = (String)componentResource.getAttributes().get("target");

            // If there is no target attribute, set target to be the default value head
            if (target == null)
            {
                target = "head";
            }
        }

        // Call getComponentResources to obtain the child list for the given target
        List<UIComponent> componentResources = _getComponentResources(context, target);

        // If the component ID of componentResource matches the ID of a resource
        // that has already been added, remove the old resource.
        String componentId = componentResource.getId();
        
        if (componentId == null)
        {
            // componentResource can have no id - calling createUniqueId makes us sure that component will have one
            // https://issues.apache.org/jira/browse/MYFACES-2775
            componentId = createUniqueId(context, null);
            componentResource.setId(componentId);
        }
        
        // This var helps to handle the case when we try to add a component that already is
        // on the resource list, because PostAddToViewEvent also is sent to components 
        // backing resources. The problem start when a component is already inside
        // componentResources list and we try to relocate it again. This leads to a StackOverflowException
        // so we need to check if a component is and prevent remove and add it again. Note
        // that remove and then add a component trigger another PostAddToViewEvent. The right
        // point to prevent this StackOverflowException is here, because this method is 
        // responsible to traverse the componentResources list and add when necessary.
        boolean alreadyAdded = false;

        //The check is only necessary if the component resource is part of the tree.
        if (componentResource.isInView())
        {
            if (componentResource.getParent() != null &&
                componentResource.getParent().getId() != null &&
                componentResource.getParent().getId().equals(JAVAX_FACES_LOCATION_PREFIX + target))
            {
                // We can assume safely that the component is in place, because there is no way to 
                // put a component resource on a component resource container without call addComponentResource
                // so relocation here will not happen.
                alreadyAdded = true;
            }
            else if (componentId != null)
            {
                for(Iterator<UIComponent> it = componentResources.iterator(); it.hasNext();)
                {
                    UIComponent component = it.next();
                    if(componentId.equals(component.getId()) && componentResource != component)
                    {
                        if (!component.isCachedFacesContext())
                        {
                            try
                            {
                                component.setCachedFacesContext(context);
                                it.remove();
                            }
                            finally
                            {
                                component.setCachedFacesContext(null);
                            }
                        }
                        else
                        {
                            it.remove();
                        }
                    }
                    else if (componentResource == component)
                    {
                        alreadyAdded = true;
                    }
                }
            }
        }
        else if (componentId != null)
        {
            for(Iterator<UIComponent> it = componentResources.iterator(); it.hasNext();)
            {
                UIComponent component = it.next();
                if(componentId.equals(component.getId()) && componentResource != component)
                {
                    if (!component.isCachedFacesContext())
                    {
                        try
                        {
                            component.setCachedFacesContext(context);
                            it.remove();
                        }
                        finally
                        {
                            component.setCachedFacesContext(null);
                        }
                    }
                    else
                    {
                        it.remove();
                    }
                }
                else if (componentResource == component)
                {
                    alreadyAdded = true;
                }
            }
        }
        
        // Add the component resource to the list
        if (!alreadyAdded)
        {
            if (!componentResource.isCachedFacesContext())
            {
                try
                {
                    componentResource.setCachedFacesContext(context);
                    componentResources.add(componentResource);
                }
                finally
                {
                    componentResource.setCachedFacesContext(context);
                }
            }
            else
            {
                componentResources.add(componentResource);
            }
        }
    }

    /**
     * Adds a The phaseListeners attached to ViewRoot.
     */
    public void addPhaseListener(PhaseListener phaseListener)
    {
        if (phaseListener == null)
        {
            throw new NullPointerException("phaseListener");
        }
        
        getStateHelper().add(PropertyKeys.phaseListeners, phaseListener);
    }

    /**
     * @since 2.0
     */
    public void broadcastEvents(FacesContext context, PhaseId phaseId)
    {
        if (_events == null)
        {
            return;
        }
        
        Events events = _getEvents(phaseId);
        
        // Spec. 3.4.2.6 Event Broadcasting:
        // Queue one or more additional events, from the same source
        // component or a different one, for processing during the
        // current lifecycle phase.
        
        // Unfortunately with that requirement it is easy to create infinite loop in processing. One example can be:
        //
        // public processAction(ActionEvent actionEvent)
        // {
        // actionEvent  = new ActionEvent(actionEvent.getComponent());
        // actionEvent.queue();
        // }
        // 
        // Thus we iterate here only 15x. If iteration overreachs 15 we output a warning  
        
        int loops = 0;
        int maxLoops = 15;
        Collection<FacesEvent> eventsAborted = new LinkedList<FacesEvent>(); 
        do
        {
            // First broadcast events that have been queued for PhaseId.ANY_PHASE.
            boolean noUnexpectedException = _broadcastAll(context, events.getAnyPhase(), eventsAborted);
            if (!noUnexpectedException)
            {
                return;
            }
            List<FacesEvent> eventsOnPhase = events.getOnPhase();
            if (!eventsAborted.isEmpty())
            {
                eventsOnPhase.removeAll(eventsAborted);
                eventsAborted.clear();
            }
            noUnexpectedException = _broadcastAll(context, eventsOnPhase, eventsAborted);
            if (!noUnexpectedException)
            {
                return;
            }

            events = _getEvents(phaseId);
            loops++;
            
        } while (events.hasMoreEvents() && loops < maxLoops);
        
        if (loops == maxLoops && events.hasMoreEvents())
        {
            // broadcast reach maxLoops - probably a infinitive recursion:
            boolean production = getFacesContext().isProjectStage(ProjectStage.Production);
            Level level = production ? Level.FINE : Level.WARNING;
            if (_getLogger().isLoggable(level))
            {
                List<String> name = new ArrayList<String>(events.getAnyPhase().size() + events.getOnPhase().size());
                for (FacesEvent facesEvent : events.getAnyPhase())
                {
                    String clientId = facesEvent.getComponent().getClientId(getFacesContext());
                    name.add(clientId);
                }
                for (FacesEvent facesEvent : events.getOnPhase())
                {
                    String clientId = facesEvent.getComponent().getClientId(getFacesContext());
                    name.add(clientId);
                }
                _getLogger().log(level,
                        "Event broadcating for PhaseId {0} at UIViewRoot {1} reaches maximal limit, please check " +
                        "listeners for infinite recursion. Component id: {2}",
                        new Object [] {phaseId, getViewId(), name});
            }
        }
    }


    /**
     * Provides a unique id for this component instance.
     */
    public String createUniqueId()
    {
        return createUniqueId(getFacesContext(), null);
    }

    /**
     * 
     * {@inheritDoc}
     * 
     * @since 2.0
     */
    public String createUniqueId(FacesContext context, String seed)
    {

        // Generate an identifier for a component. The identifier will be prefixed with
        // UNIQUE_ID_PREFIX, and will be unique within this UIViewRoot.
        if(seed==null)
        {
            if (isResourceDependencyUniqueId())
            {
                Integer uniqueIdCounter = (Integer) getStateHelper().get(
                    PropertyKeys.resourceDependencyUniqueIdCounter);
                uniqueIdCounter = (uniqueIdCounter == null) ? 0 : uniqueIdCounter;
                getStateHelper().put(PropertyKeys.resourceDependencyUniqueIdCounter, (uniqueIdCounter+1));
                if (uniqueIdCounter >= _ComponentUtils.UNIQUE_COMPONENT_RD_IDS_SIZE)
                {
                    StringBuilder bld = _getSharedStringBuilder(context);
                    return bld.append(UNIQUE_ID_PREFIX).append(
                        _ComponentUtils.RD_ID_PREFIX).append(uniqueIdCounter).toString();
                }
                else
                {
                    return _ComponentUtils.UNIQUE_COMPONENT_RD_IDS[uniqueIdCounter];
                }
            }
            else
            {
                Integer uniqueIdCounter = (Integer) getStateHelper().get(PropertyKeys.uniqueIdCounter);
                uniqueIdCounter = (uniqueIdCounter == null) ? 0 : uniqueIdCounter;
                getStateHelper().put(PropertyKeys.uniqueIdCounter, (uniqueIdCounter+1));
                if (uniqueIdCounter >= _ComponentUtils.UNIQUE_COMPONENT_V_IDS_SIZE)
                {
                    StringBuilder bld = _getSharedStringBuilder(context);
                    return bld.append(UNIQUE_ID_PREFIX).append(
                        _ComponentUtils.V_ID_PREFIX).append(uniqueIdCounter).toString();
                }
                else
                {
                    return _ComponentUtils.UNIQUE_COMPONENT_V_IDS[uniqueIdCounter];
                }
            }
        }
        // Optionally, a unique seed value can be supplied by component creators which
        // should be included in the generated unique id.
        else
        {
            StringBuilder bld = _getSharedStringBuilder(context);
            return bld.append(UNIQUE_ID_PREFIX).append(seed).toString();
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException
    {
        checkNull(context, "context");

        boolean skipPhase = false;

        try
        {
            skipPhase = notifyListeners(context, PhaseId.RENDER_RESPONSE, getBeforePhaseListener(), true);
        }
        catch (Exception e)
        {
            // following the spec we have to swallow the exception
            _getLogger().log(Level.SEVERE, "Exception while processing phase listener: " + e.getMessage(), e);
        }

        if (!skipPhase)
        {
            //prerendering happens, we now publish the prerender view event
            //the specs states that the viewroot as source is about to be rendered
            //hence we issue the event immediately before publish, if the phase is not skipped
            //context.getApplication().publishEvent(context, PreRenderViewEvent.class, this);
            //then the view rendering is about to begin
            super.encodeBegin(context);
        }
        else
        {
            pushComponentToEL(context, this);
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public void encodeChildren(FacesContext context) throws IOException
    {
        if (context.getResponseComplete())
        {
            return;
        }
        PartialViewContext pContext = context.getPartialViewContext();
        
        // If PartialViewContext.isAjaxRequest() returns true
        if (pContext.isAjaxRequest())
        {
            // Perform partial rendering by calling PartialViewContext.processPartial() with PhaseId.RENDER_RESPONSE.
            //sectin 13.4.3 of the jsf2 specification
            pContext.processPartial(PhaseId.RENDER_RESPONSE);
        }
        else
        {
            // If PartialViewContext.isAjaxRequest() returns false
            // delegate to super.encodeChildren(javax.faces.context.FacesContext) method.
            super.encodeChildren(context);
        }
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException
    {
        checkNull(context, "context");

        if (!context.getResponseComplete())
        {
            super.encodeEnd(context);
            
            // the call to encodeAll() on every UIViewParameter here is only necessary
            // if the current request is _not_ an AJAX request, because if it was an
            // AJAX request, the call would already have happened in PartialViewContextImpl and
            // would anyway be too late here, because the state would already have been generated
            PartialViewContext partialContext = context.getPartialViewContext();
            if (!partialContext.isAjaxRequest())
            {
                ViewDeclarationLanguage vdl
                        = context.getApplication().getViewHandler().getViewDeclarationLanguage(context, getViewId());
                if (vdl != null)
                {
                    // If the current view has view parameters, as indicated by a non-empty
                    // and non-UnsupportedOperationException throwing
                    // return from ViewDeclarationLanguage.getViewMetadata(javax.faces.context.FacesContext, String)
                    ViewMetadata metadata = null;
                    try
                    {
                        metadata = vdl.getViewMetadata(context, getViewId());    
                    }
                    catch(UnsupportedOperationException e)
                    {
                        _getLogger().log(Level.SEVERE, "Exception while obtaining the view metadata: " +
                                e.getMessage(), e);
                    }
                    
                    if (metadata != null)
                    {
                        try
                        {
                            Collection<UIViewParameter> viewParams = ViewMetadata.getViewParameters(this);    
                            if(!viewParams.isEmpty())
                            {
                                // call UIViewParameter.encodeAll(javax.faces.context.FacesContext) on each parameter.
                                for(UIViewParameter param : viewParams)
                                {
                                    param.encodeAll(context);
                                }
                            }
                        }
                        catch(UnsupportedOperationException e)
                        {
                            // If calling getViewParameters() causes UnsupportedOperationException
                            // to be thrown, the exception must be silently swallowed.
                        }
                    }
                }
            }
        }
        
        try
        {
            notifyListeners(context, PhaseId.RENDER_RESPONSE, getAfterPhaseListener(), false);
        }
        catch (Exception e)
        {
            // following the spec we have to swallow the exception
            _getLogger().log(Level.SEVERE, "Exception while processing phase listener: " + e.getMessage(), e);
        }
    }

    /**
     * MethodBinding pointing to a method that takes a javax.faces.event.PhaseEvent and returns void, called after every
     * phase except for restore view.
     *
     * @return the new afterPhaseListener value
     */
    @JSFProperty(returnSignature = "void", methodSignature = "javax.faces.event.PhaseEvent",
                 jspName = "afterPhase", stateHolder=true)
    public MethodExpression getAfterPhaseListener()
    {
        return (MethodExpression) getStateHelper().eval(PropertyKeys.afterPhaseListener);
    }

    /**
     * MethodBinding pointing to a method that takes a javax.faces.event.PhaseEvent and returns void, called before
     * every phase except for restore view.
     *
     * @return the new beforePhaseListener value
     */
    @JSFProperty(returnSignature = "void", methodSignature = "javax.faces.event.PhaseEvent",
                 jspName = "beforePhase", stateHolder=true)
    public MethodExpression getBeforePhaseListener()
    {
        return (MethodExpression) getStateHelper().eval(PropertyKeys.beforePhaseListener);
    }

    /**
     * @since 2.0
     */
    public List<UIComponent> getComponentResources(FacesContext context, String target)
    {
        if (target == null)
        {
            throw new NullPointerException("target");
        }
        // Locate the facet for the component by calling getFacet() using target as the argument
        UIComponent facet = getFacet(target);

        /*
        // If the facet is not found,
        if (facet == null)
        {
            // create the facet by calling context.getApplication().createComponent()
            // using javax.faces.Panel as the argument
            facet = context.getApplication().createComponent("javax.faces.Panel");

            // Set the id of the facet to be target
            facet.setId(target);

            // Add the facet to the facets Map using target as the key
            getFacets().put(target, facet);
        }

        // Return the children of the facet
        // The API doc indicates that this method should "Return an unmodifiable
        // List of UIComponents for the provided target argument."
        // and also that "If no children are found for the facet, return Collections.emptyList()."
        List<UIComponent> children = facet.getChildren();
        return ( children == null ? Collections.<UIComponent>emptyList() : Collections.unmodifiableList(children) );
        */
        if (facet != null)
        {
            if (facet.getChildCount() > 0)
            {
                return Collections.unmodifiableList(facet.getChildren());
            }
            else
            {
                return Collections.<UIComponent>emptyList();
            }
        }
        return Collections.<UIComponent>emptyList();
    }
    
    private List<UIComponent> _getComponentResources(FacesContext context, String target)
    {
        // Locate the facet for the component by calling getFacet() using target as the argument
        UIComponent facet = getFacet(target);

        // If the facet is not found,
        if (facet == null)
        {
            // create the facet by calling context.getApplication().createComponent()
            // using javax.faces.Panel as the argument
            facet = context.getApplication().createComponent(context,
                "javax.faces.ComponentResourceContainer", null);

            // Set the id of the facet to be target
            if (target.equals("head"))
            {
                facet.setId(JAVAX_FACES_LOCATION_HEAD);
            }
            else if (target.equals("body"))
            {
                facet.setId(JAVAX_FACES_LOCATION_BODY);
            }
            else if (target.equals("form"))
            {
                facet.setId(JAVAX_FACES_LOCATION_FORM);
            }
            else
            {
                facet.setId(JAVAX_FACES_LOCATION_PREFIX + target);
            }
            
            // From jsr-314-open list it was made clear this facet is transient,
            // because all component resources does not change its inner state between
            // requests
            //
            // MYFACES-3047 It was found that resources added using ResourceDependency annotation
            // requires to be saved and restored, so it is not possible to mark this facets
            // as transient. The previous statement is true only for PSS.
            //facet.setTransient(true);

            // Add the facet to the facets Map using target as the key
            getFacets().put(target, facet);
        }
        return facet.getChildren();
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }

    /**
     * The locale for this view.
     * <p>
     * Defaults to the default locale specified in the faces configuration file.
     * </p>
     */
    @JSFProperty
    public Locale getLocale()
    {
        Object locale = getStateHelper().get(PropertyKeys.locale);
        if (locale != null)
        {
            return (Locale)locale;
        }
        ValueExpression expression = getValueExpression(PropertyKeys.locale.toString());
        if (expression != null)
        {
            Object veLocale = expression.getValue(getFacesContext().getELContext());
            if (veLocale instanceof Locale)
            {
                return (Locale) veLocale;
            }
            else
            {
                return (Locale) _LocaleUtils.toLocale(veLocale.toString());
            }
        }
        else
        {
            locale = getFacesContext().getApplication().getViewHandler().calculateLocale(getFacesContext());

            if (locale instanceof Locale)
            {
                return (Locale)locale;
            }
            else if (locale instanceof String)
            {
                return _LocaleUtils.toLocale((String)locale);
            }
        }

        return getFacesContext().getApplication().getViewHandler().calculateLocale(getFacesContext());
    }

    /**
     * @since 2.0
     */
    public List<PhaseListener> getPhaseListeners()
    {
        List<PhaseListener> listeners = (List<PhaseListener>) getStateHelper().get(PropertyKeys.phaseListeners);
        if (listeners == null)
        {
            listeners = Collections.emptyList();
        }
        else
        {
            listeners = Collections.unmodifiableList(listeners);
        }

        return listeners;
    }

    /**
     * Defines what renderkit should be used to render this view.
     */
    @JSFProperty
    public String getRenderKitId()
    {
        return (String) getStateHelper().eval(PropertyKeys.renderKitId);
    }

    /**
     * @since 2.0
     */
    @Override
    public boolean getRendersChildren()
    {
        // Call UIComponentBase.getRendersChildren() 
        // If PartialViewContext.isAjaxRequest()  returns true this method must return true.
        PartialViewContext context = getFacesContext().getPartialViewContext();

        return (context.isAjaxRequest()) ? true : super.getRendersChildren();
    }

    /**
     * A unique identifier for the "template" from which this view was generated.
     * <p>
     * Typically this is the filesystem path to the template file, but the exact details are the responsibility of the
     * current ViewHandler implementation.
     */
    @JSFProperty(tagExcluded = true)
    public String getViewId()
    {
        return (String) getStateHelper().get(PropertyKeys.viewId);
    }

    /**
     * @since 2.0
     */
    public Map<String, Object> getViewMap()
    {
        return this.getViewMap(true);
    }

    /**
     * @since 2.0
     */
    public Map<String, Object> getViewMap(boolean create)
    {
        if (_viewScope == null && create)
        {
            FacesContext facesContext = getFacesContext();
            if (VIEW_SCOPE_PROXY_MAP_CLASS != null)
            {
                _viewScope = (Map<String, Object>) 
                    _ClassUtils.newInstance(VIEW_SCOPE_PROXY_MAP_CLASS);
                facesContext.getApplication().publishEvent(facesContext, PostConstructViewMapEvent.class, this);
            }
            else
            {
                //Default to map for testing purposes
                _viewScope = new ViewScope();
                facesContext.getApplication().publishEvent(facesContext, PostConstructViewMapEvent.class, this);
            }
        }

        return _viewScope;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInView()
    {
        return true;
    }

    public void processApplication(final FacesContext context)
    {
        checkNull(context, "context");
        _process(context, PhaseId.INVOKE_APPLICATION, null);
    }

    @Override
    public void processDecodes(FacesContext context)
    {
        checkNull(context, "context");
        _process(context, PhaseId.APPLY_REQUEST_VALUES, APPLY_REQUEST_VALUES_PROCESSOR);
    }

    /**
     * @since 2.0
     */
    @Override
    public void processRestoreState(FacesContext context, Object state)
    {
        // The default implementation must call UIComponentBase.processRestoreState(javax.faces.context.FacesContext,
        // java.lang.Object) from within a try block.
        try
        {
            super.processRestoreState(context, state);
        }
        finally
        {
            // The try block must have a finally block that ensures that no FacesEvents remain in the event queue
            broadcastEvents(context, PhaseId.RESTORE_VIEW);

            //visitTree(VisitContext.createVisitContext(context), new RestoreStateCallback());
        }
    }

    @Override
    public void queueEvent(FacesEvent event)
    {
        checkNull(event, "event");
        if (_events == null)
        {
            _events = new ArrayList<FacesEvent>();
        }

        _events.add(event);
    }

    @Override
    public void processValidators(FacesContext context)
    {
        checkNull(context, "context");
        _process(context, PhaseId.PROCESS_VALIDATIONS, PROCESS_VALIDATORS_PROCESSOR);
    }

    @Override
    public void processUpdates(FacesContext context)
    {
        checkNull(context, "context");
        _process(context, PhaseId.UPDATE_MODEL_VALUES, UPDATE_MODEL_PROCESSOR);
    }

    public void setLocale(Locale locale)
    {
        getStateHelper().put(PropertyKeys.locale, locale );
    }

    /**
     * Invoke view-specific phase listeners, plus an optional EL MethodExpression.
     * <p>
     * JSF1.2 adds the ability for PhaseListener objects to be added to a UIViewRoot instance, and for
     * "beforePhaseListener" and "afterPhaseListener" EL expressions to be defined on the viewroot. This method is
     * expected to be called at appropriate times, and will then execute the relevant listener callbacks.
     * <p>
     * Parameter "listener" may be null. If not null, then it is an EL expression pointing to a user method that will be
     * invoked.
     * <p>
     * Note that the global PhaseListeners are invoked via the Lifecycle implementation, not from this method here.
     * <p>
     * These PhaseListeners are processed with the same rules as the globally defined PhaseListeners, except
     * that any Exceptions, which may occur during the execution of the PhaseListeners, will only be logged
     * and not published to the ExceptionHandler.
     */
    private boolean notifyListeners(FacesContext context, PhaseId phaseId, MethodExpression listener,
                                    boolean beforePhase)
    {
        List<PhaseListener> phaseListeners = (List<PhaseListener>) getStateHelper().get(PropertyKeys.phaseListeners);
        // Check if any listener was called
        boolean listenerCalled = false;
        if (listener != null || (phaseListeners != null && !phaseListeners.isEmpty()))
        {
            // how many listeners do we have? (the MethodExpression listener is counted in either way)
            // NOTE: beforePhaseSuccess[0] always refers to the MethodExpression listener
            int listenerCount = (phaseListeners != null ? phaseListeners.size() + 1 : 1);
            
            boolean[] beforePhaseSuccess;
            if (beforePhase)
            {
                beforePhaseSuccess = new boolean[listenerCount];
                _getListenerSuccessMap().put(phaseId, beforePhaseSuccess);
            }
            else
            {
                // afterPhase - get beforePhaseSuccess from the Map
                beforePhaseSuccess = _getListenerSuccessMap().get(phaseId);
                if (beforePhaseSuccess == null)
                {
                    // no Map available - assume that everything went well
                    beforePhaseSuccess = new boolean[listenerCount];
                    Arrays.fill(beforePhaseSuccess, true);
                }
            }
            
            PhaseEvent event = createEvent(context, phaseId);

            // only invoke the listener if we are in beforePhase
            // or if the related before PhaseListener finished without an Exception
            if (listener != null && (beforePhase || beforePhaseSuccess[0]))
            {
                listenerCalled = true;
                try
                {
                    listener.invoke(context.getELContext(), new Object[] { event });
                    beforePhaseSuccess[0] = true;
                }
                catch (Throwable t) 
                {
                    beforePhaseSuccess[0] = false; // redundant - for clarity
                    _getLogger().log(Level.SEVERE, "An Exception occured while processing " +
                                             listener.getExpressionString() + 
                                             " in Phase " + phaseId, t);
                    if (beforePhase)
                    {
                        return context.getResponseComplete() ||
                                (context.getRenderResponse() && !PhaseId.RENDER_RESPONSE.equals(phaseId));
                    }
                }
            }
            else if (beforePhase)
            {
                // there is no beforePhase MethodExpression listener
                beforePhaseSuccess[0] = true;
            }

            if (phaseListeners != null && !phaseListeners.isEmpty())
            {
                if (beforePhase)
                {
                    // process listeners in ascending order
                    for (int i = 0; i < beforePhaseSuccess.length - 1; i++)
                    {
                        PhaseListener phaseListener;
                        try 
                        {
                            phaseListener = phaseListeners.get(i);
                        }
                        catch (IndexOutOfBoundsException e)
                        {
                            // happens when a PhaseListener removes another PhaseListener 
                            // from UIViewRoot in its beforePhase method
                            throw new IllegalStateException("A PhaseListener must not remove " +
                                    "PhaseListeners from UIViewRoot.");
                        }
                        PhaseId listenerPhaseId = phaseListener.getPhaseId();
                        if (phaseId.equals(listenerPhaseId) || PhaseId.ANY_PHASE.equals(listenerPhaseId))
                        {
                            listenerCalled = true;
                            try
                            {
                                phaseListener.beforePhase(event);
                                beforePhaseSuccess[i + 1] = true;
                            }
                            catch (Throwable t) 
                            {
                                beforePhaseSuccess[i + 1] = false; // redundant - for clarity
                                _getLogger().log(Level.SEVERE, "An Exception occured while processing the " +
                                                         "beforePhase method of PhaseListener " + phaseListener +
                                                         " in Phase " + phaseId, t);
                                return context.getResponseComplete() ||
                                        (context.getRenderResponse() && !PhaseId.RENDER_RESPONSE.equals(phaseId));
                            }
                        }
                    }
                }
                else
                {
                    // afterPhase
                    // process listeners in descending order
                    for (int i = beforePhaseSuccess.length - 1; i > 0; i--)
                    {
                        PhaseListener phaseListener;
                        try 
                        {
                            phaseListener = phaseListeners.get(i - 1);
                        }
                        catch (IndexOutOfBoundsException e)
                        {
                            // happens when a PhaseListener removes another PhaseListener 
                            // from UIViewRoot in its beforePhase or afterPhase method
                            throw new IllegalStateException("A PhaseListener must not remove " +
                                    "PhaseListeners from UIViewRoot.");
                        }
                        PhaseId listenerPhaseId = phaseListener.getPhaseId();
                        if ((phaseId.equals(listenerPhaseId) || PhaseId.ANY_PHASE.equals(listenerPhaseId))
                                && beforePhaseSuccess[i])
                        {
                            listenerCalled = true;
                            try
                            {
                                phaseListener.afterPhase(event);
                            }
                            catch (Throwable t) 
                            {
                                logger.log(Level.SEVERE, "An Exception occured while processing the " +
                                                         "afterPhase method of PhaseListener " + phaseListener +
                                                         " in Phase " + phaseId, t);
                            }
                        }
                    }
                }
            }
        }

        // The spec javadoc says "... Upon return from the listener, call FacesContext.getResponseComplete() 
        // and FacesContext.getRenderResponse(). If either return true set the internal state flag to true. ..."
        // and later it says:
        // "... Execute any processing for this phase if the internal state flag was not set. ..."
        // But after some testing it seems if the internal state flag is not set, the check is not done and the
        // phase is not skipped. The only exception is in render response phase.
        if (listenerCalled)
        {
            if (beforePhase)
            {
                return context.getResponseComplete() ||
                        (context.getRenderResponse() && !PhaseId.RENDER_RESPONSE.equals(phaseId));
            }
            else
            {
                return context.getResponseComplete() || context.getRenderResponse();
            }
        }
        else
        {
            if (beforePhase)
            {
                if (PhaseId.RENDER_RESPONSE.equals(phaseId))
                {
                    return context.getResponseComplete();
                }
                else
                {
                    // Don't check and don't skip
                    return false;
                }
            }
            else
            {
                // Note if is afterPhase the return value is not relevant.
                return context.getResponseComplete() || context.getRenderResponse();
            }
        }
    }

    private PhaseEvent createEvent(FacesContext context, PhaseId phaseId)
    {
        if (_lifecycle == null)
        {
            LifecycleFactory factory = (LifecycleFactory)FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
            String id = context.getExternalContext().getInitParameter(FacesServlet.LIFECYCLE_ID_ATTR);
            if (id == null)
            {
                id = LifecycleFactory.DEFAULT_LIFECYCLE;
            }
            _lifecycle = factory.getLifecycle(id);
        }
        return new PhaseEvent(context, phaseId, _lifecycle);
    }

    /**
     * Broadcast all events in the specified collection, stopping the at any time an AbortProcessingException
     * is thrown.
     *
     * @param context the current JSF context
     * @param events the events to broadcast
     * @return 
     *
     * @return <code>true</code> if the broadcast was completed without unexpected abortion/exception,
     *  <code>false</code> otherwise
     */
    private boolean _broadcastAll(FacesContext context,
                               List<? extends FacesEvent> events,
                               Collection<FacesEvent> eventsAborted)
    {
        assert events != null;

        for (int i = 0; i < events.size(); i++)
        {
            FacesEvent event = events.get(i);
            UIComponent source = event.getComponent();
            UIComponent compositeParent = UIComponent.getCompositeComponentParent(source);
            if (compositeParent != null)
            {
                pushComponentToEL(context, compositeParent);
            }
            // Push the source as the current component
            pushComponentToEL(context, source);

            try
            {
                // Actual event broadcasting
                if (!source.isCachedFacesContext())
                {
                    try
                    {
                        source.setCachedFacesContext(context);
                        source.broadcast(event);
                    }
                    finally
                    {
                        source.setCachedFacesContext(null);
                    }
                }
                else
                {
                    source.broadcast(event);
                }
            }
            catch (Exception e)
            {

                Throwable cause = e;
                AbortProcessingException ape = null;
                do
                {
                    if (cause != null && cause instanceof AbortProcessingException)
                    {
                        ape = (AbortProcessingException) cause;
                        break;
                    }
                    cause = cause.getCause();
                }
                while (cause != null);
                
                // for any other exception publish ExceptionQueuedEvent
                // publish the Exception to be handled by the ExceptionHandler
                // to publish or to not publish APE? That is the question : MYFACES-3199. We publish it,
                // because user can handle it in custom exception handler then. 
                if (ape != null)
                {
                    e = ape;
                }
                ExceptionQueuedEventContext exceptionContext 
                        = new ExceptionQueuedEventContext(context, e, source, context.getCurrentPhaseId());
                context.getApplication().publishEvent(context, ExceptionQueuedEvent.class, exceptionContext);

                
                if (ape != null)
                {
                    // APE found,  abortion for this event only
                    eventsAborted.add(event);
                }
                else
                {
                    // We can't continue broadcast processing if other exception is thrown:
                    return false;
                }
            }
            finally
            {
                // Restore the current component
                source.popComponentFromEL(context);
                if (compositeParent != null)
                {
                    compositeParent.popComponentFromEL(context);
                }
            }
        }
        return true;
    }

    private void clearEvents()
    {
        _events = null;
    }

    private void checkNull(Object value, String valueLabel)
    {
        if (value == null)
        {
            throw new NullPointerException(valueLabel + " is null");
        }
    }

    public void setRenderKitId(String renderKitId)
    {
        getStateHelper().put(PropertyKeys.renderKitId, renderKitId );
    }

    /**
     * DO NOT USE.
     * <p>
     * This inherited property is disabled. Although this class extends a base-class that defines a read/write rendered
     * property, this particular subclass does not support setting it. Yes, this is broken OO design: direct all
     * complaints to the JSF spec group.
     */
    @Override
    @JSFProperty(tagExcluded = true)
    public void setRendered(boolean state)
    {
        // Call parent method due to TCK problems
        super.setRendered(state);
        // throw new UnsupportedOperationException();
    }

    @JSFProperty(tagExcluded=true)
    @Override
    public void setId(String id)
    {
        super.setId(id);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setInView(boolean isInView)
    {
        // no-op view root is always in view
    }

    public void removeComponentResource(FacesContext context, UIComponent componentResource)
    {
        removeComponentResource(context, componentResource, null);
    }

    public void removeComponentResource(FacesContext context, UIComponent componentResource, String target)
    {
        // If the target argument is null
        if (target == null)
        {
            // Look for a target attribute on the component
            target = (String)componentResource.getAttributes().get("target");

            // If there is no target attribute
            if (target == null)
            {
                // Set target to be the default value head
                target = "head";
            }
        }


        // Call getComponentResources to obtain the child list for the given target.
        //List<UIComponent> componentResources = getComponentResources(context, target);
        UIComponent facet = getFacet(target);
        if (facet != null)
        {
            //Only if the facet is found it is possible to remove the resource,
            //otherwise nothing should happen (call to getComponentResource trigger
            //creation of facet)
            // Remove the component resource from the child list
            facet.getChildren().remove(componentResource);
        }
    }

    public void setViewId(String viewId)
    {
        // It really doesn't make much sense to allow null here.
        // However the TCK does not check for it, and sun's implementation
        // allows it so here we allow it too.
        getStateHelper().put(PropertyKeys.viewId, viewId );
    }

    /**
     * Removes a The phaseListeners attached to ViewRoot.
     */
    public void removePhaseListener(PhaseListener phaseListener)
    {
        if (phaseListener == null)
        {
            return;
        }

        getStateHelper().remove(PropertyKeys.phaseListeners, phaseListener);
    }

    /**
     * Sets
     *
     * @param beforePhaseListener
     *            the new beforePhaseListener value
     */
    public void setBeforePhaseListener(MethodExpression beforePhaseListener)
    {
        getStateHelper().put(PropertyKeys.beforePhaseListener, beforePhaseListener);
    }

    /**
     * Sets
     *
     * @param afterPhaseListener
     *            the new afterPhaseListener value
     */
    public void setAfterPhaseListener(MethodExpression afterPhaseListener)
    {
        getStateHelper().put(PropertyKeys.afterPhaseListener, afterPhaseListener);
    }

    /**
     * @return the clearTransientMapOnSaveState
     */
    int getResetSaveStateMode()
    {
        return _resetSaveStateMode;
    }

    /**
     * @param clearTransientMapOnSaveState the clearTransientMapOnSaveState to set
     */
    void setResetSaveStateMode(int clearTransientMapOnSaveState)
    {
        this._resetSaveStateMode = clearTransientMapOnSaveState;
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        if (_attributesMap == null)
        {
            _attributesMap = new _ViewAttributeMap(this, super.getAttributes());
        }
        return _attributesMap;
    }
    
    /**
     * @since 2.2
     * @param context
     * @param clientIds 
     */
    public void resetValues(FacesContext context,
                        java.util.Collection<java.lang.String> clientIds)    
    {
        VisitContext visitContext = (VisitContext) VisitContext.createVisitContext(
            context, clientIds, null);
        this.visitTree(visitContext, RESET_VALUES_CALLBACK);
    }

    /**
     * Indicates if the component is created when facelets builds the view and
     * is caused by the presence of a ResourceDependency annotation.
     * 
     * @return the _resourceDependencyUniqueId
     */
    boolean isResourceDependencyUniqueId()
    {
        return _resourceDependencyUniqueId;
    }

    void setResourceDependencyUniqueId(boolean resourceDependencyUniqueId)
    {
        this._resourceDependencyUniqueId = resourceDependencyUniqueId;
    }
    
    enum PropertyKeys
    {
         afterPhaseListener
        , beforePhaseListener
        , phaseListeners
        , locale
        , renderKitId
        , viewId
        , uniqueIdCounter
        , resourceDependencyUniqueIdCounter
    }
    
    @Override
    public Object saveState(FacesContext facesContext)
    {
        if (getResetSaveStateMode() == RESET_MODE_SOFT)
        {
            // Clear view listeners.
            if (_systemEventListeners != null)
            {
                _systemEventListeners.clear();
            }
            if (_events != null)
            {
                _events.clear();
            }
            if (listenerSuccessMap != null)
            {
                listenerSuccessMap.clear();
            }
            _restoreViewScopeStateCalled = false;
        }
        if (getResetSaveStateMode() == RESET_MODE_HARD)
        {
            // Clear view listeners.
            if (_systemEventListeners != null)
            {
                _systemEventListeners.clear();
            }
            if (_events != null)
            {
                _events.clear();
            }
            if (listenerSuccessMap != null)
            {
                listenerSuccessMap.clear();
            }
            if (_viewScope != null)
            {
                if (VIEW_SCOPE_PROXY_MAP_CLASS.isInstance(_viewScope))
                {
                    _viewScope = null;
                }
                else
                {
                    _viewScope.clear();
                }
            }
            _restoreViewScopeStateCalled = false;
        }
        
        if (initialStateMarked())
        {
            Object parentSaved = super.saveState(facesContext);
            if (_viewScope != null && 
                Boolean.TRUE.equals(facesContext.getAttributes().get(
                SKIP_VIEW_MAP_SAVE_STATE)))
            {
                if (parentSaved == null)
                {
                    return null;
                }
                return new Object[]{parentSaved, null};
            }
            
            if (parentSaved == null && _viewScope == null)
            {
                //No values
                return null;
            }
            else if (parentSaved == null && _viewScope != null && _viewScope.size() == 0
                && !(_viewScope instanceof StateHolder) )
            {
                //Empty view scope, no values
                return null;
            }
            
            Object[] values = new Object[2];
            values[0] = parentSaved;
            values[1] = saveAttachedState(facesContext,_viewScope);
            return values;
        }
        else
        {
            if (_viewScope != null && 
                Boolean.TRUE.equals(facesContext.getAttributes().get(
                SKIP_VIEW_MAP_SAVE_STATE)))
            {
                return new Object[]{super.saveState(facesContext), null};
            }
            Object[] values = new Object[2];
            values[0] = super.saveState(facesContext);
            values[1] = saveAttachedState(facesContext,_viewScope);
            return values;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void restoreState(FacesContext facesContext, Object state)
    {
        if (state == null)
        {
            return;
        }
        
        Object[] values = (Object[])state;
        super.restoreState(facesContext,values[0]);
        // JSF 2.2 spec says that restoreViewScopeState can be called but only if
        // StateManagementStrategy is used. If that's not the case (JSF 1.2 state saving),
        // restoreViewScopeState could not be called, so this code should avoid restore
        // the state twice.
        if (!_restoreViewScopeStateCalled)
        {
            _viewScope = (Map<String, Object>) restoreAttachedState(facesContext, values[1]);
        }
        else
        {
            _restoreViewScopeStateCalled = false;
        }
    }
    
    /**
     * @since 2.2
     * @param facesContext
     * @param state 
     */
    public void restoreViewScopeState(FacesContext facesContext, Object state)
    {
        if (state == null)
        {
            return;
        }
        //StateManagementStrategy says "... obtain the state of the UIViewRoot from the 
        // state Object returned from ResponseStateManager.getState(javax.faces.context.FacesContext,
        // java.lang.String) and pass that to UIViewRoot.restoreViewScopeState(
        // javax.faces.context.FacesContext, java.lang.Object).
        // Note restoreState() will be called later, and it will restore the view. If
        // we restore the component state here, later it could be a problem in the later
        // restoreState() call, because the initial state will not be the same.
        
        Object[] values = (Object[])state;
        _viewScope = (Map<String, Object>) restoreAttachedState(facesContext, values[1]);
        _restoreViewScopeStateCalled = true;
    }
    
    public List<SystemEventListener> getViewListenersForEventClass(Class<? extends SystemEvent> systemEvent)
    {
        checkNull (systemEvent, "systemEvent");
        if (_systemEventListeners == null)
        {
            return null;
        }
        return _systemEventListeners.get (systemEvent);
    }
    
    public void subscribeToViewEvent(Class<? extends SystemEvent> systemEvent,
            SystemEventListener listener)
    {
        List<SystemEventListener> listeners;
        
        checkNull (systemEvent, "systemEvent");
        checkNull (listener, "listener");
        
        if (_systemEventListeners == null)
        {
            _systemEventListeners = new HashMap<Class<? extends SystemEvent>, List<SystemEventListener>>();
        }
        
        listeners = _systemEventListeners.get (systemEvent);
        
        if (listeners == null)
        {
            listeners = new ArrayList<SystemEventListener>();
            
            _systemEventListeners.put (systemEvent, listeners);
        }
        
        listeners.add (listener);
    }
    
    public void unsubscribeFromViewEvent(Class<? extends SystemEvent> systemEvent,
            SystemEventListener listener)
    {
        List<SystemEventListener> listeners;
        
        checkNull (systemEvent, "systemEvent");
        checkNull (listener, "listener");
        
        if (_systemEventListeners == null)
        {
            return;
        }
        
        listeners = _systemEventListeners.get (systemEvent);
        
        if (listeners != null)
        {
            listeners.remove (listener);
        }
    }

    /**
     * Process the specified phase by calling PhaseListener.beforePhase for every phase listeners defined on this
     * view root, then calling the process method of the processor, broadcasting relevant events and finally
     * notifying the afterPhase method of every phase listeners registered on this view root.
     *
     * @param context
     * @param phaseId
     * @param processor
     * @param broadcast
     *
     * @return
     */
    private boolean _process(FacesContext context, PhaseId phaseId, PhaseProcessor processor)
    {
        RuntimeException processingException = null;
        try
        {
            if (!notifyListeners(context, phaseId, getBeforePhaseListener(), true))
            {
                try
                {
                    if (processor != null)
                    {
                        processor.process(context, this);
                    }
        
                    broadcastEvents(context, phaseId);
                }
                catch (RuntimeException re)
                {
                    // catch any Exception that occures while processing the phase
                    // to ensure invocation of the afterPhase methods
                    processingException = re;
                }
            }
        }
        finally
        {
            if (context.getRenderResponse() || context.getResponseComplete())
            {
                clearEvents();
            }            
        }

        boolean retVal = notifyListeners(context, phaseId, getAfterPhaseListener(), false);
        if (processingException == null) 
        {
            return retVal;   
        }
        else
        {
            throw processingException;
        }
    }

    private void _processDecodesDefault(FacesContext context)
    {
        super.processDecodes(context);
    }

    private void _processUpdatesDefault(FacesContext context)
    {
        super.processUpdates(context);
    }

    private void _processValidatorsDefault(FacesContext context)
    {
        super.processValidators(context);
    }

    /**
     * Gathers all event for current and ANY phase
     * @param phaseId current phase id
     */
    private Events _getEvents(PhaseId phaseId)
    {
        // Gather the events and purge the event list to prevent concurrent modification during broadcasting
        int size = _events.size();
        List<FacesEvent> anyPhase = new ArrayList<FacesEvent>(size);
        List<FacesEvent> onPhase = new ArrayList<FacesEvent>(size);
        
        for (int i = 0; i < size; i++)
        {
            FacesEvent event = _events.get(i);
            if (event.getPhaseId().equals(PhaseId.ANY_PHASE))
            {
                anyPhase.add(event);
                _events.remove(i);
                size--;
                i--;
            }
            else if (event.getPhaseId().equals(phaseId))
            {
                onPhase.add(event);
                _events.remove(i);
                size--;
                i--;
            }
        }
        
        return new Events(anyPhase, onPhase);
    }
    
    private Logger _getLogger()
    {
        if (logger == null)
        {
            logger = Logger.getLogger(UIViewRoot.class.getName());
        }
        return logger;
    }

    private Map<PhaseId, boolean[]> _getListenerSuccessMap()
    {
        // lazy init: 
        if (listenerSuccessMap == null)
        {
            listenerSuccessMap = new HashMap<PhaseId, boolean[]>();
        }
        return listenerSuccessMap;
    }

    private static interface PhaseProcessor
    {
        public void process(FacesContext context, UIViewRoot root);
    }

    private static class ApplyRequestValuesPhaseProcessor implements PhaseProcessor
    {
        public void process(FacesContext context, UIViewRoot root)
        {
            PartialViewContext pvc = context.getPartialViewContext();
            // Perform partial processing by calling PartialViewContext.processPartial(javax.faces.event.PhaseId)
            // with PhaseId.UPDATE_MODEL_VALUES if:
            //   * PartialViewContext.isPartialRequest() returns true and we don't have a request to process all
            // components in the view (PartialViewContext.isExecuteAll() returns false)
            //section 13.4.2 from the  JSF2  spec also see https://issues.apache.org/jira/browse/MYFACES-2119
            if (pvc.isPartialRequest() && !pvc.isExecuteAll())
            {
                pvc.processPartial(PhaseId.APPLY_REQUEST_VALUES);
            }
            // Perform full processing by calling UIComponentBase.processUpdates(javax.faces.context.FacesContext)
            // if one of the following conditions are met:
            // *   PartialViewContext.isPartialRequest() returns true and we have a request to process all components
            // in the view (PartialViewContext.isExecuteAll() returns true)
            // *   PartialViewContext.isPartialRequest() returns false
            else
            {
                root._processDecodesDefault(context);
            }
        }
    }

    private static class ProcessValidatorPhaseProcessor implements PhaseProcessor
    {
        public void process(FacesContext context, UIViewRoot root)
        {
            PartialViewContext pvc = context.getPartialViewContext();
            // Perform partial processing by calling PartialViewContext.processPartial(javax.faces.event.PhaseId)
            // with PhaseId.UPDATE_MODEL_VALUES if:
            // PartialViewContext.isPartialRequest() returns true and we don't have a request to process all components
            // in the view (PartialViewContext.isExecuteAll() returns false)
            //section 13.4.2 from the  JSF2  spec also see https://issues.apache.org/jira/browse/MYFACES-2119
            if (pvc.isPartialRequest() && !pvc.isExecuteAll())
            {
                pvc.processPartial(PhaseId.PROCESS_VALIDATIONS);
            }
            // Perform full processing by calling UIComponentBase.processUpdates(javax.faces.context.FacesContext)
            // if one of the following conditions are met:
            // *   PartialViewContext.isPartialRequest() returns true and we have a request to process all components
            // in the view (PartialViewContext.isExecuteAll() returns true)
            // *   PartialViewContext.isPartialRequest() returns false
            else
            {
                root._processValidatorsDefault(context);
            }
        }
    }

    private static class UpdateModelPhaseProcessor implements PhaseProcessor
    {
        public void process(FacesContext context, UIViewRoot root)
        {
            PartialViewContext pvc = context.getPartialViewContext();
            // Perform partial processing by calling PartialViewContext.processPartial(javax.faces.event.PhaseId)
            // with PhaseId.UPDATE_MODEL_VALUES if:
            //   * PartialViewContext.isPartialRequest() returns true and we don't have a request to process
            // all components in the view (PartialViewContext.isExecuteAll() returns false)
            //section 13.4.2 from the JSF2 spec also see https://issues.apache.org/jira/browse/MYFACES-2119
            if (pvc.isPartialRequest() && !pvc.isExecuteAll())
            {
                pvc.processPartial(PhaseId.UPDATE_MODEL_VALUES);
            }
            // Perform full processing by calling UIComponentBase.processUpdates(javax.faces.context.FacesContext)
            // if one of the following conditions are met:
            // *   PartialViewContext.isPartialRequest() returns true and we have a request to process all components
            // in the view (PartialViewContext.isExecuteAll() returns true)
            // *   PartialViewContext.isPartialRequest() returns false
            else
            {
                root._processUpdatesDefault(context);
            }
        }
    }

/*
    private static class RestoreStateCallback implements VisitCallback
    {
        private PostRestoreStateEvent event;

        public VisitResult visit(VisitContext context, UIComponent target)
        {
            if (event == null)
            {
                event = new PostRestoreStateEvent(target);
            }
            else
            {
                event.setComponent(target);
            }

            // call the processEvent method of the current component.
            // The argument event must be an instance of AfterRestoreStateEvent whose component
            // property is the current component in the traversal.
            target.processEvent(event);
            
            return VisitResult.ACCEPT;
        }
    }
*/

    // we cannot make this class a inner class, because the 
    // enclosing class (UIViewRoot) would also have to be serialized.
    /**
     * @deprecated replaced by org.apache.myfaces.view.ViewScopeProxyMap
     */
    @Deprecated
    private static class ViewScope extends HashMap<String, Object>
    {
        
        private static final long serialVersionUID = -1088893802269478164L;
        
        @Override
        public void clear()
        {
            /*
             * The returned Map must be implemented such that calling clear() on the Map causes
             * Application.publishEvent(java.lang.Class, java.lang.Object) to be called, passing
             * ViewMapDestroyedEvent.class as the first argument and this UIViewRoot instance as the second argument.
             */
            FacesContext facesContext = FacesContext.getCurrentInstance();
            facesContext.getApplication().publishEvent(facesContext, 
                    PreDestroyViewMapEvent.class, facesContext.getViewRoot());
            
            super.clear();
        }
        
    }

    /**
     * Agregates events for ANY_PHASE and current phase 
     */
    private static class Events
    {
        
        private final List<FacesEvent> _anyPhase;
        
        private final List<FacesEvent> _onPhase;
        
        public Events(List<FacesEvent> anyPhase, List<FacesEvent> onPhase)
        {
            super();
            this._anyPhase = anyPhase;
            this._onPhase = onPhase;
        }

        public boolean hasMoreEvents()
        {
            return (_anyPhase != null && _anyPhase.size() > 0) || (_onPhase != null && _onPhase.size() > 0); 
        }

        public List<FacesEvent> getAnyPhase()
        {
            return _anyPhase;
        }

        public List<FacesEvent> getOnPhase()
        {
            return _onPhase;
        }
    }
    
    private static class ResetValuesCallback implements VisitCallback
    {
        public VisitResult visit(VisitContext context, UIComponent target)
        {
            if (target instanceof EditableValueHolder)
            {
                ((EditableValueHolder)target).resetValue();
            }
            return VisitResult.ACCEPT;
        }
    }
}
