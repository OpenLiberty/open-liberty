/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.saml.fat.jaxrs.config.IDPInitiated;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml.fat.jaxrs.config.utils.RSSamlConfigSettings;
import com.ibm.ws.security.saml.fat.jaxrs.config.utils.RSSamlProviderSettings;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link to an
 * application installed on a WebSphere SP. When the Web client invokes the SP
 * application, it is redirected to a TFIM IdP which issues a login challenge to
 * the Web client. The Web Client fills in the login form and after a successful
 * login, receives a SAML 2.0 token from the TFIM IdP. The client invokes the SP
 * application by sending the SAML 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RSSamlIDPInitiatedSSLConfigWithReconfigTests extends RSSamlIDPInitiatedConfigCommonTests {

    /*****************************************
     * TESTS
     **************************************/

    /**************************************
     * signatureMethodAlgorithm
     **************************************/

    /**
     * Test purpose: - signatureMethodAlgorithm: SHA1 Expected results: - The
     * SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_signatureMethodAlgorithm_SHA1() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setSignatureMethodAlgorithm("SHA1");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - signatureMethodAlgorithm: SHA128 Expected results: - The
     * SAML token should be successfully processed by JAX-RS.
     *
     * @throws Exception
     */
    @Test
    public void RSSamlIDPInitiatedConfigTests_signatureMethodAlgorithm_SHA128() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setSignatureMethodAlgorithm("SHA128");

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, testSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - signatureMethodAlgorithm: SHA256 Expected results: - 401
     * when invoking JAX-RS. - CWWKS5049E message in the app server log saying
     * the signature was not valid (weaker than required).
     *
     * @throws Exception
     */
    //@ExpectedFFDC({ "com.ibm.ws.security.saml.error.SamlException", "org.opensaml.messaging.handler.MessageHandlerException" })
    //@Test - TODO : enable this test when we update signature algorithm config to support higher than sha256
    public void RSSamlIDPInitiatedConfigTests_signatureMethodAlgorithm_SHA256() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setSignatureMethodAlgorithm("SHA256");

        List<validationData> expectations = get401ExpectationsForJaxrsGet("Did not get the expected message saying the received signature was not valid and weaker than required.",
                SAMLMessageConstants.CWWKS5049E_SIGNATURE_NOT_TRUSTED_OR_VALID);

        generalConfigTest(updatedRsSamlSettings, expectations, testSettings);
    }

    /**
     * Test purpose: - signatureMethodAlgorithm: SHA128 - SAML SP specifies
     * SHA256 as the signature algorithm Expected results: - TODO
     *
     * @throws Exception
     */
    // !@Test
    public void RSSamlIDPInitiatedConfigTests_signatureMethodAlgorithm_SHA128_sp256() throws Exception {

        RSSamlConfigSettings updatedRsSamlSettings = rsConfigSettings.copyConfigSettings();
        RSSamlProviderSettings updatedProviderSettings = updatedRsSamlSettings.getDefaultRSSamlProviderSettings();
        updatedProviderSettings.setSignatureMethodAlgorithm("SHA128");

        // Update test settings to use an SP that encrypts SAML assertions
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", SP_SIG_ALG_SHA256, true);
        updatedTestSettings.setSpecificIDPChallenge(2);

        List<validationData> expectations = commonUtils.getGoodExpectationsForJaxrsGet(flowType, updatedTestSettings);

        generalConfigTest(updatedRsSamlSettings, expectations, updatedTestSettings);
    }
}
