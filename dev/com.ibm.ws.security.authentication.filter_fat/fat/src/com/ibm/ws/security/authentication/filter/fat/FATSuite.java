/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.authentication.filter.fat;

import java.net.InetAddress;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                AuthFilterElementTest.class,
                DynamicAuthFilterTest.class
})
public class FATSuite {
    private static final Class<?> c = FATSuite.class;
    public static boolean LOCALHOST_DEFAULT_IP_ADDRESS = true; //127.0.0.1       localhost
    /**
     * Rule to setup users, SPNs etc on the KDC.
     */
    @ClassRule
    public static ExternalResource beforeRule = new ExternalResource() {
        @Override
        protected void before() throws Exception {
            String thisMethod = "before";
            Log.info(c, thisMethod, "Performing the common setup for all test classes");

            String ip = InetAddress.getByName("localhost").getHostAddress();
            if (!"127.0.0.1".equals(ip)) {
                Log.info(c, thisMethod, "The localhost ip address is " + ip + " (not default value 127.0.0.1), localhost ip address tests will not be run");
                LOCALHOST_DEFAULT_IP_ADDRESS = false;
            }

            InetAddress localHost = InetAddress.getLocalHost();
            String canonicalHostName = localHost.getCanonicalHostName();

            try {
                CommonTest.commonSetUp("com.ibm.ws.security.authentication.filter.fat.setup", null, AuthFilterConstants.NO_APPS, AuthFilterConstants.NO_PROPS,
                                       AuthFilterConstants.DONT_START_SERVER);
            } catch (Exception e) {
                Log.info(c, thisMethod, "Common setup failed, tests will not run: " + e.getMessage(), e);
                throw (new Exception("Common setup failed, tests will not run: " + e.getMessage(), e));
            }

            Log.info(c, thisMethod, "Common setup is complete");
        }
    };
}
