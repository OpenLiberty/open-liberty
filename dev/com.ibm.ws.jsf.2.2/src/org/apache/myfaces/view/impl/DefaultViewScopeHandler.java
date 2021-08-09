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
package org.apache.myfaces.view.impl;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import org.apache.myfaces.config.ManagedBeanDestroyer;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.config.annotation.LifecycleProvider;
import org.apache.myfaces.config.annotation.LifecycleProviderFactory;
import org.apache.myfaces.shared.util.SubKeyMap;
import org.apache.myfaces.spi.ViewScopeProvider;

/**
 * Minimal implementation for view scope without CDI but always store
 * the beans into session.
 * 
 * @author Leonardo Uribe
 */
public class DefaultViewScopeHandler extends ViewScopeProvider
{
    private static final String VIEW_SCOPE_PREFIX = "oam.view.SCOPE";
    
    private static final String VIEW_SCOPE_PREFIX_KEY = VIEW_SCOPE_PREFIX+".KEY";
    
    private static final String VIEW_SCOPE_PREFIX_MAP = VIEW_SCOPE_PREFIX+".MAP";
    
    static final char SEPARATOR_CHAR = '.';
    
    private final AtomicLong _count;
    
    private ManagedBeanDestroyer _mbDestroyer;
    
    public DefaultViewScopeHandler()
    {
        _count = new AtomicLong(_getSeed());
    }
    
    /**
     * Returns a cryptographically secure random number to use as the _count seed
     */
    private static long _getSeed()
    {
        SecureRandom rng;
        try
        {
            // try SHA1 first
            rng = SecureRandom.getInstance("SHA1PRNG");
        }
        catch (NoSuchAlgorithmException e)
        {
            // SHA1 not present, so try the default (which could potentially not be
            // cryptographically secure)
            rng = new SecureRandom();
        }

        // use 48 bits for strength and fill them in
        byte[] randomBytes = new byte[6];
        rng.nextBytes(randomBytes);

        // convert to a long
        return new BigInteger(randomBytes).longValue();
    }
    
    /**
     * Get the next token to be assigned to this request
     * 
     * @return
     */
    private String _getNextToken()
    {
        // atomically increment the value
        long nextToken = _count.incrementAndGet();

        // convert using base 36 because it is a fast efficient subset of base-64
        return Long.toString(nextToken, 36);
    }
    
    public void onSessionDestroyed()
    {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext.getExternalContext().getSession(false) != null)
        {
            ExternalContext external = facesContext.getExternalContext();
            Map<String, Object> sessionMap = external.getSessionMap();
            String prefix = VIEW_SCOPE_PREFIX_MAP + SEPARATOR_CHAR;
            Set<String> viewScopeIdSet = new HashSet<String>();
            for (Map.Entry<String,Object> entry: sessionMap.entrySet())
            {
                if (entry.getKey() != null && 
                    entry.getKey().startsWith(prefix))
                {
                    String viewScopeId = entry.getKey().substring(prefix.length(), 
                            entry.getKey().indexOf(SEPARATOR_CHAR, prefix.length()));
                    viewScopeIdSet.add(viewScopeId);
                }
            }
            if (!viewScopeIdSet.isEmpty())
            {
                for (String viewScopeId : viewScopeIdSet )
                {
                    this.destroyViewScopeMap(facesContext, viewScopeId);
                }
            }
        }
    }
    
    public Map<String, Object> createViewScopeMap(FacesContext facesContext, String viewScopeId)
    {
        String fullToken = VIEW_SCOPE_PREFIX_MAP + SEPARATOR_CHAR + viewScopeId + SEPARATOR_CHAR;
        Map<String, Object> map = _createSubKeyMap(facesContext, fullToken);
        return map;
    }
    
    public Map<String, Object> restoreViewScopeMap(FacesContext facesContext, String viewScopeId)
    {
        String fullToken = VIEW_SCOPE_PREFIX_MAP + SEPARATOR_CHAR + viewScopeId + SEPARATOR_CHAR;
        Map<String, Object> map = _createSubKeyMap(facesContext, fullToken);
        return map;
    }
    
    private Map<String, Object> _createSubKeyMap(FacesContext context, String prefix)
    {
        ExternalContext external = context.getExternalContext();
        Map<String, Object> sessionMap = external.getSessionMap();

        return new SubKeyMap<Object>(sessionMap, prefix);
    }
    
    public String generateViewScopeId(FacesContext facesContext)
    {
        // To ensure uniqueness in this part we use a counter that 
        // is stored into session and we add a random number to
        // make difficult to guess the next number.
        ExternalContext externalContext = facesContext.getExternalContext();
        Object sessionObj = externalContext.getSession(true);
        Integer sequence = null;
        // synchronized to increase sequence if multiple requests
        // are handled at the same time for the session
        synchronized (sessionObj) 
        {
            Map<String, Object> map = externalContext.getSessionMap();
            sequence = (Integer) map.get(VIEW_SCOPE_PREFIX_KEY);
            if (sequence == null || sequence.intValue() == Integer.MAX_VALUE)
            {
                sequence = Integer.valueOf(1);
            }
            else
            {
                sequence = Integer.valueOf(sequence.intValue());
            }
            map.put(VIEW_SCOPE_PREFIX_KEY, sequence);
        }
        return _getNextToken()+'_'+sequence.toString();
    }

    @Override
    public void destroyViewScopeMap(FacesContext facesContext, String viewScopeId)
    {
        if (facesContext.getExternalContext().getSession(false) != null)
        {        
            String fullToken = VIEW_SCOPE_PREFIX_MAP + SEPARATOR_CHAR + viewScopeId + SEPARATOR_CHAR;
            Map<String, Object> map = _createSubKeyMap(facesContext, fullToken);
            
            ManagedBeanDestroyer mbDestroyer = getManagedBeanDestroyer(facesContext.getExternalContext());
            for (Map.Entry<String,Object> entry : map.entrySet())
            {
                mbDestroyer.destroy(entry.getKey(), entry.getValue());
            }
            
            map.clear();
        }
    }
    
    protected ManagedBeanDestroyer getManagedBeanDestroyer(ExternalContext externalContext)
    {
        if (_mbDestroyer == null)
        {
            RuntimeConfig runtimeConfig = RuntimeConfig.getCurrentInstance(externalContext);
            LifecycleProvider lifecycleProvider = LifecycleProviderFactory
                    .getLifecycleProviderFactory(externalContext).getLifecycleProvider(externalContext);

            _mbDestroyer = new ManagedBeanDestroyer(lifecycleProvider, runtimeConfig);
        }
        return _mbDestroyer;
    }
}
