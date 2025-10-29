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

import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.encryption.XMLCipherUtil;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.encryption.params.ConcatKDFParams;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.utils.I18n;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Key DerivationAlgorithm implementation, defined in Section 5.8.1 of NIST SP 800-56A [SP800-56A], and is equivalent
 * to the KDF3 function defined in ANSI X9.44-2007 [ANSI-X9-44-2007] when the contents of the OtherInfo parameter
 * is structured as in NIST SP 800-56A.
 * <p>
 * Identifier of the key derivation algorithm:  http://www.w3.org/2009/xmlenc11#ConcatKDF
 */
public class ConcatKDF implements DerivationAlgorithm<ConcatKDFParams> {

        private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ConcatKDF.class);

    /**
     * Derives a key from the shared secret and other concat kdf parameters.
     *
     * @param sharedSecret The "shared" secret used for the key derivation (e.g. the secret key)
     * @param concatKDFParams The concat key derivation parameters
     * @return the derived key bytes
     * @throws IllegalArgumentException if the concat KDF parameters are not set
     * @throws XMLSecurityException if the key derivation parameters are invalid
     */
    @Override
    public byte[] deriveKey(byte[] sharedSecret, ConcatKDFParams concatKDFParams) throws XMLSecurityException {

        // check if the parameters are set
        if (concatKDFParams == null) {
            throw new IllegalArgumentException(I18n.translate("KeyDerivation.MissingParameters"));
        }

        // concatenate the bitstrings in following order algID || partyUInfo || partyVInfo || suppPubInfo || suppPrivInfo
        final byte[] otherInfo = concatParameters(concatKDFParams.getAlgorithmID(),
                concatKDFParams.getPartyUInfo(), concatKDFParams.getPartyVInfo(),
                concatKDFParams.getSuppPubInfo(), concatKDFParams.getSuppPrivInfo());

        // get the digest algorithm
        MessageDigest digest = MessageDigestAlgorithm.getDigestInstance(concatKDFParams.getDigestAlgorithm());
        int genKeyLength = concatKDFParams.getKeyLength();

        int iDigestLength = digest.getDigestLength();
        if (genKeyLength / (long) iDigestLength > Integer.MAX_VALUE) {
            throw new XMLSecurityException("KeyDerivation.InvalidParameter", new Object[]{"key length"} );
        }
        int toGenerateSize = genKeyLength;

        digest.reset();
        ByteBuffer indexBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);

        ByteBuffer result = ByteBuffer.allocate(toGenerateSize);

        int counter = 1;
        while (toGenerateSize > 0) {
            indexBuffer.position(0);
            indexBuffer.putInt(counter++);
            indexBuffer.position(0);
            digest.update(indexBuffer);
            digest.update(sharedSecret);
            if (otherInfo != null && otherInfo.length > 0) {
                digest.update(otherInfo);
            }
            result.put(digest.digest(), 0, Math.min(toGenerateSize, iDigestLength));
            toGenerateSize -= iDigestLength;
        }

        return result.array();
    }

    /**
     * Simple method to concatenate non-padded bitstream ConcatKDF parameters.
     * If parameters are null the value is ignored.
     *
     * @param parameters the parameters to concatenate
     * @return the concatenated parameters as byte array
     */
    private static byte[] concatParameters(final String... parameters) throws XMLEncryptionException {

        List<byte[]> byteParams = new ArrayList<>();
        for (String parameter : parameters) {
            byte[] bytes = parseBitString(parameter);
            byteParams.add(bytes);
        }
        // get bytearrays size
        int iSize = byteParams.stream().map(ConcatKDF::getSize).reduce(0, Integer::sum);

        ByteBuffer buffer = ByteBuffer
                .allocate(iSize);
        byteParams.forEach(buffer::put);
        return buffer.array();
    }

    /**
     * The method validates the bitstring parameter structure and returns byte array of the parameter.
     * <p/>
     * The bitstring is divided into octets using big-endian encoding. Parameter starts with two characters (hex number)
     * defining the number of padding bits followed by hex-string. The length of the bitstring is not a multiple of 8
     * then add padding bits (value 0) as necessary to the last octet to make it a multiple of 8.
     *
     * @param kdfParameter the parameter to parse
     * @return the parsed parameter as byte array
     */
    private static byte[] parseBitString(final String kdfParameter) throws XMLEncryptionException {
        // ignore empty parameters
        if (kdfParameter == null || kdfParameter.isEmpty()) {
            return new byte[0];
        }
        String kdfP = kdfParameter.trim();
        int paramLen = kdfP.length();
        // bit string must have two chars following by first byte defining the number of padding bits
        if (paramLen < 4) {
            LOG.error("ConcatKDF parameter is to short");
            throw new XMLEncryptionException( "KeyDerivation.TooShortParameter", kdfParameter);
        }
        if (paramLen % 2 != 0) {
            LOG.error("Invalid length of ConcatKDF parameter [{0}]!", kdfP);
            throw new XMLEncryptionException( "KeyDerivation.InvalidParameter", kdfParameter);
        }
        int iPadding;
        String strPadding = kdfP.substring(0, 2);
        try {
            iPadding = Integer.parseInt(strPadding, 16);
        } catch (NumberFormatException e) {
            LOG.error("Invalid padding number: [{0}]! Number is not Hexadecimal!", strPadding);
            throw new XMLEncryptionException(e, "KeyDerivation.InvalidParameter", new Object[]{kdfParameter});
        }

        if (iPadding != 0) {
            LOG.error("Padded ConcatKDF parameters are not supported");
            throw new XMLEncryptionException( "KeyDerivation.NotSupportedParameter", kdfParameter);
        }
        // skip first two chars since they are padding bytes,
        kdfP = kdfP.substring(2);
        return XMLCipherUtil.hexStringToByteArray(kdfP);
    }

    /**
     * Method returns the size of the array or 0 if the array is null.
     *
     * @param array the array to get the size from
     * @return the size of the array or 0 if the array is null
     */
    private static int getSize(byte[] array) {
        return array == null ? 0 : array.length;
    }
}
