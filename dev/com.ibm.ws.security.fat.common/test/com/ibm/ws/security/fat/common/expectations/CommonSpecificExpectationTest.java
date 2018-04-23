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

import org.junit.Test;

import test.common.SharedOutputManager;

public abstract class CommonSpecificExpectationTest extends CommonExpectationTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    protected final com.gargoylesoftware.htmlunit.WebResponse htmlunitWebResponse = mockery.mock(com.gargoylesoftware.htmlunit.WebResponse.class);
    protected final com.gargoylesoftware.htmlunit.html.HtmlPage htmlunitHtmlPage = mockery.mock(com.gargoylesoftware.htmlunit.html.HtmlPage.class);
    protected final com.gargoylesoftware.htmlunit.TextPage htmlunitTextPage = mockery.mock(com.gargoylesoftware.htmlunit.TextPage.class);
    protected final com.gargoylesoftware.htmlunit.xml.XmlPage htmlunitXmlPage = mockery.mock(com.gargoylesoftware.htmlunit.xml.XmlPage.class);

    @Test
    public void test_validate_nullCurrentTestAction() {
        try {
            Expectation exp = createBasicExpectation();

            String currentTestAction = null;
            Object content = null;
            exp.validate(currentTestAction, content);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_mismatchedCurrentTestAction() {
        try {
            Expectation exp = createBasicExpectation();

            String currentTestAction = "some other action";
            Object content = null;
            exp.validate(currentTestAction, content);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_currentTestActionDifferentCasing() {
        try {
            Expectation exp = createBasicExpectation();

            String currentTestAction = exp.getAction().toUpperCase();
            Object content = null;
            exp.validate(currentTestAction, content);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_currentTestActionSubstring() {
        try {
            Expectation exp = createBasicExpectation();

            String currentTestAction = exp.getAction().substring(1);
            Object content = null;
            exp.validate(currentTestAction, content);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_currentTestActionSuperstring() {
        try {
            Expectation exp = createBasicExpectation();

            String currentTestAction = exp.getAction() + " ";
            Object content = null;
            exp.validate(currentTestAction, content);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    /**
     * Sublcasses must override this method to return the appropriate object type for the class under test.
     */
    protected abstract Expectation createBasicExpectation();

}
