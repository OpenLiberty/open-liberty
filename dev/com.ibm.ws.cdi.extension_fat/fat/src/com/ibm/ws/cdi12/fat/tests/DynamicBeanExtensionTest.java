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

import org.junit.ClassRule;
import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;

/**
 * Scope tests for Dynamically Added Beans
 */
@Mode(FULL)
public class DynamicBeanExtensionTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12DynamicallyAddedBeansServer");

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {

        JavaArchive dynamicallyAddedBeansJar = ShrinkWrap.create(JavaArchive.class,"dynamicallyAddedBeans.jar")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.DynamicBean1Bean")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.MyCDIExtension")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.DynamicBean2Bean")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.DynamicBean1")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.DefaultLiteral")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.DynamicBean2")
                        .add(new FileAsset(new File("test-applications/dynamicallyAddedBeans.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");

        WebArchive dynamicallyAddedBeans = ShrinkWrap.create(WebArchive.class, "dynamicallyAddedBeans.war")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.web.DynamicBeansServlet");

        return ShrinkWrap.create(EnterpriseArchive.class,"dynamicallyAddedBeans.ear")
                        .add(new FileAsset(new File("test-applications/dynamicallyAddedBeans.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(dynamicallyAddedBeans)
                        .addAsLibrary(dynamicallyAddedBeansJar);
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
