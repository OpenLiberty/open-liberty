/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.websphere.simplicity.log.Log;

public class ValidationData {

    static private final Class<?> thisClass = ValidationData.class;
    static private String[] ALL_TEST_ACTIONS;

    public ValidationData() {

    }

    public ValidationData(String[] allTestActions) {

        ALL_TEST_ACTIONS = allTestActions;
    }

    public class validationData {

        String action; // specify the test_action that should check this.
        String where; // response, console(.log), Messages(.log), trace(.log)
        String checkType; // match (regexp), contains
        String printMsg; // message to print in the event the string searched
                         // for is NOT found
        String validationKey; // if key/value pair, the key
        String validationValue; // value to search for (string in message, value
                                // of key, ...)

        public validationData(String step, String loc, String chkType,
                              String msg, String key, String value) {
            action = step;
            where = loc;
            checkType = chkType;
            printMsg = msg;
            validationKey = key;
            validationValue = value;
        }

        public String getAction() {

            return action;
        };

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

    }

    /*
     * public ArrayList<validationMsg> addExpectation(ArrayList<validationMsg>
     * expected, String printString, String searchString) throws Exception {
     * 
     * return addExpectation(expected, STRING_CONTAINS, null, RESPONSE,
     * printString, searchString) ;
     * 
     * addResponseExpectation addJSONExpectation addLogExpectation
     * 
     * }
     */
    public List<validationData> addExpectation(List<validationData> expected,
                                               String action, String where, String checkType, String printString,
                                               String key, String value) throws Exception {

        try {
            if (expected == null) {
                expected = new ArrayList<validationData>();
            }
            if (checkType == null) {
                checkType = Constants.STRING_CONTAINS;
            }
            expected.add(new validationData(action, where, checkType, printString, key, value));
            return expected;
        } catch (Exception e) {
            Log.info(thisClass, "addExpectation",
                     "Error occured while trying to set an expectation during test setup");
            throw e;
        }
    }

    public List<validationData> addResponseExpectation(
                                                       List<validationData> expected, String action, String printString,
                                                       String value) throws Exception {

        try {
            return addExpectation(expected, action, Constants.RESPONSE_FULL,
                                  Constants.STRING_CONTAINS, printString, null, value);

        } catch (Exception e) {
            Log.info(thisClass, "addExpectation",
                     "Error occured while trying to set an expectation during test setup");
            throw e;
        }
    }

    public List<validationData> addResponseStatusExpectation(List<validationData> expected, String action, int value) throws Exception {

        try {
            String status = Integer.toString(value);
            // pass in a null message as we should be able to generate that
            // generically at the time we do the check
            return addExpectation(expected, action, Constants.RESPONSE_STATUS,
                                  Constants.STRING_CONTAINS, "Did not receive status code "
                                                             + status + ".",
                                  null, status);

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

        String thisMethod = "addSuccessStatusCodes";
        try {
            String status = Integer.toString(Constants.OK_STATUS);
            // pass in a null message as we should be able to generate that
            // generically at the time we do the check
            ArrayList<String> temp = new ArrayList<String>();
            temp.addAll(Arrays.asList(ALL_TEST_ACTIONS));
            String[] allTestActions = temp.toArray(new String[ALL_TEST_ACTIONS.length]);
            for (String action : allTestActions) {
                if (exceptAction != null & action.equals(exceptAction)) {
                    Log.info(thisClass, thisMethod,
                             "Skip adding expected status code for action: "
                                                    + exceptAction);
                } else {
                    expected = addExpectation(expected, action,
                                              Constants.RESPONSE_STATUS,
                                              Constants.STRING_CONTAINS,
                                              "Did not receive status code " + status + ".",
                                              null, status);
                }
            }
            return expected;

        } catch (Exception e) {
            Log.info(thisClass, "addExpectation",
                     "Error occured while trying to set an expectation during test setup");
            throw e;
        }
    }

}
