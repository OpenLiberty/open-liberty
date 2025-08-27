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
package org.apache.xml.security.encryption.keys.content.derivedKey;

import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.encryption.params.HKDFParams;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.utils.I18n;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


/**
 * The implementation of the HMAC-based Extract-and-Expand Key Derivation Function (HKDF)
 * as defined in <a href="https://datatracker.ietf.org/doc/html/rfc5869">RFC 5869</a>.
 * <p>
 * The HKDF algorithm is defined as follows:
 * <pre>
 * N = ceil(L/HashLen)
 * T = T(1) | T(2) | T(3) | ... | T(N)
 * OKM = first L bytes of T
 * where:
 * T(0) = empty string (zero length)
 * T(1) = HMAC-Hash(PRK, T(0) | info | 0x01)
 * T(2) = HMAC-Hash(PRK, T(1) | info | 0x02)
 * T(3) = HMAC-Hash(PRK, T(2) | info | 0x03)
 * ...
 * </pre>
 */
public class HKDF implements DerivationAlgorithm<HKDFParams> {


	private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(HKDFParams.class);
    /**
     * Derive a key using the HMAC-based Extract-and-Expand Key Derivation Function (HKDF)
     * as defined in <a href="https://datatracker.ietf.org/doc/html/rfc5869">RFC 5869</a>.
     *
     * @param secret The "shared" secret to use for key derivation
     * @param params The key derivation parameters (salt, info, key length, ...)
     * @return The derived key of the specified length in bytes defined in the params
     * @throws IllegalArgumentException if the parameters are missing
     * @throws XMLSecurityException     if the hmac hash algorithm is not supported
     */
    @Override
    public byte[] deriveKey(byte[] secret, HKDFParams params) throws XMLSecurityException {
        // check if the parameters are set
        if (params == null) {
            throw new IllegalArgumentException(I18n.translate("KeyDerivation.MissingParameters"));
        }

        String jceAlgorithmName = JCEMapper.translateURItoJCEID(params.getHmacHashAlgorithm());
        if (jceAlgorithmName == null) {
            throw new XMLSecurityException("KeyDerivation.NotSupportedParameter", new Object[]{params.getHmacHashAlgorithm()});
        }

        byte[] prk = extractKey(jceAlgorithmName, params.getSalt(), secret);
        return expandKey(jceAlgorithmName, prk, params.getInfo(), params.getKeyLength());
    }

    /**
     * The method "extracts" the pseudo-random key (PRK) based on HMAC-Hash function
     * (optional) salt value (a non-secret random value) and the shared secret/input
     * keying material (IKM).
     * Calculation of the  extracted key:
     * <pre>PRK = HMAC-Hash(salt, IKM)</pre>
     *
     * @param jceAlgorithmName the java JCE HMAC algorithm name to use for key derivation
     *                         (e.g. HmacSHA256, HmacSHA384, HmacSHA512)
     * @param salt             the optional salt value (a non-secret random value);
     * @param secret           the shared secret/input keying material (IKM) to use for
     *                         key derivation
     * @return the pseudo-random key bytes
     * @throws XMLSecurityException if the jceAlgorithmName is not supported
     */
    public byte[] extractKey(String jceAlgorithmName, byte[] salt, byte[] secret) throws XMLSecurityException {
        Mac hMac = initHMac(jceAlgorithmName, salt, true);
        hMac.reset();
        return hMac.doFinal(secret);
    }

    /**
     * The method inits Hash-MAC with given PRK (as salt) and output OKM is calculated as follows:
     * <pre>
     *  T(0) = empty string (zero length)
     *  T(1) = HMAC-Hash(PRK, T(0) | info | 0x01)
     *  T(2) = HMAC-Hash(PRK, T(1) | info | 0x02)
     *  T(3) = HMAC-Hash(PRK, T(2) | info | 0x03)
     *  ...
     *  </pre>
     *
     * @param jceHmacAlgorithmName the java JCE HMAC algorithm name to use to expand
     *                             the key (e.g. HmacSHA256, HmacSHA384, HmacSHA512)
     * @param prk                  pseudo-random key derived from the shared secret
     * @param info                 used to derive the key
     * @param keyLength            key length in bytes of the derived key
     * @return the output keying material (OKM) size of keyLength octets
     * @throws XMLSecurityException if the jceHmacAlgorithmName is not supported
     */
    public byte[] expandKey(String jceHmacAlgorithmName, byte[] prk, byte[] info, long keyLength) throws XMLSecurityException {
        // prepare for expanding the key
        Mac hMac = initHMac(jceHmacAlgorithmName, prk, false);
        int iMacLength = hMac.getMacLength();

        int toGenerateSize = (int) keyLength;
        ByteBuffer result = ByteBuffer.allocate(toGenerateSize);
        byte[] prevResult = new byte[0];
        short counter = 1;
        while (toGenerateSize > 0) {
            hMac.reset();
            hMac.update(prevResult);
            if (info != null && info.length > 0) {
                hMac.update(info);
            }
            hMac.update((byte) counter);
            prevResult = hMac.doFinal();
            result.put(prevResult, 0, Math.min(toGenerateSize, iMacLength));
            // get ready for next iteration
            toGenerateSize -= iMacLength;
            counter++;
        }
        return result.array();
    }

    /**
     * Method initializes a Message Authentication Code (MAC) object using the
     * init secret/salt or an empty byte array if initSecret parameter is null or empty.
     *
     * @param jceAlgorithmName the java JCE HMAC algorithm name to use to init Mac
     * @param initSecret       the secret/salt to initialize the hmac
     * @param initPRK          if true, the salt is initialized with a string of zero octets
     *                         as long as the hash function output see [RFC5869] Section 2.2
     * @return Initialized Mac object
     * @throws XMLSecurityException if the hmac algorithm is not supported or if it
     *  fails to initialize
     */
    private Mac initHMac(String jceAlgorithmName, byte[] initSecret, boolean initPRK) throws XMLSecurityException {
        Mac mac;
        try {
            LOG.debug("Init Mac with hash algorithm: [{}] " + jceAlgorithmName);
            mac = Mac.getInstance(jceAlgorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new XMLSecurityException(e, "KeyDerivation.NotSupportedParameter", new Object[]{jceAlgorithmName});
        }

        if (initPRK && (initSecret == null || initSecret.length == 0)) {
            //  If "initSecret"/salt is not provided, a string of zero octets as long as the hash function output is used
            LOG.debug("Init Mac with hmac algorithm [{}] and empty salt! " + jceAlgorithmName);
            initSecret = new byte[mac.getMacLength()];
        }
        SecretKeySpec secretKey = new SecretKeySpec(initSecret, jceAlgorithmName);
        try {
            mac.init(secretKey);
        } catch (InvalidKeyException e) {
            throw new XMLSecurityException(e);
        }
        return mac;
    }
}
