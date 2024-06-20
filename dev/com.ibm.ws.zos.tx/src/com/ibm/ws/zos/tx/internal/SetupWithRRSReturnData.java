/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.tx.internal;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.JTA.Util;

/**
 * Holds data saved during initial setup processing with RRS.
 */
public class SetupWithRRSReturnData {

    /**
     * Resource manager token.
     */
    private byte[] resMgrToken;

    /**
     * Resource manager registry token.
     */
    private byte[] resMgrRegistryToken;

    /**
     * Resource manager name registry token.
     */
    private byte[] resMgrNameRegistryToken;

    /**
     * Throwable that indicates a setup failure.
     */
    private Throwable throwable;

    /**
     * Sets the resource manager token.
     *
     * @param resMgrRegistryToken The resource manager registry token.
     */
    @Trivial
    protected void setResMgrToken(byte[] resMgrToken) {
        this.resMgrToken = resMgrToken;
    }

    /**
     * Retrieves the resource manager token.
     *
     * @return The resource manager registry token bytes.
     */
    @Trivial
    protected byte[] getResMgrToken() {
        return resMgrToken;
    }

    /**
     * Sets the resource manager registry token.
     *
     * @param resMgrRegistryToken The resource manager registry token.
     */
    @Trivial
    protected void setResMgrRegistryToken(byte[] resMgrRegistryToken) {
        this.resMgrRegistryToken = resMgrRegistryToken;
    }

    /**
     * Retrieves the resource manager registry token.
     *
     * @return The resource manager registry token bytes.
     */
    @Trivial
    protected byte[] getResMgrRegistryToken() {
        return resMgrRegistryToken;
    }

    /**
     * Sets the resource manager name registry token.
     *
     * @param resMgrNameRegistryToken The resource manager name registry token.
     */
    @Trivial
    protected void setRMNameRegistryToken(byte[] resMgrNameRegistryToken) {
        this.resMgrNameRegistryToken = resMgrNameRegistryToken;
    }

    /**
     * Retrieves the resource manager name registry token.
     *
     * @return The resource manager name registry token bytes.
     */
    @Trivial
    protected byte[] getResMgrNameRegistryToken() {
        return resMgrNameRegistryToken;
    }

    /**
     * Sets a Throwable.
     *
     * @param t The Throwable.
     */
    @Trivial
    protected void setThrowable(Throwable t) {
        throwable = t;
    }

    /**
     * Retrieves a Throwable.
     *
     * @return A Throwable if one is set. Null Otherwise.
     */
    @Trivial
    protected Throwable getThrowable() {
        return throwable;
    }

    /**
     * Prints the data associated with this class.
     *
     * @return A string representation of the data.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(" [ResourceManagerToken: ");
        sb.append((resMgrToken == null) ? "NULL" : Util.toHexString(resMgrToken));
        sb.append(", ResourceManagerRegistryToken: ");
        sb.append((resMgrRegistryToken == null) ? "NULL" : Util.toHexString(resMgrRegistryToken));
        sb.append(", ResourceManagerNameRegistryToken: ");
        sb.append((resMgrNameRegistryToken == null) ? "NULL" : Util.toHexString(resMgrNameRegistryToken));
        sb.append(", Throwable: ");
        sb.append((throwable == null) ? "NULL" : throwable.toString());
        sb.append("]");

        return sb.toString();
    }
}
