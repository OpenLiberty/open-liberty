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

import java.io.Serializable;
import javax.el.ValueExpression;
import javax.faces.component.behavior.ClientBehaviorHolder;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFComponent;
import org.apache.myfaces.buildtools.maven2.plugin.builder.annotation.JSFProperty;

/**
 *
 */
@JSFComponent
(clazz = "javax.faces.component.UIWebsocket",template=true,
 name = "f:websocket",
 defaultRendererType = "javax.faces.Websocket",
 implementz = "javax.faces.component.behavior.ClientBehaviorHolder",
 bodyContent = "empty")
abstract class _UIWebsocket extends UIComponentBase
{
    
    static public final String COMPONENT_FAMILY = "javax.faces.Script";
    static public final String COMPONENT_TYPE = "javax.faces.Websocket";

    @JSFProperty
    public abstract String getChannel();
    
    @JSFProperty
    public abstract String getScope();
    
    @JSFProperty
    public abstract Serializable getUser();
    
    @JSFProperty
    public abstract String getOnopen();
    
    @JSFProperty
    public abstract String getOnmessage();
    
    @JSFProperty
    public abstract String getOnclose();

    @JSFProperty(defaultValue = "true")
    public abstract boolean isConnected();

    @Override
    public void setValueExpression(String name, ValueExpression binding) 
    {
        if (PropertyKeys.channel.toString().equals(name) || PropertyKeys.scope.toString().equals(name)) 
        {
            throw new IllegalArgumentException(name);
        }

        if (PropertyKeys.user.toString().equals(name)) 
        {
            Object user = binding.getValue(getFacesContext().getELContext());

            if (user != null && !(user instanceof Serializable)) 
            {
                throw new IllegalArgumentException("f:websocket 'user' attribute does not represent a valid user identifier because it is not Serializable.");
            }
        }

        super.setValueExpression(name, binding);
    }

    enum PropertyKeys 
    {
        channel, scope, user, onopen, onmessage, onclose, connected;
    }

}
