/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.tx.internal;

import java.io.Serializable;
import java.util.Arrays;

import javax.transaction.xa.Xid;

/**
 *
 */
public class NativeXAResourceInfo implements Serializable {

    /**
     * Serial Id.
     */
    private static final long serialVersionUID = -3833379453850761316L;

    /**
     * The JNDI name
     */
    final String pmiName;

    /**
     * The transaction ID.
     */
    final Xid xid;

    /**
     * The resource manager name.
     */
    final String resMgrName;

    /**
     * Constructor.
     */
    public NativeXAResourceInfo(String pmiName, Xid xid, String resMgrName) {
        this.pmiName = pmiName;
        this.xid = xid;
        this.resMgrName = resMgrName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        boolean isEqual = false;
        if (o instanceof NativeXAResourceInfo) {
            NativeXAResourceInfo other = (NativeXAResourceInfo) o;
            if (other.xid != null &&
                this.xid != null &&
                Arrays.equals(other.xid.getGlobalTransactionId(), this.xid.getGlobalTransactionId()) &&
                other.resMgrName.equals(this.resMgrName) &&
                other.pmiName.equals(this.pmiName)) {
                isEqual = true;
            }
        }

        return isEqual;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hc = 1;

        if (xid != null) {
            hc = hc * 64 + Arrays.hashCode(xid.getGlobalTransactionId());
        }

        if (pmiName != null) {
            hc = hc * 64 + pmiName.hashCode();
        }

        if (pmiName != null) {
            hc = hc * 64 + resMgrName.hashCode();
        }

        return hc;
    }

    /**
     * Retrieves the resource manager name.
     *
     * @return The resource manager name.
     */
    public String getResourceManagerName() {
        return resMgrName;
    }
}
