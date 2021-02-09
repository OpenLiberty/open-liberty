/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.core;

import java.util.logging.Logger;

import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageEndpointFactoryWrapper;
import com.ibm.ws.ejbcontainer.fat.rar.work.FVTComplexWorkImpl;
import com.ibm.ws.ejbcontainer.fat.rar.work.FVTSimpleWorkImpl;
import com.ibm.ws.ejbcontainer.fat.rar.work.FVTWorkDispatcher;
import com.ibm.ws.ejbcontainer.fat.rar.work.FVTWorkImpl;

/**
 * <p>This class is an object cache to store the work instances.</p>
 */
public class FVTObjectCache {
    private final static String CLASSNAME = FVTObjectCache.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public static final FVTObjectCache cache = new FVTObjectCache();

    // XAResource object.
    private final int maxXAResources = 10;
    private int numXAResources;
    private final FVTXAResourceImpl[] xaResources = new FVTXAResourceImpl[maxXAResources];

    // FVTComplexWorkImpl object.
    private final int maxComplexWorks = 10;
    private int numComplexWorks;
    private final FVTComplexWorkImpl[] complexWorks = new FVTComplexWorkImpl[maxComplexWorks];

    // FVTSimplWorkImpl object.
    private final int maxSimpleWorks = 10;
    private int numSimpleWorks;
    private final FVTSimpleWorkImpl[] simpleWorks = new FVTSimpleWorkImpl[maxSimpleWorks];

    FVTObjectCache() {
    }

    /**
     * @return the singleton ObjectCache instance.
     */
    public static final FVTObjectCache get() {
        return cache;
    }

    /**
     * Retrieve a FVT xa resource object from the cache if possible, otherwise make a new one.
     *
     * @return FVT xa resource object.
     */
    public final FVTXAResourceImpl getXAResource() {
        FVTXAResourceImpl xaResource;

        synchronized (xaResources) {
            xaResource = numXAResources > 0 ? xaResources[--numXAResources] : null;
        }

        return xaResource == null ? new FVTXAResourceImpl() : xaResource.recycle();
    }

    /**
     * Put a FVT xa resource back in the cache if there's room for it, otherwise let it
     * go away.
     *
     * @param xaResource An FVT xa resource object.
     *
     * @return true if we cached the handle; false if we didn't.
     */
    public final boolean returnXAResource(FVTXAResourceImpl xaResource) {
        synchronized (xaResources) {
            if (numXAResources >= maxXAResources) {
                svLogger.info("returnXAResource returning false");
                return false;
            }

            xaResources[numXAResources++] = xaResource;
        }

        svLogger.info("returnXAResource returning true");
        return true;
    }

    /**
     * Retrieve a FVTComplexWorkImpl object from the cache if possible, otherwise make a new one.
     *
     * @param deliveryId the name of the work
     * @param message the FVTMessage object
     * @param workDispatcher the work dispatcher
     *
     * @return an FVTComplexWorkImpl object.
     */
    public final FVTComplexWorkImpl getComplexWork(String deliveryId, FVTMessage message, FVTWorkDispatcher dispatcher) {
        FVTComplexWorkImpl work;

        synchronized (complexWorks) {
            work = numComplexWorks > 0 ? complexWorks[--numComplexWorks] : null;
        }

        return work == null ? new FVTComplexWorkImpl(deliveryId, message, dispatcher) : work.recycle(deliveryId, message, dispatcher);
    }

    /**
     * Retrieve a FVTSimpleWorkImpl object from the cache if possible, otherwise make a new one.
     *
     * @param deliveryId the name of the work
     * @param endpointName the endpoint name
     * @param factoryWrapper the Message endpoint factory wrapper object
     * @param resource the xa resource
     *
     * @return an FVTSimpleWorkImpl object.
     */
    public final FVTSimpleWorkImpl getSimpleWork(String deliveryId, String endpointName, MessageEndpointFactoryWrapper factoryWrapper, FVTXAResourceImpl resource) {
        FVTSimpleWorkImpl work;

        synchronized (simpleWorks) {
            work = numSimpleWorks > 0 ? simpleWorks[--numSimpleWorks] : null;
        }

        return work == null ? new FVTSimpleWorkImpl(deliveryId, endpointName, factoryWrapper, resource) : work.recycle(deliveryId, endpointName, factoryWrapper, resource);
    }

    /**
     * Put a FVTWorkImpl object back in the cache if there's room for it, otherwise let it
     * go away.
     *
     * @param work An FVTWorkImpl object.
     *
     * @return true if we cached the handle; false if we didn't.
     */
    public final boolean returnWork(FVTWorkImpl work) {
        if (work instanceof FVTComplexWorkImpl) {
            synchronized (complexWorks) {
                if (numComplexWorks >= maxComplexWorks) {
                    svLogger.info("returnWork returning false - Cannot cache this FVTComplexWorkImpl object");
                    return false;
                }

                complexWorks[numComplexWorks++] = (FVTComplexWorkImpl) work;
            }
        } else if (work instanceof FVTSimpleWorkImpl) {
            synchronized (simpleWorks) {
                if (numSimpleWorks >= maxSimpleWorks) {
                    svLogger.info("returnWork returning false - Cannot cache this FVTSimpleWorkImpl object");
                    return false;
                }

                simpleWorks[numSimpleWorks++] = (FVTSimpleWorkImpl) work;
            }
        } else {
            svLogger.info("returnWork returning false - TRA doesn't support caching this work object");
            return false;
        }

        svLogger.info("returnWork returning true");
        return true;
    }
}