/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

/**
 *
 */
public class CDIConfigPropertyTest extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("CDIConfigServer");

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testNullKey() throws Exception {
        test("nullKey", "nullKeyValue");
    }

    @Test
    public void testEmptyKey() throws Exception {
        test("emptyKey", "emptyKeyValue");
    }

    @Test
    public void testDefaultKey() throws Exception {
        test("defaultKey", "defaultKeyValue");
    }

    @Test
    public void testDefaultValueNotUsed() throws Exception {
        test("URL_KEY", "http://www.ibm.com");
    }

    @Test
    public void testDefaultValue() throws Exception {
        test("DEFAULT_URL_KEY", "http://www.default.com");
    }

    private void test(String key, String expected) throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/cdiConfig/configProperty?key=" + key, key + "=" + expected);
    }
}
