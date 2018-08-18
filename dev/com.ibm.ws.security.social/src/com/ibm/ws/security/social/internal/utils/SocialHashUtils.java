/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * Utility class to generate hash code
 */
public class SocialHashUtils extends HashUtils {

    private static final TraceComponent tc = Tr.register(SocialHashUtils.class);

    @Sensitive
    public static String decodeString(SerializableProtectedString encodedStr) {
        if (encodedStr == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Encoded string was null");
            }
            return null;
        }
        String secret = new String(encodedStr.getChars());
        secret = PasswordUtil.passwordDecode(secret);
        return secret;

    }
}
