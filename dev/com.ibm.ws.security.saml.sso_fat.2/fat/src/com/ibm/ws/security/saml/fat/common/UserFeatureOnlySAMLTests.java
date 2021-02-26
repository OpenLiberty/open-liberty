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
package com.ibm.ws.security.saml.fat.common;

import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;
import com.ibm.ws.security.saml20.fat.commonTest.SampleUserFeatureInstaller;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/*
 *
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class UserFeatureOnlySAMLTests extends SAMLCommonTest {

    private static final Class<?> thisClass = UserFeatureOnlySAMLTests.class;

    static {
        samlUserMappingUserFeature = new SampleUserFeatureInstaller(); // install the user Feature
    }

    /**
     * This test uses a server that only includes the user feature - it does NOT define the user feature instance.
     * Because just the feature was included, we will get standard SAML function - no user substitution will
     * be done.
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void userFeatureOnlySAMLTests_mainFlowTest() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        updatedTestSettings.setIdpUserName("user1");
        updatedTestSettings.setIdpUserPwd("security");
        updatedTestSettings.setSamlTokenValidationData(updatedTestSettings.getIdpUserName(), updatedTestSettings.getSamlTokenValidationData().getIssuer(), updatedTestSettings.getSamlTokenValidationData().getInResponseTo(), updatedTestSettings.getSamlTokenValidationData().getMessageID(), updatedTestSettings.getSamlTokenValidationData().getEncryptionKeyUser(), updatedTestSettings.getSamlTokenValidationData().getRecipient(), updatedTestSettings.getSamlTokenValidationData().getEncryptAlg());

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (snoop)", null, "<tr><td>User Principal</td><td>user1</td></tr>");
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not see the correct user in the repsonse output (SimpleServlet)", null, "WSPrincipal:user1");

        genericSAML(_testName, webClient, updatedTestSettings, standardFlowAltAppAgain, expectations);

    }

}
