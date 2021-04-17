/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.security.saml;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.ws.rs.ProcessingException;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.client.JAXRSClientConstants;

/**
 *
 */
public class PropagationHandler extends AbstractPhaseInterceptor<Message> {
    private static final TraceComponent tc = Tr.register(PropagationHandler.class, JAXRSClientConstants.TR_GROUP, JAXRSClientConstants.TR_RESOURCE_BUNDLE);

    public PropagationHandler() {
        super(Phase.PRE_LOGICAL);
    }

    @Override
    public void handleMessage(Message message) throws Fault {

        //see if the saml hanlder is used
        Object samlHandler = message.get(JAXRSClientConstants.SAML_HANDLER);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Please check if customer is using the [" + JAXRSClientConstants.SAML_HANDLER + "], client configuration property and the value should be true");
        }
        if (samlHandler != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The client configuration property [" + JAXRSClientConstants.SAML_HANDLER + "] value is " + samlHandler);
            }
            String handler = samlHandler.toString().toLowerCase();
            configClientSAMLHandler(message, handler);
        }
    }

    @FFDCIgnore({ NoClassDefFoundError.class })
    private void configClientSAMLHandler(Message message, String samlHander) {
        if (samlHander.equals("true")) {
            String saml = null;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Entering SAML Handler");
            }

            String address = (String) message.get(Message.ENDPOINT_ADDRESS);
            if (address.startsWith("https")) {
                // @TODO Not sure if we need this checking??
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "user is using SSL connection");
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "About to get a SAML authentication token from the runAs Subject");
            }

            // retrieve the saml token from the runAs Subject in current thread
            try {
                saml = getEncodedSaml20Token();

                if (saml != null && !saml.isEmpty()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Retrieved the encoded SAML token. About to set it on the request Header " + saml);
                    }
                    //Authorization=[saml="<SAML_HERE>"]
                    @SuppressWarnings("unchecked")
                    Map<String, List<String>> headers = (Map<String, List<String>>) message
                                    .get(Message.PROTOCOL_HEADERS);
                    headers.put("Authorization", Arrays.asList("SAML " + saml));
                    message.put(Message.PROTOCOL_HEADERS, headers);
                }
            } catch (NoClassDefFoundError ncdfe) {
                Tr.warning(tc, "failed_to_extract_saml_token_from_subject", ncdfe);
            } catch (java.lang.Throwable e) {
                Tr.warning(tc, "failed_to_extract_saml_token_from_subject", e);
                throw new ProcessingException(e);
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No client SAML handler configuration is specified, skipping this handler.");
            }
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