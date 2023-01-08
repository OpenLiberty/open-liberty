/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.crypto;

/**
 * The interface for resolving the aes encryption key.
 *
 * @ibm-spi
 *
 */
public interface KeyStringResolver {

    /**
     * Getting the key to used to for aes encryption. Takes a string that may hold the key
     * value and returns a char[] of the encryption key.
     *
     * @param val
     * @return char[]
     */
    public char[] getKey(String val);

}
