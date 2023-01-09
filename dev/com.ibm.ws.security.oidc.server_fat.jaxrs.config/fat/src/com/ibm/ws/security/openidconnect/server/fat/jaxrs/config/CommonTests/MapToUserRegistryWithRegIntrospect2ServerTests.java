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
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSSkipIfJWTToken;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.MapToRegistryCommonTest;
import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains basic OpenID Connect and OAuth tests for Resource Server (RS)
 * configuration parameters below when there is a Basic User Registry in the RS server config. The introspection
 * endpoint for access token and local validation for JWT is used by the RS to validate the token.
 * The test framwork will randomly select either access token or JWT token when running this test.
 * 
 * This test focuses on mapping the user name in the subject to the RS user registry. It does not
 * cover all the negative scenarios for initial subject creation based on the identifiers.
 * 
 * mapIdentityToRegsiteryUser = True
 * 
 * userIdentityToCreateSubject
 * userIdentifier (claim)
 * groupIdentifier (claim)
 * realmName (String)
 * realmIdentifier (claim)
 * uniqueUserIdentifier (claim)
 * 
 * validationMethod - introspect (for access token); local validation for JWT
 * validationEndpoint - introspect endpoint URL
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
 * realmName (String) takes precedence if there, otherwise use realmIdentifier
 * realmIdentifer defaults to realmName claim
 * if realmName is not specified and realmIdentifer is bad, the default is ISS (issuer) claim
 * uniqueUserId defaults to uniqueSecurityName claim
 * 
 * - For map to registry TRUE, check to see if just the user name is in the registry on the RS and if so
 * create subject from registry user on RS. The group information and realm information after the mapping
 * will be the group and realm from the RS user registry.
 * 
 * These tests use a single RS server config file with different OIDC client and filters for
 * the various combinations of configuration parameters.
 * 
 * This test depends upon the following users in the Basic user registry on the RS:
 * testuser - not in any groups on OP user registry but in group1 in RS registry
 * user1 and client02 in group1 in RS registry
 * 
 * client03 must not be in the RS registry. The client03 access-id is specified
 * in the application-bnd file but has no effect on access to the resource.
 * 
 * The Hello World application on the RS server is protected by Manager and Employee role so in order for access to be granted
 * the user must be present in the RS user registry and in the specified role in the application-bnd:
 * 
 * <application type="war" id="helloworld" name="helloworld" location="${server.config.dir}/test-apps/helloworld.war">
 * <application-bnd>
 * <security-role name="Employee">
 * <role-name>Employee</role-name>
 * <group name="group1" />
 * </security-role>
 * <security-role name="Manager">
 * <role-name>Manager</role-name>
 * <user name="testuser" />
 * <user name="client03" access-id="user:OPTestRealm/client03" />
 * </security-role>
 * </application-bnd>
 * </application>
 * 
 **/

public class MapToUserRegistryWithRegIntrospect2ServerTests extends MapToRegistryCommonTest {

    private static final Class<?> thisClass = MapToUserRegistryWithRegIntrospect2ServerTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static String targetProvider = null;
    protected static String targetISSEndpoint = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    /******* A basic user registry configured for RS server *******/
    /******* mapToUserRegistry = Yes *******/
    /******* validation method and URL = introspect *******/

    /**
     * Test Purpose: Verify that when the subject is created for a user testuser, which is not in any group on the OP, but is in
     * the
     * required Manager group in the RS user registry, that access to the RS protected resource is allowed and the subject is
     * created
     * based on RS registry with testuser (in group RSGroup). The user testuser is in the Manager role in the appliation-bnd.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject (not specified - defaults to sub = testuser)
     * <LI>groupIdentifier (not specified - defaults to groupIds)
     * <LI>realmName (not specified - defaults to realmName)
     * <LI>uniqueUserIdentifier (not specified - defaults to uniqueSecurityName)
     * </OL>
     * <P>
     * Expected Results: The subject is created with the realm and groups from the RS user registry.
     * <OL>
     * <LI>WSCredential SecurityName=testuser
     * <LI>WSCredential RealmName=RSTestRealm
     * <LI>WSCredential RealmSecurityName=RSTestRealm/testuser
     * <LI>WSCredential UniqueSecurityName=testuser
     * <LI>WSCredential groupIds=[group:RSTestRealm/RSGroup]
     * <LI>WSCredential accessId=user:RSTestRealm/testuser
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryWithRegIntrospect2ServerTests_WithRegistry_SubUserInRSRegistry_UserInManagerRole_AccessRSResource() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser("testuser");
        updatedTestSettings.setAdminPswd("testuserpwd");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapDflt");
        generalPositiveTest(updatedTestSettings, defaultBasicRPRealm, defaultBasicRSGroupUser, defaultBasicGroupRPRealmManagerGroupID, null, null);

    }

    /**
     * Test Purpose: Verify that when the subject is created for a user diffuser which is in the RP registry but does
     * not have the required role (Manager or Employee) to access the RP protected resource, that 403 Authorization failed error
     * occurs.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject (not specified - defaults to sub = diffuser)
     * <LI>groupIdentifier (not specified - defaults to groupIds)
     * <LI>realmName (not specified - defaults to realmName)
     * <LI>uniqueUserIdentifier (not specified - defaults to uniqueSecurityName)
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>403 Forbidden error
     * <LI>Message in RP server messaages.log: CWWKS9104A: Authorization failed for user diffuser while invoking helloworld on
     * /rest/helloworld_testMapDflt. The user is not granted access to any of the required roles: [Employee, Manager].
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryWithRegIntrospect2ServerTests_WithRegistry_SubUserInRSRegistry_NotInRole_AuthorizationFailed() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser("diffuser");
        updatedTestSettings.setAdminPswd("diffuserpwd");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapDflt");
        generalNegativeTest(updatedTestSettings, Constants.FORBIDDEN_STATUS, FAILED_TO_FIND_MESSAGE, CWWKS9104A_AUTHORIZATION_FAILED + ".*diffuser");

    }

    /**
     * Test Purpose: Verify that when the subject is created based on client_id claim (client03) and that user client03
     * does not exist in the RS user registry that an error 403 Authentication failed.
     * Note: This test does not apply when JWT token is used because the JWT token does not contain a client_id and validation
     * will fail.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject="client_id"
     * <LI>uniqueUserIdentifier="client_id"
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>403 Authentication failed
     * <LI>Message in RP server messaages.log: CWWKS1106A: Authentication did not succeed for the user ID client03. An invalid
     * user ID was specified
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @AllowedFFDC("com.ibm.ws.security.registry.EntryNotFoundException")
    @Test
    public void MapToUserRegistryWithRegIntrospect2ServerTests_WithRegistry_ClientIdUser_NotInRSRegistry_AuthenticationFailed() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("client03");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUserClient03");
        generalNegativeTest(updatedTestSettings, Constants.FORBIDDEN_STATUS, FAILED_TO_FIND_MESSAGE, CWWKS1106A_AUTHENTICATION_FAILED + ".*client03");

    }

    /**
     * Test Purpose: Verify that when the subject is created based on sub and the realm defaults to Issuer identifier (ISS) that
     * the RS resource can be accessed because the user id is in the RS user registry and the realm value of ISS is not
     * used in the subject creation.
     * 
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject defaults to sub (testuser)
     * <LI>realmName=""
     * <LI>realmIdentifier="badRealmIdent" Realm defaults to ISS since realmName is empty and realmIdentifier is bad.
     * </OL>
     * <P>
     * Expected Results: The subject is created with the realm and groups from the RS user registry.
     * <OL>
     * <LI>WSCredential SecurityName=testuser
     * <LI>WSCredential RealmName=RSTestRealm
     * <LI>WSCredential RealmSecurityName=RSTestRealm/testuser
     * <LI>WSCredential UniqueSecurityName=testuser
     * <LI>WSCredential groupIds=[group:RSTestRealm/RSGroup]
     * <LI>WSCredential accessId=user:RSTestRealm/testuser
     * </OL>
     **/

    @Test
    public void MapToUserRegistryWithRegIntrospect2ServerTests_WithRegistry_SubUserWithISSForRealmInRSRegistry_UserInRole_AccessRSResource() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser("testuser");
        updatedTestSettings.setAdminPswd("testuserpwd");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_realmISS");
        generalPositiveTest(updatedTestSettings, defaultBasicRPRealm, defaultBasicRSGroupUser, defaultBasicGroupRPRealmManagerGroupID, null, null);

    }

    /**
     * Test Purpose: Verify that when the subject is created based on client_id claim (client02) and that user client02
     * exists in the RP registry and its group (group1) is in the role required to access the RP protected resource, that access
     * is granted.
     * Note: This test does not apply when JWT token is used because the JWT token does not contain a client_id and validation
     * will fail.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject="client_id"
     * <LI>uniqueUserIdentifier="client_id"
     * </OL>
     * <P>
     * Expected Results: The subject is created with the realm and groups from the RS user registry.
     * <OL>
     * <LI>WSCredential SecurityName=client02
     * <LI>WSCredential RealmName=RSTestRealm
     * <LI>WSCredential RealmSecurityName=RSTestRealm/client02
     * <LI>WSCredential UniqueSecurityName=client02
     * <LI>WSCredential groupIds=[group:RSTestRealm/group1]
     * <LI>WSCredential accessId=user:RSTestRealm/client02
     * </OL>
     **/

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    public void MapToUserRegistryWithRegIntrospect2ServerTests_WithRegistry_ClientIdUserInRSRegistry_GroupInEmployeeRole_AccessRSResource() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("client02");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUserClient02");
        generalPositiveTest(updatedTestSettings, defaultBasicRPRealm, defaultBasicClient02GroupUser, defaultBasicGroupRPRealmClient02GroupID, null, null);

    }

    /**
     * Test Purpose: Verify default behavior with specification of the maximum set of config parmameters are
     * specified with mapping to the RS user registry.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentifier - sub
     * <LI>userIdentityToCreateSubject - sub
     * <LI>groupIdentifier - groupIds claim
     * <LI>realmIdentifier - realmName claim
     * <LI>realmName (String) MyRealm
     * <LI>uniqueUserIdentifier - uniqueSecurityName
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * The subject is created with the realm and groups from the RS user registry.
     * <LI>WSCredential SecurityName=user1
     * <LI>WSCredential RealmName=RSTestRealm
     * <LI>WSCredential RealmSecurityName=RSTestRealm/user1
     * <LI>WSCredential UniqueSecurityName=user1
     * <LI>WSCredential groupIds=[group:RSTestRealm/group1]
     * <LI>WSCredential accessId=user:RSTestRealm/user1
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryWithRegIntrospect2ServerTests_WithRegistry_AllIdentifiersSpecifiedUser_GroupInEmployeeRole_AccessRSResource() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser(defaultBasic1GroupUser);
        updatedTestSettings.setAdminPswd(defaultBasic1GroupUserPwd);
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMax");
        generalPositiveTest(updatedTestSettings, defaultBasicRPRealm, defaultBasic1GroupUser, defaultBasicGroupRPRealmGroupID, null, null);

    }
}
