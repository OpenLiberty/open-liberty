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
package javax.faces.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * @since 2.0
 */
public class ExceptionQueuedEventContext implements SystemEventListenerHolder
{
    // TODO: -=Leonardo Uribe=- This type of constants should be the same 
    // for ri and myfaces, to keep binary compatibility and pass TCK test.
    public static final String IN_AFTER_PHASE_KEY = ExceptionQueuedEventContext.class.getName() + ".IN_AFTER_PHASE";
    public static final String IN_BEFORE_PHASE_KEY = ExceptionQueuedEventContext.class.getName() + ".IN_BEFORE_PHASE";
    
    private Map<Object, Object> _attributes;
    private UIComponent _component;
    private FacesContext _context;
    private PhaseId _phase;
    private Throwable _thrown;

    public ExceptionQueuedEventContext(FacesContext context, Throwable thrown)
    {
        this(context, thrown, null);
    }

    public ExceptionQueuedEventContext(FacesContext context, Throwable thrown, UIComponent component)
    {
        this(context, thrown, component, null);
    }

    public ExceptionQueuedEventContext(FacesContext context, Throwable thrown, UIComponent component, PhaseId phaseId)
    {
        _context = context;
        _thrown = thrown;
        _component = component;
        _phase = (phaseId == null ? context.getCurrentPhaseId() : phaseId);
    }
    
    public Map<Object,Object> getAttributes()
    {
        if (_attributes == null)
        {
            _attributes = new HashMap<Object, Object>();
        }
        
        return _attributes;
    }
    
    public UIComponent getComponent()
    {
        return _component;
    }
    
    public FacesContext getContext()
    {
        return _context;
    }
    
    public Throwable getException()
    {
        return _thrown;
    }

    /**
     * {@inheritDoc}
     */
    public List<SystemEventListener> getListenersForEventClass(Class<? extends SystemEvent> facesEventClass)
    {
        return Collections.singletonList((SystemEventListener)getContext().getExceptionHandler());
    }
    
    public PhaseId getPhaseId()
    {
        return _phase;
    }
    
    public boolean inAfterPhase()
    {
        return (_attributes != null && _attributes.containsKey(IN_AFTER_PHASE_KEY));
    }
    
    public boolean inBeforePhase()
    {
        return (_attributes != null && _attributes.containsKey(IN_BEFORE_PHASE_KEY));
    }
}
