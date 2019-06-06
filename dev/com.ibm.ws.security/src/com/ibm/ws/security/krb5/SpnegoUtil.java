/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API
 *******************************************************************************/
package com.ibm.ws.security.krb5;

import java.util.Arrays;
import java.util.StringTokenizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;

public class SpnegoUtil {
    public static final TraceComponent tc = Tr.register(SpnegoUtil.class);
    /*
     * 1.3.6.1.5.5.2 SPNEGO 0x06, 0x06, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02
     * 1.2.840.48018.1.2.2 MS Kerberos V5 0x06, 0x09, 0x2a, 0x86, 0x48, 0x82, 0xf7, 0x12, 0x01, 0x02,0x02
     * 1.2.840.113554.1.2.2 Kerberos V5 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x12, 0x01, 0x02, 0x02
     */
    public static final byte[] SPNEGO_OID = { 0x06, 0x06, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02 };

    // private static final byte[] MS_KRB5_OID = { 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x82, (byte) 0xf7, 0x12, 0x01, 0x02, 0x02 };
    public static final byte[] KRB5_OID = { 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x12, 0x01, 0x02, 0x02 };

    /**
     * @param authzHeader
     * @return
     */
    public String extractAuthzTokenString(String authzHeader) {
        String token = null;
        if (authzHeader != null) {
            StringTokenizer st = new StringTokenizer(authzHeader);
            if (st != null) {
                st.nextToken(); // skip the "Negotiate"
                if (st.hasMoreTokens()) {
                    token = st.nextToken();
                }
            }
        }
        return token;
    }

    /**
     * This method Checks to ensure that Authorization string
     * is an SPNEGO or Kerberos authentication token
     */
    public boolean isSpnegoOrKrb5Token(String authzHeader) {
        if (authzHeader == null || !authzHeader.startsWith("Negotiate "))
            return false;

        return isSpnegoOrKrb5Token(Base64Coder.base64Decode(Base64Coder.getBytes(extractAuthzTokenString(authzHeader))));

    }

    /**
     * @param tokenByte
     * @return
     */
    public boolean isSpnegoOrKrb5Token(byte[] tokenByte) {
        if (tokenByte == null || tokenByte.length == 0)
            return false;

        if (isSpnegoOrKrb5Oid(tokenByte, SPNEGO_OID)) {
            return true;
        } else if (isSpnegoOrKrb5Oid(tokenByte, KRB5_OID))
            return true;

        return false;
    }

    private boolean isSpnegoOrKrb5Oid(byte[] tokenByte, byte[] tokenType) {
        byte[] OIDfromToken = getMechOidFromToken(tokenByte, tokenType.length);
        if (OIDfromToken == null || OIDfromToken.length == 0)
            return false;

        if (Arrays.equals(OIDfromToken, tokenType)) {
            return true;
        }
        return false;
    }

    private byte[] getMechOidFromToken(@Sensitive byte[] tokenByte, int length) {
        int mechOidStart = 4;

        if (tokenByte == null || tokenByte.length < length + mechOidStart) {
            return null;
        }

        byte[] OIDfromToken = new byte[length];
        for (int i = 0; i < length; i++) {
            OIDfromToken[i] = tokenByte[i + mechOidStart];
        }
        return OIDfromToken;
    }
}
