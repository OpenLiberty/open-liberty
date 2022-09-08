/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package jakarta.faces.component;

import jakarta.el.ValueExpression;
import jakarta.faces.context.FacesContext;
import java.util.Collections;
import org.apache.myfaces.core.api.shared.MessageUtils;
import org.apache.myfaces.core.api.shared.CommonHtmlEvents;
import org.apache.myfaces.core.api.shared.CommonHtmlAttributes;
import jakarta.faces.component.UIComponent;
import java.io.Serializable;


// Generated from class jakarta.faces.component._UIWebsocket.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class UIWebsocket extends jakarta.faces.component.UIComponentBase
    implements jakarta.faces.component.behavior.ClientBehaviorHolder
{

    static public final String COMPONENT_FAMILY =
        "jakarta.faces.Script";
    static public final String COMPONENT_TYPE =
        "jakarta.faces.Websocket";

    //BEGIN CODE COPIED FROM jakarta.faces.component._UIWebsocket
    private static final java.util.Collection EVERY_EVENT = Collections.unmodifiableList(new java.util.ArrayList<String>() { @Override public boolean contains(Object object) { return true; } });
    public void setValueExpression(java.lang.String name, jakarta.el.ValueExpression binding)
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


    //END CODE COPIED FROM jakarta.faces.component._UIWebsocket

    public UIWebsocket()
    {
        setRendererType("jakarta.faces.Websocket");
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }



    // Start UIWebsocket getEventNames template
    @Override
    public java.util.Collection<String> getEventNames()
    {
        return EVERY_EVENT;
    }
    // End UIWebsocket getEventNames template



    // Property: channel

    public String getChannel()
    {
        return (String) getStateHelper().eval(PropertyKeys.channel);
    }

    public void setChannel(String channel)
    {
        getStateHelper().put(PropertyKeys.channel, channel );
    }
    // Property: scope

    public String getScope()
    {
        return (String) getStateHelper().eval(PropertyKeys.scope);
    }

    public void setScope(String scope)
    {
        getStateHelper().put(PropertyKeys.scope, scope );
    }
    // Property: user

    public Serializable getUser()
    {
        return (Serializable) getStateHelper().eval(PropertyKeys.user);
    }

    public void setUser(Serializable user)
    {
        getStateHelper().put(PropertyKeys.user, user );
    }
    // Property: onopen

    public String getOnopen()
    {
        return (String) getStateHelper().eval(PropertyKeys.onopen);
    }

    public void setOnopen(String onopen)
    {
        getStateHelper().put(PropertyKeys.onopen, onopen );
    }
    // Property: onmessage

    public String getOnmessage()
    {
        return (String) getStateHelper().eval(PropertyKeys.onmessage);
    }

    public void setOnmessage(String onmessage)
    {
        getStateHelper().put(PropertyKeys.onmessage, onmessage );
    }
    // Property: onclose

    public String getOnclose()
    {
        return (String) getStateHelper().eval(PropertyKeys.onclose);
    }

    public void setOnclose(String onclose)
    {
        getStateHelper().put(PropertyKeys.onclose, onclose );
    }
    // Property: onerror

    public String getOnerror()
    {
        return (String) getStateHelper().eval(PropertyKeys.onerror);
    }

    public void setOnerror(String onerror)
    {
        getStateHelper().put(PropertyKeys.onerror, onerror );
    }
    // Property: connected

    public boolean isConnected()
    {
        return (Boolean) getStateHelper().eval(PropertyKeys.connected, true);
    }

    public void setConnected(boolean connected)
    {
        getStateHelper().put(PropertyKeys.connected, connected );
    }


    enum PropertyKeys
    {
         channel
        , scope
        , user
        , onopen
        , onmessage
        , onclose
        , onerror
        , connected
    }

 }
