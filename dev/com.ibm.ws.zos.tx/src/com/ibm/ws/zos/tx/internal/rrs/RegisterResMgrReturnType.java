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

import com.ibm.tx.util.ByteArray;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Holds all information returned from the call to
 * the system's register resource manager (crg4grm) service.
 */
public final class RegisterResMgrReturnType {

    /**
     * Internal return code that stipulates that the resource manager prefix being
     * used has failed to pass the authorization check before registering with RRS.
     * This return code must be in sync with one defined in tx_authorized_rrs_services.mc.
     */
    private final int RMPREFIX_FAILED_AUTHORIZATION_CHECK = -10;

    /**
     * The service's return code.
     */
    private final int rc;

    /**
     * The resource manager name.
     */
    private final byte[] rmName;

    /**
     * The resource manager name registry token.
     */
    private final byte[] rmNameRegistryToken;

    /**
     * The token representing the resource manager.
     */
    private final byte[] rmToken;

    /**
     * The registry token used to get the resource manager token.
     */
    private final byte[] rmRegistryToken;

    /**
     * The internal authorization check return code.
     */
    private final int internalAuthCheckRc;

    /**
     * The SAF authorization return code.
     */
    private final int safRc;

    /**
     * The RACF authorization return code.
     */
    private final int racfRc;

    /**
     * The RACF authorization reason code.
     */
    private final int racfRsn;

    /**
     * Constructor
     */
    public RegisterResMgrReturnType(int rc, byte[] rmName, byte[] rmNameRegistryToken, byte[] rmToken, byte[] rmRegistryToken, int internalAuthCheckRc, int safRc, int racfRc,
                                    int racfRsn) {
        this.rc = rc;
        this.rmName = rmName;
        this.rmNameRegistryToken = rmNameRegistryToken;
        this.rmToken = rmToken;
        this.rmRegistryToken = rmRegistryToken;
        this.internalAuthCheckRc = internalAuthCheckRc;
        this.safRc = safRc;
        this.racfRc = racfRc;
        this.racfRsn = racfRsn;
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
     * Retrieves the resource manager name.
     *
     * @return The resource manager name.
     */
    @Trivial
    public byte[] getResMgrName() {
        return rmName;
    }

    /**
     * Retrieves the resource manager name.
     *
     * @return The resource manager name.
     */
    @Trivial
    public byte[] getResMgrNameRegistryToken() {
        return rmNameRegistryToken;
    }

    /**
     * Retrieves the token representing the resource manager.
     *
     * @return The resource manager token.
     */
    @Trivial
    public byte[] getResMgrToken() {
        return rmToken;
    }

    /**
     * Retrieves the registry token used to get the resource manager token.
     *
     * @return The registry token used to get the resource manager token.
     */
    @Trivial
    public byte[] getResMgrRegistryToken() {
        return rmRegistryToken;
    }

    /**
     * Indicates whether or not the resource manager prefix
     * authorization check against the current user failed.
     *
     * @return True if authorization check failed. False otherwise.
     */
    @Trivial
    public boolean isRMPrefixAuthCheckFailed() {
        return (rc == RMPREFIX_FAILED_AUTHORIZATION_CHECK) ? true : false;
    }

    /**
     * Retrieves the internal authorization check return code.
     *
     * @return The SAF authentication return code.
     */
    @Trivial
    public int getInternalAuthCheckRc() {
        return internalAuthCheckRc;
    }

    /**
     * Retrieves the SAF authorization return code.
     *
     * @return The SAF authorization return code.
     */
    @Trivial
    public int getSAFRc() {
        return safRc;
    }

    /**
     * Retrieves the RACF authorization return code.
     *
     * @return The RACF authorization return code.
     */
    @Trivial
    public int getRACFRc() {
        return racfRc;
    }

    /**
     * Retrieves the RACF authorization reason code.
     *
     * @return The RACF authorization reason code.
     */
    @Trivial
    public int getRACFRsn() {
        return racfRsn;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RegisterResMgrReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", ResourceManagerNameBytes: ");
        sb.append((rmName == null) ? "null" : new ByteArray(rmName).toString());
        sb.append(", ResourceManagerNameRegistryToken: ");
        sb.append((rmNameRegistryToken == null) ? "null" : new ByteArray(rmNameRegistryToken).toString());
        sb.append(", ResourceManagerToken: ");
        sb.append((rmToken == null) ? "null" : new ByteArray(rmToken).toString());
        sb.append(", ResourceManagerRegistryToken: ");
        sb.append((rmRegistryToken == null) ? "null" : new ByteArray(rmRegistryToken).toString());
        sb.append(", InternalAuthCheckRC: ");
        sb.append(internalAuthCheckRc);
        sb.append(", SAFAuthenticationRC: ");
        sb.append(Integer.toHexString(safRc));
        sb.append(", RACFAuthenticationRC: ");
        sb.append(Integer.toHexString(racfRc));
        sb.append(", RACFAuthenticationRsnCode: ");
        sb.append(Integer.toHexString(racfRsn));
        sb.append("]");
        return sb.toString();
    }
}
