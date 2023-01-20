/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.fat.utils;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseTitleExpectation;

public class CommonExpectations extends com.ibm.ws.security.fat.common.utils.CommonExpectations {

    protected static Class<?> thisClass = CommonExpectations.class;

    /**
     * Sets expectations that will check:
     * <ol>
     * <li>200 status code in the response for the specified test action
     * <li>Response title is equivalent to expected login page title
     * </ol>
     */
    public static Expectations successfullyReachedOidcLoginPage(String testAction) {
        Expectations expectations = new Expectations();
        expectations.addSuccessStatusCodesForActions(new String[] { testAction });
        expectations.addExpectation(new ResponseTitleExpectation(testAction, Constants.STRING_EQUALS, Constants.LOGIN_TITLE, "Title of page returned during test step " + testAction
                                                                                                                             + " did not match expected value."));
        return expectations;
    }

    public static Expectations successfullyReachedOidcLoginPage() {
        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseTitleExpectation(Constants.STRING_EQUALS, Constants.LOGIN_TITLE, "Title of page returned did not match expected value."));
        return expectations;
    }

    public static Expectations successfullyReachedOidcLogoutPage() {
        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseTitleExpectation(Constants.STRING_EQUALS, Constants.LOGOUT_TITLE, "Title of page returned did not match expected value."));
        expectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_EQUALS, Constants.OK_MESSAGE, "Did not receive the ok message."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, Constants.SUCCESSFUL_LOGOUT_MSG, "Did not receive a message stating that the logout was successful."));
        return expectations;
    }

    public static Expectations successfullyReachedPostLogoutPage(boolean extraParms) {
        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
//        expectations.addExpectation(new ResponseTitleExpectation(Constants.STRING_EQUALS, Constants.LOGOUT_TITLE, "Title of page returned did not match expected value."));
        expectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_EQUALS, Constants.OK_MESSAGE, "Did not receive the ok message."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, ServletMessageConstants.REACHEDPOSTLOGOUT, "Did not receive a message stating that we landed on the Post Logout page."));
        if (extraParms) {
            expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, ServletMessageConstants.REACHEDPOSTLOGOUT, "Did not receive a message stating that we landed on the Post Logout page."));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, ServletMessageConstants.POSTLOGOUT_NOEXTRAPARMS, "Did not receive a message stating that there were no extra parms passed to the Post Logout Page."));
        }
        return expectations;
    }
}
