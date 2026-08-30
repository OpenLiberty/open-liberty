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
package com.ibm.ws.zos.tx.internal.rrs;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Holds all information returned from the native call to
 * set exit information.
 */
public class SetExitInformationReturnType {

    /**
     * The service's return code.
     */
    private final int rc;

    /**
     * Indicates whether or not logging to the RRS's METADATA logstream
     * is allowed.
     */
    private final boolean metadataLoggingAllowed;

    /**
     *
     * @param rc             The service return code.
     * @param canLogMetadata
     */
    public SetExitInformationReturnType(int rc, boolean metadataLoggingAllowed) {
        this.rc = rc;
        this.metadataLoggingAllowed = metadataLoggingAllowed;
    }

    /**
     * Retrieves the service's return code.
     *
     * @return The service's return code.
     */
    @Trivial
    public int getReturnCode() {
        return rc;
    }

    /**
     * Indicates whether or not logging to the RRS's METADATA logstream
     * is allowed.
     *
     * @return The resource manager's log name.
     */
    @Trivial
    public boolean isMetadataLoggingAllowed() {
        return metadataLoggingAllowed;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RetrieveLogNameReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", IsMetadataLoggingAllowed: ");
        sb.append(metadataLoggingAllowed);
        sb.append("]");
        return sb.toString();
    }
}
