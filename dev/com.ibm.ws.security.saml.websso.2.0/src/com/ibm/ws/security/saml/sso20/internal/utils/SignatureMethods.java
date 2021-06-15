/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    static final Map<String, Integer> validMethods = new HashMap<String, Integer>();
    static {
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1, 1); // "http://www.w3.org/2000/09/xmldsig#rsa-sha1", 1);
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
