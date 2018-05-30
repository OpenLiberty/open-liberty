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
package javax.faces.component;

import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import java.io.Serializable;
import javax.faces.component.UIComponent;


// Generated from class javax.faces.component._UIWebsocket.
//
// WARNING: This file was automatically generated. Do not edit it directly,
//          or you will lose your changes.
public class UIWebsocket extends javax.faces.component.UIOutput
    implements javax.faces.component.behavior.ClientBehaviorHolder
{

    static public final String COMPONENT_FAMILY =
        "javax.faces.Script";
    static public final String COMPONENT_TYPE =
        "javax.faces.Websocket";

    //BEGIN CODE COPIED FROM javax.faces.component._UIWebsocket 
    public java.util.Collection getEventNames()
    {
        return new java.util.Collection<String>(){

            @Override
            public int size()
            {
                return 0;
            }

            @Override
            public boolean isEmpty()
            {
                return false;
            }

            @Override
            public boolean contains(Object o)
            {
                return true;
            }

            @Override
            public java.util.Iterator<String> iterator()
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Object[] toArray()
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public <T> T[] toArray(T[] a)
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean add(String e)
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean remove(Object o)
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean containsAll(java.util.Collection<?> c)
            {
                return true;
            }

            @Override
            public boolean addAll(java.util.Collection<? extends String> c)
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean removeAll(java.util.Collection<?> c)
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean retainAll(java.util.Collection<?> c)
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void clear()
            {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }


    //END CODE COPIED FROM javax.faces.component._UIWebsocket

    public UIWebsocket()
    {
        setRendererType("javax.faces.Websocket");
    }

    @Override    
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }



    
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
        , connected
    }

 }
