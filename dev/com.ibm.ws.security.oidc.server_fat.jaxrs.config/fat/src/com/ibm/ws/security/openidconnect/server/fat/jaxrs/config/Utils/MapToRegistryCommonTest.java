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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils;

import java.util.List;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

public class MapToRegistryCommonTest extends CommonTest {

    private static final Class<?> thisClass = MapToRegistryCommonTest.class;
    protected static CommonTools commonConfigTools = new CommonTools();
    protected static String[] goodActions = null;

    public ValidationData vData = new ValidationData();

    public static final String FAILED_TO_FIND_MESSAGE = "Failed to find expected message in RS messages.log: ";

    String myMethod = "MyMethod";

    // Message constants
    public static final String CWWKS9104A_AUTHORIZATION_FAILED = "CWWKS9104A";
    public static final String CWWKS1106A_AUTHENTICATION_FAILED = "CWWKS1106A";

    // WSCredential constants
    public static final String TYPE_USER = "user";
    public static final String TYPE_GROUP = "group";
    public static final String TYPE_SEPARATOR = ":";
    public static final String REALM_SEPARATOR = "/";

    // Subject validation constants
    public String defaultBasicOPRealm = "OPTestRealm";
    public String defaultBasicRPRealm = "RSTestRealm";
    public String defaultRSLdapRealm = "RSLdapIDSRealm";
    public String nullRealm = "null";
    public String defaultSpecifiedRealm = "MyRealm";
    public static String defaultISSRealm = "http:";
    public String defaultLdapCn = "cn=";
    public String defaultLdapSuffix = ",o=ibm,c=us";

    public String defaultBasicNoGroupUser = "testuser";
    public String defaultBasicNoGroupUserPw = "security";
    public String defaultEmptyGroup = "";

    public String defaultLdapNoGroupUser = "testuser";
    public String defaultLdapNoGroupUserDN = "cn=" + defaultLdapNoGroupUser + defaultLdapSuffix;
    public String defaultLdapNoGroupUserPw = "testuserpwd";

    public String defaultBasic1GroupUser = "user1";
    public String defaultBasic1GroupUserPwd = "user1pwd";
    public String defaultBasicGroupOPRealmGroupID = "group:" + defaultBasicOPRealm + "/group1";
    public String defaultBasicGroupOPRealmWithSubAsIdentifier = "group:" + defaultBasicOPRealm + "/user1";
    public String defaultBasicGroupSpecifiedRealmGroupID = "group:" + defaultSpecifiedRealm + "/group1";
    public String defaultBasicGroupRPRealmGroupID = "group:" + defaultBasicRPRealm + "/group1";

    public String defaultLdap1GroupUser = "oidcu1";
    public String defaultLdap1GroupUserPwd = "security";
    public String defaultLdap1Group = "oidcg1";
    public String defaultLdap1GroupUserDN = "cn=" + defaultLdap1GroupUser + defaultLdapSuffix;

    public String defaultBasicRSGroupUser = "testuser";
    public String defaultBasicGroupRPRealmManagerGroupID = "group:" + defaultBasicRPRealm + "/RSGroup";

    public String defaultMismatchRSGroupUser = "oidcu1";
    public String defaultMismatchGroupRPRealmManagerGroupID = "group:" + defaultBasicRPRealm + "/RSGroup";

    public String defaultMismatchGivenNameUser = "oidcu1First";

    public String defaultBasicClient02GroupUser = "client02";
    public String defaultBasicGroupRPRealmClient02GroupID = "group:" + defaultBasicRPRealm + "/group1";

    public String defaultBasicClient04GroupUser = "client04";
    public String defaultBasicGroupRPRealmClient04GroupID = "group:" + defaultBasicRPRealm + "/group1";

    /********************** Helper methods *************************/
    /**
     * Build expectations to validate the values in the Public credential - WSCredential for
     * the Subject for realmName, realmSecurityName, securityName, accessId and
     * groupIds.
     *
     * @param expectations
     * @param realmName
     * @param securityName
     * @param groupIds
     * @return expectations with added validation checks
     * @throws Exception
     */
    public List<validationData> addIdentityInfoExpectations(
            List<validationData> expectations, String realmName,
            String securityName, String groupName, String uniqueSecurityName, String accessIdName) throws Exception {

        String realmSearchString = "WSCredential RealmName=" + realmName;
        String realmSecurityNameSearchString = "WSCredential RealmSecurityName=" + realmName + "/" + securityName;
        String securityNameSearchString = "WSCredential SecurityName=" + securityName;

        String uniqueSecurityNameString;
        if (uniqueSecurityName != null) {
            uniqueSecurityNameString = "WSCredential UniqueSecurityName=" + uniqueSecurityName;
        } else {
            uniqueSecurityNameString = "WSCredential UniqueSecurityName=" + securityName;
        }

        String accessIdSearchString;
        if (accessIdName != null) {
            accessIdSearchString = "WSCredential accessId=" + accessIdName;
        } else {
            accessIdSearchString = "WSCredential accessId=user:" + realmName + "/" + securityName;
        }

        String groupSearchString = "WSCredential groupIds=[" + groupName + "]";

        expectations = vData.addExpectation(expectations,
                Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did not receive the expected realm name settings.", null,
                realmSearchString);
        expectations = vData.addExpectation(expectations,
                Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did not receive the expected realm security name settings.",
                null, realmSecurityNameSearchString);
        expectations = vData.addExpectation(expectations,
                Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did not receive the expected security name settings.", null,
                securityNameSearchString);
        expectations = vData.addExpectation(expectations,
                Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did not receive the expected group settings.", null,
                groupSearchString);
        expectations = vData.addExpectation(expectations,
                Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did not receive the expected accessId settings.", null,
                accessIdSearchString);
        expectations = vData.addExpectation(expectations,
                Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Did not receive the expected unique security name settings.",
                null, uniqueSecurityNameString);

        return expectations;
    }

    /**
     * Performs the common steps of a positive test:
     * - Access OP for authorization grant
     * - Access OP server for access token - Use access
     * - Use access token token to access a protected resource (snoop) on the OP server - Use the
     * - Use same access token to access a protected resource on a separate Resource Server (RS)
     *
     * Caller passes in values specific to its test that should be
     * validated in the resulting subject.
     *
     * @param testSettings
     *
     * @param expectedRealm
     * @param expectedUser
     * @param expectedGroup
     * @param expectedUniqueSecurityName
     * @param expectedUniqueAccessId
     * @throws Exception
     */
    public void generalPositiveTest(TestSettings testSettings,
            String expectedRealm, String expectedUser, String expectedGroup, String expectedUniqueSecurityName, String expectedUniqueAccessId) throws Exception {

        List<validationData> expectations = commonConfigTools.setGoodHelloWorldExpectations(testSettings);
        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, testSettings, goodActions, addIdentityInfoExpectations(expectations, expectedRealm, expectedUser, expectedGroup, expectedUniqueSecurityName, expectedUniqueAccessId));
    }

    /**
     * Performs the common steps of a negative test which expects an error
     * - Access OP for authorization grant
     * - Access OP server for access token - Use access
     * - Use access token token to access a protected resource (snoop) on the OP server - Use the
     * - Use same access token to access a protected resource on a separate Resource Server (RS) and
     * expect an error status code which is passed in along with the expected logMessage in the RS messages.log
     * and the failure message to print if the logMessage is not found.
     *
     * Returns 200 status code expectations for all actions except for invoking the protected resource on the Resource Server.
     * Sets a 401 status code expectation
     * for the INVOKE_RS_PROTECTED_RESOURCE invocation and will look for logMessage in the RS server's messages.log file,
     * outputting failureMessage
     * if logMessage is not found.
     *
     * @param testSettings
     * @param expectedStatusCode
     *            (401 exception, 403, or other status code)
     * @param failureMessage
     * @param logMessage
     * @throws Exception
     */
    public void generalNegativeTest(TestSettings testSettings, int expectedStatusCode, String failureMessage, String logMessage) throws Exception {

        List<validationData> expectations = commonConfigTools.setGoodHelloWorldExpectations(testSettings);
        expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_RS_PROTECTED_RESOURCE);

        if (expectedStatusCode == Constants.UNAUTHORIZED_STATUS) {
            // process exception received for 401
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);

        } else {
            // process return code from 403 or other status
            expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, expectedStatusCode);
        }
        if (failureMessage != null) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                    failureMessage, logMessage);
        }

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, testSettings, goodActions, expectations);
    }

    /**
     * Performs the common steps of a not found exception type test - - Access
     * OP for authorization grant - Access OP server for access token - Use
     * access token to access a protected resource (snoop) on the OP server -
     * Attempt to access protected resource on Resource Server (RS) but expect
     * 403 error along with messages in the messages.log.
     *
     * These tests expect a failure due to a user or registry not being found.
     *
     * @param testSettings
     *            - testSettings for the test being executed
     * @throws Exception
     */
    public void notfoundExceptionTest(TestSettings testSettings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.FORBIDDEN_STATUS);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, testSettings, goodActions, expectations);

    }

    /**
     * Builds LDAP user access ID string based on realm, user and suffix
     *
     * @param realm
     *            - LDAP realm name
     * @param user
     *            - LDAP short user name
     * @param suffix
     *            - LDAP suffix
     */
    public String buildLdapUserAccessId(String realm, String user, String suffix) {
        String ldapAccessId;
        return ldapAccessId = TYPE_USER + TYPE_SEPARATOR + realm + REALM_SEPARATOR + defaultLdapCn + user + suffix;

    }

    /**
     * Builds LDAP group string based on realm, group and suffix
     *
     * @param realm
     *            - LDAP realm name
     * @param user
     *            - LDAP group name
     * @param suffix
     *            - LDAP suffix
     */
    public String buildLdapGroupId(String realm, String group, String suffix) {

        String ldapGroupId;
        return ldapGroupId = TYPE_GROUP + TYPE_SEPARATOR + realm + REALM_SEPARATOR + defaultLdapCn + group + suffix;

    }

}
