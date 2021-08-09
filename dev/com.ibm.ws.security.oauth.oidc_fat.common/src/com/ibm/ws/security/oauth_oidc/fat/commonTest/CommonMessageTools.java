/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
        printOAuthOidcExpectations(expectations, null);
    }

    /**
     * Use {@link CommonMessageTools#printExpectations(Expectations, String[])}.
     */
    @Deprecated
    public void printOAuthOidcExpectations(List<validationData> expectations, String[] actions) throws Exception {
        printExpectations(ValidationDataToExpectationConverter.convertValidationDataList(expectations), actions);
    }

}