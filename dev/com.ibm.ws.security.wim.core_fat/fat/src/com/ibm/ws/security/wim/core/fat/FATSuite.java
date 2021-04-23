/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.core.fat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
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
public class FATSuite extends CommonLocalLDAPServerSuite {

    /*
     * Repeat tests for Jakarta EE 9.
     *
     * Wee need to replace appSecurity-1.0 with appSecurity-4.0. The appSecurity-3.0 and 4.0 features
     * no longer include ldapRegistry-3.0, so we need to add that as well.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction())
                    .andWith(new JakartaEE9Action().addFeature("appSecurity-4.0").removeFeature("appSecurity-1.0").alwaysAddFeature("ldapRegistry-3.0"));

    /**
     * JakartaEE9 transform a list of applications. The applications must exist at '<server>/<appPath>'.
     *
     * @param myServer The server to transform the applications on.
     * @param appPaths The relative paths (from the root server directory) of the applications to transform.
     */
    public static void transformApps(LibertyServer myServer, String... appPaths) {
        if (JakartaEE9Action.isActive()) {
            for (String appPath : appPaths) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + appPath);
                JakartaEE9Action.transformApp(someArchive);
            }
        }
    }
}