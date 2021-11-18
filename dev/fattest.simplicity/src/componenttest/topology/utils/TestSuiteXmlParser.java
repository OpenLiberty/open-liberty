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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestSuiteXmlParser extends DefaultHandler {

    private static final String TEST_SUITE = "testsuite";
    private static final String TEST_CASE = "testcase";
    private static final String ERROR = "error";
    private static final String FAILURE = "failure";
    private static final String PROPERTY = "property";

    private TestSuiteResult currentSuite = null;
    private TestCaseResult currentCase = null;

    private List<TestSuiteResult> results = new LinkedList<TestSuiteResult>();

    public List<TestSuiteResult> getResults() {
        return results;
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void startElement(String uri, String lName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
            case TEST_SUITE:
                int errors = Integer.parseInt(attributes.getValue("errors"));
                int failures = Integer.parseInt(attributes.getValue("failures"));
                String name = attributes.getValue("name");
                String timestamp = attributes.getValue("timestamp");
                String tests = attributes.getValue("tests");
                TestSuiteResult testSuiteResult = new TestSuiteResult(name, failures, errors, timestamp, tests);
                results.add(testSuiteResult);
                currentSuite = testSuiteResult;
                break;
            case TEST_CASE:
                if (currentSuite == null) {
                    throw new IllegalStateException("currentSuite is null");
                }
                String testPackageName = attributes.getValue("classname");
                String testName = attributes.getValue("name");
                TestCaseResult testCaseResult = new TestCaseResult(testName, testPackageName);
                currentSuite.addResult(testCaseResult);
                currentCase = testCaseResult;
                break;
            case ERROR:
                if (currentCase == null) {
                    throw new IllegalStateException("currentCase is null");
                }
                currentCase.setError();
                break;
            case FAILURE:
                if (currentCase == null) {
                    throw new IllegalStateException("currentCase is null");
                }
                currentCase.setFailure();
                break;
        }
    }

    @Override
    public void endElement(String uri, String lName, String qName) throws SAXException {
        switch (qName) {
            case TEST_SUITE:
                currentSuite = null;
                break;
            case TEST_CASE:
                currentCase = null;
                break;
            case ERROR:
                //Empty
                break;
            case FAILURE:
                //Empty
                break;
        }
    }
}
