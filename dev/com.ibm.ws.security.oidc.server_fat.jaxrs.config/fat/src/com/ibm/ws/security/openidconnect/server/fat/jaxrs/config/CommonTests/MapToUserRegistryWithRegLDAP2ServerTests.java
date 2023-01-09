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

/**
 * This is the test class that contains OpenID Connect and OAuth tests for the Resource Server (RS) configuration parameter
 * mapIdentityToRegistryUser=true.
 * The OP accesses an LDAP registry to create the token for the user. Then the validation endpoint (either introspect or userinfo
 * for accesst token or local validation for JWT)
 * is used to validate the token and determine the claim to create the user name based on the userIdentityToCreateSubject or
 * userIdentifier.
 * The RS maps that user name to its LDAP registry (on a different LDAP server) in order to create the subject. The RS LDAP has a
 * different realm from the OP
 * and its realm is used to create the subject.
 * 
 * The OIDC extension of this test uses different scopes to obtain different claims (email, given_name, address) from the actual
 * LDAP registry with userinfo.
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
 * This test depends upon the following users in the LDAP user registry on the OP and on the RS:
 * 
 * oidcg1 (group)
 * oidcu1 (user)
 * oidcg2 (group
 * oidcu1, oidcu2 (users)
 * oidcu3 (no group)
 * testuser (no group)
 * 
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
 * <group name="oidcg1" />
 * </security-role>
 * <security-role name="Manager">
 * <role-name>Manager</role-name>
 * <user name="oidcu2" />
 * <user name="testuser" />
 * <user name="oidcu1First" />
 * </security-role>
 * </application-bnd>
 * </application>
 * 
 **/

public class MapToUserRegistryWithRegLDAP2ServerTests extends MapToRegistryCommonTest {

    private static final Class<?> thisClass = MapToUserRegistryWithRegLDAP2ServerTests.class;
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

    /******* An actual LDAP user registry configured for OP and RS server *******/
    /******* mapIdentityToRegsiteryUser = True *******/
    /******* validation method and URL = userinfo or introspect *******/

    /**
     * Test Purpose: Verify that when the subject is created for a user testuser which is not in any group in the OP LDAP registry
     * and also in
     * the RS user registry in the Manager role to access the RS protected resource, that access is allowed.
     * For this test, the "profile" scope is included in the access token.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject (not specified - defaults to sub = testuser)
     * </OL>
     * <P>
     * Expected Results: The subject is created with the realm and groups from the LDAP user registry on RS.
     * <OL>
     * <LI>WSCredential SecurityName=testuser
     * <LI>WSCredential RealmName=RSLdapIDSRealm
     * <LI>WSCredential RealmSecurityName=RSLdapIDSRealm/testuser
     * <LI>WSCredential UniqueSecurityName=cn=testuser,o=ibm,c=us
     * <LI>WSCredential groupIds=[]
     * <LI>WSCredential accessId=user:RSLdapIDSRealm/cn=testuser,o=ibm,c=us
     * </OL>
     **/
    @Test
    public void MapToUserRegistryWithRegLDAP2ServerTests_SubUserInRSRegistry_UserInManagerRole_AccessRSResource() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("profile");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapDflt");
        generalPositiveTest(updatedTestSettings, defaultRSLdapRealm, defaultLdapNoGroupUser, defaultEmptyGroup, defaultLdapNoGroupUserDN, buildLdapUserAccessId(defaultRSLdapRealm, defaultLdapNoGroupUser, defaultLdapSuffix));

    }

    /**
     * Test Purpose: Verify that when the subject is created for a user oidcu1 (in group oidcg1 with Manager role) in RS registry
     * that
     * that access to the RS protected resource is allowed.
     * For this test, only the "openid" scope is included in the access token.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject (not specified - defaults to sub = oidcu1)
     * </OL>
     * <P>
     * Expected Results: The subject is created with the realm and groups from the LDAP user registry on RS.
     * <OL>
     * <LI>WSCredential SecurityName=oidcu1
     * <LI>WSCredential RealmName=RSLdapIDSRealm
     * <LI>WSCredential RealmSecurityName=RSLdapIDSRealm/oidcu1
     * <LI>WSCredential UniqueSecurityName=cn=oidcu1,o=ibm,c=us
     * <LI>WSCredential groupIds=[group:RSLdapIDSRealm/cn=oidcg1,o=ibm,c=us]
     * <LI>WSCredential accessId=user:RSLdapIDSRealm/cn=oidcu1,o=ibm,c=us
     * </OL>
     **/
    @Test
    public void MapToUserRegistryWithRegLDAP2ServerTests_SubUserInRSRegistry_GroupInManagerRole_AccessRSResource() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid");
        updatedTestSettings.setAdminUser("oidcu1");
        updatedTestSettings.setAdminPswd("security");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapDflt");
        generalPositiveTest(updatedTestSettings, defaultRSLdapRealm, defaultLdap1GroupUser, buildLdapGroupId(defaultRSLdapRealm, defaultLdap1Group, defaultLdapSuffix), defaultLdap1GroupUserDN, buildLdapUserAccessId(defaultRSLdapRealm, defaultLdap1GroupUser, defaultLdapSuffix));

    }

    /**
     * Test Purpose: Verify that when the subject is created for a user oidcu3 which is in the RS registry but does
     * not have the required role (Manager or Employee) to access the RS protected resource, that 403 Authorization failed error
     * occurs.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject (not specified - defaults to sub = oidcu3)
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>403 Forbidden error
     * <LI>Message in RP server messaages.log: CWWKS9104A: Authorization failed for user oidcu3 while invoking helloworld on
     * /rest/helloworld_testMapDflt. The user is not granted access to any of the required roles: [Employee, Manager].
     * </OL>
     **/
    @Test
    public void MapToUserRegistryWithRegLDAP2ServerTests_SubUserInRSRegistry_NotInRole_AuthorizationFailed() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser("oidcu3");
        updatedTestSettings.setAdminPswd("security");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testMapDflt");
        generalNegativeTest(updatedTestSettings, Constants.FORBIDDEN_STATUS, FAILED_TO_FIND_MESSAGE, CWWKS9104A_AUTHORIZATION_FAILED + ".*oidcu3");

    }

}
