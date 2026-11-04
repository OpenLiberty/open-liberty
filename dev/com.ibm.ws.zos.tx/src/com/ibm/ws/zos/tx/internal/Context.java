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

/**
 * Interface representing an RRS transaction context.
 */
public interface Context {
    /**
     * Gets the context token.
     *
     * @return The RRS context token.
     */
    public byte[] getContextToken();

    /**
     * Gets the registry token.
     *
     * @return The registry token used to obtain the RRS context token from the
     *         native registry.
     */
    public byte[] getContextRegistryToken();

    /**
     * Stores the token used to look up the context interest token for this context, in the native registry.
     *
     * @param token The context interest registry token.
     */
    public void setContextInterestRegistryToken(byte[] token);

    /**
     * Retrieves the token used to look up the stored context interest token in the native registry.
     *
     * @return The token used to look up the stored context interest token in the native registry.
     */
    public byte[] getContextInterestRegistryToken();
}
