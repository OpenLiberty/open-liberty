/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class CanonicalHostNameTest extends CommonTest {

    private static final Class<?> c = CanonicalHostNameTest.class;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting the server and kerberos setup...");
        commonSetUp("CanonicalHostNameTest", null, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS, SPNEGOConstants.DONT_CREATE_SSL_CLIENT,
                    SPNEGOConstants.CREATE_SPN_AND_KEYTAB, SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.CREATE_SPNEGO_TOKEN, SPNEGOConstants.DONT_SET_AS_COMMON_TOKEN,
                    SPNEGOConstants.DONT_USE_CANONICAL_NAME, SPNEGOConstants.DONT_USE_COMMON_KEYTAB, SPNEGOConstants.START_SERVER);
    }

    /**
     * Test description:
     * - Set server xml file to have a false value for canoncialHostName
     * - Restart the server
     *
     * Expected results:
     * - Should be able to access SPNEGO protected resource with short hostname
     * and canonicalHostName set to false
     */
    @Mode(TestMode.QUARANTINE)
    @Test
    public void testcanonicalHostName_FalsewithShortName() {
        try {
            String shortHostName = java.net.InetAddress.getLocalHost().getHostName();
            if (shortHostName.contains(SPNEGOConstants.IBM_DOMAIN)) {
                shortHostName = shortHostName.substring(0, shortHostName.indexOf("."));
            }
            // Add bootstrap properties
            Map<String, String> bootstrapProps = new HashMap<String, String>();
            bootstrapProps.put(SPNEGOConstants.PROP_TEST_SYSTEM_SHORTHOST_NAME, shortHostName);
            testHelper.addBootstrapProperties(myServer, bootstrapProps);

            // checking to make sure DNS name for host is short name, otherwise test will fail
            String localHostName = java.net.InetAddress.getLocalHost().getHostName();
            Log.info(c, name.getMethodName(), "Hostname used is: " + localHostName);
            if (!localHostName.contains(SPNEGOConstants.IBM_DOMAIN)) {
                testHelper.reconfigureServer("canonicalHostNameFalse.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
                // check message log for error message will be done in reconfigureServer call
                successfulSpnegoServletCall(createHeaders(), InitClass.FIRST_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);
            } else {
                Log.info(c, name.getMethodName(), "Found IBM domain in hostname so skipping the test");
            }
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }
}
