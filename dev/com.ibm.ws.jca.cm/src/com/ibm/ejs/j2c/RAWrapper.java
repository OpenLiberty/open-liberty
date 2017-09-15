/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.io.Serializable;

import javax.resource.spi.ResourceAdapter;

/**
 * Common parent class for RAWrapperImpl (for the server) and
 * RAWrapperEECImpl (for the embeddable EJB container).
 */
public abstract class RAWrapper implements Serializable {
    private static final long serialVersionUID = 2371451642881427062L; 

    static final int SERVER_COMP_START = 1; // The RA is being started during normal server startup
    static final int MBEAN_START = 2; // The RA is being started via mbean calls
    static final int XARECOVERY_START = 3; // The RA is being started for transaction recovery
    static final int HA_START = 4; // LI4396-1 RA is being started from the HA memberIsActivated call 

    /**
     * @return the JCA specification version supported by the resource adapter.
     */
    public abstract J2CConstants.JCASpecVersion getJcaSpecVersion();

    /**
     * @return the archive path of a resource adapter.
     */
    public abstract String getArchivePath(); 

    /**
     * @return the resource adapter instance.
     */
    public abstract ResourceAdapter getRA();

    /**
     * @return the class name of the resource adapter.
     */
    public abstract String getRaClassName();

    /**
     * @return the rasource adatper key.
     */
    public abstract String getRAKey();

    /**
     * @return the resource adapter name.
     */
    public abstract String getRAName();

    /**
     * @return true if the resource adapter is embedded, otherwise false.
     */
    public abstract boolean isEmbedded();

    /**
     * @return whether this resource adapter should have no more than one instance
     *         (singleton) in the JVM.
     */
    public abstract boolean isForceSingleRAInstance();

    /**
     * @return true if the resource adapter is started, otherwise false.
     */
    public abstract boolean isStarted();

    /**
     * @return true if the resource adapter was created specifically for recovery, otherwise false.
     */
    protected abstract boolean isTXRecovery();

    /**
     * Starts the resource adapter. Refer to the implementation class for more details.
     * 
     * @param j2cRA must be either an instance of com.ibm.wsspi.runtime.config.ConfigObject (when running in the traditional WAS server) or NULL (when running elsewhere).
     */
    public abstract void startRA(Object j2cRA, String mbParentName, boolean embedded, int source) throws Exception;

    /**
     * Stops the resource adapter. Refer to the implementation class for more details.
     */
    public abstract void stopRA(boolean originatesFromTM) throws Exception;
}
