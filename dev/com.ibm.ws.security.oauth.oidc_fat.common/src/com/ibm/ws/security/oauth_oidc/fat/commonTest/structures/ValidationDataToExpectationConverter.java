/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.structures;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

public class ValidationDataToExpectationConverter {

    /**
     * Converts the provided List of validationData to an Expectations class containing individual Expectation classes that
     * represent each expectation entry in the list.
     */
    public static Expectations convertValidationDataList(List<validationData> expectations) {
        Expectations convertedExps = new Expectations();
        if (expectations == null || expectations.isEmpty()) {
            return convertedExps;
        }
        for (validationData vData : expectations) {
            Expectation convertedExp = convertValidationData(vData);
            if (convertedExp != null) {
                convertedExps.addExpectation(convertedExp);
            }
        }
        return convertedExps;
    }

    /**
     * 
     * Converts the provided validationData instance into an Expectation class instance.
     */
    public static Expectation convertValidationData(validationData expectation) {
        if (expectation == null) {
            return null;
        }
        return new OAuthOidcExpectation(expectation.getAction(), expectation.getServerRef(), expectation.getWhere(), expectation.getCheckType(), expectation.getValidationKey(), expectation.getValidationValue(), expectation.getPrintMsg());
    }

    /**
     * Converts the provided Expectations class to a List of validationData. To be used to help deprecate the validationData
     * classes.
     */
    public static List<validationData> convertExpectations(Expectations expectations) throws Exception {
        List<validationData> convertedExps = new ArrayList<validationData>();
        if (expectations == null || expectations.getExpectations().isEmpty()) {
            return convertedExps;
        }
        for (Expectation expectation : expectations.getExpectations()) {
            validationData convertedExp = convertExpectation(expectation);
            if (convertedExp != null) {
                convertedExps.add(convertedExp);
            }
        }
        return convertedExps;
    }

    /**
     * Converts the provided Expectation instance into a validationData class instance.
     */
    public static validationData convertExpectation(Expectation expectation) throws Exception {
        if (expectation == null) {
            return null;
        }
        ValidationData vData = new ValidationData();
        // TODO - get appropriate server object
        return vData.setValidationData(expectation.getAction(), null, expectation.getSearchLocation(), expectation.getCheckType(), expectation.getFailureMsg(), expectation.getValidationKey(), expectation.getValidationValue());
    }

}