/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.util.List;

import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.structures.ValidationDataToExpectationConverter;

/**
 * Common messaging/logging tools for openidconnect
 */
public class CommonMessageTools extends com.ibm.ws.security.fat.common.CommonMessageTools {

    /**
     * Use {@link CommonMessageTools#printExpectations(Expectations)}.
     */
    @Deprecated
    public void printOAuthOidcExpectations(List<validationData> expectations) throws Exception {
        printOAuthOidcExpectations(expectations, null, null);
    }

    @Deprecated
    public void printOAuthOidcExpectations(List<validationData> expectations, TestSettings settings) throws Exception {
        printOAuthOidcExpectations(expectations, null, settings);
    }

    /**
     * Use {@link CommonMessageTools#printExpectations(Expectations, String[])}.
     */
    @Deprecated
    public void printOAuthOidcExpectations(List<validationData> expectations, String[] actions, TestSettings settings) throws Exception {
        if (settings != null && settings.getAllowPrint()) {
            printExpectations(ValidationDataToExpectationConverter.convertValidationDataList(expectations), actions);
        }
    }

    public void printTestSettings(TestSettings settings) throws Exception {
        if (settings != null) {
            settings.printTestSettings();
        }
    }
}
