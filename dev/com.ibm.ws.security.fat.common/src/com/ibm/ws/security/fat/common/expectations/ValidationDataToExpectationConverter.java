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

package com.ibm.ws.security.fat.common.expectations;

import java.util.List;

import com.ibm.ws.security.fat.common.ValidationData.validationData;

public class ValidationDataToExpectationConverter {

    /**
     * Converts the provided List of validationData to an Expectations class containing individual Expectation classes that
     * represent each expectation entry in the list.
     * 
     * @deprecated The expectations returned are for debug only.
     */
    @Deprecated
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
     * Converts the provided validationData instance into an Expectation class instance.
     */
    private static Expectation convertValidationData(validationData expectation) {
        if (expectation == null) {
            return null;
        }
        return new DummyExpectation(expectation.getAction(), expectation.getWhere(), expectation.getCheckType(), expectation.getValidationKey(), expectation.getValidationValue(), expectation.getPrintMsg());
    }

    static class DummyExpectation extends Expectation {
        
        DummyExpectation(String testAction, String searchLocation, String checkType, String searchKey, String searchFor, String failureMsg) {
            super(testAction, searchLocation, checkType, searchKey, searchFor, failureMsg);
        }

        @Override
        protected void validate(Object contentToValidate) throws Exception {
            throw new UnsupportedOperationException("Dummy expectations are for debug only and cannot be used to validate.");
        }
        
    }
}