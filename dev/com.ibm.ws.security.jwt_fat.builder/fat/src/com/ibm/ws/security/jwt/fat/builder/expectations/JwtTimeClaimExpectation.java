/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder.expectations;

import javax.json.JsonObject;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.jwt.expectations.JwtApiExpectation;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;

public class JwtTimeClaimExpectation extends JwtApiExpectation {
    public static final TestValidationUtils validationUtils = new TestValidationUtils();

    /**
     * @param testAction
     * @param searchLocation
     * @param checkType
     * @param searchFor
     * @param failureMsg
     */
    public JwtTimeClaimExpectation(String errorId, String configId) {
        super(errorId, configId);
    }

    public JwtTimeClaimExpectation(String checkType, String searchFor, String failureMsg) {
        super(checkType, searchFor, failureMsg);
    }

    public JwtTimeClaimExpectation(String prefix, String key, Object value, ValidationMsgType claimType) {
        super(prefix, key, value, claimType);
    }

    public JwtTimeClaimExpectation(String testAction, String searchLocation, String checkType, String searchKey, String searchFor, String failureMsg) {
        super(testAction, searchLocation, checkType, searchKey, searchFor, failureMsg);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.fat.common.expectations.Expectation#validate(java.lang.Object)
     */
    @Override
    protected void validate(Object contentToValidate) throws Exception {

        try {
            Long testWindow = 120000L; // 2 minutes shoulds like a lot, but we do have some 4 minute timeouts, so, this may not be enough
            JsonObject payload = BuilderHelpers.extractJwtPayload(contentToValidate);
            if (payload == null) {
                throw new Exception(failureMsg + " Failed to find the payload in the response");
            }

            if (Constants.TIME_TYPE.equals(checkType)) {
                long expectedValue = Long.parseLong(validationValue);
                long actualValue = payload.getJsonNumber(validationKey).longValue();
                Log.info(thisClass, "validate", "Validting that calculated time is appropriate (" + expectedValue + " <= " + actualValue + " <= " + (expectedValue + testWindow) + " (expectedValue + testWindow))");
                validationUtils.assertTrueAndLog("validateStringNull", "The expected value (" + validationValue + ") for the " + validationKey + " claim is earlier/less than the actual value (" + actualValue + ")", (expectedValue <= actualValue));
                validationUtils.assertTrueAndLog("validateStringNull", "The actual value (" + actualValue + ") for the " + validationKey + " claim is outside the 2 minute window for the expected value (" + validationValue + ")", (actualValue <= (expectedValue + testWindow)));
                return;
            } else {
                String actualValue = payload.get(validationKey).toString();
                validationValue = validationValue.replace("replaceWithRealTime", actualValue);
                failureMsg = failureMsg.replace("replaceWithRealTime", actualValue);
            }
        } catch (Exception e) {
            throw new Exception(failureMsg + " Failed to validate response text: " + e.getMessage());
        }
        super.validate(contentToValidate);

    }

}
