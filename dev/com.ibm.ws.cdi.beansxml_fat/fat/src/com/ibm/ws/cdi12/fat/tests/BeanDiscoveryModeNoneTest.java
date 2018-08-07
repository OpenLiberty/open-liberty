/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
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

import org.junit.AfterClass;
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
import componenttest.custom.junit.runner.Mode;

public class BeanDiscoveryModeNoneTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12BeanDiscoveryModeNoneServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
         
         return ShrinkWrap.create(WebArchive.class, "beanDiscoveryModeNone.war")
                        .addClass("com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestBean1")
                        .add(new FileAsset(new File("test-applications/beanDiscoveryModeNone.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/beanDiscoveryModeNone.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    @ExpectedFFDC({ "javax.servlet.UnavailableException", "com.ibm.wsspi.injectionengine.InjectionException" })
    public void testcorrectExceptionThrown() throws Exception {
        this.verifyStatusCode("/beanDiscoveryModeNone/TestServlet", 404);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * Error 404: javax.servlet.UnavailableException: SRVE0319E: For the
             * [com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet] servlet,
             * com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet servlet class
             * was found, but a resource injection failure has occurred. The @Inject
             * java.lang.reflect.Field.testBean1 reference of type com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestBean1
             * for the null component in the beanDiscoveryModeNone.war module of the
             * beanDiscoveryModeNone application cannot be resolved.
             */
            SHARED_SERVER.getLibertyServer().stopServer("SRVE0319E", "CWNEN0035E", "CWOWB1008E");
        }
    }

}
