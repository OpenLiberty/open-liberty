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
 * Scope tests for Dynamically Added Beans
 */
@Mode(FULL)
public class DynamicBeanExtensionTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12DynamicallyAddedBeansServer");

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    /**
     * Test that bean classes which are loaded by the Root ClassLoader can be injected correctly
     *
     * @throws Exception
     */
    @Test
    public void testDynamicallyAddedBeans() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        SHARED_SERVER.verifyResponse(browser, "/dynamicallyAddedBeans/", new String[] { "DynamicBean1 count: 1, 2", "DynamicBean2 count: 1, 2" });
        SHARED_SERVER.verifyResponse(browser, "/dynamicallyAddedBeans/", new String[] { "DynamicBean1 count: 3, 4", "DynamicBean2 count: 1, 2" });
        browser = createWebBrowserForTestCase();
        SHARED_SERVER.verifyResponse(browser, "/dynamicallyAddedBeans/", new String[] { "DynamicBean1 count: 1, 2", "DynamicBean2 count: 1, 2" });
    }

}
