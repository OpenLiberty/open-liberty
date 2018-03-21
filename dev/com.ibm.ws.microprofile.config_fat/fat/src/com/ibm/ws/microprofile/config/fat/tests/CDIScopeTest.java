/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

import componenttest.custom.junit.runner.FATRunner;

/**
 *
 */
@RunWith(FATRunner.class)
public class CDIScopeTest extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new ShrinkWrapSharedServer("CDIScopeServer");

    @BuildShrinkWrap
    public static WebArchive buildApp() {
        return SharedShrinkWrapApps.cdiConfigServerApps();
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testConfigScope() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        //set a system property
        getSharedServer().verifyResponse(browser, "/cdiConfig/system?key=SYS_PROP&value=value1", "SYS_PROP=value1");
        //check it
        getSharedServer().verifyResponse(browser, "/cdiConfig/system?key=SYS_PROP", "SYS_PROP=value1");
        //change it
        getSharedServer().verifyResponse(browser, "/cdiConfig/system?key=SYS_PROP&value=value2", "SYS_PROP=value2");
        //check it again ... it shouldn't have changed because the injected property should be Session scoped
        getSharedServer().verifyResponse(browser, "/cdiConfig/system?key=SYS_PROP", "SYS_PROP=value1");
    }
}
