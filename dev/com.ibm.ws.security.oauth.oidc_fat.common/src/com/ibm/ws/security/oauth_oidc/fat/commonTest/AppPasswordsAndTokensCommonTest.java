/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import static org.junit.Assert.fail;

import java.net.URLEncoder;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.joda.time.Instant;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

/**
 * Test class for app-password and app-token endpoint test tools
 *
 * @author chrisc
 *
 */
public class AppPasswordsAndTokensCommonTest extends CommonTest {

    private static final Class<?> thisClass = AppPasswordsAndTokensCommonTest.class;

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    // adding rules in the main test class so we have access to all of the flags that are set

    /*********** NOTE: callSpecificCheck's return TRUE to SKIP a test ***********/

    /**
     * Rult to skip test if: Style of config is OIDC (test runs for oauth2Login, or oidcLogin (generic or provider specific
     * doesn't matter))
     *
     * @author chrisc
     *
     */
    public static class skipIfJWTAccess_token extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {

            if (testSettings.getRsTokenType() == Constants.JWT_TOKEN) {
                Log.info(thisClass, "skipIfJWTAccess_token", "OP is issuing JWT access_tokens - skipt test");
                testSkipped();
                return true;
            }
            Log.info(thisClass, "skipIfJWTAccess_token", "OP is issuing opaque access_tokens - run test");
            return false;
        }
    }

    // protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    // protected static CommonCookieTools cookieTools = new CommonCookieTools();
    // public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    // protected static String targetProvider = null;
    // protected static String[] goodActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static final String AppPasswordsOPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.app_passwords.OPserver";
    protected static final String AppPasswordsOP2ServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.app_passwords.OP2server";
    protected static final String AppPasswordsRSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.app_passwords.RSserver";
    protected static final String AppTokensOPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.app_tokens.OPserver";
    protected static final String AppTokensOP2ServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.app_tokens.OP2server";
    protected static final String AppTokensRSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.app_tokens.RSserver";
    protected static final String AppPasswordsOPServerAuditName = "com.ibm.ws.security.openidconnect.server-1.0_fat.app_passwords.OPserver.audit";
    protected static final String AppTokensOPServerAuditName = "com.ibm.ws.security.openidconnect.server-1.0_fat.app_tokens.OPserver.audit";

    protected static final long tenSeconds = 10L * 1000L;
    protected static final long fifteenSeconds = 15L * 1000L;
    protected static final long twentySeconds = 20L * 1000L;
    protected static final long thirtySeconds = 30L * 1000L;
    protected static final long oneMinute = 60L * 1000L;
    protected static final long oneHour = 60L * oneMinute;
    protected static final long oneDay = 24L * oneHour;
    protected static final long oneWeek = 7L * oneDay;
    protected static final long ninetyDays = 90L * oneDay;
    public static final long unknownLifetime = 0L;

    protected static final boolean ExpectSuccess = true;
    protected static final boolean ExpectFailure = false;

    protected static final String appPasswordNotSetOrUnknown = null;
    protected static final String appTokenNotSetOrUnknown = null;
    protected static final String appIdNotSetOrUnknown = null;
    protected static final String userNotSetOrUnknown = null;
    protected static final String nameNotSetOrUnknown = null;
    protected static final String usedByNotSetOrUnknown = null;
    protected static final String doNotOverrideProvider = null;
    protected static final String doNotOverrideApp = null;
    protected static final long createdAtNotSetOrUnknown = 0L;
    protected static final long expiresAtAtNotSetOrUnknown = 0L;

    protected static final String createType = "create";
    protected static final String listType = "list";
    protected static final String revokeType = "revoke";

    protected static final String appPasswordsEndpoint = "app-passwords";
    protected static final String appTokensEndpoint = "app-tokens";

    // OP Provider settings
    protected static final String baseOidcProvider = Constants.OIDCCONFIGSAMPLE_APP;
    protected static final String baseOidcProviderCopy = Constants.OIDCCONFIGSAMPLE_APP + "_copy";
    protected static final String shortLivedAccessTokenProvider = Constants.OIDCCONFIGSAMPLE_APP + "_shortLivedAccessToken";
    protected static final String appPwExchangeNotAllowedProvider = Constants.OIDCCONFIGSAMPLE_APP + "_appPwExchangeNotAllowed";
    protected static final String userClientTokenLimitProvider = Constants.OIDCCONFIGSAMPLE_APP + "_userClientTokenLimit";
    protected static final String shortLivedAppPasswordProvider = Constants.OIDCCONFIGSAMPLE_APP + "_shortLivedAppPw";
    protected static final String shortLivedAppTokenProvider = Constants.OIDCCONFIGSAMPLE_APP + "_shortLivedAppTok";
    protected static final String appPasswordLimitProvider = Constants.OIDCCONFIGSAMPLE_APP + "_maxAppPwAllowed";
    protected static final String appTokenLimitProvider = Constants.OIDCCONFIGSAMPLE_APP + "_maxAppTokAllowed";
    protected static final String missingPasswordGrantTypeProvider = Constants.OIDCCONFIGSAMPLE_APP + "_missingPasswordGrantType";

    protected static boolean defaultUseLdap = useLdap;

    protected long beforeTestTime = getNow();

    protected static final List<String> allGrantTypes = Arrays.asList("authorization_code", "implicit", "refresh_token", "client_credentials", "password", "urn:ietf:params:oauth:grant-type:jwt-bearer");
    protected static final List<String> rspTypes = Arrays.asList("code", "token", "id_token token");

    public class TokenValues {
        private final Class<?> c = TokenValues.class;

        private JsonObject rawJson;
        private String app_password;
        private String app_token;
        private String app_id;
        private String name;
        private String user;
        private String usedBy;
        private long created_at;
        private long expires_at;

        public String getApp_password() {
            return app_password;
        }

        public void setApp_password(String app_password) {
            this.app_password = app_password;
        }

        public String getApp_token() {
            return app_token;
        }

        public void setApp_token(String app_token) {
            this.app_token = app_token;
        }

        public String getApp_id() {
            return app_id;
        }

        public void setApp_id(String app_id) {
            this.app_id = app_id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getUsedBy() {
            return usedBy;
        }

        public long getCreated_at() {
            return created_at;
        }

        public void setCreated_at(long created_at) {
            this.created_at = created_at;
        }

        public long getExpires_at() {
            return expires_at;
        }

        public void setExpires_at(long expires_at) {
            this.expires_at = expires_at;
        }

        public JsonObject getRawJson() {
            return rawJson;
        }

        public TokenValues() {

        }

        public TokenValues(JsonNode jsonInfo) throws Exception {
            this(getStringValue(jsonInfo, Constants.APP_PASSWORD_KEY), getStringValue(jsonInfo, Constants.APP_TOKEN_KEY), getStringValue(jsonInfo, Constants.APP_ID_KEY), getStringValue(jsonInfo, Constants.APP_NAME_KEY), getStringValue(jsonInfo, Constants.USER_NAME_KEY), getLongValue(jsonInfo, Constants.CREATED_AT_KEY), getLongValue(jsonInfo, Constants.EXPIRES_AT_KEY));

            JsonReader reader = Json.createReader(new StringReader(jsonInfo.toString()));
            rawJson = reader.readObject();

            if (rawJson.containsKey("used_by")) {
                usedBy = rawJson.getString("used_by");
            }
        }

        public TokenValues(String inAppPassword, String inAppToken, String inAppId, String inAppName, String inUserId, long inCreatedAt, long inExpiresAt) {
            app_password = inAppPassword;
            app_token = inAppToken;
            app_id = inAppId;
            name = inAppName;
            user = inUserId;
            created_at = inCreatedAt;
            expires_at = inExpiresAt;
        }

        public void printTokenValues() {
            Log.info(c, "printTokenValues", "app_password: " + app_password);
            Log.info(c, "printTokenValues", "app_token: " + app_token);
            Log.info(c, "printTokenValues", "app_id: " + app_id);
            Log.info(c, "printTokenValues", "name: " + name);
            Log.info(c, "printTokenValues", "user: " + user);
            Log.info(c, "printTokenValues", "created_at: " + created_at);
            Log.info(c, "printTokenValues", "expires_at: " + expires_at);
            Log.info(c, "printTokenValues", "rawJson: " + rawJson);
        }

        @Override
        public String toString() {
            return "TokenValue: {" + "app_password: " + app_password + ", app_token: " + app_token + ", app_id: " + app_id + ", name: " + name + ", user: " + user + ", created_at: " + created_at + ", expires_at: " + expires_at + ", raw_json: " + rawJson + "}";
        }

    }

    public class AllTokens {
        private final Class<?> c = AllTokens.class;

        private String access_token;
        private String refresh_token;
        private String id_token;

        public String getAccessToken() {
            return access_token;
        }

        public void setAccessToken(String access_token) {
            this.access_token = access_token;
        }

        public String getRefreshToken() {
            return refresh_token;
        }

        public void setRefreshToken(String refresh_token) {
            this.refresh_token = refresh_token;
        }

        public String getIdToken() {
            return id_token;
        }

        public void setIdToken(String id_token) {
            this.id_token = id_token;
        }
    }

    // When we invoke the app-password endpoint to create an app-password, it'll return only
    // {"app_password":"<value>","app_id":"<value>","created_at":"<sometime around now>","expires_at":"<sometime around now + lifetime"}
    // so, the only things we can really validate are the times - it's not logging the appName, user, or usedBy (list gives more details)
    public TokenValues setCreateTokenValues(long tokenLifetime) {
        return new TokenValues(appPasswordNotSetOrUnknown, appTokenNotSetOrUnknown, appIdNotSetOrUnknown, nameNotSetOrUnknown, userNotSetOrUnknown, getNow(), tokenLifetime);
    }

    public void printTokenValues(List<TokenValues> tvs) {
        for (TokenValues tv : tvs) {
            tv.printTokenValues();
        }
    }

    public List<TokenValues> addTokenValuesToList(TokenValues values) {
        return addTokenValuesToList(values, null);
    }

    public List<TokenValues> addTokenValuesToList(TokenValues values, List<TokenValues> listOfValues) {
        if (listOfValues == null) {
            listOfValues = new ArrayList<TokenValues>();
        }
        if (values != null) {
            listOfValues.add(values);
        }
        return listOfValues;
    }

    // override the @Before method so that we can get the time before the test starts
    // (the tests need this to validate calculated timestamps)
    @Override
    @Before
    public void setTestName() throws Exception {
        beforeTestTime = getNow();
        super.setTestName();

    }

    @BeforeClass
    public static void beforeClass() {
        useLdap = false;
        testSettings.setAllowPrint(false);
        Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);
    }

    @AfterClass
    public static void afterClass() {
        useLdap = defaultUseLdap;
        testSettings.setAllowPrint(true);
        Log.info(thisClass, "afterClass", "Resetting useLdap to: " + useLdap);
    }

    /**
     * Get an access_token - the configuration will dictate whether it is an access_token or a JWT access_token
     *
     * @param settings
     *            - the current test settings
     * @return - returns the generated access_token
     * @throws Exception
     */
    public String getAccessToken(TestSettings settings) throws Exception {
        WebConversation wc = new WebConversation();
        return getAccessToken(wc, settings);
    }

    public String getAccessToken(WebConversation wc, TestSettings settings) throws Exception {

        // List<validationData> expectations = vData.addSuccessStatusCodes();
        //
        // WebResponse response = genericOP(_testName, wc, settings, Constants.BASIC_TOKEN_NOJSP_ACTIONS, expectations);
        // String accessToken = validationTools.getTokenForType(settings, response);
        // Log.info(thisClass, "getAccessToken", "The access_token value is: " + accessToken);
        // return accessToken;
        return getAllNewTokens(settings).getAccessToken();

    }

    public String getRefreshToken(TestSettings settings) throws Exception {
        // WebConversation wc = new WebConversation();
        // List<validationData> expectations = vData.addSuccessStatusCodes();
        //
        // WebResponse response = genericOP(_testName, wc, settings, Constants.BASIC_TOKEN_NOJSP_ACTIONS, expectations);
        // String refreshToken = validationTools.getTokenFromResponse(response, Constants.REFRESH_TOKEN_KEY);
        // Log.info(thisClass, "getRefreshToken", "The refresh_token value is: " + refreshToken);
        // return refreshToken;
        return getAllNewTokens(settings).getRefreshToken();

    }

    public String getIdToken(TestSettings settings) throws Exception {
        // WebConversation wc = new WebConversation();
        // List<validationData> expectations = vData.addSuccessStatusCodes();
        //
        // WebResponse response = genericOP(_testName, wc, settings, Constants.BASIC_TOKEN_NOJSP_ACTIONS, expectations);
        // String idToken = validationTools.getTokenFromResponse(response, Constants.ID_TOKEN_KEY);
        // Log.info(thisClass, "getIdToken", "The id_token value is: " + idToken);
        // return idToken;
        return getAllNewTokens(settings).getIdToken();

    }

    public AllTokens getAllNewTokens(TestSettings settings) throws Exception {
        WebConversation wc = new WebConversation();
        List<validationData> expectations = vData.addSuccessStatusCodes();
        vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_DOES_NOT_CONTAIN, "Ended up back on the login page but should not have. Check if user authentication failed.", null, "login.jsp");

        WebResponse response = genericOP(_testName, wc, settings, Constants.BASIC_TOKEN_NOJSP_ACTIONS, expectations);
        AllTokens tokens = new AllTokens();
        tokens.setAccessToken(validationTools.getTokenForType(settings, response));
        tokens.setRefreshToken(validationTools.getTokenFromResponse(response, Constants.REFRESH_TOKEN_KEY));
        tokens.setIdToken(validationTools.getTokenFromResponse(response, Constants.ID_TOKEN_KEY));
        Log.info(thisClass, "getAccessToken", "The access_token value is: " + tokens.getAccessToken());
        Log.info(thisClass, "getRefreshToken", "The refresh_token value is: " + tokens.getRefreshToken());
        Log.info(thisClass, "getIdToken", "The id_token value is: " + tokens.getIdToken());
        return tokens;

    }

    public String getRefreshedAccessToken(TestSettings settings, String refreshToken) throws Exception {
        return getRefreshedTokens(settings, refreshToken).getAccessToken();
    }

    public AllTokens getRefreshedTokens(TestSettings settings, String refreshToken) throws Exception {

        WebConversation wc = new WebConversation();
        List<validationData> expectations = vData.addSuccessStatusCodes();
        WebResponse response = invokeGenericForm_refreshToken(_testName, wc, settings, refreshToken, expectations);
        AllTokens refreshedTokens = new AllTokens();
        refreshedTokens.setAccessToken(validationTools.getTokenForType(settings, response));
        refreshedTokens.setRefreshToken(validationTools.getTokenFromResponse(response, Constants.REFRESH_TOKEN_KEY));
        refreshedTokens.setIdToken(validationTools.getTokenFromResponse(response, Constants.ID_TOKEN_KEY));
        Log.info(thisClass, "getAccessToken", "The refrehsed access_token value is: " + refreshedTokens.getAccessToken());
        Log.info(thisClass, "getRefreshToken", "The refreshed refresh_token value is: " + refreshedTokens.getRefreshToken());
        Log.info(thisClass, "getIdToken", "The refreshed id_token value is: " + refreshedTokens.getIdToken());
        return refreshedTokens;
    }

    public void revokeAccessToken(TestSettings settings, String accessToken) throws Exception {
        WebConversation wc = new WebConversation();
        List<validationData> expectations = vData.addSuccessStatusCodes();
        invokeGenericForm_revokeToken(_testName, wc, settings, accessToken, expectations);

    }

    /**
     * Use the app-password endpoint to create an app-password from the access_token passed in
     *
     * @param settings
     *            - the current test settings
     * @param accessToken
     *            - the access_token to use to generate the app-password
     * @param appName
     *            - the app name to register the app-password with
     * @param expectAnAppPassword
     *            - do we expect invokeAppPasswordEndpoint_create to be successful - if so, we should look for the app-password in
     *            the resopnse
     * @param expectations
     *            - what the caller expects to find/happen
     * @return - the generated app-password
     * @throws Exception
     */
    // appName is required - for negative tests that omit it, call either createAppPassword or invokeAppPasswordEndpoint_create directly and pass null
    // for the value
    // public TokenValues createAppPassword(TestSettings settings, String accessToken, boolean expectAnAppPassword, List<validationData> expectations)
    // throws Exception {
    // WebConversation wc = new WebConversation();
    // return createAppPassword(wc, settings, accessToken, null, null, expectAnAppPassword, expectations);
    // }

    public TokenValues createAppPasswords(TestSettings settings, String accessToken, String appName, boolean expectAnAppPassword, List<validationData> expectations) throws Exception {
        WebConversation wc = new WebConversation();
        return createAppPasswords(wc, settings, accessToken, appName, usedByNotSetOrUnknown, expectAnAppPassword, expectations);
    }

    public TokenValues createAppPasswords(TestSettings settings, String accessToken, String appName, String usedBy, boolean expectAnAppPassword, List<validationData> expectations) throws Exception {
        WebConversation wc = new WebConversation();
        return createAppPasswords(wc, settings, accessToken, appName, usedBy, expectAnAppPassword, expectations);
    }

    public TokenValues createAppPasswords(WebConversation wc, TestSettings settings, String accessToken, String appName, String usedBy, boolean expectAnAppPassword, List<validationData> expectations) throws Exception {
        return createAppPasswords(wc, settings, accessToken, appName, usedBy, Constants.HEADER, expectAnAppPassword, expectations);
    }

    public TokenValues createAppPasswords(WebConversation wc, TestSettings settings, String accessToken, String appName, String usedBy, String clientLocation, boolean expectAnAppPassword, List<validationData> expectations) throws Exception {

        WebResponse response = invokeAppPasswordsEndpoint_create(_testName, wc, settings, accessToken, appName, usedBy, clientLocation, expectations);
        List<TokenValues> values = null;
        if (expectAnAppPassword) {
            // create should generate info for one app-password - the tool will return a list of TokenValues - just grab the first entry
            values = getTokenInfoFromResponse(response);
            TokenValues aTokenValue = null;
            if (values != null) {
                aTokenValue = values.get(0);
                Log.info(thisClass, "createAppPassword", "The app-password value is: " + aTokenValue.getApp_password());
                // make sure that we were issued an opaque access_token (not a jwt)
                if (aTokenValue.getApp_password() != null) {
                    int len = aTokenValue.getApp_password().split("\\.").length;
                    if (len != 1) {
                        fail("Expected an opaque app-password that should have 1 part.  The app-password has " + len + " parts.");
                    }
                }
            }
            return aTokenValue;

        } else {
            // return null - there is no app-password - the call to invokeAppPasswordEndpoint_create should have validated
            // the response/behavior.  Any additional checking can be done by the caller
            return null;
        }
    }

    public List<TokenValues> createAndValidateAppPasswordsFromSameAccessToken(TestSettings settings, String accessToken, String appName, long lifetime, int numTokens) throws Exception {
        List<TokenValues> tokenValues = new ArrayList<TokenValues>();
        return createAndValidateAppPasswordsFromSameAccessToken(tokenValues, settings, accessToken, appName, null, lifetime, numTokens);
    }

    public List<TokenValues> createAndValidateAppPasswordsFromSameAccessToken(TestSettings settings, String accessToken, String appName, String usedBy, long lifetime, int numTokens) throws Exception {
        List<TokenValues> tokenValues = new ArrayList<TokenValues>();
        return createAndValidateAppPasswordsFromSameAccessToken(tokenValues, settings, accessToken, appName, usedBy, lifetime, numTokens);
    }

    public List<TokenValues> createAndValidateAppPasswordsFromSameAccessToken(List<TokenValues> tokenValues, TestSettings settings, String accessToken, String appName, String usedBy, long lifetime, int numTokens) throws Exception {
        Log.info(thisClass, "createAndValidateAppPasswordsFromSameAccessToken", "Creating and validating " + numTokens + " app passwords with access token [" + accessToken + "].");
        for (int i = 0; i < numTokens; i++) {
            String app_name = appName + Integer.toString(i);
            TokenValues actualValues = createAppPasswords(settings, accessToken, app_name, usedBy, ExpectSuccess, getGoodCreateAppPasswordsExpectations(settings));
            validateAppPasswordsCreateValues(actualValues, lifetime);
            // the list returned from this call can be used to validate data later, add values that were set, but not in the response from create for future use (name and user)
            actualValues.setName(app_name);
            actualValues.setUser(settings.getAdminUser());
            tokenValues.add(actualValues);
            Log.info(thisClass, "createAndValidateAppPasswordsFromSameAccessToken", "Created: " + (i + 1) + " app-passwords");
        }
        return tokenValues;
    }

    public List<TokenValues> listAppPasswords(TestSettings settings, String accessToken, String userId, List<validationData> expectations) throws Exception {
        WebConversation wc = new WebConversation();
        return listAppPasswords(wc, settings, accessToken, userId, expectations);
    }

    public List<TokenValues> listAppPasswords(WebConversation wc, TestSettings settings, String accessToken, String userId, List<validationData> expectations) throws Exception {

        return listAppPasswords(wc, settings, accessToken, userId, Constants.HEADER, expectations);

    }

    public List<TokenValues> listAppPasswords(WebConversation wc, TestSettings settings, String accessToken, String userId, String clientLocation, List<validationData> expectations) throws Exception {
        WebResponse response = invokeAppPasswordsEndpoint_list(_testName, wc, settings, accessToken, userId, clientLocation, expectations);
        // should generate a list of app-password entries
        return getTokenInfoFromResponse(response);
    }

    /**
     * Given a user, add expectations to ensure that app-passwords were found for the user
     */
    protected List<TokenValues> successfullyListNonEmptyAppPasswordsForUser(TestSettings settings, String userName, String accessToken) throws Exception {
        List<validationData> expectations = getSuccessfulListAppPasswordExpectations(settings, userName);
        addResponseContainsAppPasswordsExpectation(expectations, userName);

        return listAppPasswords(settings, accessToken, userName, expectations);
    }

    /**
     * Given a user, add expectations to ensure no app-passwords were found for the user
     */
    public List<TokenValues> successfullyListEmptyAppPasswordsForUser(TestSettings settings, String userName, String accessToken) throws Exception {
        List<validationData> expectations = getSuccessfulListAppPasswordExpectations(settings, userName);
        addResponseDoesNotContainAppPasswordsExpectation(expectations, userName);

        return listAppPasswords(settings, accessToken, userName, expectations);
    }

    public void revokeAppPasswords(TestSettings settings, String accessToken, String userId, String appId, List<validationData> expectations) throws Exception {
        WebConversation wc = new WebConversation();
        revokeAppPasswords(wc, settings, accessToken, userId, appId, expectations);
    }

    public void revokeAppPasswords(WebConversation wc, TestSettings settings, String accessToken, String userId, String appId, List<validationData> expectations) throws Exception {
        revokeAppPasswords(wc, settings, accessToken, userId, appId, Constants.HEADER, expectations);

    }

    public void revokeAppPasswords(WebConversation wc, TestSettings settings, String accessToken, String userId, String appId, String clientLocation, List<validationData> expectations) throws Exception {

        invokeAppPasswordsEndpoint_revoke(_testName, wc, settings, accessToken, userId, appId, clientLocation, expectations);

    }

    protected void verifyValidAppPasswordCanObtainNewToken(TestSettings settings, TokenValues appPassword) throws Exception {
        getAccessTokenFromAppPassword(settings, appPassword.getApp_password(), ExpectSuccess, getGoodTokenEndpointExpectations(settings));
    }

    protected void verifyValidAppPasswordObtainNewTokenFails(TestSettings settings, TokenValues appPassword) throws Exception {
        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_TOKEN_ENDPOINT, Constants.UNAUTHORIZED_STATUS);
         expectations = validationTools.addMessageExpectation(testOPServer, 
        		 expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.MESSAGES_LOG, 
        		 Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWOAU0074E_CANNOT_COMPLETE_PASSWORD_EXCHANGE + " error message.", 
        		 MessageConstants.CWOAU0074E_CANNOT_COMPLETE_PASSWORD_EXCHANGE);
        getAccessTokenFromAppPassword(settings, appPassword.getApp_password(), ExpectFailure, expectations);
    }

    protected void verifyValidAppPasswordCanObtainNewToken(TestSettings settings, List<TokenValues> appPasswords) throws Exception {
        for (TokenValues ap : appPasswords) {
            verifyValidAppPasswordCanObtainNewToken(settings, ap);
        }
    }

    protected void verifyRevokedAppPasswordCannotObtainNewToken(TestSettings settings, List<TokenValues> appPasswords) throws Exception {
        for (TokenValues ap : appPasswords) {
            verifyRevokedAppPasswordCannotObtainNewToken(settings, ap);
        }
    }

    protected void verifyRevokedAppPasswordCannotObtainNewToken(TestSettings settings, TokenValues appPassword) throws Exception {
        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_TOKEN_ENDPOINT, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.ERROR_CODE_INVALID_CLIENT + " error message.", null, "\"error\":\"" + Constants.ERROR_CODE_INVALID_CLIENT + "\"");
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWOAU0074E_CANNOT_COMPLETE_PASSWORD_EXCHANGE + " error message.", MessageConstants.CWOAU0074E_CANNOT_COMPLETE_PASSWORD_EXCHANGE);

        getAccessTokenFromAppPassword(testSettings, appPassword.getApp_password(), ExpectFailure, expectations);
    }

    protected void verifyRevokedAppTokenCannotBeUsedToAccessProtectedResource(String testName, TestSettings settings, String appToken) throws Exception {
        List<validationData> expectations = addGeneralRSProtectedApp401Expectations();

        if (genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN);
        } else {
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
        }
        helpers.invokeRsProtectedResource(testName, new WebConversation(), appToken, settings, expectations);
    }

    protected void verifyRevokedAppTokenCannotBeUsedToAccessProtectedResource(String testName, TestSettings settings, List<TokenValues> appTokens) throws Exception {
        for (TokenValues appToken : appTokens) {
            verifyRevokedAppTokenCannotBeUsedToAccessProtectedResource(_testName, settings, appToken.getApp_token());
        }
    }

    protected void verifyValidAppTokenCanBeUsedToAccessProtectedResource(String testName, TestSettings settings, String appToken) throws Exception {
        helpers.invokeRsProtectedResource(testName, new WebConversation(), appToken, settings, addRSProtectedAppExpectations(settings, appToken));
    }

    protected void verifyValidAppTokenCanBeUsedToAccessProtectedResource(String testName, TestSettings settings, List<TokenValues> appTokens) throws Exception {
        for (TokenValues appToken : appTokens) {
            verifyValidAppTokenCanBeUsedToAccessProtectedResource(_testName, settings, appToken.getApp_token());
        }
    }

    /**
     * Revoke all of the app-passwords referenced by the access_token/userId/appId passed in Finally, validate that the
     * app-passwords have been
     * revoked by listing all app-passwords for the same accessToken token and ensure that what should have been revoked are
     * missing
     *
     * @param toBeDeletedValues
     *            - the TokenValues representing the app-passwords that should be revoked
     * @param settings
     *            - the current test settings
     * @param accessToken
     *            - the access token to revoke app-passwords for
     * @param userId
     *            - the user name to revoke app-passwords for
     * @param appId
     *            - the app id to revoke app-passwords for
     * @throws Exception
     */
    public void revokeAndValidateAppPasswords(List<TokenValues> toBeDeletedValues, TestSettings settings, String accessToken, String userId, String appId) throws Exception {

        revokeAppPasswords(settings, accessToken, userId, appId, getGoodRevokeAppPasswordsExpectations(settings));
        // list all app-passwords for a specific access_token
        List<TokenValues> afterValues = listAppPasswords(settings, accessToken, userId, getGoodListAppTokensExpectations(settings));
        // loop through what's left after the revoke
        for (TokenValues values : afterValues) {
            // make sure that no entries exist for the userId if it was specified
            if (userId != userNotSetOrUnknown && values.getUser().equals(userId)) {
                fail("");
            }
            // make sure that no entries exist for the appIdd if it was specified
            if (appId != appIdNotSetOrUnknown && values.getApp_id().equals(appId)) {
                fail("");
            }
            // Finally, loop through he list of entries that the caller expected to be deleted and make sure that they are gone.
            if (toBeDeletedValues != null) {
                for (TokenValues dValues : toBeDeletedValues) {
                    if (values.getApp_id().equals(dValues.getApp_id())) {
                        fail("");
                    }
                }
            }
            // don't think we need to make sure that any new entries have shown up (no entries in after that were not there in a before list)
        }

    }

    /**
     * Use the app-token endpoint to create an app-token from the access_token passed in
     *
     * @param settings
     *            - the current test settings
     * @param accessToken
     *            - the access_token to use to generate the app-token
     * @param appName
     *            - the appname to register the app-token with
     * @param expectAnAppToken
     *            - do we expect invokeAppTokenEndpoint_create to be successful - if so, we should look for the app-token in the
     *            resopnse
     * @param expectations
     *            - what the caller expects to find/happen
     * @return - the generated app-token
     * @throws Exception
     */
    public TokenValues createAppTokens(TestSettings settings, String accessToken, String appName, boolean expectAnAppToken, List<validationData> expectations) throws Exception {
        return createAppTokens(settings, accessToken, appName, usedByNotSetOrUnknown, expectAnAppToken, expectations);
    }

    public TokenValues createAppTokens(TestSettings settings, String accessToken, String appName, String usedBy, boolean expectAnAppToken, List<validationData> expectations) throws Exception {
        WebConversation wc = new WebConversation();
        return createAppTokens(wc, settings, accessToken, appName, usedBy, Constants.HEADER, expectAnAppToken, expectations);
    }

    public TokenValues createAppTokens(WebConversation wc, TestSettings settings, String accessToken, String appName, String clientLocation, boolean expectAnAppToken, List<validationData> expectations) throws Exception {
        return createAppTokens(wc, settings, accessToken, appName, usedByNotSetOrUnknown, clientLocation, expectAnAppToken, expectations);
    }

    public TokenValues createAppTokens(WebConversation wc, TestSettings settings, String accessToken, String appName, String usedBy, String clientLocation, boolean expectAnAppToken, List<validationData> expectations) throws Exception {
        WebResponse response = invokeAppTokensEndpoint_create(_testName, wc, settings, accessToken, appName, usedBy, clientLocation, expectations);
        List<TokenValues> values = null;
        if (expectAnAppToken) {
            values = getTokenInfoFromResponse(response);
            TokenValues aTokenValue = null;
            if (values != null) {
                aTokenValue = values.get(0);
                Log.info(thisClass, "createAppToken", "The app-token value is: " + aTokenValue.getApp_token());
            }
            return aTokenValue;
        } else {
            // fail should terminate the execution, so the return null should be not needed, but we need to be able to compile
            //            fail("Information about the created app-token was not found in the response");
            return null;
        }
    }

    public List<TokenValues> createAndValidateAppTokensFromSameAccessToken(TestSettings settings, String accessToken, String appName, long lifetime, int numTokens) throws Exception {
        return createAndValidateAppTokensFromSameAccessToken(settings, accessToken, appName, usedByNotSetOrUnknown, lifetime, numTokens);
    }

    public List<TokenValues> createAndValidateAppTokensFromSameAccessToken(TestSettings settings, String accessToken, String appName, String usedBy, long lifetime, int numTokens) throws Exception {
        List<TokenValues> tokenValues = new ArrayList<TokenValues>();
        return createAndValidateAppTokensFromSameAccessToken(tokenValues, settings, accessToken, appName, usedBy, lifetime, numTokens);
    }

    public List<TokenValues> createAndValidateAppTokensFromSameAccessToken(List<TokenValues> tokenValues, TestSettings settings, String accessToken, String appName, String usedBy, long lifetime, int numTokens) throws Exception {
        Log.info(thisClass, "createAndValidateAppTokensFromSameAccessToken", "Creating and validating " + numTokens + " app tokens with access token [" + accessToken + "].");
        for (int i = 0; i < numTokens; i++) {
            String app_name = appName + Integer.toString(i);
            TokenValues actualValues = createAppTokens(settings, accessToken, app_name, usedBy, ExpectSuccess, getGoodCreateAppTokensExpectations(settings));
            validateAppTokensCreateValues(actualValues, lifetime);
            // the list returned from this call can be used to validate data later, add values that were set, but not in the response from create for future use (name and user)
            actualValues.setName(app_name);
            actualValues.setUser(settings.getAdminUser());
            tokenValues.add(actualValues);
            Log.info(thisClass, "createAndValidateAppTokensFromSameAccessToken", "Created: " + (i + 1) + " app-tokens");
        }
        return tokenValues;
    }

    public List<TokenValues> listAppTokens(TestSettings settings, String accessToken, String userId, boolean expectAnAppTokenList, List<validationData> expectations) throws Exception {
        WebConversation wc = new WebConversation();
        return listAppTokens(wc, settings, accessToken, userId, expectAnAppTokenList, expectations);
    }

    public List<TokenValues> listAppTokens(WebConversation wc, TestSettings settings, String accessToken, String userId, boolean expectAnAppTokenList, List<validationData> expectations) throws Exception {
        WebResponse response = invokeAppTokensEndpoint_list(_testName, wc, settings, accessToken, userId, expectations);
        // should generate a list of app-token entries
        return getTokenInfoFromResponse(response);
    }

    /**
     * Given a user, add expectations to ensure that app-tokens were found for the user
     */
    protected List<TokenValues> successfullyListNonEmptyAppTokensForUser(TestSettings settings, String userName, String accessToken) throws Exception {
        List<validationData> expectations = getSuccessfulListAppTokensExpectations(settings, userName);
        addResponseContainsAppTokensExpectation(expectations, userName);

        return listAppTokens(settings, accessToken, userName, ExpectSuccess, expectations);
    }

    /**
     * Given a user, add expectations to ensure no app-tokens were found for the user
     */
    protected List<TokenValues> successfullyListEmptyAppTokensForUser(TestSettings settings, String userName, String accessToken) throws Exception {
        List<validationData> expectations = getSuccessfulListAppTokensExpectations(settings, userName);
        addResponseDoesNotContainAppTokensExpectation(expectations, userName);

        return listAppTokens(settings, accessToken, userName, ExpectSuccess, expectations);
    }

    public void revokeAppTokens(TestSettings settings, String accessToken, String userId, String appId, List<validationData> expectations) throws Exception {
        WebConversation wc = new WebConversation();
        revokeAppTokens(wc, settings, accessToken, userId, appId, expectations);
    }

    public void revokeAppTokens(WebConversation wc, TestSettings settings, String accessToken, String userId, String appId, List<validationData> expectations) throws Exception {

        invokeAppTokensEndpoint_revoke(_testName, wc, settings, accessToken, userId, appId, expectations);

    }

    /**
     * Revoke all of the app-tokens referenced by the access_token/userId/appId passed in Finally, validate that the
     * app-tokens have been revoked
     * by listing all app-tokens for the same accessToken token and ensure that what should have been revoked are missing
     *
     * @param toBeDeletedValues
     *            - the TokenValues representing the app-tokens that should be revoked
     * @param settings
     *            - the current test settings
     * @param accessToken
     *            - the access token to revoke app-tokens for
     * @param userId
     *            - the user name to revoke app-tokens for
     * @param appId
     *            - the app id to revoke app-tokens for
     * @throws Exception
     */
    public void revokeAndValidateAppTokens(List<TokenValues> toBeDeletedValues, TestSettings settings, String accessToken, String userId, String appId) throws Exception {

        revokeAppTokens(settings, accessToken, userId, appId, getGoodRevokeAppTokensExpectations(settings));
        // list all app-tokens for a specific access_token
        List<TokenValues> afterValues = listAppTokens(settings, accessToken, userNotSetOrUnknown, ExpectSuccess, getGoodListAppTokensExpectations(settings));
        // loop through what's left after the revoke
        for (TokenValues values : afterValues) {
            // make sure that no entries exist for the userId if it was specified
            if (userId != userNotSetOrUnknown && values.getUser().equals(userId)) {
                fail("");
            }
            // make sure that no entries exist for the appId if it was specified
            if (appId != appIdNotSetOrUnknown && values.getApp_id().equals(appId)) {
                fail("");
            }
            // Finally, loop through he list of entries that the caller expected to be deleted and make sure that they are gone.
            if (toBeDeletedValues != null) {
                for (TokenValues dValues : toBeDeletedValues) {
                    if (values.getApp_id().equals(dValues.getApp_id())) {
                        fail("");
                    }
                }
            }
            // don't think we need to make sure that any new entries have shown up (no entries in after that were not there in a before list)
        }

    }

    /**
     * Use the token endpoint to exchange an app-password for a new access-token
     *
     * @param settings
     *            - the current test settings
     * @param expectAnAcessToken
     *            - do we expect the call to invokeTokenEndpoint_password to be successful - if so, we should look for the
     *            access_token in the
     *            response
     * @param appPassword
     *            - the previously obtained app-password
     * @param expectations
     *            - what the caller expects to find/happen
     * @return - the the newly generated access_token
     * @throws Exception
     */
    public String getAccessTokenFromAppPassword(TestSettings settings, String appPassword, boolean expectAnAcessToken, List<validationData> expectations) throws Exception {
        WebConversation wc = new WebConversation();
        return getTokensFromAppPassword(wc, settings, appPassword, expectAnAcessToken, expectations).getAccessToken();

    }

    public AllTokens getTokensFromAppPassword(TestSettings settings, String appPassword, boolean expectAnAcessToken, List<validationData> expectations) throws Exception {
        WebConversation wc = new WebConversation();
        return getTokensFromAppPassword(wc, settings, appPassword, expectAnAcessToken, expectations);

    }

    public AllTokens getTokensFromAppPassword(WebConversation wc, TestSettings settings, String appPassword, boolean expectAnAcessToken, List<validationData> expectations) throws Exception {

        TestSettings updatedTestSettings = settings.copyTestSettings();
        updatedTestSettings.setAdminPswd(appPassword);

        WebResponse response = invokeTokenEndpoint_password(_testName, wc, updatedTestSettings, expectations);
        AllTokens tokens = new AllTokens();

        if (expectAnAcessToken) {
            String accessToken = validationTools.getTokenForType(updatedTestSettings, response);
            if (accessToken != null) {
                int len = accessToken.split("\\.").length;
                if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
                    if (len != 1) {
                        fail("Expected an opaque access token that should have 1 part.  The token has " + len + " parts.");
                    }
                } else {
                    if (len != 3) {
                        // our tests should only be issuing tokens with 3 parts (technically, 2 or 3 parts are allowed)
                        fail("Expected a JWT access token that should have 3 parts.  The token has " + len + " parts.");
                    }
                }
            }
            tokens.setAccessToken(accessToken);
            tokens.setRefreshToken(validationTools.getTokenFromResponse(response, Constants.REFRESH_TOKEN_KEY));
            tokens.setIdToken(validationTools.getTokenFromResponse(response, Constants.ID_TOKEN_KEY));
        }
        Log.info(thisClass, "getTokensFromAppPassword", "The access_token from app-password value is: " + tokens.getAccessToken());
        Log.info(thisClass, "getTokensFromAppPassword", "The refresh_token from app-password value is: " + tokens.getRefreshToken());
        Log.info(thisClass, "getTokensFromAppPassword", "The id_token from app-password value is: " + tokens.getIdToken());
        return tokens;

    }

    /**
     * Creates a list of TokenValues objects containing the data recorded in Web response
     */
    public List<TokenValues> getTokenInfoFromResponse(Object response) throws Exception {
        String thisMethod = "getTokenInfoFromResponse";
        msgUtils.printMethodName(thisMethod, "Start of");

        List<TokenValues> values = new ArrayList<TokenValues>();
        try {
            String respReceived = AutomationTools.getResponseText(response); // maybe need to remove the starting/ending {}
            Log.info(thisClass, thisMethod, "raw response: " + respReceived);
            if (respReceived == null || respReceived.isEmpty()) {
                return null;
            }
            values = parseTokenValuesFromResponse(respReceived);
            printTokenValues(values);
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e);
            // return null - to make a point that something went wrong - we may decide that a partial list is ok and remove the next line.
            // I would hope that the test case checks for some value in the response before calling this method (ie: app_id, app_password, app_token,
            // ...)
            return null;
        }
        return values;
    }

    private List<TokenValues> parseTokenValuesFromResponse(String respReceived) throws Exception {
        String method = "parseTokenValuesFromResponse";
        List<TokenValues> values = new ArrayList<TokenValues>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode matrix = mapper.readValue(respReceived, JsonNode.class);
        if (matrix.isArray()) {
            Log.info(thisClass, method, "Response is Array");
            parseTokenValuesFromArrayNode(values, matrix);
        } else {
            Log.info(thisClass, method, "Response is one Node");
            parseTokenValuesFromSingleNode(values, matrix);
        }
        return values;
    }

    private void parseTokenValuesFromArrayNode(List<TokenValues> values, JsonNode matrix) throws Exception {
        Iterator<JsonNode> iter = matrix.iterator();
        while (iter.hasNext()) {
            JsonNode tokenInfo = iter.next();
            values.add(getValuesFromNode(tokenInfo));
        }
    }

    private void parseTokenValuesFromSingleNode(List<TokenValues> values, JsonNode matrix) throws Exception {
        JsonNode tokenArray = matrix.get("app-passwords");
        if (tokenArray == null) {
            tokenArray = matrix.get("app-tokens");
        }
        if (tokenArray != null) {
            parseTokenValuesFromArrayNode(values, tokenArray);
        } else {
            values.add(getValuesFromNode(matrix));
        }
    }

    public TokenValues getValuesFromNode(JsonNode node) throws Exception {

        TokenValues val = new TokenValues(node);

        return val;
    }

    public String getStringValue(JsonNode node, String field) throws Exception {
        if (node.has(field)) {
            return node.get(field).asText();
        } else {
            return null;
        }
    }

    public long getLongValue(JsonNode node, String field) throws Exception {
        if (node.has(field)) {
            return node.get(field).asLong();
        } else {
            return 0L;
        }
    }

    /**
     * When we create an app-password, the create will return app_password, app_id, created_at and expires_at in the response. We
     * need to make sure
     * that we have some value for app_password and app_id as both of these are created by the app-password endpoint (we hav eno
     * way of knowing what
     * that value could be). The created_at value should be the current time or maybe 10 or 20 seconds earlier. The expires_at
     * value should be the
     * created_at time plus the lifetime that is configured in the provider.
     *
     * @param av
     * @param expected
     * @throws Exception
     */
    public void validateAppPasswordsCreateValues(TokenValues av, long lifetime) throws Exception {

        msgUtils.assertTrueAndLog(_testName, "The app_id value was not found in the response from the app-password create request", av.getApp_id() != appIdNotSetOrUnknown);
        msgUtils.assertTrueAndLog(_testName, "The app_password value was not found in the response from the app-password create request", av.getApp_password() != appPasswordNotSetOrUnknown);
        msgUtils.assertTrueAndLog(_testName, "An app_token value was found in the response from the app-password create request and should not exist", av.getApp_token() == appTokenNotSetOrUnknown);
        validateCommonCreateValues("app-password", av, lifetime);
    }

    public void validateAppTokensCreateValues(TokenValues av, long lifetime) throws Exception {

        msgUtils.assertTrueAndLog(_testName, "An app_id value was found in the response from the app-token create request and should not exist", av.getApp_id() != appIdNotSetOrUnknown);
        msgUtils.assertTrueAndLog(_testName, "An app_password value was found in the response from the app-token create request and should not exist", av.getApp_password() == appPasswordNotSetOrUnknown);
        msgUtils.assertTrueAndLog(_testName, "The app_token value was not found in the response from the app-token create request", av.getApp_token() != appTokenNotSetOrUnknown);
        validateCommonCreateValues("app-token", av, lifetime);
    }

    public void validateCommonCreateValues(String createEndpoint, TokenValues av, long lifetime) throws Exception {
        msgUtils.assertTrueAndLog(_testName, "The name value was found in the response from the " + createEndpoint + " create request and should not exist", av.getName() == nameNotSetOrUnknown);
        msgUtils.assertTrueAndLog(_testName, "The user value was found in the response from the " + createEndpoint + " create request and should not exist", av.getUser() == userNotSetOrUnknown);
        validateTime(createEndpoint, av, lifetime);
    }

    public void validateTime(String createEndpoint, TokenValues av, long lifetime) throws Exception {
        long now = getNow();
        msgUtils.assertTrueAndLog(_testName, "The created_at time [" + av.getCreated_at() + "] in the response from " + createEndpoint + " is not >= the time before the create [" + beforeTestTime + " ] and <= the time now [" + now + "].", (av.getCreated_at() >= beforeTestTime && av.getCreated_at() <= now));
        if (lifetime != unknownLifetime) { // skip checking expiration - caller didn't know what value config has set
            msgUtils.assertTrueAndLog(_testName, "The expires_at time [" + av.getExpires_at() + "] in the response from " + createEndpoint + " is not >= the time before the create [" + beforeTestTime + "] plus the lifetime [" + lifetime + "] and not <= the time now [" + now + "] plus the lifetime [" + lifetime + "].", ((av.getExpires_at() >= beforeTestTime + lifetime) && (av.getExpires_at() <= (now + lifetime))));
        }
    }

    /**
     * When app-passwords List (GET) is run, it will return all appropriate app-passwords (based on user/app-id...)
     * We need to make sure that all appropriate app-passwords were returned and that the data shown for each is
     * correct. We have a list of the app-passwords that the test created (along with the data for each). We need
     * to make sure sure that all of them are listed. We also need to make sure that no app-tokens are included
     * in the list.
     *
     * @param values
     * @param expectedValues
     * @throws Exception
     */
    public void validateAppPasswordsListValues(List<TokenValues> values, List<TokenValues> expectedValues) throws Exception {

        msgUtils.assertTrueAndLog(_testName, "The expected app_id was NOT found", validateAppIdsInResponse(values, expectedValues));

        validateCommonListValues(values, expectedValues);

    }

    /**
     * When app-tokens List (GET) is run, it will return all appropriate app-tokens (based on user/token-id...)
     * We need to make sure that all appropriate app-tokens were returned and that the data shown for each is
     * correct. We have a list of the app-tokens that the test created (along with the data for each). We need
     * to make sure sure that all of them are listed. We also need to make sure that no app-passwords are included
     * in the list.
     *
     * @param values
     * @param expectedValues
     * @throws Exception
     */
    public void validateAppTokensListValues(List<TokenValues> values, List<TokenValues> expectedValues) throws Exception {

        msgUtils.assertTrueAndLog(_testName, "The expected app_id was NOT found", validateAppIdsInResponse(values, expectedValues));

        validateCommonListValues(values, expectedValues);

    }

    public void validateCommonListValues(List<TokenValues> values, List<TokenValues> expectedValues) throws Exception {

        // list doesn't return the app-password or app-token (just the id's)
        msgUtils.assertTrueAndLog(_testName, "An unexpected app_password was found", validateNoAppPasswordsInResponse(values));
        msgUtils.assertTrueAndLog(_testName, "An unexpected app_token was found", validateNoAppTokensInResponse(values));

        msgUtils.assertTrueAndLog(_testName, "The expected name was NOT valid", validateNamesInResponse(values, expectedValues));
        msgUtils.assertTrueAndLog(_testName, "The expected user was NOT valid", validateUsersInResponse(values, expectedValues));

        msgUtils.assertTrueAndLog(_testName, "The expected created_at was NOT valid", validateCreatedAtsInResponse(values, expectedValues));
        msgUtils.assertTrueAndLog(_testName, "The expected expires_at was NOT valid", validateExpiresAtsInResponse(values, expectedValues));
    }

    /**
     * Validates that data does not exist in the response of the called command
     *
     * @param response
     *            - the response from an invoked command
     * @param expectedValues
     *            - The token info that we expect to NOT see
     * @throws Exception
     */
    //    public void validateDoesNotExistInResponse(Object response, List<TokenValues> expectedValues) throws Exception {
    //        List<TokenValues> values = getTokenInfoFromResponse(response);
    //        validateDoesNotExistInResponse(values, expectedValues);
    //    }

    public void validateAppPasswordDoesNotExistInResponse(List<TokenValues> values, List<TokenValues> expectedValues) throws Exception {
        // make sure we have all the app_ids that we expect
        for (TokenValues tv : expectedValues) {
            msgUtils.assertTrueAndLog(_testName, "The expected app_id [" + tv.getApp_id() + "] was NOT found", !validateAppIdInResponse(values, tv));
        }
    }

    public void validateAppTokenDoesNotExistInResponse(List<TokenValues> values, List<TokenValues> expectedValues) throws Exception {
        // make sure we have all the app_ids that we expect
        for (TokenValues tv : expectedValues) {
            msgUtils.assertTrueAndLog(_testName, "The expected app_id [" + tv.getApp_id() + "] was NOT found", !validateAppIdInResponse(values, tv));
        }
    }

    public boolean validateNoAppIdsInResponse(List<TokenValues> actualValues) throws Exception {

        for (TokenValues av : actualValues) {
            if (av.getApp_id() != appIdNotSetOrUnknown) {
                Log.info(thisClass, "validateNoAppIdsInResponse", "Found app_id: " + av.getApp_id() + " and should not have");
                return false;
            }
        }
        return true;
    }

    /**
     * Makes sure that all app_ids in expectedValues are found in the response Caller determines if any missing app_ids is a
     * problem
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues
     *            - List of TokenValues that we expect to find
     * @return - return true if all app_ids were found, false if at least one app_id was not found
     * @throws Exception
     */
    public boolean validateAppIdsInResponse(List<TokenValues> actualValues, List<TokenValues> expectedValues) throws Exception {
        boolean allFound = true;
        for (TokenValues expValues : expectedValues) {
            if (!validateAppIdInResponse(actualValues, expValues)) {
                allFound = false;
            }
        }
        return allFound;
    }

    /**
     * Makes sure that one specific app_id is found the the response. Caller determines if not finding the app_id is a problem.
     * Only List uses this method - don't worry about not set values - shouldn't get here if the app_id is not set
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues
     *            - One TokenValues containing the app_id that we want to check
     * @return - returns true if the app_id is found, false, if it is not
     * @throws Exception
     */
    public boolean validateAppIdInResponse(List<TokenValues> actualValues, TokenValues expectedValues) throws Exception {
        // loop through actual and compare against expected - as soon as we have a mis-match, go on to the next one
        for (TokenValues av : actualValues) {
            if (expectedValues.getApp_id() == appIdNotSetOrUnknown) {
                return false; // should not get here with an null app_id
            } else {
                if (expectedValues.getApp_id().equals(av.getApp_id())) {
                    return true;
                }
                // try the next value in the list
            }
        }
        Log.info(thisClass, "validateAppIdResponse", "Looped through all actual values and did not find a match for the expected app_id: " + expectedValues.getApp_id());
        return false;
    }

    public boolean validateNoAppPasswordsInResponse(List<TokenValues> actualValues) throws Exception {

        for (TokenValues av : actualValues) {
            if (av.getApp_password() != appPasswordNotSetOrUnknown) {
                Log.info(thisClass, "validateNoAppPasswordsInResponse", "Found app_password: " + av.getApp_password() + " and should not have");
                return false;
            }
        }
        return true;
    }

    /**
     * Makes sure that a value is found or not found for app_password in each matched app_id. Caller determines if not finding the
     * expected value is a
     * problem.
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues
     *            - List of TokenValues that we will validate - making sure that we have or don't have an app_password value when
     *            we match the app_id
     * @return - returns true if the app_password exists or doesn't exist as the expectValues dictate, false if not
     * @throws Exception
     */
    public boolean validateAppPasswordsInResponse(String endpoint, List<TokenValues> actualValues, List<TokenValues> expectedValues) throws Exception {
        boolean allFound = true;
        for (TokenValues expValues : expectedValues) {
            if (!validateAppPasswordInResponse(endpoint, actualValues, expValues)) {
                allFound = false;
            }
        }
        return allFound;
    }

    /**
     * Makes sure that when we find the matching entry (app_id) in actualValues, the app_password exists if that's what's
     * expected, or doesn't exist
     * if that's what's expected.
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues-
     *            One TokenValues containing the app_id and a set or un-set app_password
     * @return - returns true if the app_password is set correctly, false otherwise
     * @throws Exception
     */
    public boolean validateAppPasswordInResponse(String endpoint, List<TokenValues> actualValues, TokenValues expectedValues) throws Exception {
        // loop through actual and compare against expected - app_id must match first (we want to compare the appPassword of that entry only)
        for (TokenValues av : actualValues) {

            if (expectedValues.getApp_password() == appPasswordNotSetOrUnknown) {
                // we didn't know the app_password (we were calling create or testing app-tokens)
                if (endpoint.equals(appPasswordsEndpoint)) {
                    if (av.getApp_password() != appPasswordNotSetOrUnknown) {
                        return true;
                    }
                    Log.info(thisClass, "validateAppPasswordInResponse", "Did not an app_password in the response");
                    return false;
                } else {
                    if (av.getApp_password() == appPasswordNotSetOrUnknown) {
                        return true;
                    }
                    Log.info(thisClass, "validateAppPasswordInResponse", "Found an app_password in the response and should not have");
                    return false;
                }
            } else {
                if (expectedValues.getApp_password().equals(av.getApp_password())) {
                    return true;
                }
                Log.info(thisClass, "validateAppPasswordInResponse", "Did not find app_password: [" + expectedValues.getApp_password() + "] in the response");
                return false;
            }
        }
        Log.info(thisClass, "validateAppPasswordInResponse", "None of the expected cases were handled - should not get to this point in the method");
        return false;

    }

    public boolean validateNoAppTokensInResponse(List<TokenValues> actualValues) throws Exception {

        for (TokenValues av : actualValues) {
            if (av.getApp_token() != appTokenNotSetOrUnknown) {
                Log.info(thisClass, "validateNoAppTokensInResponse", "Found app_token: " + av.getApp_token() + " and should not have");
                return false;
            }
        }
        return true;
    }

    /**
     * Makes sure that a value is found or not found for app_token in each matched app_id. Caller determines if not finding the
     * expected value is a
     * problem.
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues
     *            - List of TokenValues that we will validate - making sure that we have or don't have an app_token value when we
     *            match the app_id
     * @return - returns true if the app_token exists or doesn't exist as the expectValues dictate, false if not
     * @throws Exception
     */
    public boolean validateAppTokensInResponse(String endpoint, List<TokenValues> actualValues, List<TokenValues> expectedValues) throws Exception {
        boolean allFound = true;
        for (TokenValues expValues : expectedValues) {
            if (!validateAppTokenInResponse(endpoint, actualValues, expValues)) {
                allFound = false;
            }
        }
        return allFound;
    }

    /**
     * Makes sure that when we find the matching entry (app_id) in actualValues, the app_token exists if that's what's expected,
     * or doesn't exist if
     * that's what's expected.
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues-
     *            One TokenValues containing the app_id and a set or un-set app_token
     * @return - returns true if the app_token is set correctly, false otherwise
     * @throws Exception
     */
    public boolean validateAppTokenInResponse(String endpoint, List<TokenValues> actualValues, TokenValues expectedValues) throws Exception {
        // loop through actual and compare against expected - app_token must match first (we want to compare the appToken of that entry only)
        for (TokenValues av : actualValues) {
            if (expectedValues.getApp_token() == appTokenNotSetOrUnknown) {
                // we didn't know the app_id (we were calling create or testing app-passwords)
                if (endpoint.equals(appTokensEndpoint)) {
                    if (av.getApp_token() != appTokenNotSetOrUnknown) {
                        return true;
                    }
                    Log.info(thisClass, "validateAppTokenInResponse", "Did not an app_token in the response");
                    return false;
                } else {
                    if (av.getApp_token() == appTokenNotSetOrUnknown) {
                        return true;
                    }
                    Log.info(thisClass, "validateAppTokenInResponse", "Found an app_token in the response and should not have");
                    return false;
                }
            } else {
                if (expectedValues.getApp_token().equals(av.getApp_token())) {
                    return true;
                }
                Log.info(thisClass, "validateAppTokenInResponse", "Did not find app_token: [" + expectedValues.getApp_token() + "] in the response");
                return false;
            }
        }
        Log.info(thisClass, "validateAppTokenInResponse", "None of the expected cases were handled - should not get to this point in the method");
        return false;

    }

    /**
     * Makes sure that name is set properly for each matched app_id. Caller determines if not finding the expected value is a
     * problem.
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues
     *            - List of TokenValues that we will validate - (specifically need app_id and name for this check)
     * @return - returns true if all names are correct
     * @throws Exception
     */
    public boolean validateNamesInResponse(List<TokenValues> actualValues, List<TokenValues> expectedValues) throws Exception {
        boolean allFound = true;
        for (TokenValues expValues : expectedValues) {
            if (!validateNameInResponse(actualValues, expValues)) {
                allFound = false;
            }
        }
        return allFound;
    }

    /**
     * Makes sure that when we find the matching entry (app_id) in actualValues, we then check to make sure that the expected name
     * matches the actual
     * name of that entry
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues-
     *            One TokenValues containing the values to validate (specifically app_id and name)
     * @return - returns true if the name is set correctly, false otherwise
     * @throws Exception
     */
    public boolean validateNameInResponse(List<TokenValues> actualValues, TokenValues expectedValues) throws Exception {
        // loop through actual and compare against expected - app_id must match first (we want to compare the name of the corresponding entry)
        TokenValues av = findTokenMatch(actualValues, expectedValues);
        if (av != null) {
            if (av.getName().equals(expectedValues.getName())) {
                return true;
            }
        }
        Log.info(thisClass, "validateNameInResponse", "Did not find correct name (" + expectedValues.getName() + ") for app_id: [" + expectedValues.getApp_id() + "] in response");
        return false;
    }

    /**
     * Makes sure that user is set properly for each matched app_id. Caller determines if not finding the expected value is a
     * problem.
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues
     *            - List of TokenValues that we will validate - (specifically need app_id and user for this check)
     * @return - returns true if all users are correct
     * @throws Exception
     */
    public boolean validateUsersInResponse(List<TokenValues> actualValues, List<TokenValues> expectedValues) throws Exception {
        boolean allFound = true;
        for (TokenValues expValues : expectedValues) {
            if (!validateUserInResponse(actualValues, expValues)) {
                allFound = false;
            }
        }
        return allFound;
    }

    /**
     * Makes sure that when we find the matching entry (app_id) in actualValues, we then check to make sure that the expected user
     * matches the actual
     * user of that entry
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues-
     *            One TokenValues containing the values to validate (specifically app_id and user)
     * @return - returns true if user is set correctly, false otherwise
     * @throws Exception
     */
    public boolean validateUserInResponse(List<TokenValues> actualValues, TokenValues expectedValues) throws Exception {
        // loop through actual and compare against expected - app_id must match first (we want to compare the user of the corresponding entry)
        TokenValues av = findTokenMatch(actualValues, expectedValues);
        if (av != null) {
            if (av.getUser().equals(expectedValues.getUser())) {
                return true;
            }
        }
        Log.info(thisClass, "validateUserInResponse", "Did not find correct user (" + expectedValues.getUser() + ") for app_id: [" + expectedValues.getApp_id() + "] in response");
        return false;
    }

    /**
     * Makes sure that created_at is set properly for each matched app_id. Caller determines if not finding the expected value is
     * a problem.
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues
     *            - List of TokenValues that we will validate - (specifically need app_id and created_at for this check)
     * @return - returns true if all created_at are correct
     * @throws Exception
     */
    public boolean validateCreatedAtsInResponse(List<TokenValues> actualValues, List<TokenValues> expectedValues) throws Exception {
        boolean allFound = true;
        for (TokenValues expValues : expectedValues) {
            if (!validateCreatedAtInResponse(actualValues, expValues)) {
                allFound = false;
            }
        }
        return allFound;
    }

    /**
     * Makes sure that when we find the matching entry (app_id) in actualValues, we then check to make sure that the actual
     * create_at matches the
     * expected created_at of that entry. ("match" using some fuzzy logic - makes sure that the actual created_at is after the
     * test start and before
     * the current time)
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues-
     *            One TokenValues containing the values to validate (specifically app_id and created_at)
     * @return - returns true if created_at is set correctly, false otherwise
     * @throws Exception
     */
    public boolean validateCreatedAtInResponse(List<TokenValues> actualValues, TokenValues expectedValues) throws Exception {
        // loop through actual and compare against expected - app_id must match first (unless we're doing a create and the expected app_id
        // won't exist)
        // (we'll want to check the created_at value of that entry only)
        TokenValues av = findTokenMatch(actualValues, expectedValues);
        if (av != null) {
            if (av.getCreated_at() == expectedValues.getCreated_at()) {
                return true;
            }
        }

        Log.info(thisClass, "validateCreatedAtInResponse", "Did not find correct created_at for app_id: [" + expectedValues.getApp_id() + "] in response");
        return false;
    }

    /**
     * Makes sure that expires_at is set properly for each matched app_id. Caller determines if not finding the expected value is
     * a problem.
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues
     *            - List of TokenValues that we will validate - (specifically need app_id and expires_at for this check)
     * @return - returns true if all expires_at are correct
     * @throws Exception
     */
    public boolean validateExpiresAtsInResponse(List<TokenValues> actualValues, List<TokenValues> expectedValues) throws Exception {
        boolean allFound = true;
        for (TokenValues expValues : expectedValues) {
            if (!validateExpiresAtInResponse(actualValues, expValues)) {
                allFound = false;
            }
        }
        return allFound;
    }

    /**
     * Makes sure that when we find the matching entry (app_id) in actualValues, we then check to make sure that the actual
     * expires_at matches the
     * expected expires_at of that entry. ("match" using some fuzzy logic - makes sure that expires_at // * after test start +
     * lifetime and less than
     * the current time + lifetime) is actual created_at + lifetime (created_at checked already)
     *
     * @param actualValues
     *            - List of TokenValues parsed from the response
     * @param expectedValues-
     *            One TokenValues containing the values to validate (specifically app_id and exires_at)
     * @return - returns true if created_at is set correctly, false otherwise
     * @throws Exception
     */
    public boolean validateExpiresAtInResponse(List<TokenValues> actualValues, TokenValues expectedValues) throws Exception {
        // loop through actual and compare against expected - app_id must match first (we'll want to check the expires_at value of that entry only)

        TokenValues av = findTokenMatch(actualValues, expectedValues);
        if (av != null) {
            if (av.getExpires_at() == expectedValues.getExpires_at()) {
                return true;
            }
        }
        Log.info(thisClass, "validateExpiresAtInResponse", "Did not find correct expires_at for app_id: [" + expectedValues.getApp_id() + "] in response");
        return false;

        //        for (TokenValues av : actualValues) {
        //            if (av.getApp_id().equals(expectedValues.getApp_id())) {
        //                Log.info(thisClass, "validateExpiresAtInResponse", av.getApp_id());
        //                // the expires at time should be between created_at+lifetime and now+lifetime (lifetime is passed in as the actual expires_at time)
        //                // expected expires_at can be either the lifetime or the actual expiration time - we need to match one or the other
        //                Log.info(thisClass, "validateExpiresAtInResponse", "Validating expires_at [" + av.getExpires_at() + "] equals [" + (av.getCreated_at() + expectedValues.getExpires_at()) + "]");
        //                if (av.getExpires_at() == (av.getCreated_at() + expectedValues.getExpires_at())) {
        //                    Log.info(thisClass, "validateExpiresAtInResponse", "matched with lifetime");
        //                    return true;
        //                }
        //                Log.info(thisClass, "validateExpiresAtInResponse", "Validating expires_at [" + av.getExpires_at() + "] equals [" + expectedValues.getExpires_at() + "]");
        //                if (av.getExpires_at() == expectedValues.getExpires_at()) {
        //                    Log.info(thisClass, "validateExpiresAtInResponse", "matched with actual time");
        //                    return true;
        //                }
        //                Log.info(thisClass, "validateExpiresAtInResponse", "if matched, should not be here");
        //                break; // we found the matching app_id, but the expires_at time was not valid
        //            }
        //        }
        //        Log.info(thisClass, "validateExpiresAtInResponse", "Did not find correct expires_at for app_id: [" + expectedValues.getApp_id() + "] in response");
        //        return false;
    }

    public TokenValues findTokenMatch(List<TokenValues> actualValues, TokenValues expectedValues) throws Exception {

        if (expectedValues.getApp_id() != null) {
            for (TokenValues av : actualValues) {
                if (expectedValues.getApp_id().equals(av.getApp_id())) {
                    return av;
                }
            }
        }
        return null;
    }

    public long getNow() {
        return new Instant().getMillis();
    }

    public TestSettings updateTestSettingsForAppPasswordsTests(TestSettings settings, String clientId, String clientSecret, String user, String password, String provider, String app) throws Exception {

        TestSettings updatedTestSettings = updateTestSettingsForAppEndpointTests(settings, clientId, clientSecret, user, password, provider);
        if (app != doNotOverrideApp) {
            updatedTestSettings.setRSProtectedResource(replaceLast(settings.getRSProtectedResource(), "helloworld_genAppPw01", app));
        }
        return updatedTestSettings;

    }

    public TestSettings updateTestSettingsForAppTokensTests(TestSettings settings, String clientId, String clientSecret, String user, String password, String provider, String app) throws Exception {

        TestSettings updatedTestSettings = updateTestSettingsForAppEndpointTests(settings, clientId, clientSecret, user, password, provider);
        if (app != doNotOverrideApp) {
            updatedTestSettings.setRSProtectedResource(replaceLast(settings.getRSProtectedResource(), "helloworld_genAppTok01", app));
        }

        return updatedTestSettings;
    }

    public TestSettings updateTestSettingsForAppEndpointTests(TestSettings settings, String clientId, String clientSecret, String user, String password, String provider) throws Exception {
        TestSettings updatedTestSettings = settings.copyTestSettings();
        updatedTestSettings.setClientID(clientId);
        updatedTestSettings.setClientName(clientId);
        updatedTestSettings.setClientSecret(clientSecret);
        updatedTestSettings.setAdminUser(user);
        updatedTestSettings.setAdminPswd(password);
        if (provider != doNotOverrideProvider) {
            updatedTestSettings.setAuthorizeEndpt(settings.getAuthorizeEndpt().replace(Constants.OIDCCONFIGSAMPLE_APP, provider));
            updatedTestSettings.setTokenEndpt(settings.getTokenEndpt().replace(Constants.OIDCCONFIGSAMPLE_APP, provider));
            updatedTestSettings.setUserinfoEndpt(settings.getUserinfoEndpt().replace(Constants.OIDCCONFIGSAMPLE_APP, provider));
            updatedTestSettings.setIntrospectionEndpt(settings.getIntrospectionEndpt().replace(Constants.OIDCCONFIGSAMPLE_APP, provider));
            updatedTestSettings.setAppPasswordEndpt(settings.getAppPasswordsEndpt().replace(Constants.OIDCCONFIGSAMPLE_APP, provider));
            updatedTestSettings.setAppTokenEndpt(settings.getAppTokensEndpt().replace(Constants.OIDCCONFIGSAMPLE_APP, provider));
        }
        return updatedTestSettings;
    }

    public static void setAppPasswordsDefaultsForTests() throws Exception {

        testSettings.setClientID("genAppPw01");
        testSettings.setClientName("genAppPw01");
        if (genericTestServer != null) { // some test classes only use an OP (don't invoke an app), so don't set the app url
            testSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_genAppPw01");
        }
    }

    public static void setAppTokensDefaultsForTests() throws Exception {

        testSettings.setClientID("genAppTok01");
        testSettings.setClientName("genAppTok01");
        if (genericTestServer != null) { // some test classes only use an OP (don't invoke an app), so don't set the app url
            testSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_genAppTok01");
        }
    }

    public String replaceLast(String fullString, String oldString, String newString) {
        int index = fullString.lastIndexOf(oldString);
        if (index == -1) {
            return fullString;
        }
        return fullString.substring(0, index) + newString + fullString.substring(index + oldString.length());

    }

    public static void fixJWKValidationEndpointsIfNeeded(String certType) throws Exception {

        String thisMethod = "fixJWKValidationEndpointsIfNeeded";
        msgUtils.printMethodName(thisMethod);
        // the JWK validation URL's are too complex to build in the xml
        HashMap<String, String> jwkMap = new HashMap<String, String>();
        jwkMap.put("oidcJWKValidationURL_baseOidcProviderCopy", baseOidcProviderCopy);
        jwkMap.put("oidcJWKValidationURL_shortLivedAccessToken", shortLivedAccessTokenProvider);
        jwkMap.put("oidcJWKValidationURL_appPwExchangeNotAllowed", appPwExchangeNotAllowedProvider);
        jwkMap.put("oidcJWKValidationURL_userClientTokenLimit", userClientTokenLimitProvider);
        jwkMap.put("oidcJWKValidationURL_shortLivedAppPassword", shortLivedAppPasswordProvider);
        jwkMap.put("oidcJWKValidationURL_appPasswordAppTokenLimit", appPasswordLimitProvider);

        setJWKValidationMap(jwkMap);
    }

    /********************** Expectations utilities *********************/

    /**
     * Get expectations for successful invocation of the app-password endpoint (for create)
     *
     * @param settings
     *            - the current test settings
     * @return - the generated expectations
     * @throws Exception
     */
    public List<validationData> getGoodCreateAppPasswordsExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(new String[] { Constants.INVOKE_APP_PASSWORDS_ENDPOINT_CREATE });
        expectations = vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_CREATE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.APP_PASSWORD_KEY, null, Constants.APP_PASSWORD_KEY);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_CREATE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.APP_ID_KEY, null, Constants.APP_ID_KEY);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_CREATE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.CREATED_AT_KEY, null, Constants.CREATED_AT_KEY);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_CREATE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.EXPIRES_AT_KEY, null, Constants.EXPIRES_AT_KEY);

        return expectations;

    }

    protected List<validationData> getGoodListAppPasswordsExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(new String[] { Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST });

        return expectations;

    }

    /**
     * Adds expectations that ensure successful app-password list response expectations
     */
    protected List<validationData> getSuccessfulListAppPasswordExpectations(TestSettings settings, String userNameParameterValue) throws Exception {
        List<validationData> listExpectations = getGoodListAppPasswordsExpectations(settings);
        vData.addExpectation(listExpectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find an apparent key for app passwords in the response but should have.", null, "\"app-passwords\":");
        // Ensure the user_id attribute either does or does not appear in the response URL
        if (userNameParameterValue == null) {
            vData.addExpectation(listExpectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST, Constants.RESPONSE_URL, Constants.STRING_DOES_NOT_CONTAIN, "Found user_id parameter in the response URL but should not have.", null, "user_id");
        } else {
            String encodedUserName = URLEncoder.encode(userNameParameterValue, "UTF-8");
            vData.addExpectation(listExpectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not find the expected user_id parameter and value in the response URL.", null, "user_id=" + encodedUserName);
        }
        return listExpectations;
    }

    /**
     * Adds expectations that ensure the response contains no app-passwords
     */
    protected void addResponseDoesNotContainAppPasswordsExpectation(List<validationData> expectations, String userName) throws Exception {
        vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Should not have found any app passwords in the response for user [" + userName + "], but did.", null, "\"app-passwords\":[]");
    }

    /**
     * Adds expectations that ensure the response does contain app-passwords rather than an empty array
     */
    protected void addResponseContainsAppPasswordsExpectation(List<validationData> expectations, String userName) throws Exception {
        vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found empty array of app passwords for user [" + userName + "] in web response but should not have.", null, "\"app-passwords\":[]");
    }

    protected List<validationData> getGoodRevokeAppPasswordsExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(new String[] { Constants.INVOKE_APP_PASSWORDS_ENDPOINT_REVOKE });
        expectations = vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_REVOKE, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not receive the OK message", null, Constants.OK_MESSAGE);

        return expectations;

    }

    /**
     * Get expectations for successful invocation of the app-token endpoint (for create)
     *
     * @param settings
     *            - the current test settings
     * @return - the generated expectations
     * @throws Exception
     */
    protected List<validationData> getGoodCreateAppTokensExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(new String[] { Constants.INVOKE_APP_TOKENS_ENDPOINT_CREATE });
        expectations = vData.addExpectation(expectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_CREATE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.APP_TOKEN_KEY, null, Constants.APP_TOKEN_KEY);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_CREATE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.APP_ID_KEY, null, Constants.APP_ID_KEY);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_CREATE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.CREATED_AT_KEY, null, Constants.CREATED_AT_KEY);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_CREATE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.EXPIRES_AT_KEY, null, Constants.EXPIRES_AT_KEY);

        return expectations;

    }

    protected List<validationData> getGoodListAppTokensExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(new String[] { Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST });

        return expectations;

    }

    /**
     * Adds expectations that ensure successful app-tokens list response expectations
     */
    protected List<validationData> getSuccessfulListAppTokensExpectations(TestSettings settings, String userNameParameterValue) throws Exception {
        List<validationData> listExpectations = getGoodListAppTokensExpectations(settings);
        vData.addExpectation(listExpectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find an apparent key for app tokens in the response but should have.", null, "\"app-tokens\":");
        // Ensure the user_id attribute either does or does not appear in the response URL
        if (userNameParameterValue == null) {
            vData.addExpectation(listExpectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST, Constants.RESPONSE_URL, Constants.STRING_DOES_NOT_CONTAIN, "Found user_id parameter in the response URL but should not have.", null, "user_id");
        } else {
            String encodedUserName = URLEncoder.encode(userNameParameterValue, "UTF-8");
            vData.addExpectation(listExpectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not find the expected user_id parameter and value in the response URL.", null, "user_id=" + encodedUserName);
        }
        return listExpectations;
    }

    /**
     * Adds expectations that ensure the response contains no app-tokens
     */
    protected void addResponseDoesNotContainAppTokensExpectation(List<validationData> expectations, String userName) throws Exception {
        vData.addExpectation(expectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Should not have found any app tokens in the response for user [" + userName + "], but did.", null, "\"app-tokens\":[]");
    }

    /**
     * Adds expectations that ensure the response does contain app-tokens
     */
    protected void addResponseContainsAppTokensExpectation(List<validationData> expectations, String userName) throws Exception {
        vData.addExpectation(expectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found empty array of app tokens for user [" + userName + "] in web response but should not have.", null, "\"app-tokens\":[]");
    }

    protected List<validationData> getGoodRevokeAppTokensExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(new String[] { Constants.INVOKE_APP_TOKENS_ENDPOINT_REVOKE });

        return expectations;

    }

    /**
     * Get expectations for successful invocation of the token endpoint
     *
     * @param settings
     *            - the current test settings
     * @return - the generated expectations
     * @throws Exception
     */
    public List<validationData> getGoodTokenEndpointExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(new String[] { Constants.INVOKE_TOKEN_ENDPOINT });
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.ACCESS_TOKEN_KEY, null, Constants.ACCESS_TOKEN_KEY);

        return expectations;

    }

    protected List<validationData> getGoodTokenEndpointWithWarningExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = getGoodTokenEndpointExpectations(settings);
        //        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find CWOAU0076W message in OP log saying the OAuth client is not configured to allow app passwords.", MessageConstants.CWOAU0076W_CLIENT_DISALLOWS_APP_PASSWORDS);
        // sometimes get this, sometimes don't - it's just a warning about a config that we're setting up on purpose.
        testOPServer.addIgnoredServerException(MessageConstants.CWOAU0076W_CLIENT_DISALLOWS_APP_PASSWORDS);
        return expectations;
    }

    /**
     * Add expectations for the invocation of the protected app to the set of already existing expectations
     *
     * @param expectations
     *            - existing expectations
     * @param settings
     *            - the current test settings
     * @return - the generated expectations
     * @throws Exception
     */
    protected List<validationData> addRSProtectedAppExpectations(TestSettings settings, String access_token) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not get to the Helloworld Url", null, Constants.HELLOWORLD_PROTECTED_RESOURCE);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the Helloworld App", null, Constants.HELLOWORLD_MSG);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did NOT match the user: " + settings.getUserName(), null, "WSPrincipal:" + settings.getAdminUser());
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "The access_token is missing in the app output", null, "access_token=" + access_token);

        return expectations;

    }

    /**
     * Add expectations for the invocation of the protected app to the set of already existing expectations
     *
     * @param expectations
     *            - existing expectations
     * @param settings
     *            - the current test settings
     * @return - the generated expectations
     * @throws Exception
     */
    protected List<validationData> addRSProtectedApp401Expectations(String failingAction, TestSettings settings) throws Exception {

        List<validationData> expectations = addGeneralRSProtectedApp401Expectations();

        if (genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
            String tokenerrmsg = TestSettings.StoreType.LOCAL.equals(settings.getStoreType()) ? MessageConstants.CWWKS1622E_USERINFO_MISSING_ACCESS_TOKEN : MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN;
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", tokenerrmsg);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", tokenerrmsg);
        } else {
            if (!TestSettings.StoreType.LOCAL.equals(settings.getStoreType())) {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            }
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a message that the OIDC client failed the request", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
        }
        return expectations;

    }

    /**
     * With app-tokens, when the validation method is userinfo, usedBy will NOT be enforced.
     *
     * @param settings
     *            - the current test settings
     * @param app_token
     *            - the access_token that the test used to access the protected app
     * @return - the generated expectations
     * @throws Exception
     */
    protected List<validationData> addClientMisMatchWithAppTokens(TestSettings settings, String app_token) throws Exception {
        List<validationData> expectations = null;
        if (genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
            expectations = addRSProtectedAppExpectations(settings, app_token);
        } else {
            expectations = addGeneralRSProtectedApp401Expectations();
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE + " error message.", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
        }
        return expectations;
    }

    /**
     * Create general expectations for cases where 401 status is expected - used to validate older command responses.
     * app-password/app-token 401's are a little cleaner
     *
     * @return
     * @throws Exception
     */
    protected List<validationData> addGeneralRSProtectedApp401Expectations() throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.UNAUTHORIZED_STATUS);

        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not receive a message that the OIDC client failed the request", null, ".*" + Constants.ERROR_RESPONSE_PARM + ".*" + Constants.UNAUTHORIZED_STATUS + ".*");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_MESSAGE, Constants.STRING_MATCHES, "Did not receive unauthorized message", null, Constants.UNAUTHORIZED_MESSAGE);

        return expectations;
    }

    protected List<validationData> getInvalidResourceOwnerExpectations(String failingAction) throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, failingAction, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.ERROR_CODE_INVALID_CLIENT + " message.", null, "\"error\":\"" + Constants.ERROR_CODE_INVALID_CLIENT + "\"");
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWOAU0074E_CANNOT_COMPLETE_PASSWORD_EXCHANGE + " error message.", MessageConstants.CWOAU0074E_CANNOT_COMPLETE_PASSWORD_EXCHANGE);

        return expectations;
    }

    protected List<validationData> getInvalidClientExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getInvalidResourceOwnerExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWOAU0077E_CLIENT_MISMATCH + " error message.", MessageConstants.CWOAU0077E_CLIENT_MISMATCH);

        return expectations;
    }

    /**
     * Add expectations for:
     * Response (StatusCode): 400
     * Response (Message): Bad Request
     * Response (Full): {"error":"invalid_request"}
     *
     * @param failingAction
     *            - the step where the failure should occur
     * @return
     * @throws Exception
     */
    protected List<validationData> getBaseBadRequestExpectations(String failingAction) throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, failingAction, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not receive Bad Request in the message.", null, Constants.BAD_REQUEST);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.ERROR_CODE_INVALID_REQUEST + " message.", null, "\"error\":\"" + Constants.ERROR_CODE_INVALID_REQUEST + "\"");
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an apparent key for app passwords in the response but should not have.", null, "\"app-passwords\":");
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an apparent key for app tokens in the response but should not have.", null, "\"app-tokens\":");

        return expectations;
    }

    /**
     * Add expectations for:
     * Response (StatusCode): 401
     * Response (Message): Unauthorized
     * Response (Full): {"error":"invalid_client"}
     *
     * @param failingAction
     *            - the step where the failure should occur
     * @return
     * @throws Exception
     */
    protected List<validationData> getBaseUnauthorizedExpectations(String failingAction) throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, failingAction, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_MESSAGE, Constants.STRING_MATCHES, "Did not receive unauthorized message", null, Constants.UNAUTHORIZED_MESSAGE);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.ERROR_CODE_INVALID_CLIENT + " message.", null, "\"error\":\"" + Constants.ERROR_CODE_INVALID_CLIENT + "\"");
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an apparent key for app passwords in the response but should not have.", null, "\"app-passwords\":");
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an apparent key for app tokens in the response but should not have.", null, "\"app-tokens\":");

        return expectations;
    }

    protected List<validationData> getMissingAccessTokenInRequestExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseBadRequestExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a " + MessageConstants.CWWKS1490E_ACCESS_TOKEN_MISSING + " error message in the log.", MessageConstants.CWWKS1490E_ACCESS_TOKEN_MISSING);

        return expectations;
    }

    protected List<validationData> getBadAccessTokenInRequestExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseBadRequestExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive a " + MessageConstants.CWWKS1489E_ACCESS_TOKEN_BAD + " error message in the log.", MessageConstants.CWWKS1489E_ACCESS_TOKEN_BAD);

        return expectations;
    }

    protected List<validationData> getMissingAppNameInRequestExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseBadRequestExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1435E_MISSING_APP_NAME + " error message.", MessageConstants.CWWKS1435E_MISSING_APP_NAME);

        return expectations;
    }

    protected List<validationData> getEmptyClientInRequestExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseUnauthorizedExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1484E_OAUTH_INVALID_REQUEST_NO_ID_SECRET + " error message.", MessageConstants.CWWKS1484E_OAUTH_INVALID_REQUEST_NO_ID_SECRET);
        return expectations;
    }

    protected List<validationData> getBadClientInRequestExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseUnauthorizedExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1485E_CLIENT_AUTH_FAILED + " error message.", MessageConstants.CWWKS1485E_CLIENT_AUTH_FAILED);
        return expectations;
    }

    protected List<validationData> getExceededCreateMaxExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseBadRequestExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1487E_EXCEEDED_MAX + " error message in the log.", MessageConstants.CWWKS1487E_EXCEEDED_MAX);

        return expectations;
    }

    protected List<validationData> getInvalidAppNameExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseBadRequestExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1482E_APP_NAME_ALREADY_USED + " error message in the log.", MessageConstants.CWWKS1482E_APP_NAME_ALREADY_USED);

        return expectations;
    }

    protected List<validationData> getNoPermissionForAttributeInRequestExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseBadRequestExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1483E_NO_PERMISSION_ON_ATTRIBUTE + " error message.", MessageConstants.CWWKS1483E_NO_PERMISSION_ON_ATTRIBUTE);

        return expectations;
    }

    protected List<validationData> getUsedByLengthExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseBadRequestExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1488E_USED_BY_TOO_LONG + " error message in the log.", MessageConstants.CWWKS1488E_USED_BY_TOO_LONG);

        return expectations;
    }

    protected List<validationData> getMisMatchedClientExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseBadRequestExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1492E_CLIENT_MISMATCH + " error message in the log.", MessageConstants.CWWKS1492E_CLIENT_MISMATCH);

        return expectations;
    }

    protected List<validationData> getMissingBasicAuthenticationCredentials(String failingAction) throws Exception {

        List<validationData> expectations = getBaseUnauthorizedExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1493E_BASIC_AUTH_MISSING_IN_HEADER + " error message.", MessageConstants.CWWKS1493E_BASIC_AUTH_MISSING_IN_HEADER);

        return expectations;
    }

    protected List<validationData> getExceedUserClientTokenLimitExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseBadRequestExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWOAU0054E_EXCEEDED_USER_CLIENT_TOKEN_LIMIT + " error message in the log.", MessageConstants.CWOAU0054E_EXCEEDED_USER_CLIENT_TOKEN_LIMIT);

        return expectations;
    }

    protected List<validationData> getClientIdClientSecretMustBeInHeaderExpectations(String failingAction) throws Exception {

        List<validationData> expectations = getBaseUnauthorizedExpectations(failingAction);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1491E_CLIENT_AUTH_MUST_BE_IN_HEADER + " error message in the log.", MessageConstants.CWWKS1491E_CLIENT_AUTH_MUST_BE_IN_HEADER);

        return expectations;
    }

    // method expects a different error code than baseBadRequest checks
    protected List<validationData> getAppPasswordAppTokenNotAllowedExpectations(String failingAction, TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, failingAction, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not receive Bad Request in the message.", null, Constants.BAD_REQUEST);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.ERROR_CODE_UNAUTHORIZED_CLIENT + " error message.", null, "\"error\":\"" + Constants.ERROR_CODE_UNAUTHORIZED_CLIENT + "\"");
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1486E_CLIENT_NOT_AUTH_FOR_APP_PWD_OR_APP_TOKEN + " error message.", MessageConstants.CWWKS1486E_CLIENT_NOT_AUTH_FOR_APP_PWD_OR_APP_TOKEN);

        return expectations;
    }

    /**
     * Add expectations for passwordGrantRequiresAppPassword false tests
     *
     * @param failingAction
     * @param settings
     * @return
     * @throws Exception
     */
    protected List<validationData> getPasswordGrantRequiresAppPasswordFalseExpectations(String failingAction) throws Exception {

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_TOKEN_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not receive an " + Constants.BAD_REQUEST + " error message.", null, Constants.BAD_REQUEST);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.ERROR_RESOURCE_OWNER_BAD_CREDS + " error message.", null, Constants.ERROR_RESOURCE_OWNER_BAD_CREDS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + Constants.ERROR_SERVER_ERROR + " error message.", null, Constants.ERROR_SERVER_ERROR);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWIML4537E_PRINCIPAL_NOT_FOUND + " error message.", MessageConstants.CWIML4537E_PRINCIPAL_NOT_FOUND);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWOAU0069E_INVALID_RESOURCE_OWNER + " error message.", MessageConstants.CWOAU0069E_INVALID_RESOURCE_OWNER);

        return expectations;
    }

    protected List<validationData> getExpiredAppTokenExpectations(String failingAction, TestSettings settings) throws Exception {
        TestSettings updatedSettings = settings.copyTestSettings();
        updatedSettings.setRsTokenType(Constants.ACCESS_TOKEN_KEY);
        return getExpiredAccessTokenExpectations(failingAction, updatedSettings);

    }

    protected List<validationData> getHttpsRequiredExpectations(String failingAction) throws Exception {
        List<validationData> expectations = vData.addResponseStatusExpectation(null, failingAction, Constants.NOT_FOUND_STATUS);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWOAU0073E_FRONT_END_ERROR + " message.", null, MessageConstants.CWOAU0073E_FRONT_END_ERROR);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWOAU0037E_HTTPS_REQUIRED + " error message.", MessageConstants.CWOAU0037E_HTTPS_REQUIRED);

        return expectations;
    }

    protected List<validationData> getExpiredAccessTokenExpectations(String failingAction, TestSettings settings) throws Exception {

        //        Log.info(thisClass, "getExpiredAccessTokenExpectations", "Token Type: " + settings.getRsTokenType());
        List<validationData> expectations = vData.addResponseStatusExpectation(null, failingAction, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_MESSAGE, Constants.STRING_CONTAINS, "Did not get unauthorized message", null, Constants.UNAUTHORIZED_MESSAGE);
        expectations = vData.addExpectation(expectations, failingAction, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS, "Did not get " + Constants.INVALID_TOKEN_ERROR + " in the header", null, Constants.INVALID_TOKEN_ERROR);
        if (settings.getRsTokenType() == Constants.ACCESS_TOKEN_KEY) {
            if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID + " error message.", MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            } else {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN + " error message.", MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID + " error message.", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID);
            }
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1773E_TOKEN_EXPIRED + " error message.", MessageConstants.CWWKS1773E_TOKEN_EXPIRED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE + " error message.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        }

        if (settings.getRsTokenType() == Constants.ACCESS_TOKEN_KEY) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, failingAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not receive an " + MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE + " error message.", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
        }
        //        msgUtils.printOAuthOidcExpectations(expectations, new String[] { failingAction });
        return expectations;
    }

    public static void restoreDerbyForAppPasswords(TestSettings settings) throws Exception {
        initDerbyForAppPassword(settings);
    }

    public static void initDerbyForAppPassword(TestSettings settings) throws Exception {

        String thisMethod = "initDerbyForAppPassword";
        msgUtils.printMethodName(thisMethod);

        ClientRegistrationTools crTools = new ClientRegistrationTools();
        List<endpointSettings> headers = crTools.setRequestHeaders(settings);

        //setRegistrationBody(TestServer opServer, String clientId, String clientSecret, String clientName,
        //                boolean appPasswordAllowed, boolean appTokenAllowed, String scope, String preauthScope, List<String> grants, List<String> rspTypes, boolean introspect )

        // set the client values
        JSONObject clientData_genAppPw01 = crTools.setRegistrationBody(testOPServer, "genAppPw01", "secret", "genAppPw01", true, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        JSONObject clientData_genAppPw02 = crTools.setRegistrationBody(testOPServer, "genAppPw02", "secret", "genAppPw02", true, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        JSONObject clientData_genAppPwAndAppToken = crTools.setRegistrationBody(testOPServer, "genAppPwAndAppToken", "secret", "genAppPwAndAppToken", true, true, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        JSONObject clientData_NoAppPw01 = crTools.setRegistrationBody(testOPServer, "NoAppPw01", "secret", "NoAppPw01", false, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        JSONObject clientData_NoAppPw02 = crTools.setRegistrationBody(testOPServer, "NoAppPw02", "secret", "NoAppPw02", false, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        // create all of the clients (associating them with the provider (first parm)
        crTools.createClientEntries(testSettings.getRegistrationEndpt(), headers, Arrays.asList(clientData_genAppPw01, clientData_genAppPw02, clientData_genAppPwAndAppToken, clientData_NoAppPw01, clientData_NoAppPw02));
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_copy"), headers, Arrays.asList(clientData_genAppPw01, clientData_genAppPw02, clientData_genAppPwAndAppToken, clientData_NoAppPw01, clientData_NoAppPw02));

        JSONObject clientData_genShortLivedAccessToken = crTools.setRegistrationBody(testOPServer, "genShortLivedAccessToken", "secret", "genShortLivedAccessToken", true, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_shortLivedAccessToken"), headers, Arrays.asList(clientData_genShortLivedAccessToken));

        JSONObject clientData_appPwExchangeNotAllowed = crTools.setRegistrationBody(testOPServer, "appPwExchangeNotAllowed", "secret", "appPwExchangeNotAllowed", true, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_appPwExchangeNotAllowed"), headers, Arrays.asList(clientData_appPwExchangeNotAllowed));

        JSONObject clientData_userClientTokenLimit1 = crTools.setRegistrationBody(testOPServer, "userClientTokenLimit1", "secret", "userClientTokenLimit1", true, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        JSONObject clientData_userClientTokenLimit2 = crTools.setRegistrationBody(testOPServer, "userClientTokenLimit2", "secret", "userClientTokenLimit2", true, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_userClientTokenLimit"), headers, Arrays.asList(clientData_userClientTokenLimit1, clientData_userClientTokenLimit2));

        JSONObject clientData_genShortLivedAppPw = crTools.setRegistrationBody(testOPServer, "genShortLivedAppPw", "secret", "genShortLivedAppPw", true, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_shortLivedAppPw"), headers, Arrays.asList(clientData_genShortLivedAppPw));

        JSONObject clientData_genAppPwLimitedNumAllowed = crTools.setRegistrationBody(testOPServer, "genAppPwLimitedNumAllowed", "secret", "genAppPwLimitedNumAllowed", true, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_maxAppPwAllowed"), headers, Arrays.asList(clientData_genAppPwLimitedNumAllowed));

        JSONObject clientData_missingPasswordGrantType = crTools.setRegistrationBody(testOPServer, "missingPasswordGrantType", "secret", "missingPasswordGrantType", true, false, "openid scope1 scope2", "openid scope1 scope2", Arrays.asList("authorization_code", "implicit", "refresh_token", "client_credentials", "urn:ietf:params:oauth:grant-type:jwt-bearer"), rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_missingPasswordGrantType"), headers, Arrays.asList(clientData_missingPasswordGrantType));

    }

    public static void initDerbyForAppToken(TestSettings settings) throws Exception {

        String thisMethod = "initDerbyForAppToken";
        msgUtils.printMethodName(thisMethod);

        ClientRegistrationTools crTools = new ClientRegistrationTools();
        List<endpointSettings> headers = crTools.setRequestHeaders(settings);

        //setRegistrationBody(TestServer opServer, String clientId, String clientSecret, String clientName,
        //                boolean appPasswordAllowed, boolean appTokenAllowed, String scope, String preauthScope, List<String> grants, List<String> rspTypes, boolean introspect )

        // set the client values
        JSONObject clientData_genAppTok01 = crTools.setRegistrationBody(testOPServer, "genAppTok01", "secret", "genAppTok01", false, true, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        JSONObject clientData_genAppTok02 = crTools.setRegistrationBody(testOPServer, "genAppTok02", "secret", "genAppTok02", false, true, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        JSONObject clientData_genAppPwAndAppToken = crTools.setRegistrationBody(testOPServer, "genAppPwAndAppToken", "secret", "genAppPwAndAppToken", true, true, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        JSONObject clientData_NoAppTok01 = crTools.setRegistrationBody(testOPServer, "NoAppTok01", "secret", "NoAppTok01", false, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        JSONObject clientData_NoAppTok02 = crTools.setRegistrationBody(testOPServer, "NoAppTok02", "secret", "NoAppTok02", false, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        // create all of the clients (associating them with the provider (first parm)
        crTools.createClientEntries(testSettings.getRegistrationEndpt(), headers, Arrays.asList(clientData_genAppTok01, clientData_genAppTok02, clientData_genAppPwAndAppToken, clientData_NoAppTok01, clientData_NoAppTok02));
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_copy"), headers, Arrays.asList(clientData_genAppTok01, clientData_genAppTok02, clientData_genAppPwAndAppToken, clientData_NoAppTok01, clientData_NoAppTok02));

        JSONObject clientData_genShortLivedAccessToken = crTools.setRegistrationBody(testOPServer, "genShortLivedAccessToken", "secret", "genShortLivedAccessToken", false, true, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_shortLivedAccessToken"), headers, Arrays.asList(clientData_genShortLivedAccessToken));

        //        JSONObject clientData20 = crTools.setRegistrationBody(testOPServer, "appPwExchangeNotAllowed", "secret", "appPwExchangeNotAllowed", true, false, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        //        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_appPwExchangeNotAllowed"), headers, Arrays.asList(clientData20));

        JSONObject clientData_userClientTokenLimit1 = crTools.setRegistrationBody(testOPServer, "userClientTokenLimit1", "secret", "userClientTokenLimit1", false, true, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        JSONObject clientData_userClientTokenLimit2 = crTools.setRegistrationBody(testOPServer, "userClientTokenLimit2", "secret", "userClientTokenLimit2", false, true, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_userClientTokenLimit"), headers, Arrays.asList(clientData_userClientTokenLimit1, clientData_userClientTokenLimit2));

        JSONObject clientData_genShortLivedAppTok = crTools.setRegistrationBody(testOPServer, "genShortLivedAppTok", "secret", "genShortLivedAppTok", false, true, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_shortLivedAppTok"), headers, Arrays.asList(clientData_genShortLivedAppTok));

        JSONObject clientData_genAppTokLimitedNumAllowed = crTools.setRegistrationBody(testOPServer, "genAppTokLimitedNumAllowed", "secret", "genAppTokLimitedNumAllowed", false, true, "openid scope1 scope2", "openid scope1 scope2", allGrantTypes, rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_maxAppTokAllowed"), headers, Arrays.asList(clientData_genAppTokLimitedNumAllowed));

        JSONObject clientData_missingPasswordGrantType = crTools.setRegistrationBody(testOPServer, "missingPasswordGrantType", "secret", "missingPasswordGrantType", false, true, "openid scope1 scope2", "openid scope1 scope2", Arrays.asList("authorization_code", "implicit", "refresh_token", "client_credentials", "urn:ietf:params:oauth:grant-type:jwt-bearer"), rspTypes, true);
        crTools.createClientEntries(testSettings.getRegistrationEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_missingPasswordGrantType"), headers, Arrays.asList(clientData_missingPasswordGrantType));

    }

}
