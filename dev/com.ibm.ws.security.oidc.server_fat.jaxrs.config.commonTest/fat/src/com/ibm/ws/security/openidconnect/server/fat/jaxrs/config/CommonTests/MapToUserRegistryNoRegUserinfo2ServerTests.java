/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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

package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSSkipIfJWTToken;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.MapToRegistryCommonTest;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains basic OpenID Connect and OAuth tests for Resource Server (RS)
 * configuration parameters below when there is no user registry in the RS server config. The userinfo
 * endpoint for access token and local validation for JWT token is used by the RS to validate the token.
 * The test framework with randomly select between the token type when the test is run.
 * 
 * This contains both positive and negative tests of the following configuration attributes.
 * 
 * mapIdentityToRegsiteryUser - False
 * userIdentityToCreateSubject
 * userIdentifier
 * groupIdentifier
 * realmIdentifier
 * uniqueUserIdentifier
 * 
 * validationMethod - userinfo (default for access token if not specified)
 * validationEndpoint - userinfo endpoint URL
 * 
 * The basic flow for these tests consists of
 * - Access OP for authorization grant
 * - Access OP server for access token or JWT token
 * - Use access token to access a protected resource (snoop) on the OP server
 * - Attempt to access protected resource on Resource Server (RS) with same token
 * - validate access token with the endpoint specified in config to make sure token valid and active
 * - if valid, create subject based on config parameters
 * - if userIdentifier present (does not default), use the value of the claim it specifies to create subject
 * - if not (by default) use userIdentityToCreateSubject which defaults to "sub" claim
 * - process other identifiers and take defaults if they are not specified (groupIdentifier, realmIdentifer, uniqueUserIdentifier)
 * 
 * groupIdentifier defaults to groupIds claim
 * realmName (String) no default. If not used, then process realmIdentifier.
 * realmIdentifer defaults to realmName claim
 * uniqueUserId defaults to uniqueSecurityName claim
 * 
 * These tests use a single RS server config file with different OIDC client and filters for
 * the various combinations of configuration parameters.
 * 
 * This test depends upon the following users in the registry
 * testuser - not in any groups
 * user1 in group1
 * diffuser in group1
 * 
 **/

public class MapToUserRegistryNoRegUserinfo2ServerTests extends MapToRegistryCommonTest {

    private static final Class<?> thisClass = MapToUserRegistryNoRegUserinfo2ServerTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static String targetProvider = null;
    protected static String targetISSEndpoint = null;

    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";

    protected String defaultISSAccessId = "user:http://" + targetISSEndpoint;

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    /******* No user registry configured for RS server *******/
    /******* mapToUserRegistry = No *******/
    /******* validation method and URL = userinfo *******/

    /**
     * Test Purpose: Verify default behavior with minimal config parmameters when no RS user registry.
     * <OL>
     * <LI>validationMethod specified as userinfo
     * <LI>userIdentifier (not specified - not used)
     * <LI>userIdentityToCreateSubject (not specified - defaults to sub)
     * <LI>mapIdentityToRegistryUser = False (default)
     * <LI>groupIdentifier (not specified - defaults to groupIds)
     * <LI>realmName (String not specified - defaults to realmIdentifer)
     * <LI>realmIdentifer (not specified - defaults to ISS claim)
     * <LI>uniqueUserIdentifier (not specified - defaults to uniqueSecurityName)
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The default behavior is userIdentifierToCreateSubject which defaults to sub claim for subject creation with defaults
     * for the other identifiers which are not specified. For access_token, the realm defaults to ISS since neither realmName nor
     * realmIdentifier are specified and there is no realmName claim in userinfo output for access token. For JWT token, there is
     * a realmName claim, so the value with default to realmName (OPTestRealm) Access to the protected app on RS is allowed the
     * app prints subject info for validation.
     * 
     * Results with access_token:
     * <LI>WSCredential SecurityName=testuser
     * <LI>WSCredential RealmName=http://localhost:8940/oidc/endpoint/OidcConfigSample
     * <LI>WSCredential RealmSecurityName=http://localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     * <LI>WSCredential UniqueSecurityName=testuser
     * <LI>WSCredential groupIds=[]
     * <LI>WSCredential accessId=user:http://localhost:8940/oidc/endpoint/OidcConfigSample/testuser Results with JWT token:
     * <LI>WSCredential SecurityName=testuser
     * <LI>WSCredential RealmName=OPTestRealm
     * <LI>WSCredential RealmSecurityName=OPTestRealm/testuser
     * <LI>WSCredential UniqueSecurityName=testuser
     * <LI>WSCredential groupIds=[]
     * <LI>WSCredential accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryNoRegUserinfo2ServerTests_NoRegistry_MinimalCfgTest() throws Exception {
        String defaultISSSecurityName = defaultBasicNoGroupUser;
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapDflt");

        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            generalPositiveTest(updatedTestSettings, defaultISSRealm, defaultBasicNoGroupUser, defaultEmptyGroup, defaultISSSecurityName, defaultISSAccessId);
        } else {
            generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);
        }
    }

    /**
     * Test Purpose: Verify default behavior with specification of the maximum set of config parmameters when no RS user registry
     * <OL>
     * <LI>userIdentifier - sub
     * <LI>userIdentityToCreateSubject - sub
     * <LI>mapIdentityToRegistryUser - false
     * <LI>groupIdentifier - groupIds
     * <LI>realmName (String) -MyRealm
     * <LI>realmIdentifier - realmName
     * <LI>uniqueUserIdentifier - uniqueSecurityName
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * The subject is created with the following:
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName (String) - MyRealm (takes precedence)
     * <LI>RealmIdentifier - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - MyRealm /user1
     * <LI>groupIds=[group:MyRealm /group1]
     * <LI>accessId=user:MyRealm /user1
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryNoRegUserinfo2ServerTests_NoRegistry_MaximumCfgTest() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMax");
        generalPositiveTest(updatedTestSettings, defaultSpecifiedRealm, defaultBasic1GroupUser, defaultBasicGroupSpecifiedRealmGroupID, null, null);
    }

    /**
     * Test Purpose: Verify when no userIdentity is specified that the userIdentityToCreateSubject is used and a bad
     * value results in an error
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier(not specified)
     * <LI>userIdentityToCreateSubject - badClaim
     * <LI>others default
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>403 Forbidden
     * <LI>Message in RS messages.log: CWWKS1722E: The resource server failed the authentication request because the access token
     * does not contain the claim [badClaim] specified by the [userIdentityToCreateSubject] attribute.
     * 
     * For JWT token -- CWWKS1738E: The OpenID Connect client [client01] failed to authenticate the JSON Web Token because the
     * claim [badClaim] specified by the [userIdentityToCreateSubject] configuration attribute was not included in the token.
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryNoRegUserinfo2ServerTests_NoRegistry_UserIdentifierAndUserIDCreateSubject_bothBadError() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUIdsBad");
        if (Constants.SUPPORTED.equals(updatedTestSettings.getInboundProp())) {
            generalNegativeTest(updatedTestSettings, Constants.UNAUTHORIZED_STATUS, null, null);
        } else {
            String expectedErrorMessage;
            if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
                expectedErrorMessage = MessageConstants.CWWKS1722E_UNABLE_TO_AUTHENTICATE_BADCLAIM + ".*\\[badClaim\\]";
            } else {
                expectedErrorMessage = MessageConstants.CWWKS1738E_JWT_MISSING_CLAIM + ".*\\[badClaim\\]";
            }
            generalNegativeTest(updatedTestSettings, Constants.UNAUTHORIZED_STATUS, FAILED_TO_FIND_MESSAGE, expectedErrorMessage);

        }
    }

    /**
     * Test Purpose: Verify when no userIdentity is specified that the userIdentityToCreateSubject is used and a good
     * value such as uniqueSecurityName results in successful subject creation
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier (not specified)
     * <LI>userIdentityToCreateSubject - client_id (value: client01)
     * <LI>groupIdentifier - defaults to groupIds from introspect validation
     * <LI>realmIdentifier - defaults to realmName from introspect validation
     * <LI>uniqueUserIdentifier - defaults to uniqueSecurityName from introspect validation
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The userIdentifierToCreateSubject with a good value allows access to protected app on RS and app displays subject which
     * is validated for the following. Results with access token:
     * <LI>SecurityName - testuser
     * <LI>RealmName - http:localhost:8940/oidc/endpoint/OidcConfigSample
     * <LI>RealmSecurityName - http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     * <LI>groupIds - groupids[]
     * <LI>accessId=user:http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser Results with JWT token:
     * <LI>SecurityName - testuser
     * <LI>RealmName - OPTestRealm
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>groupIds - groupids[]
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegUserinfo2ServerTests_NoRegistry_UserIDCreateSubject_goodClaim() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUIdSubOK");
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            generalPositiveTest(updatedTestSettings, defaultISSRealm, defaultBasicNoGroupUser, defaultEmptyGroup, defaultBasicNoGroupUser, defaultISSAccessId);
        } else {
            generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);
        }

    }

    /**
     * Test Purpose: Verify when both userIdentifier and other Identifers are all good values that the subject is created
     * with correct values.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - sub
     * <LI>realmIdentifier - realmName
     * <LI>groupIdentifier - groupIds
     * <LI>uniqueUserIdentifier - uniqueSecurityName
     * <LI>userIdentityToCreateSubject - No
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * Subject created with the following: Results with access token:
     * <LI>SecurityName - testuser
     * <LI>RealmName - http:localhost:8940/oidc/endpoint/OidcConfigSample
     * <LI>RealmSecurityName - http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     * <LI>groupIds - groupids[]
     * <LI>accessId=user:http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser Results with JWT token:
     * <LI>SecurityName - testuser
     * <LI>RealmName - OPTestRealm
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>groupIds - groupids[]
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegUserinfo2ServerTests_NoRegistry_IdentifiersGood() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testIdentsGood");
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            generalPositiveTest(updatedTestSettings, defaultISSRealm, defaultBasicNoGroupUser, defaultEmptyGroup, defaultBasicNoGroupUser, defaultISSAccessId);
        } else {
            generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);
        }

    }

    /**
     * Test Purpose: Verify when realmIdentifier is bad and realmName claim is empty, that the
     * default realmName attribute is used to create the subject.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - sub
     * <LI>realmName "" (empty string)
     * <LI>realmIdentifier - bad claim value -- badRealmName
     * <LI>groupIdentifier - groupIds
     * <LI>uniqueUserIdentifier - uniqueSecurityName
     * <LI>userIdentityToCreateSubject - No
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * Subject created with following content: Results with access token:
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - OP realm (http:localhost:8940/oidc/endpoint/OidcConfigSample)
     * <LI>RealmSecurityName - http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     * <LI>groupIds - groupids[]
     * <LI>accessId=user:http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser Results with JWT token:
     * <LI>SecurityName - testuser
     * <LI>RealmName - OPTestRealm
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>groupIds - groupids[]
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegUserinfo2ServerTests_NoRegistry_RealmIdentifierBadDefaultsToRealmName() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testRealmsBad");
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            generalPositiveTest(updatedTestSettings, defaultISSRealm, defaultBasicNoGroupUser, defaultEmptyGroup, defaultBasicNoGroupUser, defaultISSAccessId);
        } else {
            generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);
        }

    }

    /**
     * Test Purpose: Verify when the groupIdentifier is bad that the
     * groupIds in the subject defaults to no groups [].
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - sub
     * <LI>realmIdentifier - realmName
     * <LI>groupIdentifier - badGroupIds (claim does not exist)
     * <LI>uniqueUserIdentifier - uniqueSecurityName
     * <LI>userIdentityToCreateSubject - No
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * Subject created with: Results with access token:
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - http:localhost:8940/oidc/endpoint/OidcConfigSample
     * <LI>RealmSecurityName - http:localhost:8940/oidc/endpoint/OidcConfigSample/user1
     * <LI>groupIds - []
     * <LI>accessId=user:http:localhost:8940/oidc/endpoint/OidcConfigSample/user1 Results with JWT token:
     * <LI>SecurityName - user1
     * <LI>RealmName - OPTestRealm
     * <LI>RealmSecurityName - OPTestRealm/user1
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/user1
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegUserinfo2ServerTests_NoRegistry_GroupIdentifierBad_DefaultsToNoGroups() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testGroupBad");
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            generalPositiveTest(updatedTestSettings, defaultISSRealm, defaultBasic1GroupUser, defaultEmptyGroup, defaultBasic1GroupUser, defaultISSAccessId);
        } else {
            generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasic1GroupUser, defaultEmptyGroup, null, null);
        }

    }

    /**
     * Test Purpose: Verify behavior with when the realmName value is empty and realmIdentifier is also a bad claim value.
     * In this case, the realm should default to ISS value.
     * Note: This test does not apply to the JWT token as the JWT token validation will find a realmName claim in the token and
     * will use it
     * so the default will not be the Issuer.
     * <OL>
     * <LI>userIdentifier - none
     * <LI>userIdentityToCreateSubject - none
     * <LI>mapIdentityToRegistryUser - false
     * <LI>realmName - "" empty
     * <LI>realmIdentifier - bad value so default will be ISS
     * </OL>
     * <P>
     * Expected Results
     * <OL>
     * <LI>The default behavior is to use issuer (ISS) for the realmName when the realmName and realmIdentifer are bad and not
     * used.
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - http:localhost:8940/oidc/endpoint/OidcConfigSample
     * <LI>RealmSecurityName - http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     * <LI>UniqueSecurityName - http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     * <LI>groupIds=[]
     * <LI>accessId=user:http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser/
     * 
     * </OL>
     **/

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    public void MapToUserRegistryNoRegUserinfo2ServerTests_NoRegistry_bothRealmsBadDefaultsToISS() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testRealmsBad");
        generalPositiveTest(updatedTestSettings, defaultISSRealm, defaultBasicNoGroupUser, defaultEmptyGroup, defaultBasicNoGroupUser, defaultISSAccessId);

    }

    /**
     * Test Purpose: Verify when the uniqueUserIdentifier is specified as a bad claim type (iat numeric type)
     * that it defaults to the value of the uniqueSecurityName claim.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>uniqueUserIdentifier - type iat (numeric)
     * <LI>others default
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * Results with Access Token:
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - http:localhost:8940/oidc/endpoint/OidcConfigSample
     * <LI>RealmSecurityName - http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     * <LI>UniqueSecurityName - testuser
     * <LI>groupIds - []
     * <LI>accessId=user:http:localhost:8940/oidc/endpoint/OidcConfigSample/testuser Results with JWT token:
     * <LI>SecurityName - testuser
     * <LI>RealmName - OPTestRealm
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/

    @Test
    public void MapToUserRegistryNoRegUserinfo2ServerTests_NoRegistry_unqiueUserId_badClaimTypeError_DefaultsToUniqueSecurityName() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testuniqueIdTypeBad");

        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            generalPositiveTest(updatedTestSettings, defaultISSRealm, defaultBasicNoGroupUser, defaultEmptyGroup, defaultBasicNoGroupUser, defaultISSAccessId);
        } else {
            generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);
        }
    }
}
