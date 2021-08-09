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

import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

public interface SsoConfig {

    /*
     *
     * @return the SAML Version the instance id handling
     */
    public Constants.SamlSsoVersion getSamlVersion();

    /*
     * Saml20ProviderID could be changed during Server.xml dynamic changes
     * It oughts to be consistent with Saml20Service
     *
     * @return Saml20ProviderId
     */
    public String getProviderId();

    /*
     *
     * @return AuthenticationFilter if find one, otherwise null
     */
    public AuthenticationFilter getAuthFilter(ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef);

    /*
     *
     * @return AuthFilterID if find one, otherwise null
     */
    public String getAuthFilterId();

    /*
     *
     * @return clockSkew in milliSeconds
     */
    public long getClockSkew();

    public boolean isWantAssertionsSigned();

    public boolean isAuthnRequestsSigned();

    public boolean isIncludeX509InSPMetadata();

    public boolean isForceAuthn();

    public boolean isPassive();

    public Boolean getAllowCreate(); // this could be null, since no default value is set

    public String[] getAuthnContextClassRef();

    public String getAuthnContextComparisonType(); // exact, minimum, maximum or better

    public String getNameIDFormat();

    public String getIdpMetadata();

    public AcsDOMMetadataProvider getIdpMetadataProvider();

    public String getKeyStoreRef();

    public String getKeyAlias();

    @Sensitive
    public String getKeyPassword();

    public String getLoginPageURL();

    public String getErrorPageURL();

    public long getTokenReplayTimeout();

    public long getSessionNotOnOrAfter();

    public String getUserIdentifier();

    public String getGroupIdentifier();

    public String getUserUniqueIdentifier();

    public String getRealmIdentifier();

    public boolean isIncludeTokenInSubject();

    public Constants.MapToUserRegistry getMapToUserRegistry();

    public String getSignatureMethodAlgorithm();

    public boolean isDisableLtpaCookie();

    public String getSpCookieName(WsLocationAdmin locationAdmin);

    // trustEngine data
    public boolean isPkixTrustEngineEnabled();

    public List<String> getPkixX509CertificateList();

    public List<String> getPkixCrlList();

    public Collection<X509Certificate> getPkixTrustAnchors();

    /*
     *
     * @return the alive time period of a authnRequest
     */
    public long getAuthnRequestTime();

    public String[] getPkixTrustedIssuers();

    public boolean isEnabled();

    public String getRealmName();

    boolean isHttpsRequired();

    boolean isAllowCustomCacheKey();

    public String getSpHostAndPort();

    public boolean createSession();

    /**
     * @param configAdmin
     */
    public void setConfigAdmin(ConfigurationAdmin configAdmin);

    /**
     * @return
     */
    public Collection<X509CRL> getX509Crls();

    /* default Saml,saml,SAML */
    public String getHeaderName();

    /* default Saml,saml,SAML */
    public ArrayList<String> getHeaderNames();

    public String[] getAudiences();

    public boolean isReAuthnOnAssertionExpire();

    /* milliseconds */
    public long getReAuthnCushion();

    public String getTargetPageUrl();

    public boolean getUseRelayStateForTarget();

    public String getPostLogoutRedirectUrl();

    public boolean isServletRequestLogoutPerformsSamlLogout();

}
