/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DynamicAuthFilterTest extends CommonTest {

    private static final Class<?> c = DynamicAuthFilterTest.class;
    private static final boolean checkforAuthFilterMsg = false;

    @BeforeClass
    public static void setUp() throws Exception {

        Log.info(c, "setUp", "Starting the server...");
        CommonTest.commonSetUp("DynamicAuthFilterTest", null, AuthFilterConstants.NO_APPS, AuthFilterConstants.NO_PROPS,
                               AuthFilterConstants.START_SERVER);
    }

    @Before
    public void obtainSSOCookie() throws Exception {
        ssoCookie = getAndAssertSSOCookieForUser(AuthFilterConstants.USER0, AuthFilterConstants.USER0_PWD,
                                                 AuthFilterConstants.IS_EMPLOYEE, AuthFilterConstants.IS_NOT_MANAGER);
    }

    /**
     * Test description:
     * - Reconfigure the server to set a malformed IP in the authfilter
     * - Do a successful a servlet call since all resources will be protected.
     * - Reconfigure the server to add elements on the authfilter.
     * - Do another successful a servlet call.
     * Expected results:
     * - 1) A 200 should be received since all resources will be protected.(but expect the error message to pop out).
     * - 2) Another 200 since we use a valid configuration.
     */

    // @Test
    @AllowedFFDC({ "java.net.UnknownHostException",
                   "com.ibm.ws.security.authentication.filter.internal.FilterException" })
    public void testAuthFilterMalformedIPtoCorrectIp() throws Exception {
        // we now update the auth filter to add a bad URL pattern and do an
        // unsucessful a servlet call
        testHelper.reconfigureServer("serverAuthFilterRemoteAddressWithMalformedIp.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonSuccessfulLtpaServletCall(ssoCookie);
        testHelper.checkForMessages(true, MALFORMED_IPRANGE_SPECIFIED_CWWKS4354E);

        testHelper.reconfigureServer("ltpaDefaultConfig.xml", name.getMethodName(), null,
                                     AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonSuccessfulLtpaServletCall(ssoCookie);

        List<String> checkMsgs = new ArrayList<String>();
        checkMsgs.add(MALFORMED_IPRANGE_SPECIFIED_CWWKS4354E);
        testHelper.waitForMessages(checkMsgs, AuthFilterConstants.MESSAGE_NOT_EXPECTED,
                                   AuthFilterConstants.MESSAGE_NOT_EXPECTED_LOG_SEARCH_TIMEOUT);
    }

    /**
     * Test description:
     * - Do a Servlet call with valid LTPA token and correct URL Pattern.
     * - Then reconfigure the server in order to set an authFilter config with badURLPattern.
     * - Do another a Servlet call with valid LTPA Token but set to a different URL Pattern.
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all required configs.
     * - 2) After the server has been reconfigured to add the bad URL pattern, we should get a 401
     */

    @Test
    public void testAuthFilterURLPatternValidThenInvalid() throws Exception {
        testHelper.reconfigureServer("ltpaDefaultConfig.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);

        commonSuccessfulLtpaServletCall(ssoCookie);

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add(AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
        testHelper.reconfigureServer("serverAuthFilterBadURLPattern.xml", name.getMethodName(),
                                     startMsgs, AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonUnsuccessfulLtpaServletCall(ssoCookie);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description:
     * - Then reconfigure the server in order to set an authFilter config with badURLPattern.
     * - Do a Servlet call with valid LTPA Token but set to a different URL Pattern.
     * - Then reconfigure the server in order to set a good url pattern.
     * - Do a Servlet call with correct LTPA token and valid URL Pattern.
     * Expected results:
     * - 1) After the server has been reconfigured to add the bad URL pattern, we should get a 401
     * - 2) A 200 should be received since we reconfigured the server to include all required configs.
     */
    @Test
    public void testAuthFilterURLPatternInvalidThenValid() throws Exception {
        testHelper.reconfigureServer("serverAuthFilterBadURLPattern.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.RESTART_SERVER, checkforAuthFilterMsg);
        commonUnsuccessfulLtpaServletCall(ssoCookie);
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add(AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
        testHelper.reconfigureServer("ltpaDefaultConfig.xml", name.getMethodName(), startMsgs,
                                     AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonSuccessfulLtpaServletCall(ssoCookie);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_PROCESSED_CWWKS4358I);
    }

    /**
     * Test description:
     * - Do a Servlet call with valid LTPA token and correct URL Pattern.
     * - Then reconfigure the server in order to modify the requestURL to use an invalid url pattern
     * - Do another Servlet call with valid LTPA Token but bad requestURL URL pattern.
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all required configs.
     * - 2) After the server has been reconfigured to add the bad URL pattern, we should get a 401
     */

    @Test
    public void testAuthFilterRequestUrlPatternValidThenInvalid() throws Exception {

        testHelper.reconfigureServer("ltpaDefaultConfig.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.RESTART_SERVER, checkforAuthFilterMsg);
        commonSuccessfulLtpaServletCall(ssoCookie);

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add(AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I);

        testHelper.reconfigureServer("serverRequestURLBadUrlPattern.xml", name.getMethodName(), startMsgs,
                                     AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonUnsuccessfulLtpaServletCall(ssoCookie);
    }

    /**
     * Test description:
     * - Then reconfigure the server in order to modify the requestURL to use an invalid url pattern
     * - Do a Servlet call with valid LTPA Token but bad requestURL URL pattern. - Then reconfigure the server in order to set a good url pattern.
     * - Do a Servlet call with correct LTPA token and valid URL Pattern.
     * Expected results:
     * - 1) After the server has been reconfigured to add the bad URL pattern, we should get a 401
     * - 2) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testAuthFilterRequestURLPatternInvalidThenValid() throws Exception {
        testHelper.reconfigureServer("serverRequestURLBadUrlPattern.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonUnsuccessfulLtpaServletCall(ssoCookie);

        // reconfigure the server to add the original configuration and do
        // sucessful LTPA cookie call.
        List<String> startMsgs = new ArrayList<String>();
        testHelper.reconfigureServer("ltpaDefaultConfig.xml", name.getMethodName(), startMsgs, AuthFilterConstants.DONT_RESTART_SERVER, false);
        commonSuccessfulLtpaServletCall(ssoCookie);
    }

    /**
     * Test description:
     * - Do a Servlet call with valid LTPA token and requestUrl matchType equals to contain
     * - Then reconfigure the server in order to modify the requestURL to use matchtype equals to notContain
     * - Do another Servlet call with valid LTPA Token but matchtype equals to notContain
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all required configs.
     * - 2) After the server has been reconfigured to matchtype equals to notContain, we should get a 401
     */

    @Test
    public void testAuthFilterRequestUrlMatchContainsToNotContain() throws Exception {
        testHelper.reconfigureServer("ltpaDefaultConfig.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonSuccessfulLtpaServletCall(ssoCookie);

        testHelper.reconfigureServer("serverRequestURLBadUrlPattern.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonUnsuccessfulLtpaServletCall(ssoCookie);
    }

    /**
     * Test description:
     * - Reconfigure the server in order to modify the requestURL matchType to notContain
     * - Do a Servlet call with valid LTPA Token but setting the matchType notContain
     * - Then reconfigure the server in order to set the match type to contain
     * - Do a Servlet call with correct LTPA token and valid URL Pattern.
     * Expected results:
     * - 1) After the server has been reconfigured to set the matchType to notContain, we should get a 401
     * - 2) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testAuthFilterRequestUrlMatchNotContainsToContain() throws Exception {
        testHelper.reconfigureServer("serverRequestURLBadUrlPattern.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonUnsuccessfulLtpaServletCall(ssoCookie);

        testHelper.reconfigureServer("ltpaDefaultConfig.xml", name.getMethodName(), AuthFilterConstants.NO_MSGS,
                                     AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonSuccessfulLtpaServletCall(ssoCookie);
    }

    /**
     * Test description:
     * - Reconfigure the server to change the requestURL to webapp with matchtype equals contains.
     * - Do a servlet call
     * - Then reconfigure the server in order to modify the webAppL to use the match type notContain
     * - Do another Servlet call with valid LTPA Token but the matchtype set to notContain
     * Expected results:
     * - 1) A 200 should be received since we reconfigured the server to include all required configs.
     * - 2) After the server has been reconfigured to add the notContain, we should get a 401
     */

    @Test
    public void testAuthFilterWebAppContainsToNotContain() throws Exception {
        testHelper.reconfigureServer("serverAuthFilterWebAppContains.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonSuccessfulLtpaServletCall(ssoCookie);

        // we now update the auth filter to use webApp and matchtype Notcontain
        // and do an unsucessful LTPA call
        testHelper.reconfigureServer("serverAuthFilterWebAppNotContain.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonUnsuccessfulLtpaServletCall(ssoCookie);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I);
    }

    /**
     * Test description:
     * - Reconfigure the server in order to modify the webAppL to use the match type notContain
     * - Do another LTAP Servlet call with valid SPENGO Token but the matchtype set to notContain
     * - Reconfigure the server to change the requestURL to webapp with matchtype equals contains.
     * - Do a servlet call
     * Expected results:
     * - 1) After the server has been reconfigured to add the notContain, we should get a 401
     * - 2) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testAuthFilterWebAppNotContainsToContain() throws Exception {
        // we now update the auth filter to use webApp and matchtype Notcontain
        // and do an unsucessful LTPA call
        testHelper.reconfigureServer("serverAuthFilterWebAppNotContain.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonUnsuccessfulLtpaServletCall(ssoCookie);

        // we now update the auth filter to use webApp and matchtype contain and
        // do an sucessful LTPA call
        testHelper.reconfigureServer("serverAuthFilterWebAppContains.xml", name.getMethodName(),
                                     AuthFilterConstants.NO_MSGS, AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I);
        commonSuccessfulLtpaServletCall(ssoCookie);
    }

    /**
     * Test description:
     * - Do a servlet call using the reqUrl.
     * - Reconfigure the server to change the requestURL to webapp with match type equals notContain
     * - Reconfigure the server to change the matchtype equals to contains.
     * - Do a servlet call with webAPP specified in the server.xml
     * Expected results:
     * - 1) A 200 should be obtained since we are using the default config.
     * - 2) A 401 should be obtain because we are using a matchtype notContain in the webAPP
     * - 3) A 200 should be received since we reconfigured the server to include all required configs.
     */

    @Test
    public void testAuthFilterFromRequestUrlToWebAppMatchNotContainToWebAppMatchContain() throws Exception {
        // we now update the auth filter to use webApp and matchtype Notcontain
        // and do an unsucessful LTPA call
        testHelper.reconfigureServer("serverAuthFilterWebAppNotContain.xml", name.getMethodName(), null,
                                     AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonUnsuccessfulLtpaServletCall(ssoCookie);

        testHelper.reconfigureServer("serverAuthFilterWebAppContains.xml", name.getMethodName(), null,
                                     AuthFilterConstants.DONT_RESTART_SERVER, checkforAuthFilterMsg);
        commonSuccessfulLtpaServletCall(ssoCookie);
        testHelper.checkForMessages(true, AUTHENTICATION_FILTER_MODIFIED_CWWKS4359I);
    }

}