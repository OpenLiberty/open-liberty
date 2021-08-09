/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import java.util.concurrent.ScheduledExecutorService;

import com.ibm.ws.ejbcontainer.EJBPMICollaborator;

/**
 * A PoolManager is a factory and manager for pools of object instances.
 * 
 * Implementations of Pools created by this PoolManager are expected to
 * allow the pool to grow to maxPoolSize and periodically shrink it back
 * to minPoolSize.
 * 
 * @see Pool
 * @see PooledObject
 * @see PoolDiscardStrategy
 * @see com.ibm.websphere.pmi.PmiBean
 */
public abstract class PoolManager {

    /**
     * Create a PoolManager to manage one or more pools. Each PoolManager may be
     * set to use a unique drain interval and other behavioral parameters for the pools
     * it manages.
     */
    public static PoolManager newInstance()
    {
        return new PoolManagerImpl(); // F743-33394
    }

    /**
     * Set the interval for invoking the pool's draining algorithm. The actual
     * algorithm for shrinking the pool is dependent on the pool's implementation.
     * 
     * @param di The interval in milliseconds.
     */
    public abstract void setDrainInterval(long di);

    /**
     * Set the ScheduledExecutorService instance for the EJB container.
     * 
     * @param scheduledExecutor The scheduled executor service for timers.
     */
    public abstract void setScheduledExecutorService(ScheduledExecutorService scheduledExecutor);

    /**
     * Create a thread-safe Pool with no PMI notifications.
     * 
     * @param minPoolSize minimum pool size
     * @param maxPoolSize maximum pool size
     * @return the newly created pool.
     */
    public abstract Pool createThreadSafePool(int minPoolSize, int maxPoolSize);

    /**
     * Create a thread-safe Pool with PMI notifications.
     * 
     * @param minPoolSize minimum pool size
     * @param maxPoolSize maximum pool size
     * @param beanPerfData interface for recording pool stats
     * @return the newly created pool.
     */
    public abstract Pool createThreadSafePool(int minPoolSize, int maxPoolSize, EJBPMICollaborator beanPerfData);

    /**
     * Create a thread-safe Pool with PMI and DiscardStrategy functionality.
     * 
     * <p><b>This method is deprecated.
     * Use the PooledObject interface and createPool(min, max, PmiBean) method instead.</b>
     * 
     * @param minPoolSize minimum pool size
     * @param maxPoolSize maximum pool size
     * @param beanPerfData interface for recording pool stats
     * @discardStrategy to be invoked when object is removed from the pool.
     * @return the newly created pool.
     */
    public abstract Pool create(int minPoolSize, int maxPoolSize, EJBPMICollaborator beanPerfData,
                                PoolDiscardStrategy discardStrategy);

    /**
     * Cancels the pool manager - stops it from scheduling the alarm for draining.
     * d583637
     */
    public abstract void cancel();

} // PoolManager

