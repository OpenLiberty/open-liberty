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
package org.apache.myfaces.application;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.faces.FacesException;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.SystemEvent;
import jakarta.faces.event.SystemEventListener;
import jakarta.faces.event.SystemEventListenerHolder;
import org.apache.myfaces.core.api.shared.lang.Assert;

public class ApplicationImplEventManager
{
    private static final Logger log = Logger.getLogger(ApplicationImplEventManager.class.getName());
    
    protected static class EventInfo
    {
        private Class<? extends SystemEvent> systemEventClass;
        private Class<?> sourceClass;
        private SystemEventListener listener;
    }
    
    private ConcurrentHashMap<Class<? extends SystemEvent>, List<EventInfo>> globalListeners
            = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Class<? extends SystemEvent>, Constructor<? extends SystemEvent>> constructorCache
            = new ConcurrentHashMap<>();
    
    public void publishEvent(FacesContext facesContext, Class<? extends SystemEvent> systemEventClass, Object source)
    {
        Assert.notNull(source, "source");
        publishEvent(facesContext, systemEventClass, source.getClass(), source);
    }

    public void publishEvent(FacesContext facesContext, Class<? extends SystemEvent> systemEventClass,
                             Class<?> sourceBaseType, Object source)
    {
        Assert.notNull(facesContext, "facesContext");
        Assert.notNull(systemEventClass, "systemEventClass");
        Assert.notNull(source, "source");

        //Call events only if event processing is enabled.
        if (!facesContext.isProcessingEvents())
        {
            return;
        }
        
        // spec: If this argument is null the return from source.getClass() must be used as the sourceBaseType. 
        if (sourceBaseType == null)
        {
            sourceBaseType = source.getClass();
        }
        
        try
        {
            SystemEvent event = null;
            
            // component attached listeners
            if (source instanceof SystemEventListenerHolder)
            {
                List<SystemEventListener> listeners =
                        ((SystemEventListenerHolder) source).getListenersForEventClass(systemEventClass);
                event = processComponentAttachedListeners(facesContext, listeners, systemEventClass, source, event);
            }

            
            // view attached listeners
            UIViewRoot viewRoot = facesContext.getViewRoot();
            if (viewRoot != null)
            {
                List<SystemEventListener> listeners = viewRoot.getViewListenersForEventClass(systemEventClass);
                event = processViewAttachedListeners(facesContext, listeners, systemEventClass, source, event);
            }

            
            // global listeners
            List<EventInfo> eventInfos = globalListeners.get(systemEventClass);
            event = processGlobalListeners(facesContext, eventInfos, systemEventClass, source, event, sourceBaseType);
        }
        catch (AbortProcessingException e)
        {
            // If the act of invoking the processListener method causes an AbortProcessingException to be thrown,
            // processing of the listeners must be aborted, no further processing of the listeners for this event must
            // take place, and the exception must be logged with Level.SEVERE.
            log.log(Level.SEVERE, "Event processing was aborted", e);
        }
    }
    
    
    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener)
    {
        subscribeToEvent(systemEventClass, null, listener);
    }

    public void subscribeToEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
                                 SystemEventListener listener)
    {
        Assert.notNull(systemEventClass, "systemEventClass");
        Assert.notNull(listener, "listener");

        List<EventInfo> eventInfos = globalListeners.get(systemEventClass);
        if (eventInfos == null)
        {
            eventInfos = new CopyOnWriteArrayList<>();
            globalListeners.put(systemEventClass, eventInfos);
        }

        EventInfo eventInfo = new EventInfo();
        eventInfo.systemEventClass = systemEventClass;
        eventInfo.sourceClass = sourceClass;
        eventInfo.listener = listener;
        
        eventInfos.add(eventInfo);
    }
    
    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, SystemEventListener listener)
    {
        unsubscribeFromEvent(systemEventClass, null, listener);
    }

    public void unsubscribeFromEvent(Class<? extends SystemEvent> systemEventClass, Class<?> sourceClass,
                                     SystemEventListener listener)
    {
        Assert.notNull(systemEventClass, "systemEventClass");
        Assert.notNull(listener, "listener");

        List<EventInfo> eventInfos = globalListeners.get(systemEventClass);
        if (eventInfos == null || eventInfos.isEmpty())
        {
            return;
        }

        if (sourceClass == null)
        {
            eventInfos.removeIf(e -> e.listener.equals(listener));
        }
        else
        {
            eventInfos.removeIf(e -> e.sourceClass == sourceClass && e.listener.equals(listener));
        }
    }
    
  
    
    
    protected SystemEvent createEvent(Class<? extends SystemEvent> systemEventClass, FacesContext facesContext,
            Object source)
    {
        Constructor<? extends SystemEvent> constructor = constructorCache.get(systemEventClass);
        if (constructor == null)
        {
            constructor = getConstructor(systemEventClass);
            constructorCache.put(systemEventClass, constructor);
        }

        try
        {
            if (constructor.getParameterTypes().length == 2)
            {
                return constructor.newInstance(facesContext, source);
            }

            return constructor.newInstance(source);
        }
        catch (Exception e)
        {
            throw new FacesException("Couldn't instanciate system event of type " + 
                    systemEventClass.getName(), e);
        }
    }
    
    protected Constructor<? extends SystemEvent> getConstructor(Class<? extends SystemEvent> systemEventClass)
    {
        Constructor<?>[] constructors = systemEventClass.getConstructors();
        Constructor<? extends SystemEvent> constructor = null;

        // try to lookup the new 2 parameter constructor
        for (Constructor<?> c : constructors)
        {
            if (c.getParameterTypes().length == 2)
            {
                // Safe cast, since the constructor belongs
                // to a class of type SystemEvent
                constructor = (Constructor<? extends SystemEvent>) c;
                break;
            }
        }

        // try to lookup the old 1 parameter constructor
        if (constructor == null)
        {
            for (Constructor<?> c : constructors)
            {
                if (c.getParameterTypes().length == 1)
                {
                    // Safe cast, since the constructor belongs
                    // to a class of type SystemEvent
                    constructor = (Constructor<? extends SystemEvent>) c;
                    break;
                }
            }
        }

        return constructor;
    }
    


    protected SystemEvent processComponentAttachedListeners(FacesContext facesContext,
            List<? extends SystemEventListener> listeners, Class<? extends SystemEvent> systemEventClass,
            Object source, SystemEvent event)
    {
        if (listeners == null || listeners.isEmpty())
        {
            return event;
        }

        for (int i  = 0, size = listeners.size(); i < size; i++)
        {
            SystemEventListener listener = listeners.get(i);
            if (listener.isListenerForSource(source))
            {
                // Lazy construct the event; zhis same event instance must be passed to all listener instances.
                if (event == null)
                {
                    event = createEvent(systemEventClass, facesContext, source);
                }

                if (event.isAppropriateListener(listener))
                {
                    event.processListener(listener);
                }
            }
        }

        return event;
    }
    
    protected SystemEvent processViewAttachedListeners(FacesContext facesContext,
            List<? extends SystemEventListener> listeners,
            Class<? extends SystemEvent> systemEventClass, Object source,
            SystemEvent event)
    {
        if (listeners == null || listeners.isEmpty())
        {
            return event;
        }

        int processedListenerIndex = 0;

        // Do it with a copy because the list could be changed during a event see MYFACES-2935
        List<SystemEventListener> listenersCopy = new ArrayList<>(listeners);

        // If the inner for is succesful, processedListenerIndex == listenersCopy.size()
        // and the loop will be complete.
        while (processedListenerIndex < listenersCopy.size())
        {                
            for (; processedListenerIndex < listenersCopy.size(); processedListenerIndex++ )
            {
                SystemEventListener listener = listenersCopy.get(processedListenerIndex);
                if (listener.isListenerForSource(source))
                {
                    // Lazy construct the event; zhis same event instance must be passed to all listener instances.
                    if (event == null)
                    {
                        event = createEvent(systemEventClass, facesContext, source);
                    }

                    if (event.isAppropriateListener(listener))
                    {
                        event.processListener(listener);
                    }
                }
            }

            boolean listChanged = false;
            if (listeners.size() == listenersCopy.size())
            {
                for (int i = 0; i < listenersCopy.size(); i++)
                {
                    if (listenersCopy.get(i) != listeners.get(i))
                    {
                        listChanged = true;
                        break;
                    }
                }
            }
            else
            {
                listChanged = true;
            }

            if (listChanged)
            {
                for (int i = 0; i < listeners.size(); i++)
                {
                    SystemEventListener listener = listeners.get(i);

                    // check if listenersCopy.get(i) is valid
                    if (i < listenersCopy.size())
                    {
                        // The normal case is a listener was added, 
                        // so as heuristic, check first if we can find it at the same location
                        if (!listener.equals(listenersCopy.get(i)) &&
                            !listenersCopy.contains(listener))
                        {
                            listenersCopy.add(listener);
                        }
                    }
                    else
                    {
                        if (!listenersCopy.contains(listener))
                        {
                            listenersCopy.add(listener);
                        }
                    }
                }
            }
        }

        return event;
    }
    
    protected SystemEvent processGlobalListeners(FacesContext facesContext, List<EventInfo> eventInfos,
            Class<? extends SystemEvent> systemEventClass, Object source, SystemEvent event, Class<?> sourceBaseType)
    {
        if (eventInfos == null || eventInfos.isEmpty())
        {
            return event;
        }
        
        for (int i  = 0, size = eventInfos.size(); i < size; i++)
        {
            EventInfo eventInfo = eventInfos.get(i);
            if (eventInfo.sourceClass != null && !eventInfo.sourceClass.isAssignableFrom(sourceBaseType))
            {
                continue;
            }

            if (eventInfo.listener.isListenerForSource(source))
            {
                if (event == null)
                {
                    event = createEvent(systemEventClass, facesContext, source);
                }

                if (event.isAppropriateListener(eventInfo.listener))
                {
                    event.processListener(eventInfo.listener);
                }
            }
        }
        
        return event;
    }
}
