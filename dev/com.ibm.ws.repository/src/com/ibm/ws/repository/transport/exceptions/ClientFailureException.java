/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.transport.exceptions;

/**
 *
 */
public class ClientFailureException extends Exception {

    /**  */
    private static final long serialVersionUID = 1049134339676366647L;

    private final String assetId;

    public ClientFailureException(String message, String assetId) {
        super(message);
        this.assetId = assetId;
    }

    public ClientFailureException(String message, String assetId, Throwable cause) {
        super(message, cause);
        this.assetId = assetId;
    }

    @Override
    public Throwable getCause() {
        return super.getCause();
    }

    public String getAssetId() {
        return this.assetId;
    }
}
