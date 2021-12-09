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

import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.MessageConstants;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class Krb5ConfigTest extends CommonTest {

    private static final Class<?> c = Krb5ConfigTest.class;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting the server...");
        commonSetUp("Krb5ConfigTest", null, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS, SPNEGOConstants.DONT_CREATE_SSL_CLIENT, SPNEGOConstants.CREATE_SPN_AND_KEYTAB,
                    SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.DONT_CREATE_SPNEGO_TOKEN, SPNEGOConstants.DONT_SET_AS_COMMON_TOKEN, SPNEGOConstants.USE_CANONICAL_NAME,
                    SPNEGOConstants.DONT_USE_COMMON_KEYTAB, SPNEGOConstants.START_SERVER);
    }

    /**
     * This calls a method that will run in order to make sure we delete the winnt directory and keytab/krb5.ini
     * files if it was created while running the default krb config/keytab test. If it was created then
     * we will delete it, if not we will keep it.
     *
     * @throws Exception
     */
    @AfterClass
    public static void removeWinntFolder() throws Exception {
        //For non IBM jdk, even though we don't run any test the after class always gets run.
        //This causes an npe when it gets here since the kdchelper is only initialized when
        //we actually run these test.
        if (kdcHelper != null) {
            kdcHelper.removeWinNtFilesAndFolderDefaultLocation();
        }
    }

    /**
     * Test description:
     * - Specify a non-existent location for the krb5Config attribute in server.xml.
     * - The server will not be restarted because only the config changes need to be tested.
     *
     * Expected results:
     * - The CWWKS4303E message should appear in the logs saying the specified Kerberos configuration file could not be
     * found.
     */
    //ffdc is expected on jdk 11
    @Test
    @AllowedFFDC("org.ietf.jgss.GSSException")
    public void testKrbConfigNotFound_NonExistentLocation() {
        try {
            testHelper.reconfigureServer("krbConfigNotFound_NonExistentLocation.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            testHelper.addShutdownMessages("CWWKS4303E", "CWWKS4308E", "CWWKS4309E");
            commonUnsuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.KRBCONFIGFILE_NOT_FOUND_CWWKS4303E);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Specify a non-existent location for the krb5Keytab attribute in server.xml.
     * - The server will not be restarted because only the config changes need to be tested.
     *
     * Expected results:
     * - The CWWKS4305E message should appear in the logs saying the specified Kerberos keytab file could not be found.
     */

    @Test
    public void testKrbKeytabNotFound_NonExistentLocation() {
        try {
            testHelper.reconfigureServer("krbKeytabNotFound_NonExistentLocation.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            testHelper.addShutdownMessages("CWWKS4305E");
            commonUnsuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.KEYTAB_NOT_FOUND_CWWKS4305E);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Specify a non-existent file name for the krb5Config attribute in server.xml.
     * - The server will not be restarted because only the config changes need to be tested.
     *
     * Expected results:
     * - The CWWKS4303E message should appear in the logs saying the specified Kerberos configuration file could not be
     * found.
     */
    //ffdc is expected on jdk 11
    @Test
    @AllowedFFDC("org.ietf.jgss.GSSException")
    public void testInvalidKRBConfig_NonExistentFileName() {
        try {
            testHelper.reconfigureServer("invalidKrbConfigFileName_NonExistentFileName.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            testHelper.addShutdownMessages("CWWKS4303E", "CWWKS4313E", "CWWKS4308E", "CWWKS4309E");
            commonUnsuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.KRBCONFIGFILE_NOT_FOUND_CWWKS4303E);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Specify a non-existent file name for the krb5Keytab attribute in server.xml.
     * - The server will not be restarted because only the config changes need to be tested.
     *
     * Expected results:
     * - The CWWKS4305E message should appear in the logs saying the specified Kerberos keytab file could not be found.
     */

    @Test
    public void testInvalidKRBKeytab_NonExistentFileName() {
        try {
            testHelper.reconfigureServer("invalidKrbKeytabFileName_NonExistentFileName.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            testHelper.addShutdownMessages("CWWKS4305E");
            commonUnsuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.KEYTAB_NOT_FOUND_CWWKS4305E);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - krbConfig attribute is not specified in server.xml.
     * - The server will be restarted.
     * - The krbconfig and keytab will be added at their default location either Windows or winnt folder.
     *
     * Expected results:
     * - When running on a Windows machine, the CWWKS4302I message should appear in the logs saying the Kerberos
     * configuration file is not specified in the server.xml file so the default will be used.
     */
    //ffdc is expected on jdk 11
    @Test
    @AllowedFFDC("org.ietf.jgss.GSSException")
    public void testKRBConfigFileAtDefaultLocation() {
        try {
            OperatingSystem osName = myServer.getMachine().getOperatingSystem();

            if (osName.equals(OperatingSystem.WINDOWS)) {
                if (kdcHelper != null) {
                    kdcHelper.removeWinNtFilesAndFolderDefaultLocation();
                }
                Log.info(c, name.getMethodName(), "This is a Windows OS, we expect to receive the message ID: CWWKS4302I ");
                kdcHelper.copyConfFilesToDefaultLocation(SPNEGOConstants.KRB5_CONF_FILE, SPNEGOConstants.KRB5_KEYTAB_FILE);
                testHelper.reconfigureServer("krbConfigAtDefaultLocation.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
                commonUnsuccessfulSpnegoServletCall();
                testHelper.checkForMessages(true, MessageConstants.KRBCONFIGFILE_NOT_SPECIFIED_CWWKS4302I);
            } else {
                Log.info(c, name.getMethodName(), "This is not Windows OS. Since the userID is not root, this test will be ignored");
                testHelper.addShutdownMessages("CWWKS4312E");
            }
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - krbKeytab attribute is not specified in server.xml.
     * - The server will not be restarted because only the config changes need to be tested.
     * - The krbconfig and keytab will be added at their default location either Windows or winnt folder.
     *
     * Expected results:
     * - When running on a Windows Machine, the CWWKS4304I message should appear in the logs saying the Kerberos keytab
     * file is not specified in the server.xml file so the default will be used.
     */

    @Test
    public void testKRBKeytabFileAtDefaultLocation() {
        try {
            OperatingSystem osName = myServer.getMachine().getOperatingSystem();
            if (osName.equals(OperatingSystem.WINDOWS)) {
                if (kdcHelper != null) {
                    kdcHelper.deleteKeytabFileAtDefaultLocation();
                }
                Log.info(c, name.getMethodName(), "This is a Windows OS, we expect to receive the message ID: CWWKS4304I ");
                kdcHelper.copyConfFilesToDefaultLocation(SPNEGOConstants.KRB5_CONF_FILE, SPNEGOConstants.KRB5_KEYTAB_FILE);
                testHelper.reconfigureServer("krbKeytabAtDefaultLocation.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
                commonUnsuccessfulSpnegoServletCall();
                testHelper.checkForMessages(true, MessageConstants.KEYTAB_NOT_SPECIFIED_CWWKS4304I);

            } else {
                Log.info(c, name.getMethodName(), "This is not Windows OS. Since the userID is not root, this test will be ignored");
                testHelper.addShutdownMessages("CWWKS4312E");
            }
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }
}
