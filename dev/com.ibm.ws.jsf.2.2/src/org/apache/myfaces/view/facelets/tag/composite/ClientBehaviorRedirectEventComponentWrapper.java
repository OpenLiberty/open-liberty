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

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.component.ContextCallback;
import javax.faces.component.UIComponent;
import javax.faces.component.behavior.ClientBehavior;
import javax.faces.component.behavior.ClientBehaviorHolder;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.FacesEvent;
import javax.faces.event.FacesListener;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.faces.render.Renderer;

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
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1454241 $ $Date: 2013-03-08 04:29:07 +0000 (Fri, 08 Mar 2013) $
 */
public class ClientBehaviorRedirectEventComponentWrapper extends UIComponent 
    implements FacesWrapper<UIComponent>, ClientBehaviorHolder
{

    private final UIComponent _delegate;
    private final String _sourceEvent; //cc:clientBehavior "name"
    private final String _targetEvent; //cc:clientBehavior "event"

    public ClientBehaviorRedirectEventComponentWrapper(UIComponent delegate,
            String sourceEvent, String targetEvent)
    {
        super();
        _delegate = delegate;
        _sourceEvent = sourceEvent;
        _targetEvent = targetEvent;
    }

    public UIComponent getWrapped()
    {
        return _delegate;
    }

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

    public Map<String, List<ClientBehavior>> getClientBehaviors()
    {
        Map<String, List<ClientBehavior>> clientBehaviors = new HashMap<String, List<ClientBehavior>>(1);
        clientBehaviors.put(_sourceEvent, ((ClientBehaviorHolder)_delegate).getClientBehaviors().get(_targetEvent));
        return Collections.unmodifiableMap(clientBehaviors);
    }

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

    public Collection<String> getEventNames()
    {
        return Collections.singletonList(_sourceEvent);
    }
    
    // UIComponent wrapping. Just delegate.    
    public void broadcast(FacesEvent event) throws AbortProcessingException
    {
        _delegate.broadcast(event);
    }

    public void clearInitialState()
    {
        _delegate.clearInitialState();
    }

    public void decode(FacesContext context)
    {
        _delegate.decode(context);
    }

    public void encodeAll(FacesContext context) throws IOException
    {
        _delegate.encodeAll(context);
    }

    public void encodeBegin(FacesContext context) throws IOException
    {
        _delegate.encodeBegin(context);
    }

    public void encodeChildren(FacesContext context) throws IOException
    {
        _delegate.encodeChildren(context);
    }

    public void encodeEnd(FacesContext context) throws IOException
    {
        _delegate.encodeEnd(context);
    }

    public UIComponent findComponent(String expr)
    {
        return _delegate.findComponent(expr);
    }

    public Map<String, Object> getAttributes()
    {
        return _delegate.getAttributes();
    }

    public int getChildCount()
    {
        return _delegate.getChildCount();
    }

    public List<UIComponent> getChildren()
    {
        return _delegate.getChildren();
    }

    public String getClientId()
    {
        return _delegate.getClientId();
    }

    public String getClientId(FacesContext context)
    {
        return _delegate.getClientId(context);
    }

    public String getContainerClientId(FacesContext ctx)
    {
        return _delegate.getContainerClientId(ctx);
    }

    public UIComponent getFacet(String name)
    {
        return _delegate.getFacet(name);
    }

    public int getFacetCount()
    {
        return _delegate.getFacetCount();
    }

    public Map<String, UIComponent> getFacets()
    {
        return _delegate.getFacets();
    }

    public Iterator<UIComponent> getFacetsAndChildren()
    {
        return _delegate.getFacetsAndChildren();
    }

    public String getFamily()
    {
        return _delegate.getFamily();
    }

    public String getId()
    {
        return _delegate.getId();
    }

    public List<SystemEventListener> getListenersForEventClass(
            Class<? extends SystemEvent> eventClass)
    {
        return _delegate.getListenersForEventClass(eventClass);
    }

    public UIComponent getNamingContainer()
    {
        return _delegate.getNamingContainer();
    }

    public UIComponent getParent()
    {
        return _delegate.getParent();
    }

    public String getRendererType()
    {
        return _delegate.getRendererType();
    }

    public boolean getRendersChildren()
    {
        return _delegate.getRendersChildren();
    }

    public Map<String, String> getResourceBundleMap()
    {
        return _delegate.getResourceBundleMap();
    }

    public ValueBinding getValueBinding(String name)
    {
        return _delegate.getValueBinding(name);
    }

    public ValueExpression getValueExpression(String name)
    {
        return _delegate.getValueExpression(name);
    }

    public boolean initialStateMarked()
    {
        return _delegate.initialStateMarked();
    }

    public boolean invokeOnComponent(FacesContext context, String clientId,
            ContextCallback callback) throws FacesException
    {
        return _delegate.invokeOnComponent(context, clientId, callback);
    }

    public boolean isInView()
    {
        return _delegate.isInView();
    }

    public boolean isRendered()
    {
        return _delegate.isRendered();
    }

    public boolean isTransient()
    {
        return _delegate.isTransient();
    }

    public void markInitialState()
    {
        _delegate.markInitialState();
    }

    public void processDecodes(FacesContext context)
    {
        _delegate.processDecodes(context);
    }

    public void processEvent(ComponentSystemEvent event)
            throws AbortProcessingException
    {
        _delegate.processEvent(event);
    }

    public void processRestoreState(FacesContext context, Object state)
    {
        _delegate.processRestoreState(context, state);
    }

    public Object processSaveState(FacesContext context)
    {
        return _delegate.processSaveState(context);
    }

    public void processUpdates(FacesContext context)
    {
        _delegate.processUpdates(context);
    }

    public void processValidators(FacesContext context)
    {
        _delegate.processValidators(context);
    }

    public void queueEvent(FacesEvent event)
    {
        _delegate.queueEvent(event);
    }

    public void restoreState(FacesContext context, Object state)
    {
        _delegate.restoreState(context, state);
    }

    public Object saveState(FacesContext context)
    {
        return _delegate.saveState(context);
    }

    public void setId(String id)
    {
        _delegate.setId(id);
    }

    public void setInView(boolean isInView)
    {
        _delegate.setInView(isInView);
    }

    public void setParent(UIComponent parent)
    {
        _delegate.setParent(parent);
    }

    public void setRendered(boolean rendered)
    {
        _delegate.setRendered(rendered);
    }

    public void setRendererType(String rendererType)
    {
        _delegate.setRendererType(rendererType);
    }

    public void setTransient(boolean newTransientValue)
    {
        _delegate.setTransient(newTransientValue);
    }

    public void setValueBinding(String name, ValueBinding binding)
    {
        _delegate.setValueBinding(name, binding);
    }

    public void setValueExpression(String name, ValueExpression expression)
    {
        _delegate.setValueExpression(name, expression);
    }

    public void subscribeToEvent(Class<? extends SystemEvent> eventClass,
            ComponentSystemEventListener componentListener)
    {
        _delegate.subscribeToEvent(eventClass, componentListener);
    }

    public void unsubscribeFromEvent(Class<? extends SystemEvent> eventClass,
            ComponentSystemEventListener componentListener)
    {
        _delegate.unsubscribeFromEvent(eventClass, componentListener);
    }

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
}
