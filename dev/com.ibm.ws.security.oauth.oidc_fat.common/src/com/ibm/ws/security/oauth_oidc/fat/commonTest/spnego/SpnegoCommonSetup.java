/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

import com.ibm.websphere.simplicity.ConnectionInfo;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.InitClass;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.ExternalTestService;
import componenttest.topology.impl.JavaInfo.Vendor;

public class SpnegoCommonSetup extends InitClass {
    private static final Class<?> c = SpnegoCommonSetup.class;

    @ClassRule
    public static ExternalResource testRule = new ExternalResource() {
        /**
         * Creates the SPN and keytab file to be used in any ensuing tests. Test classes can elect to create their own
         * SPN and keytab file if needed. A common SPNEGO token is also created which can be used by all test classes.
         * The common SPNEGO token should be refreshed at a certain interval, specified by the
         * TOKEN_REFRESH_LIFETIME_SECONDS variable.
         */
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
                SpnegoOIDCCommonTest.commonSpnegoConfigSetup(null,"com.ibm.ws.security.openidconnect.fat.spnego.setup", null, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS,
                                                 SPNEGOConstants.DONT_CREATE_SSL_CLIENT,
                                                 SPNEGOConstants.CREATE_SPN_AND_KEYTAB, SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.CREATE_SPNEGO_TOKEN, SPNEGOConstants.SET_AS_COMMON_TOKEN,
                                                 SPNEGOConstants.USE_CANONICAL_NAME, SPNEGOConstants.DONT_USE_COMMON_KEYTAB, SPNEGOConstants.DONT_START_SERVER);
            } catch (Exception e) {
            	String message = CommonTest.maskHostnameAndPassword(e.getMessage());
                Log.info(c, thisMethod, "Common setup failed, tests will not run: " + message, e);
                throw (new Exception("Common setup failed, tests will not run: " + message, e));
            }

            // The keytab file needs to be copied into the /tmp folder so we can include it in the other servers
            SpnegoOIDCCommonTest.getMyServer().copyFileToTempDir(SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1) + SPNEGOConstants.KRB5_KEYTAB_FILE, SPNEGOConstants.KRB5_KEYTAB_FILE);
            KEYTAB_FILE_LOCATION = "tmp/" + SPNEGOConstants.KRB5_KEYTAB_FILE;

            Log.info(c, thisMethod, "Common setup is complete");
        }

        private boolean isSupportJDK() throws IOException {
            String thisMethod = "isSupportJDK";
            JavaInfo javaInfo= JavaInfo.forServer(LibertyServerFactory.getLibertyServer("com.ibm.ws.security.openidconnect.fat.spnego.setup"));
            IBM_JDK_V8_LOWER = javaInfo.vendor() == Vendor.IBM && javaInfo.majorVersion() <= 8;
            SUN_ORACLE_JDK_V8_HIGHER = javaInfo.vendor() == Vendor.SUN_ORACLE && javaInfo.majorVersion() >= 8;
            OTHER_SUPPORT_JDKS = javaInfo.majorVersion() >= 11 || SUN_ORACLE_JDK_V8_HIGHER;
            IBM_HYBRID_JDK = isHybridJDK(javaInfo);

            Log.info(c, thisMethod, "The JDK used on this system is version: " + javaInfo.majorVersion() + " and vendor: " + javaInfo.vendor());
            if (!IBM_JDK_V8_LOWER && !OTHER_SUPPORT_JDKS && !SUN_ORACLE_JDK_V8_HIGHER) {
                Log.info(c,
                         thisMethod, "The JDK used on this system is version: " + javaInfo.majorVersion() + " and vendor: " + javaInfo.vendor()
                                     + ". Because only IBM JDK version 8 or less, Oracle and Open JDK version 8 and higher and JDK version 11 are currently supported, no tests will be run.");
                RUN_TESTS = false;
            }
            if (IBM_HYBRID_JDK) {
                RUN_TESTS = false;
            }
            Log.info(c, thisMethod,
                     "The JDK vendor used is " + javaInfo.vendor() + " and version: " + javaInfo.majorVersion());

            Log.info(c, thisMethod, "The JDK vendor used is " + javaInfo.vendor() + " and version: " + javaInfo.majorVersion());
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
                

        @Override
        protected void after() {
            try {
                if (RUN_TESTS) {
                    SpnegoOIDCCommonTest.getKdcHelper().deleteUser();
                    SpnegoOIDCCommonTest.getKdcHelper().deleteRemoteFileFromRemoteMachine(SpnegoOIDCCommonTest.getKdcHelper().getKdcMachine(), SPNEGOConstants.KRB5_KEYTAB_FILE);
                    SpnegoOIDCCommonTest.getKdcHelper().deleteRemoteFileFromRemoteMachine(SpnegoOIDCCommonTest.getKdcHelper().getKdcMachine(), SPNEGOConstants.KRB5_KEYTAB_TEMP_SUFFIX);
                }
            } catch (Exception e) {
            	Log.info(c, "after", "Exception thrown while deleting SPN: " + CommonTest.maskHostnameAndPassword(e.getMessage()), e);
            }
        };
    };
}