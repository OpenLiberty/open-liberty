/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.db;

import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ServletContext;
import javax.transaction.UserTransaction;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.session.MemoryStoreHelper;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.store.common.LoggingUtil;
import com.ibm.ws.session.utils.SessionLoader;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.resource.ResourceConfigFactory;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.session.IStore;

/**
 * Constructs DatabaseStore instances.
 */
public class DatabaseStoreService implements SessionStoreService {

    private static final String methodClassName = DatabaseStoreService.class.getSimpleName();

    // Since there's at most one DataStoreService defined in the server, it's okay to use static references for now
    private Map<String, Object> configurationProperties = null;
    private final AtomicServiceReference<EmbeddableWebSphereTransactionManager> embeddableWebSphereTransactionManagerRef = new AtomicServiceReference<EmbeddableWebSphereTransactionManager>("embeddableWebSphereTransactionManager");
    private final AtomicServiceReference<LocalTransactionCurrent> localTransactionCurrentRef = new AtomicServiceReference<LocalTransactionCurrent>("localTransactionCurrent");
    private final AtomicServiceReference<ResourceConfigFactory> resourceConfigFactoryRef = new AtomicServiceReference<ResourceConfigFactory>("resourceConfigFactory");
    private final AtomicServiceReference<ResourceFactory> dataSourceFactoryRef = new AtomicServiceReference<ResourceFactory>("dataSourceFactory");
    private final AtomicServiceReference<UOWCurrent> uowCurrentRef = new AtomicServiceReference<UOWCurrent>("uowCurrent");
    private final AtomicServiceReference<UserTransaction> userTransactionRef = new AtomicServiceReference<UserTransaction>("userTransaction");
    private final AtomicServiceReference<SerializationService> serializationServiceRef = new AtomicServiceReference<SerializationService>("serializationService");

    private static boolean completedPassivation = true; // 128284

    public ResourceFactory getDataSourceFactory() {
        return this.dataSourceFactoryRef.getService();
    }

    public LocalTransactionCurrent getLocalTransactionCurrent() {
        return this.localTransactionCurrentRef.getService();
    }

    public ResourceConfigFactory getResourceConfigFactory() {
        return this.resourceConfigFactoryRef.getService();
    }

    public SerializationService getSerializationService() {
        return this.serializationServiceRef.getService();
    }

    public static synchronized void setCompletedPassivation(boolean isInProcessOfStopping) { // 128284
        completedPassivation = isInProcessOfStopping;
    }

    public static synchronized boolean isCompletedPassivation() { // 128284
        return completedPassivation;
    }

    /**
     * Get the current unit of work.
     *
     * @return the current unit of work. Null if not available.
     */
    public UOWCurrent getUOWCurrent() {
        return this.uowCurrentRef.getService();
    }

    public UserTransaction getUserTransaction() {
        return this.userTransactionRef.getService();
    }

    public EmbeddableWebSphereTransactionManager getEmbeddableWebSphereTransactionManager() {
        return this.embeddableWebSphereTransactionManagerRef.getService();
    }

    @Override
    public IStore createStore(SessionManagerConfig smc, String smid, ServletContext sc, MemoryStoreHelper storeHelper, ClassLoader classLoader, boolean applicationSessionStore) {
        IStore store = new DatabaseStore(smc, smid, sc, storeHelper, applicationSessionStore, this);
        store.setLoader(new SessionLoader(serializationServiceRef.getServiceWithException(), classLoader, applicationSessionStore));
        setCompletedPassivation(false);
        return store;
    }

    @Override
    public boolean isValid() {
        //        if(configurationProperties==null) {
        //            return false;
        //        }
        //        Enumeration keys = configurationProperties.keys();
        //        while(keys.hasMoreElements()) {
        //            Object key = keys.nextElement();
        //            if("dataSourceRef".equals(key)) {
        //                Object value = configurationProperties.get(key);
        //                if(value==null) {
        //                    return false;
        //                }
        //                return !value.toString().trim().isEmpty();
        //            }
        //        }
        //        return false;
        return true; // since the dataSourceService is required, declarative services will not activate this service unless the config and dataSourceService are satisfied
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return configurationProperties;
    }

    /**
     * Declarative Services method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     */
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "activate", "context=" + context);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "activate", "properties=" + properties);
        }
        embeddableWebSphereTransactionManagerRef.activate(context);
        localTransactionCurrentRef.activate(context);
        resourceConfigFactoryRef.activate(context);
        dataSourceFactoryRef.activate(context);
        uowCurrentRef.activate(context);
        userTransactionRef.activate(context);
        serializationServiceRef.activate(context);
        configurationProperties = properties;

    }

    /**
     * Declarative Services method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "deactivate", "deactivating service references");
        }
        embeddableWebSphereTransactionManagerRef.deactivate(context);
        localTransactionCurrentRef.deactivate(context);
        resourceConfigFactoryRef.deactivate(context);
        dataSourceFactoryRef.deactivate(context);
        uowCurrentRef.deactivate(context);
        userTransactionRef.deactivate(context);
        if (isCompletedPassivation()) { //START 128284
            serializationServiceRef.deactivate(context);
        } else {

            while (!isCompletedPassivation()){
                try {
                    Thread.sleep(100L); // sleep 1/10th of a second
                } catch (InterruptedException e) {
                    FFDCFilter.processException(e, this.getClass().getName(), "180");
                } finally {
                    serializationServiceRef.deactivate(context);
                }
            }
        } // END 128284

    }

    /**
     * Called by Declarative Services to modify service config properties
     *
     * @param context for this component instance
     */
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "modified", "context=" + context);
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "modified", "properties=" + properties);
        }
        configurationProperties = properties;
    }

    /**
     * Declarative Services method for setting the data source service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void setDataSourceFactory(ServiceReference<ResourceFactory> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "setDataSourceFactory", "setting " + ref);
        }
        dataSourceFactoryRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the data source service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void unsetDataSourceFactory(ServiceReference<ResourceFactory> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "unsetDataSourceFactory", "unsetting " + ref);
        }
        dataSourceFactoryRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the ResourceConfigFactory service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void setResourceConfigFactory(ServiceReference<ResourceConfigFactory> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "setResourceConfigFactory", "setting " + ref);
        }
        resourceConfigFactoryRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the ResourceConfigFactory service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void unsetResourceConfigFactory(ServiceReference<ResourceConfigFactory> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "unsetResourceConfigFactory", "unsetting " + ref);
        }
        resourceConfigFactoryRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the LocalTransactionCurrent service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void setLocalTransactionCurrent(ServiceReference<LocalTransactionCurrent> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "setLocalTransactionCurrent", "setting " + ref);
        }
        localTransactionCurrentRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the LocalTransactionCurrent service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void unsetLocalTransactionCurrent(ServiceReference<LocalTransactionCurrent> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "unsetLocalTransactionCurrent", "unsetting " + ref);
        }
        localTransactionCurrentRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the EmbeddableWebSphereTransactionManager service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void setEmbeddableWebSphereTransactionManager(ServiceReference<EmbeddableWebSphereTransactionManager> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "setEmbeddableWebSphereTransactionManager", "setting " + ref);
        }
        embeddableWebSphereTransactionManagerRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the EmbeddableWebSphereTransactionManager service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void unsetEmbeddableWebSphereTransactionManager(ServiceReference<EmbeddableWebSphereTransactionManager> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "unsetEmbeddableWebSphereTransactionManager", "unsetting " + ref);
        }
        embeddableWebSphereTransactionManagerRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the UOWCurrent service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void setUowCurrent(ServiceReference<UOWCurrent> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "setUowCurrent", "setting " + ref);
        }
        uowCurrentRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the UOWCurrent service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void unsetUowCurrent(ServiceReference<UOWCurrent> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "unsetUowCurrent", "unsetting " + ref);
        }
        uowCurrentRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the UserTransaction service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void setUserTransaction(ServiceReference<UserTransaction> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "setUserTransaction", "setting " + ref);
        }
        userTransactionRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the UserTransaction service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void unsetUserTransaction(ServiceReference<UserTransaction> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "unsetUserTransaction", "unsetting " + ref);
        }
        userTransactionRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for setting the SerializationService service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void setSerializationService(ServiceReference<SerializationService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "setSerializationService", "setting " + ref);
        }
        serializationServiceRef.setReference(ref);
    }

    /**
     * Declarative Services method for unsetting the SerializationService service reference
     *
     * @param ref reference to service object; type of service object is verified
     */
    protected void unsetSerializationService(ServiceReference<SerializationService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, "unsetSerializationService", "unsetting " + ref);
        }
        serializationServiceRef.unsetReference(ref);
    }
}
