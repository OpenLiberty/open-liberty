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
package io.openliberty.restfulWS.client.security;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import javax.security.auth.Subject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.HttpHeaders;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class SamlPropagationHandler {
    private static final TraceComponent tc = Tr.register(SamlPropagationHandler.class);

    @FFDCIgnore({ NoClassDefFoundError.class })
    public static void configClientSAMLHandler(ClientRequestContext crc) {
            String saml = null;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Entering SAML Handler - About to get a SAML authentication token from the runAs Subject");
            }

            // retrieve the saml token from the runAs Subject in current thread
            try {
                saml = getEncodedSaml20Token();

                if (saml != null && !saml.isEmpty()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Retrieved the encoded SAML token. About to set it on the request Header " + saml);
                    }
                    crc.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "SAML " + saml);
                }
            } catch (NoClassDefFoundError ncdfe) {
                Tr.warning(tc, "failed_to_extract_saml_token_from_subject", ncdfe);
            } catch (java.lang.Throwable e) {
                Tr.warning(tc, "failed_to_extract_saml_token_from_subject", e);
                throw new ProcessingException(e);
            }
    }

    @FFDCIgnore({ NoSuchMethodException.class })
    public static String getEncodedSaml20Token() {
        String base64Saml = null;
        String samlString = null;
        try {
            Subject subject = WSSubject.getRunAsSubject();

            for (Object credential : subject.getPrivateCredentials()) {
                try {
                    Class<?> credentialClass = credential.getClass();
                    Method method = credentialClass.getDeclaredMethod("getSAMLAsString");
                    samlString = (String) method.invoke(credential);
                    break;
                } catch (NoSuchMethodException e) {
                    continue;
                } catch (Exception e) {
                    Tr.warning(tc, "failed_to_extract_saml_token_from_subject", e.getLocalizedMessage());
                }
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while getting SAML token from subject:", e.getCause());
            }
            Tr.warning(tc, "failed_to_extract_saml_token_from_subject", e.getLocalizedMessage());
        }
        if (samlString != null) {
            byte output[] = samlString.getBytes(StandardCharsets.UTF_8);
            if (output != null) {
                base64Saml = Base64Coder.base64EncodeToString(output);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Error while trying to get token bytes using utf-8:");
                }
            }
        }

        return base64Saml;
    }
}