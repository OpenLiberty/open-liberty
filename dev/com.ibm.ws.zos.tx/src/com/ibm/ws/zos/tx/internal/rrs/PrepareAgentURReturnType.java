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

import com.ibm.tx.util.ByteArray;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Class that encapsulates the return data from the Liberty server's RRS prepare
 * flow, which consists of a call to express context interest (CTX4EINT) and
 * agent prepare (ATR4APRP).
 */
public class PrepareAgentURReturnType {

    /** The return code from ATR4APRP. */
    private final int agentPrepareReturnCode;

    /** The return code from CTX4EINT. */
    private final int expressContextInterestReturnCode;

    /** The registry token used to look up the context interest token. */
    private final byte[] contextInterestRegistryToken;

    /**
     * Constructor.
     *
     * @param agentPrepareReturnCode           The return code from the ATR4APRP service.
     * @param expressContextInterestReturnCode The return code from the CTX4EINT service.
     * @param contextInterestRegistryToken     The token used to look up the context interest
     *                                             token returned from the CTX4EINT service, from
     *                                             the native registry.
     */
    public PrepareAgentURReturnType(int agentPrepareReturnCode, int expressContextInterestReturnCode, byte[] contextInterestRegistryToken) {
        this.agentPrepareReturnCode = agentPrepareReturnCode;
        this.expressContextInterestReturnCode = expressContextInterestReturnCode;
        if (contextInterestRegistryToken != null) {
            this.contextInterestRegistryToken = new byte[contextInterestRegistryToken.length];
            System.arraycopy(contextInterestRegistryToken, 0, this.contextInterestRegistryToken, 0, contextInterestRegistryToken.length);
        } else {
            this.contextInterestRegistryToken = null;
        }
    }

    /**
     * Gets the return code from the ATR4APRP service.
     *
     * @return The return code from the ATR4APRP service.
     */
    @Trivial
    public int getAgentPrepareReturnCode() {
        return agentPrepareReturnCode;
    }

    /**
     * Gets the return code from the CTX4EINT service.
     *
     * @return The return code from the CTX4EINT service.
     */
    @Trivial
    public int getExpressContextInterestReturnCode() {
        return expressContextInterestReturnCode;
    }

    /**
     * Gets the token used to look up the context interest token from the native registry.
     *
     * @return The token used to look up the context interest token from the native registry.
     */
    @Trivial
    public byte[] getContextInterestRegistryToken() {
        byte[] contextInterestRegistryToken = null;

        if (this.contextInterestRegistryToken != null) {
            contextInterestRegistryToken = new byte[this.contextInterestRegistryToken.length];
            System.arraycopy(this.contextInterestRegistryToken, 0, contextInterestRegistryToken, 0, contextInterestRegistryToken.length);
        }

        return contextInterestRegistryToken;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("PrepareAgentURRetyrnType [");
        sb.append("ATR4APRP ReturnCode: ");
        sb.append(Integer.toHexString(agentPrepareReturnCode));
        sb.append(", CTX4EINT ReturnCode: ");
        sb.append(Integer.toHexString(expressContextInterestReturnCode));
        sb.append(", ContextInterestRegistryToken: ");
        sb.append((contextInterestRegistryToken == null) ? "null" : new ByteArray(contextInterestRegistryToken).toString());
        sb.append("]");
        return sb.toString();
    }
}
