/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.expectations;

import java.util.regex.Pattern;

import javax.json.JsonValue.ValueType;

import com.ibm.ws.security.fat.common.Constants.CheckType;
import com.ibm.ws.security.openidconnect.common.Constants;

public class UserInfoJsonExpectation extends com.ibm.ws.security.fat.common.social.expectations.UserInfoJsonExpectation {

    public static final String USER_INFO_SERVLET_OUTPUT_REGEX = Pattern.quote(Constants.USERINFO_STR + "=") + "(\\{.*\\})";

    public UserInfoJsonExpectation(String expectedKey) {
        super(expectedKey);
    }

    public UserInfoJsonExpectation(String expectedKey, CheckType checkType, Object expectedValue) {
        super(expectedKey, checkType, expectedValue);
    }

    public UserInfoJsonExpectation(String expectedKey, ValueType expectedValueType) {
        super(expectedKey, expectedValueType);
    }

    public UserInfoJsonExpectation(String expectedKey, ValueType expectedValueType, Object expectedValue) {
        super(expectedKey, expectedValueType, expectedValue);
    }

    @Override
    protected String getRegexForExtractingUserInfoString() {
        return USER_INFO_SERVLET_OUTPUT_REGEX;
    }

}
