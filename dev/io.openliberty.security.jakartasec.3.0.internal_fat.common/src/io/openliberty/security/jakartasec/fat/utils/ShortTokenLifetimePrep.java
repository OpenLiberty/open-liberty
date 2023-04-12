/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.utils;

import java.util.Arrays;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectations;

import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;

public class ShortTokenLifetimePrep extends CommonAnnotatedSecurityTests {

    /**
     * Try to access a protected app to have the OP issue tokens - issuing the first token after starting
     * the server is taking too long for tests that use tokens with short lifetimes. The tokens are expired
     * before the client gets them back from the OP. We're not seeing the problem after the first tokens are
     * issued.
     *
     * @param httpsBase - Base of the url of the app to invoke https://localhost:<port>/
     * @param appName - unique name of the app to invoke
     * @throws Exception
     */
    public void shortTokenLifetimePrep(LibertyServer opServer, String httpsBase, String appName) throws Exception {

        try {

            String thisMethod = "shortTokenLifetimePrep";
            loggingUtils.printMethodName(thisMethod);

            // sleep and give apps a little more time to get ready
            actions.testLogAndSleep(30);

            // if the tokens are expired by the time the op returns them, we can get an error out of userinfo
            // since we're using this method, we know that this possibility existss - just ignore the message
            opServer.addIgnoredErrors(Arrays.asList(MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN));

            WebClient webClient = getAndSaveWebClient();

            initResponseValues();

            String url = httpsBase + "/" + appName;

            Page response = invokeAppReturnLoginPage(webClient, url);

            response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);
            // confirm protected resource was accessed
            // Just check for a good status code - on faster machines, we'll land on the test app, on
            // slow machines, we'll land on the logout page (since the token will be expired by the time it's returned
            Expectations expectations = new Expectations();
            expectations.addSuccessCodeForCurrentAction();
            validationUtils.validateResult(response, expectations);

        } catch (Exception e) {
            Log.info(thisClass, "shortTokenLifetimePrep",
                     "A failure occurred trying to \"warm up\" the OP (meaning it takes a long time to issue the very first token and short token lifetime tests will time out)");
            Log.info(thisClass, "shortTokenLifetimePrep", e.getMessage());
        }

    }
}