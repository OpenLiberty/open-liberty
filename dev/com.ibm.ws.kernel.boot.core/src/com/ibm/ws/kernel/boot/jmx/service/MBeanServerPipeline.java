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
package com.ibm.ws.kernel.boot.jmx.service;

/**
 * Service for injection and removal of MBeanServerForwarder
 * filters into and from the platform MBeanServer.
 */
public interface MBeanServerPipeline {

    /**
     * Returns true if this MBeanServerPipeline contains the
     * given MBeanServerForwarderDelegate.
     */
    public boolean contains(MBeanServerForwarderDelegate filter);

    /**
     * Inserts an MBeanServerForwarderDelegate into the MBeanServerPipeline.
     * Returns true if successful; false otherwise.
     */
    public boolean insert(MBeanServerForwarderDelegate filter);

    /**
     * Removes an MBeanServerForwarderDelegate from the MBeanServerPipeline.
     * Returns true if successful; false otherwise.
     */
    public boolean remove(MBeanServerForwarderDelegate filter);

}
