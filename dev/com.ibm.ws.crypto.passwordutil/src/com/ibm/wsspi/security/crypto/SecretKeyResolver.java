/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Interface for resolving an opaque hardware-backed AES key (e.g. from ICSF/CKDS
 * via the IBMJCECCA security provider on z/OS). Implementations must not call
 * {@code getEncoded()} on the returned key — key material never leaves hardware.
 *
 * @ibm-spi
 */
public interface SecretKeyResolver {

    /**
     * Returns the hardware-backed {@link Key} to be used directly with a {@link javax.crypto.Cipher}.
     *
     * @return the Key reference
     * @throws NoSuchAlgorithmException if the required algorithm is not available
     * @throws InvalidKeySpecException  if the key material is invalid or improperly encoded
     */
    Key getKey() throws NoSuchAlgorithmException, InvalidKeySpecException;

    /**
     * Returns a human-readable description of this resolver suitable for log messages.
     * The description must not contain any sensitive key material.
     *
     * <p>The default implementation returns the simple class name of the resolver, which
     * is sufficient for most cases. Implementations that carry meaningful user-visible
     * context (e.g. an ICSF key label) should override this method to include that
     * context so it can be surfaced in the server log.
     *
     * @return a non-null, non-sensitive description string
     */
    default String getDescription() {
        return getClass().getSimpleName();
    }
}
