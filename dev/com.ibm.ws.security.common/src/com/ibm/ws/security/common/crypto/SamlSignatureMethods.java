/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.crypto.common;

import java.util.HashMap;
import java.util.Map;

import org.opensaml.xmlsec.signature.support.SignatureConstants;

public class SamlSignatureMethods {
    static final Map<String, Integer> validMethods = new HashMap<String, Integer>();
    static {
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1, 1); // "http://www.w3.org/2000/09/xmldsig#rsa-sha1", 1);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256, 256); // "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", 256);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA384, 384); // "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384", 384);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512, 512); // "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", 512);
    };

    public static int toInteger(String method) {
        Integer num = validMethods.get(method);
        return num != null ? num : 0;
    }
}
