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
package javax.faces.component.behavior;

import java.util.Collection;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * @since 2.0
 */
public abstract class ClientBehaviorContext
{

    public static ClientBehaviorContext createClientBehaviorContext(FacesContext context,
                                                                    UIComponent component,
                                                                    String eventName,
                                                                    String sourceId,
                                                                    Collection<Parameter> parameters)
    {
        // This method is weird... Creating a dummy impl class seems stupid, yet I don't see any other way...
        if(context == null)
        {
            throw new NullPointerException("context argument must not be null");
        }
        if(component == null)
        {
            throw new NullPointerException("component argument must not be null");
        }
        if(eventName == null)
        {
            throw new NullPointerException("eventName argument must not be null");
        }

        return new ClientBehaviorContextImpl(context,component,eventName,sourceId, parameters);
    }

    public abstract UIComponent getComponent();

    public abstract String getEventName();

    public abstract FacesContext getFacesContext();

    public abstract Collection<Parameter> getParameters();

    public abstract String getSourceId();

    /**
     * @since 2.0
     */
    public static class Parameter
    {
        private String _name;
        private Object _value;

        public Parameter(String name, Object value)
        {
            if (name == null)
            {
                throw new NullPointerException("name");
            }

            _name = name;
            _value = value;
        }

        public String getName()
        {
            return _name;
        }

        public Object getValue()
        {
            return _value;
        }
    }
    
    private static final class ClientBehaviorContextImpl extends ClientBehaviorContext
    {
        private FacesContext _facesContext;
        private UIComponent _component;
        private String _eventName;
        private String _sourceId;
        private Collection<ClientBehaviorContext.Parameter> _parameters;
        
        public ClientBehaviorContextImpl(FacesContext context, UIComponent component, String eventName,
                String sourceId, Collection<ClientBehaviorContext.Parameter> parameters)
        {
            _facesContext = context;
            _component = component;
            _eventName = eventName;
            _sourceId = sourceId;
            _parameters = parameters;            
        }

        @Override
        public UIComponent getComponent()
        {
            return _component;
        }

        @Override
        public String getEventName()
        {
            return _eventName;
        }

        @Override
        public FacesContext getFacesContext()
        {
            return _facesContext;
        }

        @Override
        public Collection<Parameter> getParameters()
        {
            return _parameters;
        }

        @Override
        public String getSourceId()
        {
            return _sourceId;
        }
    }
}
