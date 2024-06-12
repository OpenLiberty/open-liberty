/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.common.crypto;

import org.opensaml.xmlsec.signature.support.SignatureConstants;

public class SamlSignatureUtils {

    public static String getSignatureMethodAlgorithm(String configName, String signatureMethodAlgorithm) {
        // Set the default algorithm
        String algorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;

        if ("SHA1".equalsIgnoreCase(signatureMethodAlgorithm)) {
            CryptoMessageUtils.logUnsecureAlgorithm(configName, signatureMethodAlgorithm);
            algorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
        } else if ("SHA128".equalsIgnoreCase(signatureMethodAlgorithm)) {
            return SignatureConstants.MORE_ALGO_NS + "rsa-sha128"; //??????? from the original code
        } else if ("SHA384".equalsIgnoreCase(signatureMethodAlgorithm)) {
            algorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA384;
        } else if ("SHA512".equalsIgnoreCase(signatureMethodAlgorithm)) {
            algorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512;
        }
 
        return algorithm;
    }
}
