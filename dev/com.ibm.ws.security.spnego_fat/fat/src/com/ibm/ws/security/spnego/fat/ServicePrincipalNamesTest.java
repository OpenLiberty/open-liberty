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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.KdcHelper;
import com.ibm.ws.security.spnego.fat.config.MessageConstants;
import com.ibm.ws.security.spnego.fat.config.MsKdcHelper;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
public class ServicePrincipalNamesTest extends CommonTest {

    private static final Class<?> c = ServicePrincipalNamesTest.class;

    private final String BACKUP_KEYTAB_NAME = SPNEGOConstants.KRB5_KEYTAB_FILE + ".orig";
    private final static String PROP_SPN_2 = "security.spnego.spn.2";
    private final static String PROP_REALM_1 = "security.spnego.realm.1";
    private final static String PROP_REALM_2 = "security.spnego.realm.2";
    private final String REALM_2 = "REALM2.COM";

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";
        Log.info(c, thisMethod, "Setting up...");

        List<String> checkApps = new ArrayList<String>();
        checkApps.add("basicauth");

        commonSetUp("ServicePrincipalNamesTest", null, checkApps, SPNEGOConstants.NO_PROPS, SPNEGOConstants.DONT_START_SERVER);

        FATSuite.transformApps(myServer, "basicauth.war");
    }

    /**
     * Test description:
     * - Set server xml file to have the servicePrincipalNames attribute value equal to the default SPN.
     * - Access a protected resource by including a SPNEGO token created with a matching valid SPN in the request.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     */

    @Test
    public void testSingleSpn() {
        try {
            testHelper.reconfigureServer("singleSpn.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            // Access the servlet using the default SPN
            commonSuccessfulSpnegoServletCall();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have the servicePrincipalNames attribute value equal to the default SPN, including the realm name.
     * - Access a protected resource by including a SPNEGO token created with a matching valid SPN in the request.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     */

    @Test
    public void testSingleSpnWithRealm() {
        try {
            // Add bootstrap property for the realm name
            Map<String, String> bootstrapProps = new HashMap<String, String>();
            bootstrapProps.put(PROP_REALM_1, InitClass.KDC_REALM);
            testHelper.addBootstrapProperties(myServer, bootstrapProps);

            testHelper.reconfigureServer("singleSpnWithRealm.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Access the servlet using the default SPN
            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have the servicePrincipalNames attribute value equal to a list of authorized SPNs. In this test, the test system host
     * name and "localhost" are used as the authorized SPNs.
     * - Access a protected resource by including a SPNEGO token created with one of the authorized SPNs in the request.
     * - Access the protected resource again by including a SPNEGO token created with the other authorized SPN in the request.
     *
     * Expected results:
     * - Access to the protected resource in both cases should be successful.
     */

    @Test
    public void testValidSpnList() {

        try {
            testHelper.reconfigureServer("validSpnList.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Access the servlet using one of the valid SPNs in the list
            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using token created using first authorized SPN");
            commonSuccessfulSpnegoServletCall();

            // Access the servlet using another valid SPN in the list
            String newToken = testHelper.createSpnegoToken(InitClass.FIRST_USER, InitClass.FIRST_USER_PWD, "localhost", SPNEGOConstants.SERVER_KRB5_CONFIG_FILE, krb5Helper);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + newToken, SPNEGOConstants.FIREFOX, "localhost", null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet again using token created using second authorized SPN");
            successfulSpnegoServletCall(headers, InitClass.FIRST_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have the servicePrincipalNames attribute value equal to a blank list (empty string).
     * - Access a protected resource by including a SPNEGO token created with the default SPN in the request.
     *
     * Expected results:
     * - CWWKS4314I message should be output saying the default SPN will be used since none are specified in the
     * config.
     * - Authentication should be successful and access to the protected resource should be granted.
     */

//    @Test  TODO: enable later
    public void testValidSpnList_Blank() {
        try {
            if (InitClass.isRndHostName) {
                Log.info(c, name.getMethodName(), "Not running " + name.getMethodName() + " because randomized hostname is used.");
                return;
            }
            testHelper.reconfigureServer("emptySpnList.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Access the servlet using the default SPN
            commonSuccessfulSpnegoServletCall();

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.SPN_NOT_SPECIFIED_CWWKS4314I);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have the servicePrincipalNames attribute value equal to a list of authorized SPNs where
     * both SPNs map to the same SPNEGO service and are included in the keytab file.
     * - Access a protected resource by including a SPNEGO token created with one of the authorized SPNs in the request.
     * - Access the protected resource again by including a SPNEGO token created with the other authorized SPN in the request.
     *
     * Expected results:
     * - Access to the protected resource in both cases should be successful.
     */

    @Test
    public void testValidSpnList_AliasSpn() {
        String aliasSpnHost = "aliasSpnHost_" + TARGET_SERVER;
        KdcHelper testKdcHelper = null;
        /*
         * Other JDKs do not refresh the keytab files
         */
        if (FATSuite.OTHER_SUPPORT_JDKS) {
            return;
        }
        testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);

        String user = testKdcHelper.getDefaultUserName();

        try {
            backUpKeytab();
        } catch (Exception e) {
            Log.error(c, name.getMethodName(), e, "Failed to back up existing keytab file.");
        }

        try {
            Log.info(c, name.getMethodName(), "Adding additional SPN to keytab for user: " + user);
            testKdcHelper.addSpnToKeytab(user, aliasSpnHost);

            // Add bootstrap property for the alias SPN
            Map<String, String> bootstrapProps = new HashMap<String, String>();
            bootstrapProps.put(PROP_SPN_2, aliasSpnHost);
            testHelper.addBootstrapProperties(myServer, bootstrapProps);

            testHelper.reconfigureServer("testSystemHostSpnAndSecondHostSpn.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            String aliasToken = testHelper.createSpnegoToken(InitClass.FIRST_USER, InitClass.FIRST_USER_PWD, aliasSpnHost, SPNEGOConstants.SERVER_KRB5_CONFIG_FILE, krb5Helper);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + aliasToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using token created using alias SPN");
            successfulSpnegoServletCall(headers, InitClass.FIRST_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using common token");
            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        } finally {
            try {
                // Delete the additional SPN added for this test and restore the original keytab
                Log.info(c, name.getMethodName(), "Deleting additional SPN that was created for this test.");
                testKdcHelper.deleteSpnForUser(user, aliasSpnHost);
                restoreKeytab();
            } catch (Exception e) {
                Log.error(c, name.getMethodName(), e, "Failed to delete the additional SPN that was added for this test or restore the original keytab.");
                fail("Failed to delete the additional SPN that was added for this test or restore the original keytab: " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Test description:
     * - Set server xml file to have the servicePrincipalNames attribute value equal to a poorly formatted list of SPNs.
     * - Access a protected resource by including a SPNEGO token created with the default SPN in the request.
     *
     * Expected results:
     * - CWWKS4308E messages should be output saying the server was unable create GSS credentials for the invalid SPNs.
     * - CWWKS4309E message should be output saying no GSS credentials could be created for any SPNs specified, so
     * SPNEGO will not be used.
     * - Multiple FFDCs for GSSExceptions should be thrown due to multiple invalid SPNs appearing in the list.
     * - Authentication will fail for all SPNs tried, resulting in a 403.
     */

    @AllowedFFDC({ "org.ietf.jgss.GSSException" })
    @Test
    public void testInvalidSpnList_BadFormat() {
        try {
            testHelper.reconfigureServer("invalidSpnList_badFormat.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKE0701E", "CWWKS4308E", "CWWKS4309E");
            List<String> checkMsgs = new ArrayList<String>();

            if (FATSuite.OTHER_SUPPORT_JDKS) {
                // JDK 11 the strings /)-!@#$%, ^&amp;*(), ;[] are not supported, therefore the error message
                //is different.
                commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_UNAUTHORIZED);
                checkMsgs.add(MessageConstants.JDK11_INVALID_CHARACTER_CWWKE0701E);

            } else {
                // Access the servlet using the default SPN
                commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_FORBIDDEN);

                checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_SPN_CWWKS4308E);
                checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_ANY_SPN_CWWKS4309E);

            }
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have the servicePrincipalNames attribute value equal to a poorly formatted list of SPNs.
     * - Access a protected resource by including a SPNEGO token created with the default SPN in the request.
     *
     * Expected results:
     * - CWWKS4308E messages should be output saying the server was unable create GSS credentials for the invalid SPNs.
     * - Multiple FFDCs for GSSExceptions should be thrown due to multiple invalid SPNs appearing in the list.
     * - Authentication will ultimately be successful for the default SPN.
     */
    @AllowedFFDC({ "org.ietf.jgss.GSSException" })
    @Test
    public void testInvalidSpnList_BadFormatGoodSpn() {
        try {
            testHelper.reconfigureServer("invalidSpnList_badFormatGoodSpn.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKE0701E", "CWWKS4308E");
            List<String> checkMsgs = new ArrayList<String>();
            if (FATSuite.OTHER_SUPPORT_JDKS) {
                // JDK 11 the strings /)-!@#$%, ^&amp;*(), ;[] are not supported, therefore the error message
                //is different.
                commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_UNAUTHORIZED);
                checkMsgs.add(MessageConstants.JDK11_INVALID_CHARACTER_CWWKE0701E);

            } else {
                // Access the servlet using the default SPN
                commonSuccessfulSpnegoServletCall();

                checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_SPN_CWWKS4308E);
                testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);
            }

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have the servicePrincipalNames attribute value equal to a list of SPNs that use the same host name.
     * - Access a protected resource by including a SPNEGO token created with a valid SPN in the request.
     *
     * Expected results:
     * - CWWKS4316W message should be output saying more than one SPN is specified for this host name.
     * - Authentication should be successful and access to the protected resource should be granted.
     */

    @Test
    public void testInvalidSpnList_DuplicateHostnames() {
        try {
            testHelper.reconfigureServer("invalidSpnList_duplicateHostNames.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4316W");

            // Access the servlet using the default SPN
            commonSuccessfulSpnegoServletCall();

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.MULTIPLE_SPN_FOR_ONE_HOSTNAME_CWWKS4316W);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have the servicePrincipalNames attribute value equal to an authorized SPN.
     * - Access a protected resource by including a SPNEGO token created with an unauthorized SPN in the request.
     *
     * Expected results:
     * - CWWKS4315E message should be output saying a GSSCredential for the unauthorized SPN could not be found.
     * - Access to the protected resource should be denied, resulting in a 401.
     */

    @Test
    public void testAuthorizedSpn_ClientSendsTokenForUnauthorizedSpn() {
        try {
            List<String> startMsgs = new ArrayList<String>();
            testHelper.reconfigureServer("localhostAuthorizedSpn.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4315E");

            // localhost is explicitly authorized in the server config but the SPN for the test machine is not
            commonUnsuccessfulSpnegoServletCall();

            List<String> errorMsg = new ArrayList<String>();
            errorMsg.add(MessageConstants.GSSCREDENTIAL_NOT_FOUND_FOR_SPN_CWWKS4315E);
            testHelper.waitForMessages(errorMsg, SPNEGOConstants.EXPECTED_MESSAGE);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set an additional SPN for an existing user on the KDC machine but do not include it in the keytab file.
     * - Set server xml file to have the servicePrincipalNames attribute value equal to the unauthorized SPN.
     * - Access a protected resource by including a SPNEGO token created with a matching unauthorized SPN in the request.
     *
     * Expected results:
     * - CWWKS4308E message should be output saying the server was unable create GSS credentials for the unauthorized SPN.
     * - CWWKS4309E message should be output saying no GSS credentials could be created for any SPNs specified, so SPNEGO
     * will not be used.
     * - FFDC for GSSException should be thrown due to unauthorized SPNs appearing in the list.
     * - Authentication will fail for the unauthorized SPN, resulting in a 403.
     */

    @ExpectedFFDC({ "org.ietf.jgss.GSSException" })
    @Test
    public void testUnauthorizedSpn_ClientSendsTokenForUnauthorizedSpn() {
        String unauthorizedSpnHost = "unauthorizedSpnHost" + TARGET_SERVER;
        KdcHelper testKdcHelper = null;
        testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);

        String user = testKdcHelper.getDefaultUserName();

        try {
            Log.info(c, name.getMethodName(), "Setting additional SPN for user: " + user);
            testKdcHelper.setSpnForUser(user, unauthorizedSpnHost);

            // Add bootstrap property for the SPN not contained in the keytab file
            Map<String, String> bootstrapProps = new HashMap<String, String>();
            bootstrapProps.put(PROP_SPN_2, unauthorizedSpnHost);
            testHelper.addBootstrapProperties(myServer, bootstrapProps);

            testHelper.reconfigureServer("unauthorizedSpn.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4308E", "CWWKS4309E");

            String unauthorizedToken = testHelper.createSpnegoToken(InitClass.FIRST_USER, InitClass.FIRST_USER_PWD, unauthorizedSpnHost, SPNEGOConstants.SERVER_KRB5_CONFIG_FILE,
                                                                    krb5Helper);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + unauthorizedToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT, HttpServletResponse.SC_FORBIDDEN);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_SPN_CWWKS4308E);
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_ANY_SPN_CWWKS4309E);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        } finally {
            try {
                // Delete the additional SPN added for this test
                Log.info(c, name.getMethodName(), "Deleting additional SPN that was created for this test.");
                testKdcHelper.deleteSpnForUser(user, unauthorizedSpnHost);
            } catch (Exception e) {
                Log.error(c, name.getMethodName(), e, "Could not delete the additional SPN that was added for this test.");
                fail("Failed to delete the additional SPN that was added for this test: " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Test description:
     * - Set an additional SPN for an existing user on the KDC machine but do not include it in the keytab file.
     * - Set server xml file to have the servicePrincipalNames attribute value equal to a list of two SPNs: one authorized
     * and one matching the additional unauthorized SPN.
     * - Access a protected resource by including a SPNEGO token created with the authorized SPN in the request.
     *
     * Expected results:
     * - CWWKS4308E message should be output saying the server was unable create GSS credentials for the unauthorized SPN.
     * - FFDC for GSSException should be thrown due to unauthorized SPN appearing in the list.
     * - Authentication should be successful for the authorized SPN and access to the protected resource should be granted.
     */

    @ExpectedFFDC({ "org.ietf.jgss.GSSException" })
    @Test
    public void testAuthorizedAndUnauthorizedSpnList_ClientSendsTokenForAuthorizedSpn() {
        String spnNotInKeytab = "secondUnauthorizedSpnHost" + TARGET_SERVER;
        KdcHelper testKdcHelper = null;
        testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);

        String user = testKdcHelper.getDefaultUserName();
        try {
            Log.info(c, name.getMethodName(), "Will set additional SPN for user: " + user);
            testKdcHelper.setSpnForUser(user, spnNotInKeytab);

            // Add bootstrap property for the SPN not contained in the keytab file
            Map<String, String> bootstrapProps = new HashMap<String, String>();
            bootstrapProps.put(PROP_SPN_2, spnNotInKeytab);
            testHelper.addBootstrapProperties(myServer, bootstrapProps);

            testHelper.reconfigureServer("testSystemHostSpnAndSecondHostSpn.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4308E");

            // Access the servlet using the default SPN
            commonSuccessfulSpnegoServletCall();

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_SPN_CWWKS4308E);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        } finally {
            try {
                // Delete the additional SPN added for this test
                Log.info(c, name.getMethodName(), "Deleting additional SPN that was created for this test.");
                testKdcHelper.deleteSpnForUser(user, spnNotInKeytab);
            } catch (Exception e) {
                Log.error(c, name.getMethodName(), e, "Could not delete the additional SPN that was added for this test.");
                fail("Failed to delete the additional SPN that was added for this test: " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Test description:
     * - Set an additional SPN for an existing user on the KDC machine but do not include it in the keytab file.
     * - Set server xml file to have the servicePrincipalNames attribute value equal to a list of two SPNs: one authorized
     * and one matching the additional unauthorized SPN.
     * - Access a protected resource by including a SPNEGO token created with the unauthorized SPN in the request.
     *
     * Expected results:
     * - CWWKS4308E message should be output saying the server was unable create GSS credentials for the unauthorized SPN.
     * - FFDC for GSSException should be thrown due to unauthorized SPN appearing in the list.
     * - Authentication will fail for the unauthorized SPN, resulting in a 403.
     */
    @ExpectedFFDC({ "org.ietf.jgss.GSSException" })
//    @Test
    public void testAuthorizedAndUnauthorizedSpnList_ClientSendsTokenForUnauthorizedSpn() {
        // TODO Does the SPN used to create the SPNEGO token matter? The runtime gets the request host name, sees the value of TARGET_SERVER,
        // finds a valid GSSCredential for TARGET_SERVER, and says authentication was successful
        String spnNotInKeytab = "unauthorizedSpnNotInKeytab";
        KdcHelper testKdcHelper = null;

        testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);

        String user = testKdcHelper.getDefaultUserName();
        try {
            Log.info(c, name.getMethodName(), "Will set additional SPN for user: " + user);
            testKdcHelper.setSpnForUser(user, spnNotInKeytab);

            // Add bootstrap property for the SPN not contained in the keytab file
            Map<String, String> bootstrapProps = new HashMap<String, String>();
            bootstrapProps.put(PROP_SPN_2, spnNotInKeytab);
            testHelper.addBootstrapProperties(myServer, bootstrapProps);

            testHelper.reconfigureServer("testSystemHostSpnAndSecondHostSpn.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            String unauthorizedToken = testHelper.createSpnegoToken(InitClass.FIRST_USER, InitClass.FIRST_USER_PWD, spnNotInKeytab, SPNEGOConstants.SERVER_KRB5_CONFIG_FILE,
                                                                    krb5Helper);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + unauthorizedToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT, HttpServletResponse.SC_FORBIDDEN);

            List<String> errorMsg = new ArrayList<String>();
            errorMsg.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_SPN_CWWKS4308E);
            testHelper.waitForMessages(errorMsg, SPNEGOConstants.EXPECTED_MESSAGE);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        } finally {
            try {
                // Delete the additional SPN added for this test
                Log.info(c, name.getMethodName(), "Deleting additional SPN that was created for this test.");
                testKdcHelper.deleteSpnForUser(user, spnNotInKeytab);
            } catch (Exception e) {
                Log.error(c, name.getMethodName(), e, "Could not delete the additional SPN that was added for this test.");
                fail("Failed to delete the additional SPN that was added for this test: " + e.getMessage());
            }
        }
    }

    /**
     * Test description:
     * - Set server xml file to have the servicePrincipalNames attribute value equal to an SPN not in the server realm.
     * - Access a protected resource by including a SPNEGO token created with the default SPN in the request.
     *
     * Expected results:
     * - CWWKS4308E message should be output saying the server was unable create GSS credentials for the invalid SPN.
     * - CWWKS4309E message should be output saying no GSS credentials could be created for any SPNs specified, so
     * SPNEGO will not be used.
     * - FFDC for GSSException should be thrown due to the invalid SPN appearing in the servicePrincipalNames list.
     * - Authentication will fail for the SPN attempted, resulting in a 403.
     */

    @ExpectedFFDC({ "org.ietf.jgss.GSSException" })
    @Test
    public void testSpnNotInServerRealm() {
        try {
            // Add bootstrap properties for the second valid host name
            Map<String, String> bootstrapProps = new HashMap<String, String>();
            bootstrapProps.put(PROP_REALM_2, REALM_2);
            testHelper.addBootstrapProperties(myServer, bootstrapProps);

            testHelper.reconfigureServer("spnNotInServerRealm.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4309E", "CWWKS4308E");

            // Access the servlet using the default SPN
            commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_FORBIDDEN);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_SPN_CWWKS4308E);
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_ANY_SPN_CWWKS4309E);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Backs up the krb5.keytab file within the server root to the server's temp directory.
     *
     * @throws Exception
     */
    private void backUpKeytab() throws Exception {
        String methodName = "backUpKeytab";
        Log.info(c, methodName, "Backing up keytab file for test " + name.getMethodName());
        myServer.copyFileToTempDir(SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1) + SPNEGOConstants.KRB5_KEYTAB_FILE, BACKUP_KEYTAB_NAME);
    }

    /**
     * Restores the backed up krb5.keytab file from the server's temp directory back into the Kerberos resource location
     * within the server root. If a keytab file already exists in the Kerberos resource location, it is renamed to be
     * identified with the current test.
     *
     * @throws Exception
     */
    private void restoreKeytab() throws Exception {
        String methodName = "restoreKeytab";
        final String krbResourceLocation = SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1);
        Log.info(c, methodName, "Restoring old keytab file and backing up new keytab file created for test " + name.getMethodName());

        if (myServer.fileExistsInLibertyServerRoot(krbResourceLocation + SPNEGOConstants.KRB5_KEYTAB_FILE)) {
            Log.info(c, methodName, "Renaming existing keytab file for later examination");
            myServer.renameLibertyServerRootFile(krbResourceLocation + SPNEGOConstants.KRB5_KEYTAB_FILE,
                                                 krbResourceLocation + SPNEGOConstants.KRB5_KEYTAB_FILE + "." + name.getMethodName());
        }

        Log.info(c, methodName, "Copying backup keytab file back into Kerberos resource location");
        myServer.copyFileToLibertyServerRoot(krbResourceLocation, "tmp/" + BACKUP_KEYTAB_NAME);
        Log.info(c, methodName, "Renaming backup keytab file back to " + SPNEGOConstants.KRB5_KEYTAB_FILE);
        myServer.renameLibertyServerRootFile(krbResourceLocation + BACKUP_KEYTAB_NAME, krbResourceLocation + SPNEGOConstants.KRB5_KEYTAB_FILE);
    }
}
