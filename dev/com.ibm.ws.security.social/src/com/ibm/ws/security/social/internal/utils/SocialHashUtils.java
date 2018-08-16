/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
