/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.identitystore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.json.java.JSONObject;

import io.openliberty.security.jakartasec.tokens.OpenIdClaimsImpl;
import io.openliberty.security.oidcclientcore.discovery.OidcDiscoveryConstants;
import io.openliberty.security.oidcclientcore.storage.OidcStorageUtils;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;
import jakarta.security.enterprise.identitystore.openid.AccessToken;
import jakarta.security.enterprise.identitystore.openid.IdentityToken;
import jakarta.security.enterprise.identitystore.openid.OpenIdClaims;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.security.enterprise.identitystore.openid.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import test.common.SharedOutputManager;

public class OpenIdContextImplTest {

    private static final String SUBJECT_IN_ID_TOKEN = "Jackson";
    protected static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final String TOKEN_TYPE_MAC = "MAC";
    private static final Long ONE_HOUR = Long.valueOf(3600);
    protected static final String STATE = "1234567890";
    protected static final String clientID = "myClient";

    private final Mockery mockery = new JUnit4Mockery();

    private AccessToken accessToken;
    private IdentityToken identityToken;
    private RefreshToken refreshToken;
    private OpenIdClaims userinfoClaims;
    private JsonObject providerMetadata;
    private JsonObject jsonObject;
    private OpenIdContext openIdContext;
    private HttpServletRequest request;
    private HttpServletResponse response;

    private final String sampleStringValue = "some string value";

    SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");
    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        accessToken = mockery.mock(AccessToken.class);
        identityToken = mockery.mock(IdentityToken.class);
        refreshToken = mockery.mock(RefreshToken.class);
        userinfoClaims = mockery.mock(OpenIdClaims.class);
        providerMetadata = Json.createObjectBuilder().add(OpenIdConstant.ISSUER, "https://localhost:9443/oidc/endpoint/OP/authorize").build();

        openIdContext = new OpenIdContextImpl(SUBJECT_IN_ID_TOKEN, TOKEN_TYPE_BEARER, accessToken, identityToken, userinfoClaims, providerMetadata, STATE, true, clientID);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetSubject() {
        assertEquals("The subject identifier must be set.", SUBJECT_IN_ID_TOKEN, openIdContext.getSubject());
    }

    @Test
    public void testGetTokenType() {
        assertEquals("The token type must be set.", TOKEN_TYPE_BEARER, openIdContext.getTokenType());
    }

    @Test
    public void testGetAccessToken() {
        assertEquals("The access token must be set.", accessToken, openIdContext.getAccessToken());
    }

    @Test
    public void testGetIdentityToken() {
        assertEquals("The identity token must be set.", identityToken, openIdContext.getIdentityToken());
    }

    @Test
    public void testGetRefreshToken_notSet() {
        Optional<RefreshToken> optionalRefreshToken = openIdContext.getRefreshToken();

        assertFalse("The refresh token must not be set.", optionalRefreshToken.isPresent());
    }

    @Test
    public void testGetRefreshToken_set() {
        ((OpenIdContextImpl) openIdContext).setRefreshToken(refreshToken);
        Optional<RefreshToken> optionalRefreshToken = openIdContext.getRefreshToken();

        assertEquals("The refresh token must be set.", refreshToken, optionalRefreshToken.get());
    }

    @Test
    public void testGetExpiresIn_notSet() {
        Optional<Long> optionalExpiresIn = openIdContext.getExpiresIn();

        assertFalse("The 'expires in' must not be set.", optionalExpiresIn.isPresent());
    }

    @Test
    public void testGetExpiresIn_set() {
        ((OpenIdContextImpl) openIdContext).setExpiresIn(ONE_HOUR);
        Optional<Long> optionalExpiresIn = openIdContext.getExpiresIn();

        assertEquals("The 'expires in' must be set.", ONE_HOUR, optionalExpiresIn.get());
    }

    /**
     * Set all the OpenIdClaims variables and verify we get a JsonObject back containing them.
     */
    @Test
    public void testGetClaimsJson() {

        Map<String, Object> claimsMap = new HashMap<String, Object>();
        claimsMap.put(OpenIdConstant.ADDRESS, "123 This Street");
        claimsMap.put(OpenIdConstant.BIRTHDATE, "01/01/2021");
        claimsMap.put(OpenIdConstant.EMAIL, "myemailaddress@ibm.com");
        claimsMap.put(OpenIdConstant.EMAIL_VERIFIED, "true");
        claimsMap.put(OpenIdConstant.FAMILY_NAME, "myfamilyname");
        claimsMap.put(OpenIdConstant.GENDER, "Non-binary");
        claimsMap.put(OpenIdConstant.GIVEN_NAME, "mygivenname");
        claimsMap.put(OpenIdConstant.LOCALE, "US_EN");
        claimsMap.put(OpenIdConstant.MIDDLE_NAME, "mymiddlename");
        claimsMap.put(OpenIdConstant.NAME, "myname");
        claimsMap.put(OpenIdConstant.NICKNAME, "mynickname");
        claimsMap.put(OpenIdConstant.PHONE_NUMBER, "555-555-5555");
        claimsMap.put(OpenIdConstant.PHONE_NUMBER_VERIFIED, "true");
        claimsMap.put(OpenIdConstant.PICTURE, "myphoto.jpg");
        claimsMap.put(OpenIdConstant.PREFERRED_USERNAME, "myusername");
        claimsMap.put(OpenIdConstant.PROFILE, "myprofile");
        claimsMap.put(OpenIdConstant.SUBJECT_IDENTIFIER, "mySubject");
        claimsMap.put(OpenIdConstant.UPDATED_AT, "12:00AM");
        claimsMap.put(OpenIdConstant.WEBSITE, "www.my.claims.website");
        claimsMap.put(OpenIdConstant.ZONEINFO, "CST");
        OpenIdClaims userinfo = new OpenIdClaimsImpl(claimsMap);

        OpenIdContext openIdContextWithUserInfo = new OpenIdContextImpl(SUBJECT_IN_ID_TOKEN, TOKEN_TYPE_BEARER, accessToken, identityToken, userinfo, providerMetadata, STATE, true, clientID);

        JsonObject json = openIdContextWithUserInfo.getClaimsJson();

        assertNotNull("The userinfo claims as Json must be set on the OpenIdContext.", json);
        assertEquals("Street should match", "123 This Street", json.getString(OpenIdConstant.ADDRESS));
        assertEquals("Birthday should match", "01/01/2021", json.getString(OpenIdConstant.BIRTHDATE));
        assertEquals("Email address should match", "myemailaddress@ibm.com", json.getString(OpenIdConstant.EMAIL));
        assertEquals("Email address verified should match", "true", json.getString(OpenIdConstant.EMAIL_VERIFIED));
        assertEquals("Family name should match", "myfamilyname", json.getString(OpenIdConstant.FAMILY_NAME));
        assertEquals("Gender should match", "Non-binary", json.getString(OpenIdConstant.GENDER));
        assertEquals("Given name should match", "mygivenname", json.getString(OpenIdConstant.GIVEN_NAME));
        assertEquals("Locale should match", "US_EN", json.getString(OpenIdConstant.LOCALE));
        assertEquals("Middle name should match", "mymiddlename", json.getString(OpenIdConstant.MIDDLE_NAME));
        assertEquals("Name should match", "myname", json.getString(OpenIdConstant.NAME));
        assertEquals("Nickname should match", "mynickname", json.getString(OpenIdConstant.NICKNAME));
        assertEquals("Phone number should match", "555-555-5555", json.getString(OpenIdConstant.PHONE_NUMBER));
        assertEquals("Phone number verifed should match", "true", json.getString(OpenIdConstant.PHONE_NUMBER_VERIFIED));
        assertEquals("Picture should match", "myphoto.jpg", json.getString(OpenIdConstant.PICTURE));
        assertEquals("Preferred username should match", "myusername", json.getString(OpenIdConstant.PREFERRED_USERNAME));
        assertEquals("Profile should match", "myprofile", json.getString(OpenIdConstant.PROFILE));
        assertEquals("Subject should match", "mySubject", json.getString(OpenIdConstant.SUBJECT_IDENTIFIER));
        assertEquals("Updated at should match", "12:00AM", json.getString(OpenIdConstant.UPDATED_AT));
        assertEquals("Website should match", "www.my.claims.website", json.getString(OpenIdConstant.WEBSITE));
        assertEquals("Zone info at should match", "CST", json.getString(OpenIdConstant.ZONEINFO));

    }

    /**
     * Do not set the optional variables on OpenIdClaims and verify they are not present on the JsonObject
     */
    @Test
    public void testGetClaimsJson_empty() {
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        // Subject is required
        claimsMap.put(OpenIdConstant.SUBJECT_IDENTIFIER, "mySubject");

        OpenIdClaims userinfo = new OpenIdClaimsImpl(claimsMap);

        OpenIdContext openIdContextWithUserInfo = new OpenIdContextImpl(SUBJECT_IN_ID_TOKEN, TOKEN_TYPE_BEARER, accessToken, identityToken, userinfo, providerMetadata, STATE, true, clientID);
        JsonObject json = openIdContextWithUserInfo.getClaimsJson();

        assertNotNull("The userinfo claims as Json must be set on the OpenIdContext.", json);

        assertEquals("Subject should match", "mySubject", json.getString(OpenIdConstant.SUBJECT_IDENTIFIER));

        assertFalse("Street should not be set", json.containsKey(OpenIdConstant.ADDRESS));
        assertFalse("Birthday should not be set", json.containsKey(OpenIdConstant.BIRTHDATE));
        assertFalse("Email address should not be set", json.containsKey(OpenIdConstant.EMAIL));
        assertFalse("Email address verified should not be set", json.containsKey(OpenIdConstant.EMAIL_VERIFIED));
        assertFalse("Family name should not be set", json.containsKey(OpenIdConstant.FAMILY_NAME));
        assertFalse("Gender should not be set", json.containsKey(OpenIdConstant.GENDER));
        assertFalse("Given name should not be set", json.containsKey(OpenIdConstant.GIVEN_NAME));
        assertFalse("Locale should not be set", json.containsKey(OpenIdConstant.LOCALE));
        assertFalse("Middle name should not be set", json.containsKey(OpenIdConstant.MIDDLE_NAME));
        assertFalse("Name should not be set", json.containsKey(OpenIdConstant.NAME));
        assertFalse("Nickname should not be set", json.containsKey(OpenIdConstant.NICKNAME));
        assertFalse("Phone number should not be set", json.containsKey(OpenIdConstant.PHONE_NUMBER));
        assertFalse("Phone number verifed should not be set", json.containsKey(OpenIdConstant.PHONE_NUMBER_VERIFIED));
        assertFalse("Picture should not be set", json.containsKey(OpenIdConstant.PICTURE));
        assertFalse("Preferred username should not be set", json.containsKey(OpenIdConstant.PREFERRED_USERNAME));
        assertFalse("Profile should not be set", json.containsKey(OpenIdConstant.PROFILE));
        assertFalse("Updated at should not be set", json.containsKey(OpenIdConstant.UPDATED_AT));
        assertFalse("Website should not be set", json.containsKey(OpenIdConstant.WEBSITE));
        assertFalse("Zone info at should not be set", json.containsKey(OpenIdConstant.ZONEINFO));

    }

    /**
     * Send in a null OpenIdClaims, make sure we don't fail when the claimsJson String is requested.
     */
    @Test
    public void testGetClaimsJson_null() {

        OpenIdContext openIdContextWithUserInfo = new OpenIdContextImpl(SUBJECT_IN_ID_TOKEN, TOKEN_TYPE_BEARER, accessToken, identityToken, null, providerMetadata, STATE, true, clientID);
        JsonObject json = openIdContextWithUserInfo.getClaimsJson();

        assertNull("The userinfo was null, json request will be null.", json);
    }

    /**
     * Send in an empty OpenIdClaims object, make sure we handle the IllegalArgumentException from OpenIdClaims.getSubject()
     */
    @Test
    public void testGetClaimsJson_missing_subject() {

        Map<String, Object> claimsMap = new HashMap<String, Object>();

        OpenIdClaims userinfo = new OpenIdClaimsImpl(claimsMap);

        OpenIdContext openIdContextWithUserInfo = new OpenIdContextImpl(SUBJECT_IN_ID_TOKEN, TOKEN_TYPE_BEARER, accessToken, identityToken, userinfo, providerMetadata, STATE, true, clientID);
        JsonObject json = openIdContextWithUserInfo.getClaimsJson();

        assertNotNull("The userinfo claims as Json must be set on the OpenIdContext.", json);

        assertFalse("Subject should not be set", json.containsKey(OpenIdConstant.SUBJECT_IDENTIFIER));
    }

    @Test
    public void testGetClaims() {
        assertEquals("The userinfo claims must be set.", userinfoClaims, openIdContext.getClaims());
    }

    @Test
    public void testGetProviderMetadata() {
        assertEquals("The provider metadata must be set.", providerMetadata, openIdContext.getProviderMetadata());
    }

    @Test
    public void testGetStoredValue_sessionBasedStorage() {
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        HttpSession session = mockery.mock(HttpSession.class);
        String originalRequestUrl = "originalRequestUrl";

        mockery.checking(new Expectations() {
            {
                allowing(request).getSession();
                will(returnValue(session));
                allowing(session).getAttribute(OidcStorageUtils.getOriginalReqUrlStorageKey(STATE));
                will(returnValue(originalRequestUrl));
            }
        });

        Optional<String> originalRequestStringOptional = openIdContext.getStoredValue(request, response, OpenIdConstant.ORIGINAL_REQUEST);

        assertEquals("The original request must be found in the OpenIdContext.", originalRequestUrl, originalRequestStringOptional.get());
    }

    @Test
    public void testGetStoredValue_missingStoredValue_shouldReturnEmpty() {
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);

        try {

            Optional<String> someMissingStoredValue = openIdContext.getStoredValue(request, response, OpenIdConstant.SUBJECT_IDENTIFIER);
            assertFalse(someMissingStoredValue.isPresent());

        } catch (Exception ex) {
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    /**
     * Testing this helper class to convert a JSONObject to a Jakarta JsonObject. See also:
     * MetadataUtilsTest.test_getProviderMetadata_providerMetadataHasValue and
     * OidcIdentityStore.getProviderMetadataAsJsonObject
     *
     * @throws Exception
     */
    @Test
    public void test_convertJsonObject() throws Exception {
        JSONObject discoveryData = new JSONObject();
        discoveryData.put(OidcDiscoveryConstants.METADATA_KEY_ISSUER, sampleStringValue);

        JsonObject result = OpenIdContextUtils.convertJsonObject(discoveryData);

        assertNotNull("Should have returned providerMetadata JsonObject", result);
        assertTrue("Expected " + OidcDiscoveryConstants.METADATA_KEY_ISSUER + " key in JsonObject", result.containsKey(OidcDiscoveryConstants.METADATA_KEY_ISSUER));
        assertEquals(sampleStringValue, result.getString(OidcDiscoveryConstants.METADATA_KEY_ISSUER));

    }

    @Test
    public void test_getProviderMetadata_providerMetadataNull() throws Exception {
        JSONObject discoveryData = null;

        JsonObject result = OpenIdContextUtils.convertJsonObject(discoveryData);

        assertNull("Should not have returned providerMetadata JsonObject", result);

    }

    @Test
    public void test_getProviderMetadata_providerMetadataIsEmpty() throws Exception {
        JSONObject discoveryData = new JSONObject();

        JsonObject result = OpenIdContextUtils.convertJsonObject(discoveryData);

        assertNotNull("Should have returned providerMetadata JsonObject", result);
        assertTrue("Expected empty JSONObject", result.isEmpty());

    }
}
