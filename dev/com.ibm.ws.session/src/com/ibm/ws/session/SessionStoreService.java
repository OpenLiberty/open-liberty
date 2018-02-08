/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import java.util.Map;

import javax.servlet.ServletContext;

import com.ibm.wsspi.session.IStore;

/**
 * Constructs IStore instances
 */
public interface SessionStoreService {

    /**
     * Constructs a new store instance for the session manager to use for persistence
     * 
     * @param smc the session manager configuration
     * @param smid the store ID
     * @param sc the Servlet context
     * @param storeHelper the memory store helper
     * @param classLoader the application classloader
     * @param applicationSessionStore
     * @return the store to use for session persistence, or null if the default store should be used
     */
    public IStore createStore(SessionManagerConfig smc, String smid, ServletContext sc, MemoryStoreHelper storeHelper, ClassLoader classLoader, boolean applicationSessionStore);

    /**
     * <p>
     * Reads service configuration to determine whether it is valid.
     * </p>
     * <p>Consider the case where a user specifies the sessionDatabase-1.0 feature in server.xml,
     * but does not specify a databaseStore configuration. In this case, the user may
     * intentionally want to start the server with the sessionDatabase-1.0 bundles,
     * but use the (default) session memory store. Since declarative services will
     * provide the DatabaseStoreService regardless, we need to check if the user actually
     * attempted to define a databaseStore configuration. If no configuration is provided,
     * notify the session manager that the default store should be used.
     * </p>
     * 
     * @return true if the configuration of this service is valid
     */
    public boolean isValid();

    /**
     * To maintain compatibility with tWAS,
     * we need to initialize the server-level SessionManagerConfig instance
     * with configuration properties from the underlying store.
     * This method returns the store's configuration properties.
     * 
     * @return configuration properties of the store
     */
    public Map<String, Object> getConfiguration();

    /**
     * Invoked to indicate whether or not the session store is in the process of stopping.
     * 
     * @param isInProcessOfStopping
     */
    public void setCompletedPassivation(boolean isInProcessOfStopping);
}
