/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.cache.AuthCacheConfig;
import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheEvictionListener;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.cache.CacheObject;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.notifications.SecurityChangeListener;
import com.ibm.ws.security.registry.UserRegistryChangeListener;
import com.ibm.ws.security.util.ByteArray;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Implements the authentication cache.
 */
public class AuthCacheServiceImpl implements AuthCacheService, UserRegistryChangeListener, SecurityChangeListener {

    protected static final String KEY_CREDENTIAL_SERVICE = "credentialService";

    private static final TraceComponent tc = Tr.register(AuthCacheServiceImpl.class);

    private Cache cache;
    private final Set<CacheKeyProvider> cacheKeyProviders = new HashSet<CacheKeyProvider>();
    private boolean allowBasicAuthLookup = true;
    private int initialSize = 50;
    private int maxSize = 25000;
    private long timeoutInMilliSeconds = 600000L;

    private AuthCacheConfig authCacheConfig;
    private final Set<CacheEvictionListener> cacheEvictionListenerSet = new HashSet<CacheEvictionListener>();
    private final AtomicServiceReference<CredentialsService> credServiceRef = new AtomicServiceReference<CredentialsService>(KEY_CREDENTIAL_SERVICE);

    /** {@inheritDoc} */
    @FFDCIgnore(Exception.class)
    @Override
    public void insert(Subject subject, String userid, @Sensitive String password) {
        try {
            CacheObject cacheObject = new CacheObject(subject);
            CacheContext cacheContext = new CacheContext(authCacheConfig, cacheObject, userid, password);
            commonInsert(cacheContext, cacheObject);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was a problem caching the subject.", e);
            }
        }
    }

    /** {@inheritDoc} */
    @FFDCIgnore(Exception.class)
    @Override
    public void insert(Subject subject, X509Certificate[] certChain) {
        try {
            CacheObject cacheObject = new CacheObject(subject);
            CacheContext cacheContext = new CacheContext(authCacheConfig, cacheObject, certChain);
            commonInsert(cacheContext, cacheObject);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was a problem caching the subject.", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @FFDCIgnore(Exception.class)
    @Override
    public void insert(Subject subject) {
        try {
            CacheObject cacheObject = new CacheObject(subject);
            CacheContext cacheContext = new CacheContext(authCacheConfig, cacheObject);
            commonInsert(cacheContext, cacheObject);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was a problem caching the subject.", e);
            }
        }
    }

    private void commonInsert(CacheContext cacheContext, CacheObject cacheObject) throws Exception {
        for (CacheKeyProvider provider : cacheKeyProviders) {
            Object cacheKey = provider.provideKey(cacheContext);
            if (cacheKey instanceof Set<?>) {
                for (Object key : (Set<?>) cacheKey) {
                    addCacheObject(key, cacheObject);
                }
            } else if (cacheKey != null) {
                addCacheObject(cacheKey, cacheObject);
            }
        }
    }

    private void addCacheObject(Object key, CacheObject cacheObject) {
        cacheObject.addLookupKey(key);
        cache.insert(key, cacheObject);
    }

    /** {@inheritDoc} */
    @Override
    public Subject getSubject(Object cacheKey) {
        Subject subject = null;
        if (cacheKey != null) {
            CacheObject cacheObject = getCachedObject(cacheKey);
            subject = cacheObject != null ? cacheObject.getSubject() : null;
            subject = optionallyRemoveEntryForInvalidSubject(cacheObject, subject);
        }
        return subject;
    }

    private Subject optionallyRemoveEntryForInvalidSubject(CacheObject cacheObject, Subject subject) {
        if (subject != null) {
            CredentialsService credService = credServiceRef.getService();
            if (credService != null && !credService.isSubjectValid(subject)) {
                removeCachedObject(cacheObject);
                subject = null;
            }
        }
        return subject;
    }

    /** {@inheritDoc} */
    @Override
    public void remove(Object cacheKey) {
        if (cacheKey != null) {
            CacheObject cacheObject = getCachedObject(cacheKey);
            if (cacheObject != null) {
                removeCachedObject(cacheObject);
            }
        }
    }

    private void removeCachedObject(CacheObject cacheObject) {
        List<Object> lookupKeys = cacheObject.getLookupKeys();
        synchronized (lookupKeys) {
            for (Object lookupKey : lookupKeys) {
                cache.remove(lookupKey);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeAllEntries() {
        cache.clearAllEntries();
    }

    protected CacheObject getCachedObject(Object cacheKey) {
        if (cacheKey instanceof byte[]) {
            cacheKey = new ByteArray((byte[]) cacheKey);
        }
        return (CacheObject) cache.get(cacheKey);
    }

    protected void activate(ComponentContext componentContext, Map<String, Object> newProperties) {
        credServiceRef.activate(componentContext);
        modified(newProperties);
    }

    protected void modified(Map<String, Object> newProperties) {
        initialSize = (Integer) newProperties.get("initialSize");
        maxSize = (Integer) newProperties.get("maxSize");
        timeoutInMilliSeconds = (Long) newProperties.get("timeout");
        allowBasicAuthLookup = (Boolean) newProperties.get("allowBasicAuthLookup");
        if (initialSize > maxSize) {
            initialSize = maxSize;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The initial size of cache is greater than the maximum size, so resetting the initial size to maximum size " + initialSize);
            }
        }
        authCacheConfig = new AuthCacheConfigImpl(initialSize, maxSize, timeoutInMilliSeconds, allowBasicAuthLookup);
        stopCacheEvictionTask();
        cache = new Cache(initialSize, maxSize, timeoutInMilliSeconds, cacheEvictionListenerSet);
    }

    protected void deactivate(ComponentContext componentContext) {
        stopCacheEvictionTask();
        credServiceRef.deactivate(componentContext);
    }

    private void stopCacheEvictionTask() {
        if (cache != null) {
            cache.stopEvictionTask();
        }
    }

    protected void setCacheKeyProvider(CacheKeyProvider provider) {
        cacheKeyProviders.add(provider);
    }

    protected void unsetCacheKeyProvider(CacheKeyProvider provider) {
        cacheKeyProviders.remove(provider);
    }

    protected void setCacheEvictionListener(CacheEvictionListener cacheEvictionListener) {
        cacheEvictionListenerSet.add(cacheEvictionListener);
    }

    protected void unsetCacheEvictionListener(CacheEvictionListener cacheEvictionListener) {
        cacheEvictionListenerSet.remove(cacheEvictionListener);
    }

    protected void setCredentialService(ServiceReference<CredentialsService> reference) {
        credServiceRef.setReference(reference);
    }

    protected void unsetCredentialService(ServiceReference<CredentialsService> reference) {
        credServiceRef.unsetReference(reference);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyOfUserRegistryChange() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Clearing auth cache as UserRegistry configuration has changed.");
        }
        removeAllEntries();
    }

    /** {@inheritDoc} */
    @Override
    public void notifyChange() {
        removeAllEntries();
    }

}
