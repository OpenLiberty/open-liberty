/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.fat;

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
@SuiteClasses({
                OAuth20DeclarativeAuthz.class,
                OAuth20CustomConsentForm.class,
                OAuth20CustomLoginForm.class,
                OAuth20MediatorTest.class,
                OAuth20DynamicConfig01.class,
                OAuth20WebClient01.class,
                OAuth20WebClient02.class,
                OAuth20WebClient03.class,
                OAuth20WebClient04.class,
                OAuth20WebClient05.class,
                OAuth20UnRegClient01.class,
                OAuth20UnRegClient02.class,
                OAuth20PublicClient01.class,
                OAuth20PublicClient02.class,
                OAuth20PublicClient03.class,
                OAuth20BadCredsClient01.class,
                OAuth20BadTokClient01.class,
                OAuth20ConfigClient01.class,
                OAuth20RefreshTok01.class,
                //               OAuth20NoFilterClient01.class,
                OAuth20OnlyClient01.class,
                OAuth20DerbyClient01XOR.class,
                OAuth20DerbyClient01Hash.class,
                OAuth20CustomStoreClient01Hash.class,
                OAuth20CustomStoreClient01XOR.class,
                OAuth20CustomStoreBellClient01.class,
                OAuth20DerbyClient02.class,
                OAuth20CustomStoreClient02.class,
                OAuth20CustomStoreBellClient02.class,
                OAuth20DerbyClient03.class,
                OAuth20CustomStoreClient03.class,
                OAuth20CustomStoreBellClient03.class,
                TestResourceOwnerValidationMediator.class,
                OAuth20WebClientError.class,
                OAuth20AccessNonexistentPageTest.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends CommonLocalLDAPServerSuite {

    /*
     * Run EE9 tests in LITE mode and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(new JakartaEE9Action());

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
