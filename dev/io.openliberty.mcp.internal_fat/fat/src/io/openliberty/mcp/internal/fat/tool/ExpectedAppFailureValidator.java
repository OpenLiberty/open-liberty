/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.matchers.Matchers;
import componenttest.topology.impl.LibertyServer;

public class ExpectedAppFailureValidator {

    private static String APP_STARTED_CODE = "CWWKZ000[13]I";
    public static String APP_START_FAILED_CODE = "CWWKZ000[24]E";

    /**
     * @param server server the liberty server (configured to disable validation for intentional app failures)
     * @param appName The name of the app used to encapsulate the application
     * @param packageInfo the package for where all of the test classes will be located
     * @throws Exception
     */
    public static void deployAppToAssertFailure(LibertyServer server, String appName, Package packageInfo) throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, appName + ".war").addPackage(packageInfo);
        ShrinkHelper.exportDropinAppToServer(server, war, DISABLE_VALIDATION, SERVER_ONLY);
        server.startServer();
        assertThatAppHasFailed(server, appName);
    }

    /**
     * Asserts that a log message is found indicating that an app has failed to start.
     * This should usually be called before checking for any other validation messages.
     *
     * @param server the liberty server
     * @param appName the app name to search for (also used in naming the war file)
     */
    private static void assertThatAppHasFailed(LibertyServer server, String appName) throws Exception {
        String logLine = server.waitForStringInLog(APP_STARTED_CODE + "|" + APP_START_FAILED_CODE + ".*" + Pattern.quote(appName));
        assertThat("App " + appName + " start failed. Found log: " + logLine, logLine, allOf(notNullValue(), Matchers.containsPattern(APP_START_FAILED_CODE)));
    }

    /**
     * @param noErrorsFoundMsg the error message if the expected errors are not found in the logs
     * @param expectedErrorHeader each list of error messages is preceded by a header with details about the error list
     * @param expectedErrorList a list of expected error messages
     * @param server the liberty server
     * @throws Exception
     */
    public static void findAndAssertExpectedErrorsInLogs(String noErrorsFoundMsg, String expectedErrorHeader, List<String> expectedErrorList, LibertyServer server)
                    throws Exception {
        assertTrue("Expected header not found: " + expectedErrorHeader, !server.findStringsInLogs(expectedErrorHeader).isEmpty());
        for (String err : expectedErrorList) {
            assertTrue(noErrorsFoundMsg + " [" + err + "] expected and not found", !server.findStringsInLogs(err).isEmpty());
        }
    }
}
