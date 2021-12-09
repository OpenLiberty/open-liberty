/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.InitClass;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.JavaInfo.Vendor;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

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
public class FATSuite extends ApacheKDCforSPNEGO {
    private static final Class<?> c = FATSuite.class;

    //@ClassRule
    public static RepeatTests repeat = RepeatTests.withoutModification().andWith(new JakartaEE9Action());

    /**
     * Rule to setup users, SPNs etc on the KDC.
     */
    @ClassRule
    public static ExternalResource beforeRule = new ExternalResource() {
        /**
         * Creates the SPN and keytab file to be used in any ensuing tests. Test classes can elect to create their own
         * SPN and keytab file if needed. A common SPNEGO token is also created which can be used by all test classes.
         * The common SPNEGO token should be refreshed at a certain interval, specified by the
         * TOKEN_REFRESH_LIFETIME_SECONDS variable.
         */
        @Override
        protected void before() throws Exception {
            String thisMethod = "before";
            Log.info(c, thisMethod, "Performing the common setup for all test classes");

            isSupportJDK();

            /*
             * String ip = InetAddress.getByName("localhost").getHostAddress();
             * if (!"127.0.0.1".equals(ip)) {
             * Log.info(c, thisMethod, "The localhost ip address is " + ip + " (not default value 127.0.0.1), localhost ip address tests will not be run");
             * LOCALHOST_DEFAULT_IP_ADDRESS = false;
             * }
             */

            ApacheKDCCommonTest.setGlobalLoggingLevel(Level.WARNING);

            Log.info(c, thisMethod, "Common setup is complete");
        }

        private boolean isSupportJDK() throws IOException {
            String thisMethod = "isSupportJDK";
            boolean runTests = true;
            JavaInfo javaInfo = JavaInfo.forServer(LibertyServerFactory.getLibertyServer("BasicAuthTest"));

            IBM_JDK_V8_LOWER = javaInfo.vendor() == Vendor.IBM && javaInfo.majorVersion() <= 8;
            SUN_ORACLE_JDK_V8_HIGHER = javaInfo.vendor() == Vendor.SUN_ORACLE && javaInfo.majorVersion() >= 8;
            OTHER_SUPPORT_JDKS = javaInfo.majorVersion() >= 11 || SUN_ORACLE_JDK_V8_HIGHER;
            InitClass.OTHER_SUPPORT_JDKS = OTHER_SUPPORT_JDKS;
            InitClass.KDC_REALM = KDC_REALM;
            InitClass.KDC_HOSTNAME = ldapServerHostName;
            IBM_HYBRID_JDK = isHybridJDK(javaInfo);

            Log.info(c, thisMethod, "The JDK used on this system is version: " + javaInfo.majorVersion() + " and vendor: " + javaInfo.vendor());
            if (!IBM_JDK_V8_LOWER && !OTHER_SUPPORT_JDKS && !SUN_ORACLE_JDK_V8_HIGHER) {
                Log.info(c, thisMethod, "The JDK used on this system is version: " + javaInfo.majorVersion() + " and vendor: " + javaInfo.vendor() +
                                        ". Because only IBM JDK version 8 or less, Oracle and Open JDK version 8 and higher and JDK version 11 are currently supported, no tests will be run.");
                runTests = false;
            }
            if (IBM_HYBRID_JDK) {
                runTests = false;
            }
            Log.info(c, thisMethod, "The JDK vendor used is " + javaInfo.vendor() + " and version: " + javaInfo.majorVersion());

            if (!runTests) {
                Log.info(c, thisMethod, "=== JDK NOT SUPPORTED FOR SPNEGO FAT TESTS ===");
                Log.info(c, thisMethod, "=== SKIPPING SPNEGO FAT TESTS ===");
            }

            InitClass.RUN_TESTS = runTests;
            return runTests;
        };

        private boolean isHybridJDK(JavaInfo javaInfo) {
            String thisMethod = "isHybridJDK";

            boolean hybridJdk = false;
            String javaRuntime = System.getProperty("java.runtime.version");
            Log.info(c, thisMethod, "The  current runtime version is: " + javaRuntime);

            if ((javaInfo.vendor() == Vendor.SUN_ORACLE) && javaRuntime.contains("SR")) {
                hybridJdk = true;
            } else {
                hybridJdk = false;
            }
            Log.info(c, thisMethod, "Hybrid JDK: " + hybridJdk);
            return hybridJdk;
        }
    };

    /**
     * JakartaEE9 transform a list of applications. The applications are the simple app names and they must exist at '<server>/apps/<appname>'.
     *
     * @param myServer The server to transform the applications on.
     * @param apps The simple names of the applications to transform.
     */
    public static void transformApps(LibertyServer myServer, String... apps) {
        if (JakartaEE9Action.isActive()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + "apps" + File.separatorChar + app);
                JakartaEE9Action.transformApp(someArchive);
            }
        }
    }
}
