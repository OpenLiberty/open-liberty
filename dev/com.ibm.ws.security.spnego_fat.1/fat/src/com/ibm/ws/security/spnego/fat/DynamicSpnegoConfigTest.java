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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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
public class DynamicSpnegoConfigTest extends CommonTest {

    private static final Class<?> c = DynamicSpnegoConfigTest.class;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting the server and kerberos setup...");
        commonSetUp("DynamicSpnegoConfigTest");
    }

    /**
     * The beforeTest method will call reconfigureServer to reset the server configuration back to a default
     * setting. This is needed to ensure we can test for proper messages when we make dynamic updates
     */
    @Before
    public void beforeTest() throws Exception {
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add(MessageConstants.FEATURE_UPDATE_COMPLETE_CWWKF0008I);
        testHelper.reconfigureServer("spnegoDefaultConfig.xml", "beforeTest()", startMsgs, SPNEGOConstants.RESTART_SERVER);
        if (FATSuite.OTHER_SUPPORT_JDKS) {
            createNewSpnegoToken(SPNEGOConstants.CREATE_SPNEGO_TOKEN);
        }
    }

    public void generateSpnegoTokenInsideTest() throws Exception {
        if (FATSuite.OTHER_SUPPORT_JDKS) {
            createNewSpnegoToken(SPNEGOConstants.CREATE_SPNEGO_TOKEN);
        }
    }

    /**
     * Test description:
     * - Restart and reconfigure the server to add the SPNEGO feature.
     * - Do a SPNEGO Servlet call with valid SPNEGO token and feature.
     * - Then reconfigure the server to remove the SPNEGO feature.
     * - Do another SPNEGO Servlet call with valid SPNEGO token but NO Feature.
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all required configs.
     * - 2) After the server has been reconfigured to remove the SPNEGO feature, we should get a 401.
     */

    @Test
    public void testAddthenRemoveSPNEGOFeature() {
        try {
            List<String> startMsgs = new ArrayList<String>();

            //we do a spnego call with the default config
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

            //reconfig the server to remove the spnego feature and then we do an unsucesfull spnego call
            startMsgs.add(MessageConstants.FEATURE_UPDATE_COMPLETE_CWWKF0008I);
            testHelper.reconfigureServer("serverNoSpnegoFeature.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Then reconfigure the server to remove the SPNEGO feature.
     * - Do a SPNEGO Servlet call with valid SPNEGO token but NO Feature.
     * - Then reconfigure the server to add the SPNEGO feature.
     * - Do another SPNEGO Servlet call with valid SPNEGO token and Feature.
     * Expected results:
     * - 1) After the server has been reconfigured to remove the SPNEGO feature, we should get a 401
     * - 2) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testRemovethenAddSPNEGOFeature() {
        try {
            List<String> startMsgs = new ArrayList<String>();

            //reconfigure the server without spnego feature then do an unsucessful spnego call
            startMsgs.add(MessageConstants.FEATURE_UPDATE_COMPLETE_CWWKF0008I);
            testHelper.reconfigureServer("serverNoSpnegoFeature.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();

            //now we reconfigure the server to add the spengo feature and do a sucessful spnego call
            startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.FEATURE_UPDATE_COMPLETE_CWWKF0008I);
            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Then reconfigure the server to set an incorrect location for the krbconfig file.
     * - Do a SPNEGO Servlet call but will fail with 403.
     * - Then reconfigure the server to add the correct location of the krbconfig file
     * - Do another SPNEGO Servlet call with valid SPNEGO configuration.
     * Expected results:
     * - 1) After the server has been reconfigured to use an invalid krbconfig file, we should get a 403.
     * - 2) A 200 should be received since we reconfigured the server to include all valid configs.
     */

    //@AllowedFFDC({ "org.ietf.jgss.GSSException" })
    //@Test
    public void testKRBConfigPathNotFoundthenFound() {
        try {
            //Reconfigure the server to one that will not find the KRB config file.
            testHelper.reconfigureServer("serverKrbConfigNotFound_PathNotFound.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            //Because it was not found, we should get a 403 error in our request.
            commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_FORBIDDEN);
            testHelper.checkForMessages(true, MessageConstants.KRBCONFIGFILE_NOT_FOUND_CWWKS4303E);

            //Extra error messages and FFDC should be logged.
            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_SPN_CWWKS4308E);
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_ANY_SPN_CWWKS4309E);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);

            List<String> startMsgs = new ArrayList<String>();
            //TODO: need to come back after defect 155621 has been delivered
            //the startMgs needs to be uncommented and the reconfigure method needs to be changed to false
            //now we reconfigure the server to add the correct location for the krb5config file.
            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            //The spnego servlet call should work now.
            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Then reconfigure the server to add the correct location of the krbconfig file
     * - Do another SPNEGO Servlet call with valid SPNEGO configuration.
     * - Then reconfigure the server to set an incorrect location for the krbconfig file.
     * - Do a SPNEGO Servlet call but will fail with 403.
     * Expected results:
     * - 1) A 200 should be received since the server has all valid configs.
     * - 2) After the server has been reconfigured to use an invalid krbconfig file, we should get a 403.
     */

    //@AllowedFFDC({ "org.ietf.jgss.GSSException" })
    //@Test
    public void testKRBConfigPathFoundthenNotFound() {
        try {

            //Do a SPNEGO Servlet call
            //TODO need to uncomment this after defect 155621 is delivered
            //commonSuccessfulSpnegoServletCall();

            //Reconfigure the server to one that will not find the KRB config file.
            testHelper.reconfigureServer("serverKrbConfigNotFound_PathNotFound.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            //Because it was not found, we should get a 403 error in our request.
            commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_FORBIDDEN);
            testHelper.checkForMessages(true, MessageConstants.KRBCONFIGFILE_NOT_FOUND_CWWKS4303E);
            //Extra error messages and FFDC should be logged.
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
     * Test description:
     * - Then reconfigure the server to set an incorrect location for the keytab file.
     * - Do a SPNEGO Servlet call but will fail with 403.
     * - Then reconfigure the server to add the correct location of the keytab file
     * - Do another SPNEGO Servlet call with valid SPNEGO configuration.
     * Expected results:
     * - 1) After the server has been reconfigured to use an invalid keytab file, we should get a 403.
     * - 2) A 200 should be received since we reconfigured the server to include all valid configs.
     */

    @Test
    public void testKrbKeytabPathNotFoundthenFound() {
        try {

            //Reconfigure the server to change the path of the keytab file.
            testHelper.reconfigureServer("serverKrbKeytabNotFound_PathNotFound.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4305E");
            generateSpnegoTokenInsideTest();
            //because of this the servlet call should fail with 403
            commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_FORBIDDEN);
            testHelper.checkForMessages(true, MessageConstants.KEYTAB_NOT_FOUND_CWWKS4305E);

            List<String> startMsgs = new ArrayList<String>();
            //now we reconfigure the server to use the default config.
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            //the spnego servlet call should now pass.
            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Then reconfigure the server to add the correct location of the keytab file
     * - Do another SPNEGO Servlet call with valid SPNEGO configuration.
     * - Then reconfigure the server to set an incorrect location for the keytab file.
     * - Do a SPNEGO Servlet call but will fail with 403.
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all valid configs.
     * - 2) After the server has been reconfigured to use an invalid keytab file, we should get a 403.
     */

    @Test
    public void testKrbKeytabPathFoundthenNotFound() {
        try {

            List<String> startMsgs = new ArrayList<String>();
            //Test default SPNEGO config, this should pass.
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

            //Reconfigure the server to change the path of the keytab file.
            startMsgs.add(MessageConstants.KEYTAB_NOT_FOUND_CWWKS4305E);
            testHelper.reconfigureServer("serverKrbKeytabNotFound_PathNotFound.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4305E");
            generateSpnegoTokenInsideTest();
            //We get a 403 error now.
            commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_FORBIDDEN);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Then reconfigure the server in order to remove authFilter element from the SPNEGO config.
     * - Do a SPNEGO Servlet call with valid SPNEGO token but no Auth filter element specified in the SPENGO Config.
     * - Then reconfigure the server in order to add the authFilter element to the SPNEGO config.
     * - Do another SPNEGO Servlet call with valid SPNEGO token and the authFilter element specified in the SPENGO Config.
     * Expected results:
     * - 1) A 200 should be received since all the resources become protected when the authFilter property,
     * does not exist in the SPNEGO config. Since the AuthFilter is not present then the CWWKS4358I should not be called.
     * - 2) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testNoAuthFilterThenAdd() {
        try {

            //reconfigure the server to remove the auth filter and do a sucessful spnego call
            testHelper.reconfigureServer("serverSpnegoNoAuthFilter.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

            //Since we are not specifying the auth filter on the server config, the CWWKS4358I message should
            //not appear on the logs.
            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.MESSAGE_NOT_EXPECTED, SPNEGOConstants.MESSAGE_NOT_EXPECTED_LOG_SEARCH_TIMEOUT);

            //since the wait timeout is of 5 minutes and the Spnego token expires around that time,
            //we need to create a new one
            createNewSpnegoToken(true);

            //reconfigure the server to add the auth filter and do a sucessful spnego call
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Do a SPNEGO Servlet call with valid SPNEGO token and the authFilter element specified in the SPENGO Config.
     * - Then reconfigure the server in order to remove authFilter element from the SPNEGO config.
     * - Do a SPNEGO Servlet call with valid SPNEGO token but no Auth filter element specified in the SPENGO Config.
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all required configs.
     * - 2) A 200 should be received since all the resources become protected when the authFilter property,
     * does not exist in the SPNEGO config. Since the AuthFilter is not present then the CWWKS4358I should not be called.
     */

    @Test
    public void testWithAuthFilterThenRemove() {
        try {
            generateSpnegoTokenInsideTest();
            //do a sucessful spnego call
            commonSuccessfulSpnegoServletCall();
            //since the wait timeout is of 5 minutes and the Spnego token expires around that time,
            //we need to create a new one
            createNewSpnegoToken(true);

            List<String> startMsgs = new ArrayList<String>();
            //reconfigure the server to remove the auth filter of the spnego config.
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverSpnegoNoAuthFilter.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            //this should pass, since all resources are now protected.
            commonSuccessfulSpnegoServletCall();
            List<String> checkMsgs = new ArrayList<String>();

            //Since we are not specifying the auth filter on the server config, the CWWKS4358I message should
            //not appear on the logs.
            checkMsgs.add(MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.MESSAGE_NOT_EXPECTED, SPNEGOConstants.MESSAGE_NOT_EXPECTED_LOG_SEARCH_TIMEOUT);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Then reconfigure the server in order to remove authFilter element from the SPNEGO config.
     * - Do a SPNEGO Servlet call with valid SPNEGO token but no Auth filter element specified in the SPENGO Config.
     * - Then add an authfilter config with bad URL Pattern.
     * - Do a SPNEGO Servlet call that will fail with 401.
     * - Then reconfigure the server in order to add the authFilter element to the SPNEGO config.
     * - Do another SPNEGO Servlet call with valid SPNEGO token and the authFilter element specified in the SPENGO Config.
     * Expected results:
     * - 1) A 200 should be received since all the resources become protected when the authFilter property,
     * does not exist in the SPNEGO config.
     * - 2) After the server has been reconfigured to add the bad URL pattern, we should get a 401
     * - 3) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testNoAuthFilterThenInvalidThenValid() {
        try {

            //reconfigure the server to remove the auth filter and do a sucessful spnego call
            testHelper.reconfigureServer("serverSpnegoNoAuthFilter.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

            //reconfigure the server to add bad url pattern then do a unsucessful spnego call
            List<String> startMsgs = new ArrayList<String>();
            //startMsgs.add(MessageConstants.MSG_CWWKS4359I);
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverAuthFilterBadURLPattern.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();

            //reconfigure the server to add the auth filter and do a sucessful spnego call
            startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Reconfigure the server in order to add a valid SPN Attribute.
     * - Do a SPNEGO Servlet call with valid SPNEGO token and valid SPN attribute.
     * - Reconfigure the server to remove the SPN Attribute.
     * - Do another SPNEGO Serlet call with valid SPNEGO token and default SPN attribute.
     * Expected results:
     * - 1) A 200 should be received since we are using a valid SPN. In addition to that, when
     * we do the reconfigure the CWWKS4314I should not show up on the logs since we are specifying
     * an SPN on the configuration.
     * - 2) A 200 should be received since the default SPN will be used. In this case the CWWKS4314I should
     * appear on the log, since we do specify the SPN attribute in the config.
     */

    @Test
    public void testSPNAttributeValidthenNone() {
        try {
            testHelper.reconfigureServer("serverSpnegoWithSPNAttribute.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();

            //do a sucessful spnego call
            commonSuccessfulSpnegoServletCall();

            //Since the CWWKS4314I message should not show since the SPN is being specified in the server.xml
            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.SPN_NOT_SPECIFIED_CWWKS4314I);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.MESSAGE_NOT_EXPECTED, SPNEGOConstants.MESSAGE_NOT_EXPECTED_LOG_SEARCH_TIMEOUT);

            //since the wait timeout is of 5 minutes and the Spnego token expires around that time,
            //we need to create a new one
            createNewSpnegoToken(true);

            //reconfigure the server to remove the SPN attribute.
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Do a SPNEGO Servlet call with valid SPNEGO token and the default SPN attribute.
     * - Reconfigure the server in order to add a valid SPN Attribute.
     * - Do a SPNEGO Serlet call with valid SPNEGO token and valid SPN attribute.
     * Expected results:
     * - 1) A 200 should be received since the default SPN will be used. In this case the CWWKS4314I should
     * appear on the log, since we do specify the SPN attribute in the config.
     * - 2) A 200 should be received since we are using a valid SPN. In addition to that, when
     * we do the reconfigure the CWWKS4314I should not show up on the logs since we are specifying
     * an SPN on the configuration.
     */

    @Test
    public void testSPNAttributeNonethenValid() {
        try {
            //Do a Spnego servlet call with the default config
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

            //since the wait timeout is of 5 minutes and the Spnego token expires around that time,
            //we need to create a new one
            createNewSpnegoToken(true);

            List<String> startMsgs = new ArrayList<String>();
            //Reconfigure the server to add a valid SPN attribute.
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverSpnegoWithSPNAttribute.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            List<String> checkMsgs = new ArrayList<String>();

            //Since the CWWKS4314I message should not show since the SPN is being specified in the server.xml
            checkMsgs.add(MessageConstants.SPN_NOT_SPECIFIED_CWWKS4314I);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.MESSAGE_NOT_EXPECTED, SPNEGOConstants.MESSAGE_NOT_EXPECTED_LOG_SEARCH_TIMEOUT);
            //do a sucessful spnego call
            commonSuccessfulSpnegoServletCall();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Reconfigure the server in order to add an valid SPN.
     * - Do a SPNEGO Serlet call with valid SPNEGO token and valid SPN.
     * - Reconfigure the server to set an invalid SPN.
     * - Do another SPNEGO Servlet call with the valid SPNEGO token and invalid SPN.
     * Expected results:
     * - 1) A 200 should be received since we are using a valid SPN.
     * - 2) A 403 should be received since we used an invalid SPN.
     */
    //TODO could IBM JDK throw and exception but JDK 11 just returns null?
    @AllowedFFDC({ "org.ietf.jgss.GSSException" })
    @Test
    public void testSPNAttributeValidThenInvalid() {
        try {
            //Reconfigure the server with a valid spn attribute.
            testHelper.reconfigureServer("serverSpnegoWithSPNAttribute.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            //the test should pass.
            commonSuccessfulSpnegoServletCall();

            //Now reconfigure the server with an invalid spn.
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverSpnegoWithInvalidSPNAttribute.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4308E", "CWWKS4309E");
            generateSpnegoTokenInsideTest();

            //the test should now fail with 403.
            commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_FORBIDDEN);
            //Extra error messages and FFDC should be logged.
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
     * Test description:
     * - Reconfigure the server to set an invalid SPN.
     * - Do a SPNEGO Servlet call with valid SPNEGO token and invalid SPN.
     * - Reconfigure the server in order to add a valid SPN.
     * - Do another SPNEGO Serlet call with valid SPNEGO token and a valid SPN.
     * Expected results:
     * - 1) A 403 should be received since we used an invalid SPN.
     * - 2) A 200 should be received since we are using a valid SPN.
     */

    @AllowedFFDC({ "org.ietf.jgss.GSSException" })
    @Test
    public void testSPNAttributeInvalidThenValid() {
        try {

            //reconfigue the server to use an invalid SPN.
            testHelper.reconfigureServer("serverSpnegoWithInvalidSPNAttribute.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4308E", "CWWKS4309E");
            generateSpnegoTokenInsideTest();
            //The test should fail with 403.
            commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_FORBIDDEN);
            //new error messages and ffdc errors should be thrown
            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_SPN_CWWKS4308E);
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_ANY_SPN_CWWKS4309E);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);

            //now reconfigure the server with a valid spn attribute.
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverSpnegoWithSPNAttribute.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            //the test should now pass.
            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Do a SPNEGO Servlet call with valid SPNEGO token and the default SPN attribute.
     * - Reconfigure the server to set an invalid SPN.
     * - Do a SPNEGO Servlet call with valid SPNEGO token and invalid SPN.
     * - Reconfigure the server in order to add a valid SPN.
     * - Do another SPNEGO Serlet call with valid SPNEGO token and a valid SPN.
     * Expected results:
     * - 1) A 200 should be received since we use the default value for the SPN.
     * - 1) A 403 should be received since we used an invalid SPN.
     * - 2) A 200 should be received since we are using a valid SPN.
     */

    @AllowedFFDC({ "org.ietf.jgss.GSSException" })
    @Test
    public void testSPNAttributeNoneThenAddInvalidThenAddValid() {
        try {

            //Do a Spnego servlet call with the default config no SPN Attribute
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

            //reconfigue the server to use an invalid SPN.
            testHelper.reconfigureServer("serverSpnegoWithInvalidSPNAttribute.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4308E", "CWWKS4309E");
            generateSpnegoTokenInsideTest();
            //The test should fail with 403.
            commonUnsuccessfulSpnegoServletCall(HttpServletResponse.SC_FORBIDDEN);

            //new error messages and ffdc errors should be thrown
            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_SPN_CWWKS4308E);
            checkMsgs.add(MessageConstants.CANNOT_CREATE_GSSCREDENTIAL_FOR_ANY_SPN_CWWKS4309E);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.EXPECTED_MESSAGE);

            //now reconfigure the server with a valid spn attribute.
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverSpnegoWithSPNAttribute.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            //the test should now pass.
            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Reconfigure the server to add bad authFilter config to the spnego configuration
     * - Do a SPNEGO servlet call with this configuration. This should fail with 401.
     * - Next reconfigure the server to use a good authfilter configuration.
     * - Do another SPNEGO servlet call. This time should pass with a 200.
     *
     * Expected results:
     * - 1) A 401 should be thrown on the first attempt since we are using a bad auth filter configuration.
     * - 2) A 200 should be received since we switch to use the good authfilter configuration.
     */

    @Test
    public void testMultipleAuthFilterConfigFromBadtoGoodURLPattern() {
        try {

            //Now we are going to proceed and modify the configuration to use the bad Auth Filter and see if we get the 401 exception
            //as expected.
            testHelper.reconfigureServer("serverMultipleAuthFiltersUseBad.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

            //Next we are going to reconfigure the server to use the correct auth filter config and see if we can get the expected 200.
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverMultipleAuthFiltersUseGood.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Next reconfigure the server to use a good authfilter configuration.
     * - Do another SPNEGO servlet call. This time should pass with a 200.
     * - Reconfigure the server to add bad authFilter config to the spnego configuration
     * - Do a SPNEGO servlet call with this configuration. This should fail with 401.
     *
     * Expected results:
     * - 1) A 200 should be received since we switch to use the good authfilter configuration.
     * - 2) A 401 should be thrown on the first attempt since we are using a bad auth filter configuration.
     */

    @Test
    public void testMultipleAuthFilterConfigFromGoodtoBadURLPattern() {
        try {

            //Next we are going to reconfigure the server to use the correct auth filter config and see if we can get the expected 200.
            testHelper.reconfigureServer("serverMultipleAuthFiltersUseGood.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

            //Now we are going to proceed and modify the configuration to use the bad Auth Filter and see if we get the 401 exception
            //as expected.
            List<String> startMsgs = new ArrayList<String>();
            startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverMultipleAuthFiltersUseBad.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }
}
