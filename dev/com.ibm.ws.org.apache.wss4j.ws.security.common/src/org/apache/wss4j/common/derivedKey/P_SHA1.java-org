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

package org.apache.wss4j.common.derivedKey;

/**
 *
 <pre>
 P_SHA-1 DEFINITION
 ==================
 <b>P_SHA-1(secret, seed)</b> =
 HMAC_SHA-1(secret, A(1) + seed) +
 HMAC_SHA-1(secret, A(2) + seed) +
 HMAC_SHA-1(secret, A(3) + seed) + ...
 <i>Where + indicates concatenation.</i>
 <br>
 A() is defined as:
 A(0) = seed
 A(i) = HMAC_SHA-1(secret, A(i-1))
 <br>
 <i>Source : RFC 2246 - The TLS Protocol Version 1.0
 Section 5. HMAC and the pseudorandom function</i>
 </pre>
 */

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import org.apache.wss4j.common.ext.WSSecurityException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class P_SHA1 implements DerivationAlgorithm {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(P_SHA1.class);

    @Override
    public byte[] createKey(byte[] secret, byte[] seed, int offset, long length)
            throws WSSecurityException {

        try {
            Mac mac = Mac.getInstance("HmacSHA1");

            byte[] tempBytes = pHash(secret, seed, mac, offset + (int) length);

            byte[] key = new byte[(int) length];

            System.arraycopy(tempBytes, offset, key, 0, key.length);

            return key;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "errorInKeyDerivation");
        }
    }

    /**
     * P_hash as defined in RFC 2246 for TLS.
     *
     * @param secret is the key for the HMAC
     * @param seed the seed value to start the generation - A(0)
     * @param mac the HMAC algorithm
     * @param required number of bytes to generate
     * @return a byte array that contains a secret key
     * @throws InvalidKeyException
     */
    private static byte[] pHash(byte[] secret, byte[] seed, Mac mac, int required)
            throws InvalidKeyException {

        byte[] out = new byte[required];
        int offset = 0, tocpy;
        byte[] a = seed; // a(0) is the seed
        byte[] tmp;

        SecretKeySpec key = new SecretKeySpec(secret, "HMACSHA1");
        mac.init(key);

        int bytesRequired = required;
        while (bytesRequired > 0) {
            mac.update(a);
            a = mac.doFinal();
            mac.update(a);
            mac.update(seed);
            tmp = mac.doFinal();
            tocpy = Math.min(bytesRequired, tmp.length);
            System.arraycopy(tmp, 0, out, offset, tocpy);
            offset += tocpy;
            bytesRequired -= tocpy;
        }

        try {
            key.destroy();
        } catch (DestroyFailedException e) {
            LOG.debug("Error destroying key: {}", e.getMessage());
        }
        return out;
    }
}
