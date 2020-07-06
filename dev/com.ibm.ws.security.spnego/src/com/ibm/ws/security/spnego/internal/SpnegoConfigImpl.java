/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.internal;

import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ietf.jgss.GSSCredential;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.kerberos.auth.KerberosService;
import com.ibm.ws.security.spnego.ErrorPageConfig;
import com.ibm.ws.security.spnego.SpnegoConfig;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 * Represents security configurable options for SPNEGO web.
 */
public class SpnegoConfigImpl implements SpnegoConfig {

    private static final TraceComponent tc = Tr.register(SpnegoConfigImpl.class);

    public static final String KEY_ID = "id";
    public static final String KEY_AUTH_FILTER_REF = "authFilterRef";

    public static final String KEY_HOST_NAME = "hostName";
    public static final String KEY_ALLOW_LOCAL_HOST = "allowLocalHost";
    public static final String KEY_CANONICAL_HOST_NAME = "canonicalHostName";
    public static final String KEY_KRB5_CONFIG = "krb5Config";
    public static final String KEY_KRB5_KEYTAB = "krb5Keytab";
    public static final String KEY_KERBEROR_REALM_NAME = "kerberosRealmName";
    public static final String KEY_SERVICE_PRINCIPAL_NAMES = "servicePrincipalNames";

    public static final String KEY_SKIP_FOR_UNPROTECTED_URI = "skipForUnprotectedURI";
    public static final String KEY_DISABLE_FAIL_OVER_TO_APP_AUTH_TYPE = "disableFailOverToAppAuthType";

    public static final String KEY_INVOKE_AFTER_SSO = "invokeAfterSSO";

    public static final String KEY_SPNEGO_NOT_SUPPORTED_ERROR_PAGE_URL = "spnegoNotSupportedErrorPageURL";
    public static final String KEY_NTLM_TOKEN_RECEIVED_ERROR_PAGE_URL = "ntlmTokenReceivedErrorPageURL";

    public static final String KEY_TRIM_KERBEROS_REALM_NAME_FROM_PRINCIPAL = "trimKerberosRealmNameFromPrincipal";

    public static final String KEY_INCLUDE_CLIENT_GSS_CREDENTIAL_IN_SUBJECT = "includeClientGSSCredentialInSubject";

    public static final String KEY_INCLUDE_CUSTOM_CACHE_KEY_IN_SUBJECT = "includeCustomCacheKeyInSubject";

    public static final String KEY_DISABLE_LTPA_COOKIE = "disableLtpaCookie";

    public static final String LOCAL_HOST = "localhost";
    public static final String HTTP_LOCAL_HOST = "HTTP/localhost";
    public static final String[] localhost = { LOCAL_HOST };

    static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    private WsLocationAdmin locationAdmin = null;
    private final KerberosService kerbSvc;
    private String id;
    private String authFilterRef;
    private boolean allowLocalHost;
    private String hostName;
    private boolean canonicalHostName;
    private String krb5Config;
    private String krb5Keytab;
    private String kerberosRealmName;
    private List<String> servicePrincipalNames;
    private boolean skipForUnprotectedURI;
    private boolean disableFailOverToAppAuthType;
    private boolean invokeAfterSSO;
    private String spnegoNotSupportedErrorPageURL;
    private String ntlmTokenReceivedErrorPageURL;
    private boolean includeCustomCacheKeyInSubject;

    private boolean trimKerberosRealmNameFromPrincipal;
    private boolean includeClientGSSCredentialInSubject;
    private ErrorPageConfig errorPageConfig = null;
    private final SpnGssCredential spnGssCredential = new SpnGssCredential();
    private Krb5DefaultFile krb5DefaultFile = null;
    private boolean disableLtpaCookie;

    public SpnegoConfigImpl(WsLocationAdmin locationAdmin,
                            KerberosService kerberosService,
                            Map<String, Object> props) {
        this.locationAdmin = locationAdmin;
        this.kerbSvc = kerberosService;
        krb5DefaultFile = new Krb5DefaultFile(locationAdmin);
        processConfig(props);
        initSpnGssCrendential();
    }

    protected boolean initSpnGssCrendential() {
        boolean result = true;
        errorPageConfig = new ErrorPageConfig(spnegoNotSupportedErrorPageURL, ntlmTokenReceivedErrorPageURL);

        if (krb5Keytab == null || krb5Keytab.length() == 0) {
            result = false;
        } else {
            spnGssCredential.init(servicePrincipalNames, this);
            if (spnGssCredential.isEmpty()) {
                result = false;
            }
        }

        return result;
    }

    /**
     * @param props
     */
    protected void processConfig(Map<String, Object> props) {
        if (props == null || props.isEmpty())
            return;
        id = (String) props.get(KEY_ID);
        authFilterRef = (String) props.get(KEY_AUTH_FILTER_REF);
        hostName = (String) props.get(KEY_HOST_NAME);
        allowLocalHost = (Boolean) props.get(KEY_ALLOW_LOCAL_HOST);
        canonicalHostName = (Boolean) props.get(KEY_CANONICAL_HOST_NAME);
        krb5Config = processKrb5Config(props);
        krb5Keytab = processKrb5Keytab(props);
        String spns = (String) props.get(KEY_SERVICE_PRINCIPAL_NAMES);
        skipForUnprotectedURI = (Boolean) props.get(KEY_SKIP_FOR_UNPROTECTED_URI);
        disableFailOverToAppAuthType = (Boolean) props.get(KEY_DISABLE_FAIL_OVER_TO_APP_AUTH_TYPE);
        invokeAfterSSO = (Boolean) props.get(KEY_INVOKE_AFTER_SSO);
        spnegoNotSupportedErrorPageURL = (String) props.get(KEY_SPNEGO_NOT_SUPPORTED_ERROR_PAGE_URL);
        ntlmTokenReceivedErrorPageURL = (String) props.get(KEY_NTLM_TOKEN_RECEIVED_ERROR_PAGE_URL);
        trimKerberosRealmNameFromPrincipal = (Boolean) props.get(KEY_TRIM_KERBEROS_REALM_NAME_FROM_PRINCIPAL);
        includeClientGSSCredentialInSubject = (Boolean) props.get(KEY_INCLUDE_CLIENT_GSS_CREDENTIAL_IN_SUBJECT);
        servicePrincipalNames = resolveServicePrincipalNames((String) props.get(KEY_SERVICE_PRINCIPAL_NAMES));
        includeCustomCacheKeyInSubject = (Boolean) props.get(KEY_INCLUDE_CUSTOM_CACHE_KEY_IN_SUBJECT);
        disableLtpaCookie = (Boolean) props.get(KEY_DISABLE_LTPA_COOKIE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "id: " + id);
            Tr.debug(tc, "authFilterRef: " + authFilterRef);
            Tr.debug(tc, "hostName: " + hostName);
            Tr.debug(tc, "allowLocalHost: " + allowLocalHost);
            Tr.debug(tc, "canonicalHostName: " + canonicalHostName);
            Tr.debug(tc, "krb5Config: " + krb5Config);
            Tr.debug(tc, "krb5Keytab: " + krb5Keytab);
            Tr.debug(tc, "kerberosRealmName: " + kerberosRealmName);
            Tr.debug(tc, "spns: " + spns);
            Tr.debug(tc, "skipForUnprotectedURI: " + skipForUnprotectedURI);
            Tr.debug(tc, "disableFailOverToAppAuthType: " + disableFailOverToAppAuthType);
            Tr.debug(tc, "invokeAfterSSO: " + invokeAfterSSO);
            Tr.debug(tc, "spnegoNotSupportedErrorPageURL: " + spnegoNotSupportedErrorPageURL);
            Tr.debug(tc, "ntlmTokenReceivedErrorPageURL: " + ntlmTokenReceivedErrorPageURL);
            Tr.debug(tc, "trimKerberosRealmNameFromPrincipal: " + trimKerberosRealmNameFromPrincipal);
            Tr.debug(tc, "includeClientGSSCredentialInSubject: " + includeClientGSSCredentialInSubject);
            Tr.debug(tc, "includeCustomCacheKeyInSubject: " + includeCustomCacheKeyInSubject);
            Tr.debug(tc, "disableLtpaCookie: " + disableLtpaCookie);
        }
    }

    /**
     * @param props
     */
    protected String processKrb5Keytab(Map<String, Object> props) {
        String keytab = (String) props.get(KEY_KRB5_KEYTAB);
        Path kerbKeytab = kerbSvc.getKeytab(); // from the <kerberos> element

        if (keytab == null && kerbKeytab != null) {
            keytab = kerbKeytab.toAbsolutePath().toString();
        }

        if (keytab != null) {
            WsResource kt = locationAdmin.resolveResource(keytab);
            if (kt == null || !kt.exists()) {
                Tr.error(tc, "SPNEGO_KRB5_KEYTAB_FILE_NOT_FOUND", keytab);
                return null;
            } else {
                return keytab;
            }
        } else {
            return krb5DefaultFile.getDefaultKrb5KeytabFile();
        }
    }

    /**
     * @param props
     */
    protected String processKrb5Config(Map<String, Object> props) {
        String krbCf = (String) props.get(KEY_KRB5_CONFIG);
        Path kerbConfigFile = kerbSvc.getConfigFile(); // from the <kerberos> element

        if (kerbConfigFile != null) {
            if (krbCf == null) {
                krbCf = kerbConfigFile.toAbsolutePath().toString();
            } else if (!kerbConfigFile.toAbsolutePath().toString().equals(krbCf)) {
                // Error: Conflicting values specified on <spnego> and <kerberos> element
                Tr.error(tc, "SPNEGO_CONFLICTING_SETTINGS_CWWKS4323E", "configFile", "<kerberos>", KEY_KRB5_CONFIG, "<spnego>");
                return null;
            } else {
                // both values are set but are equal, tolerate it
            }
        }

        if (krbCf != null) {
            WsResource kcf = locationAdmin.resolveResource(krbCf);
            if (kcf == null || !kcf.exists()) {
                Tr.error(tc, "SPNEGO_KRB5_CONFIG_FILE_NOT_FOUND", krbCf);
                return null;
            } else {
                return krbCf;
            }
        } else {
            return krb5DefaultFile.getDefaultKrb5ConfigFile();
        }
    }

    /**
     * @return the id
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the canonicalHostName
     */
    @Override
    public boolean getAllowLocalHost() {
        return allowLocalHost;
    }

    /**
     * @return the canonicalHostName
     */
    @Override
    public boolean isCanonicalHostName() {
        return canonicalHostName;
    }

    /**
     * @return the krb5Config
     */
    @Override
    public String getKrb5Config() {
        return krb5Config;
    }

    /**
     * @return the krb5Config
     */
    @Override
    public String getKrb5Keytab() {
        return krb5Keytab;
    }

    /**
     * @return the skipForUnprotectedURI
     */
    @Override
    public boolean getSkipForUnprotectedURI() {
        return skipForUnprotectedURI;
    }

    /**
     * @return the doNotfailOverToAppAuthType
     */
    @Override
    public boolean getDisableFailOverToAppAuthType() {
        return disableFailOverToAppAuthType;
    }

    /**
     * @return the invokeAfterSSO
     */
    @Override
    public boolean isInvokeAfterSSO() {
        return invokeAfterSSO;
    }

    /**
     * @return the spnegoNotSupportedErrorPageURL
     */
    @Override
    public String getSpnegoNotSupportedErrorPageURL() {
        return spnegoNotSupportedErrorPageURL;
    }

    /**
     * @return the ntlmTokenReceivedErrorPageURL
     */
    @Override
    public String getNtlmTokenReceivedErrorPageURL() {
        return ntlmTokenReceivedErrorPageURL;
    }

    /**
     * @return the trimKerberosRealmNameFromPrincipal
     */
    @Override
    public boolean isTrimKerberosRealmNameFromPrincipal() {
        return trimKerberosRealmNameFromPrincipal;
    }

    @Override
    public boolean isIncludeClientGSSCredentialInSubject() {
        return includeClientGSSCredentialInSubject;
    }

    @Override
    public boolean isIncludeCustomCacheKeyInSubject() {
        return includeCustomCacheKeyInSubject;
    }

    @Override
    public ErrorPageConfig getErrorPageConfig() {
        return errorPageConfig;
    }

    @Override
    public GSSCredential getSpnGSSCredential(String hostName) {
        return spnGssCredential.getSpnGSSCredential(hostName);
    }

    @Override
    public boolean isSpnGssCredentialEmpty() {
        return spnGssCredential.isEmpty();
    }

    /**
     * @param servicePrincipalNames
     */
    public List<String> resolveServicePrincipalNames(String servicePrincipalNames) {

        List<String> spns = new ArrayList<String>();
        if ((servicePrincipalNames == null || servicePrincipalNames.length() == 0)) {
            if (allowLocalHost) {
                spns.add(HTTP_LOCAL_HOST);
            }
            String lh = getHostName();
            if (lh != null) {
                spns.add("HTTP/" + lh);
            }
            Tr.info(tc, "SPNEGO_DEFAULT_SPNS", spns.toString());
        } else {
            String[] valuesArray = servicePrincipalNames.split(",");
            for (int i = 0; i < valuesArray.length; i++) {
                String spn = valuesArray[i].trim();

                if (!spn.startsWith("HTTP/")) {
                    spn = "HTTP/" + spn;
                }
                spns.add(spn);
            }
        }
        return spns;
    }

    @Override
    public String getHostName() {
        String host = null;
        try {
            host = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    if (canonicalHostName) {
                        return java.net.InetAddress.getLocalHost().getCanonicalHostName();
                    } else {
                        return java.net.InetAddress.getLocalHost().getHostName();
                    }
                }
            });
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Un-expected exception: ", e);
            }
        }

        return host;
    }

    @Override
    public boolean isDisableLtpaCookie() {
        return disableLtpaCookie;
    }
}
