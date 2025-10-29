/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.encryption.params;

/**
 * Abstract key derivation class contains the basic parameters used for the key derivation.
 * The class should be extended to provide algorithm specific parameters.
 */
public abstract class KeyDerivationParameters {
    private final String algorithm;
    private final int keyBitLength;

    protected KeyDerivationParameters(String algorithm, int keyLength) {
        this.algorithm = algorithm;
        this.keyBitLength = keyLength;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * The length of the derived key in bits.
     * @return
     */
    public int getKeyBitLength() {
        return keyBitLength;
    }

    /**
     * The length of the derived key in bytes needed to store the key in bitSize.
     * For example: if the key is 9 bits long, the length of the key in bytes is 2, but
     * the key is stored in 8 bits the length in bytes is 1.

     * @return the length of the derived key in bytes
     */
    public int getKeyLength() {
        return keyBitLength / 8 + (keyBitLength % 8 == 0 ? 0 : 1);
    }
}
