/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.jaxrs.config.IDPInitiated;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.saml.fat.jaxrs.config.utils.RSSamlConfigSettings;
import com.ibm.ws.security.saml.fat.jaxrs.config.utils.RSSamlProviderSettings;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link to an
 * application installed on a WebSphere SP. When the Web client invokes the SP
 * application, it is redirected to a TFIM IdP which issues a login challenge to
 * the Web client. The Web Client fills in the login form and after a successful
 * login, receives a SAML 2.0 token from the TFIM IdP. The client invokes the SP
 * application by sending the SAML 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RSSamlIDPInitiatedMapToUserRegistryConfigTests extends RSSamlIDPInitiatedConfigCommonTests {

    /*****************************************
     * TESTS
     **************************************/

    /*
     * This test verifies the realm, user and group that are in the subject. The
     * values are set based on several configuration options. These tests will
     * modify the configuration and then check for the appropriate values in the
     * subject.
     */

    /******* mapToUserRegistry = No *******/

    /********** identifiers are Good **********/

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: IDP server's realm User: user1 Group:
     * Default IDP groups
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_inRegistry_identifiersGood() throws Exception {

        RSSamlConfigSettings updatedRsConfigSettings = copyDefaultSettingsForMappingTest(REGISTRY_WITHOUT_IDP_USERS);

        RSSamlProviderSettings updatedProviderSettings = updatedRsConfigSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setMapToUserRegistry("No");

        updateConfigFile(testAppServer, baseSamlServerConfig, updatedRsConfigSettings, testServerConfigFile);

        generalPositiveTest(testServerConfigFile, defaultIDPRealm, default2ServerUser, defaultIDPGroup);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: IDP server's realm User: user1 Group:
     * Default IDP groups
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_noRegistry_identifiersGood() throws Exception {

        RSSamlConfigSettings updatedRsConfigSettings = copyDefaultSettingsForMappingTest(NO_REGISTRY);

        RSSamlProviderSettings updatedProviderSettings = updatedRsConfigSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setMapToUserRegistry("No");

        updateConfigFile(testAppServer, baseSamlServerConfig, updatedRsConfigSettings, testServerConfigFile);

        generalPositiveTest(testServerConfigFile, defaultIDPRealm, default2ServerUser, defaultIDPGroup);

    }

    /********** identifiers are Omitted **********/

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = True
     * userIdentifier = Omitted userUniqueIdentifier = Omitted groupIdentfier =
     * Omitted realmIdentifier = Omitted
     *
     * Expected/checked values: Realm: Local server's configured realm User:
     * user1 Group: null
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_inRegistry_identifiersOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_inRegistry_identifiersOmitted.xml", default2ServerCfgRealm, default2ServerUser, defaultNullGroup);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = False
     * userIdentifier = Omitted userUniqueIdentifier = Omitted groupIdentfier =
     * Omitted realmIdentifier = Omitted
     *
     * Expected/checked values: Realm: Local server's configured realm User:
     * user1 Group: null
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_noRegistry_identifiersOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_noRegistry_identifiersOmitted.xml", default2ServerCfgRealm, default2ServerUser, defaultNullGroup);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = Omitted realmIdentifier = valid value
     *
     * Expected/checked values: Realm: IDP server's realm User: user1 Group:
     * null
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_inRegistry_groupIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_inRegistry_groupIdentifierOmitted.xml", defaultIDPRealm, default2ServerUser, defaultNullGroup);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = Omitted realmIdentifier = valid value
     *
     * Expected/checked values: Realm: IDP server's realm User: user1 Group:
     * null
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_noRegistry_groupIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_noRegistry_groupIdentifierOmitted.xml", defaultIDPRealm, default2ServerUser, defaultNullGroup);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = Omitted
     *
     * Expected/checked values: Realm: Local server's configured realm User:
     * user1 Group: Local server's configured groups
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_inRegistry_realmIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_inRegistry_realmIdentifierOmitted.xml", default2ServerCfgRealm, default2ServerUser, default2ServerCfgGroups);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = Omitted
     *
     * Expected/checked values: Realm: Local server's configured realm User:
     * user1 Group: Local server's configured groups
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_noRegistry_realmIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_noRegistry_realmIdentifierOmitted.xml", default2ServerCfgRealm, default2ServerUser, default2ServerCfgGroups);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = True
     * userIdentifier = Omitted userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: IDP server's realm User: user1 Group:
     * Default IDP groups
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_inRegistry_userIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_inRegistry_userIdentifierOmitted.xml", defaultIDPRealm, default2ServerUser, defaultIDPGroup);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = False
     * userIdentifier = Omitted userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: IDP server's realm User: user1 Group:
     * Default IDP groups
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_noRegistry_userIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_noRegistry_userIdentifierOmitted.xml", defaultIDPRealm, default2ServerUser, defaultIDPGroup);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = Omitted
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: IDP server's realm User: user1 Group:
     * Default IDP groups
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_inRegistry_userUniqueIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_inRegistry_userUniqueIdentifierOmitted.xml", defaultIDPRealm, default2ServerUser, defaultIDPGroup);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = Omitted
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: IDP server's realm User: user1 Group:
     * Default IDP groups
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_noRegistry_userUniqueIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_noRegistry_userUniqueIdentifierOmitted.xml", defaultIDPRealm, default2ServerUser, defaultIDPGroup);

    }

    /********** identifiers are Bad **********/

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = BAD value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: IDP server's realm User: user1 Group:
     * null
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_inRegistry_groupIdentifierBad() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_inRegistry_groupIdentifierBad.xml", defaultIDPRealm, default2ServerUser, defaultNullGroup);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = BAD value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: IDP server's realm User: user1 Group:
     * null
     */
    // skip test if using external ldap (some mappings are different)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalLDAP.class)
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_noRegistry_groupIdentifierBad() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_False_noRegistry_groupIdentifierBad.xml", defaultIDPRealm, default2ServerUser, defaultNullGroup);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = BAD value
     *
     * Expected/checked values: Bad Attribute exception - looking for "realm" in
     * the message
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_inRegistry_realmIdentifierBad() throws Exception {

        badAttrValueTest("server_2_caller_mapUserIdentifierToRegistryUser_False_inRegistry_realmIdentifierBad.xml", realmString);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = BAD value
     *
     * Expected/checked values: Bad Attribute exception - looking for "realm" in
     * the message
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_noRegistry_realmIdentifierBad() throws Exception {

        badAttrValueTest("server_2_caller_mapUserIdentifierToRegistryUser_False_noRegistry_realmIdentifierBad.xml", realmString);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = True
     * userIdentifier = BAD value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Bad Attribute exception - looking for
     * "user name" in the message
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_inRegistry_userIdentifierBad() throws Exception {

        badAttrValueTest("server_2_caller_mapUserIdentifierToRegistryUser_False_inRegistry_userIdentifierBad.xml", userNameString);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = False
     * userIdentifier = BAD value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Bad Attribute exception - looking for
     * "user name" in the message
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_noRegistry_userIdentifierBad() throws Exception {

        badAttrValueTest("server_2_caller_mapUserIdentifierToRegistryUser_False_noRegistry_userIdentifierBad.xml", userNameString);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = BAD value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Bad Attribute exception - looking for
     * "unique user name" in the message
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_inRegistry_userUniqueIdentifierBad() throws Exception {

        badAttrValueTest("server_2_caller_mapUserIdentifierToRegistryUser_False_inRegistry_userUniqueIdentifierBad.xml", uniqueUserNameString);

    }

    /*
     * config settings: mapToUserRegistry = No is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = BAD value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Bad Attribute exception - looking for
     * "unique user name" in the message
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_No_noRegistry_userUniqueIdentifierBad() throws Exception {

        badAttrValueTest("server_2_caller_mapUserIdentifierToRegistryUser_False_noRegistry_userUniqueIdentifierBad.xml", uniqueUserNameString);

    }

    /******* mapToUserRegistry = User *******/

    /********** identifiers are Good **********/

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: Local server's realm User: user1 Group:
     * Local server's group
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_inRegistry_identifiersGood() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_True_inRegistry_identifiersGood.xml", defaultLocalRealm, default2ServerUser, default2ServerGroup);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: EntryNotFoundException expected
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_noRegistry_identifiersGood() throws Exception {

        notfoundExceptionTest("server_2_caller_mapUserIdentifierToRegistryUser_True_noRegistry_identifiersGood.xml");

    }

    /********** identifiers are Omitted **********/

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = True
     * userIdentifier = Omitted userUniqueIdentifier = Omitted groupIdentfier =
     * Omitted realmIdentifier = Omitted
     *
     * Expected/checked values: Realm: Local server's realm User: user1 Group:
     * Local server's group
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_inRegistry_identifiersOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_True_inRegistry_identifiersOmitted.xml", defaultLocalRealm, default2ServerUser, default2ServerGroup);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = False
     * userIdentifier = Omitted userUniqueIdentifier = Omitted groupIdentfier =
     * Omitted realmIdentifier = Omitted
     *
     * Expected/checked values: EntryNotFoundException expected
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_noRegistry_identifiersOmitted() throws Exception {

        notfoundExceptionTest("server_2_caller_mapUserIdentifierToRegistryUser_True_noRegistry_identifiersOmitted.xml");

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = Omitted realmIdentifier = valid value
     *
     * Expected/checked values: Realm: Local server's realm User: user1 Group:
     * Local server's group
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_inRegistry_groupIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_True_inRegistry_groupIdentifierOmitted.xml", defaultLocalRealm, default2ServerUser, default2ServerGroup);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = Omitted realmIdentifier = valid value
     *
     * Expected/checked values: EntryNotFoundException expected
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_noRegistry_groupIdentifierOmitted() throws Exception {

        notfoundExceptionTest("server_2_caller_mapUserIdentifierToRegistryUser_True_noRegistry_groupIdentifierOmitted.xml");

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = Omitted
     *
     * Expected/checked values: Realm: Local server's realm User: user1 Group:
     * Local server's group
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_inRegistry_realmIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_True_inRegistry_realmIdentifierOmitted.xml", defaultLocalRealm, default2ServerUser, default2ServerGroup);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = Omitted
     *
     * Expected/checked values: EntryNotFoundException expected
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_noRegistry_realmIdentifierOmitted() throws Exception {

        notfoundExceptionTest("server_2_caller_mapUserIdentifierToRegistryUser_True_noRegistry_realmIdentifierOmitted.xml");

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = True
     * userIdentifier = Omitted userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: Local server's realm User: user1 Group:
     * Local server's group
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_inRegistry_userIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_True_inRegistry_userIdentifierOmitted.xml", defaultLocalRealm, default2ServerUser, default2ServerGroup);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = False
     * userIdentifier = Omitted userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: EntryNotFoundException expected
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_noRegistry_userIdentifierOmitted() throws Exception {

        notfoundExceptionTest("server_2_caller_mapUserIdentifierToRegistryUser_True_noRegistry_userIdentifierOmitted.xml");

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = Omitted
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: Local server's realm User: user1 Group:
     * Local server's group
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_inRegistry_userUniqueIdentifierOmitted() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_True_inRegistry_userUniqueIdentifierOmitted.xml", defaultLocalRealm, default2ServerUser, default2ServerGroup);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = Omitted
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: EntryNotFoundException expected
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_noRegistry_userUniqueIdentifierOmitted() throws Exception {

        notfoundExceptionTest("server_2_caller_mapUserIdentifierToRegistryUser_True_noRegistry_userUniqueIdentifierOmitted.xml");

    }

    /********** identifiers are Bad **********/

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = BAD value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: Local server's realm User: user1 Group:
     * Local server's group
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_inRegistry_groupIdentifierBad() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_True_inRegistry_groupIdentifierBad.xml", defaultLocalRealm, default2ServerUser, default2ServerGroup);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = BAD value realmIdentifier = valid value
     *
     * Expected/checked values: EntryNotFoundException expected
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_noRegistry_groupIdentifierBad() throws Exception {

        notfoundExceptionTest("server_2_caller_mapUserIdentifierToRegistryUser_True_noRegistry_groupIdentifierBad.xml");

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = BAD value
     *
     * Expected/checked values: Realm: Local server's realm User: user1 Group:
     * Local server's group
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_inRegistry_realmIdentifierBad() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_True_inRegistry_realmIdentifierBad.xml", defaultLocalRealm, default2ServerUser, default2ServerGroup);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = BAD value
     *
     * Expected/checked values: EntryNotFoundException expected
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_noRegistry_realmIdentifierBad() throws Exception {

        notfoundExceptionTest("server_2_caller_mapUserIdentifierToRegistryUser_True_noRegistry_realmIdentifierBad.xml");

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = True
     * userIdentifier = BAD value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Bad Attribute exception - looking for
     * userIdentifier in the message
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_inRegistry_userIdentifierBad() throws Exception {

        badAttrValueTest("server_2_caller_mapUserIdentifierToRegistryUser_True_inRegistry_userIdentifierBad.xml", userNameString);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = False
     * userIdentifier = BAD value userUniqueIdentifier = valid value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Bad Attribute exception - looking for
     * userIdentifier in the message
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_noRegistry_userIdentifierBad() throws Exception {

        badAttrValueTest("server_2_caller_mapUserIdentifierToRegistryUser_True_noRegistry_userIdentifierBad.xml", userNameString);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = True
     * userIdentifier = valid value userUniqueIdentifier = BAD value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: Realm: Local server's realm User: user1 Group:
     * Local server's group
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_inRegistry_userUniqueIdentifierBad() throws Exception {

        generalPositiveTest("server_2_caller_mapUserIdentifierToRegistryUser_True_inRegistry_userUniqueIdentifierBad.xml", defaultLocalRealm, default2ServerUser, default2ServerGroup);

    }

    /*
     * config settings: mapToUserRegistry = User is User in the Registry = False
     * userIdentifier = valid value userUniqueIdentifier = BAD value
     * groupIdentfier = valid value realmIdentifier = valid value
     *
     * Expected/checked values: EntryNotFoundException expected
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void RSSamlIDPInitiatedConfigTests_mapToUserRegistry_User_noRegistry_userUniqueIdentifierBad() throws Exception {

        notfoundExceptionTest("server_2_caller_mapUserIdentifierToRegistryUser_True_noRegistry_userUniqueIdentifierBad.xml");

    }

}
