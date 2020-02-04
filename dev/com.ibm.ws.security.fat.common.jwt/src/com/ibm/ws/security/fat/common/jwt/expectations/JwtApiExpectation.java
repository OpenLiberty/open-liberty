/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt.expectations;

import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.JwtMessageConstants;

public class JwtApiExpectation extends ResponseFullExpectation {

    public enum ValidationMsgType {
        SPECIFIC_CLAIM_API, CLAIM_LIST_MEMBER, CLAIM_FROM_LIST, HEADER_CLAIM_FROM_LIST
    }

    /**
     * @param testAction
     * @param searchLocation
     * @param checkType
     * @param searchFor
     * @param failureMsg
     */
    public JwtApiExpectation(String errorId, String configId) {
        super(null, JwtConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING + ".+" + configId + ".+"
                                                 + errorId, "Response did not show the expected " + errorId + " failure.");
    }

    public JwtApiExpectation(String checkType, String searchFor, String failureMsg) {
        super(checkType, searchFor, failureMsg);
    }

    public JwtApiExpectation(String testAction, String searchLocation, String checkType, String searchKey, String searchFor, String failureMsg) {
        super(testAction, searchLocation, checkType, searchKey, searchFor, failureMsg);
    }

    public JwtApiExpectation(String key, Object value, ValidationMsgType claimType) {
        this("", key, value, claimType);

    }

    public JwtApiExpectation(String prefix, String key, Object value, ValidationMsgType claimType) {
        super(null, JwtConstants.STRING_MATCHES, null, "Response from test step  did not match expected value.");
        validationKey = key;

        // based on what we're trying to check, override the validation value and the check type
        switch (claimType) {
            case SPECIFIC_CLAIM_API:
                checkType = JwtConstants.STRING_MATCHES;
                validationValue = buildClaimApiString(prefix, key, value);
                break;

            case CLAIM_LIST_MEMBER:
                if (value == null || value.equals("-1")) {
                    checkType = JwtConstants.STRING_DOES_NOT_MATCH;
                } else {
                    checkType = JwtConstants.STRING_MATCHES;
                }
                validationValue = buildJsonClaimString(prefix, key, value);
                break;

            case CLAIM_FROM_LIST:
                if (value == null || value.equals("-1")) {
                    checkType = JwtConstants.STRING_DOES_NOT_MATCH;
                } else {
                    checkType = JwtConstants.STRING_MATCHES;
                }
                validationValue = buildJsonAllClaimString(prefix, JwtConstants.JWT_JSON + JwtConstants.JWT_GETALLCLAIMS, key, value);
                break;
            case HEADER_CLAIM_FROM_LIST:
                if (value == null || value.equals("-1")) {
                    checkType = JwtConstants.STRING_DOES_NOT_MATCH;
                } else {
                    checkType = JwtConstants.STRING_MATCHES;
                }
                validationValue = buildJsonAllClaimString(prefix, JwtConstants.JWT_TOKEN_HEADER_JSON, key, value);
                break;

            default:
                break;
        }
    }

    public String buildClaimApiString(String prefix, String keyLogName, Object value) {

        String builtString = prefix + keyLogName + ".*" + value;

        return builtString;
    }

    public String buildJsonClaimString(String prefix, String key, Object value) {

        String builtString = "garbage";
        String newValue = "garbage";
        if (value == null) {
            newValue = ":" + ".*null";
        } else {
            if (value.equals("-1")) {
                newValue = ":";
            } else {
                newValue = value.toString().replace("[", "").replace("]", "");
            }
        }
        builtString = prefix + JwtConstants.JWT_JSON + "\\{" + ".*" + key + ".*" + newValue + ".*\\}";
        return builtString;
    }

    public String buildJsonAllClaimString(String prefix, String subPrefix, String key, Object value) {

        String builtString = "garbage";
        if (value instanceof Long) {
            if (value.equals(-1L)) {
                builtString = prefix + subPrefix + ".*" + key + ".*";
            }
        }
        builtString = prefix + subPrefix + ".*" + key + ".*" + value;

        return builtString;
    }

}
