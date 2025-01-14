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
package com.ibm.ws.security.saml.sso20.internal;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.security.authentication.filter.AuthenticationFilter;
import com.ibm.ws.security.common.config.CommonConfigUtils;
import com.ibm.ws.security.filemonitor.FileBasedActionable;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.utils.FileInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.MsgCtxUtil;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;
import com.ibm.ws.security.saml.sso20.metadata.AcsDOMMetadataProvider;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class SsoConfigImpl extends PkixTrustEngineConfig implements SsoConfig, FileBasedActionable {
    public static final TraceComponent tc = Tr.register(SsoConfigImpl.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
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
    static final String KEY_disableInitialRequestCookie = "disableInitialRequestCookie";
    static final String KEY_disableLtpaCookie = "disableLtpaCookie";
    static final String KEY_spCookieName = "spCookieName";
    static final String KEY_realmName = "realmName";
    static final String KEY_headerName = "headerName";
    static final String KEY_audiences = "audiences";
    static final String KEY_spHostAndPort = "spHostAndPort";
    static final String KEY_targetPageUrl = "targetPageUrl";
    static final String KEY_reAuthnOnAssertionExpire = "reAuthnOnAssertionExpire";
    static final String KEY_reAuthnCushion = "reAuthnCushion";
    // static final String KEY_trustedIssuers = "trustedIssuers"; // handled in pkixTrustEngineConfig
    static final String KEY_servletRequestLogoutPerformsSamlLogout = "spLogout";

    static final String KEY_enabled = "enabled";

    static final String KEY_httpsRequired = "httpsRequired";
    static final String KEY_allowCustomCacheKey = "allowCustomCacheKey";
    static final String KEY_createSession = "createSession";
    static final String KEY_useRelayStateForTarget = "useRelayStateForTarget";
    public static final String KEY_postLogoutRedirectUrl = "postLogoutRedirectUrl";

    static final String[] notInUseAttributes = new String[] { KEY_headerName, KEY_audiences };

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

    boolean mapUserIdentifierToUserRegistry = false;
    boolean setLtpaCookie = false;
    boolean wantAssertionsSigned = true;
    String realmName = null;

    String headerName = null;
    ArrayList<String> headerNames = null;
    String[] audiences = new String[] { "ANY" };
    long authnRequestTimeMilliSeconds = 600000; // 10 minutes

    boolean authnRequestsSigned = true;
    boolean includeX509InSPMetadata = true;
    boolean forceAuthn = false;
    boolean isPassive = false;
    Boolean allowCreate = null;
    String[] authnContextClassRefs = null;
    String authnContextComparisonType = null;
    String nameIDFormat = null;
    String idpMetadata = null;
    AcsDOMMetadataProvider idpMetadataProvider = null;
    String loginPageURL = null;
    String errorPageURL = null;
    long tokenReplayTimeout = 30 * 60 * 1000;
    long sessionNotOnOrAfter = 2 * 60 * 60 * 1000; // 2 hours
    boolean allowCustomCacheKey = true; // default is allowCustomCacheKey

    Constants.MapToUserRegistry mapToUserRegistry = Constants.MapToUserRegistry.No;
    boolean disableInitialRequestCookie = false;
    boolean disableLtpaCookie = true;
    String spCookieName = null;
    String spHostAndPort = null;
    String targetPageUrl = null;
    boolean bIdpMetadataProviderHandled = false;
    boolean createSession = true;
    boolean reAuthnOnAssertionExpire = false;
    long reAuthnCushion = 0l;
    private String bundleLocation;
    boolean useRelayStateForTarget = true;
    String postLogoutRedirectUrl = null;
    private boolean servletRequestLogoutPerformsSamlLogout = false;

    static HashMap<String, String> nameIDFormatMap = new HashMap<String, String>();
    static {
        nameIDFormatMap.put(Constants.NAME_ID_SHORT_UNSPECIFIED, Constants.NAME_ID_FORMAT_UNSPECIFIED);
        nameIDFormatMap.put(Constants.NAME_ID_SHORT_EMAIL, Constants.NAME_ID_FORMAT_EMAIL);
        nameIDFormatMap.put(Constants.NAME_ID_SHORT_X509_SUBJECT, Constants.NAME_ID_FORMAT_X509_SUBJECT);
        nameIDFormatMap.put(Constants.NAME_ID_SHORT_WIN_DOMAIN_QUALIFIED, Constants.NAME_ID_FORMAT_WIN_DOMAIN_QUALIFIED);
        nameIDFormatMap.put(Constants.NAME_ID_SHORT_KERBEROS, Constants.NAME_ID_FORMAT_KERBEROS);
        nameIDFormatMap.put(Constants.NAME_ID_SHORT_ENTITY, Constants.NAME_ID_FORMAT_ENTITY);
        nameIDFormatMap.put(Constants.NAME_ID_SHORT_PERSISTENT, Constants.NAME_ID_FORMAT_PERSISTENT);
        nameIDFormatMap.put(Constants.NAME_ID_SHORT_TRANSIENT, Constants.NAME_ID_FORMAT_TRANSIENT);
        nameIDFormatMap.put(Constants.NAME_ID_SHORT_ENCRYPTED, Constants.NAME_ID_FORMAT_ENCRYPTED);
    }

    CommonConfigUtils configUtils = new CommonConfigUtils();

    public SsoConfigImpl() {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml20.SamlConfig#getVersion()
     */
    @Override
    public Constants.SamlSsoVersion getSamlVersion() {
        return Constants.SamlSsoVersion.SAMLSSO20;
    }

    public SsoConfigImpl(ComponentContext cc,
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
     * @throws SamlException
     *
     */
    private void processProps(Map<String, Object> props) throws SamlException {
        // warn users on the ignored attributes
        Tr.warning(tc, "SAML_CONFIG_IGNORE_ATTRIBUTES", new Object[] { "false", ignoreAttributes, providerId });

        clockSkewMilliSeconds = (Long) props.get(KEY_clockSkew); // milliseconds
        authnRequestTimeMilliSeconds = (Long) props.get(KEY_authnRequestTime); // milliseconds
        httpsRequired = (Boolean) props.get(KEY_httpsRequired);
        allowCustomCacheKey = (Boolean) props.get(KEY_allowCustomCacheKey);
        wantAssertionsSigned = (Boolean) props.get(KEY_wantAssertionsSigned);
        signatureMethodAlgorithm = trim((String) props.get(KEY_signatureMethodAlgorithm));
        if (CryptoUtils.isAlgorithmInsecure(signatureMethodAlgorithm)) {
            CryptoUtils.logInsecureAlgorithm(KEY_signatureMethodAlgorithm, signatureMethodAlgorithm);
        }
        authnRequestsSigned = (Boolean) props.get(KEY_authnRequestsSigned);
        includeX509InSPMetadata = (Boolean) props.get(KEY_includeX509InSPMetadata);
        forceAuthn = (Boolean) props.get(KEY_forceAuthn);
        isPassive = (Boolean) props.get(KEY_isPassive);
        allowCreate = (Boolean) props.get(KEY_allowCreate);
        authnContextClassRefs = trim((String[]) props.get(KEY_authnContextClassRef));
        authnContextComparisonType = trim((String) props.get(KEY_authnContextComparisonType));
        nameIDFormat = processNameIDFormat(props, (String) props.get(KEY_nameIDFormat));
        idpMetadata = trim((String) props.get(KEY_idpMetadata));
        idpMetadataProvider = null; // ensure: whenever changed, idpMetadaProvider is renewed.
        keyStoreRef = trim((String) props.get(KEY_keyStoreRef));
        keyAlias = trim((String) props.get(KEY_keyAlias));
        keyPassword = getPassword((SerializableProtectedString) props.get(KEY_keyPassword));
        loginPageURL = trim((String) props.get(KEY_loginPageURL));
        errorPageURL = trim((String) props.get(KEY_errorPageURL));
        tokenReplayTimeout = (Long) props.get(KEY_tokenReplayTimeout);
        sessionNotOnOrAfter = (Long) props.get(KEY_sessionNotOnOrAfter);
        userIdentifier = trim((String) props.get(KEY_userIdentifier));
        groupIdentifier = trim((String) props.get(KEY_groupIdentifier));
        userUniqueIdentifier = trim((String) props.get(KEY_userUniqueIdentifier));
        if (userUniqueIdentifier == null || userUniqueIdentifier.isEmpty()) {
            userUniqueIdentifier = userIdentifier;
        }
        realmIdentifier = trim((String) props.get(KEY_realmIdentifier));
        includeTokenInSubject = (Boolean) props.get(KEY_includeTokenInSubject);
        mapToUserRegistry = Constants.MapToUserRegistry.valueOf((String) props.get(KEY_mapToUserRegistry));
        disableInitialRequestCookie = (Boolean) props.get(KEY_disableInitialRequestCookie);
        disableLtpaCookie = (Boolean) props.get(KEY_disableLtpaCookie);
        spCookieName = trim((String) props.get(KEY_spCookieName));
        realmName = trim((String) props.get(KEY_realmName));
        spHostAndPort = trim((String) props.get(KEY_spHostAndPort));
        targetPageUrl = trim((String) props.get(KEY_targetPageUrl));
        enabled = (Boolean) props.get(KEY_enabled);
        if (props.get(KEY_createSession) != null) {
            createSession = (Boolean) props.get(KEY_createSession);
        }
        reAuthnOnAssertionExpire = (Boolean) props.get(KEY_reAuthnOnAssertionExpire);
        reAuthnCushion = (Long) props.get(KEY_reAuthnCushion);
        useRelayStateForTarget = (Boolean) props.get(KEY_useRelayStateForTarget);
        postLogoutRedirectUrl = configUtils.getConfigAttribute(props, KEY_postLogoutRedirectUrl);
        servletRequestLogoutPerformsSamlLogout = (Boolean) props.get(KEY_servletRequestLogoutPerformsSamlLogout);

        // Handle the tc debug
        processPkixTrustEngine(props);
    }

    /**
     * @param props
     * @throws SamlException
     */
    private void processPkixTrustEngine(Map<String, Object> props) throws SamlException {
        try {
            super.processPkixTrustEngine(props, configAdmin, bundleLocation);
        } catch (Exception e) {
            throw new SamlException(e); // let SamlException handle the unexpected Exception
        }
    }

    /**
     * @param props2
     * @param object
     * @return
     */
    String processNameIDFormat(Map<String, Object> props, String shortNameIDFormat) {
        String result = null;
        if (shortNameIDFormat == null || shortNameIDFormat.isEmpty()) { // no NameID Ref
            return Constants.NAME_ID_FORMAT_EMAIL; // return EMAIL by default
        }
        if (shortNameIDFormat.equals("customize")) {
            result = (String) props.get(KEY_customizeNameIDFormat);
        } else {
            result = nameIDFormatMap.get(shortNameIDFormat);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, providerId + "> NameIDFormat:" + result + " id:" + shortNameIDFormat);
        }
        return result;
    }

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

    @Override
    public String getProviderId() {
        return providerId;
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

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SamlConfig#getClockSkew()
     */
    @Override
    public long getClockSkew() {
        return clockSkewMilliSeconds;
    }

    @Override
    public boolean isWantAssertionsSigned() {
        return wantAssertionsSigned;
    }

    @Override
    public boolean isAuthnRequestsSigned() {
        return authnRequestsSigned;
    }

    @Override
    public boolean isForceAuthn() {
        return forceAuthn;
    }

    @Override
    public boolean isPassive() {
        return isPassive;
    }

    @Override
    public String[] getAuthnContextClassRef() {
        return authnContextClassRefs == null ? null : authnContextClassRefs.clone();
    }

    @Override
    public String getNameIDFormat() {
        return nameIDFormat;
    }

    @Override
    public String getIdpMetadata() {
        return idpMetadata;
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

    @Override
    public String getLoginPageURL() {
        return loginPageURL;
    }

    @Override
    public String getErrorPageURL() {
        return errorPageURL;
    }

    @Override
    public long getTokenReplayTimeout() {
        return tokenReplayTimeout;
    }

    @Override
    public long getSessionNotOnOrAfter() {
        return sessionNotOnOrAfter;
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
    public boolean isIncludeTokenInSubject() {
        return includeTokenInSubject;
    }

    @Override
    public Constants.MapToUserRegistry getMapToUserRegistry() {
        return mapToUserRegistry;
    }

    @Override
    public String toString() {
        String result = "notInitialized yet";
        if (bInit) {
            result = "\nproviderId:" + providerId
                     + "\nwantAssertionsSigned:" + wantAssertionsSigned
                     + "\nsignatureMethodAlgorithm:" + signatureMethodAlgorithm
                     + "\nauthnRequestsSigned:" + authnRequestsSigned
                     + "\nforceAuthn:" + forceAuthn
                     + "\ncreateSession:" + createSession
                     + "\nisPassive:" + isPassive
                     + "\nallowCreate:" + allowCreate
                     // + "\nauthnContextClassRef:" + authnContextClassRefs
                     + "\nauthnContextComparisonType:" + authnContextComparisonType
                     + "\nnameIDFormat:" + nameIDFormat
                     + "\nidpMetadata:" + idpMetadata
                     + "\nkeyStoreRef:" + keyStoreRef
                     + "\nkeyAlias:" + keyAlias
                     + "\nkeyPassword:" + (keyPassword == null ? "null" : "*****")
                     + "\nloginPageURL:" + loginPageURL
                     + "\nerrorPageURL:" + errorPageURL
                     + "\ntokenReplayTimeout:" + tokenReplayTimeout
                     + "\nuserIdentifier:" + userIdentifier
                     + "\ngroupIdentifier:" + groupIdentifier
                     + "\nuserUniqueIdentifier:" + userUniqueIdentifier
                     + "\nrealmIdentifier:" + realmIdentifier
                     + "\nincludeTokenInSubject:" + includeTokenInSubject
                     + "\nmapToUserRegistry:" + mapToUserRegistry
                     + "\ndisableInitialRequestCookie:" + disableInitialRequestCookie
                     + "\ndisableLtpaCookie:" + disableLtpaCookie
                     + "\nspCookieName:" + spCookieName
                     + "\nrealmName:" + realmName
                     + "\nspHostAndPort:" + spHostAndPort
                     + "\ntargetPageUrl:" + targetPageUrl
                     + "\nuseRelayStateForTarget:" + useRelayStateForTarget
                     + "\ntrustedIssuers:" + (trustedIssuers == null ? "null" : trustedIssuers.length)
                     + "\nenabled:" + enabled
                     + "\nincludeX509InSPMetadata:" + includeX509InSPMetadata
                     + (!isPkixTrustEngineEnabled ? ";" : "\npkixTrustEngine enabled"
                                                          + "\nx509 cert list:" + pkixX509List.toString()
                                                          + "\ncrl list:" + pkixCrlList.toString())
                     + "\npostLogoutRedirectUrl:" + postLogoutRedirectUrl
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
        } else if ("SHA1".equalsIgnoreCase(signatureMethodAlgorithm)) {
            return SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;
        }
        return SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getIdpMetadataProvider()
     */
    @Override
    public AcsDOMMetadataProvider getIdpMetadataProvider() {
        try {
            if (!bIdpMetadataProviderHandled) {
                idpMetadataProvider = MsgCtxUtil.parseIdpMetadataProvider(this);
                bIdpMetadataProviderHandled = true;
            }
        } catch (SamlException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Can not parse MetadataFile:" + idpMetadata);
            }
            // Do not handle the error for now.
            // Let the one who is getting the AcsDOMMetadataProvider decide what to do
        }
        return idpMetadataProvider;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.filemonitor.FileBasedActionable#performFileBasedAction()
     */
    @Override
    public void performFileBasedAction(Collection<File> files) {
        idpMetadataProvider = null;
        bIdpMetadataProviderHandled = false;
        Tr.info(tc, "SAML20_IDP_METADATA_FILE_CHANGED", new Object[] { idpMetadata, providerId });
        // CWWKS5023I: The Service Provider [{0}] had detected the IdP Metadata file [{1}] is modified.
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.filemonitor.FileBasedActionable#getBundleContext()
     */
    @Override
    public BundleContext getBundleContext() {
        if (cc != null) {
            return cc.getBundleContext();
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getAllowCreate()
     */
    @Override
    public Boolean getAllowCreate() {
        return allowCreate;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getAuthnContextComparisonType()
     */
    @Override
    public String getAuthnContextComparisonType() {
        return authnContextComparisonType;
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
     * @see com.ibm.ws.security.saml.SsoConfig#getSpCookieName(com.ibm.wsspi.kernel.service.location.WsLocationAdmin)
     */
    @Override
    public String getSpCookieName(WsLocationAdmin locationAdmin) {
        if (spCookieName == null || spCookieName.isEmpty()) {
            String cookieLongName = "";
            if (locationAdmin != null) {
                String usrLocation = locationAdmin.resolveString(Constants.WLP_USER_DIR).replace('\\', '/');
                String slash = usrLocation.endsWith("/") ? "" : "/";
                cookieLongName = FileInfo.getHostName() + "_" + usrLocation + slash + "servers/" + locationAdmin.getServerName() + "/" + providerId;
            } else {
                Tr.error(tc, "OSGI_SERVICE_ERROR", "WsLocationAdmin");
                cookieLongName = providerId;
            }
            spCookieName = Constants.COOKIE_NAME_SP_PREFIX + SamlUtil.hash(cookieLongName);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cookieHashName: " + spCookieName + " cookieLongName: " + cookieLongName);
            }
        }
        return spCookieName;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getX509CertificateList()
     */
    @Override
    public List<String> getPkixX509CertificateList() {
        return pkixX509List;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getCRLList()
     */
    @Override
    public List<String> getPkixCrlList() {
        return pkixCrlList;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getTrustAnchor()
     */
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

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isTrustEngineEnabled()
     */
    @Override
    public boolean isPkixTrustEngineEnabled() {
        return isPkixTrustEngineEnabled;
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
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getAuthnRequestTime()
     */
    @Override
    public long getAuthnRequestTime() {
        return authnRequestTimeMilliSeconds;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getTrustedIssuers()
     */
    @Override
    public String[] getPkixTrustedIssuers() {
        return trustedIssuers == null ? null : trustedIssuers.clone();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
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

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getSpHostAndPort()
     */
    @Override
    public String getSpHostAndPort() {
        return spHostAndPort;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isHttpsRequired()
     */
    @Override
    public boolean isHttpsRequired() {
        return httpsRequired;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isShareLtpaCookie()
     */
    @Override
    public boolean isAllowCustomCacheKey() {
        // Make sure once disableLtpaToken then
        // the allowCustomCacheKey has to be true
        // Other wise, the SP_Cookie won't work. (See Authenticator)
        if (disableLtpaCookie)
            return true;
        else
            return allowCustomCacheKey;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#createSessiont()
     */
    @Override
    public boolean createSession() {
        return createSession;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getHeaderName()
     */
    @Override
    public String getHeaderName() {
        // default HeaderName
        return unexpectedCall("Saml,saml,SAML");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getHeaderNames()
     */
    @Override
    public ArrayList<String> getHeaderNames() {
        ArrayList<String> defaultHeaderNames = new ArrayList<String>();
        defaultHeaderNames.add("Saml");
        defaultHeaderNames.add("saml");
        defaultHeaderNames.add("SAML");
        return unexpectedCall(defaultHeaderNames);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getAudiences()
     */
    @Override
    public String[] getAudiences() {
        // Default ANY
        return unexpectedCall(new String[] { "ANY" });
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isReAuthnOnAssertionExpire()
     */
    @Override
    public boolean isReAuthnOnAssertionExpire() {
        return reAuthnOnAssertionExpire;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getReAuthnCushion()
     */
    @Override
    public long getReAuthnCushion() {
        return reAuthnCushion;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SsoConfig#getTargetPageUrl()
     */
    @Override
    public String getTargetPageUrl() {
        return targetPageUrl;
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
        return useRelayStateForTarget;
    }

    @Override
    public String getPostLogoutRedirectUrl() {
        return postLogoutRedirectUrl;
    }

    /*
     * (non-Javadoc)
     *
     *
     * @see com.ibm.ws.security.saml.SsoConfig#isServletRequestLogoutPerformsSamlLogout()
     */
    @Override
    public boolean isServletRequestLogoutPerformsSamlLogout() {
        return servletRequestLogoutPerformsSamlLogout;
    }

    @Override
    public boolean isDisableInitialRequestCookie() {
        return this.disableInitialRequestCookie;
    }

    @Override
    public void performFileBasedAction(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        // TODO Auto-generated method stub

    }

}
