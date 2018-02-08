/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

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

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.annotation.ExpectedFFDC;

/**
 * All CDI tests with all applicable server features enabled.
 */
public class StatefulSessionBeanInjectionTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12StatefulSessionBeanServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        JavaArchive statefulSessionBeanInjection = ShrinkWrap.create(JavaArchive.class,"statefulSessionBeanInjection.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.InjectedEJBImpl")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.InjectedEJB")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.InjectedBean1")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.InjectedBean2")
                        .add(new FileAsset(new File("test-applications/statefulSessionBeanInjection.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        return ShrinkWrap.create(WebArchive.class, "statefulSessionBeanInjection.war")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.servlet.RemoveServlet")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.servlet.TestServlet")
                        .add(new FileAsset(new File("test-applications/statefulSessionBeanInjection.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(statefulSessionBeanInjection);

    }

    @Test
    @ExpectedFFDC("javax.ejb.NoSuchEJBException")
    public void testStatefulEJBRemoveMethod() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        this.verifyResponse(browser,
                            "/statefulSessionBeanInjection/",
                            "Test Sucessful! - STATE1");

        this.verifyResponse(browser,
                            "/statefulSessionBeanInjection/",
                            "Test Sucessful! - STATE2");

        this.verifyResponse(browser,
                            "/statefulSessionBeanInjection/remove",
                            "EJB Removed!");

        this.verifyResponse(browser,
                            "/statefulSessionBeanInjection/",
                            "NoSuchEJBException");
        // TODO Note that we stop the server in the test so that the expected FFDC on shutdown
        // happens in the testcase.  It is questionable that this FFDC is produced here.
        // It makes for the appearance of some leak with removed EJBs in the weld session
        getSharedServer().getLibertyServer().stopServer();
    }
}
