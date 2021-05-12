/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.AlwaysRunAndPassTest;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
        // Ported list of tests (some already renamed)
        AlwaysRunAndPassTest.class,

        // Basic Functional tests
        JwtBuilderApiBasicTests.class,
        JwtBuilderApiWithLDAPBasicTests.class,
        JwkEndpointValidationUrlTests.class,

        // Configuration Tests
        JwtBuilderAPIConfigTests.class,
        JwtBuilderAPIConfigNoIdTests.class,
        JwtBuilderAPIConfigAltKeyStoreTests.class,
        JwtBuilderAPIWithLDAPConfigTests.class,
        JwtBuilderAPIMinimumConfigTests.class,
        JwtBuilderAPIMinimumRunnableConfigTests.class,
        JwtBuilderAPIMinimumSSLConfigGlobalTests.class,
        JwtBuilderAPIMinimumSSLConfigBuilderTests.class

})

public class FATSuite {

    public static boolean runAsCollection = false;

    /*
     * Run EE9 tests in LITE mode and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
    	.andWith(new JakartaEE9Action().forServerConfigPaths("publish/servers", "publish/shared/config"));
    
    /**
     * JakartaEE9 transform a list of applications.
     *
     * @param myServer The server to transform the applications on.
     * @param apps     The names of the applications to transform. Should include the path from the server root directory.
     */
    public static void transformApps(LibertyServer myServer, String... apps) {
        if (JakartaEE9Action.isActive()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + app);
                JakartaEE9Action.transformApp(someArchive);
            }
        }
    }
}
