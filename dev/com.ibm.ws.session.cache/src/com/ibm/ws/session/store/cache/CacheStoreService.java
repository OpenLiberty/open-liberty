/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.cache;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.transaction.UserTransaction;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.utils.SessionLoader;
import com.ibm.wsspi.session.IStore;

/**
 * Constructs CacheStore instances.
 */
@Component(name = "com.ibm.ws.session.cache", configurationPolicy = ConfigurationPolicy.OPTIONAL, service = { SessionStoreService.class })
public class CacheStoreService implements SessionStoreService {
    private Map<String, Object> configurationProperties;

    private volatile boolean completedPassivation = true;

    @Reference
    protected SerializationService serializationService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected volatile UserTransaction userTransaction;

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     * @param props service properties
     */
    protected void activate(ComponentContext context, Map<String, Object> props) {
        configurationProperties = props;
    }

    @Override
    public IStore createStore(SessionManagerConfig smc, String smid, ServletContext sc, MemoryStoreHelper storeHelper, ClassLoader classLoader, boolean applicationSessionStore) {
        IStore store = new CacheStore(smc, smid, sc, storeHelper, applicationSessionStore, this);
        store.setLoader(new SessionLoader(serializationService, classLoader, applicationSessionStore));
        setCompletedPassivation(false);
        return store;
    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     */
    protected void deactivate(ComponentContext context) {
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return configurationProperties;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void setCompletedPassivation(boolean isInProcessOfStopping) {
        completedPassivation = isInProcessOfStopping;
    }
}
