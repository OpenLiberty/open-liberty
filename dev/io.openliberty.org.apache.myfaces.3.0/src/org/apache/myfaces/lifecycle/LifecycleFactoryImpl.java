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
package org.apache.myfaces.lifecycle;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.faces.FacesException;
import jakarta.faces.lifecycle.Lifecycle;
import jakarta.faces.lifecycle.LifecycleFactory;

/**
 * @author Manfred Geiler (latest modification by $Author$)
 * @author Anton Koinov
 * @version $Revision$ $Date$
 */
public class LifecycleFactoryImpl extends LifecycleFactory
{
    /**
     * At start we used synchronized blocks for addLifecycle and getLifecycle. But thinking about it,
     * use a ConcurrentHashMap is better, because retrieval operations (including get) generally 
     * do not block, and it is more often retrieval (at begin of all requests) than addition (when 
     * startup listener is called and configuration occur). 
     */
    private final Map<String, Lifecycle> _lifecycles = new ConcurrentHashMap<String, Lifecycle>();

    public LifecycleFactoryImpl()
    {
        addLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE, new LifecycleImpl());
    }

    public void purgeLifecycle()
    {
        // Note this is not safe, because if by some coincidence one thread call getLifecycle between
        // the two lines below it will throw IllegalArgumentException, but this method is not supposed 
        // to be called in production, so it is ok.
        _lifecycles.clear();
        addLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE, new LifecycleImpl());
    }

    @Override
    public void addLifecycle(String id, Lifecycle lifecycle)
    {
        //synchronized (_lifecycles)
        //{
            if (_lifecycles.get(id) != null)
            {
                throw new IllegalArgumentException("Lifecycle with id '" + id + "' already exists.");
            }
            _lifecycles.put(id, lifecycle);
        //}
    }

    @Override
    public Lifecycle getLifecycle(String id) throws FacesException
    {
        //synchronized (_lifecycles)
        //{
            Lifecycle lifecycle = _lifecycles.get(id);
            if (lifecycle == null)
            {
                throw new IllegalArgumentException("Unknown lifecycle '" + id + "'.");
            }
            return lifecycle;
        //}
    }

    @Override
    public Iterator<String> getLifecycleIds()
    {
        return _lifecycles.keySet().iterator();
    }
}
