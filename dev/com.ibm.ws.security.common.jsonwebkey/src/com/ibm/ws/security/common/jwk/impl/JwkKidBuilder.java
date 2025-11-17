/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.impl;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.ibm.ws.common.crypto.CryptoUtils;

public class JwkKidBuilder {

    public JwkKidBuilder() {

    }

    public String buildKeyId(Key cert) {
        if (cert != null && cert.getEncoded() != null) {
            byte[] certhash = null;
            try {
                certhash = MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA_256).digest(cert.getEncoded());
            } catch (NoSuchAlgorithmException e) {
            }
            if (certhash != null) {
                return org.jose4j.base64url.Base64Url.encode(certhash);
            }
        }
        return null;
    }
}
