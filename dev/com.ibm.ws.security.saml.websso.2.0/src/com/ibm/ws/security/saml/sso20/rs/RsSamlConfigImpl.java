/*******************************************************************************
 * Copyright (c) 2021,2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.rs;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.PkixTrustEngineConfig;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class RsSamlConfigImpl extends PkixTrustEngineConfig implements SsoConfig {
    public static final TraceComponent tc = Tr.register(RsSamlConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    public static final String KEY_ID = "id";
    public static final Object KEY_PROVIDER_ID = "id";
    public static final String KEY_SERVICE_PID = "service.pid";
    public static final String CFG_KEY_AUTH_FILTER_REF = "authFilterRef";

    public static final String KEY_clockSkew = "clockSkew";
    public static final String KEY_authnRequestTime = "authnRequestTime";
    private String providerId = null;
    private Map<String, Object> props = null;
    private volatile ConfigurationAdmin configAdmin = null;
    private HashMap<String, String> filterIdMap = null;

    static final String KEY_wantAssertionsSigned = "wantAssertionsSigned";
    static final String KEY_includeX509InSPMetadata = "includeX509InSPMetadata";
    static final String KEY_signatureMethodAlgorithm = "signatureMethodAlgorithm";
    static final String KEY_authnRequestsSigned = "authnRequestsSigned";
    static final String KEY_forceAuthn = "forceAuthn";
    static final String KEY_isPassive = "isPassive";
    static final String KEY_allowCreate = "allowCreate";
    static final String KEY_authnContextClassRef = "authnContextClassRef";
    static final String KEY_authnContextComparisonType = "authnContextComparisonType";
    static final String KEY_nameIDFormat = "nameIDFormat";
    static final String KEY_customizeNameIDFormat = "customizeNameIDFormat";
    static final String KEY_idpMetadata = "idpMetadata";
    static final String KEY_keyStoreRef = "keyStoreRef";
    static final String KEY_keyAlias = "keyAlias";
    static final String KEY_keyPassword = "keyPassword";
    static final String KEY_loginPageURL = "loginPageURL";
    static final String KEY_errorPageURL = "errorPageURL";
    static final String KEY_tokenReplayTimeout = "tokenReplayTimeout";
    static final String KEY_sessionNotOnOrAfter = "sessionNotOnOrAfter";
    static final String KEY_userIdentifier = "userIdentifier";
    static final String KEY_groupIdentifier = "groupIdentifier";
    static final String KEY_userUniqueIdentifier = "userUniqueIdentifier";
    static final String KEY_realmIdentifier = "realmIdentifier";
    static final String KEY_includeTokenInSubject = "includeTokenInSubject";
    static final String KEY_mapToUserRegistry = "mapToUserRegistry";
    static final String KEY_disableLtpaCookie = "disableLtpaCookie";
    static final String KEY_spCookieName = "spCookieName";
    static final String KEY_realmName = "realmName";
    static final String KEY_headerName = "headerName";
    static final String KEY_audiences = "audiences";
    static final String KEY_spHostAndPort = "spHostAndPort";
    static final String KEY_trustedIssuers = "trustedIssuers";

    static final String KEY_enabled = "enabled";

    static final String KEY_httpsRequired = "httpsRequired";
    static final String KEY_allowCustomCacheKey = "allowCustomCacheKey";
    static final String KEY_createSession = "createSession";
    static final String KEY_reAuthnOnAssertionExpire = "reAuthnOnAssertionExpire";
    static final String KEY_reAuthnCushion = "reAuthnCushion";
    static final String KEY_targetPageUrl = "targetPageUrl";
    static final String KEY_useRelayStateForTarget = "useRelayStateForTarget";
    static final String KEY_servletRequestLogoutPerformsSamlLogout = "spLogout";

    static final String[] notInUseAttributes = new String[] {
                                                              KEY_authnRequestsSigned, KEY_forceAuthn, KEY_isPassive,
                                                              KEY_allowCreate, KEY_authnContextClassRef, KEY_authnContextComparisonType,
                                                              KEY_nameIDFormat, KEY_customizeNameIDFormat, KEY_idpMetadata,
                                                              KEY_loginPageURL, KEY_errorPageURL, KEY_tokenReplayTimeout,
                                                              KEY_sessionNotOnOrAfter, KEY_includeTokenInSubject, KEY_spCookieName,
                                                              KEY_spHostAndPort, KEY_targetPageUrl, KEY_httpsRequired,
                                                              KEY_allowCustomCacheKey, KEY_createSession, KEY_reAuthnOnAssertionExpire,
                                                              KEY_reAuthnCushion
    };

    static final String ignoreAttributes;
    static {
        String tmpStr = notInUseAttributes[0];
        for (int iI = 1; iI < notInUseAttributes.length; iI++) {
            tmpStr = tmpStr.concat(", ").concat(notInUseAttributes[iI]);
        }
        ignoreAttributes = tmpStr;
    }

    boolean bInit = false;
    boolean enabled = true;
    ComponentContext cc = null;
    SsoSamlService parentSsoService = null;

    long clockSkewMilliSeconds = 300000; // 5 minutes
    String keyStoreRef = null;
    String keyAlias = null;
    String keyPassword = null;
    String signatureMethodAlgorithm = "SHA256";
    String userIdentifier = "NameID";
    String groupIdentifier = null;
    String userUniqueIdentifier = "NameID";
    String realmIdentifier = "issuer";
    boolean includeTokenInSubject = true;
    boolean httpsRequired = true;

    Constants.MapToUserRegistry mapToUserRegistry = Constants.MapToUserRegistry.No;
    boolean disableLtpaCookie = true;
    boolean wantAssertionsSigned = true;
    boolean includeX509InSPMetadata = true;
    String realmName = null;

    String headerName = null;
    String[] arrayHeaderNames = null;
    ArrayList<String> headerNames = null;
    String[] audiences = new String[] { "ANY" };
    private final String bundleLocation;
    private boolean servletRequestLogoutPerformsSamlLogout = false;

    public RsSamlConfigImpl(ComponentContext cc,
                            Map<String, Object> props,
                            ConfigurationAdmin configAdmin,
                            HashMap<String, String> filterIdMap,
                            SsoSamlService parentSsoService) {
        this.parentSsoService = parentSsoService;
        this.cc = cc;
        this.bundleLocation = cc.getBundleContext().getBundle().getLocation();
        try {
            setConfig(props, configAdmin, filterIdMap);
        } catch (SamlException e) {
            // Handle the Error message, in case, it dir not
            // For now, yes, it displays error message
        }
    }

    public void setConfig(Map<String, Object> props, ConfigurationAdmin configAdmin, HashMap<String, String> filterIdMap) throws SamlException {
        bInit = true;
        providerId = (String) props.get(KEY_PROVIDER_ID);
        this.props = props;
        this.filterIdMap = filterIdMap;
        setConfigAdmin(configAdmin);
        processProps(props);
        if (providerId == null || providerId.isEmpty()) {
            // SP ID cannot be empty since it is required in order to construct proper URLs and other information for the SP
            Tr.error(tc, "SAML20_SP_ID_ATTRIBUTE_EMPTY");
        }
    }

    /**
     * @throws Exception
     *
     */
    private void processProps(Map<String, Object> props) {
        // warn users on the ignored attributes
        Tr.warning(tc, "SAML_CONFIG_IGNORE_ATTRIBUTES", new Object[] { "true", ignoreAttributes, providerId });

        clockSkewMilliSeconds = (Long) props.get(KEY_clockSkew); // milliseconds
        signatureMethodAlgorithm = trim((String) props.get(KEY_signatureMethodAlgorithm));
        if (CryptoUtils.isAlgorithmInsecure(signatureMethodAlgorithm)) {
            CryptoUtils.logInsecureAlgorithm(KEY_signatureMethodAlgorithm, signatureMethodAlgorithm);
        }
        userIdentifier = trim((String) props.get(KEY_userIdentifier));
        groupIdentifier = trim((String) props.get(KEY_groupIdentifier));
        userUniqueIdentifier = trim((String) props.get(KEY_userUniqueIdentifier));
        if (userUniqueIdentifier == null || userUniqueIdentifier.isEmpty()) {
            userUniqueIdentifier = userIdentifier;
        }
        realmIdentifier = trim((String) props.get(KEY_realmIdentifier));
        mapToUserRegistry = Constants.MapToUserRegistry.valueOf((String) props.get(KEY_mapToUserRegistry));
        disableLtpaCookie = (Boolean) props.get(KEY_disableLtpaCookie);
        wantAssertionsSigned = (Boolean) props.get(KEY_wantAssertionsSigned);
        includeX509InSPMetadata = (Boolean) props.get(KEY_includeX509InSPMetadata);
        realmName = trim((String) props.get(KEY_realmName));
        arrayHeaderNames = trim((String[]) props.get(KEY_headerName));
        if (arrayHeaderNames == null || arrayHeaderNames.length == 0) {
            arrayHeaderNames = new String[] { "Saml", "saml", "SAML" }; // 204127
        }
        headerName = Array2String(arrayHeaderNames);
        audiences = trim((String[]) props.get(KEY_audiences));

        keyStoreRef = trim((String) props.get(KEY_keyStoreRef));
        keyAlias = trim((String) props.get(KEY_keyAlias));
        keyPassword = getPassword((SerializableProtectedString) props.get(KEY_keyPassword));
        enabled = (Boolean) props.get(KEY_enabled);
        servletRequestLogoutPerformsSamlLogout = (Boolean) props.get(KEY_servletRequestLogoutPerformsSamlLogout);

        // Handle the tc debug
        processPkixTrustEngine(props);
        if (trustedIssuers == null || trustedIssuers.length < 1) {
            // we have to set it as TRUST_ALL_ISSUERS in case it's empty
            // otherwise, it will fail later when validateIssuer
            // since rsSaml does not have idp-metadata.xml
            trustedIssuers = new String[] { Constants.TRUST_ALL_ISSUERS };
        }
    }

    /**
     * @param trustedIssuers
     * @return
     */
    String Array2String(String[] trustedIssuers) {
        String result = "";
        for (int iI = 0; iI < trustedIssuers.length; iI++) {
            if (iI > 0) {
                result = result.concat(", ");
            }
            result = result.concat(trustedIssuers[iI]);
        }
        return result;
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public String getUserIdentifier() {
        return userIdentifier;
    }

    @Override
    public String getGroupIdentifier() {
        return groupIdentifier;
    }

    @Override
    public String getUserUniqueIdentifier() {
        return userUniqueIdentifier;
    }

    @Override
    public String getRealmIdentifier() {
        return realmIdentifier;
    }

    @Override
    public Constants.MapToUserRegistry getMapToUserRegistry() {
        return mapToUserRegistry;
    }

    @Override
    public String toString() {
        String result = "Not initialized yet";
        if (bInit) {
            result = "\nconfigId:" + providerId
                     + "\nuserIdentifier:" + userIdentifier
                     + "\ngroupIdentifier:" + groupIdentifier
                     + "\nuserUniqueIdentifier:" + userUniqueIdentifier
                     + "\nrealmIdentifier:" + realmIdentifier
                     + "\nincludeTokenInSubject:" + includeTokenInSubject
                     + "\nmapUserIdentifierToUserRegistry:" + mapToUserRegistry
                     + "\ndisableLtpaCookie:" + disableLtpaCookie
                     + "\nwantAssertionsSigned:" + wantAssertionsSigned
                     + "\nrealmName:" + realmName
                     + "\nheaderName:" + headerName
                     + "\nclockSkew:" + clockSkewMilliSeconds
                     + (audiences == null || audiences.length < 0 ? "\naudiences=null" : "\naudiences(" + audiences.length + "):" + audiences[0])
                     + "\nkeyStoreRef:" + keyStoreRef
                     + "\nkeyAlias:" + keyAlias
                     + "\nkeyPassword exists:" + (keyPassword != null)
                     + "\nsignatureMethodAlgorithm:" + signatureMethodAlgorithm
                     + "\nSize pkixX509List:" + pkixX509List.size()
                     + "\nSize pkixCrlList:" + pkixCrlList.size()
                     + "\ntrustAnchorName:" + trustAnchorName
                     + "\nincludeX509InSPMetadata:" + includeX509InSPMetadata
                     + "\nservletRequestLogoutPerformsSamlLogout: " + servletRequestLogoutPerformsSamlLogout;
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getSignatureMethodAlgorithm()
     */
    @Override
    public String getSignatureMethodAlgorithm() {
        if ("SHA256".equalsIgnoreCase(signatureMethodAlgorithm)) {
            return SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;
        } else if ("SHA128".equalsIgnoreCase(signatureMethodAlgorithm)) {
            return SignatureConstants.MORE_ALGO_NS + "rsa-sha128"; //???????
        } else if ("SHA1".equalsIgnoreCase(signatureMethodAlgorithm)) {
            return SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
        }
        return SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isDisableLtpaCookie()
     */
    @Override
    public boolean isDisableLtpaCookie() {
        return disableLtpaCookie;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getRealmName()
     */
    @Override
    public String getRealmName() {
        return realmName;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public ArrayList<String> getHeaderNames() {
        if (headerNames == null) {
            // lazy initialization.
            headerNames = new ArrayList<String>();
            // arrayHeaderNames will always have values by default
            for (int iCnt = 0; iCnt < arrayHeaderNames.length; iCnt++) {
                String nextToken = arrayHeaderNames[iCnt].trim();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "headerName(" + (iCnt) + "):" + nextToken);
                }
                headerNames.add(nextToken);
            }
        }
        return (ArrayList<String>) headerNames.clone(); // prevent any accidental updates
    }

    /** {@inheritDoc} */
    @Override
    public String getHeaderName() {
        return headerName;
    }

    /** {@inheritDoc} */
    @Override
    public long getClockSkew() {
        return clockSkewMilliSeconds;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getAudiences() {
        if (audiences == null) {
            return new String[0];
        }
        return audiences.clone();
    }

    @Override
    public String getAuthFilterId() {
        if (props != null) {
            String authFilterRef = (String) props.get(CFG_KEY_AUTH_FILTER_REF);
            return getAuthFilterId(authFilterRef);
        }
        return null;
    }

    public String getAuthFilterId(String authFilterRef) {
        if (authFilterRef == null || authFilterRef.isEmpty())
            return null;
        String id = filterIdMap.get(authFilterRef);
        if (id != null) {
            return id;
        }
        Configuration config = null;
        try {
            if (configAdmin != null)
                config = configAdmin.getConfiguration(authFilterRef, null);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid authFilterRef configuration", e.getMessage());
            }
            return null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "authFilterRef configuration", config);
        }
        if (config == null)
            return null;
        Dictionary<String, Object> props = config.getProperties();
        if (props == null)
            return null;
        id = (String) props.get(KEY_ID);
        return id;
    }

    /**
     * @param authFilterServiceRef
     * @return
     */
    @Override
    public AuthenticationFilter getAuthFilter(ConcurrentServiceReferenceMap<String, AuthenticationFilter> authFilterServiceRef) {
        if (props != null) {
            String authFilterRef = (String) props.get(CFG_KEY_AUTH_FILTER_REF);
            if (authFilterRef != null && !authFilterRef.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    String id = getAuthFilterId(authFilterRef);
                    Tr.debug(tc, providerId + "> Ref:" + authFilterRef + " id:" + id);
                }
                AuthenticationFilter authnFilter = authFilterServiceRef.getService(authFilterRef);
                if (authnFilter == null) { // if authnFilter points to none, then we accept all the request...
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "AuthnFilter Ref:" + authFilterRef + " points to no AuthnFilter, we accept all the requests");
                    }
                    //authnFilter = new DefaultAuthenticationFilter();
                    //authnFilter.setProcessAll(false);
                    Tr.error(tc, "SAML20_AUTH_FILTER_NOT_EXISTING",
                             new Object[] { getAuthFilterId(authFilterRef), providerId });
                    // SAML20_AUTH_FILTER_NOT_EXISTING=CWWKS5075E: Cannot find an instance of authnFilter for the authFilterRef [{0}] in Service Provider [{1}]. Please correct the configuration as soon as possible.
                }
                return authnFilter;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWantAssertionsSigned() {
        return wantAssertionsSigned;
    }

    /**
     * @param props
     * @throws Exception
     */
    private void processPkixTrustEngine(Map<String, Object> props) {
        try {
            super.processPkixTrustEngine(props, configAdmin, bundleLocation);
        } catch (Exception e) {

        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getPkixX509CertificateList() {
        return pkixX509List;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getPkixCrlList() {
        return pkixCrlList;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<X509Certificate> getPkixTrustAnchors() {
        Collection<X509Certificate> pkixTrustAnchors = new ArrayList<X509Certificate>();
        try {
            parentSsoService.searchTrustAnchors(pkixTrustAnchors, trustAnchorName);
            addX509Certs(pkixTrustAnchors);
        } catch (SamlException e) {
            // ignore it. The SSL keyStoreService may not be ready
        }

        return pkixTrustAnchors;
    }

    /** {@inheritDoc} */
    public String getPkixTrustAnchorName() {
        return trustAnchorName;
    }

    @Override
    public String getKeyStoreRef() {
        return keyStoreRef;
    }

    @Override
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    @Sensitive
    public String getKeyPassword() {
        return keyPassword;
    }

    /**
     * @return
     */
    @Sensitive
    String getPassword(SerializableProtectedString keyPassword) {
        if (keyPassword == null || keyPassword.isEmpty()) {
            return null;
        } else {
            String encoded_string = new String(keyPassword.getChars());
            // decode
            return PasswordUtil.passwordDecode(encoded_string);
        }
    }

    /*
     *
     * @return the SAML Version the instance id handling
     */
    @Override
    public Constants.SamlSsoVersion getSamlVersion() {
        return Constants.SamlSsoVersion.SAMLSSO20;
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isAuthnRequestsSigned()
     */
    @Override
    public boolean isAuthnRequestsSigned() {
        return unexpectedCall(false);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isForceAuthn()
     */
    @Override
    public boolean isForceAuthn() {
        return unexpectedCall(false);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isPassive()
     */
    @Override
    public boolean isPassive() {
        return unexpectedCall(false);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getAllowCreate()
     */
    @Override
    public Boolean getAllowCreate() { // this could be null, since no default value is set
        return unexpectedCall(false);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getAuthnContextClassRef()
     */
    @Override
    public String[] getAuthnContextClassRef() {
        return unexpectedCall(new String[0]);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getAuthnContextComparisonType()
     */
    @Override
    public String getAuthnContextComparisonType() { // exact, minimum, maximum or better
        return unexpectedCall("exact");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getNameIDFormat()
     */
    @Override
    public String getNameIDFormat() {
        return unexpectedCall((String) null);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getIdpMetadata()
     */
    @Override
    public String getIdpMetadata() {
        return unexpectedCall((String) null);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getIdpMetadataProvider()
     */
    @Override
    public AcsDOMMetadataProvider getIdpMetadataProvider() {
        return unexpectedCall((AcsDOMMetadataProvider) null);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getLoginPageURL()
     */
    @Override
    public String getLoginPageURL() {
        return unexpectedCall((String) null);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getErrorPageURL()
     */
    @Override
    public String getErrorPageURL() {
        return unexpectedCall((String) null);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getTokenReplayTimeout()
     */
    @Override
    public long getTokenReplayTimeout() {
        return unexpectedCall(30 * 60 * 1000);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getSessionNotOnOrAfter()
     */
    @Override
    public long getSessionNotOnOrAfter() {
        return unexpectedCall(2 * 60 * 60 * 1000); // 2 hours
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isIncludeTokenInSubject()
     */
    @Override
    public boolean isIncludeTokenInSubject() {
        return true;
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getSpCookieName(com.ibm.wsspi.kernel.service.location.WsLocationAdmin)
     */
    @Override
    public String getSpCookieName(WsLocationAdmin locationAdmin) {
        return unexpectedCall((String) null);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isPkixTrustEngineEnabled()
     */
    @Override
    public boolean isPkixTrustEngineEnabled() {
        return true;
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getAuthnRequestTime()
     */
    @Override
    public long getAuthnRequestTime() {
        return unexpectedCall(600000);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isHttpsRequired()
     */
    @Override
    public boolean isHttpsRequired() {
        return unexpectedCall(false); // Not needed?
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isAllowCustomCacheKey()
     */
    @Override
    public boolean isAllowCustomCacheKey() {
        return false;
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getSpHostAndPort()
     */
    @Override
    public String getSpHostAndPort() {
        return unexpectedCall((String) null);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#createSession()
     */
    @Override
    public boolean createSession() {
        return unexpectedCall(true);
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#setConfigAdmin(org.osgi.service.cm.ConfigurationAdmin)
     */
    @Override
    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
        if (props != null) {
            if (tc.isDebugEnabled()) {
                String authFilterRef = (String) props.get(CFG_KEY_AUTH_FILTER_REF);
                Tr.debug(tc, providerId + "> saml AuthenticationFilter Ref:" + authFilterRef + " id:" + getAuthFilterId(authFilterRef));
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isReAuthnOnAssertionExpire()
     */
    @Override
    public boolean isReAuthnOnAssertionExpire() {
        return unexpectedCall(false);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getReAuthnCushion()
     */
    @Override
    public long getReAuthnCushion() {
        return unexpectedCall(0l);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getTargetPageUrl()
     */
    @Override
    public String getTargetPageUrl() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isIncludeX509InSPMetadata()
     */
    @Override
    public boolean isIncludeX509InSPMetadata() {
        return includeX509InSPMetadata;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getUseRelayStateForTarget()
     */
    @Override
    public boolean getUseRelayStateForTarget() {
        return unexpectedCall(true);
    }

    @Override
    public String getPostLogoutRedirectUrl() {
        return null;
    }

    @Override
    public boolean isServletRequestLogoutPerformsSamlLogout() {
        return servletRequestLogoutPerformsSamlLogout;
    }

    @Override
    public boolean isDisableInitialRequestCookie() {
        return unexpectedCall(false);
    }

}
