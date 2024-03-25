/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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

package com.ibm.ws.security.wim.core.fat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.com.unboundid.InMemoryADLDAPServer;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.com.unboundid.InMemoryTDSLDAPServer;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({ InvalidBaseEntryInRealmTest.class,
                MaxSearchResultTest.class,
                DynamicUpdateTest.class,
                NoRegistryConfiguredTest.class,
                WimCoreRegressionTest.class,
                ConfigManagerInitModifyTest.class,
                ConfigManagerFeatureTest.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    private static InMemoryLDAPServer tdsLdapServer, adLdapServer;

    /**
     * Repeat tests for Jakarta EE 9 and 10.
     *
     * We need to replace appSecurity-1.0 with appSecurity-4.0. The appSecurity-3.0 4.0 and 5.0 features
     * no longer include ldapRegistry-3.0, so we need to add that as well.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction())
                    .andWith(new JakartaEE9Action().addFeature("appSecurity-4.0")
                                    .removeFeature("appSecurity-1.0")
                                    .alwaysAddFeature("ldapRegistry-3.0")
                                    .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(new JakartaEE10Action().addFeature("appSecurity-5.0").removeFeature("appSecurity-1.0").alwaysAddFeature("ldapRegistry-3.0"));

    @BeforeClass
    public static void setup() throws Exception {
        /*
         * Force the tests to use local LDAP server and start up the TDS and AD LDAP instances.
         */
        System.setProperty("fat.test.really.use.local.ldap", "true"); // Force local LDAP.
        tdsLdapServer = new InMemoryTDSLDAPServer(InMemoryTDSLDAPServer.getWellKnownLdapPort(), InMemoryTDSLDAPServer.getWellKnownLdapsPort());
        adLdapServer = new InMemoryADLDAPServer(InMemoryADLDAPServer.getWellKnownLdapPort(), InMemoryADLDAPServer.getWellKnownLdapsPort());
    }

    @AfterClass
    public static void teardown() {
        if (tdsLdapServer != null) {
            tdsLdapServer.shutDown();
        }
        if (adLdapServer != null) {
            adLdapServer.shutDown();
        }
    }

    /**
     * JakartaEE9 transform a list of applications. The applications must exist at '<server>/<appPath>'.
     *
     * @param myServer The server to transform the applications on.
     * @param appPaths The relative paths (from the root server directory) of the applications to transform.
     */
    public static void transformApps(LibertyServer myServer, String... appPaths) {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            for (String appPath : appPaths) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + appPath);
                JakartaEEAction.transformApp(someArchive);
            }
        }
    }
}