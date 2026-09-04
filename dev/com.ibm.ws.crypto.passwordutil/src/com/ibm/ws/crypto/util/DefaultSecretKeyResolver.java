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
package com.ibm.ws.crypto.util;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import com.ibm.wsspi.security.crypto.SecretKeyResolver;

/**
 * Default software-backed {@link SecretKeyResolver} used for each AES key version.
 * <p>
 * For V0 and V1 the key is derived using PBKDF2; for V2 the key is Base64-decoded.
 * Key caching is handled by {@link AESKeyManager.KeyVersion}'s own {@code KeyHolder} cache.
 * <p>
 * Package-private: internal implementation detail, not part of the public or SPI surface.
 */
class DefaultSecretKeyResolver implements SecretKeyResolver {

    private final AESKeyManager.KeyVersion version;

    /**
     * @param version the {@link AESKeyManager.KeyVersion} this resolver is associated with.
     */
    DefaultSecretKeyResolver(AESKeyManager.KeyVersion version) {
        this.version = version;
    }

    @Override
    public Key getKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Delegate entirely to AESKeyManager which handles both key resolution and caching
        // via KeyVersion's KeyHolder AtomicReference.
        return AESKeyManager.getKey(version, null);
    }
}
