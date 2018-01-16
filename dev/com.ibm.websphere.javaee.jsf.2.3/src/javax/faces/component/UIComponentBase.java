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

import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFJspProperty;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.behavior.Behavior;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.BehaviorEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreRemoveFromViewEvent;
import javax.faces.event.PreRenderComponentEvent;
import javax.faces.event.PreValidateEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.render.RenderKit;
import javax.faces.render.Renderer;
import javax.faces.view.Location;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.event.PhaseId;


/**
 * Standard implementation of the UIComponent base class; all standard JSF components extend this class.
 * <p>
 * <i>Disclaimer</i>: The official definition for the behaviour of this class is the JSF 1.1 specification but for legal
 * reasons the specification cannot be replicated here. Any javadoc here therefore describes the current implementation
 * rather than the spec, though this class has been verified as correctly implementing the spec.
 * 
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a> for
 * more.
 */
@JSFComponent(type = "javax.faces.ComponentBase", family = "javax.faces.ComponentBase",
              desc = "base component when all components must inherit",
              tagClass = "javax.faces.webapp.UIComponentELTag", configExcluded = true)
@JSFJspProperty(name = "binding", returnType = "javax.faces.component.UIComponent",
                longDesc = "Identifies a backing bean property (of type UIComponent or appropriate subclass) to bind "
                           + "to this component instance. This value must be an EL expression.",
                desc = "backing bean property to bind to this component instance")
public abstract class UIComponentBase extends UIComponent
{
    //private static Log log = LogFactory.getLog(UIComponentBase.class);
    private static Logger log = Logger.getLogger(UIComponentBase.class.getName());

    private static final Iterator<UIComponent> _EMPTY_UICOMPONENT_ITERATOR = new _EmptyIterator<UIComponent>();

    private static final String _STRING_BUILDER_KEY
            = "javax.faces.component.UIComponentBase.SHARED_STRING_BUILDER";

    static final int RESET_MODE_OFF = 0;
    static final int RESET_MODE_SOFT = 1;
    static final int RESET_MODE_HARD = 2;

    private _ComponentAttributesMap _attributesMap = null;
    private _PassThroughAttributesMap _passthroughAttributesMap = null;
    private List<UIComponent> _childrenList = null;
    private Map<String, UIComponent> _facetMap = null;
    private _DeltaList<FacesListener> _facesListeners = null;
    private String _clientId = null;
    private String _id = null;
    private UIComponent _parent = null;
    private boolean _transient = false;
    
    //private boolean _isRendererTypeSet = false;
    private String _rendererType;
    private String _markCreated;
    private String _facetName;
    //private boolean _addedByHandler = false;
    //private boolean _facetCreatedUIPanel = false;
    //private boolean _passthroughAttributesMapSet = false;
    
    private int _capabilities = 0;
    private final static int FLAG_IS_RENDERER_TYPE_SET = 1;
    private final static int FLAG_ADDED_BY_HANDLER = 2;
    private final static int FLAG_FACET_CREATED_UIPANEL = 4;
    private final static int FLAG_PASSTHROUGH_ATTRIBUTE_MAP_SET = 8;

    /**
     * This map holds ClientBehavior instances.
     * 
     *  Note that BehaviorBase implements PartialStateHolder, so this class 
     *  should deal with that fact on clearInitialState() and 
     *  markInitialState() methods.
     * 
     *  Also, the map used by this instance is not set from outside this class.
     *  
     *  Note it is possible (but maybe not expected/valid) to manipulate 
     *  the values of the map(the list) but not put instances on the map 
     *  directly, because ClientBehaviorHolder.getClientBehaviors says that 
     *  this method should return a non null unmodificable map.
     *  
     */
    private Map<String, List<ClientBehavior>> _behaviorsMap = null;
    private transient Map<String, List<ClientBehavior>> _unmodifiableBehaviorsMap = null;
    
    private transient FacesContext _facesContext;
    private transient Boolean _cachedIsRendered;
    private transient Renderer _cachedRenderer;
    
    public UIComponentBase()
    {
    }

    /**
     * Put the provided value-binding into a map of value-bindings associated with this component.
     * 
     * @deprecated Replaced by setValueExpression
     */
    @Override
    public void setValueBinding(String name, ValueBinding binding)
    {
        setValueExpression(name, binding == null ? null : new _ValueBindingToValueExpression(binding));
    }

    /**
     * Set an identifier for this component which is unique within the scope of the nearest ancestor NamingContainer
     * component. The id is not necessarily unique across all components in the current view.
     * <p>
     * The id must start with an underscore if it is generated by the JSF framework, and must <i>not</i> start with an
     * underscore if it has been specified by the user (eg in a JSP tag).
     * <p>
     * The first character of the id must be an underscore or letter. Following characters may be letters, digits,
     * underscores or dashes.
     * <p>
     * Null is allowed as a parameter, and will reset the id to null.
     * <p>
     * The clientId of this component is reset by this method; see getClientId for more info.
     * 
     * @throws IllegalArgumentException
     *             if the id is not valid.
     */
    @Override
    public void setId(String id)
    {
        isIdValid(id);
        _id = id;
        _clientId = null;
    }

    /**
     * <p>Set the parent <code>UIComponent</code> of this
     * <code>UIComponent</code>.</p>
     * 
     * @param parent The new parent, or <code>null</code> for the root node
     *  of a component tree
     */
    @Override
    public void setParent(UIComponent parent)
    {
        // removing kids OR this is UIViewRoot
        if (parent == null)
        {
            // not UIViewRoot...
            if (_parent != null && _parent.isInView())
            {
                // trigger the "remove event" lifecycle
                // and call setInView(false) for all children/facets
                // doing this => recursive
                FacesContext facesContext = getFacesContext();
                if (facesContext.isProcessingEvents())
                {
                    _publishPreRemoveFromViewEvent(facesContext, this);
                }
                else
                {
                    _updateInView(this, false);
                }
            }
            _parent = null;
        }
        else
        {
            _parent = parent;
            if (parent.isInView())
            {
                // trigger the ADD_EVENT and call setInView(true)
                // recursive for all kids/facets...
                // Application.publishEvent(java.lang.Class, java.lang.Object)  must be called, passing 
                // PostAddToViewEvent.class as the first argument and the newly added component as the second 
                // argument.
                FacesContext facesContext = parent.isCachedFacesContext() ?
                    parent.getFacesContext() : getFacesContext();
                if (facesContext.isProcessingEvents())
                {
                    _publishPostAddToViewEvent(facesContext, this);
                }
                else
                {
                    _updateInView(this, true);
                }
            }
        }
    }

    
    /**
     * Publish PostAddToViewEvent to the component and all facets and children.
     * 
     * @param context
     * @param component
     */
    private static void _publishPostAddToViewEvent(FacesContext context, UIComponent component)
    {
        component.setInView(true);
        context.getApplication().publishEvent(context, PostAddToViewEvent.class, component.getClass(), component);
        
        if (component.getChildCount() > 0)
        {
            // PostAddToViewEvent could cause component relocation
            // (h:outputScript, h:outputStylesheet, composite:insertChildren, composite:insertFacet)
            // so we need to check if the component was relocated or not
          
            List<UIComponent> children = component.getChildren();
            for (int i = 0; i < children.size(); i++)
            {
                // spin on same index while component removed/replaced
                // to prevent skipping components:
                while (true)
                {
                    UIComponent child = children.get(i);
                    child.pushComponentToEL(context, child);
                    try
                    {
                        _publishPostAddToViewEvent(context, child);
                    }
                    finally
                    {
                        child.popComponentFromEL(context);
                    }
                    if (i < children.size() && children.get(i) != child)
                    {
                        continue;
                    }
                    break;
                }
            }
        }
        if (component.getFacetCount() > 0)
        {
            for (UIComponent child : component.getFacets().values())
            {
                child.pushComponentToEL(context, child);
                try
                {
                    _publishPostAddToViewEvent(context, child);
                }
                finally
                {
                    child.popComponentFromEL(context);
                }
            }
        }        
    }
    
    /**
     * Publish PreRemoveFromViewEvent to the component and all facets and children.
     * 
     * @param context
     * @param component
     */
    private static void _publishPreRemoveFromViewEvent(FacesContext context, UIComponent component)
    {
        component.setInView(false);
        context.getApplication().publishEvent(context, PreRemoveFromViewEvent.class, component.getClass(), component);
        
        if (component.getChildCount() > 0)
        {
            for (int i = 0, childCount = component.getChildCount(); i < childCount; i++)
            {
                UIComponent child = component.getChildren().get(i);
                _publishPreRemoveFromViewEvent(context, child);
            }
        }
        if (component.getFacetCount() > 0)
        {
            for (UIComponent child : component.getFacets().values())
            {
                _publishPreRemoveFromViewEvent(context, child);
            }
        }        
    }    
    
    private static void _updateInView(UIComponent component, boolean isInView)
    {
        component.setInView(isInView);
        
        if (component.getChildCount() > 0)
        {
            for (int i = 0, childCount = component.getChildCount(); i < childCount; i++)
            {
                UIComponent child = component.getChildren().get(i);
                _updateInView(child, isInView);
            }
        }
        if (component.getFacetCount() > 0)
        {
            for (UIComponent child : component.getFacets().values())
            {
                _updateInView(child, isInView);
            }
        }        
    }  
    
    /**
     * 
     * @param eventName
     * @param behavior
     * 
     * @since 2.0
     */
    public void addClientBehavior(String eventName, ClientBehavior behavior)
    {
        Collection<String> eventNames = getEventNames();
        
        if(eventNames == null)
        {
            //component didn't implement getEventNames properly
            //log an error and return
            if(log.isLoggable(Level.SEVERE))
            {
                log.severe("attempted to add a behavior to a component which did not properly "
                           + "implement getEventNames.  getEventNames must not return null");
                return;
            }
        }
        
        if(eventNames.contains(eventName))
        {
            if(_behaviorsMap == null)
            {
                _behaviorsMap = new HashMap<String,List<ClientBehavior>>();
            }
            
            List<ClientBehavior> behaviorsForEvent = _behaviorsMap.get(eventName);
            if(behaviorsForEvent == null)
            {
                // Normally have client only 1 client behaviour per event name,
                // so size 2 must be sufficient: 
                behaviorsForEvent = new _DeltaList<ClientBehavior>(2);
                _behaviorsMap.put(eventName, behaviorsForEvent);
            }
            
            behaviorsForEvent.add(behavior);
            _unmodifiableBehaviorsMap = null;
        }
    }

    /**
     * Invoke any listeners attached to this object which are listening for an event whose type matches the specified
     * event's runtime type.
     * <p>
     * This method does not propagate the event up to parent components, ie listeners attached to parent components
     * don't automatically get called.
     * <p>
     * If any of the listeners throws AbortProcessingException then that exception will prevent any further listener
     * callbacks from occurring, and the exception propagates out of this method without alteration.
     * <p>
     * ActionEvent events are typically queued by the renderer associated with this component in its decode method;
     * ValueChangeEvent events by the component's validate method. In either case the event's source property references
     * a component. At some later time the UIViewRoot component iterates over its queued events and invokes the
     * broadcast method on each event's source object.
     * 
     * @param event
     *            must not be null.
     */
    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException
    {
        if (event == null)
        {
            throw new NullPointerException("event");
        }
        
        if (event instanceof BehaviorEvent && event.getComponent() == this)
        {
            Behavior behavior = ((BehaviorEvent) event).getBehavior();
            behavior.broadcast((BehaviorEvent) event);
        }

        if (_facesListeners == null)
        {
            return;
        }
        // perf: _facesListeners is RandomAccess instance (javax.faces.component._DeltaList)
        for (int i = 0, size = _facesListeners.size(); i < size; i++)
        {
            FacesListener facesListener = _facesListeners.get(i);
            if (event.isAppropriateListener(facesListener))
            {
                event.processListener(facesListener);
            }
        }
    }
    
    public void clearInitialState()
    {
        super.clearInitialState();
        if (_facesListeners != null)
        {
            _facesListeners.clearInitialState();
        }
        if (_behaviorsMap != null)
        {
            for (Map.Entry<String, List<ClientBehavior> > entry : _behaviorsMap.entrySet())
            {
                ((PartialStateHolder) entry.getValue()).clearInitialState();
            }
        }
        if (_systemEventListenerClassMap != null)
        {
            for (Map.Entry<Class<? extends SystemEvent>, List<SystemEventListener>> entry : 
                _systemEventListenerClassMap.entrySet())
            {
                ((PartialStateHolder) entry.getValue()).clearInitialState();
            }
        }
        //_isRendererTypeSet = false;
        _capabilities &= ~(FLAG_IS_RENDERER_TYPE_SET);
    }

    /**
     * Check the submitted form parameters for data associated with this component. This default implementation
     * delegates to this component's renderer if there is one, and otherwise ignores the call.
     */
    @Override
    public void decode(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        
        setCachedRenderer(null);
        Renderer renderer = getRenderer(context);
        if (renderer != null)
        {
            setCachedRenderer(renderer);
            try
            {
                renderer.decode(context, this);
            }
            finally
            {
                setCachedRenderer(null);
            }
        }

    }

    public void encodeAll(FacesContext context) throws IOException
    {
        if (context == null)
        {
            throw new NullPointerException();
        }

        pushComponentToEL(context, this);
        try
        {
            setCachedIsRendered(null);
            boolean rendered;
            try
            {
                setCachedFacesContext(context);
                rendered = isRendered();
            }
            finally
            {
                setCachedFacesContext(null);
            } 
            setCachedIsRendered(rendered);
            if (!rendered)
            {
                setCachedIsRendered(null);
                return;
            }
            setCachedRenderer(null);
            setCachedRenderer(getRenderer(context));
        }
        finally
        {
            popComponentFromEL(context);
        }

        try
        {
            //if (isRendered()) {
            this.encodeBegin(context);

            // rendering children
            boolean rendersChildren;
            try
            {
                setCachedFacesContext(context);
                rendersChildren = this.getRendersChildren();
            }
            finally
            {
                setCachedFacesContext(null);
            }
            if (rendersChildren)
            {
                this.encodeChildren(context);
            } // let children render itself
            else
            {
                if (this.getChildCount() > 0)
                {
                    for (int i = 0; i < this.getChildCount(); i++)
                    {
                        UIComponent comp = this.getChildren().get(i);
                        comp.encodeAll(context);
                    }
                }
            }
            this.encodeEnd(context);
            //}
        }
        finally
        {
            setCachedIsRendered(null);
            setCachedRenderer(null);
        }
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        try
        {
            setCachedFacesContext(context);
            // Call UIComponent.pushComponentToEL(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
            pushComponentToEL(context, this);
    
            if (isRendered())
            {
                // If our rendered property is true, render the beginning of the current state of this
                // UIComponent to the response contained in the specified FacesContext.
    
                // Call Application.publishEvent(java.lang.Class, java.lang.Object), passing BeforeRenderEvent.class as
                // the first argument and the component instance to be rendered as the second argument.
    
                // The main issue we have here is that the listeners are normally just registered
                // to UIComponent, how do we deal with inherited ones?
                // We have to ask the EG
                context.getApplication().publishEvent(context,  PreRenderComponentEvent.class, UIComponent.class, this);
    
                Renderer renderer = getRenderer(context);
                if (renderer != null)
                {
                    // If a Renderer is associated with this UIComponent, the actual encoding will be delegated to
                    // Renderer.encodeBegin(FacesContext, UIComponent).
                    renderer.encodeBegin(context, this);
                }
            }
        }
        finally
        {
            setCachedFacesContext(null);
        }
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        boolean isCachedFacesContext = isCachedFacesContext();
        try
        {
            if (!isCachedFacesContext)
            {
                setCachedFacesContext(context);
            }
            if (isRendered())
            {
                // If our rendered property is true, render the child UIComponents of this UIComponent.
    
                Renderer renderer = getRenderer(context);
                if (renderer == null)
                {
                    // If no Renderer is associated with this UIComponent, iterate over each of the children of this
                    // component and call UIComponent.encodeAll(javax.faces.context.FacesContext).
                    if (getChildCount() > 0)
                    {
                        for (int i = 0, childCount = getChildCount(); i < childCount; i++)
                        {
                            UIComponent child = getChildren().get(i);
                            child.encodeAll(context);
                        }
                    }
                }
                else
                {
                    // If a Renderer is associated with this UIComponent, the actual encoding will be delegated to
                    // Renderer.encodeChildren(FacesContext, UIComponent).
                    renderer.encodeChildren(context, this);
                }
            }
        }
        finally
        {
            if (!isCachedFacesContext)
            {
                setCachedFacesContext(null);
            }
        }
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        try
        {
            setCachedFacesContext(context);
            if (isRendered())
            {
                // If our rendered property is true, render the ending of the current state of this UIComponent.
                Renderer renderer = getRenderer(context);
                if (renderer != null)
                {
                    // If a Renderer is associated with this UIComponent, the actual encoding will be delegated to
                    // Renderer.encodeEnd(FacesContext, UIComponent).
                    renderer.encodeEnd(context, this);
                }
            }
        }
        finally
        {
            // Call UIComponent.popComponentFromEL(javax.faces.context.FacesContext). before returning regardless
            // of the value of the rendered property.
            popComponentFromEL(context);
            setCachedFacesContext(null);
        }
    }
    
    /**
     * Standard method for finding other components by id, inherited by most UIComponent objects.
     * <p>
     * The lookup is performed in a manner similar to finding a file in a filesystem; there is a "base" at which to
     * start, and the id can be for something in the "local directory", or can include a relative path. Here,
     * NamingContainer components fill the role of directories, and ":" is the "path separator". Note, however, that
     * although components have a strict parent/child hierarchy, component ids are only prefixed ("namespaced") with the
     * id of their parent when the parent is a NamingContainer.
     * <p>
     * The base node at which the search starts is determined as follows:
     * <ul>
     * <li>When expr starts with ':', the search starts with the root component of the tree that this component is in
     * (ie the ancestor whose parent is null).
     * <li>Otherwise, if this component is a NamingContainer then the search starts with this component.
     * <li>Otherwise, the search starts from the nearest ancestor NamingContainer (or the root component if there is no
     * NamingContainer ancestor).
     * </ul>
     * 
     * @param expr
     *            is of form "id1:id2:id3".
     * @return UIComponent or null if no component with the specified id is found.
     */

    @Override
    public UIComponent findComponent(String expr)
    {
        if (expr == null)
        {
            throw new NullPointerException("expr");
        }
        if (expr.length() == 0)
        {
            return null;
        }

        char separatorChar = getFacesContext().getNamingContainerSeparatorChar();
        UIComponent findBase;
        if (expr.charAt(0) == separatorChar)
        {
            findBase = _ComponentUtils.getRootComponent(this);
            expr = expr.substring(1);
        }
        else
        {
            if (this instanceof NamingContainer)
            {
                findBase = this;
            }
            else
            {
                findBase = _ComponentUtils.findParentNamingContainer(this, true /* root if not found */);
            }
        }

        int separator = expr.indexOf(separatorChar);
        if (separator == -1)
        {
            return _ComponentUtils.findComponent(findBase, expr, separatorChar);
        }

        String id = expr.substring(0, separator);
        findBase = _ComponentUtils.findComponent(findBase, id, separatorChar);
        if (findBase == null)
        {
            return null;
        }

        if (!(findBase instanceof NamingContainer))
        {
            throw new IllegalArgumentException("Intermediate identifier " + id + " in search expression " + expr
                    + " identifies a UIComponent that is not a NamingContainer");
        }

        return findBase.findComponent(expr.substring(separator + 1));

    }

    /**
     * Get a map through which all the UIComponent's properties, value-bindings and non-property attributes can be read
     * and written.
     * <p>
     * When writing to the returned map:
     * <ul>
     * <li>If this component has an explicit property for the specified key then the setter method is called. An
     * IllegalArgumentException is thrown if the property is read-only. If the property is readable then the old value
     * is returned, otherwise null is returned.
     * <li>Otherwise the key/value pair is stored in a map associated with the component.
     * </ul>
     * Note that value-bindings are <i>not</i> written by put calls to this map. Writing to the attributes map using a
     * key for which a value-binding exists will just store the value in the attributes map rather than evaluating the
     * binding, effectively "hiding" the value-binding from later attributes.get calls. Setter methods on components
     * commonly do <i>not</i> evaluate a binding of the same name; they just store the provided value directly on the
     * component.
     * <p>
     * When reading from the returned map:
     * <ul>
     * <li>If this component has an explicit property for the specified key then the getter method is called. If the
     * property exists, but is read-only (ie only a setter method is defined) then an IllegalArgumentException is
     * thrown.
     * <li>If the attribute map associated with the component has an entry with the specified key, then that is
     * returned.
     * <li>If this component has a value-binding for the specified key, then the value-binding is evaluated to fetch the
     * value.
     * <li>Otherwise, null is returned.
     * </ul>
     * Note that components commonly define getter methods such that they evaluate a value-binding of the same name if
     * there isn't yet a local property.
     * <p>
     * Assigning values to the map which are not explicit properties on the underlying component can be used to "tunnel"
     * attributes from the JSP tag (or view-specific equivalent) to the associated renderer without modifying the
     * component itself.
     * <p>
     * Any value-bindings and non-property attributes stored in this map are automatically serialized along with the
     * component when the view is serialized.
     */
    @Override
    public Map<String, Object> getAttributes()
    {
        if (_attributesMap == null)
        {
            _attributesMap = new _ComponentAttributesMap(this);
        }

        return _attributesMap;
    }

    @Override
    public Map<String, Object> getPassThroughAttributes(boolean create)
    {
        // Take into account the param "create" in MyFaces case does not have
        // sense at all
        if (_passthroughAttributesMap == null)
        {
            if (!create)
            {
                if ((_capabilities & FLAG_PASSTHROUGH_ATTRIBUTE_MAP_SET) != 0)
                {
                    // Was already created, return wrapper
                    _passthroughAttributesMap = new _PassThroughAttributesMap(this);
                }
            }
            else
            {
                _passthroughAttributesMap = new _PassThroughAttributesMap(this);
                _capabilities |= FLAG_PASSTHROUGH_ATTRIBUTE_MAP_SET;
            }
        }
        return _passthroughAttributesMap;
    }

    /**
     * Return the number of direct child components this component has.
     * <p>
     * Identical to getChildren().size() except that when this component has no children this method will not force an
     * empty list to be created.
     */
    @Override
    public int getChildCount()
    {
        return _childrenList == null ? 0 : _childrenList.size();
    }

    /**
     * Return a list of the UIComponent objects which are direct children of this component.
     * <p>
     * The list object returned has some non-standard behaviour:
     * <ul>
     * <li>The list is type-checked; only UIComponent objects can be added.
     * <li>If a component is added to the list with an id which is the same as some other component in the list then an
     * exception is thrown. However multiple components with a null id may be added.
     * <li>The component's parent property is set to this component. If the component already had a parent, then the
     * component is first removed from its original parent's child list.
     * </ul>
     */
    @Override
    public List<UIComponent> getChildren()
    {
        if (_childrenList == null)
        {
            _childrenList = new _ComponentChildrenList(this);
        }
        return _childrenList;
    }
    
    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public Map<String,List<ClientBehavior>> getClientBehaviors()
    {
        if(_behaviorsMap == null)
        {
            return Collections.emptyMap();
        }

        return wrapBehaviorsMap();
    }

    /**
     * Get a string which can be output to the response which uniquely identifies this UIComponent within the current
     * view.
     * <p>
     * The component should have an id attribute already assigned to it; however if the id property is currently null
     * then a unique id is generated and set for this component. This only happens when components are programmatically
     * created without ids, as components created by a ViewHandler should be assigned ids when they are created.
     * <p>
     * If this component is a descendant of a NamingContainer then the client id is of form
     * "{namingContainerId}:{componentId}". Note that the naming container's id may itself be of compound form if it has
     * an ancestor naming container. Note also that this only applies to naming containers; other UIComponent types in
     * the component's ancestry do not affect the clientId.
     * <p>
     * Finally the renderer associated with this component is asked to convert the id into a suitable form. This allows
     * escaping of any characters in the clientId which are significant for the markup language generated by that
     * renderer.
     */
    @Override
    public String getClientId(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        if (_clientId != null)
        {
            return _clientId;
        }

        //boolean idWasNull = false;
        String id = getId();
        if (id == null)
        {
            // Although this is an error prone side effect, we automatically create a new id
            // just to be compatible to the RI
            
            // The documentation of UniqueIdVendor says that this interface should be implemented by
            // components that also implements NamingContainer. The only component that does not implement
            // NamingContainer but UniqueIdVendor is UIViewRoot. Anyway we just can't be 100% sure about this
            // fact, so it is better to scan for the closest UniqueIdVendor. If it is not found use 
            // viewRoot.createUniqueId, otherwise use UniqueIdVendor.createUniqueId(context,seed).
            UniqueIdVendor parentUniqueIdVendor = _ComponentUtils.findParentUniqueIdVendor(this);
            if (parentUniqueIdVendor == null)
            {
                UIViewRoot viewRoot = context.getViewRoot();
                if (viewRoot != null)
                {
                    id = viewRoot.createUniqueId();
                }
                else
                {
                    // The RI throws a NPE
                    String location = getComponentLocation(this);
                    throw new FacesException("Cannot create clientId. No id is assigned for component"
                            + " to create an id and UIViewRoot is not defined: "
                            + getPathToComponent(this)
                            + (location != null ? " created from: " + location : ""));
                }
            }
            else
            {
                id = parentUniqueIdVendor.createUniqueId(context, null);
            }
            setId(id);
            // We remember that the id was null and log a warning down below
            // idWasNull = true;
        }

        UIComponent namingContainer = _ComponentUtils.findParentNamingContainer(this, false);
        if (namingContainer != null)
        {
            String containerClientId = namingContainer.getContainerClientId(context);
            if (containerClientId != null)
            {
                StringBuilder bld = _getSharedStringBuilder(context);
                _clientId = bld.append(containerClientId).append(
                                      context.getNamingContainerSeparatorChar()).append(id).toString();
            }
            else
            {
                _clientId = id;
            }
        }
        else
        {
            _clientId = id;
        }

        Renderer renderer = getRenderer(context);
        if (renderer != null)
        {
            _clientId = renderer.convertClientId(context, _clientId);
        }

        // -=Leonardo Uribe=- In jsf 1.1 and 1.2 this warning has sense, but in jsf 2.0 it is common to have
        // components without any explicit id (UIViewParameter components and UIOuput resource components) instances.
        // So, this warning is becoming obsolete in this new context and should be removed.
        //if (idWasNull && log.isLoggable(Level.WARNING))
        //{
        //    log.warning("WARNING: Component " + _clientId
        //            + " just got an automatic id, because there was no id assigned yet. "
        //            + "If this component was created dynamically (i.e. not by a JSP tag) you should assign it an "
        //            + "explicit static id or assign it the id you get from "
        //            + "the createUniqueId from the current UIViewRoot "
        //            + "component right after creation! Path to Component: " + getPathToComponent(this));
        //}

        return _clientId;
    }
    
    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public String getDefaultEventName()
    {
        // if a default event exists for a component, this method is overriden thus assume null
        return null;
    }
    
    /**
     * 
     * @return
     * 
     * @since 2.0
     */
    public Collection<String> getEventNames()
    {
        // must be specified by the implementing component.
        // Returning null will force an error message in addClientBehavior.
        return null;
    }

    @Override
    public UIComponent getFacet(String name)
    {
        return _facetMap == null ? null : _facetMap.get(name);
    }

    /**
     * @since 1.2
     */
    @Override
    public int getFacetCount()
    {
        return _facetMap == null ? 0 : _facetMap.size();
    }

    @Override
    public Map<String, UIComponent> getFacets()
    {
        if (_facetMap == null)
        {
            _facetMap = new _ComponentFacetMap<UIComponent>(this);
        }
        return _facetMap;
    }

    @Override
    public Iterator<UIComponent> getFacetsAndChildren()
    {
        // we can't use _facetMap and _childrenList here directly,
        // because some component implementation could keep their 
        // own properties for facets and children and just override
        // getFacets() and getChildren() (e.g. seen in PrimeFaces).
        // See MYFACES-2611 for details.
        if (getFacetCount() == 0)
        {
            if (getChildCount() == 0)
            {
                return _EMPTY_UICOMPONENT_ITERATOR;
            }

            return getChildren().iterator();
        }
        else
        {
            if (getChildCount() == 0)
            {
                return getFacets().values().iterator();
            }

            return new _FacetsAndChildrenIterator(getFacets(), getChildren());
        }
    }

    /**
     * Get a string which uniquely identifies this UIComponent within the scope of the nearest ancestor NamingContainer
     * component. The id is not necessarily unique across all components in the current view.
     */
    @JSFProperty(rtexprvalue = true)
    public String getId()
    {
        return _id;
    }

    @Override
    public UIComponent getParent()
    {
        return _parent;
    }

    @Override
    public String getRendererType()
    {
        // rendererType is literal-only, no ValueExpression - MYFACES-3136:
        // Even if this is true, according to JSF spec section 8 Rendering Model,
        // this part is essential to implement "delegated implementation" pattern,
        // so we can't do this optimization here. Instead, JSF developers could prevent
        // this evaluation overriding this method directly.
        if (_rendererType != null)
        {
            return _rendererType;
        }
        ValueExpression expression = getValueExpression("rendererType");
        if (expression != null)
        {
            return (String) expression.getValue(getFacesContext().getELContext());
        }
        return null;
    }

    /**
     * Indicates whether this component or its renderer manages the invocation of the rendering methods of its child
     * components. When this is true:
     * <ul>
     * <li>This component's encodeBegin method will only be called after all the child components have been created and
     * added to this component. <li>This component's encodeChildren method will be called after its encodeBegin method.
     * Components for which this method returns false do not get this method invoked at all. <li>No rendering methods
     * will be called automatically on child components; this component is required to invoke the
     * encodeBegin/encodeEnd/etc on them itself.
     * </ul>
     */
    @Override
    public boolean getRendersChildren()
    {
        Renderer renderer = getRenderer(getFacesContext());
        return renderer != null ? renderer.getRendersChildren() : false;
    }

    /**
     * Get the named value-binding associated with this component.
     * <p>
     * Value-bindings are stored in a map associated with the component, though there is commonly a property
     * (setter/getter methods) of the same name defined on the component itself which evaluates the value-binding when
     * called.
     * 
     * @deprecated Replaced by getValueExpression
     */
    @Override
    public ValueBinding getValueBinding(String name)
    {
        ValueExpression expression = getValueExpression(name);
        if (expression != null)
        {
            if (expression instanceof _ValueBindingToValueExpression)
            {
                return ((_ValueBindingToValueExpression) expression).getValueBinding();
            }
            return new _ValueExpressionToValueBinding(expression);
        }
        return null;
    }
    
    /**
     * <code>invokeOnComponent</code> must be implemented in <code>UIComponentBase</code> too...
     */
    @Override
    public boolean invokeOnComponent(FacesContext context, String clientId, ContextCallback callback)
            throws FacesException
    {
        if (isCachedFacesContext())
        {
            return super.invokeOnComponent(context, clientId, callback);
        }
        else
        {
            try
            {
                setCachedFacesContext(context);
                return super.invokeOnComponent(context, clientId, callback);
            }
            finally
            {
                setCachedFacesContext(null);
            }
        }
    }

    @Override
    public boolean visitTree(VisitContext context, VisitCallback callback)
    {
        if (isCachedFacesContext())
        {
            return super.visitTree(context, callback);
        }
        else
        {
            try
            {
                setCachedFacesContext(context.getFacesContext());
                return super.visitTree(context, callback);
            }
            finally
            {
                setCachedFacesContext(null);
            }
        }
    }

    /**
     * A boolean value that indicates whether this component should be rendered. Default value: true.
     **/
    @Override
    @JSFProperty
    public boolean isRendered()
    {
        if (_cachedIsRendered != null)
        {
            return Boolean.TRUE.equals(_cachedIsRendered);
        }
        return (Boolean) getStateHelper().eval(PropertyKeys.rendered, DEFAULT_RENDERED);
    }

    @JSFProperty(literalOnly = true, istransient = true, tagExcluded = true)
    public boolean isTransient()
    {
        return _transient;
    }
    
    public void markInitialState()
    {
        super.markInitialState();
        
        // Enable copyFullInitialState behavior when delta is written into this component.
        ((_DeltaStateHelper)getStateHelper()).setCopyFullInitialState(true);
        
        if (_facesListeners != null)
        {
            _facesListeners.markInitialState();
        }
        if (_behaviorsMap != null)
        {
            for (Map.Entry<String, List<ClientBehavior> > entry : _behaviorsMap.entrySet())
            {
                ((PartialStateHolder) entry.getValue()).markInitialState();
            }
        }
        if (_systemEventListenerClassMap != null)
        {
            for (Map.Entry<Class<? extends SystemEvent>, List<SystemEventListener>> entry : 
                _systemEventListenerClassMap.entrySet())
            {
                ((PartialStateHolder) entry.getValue()).markInitialState();
            }
        }
    }

    @Override
    protected void addFacesListener(FacesListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("listener");
        }
        if (_facesListeners == null)
        {
            // How many facesListeners have single component normally? 
            _facesListeners = new _DeltaList<FacesListener>(5);
        }
        _facesListeners.add(listener);
    }

    @Override
    protected FacesContext getFacesContext()
    {
        if (_facesContext == null)
        {
            return FacesContext.getCurrentInstance();
        }
        else
        {
            return _facesContext;
        }
    }

    // FIXME: Notify EG for generic usage
    @Override
    protected FacesListener[] getFacesListeners(Class clazz)
    {
        if (clazz == null)
        {
            throw new NullPointerException("Class is null");
        }
        if (!FacesListener.class.isAssignableFrom(clazz))
        {
            throw new IllegalArgumentException("Class " + clazz.getName() + " must implement " + FacesListener.class);
        }

        if (_facesListeners == null)
        {
            return (FacesListener[]) Array.newInstance(clazz, 0);
        }
        List<FacesListener> lst = null;
        // perf: _facesListeners is RandomAccess instance (javax.faces.component._DeltaList)
        for (int i = 0, size = _facesListeners.size(); i < size; i++)
        {
            FacesListener facesListener = _facesListeners.get(i);
            if (facesListener != null && clazz.isAssignableFrom(facesListener.getClass()))
            {
                if (lst == null)
                {
                    lst = new ArrayList<FacesListener>();
                }
                lst.add(facesListener);
            }
        }
        if (lst == null)
        {
            return (FacesListener[]) Array.newInstance(clazz, 0);
        }

        return lst.toArray((FacesListener[]) Array.newInstance(clazz, lst.size()));
    }

    @Override
    protected Renderer getRenderer(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        Renderer renderer = getCachedRenderer();
        if (renderer != null)
        {
            return renderer;
        }
        String rendererType = getRendererType();
        if (rendererType == null)
        {
            return null;
        }
        
        RenderKit renderKit = context.getRenderKit();
        renderer = renderKit.getRenderer(getFamily(), rendererType);
        if (renderer == null)
        {
            String location = getComponentLocation(this);
            String logStr = "No Renderer found for component " + getPathToComponent(this)
                    + " (component-family=" + getFamily()
                    + ", renderer-type=" + rendererType + ")"
                    + (location != null ? " created from: " + location : "");
            
            getFacesContext().getExternalContext().log(logStr);
            log.warning(logStr);
        }
        return renderer;
    }

    @Override
    protected void removeFacesListener(FacesListener listener)
    {
        if (listener == null)
        {
            throw new NullPointerException("listener is null");
        }

        if (_facesListeners != null)
        {
            _facesListeners.remove(listener);
        }
    }

    @Override
    public void queueEvent(FacesEvent event)
    {
        if (event == null)
        {
            throw new NullPointerException("event");
        }
        UIComponent parent = getParent();
        if (parent == null)
        {
            throw new IllegalStateException("component is not a descendant of a UIViewRoot");
        }
        parent.queueEvent(event);
    }

    @Override
    public void processDecodes(FacesContext context)
    {
        try
        {
            setCachedFacesContext(context);
            // Call UIComponent.pushComponentToEL(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
            pushComponentToEL(context, this);
            if (_isPhaseExecutable(context))
            {
                // Call the processDecodes() method of all facets and children of this UIComponent, in the order
                // determined by a call to getFacetsAndChildren().
                int facetCount = getFacetCount();
                if (facetCount > 0)
                {
                    for (UIComponent facet : getFacets().values())
                    {
                        facet.processDecodes(context);
                    }
                }
                for (int i = 0, childCount = getChildCount(); i < childCount; i++)
                {
                    UIComponent child = getChildren().get(i);
                    child.processDecodes(context);
                }

                try
                {
                    // Call the decode() method of this component.
                    decode(context);
                }
                catch (RuntimeException e)
                {
                    // If a RuntimeException is thrown during decode processing, call FacesContext.renderResponse()
                    // and re-throw the exception.
                    context.renderResponse();
                    throw e;
                }
            }
        }
        finally
        {
            // Call UIComponent.popComponentFromEL(javax.faces.context.FacesContext) from inside of a finally
            // block, just before returning.

            popComponentFromEL(context);
            setCachedFacesContext(null);
        }
    }

    @Override
    public void processValidators(FacesContext context)
    {
        try
        {
            setCachedFacesContext(context);
            // Call UIComponent.pushComponentToEL(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
            pushComponentToEL(context, this);
            if (_isPhaseExecutable(context))
            {
                //Pre validation event dispatch for component
                context.getApplication().publishEvent(context,  PreValidateEvent.class, getClass(), this);
                
                try
                {
                    // Call the processValidators() method of all facets and children of this UIComponent, in the order
                    // determined by a call to getFacetsAndChildren().
                    int facetCount = getFacetCount();
                    if (facetCount > 0)
                    {
                        for (UIComponent facet : getFacets().values())
                        {
                            facet.processValidators(context);
                        }
                    }
    
                    for (int i = 0, childCount = getChildCount(); i < childCount; i++)
                    {
                        UIComponent child = getChildren().get(i);
                        child.processValidators(context);
                    }
                }
                finally
                {
                    context.getApplication().publishEvent(context,  PostValidateEvent.class, getClass(), this);
                }
            }
        }
        finally
        {
            popComponentFromEL(context);
            setCachedFacesContext(null);
        }
    }

    /**
     * This isn't an input component, so just pass on the processUpdates call to child components and facets that might
     * be input components.
     * <p>
     * Components that were never rendered can't possibly be receiving update data (no corresponding fields were ever
     * put into the response) so if this component is not rendered then this method does not invoke processUpdates on
     * its children.
     */
    @Override
    public void processUpdates(FacesContext context)
    {
        try
        {
            setCachedFacesContext(context);
            // Call UIComponent.pushComponentToEL(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
            pushComponentToEL(context, this);
            if (_isPhaseExecutable(context))
            {
                // Call the processUpdates() method of all facets and children of this UIComponent, in the order
                // determined by a call to getFacetsAndChildren().
                int facetCount = getFacetCount();
                if (facetCount > 0)
                {
                    for (UIComponent facet : getFacets().values())
                    {
                        facet.processUpdates(context);
                    }
                }

                for (int i = 0, childCount = getChildCount(); i < childCount; i++)
                {
                    UIComponent child = getChildren().get(i);
                    child.processUpdates(context);
                }
            }
        }
        finally
        {
            // After returning from the processUpdates() method on a child or facet, call
            // UIComponent.popComponentFromEL(javax.faces.context.FacesContext)
            popComponentFromEL(context);
            
            setCachedFacesContext(null);
        }
    }

    @Override
    public Object processSaveState(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        if (isTransient())
        {
            // consult the transient property of this component. If true, just return null.
            return null;
        }

        // Call UIComponent.pushComponentToEL(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
        pushComponentToEL(context, this);

        Map<String, Object> facetMap;

        List<Object> childrenList;
        
        Object savedState;
        try
        {
            facetMap = null;
            int facetCount = getFacetCount();
            if (facetCount > 0)
            {
                // Call the processSaveState() method of all facets and children of this UIComponent in the order
                // determined by a call to getFacetsAndChildren(), skipping children and facets that are transient.

                // To improve speed and robustness, the facets and children processing is splited to maintain the
                // facet --> state coherence based on the facet's name
                for (Map.Entry<String, UIComponent> entry : getFacets().entrySet())
                {
                    UIComponent component = entry.getValue();
                    if (!component.isTransient())
                    {
                        if (facetMap == null)
                        {
                            facetMap = new HashMap<String, Object>(facetCount, 1);
                        }

                        facetMap.put(entry.getKey(), component.processSaveState(context));

                        // Ensure that UIComponent.popComponentFromEL(javax.faces.context.FacesContext) is called
                        // correctly after each child or facet.
                        // popComponentFromEL(context);
                    }
                }
            }
            childrenList = null;
            int childCount = getChildCount();
            if (childCount > 0)
            {
                // Call the processSaveState() method of all facets and children of this UIComponent in the order
                // determined by a call to getFacetsAndChildren(), skipping children and facets that are transient.

                // To improve speed and robustness, the facets and children processing is splited to maintain the
                // facet --> state coherence based on the facet's name
                for (int i = 0; i < childCount; i++)
                {
                    UIComponent child = getChildren().get(i);
                    if (!child.isTransient())
                    {
                        if (childrenList == null)
                        {
                            childrenList = new ArrayList<Object>(childCount);
                        }

                        Object childState = child.processSaveState(context);
                        if (childState != null)
                        { // FIXME: Isn't that check dangerous for restoration since the child isn't marked transient?
                            childrenList.add(childState);
                        }

                        // Ensure that UIComponent.popComponentFromEL(javax.faces.context.FacesContext) is called
                        // correctly after each child or facet.
                    }
                }
            }
            
            // Call the saveState() method of this component.
            savedState = saveState(context);
        }
        finally
        {
            popComponentFromEL(context);
        }

        // Encapsulate the child state and your state into a Serializable Object and return it.
        return new Object[] { savedState, facetMap, childrenList };
    }

    @SuppressWarnings("unchecked")
    @Override
    public void processRestoreState(FacesContext context, Object state)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        Object[] stateValues = (Object[]) state;

        try
        {
            // Call UIComponent.pushComponentToEL(javax.faces.context.FacesContext, javax.faces.component.UIComponent)
            pushComponentToEL(context, this);

            // Call the restoreState() method of this component.
            restoreState(context, stateValues[0]);
            
            Map<String, Object> facetMap = (Map<String, Object>) stateValues[1];
            if (facetMap != null && getFacetCount() > 0)
            {
                // Call the processRestoreState() method of all facets and children of this UIComponent in the order
                // determined by a call to getFacetsAndChildren().

                // To improve speed and robustness, the facets and children processing is splited to maintain the
                // facet --> state coherence based on the facet's name
                for (Map.Entry<String, UIComponent> entry : getFacets().entrySet())
                {
                    Object facetState = facetMap.get(entry.getKey());
                    if (facetState != null)
                    {
                        entry.getValue().processRestoreState(context, facetState);

                        // After returning from the processRestoreState() method on a child or facet, call
                        // UIComponent.popComponentFromEL(javax.faces.context.FacesContext)
                        // popComponentFromEL(context);
                    }
                    else
                    {
                        context.getExternalContext().log("No state found to restore facet " + entry.getKey());
                    }
                }
            }
            List<Object> childrenList = (List<Object>) stateValues[2];
            if (childrenList != null && getChildCount() > 0)
            {
                // Call the processRestoreState() method of all facets and children of this UIComponent in the order
                // determined by a call to getFacetsAndChildren().

                // To improve speed and robustness, the facets and children processing is splited to maintain the
                // facet --> state coherence based on the facet's name
                int idx = 0;
                for (int i = 0, childCount = getChildCount(); i < childCount; i++)
                {
                    UIComponent child = getChildren().get(i);
                    if (!child.isTransient())
                    {
                        Object childState = childrenList.get(idx++);
                        if (childState != null)
                        {
                            child.processRestoreState(context, childState);

                            // After returning from the processRestoreState() method on a child or facet, call
                            // UIComponent.popComponentFromEL(javax.faces.context.FacesContext)
                            // popComponentFromEL(context);
                        }
                        else
                        {
                            context.getExternalContext().log("No state found to restore child of component " + getId());
                        }
                    }
                }
            }
        }
        finally
        {
            popComponentFromEL(context);
        }
    }
    
    /**
     * Gets the Location of the given UIComponent from its attribute map.
     * @param component
     * @return
     */
    private String getComponentLocation(UIComponent component)
    {
        Location location = (Location) component.getAttributes()
                .get(UIComponent.VIEW_LOCATION_KEY);
        if (location != null)
        {
            return location.toString();
        }
        return null;
    }

    private String getPathToComponent(UIComponent component)
    {
        StringBuffer buf = new StringBuffer();

        if (component == null)
        {
            buf.append("{Component-Path : ");
            buf.append("[null]}");
            return buf.toString();
        }

        getPathToComponent(component, buf);

        buf.insert(0, "{Component-Path : ");
        buf.append("}");

        return buf.toString();
    }

    private void getPathToComponent(UIComponent component, StringBuffer buf)
    {
        if (component == null)
        {
            return;
        }

        StringBuffer intBuf = new StringBuffer();

        intBuf.append("[Class: ");
        intBuf.append(component.getClass().getName());
        if (component instanceof UIViewRoot)
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

        buf.insert(0, intBuf.toString());

        getPathToComponent(component.getParent(), buf);
    }

    public void setTransient(boolean transientFlag)
    {
        _transient = transientFlag;
    }

    /**
     * Serializes objects which are "attached" to this component but which are not UIComponent children of it. Examples
     * are validator and listener objects. To be precise, it returns an object which implements java.io.Serializable,
     * and which when serialized will persist the state of the provided object.
     * <p>
     * If the attachedObject is a List then every object in the list is saved via a call to this method, and the
     * returned wrapper object contains a List object.
     * <p>
     * If the object implements StateHolder then the object's saveState is called immediately, and a wrapper is returned
     * which contains both this saved state and the original class name. However in the case where the
     * StateHolder.isTransient method returns true, null is returned instead.
     * <p>
     * If the object implements java.io.Serializable then the object is simply returned immediately; standard java
     * serialization will later be used to store this object.
     * <p>
     * In all other cases, a wrapper is returned which simply stores the type of the provided object. When deserialized,
     * a default instance of that type will be recreated.
     */
    public static Object saveAttachedState(FacesContext context, Object attachedObject)
    {
        if (context == null)
        {
            throw new NullPointerException ("context");
        }
        
        if (attachedObject == null)
        {
            return null;
        }
        // StateHolder interface should take precedence over
        // List children
        if (attachedObject instanceof StateHolder)
        {
            StateHolder holder = (StateHolder) attachedObject;
            if (holder.isTransient())
            {
                return null;
            }

            return new _AttachedStateWrapper(attachedObject.getClass(), holder.saveState(context));
        }
        else if (attachedObject instanceof Collection)
        {
            if (ArrayList.class.equals(attachedObject.getClass()))
            {
                ArrayList<?> list = (ArrayList<?>) attachedObject;
                int size = list.size();
                List<Object> lst = new ArrayList<Object>(size);
                for (int i = 0; i < size; i++)
                {
                    Object item = list.get(i);
                    if (item != null)
                    {
                        lst.add(saveAttachedState(context, item));
                    }
                }
                return new _AttachedListStateWrapper(lst);
            }
            else
            {
                List<Object> lst = new ArrayList<Object>(((Collection<?>) attachedObject).size());
                for (Object item : (Collection<?>) attachedObject)
                {
                    if (item != null)
                    {
                        lst.add(saveAttachedState(context, item));
                    }
                }
                return new _AttachedCollectionStateWrapper(attachedObject.getClass(), lst);
            }
        }
        else if (attachedObject instanceof Serializable)
        {
            return attachedObject;
        }
        else
        {
            return new _AttachedStateWrapper(attachedObject.getClass(), null);
        }
    }

    public static Object restoreAttachedState(FacesContext context, Object stateObj) throws IllegalStateException
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }
        if (stateObj == null)
        {
            return null;
        }
        if (stateObj instanceof _AttachedListStateWrapper)
        {
            // perf: getWrappedStateList in _AttachedListStateWrapper is always ArrayList: see saveAttachedState
            ArrayList<Object> lst = (ArrayList<Object>) ((_AttachedListStateWrapper) stateObj).getWrappedStateList();
            List<Object> restoredList = new ArrayList<Object>(lst.size());
            for (int i = 0, size = lst.size(); i < size; i++)
            {
                Object item = lst.get(i);
                restoredList.add(restoreAttachedState(context, item));
            }
            return restoredList;
        }
        else if (stateObj instanceof _AttachedCollectionStateWrapper)
        {
            _AttachedCollectionStateWrapper wrappedState = (_AttachedCollectionStateWrapper) stateObj; 
            Class<?> clazz = wrappedState.getClazz();
            List<Object> lst = wrappedState.getWrappedStateList();
            Collection restoredList;
            try
            {
                restoredList = (Collection) clazz.newInstance();
            }
            catch (InstantiationException e)
            {
                throw new RuntimeException("Could not restore StateHolder of type " + clazz.getName()
                        + " (missing no-args constructor?)", e);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }

            for (Object item : lst)
            {
                restoredList.add(restoreAttachedState(context, item));
            }
            return restoredList;

        }
        else if (stateObj instanceof _AttachedStateWrapper)
        {
            Class<?> clazz = ((_AttachedStateWrapper) stateObj).getClazz();
            Object restoredObject;
            try
            {
                restoredObject = clazz.newInstance();
            }
            catch (InstantiationException e)
            {
                throw new RuntimeException("Could not restore StateHolder of type " + clazz.getName()
                        + " (missing no-args constructor?)", e);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
            if (restoredObject instanceof StateHolder)
            {
                _AttachedStateWrapper wrapper = (_AttachedStateWrapper) stateObj;
                Object wrappedState = wrapper.getWrappedStateObject();

                StateHolder holder = (StateHolder) restoredObject;
                holder.restoreState(context, wrappedState);
            }
            return restoredObject;
        }
        else
        {
            return stateObj;
        }
    }
    
    private static final int FULL_STATE_ARRAY_SIZE = 10;

    /**
     * Invoked after the render phase has completed, this method returns an object which can be passed to the
     * restoreState of some other instance of UIComponentBase to reset that object's state to the same values as this
     * object currently has.
     */
    public Object saveState(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException ("context");
        }
        
        if (context.getViewRoot() != null)
        {
            if (context.getViewRoot().getResetSaveStateMode() == RESET_MODE_SOFT)
            {
                // Force FacesContext cleanup to prevent leak it.
                setCachedFacesContext(null);
                // Reset state to recalculate state first.
                StateHelper stateHelper = getStateHelper(false);
                if (stateHelper != null)
                {
                    ((_DeltaStateHelper)stateHelper).resetSoftState(context);
                }
            }
            if (context.getViewRoot().getResetSaveStateMode() == RESET_MODE_HARD)
            {
                // Force FacesContext cleanup to prevent leak it.
                setCachedFacesContext(null);
                // Reset state to recalculate state first.
                StateHelper stateHelper = getStateHelper(false);
                if (stateHelper != null)
                {
                    ((_DeltaStateHelper)stateHelper).resetHardState(context);
                }
            }
        }
        if (initialStateMarked())
        {
            //Delta
            //_id and _clientId was already restored from template
            //and never changes during component life.
            Object facesListenersSaved = saveFacesListenersList(context);
            Object behaviorsMapSaved = saveBehaviorsMap(context);
            Object systemEventListenerClassMapSaved = saveSystemEventListenerClassMap(context);
            Object stateHelperSaved = null;
            StateHelper stateHelper = getStateHelper(false);
            if (stateHelper != null)
            {
                stateHelperSaved = stateHelper.saveState(context);
            }
            
            if (facesListenersSaved == null && stateHelperSaved == null && 
                behaviorsMapSaved == null && systemEventListenerClassMapSaved == null &&
               !((_capabilities & FLAG_IS_RENDERER_TYPE_SET) != 0))
            {
                return null;
            }
            
            Object transientState = null;
            if (!context.getCurrentPhaseId().equals(PhaseId.RENDER_RESPONSE))
            {
                transientState = saveTransientState(context);
            }
            
            if (transientState != null)
            {
                if ((_capabilities & FLAG_IS_RENDERER_TYPE_SET) != 0)
                {
                    return new Object[] {facesListenersSaved, stateHelperSaved, behaviorsMapSaved,
                                        systemEventListenerClassMapSaved, transientState,
                                        _rendererType};
                }
                else
                {
                    return new Object[] {facesListenersSaved, stateHelperSaved, behaviorsMapSaved,
                        systemEventListenerClassMapSaved, transientState};
                }
            }
            else
            {
                if ((_capabilities & FLAG_IS_RENDERER_TYPE_SET) != 0)
                {
                    return new Object[] {facesListenersSaved, stateHelperSaved, behaviorsMapSaved,
                                        systemEventListenerClassMapSaved, null,
                                        _rendererType};
                }
                else
                {
                    return new Object[] {facesListenersSaved, stateHelperSaved, behaviorsMapSaved,
                        systemEventListenerClassMapSaved};
                }
            }
        }
        else
        {
            //Full
            Object values[] = new Object[FULL_STATE_ARRAY_SIZE];
            values[0] = saveFacesListenersList(context);
            StateHelper stateHelper = getStateHelper(false);
            if (stateHelper != null)
            {
                values[1] = stateHelper.saveState(context);
            }
            values[2] = saveBehaviorsMap(context);
            values[3] = saveSystemEventListenerClassMap(context);
            values[4] = _id;
            values[5] = _clientId;
            values[6] = _markCreated;
            values[7] = _rendererType;
            values[8] = _capabilities;
            if (!context.getCurrentPhaseId().equals(PhaseId.RENDER_RESPONSE))
            {
                values[9] = saveTransientState(context);
            }
            //values[8] = _isRendererTypeSet;
            //values[9] = _addedByHandler;
            //values[10] = _facetCreatedUIPanel;

            return values;
        }
    }

    /**
     * Invoked in the "restore view" phase, this initialises this object's members from the values saved previously into
     * the provided state object.
     * <p>
     * 
     * @param state
     *            is an object previously returned by the saveState method of this class.
     */
    @SuppressWarnings("unchecked")
    public void restoreState(FacesContext context, Object state)
    {
        if (context == null)
        {
            throw new NullPointerException ("context");
        }
        
        if (state == null)
        {
            //Only happens if initialStateMarked return true
            
            if (initialStateMarked())
            {
                return;
            }
            
            throw new NullPointerException ("state");
        }
        
        Object values[] = (Object[]) state;
        
        if ( values.length == FULL_STATE_ARRAY_SIZE && initialStateMarked())
        {
            //Delta mode is active, but we are restoring a full state.
            //we need to clear the initial state, to restore state without
            //take into account delta.
            clearInitialState();
        }
        
        if (values[0] instanceof _AttachedDeltaWrapper)
        {
            //Delta: check for null is not necessary since _facesListener field
            //is only set once and never reset
            //if (_facesListeners != null)
            //{
                ((StateHolder)_facesListeners).restoreState(context,
                        ((_AttachedDeltaWrapper) values[0]).getWrappedStateObject());
            //}
        }
        else if (values[0] != null || (values.length == FULL_STATE_ARRAY_SIZE))
        {
            //Full
            _facesListeners = (_DeltaList<FacesListener>)
                restoreAttachedState(context,values[0]);
        }
        // Note that if values[0] == null && initialStateMarked(),
        // means delta is null, not that _facesListeners == null. 
        // We can do this here because _facesListeners instance once
        // is created is never replaced or set to null.
        
        getStateHelper().restoreState(context, values[1]);
        
        if (values.length == FULL_STATE_ARRAY_SIZE)
        {
            _id = (String) values[4];
            _clientId = (String) values[5];
            _markCreated = (String) values[6];
            _rendererType = (String) values[7];
            _capabilities = (Integer) values[8];
            //_isRendererTypeSet = (Boolean) values[8];
            //_addedByHandler = (Boolean) values[9];
            //_facetCreatedUIPanel = (Boolean) values[10];
        }
        else if (values.length == 6)
        {
            restoreTransientState(context, values[4]);
            _rendererType = (String) values[5];
            //_isRendererTypeSet = true;
            _capabilities |= FLAG_IS_RENDERER_TYPE_SET;
        }
        else if (values.length == 5)
        {
            restoreTransientState(context, values[4]);
        }
        
        
        // rendererType needs to be restored before SystemEventListener,
        // otherwise UIComponent.getCurrentComponent(context).getRenderer(context)
        // will not work correctly
        if (values.length == FULL_STATE_ARRAY_SIZE)
        {
            //Full restore
            restoreFullBehaviorsMap(context, values[2]);
            restoreFullSystemEventListenerClassMap(context, values[3]);
            restoreTransientState(context, values[9]);
        }
        else
        {
            //Delta restore
            restoreDeltaBehaviorsMap(context, values[2]);
            restoreDeltaSystemEventListenerClassMap(context, values[3]);
        }
    }
    
    private Object saveFacesListenersList(FacesContext facesContext)
    {
        PartialStateHolder holder = (PartialStateHolder) _facesListeners;
        if (initialStateMarked() && _facesListeners != null && holder.initialStateMarked())
        {                
            Object attachedState = holder.saveState(facesContext);
            if (attachedState != null)
            {
                return new _AttachedDeltaWrapper(_facesListeners.getClass(),
                        attachedState);
            }
            //_facesListeners instances once is created never changes, we can return null
            return null;
        }
        else
        {
            return saveAttachedState(facesContext,_facesListeners);
        }            
    }

    @SuppressWarnings("unchecked")
    private void restoreFullBehaviorsMap(FacesContext facesContext, Object stateObj)
    {
        if (stateObj != null)
        {
            Map<String, Object> stateMap = (Map<String, Object>) stateObj;
            int initCapacity = (stateMap.size() * 4 + 3) / 3;
            _behaviorsMap = new HashMap<String,  List<ClientBehavior> >(initCapacity);
            _unmodifiableBehaviorsMap = null;
            for (Map.Entry<String, Object> entry : stateMap.entrySet())
            {
                _behaviorsMap.put(entry.getKey(),
                                  (List<ClientBehavior>) restoreAttachedState(facesContext, entry.getValue()));
            }
        }
        else
        {
            _behaviorsMap = null;
            _unmodifiableBehaviorsMap = null;
        }        
    }
    
    @SuppressWarnings("unchecked")
    private void restoreDeltaBehaviorsMap(FacesContext facesContext, Object stateObj)
    {
        if (stateObj != null)
        {
            _unmodifiableBehaviorsMap = null;
            Map<String, Object> stateMap = (Map<String, Object>) stateObj;
            int initCapacity = (stateMap.size() * 4 + 3) / 3;
            if (_behaviorsMap == null)
            {
                _behaviorsMap = new HashMap<String,  List<ClientBehavior> >(initCapacity);
            }
            for (Map.Entry<String, Object> entry : stateMap.entrySet())
            {
                Object savedObject = entry.getValue(); 
                if (savedObject instanceof _AttachedDeltaWrapper)
                {
                    StateHolder holderList = (StateHolder) _behaviorsMap.get(entry.getKey());
                    holderList.restoreState(facesContext,
                                            ((_AttachedDeltaWrapper) savedObject).getWrappedStateObject());
                }
                else
                {
                    _behaviorsMap.put(entry.getKey(),
                                      (List<ClientBehavior>) restoreAttachedState(facesContext, savedObject));
                }
            }
        }
    }
    
    private Object saveBehaviorsMap(FacesContext facesContext)
    {
        if (_behaviorsMap != null)
        {
            if (initialStateMarked())
            {
                HashMap<String, Object> stateMap = new HashMap<String, Object>(_behaviorsMap.size(), 1);
                boolean nullDelta = true;
                for (Map.Entry<String, List<ClientBehavior> > entry : _behaviorsMap.entrySet())
                {
                    // The list is always an instance of _DeltaList so we can cast to
                    // PartialStateHolder 
                    PartialStateHolder holder = (PartialStateHolder) entry.getValue();
                    if (holder.initialStateMarked())
                    {
                        Object attachedState = holder.saveState(facesContext);
                        if (attachedState != null)
                        {
                            stateMap.put(entry.getKey(), new _AttachedDeltaWrapper(_behaviorsMap.getClass(),
                                    attachedState));
                            nullDelta = false;
                        }
                    }
                    else
                    {
                        stateMap.put(entry.getKey(), saveAttachedState(facesContext, holder));
                        nullDelta = false;
                    }
                }
                if (nullDelta)
                {
                    return null;
                }
                return stateMap;
            }
            else
            {
                //Save it in the traditional way
                HashMap<String, Object> stateMap = 
                    new HashMap<String, Object>(_behaviorsMap.size(), 1);
                for (Map.Entry<String, List<ClientBehavior> > entry : _behaviorsMap.entrySet())
                {
                    stateMap.put(entry.getKey(), saveAttachedState(facesContext, entry.getValue()));
                }
                return stateMap;
            }
        }
        else
        {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private void restoreFullSystemEventListenerClassMap(FacesContext facesContext, Object stateObj)
    {
        if (stateObj != null)
        {
            Map<Class<? extends SystemEvent>, Object> stateMap = (Map<Class<? extends SystemEvent>, Object>) stateObj;
            int initCapacity = (stateMap.size() * 4 + 3) / 3;
            _systemEventListenerClassMap
                    = new HashMap<Class<? extends SystemEvent>, List<SystemEventListener>>(initCapacity);
            for (Map.Entry<Class<? extends SystemEvent>, Object> entry : stateMap.entrySet())
            {
                _systemEventListenerClassMap.put(entry.getKey(),
                        (List<SystemEventListener>) restoreAttachedState(facesContext, entry.getValue()));
            }
        }
        else
        {
            _systemEventListenerClassMap = null;
        }        
    }
    
    @SuppressWarnings("unchecked")
    private void restoreDeltaSystemEventListenerClassMap(FacesContext facesContext, Object stateObj)
    {
        if (stateObj != null)
        {
            Map<Class<? extends SystemEvent>, Object> stateMap = (Map<Class<? extends SystemEvent>, Object>) stateObj;
            int initCapacity = (stateMap.size() * 4 + 3) / 3;
            if (_systemEventListenerClassMap == null)
            {
                _systemEventListenerClassMap
                        = new HashMap<Class<? extends SystemEvent>, List<SystemEventListener>>(initCapacity);
            }
            for (Map.Entry<Class<? extends SystemEvent>, Object> entry : stateMap.entrySet())
            {
                Object savedObject = entry.getValue(); 
                if (savedObject instanceof _AttachedDeltaWrapper)
                {
                    StateHolder holderList = (StateHolder) _systemEventListenerClassMap.get(entry.getKey());
                    holderList.restoreState(facesContext,
                                            ((_AttachedDeltaWrapper) savedObject).getWrappedStateObject());
                }
                else
                {
                    _systemEventListenerClassMap.put(entry.getKey(),
                            (List<SystemEventListener>) restoreAttachedState(facesContext, savedObject));
                }
            }
        }
    }
    
    private Object saveSystemEventListenerClassMap(FacesContext facesContext)
    {
        if (_systemEventListenerClassMap != null)
        {
            if (initialStateMarked())
            {
                HashMap<Class<? extends SystemEvent>, Object> stateMap
                        = new HashMap<Class<? extends SystemEvent>, Object>(_systemEventListenerClassMap.size(), 1);
                boolean nullDelta = true;
                for (Map.Entry<Class<? extends SystemEvent>, List<SystemEventListener> > entry
                        : _systemEventListenerClassMap.entrySet())
                {
                    // The list is always an instance of _DeltaList so we can cast to
                    // PartialStateHolder 
                    PartialStateHolder holder = (PartialStateHolder) entry.getValue();
                    if (holder.initialStateMarked())
                    {
                        Object attachedState = holder.saveState(facesContext);
                        if (attachedState != null)
                        {
                            stateMap.put(entry.getKey(),
                                    new _AttachedDeltaWrapper(_systemEventListenerClassMap.getClass(), attachedState));
                            nullDelta = false;
                        }
                    }
                    else
                    {
                        stateMap.put(entry.getKey(), saveAttachedState(facesContext, holder));
                        nullDelta = false;
                    }
                }
                if (nullDelta)
                {
                    return null;
                }
                return stateMap;
            }
            else
            {
                //Save it in the traditional way
                HashMap<Class<? extends SystemEvent>, Object> stateMap = 
                    new HashMap<Class<? extends SystemEvent>, Object>(_systemEventListenerClassMap.size(), 1);
                for (Map.Entry<Class<? extends SystemEvent>, List<SystemEventListener> > entry
                        : _systemEventListenerClassMap.entrySet())
                {
                    stateMap.put(entry.getKey(), saveAttachedState(facesContext, entry.getValue()));
                }
                return stateMap;
            }
        }
        else
        {
            return null;
        }
    }
    
    /*
    private Object saveBindings(FacesContext context)
    {
        if (bindings != null)
        {
            HashMap<String, Object> stateMap = new HashMap<String, Object>(bindings.size(), 1);
            for (Iterator<Entry<String, ValueExpression>> it = bindings.entrySet().iterator(); it.hasNext();)
            {
                Entry<String, ValueExpression> entry = it.next();
                stateMap.put(entry.getKey(), saveAttachedState(context, entry.getValue()));
            }
            return stateMap;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void restoreValueExpressionMap(FacesContext context, Object stateObj)
    {
        if (stateObj != null)
        {
            Map<String, Object> stateMap = (Map<String, Object>) stateObj;
            int initCapacity = (stateMap.size() * 4 + 3) / 3;
            bindings = new HashMap<String, ValueExpression>(initCapacity);
            for (Map.Entry<String, Object> entry : stateMap.entrySet())
            {
                bindings.put(entry.getKey(), (ValueExpression) restoreAttachedState(context, entry.getValue()));
            }
        }
        else
        {
            bindings = null;
        }
    }*/

    /**
     * @param string
     *            the component id, that should be a vaild one.
     */
    private void isIdValid(String string)
    {

        // is there any component identifier ?
        if (string == null)
        {
            return;
        }

        // Component identifiers must obey the following syntax restrictions:
        // 1. Must not be a zero-length String.
        if (string.length() == 0)
        {
            throw new IllegalArgumentException("component identifier must not be a zero-length String");
        }

        // If new id is the same as old it must be valid
        if (string.equals(_id))
        {
            return;
        }

        // 2. First character must be a letter or an underscore ('_').
        if (!Character.isLetter(string.charAt(0)) && string.charAt(0) != '_')
        {
            throw new IllegalArgumentException("component identifier's first character must be a letter "
                                               + "or an underscore ('_')! But it is \""
                                               + string.charAt(0) + "\"");
        }
        for (int i = 1; i < string.length(); i++)
        {
            char c = string.charAt(i);
            // 3. Subsequent characters must be a letter, a digit, an underscore ('_'), or a dash ('-').
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_')
            {
                throw new IllegalArgumentException("Subsequent characters of component identifier must be a letter, "
                                                   + "a digit, an underscore ('_'), or a dash ('-')! "
                                                   + "But component identifier \"" + string + "\" contains \""
                                                   + c + "\"");
            }
        }
    }

    private boolean _isPhaseExecutable(FacesContext context)
    {
        if (context == null)
        {
            throw new NullPointerException("context");
        }

        // If the rendered property of this UIComponent is false, skip further processing.
        return isRendered();
    }

    boolean isCachedFacesContext()
    {
        return _facesContext != null;
    }
    
    void setCachedFacesContext(FacesContext facesContext)
    {
        _facesContext = facesContext;
    }
    
    Renderer getCachedRenderer()
    {
        return _cachedRenderer;
    }
    
    void setCachedRenderer(Renderer renderer)
    {
        _cachedRenderer = renderer;
    }

    Boolean isCachedIsRendered()
    {
        return _cachedIsRendered;
    }
    
    void setCachedIsRendered(Boolean rendered)
    {
       _cachedIsRendered = rendered;
    }
    
    <T> T getExpressionValue(String attribute, T explizitValue, T defaultValueIfExpressionNull)
    {
        return _ComponentUtils.getExpressionValue(this, attribute, explizitValue, defaultValueIfExpressionNull);
    }

    void setOamVfMarkCreated(String markCreated)
    {
        _markCreated = markCreated;
    }
    
    String getOamVfMarkCreated()
    {
        return _markCreated;
    }
    
    String getOamVfFacetName()
    {
        return _facetName;
    }
    
    void setOamVfFacetName(String facetName)
    {
        _facetName = facetName;
    }
    
    boolean isOamVfAddedByHandler()
    {
        return (_capabilities & FLAG_ADDED_BY_HANDLER) != 0;
    }
    
    void setOamVfAddedByHandler(boolean addedByHandler)
    {
        if (addedByHandler)
        {
            _capabilities |= FLAG_ADDED_BY_HANDLER;
        }
        else
        {
            _capabilities &= ~(FLAG_ADDED_BY_HANDLER);
        }
        //_addedByHandler = addedByHandler;
    }
    
    boolean isOamVfFacetCreatedUIPanel()
    {
        return (_capabilities & FLAG_FACET_CREATED_UIPANEL) != 0;
    }
    
    void setOamVfFacetCreatedUIPanel(boolean facetCreatedUIPanel)
    {
        //_facetCreatedUIPanel = facetCreatedUIPanel;
        if (facetCreatedUIPanel)
        {
            _capabilities |= FLAG_FACET_CREATED_UIPANEL;
        }
        else
        {
            _capabilities &= ~(FLAG_FACET_CREATED_UIPANEL);
        }
    }

/**
     * <p>
     * This gets a single FacesContext-local shared stringbuilder instance, each time you call
     * _getSharedStringBuilder it sets the length of the stringBuilder instance to 0.
     * </p><p>
     * This allows you to use the same StringBuilder instance over and over.
     * You must call toString on the instance before calling _getSharedStringBuilder again.
     * </p>
     * Example that works
     * <pre><code>
     * StringBuilder sb1 = _getSharedStringBuilder();
     * sb1.append(a).append(b);
     * String c = sb1.toString();
     *
     * StringBuilder sb2 = _getSharedStringBuilder();
     * sb2.append(b).append(a);
     * String d = sb2.toString();
     * </code></pre>
     * <br><br>
     * Example that doesn't work, you must call toString on sb1 before
     * calling _getSharedStringBuilder again.
     * <pre><code>
     * StringBuilder sb1 = _getSharedStringBuilder();
     * StringBuilder sb2 = _getSharedStringBuilder();
     *
     * sb1.append(a).append(b);
     * String c = sb1.toString();
     *
     * sb2.append(b).append(a);
     * String d = sb2.toString();
     * </code></pre>
     *
     */
    static StringBuilder _getSharedStringBuilder()
    {
        return _getSharedStringBuilder(FacesContext.getCurrentInstance());
    }

    // TODO checkstyle complains; does this have to lead with __ ?
    static StringBuilder _getSharedStringBuilder(FacesContext facesContext)
    {
        Map<Object, Object> attributes = facesContext.getAttributes();

        StringBuilder sb = (StringBuilder) attributes.get(_STRING_BUILDER_KEY);

        if (sb == null)
        {
            sb = new StringBuilder();
            attributes.put(_STRING_BUILDER_KEY, sb);
        }
        else
        {

            // clear out the stringBuilder by setting the length to 0
            sb.setLength(0);
        }

        return sb;
    }

    // ------------------ GENERATED CODE BEGIN (do not modify!) --------------------

    private static final Boolean DEFAULT_RENDERED = Boolean.TRUE;

    @Override
    public void setRendered(boolean rendered)
    {
        getStateHelper().put(PropertyKeys.rendered, rendered );
        setCachedIsRendered(null);
    }

    @Override
    public void setRendererType(String rendererType)
    {
        this._rendererType = rendererType;
        if (initialStateMarked())
        {
            //This flag just indicates the rendererType 
            //should be included on the delta
            //this._isRendererTypeSet = true;
            _capabilities |= FLAG_IS_RENDERER_TYPE_SET;
        }
        setCachedRenderer(null);
    }

    // ------------------ GENERATED CODE END ---------------------------------------

    private Map<String, List<ClientBehavior>> wrapBehaviorsMap()
    {
        if (_unmodifiableBehaviorsMap == null)
        {
            _unmodifiableBehaviorsMap = Collections.unmodifiableMap(_behaviorsMap); 
        }
        return _unmodifiableBehaviorsMap; 
    }
}
