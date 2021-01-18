/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.ibm.websphere.simplicity.log.Log;

public class TestSettings {

    static private final Class<?> thisClass = TestSettings.class;

    // OP
    protected String flowType = null;
    protected String firstClientUrl = null;
    protected String firstClientUrlSSL = null;
    protected String clientRedirect = null;
    protected String authorizeEndpt = null;
    protected String tokenEndpt = null;
    protected String introspectionEndpt = null;
    protected String revocationEndpt = null;
    protected String discoveryEndpt = null;
    protected String coverageMapEndpt = null;
    protected String registryEndpt = null;
    protected String userinfoEndpt = null;
    protected String jwkEndpt = null;
    protected String appPasswordEndpt = null;
    protected String appTokenEndpt = null;
    protected String endSession = null;
    protected String postLogoutRedirect = null;
    protected String protectedResource = null;
    protected String rsProtectedResource = null;
    protected String refreshTokUrl = null;
    protected String jwtTokUrl = null;
    protected String issuer = null;
    protected Boolean jti = false;
    protected String resourceIds = null;
    protected String autoauthz = null;
    protected String clientName = null;
    protected String clientID = null;
    protected String clientSecret = null;
    protected String scope = null;
    protected String state = null;
    protected String adminUser = null;
    protected String adminPswd = null;
    protected String responseType = null;
    protected String nonce = null;
    protected String realm = null;
    protected String loginPrompt = null;
    protected String groupIds = null;
    protected String accessTimeout = null; // take the default
    protected String signatureAlg = null; // take the default
    protected String where = null;
    protected String headerName = null;
    protected String rsTokenType = null;
    protected String rsTokenKey = null;
    protected String rsCertType = null;
    protected Boolean validateJWTTimeStamps = true;
    protected String codeVerifier = null;
    protected String codeChallenge = null;
    protected String codeChallengeMethod = null;

    protected String ConfigTAIProvider = null;
    protected String ConfigNoFilter = null;
    protected String ConfigDerby = null;
    protected String ConfigSample = null;
    protected String ConfigMediator = null;
    protected String ConfigTAI = null;
    protected String ConfigPublic = null;

    protected String responseMode = "response_mode"; // default value is
                                                     // response_mode
    protected String prompt; // default value is "consent" for oidc
                             // and "none" for oauth

    protected StoreType storeType = StoreType.LOCAL;

    protected boolean isHash = false;

    protected String componentID = null;

    protected String httpString = null;

    protected Integer httpPort = null;

    protected boolean allowPrint = true;

    public enum StoreType {
        DATABASE, LOCAL, CUSTOM, CUSTOMBELL
    }

    // RP
    protected String testURL = null;
    protected String provider = null;
    protected String providerType = null;
    protected String userParm = null;
    protected String passParm = null;
    protected String userName = null;
    protected String userPassword = null;
    protected String fullUserName = null;
    protected String clientIdentity = null;
    protected String loginTitle = null;
    protected String loginButton = null;
    protected String confirmButton = null;
    protected String confirmButtonValue = null;
    protected Long confirmSleep = null;
    protected String testURL2 = null;
    protected Map<String, String> requestParms = null;
    protected Map<String, String> requestFileParms = null;
    protected String jwtId = null;
    protected boolean useJwtConsumer = false;
    protected String jwtConsumerUrl = null;
    protected List<String> requiredJwtKeys = null;

    protected String inboundProp = null;

    public TestSettings() {
    }

    public TestSettings(TestSettings settings) {
        setAllValues(settings);
    }

    public void setAllValues(TestSettings settings) {

        // OP
        flowType = settings.flowType;
        firstClientUrl = settings.firstClientUrl;
        firstClientUrlSSL = settings.firstClientUrlSSL;
        clientRedirect = settings.clientRedirect;
        authorizeEndpt = settings.authorizeEndpt;
        tokenEndpt = settings.tokenEndpt;
        introspectionEndpt = settings.introspectionEndpt;
        revocationEndpt = settings.revocationEndpt;
        discoveryEndpt = settings.discoveryEndpt;
        registryEndpt = settings.registryEndpt;
        coverageMapEndpt = settings.coverageMapEndpt;
        userinfoEndpt = settings.userinfoEndpt;
        jwkEndpt = settings.jwkEndpt;
        appPasswordEndpt = settings.appPasswordEndpt;
        appTokenEndpt = settings.appTokenEndpt;
        endSession = settings.endSession;
        postLogoutRedirect = settings.postLogoutRedirect;
        endSession = settings.endSession;
        postLogoutRedirect = settings.postLogoutRedirect;
        protectedResource = settings.protectedResource;
        rsProtectedResource = settings.rsProtectedResource;
        refreshTokUrl = settings.refreshTokUrl;
        jwtTokUrl = settings.jwtTokUrl;
        issuer = settings.issuer;
        jti = settings.jti;
        resourceIds = settings.resourceIds;
        autoauthz = settings.autoauthz;
        clientName = settings.clientName;
        clientID = settings.clientID;
        clientSecret = settings.clientSecret;
        adminUser = settings.adminUser;
        adminPswd = settings.adminPswd;
        state = settings.state;
        scope = settings.scope;
        responseType = settings.responseType;
        nonce = settings.nonce;
        accessTimeout = settings.accessTimeout;
        signatureAlg = settings.signatureAlg;
        where = settings.where;
        headerName = settings.headerName;
        rsTokenType = settings.rsTokenType;
        rsTokenKey = settings.rsTokenKey;
        rsCertType = settings.rsCertType;
        validateJWTTimeStamps = settings.validateJWTTimeStamps;
        realm = settings.realm;
        loginPrompt = settings.loginPrompt;
        groupIds = settings.groupIds;
        allowPrint = settings.allowPrint;
        codeVerifier = settings.codeVerifier;
        codeChallenge = settings.codeChallenge;
        codeChallengeMethod = settings.codeChallengeMethod;

        // RP
        testURL = settings.testURL;
        provider = settings.provider;
        providerType = settings.providerType;
        userParm = settings.userParm;
        passParm = settings.passParm;
        userName = settings.userName;
        userPassword = settings.userPassword;
        fullUserName = settings.fullUserName;
        clientIdentity = settings.clientIdentity;
        loginTitle = settings.loginTitle;
        loginButton = settings.loginButton;
        confirmButton = settings.confirmButton;
        confirmButtonValue = settings.confirmButtonValue;
        confirmSleep = settings.confirmSleep;
        testURL2 = settings.testURL2;
        inboundProp = settings.inboundProp;
        if (settings.requestParms != null) {
            requestParms = new HashMap<String, String>(settings.requestParms);
        }
        if (settings.requestFileParms != null) {
            requestFileParms = new HashMap<String, String>(settings.requestFileParms);
        }
        jwtId = settings.jwtId;
        useJwtConsumer = settings.useJwtConsumer;
        jwtConsumerUrl = settings.jwtConsumerUrl;
        if (settings.requiredJwtKeys != null) {
            requiredJwtKeys = new ArrayList<String>();
            requiredJwtKeys.addAll(settings.requiredJwtKeys);
        } else {
            requiredJwtKeys = null;
        }
        storeType = settings.storeType;
        isHash = settings.isHash;
        httpPort = settings.httpPort;
        httpString = settings.httpString;
        componentID = settings.componentID;
    }

    public TestSettings copyTestSettings() {
        return new TestSettings(this);
    }

    public TestSettings copyFromTestSettings(TestSettings current) {
        Log.info(thisClass, "copyFromTestSettings", "copying TestSettings");
        setAllValues(current);
        return this;
    }

    public void printTestSettings() {
        String thisMethod = "printTestSettings";

        if (!allowPrint) {
            return;
        }

        Log.info(thisClass, thisMethod, "Test Settings: ");
        Log.info(thisClass, thisMethod, "flowType: " + flowType);
        Log.info(thisClass, thisMethod, "firstClientUrl: " + firstClientUrl);
        Log.info(thisClass, thisMethod, "firstClientUrlSSL: " + firstClientUrlSSL);
        Log.info(thisClass, thisMethod, "clientRedirect: " + clientRedirect);
        Log.info(thisClass, thisMethod, "authorizeEndpt: " + authorizeEndpt);
        Log.info(thisClass, thisMethod, "tokenEndpt: " + tokenEndpt);
        Log.info(thisClass, thisMethod, "introspectionEndpt: " + introspectionEndpt);
        Log.info(thisClass, thisMethod, "revocationEndpt: " + revocationEndpt);
        Log.info(thisClass, thisMethod, "discoveryEndpt: " + discoveryEndpt);
        Log.info(thisClass, thisMethod, "coverageMapEndpt: " + coverageMapEndpt);
        Log.info(thisClass, thisMethod, "registryEndpt: " + registryEndpt);
        Log.info(thisClass, thisMethod, "userinfoEndpt: " + userinfoEndpt);
        Log.info(thisClass, thisMethod, "jwkEndpt" + jwkEndpt);
        Log.info(thisClass, thisMethod, "appPasswordEndpt" + appPasswordEndpt);
        Log.info(thisClass, thisMethod, "appTokenEndpt" + appTokenEndpt);
        Log.info(thisClass, thisMethod, "endSession: " + endSession);
        Log.info(thisClass, thisMethod, "postLogoutRedirect: " + postLogoutRedirect);
        Log.info(thisClass, thisMethod, "protectedResource: " + protectedResource);
        Log.info(thisClass, thisMethod, "rsProtectedResource: " + rsProtectedResource);
        Log.info(thisClass, thisMethod, "refreshTokUrl: " + refreshTokUrl);
        Log.info(thisClass, thisMethod, "jwtTokUrl: " + jwtTokUrl);
        Log.info(thisClass, thisMethod, "issuer: " + issuer);
        Log.info(thisClass, thisMethod, "jti: " + jti);
        Log.info(thisClass, thisMethod, "resourceIds: " + resourceIds);
        Log.info(thisClass, thisMethod, "autoauthz: " + autoauthz);
        Log.info(thisClass, thisMethod, "clientName: " + clientName);
        Log.info(thisClass, thisMethod, "clientID: " + clientID);
        Log.info(thisClass, thisMethod, "clientSecret: " + clientSecret);
        Log.info(thisClass, thisMethod, "adminUser: " + adminUser);
        Log.info(thisClass, thisMethod, "adminPswd: " + adminPswd);
        Log.info(thisClass, thisMethod, "state: " + state);
        Log.info(thisClass, thisMethod, "scope: " + scope);
        Log.info(thisClass, thisMethod, "responseType: " + responseType);
        Log.info(thisClass, thisMethod, "nonce: " + nonce);
        Log.info(thisClass, thisMethod, "accessTimeout: " + accessTimeout);
        Log.info(thisClass, thisMethod, "signatureAlg: " + signatureAlg);
        Log.info(thisClass, thisMethod, "where: " + where);
        Log.info(thisClass, thisMethod, "headerName: " + headerName);
        Log.info(thisClass, thisMethod, "rsTokenType: " + rsTokenType);
        Log.info(thisClass, thisMethod, "rsTokenKey: " + rsTokenKey);
        Log.info(thisClass, thisMethod, "rsCertType: " + rsCertType);
        Log.info(thisClass, thisMethod, "validateJWTTimeStamps: " + validateJWTTimeStamps);
        Log.info(thisClass, thisMethod, "realm: " + realm);
        Log.info(thisClass, thisMethod, "loginPrompt: " + loginPrompt);
        Log.info(thisClass, thisMethod, "groupIds: " + groupIds);
        Log.info(thisClass, thisMethod, "codeVerifier: " + codeVerifier);
        Log.info(thisClass, thisMethod, "codeChallenge: " + codeChallenge);
        Log.info(thisClass, thisMethod, "codeChallengeMethod: " + codeChallengeMethod);

        Log.info(thisClass, thisMethod, "testURL: " + testURL);
        Log.info(thisClass, thisMethod, "provider: " + provider);
        Log.info(thisClass, thisMethod, "providerType: " + providerType);
        Log.info(thisClass, thisMethod, "userParm: " + userParm);
        Log.info(thisClass, thisMethod, "passParm: " + passParm);
        Log.info(thisClass, thisMethod, "userName: " + userName);
        Log.info(thisClass, thisMethod, "userPassword: " + userPassword);
        Log.info(thisClass, thisMethod, "fullUserName: " + fullUserName);
        Log.info(thisClass, thisMethod, "clientIdentity: " + clientIdentity);
        Log.info(thisClass, thisMethod, "loginButton: " + loginButton);
        Log.info(thisClass, thisMethod, "confirmButton: " + confirmButton);
        Log.info(thisClass, thisMethod, "confirmButtonValue: " + confirmButtonValue);
        Log.info(thisClass, thisMethod, "loginTitle: " + loginTitle);
        Log.info(thisClass, thisMethod, "inBoundProp: " + inboundProp);
        Log.info(thisClass, thisMethod, "requestParms: " + requestParms);
        Log.info(thisClass, thisMethod, "requestFileParms: " + requestFileParms);
        Log.info(thisClass, thisMethod, "jwtId: " + jwtId);
        Log.info(thisClass, thisMethod, "useJwtConsumer: " + useJwtConsumer);
        Log.info(thisClass, thisMethod, "jwtConsumerUrl: " + jwtConsumerUrl);
        Log.info(thisClass, thisMethod, "requiredJwtKeys: " + requiredJwtKeys);
        Log.info(thisClass, thisMethod, "allowPrint: " + allowPrint);
        Log.info(thisClass, thisMethod, "storeType: " + storeType.toString());

    }

    public String getDetailOAuthOPFirstClientUrl() {
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.CLIENT_JSP;
    }

    public String getDetailOAuthOPFirstClientUrlSSL() {
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.CLIENT_JSP;
    }

    public String getDetailOAuthOPClientRedirect() {
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.REDIRECT_JSP;
    }

    public String getDetailOAuthOPAuthorizeEndpt() {
        return "/" + Constants.OAUTH_ROOT + "/" + getRandomType() + "/" + Constants.OAUTHCONFIGSAMPLE_APP + "/" + Constants.AUTHORIZE_ENDPOINT;
    }

    public String getDetailOAuthOPTokenEndpt() {
        return "/" + Constants.OAUTH_ROOT + "/" + getRandomType() + "/" + Constants.OAUTHCONFIGSAMPLE_APP + "/" + Constants.TOKEN_ENDPOINT;
    }

    public String getDetailOAuthOPIntrospectionEndPt() {
        return "/" + Constants.OAUTH_ROOT + "/" + getRandomType() + "/" + Constants.OAUTHCONFIGSAMPLE_APP + "/" + Constants.INTROSPECTION_ENDPOINT;
    }

    public String getDetailOAuthOPRevocationEndpt() {
        return "/" + Constants.OAUTH_ROOT + "/" + getRandomType() + "/" + Constants.OAUTHCONFIGSAMPLE_APP + "/" + Constants.REVOCATION_ENDPOINT;
    }

    public String getDetailOAuthOPProviderPT() {
        return "/" + Constants.OAUTH_ROOT + "/" + Constants.PROVIDERS_TYPE + "/" + Constants.OAUTHCONFIGSAMPLE_APP;
    }

    public String getDetailOAuthOPProviderET() {
        return "/" + Constants.OAUTH_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OAUTHCONFIGSAMPLE_APP;
    }

    public String getDetailOAuthOPProtectedResource() {
        return "/" + Constants.OAUTH_TAI_ROOT + "/" + Constants.SNOOP;
    }

    public String getDetailOAuthRSProtectedResource() {
        return "/" + Constants.OAUTH_TAI_ROOT + "/" + Constants.SNOOP;
    }

    public String getDetailOAuthOPRefreshTokUrl() {
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.REFRESH_JSP;
    }

    public String getDetailOAuthOPJwtTokUrl() {
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.JWT_JSP;
    }

    public String getDefaultJwtConsumerClientUrl() {
        return "/" + Constants.JWT_CONSUMER_ENDPOINT;
    }

    public String getDetailOAuthOPAppPasswordEndpt() {
        return "/" + Constants.OAUTH_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OAUTHCONFIGSAMPLE_APP + "/" + Constants.APP_PASSWORD_ENDPOINT;
    }

    public String getDetailOAuthOPAppTokenEndpt() {
        return "/" + Constants.OAUTH_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OAUTHCONFIGSAMPLE_APP + "/" + Constants.APP_TOKEN_ENDPOINT;
    }

    public String getDetailOAuthOPRegistrationEndpt() {
        return "/" + Constants.OAUTH_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OAUTHCONFIGSAMPLE_APP + "/" + Constants.REGISTRATION_ENDPOINT;
    }

    public String getDetailOAuthOPCoverageMapEndpt() {
        return "/" + Constants.OAUTH_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OAUTHCONFIGSAMPLE_APP + "/" + Constants.COVERAGE_MAP_ENDPOINT;
    }

    /**
     *
     * @param httpStart
     *            prefix Url string (ie: http://localhost:DefaultPort)
     * @param httpsStart
     *            prefix Url string (ie: https://localhost:DefaultSSLPort)
     * @return returns a TestSettings object with the default values set
     */
    public void setDefaultOAuthOPTestSettings(String httpStart, String httpsStart) {

        // flow type shouldn't matter - only important for some implicit tests,
        // let those set as needed
        flowType = null;
        firstClientUrl = httpStart + getDetailOAuthOPFirstClientUrl();
        firstClientUrlSSL = httpsStart + getDetailOAuthOPFirstClientUrlSSL();
        clientRedirect = httpStart + getDetailOAuthOPClientRedirect();
        authorizeEndpt = httpsStart + getDetailOAuthOPAuthorizeEndpt();
        tokenEndpt = httpsStart + getDetailOAuthOPTokenEndpt();
        introspectionEndpt = httpsStart + getDetailOAuthOPIntrospectionEndPt();
        revocationEndpt = httpsStart + getDetailOAuthOPRevocationEndpt();
        protectedResource = httpsStart + getDetailOAuthOPProtectedResource();
        rsProtectedResource = httpsStart + getDetailOAuthRSProtectedResource();
        refreshTokUrl = httpStart + getDetailOAuthOPRefreshTokUrl();
        jwtTokUrl = httpsStart + getDetailOAuthOPJwtTokUrl();
        issuer = httpsStart + getDetailOAuthOPProviderET();
        jti = false;
        resourceIds = null;
        autoauthz = "true";
        clientName = "client01";
        clientID = "client01";
        clientSecret = "secret";
        adminUser = "testuser";
        adminPswd = "testuserpwd";
        state = "Lvj9Z2l8jMSMrtWG1F3Z"; // thie will need to be updated when
                                        // state is generated by the RP
        scope = "scope1 scope2";
        nonce = null;
        realm = Constants.BASIC_REALM;
        loginPrompt = null;
        groupIds = null;
        prompt = "none";
        where = Constants.PARM;
        headerName = Constants.AUTHORIZATION;
        rsTokenType = Constants.ACCESS_TOKEN_KEY;
        rsTokenKey = Constants.ACCESS_TOKEN_KEY;
        rsCertType = Constants.X509_CERT;
        validateJWTTimeStamps = true;
        codeVerifier = null;
        codeChallenge = null;
        codeChallengeMethod = null;

        ConfigTAIProvider = Constants.OAUTHCONFIGTAI_APP;
        ConfigNoFilter = Constants.OAUTHCONFIGNOFILTER_APP;
        ConfigDerby = Constants.OAUTHCONFIGDERBY_APP;
        ConfigSample = Constants.OAUTHCONFIGSAMPLE_APP;
        ConfigMediator = Constants.OAUTHCONFIGMEDIATOR_APP;
        ConfigTAI = Constants.OAUTH_TAI_ROOT;
        ConfigPublic = Constants.OAUTHCONFIGPUBLIC_APP;

        appPasswordEndpt = httpsStart + getDetailOAuthOPAppPasswordEndpt();
        appTokenEndpt = httpsStart + getDetailOAuthOPAppTokenEndpt();
        registryEndpt = httpsStart + getDetailOAuthOPRegistrationEndpt();
        coverageMapEndpt = httpsStart + getDetailOAuthOPCoverageMapEndpt();

    }

    public String getDetailOIDCOPFirstClientUrl() {
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.CLIENT_JSP;
    }

    public String getDetailOIDCOPFirstClientUrlSSL() {
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.CLIENT_JSP;
    }

    public String getDetailOIDCOPClientRedirect() {
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.REDIRECT_JSP;
    }

    public String getDetailOIDCOPAuthorizeEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + getRandomType() + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.AUTHORIZE_ENDPOINT;
    }

    public String getDetailOIDCOPTokenEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + getRandomType() + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.TOKEN_ENDPOINT;
    }

    public String getDetailOIDCOPProviderPT() {
        return "/" + Constants.OIDC_ROOT + "/" + Constants.PROVIDERS_TYPE + "/" + Constants.OIDCCONFIGSAMPLE_APP;
    }

    public String getDetailOIDCOPProviderET() {
        return "/" + Constants.OIDC_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OIDCCONFIGSAMPLE_APP;
    }

    public String getDetailOIDCOPIntrospectionEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + getRandomType() + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.INTROSPECTION_ENDPOINT;
    }

    public String getDetailOIDCOPRevocationEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + getRandomType() + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.REVOCATION_ENDPOINT;
    }

    public String getDetailOIDCOPDiscoveryEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.DISCOVERY_ENDPOINT;
    }

    public String getDetailOIDCOPCoverageMapEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.COVERAGE_MAP_ENDPOINT;
    }

    public String getDetailOIDCOPRegistrationEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.REGISTRATION_ENDPOINT;
    }

    public String getDetailOIDCOPUserinfoEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.USERINFO_ENDPOINT;
    }

    public String getDetailOIDCOPJwkEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.JWK_ENDPOINT;
    }

    public String getDetailOIDCOPAppPasswordEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.APP_PASSWORD_ENDPOINT;
    }

    public String getDetailOIDCOPAppTokenEndpt() {
        return "/" + Constants.OIDC_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.APP_TOKEN_ENDPOINT;
    }

    public String getDetailOIDCOPEndSession() {
        return "/" + Constants.OIDC_ROOT + "/" + Constants.ENDPOINT_TYPE + "/" + Constants.OIDCCONFIGSAMPLE_APP + "/" + Constants.END_SESSION;
    }

    public String getDetailOIDCOPPostLogoutRedirect() {
        // return "/" + Constants.OIDC_TAI_ROOT + "/" +
        // Constants.POST_LOGIN_REDIRECT;
        // return "/" + Constants.OIDC_TAI_ROOT + "/" + Constants.SNOOP;
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.CLIENT_JSP;
    }

    public String getDetailOIDCOPProtectedResource() {
        return "/" + Constants.OIDC_TAI_ROOT + "/" + Constants.SNOOP;
    }

    public String GetDetailOIDCRSProtectedResource() {
        return "/" + Constants.OIDC_TAI_ROOT + "/" + Constants.SNOOP;
    }

    public String getDetailOIDCOPRefreshTokUrl() {
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.REFRESH_JSP;
    }

    public String getDetailOIDCOPJwtTokUrl() {
        return "/" + Constants.OAUTHCLIENT_APP + "/" + Constants.JWT_JSP;
    }

    /**
     *
     * @param httpStart
     *            prefix Url string (ie: http://localhost:DefaultPort)
     * @param httpsStart
     *            prefix Url string (ie: https://localhost:DefaultSSLPort)
     * @return returns a TestSettings object with the default values set
     */
    public void setDefaultOIDCOPTestSettings(String httpStart, String httpsStart) {

        // flow type shouldn't matter - only important for some implicit tests,
        // let those set as needed
        flowType = null;
        firstClientUrl = httpStart + getDetailOIDCOPFirstClientUrl();
        firstClientUrlSSL = httpsStart + getDetailOIDCOPFirstClientUrlSSL();
        clientRedirect = httpStart + getDetailOIDCOPClientRedirect();
        authorizeEndpt = httpsStart + getDetailOIDCOPAuthorizeEndpt();
        tokenEndpt = httpsStart + getDetailOIDCOPTokenEndpt();
        introspectionEndpt = httpsStart + getDetailOIDCOPIntrospectionEndpt();
        revocationEndpt = httpsStart + getDetailOIDCOPRevocationEndpt();
        discoveryEndpt = httpsStart + getDetailOIDCOPDiscoveryEndpt();
        coverageMapEndpt = httpsStart + getDetailOIDCOPCoverageMapEndpt();
        registryEndpt = httpsStart + getDetailOIDCOPRegistrationEndpt();
        userinfoEndpt = httpsStart + getDetailOIDCOPUserinfoEndpt();
        jwkEndpt = httpsStart + getDetailOIDCOPJwkEndpt();
        appPasswordEndpt = httpsStart + getDetailOIDCOPAppPasswordEndpt();
        appTokenEndpt = httpsStart + getDetailOIDCOPAppTokenEndpt();
        endSession = httpsStart + getDetailOIDCOPEndSession();
        postLogoutRedirect = httpsStart + getDetailOIDCOPPostLogoutRedirect();
        protectedResource = httpsStart + getDetailOIDCOPProtectedResource();
        rsProtectedResource = httpsStart + GetDetailOIDCRSProtectedResource();
        refreshTokUrl = httpStart + getDetailOIDCOPRefreshTokUrl();
        jwtTokUrl = httpsStart + getDetailOIDCOPJwtTokUrl();
        issuer = httpsStart + getDetailOIDCOPProviderET();
        jti = false;
        resourceIds = null;
        autoauthz = "true";
        clientName = "client01";
        clientID = "client01";
        clientSecret = "secret";
        adminUser = "testuser";
        adminPswd = "testuserpwd";
        state = "Lvj9Z2l8jMSMrtWG1F3Z"; // This may need to be updated when the
                                        // state is generated by the RP
        scope = "openid scope1 scope2";
        // nonce = Constants.DEFAULT_NONCE ;
        nonce = null;
        realm = Constants.BASIC_REALM;
        loginPrompt = null;
        groupIds = null;
        prompt = "consent";
        where = Constants.PARM;
        headerName = Constants.AUTHORIZATION;
        rsTokenType = Constants.ACCESS_TOKEN_KEY;
        rsTokenKey = Constants.ACCESS_TOKEN_KEY;
        rsCertType = Constants.X509_CERT;
        validateJWTTimeStamps = true;
        codeVerifier = null;
        codeChallenge = null;
        codeChallengeMethod = null;

        ConfigTAIProvider = Constants.OIDCCONFIGTAI_APP;
        ConfigNoFilter = Constants.OIDCCONFIGNOFILTER_APP;
        ConfigDerby = Constants.OIDCCONFIGDERBY_APP;
        ConfigSample = Constants.OIDCCONFIGSAMPLE_APP;
        ConfigMediator = Constants.OIDCCONFIGMEDIATOR_APP;
        ConfigTAI = Constants.OIDC_TAI_ROOT;
        ConfigPublic = Constants.OIDCCONFIGPUBLIC_APP;

    }

    public void setFlowType(String inFlowType) {
        flowType = inFlowType;
    }

    public String getFlowType() {
        Log.info(thisClass, "getFlowType", flowType);
        return flowType;
    }

    public void setFirstClientURL(String inFirstclientUrl) {
        firstClientUrl = inFirstclientUrl;
    }

    public String getFirstClientURL() {
        return firstClientUrl;
    }

    public void setFirstClientUrlSSL(String inFirstClientUrlSSL) {
        firstClientUrlSSL = inFirstClientUrlSSL;
    }

    public String getFirstClientUrlSSL() {
        return firstClientUrlSSL;
    }

    public void setClientRedirect(String inClientRedirect) {
        clientRedirect = inClientRedirect;
    }

    public String getClientRedirect() {
        return clientRedirect;
    }

    public void setAuthorizeEndpt(String inAuthorizeEndpt) {
        authorizeEndpt = inAuthorizeEndpt;
    }

    public String getAuthorizeEndpt() {
        return authorizeEndpt;
    }

    public void setTokenEndpt(String inTokenEndpt) {
        tokenEndpt = inTokenEndpt;
    }

    public String getTokenEndpt() {
        return tokenEndpt;
    }

    public void setIntrospectionEndpt(String inIntrospectionEndpt) {
        introspectionEndpt = inIntrospectionEndpt;
    }

    public String getIntrospectionEndpt() {
        return introspectionEndpt;
    }

    public void setRevocationEndpt(String inRevocationEndpt) {
        revocationEndpt = inRevocationEndpt;
    }

    public String getRevocationEndpt() {
        return revocationEndpt;
    }

    public void setDiscoveryEndpt(String inDiscoveryEndpt) {
        discoveryEndpt = inDiscoveryEndpt;
    }

    public String getDiscoveryEndpt() {
        return discoveryEndpt;
    }

    public void setCoverageMapEndpt(String inCoverageMapEndpt) {
        coverageMapEndpt = inCoverageMapEndpt;
    }

    public String getCoverageMapEndpt() {
        return coverageMapEndpt;
    }

    public void setRegistrationEndpt(String inRegistryEndpt) {
        registryEndpt = inRegistryEndpt;
    }

    public String getRegistrationEndpt() {
        return registryEndpt;
    }

    public void setUserinfoEndpt(String inUserinfoEndpt) {
        userinfoEndpt = inUserinfoEndpt;
    }

    public String getUserinfoEndpt() {
        return userinfoEndpt;
    }

    public void setJwkEndpt(String inJwkEndpt) {
        jwkEndpt = inJwkEndpt;
    }

    public String getJwkEndpt() {
        return jwkEndpt;
    }

    public void setAppPasswordEndpt(String inAppPasswordEndpt) {
        appPasswordEndpt = inAppPasswordEndpt;
    }

    public String getAppPasswordsEndpt() {
        return appPasswordEndpt;
    }

    public void setAppTokenEndpt(String inAppTokenEndpt) {
        appTokenEndpt = inAppTokenEndpt;
    }

    public String getAppTokensEndpt() {
        return appTokenEndpt;
    }

    public void setEndSession(String inEndSession) {
        endSession = inEndSession;
    }

    public String getEndSession() {
        return endSession;
    }

    public void setPostLogoutRedirect(String inPostLogoutRedirect) {
        postLogoutRedirect = inPostLogoutRedirect;
    }

    public String getPostLogoutRedirect() {
        return postLogoutRedirect;
    }

    public void setProtectedResource(String inProtectedResource) {
        protectedResource = inProtectedResource;
    }

    public String getProtectedResource() {
        return protectedResource;
    }

    public void setRSProtectedResource(String inRSProtectedResource) {
        rsProtectedResource = inRSProtectedResource;
    }

    public String getRSProtectedResource() {
        return rsProtectedResource;
    }

    public void setRefreshTokUrl(String inRefreshTokUrl) {
        refreshTokUrl = inRefreshTokUrl;
    }

    public String getRefreshTokUrl() {
        return refreshTokUrl;
    }

    public void setJwtTokUrl(String inJwtTokUrl) {
        jwtTokUrl = inJwtTokUrl;
    }

    public String getJwtTokUrl() {
        return jwtTokUrl;
    }

    public void setIssuer(String inIssuer) {
        issuer = inIssuer;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setJti(Boolean inJti) {
        jti = inJti;
    }

    public Boolean getJti() {
        return jti;
    }

    public void setResourceIds(String inResourceIds) {
        resourceIds = inResourceIds;
    }

    public String getResourceIds() {
        return resourceIds;
    }

    public void setAutoAuthz(String inAutoauthz) {
        autoauthz = inAutoauthz;
    }

    public String getAutoAuthz() {
        return autoauthz;
    }

    public void setClientName(String inClientName) {
        clientName = inClientName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientID(String inClientID) {
        clientID = inClientID;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientSecret(String inClientSecret) {
        clientSecret = inClientSecret;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setAdminUser(String inAdminUser) {
        adminUser = inAdminUser;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminPswd(String inAdminPswd) {
        adminPswd = inAdminPswd;
    }

    public String getAdminPswd() {
        return adminPswd;
    }

    public void setState(String inState) {
        state = inState;
    }

    public String getState() {
        return state;
    }

    public void setScope(String inScope) {
        scope = inScope;
    }

    public String getScope() {
        return scope;
    }

    public void setResponseType(String inResponseType) {
        responseType = inResponseType;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setNonce(String inNonce) {
        nonce = inNonce;
    }

    public String getNonce() {
        return nonce;
    }

    public void setAccessTimeout(String inAccessTimeout) {
        accessTimeout = inAccessTimeout;
    }

    public String getAccessTimeout() {
        return accessTimeout;
    }

    public void setSignatureAlg(String inSignatureAlg) {
        signatureAlg = inSignatureAlg;
    }

    public String getSignatureAlg() {
        return signatureAlg;
    }

    public void setWhere(String inWhere) {
        where = inWhere;
    }

    public String getWhere() {
        return where;
    }

    public void setHeaderName(String inHeaderName) {
        headerName = inHeaderName;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setRsTokenType(String inRsTokenType) {
        rsTokenType = inRsTokenType;
    }

    public String getRsTokenType() {
        return rsTokenType;
    }

    public void setRsTokenKey(String inRsTokenKey) {
        rsTokenKey = inRsTokenKey;
    }

    public String getRsTokenKey() {
        return rsTokenKey;
    }

    public void setRsCertType(String inRsCertType) {
        rsCertType = inRsCertType;
    }

    public String getRsCertType() {
        return rsCertType;
    }

    public void setValidateJWTTimeStamps(Boolean inValidateJWTTimeStamps) {
        validateJWTTimeStamps = inValidateJWTTimeStamps;
    }

    public Boolean getValidateJWTTimeStamps() {
        return validateJWTTimeStamps;
    }

    public void setRealm(String inRealm) {
        realm = inRealm;
    }

    public String getRealm() {
        return realm;
    }

    public void setLoginPrompt(String inLoginPrompt) {
        loginPrompt = inLoginPrompt;
    }

    public String getLoginPrompt() {
        return loginPrompt;
    }

    public void setGroupIds(String inGroupIds) {
        groupIds = inGroupIds;
    }

    public String getGroupIds() {
        return groupIds;
    }

    public void setTestURL(String inURL) {
        testURL = inURL;
    }

    public String getTestURL() {
        return testURL;
    }

    public void setProvider(String inProvider) {
        provider = inProvider;
    }

    public String getProvider() {
        return provider;
    }

    public void setProviderType(String inType) {
        providerType = inType;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setUserParm(String inParm) {
        userParm = inParm;
    }

    public String getUserParm() {
        return userParm;
    }

    public void setPassParm(String inParm) {
        passParm = inParm;
    }

    public String getPassParm() {
        return passParm;
    }

    public void setUserName(String inUserName) {
        userName = inUserName;
    }

    public String getUserName() {
        return userName;
    }

    public void setFullUserName(String inUserName) {
        fullUserName = inUserName;
    }

    public String getFullUserName() {
        return fullUserName;
    }

    public void setClientIdentity(String inClientIdentity) {
        clientIdentity = inClientIdentity;
    }

    public String getClientIdentity() {
        return clientIdentity;
    }

    public void setUserPassword(String inPassWord) {
        userPassword = inPassWord;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setLoginButton(String inButton) {
        loginButton = inButton;
    }

    public String getLoginButton() {
        return loginButton;
    }

    public void setConfirmButton(String inButton) {
        confirmButton = inButton;
    }

    public String getConfirmButton() {
        return confirmButton;
    }

    public void setConfirmButtonValue(String inValue) {
        confirmButtonValue = inValue;
    }

    public String getConfirmButtonValue() {
        return confirmButtonValue;
    }

    public void setConfirmSleep(Long inSleep) {
        confirmSleep = inSleep;
    }

    public Long getConfirmSleep() {
        return confirmSleep;
    }

    public void setLoginTitle(String inTitle) {
        loginTitle = inTitle;
    }

    public String getLoginTitle() {
        return loginTitle;
    }

    public void setTestURL2(String inURL) {
        testURL2 = inURL;
    }

    public String getTestURL2() {
        return testURL2;
    }

    public void setRequestParms(Map<String, String> inReqParms) {
        if (inReqParms != null) {
            requestParms = new HashMap<String, String>(inReqParms);
        } else {
            requestParms = null;
        }
    }

    public Map<String, String> getRequestParms() {
        if (requestParms != null) {
            Map<String, String> newReqParms = new HashMap<String, String>(requestParms);
            return newReqParms;
        } else {
            return null;
        }
    }

    public void setRequestFileParms(Map<String, String> inReqFileParms) {
        if (inReqFileParms != null) {
            requestFileParms = new HashMap<String, String>(inReqFileParms);
        } else {
            requestFileParms = null;
        }
    }

    public Map<String, String> getRequestFileParms() {
        if (requestFileParms != null) {
            Map<String, String> newReqFileParms = new HashMap<String, String>(requestFileParms);
            return newReqFileParms;
        } else {
            return null;
        }
    }

    public void setJwtId(String inJwtId) {
        jwtId = inJwtId;
    }

    public String getJwtId() {
        return jwtId;
    }

    public void setConfigTAIProvider(String inApp) {
        ConfigTAIProvider = inApp;
    }

    public String getConfigTAIProvider() {
        return ConfigTAIProvider;
    }

    public void setConfigNoFilter(String inApp) {
        ConfigNoFilter = inApp;
    }

    public String getConfigNoFilter() {
        return ConfigNoFilter;
    }

    public void setConfigDerby(String inApp) {
        ConfigDerby = inApp;
    }

    public String getConfigDerby() {
        return ConfigDerby;
    }

    public void setConfigSample(String inApp) {
        ConfigSample = inApp;
    }

    public String getConfigSample() {
        return ConfigSample;
    }

    public void setConfigMediator(String inApp) {
        ConfigMediator = inApp;
    }

    public String getConfigMediator() {
        return ConfigMediator;
    }

    public void setConfigTAI(String inApp) {
        ConfigTAI = inApp;
    }

    public String getConfigTAI() {
        return ConfigTAI;
    }

    public void setConfigPublic(String inApp) {
        ConfigPublic = inApp;
    }

    public String getConfigPublic() {
        return ConfigPublic;
    }

    public String getInboundProp() {
        return inboundProp;
    }

    public void setInboundProp(String inInboundProp) {
        inboundProp = inInboundProp;
    }

    public boolean getUseJwtConsumer() {
        return useJwtConsumer;
    }

    public void setUseJwtConsumer(boolean inUseJwtConsumer) {
        useJwtConsumer = inUseJwtConsumer;
    }

    public String getJwtConsumerUrl() {
        return jwtConsumerUrl;
    }

    public void setJwtConsumerUrl(String inJwtConsumerUrl) {
        jwtConsumerUrl = inJwtConsumerUrl;
    }

    public void setRequiredJwtKeys(List<String> inRequiredJwtKeys) {
        if (inRequiredJwtKeys != null) {
            requiredJwtKeys = new ArrayList<String>(inRequiredJwtKeys);
        } else {
            requiredJwtKeys = null;
        }

    }

    public List<String> getRequiredJwtKeys() {
        if (requiredJwtKeys != null) {
            List<String> newRequiredJwtKeys = new ArrayList<String>(requiredJwtKeys);
            return newRequiredJwtKeys;
        } else {
            return null;
        }
    }

    public void setAllowPrint(boolean inAllowPrint) {
        if (inAllowPrint) {
            Log.info(thisClass, "setAllowPrint", "Enable printing of settings and expectations");
        } else {
            Log.info(thisClass, "setAllowPrint", "Disable printing of settings and expectations");
        }
        allowPrint = inAllowPrint;
    }

    public boolean getAllowPrint() {
        return allowPrint;
    }

    public void setCodeVerifier(String inCodeVerifier) {
        codeVerifier = inCodeVerifier;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeChallenge(String inCodeChallenge) {
        codeChallenge = inCodeChallenge;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallengeMethod(String inCodeChallengeMethod) {
        codeChallengeMethod = inCodeChallengeMethod;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void addRequiredJwtKey(String key) {
        if (requiredJwtKeys == null) {
            requiredJwtKeys = new ArrayList<String>();
        }
        if (key != null) {
            requiredJwtKeys.add(key);
        }
    }

    public void removeRequiredJwtKey(String key) {
        if (requiredJwtKeys != null && key != null) {
            requiredJwtKeys.remove(key);
        }
    }

    public List<String> getDefaultRequiredJwtKeys() {
        ArrayList<String> requiredKeys = new ArrayList<String>();
        requiredKeys.add(Constants.PAYLOAD_ISSUER);
        requiredKeys.add(Constants.PAYLOAD_SUBJECT);
        requiredKeys.add(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        requiredKeys.add(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        requiredKeys.add(Constants.TOKEN_TYPE_KEY);
        requiredKeys.add(Constants.IDTOK_REALM_KEY);
        requiredKeys.add(Constants.JWT_SCOPE);
        return requiredKeys;
    }

    // public void setDefaultOIDCRPTestSettings(String httpStart, String
    // httpsStart, String targetUrl, String type) {
    // setDefaultOAuthRPTestSettings(httpStart, httpsStart, targetUrl, type) ;
    // }

    // may need different settings for openid vs openidconnect - keeping it
    // generic for now.
    // public void setDefaultOIDCRPTestSettings(String httpStart, String
    // httpsStart, String targetUrl, String type) {
    public void setDefaultRPTestSettings(String httpStart, String httpsStart, String targetUrl, String type) {

        testURL = httpsStart + "/" + targetUrl + "/" + Constants.DEFAULT_SERVLET;
        firstClientUrlSSL = httpsStart + "/" + Constants.OAUTHCLIENT_APP + "/client.jsp";

        testURL2 = testURL;

        if (type.equals(Constants.BLUEID_TYPE)) {
            Log.info(thisClass, "setDefaultRPTestSettings", "Setting default settings for BlueID");
            provider = Constants.BLUEID_PROVIDER;
            providerType = Constants.BLUEID_TYPE;
            userParm = Constants.BLUEID_USERPARM;
            passParm = Constants.BLUEID_PASSPARM;
            userName = Constants.BLUEID_USERNAME;
            userPassword = Constants.BLUEID_USERPASSWORD;
            fullUserName = Constants.BLUEID_FULLUSERNAME;
            clientIdentity = Constants.BLUEID_CLIENTIDENTITY;
            loginButton = Constants.BLUEID_LOGINBUTTON;
            confirmButton = Constants.BLUEID_CONFIRMBUTTON;
            confirmButtonValue = Constants.BLUEID_CONFIRMBUTTONVALUE;
            confirmSleep = Constants.BLUEID_CONFIRMSLEEP;
            loginTitle = Constants.BLUEID_LOGINTITLE;

        }

        if (type.equals(Constants.FREEXRI_TYPE)) {
            Log.info(thisClass, "setDefaultRPTestSettings", "Setting default settings for FreeXRI");

            provider = Constants.FREEXRI_PROVIDER;
            providerType = Constants.FREEXRI_TYPE;
            userParm = Constants.FREEXRI_USERPARM;
            passParm = Constants.FREEXRI_PASSPARM;
            userName = Constants.FREEXRI_USERNAME;
            userPassword = Constants.FREEXRI_USERPASSWORD;
            fullUserName = Constants.FREEXRI_FULLUSERNAME;
            clientIdentity = Constants.FREEXRI_CLIENTIDENTITY;
            loginButton = Constants.FREEXRI_LOGINBUTTON;
            confirmButton = Constants.FREEXRI_CONFIRMBUTTON;
            confirmButtonValue = Constants.FREEXRI_CONFIRMBUTTONVALUE;
            confirmSleep = Constants.FREEXRI_CONFIRMSLEEP;
            loginTitle = Constants.FREEXRI_LOGINTITLE;
        }
        if (type.equals(Constants.GOOGLE_TYPE)) {
            Log.info(thisClass, "setDefaultRPTestSettings", "Setting default settings for Google");
            provider = Constants.GOOGLE_PROVIDER;
            providerType = Constants.GOOGLE_TYPE;
            userParm = Constants.GOOGLE_USERPARM;
            passParm = Constants.GOOGLE_PASSPARM;
            userName = Constants.GOOGLE_USERNAME;
            userPassword = Constants.GOOGLE_USERPASSWORD;
            fullUserName = Constants.GOOGLE_FULLUSERNAME;
            clientIdentity = Constants.GOOGLE_CLIENTIDENTITY;
            loginButton = Constants.GOOGLE_LOGINBUTTON;
            confirmButton = Constants.GOOGLE_CONFIRMBUTTON;
            confirmButtonValue = Constants.GOOGLE_CONFIRMBUTTONVALUE;
            confirmSleep = Constants.GOOGLE_CONFIRMSLEEP;
            loginTitle = Constants.GOOGLE_LOGINTITLE;

        }

        if (type.equals(Constants.MYOPENID_TYPE)) {
            Log.info(thisClass, "setDefaultRPTestSettings", "Setting default settings for MyOpenID");
            provider = Constants.MYOPENID_PROVIDER;
            providerType = Constants.MYOPENID_TYPE;
            userParm = Constants.MYOPENID_USERPARM;
            passParm = Constants.MYOPENID_PASSPARM;
            userName = Constants.MYOPENID_USERNAME;
            userPassword = Constants.MYOPENID_USERPASSWORD;
            fullUserName = Constants.MYOPENID_FULLUSERNAME;
            clientIdentity = Constants.MYOPENID_CLIENTIDENTITY;
            loginButton = Constants.MYOPENID_LOGINBUTTON;
            confirmButton = Constants.MYOPENID_CONFIRMBUTTON;
            confirmButtonValue = Constants.MYOPENID_CONFIRMBUTTONVALUE;
            confirmSleep = Constants.MYOPENID_CONFIRMSLEEP;
            loginTitle = Constants.MYOPENID_LOGINTITLE;

        }

        if (type.equals(Constants.TFIM_TYPE)) {
            Log.info(thisClass, "setDefaultRPTestSettings", "Setting default settings for TFIM");
            provider = Constants.TFIM_PROVIDER;
            providerType = Constants.TFIM_TYPE;
            userParm = Constants.TFIM_USERPARM;
            passParm = Constants.TFIM_PASSPARM;
            userName = Constants.TFIM_USERNAME;
            userPassword = Constants.TFIM_USERPASSWORD;
            fullUserName = Constants.TFIM_FULLUSERNAME;
            clientIdentity = Constants.TFIM_CLIENTIDENTITY;
            loginButton = Constants.TFIM_LOGINBUTTON;
            confirmButton = Constants.TFIM_CONFIRMBUTTON;
            confirmButtonValue = Constants.TFIM_CONFIRMBUTTONVALUE;
            confirmSleep = Constants.TFIM_CONFIRMSLEEP;
            loginTitle = Constants.TFIM_LOGINTITLE;

        }

        if (type.equals(Constants.YAHOO_TYPE)) {
            Log.info(thisClass, "setDefaultRPTestSettings", "Setting default settings for Yahoo");
            provider = Constants.YAHOO_PROVIDER;
            providerType = Constants.YAHOO_TYPE;
            userParm = Constants.YAHOO_USERPARM;
            passParm = Constants.YAHOO_PASSPARM;
            userName = Constants.YAHOO_USERNAME;
            userPassword = Constants.YAHOO_USERPASSWORD;
            fullUserName = Constants.YAHOO_FULLUSERNAME;
            clientIdentity = Constants.YAHOO_CLIENTIDENTITY;
            loginButton = Constants.YAHOO_LOGINBUTTON;
            confirmButton = Constants.YAHOO_CONFIRMBUTTON;
            confirmButtonValue = Constants.YAHOO_CONFIRMBUTTONVALUE;
            confirmSleep = Constants.YAHOO_CONFIRMSLEEP;
            loginTitle = Constants.YAHOO_LOGINTITLE;

        }

        if (type.equals(Constants.IBMOIDC_TYPE)) {
            firstClientUrl = httpsStart + "/" + targetUrl + "/" + Constants.DEFAULT_SERVLET;
            Log.info(thisClass, "setDefaultRPTestSettings", "Setting default settings for OIDC OP");
            provider = Constants.OIDC_PROVIDER;
            providerType = Constants.IBMOIDC_TYPE;
            userParm = Constants.OIDC_USERPARM;
            passParm = Constants.OIDC_PASSPARM;
            userName = Constants.OIDC_USERNAME;
            userPassword = Constants.OIDC_USERPASSWORD;
            fullUserName = Constants.OIDC_FULLUSERNAME;
            clientIdentity = Constants.OIDC_CLIENTIDENTITY;
            loginButton = Constants.OIDC_LOGINBUTTON;
            confirmButton = Constants.OIDC_CONFIRMBUTTON;
            confirmButtonValue = Constants.OIDC_CONFIRMBUTTONVALUE;
            confirmSleep = Constants.OIDC_CONFIRMSLEEP;
            loginTitle = Constants.OIDC_LOGINTITLE;
            nonce = null;

        }

    }

    public void setDefaultJwtConsumerTestSettings(String httpStart, String httpsStart) {
        Log.info(thisClass, "setDefaultJwtConsumerTestSettings", "Setting default settings for JWT consumer");

        protectedResource = httpsStart + getDefaultJwtConsumerClientUrl();
        issuer = protectedResource;
        clientName = "client01";
        clientID = "client01";
        clientSecret = "secret";
        adminUser = "testuser";
        adminPswd = "testuserpwd";
        scope = "scope1 scope2";
        realm = Constants.BASIC_REALM;
        where = Constants.PARM;
        headerName = Constants.AUTHORIZATION;
        rsTokenType = Constants.ACCESS_TOKEN_KEY;
        rsTokenKey = Constants.ACCESS_TOKEN_KEY;
        rsCertType = Constants.X509_CERT;
        validateJWTTimeStamps = true;

    }

    /**
     *
     * @param httpStart
     *            prefix Url string (ie: http://localhost:DefaultPort)
     * @param httpsStart
     *            prefix Url string (ie: https://localhost:DefaultSSLPort)
     * @return returns a TestSettings object with the default values set
     */
    public void setDefaultGenericTestSettings(String httpStart, String httpsStart) {
        where = getRandomWhere();
    }

    public String getRandomType() {

        Random rand = new Random();
        Integer num = rand.nextInt(1000);
        // Log.info(thisClass, "getRandomType", "random number: " + num) ;
        if (num % 2 == 0) {
            return Constants.PROVIDERS_TYPE;
        } else {
            return Constants.ENDPOINT_TYPE;
        }

    }

    public String getRandomWhere() {

        String[] whereArray = new String[] { Constants.HEADER, Constants.PARM, Constants.HEADER, Constants.PARM };
        String thisMethod = "getRandomWhere";
        Log.info(thisClass, thisMethod, "Determining Where to put the access_token");
        Random rand = new Random();
        Integer num = rand.nextInt(1000);
        int div = num % whereArray.length;
        Log.info(thisClass, thisMethod, "Choosing entry from index: " + div);

        String entry = whereArray[div];
        Log.info(thisClass, thisMethod, "Entry chosen: " + entry);
        return entry;
    }

    public void addRequestParms() throws Exception {

        requestParms = new HashMap<String, String>();

        requestParms.put("Testing_parm1", "Parm1_value");
        requestParms.put("Testing_parm2", "Parm2_value");
        requestParms.put("Testing_parm3", "Parm3_value");
        // Log.info(thisClass, "addRequestParms", "the request parms: " +
        // requestParms) ;
    }

    public void addRequestFileParms() throws Exception {

        requestFileParms = new HashMap<String, String>();

        requestFileParms.put("textFile", "testFiles/testFile.txt");
        // requestFileParms.put("binaryFile", "testFiles/binaryFile.class") ;

    }

    public void addLargeRequestParms() throws Exception {
        addLargeRequestParms(3000);
    }

    public void addLargeRequestParms(Integer stringLen) throws Exception {

        StringBuilder sb = new StringBuilder(stringLen);
        for (int i = 0; i < 3000; i++) {
            sb.append('a');
        }

        requestParms = new HashMap<String, String>();
        requestParms.put("VeryLongParm", sb.toString());
    }

    /**
     * @return the responseMode
     */
    public String getResponseMode() {
        return responseMode;
    }

    /**
     * @param responseMode
     *            the responseMode to set
     */
    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    /**
     * @return the prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * @param prompt
     *            the prompt to set
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setStoreType(StoreType st) {
        storeType = st;
    }

    public StoreType getStoreType() {
        return storeType;
    }

    public void setHashed(boolean h) {
        isHash = h;
    }

    public boolean isHash() {
        return isHash;
    }

    public String getComponentID() {
        return componentID;
    }

    public void setComponentID(String componentID) {
        this.componentID = componentID;
    }

    public String getHttpString() {
        return httpString;
    }

    public void setHttpString(String httpString) {
        this.httpString = httpString;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(Integer httpPort) {
        this.httpPort = httpPort;
    }
}
