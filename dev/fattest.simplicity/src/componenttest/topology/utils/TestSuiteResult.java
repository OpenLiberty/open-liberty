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
package componenttest.topology.utils;

import java.util.List;
import java.util.LinkedList;
import java.util.Scanner;

public class TestSuiteResult {
    private int failures;
    private int errors;

    private String packageName;
    private String name;
    private String timestamp;
    private String tests;

    private List<TestCaseResult> results = new LinkedList<TestCaseResult>();

    public TestSuiteResult(String name, int failures, int errors, String timestamp, String tests) {
        this.failures = failures;
        this.errors = errors;
        this.name = name;
        this.timestamp = timestamp;
        this.tests = tests;
    }

    public void addResult(TestCaseResult result) {
        results.add(result);
    }

    public String toString() {
        StringBuilder strBuild = new StringBuilder();

        String pass;
        if (failures == 0 && errors == 0) {
            pass = "PASSED";
        } else {
            pass = "FAILED";
        }
        strBuild.append("Test suite: " + name +  " "  +  timestamp);
        strBuild.append(System.lineSeparator());
        strBuild.append("Tests:" + tests + " Failures:" + failures + " Errors:" + errors);
        strBuild.append(System.lineSeparator());

        for (TestCaseResult testCase : results) { 
            strBuild.append("   " + testCase.toString());
            strBuild.append(System.lineSeparator());
        }
        return strBuild.toString();
    }
}
