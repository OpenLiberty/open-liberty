/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.KdcHelper;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.JavaInfo.Vendor;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                // AlwaysPassesTest.class,
                // DynamicSpnegoConfigTest.class,
                 S4U2SelfTest.class,
                S4U2ProxyTest.class
})
public class FATSuite extends InitClass {
    private static final Class<?> c = FATSuite.class;

    @ClassRule
    public static RepeatTests repeat = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(new JakartaEE10Action());

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

            getKDCInfoFromConsul();

            if (!isSupportJDK())
                return;


            String ip = InetAddress.getByName("localhost").getHostAddress();
            if (!"127.0.0.1".equals(ip)) {
                Log.info(c, thisMethod, "The localhost ip address is " + ip + " (not default value 127.0.0.1), localhost ip address tests will not be run");
                LOCALHOST_DEFAULT_IP_ADDRESS = false;
            }

            try {
                CommonTest.commonSetUp("com.ibm.ws.security.spnego.fat.setup", null, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS,
                                       SPNEGOConstants.DONT_CREATE_SSL_CLIENT,
                                       SPNEGOConstants.CREATE_SPN_AND_KEYTAB, SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.CREATE_SPNEGO_TOKEN,
                                       SPNEGOConstants.SET_AS_COMMON_TOKEN, SPNEGOConstants.USE_CANONICAL_NAME, SPNEGOConstants.DONT_USE_COMMON_KEYTAB,
                                       SPNEGOConstants.DONT_START_SERVER);
            } catch (Exception e) {
                String message = CommonTest.maskHostnameAndPassword(e.getMessage());
                Log.info(c, thisMethod, "Common setup failed, tests will not run: " + message, e);
                throw (new Exception("Common setup failed, tests will not run: " + message, e));
            }

            // The keytab file needs to be copied into the /tmp folder so we can include it in the other servers
            CommonTest.getMyServer().copyFileToTempDir(SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1) + SPNEGOConstants.KRB5_KEYTAB_FILE, SPNEGOConstants.KRB5_KEYTAB_FILE);
            KEYTAB_FILE_LOCATION = "tmp/" + SPNEGOConstants.KRB5_KEYTAB_FILE;

            Log.info(c, thisMethod, "Common setup is complete");
        }

        private boolean isSupportJDK() throws IOException {
            String thisMethod = "isSupportJDK";
            JavaInfo javaInfo = JavaInfo.forServer(LibertyServerFactory.getLibertyServer("DynamicSpnegoConfigTest"));

            IBM_HYBRID_JDK = isHybridJDK(javaInfo);

            Log.info(c, thisMethod, "The JDK used on this system is version: " + javaInfo.majorVersion() + " and vendor: " + javaInfo.vendor());
            if (IBM_HYBRID_JDK) {
                Log.info(c, thisMethod, "SPENGO and constrained delegation do not support IBM hybrid JDK. Test will not be run as isHybridJDK: " + IBM_HYBRID_JDK);
                RUN_TESTS = false;
            }
            return RUN_TESTS;
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
     * Rule to cleanup users, SPNs etc from the KDC. This rule is separate from the setup
     * rule b/c the after method is not called when the before method fails.
     */
    @ClassRule
    public static ExternalResource afterRule = new ExternalResource() {
        @SuppressWarnings("restriction")
        @Override
        protected void after() {
            try {
                if (RUN_TESTS) {
                    KdcHelper kdcHelper = CommonTest.getKdcHelper();
                    kdcHelper.deleteUser();
                    kdcHelper.deleteRemoteFileFromRemoteMachine(CommonTest.getKdcHelper().getKdcMachine(), SPNEGOConstants.KRB5_KEYTAB_FILE);

                    kdcHelper.deleteVbsScriptsFromKDC();
                    /*
                     * Don't delete the localhost_HTTP_krb5.keytab from the remote machine.
                     */
                    if (!"localhost".equalsIgnoreCase(InitClass.serverShortHostName)) {
                        kdcHelper.deleteRemoteFileFromRemoteMachine(CommonTest.getKdcHelper().getKdcMachine(),
                                                                    InitClass.serverShortHostName + SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX);
                    }
                }
            } catch (Exception e) {
                Log.info(c, "after", "Exception thrown while deleting SPN: " + CommonTest.maskHostnameAndPassword(e.getMessage()));
                e.printStackTrace();
            }
        };
    };

    /**
     * JakartaEE9 transform a list of applications. The applications are the simple app names and they must exist at '<server>/apps/<appname>'.
     *
     * @param myServer The server to transform the applications on.
     * @param apps     The simple names of the applications to transform.
     */
    public static void transformApps(LibertyServer myServer, String... apps) {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + "apps" + File.separatorChar + app);
                JakartaEEAction.transformApp(someArchive);
            }
        }

    }
}
