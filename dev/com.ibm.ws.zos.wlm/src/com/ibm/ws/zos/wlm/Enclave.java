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
package com.ibm.ws.zos.wlm;

/**
 * Enclave.
 */
public interface Enclave {

    /**
     * Gets the string representation of to enclave token.
     */
    public String getStringToken();

    /**
     * Gets the enclave's auto deletion indicator to allow the enclave manager to delete
     * an enclave on the last leave.
     *
     * @return The enclave's auto deletion indicator.
     */
    public boolean getAutoDelete();

    /**
     * Sets the enclave's auto deletion indicator to allow the enclave manager to delete
     * an enclave on the last leave.
     *
     * @param delete The enclave's auto deletion indicator.
     */
    public void setAutoDelete(boolean delete);

    /**
     * Determine of this Enclave is still &quot;in-use&qout; or not. If the
     * Enclave is not in use, the EnclaveManager will deregister or delete the
     * WLM Enclave that this object represents.
     */
    public boolean isInUse();
}
