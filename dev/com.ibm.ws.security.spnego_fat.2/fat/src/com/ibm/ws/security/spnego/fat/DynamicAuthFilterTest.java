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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.MessageConstants;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
public class DynamicAuthFilterTest extends CommonTest {

    private static final Class<?> c = DynamicAuthFilterTest.class;

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(c, "setUp", "Starting the server and kerberos setup...");
        commonSetUp("DynamicAuthFilterTest");
    }

    /**
     * x
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
     * - Reconfigure the server to remove the elements of the authfilter
     * - Do a successful spnego servlet call since all resources will be protected.
     * - Reconfigure the server to add elements on the authfilter.
     * - do another successfull spnego servlet call.
     * Expected results:
     * - 1) A 200 should be received since all resources will be protected.(but expect the correct message to pop out)
     * - 2) Another 200 since we use a valid configuration.
     */

    //@Test
    public void testAuthFilterElementsNotSpecifiedtoSpecified() {
        try {

            testHelper.reconfigureServer("serverAuthFilterRefNoElementSpecified.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();
            List<String> msgs = new ArrayList<String>();

            testHelper.checkForMessages(true, MessageConstants.SPN_NOT_SPECIFIED_CWWKS4314I);

            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I, MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();
            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Reconfigure the server to remove the id of the authfilter
     * - Do a sucessful spnego servlet call since all resources will be protected.
     * - Reconfigure the server to add elements on the authfilter.
     * - do another sucessfull spnego servlet call.
     * Expected results:
     * - 1) A 200 should be received since all resources will be protected.(but expect the error message to pop out).
     * - 2) Another 200 since we use a valid configuration.
     */

    @Test
    public void testAuthFilterIDNotSpecifiedtoSpecified() {
        try {
            List<String> startMsgs = new ArrayList<String>();

            startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();

            List<String> checkMsgs = new ArrayList<String>();
            //Since the CWWKS4357I message should not show since the SPN is being specified in the server.xml
            checkMsgs.add(MessageConstants.AUTHENTICATION_FILTER_MISSING_ID_ATTRIBUTE_CWWKS4360E);
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
     * - Reconfigure the server to set a malformed IP in the authfilter
     * - Do a successful spnego servlet call since all resources will be protected.
     * - Reconfigure the server to add elements on the authfilter.
     * - do another successful spnego servlet call.
     * Expected results:
     * - 1) A 200 should be received since all resources will be protected.(but expect the error message to pop out).
     * - 2) Another 200 since we use a valid configuration.
     */

    @Test
    @AllowedFFDC({ "java.net.UnknownHostException", "com.ibm.ws.security.authentication.filter.internal.FilterException" })
    public void testAuthFilterMalformedIPtoCorrectIp() {
        try {
            //we now update the auth filter to add a bad URL pattern and do an unsucessful spnego call
            testHelper.reconfigureServer("serverAuthFilterRemoteAddressWithMalformedIp.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.MALFORMED_IPRANGE_SPECIFIED_CWWKS4354E);

            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();

            commonSuccessfulSpnegoServletCall();

            List<String> checkMsgs = new ArrayList<String>();
            //Since the CWWKS4357I message should not show since the SPN is being specified in the server.xml
            checkMsgs.add(MessageConstants.MALFORMED_IPRANGE_SPECIFIED_CWWKS4354E);
            testHelper.waitForMessages(checkMsgs, SPNEGOConstants.MESSAGE_NOT_EXPECTED, SPNEGOConstants.MESSAGE_NOT_EXPECTED_LOG_SEARCH_TIMEOUT);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Do a SPNEGO Servlet call with valid SPNEGO token and correct URL Pattern.
     * - Then reconfigure the server in order to set an authFilter config with badURLPattern.
     * - Do another SPNEGO Servlet call with valid SPENGO Token but set to a different URL Pattern.
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all required configs.
     * - 2) After the server has been reconfigured to add the bad URL pattern, we should get a 401
     */

    @Test
    public void testAuthFilterURLPatternValidThenInvalid() {
        try {

            commonSuccessfulSpnegoServletCall();
            testHelper.reconfigureServer("serverAuthFilterBadURLPattern.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Then reconfigure the server in order to set an authFilter config with badURLPattern.
     * - Do a SPNEGO Servlet call with valid SPENGO Token but set to a different URL Pattern.
     * - Then reconfigure the server in order to set a good url pattern.
     * - Do a SPNEGO Servlet call with correct SPNEGO token and valid URL Pattern.
     * Expected results:
     * - 1) After the server has been reconfigured to add the bad URL pattern, we should get a 401
     * - 2) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testAuthFilterURLPatternInvalidThenValid() {
        try {
            //reconfigure the server to add bad url pattern then do a unsucessful spnego call
            testHelper.reconfigureServer("serverAuthFilterBadURLPattern.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

            //reconfigure the server to add the original configuration and do sucessful spnego call.
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
     * - Do a SPNEGO Servlet call with valid SPNEGO token and correct URL Pattern.
     * - Then reconfigure the server in order to modify the requestURL to use an invalid url pattern
     * - Do another SPNEGO Servlet call with valid SPENGO Token but bad requestURL URL pattern.
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all required configs.
     * - 2) After the server has been reconfigured to add the bad URL pattern, we should get a 401
     */

    @Test
    public void testAuthFilterRequestUrlPatternValidThenInvalid() {
        try {
            List<String> startMsgs = new ArrayList<String>();

            //do a sucessful spnego call
            commonSuccessfulSpnegoServletCall();

            //we now update the auth filter to add a bad URL pattern and do an unsucessful spnego call
            startMsgs.add(MessageConstants.AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I);
            testHelper.reconfigureServer("serverRequestURLBadUrlPattern.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
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
     * - Then reconfigure the server in order to modify the requestURL to use an invalid url pattern
     * - Do a SPNEGO Servlet call with valid SPENGO Token but bad requestURL URL pattern.
     * - Then reconfigure the server in order to set a good url pattern.
     * - Do a SPNEGO Servlet call with correct SPNEGO token and valid URL Pattern.
     * Expected results:
     * - 1) After the server has been reconfigured to add the bad URL pattern, we should get a 401
     * - 2) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testAuthFilterRequestURLPatternInvalidThenValid() {
        try {
            //reconfigure the server to add bad url pattern then do a unsucessful spnego call
            testHelper.reconfigureServer("serverRequestURLBadUrlPattern.xml", name.getMethodName(), false);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();

            //reconfigure the server to add the original configuration and do sucessful spnego call.
            List<String> startMsgs = new ArrayList<String>();
            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), startMsgs, false);
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
     * - Do a SPNEGO Servlet call with valid SPNEGO token and requestUrl matchType equals to contain
     * - Then reconfigure the server in order to modify the requestURL to use matchtype equals to notContain
     * - Do another SPNEGO Servlet call with valid SPENGO Token but matchtype equals to notContain
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all required configs.
     * - 2) After the server has been reconfigured to matchtype equals to notContain, we should get a 401
     */

    @Test
    public void testAuthFilterRequestUrlMatchContainsToNotContain() {
        try {
            List<String> startMsgs = new ArrayList<String>();

            //do a sucessful spnego call
            commonSuccessfulSpnegoServletCall();

            //reconfigure the server to use notcontain then do a unsucessful spnego call
            startMsgs.add(MessageConstants.AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I);
            testHelper.reconfigureServer("serverRequestURLBadUrlPattern.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
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
     * - Reconfigure the server in order to modify the requestURL matchType to notContain
     * - Do a SPNEGO Servlet call with valid SPENGO Token but setting the matchType notContain
     * - Then reconfigure the server in order to set the match type to contain
     * - Do a SPNEGO Servlet call with correct SPNEGO token and valid URL Pattern.
     * Expected results:
     * - 1) After the server has been reconfigured to set the matchType to notContain, we should get a 401
     * - 2) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testAuthFilterRequestUrlMatchNotContainsToContain() {
        try {
            //reconfigure the server to use notcontain then do a unsucessful spnego call
            testHelper.reconfigureServer("serverRequestURLBadUrlPattern.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();
//            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I);

            testHelper.reconfigureServer("spnegoDefaultConfig.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
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
     * - Reconfigure the server to change the requestURL to webapp with matchtype equals contains.
     * - Do a Spnego servlet call
     * - Then reconfigure the server in order to modify the webAppL to use the match type notContain
     * - Do another SPNEGO Servlet call with valid SPENGO Token but the matchtype set to notContain
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all required configs.
     * - 2) After the server has been reconfigured to add the notContain, we should get a 401
     */

    @Test
    public void testAuthFilterWebAppContainsToNotContain() {
        try {
            testHelper.reconfigureServer("serverAuthFilterWebAppContains.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

            //we now update the auth filter to use webApp and matchtype Notcontain and do an unsucessful spnego call
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverAuthFilterWebAppNotContain.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();
            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Reconfigure the server in order to modify the webAppL to use the match type notContain
     * - Do another SPNEGO Servlet call with valid SPENGO Token but the matchtype set to notContain
     * - Reconfigure the server to change the requestURL to webapp with matchtype equals contains.
     * - Do a Spnego servlet call
     * Expected results:
     * - 1) After the server has been reconfigured to add the notContain, we should get a 401
     * - 2) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testAuthFilterWebAppNotContainsToContain() {
        try {
            List<String> startMsgs = new ArrayList<String>();

            //we now update the auth filter to use webApp and matchtype Notcontain and do an unsucessful spnego call
            testHelper.reconfigureServer("serverAuthFilterWebAppNotContain.xml", name.getMethodName(), SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();
//            testHelper.checkForMessages(true, MessageConstants.AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I);

            //we now update the auth filter to use webApp and matchtype contain and do an sucessful spnego call
            startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverAuthFilterWebAppContains.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
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
     * - Do a SPNEGO servlet call using the reqUrl.
     * - Reconfigure the server to change the requestURL to webapp with match type equals notContain
     * - Reconfigure the server to change the matchtype equals to contains.
     * - Do a Spnego servlet call with webAPP specified in the server.xml
     * Expected results:
     * - 1) A 200 should be obtained since we are using the default config.
     * - 2) A 401 should be obtain because we are using a matchtype notContain in the webAPP
     * - 3) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testAuthFilterFromRequestUrlToWebAppMatchNotContainToWebAppMatchContain() {
        try {
            List<String> startMsgs = new ArrayList<String>();

            commonSuccessfulSpnegoServletCall();

            //we now update the auth filter to use webApp and matchtype Notcontain and do an unsucessful spnego call
            startMsgs.add(MessageConstants.AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I);
            testHelper.reconfigureServer("serverAuthFilterWebAppNotContain.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonUnsuccessfulSpnegoServletCall();

            //reconfigure the server to add bad url pattern then do a unsucessful spnego call
            startMsgs = new ArrayList<String>();
            startMsgs.add(MessageConstants.SPNEGO_CONFIGURATION_MODIFIED_CWWKS4301I);
            testHelper.reconfigureServer("serverAuthFilterWebAppContains.xml", name.getMethodName(), startMsgs, SPNEGOConstants.DONT_RESTART_SERVER);
            generateSpnegoTokenInsideTest();
            commonSuccessfulSpnegoServletCall();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

}