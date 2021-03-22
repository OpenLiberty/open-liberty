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
package com.ibm.ws.security.saml;

import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.UnsolicitedResponseCache;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;

public interface SsoSamlService {

    /*
     *
     * @return the SAML Version the instance is handling
     */
    public Constants.SamlSsoVersion getSamlVersion();

    /*
     * Saml20ProviderID could be changed during Server.xml dynamic changes
     *
     * @return Saml20ProviderId
     */
    public String getProviderId();

    public SsoConfig getConfig(); //

    public Cache getAcsCookieCache(String providerId);

    public UnsolicitedResponseCache getUnsolicitedResponseCache(String providerId);

    public WebProviderAuthenticatorHelper getAuthHelper();

    public String getAuthFilterId();

    public PrivateKey getPrivateKey() throws KeyStoreException, CertificateException;

    public Certificate getSignatureCertificate() throws KeyStoreException, CertificateException;

    /**
     * @param processedTrustAnchors
     * @param trustAnchors
     * @param trustAnchorName
     * @throws SamlException
     */
    boolean searchTrustAnchors(Collection<X509Certificate> trustAnchors, String trustAnchorName) throws SamlException;

    public boolean isEnabled();

    public boolean isInboundPropagation();
    
    public String getDefaultKeyStorePassword();
}
