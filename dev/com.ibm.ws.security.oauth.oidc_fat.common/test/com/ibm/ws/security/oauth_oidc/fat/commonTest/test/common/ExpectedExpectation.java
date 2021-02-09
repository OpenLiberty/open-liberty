/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth_oidc.fat.commonTest.test.common;

/**
 * Container class to use for comparing expected Expectation values to actual Expectation values.
 */
public class ExpectedExpectation {

    public String expectedAction = null;
    public String expectedServerXml = null;
    public String expectedSearchLocation = null;
    public String expectedCheckType = null;
    public String expectedSearchKey = null;
    public String expectedSearchValue = null;
    public String expectedFailureMsg = null;

    public ExpectedExpectation(String expAction, String expServerXml, String expSearchLocation, String expCheckType, String expSearchKey, String expSearchValue, String expFailureMsg) {
        expectedAction = expAction;
        expectedServerXml = expServerXml;
        expectedSearchLocation = expSearchLocation;
        expectedCheckType = expCheckType;
        expectedSearchKey = expSearchKey;
        expectedSearchValue = expSearchValue;
        expectedFailureMsg = expFailureMsg;
    }

}
