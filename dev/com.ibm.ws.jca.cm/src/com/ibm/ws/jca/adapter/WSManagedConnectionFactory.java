/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.adapter;

import javax.resource.spi.ManagedConnectionFactory;
import javax.transaction.xa.XAResource;

import org.osgi.framework.Version;

import com.ibm.ws.resource.ResourceRefInfo;

/**
 * WebSphere Application Server extensions to the ManagedConnectionFactory interface.
 */
public abstract class WSManagedConnectionFactory implements ManagedConnectionFactory {
    private static final long serialVersionUID = -8501184761741716982L;

    /**
     * Returns the default type of branch coupling that should be used for BRANCH_COUPLING_UNSET.
     *
     * @return the default type of branch coupling: BRANCH_COUPLING_LOOSE or BRANCH_COUPLING_TIGHT.
     *         If branch coupling is not supported or it is uncertain which type of branch coupling is default,
     *         then BRANCH_COUPLING_UNSET may be returned.
     * @see ResourceRefInfo
     */
    public int getDefaultBranchCoupling() {
        return ResourceRefInfo.BRANCH_COUPLING_UNSET;
    }

    /**
     * Returns the xa.start flags (if any) to include for the specified branch coupling.
     * XAResource.TMNOFLAGS should be returned if the specified branch coupling is default.
     * -1 should be returned if the specified branch coupling is not supported.
     *
     * @param couplingType one of the BRANCH_COUPLING_* constants
     * @return the xa.start flags (if any) to include for the specified branch coupling.
     */
    public int getXAStartFlagForBranchCoupling(int couplingType) {
        if (couplingType == ResourceRefInfo.BRANCH_COUPLING_UNSET || couplingType == getDefaultBranchCoupling())
            return XAResource.TMNOFLAGS;
        else
            return -1;
    }

    /**
     * Indicated the level of JDBC support for the ManagedConnectionFactory
     *
     * @return The jdbc version which the ManagedConnectionFactory supports.
     */
    public Version getJDBCRuntimeVersion() {
        return new Version(4, 0, 0);
    }

    public boolean isPooledConnectionValidationEnabled() {
        return false;
    }
}