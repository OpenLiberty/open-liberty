/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.expectations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.simplicity.log.Log;

public class Expectations {
    private static final Class<?> thisClass = Expectations.class;

    private final List<Expectation> expectationsList = new ArrayList<Expectation>();
    private List<String> actionsList = new ArrayList<String>();

    public Expectations() {
        this(null);
    }

    public Expectations(String[] testActions) {
        if (testActions != null) {
            actionsList = Arrays.asList(testActions);
        }
    }

    public List<Expectation> getExpectations() {
        return expectationsList;
    }

    public List<String> getActionsList() {
        return actionsList;
    }

    public void addExpectation(Expectation expectation) {
        if (expectation != null) {
            expectationsList.add(expectation);
        }
    }

    public void addExpectations(Expectations expectations) {
        if (expectations == null) {
            return;
        }
        List<Expectation> newExpectations = expectations.getExpectations();
        for (Expectation newExp : newExpectations) {
            addExpectation(newExp);
        }
    }

    /**
     * Adds response status expectations to get 200 status codes for each of the actions performed by a test.
     */
    public void addSuccessStatusCodes() {
        addSuccessStatusCodes(null);
    }

    public void addSuccessStatusCodes(String exceptAction) {
        addSuccessStatusCodesForActions(exceptAction, actionsList);
    }

    public void addSuccessStatusCodesForActions(String[] testActions) {
        addSuccessStatusCodesForActions(null, testActions);
    }

    public void addSuccessStatusCodesForActions(String exceptAction, String[] testActions) {
        if (testActions != null) {
            addSuccessStatusCodesForActions(exceptAction, Arrays.asList(testActions));
        }
    }

    public void addSuccessStatusCodesForActions(String exceptAction, List<String> testActions) {
        String thisMethod = "addSuccessStatusCodesForActions";
        if (testActions == null) {
            return;
        }
        for (String action : testActions) {
            if (exceptAction != null && exceptAction.equals(action)) {
                Log.info(thisClass, thisMethod, "Skip adding expected status code for action: " + exceptAction);
                continue;
            }
            addExpectation(new ResponseStatusExpectation(action, HttpServletResponse.SC_OK));
        }
    }

    public void addSuccessCodeForCurrentAction() {
        addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
    }

}
