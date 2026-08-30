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
 * Retrieves resource manager metadata.
 */
public class RetrieveRMMetadataReturnType {

    /**
     * The service's return code
     */
    private final int rc;

    /**
     * The resource manager metadata.
     */
    private final byte[] metadata;

    /**
     * Constructor.
     *
     * @param rc       The service's return code.
     * @param metadata The resource manager metadata.
     */
    public RetrieveRMMetadataReturnType(int rc, byte[] metadata) {
        this.rc = rc;

        if (metadata != null) {
            this.metadata = new byte[metadata.length];
            System.arraycopy(metadata, 0, this.metadata, 0, metadata.length);
        } else {
            this.metadata = null;
        }
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
     * Retrieves the resource manager metadata.
     *
     * @return The metadata.
     */
    @Trivial
    public byte[] getMetaData() {
        byte[] metadataCopy = null;

        if (metadata != null) {
            metadataCopy = new byte[metadata.length];
            System.arraycopy(metadata, 0, metadataCopy, 0, metadata.length);
        }

        return metadataCopy;
    }

    /**
     * {@inheritDoc}
     */
    @Trivial
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        String data = "";
        try {
            data = new String(metadata, "IBM-1047");
        } catch (Throwable t) {
            // Absorb any exceptions and move on.
        }

        sb.append("RetrieveRMMetadataReturnType [");
        sb.append("ReturnCode: ");
        sb.append(Integer.toHexString(rc));
        sb.append(", MetaData: ");
        sb.append(data);
        sb.append("]");
        return sb.toString();
    }
}
