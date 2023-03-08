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
package com.ibm.ws.zos.tx.internal;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.xa.XAResource;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Stores data associated with a local transaction.
 */
public class LocalTransactionData extends TransactionData {

    /**
     * The context token.
     */
    private final Context contextTokenWrapper;

    /**
     * Constructor;
     */
    public LocalTransactionData(Context contextTokenWrapper) {
        super(new ArrayList<XAResource>());
        this.contextTokenWrapper = contextTokenWrapper;
    }

    /**
     * Retrieves the token associated to the context under which the transaction
     * is executing.
     *
     * @return The context token.
     */
    public byte[] getContextToken() {
        return contextTokenWrapper.getContextToken();
    }

    /**
     * Collects the data associated with the local transaction.
     *
     * @return A string representation of the data associated with the local transaction.
     */
    @Trivial
    @FFDCIgnore(Throwable.class)
    public String getData() {
        StringBuilder sb = new StringBuilder();

        try {
            List<XAResource> xaResourceList = getXAResourceList();
            byte[] contextToken = (contextTokenWrapper == null) ? null : contextTokenWrapper.getContextToken();
            sb.append("LocalTransactionData [");
            sb.append("ContextToken: ");
            sb.append((contextToken == null) ? "NULL" : Util.toHexString(contextToken) + "]");
            sb.append("\nContextData: ");
            sb.append((contextTokenWrapper == null) ? "NULL" : contextTokenWrapper.toString());
            sb.append("\nNativeLocalXAResourcesParticipatingInTran: ");
            sb.append(xaResourceList.size());

            if (xaResourceList.size() > 0) {
                sb.append("\n{");
                for (XAResource resource : xaResourceList) {
                    sb.append("\n" + resource);
                    sb.append("[" + ((NativeLocalXAResource) resource).getData() + "]");
                }
                sb.append("\n}");
            }
        } catch (Throwable t) {
            sb.append("\nLocalTransactionData.getData error: " + t.toString());
        }

        return sb.toString();
    }
}