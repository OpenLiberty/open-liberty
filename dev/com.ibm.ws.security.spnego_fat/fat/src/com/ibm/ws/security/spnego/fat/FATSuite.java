/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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
package com.ibm.ws.security.spnego.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.Network;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                BasicAuthTest.class,
//CanonicalHostNameTest.class,
//IncludeClientGSSCredentialInSubjectTest.class,
//IncludeCustomCacheKeyTest.class,
//Krb5ConfigTest.class,
///ServicePrincipalNamesTest.class,
//SpnegoTokenHelperApiTest.class,
//TrustedActiveDirectoryDomainsTest.class
})

public class FATSuite extends TestContainerSuite {

    private static final Class<?> c = FATSuite.class;

    //ADDED ON
    //KDC Users
    //TODO: clean up and remove old variables
    //Some are only called by quarantined tests that need to be updated.
    public static String KRB5_USER = "olduser";//bindUserName;
    public static String KRB5_USER_PWD = "oldpassword";//bindPassword;

    public static String KRB5_USER1 = KRB5_USER; //"user1";
    public static String KRB5_USER1_PWD = KRB5_USER_PWD; //"user1pwd";
    public static String KRB5_USER2 = KRB5_USER; //"user2";
    public static String KRB5_USER2_PWD = KRB5_USER_PWD; //"user2pwd";

    public static String COMMON_TOKEN_USER = KRB5_USER1;
    public static String COMMON_TOKEN_USER_PWD = KRB5_USER1_PWD;
    public static boolean COMMON_TOKEN_USER_IS_EMPLOYEE = true;
    public static boolean COMMON_TOKEN_USER_IS_MANAGER = false;

    public static String COMMON_SPNEGO_TOKEN = null;
    public static String KEYTAB_FILE_LOCATION = null;
    public static long COMMON_TOKEN_CREATION_DATE = 0;
    public static final double TOKEN_REFRESH_LIFETIME_SECONDS = 180;
    public static boolean LOCALHOST_DEFAULT_IP_ADDRESS = true; //127.0.0.1       localhost

    public static boolean IBM_JDK_V8_LOWER = false;
    public static boolean OTHER_SUPPORT_JDKS = false;
    public static boolean SUN_ORACLE_JDK_V8_HIGHER = false;
    public static boolean IBM_HYBRID_JDK = false;

    public static String serverShortHostName = "shortHostName";

    public static String KDC_REALM = "EXAMPLE.COM";

    public static Network network;
    public static KerberosContainer krb5;

    //@ClassRule
    public static RepeatTests repeat = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(new JakartaEE10Action());

    static {
        // Needed for IBM JDK 8 support.
        java.lang.System.setProperty("com.ibm.jsse2.overrideDefaultTLS", "true");
    }

    @BeforeClass
    public static void startKerberos() throws Exception {
        network = Network.newNetwork();
        krb5 = new KerberosContainer(network);
        krb5.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Exception firstError = null;

        try {
            krb5.stop();
            network.close();
        } catch (Exception e) {
            if (firstError == null)
                firstError = e;
            Log.error(FATSuite.class, "tearDown", e);
        }

        if (firstError != null)
            throw firstError;
    }
}