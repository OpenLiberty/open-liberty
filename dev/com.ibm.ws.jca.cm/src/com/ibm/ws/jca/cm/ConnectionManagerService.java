/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.cm;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

import com.ibm.ejs.j2c.J2CConstants;
import com.ibm.ejs.j2c.ConnectionManagerServiceImpl;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * Connection manager service.
 */
public abstract class ConnectionManagerService extends Observable {

    /**
     * Name of element used for data source configuration.
     */
    public static final String CONNECTION_MANAGER = "connectionManager";

    /**
     * Factory persistent identifier for ConnectionManagerService.
     */
    public final static String FACTORY_PID = "com.ibm.ws.jca.connectionManager";

    /**
     * Name of unique identifier property.
     */
    public static final String ID = "id";

    /**
     * Name of property controlling the maximum number of open connections per thread.
     */
    public static final String MAX_CONNECTIONS_PER_THREAD = "maxConnectionsPerThread";

    /**
     * Name of property that controls the amount of time after which an unused or idle connection
     * can be discarded during pool maintenance, if doing so does not reduce the pool below the minimum size.
     */
    public static final String MAX_IDLE_TIME = "maxIdleTime"; // Name taken from @DataSourceDefinition

    /**
     * Name of property that controls the upper limit on connection pool size.
     */
    public static final String MAX_POOL_SIZE = "maxPoolSize"; // Name taken from @DataSourceDefinition

    /**
     * Name of property that controls the minimum pool size.
     */
    public static final String MIN_POOL_SIZE = "minPoolSize"; // Name taken from @DataSourceDefinition

    /**
     * Name of property controlling the number of connections to cache per thread local.
     */
    public static final String NUM_CONNECTIONS_PER_THREAD_LOCAL = "numConnectionsPerThreadLocal";

    /**
     * List of connectionManager properties.
     */
    public static final List<String> CONNECTION_MANAGER_PROPS =
                    Collections.unmodifiableList(Arrays.asList(
                                                               J2CConstants.POOL_AgedTimeout,
                                                               J2CConstants.POOL_ConnectionTimeout,
                                                               MAX_IDLE_TIME,
                                                               MAX_CONNECTIONS_PER_THREAD,
                                                               MAX_POOL_SIZE,
                                                               MIN_POOL_SIZE,
                                                               NUM_CONNECTIONS_PER_THREAD_LOCAL,
                                                               J2CConstants.POOL_PurgePolicy,
                                                               J2CConstants.POOL_ReapTime
                                    ));

    /**
     * Create a connection manager service which is not registered in the OSGI service registry.
     * 
     * @param name name of the connection factory that is creating the default connectionManager service.
     * @return connection manager service.
     */
    public static final ConnectionManagerService createDefaultService(String name) {
        return new ConnectionManagerServiceImpl(name);
    }

    /**
     * Removes and destroys all connection factories for this service.
     */
    public abstract void destroyConnectionFactories();

    /**
     * Returns the connection manager for this configuration.
     * This method lazily initializes the connection manager service if necessary.
     * 
     * @param ref reference information, or null for a direct lookup
     * @param svc the connection factory service
     * @return the connection manager for this configuration.
     * @throws ResourceException if an error occurs
     */
    public abstract ConnectionManager getConnectionManager(ResourceInfo refInfo, AbstractConnectionFactoryService svc) throws ResourceException;

    /**
     * Returns the resource adapter classloader.
     */
    public abstract void addRaClassLoader(ClassLoader raClassLoader);

}
