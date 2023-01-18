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
package org.apache.myfaces.view.facelets.tag.composite;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.el.ValueExpression;
import jakarta.faces.FacesException;
import jakarta.faces.FacesWrapper;
import jakarta.faces.component.ContextCallback;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.behavior.ClientBehavior;
import jakarta.faces.component.behavior.ClientBehaviorHolder;
import jakarta.faces.component.visit.VisitCallback;
import jakarta.faces.component.visit.VisitContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.ComponentSystemEventListener;
import jakarta.faces.event.FacesEvent;
import jakarta.faces.event.FacesListener;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import jakarta.faces.render.Renderer;

/**
 * This class has two usages:
 * 
 * 1. On ClientBehaviorAttachedObjectTargetImpl to redirect the incoming sourceEvent
 * to the final targetEvent.   
 * 2. On FaceletsViewDeclarationLanguage.retargetAttachedObjects to redirect too, but
 * this time is to allow chain events for nested composite components.
 * 
 * This class also implements FacesWrapper interface, to make possible to retrieve the
 * real component if necessary.
 * 
 * @author Leonardo Uribe (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public class ClientBehaviorRedirectEventComponentWrapper extends UIComponent 
    implements FacesWrapper<UIComponent>, ClientBehaviorHolder
{
    private final UIComponent _composite;
    private final UIComponent _delegate;
    private final String _sourceEvent; //cc:clientBehavior "name"
    private final String _targetEvent; //cc:clientBehavior "event"

    public ClientBehaviorRedirectEventComponentWrapper(UIComponent composite, UIComponent delegate,
            String sourceEvent, String targetEvent)
    {
        super();
        _composite = composite;
        _delegate = delegate;
        _sourceEvent = sourceEvent;
        _targetEvent = targetEvent;
    }

    @Override
    public UIComponent getWrapped()
    {
        return _delegate;
    }

    @Override
    public void addClientBehavior(String eventName, ClientBehavior behavior)
    {
        if (_sourceEvent.equals(eventName))
        {
            String targetEventName = _targetEvent == null
                    ? ((ClientBehaviorHolder)_delegate).getDefaultEventName()
                    : _targetEvent;
            ((ClientBehaviorHolder)_delegate).addClientBehavior(targetEventName , behavior);
        }
    }

    @Override
    public Map<String, List<ClientBehavior>> getClientBehaviors()
    {
        Map<String, List<ClientBehavior>> clientBehaviors = new HashMap<>(1);
        clientBehaviors.put(_sourceEvent, ((ClientBehaviorHolder)_delegate).getClientBehaviors().get(_targetEvent));
        return Collections.unmodifiableMap(clientBehaviors);
    }

    @Override
    public String getDefaultEventName()
    {
        if (_targetEvent == null )
        {
            // There is no targetEvent assigned, so we need to check if there is 
            // a default event name on the delegate, if so return sourceEvent, otherwise
            // there is no default event and we can't resolve the redirection, so
            // return null. Note this usually could cause another exception later
            // (see AjaxHandler code 
            if (((ClientBehaviorHolder)_delegate).getDefaultEventName() != null)
            {
                return _sourceEvent;
            }
            else
            {
                return null;
            }
        }
        else
        {
            // We have a target event, so in this case we have to return the sourceEvent,
            // because it is expected the client behavior to be attached has this event name.
            return _sourceEvent;
        }
    }

    @Override
    public Collection<String> getEventNames()
    {
        return Collections.singletonList(_sourceEvent);
    }
    
    @Override
    public void broadcast(FacesEvent event) throws AbortProcessingException
    {
        _delegate.broadcast(event);
    }

    @Override
    public void clearInitialState()
    {
        _delegate.clearInitialState();
    }

    @Override
    public void decode(FacesContext context)
    {
        _delegate.decode(context);
    }

    @Override
    public void encodeAll(FacesContext context) throws IOException
    {
        _delegate.encodeAll(context);
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException
    {
        _delegate.encodeBegin(context);
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException
    {
        _delegate.encodeChildren(context);
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException
    {
        _delegate.encodeEnd(context);
    }

    @Override
    public UIComponent findComponent(String expr)
    {
        return _delegate.findComponent(expr);
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        return _delegate.getAttributes();
    }

    @Override
    public int getChildCount()
    {
        return _delegate.getChildCount();
    }

    @Override
    public List<UIComponent> getChildren()
    {
        return _delegate.getChildren();
    }

    @Override
    public String getClientId()
    {
        return _delegate.getClientId();
    }

    @Override
    public String getClientId(FacesContext context)
    {
        return _delegate.getClientId(context);
    }

    @Override
    public String getContainerClientId(FacesContext ctx)
    {
        return _delegate.getContainerClientId(ctx);
    }

    @Override
    public UIComponent getFacet(String name)
    {
        return _delegate.getFacet(name);
    }

    @Override
    public int getFacetCount()
    {
        return _delegate.getFacetCount();
    }

    @Override
    public Map<String, UIComponent> getFacets()
    {
        return _delegate.getFacets();
    }

    @Override
    public Iterator<UIComponent> getFacetsAndChildren()
    {
        return _delegate.getFacetsAndChildren();
    }
    
    @Override
    public String getFamily()
    {
        return _delegate.getFamily();
    }

    @Override
    public String getId()
    {
        return _delegate.getId();
    }

    @Override
    public List<SystemEventListener> getListenersForEventClass(Class<? extends SystemEvent> eventClass)
    {
        return _delegate.getListenersForEventClass(eventClass);
    }

    @Override
    public UIComponent getNamingContainer()
    {
        return _delegate.getNamingContainer();
    }

    @Override
    public UIComponent getParent()
    {
        return _delegate.getParent();
    }

    @Override
    public String getRendererType()
    {
        return _delegate.getRendererType();
    }

    @Override
    public boolean getRendersChildren()
    {
        return _delegate.getRendersChildren();
    }

    @Override
    public Map<String, String> getResourceBundleMap()
    {
        return _delegate.getResourceBundleMap();
    }

    @Override
    public ValueExpression getValueExpression(String name)
    {
        return _delegate.getValueExpression(name);
    }

    @Override
    public boolean initialStateMarked()
    {
        return _delegate.initialStateMarked();
    }

    @Override
    public boolean invokeOnComponent(FacesContext context, String clientId,
            ContextCallback callback) throws FacesException
    {
        return _delegate.invokeOnComponent(context, clientId, callback);
    }

    @Override
    public boolean isInView()
    {
        return _delegate.isInView();
    }

    @Override
    public boolean isRendered()
    {
        return _delegate.isRendered();
    }

    @Override
    public boolean isTransient()
    {
        return _delegate.isTransient();
    }

    @Override
    public void markInitialState()
    {
        _delegate.markInitialState();
    }

    @Override
    public void processDecodes(FacesContext context)
    {
        _delegate.processDecodes(context);
    }

    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException
    {
        _delegate.processEvent(event);
    }

    @Override
    public void processRestoreState(FacesContext context, Object state)
    {
        _delegate.processRestoreState(context, state);
    }

    @Override
    public Object processSaveState(FacesContext context)
    {
        return _delegate.processSaveState(context);
    }

    @Override
    public void processUpdates(FacesContext context)
    {
        _delegate.processUpdates(context);
    }

    @Override
    public void processValidators(FacesContext context)
    {
        _delegate.processValidators(context);
    }

    @Override
    public void queueEvent(FacesEvent event)
    {
        _delegate.queueEvent(event);
    }

    @Override
    public void restoreState(FacesContext context, Object state)
    {
        _delegate.restoreState(context, state);
    }

    @Override
    public Object saveState(FacesContext context)
    {
        return _delegate.saveState(context);
    }

    @Override
    public void setId(String id)
    {
        _delegate.setId(id);
    }

    @Override
    public void setInView(boolean isInView)
    {
        _delegate.setInView(isInView);
    }

    @Override
    public void setParent(UIComponent parent)
    {
        _delegate.setParent(parent);
    }

    @Override
    public void setRendered(boolean rendered)
    {
        _delegate.setRendered(rendered);
    }

    @Override
    public void setRendererType(String rendererType)
    {
        _delegate.setRendererType(rendererType);
    }

    @Override
    public void setTransient(boolean newTransientValue)
    {
        _delegate.setTransient(newTransientValue);
    }

    @Override
    public void setValueExpression(String name, ValueExpression expression)
    {
        _delegate.setValueExpression(name, expression);
    }

    @Override
    public void subscribeToEvent(Class<? extends SystemEvent> eventClass,
            ComponentSystemEventListener componentListener)
    {
        _delegate.subscribeToEvent(eventClass, componentListener);
    }

    @Override
    public void unsubscribeFromEvent(Class<? extends SystemEvent> eventClass,
            ComponentSystemEventListener componentListener)
    {
        _delegate.unsubscribeFromEvent(eventClass, componentListener);
    }

    @Override
    public boolean visitTree(VisitContext context, VisitCallback callback)
    {
        return _delegate.visitTree(context, callback);
    }
    
    // Some methods of UIComponent are protected, but for the scope of this
    // wrapper are never used, so it is safe to just do nothing or return null.

    @Override
    protected FacesContext getFacesContext()
    {
        return FacesContext.getCurrentInstance();
    }

    @Override
    protected void addFacesListener(FacesListener listener)
    {
    }

    @Override
    protected FacesListener[] getFacesListeners(Class clazz)
    {
        return null;
    }

    @Override
    protected Renderer getRenderer(FacesContext context)
    {
        return null;
    }

    @Override
    protected void removeFacesListener(FacesListener listener)
    {
    }

    @Override
    public Map<String, Object> getPassThroughAttributes(boolean create)
    {
        return getWrapped().getPassThroughAttributes(create);
    }

    public UIComponent getComposite()
    {
        return _composite;
    }

    public UIComponent getDelegate()
    {
        return _delegate;
    }
}
