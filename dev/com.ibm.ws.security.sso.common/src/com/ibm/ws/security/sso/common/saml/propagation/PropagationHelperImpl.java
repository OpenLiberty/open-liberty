/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.sso.common.saml.propagation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.common.internal.encoder.Base64Coder;

/**
 *
 */
public class PropagationHelperImpl {

    protected static final TraceComponent tc = Tr.register(PropagationHelperImpl.class,
                                                           TraceConstants.TRACE_GROUP,
                                                           TraceConstants.MESSAGE_BUNDLE);

    public static String getSAMLAsString() {
        Saml20Token token = getSaml20Token();
        String samlString = null;
        if (token != null) {
            samlString = token.getSAMLAsString();
        }
        return samlString;
    }

    public static Saml20Token getSaml20Token() {
        Saml20Token token = getSaml20TokenFromSubject();
        return token;
    }

    public static String getEncodedSaml20Token(boolean isCompressed) {
        String base64Saml = null;
        String samlString = getSAMLAsString();
        if (samlString != null) {
            if (isCompressed) {
                //compress and Base64 encode
                byte[] compressedTokenBytes = compressSamlToken(samlString);
                base64Saml = Base64Coder.base64EncodeToString(compressedTokenBytes);
            }
            else {
                byte output[] = null;
                try {
                    output = samlString.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // This should not happen.
                    // If it happens, it would be some runtime or operating system issue, so just give up and return null.
                    // ffdc data will be logged automatically.
                }
                if (output != null) {
                    base64Saml = Base64Coder.base64EncodeToString(output);
                }
                else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Error while trying to get token bytes using utf-8:");
                    }
                }

            }
        }

        return base64Saml;
    }

    private static byte[] compressSamlToken(String tokenString) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            byte output[] = null;
            try {
                if (tokenString != null) {
                    output = tokenString.getBytes("UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                // This should not happen.
                // If it happens, it would be some runtime or operating system issue, so just give up and return null.
                // ffdc data will be logged automatically.
            }
            if (output != null) {
                gzip.write(output);
            }
            else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error while trying to get token bytes using utf-8:");
                }
            }
            gzip.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            //e.printStackTrace();
            Tr.warning(tc, "failed_to_extract_saml_token_from_subject", e.getLocalizedMessage());
        }
        return out.toByteArray();

    }

    private static Saml20Token getSaml20TokenFromSubject() {
        Saml20Token samlToken = null;
        try {
            Subject subject = WSSubject.getRunAsSubject();

            samlToken = SamlCommonUtil.getSaml20TokenFromSubject(subject, false);
        } catch (WSSecurityException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while getting SAML token from subject:", e.getCause());
            }
            Tr.warning(tc, "failed_to_extract_saml_token_from_subject", e.getLocalizedMessage());
        }
        return samlToken;
    }

}
