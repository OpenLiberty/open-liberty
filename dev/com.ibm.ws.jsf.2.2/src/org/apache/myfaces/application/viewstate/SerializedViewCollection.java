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
package org.apache.myfaces.application.viewstate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import org.apache.commons.collections.map.LRUMap;
import org.apache.myfaces.shared.config.MyfacesConfig;
import org.apache.myfaces.spi.ViewScopeProvider;

/**
 *
 */
class SerializedViewCollection implements Serializable
{
    private static final Logger log = Logger.getLogger(SerializedViewCollection.class.getName());

    private static final Object[] EMPTY_STATES = new Object[]{null, null};

    private static final long serialVersionUID = -3734849062185115847L;
    private final List<SerializedViewKey> _keys = 
        new ArrayList<SerializedViewKey>(
            MyfacesConfig.INIT_PARAM_NUMBER_OF_VIEWS_IN_SESSION_DEFAULT);
    private final Map<SerializedViewKey, Object> _serializedViews = 
        new HashMap<SerializedViewKey, Object>();
    /**
     * The viewScopeIds can be shared between multiple entries of the same
     * view. To store it into session, the best is use two maps, one to 
     * associate the view key with the view scope id and other to keep track 
     * of the number of times the id is used. In that way it is possible to
     * know when a view scope id has been discarded and destroy the view scope
     * in the right time.
     */
    private HashMap<SerializedViewKey, String> _viewScopeIds = null;
    private HashMap<String, Integer> _viewScopeIdCounts = null;

    private final Map<SerializedViewKey, SerializedViewKey> _precedence =
        new HashMap<SerializedViewKey, SerializedViewKey>();
    private Map<String, SerializedViewKey> _lastWindowKeys = null;

    public void put(FacesContext context, Object state, 
        SerializedViewKey key, SerializedViewKey previousRestoredKey)
    {
        put(context, state, key, previousRestoredKey, null, null);
    }
    
    public synchronized void put(FacesContext context, Object state, 
        SerializedViewKey key, SerializedViewKey previousRestoredKey,
        ViewScopeProvider viewScopeProvider, String viewScopeId)
    {
        if (state == null)
        {
            state = EMPTY_STATES;
        }
        else if (state instanceof Object[] &&
            ((Object[])state).length == 2 &&
            ((Object[])state)[0] == null &&
            ((Object[])state)[1] == null)
        {
            // The generated state can be considered zero, set it as null
            // into the map.
            state = null;
        }

        if (_serializedViews.containsKey(key))
        {
            // Update the state, the viewScopeId does not change.
            _serializedViews.put(key, state);
            // Make sure the view is at the end of the discard queue
            while (_keys.remove(key))
            {
                // do nothing
            }
            _keys.add(key);
            return;
        }

        Integer maxCount = getNumberOfSequentialViewsInSession(context);
        if (maxCount != null)
        {
            if (previousRestoredKey != null)
            {
                if (!_serializedViews.isEmpty())
                {
                    _precedence.put((SerializedViewKey) key, previousRestoredKey);
                }
                else
                {
                    // Note when the session is invalidated, _serializedViews map is empty,
                    // but we could have a not null previousRestoredKey (the last one before
                    // invalidate the session), so we need to check that condition before
                    // set the precence. In that way, we ensure the precedence map will always
                    // have valid keys.
                    previousRestoredKey = null;
                }
            }
        }
        _serializedViews.put(key, state);
        
        if (viewScopeProvider != null && viewScopeId != null)
        {
            if (_viewScopeIds == null)
            {
                _viewScopeIds = new HashMap<SerializedViewKey, String>();
            }
            _viewScopeIds.put(key, viewScopeId);
            if (_viewScopeIdCounts == null)
            {
                _viewScopeIdCounts = new HashMap<String, Integer>();
            }
            Integer vscount = _viewScopeIdCounts.get(viewScopeId);
            vscount = (vscount == null) ? 1 : vscount + 1;
            _viewScopeIdCounts.put(viewScopeId, vscount);
        }

        while (_keys.remove(key))
        {
            // do nothing
        }
        _keys.add(key);

        if (previousRestoredKey != null && maxCount != null && maxCount > 0)
        {
            int count = 0;
            SerializedViewKey previousKey = (SerializedViewKey) key;
            do
            {
                previousKey = _precedence.get(previousKey);
                count++;
            }
            while (previousKey != null && count < maxCount);

            if (previousKey != null)
            {
                SerializedViewKey keyToRemove = (SerializedViewKey) previousKey;
                // In theory it should be only one key but just to be sure
                // do it in a loop, but in this case if cache old views is on,
                // put on that map.
                do
                {
                    while (_keys.remove(keyToRemove))
                    {
                        // do nothing
                    }

                    _serializedViews.remove(keyToRemove);
                    
                    if (viewScopeProvider != null && _viewScopeIds != null)
                    {
                        String oldViewScopeId = _viewScopeIds.remove(keyToRemove);
                        if (oldViewScopeId != null)
                        {
                            Integer vscount = _viewScopeIdCounts.get(oldViewScopeId);
                            vscount = vscount - 1;
                            if (vscount != null && vscount.intValue() < 1)
                            {
                                _viewScopeIdCounts.remove(oldViewScopeId);
                                viewScopeProvider.destroyViewScopeMap(context, oldViewScopeId);
                            }
                            else
                            {
                                _viewScopeIdCounts.put(oldViewScopeId, vscount);
                            }
                        }
                    }

                    keyToRemove = _precedence.remove(keyToRemove);
                }
                while (keyToRemove != null);
            }
        }
        int views = getNumberOfViewsInSession(context);
        while (_keys.size() > views)
        {
            key = _keys.remove(0);
            if (maxCount != null && maxCount > 0)
            {
                SerializedViewKey keyToRemove = (SerializedViewKey) key;
                // Note in this case the key to delete is the oldest one,
                // so it could be at least one precedence, but to be safe
                // do it with a loop.
                do
                {
                    keyToRemove = _precedence.remove(keyToRemove);
                }
                while (keyToRemove != null);
            }

            _serializedViews.remove(key);
            
            if (viewScopeProvider != null && _viewScopeIds != null)
            {
                String oldViewScopeId = _viewScopeIds.remove(key);
                if (oldViewScopeId != null)
                {
                    Integer vscount = _viewScopeIdCounts.get(oldViewScopeId);
                    vscount = vscount - 1;
                    if (vscount != null && vscount.intValue() < 1)
                    {
                        _viewScopeIdCounts.remove(oldViewScopeId);
                        viewScopeProvider.destroyViewScopeMap(context, oldViewScopeId);
                    }
                    else
                    {
                        _viewScopeIdCounts.put(oldViewScopeId, vscount);
                    }
                }
            }
        }
    }

    protected Integer getNumberOfSequentialViewsInSession(FacesContext context)
    {
        return MyfacesConfig.getCurrentInstance(context.getExternalContext()).getNumberOfSequentialViewsInSession();
    }

    /**
     * Reads the amount (default = 20) of views to be stored in session.
     * @see ServerSideStateCacheImpl#NUMBER_OF_VIEWS_IN_SESSION_PARAM
     * @param context FacesContext for the current request, we are processing
     * @return Number vf views stored in the session
     */
    protected int getNumberOfViewsInSession(FacesContext context)
    {
        return MyfacesConfig.getCurrentInstance(context.getExternalContext()).getNumberOfViewsInSession();
    }

    public synchronized void putLastWindowKey(FacesContext context, String id, SerializedViewKey key)
    {
        if (_lastWindowKeys == null)
        {
            Integer i = getNumberOfSequentialViewsInSession(context);
            int j = getNumberOfViewsInSession(context);
            if (i != null && i.intValue() > 0)
            {
                _lastWindowKeys = new LRUMap((j / i.intValue()) + 1);
            }
            else
            {
                _lastWindowKeys = new LRUMap(j + 1);
            }
        }
        _lastWindowKeys.put(id, key);
    }

    public SerializedViewKey getLastWindowKey(FacesContext context, String id)
    {
        if (_lastWindowKeys != null)
        {
            return _lastWindowKeys.get(id);
        }
        return null;
    }

    public Object get(SerializedViewKey key)
    {
        Object value = _serializedViews.get(key);
        if (value == null)
        {
            if (_serializedViews.containsKey(key))
            {
                return EMPTY_STATES;
            }
        }
        else if (value instanceof Object[] &&
            ((Object[])value).length == 2 &&
            ((Object[])value)[0] == null &&
            ((Object[])value)[1] == null)
        {
            // Remember inside the state map null is stored as an empty array.
            return null;
        }
        return value;
    }
}
