/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC;

import static java.lang.Thread.sleep;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServerWrapper;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonValue;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@LibertyServerWrapper
@RunWith(FATRunner.class)
public class OIDCJWKRotationTest extends CommonTest {
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    private static final Class<?> thisClass = OIDCJWKRotationTest.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {
        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");

        testSettings = new TestSettings();
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.server-1.0_fat", "server_jwkRotationTime.xml", Constants.OIDC_OP, null, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS);
    }

    @Test
    public void testJWKRotation_0m_ValidRotationTime_Success() throws Exception {
        Log.entering(thisClass, _testName);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();

        updatedTestSettings.setJwkEndpt(updatedTestSettings.getJwkEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_jwkRotationTime_0m"));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_JWK_ENDPOINT, "Did not find JWKs in the response.", "\"keys\":");

        // Initial request to get keys
        WebResponse firstResponse = genericInvokeEndpoint(_testName, wc, null, updatedTestSettings.getJwkEndpt(),
                Constants.GETMETHOD, Constants.INVOKE_JWK_ENDPOINT, null, null, expectations);

        List<String> firstKeys = Json.createReader(new StringReader(AutomationTools.getResponseText(firstResponse)))
                .readObject().getJsonArray("keys").stream().map(JsonValue::toString).collect(Collectors.toList());

        // Repeat request, should be the same key
        WebResponse secondResponse = genericInvokeEndpoint(_testName, wc, null, updatedTestSettings.getJwkEndpt(),
                Constants.GETMETHOD, Constants.INVOKE_JWK_ENDPOINT, null, null, expectations);

        List<String> secondKeys = Json.createReader(new StringReader(AutomationTools.getResponseText(secondResponse)))
                .readObject().getJsonArray("keys").stream().map(JsonValue::toString).collect(Collectors.toList());

        Assert.assertEquals(firstKeys, secondKeys);
    }

    @Test
    public void testJWKRotation_1m_ValidRotationTime_Success() throws Exception {
        Log.entering(thisClass, _testName);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();

        updatedTestSettings.setJwkEndpt(updatedTestSettings.getJwkEndpt().replaceAll("OidcConfigSample", "OidcConfigSample_jwkRotationTime_1m"));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_JWK_ENDPOINT, "Did not find JWKs in the response.", "\"keys\":");

        // Initial request to get keys
        WebResponse firstResponse = genericInvokeEndpoint(_testName, wc, null, updatedTestSettings.getJwkEndpt(),
                Constants.GETMETHOD, Constants.INVOKE_JWK_ENDPOINT, null, null, expectations);

        List<String> firstKeys = Json.createReader(new StringReader(AutomationTools.getResponseText(firstResponse)))
                .readObject().getJsonArray("keys").stream().map(JsonValue::toString).collect(Collectors.toList());

        // Wait rotation time
        sleep(60000);

        // Keys should not match
        WebResponse secondResponse = genericInvokeEndpoint(_testName, wc, null, updatedTestSettings.getJwkEndpt(),
                Constants.GETMETHOD, Constants.INVOKE_JWK_ENDPOINT, null, null, expectations);

        List<String> secondKeys = Json.createReader(new StringReader(AutomationTools.getResponseText(secondResponse)))
                .readObject().getJsonArray("keys").stream().map(JsonValue::toString).collect(Collectors.toList());

        Assert.assertEquals("Did not get the same number of keys in the second JWK request as the first request.", firstKeys.size(), secondKeys.size());
        Assert.assertFalse("The first set of JWKs should not have contained the same keys as those from the second JWK request.", firstKeys.containsAll(secondKeys));
        Assert.assertFalse("The second set of JWKs should not have contained the same keys as those from the first JWK request.", secondKeys.containsAll(firstKeys));
    }
}
