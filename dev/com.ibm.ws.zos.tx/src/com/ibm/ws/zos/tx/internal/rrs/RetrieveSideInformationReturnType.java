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

package com.ibm.ws.zos.tx.internal.rrs;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Holds all information returned from the call to
 * RRS' retrieve side information (atr4rusi) service.
 */
public final class RetrieveSideInformationReturnType {

    /**
     * The service's return code.
     */
    private final int rc;

    /**
     * The side information results.
     */
    private final int[] info;

    /**
     * Constructor
     */
    public RetrieveSideInformationReturnType(int rc, int[] info) {
        this.rc = rc;
        this.info = info;
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
     * Gets a specific side information based on the specified index value.
     * The side information results order indexing is the same as the indexing for
     * the data requested as part of the service call.
     *
     * @param index The index value within the side info array.
     *
     * @return The specific side information based on the specified index value.
     */
    @Trivial
    public int getSideInfo(int index) {
        return info[index];
    }

    /**
     * Gets all side information values.
     *
     * @return The side information array of integers.
     */
    @Trivial
    public int[] getSideInfo() {
        return info;
    }

    /**
     * The amount of side information values returned from the service.
     *
     * @return The size of the side information array.
     */
    @Trivial
    public int getInfoSize() {
        if (info == null)
            return 0;

        return info.length;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RetrieveSideInformationReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", ");

        if (info != null) {
            sb.append("SideInfoValues: ");
            for (int x = 0; x < info.length; x++) {
                sb.append(x);
                sb.append(": ");
                sb.append(info[x]);
                sb.append(". ");
            }
        }

        sb.append("]");
        return sb.toString();
    }
}