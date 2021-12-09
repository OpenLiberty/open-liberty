/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wssecurity.callback;

import java.io.IOException;
import java.io.StringReader;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.parsers.DocumentBuilder;

//import org.apache.ws.security.saml.ext.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLCallback;
//import org.opensaml.core.config.Configuration;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
//import org.opensaml.xml.parse.ParserPool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.sso.common.SsoService;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;
import com.ibm.ws.wssecurity.token.TokenUtils;

public class Saml20PropagationCallbackHandler implements CallbackHandler {
    protected static final TraceComponent tc = Tr.register(Saml20PropagationCallbackHandler.class,
                                                           WSSecurityConstants.TR_GROUP,
                                                           WSSecurityConstants.TR_RESOURCE_BUNDLE);

    final static String samlElementKey = "samlElement";

    /*
     * (non-Javadoc)
     * 
     * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "callbacks");
        }
        // For now, do not allow the saml to run when wss_saml bundle is not up
        SsoService wssSamlService = TokenUtils.getCommonSsoService(SsoService.TYPE_WSS_SAML); //"wssSaml");
        if (wssSamlService == null) {
            throw new IOException("The wsSecuritySaml-1.1 feature is not currently available. Make sure your server.xml has been configured to use the wsSecuritySaml-1.1 feature properly.");
        }

        // At least one callback must be specified in order to handle the request
        if (callbacks.length == 0) {
            Tr.error(tc, "no_callbacks_provided");
            String msg = TraceNLS.getFormattedMessage(this.getClass(),
                                                      WSSecurityConstants.TR_RESOURCE_BUNDLE,
                                                      "no_callbacks_provided",
                                                      new Object[] {},
                                                      "CWWKW0233E: No callbacks were provided to handle the request.");
            throw new IOException(msg);
        }

        // Check to make sure there's actually a SAML token in the subject
        boolean hasSaml = false;
        Saml20Token token = getSaml20TokenFromSubject();
        Element assertionElement = null;
        if (token != null) {
            assertionElement = getSamlElementFromToken(token);
            if (assertionElement != null) {
                hasSaml = true;
            }
        }
        if (!hasSaml) {
            Tr.error(tc, "no_saml_found_in_subject");
            String msg = TraceNLS.getFormattedMessage(this.getClass(),
                                                      WSSecurityConstants.TR_RESOURCE_BUNDLE,
                                                      "no_saml_found_in_subject",
                                                      new Object[] {},
                                                      "CWWKW0234E: The required SAML token is missing from the subject.");
            throw new IOException(msg);
        }

        for (int i = 0; i < callbacks.length; i++) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "callbacks:" + callbacks[i]);
            }
            if (callbacks[i] instanceof SAMLCallback) {
                // assertionElement = WSSecuritySsoServiceImpl.handleAssertionElement(assertionElement);
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                callback.setAssertionElement(assertionElement);
            }
        }

    }

    protected Element getSamlElementFromToken(Saml20Token token) {

        Element samlElement = null;
        try {
            // if the samlToken has the saml element then use it
            Map<String, Object> props = token.getProperties();
            if (props != null) {
                Element element = (Element) props.get(samlElementKey);
                if (element != null) {
                    samlElement = element;
                }
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "the element from getProperties():", samlElement);
            }
            if (samlElement == null) {
                String samlString = token.getSAMLAsString();

                // using the parser pool from openSaml
                net.shibboleth.utilities.java.support.xml.ParserPool parserPool;
                //StaticBasicParserPool ppMgr = (StaticBasicParserPool) Configuration.getParserPool();
                parserPool = XMLObjectProviderRegistrySupport.getParserPool();//v3
                //ParserPool pp = Configuration.//Configuration.getParserPool();
                DocumentBuilder builder = parserPool.getBuilder();

                Document document = builder.parse(new InputSource(new StringReader(samlString)));
                samlElement = document.getDocumentElement();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "the element from getSAMLAsString():", samlElement);
                }
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while extracting SAML element: ", e.getCause());
            }
            Tr.warning(tc, "failed_to_extract_saml_element", e.getLocalizedMessage());
        }
        return samlElement;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Saml20Token getSaml20TokenFromSubject() {
        Saml20Token samlToken = null;
        try {
            final Subject subject = WSSubject.getRunAsSubject();

            samlToken = (Saml20Token) AccessController.doPrivileged(
                            new PrivilegedExceptionAction() {
                                @Override
                                public Object run() throws Exception
                                {
                                    final Iterator authIterator = subject.getPrivateCredentials(Saml20Token.class).iterator();
                                    if (authIterator.hasNext()) {
                                        final Saml20Token token = (Saml20Token) authIterator.next();
                                        return token;
                                    }
                                    return null;
                                }
                            });
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while getting SAML token from subject:", e.getCause());
            }
            Tr.warning(tc, "failed_to_extract_saml_token_from_subject", e.getLocalizedMessage());
        }
        return samlToken;
    }
}
