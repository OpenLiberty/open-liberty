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

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.component.UIViewRoot;
import javax.faces.event.PreDestroyCustomScopeEvent;
import javax.faces.event.PreDestroyViewMapEvent;
import javax.faces.event.ScopeContext;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

import org.apache.myfaces.config.annotation.LifecycleProvider;

/**
 * Destroyes managed beans with the current LifecycleProvider.
 * This guarantees the invocation of the @PreDestroy methods.
 * @author Jakob Korherr (latest modification by $Author: lu4242 $)
 * @version $Revision: 1532606 $ $Date: 2013-10-16 00:06:54 +0000 (Wed, 16 Oct 2013) $
 * @since 2.0
 */
public class ManagedBeanDestroyer implements SystemEventListener
{
    
    private static Logger log = Logger.getLogger(ManagedBeanDestroyer.class.getName());
    
    private RuntimeConfig _runtimeConfig;
    private LifecycleProvider _lifecycleProvider;

    /**
     * Creates the ManagedBeanDestroyer for the given RuntimeConfig
     * and LifecycleProvider.
     * 
     * @param lifecycleProvider
     * @param runtimeConfig
     */
    public ManagedBeanDestroyer(LifecycleProvider lifecycleProvider,
                                RuntimeConfig runtimeConfig)
    {
        _lifecycleProvider = lifecycleProvider;
        _runtimeConfig = runtimeConfig;
    }

    public boolean isListenerForSource(Object source)
    {
        // source of PreDestroyCustomScopeEvent is ScopeContext
        // and source of PreDestroyViewMapEvent is UIViewRoot
        return (source instanceof ScopeContext) || (source instanceof UIViewRoot);
    }

    /**
     * Listens to PreDestroyCustomScopeEvent and PreDestroyViewMapEvent
     * and invokes destroy() for every managed bean in the associated scope.
     */
    public void processEvent(SystemEvent event)
    {
        Map<String, Object> scope = null;
        
        if (event instanceof PreDestroyViewMapEvent)
        {
            UIViewRoot viewRoot = (UIViewRoot) ((PreDestroyViewMapEvent) event).getComponent();
            scope = viewRoot.getViewMap(false);
            if (scope == null)
            {
                // view map does not exist --> nothing to destroy
                return;
            }
        }
        else if (event instanceof PreDestroyCustomScopeEvent)
        {
            ScopeContext scopeContext = ((PreDestroyCustomScopeEvent) event).getContext();
            scope = scopeContext.getScope();
        }
        else
        {
            // wrong event
            return;
        }
        
        if (!scope.isEmpty())
        {
            Set<String> keySet = scope.keySet();
            String[] keys = keySet.toArray(new String[keySet.size()]);
            
            for (String key : keys)
            {
                Object value = scope.get(key);
                this.destroy(key, value);
            }
        }
    }
    
    /**
     * Checks if the given managed bean exists in the RuntimeConfig.
     * @param name
     * @return
     */
    public boolean isManagedBean(String name)
    {
        return (_runtimeConfig.getManagedBean(name) != null);
    }
    
    /**
     * Destroys the given managed bean.
     * @param name
     * @param instance
     */
    public void destroy(String name, Object instance)
    {
        if (instance != null && isManagedBean(name))
        {
            try
            {
                _lifecycleProvider.destroyInstance(instance);
            } 
            catch (IllegalAccessException e)
            {
                log.log(Level.SEVERE, "Could not access @PreDestroy method of managed bean " + name, e);
            } 
            catch (InvocationTargetException e)
            {
                log.log(Level.SEVERE, "An Exception occured while invoking " +
                        "@PreDestroy method of managed bean " + name, e);
            }
        }
    }

}
