/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.crypto;

import java.security.Key;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class KeyAlgorithmChecker {

    private static final TraceComponent tc = Tr.register(KeyAlgorithmChecker.class);

    public static int UNKNOWN_HASH_SIZE = 0;

    public boolean isHSAlgorithm(String alg) {
        if (alg == null) {
            return false;
        }
        return alg.matches("HS[0-9]{3,}");
    }

    public boolean isPublicKeyValidType(Key key, String supportedSigAlg) {
        if (key == null || supportedSigAlg == null) {
            // Rely on caller to do the appropriate checks if the key or algorithm is null
            return true;
        }
        if (isRSAlgorithm(supportedSigAlg)) {
            return isValidRSAPublicKey(key);
        } else if (isESAlgorithm(supportedSigAlg)) {
            return isValidECPublicKey(supportedSigAlg, key);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Did not find matching algorithm support for [" + supportedSigAlg + "]");
        }
        return false;
    }

    public boolean isRSAlgorithm(String alg) {
        if (alg == null) {
            return false;
        }
        return alg.matches("RS[0-9]{3,}");
    }

    public boolean isValidRSAPublicKey(Key key) {
        String keyAlgorithm = key.getAlgorithm();
        // TODO - any way to check hash bit size?
        return (keyAlgorithm.equals("RSA") && key instanceof RSAPublicKey);
    }

    public boolean isESAlgorithm(String alg) {
        if (alg == null) {
            return false;
        }
        return alg.matches("ES[0-9]{3,}");
    }

    public boolean isValidECPublicKey(String supportedSigAlg, Key key) {
        if (!("EC".equals(key.getAlgorithm()) && key instanceof ECPublicKey)) {
            return false;
        }
        return isValidECKeyParameters(supportedSigAlg, (ECPublicKey) key);
    }

    boolean isValidECKeyParameters(String supportedSigAlg, ECKey key) {
        ECParameterSpec params = key.getParams();
        int fieldSize = params.getCurve().getField().getFieldSize();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Comparing supported algorithm [" + supportedSigAlg + "] against key field size [" + fieldSize + "]");
        }
        int supportedAlgKeySize = getHashSizeFromAlgorithm(supportedSigAlg);
        if (fieldSize == 521) {
            // Special case
            return supportedAlgKeySize == 512;
        }
        return (supportedAlgKeySize == fieldSize);
    }

    /**
     * Extracts the hash size from algorithm strings such as RS256, HS384, or ES512.
     */
    @FFDCIgnore(Exception.class)
    public int getHashSizeFromAlgorithm(String algorithm) {
        int hashSize = UNKNOWN_HASH_SIZE;
        String algRegex = "[RHEP]S([0-9]{3,})";
        Pattern algPattern = Pattern.compile(algRegex, Pattern.CASE_INSENSITIVE);
        Matcher algMatcher = algPattern.matcher(algorithm);
        if (!algMatcher.matches()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Algorithm [" + algorithm + "] did not match expected regex " + algRegex);
            }
            return hashSize;
        }
        String hashSizeString = algMatcher.group(1);
        try {
            hashSize = Integer.parseInt(hashSizeString);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception parsing hash size string [" + hashSizeString + "]: " + e);
            }
            return hashSize;
        }
        return hashSize;
    }

    public boolean isPrivateKeyValidType(Key key, String supportedSigAlg) {
        if (key == null || supportedSigAlg == null) {
            // Rely on caller to do the appropriate checks if the key or algorithm is null
            return true;
        }
        if (isRSAlgorithm(supportedSigAlg)) {
            return isValidRSAPrivateKey(key);
        } else if (isESAlgorithm(supportedSigAlg)) {
            return isValidECPrivateKey(supportedSigAlg, key);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Did not find matching algorithm support for [" + supportedSigAlg + "]");
        }
        return false;
    }

    public boolean isValidRSAPrivateKey(Key key) {
        String keyAlgorithm = key.getAlgorithm();
        // TODO - any way to check hash bit size?
        return (keyAlgorithm.equals("RSA") && key instanceof RSAPrivateKey);
    }

    public boolean isValidECPrivateKey(String supportedSigAlg, Key key) {
        if (!("EC".equals(key.getAlgorithm()) && key instanceof ECPrivateKey)) {
            return false;
        }
        return isValidECKeyParameters(supportedSigAlg, (ECPrivateKey) key);
    }

}
