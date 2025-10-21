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

import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.xmlsec.signature.support.SignatureConstants;

/**
 *
 */
public class SignatureMethods {
    private static enum SignatureMethodType {
        RSA,
        ECDSA,
        UNSUPPORTED
    }

    static final Map<String, Integer> validMethods = new HashMap<String, Integer>();
    static {
        // FIPS 140-3: Algorithm assessment complete; no changes required.
        // Constant definition of SHA-1 which will be used for toInteger comparison check in SAMLMessageXMLSignatureSecurityPolicyRule.java
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1, 1); // "http://www.w3.org/2000/09/xmldsig#rsa-sha1", 1);

        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA256, 256); // "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256", 256);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA384, 384); // "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384", 384);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA512, 512); // "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512", 512);

        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256, 256); // "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", 256);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA384, 384); // "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384", 384);
        validMethods.put(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512, 512); // "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", 512);
    };

    private static SignatureMethodType getSignatureMethodType(String samlMessageMethod) {
        if (samlMessageMethod.contains("ecdsa")) {
            return SignatureMethodType.ECDSA;
        }
        if (samlMessageMethod.contains("rsa")) {
            return SignatureMethodType.RSA;
        }
        return SignatureMethodType.UNSUPPORTED;
    }

    public static boolean isInboundSignatureMethodWeakerThanConfigured(String inboundMessageMethod, String configuredMethod) throws MessageHandlerException {
        boolean isWeaker = false;

        SignatureMethodType inboundMessageMethodType = getSignatureMethodType(inboundMessageMethod);
        SignatureMethodType configuredMethodType = getSignatureMethodType(configuredMethod);

        if (inboundMessageMethodType == SignatureMethodType.UNSUPPORTED) {
            throw new MessageHandlerException("SAML assertion is signed with an unsupported signature method algorithm \"" + inboundMessageMethodType + "\".");
        }

        if (inboundMessageMethodType.equals(configuredMethodType)) {
            isWeaker = toInteger(inboundMessageMethod) < toInteger(configuredMethod);
        } else if (inboundMessageMethodType == SignatureMethodType.RSA && configuredMethodType == SignatureMethodType.ECDSA) {
            isWeaker = true;
        }

        return isWeaker;
    }

    public static int toInteger(String method) {
        Integer num = validMethods.get(method);
        if (num != null) {
            return num.intValue();
        }
        return 0;
    }
}
