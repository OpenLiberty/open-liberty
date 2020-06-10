/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;

@Mode(FULL)
public class CDICurrentTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12CDICurrentServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {

        JavaArchive cdiCurrentTest = ShrinkWrap.create(JavaArchive.class,
                                                       "cdiCurrentTest.jar").addClass("com.ibm.ws.cdi12.test.current.extension.CDICurrentTestBean").addClass("com.ibm.ws.cdi12.test.current.extension.MyDeploymentVerifier").addClass("com.ibm.ws.cdi12.test.current.extension.DefaultLiteral").addClass("com.ibm.ws.cdi12.test.current.extension.CDICurrent").add(new FileAsset(new File("test-applications/cdiCurrentTest.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                                                                                                                                                                                                                                                                                                                                                   "/META-INF/services/javax.enterprise.inject.spi.Extension");

        WebArchive archive = ShrinkWrap.create(WebArchive.class,
                                               "cdiCurrentTest.war").addClass("com.ibm.ws.cdi12.test.common.web.TestServlet").addClass("com.ibm.ws.cdi12.test.common.web.SimpleBean").addAsLibrary(cdiCurrentTest);

        return archive;
    }

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testCDICurrent() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        SHARED_SERVER.verifyResponse(browser, "/cdiCurrentTest/", new String[] { "SUCCESS", "bean exists" });

        SHARED_SERVER.getApplicationMBean("cdiCurrentTest").restart();

        SHARED_SERVER.verifyResponse(browser, "/cdiCurrentTest/", new String[] { "SUCCESS", "bean exists" });
    }

}
