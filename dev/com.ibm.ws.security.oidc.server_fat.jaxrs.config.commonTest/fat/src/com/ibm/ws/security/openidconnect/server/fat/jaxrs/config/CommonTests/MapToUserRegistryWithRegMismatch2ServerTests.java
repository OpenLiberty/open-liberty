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
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.MapToRegistryCommonTest;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains OpenID Connect and OAuth tests for the Resource Server (RS) configuration parameter
 * mapIdentityToRegistryUser=true.
 * The OP accesses an LDAP registry to create the token for the user who logs in. Then the validation endpoint (either introspect
 * or userinfo for access token / local validation for JWT)
 * is used to validate the token and determine the claim to create the user name based on the userIdentityToCreateSubject or
 * userIdentifier.
 * The RS maps that user name to its Basic registry in order to create the subject and determine the realm name and groups.
 * 
 * The OIDC extension of this test includes a variation for validation with the userinfo endpoint for access token.
 * 
 * This test focuses on mapping the user name in the subject to the RS user registry. It does not cover all the negative scenarios
 * for initial subject
 * creation based on the identifiers as those are covered in MapToUserRegistryNoRegIntrospect2ServerTests.
 * 
 * mapIdentityToRegsiteryUser = True
 * 
 * userIdentityToCreateSubject (defaults to sub and used when userIdentifier is not present)
 * userIdentifier (if present this claim is used as the user name)
 * 
 * The configuration values below are not used in subject creation because the subject is based on what is in the RS user
 * registry.
 * groupIdentifier (claim)
 * realmName (String)
 * realmIdentifier (claim)
 * uniqueUserIdentifier (claim)
 * 
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
 * - For map to registry TRUE, check to see if just the user name is in the registry on the RS and if so
 * create subject from registry user on RS. Verify that the group, realm and uniqueUserID after the mapping
 * are the group, realm and uniqueID from the RS user registry.
 * 
 * These tests use a single RS server config file with different OIDC client and filters for
 * the various combinations of configuration parameters.
 * 
 * Dependencies:
 * 
 * This test depends upon the following users in the LDAP user registry on the OP:
 * 
 * oidcg1 (group)
 * oidcu1 (user)
 * oidcg2 (group
 * oidcu1, oidcu2 (users)
 * oidcu3 (no group)
 * testuser (no group)
 * 
 * 
 * This test depends upon the following users in the Basic user registry on the RS:
 * RSGroup (group)
 * oidcu1
 * oidcu1First (no group)
 * oidcu2 (no group)
 * 
 * 
 * The Hello World application on the RS server is protected by Manager and Employee role so in order for access to be granted
 * the user must be present in the RS user registry and in the specified role shown in the application-bnd:
 * 
 * <application type="war" id="helloworld" name="helloworld" location="${server.config.dir}/test-apps/helloworld.war">
 * <application-bnd>
 * <security-role name="Employee">
 * <role-name>Employee</role-name>
 * <group name="RSGroup" />
 * <group name="oidcg2" />
 * </security-role>
 * <security-role name="Manager">
 * <role-name>Manager</role-name>
 * <user name="testuser" />
 * <user name="oidcu1First" />
 * </security-role>
 * </application-bnd>
 * </application>
 * 
 **/

public class MapToUserRegistryWithRegMismatch2ServerTests extends MapToRegistryCommonTest {

    private static final Class<?> thisClass = MapToUserRegistryWithRegMismatch2ServerTests.class;
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
    /******* mapIdentityToRegsiteryUser = True *******/
    /******* validation method and URL = introspect and userinfo *******/

    /**
     * Test Purpose: Verify that when the subject is created for a user oidcu1, which is not in the required role based on OP
     * group in LDAP (oidcg1), but is in the
     * required Manager group based on the RS Basic user registry group (RSGroup), that access to the RS protected resource is
     * allowed and the subject is created
     * based on RS registry with oidcu1 (in group RSGroup).
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject (not specified - defaults to sub = oidcu1)
     * </OL>
     * <P>
     * Expected Results: The subject is created with the realm and groups from the RS Basic user registry.
     * <OL>
     * <LI>WSCredential SecurityName=oidcu1
     * <LI>WSCredential RealmName=RSTestRealm
     * <LI>WSCredential RealmSecurityName=RSTestRealm/oidcu1
     * <LI>WSCredential UniqueSecurityName=testuser
     * <LI>WSCredential groupIds=[group:RSTestRealm/RSGroup]
     * <LI>WSCredential accessId=user:RSTestRealm/oidcu1
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryWithRegMismatch2ServerTest_GroupInEmployeeRole_AccessRSResource() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapDflt");
        generalPositiveTest(updatedTestSettings, defaultBasicRPRealm, defaultMismatchRSGroupUser, defaultMismatchGroupRPRealmManagerGroupID, null, null);

    }

    /**
     * Test Purpose: Verify that when the subject is created for a user oidcu2 it disregards the group (oidcg2) from the LDAP
     * registry on the OP where
     * the token was created. Instead with mapIdentityToRegistryUser=true it uses the RS registry to determine the group for the
     * subject. In the RS registry, the
     * user oidcu2 does NOT have the required role (Manager or Employee) to access the RP protected resource and a 403
     * Authorization failed error occurs.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject (not specified - defaults to sub = oidcu2)
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>403 Forbidden error
     * <LI>Message in RS server messaages.log: CWWKS9104A: Authorization failed for user oidcu2 while invoking helloworld on
     * /rest/helloworld_testMapDflt. The user is not granted access to any of the required roles: [Employee, Manager].
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void MapToUserRegistryWithRegMismatch2ServerTest_SubUserInRSRegistry_NotInRole_AuthorizationFailed() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser("oidcu2");
        updatedTestSettings.setAdminPswd("security");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapDflt");
        generalNegativeTest(updatedTestSettings, Constants.FORBIDDEN_STATUS, FAILED_TO_FIND_MESSAGE, CWWKS9104A_AUTHORIZATION_FAILED + ".*oidcu2");

    }

    /**
     * Test Purpose: Verify that when the user identifier (oidcu3) from the sub claim exists in the OP LDAP registry, but not in
     * the RS Basic registry that
     * access to the RS protected resource will fail when mapIdentityToRegistryUser is true because the user cannot be
     * authenticated by mapping to RS registry.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject (not specified - defaults to sub = oidcu3)
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Error 403: AuthenticationFailed
     * <LI>Message in RS server messaages.log: CWWKS1106A: Authentication did not succeed for the user ID oidcu3. An invalid user
     * ID was specified FFDC with EntryNotFoundException.
     * </OL>
     **/
    @Test
    @ExpectedFFDC("com.ibm.ws.security.registry.EntryNotFoundException")
    public void MapToUserRegistryWithRegMismatch2ServerTest_SubUserNotInRSRegistry_AuthenticationFailed() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser("oidcu3");
        updatedTestSettings.setAdminPswd("security");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapDflt");
        generalNegativeTest(updatedTestSettings, Constants.FORBIDDEN_STATUS, FAILED_TO_FIND_MESSAGE, CWWKS1106A_AUTHENTICATION_FAILED + ".*oidcu3");

    }
}
