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
package com.ibm.ws.security.oauth_oidc.fat.commonTest.app.passwords_tokens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.BeforeClass;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AppPasswordsAndTokensCommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

public abstract class ListAppPasswordsOrTokensTests extends AppPasswordsAndTokensCommonTest {

    private static final Class<?> thisClass = ListAppPasswordsOrTokensTests.class;

    protected static final List<String> NORMAL_USER_LIST = Arrays.asList(new String[] { "testuser", "user1", "user2", "user3" });
    protected static final String CACHED_DATA_KEY_DELIMITER = ":";

    protected final boolean DO_NOT_GET_TOKEN_FROM_CACHE = false;

    protected static Map<String, AuthenticatedUserData> cachedUserData = new HashMap<String, AuthenticatedUserData>();
    protected static String tokenManagerAccessToken = null;

    @BeforeClass
    public static void beforeListTestClass() {
        resetStaticVariables();
    }

    @Before
    public void preTestSetup() throws Exception {
        String method = "preTestSetup";
        msgUtils.printMethodName(method + " (Start)");

        createTokenManagerAccessToken();

        msgUtils.printMethodName(method + " (End)");
    }

    protected static void resetStaticVariables() {
        tokenManagerAccessToken = null;
        cachedUserData = new HashMap<String, AuthenticatedUserData>();
    }

    protected void createTokenManagerAccessToken() throws Exception {
        if (tokenManagerAccessToken == null) {
            TestSettings tokenManagerTestSettings = getUpdatedTestSettingsForTokenManager();
            tokenManagerAccessToken = getAccessTokenAndCacheUserData(tokenManagerTestSettings);
        }
    }

    /**
     * Class used for keeping track of a single user's authentication and app password or app token data for a specific provider
     * and client.
     */
    public class AuthenticatedUserData {
        public String username;
        public String provider;
        public String clientId;
        public String password;
        public String clientSecret;

        public List<String> accessTokens = new ArrayList<String>();
        public List<TokenValues> appPasswords = new ArrayList<TokenValues>();
        public List<TokenValues> appTokens = new ArrayList<TokenValues>();

        public AuthenticatedUserData(TestSettings settings) {
            username = settings.getAdminUser();
            provider = extractOAuthProviderName(settings);
            clientId = settings.getClientID();
            // These are important to have but aren't needed to uniquely identify an entry
            password = settings.getAdminPswd();
            clientSecret = settings.getClientSecret();
        }

        public void addAccessToken(String accessToken) {
            if (accessToken != null) {
                accessTokens.add(accessToken);
            }
        }

        public String getLatestAccessToken() {
            return accessTokens.get(accessTokens.size() - 1);
        }

        public void addAppPasswords(List<TokenValues> appPasswords) {
            if (appPasswords != null) {
                this.appPasswords.addAll(appPasswords);
            }
        }

        public void addAppTokens(List<TokenValues> appTokens) {
            if (appTokens != null) {
                this.appTokens.addAll(appTokens);
            }
        }

        public void resetAppPasswords() {
            appPasswords = new ArrayList<TokenValues>();
        }

        public void resetAppTokens() {
            appTokens = new ArrayList<TokenValues>();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof AuthenticatedUserData)) {
                return false;
            }
            AuthenticatedUserData compareObject = (AuthenticatedUserData) o;
            return (Objects.equals(username, compareObject.username) && Objects.equals(provider, compareObject.provider) && Objects.equals(clientId, compareObject.clientId));
        }

        @Override
        public int hashCode() {
            return Objects.hash(username, provider, clientId);
        }
    }

    protected String extractOAuthProviderName(TestSettings settings) {
        String method = "extractOAuthProviderName";
        String authorizationEndpoint = settings.getAuthorizeEndpt();
        Pattern providerRegex = Pattern.compile(".+/([^/]+)/" + Constants.AUTHORIZE_ENDPOINT);
        Matcher matcher = providerRegex.matcher(authorizationEndpoint);
        if (!matcher.matches()) {
            Log.info(thisClass, method, "Failed to extract OAuth provider name from authorization endpoint [" + authorizationEndpoint + "]");
            return null;
        }
        return matcher.group(1);
    }

    protected String getCachedUserDataKey(TestSettings settings) {
        return settings.getAdminUser() + CACHED_DATA_KEY_DELIMITER + extractOAuthProviderName(settings) + CACHED_DATA_KEY_DELIMITER + settings.getClientID();
    }

    /**
     * Gets an access token that's been cached by the test class or obtains a new access token if a cached token is not found.
     */
    protected String getAccessTokenAndCacheUserData(TestSettings settings) throws Exception {
        return getAccessTokenAndCacheUserData(settings, true);
    }

    protected String getAccessTokenAndCacheUserData(TestSettings settings, boolean getTokenFromCacheIfAvailable) throws Exception {
        String method = "getAccessTokenAndCacheUserData";

        String mapKey = getCachedUserDataKey(settings);

        Log.info(thisClass, method, "Checking for a cached access token for user + provider + client: [" + mapKey + "]...");
        AuthenticatedUserData userData = cachedUserData.get(mapKey);

        if (isNewAccessTokenNeeded(getTokenFromCacheIfAvailable, userData)) {
            Log.info(thisClass, method, "Obtaining a new access token for user + provider + client: [" + mapKey + "]...");
            String accessToken = getAccessToken(settings);

            if (userData == null) {
                userData = new AuthenticatedUserData(settings);
            }
            userData.addAccessToken(accessToken);

            cachedUserData.put(mapKey, userData);
            Log.info(thisClass, method, "New access token for user + provider + client: [" + mapKey + "]: [" + accessToken + "]");
        }
        return userData.getLatestAccessToken();
    }

    protected boolean isNewAccessTokenNeeded(boolean getTokenFromCacheIfAvailable, AuthenticatedUserData userData) {
        return (!getTokenFromCacheIfAvailable) || (userData == null || userData.accessTokens == null || userData.accessTokens.isEmpty());
    }

    protected TestSettings getUpdatedTestSettingsForTokenManager() {
        TestSettings tokenManagerTestSettings = testSettings.copyTestSettings();
        tokenManagerTestSettings.setAdminUser(Constants.OIDC_TOKEN_MANAGER_USER);
        tokenManagerTestSettings.setAdminPswd(Constants.OIDC_TOKEN_MANAGER_PWD);
        return tokenManagerTestSettings;
    }

    /**
     * Creates a few app passwords or tokens each for a set of normal users.
     */
    protected Map<String, AuthenticatedUserData> createAppCredentialsForMultipleUsers() throws Exception {
        Map<String, AuthenticatedUserData> userData = new HashMap<String, AuthenticatedUserData>();
        for (String username : NORMAL_USER_LIST) {
            TestSettings updatedTestSettings = testSettings.copyTestSettings();
            String password = username + "pwd";
            updatedTestSettings.setAdminUser(username);
            updatedTestSettings.setAdminPswd(password);

            String accessToken = getAccessTokenAndCacheUserData(updatedTestSettings);

            List<TokenValues> appTokens = createAndValidateAppCredentialsFromSameAccessToken(updatedTestSettings, username, accessToken);

            AuthenticatedUserData alreadyCachedUserData = cachedUserData.get(getCachedUserDataKey(updatedTestSettings));
            if (alreadyCachedUserData == null) {
                alreadyCachedUserData = new AuthenticatedUserData(updatedTestSettings);
                alreadyCachedUserData.addAccessToken(accessToken);
            }
            addCredentialsToAuthenticatedUserData(appTokens, alreadyCachedUserData);

            userData.put(username, alreadyCachedUserData);
            cachedUserData.put(getCachedUserDataKey(updatedTestSettings), alreadyCachedUserData);
        }
        return userData;
    }

    /**
     * Revoke app-passwords for all users when the client allows processing of app-passwords
     */
    protected void revokeAllAppPasswords() throws Exception {
        String method = "revokeAllAppPasswords";
        for (Entry<String, AuthenticatedUserData> entry : cachedUserData.entrySet()) {
            AuthenticatedUserData userData = entry.getValue();
            if (userData.accessTokens == null || userData.accessTokens.isEmpty()) {
                continue;
            }
            if (isClientThatShouldNotAttemptPasswordRevoke(userData.provider, userData.clientId)) {
                continue;
            }
            TestSettings settings = updateTestSettingsForAppPasswordsTests(testSettings, userData.clientId, userData.clientSecret, userData.username, userData.password, userData.provider, doNotOverrideApp);
            Log.info(getClass(), method, "Revoking app passwords for user + provider + client: [" + entry.getKey() + "]...");
            revokeAndValidateAppPasswords(null, settings, userData.getLatestAccessToken(), userNotSetOrUnknown, appIdNotSetOrUnknown);

            userData.resetAppPasswords();
        }
    }

    private boolean isClientThatShouldNotAttemptPasswordRevoke(String provider, String clientId) {
        return "NoAppPw01".equals(clientId) || "NoAppPw02".equals(clientId) || "genAppPwAndAppToken".equals(clientId) || "genShortLivedAccessToken".equals(clientId);
    }

    /**
     * Creates access tokens for multiple users and then invokes the list endpoint using each user. The results for each
     * invocation should not contain any information about or related to the app passwords for the other users.
     */
    protected void test_multipleUsersCreatePasswords(boolean includeUserNameParameter) throws Exception {
        Map<String, AuthenticatedUserData> userData = createAppCredentialsForMultipleUsers();

        for (String currentUser : userData.keySet()) {
            AuthenticatedUserData currentUserData = userData.get(currentUser);
            Set<String> otherUsernames = new HashSet<String>(userData.keySet());
            otherUsernames.remove(currentUser);

            TestSettings updatedTestSettings = testSettings.copyTestSettings();
            updatedTestSettings.setAdminUser(currentUserData.username);
            updatedTestSettings.setAdminPswd(currentUserData.password);

            String userNameParameter = includeUserNameParameter ? currentUserData.username : userNotSetOrUnknown;
            List<validationData> currentUserListExpectations = getSuccessfulListAppPasswordExpectations(updatedTestSettings, userNameParameter);
            // Add expectations to ensure no data about the other users shows up in the response
            for (String otherUser : otherUsernames) {
                AuthenticatedUserData otherUserData = userData.get(otherUser);
                addUserAndAssociatedPasswordsNotFoundExpectations(currentUserListExpectations, otherUserData);
            }
            // List app passwords for current user and ensure response contains no information related to other users
            List<TokenValues> actualValues = listAppPasswords(updatedTestSettings, currentUserData.getLatestAccessToken(), userNameParameter, currentUserListExpectations);
            validateAppPasswordsListValues(actualValues, currentUserData.appPasswords);
        }
    }

    /**
     * Adds expectations that ensure the given user, access token, and relevant app password values do not appear anywhere in the
     * web response.
     */
    protected void addUserAndAssociatedPasswordsNotFoundExpectations(List<validationData> expectations, AuthenticatedUserData otherUserData) throws Exception {
        Log.info(getClass(), "addUserAndAssociatedPasswordsNotFoundExpectations", "Adding expectations to ensure app password data related to user [" + otherUserData.username + "] does not appear in response.");
        vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found a user name in the response that should not have been there.", null, otherUserData.username);
        vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an access token for a different user in the response but should not have.", null, otherUserData.getLatestAccessToken());
        if (otherUserData.appPasswords != null) {
            for (TokenValues tokenValues : otherUserData.appPasswords) {
                vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an app ID associated with a different user in the response but should not have.", null, tokenValues.getApp_id());
                vData.addExpectation(expectations, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an app password associated with a different user in the response but should not have.", null, tokenValues.getApp_password());
            }
        }
    }

    /**
     * Creates app-passwords for the user specifying used_by to indicate the client for which the app-passwords can be used
     */
    protected AuthenticatedUserData createAppPasswordsWithUsedBy(TestSettings settings, String usedByClientId) throws Exception {
        String accessToken = getAccessTokenAndCacheUserData(settings);
        List<TokenValues> createdAppPasswords = createAndValidateAppPasswordsFromSameAccessToken(settings, accessToken, _testName, usedByClientId, unknownLifetime, 2);
        AuthenticatedUserData userData = cachedUserData.get(getCachedUserDataKey(settings));
        userData.addAppPasswords(createdAppPasswords);
        return userData;
    }

    /**
     * Revoke app-tokens for all users when the client allows processing of app-tokens
     */
    protected void revokeAllAppTokens() throws Exception {
        String method = "revokeAllAppTokens";
        for (Entry<String, AuthenticatedUserData> entry : cachedUserData.entrySet()) {
            AuthenticatedUserData userData = entry.getValue();
            if (userData.accessTokens == null || userData.accessTokens.isEmpty()) {
                continue;
            }
            if (isClientThatShouldNotAttemptTokenRevoke(userData.provider, userData.clientId)) {
                continue;
            }
            TestSettings settings = updateTestSettingsForAppTokensTests(testSettings, userData.clientId, userData.clientSecret, userData.username, userData.password, userData.provider, doNotOverrideApp);
            Log.info(getClass(), method, "Revoking app tokens for user + provider + client: [" + entry.getKey() + "]...");
            revokeAndValidateAppTokens(null, settings, userData.getLatestAccessToken(), userNotSetOrUnknown, appIdNotSetOrUnknown);

            userData.resetAppTokens();
        }
    }

    protected boolean isClientThatShouldNotAttemptTokenRevoke(String provider, String clientId) {
        return "NoAppTok01".equals(clientId) || "NoAppTok02".equals(clientId) || "genAppPwAndAppToken".equals(clientId)
                || "genShortLivedAccessToken".equals(clientId) || "genShortLivedAppTok".equals(clientId) || "genAppTokLimitedNumAllowed".equals(clientId);
    }

    /**
     * Adds expectations that ensure the given user, access token, and relevant app token values do not appear anywhere in the web
     * response.
     */
    protected void addUserAndAssociatedTokensNotFoundExpectations(List<validationData> expectations, AuthenticatedUserData otherUserData) throws Exception {
        Log.info(getClass(), "addUserAndAssociatedTokensNotFoundExpectations", "Adding expectations to ensure app token data related to user [" + otherUserData.username + "] does not appear in response.");
        vData.addExpectation(expectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found a user name in the response that should not have been there.", null, otherUserData.username);
        vData.addExpectation(expectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an access token for a different user in the response but should not have.", null, otherUserData.getLatestAccessToken());
        if (otherUserData.appTokens != null) {
            for (TokenValues tokenValues : otherUserData.appTokens) {
                vData.addExpectation(expectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an app ID associated with a different user in the response but should not have.", null, tokenValues.getApp_id());
                vData.addExpectation(expectations, Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an app token associated with a different user in the response but should not have.", null, tokenValues.getApp_token());
            }
        }
    }

    /**
     * Creates app-tokens for the user specifying used_by to indicate the client for which the app-tokens can be used
     */
    protected AuthenticatedUserData createAppTokenWithUsedBy(TestSettings settings, String usedByClientId) throws Exception {
        String accessToken = getAccessTokenAndCacheUserData(settings);
        List<TokenValues> createdAppTokens = createAndValidateAppTokensFromSameAccessToken(settings, accessToken, _testName, usedByClientId, unknownLifetime, 1);
        AuthenticatedUserData userData = cachedUserData.get(getCachedUserDataKey(settings));
        userData.addAppTokens(createdAppTokens);
        return userData;
    }

    abstract protected List<TokenValues> createAndValidateAppCredentialsFromSameAccessToken(TestSettings settings, String username, String accessToken) throws Exception;

    abstract protected void addCredentialsToAuthenticatedUserData(List<TokenValues> appCredentials, AuthenticatedUserData userData);

}
