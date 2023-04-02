/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils;

import java.util.List;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

public class CommonTools {

    private static final Class<?> thisClass = CommonTools.class;

    public ValidationData vData = new ValidationData();

    public List<validationData> setGoodHelloWorldExpectations(TestSettings settings) throws Exception {
        return setGoodHelloWorldExpectations(null, settings);
    }

    public List<validationData> setGoodHelloWorldExpectations(List<validationData> expectations, TestSettings settings) throws Exception {

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }

        if (settings.getWhere() == Constants.HEADER) {
            expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, Constants.HELLOWORLD_WITH_HEADER);
        } else {
            if (settings.getWhere() == Constants.PARM) {
                expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, Constants.HELLOWORLD_WITH_PARM);
            } else {
                expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, Constants.HELLOWORLD_NO_HEADER_NO_PARM);
            }
        }
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found UnAuthenticated in the App output", null, Constants.HELLOWORLD_UNAUTHENTICATED);

        return expectations;

    }

    /**
     * Expects:
     * - Got to protected resource (helloworld)
     * - Found expected realm name ({@value Constants#BASIC_REALM})
     * - Found expected access ID
     * 
     * @return
     * @throws Exception
     */
    public List<validationData> getValidHelloWorldExpectations(TestSettings settings) throws Exception {
        return getValidHelloWorldExpectations(settings, Constants.BASIC_REALM);
    }

    /**
     * Expects:
     * - Got to protected resource (helloworld)
     * - Found expected realm name
     * - Found expected access ID
     * 
     * @return
     * @throws Exception
     */
    public List<validationData> getValidHelloWorldExpectations(TestSettings settings, String realm) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = setGoodHelloWorldExpectations(expectations, settings);
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, "Did not find expected realm name.", "RealmName: " + realm);
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, "Did not find expected access ID.", "Access ID: user:" + realm + "/" + Constants.OIDC_USERNAME);
        return expectations;
    }

    /**
     * Expects:
     * - Got to protected resource (helloworld)
     * - Found expected realm name
     * - Found expected access ID
     * 
     * @return
     * @throws Exception
     */
    public List<validationData> getValidHelloWorldExpectations(TestSettings settings, String realm, String accessId) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = setGoodHelloWorldExpectations(expectations, settings);
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, "Did not find expected realm name.", "RealmName: " + realm);
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, "Did not find expected access ID.", "Access ID: " + accessId);
        return expectations;
    }

}
