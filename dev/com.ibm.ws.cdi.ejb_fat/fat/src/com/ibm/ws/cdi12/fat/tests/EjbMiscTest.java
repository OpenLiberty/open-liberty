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

import componenttest.custom.junit.runner.Mode;

/**
 * Scope tests for EJBs
 */
@Mode(FULL)
public class EjbMiscTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12EJB32Server", EjbMiscTest.class);

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static Archive[] buildShrinkWrap() {
       Archive[] apps = new Archive[3];

       JavaArchive multipleWarEmbeddedJar = ShrinkWrap.create(JavaArchive.class,"multipleWarEmbeddedJar.jar")
                        .addClass("com.ibm.ws.cdi.lib.MyEjb");

       apps[0] = ShrinkWrap.create(WebArchive.class, "multipleWar1.war")
                        .addClass("test.multipleWar1.TestServlet")
                        .addClass("test.multipleWar1.MyBean")
                        .add(new FileAsset(new File("test-applications/multipleWar1.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml")
                        .add(new FileAsset(new File("test-applications/multipleWar1.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(multipleWarEmbeddedJar);

       apps[1] = ShrinkWrap.create(WebArchive.class, "multipleWar2.war")
                        .addClass("test.multipleWar2.TestServlet")
                        .addClass("test.multipleWar2.MyBean")
                        .add(new FileAsset(new File("test-applications/multipleWar2.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml")
                        .add(new FileAsset(new File("test-applications/multipleWar2.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(multipleWarEmbeddedJar);

       apps[2] = ShrinkWrap.create(WebArchive.class, "ejbScope.war")
                        .addClass("com.ibm.ws.cdi12.test.ejb.scope.PostConstructingStartupBean")
                        .addClass("com.ibm.ws.cdi12.test.ejb.scope.PostConstructScopeServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejb.scope.RequestScopedBean");

       return apps;
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
