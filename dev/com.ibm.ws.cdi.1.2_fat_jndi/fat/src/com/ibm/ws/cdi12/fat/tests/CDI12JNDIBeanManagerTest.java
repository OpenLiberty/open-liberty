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

import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

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

public class CDI12JNDIBeanManagerTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12BasicServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        WebArchive cdi12helloworldtest = ShrinkWrap.create(WebArchive.class, "cdi12helloworldtest.war")
                        .addClass("cdi12.helloworld.test.HelloBean")
                        .addClass("cdi12.helloworld.test.HelloServlet")
                        .add(new FileAsset(new File("test-applications/cdi12helloworldtest.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/cdi12helloworldtest.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        return ShrinkWrap.create(EnterpriseArchive.class,"cdi12helloworldtest.ear")
                        .add(new FileAsset(new File("test-applications/cdi12helloworldtest.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(cdi12helloworldtest);
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

    /**
     * Test bean manager can be looked up via java:comp/BeanManager
     *
     * @throws Exception
     */
    @Test
    public void testHelloWorldJNDIBeanManagerServlet() throws Exception {
        this.verifyResponse("/cdi12helloworldtest/hello", "Hello World CDI 1.2! JNDI BeanManager PASSED! JNDI BeanManager from Servlet PASSED!");
    }

}
