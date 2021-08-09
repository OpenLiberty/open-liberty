/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.log.Log;
import com.meterware.httpunit.WebResponse;

/**
 * This is the test class that contains common code for all of the
 * OpenID Connect RP tests. There will be OP specific test classes that extend this class.
 **/

public class ClientTestHelpers extends CommonTest {

    public static Class<?> thisClass = ClientTestHelpers.class;

    public static boolean processIDToken(WebResponse response) throws Exception {

        boolean isTokenValid = false;

        try {
            // For now just check if IDToken is present in the response
            String respText = response.getText();
            Pattern tokenPattern = Pattern.compile(Constants.IDToken_STR);
            Matcher m = tokenPattern.matcher(respText);
            if (m.find()) {
                isTokenValid = true;
            }
        } catch (Exception e1) {
            Log.info(thisClass, "processIDtokenHACK", "Exception received processing IDToken " + e1.getMessage());
        }

        return isTokenValid;

    }

    //  reconfig the op and rp servers, but not the generic server - no additional msgs to be checked for either server
    public static void reconfigServers(String testName, Boolean junitReporting, String opServerXml, String rpServerXml) throws Exception {
        reconfigServers(testName, junitReporting, opServerXml, Constants.NO_EXTRA_MSGS, rpServerXml, Constants.NO_EXTRA_MSGS, null, null);
    }

    //  reconfig the op, rp and generic servers - no additional msgs to be checked for either server
    public static void reconfigServers(String testName, Boolean junitReporting, String opServerXml, String rpServerXml, String genericServerXml) throws Exception {
        reconfigServers(testName, junitReporting, opServerXml, Constants.NO_EXTRA_MSGS, rpServerXml, Constants.NO_EXTRA_MSGS, genericServerXml, Constants.NO_EXTRA_MSGS);
    }

    public static void reconfigServers(String testName, Boolean junitReporting, String opServerXml, List<String> opStartMessages,
            String rpServerXml, List<String> rpStartMessages,
            String genericServerXml, List<String> genericStartMessages) throws Exception {

        testOPServer.reconfigServer(opServerXml, testName, junitReporting, opStartMessages);
        testRPServer.reconfigServer(rpServerXml, testName, junitReporting, rpStartMessages);
        if (genericTestServer != null) {
            genericTestServer.reconfigServer(genericServerXml, testName, junitReporting, genericStartMessages);
        }

    }

    //  reconfig the op and rp servers, but not the generic server - no additional apps or msgs to be checked for either server
    public static void restartServers(String testName, Boolean junitReporting, String opServerXml, String rpServerXml) throws Exception {
        restartServers(testName, junitReporting, opServerXml, Constants.NO_EXTRA_APPS, Constants.NO_EXTRA_MSGS,
                rpServerXml, Constants.NO_EXTRA_APPS, Constants.NO_EXTRA_MSGS,
                null, null, null);
    }

    //  reconfig the op, rp and generic servers - no additional msgs to be checked for either server
    public static void restartServers(String testName, Boolean junitReporting, String opServerXml, String rpServerXml, String genericServerXml) throws Exception {
        restartServers(testName, junitReporting, opServerXml, Constants.NO_EXTRA_APPS, Constants.NO_EXTRA_MSGS,
                rpServerXml, Constants.NO_EXTRA_APPS, Constants.NO_EXTRA_MSGS,
                genericServerXml, Constants.NO_EXTRA_APPS, Constants.NO_EXTRA_MSGS);
    }

    public static void restartServers(String testName, Boolean junitReporting, String opServerXml, List<String> opApps, List<String> opStartMessages,
            String rpServerXml, List<String> rpApps, List<String> rpStartMessages,
            String genericServerXml, List<String> genericApps, List<String> genericStartMessages) throws Exception {

        testOPServer.restartServer(opServerXml, testName, opApps, opStartMessages, junitReporting);
        testRPServer.restartServer(rpServerXml, testName, rpApps, rpStartMessages, junitReporting);
        if (genericTestServer != null) {
            genericTestServer.restartServer(genericServerXml, testName, genericApps, genericStartMessages, junitReporting);
        }

    }
}
