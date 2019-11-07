/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.registry.fat;

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.FederatedRepository;
import com.ibm.websphere.simplicity.config.wim.RealmPropertyMapping;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPFatUtils;

/**
 * This test will test federating a stand-alone Custom UserRegistry (CUR) to federated
 * registries, while maintaining the same behavior as the CUR had stand-alone.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class FederateStandaloneCurTest {

    @Server("com.ibm.ws.security.wim.registry.fat.FederateStandaloneCur")
    public static LibertyServer libertyServer;

    private static final Class<?> c = FederateStandaloneCurTest.class;
    private static UserRegistryServletConnection servlet;
    private static ServerConfiguration startConfiguration = null;

    /** The value returned from the active user registry's {@link UserRegistry#getRealm()} method when using the CUR without federatedRegistry-1.0. */
    private static final String STANDALONE_REALM = "customRealm";

    /** The value returned from the active user registry's {@link UserRegistry#getType()} method when using the CUR without federatedRegistry-1.0. */
    private static final String STANDALONE_TYPE = "CUSTOM";

    /** The value returned from the active user registry's {@link UserRegistry#getRealm()} method when using federatedRegistry-1.0. */
    private static final String FEDERATED_REALM = "FederatedRealm";

    /** The value returned from the active user registry's {@link UserRegistry#getType()} method when using federatedRegistry-1.0. */
    private static final String FEDERATED_TYPE = "WIM";

    /** Base DN for the CUR. */
    private static final String BASE_DN = "o=" + STANDALONE_REALM;

    /** The value returned from the CUR's {@link UserRegistry#getUserSecurityName(String)} method. */
    private static final String USER_SECURITY_NAME = "uid=johndoe," + BASE_DN;

    /** The value returned from the CUR's {@link UserRegistry#getUniqueUserId(String)} method. */
    private static final String USER_UNIQUE_ID = "johndoe";

    /** The value returned from the CUR's {@link UserRegistry#getUserDisplayName(String)} method. */
    private static final String USER_DISPLAY_NAME = "John Doe";

    /** The value user's password stored in the CUR. */
    private static final String USER_PASSWORD = "password";

    /** The value returned from the CUR's {@link UserRegistry#getGroupSecurityName(String)} method. */
    private static final String GROUP_SECURITY_NAME = "cn=group1," + BASE_DN;

    /** The value returned from the CUR's {@link UserRegistry#getUniqueGroupId(String)} method. */
    private static final String GROUP_UNIQUE_ID = "group1";

    /** The value returned from the CUR's {@link UserRegistry#getGroupDisplayName(String)} method. */
    private static final String GROUP_DISPLAY_NAME = "User Group #1";

    /**
     * Setup the test case.
     *
     * @throws Exception If the setup failed for some reason.
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        setupLibertyServer();
        setupCustomUserRegistry();
    }

    private static void setupLibertyServer() throws Exception {

        Log.info(c, "setUpLibertyServer", "Starting the server... (will wait for userRegistry servlet to start)");
        libertyServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        libertyServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/customRegistrySample-1.0.mf");
        libertyServer.copyFileToLibertyInstallRoot("lib", "internalfeatures/com.ibm.ws.security.registry_test.custom_1.0.jar");
        libertyServer.addInstalledAppForValidation("userRegistry");
        libertyServer.startServer(c.getName() + ".log");

        /*
         * Make sure the application has come up before proceeding
         */
        assertNotNull("Application userRegistry does not appear to have started.",
                      libertyServer.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready",
                      libertyServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      libertyServer.waitForStringInLog("CWWKF0011I"));

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(libertyServer.getHostname(), libertyServer.getHttpDefaultPort());

        if (servlet.getRealm() == null) {
            Thread.sleep(5000);
            servlet.getRealm();
        }

        /*
         * The original server configuration.
         */
        startConfiguration = libertyServer.getServerConfiguration();
    }

    /**
     * Tear down the test.
     */
    @AfterClass
    public static void teardownClass() throws Exception {
        if (libertyServer != null) {
            libertyServer.stopServer();
        }

        /*
         * Delete any files we copied to the test server and uninstall the user bundles.
         */
        libertyServer.deleteFileFromLibertyInstallRoot("lib/features/webspheresecuritylibertyinternals-1.0.mf");
        libertyServer.deleteFileFromLibertyInstallRoot("lib/features/customRegistrySample-1.0.mf");
        libertyServer.deleteFileFromLibertyInstallRoot("lib/com.ibm.ws.security.registry_test.custom_1.0.jar");
    }

    /**
     * The custom UserRegistry will contain one user and one group. It is based on some files that contains the
     * users and groups.
     *
     * @throws Exception If writing to the users and groups files failed.
     */
    private static void setupCustomUserRegistry() throws Exception {

        /**
         * Write the custom UserRegistry users file.
         *
         * <pre>
         * # Format:
         * # name:passwd:uid:gids:display name
         * # where name   = userId/userName of the user
         * #       passwd = password of the user
         * #       uid    = uniqueId of the user
         * #       gid    = groupIds of the groups that the user belongs to
         * #       display name = a (optional) display name for the user.
         * </pre>
         */
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(new File(libertyServer.getServerRoot() + "/resources/security/users.props")));
            writer.write(USER_SECURITY_NAME + ":" + USER_PASSWORD + ":" + USER_UNIQUE_ID + ":" + GROUP_UNIQUE_ID + ":" + USER_DISPLAY_NAME);
            writer.newLine();
        } finally {
            writer.close();
        }

        /**
         * Write the custom UserRegistry groups file.
         *
         * <pre>
         * # Format:
         * # name:passwd:uid:gids:display name
         * # where name   = groupId of the group
         * #       gid    = uniqueId of the group
         * #       users  = list of all the userIds that the group contains
         * #       display name = a (optional) display name for the group.
         * </pre>
         */
        try {
            writer = new BufferedWriter(new FileWriter(new File(libertyServer.getServerRoot() + "/resources/security/groups.props")));
            writer.write(GROUP_SECURITY_NAME + ":" + GROUP_UNIQUE_ID + ":" + USER_SECURITY_NAME + ":" + GROUP_DISPLAY_NAME);
            writer.newLine();
        } finally {
            writer.close();
        }
    }

    /**
     * Update the Liberty server configuration dynamically.
     *
     * @param federate Whether to enable federatedRegistry-1.0 feature.
     * @throws Exception If the server could not be updated.
     */
    private static ServerConfiguration updateLibertyConfiguration(boolean federate) throws Exception {
        ServerConfiguration server = startConfiguration.clone();

        if (federate) {
            /*
             * Configure the federated repository.
             */
            server.getFeatureManager().getFeatures().add("federatedRegistry-1.0");
            String[] participatingEntries = new String[] { BASE_DN };
            FederatedRepository federatedRepository = LDAPFatUtils.createFederatedRepository(server, FEDERATED_REALM, participatingEntries);

            // Failed with: "expected:<[johndoe]> but was:<[uid=johndoe,o=customRealm]>"
            federatedRepository.getPrimaryRealm().setUniqueUserIdMapping(new RealmPropertyMapping("uniqueId", "uniqueId"));

            // Failed with: "expected:<[John Doe]> but was:<[uid=johndoe,o=customRealm]>"
            federatedRepository.getPrimaryRealm().setUserDisplayNameMapping(new RealmPropertyMapping("displayName", "displayName"));

            // Failed with: "expected:<[User Group]> but was:<[null]>"
            federatedRepository.getPrimaryRealm().setGroupDisplayNameMapping(new RealmPropertyMapping("displayName", "displayName"));

            // Failed to find entity in getGroupSecurityName (searched for cn instead)
            federatedRepository.getPrimaryRealm().setUniqueGroupIdMapping(new RealmPropertyMapping("uniqueId", "uniqueId"));
        }

        return server;
    }

    /**
     * Run the test with the CUR as the active user registry (stand-alone).
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    public void standaloneCustomUserRegistry() throws Exception {
        ServerConfiguration config = updateLibertyConfiguration(false);
        updateConfigDynamically(libertyServer, config, true);

        doAssertions(STANDALONE_REALM, STANDALONE_TYPE);
    }

    /**
     * Run the test with CUR federated in federatedRegistry-1.0.
     *
     * @throws Exception If the test fails for some unforeseen reason.
     */
    @Test
    public void federatedCustomUserRegistry() throws Exception {
        ServerConfiguration config = updateLibertyConfiguration(true);
        updateConfigDynamically(libertyServer, config, true);

        doAssertions(FEDERATED_REALM, FEDERATED_TYPE);
    }

    /**
     * Perform assertions against the values returned from the active UserRegistry. The only
     * difference between the results when calling this with the CUR as the active UserRegistry
     * (stand-alone) and the with the CUR federated in federatedRegistry-1.0 should be the
     * {@link UserRegistry#getType()} and {@link UserRegistry#getRealm()} values.
     *
     * @param realm The value expected to be returned from {@link UserRegistry#getRealm()} call.
     * @param type The value expected to be returned from the {@link UserRegistry#getType()} call.
     * @throws Exception If the test fails for some unforeseen reason.
     */
    private static void doAssertions(String realm, String type) throws Exception {
        /*
         * Generic CUR calls.
         */
        assertEquals(realm, servlet.getRealm());
        assertEquals(type, servlet.getType());

        /*
         * UserRegistry user lookup API calls.
         */
        assertEquals(USER_SECURITY_NAME, servlet.checkPassword(USER_SECURITY_NAME, USER_PASSWORD));
        assertEquals(USER_UNIQUE_ID, servlet.getUniqueUserId(USER_SECURITY_NAME));
        assertEquals(USER_DISPLAY_NAME, servlet.getUserDisplayName(USER_SECURITY_NAME));
        assertEquals(USER_SECURITY_NAME, servlet.getUserSecurityName(USER_UNIQUE_ID));

        /*
         * UserRegistry group lookup API calls.
         */
        assertEquals(GROUP_DISPLAY_NAME, servlet.getGroupDisplayName(GROUP_SECURITY_NAME));
        assertEquals(GROUP_SECURITY_NAME, servlet.getGroupSecurityName(GROUP_UNIQUE_ID));
        assertEquals(GROUP_UNIQUE_ID, servlet.getUniqueGroupId(GROUP_SECURITY_NAME));

        /*
         * UserRegistry search API calls.
         */
        SearchResult groups = servlet.getGroups("*", 0);
        assertEquals(1, groups.getList().size());
        assertEquals(GROUP_SECURITY_NAME, groups.getList().get(0));

        List<String> groupIds = servlet.getUniqueGroupIdsForUser(USER_UNIQUE_ID);
        assertEquals(1, groupIds.size());
        assertEquals(GROUP_UNIQUE_ID, groupIds.get(0));

        SearchResult users = servlet.getUsers("*", 0);
        assertEquals(1, users.getList().size());
        assertEquals(USER_SECURITY_NAME, users.getList().get(0));

        SearchResult usersForGroup = servlet.getUsersForGroup(GROUP_SECURITY_NAME, 0);
        assertEquals(1, usersForGroup.getList().size());
        assertEquals(USER_SECURITY_NAME, usersForGroup.getList().get(0));
    }
}
