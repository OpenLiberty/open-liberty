/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;

/**
 * Scope tests for EJBs
 */
@Mode(FULL)
public class EjbMiscTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12EJB32Server");

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    /**
     * Test that the request scope is active during postConstruct for an eager singleton bean.
     *
     * @throws Exception
     */
    @Test
    public void testPostConstructRequestScope() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        SHARED_SERVER.verifyResponse(browser, "/ejbScope/PostConstructScope", "true");
    }

    @Test
    public void testDupEJBClassNames() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        SHARED_SERVER.verifyResponse(browser, "/multipleWar1", "MyEjb myWar1Bean");
        SHARED_SERVER.verifyResponse(browser, "/multipleWar2", "MyEjb myWar2Bean");
    }

}
