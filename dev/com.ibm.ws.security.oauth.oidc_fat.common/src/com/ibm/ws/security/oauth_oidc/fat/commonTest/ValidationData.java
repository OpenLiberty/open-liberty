/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.websphere.simplicity.log.Log;

public class ValidationData {

    static private final Class<?> thisClass = ValidationData.class;

    public ValidationData() {

    }

    public class validationData {

        String action; // specify the test_action that should check this.
        TestServer serverRef; // reference to server
        String where; // response, console(.log), Messages(.log), trace(.log)
        String checkType; // match (regexp), contains
        String printMsg; // message to print in the event the string searched
                         // for is NOT found
        String validationKey; // if key/value pair, the key
        String validationValue; // value to search for (string in message, value
                                // of key, ...)
        boolean isExceptionHandled;

        public validationData(String step, TestServer theServer, String loc, String chkType,
                String msg, String key, String value) {
            action = step;
            serverRef = theServer;
            where = loc;
            checkType = chkType;
            printMsg = msg;
            validationKey = key;
            validationValue = value;
            if (where != null && where.equals(Constants.EXCEPTION_MESSAGE)) {
                isExceptionHandled = false;
            }
        }

        public String getAction() {

            return action;
        };

        public TestServer getServerRef() {
            return serverRef;
        }

        public String getWhere() {

            return where;
        };

        public String getCheckType() {

            return checkType;
        };

        public String getPrintMsg() {

            return printMsg;
        };

        public String getValidationKey() {

            return validationKey;
        };

        public String getValidationValue() {

            return validationValue;
        };

        public boolean isExceptionHandled() {
            return isExceptionHandled;
        }

        public void setIsExceptionHandled(boolean isExceptionHandled) {
            this.isExceptionHandled = isExceptionHandled;
        }

        public void printValidationData() throws Exception {

            Log.info(thisClass, "printValidationData", "action: " + action);
            Log.info(thisClass, "printValidationData", "serverRef: " + serverRef);
            Log.info(thisClass, "printValidationData", "where: " + where);
            Log.info(thisClass, "printValidationData", " checkType: " + checkType);
            Log.info(thisClass, "printValidationData", "printMsg: " + printMsg);
            Log.info(thisClass, "printValidationData", "validationKey: " + validationKey);
            Log.info(thisClass, "printValidationData", "validationValue: " + validationValue);
            Log.info(thisClass, "printValidationData", "isExcetionHandled: " + Boolean.toString(isExceptionHandled));

        }
    }

    public List<validationData> addExpectation(List<validationData> expected,
            String action, String where, String checkType, String printString,
            String key, String value) throws Exception {
        return addExpectation(expected, action, null, where, checkType, printString, key, value);
    }

    public List<validationData> addExpectation(List<validationData> expected,
            String action, TestServer serverRef, String where, String checkType, String printString,
            String key, String value) throws Exception {

        try {
            if (expected == null) {
                expected = new ArrayList<validationData>();
            }
            if (checkType == null) {
                checkType = Constants.STRING_CONTAINS;
            }
            expected.add(new validationData(action, serverRef, where, checkType, printString, key, value));
            return expected;
        } catch (Exception e) {
            Log.info(thisClass, "addExpectation",
                    "Error occured while trying to set an expectation during test setup");
            throw e;
        }
    }

    public List<validationData> addExpectation(List<validationData> addTo, List<validationData> pullFrom) throws Exception {
        try {
            if (addTo == null) {
                addTo = new ArrayList<validationData>();
            }
            if (pullFrom == null) {
                return addTo;
            }
            addTo.addAll(pullFrom);
        } catch (Exception e) {
            Log.info(thisClass, "addExpectation", "Error occured while adding an expectation: " + e.toString());
            throw e;
        }
        return addTo;
    }

    public validationData setValidationData(String action, TestServer serverRef, String where, String checkType, String printString,
            String key, String value) throws Exception {

        return new validationData(action, serverRef, where, checkType, printString, key, value);

    }

    public List<validationData> addResponseExpectation(
            List<validationData> expected, String action, String printString,
            String value) throws Exception {

        try {
            return addExpectation(expected, action, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, printString, null, value);

        } catch (Exception e) {
            Log.info(thisClass, "addExpectation",
                    "Error occured while trying to set an expectation during test setup");
            throw e;
        }
    }

    public List<validationData> addResponseStatusExpectation(
            List<validationData> expected, String action, int value)
            throws Exception {

        try {
            String status = Integer.toString(value);
            // pass in a null message as we should be able to generate that
            // generically at the time we do the check
            return addExpectation(expected, action, Constants.RESPONSE_STATUS, Constants.STRING_CONTAINS, "Did not receive status code " + status + ".", null, status);

        } catch (Exception e) {
            Log.info(thisClass, "addExpectation",
                    "Error occured while trying to set an expectation during test setup");
            throw e;
        }
    }

    public List<validationData> addSuccessStatusCodes() throws Exception {
        return addSuccessStatusCodes(null, null);
    }

    public List<validationData> addSuccessStatusCodes(List<validationData> expected) throws Exception {

        return addSuccessStatusCodes(expected, null);
    }

    public List<validationData> addSuccessStatusCodes(List<validationData> expected, String exceptAction) throws Exception {
        ArrayList<String> temp = new ArrayList<String>();
        temp.addAll(Arrays.asList(Constants.OP_TEST_ACTIONS));
        temp.addAll(Arrays.asList(Constants.RP_TEST_ACTIONS));
        temp.addAll(Arrays.asList(Constants.BUILDER_TEST_ACTIONS));
        String[] allTestActions = temp.toArray(new String[Constants.OP_TEST_ACTIONS.length + Constants.RP_TEST_ACTIONS.length + Constants.BUILDER_TEST_ACTIONS.length]);

        return addSuccessStatusCodesForActions(expected, exceptAction, allTestActions);
    }

    public List<validationData> addSuccessStatusCodesForActions(String[] testActions) throws Exception {
        return addSuccessStatusCodesForActions((List<validationData>) null, testActions);
    }

    public List<validationData> addSuccessStatusCodesForActions(String exceptAction, String[] testActions) throws Exception {
        return addSuccessStatusCodesForActions(null, exceptAction, testActions);
    }

    public List<validationData> addSuccessStatusCodesForActions(List<validationData> expected, String[] testActions) throws Exception {
        return addSuccessStatusCodesForActions(expected, null, testActions);
    }

    public List<validationData> addSuccessStatusCodesForActions(List<validationData> expected, String exceptAction, String[] testActions) throws Exception {
        String thisMethod = "addSuccessStatusCodes";
        try {
            String status = Integer.toString(Constants.OK_STATUS);
            for (String action : testActions) {
                if (exceptAction != null & action.equals(exceptAction)) {
                    Log.info(thisClass, thisMethod, "Skip adding expected status code for action: " + exceptAction);
                } else {
                    expected = addExpectation(expected, action, Constants.RESPONSE_STATUS, Constants.STRING_CONTAINS, "Did not receive status code " + status + ".", null, status);
                }
            }
            return expected;

        } catch (Exception e) {
            Log.info(thisClass, "addExpectation", "Error occured while trying to set an expectation during test setup: " + e.getLocalizedMessage());
            throw e;
        }
    }

    public List<validationData> addJSONExpectation(
            List<validationData> expected, String action, String printString,
            String key, String value) throws Exception {

        try {
            return addExpectation(expected, action, Constants.JSON_OBJECT, null, printString, key, value);

        } catch (Exception e) {
            Log.info(thisClass, "addExpectation",
                    "Error occured while trying to set an expectation during test setup");
            throw e;
        }
    }

    public List<validationData> addNoTokensInResponseExpectations(List<validationData> expectations, String action) throws Exception {
        expectations = addExpectation(expectations, action, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an access_token in the response and should not have", null, Constants.ACCESS_TOKEN_KEY);
        expectations = addExpectation(expectations, action, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found an id_token in the response and should not have", null, Constants.ID_TOKEN_KEY);
        return expectations;
    }

    public List<validationData> addNoTokenInResponseExpectation(List<validationData> expectations, String action, String token) throws Exception {
        expectations = addExpectation(expectations, action, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found " + token + " in the response and should not have", null, token);
        return expectations;
    }

    public List<validationData> addTokenInResponseExpectation(List<validationData> expectations, String action, String token) throws Exception {
        expectations = addExpectation(expectations, action, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not found " + token + " in the response", null, token);
        return expectations;
    }

}