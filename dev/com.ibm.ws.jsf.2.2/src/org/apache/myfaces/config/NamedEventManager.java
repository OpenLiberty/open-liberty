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
package org.apache.myfaces.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.PostValidateEvent;
import javax.faces.event.PreRenderComponentEvent;
import javax.faces.event.PreRenderViewEvent;
import javax.faces.event.PreValidateEvent;

/**
 * The NamedEventManager class is used to keep map a short name to ComponentSystemEvent classes
 * annotated with @NamedEvent.
 */

public class NamedEventManager
{
    //private static final NamedEventManager instance = new NamedEventManager();
    
    private HashMap<String, Collection<Class<? extends ComponentSystemEvent>>> events;
    
    public NamedEventManager ()
    {
        events = new HashMap<String, Collection<Class<? extends ComponentSystemEvent>>>();
        
        // Special spec-defined values.
        
        addNamedEvent ("postAddToView", PostAddToViewEvent.class);
        addNamedEvent ("preRenderView", PreRenderViewEvent.class);
        addNamedEvent ("preRenderComponent", PreRenderComponentEvent.class);
        addNamedEvent ("preValidate", PreValidateEvent.class);
        addNamedEvent ("postValidate", PostValidateEvent.class);
    }
    
    //public static NamedEventManager getInstance ()
    //{
    //    return instance;
    //}
    
    /**
     * Registers a named event.
     * 
     * @param shortName a String containing the short name for the event, from the @NamedEvent.shortName()
     *        attribute.
     * @param cls the event class to register.
     */
    
    public void addNamedEvent (String shortName, Class<? extends ComponentSystemEvent> cls)
    {
        String key = shortName;
        Collection<Class<? extends ComponentSystemEvent>> eventList;
        
        // Per the spec, if the short name is missing, generate one.
        
        if (shortName == null)
        {
            key = getFixedName (cls);
        }
        
        eventList = events.get (key);
        
        if (eventList == null)
        {
            // First event registered to this short name.
            
            eventList = new LinkedList<Class<? extends ComponentSystemEvent>>();
            
            events.put (key, eventList);
        }
        
        eventList.add (cls);
    }
    
    /**
     * Retrieves a collection of system event classes based on their short name.
     * 
     * @param shortName the short name to look up.
     * @return a Collection of Class objects containing the system event classes registered to
     *         the given short name.
     */
    
    public Collection<Class<? extends ComponentSystemEvent>> getNamedEvent (String shortName)
    {
        return events.get (shortName);
    }
    
    /**
     * Retrieves the short name for an event class, according to spec rules.
     * 
     * @param cls the class to find the short name for.
     * @return a String containing the short name for the given class.
     */
    
    private String getFixedName (Class<? extends ComponentSystemEvent> cls)
    {
        StringBuilder result = new StringBuilder();
        String className;
        
        // Get the unqualified class name.
        
        className = cls.getSimpleName();
        
        // Strip the trailing "event" off the class name if present.
        
        if (className.toLowerCase().endsWith ("event"))
        {
            className = className.substring (0, result.length() - 5);
        }
        
        // Prepend the package name.
        
        if (cls.getPackage() != null)
        {
            result.append (cls.getPackage().getName());
            result.append ('.');
        }
        
        result.append (className);
        
        return result.toString();
    }
}
