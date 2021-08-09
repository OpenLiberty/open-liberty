/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.metadata;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.FormLoginConfigurationImpl;

import test.common.SharedOutputManager;

/**
 *
 */
public class FormLoginConfigurationTest {

    private static SharedOutputManager outputMgr;

    private final static String LOGIN_PAGE = "/loginPage.jsp";
    private final static String ERROR_PAGE = "/errorPage.jsp";
    private static FormLoginConfiguration formLoginConfiguration;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor: 
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
        formLoginConfiguration = new FormLoginConfigurationImpl(LOGIN_PAGE, ERROR_PAGE);
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testGetFormLoginPage() {
        final String methodName = "testGetFormLoginPage";
        try {
            String actualLoginPage = formLoginConfiguration.getLoginPage();
            assertEquals("The form login page must be the same as the one used in the constructor.",
                         LOGIN_PAGE, actualLoginPage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetFormErrorPage() {
        final String methodName = "testGetFormErrorPage";
        try {
            String actualErrorPage = formLoginConfiguration.getErrorPage();
            assertEquals("The form error page must be the same as the one used in the constructor.",
                         ERROR_PAGE, actualErrorPage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
