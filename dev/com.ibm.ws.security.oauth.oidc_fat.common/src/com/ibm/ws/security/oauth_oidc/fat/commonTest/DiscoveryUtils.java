/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.util.List;

import org.joda.time.Instant;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebRequest;

public class DiscoveryUtils {

    public static Class<?> thisClass = DiscoveryUtils.class;

    public static ValidationData vData = new ValidationData();
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    public static CommonTestHelpers helpers = new CommonTestHelpers();
    public static CommonValidationTools validationTools = new CommonValidationTools();

    // wait up to 20 seconds for discovery to be ready
    public static void waitForOPDiscoveryToBeReady(TestSettings settings) throws Exception {

        msgUtils.printMethodName("waitForDiscoveryToBeReady");

        WebConversation wc = new WebConversation();
        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_DISCOVERY_ENDPOINT, Constants.OK_STATUS);
        int timeout = 20;
        Long startTime = new Instant().getMillis();
        Long nowTime = startTime;
        String msg = "Waited " + timeout + " seconds and the discovery endpoint is still not responding correctly.";
        while (startTime + (timeout * 1000) > nowTime) {
            Log.info(thisClass, "waitForDiscoveryToBeReady", "in time loop");
            try {
                invokeDiscovery(wc, settings, Constants.INVOKE_DISCOVERY_ENDPOINT, expectations);
                timeout = 0; // to stop the loop
                msg = "The discovery endpoint appears to be working properly";
            } catch (Exception e) {
                Log.info(thisClass, "waitForDiscoveryToBeReady", e.toString());
            }
            nowTime = new Instant().getMillis();
        }
        Log.info(thisClass, "waitForDiscoveryToBeReady", msg);
        msgUtils.printMethodName("waitForDiscoveryToBeReady");
    }

    public static void waitForRPDiscoveryToBeReady(TestSettings testSettings) throws Exception {

        String thisMethod = "waitForRPDiscoveryToBeReady";
        int maxAttempts = 5;
        int tryNum = 1;

        while (tryNum <= maxAttempts) {
            WebConversation wc = new WebConversation();
            WebRequest request = new GetMethodWebRequest(testSettings.getTestURL());
            WebResponse response = wc.getResponse(request);
            msgUtils.printResponseParts(response, thisMethod, "Response when trying to use Discovered Server settings: ");

            int status = AutomationTools.getResponseStatusCode(response);
            if (status == Constants.OK_STATUS) {
                Log.info(thisClass, thisMethod, "Was able to to use one of the OpenidConnect clients discovery data");
                break;
            } else {
                Log.info(thisClass, thisMethod, "Discovery does not appear to be ready yet - try #" + tryNum);
                helpers.testSleep(5); // sleep 5 seconds before the next attempt
                tryNum++;
            }
        }

    }

    public static void invokeDiscovery(WebConversation wc, TestSettings settings, String action, List<validationData> expectations) throws Exception {

        WebRequest request = null;
        WebResponse response = null;
        String thisMethod = "invokeDiscovery";

        msgUtils.printMethodName(thisMethod);
        try {

            helpers.setMarkToEndOfAllServersLogs();

            Log.info(thisClass, thisMethod, "Endpoint URL: " + settings.getDiscoveryEndpt());

            request = new GetMethodWebRequest(settings.getDiscoveryEndpt());

            Log.info(thisClass, thisMethod, "Making request: " + request.toString());
            response = wc.getResponse(request);
            msgUtils.printResponseParts(response, thisMethod, "Invoke with Parms and Headers: ");
        } catch (HttpException e) {

            Log.info(thisClass, thisMethod, "Exception message: " + e.getMessage());
            Log.info(thisClass, thisMethod, "Exception response code: " + e.getResponseCode());
            Log.info(thisClass, thisMethod, "Exception response message: " + e.getResponseMessage());
            Log.info(thisClass, thisMethod, "Exception Cause: " + e.getCause());

            validationTools.validateException(expectations, action, e);

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception message: " + e.getMessage());
            Log.info(thisClass, thisMethod, "Exception stack: " + e.getStackTrace());
            Log.info(thisClass, thisMethod, "Exception response message: " + e.getLocalizedMessage());
            Log.info(thisClass, thisMethod, "Exception cause: " + e.getCause());

            validationTools.validateException(expectations, action, e);

        }

        validationTools.validateResult(response, action, expectations, settings);
    }
}
