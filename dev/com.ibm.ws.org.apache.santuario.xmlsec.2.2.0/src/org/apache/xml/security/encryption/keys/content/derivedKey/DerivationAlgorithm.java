/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.encryption.keys.content.derivedKey;

import org.apache.xml.security.encryption.params.KeyDerivationParameters;
import org.apache.xml.security.exceptions.XMLSecurityException;

/**
 * The DerivationAlgorithm is the base interface for all key derivation algorithms
 * implementation.
 */
public interface DerivationAlgorithm<T extends KeyDerivationParameters> {

    /**
     * Derives a key from the given secret and other info.
     *
     * @param secret The "shared" secret to use for key derivation (e.g. the secret key)
     * @param params The key derivation parameters implementing the KeyDerivationParameters interface
     * @return Byte array of the derived key
     * @throws XMLSecurityException in case of derivation error or invalid parameters
     */
    byte[] deriveKey(byte[] secret, T params) throws XMLSecurityException;
}
