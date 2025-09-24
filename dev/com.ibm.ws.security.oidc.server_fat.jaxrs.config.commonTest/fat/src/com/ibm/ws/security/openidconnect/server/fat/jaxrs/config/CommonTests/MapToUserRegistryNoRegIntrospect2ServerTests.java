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

import java.util.List;

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
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.CommonTools;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.MapToRegistryCommonTest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains basic OpenID Connect and OAuth tests for Resource Server (RS)
 * configuration parameters below when there is no user registry in the RS server config . The introspection
 * endpoint for access token and local endpoint for JWT is used by the RS to validate the token. The test framework
 * will randomly select between access token and JWT token.
 *
 * This contains both positive and negative tests of the following configuration attributes.
 *
 * mapIdentityToRegsiteryUser
 * userIdentityToCreateSubject
 * userIdentifier (claim)
 * groupIdentifier (claim)
 * realmName (String)
 * realmIdentifier (claim)
 * uniqueUserIdentifier (claim)
 *
 * validationMethod - introspect
 * validationEndpoint - introspect endpoint URL
 *
 * The basic flow for these tests consists of
 * - Access OP for authorization grant
 * - Access OP server for access token or JWT token
 * - Use access token to access a protected resource (snoop) on the OP server
 * - Attempt to access protected resource on Resource Server (RS) with same token
 * - validate access token with the endpoint specified in config (or local endpoint if JWT) to make sure token valid and active
 * - if valid, create subject based on config parameters
 * - if userIdentifier present (does not default), use the value of the claim it specifies to create subject
 * - if not (by default) use userIdentityToCreateSubject which defaults to "sub" claim
 * - process other identifiers and take defaults if they are not specified (groupIdentifier, realmIdentifer, uniqueUserIdentifier)
 *
 * groupIdentifier defaults to groupIds claim
 * realmName (String) takes precedence if there, otherwise use realmIdentifier
 * realmIdentifer defaults to realmName claim
 * if realmName is not specified and realmIdentifer is bad, the default is ISS (issuer) claim
 * uniqueUserId defaults to uniqueSecurityName claim
 *
 * These tests use a single RS server config file with different OIDC client and filters for the various combinations of
 * configuration parameters.
 *
 * Dependencies:
 *
 * This test depends upon the following users in the registry on the OP server
 * testuser - not in any groups
 * user1 in group1
 *
 * The Hello World application on the RS server is protected by Manager and Employee role so in order for access to be granted
 * when there is no registry on the RS,
 * the access-ids for the users and groups must be present in the application-bnd as follows:
 *
 * <application-bnd>
 * <security-role name="Employee">
 * <role-name>Employee</role-name>
 * <group name="My group1" access-id="group:MyRealm/group1" />
 * <group name="OP group1" access-id="group:OPTestRealm/group1" />
 * </security-role>
 * <security-role name="Manager">
 * <role-name>Manager</role-name>
 * <user name="user1" access-id="user:OPTestRealm/user1" />
 * <user name="client01" access-id="user:OPTestRealm/client01" />
 * <user name="testuserOP" access-id="user:OPTestRealm/testuser" />
 * <user name="testuserISSOidc" access-id="user:http://localhost:8940/oidc/endpoint/OidcConfigSample/testuser" />
 * <user name="testuserISSOauth" access-id="user:http://localhost:8940/oauth2/endpoint/OAuthConfigSample/testuser" />
 * </security-role>
 * </application-bnd>
 *
 * The access-id for the user testuser in realm NoAccessId must not be present in the application-bnd so that the negative test
 * will result in 403 error.
 *
 **/

public class MapToUserRegistryNoRegIntrospect2ServerTests extends MapToRegistryCommonTest {

    private static final Class<?> thisClass = MapToUserRegistryNoRegIntrospect2ServerTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static CommonTools commonTools = new CommonTools();
    protected static String targetProvider = null;
    protected static String targetISSEndpoint = null;

    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";

    protected String defaultISSAccessId = "user:http://" + targetISSEndpoint;

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();
    protected static int OIDC_ERROR_1 = 1;

    /******* No user registry configured for RS server *******/
    /******* mapToUserRegistry = No *******/
    /******* validation method and URL = introspect *******/

    /**
     * Test Purpose: Verify default behavior with minimal config parmameters when no RS user registry. Default values are assigned
     * for identifiers.
     * When validationMethod is not specified it defaults to introspect.
     * <OL>
     * <LI>validationMethod (not specified so defaults to introspect)
     * <LI>mapToUserRegistry (not specified defaults to NO)
     * <LI>userIdentifier (not specified - not used)
     * <LI>userIdentityToCreateSubject (not specified - defaults to sub)
     * <LI>mapIdentityToRegistryUser (not specified - defaults to false)
     * <LI>groupIdentifier (not specified - defaults to groupIds)
     * <LI>realmName (String not specified - defaults to realmIdentifer)
     * <LI>realmIdentifer (not specified - defaults to realmName claim)
     * <LI>uniqueUserIdentifier (not specified - defaults to uniqueSecurityName)
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * Subject created with the following:
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>UniqueSecurityName - testuser
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_MinimalCfgTest_DefaultsTaken() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapDflt");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);

    }

    /**
     * Test Purpose: Verify default behavior with specification of the maximum set of config parmameters are
     * specified and when no RS user registry mapping.
     * <OL>
     * <LI>userIdentifier - sub
     * <LI>userIdentityToCreateSubject - sub
     * <LI>mapIdentityToRegistryUser - false
     * <LI>groupIdentifier - groupIds claim
     * <LI>realmIdentifier - realmName claim
     * <LI>realmName (String) MyRealm
     * <LI>uniqueUserIdentifier - uniqueSecurityName
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * Subject created with the following:
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - OP realm (MyRealm)
     * <LI>RealmSecurityName - MyRealm/user1
     * <LI>UniqueSecurityName - user1
     * <LI>groupIds=[group:MyRealm/group1]
     * <LI>accessId=user:MyRealm/user1
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_MaximumCfgTest_AllIdentifiersSpecified() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMax");
        generalPositiveTest(updatedTestSettings, defaultSpecifiedRealm, defaultBasic1GroupUser, defaultBasicGroupSpecifiedRealmGroupID, null, null);

    }

    /**
     * Test Purpose: Verify when no userIdentity is specified that the userIdentityToCreateSubject is used. When a bad
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
     * <LI>The userIdentifierToCreateSubject with a value of badClaim results in an error when attempt to access protected
     * resource on the Resource Server.
     * <LI>401 Forbidden
     * <LI>Message in RS messages.log: For access token -- CWWKS1722E: The resource server failed the authentication request
     * because the access token does not contain the claim [badClaim] specified by the [userIdentityToCreateSubject] attribute.
     *
     * For JWT token -- CWWKS1738E: The OpenID Connect client [client01] failed to authenticate the JSON Web Token because the
     * claim [badClaim] specified by the [userIdentityToCreateSubject] configuration attribute was not included in the token.
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_UserIDCreateSubjectBadClaim_401Error() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUIdSubBad");
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
     * Test Purpose: Verify when no userIdentity is specified that the userIdentityToCreateSubject is used and a claim
     * other than sub can be used and results in successful subject creation.
     * Note: This does NOT apply when JWT token is used because the client_id claim is not present in that token and will fail
     * validation.
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
     * is validated for the following:
     * <LI>SecurityName - client01
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/client01
     * <LI>UniqueSecurityName - client01
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/client01
     * </OL>
     **/

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_UserIDCreateSubject_goodClaim() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUIdSubOK");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, "client01", defaultEmptyGroup, null, "user:OPTestRealm/client01");

    }

    /**
     * Test Purpose: Verify when no userIdentity is specified and if the userIdentityToCreateSubject is an empty
     * value, then the runtime should default to using "sub" claim as the user identity to create the Subject
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier(not specified)
     * <LI>userIdentityToCreateSubject - ""
     * <LI>others default
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The userIdentifierToCreateSubject with an empty value results in an error when attempt to access protected resource on
     * the Resource Server.
     * <LI>401 Unauthorized
     * <LI>Message in RS messages.log:
     * </OL>
     * For Access token --CWWKS1722E: The resource server failed the authentication request because the access token does not
     * contain the claim [null] specified by the [userIdentityToCreateSubject] attribute.
     * <LI>The userIdentifierToCreateSubject with an empty value results in subject creation with the default value of the
     * attribute which is "sub"
     * <LI>SecurityName - testuser
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>UniqueSecurityName - testuser
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     *
     *
     * </OL>
     * For JWT token -- CWWKS1738E: The OpenID Connect client [client01] failed to authenticate the JSON Web Token because
     * the claim [null] specified by the [userIdentityToCreateSubject] configuration attribute was not included in the token.
     *
     *
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_UserIDCreateSubjectEmpty_DefaultsToSubClaim() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUIdSubEmpty");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, "user:OPTestRealm/testuser");
    }

    /**
     * Test Purpose: Verify when both userIdentifier (good value) and userIdentityToCreateSubject (bad value) are specified that
     * the value of userIdentifer (sub) takes precedence and is used to create the subject.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - sub
     * <LI>userIdentityToCreateSubject - badClaim (should be overridden by userIdentifier)
     * <LI>others default
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>UniqueSecurityName - testuser
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_UserIDCreateSubjectBad_UserIdentifierTakesPrecedence() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testPrecedence");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);

    }

    /**
     * Test Purpose: Verify when both userIdentifier and other Identifiers are all good values that the subject is created
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
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>UniqueSecurityName - testuser
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_IdentifiersGood() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testIdentsGood");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);

    }

    /**
     * Test Purpose: Verify when userIdentifier is good and realmIdentifier is bad (a claim that does not exist), that
     * it defaults to the value of the realmName claim.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - sub
     * <LI>realmIdentifier - bad claim value -- badRealmName
     * <LI>groupIdentifier - groupIds
     * <LI>uniqueUserIdentifier - uniqueSecurityName
     * <LI>userIdentityToCreateSubject - No
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>UniqueSecurityName - testuser
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_RealmIdentifierBad_DefaultsToRealmNameClaim() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testRealmBad");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);

    }

    /**
     * Test Purpose: Verify when userIdentifier is good and realmIdentifier is an empty string "", that
     * the value of the realm defaults to the realmName claim.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - sub
     * <LI>realmIdentifier - "" (empty)
     * <LI>groupIdentifier - groupIds
     * <LI>uniqueUserIdentifier - uniqueSecurityName
     * <LI>userIdentityToCreateSubject - No
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>UniqueSecurityName - testuser
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_RealmIdentifierEmpty_DefaultsToRealmNameClaim() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testRealmEmpty");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);

    }

    /**
     * Test Purpose: Verify when userIdentifier is good and groupIdentifier is bad (a claim that does not exist), that
     * the groupIds in the subject defaults to no groups.
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
     * Successful access to protected app on RS and app displays subject which is validated for the following:
     * <LI>The groupIdentifier of empty string defaults to empty group in subject.
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/user1
     * <LI>UniqueSecurityName - user1
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/user1
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_GroupIdentifierBad_DefaultsToNoGroupsInSubject() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testGroupBad");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasic1GroupUser, defaultEmptyGroup, null, null);

    }

    /**
     * Test Purpose: Verify when userIdentifier is good and groupIdentifier is empty string "" , that
     * the groupIds in the subject defaults to no groups.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - sub
     * <LI>realmIdentifier - realmName
     * <LI>groupIdentifier - "" (empty string)
     * <LI>uniqueUserIdentifier - uniqueSecurityName
     * <LI>userIdentityToCreateSubject - No
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>The groupIdentifier of empty string defaults to empty group in subject.
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/user1
     * <LI>UniqueSecurityName - user1
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/user1
     * </OL>
     **/
    @Test
    @Mode(TestMode.LITE)
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_GroupIdentifierEmpty_DefaultsToNoGroupsInSubject() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testGroupEmpty");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasic1GroupUser, defaultEmptyGroup, null, null);

    }

    /**
     * Test Purpose: Verify when uniqueUserIdentifier is a bad claim name that does not exist, that the uniqueSecurityName
     * defaults to the
     * uniqueSecurityName claim in the access token.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - sub
     * <LI>realmIdentifier - realmName
     * <LI>groupIdentifier - groupIds
     * <LI>uniqueUserIdentifier - BadName
     * <LI>userIdentityToCreateSubject - No
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/user1
     * <LI>UniqueSecurityName - user1
     * <LI>groupIds - group:OPTestRealm/group1
     * <LI>accessId=user:OPTestRealm/user1
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_UniqueUserIdBad_DefaultsToUniqueSecurityName() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUniqueIdBad");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasic1GroupUser, defaultBasicGroupOPRealmGroupID, null, null);

    }

    /**
     * Test Purpose: Verify when uniqueUserIdentifier is empty that the uniqueSecurityName defaults to the
     * uniqueSecurityName claim in the access token.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - sub
     * <LI>realmIdentifier - realmName
     * <LI>groupIdentifier - groupIds
     * <LI>uniqueUserIdentifier - "" empty
     * <LI>userIdentityToCreateSubject - No
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/user1
     * <LI>UniqueSecurityName - user1
     * <LI>groupIds - group:OPTestRealm/group1
     * <LI>accessId=user:OPTestRealm/user1
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_UniqueUserIdEmpty_DefaultsToUniqueSecurityName() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUniqueIdEmpty");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasic1GroupUser, defaultBasicGroupOPRealmGroupID, null, null);

    }

    /**
     * Test Purpose: Verify when both realmName config attribute and realmIdentifier are specified that
     * the value of the String realmName is used to create the subject.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - sub
     * <LI>realmIdentifier - realmName
     * <LI>realmName - MyRealm
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - MyRealm
     * <LI>RealmSecurityName - MyRealm/user1
     * <LI>UniqueSecurityName - user1
     * <LI>groupIds=[group:MyRealm/group1]
     * <LI>accessId=user:MyRealm/user1
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_RealmNameTakesPrecedenceOverRealmIdentifier() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testRealmPrecedence");
        generalPositiveTest(updatedTestSettings, defaultSpecifiedRealm, defaultBasic1GroupUser, defaultBasicGroupSpecifiedRealmGroupID, null, null);

    }

    /**
     * Test Purpose: Verify default behavior with when no userIdentifier is specified but all other identifiers are specified.
     * The userIdentifier defaults to userIdentityToCreateSubject default value of sub. Other identifiers pick up the values of
     * claims specified.
     * <OL>
     * <LI>userIdentifier - none
     * <LI>userIdentityToCreateSubject - none
     * <LI>mapIdentityToRegistryUser - false
     * <LI>groupIdentifier - groupIds
     * <LI>realmName - realmName
     * <LI>uniqueUserIdentifier - uniqueSecurityName
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - MyRealm
     * <LI>RealmSecurityName - MyRealm/user1
     * <LI>UniqueSecurityName - user1
     * <LI>groupIds=[group:MyRealm/group1]
     * <LI>accessId=user:MyRealm/user1
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_userIdentDefaultsOthersSpecified() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testOtherIdentsOnly");
        generalPositiveTest(updatedTestSettings, defaultSpecifiedRealm, defaultBasic1GroupUser, defaultBasicGroupSpecifiedRealmGroupID, null, null);

    }

    /**
     * Test Purpose: Verify behavior with when the realmName value is empty. This results in the
     * realmIdentifier claim being used for the realmName in the subject.
     * <OL>
     * <LI>userIdentifier - none
     * <LI>userIdentityToCreateSubject - none
     * <LI>mapIdentityToRegistryUser - false
     * <LI>realmName - "" empty
     * <LI>realmIdentifier - defaults realmName claim
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - OPTestRealm
     * <LI>RealmSecurityName - OPTestRealm/user1
     * <LI>UniqueSecurityName - user1
     * <LI>groupIds=[group:OPTestRealm/group1]
     * <LI>accessId=user:OPTestRealm/user1
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_realmNameAttrEmpty_DefaultsToRealmIdentifier() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testRealmNameEmpty");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasic1GroupUser, defaultBasicGroupOPRealmGroupID, null, null);

    }

    /**
     * Test Purpose: Verify behavior with when the userIdentifier value is empty. This results in the
     * useIdentityToCreateSubject which defaults to sub claim.
     * <OL>
     * <LI>userIdentifier - "" (empty string)
     * <LI>userIdentityToCreateSubject - none
     * <LI>mapIdentityToRegistryUser - false
     * <LI>realmName - realmName
     * <LI>realmIdentifier - defaults realmName claim
     * </OL>
     * <P>
     * Expected Results: Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - OPTestRealm
     * <LI>RealmSecurityName - OPTestRealm/user1
     * <LI>UniqueSecurityName - user1
     * <LI>groupIds=[group:OPTestRealm/group1]
     * <LI>accessId=user:OPTestRealm/user1
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_userIdentifierEmpty_DefaultsToSub() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUserIdentEmpty");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasic1GroupUser, defaultBasicGroupOPRealmGroupID, null, null);

    }

    /**
     * Test Purpose: Verify behavior with when the userIdentifier value is is a BadClaim which does not exist.
     * This results in an error 401 when attempting to access the protected resource on the RS.
     * <OL>
     * <LI>userIdentifier - BadClaim
     * <LI>userIdentityToCreateSubject - none
     * <LI>mapIdentityToRegistryUser - false
     * <LI>realmName - realmName
     * <LI>realmIdentifier - defaults realmName claim
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The userIdentifier with a value of badClaim results in an error when attempt to access protected resource on the
     * Resource Server.
     * <LI>401 Forbidden
     * <LI>Message in RS messages.log: For access token -- CWWKS1722E: The resource server failed the authentication request
     * because the access token does not contain the claim [BadClaim] specified by the [userIdentifier] attribute.
     *
     * For JWT token --CWWKS1738E: The OpenID Connect client [client01] failed to authenticate the JSON Web Token because the
     * claim [BadClaim] specified by the [userIdentifier] configuration attribute was not included in the token.
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_userIdentifierBadClaim_401Error() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUserIdentBad");
        if (Constants.SUPPORTED.equals(updatedTestSettings.getInboundProp())) {
            generalNegativeTest(updatedTestSettings, Constants.UNAUTHORIZED_STATUS, null, null);
        } else {
            String expectedErrorMessage;
            if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
                expectedErrorMessage = MessageConstants.CWWKS1722E_UNABLE_TO_AUTHENTICATE_BADCLAIM + ".*\\[BadClaim\\]";
            } else {
                expectedErrorMessage = MessageConstants.CWWKS1738E_JWT_MISSING_CLAIM + ".*\\[BadClaim\\]";
            }
            generalNegativeTest(updatedTestSettings, Constants.UNAUTHORIZED_STATUS, FAILED_TO_FIND_MESSAGE, expectedErrorMessage);
        }

    }

    /**
     * Test Purpose: Verify behavior with when the realmName attribute value is empty and realmIdentifier is also a bad claim
     * value.
     * In this case, the realm should default to ISS value.
     * Note: This test does not apply for JWT token.
     * <OL>
     * <LI>userIdentifier - none
     * <LI>userIdentityToCreateSubject - none
     * <LI>mapIdentityToRegistryUser - false
     * <LI>realmName - "" empty
     * <LI>realmIdentifier - badRealmIdent - bad value so default will be ISS
     * </OL>
     * <P>
     * Expected Results
     * <OL>
     * <LI>The default behavior is to use issuer (ISS) for the realmName when the realmName and realmIdentifer are bad and not
     * used.
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - http:localhost:8940/oidc/endpoint/OidcConfigSample
     * <LI>RealmSecurityName - http://localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     * <LI>UniqueSecurityName - testuser
     * <LI>groupIds=[]
     * <LI>accessId=user:http://localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     *
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_bothRealmsBadDefaultsToISS() throws Exception {
        String defaultISSSecurityName = defaultBasicNoGroupUser;
        String defaultISSAccessId = "user:http://" + targetISSEndpoint;
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testRealmsBad");
        generalPositiveTest(updatedTestSettings, defaultISSRealm, defaultBasicNoGroupUser, defaultEmptyGroup, defaultISSSecurityName, defaultISSAccessId);

    }

    /**
     * Test Purpose: Verify when no userIdentity is specified that the userIdentityToCreateSubject is used and a bad
     * type of claim (iat numeric type rather than string type)
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier(not specified)
     * <LI>userIdentityToCreateSubject - iat (wrong type claim for subject creation)
     * <LI>others default
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The userIdentifierToCreateSubject with a value of badClaim results in an error when attempt to access protected
     * resource on the Resource Server.
     * <LI>401 Forbidden
     * <LI>For access token -- Message in RS messages.log: CWWKS1730E: The resource server failed the authentication request
     * because the data type of the [iat] claim in the access token associated with the [userIdentityToCreateSubject]
     * configuration attribute is not valid.
     *
     * For JWT token -- CWWKS1737E: The OpenID Connect client [client01] failed to validate the JSON Web Token. The cause of the
     * error was: [java.lang.ClassCastException: java.lang.Long incompatible with java.lang.String
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_UserIDCreateSubjectBadClaimType_401Error() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUIdSubTypeBad");
        if (Constants.SUPPORTED.equals(updatedTestSettings.getInboundProp())) {
            generalNegativeTest(updatedTestSettings, Constants.UNAUTHORIZED_STATUS, null, null);
        } else {
            String expectedErrorMessage;
            if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
                expectedErrorMessage = MessageConstants.CWWKS1730E_UNABLE_TO_AUTHENTICATE_BADCLAIMTYPE + ".*\\[iat\\]";
            } else {
                expectedErrorMessage = MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE;
            }
            generalNegativeTest(updatedTestSettings, Constants.UNAUTHORIZED_STATUS, FAILED_TO_FIND_MESSAGE, expectedErrorMessage);
        }

    }

    /**
     * Test Purpose: Verify when the userIdentifier is a bad type (iat numeric) that
     * the userIdentityToCreateSubject which defaults to sub is used to create subject.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentifier - claim iat which is wrong type for subject creation
     * <LI>others default
     * <P>
     * Expected Results Successful access to protected app on RS and app displays subject which is validated for the following:
     * <OL>
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>UniqueSecurityName - testuser
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_userIdentifier_badClaimType_DefaultsToSubUsed() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUIdTypeBad");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);

    }

    /**
     * Test Purpose: Verify when no userIdentity is specified that the groupIds is used and a bad
     * type of claim (sub rather than groupIds)
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>groupIdentifier - claim sub specfied which is the wrong type claim for groupIds creation
     * <LI>others default
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * Successful access to protected app on RS and app displays subject which is validated for the following:
     * <LI>The groupIdentifier of wrong claim type defaults to empty group in subject.
     * <LI>SecurityName - sub (user1)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/user1
     * <LI>UniqueSecurityName - user1
     * <LI>groupIds - [group:OPTestRealm/user1]
     * <LI>accessId=user:OPTestRealm/user1
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_groupIdentifier_subClaim_setsGroupIdsToSub() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testgroupIdTypeBad");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasic1GroupUser, defaultBasicGroupOPRealmWithSubAsIdentifier, null, null);

    }

    /**
     * Test Purpose: Verify when the realmIdentifier is specified as a bad type claim (groupsIds)
     * that it defaults to ISS.
     * Note: This test does not apply when JWT token is used.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>realmIdentifier - wrong type groupIds
     * <LI>others default
     * </OL>
     * <P>
     * Expected Results
     * <OL>
     * <LI>The default behavior is to use issuer (ISS) for the realmName when the realmName and realmIdentifer are bad and not
     * used.
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - http:localhost:8940/oidc/endpoint/OidcConfigSample
     * <LI>RealmSecurityName - http://localhost:/8940/oidc/endpoint/OidcConfigSample/testuser
     * <LI>UniqueSecurityName - http://localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     * <LI>groupIds=[]
     * <LI>accessId=user:http://localhost:8940/oidc/endpoint/OidcConfigSample/testuser
     *
     **/
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_realmIdentifier_badClaimType_defaultsToISSInSubject() throws Exception {
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testrealmIdTypeBad");
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
     * <LI>SecurityName - sub (testuser)
     * <LI>RealmName - OP realm (OPTestRealm)
     * <LI>RealmSecurityName - OPTestRealm/testuser
     * <LI>UniqueSecurityName - testuser
     * <LI>groupIds - []
     * <LI>accessId=user:OPTestRealm/testuser
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_unqiueUserId_badClaimTypeError_DefaultsToUniqueSecurityName() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testuniqueIdTypeBad");
        generalPositiveTest(updatedTestSettings, defaultBasicOPRealm, defaultBasicNoGroupUser, defaultEmptyGroup, null, null);

    }

    /**
     * Test Purpose: Verify behavior with when the realmName in the access token is assigned to the ISS, but then try to access
     * an RS protected resource with a client whose issuerIdentifier does not match the ISS in the token.
     * To start, the realmName attribute value is empty and realmIdentifier is also a bad claim value
     * and therefore the realm defaults to the issuerIdentifier. Verify successful access to the RS protected Snoop application
     * with client config
     * that has an issuer identifier that matches that in the token. Next try to access an RS protected resource with the same
     * access
     * token but with a client whose issuerIdentifier in the configuration does NOT match the realm name (ISS) in the access
     * token.
     * The resource server should fail authentication with a 401 error and message.
     * Note: This test is not applicable when the token type is JWT.
     * <OL>
     * <LI>userIdentifier - none
     * <LI>userIdentityToCreateSubject - none
     * <LI>mapIdentityToRegistryUser - false
     * <LI>realmName - "" empty
     * <LI>realmIdentifier - badRealmIdent - bad value so default will be ISS
     * </OL>
     * <P>
     * Expected Results
     * <OL>
     * <LI>The default behavior is to use issuer (ISS) for the realmName when the realmName and realmIdentifer are bad and not
     * used so access token is created with ISS as the realm name.
     * <LI>Successful access protected resource on RS from client using the access token.
     * <LI>401 error when attempt to access protected resource on RS from client whose issuerIdentifier does not match the ISS in
     * the token.
     * <LI>The folloiwng error message is found in the RS messages.log: CWWKS1724E: The resource server failed the authentication
     * request because the issuerIdentifier [https://MyServer] in the configuration does not match the "iss" claim
     * [http://localhost:8940/oidc/endpoint/OidcConfigSample] in the access token.
     *
     **/
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_RealmNameDefaultsToISS_ISSNotMatchIssuerIdentifierError() throws Exception {

        // Authenticate with OP to get an access token
        List<validationData> expectations = vData.addSuccessStatusCodes();
        WebConversation wc = new WebConversation();
        WebResponse response = genericOP(_testName, wc, testSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);
        String originalAccessToken = validationTools.getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY);

        // Access protected RS resource using acess token from OP
        List<validationData> expectations2 = commonConfigTools.setGoodHelloWorldExpectations(testSettings);
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_testMapDflt");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), originalAccessToken, updatedTestSettings, expectations2);

        // Attempt to access RS protected resource from a different client where the issuerIdentifer on the client does not match
        // the realm name (ISS) in the access token
        TestSettings errorTestSettings = testSettings.copyTestSettings();
        errorTestSettings.setClientName("client02");
        errorTestSettings.setClientSecret("secret1234");
        errorTestSettings = rsTools.updateRSProtectedResource(errorTestSettings, "helloworld_testISSErr");

        // Verify authentication fails with 401 error and the RS messages.log contains a message indicating the ISS does not match the issuerIdentifier
        List<validationData> expectations3 = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        if (!Constants.SUPPORTED.equals(updatedTestSettings.getInboundProp())) {
            expectations3 = validationTools.addMessageExpectation(genericTestServer, expectations3, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                    FAILED_TO_FIND_MESSAGE, MessageConstants.CWWKS1724E_UNABLE_TO_AUTHENTICATE_ISSUERBAD + ".*\\[BadServerISSIdent\\]");
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), originalAccessToken, errorTestSettings, expectations3);
    }

    /**
     * Test Purpose: Verify when subject defaults to sub (testuser) but with a RealmName where there is no accessId in
     * the ibm-application-bnd on the RP, that the authorization check will fail at RP.
     * <OL>
     * <LI>mapToUserRegistry -No
     * <LI>userIdentityToCreateSubject defaults to sub (testuser)
     * <LI>realmName (String) - NoAccessId
     * <LI>others default
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Authorization failed error with 403 return code
     * <LI>Message in RS messages.log: CWWKS9104A: Authorization failed for user testuser while invoking helloworld on
     * /rest/helloworld_noAccessIdForRealm. The user is not granted access to any of the required roles: [Employee, Manager].
     * </OL>
     **/
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_RealmNameSpecified_ButNoAccessIdForUserInRealm_403Error() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_noAccessIdForRealm");
        generalNegativeTest(updatedTestSettings, Constants.FORBIDDEN_STATUS, FAILED_TO_FIND_MESSAGE, CWWKS9104A_AUTHORIZATION_FAILED + ".*testuser");

    }

    /******* mapToUserRegistry = Yes (error since there is no registry) *******/
    /**
     * Test Purpose: Verify error behavior when there is no RP registry and mapToUserRegistry=true
     * <OL>
     * <LI>mapToUserRegistry - true
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>403 Forbidden
     * <LI>FFDC -com.ibm.ws.security.registry.RegistryException
     * <LI>Note: this scenario was examined for serviceability because we would have liked to have a message in the messages.log
     * to say that there is no registry configured when map to registry is true. However, we do not get control after base
     * security does this check so we cannot emit a message here. All we get is the FFDC.
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @ExpectedFFDC("com.ibm.ws.security.registry.RegistryException")
    @Test
    public void MapToUserRegistryNoRegIntrospect2ServerTests_NoRegistry_MapTrue_RegistryException() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapErr");
        notfoundExceptionTest(updatedTestSettings);

    }
}
