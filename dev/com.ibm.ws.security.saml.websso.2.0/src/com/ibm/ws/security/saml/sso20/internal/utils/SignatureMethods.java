/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.util.HashMap;
import java.util.Map;

import org.opensaml.xmlsec.signature.support.SignatureConstants;

/**
 *
 */
public class SignatureMethods {
    private static final int EC_TYPE_BIAS = 1000;

    static final Map<String, Integer> validMethods = new HashMap<String, Integer>();
    static {
        // FIPS 140-3: Algorithm assessment complete; no changes required.
        // Constant definition of SHA-1 which will be used for toInteger comparison check in SAMLMessageXMLSignatureSecurityPolicyRule.java
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1, 1); // "http://www.w3.org/2000/09/xmldsig#rsa-sha1", 1);

        // ECDSA algorithms are given bias to evaluate EC to be stronger than RSA
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA256, EC_TYPE_BIAS + 256); // "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256", 256);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA384, EC_TYPE_BIAS + 384); // "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384", 384);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA512, EC_TYPE_BIAS + 512); // "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512", 512);

        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256, 256); // "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", 256);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA384, 384); // "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384", 384);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512, 512); // "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", 512);
    };

    public static int toInteger(String method) {
        Integer num = validMethods.get(method);
        if (num != null) {
            return num.intValue();
        }
        return 0;
    }
}
