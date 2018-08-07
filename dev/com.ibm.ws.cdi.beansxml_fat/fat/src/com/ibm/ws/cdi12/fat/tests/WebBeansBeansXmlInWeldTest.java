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

import java.io.File;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WebBeansBeansXmlInWeldTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12WebBeansBeansXmlServer");

    @BuildShrinkWrap
    public static Archive[] buildShrinkWrap() {
        Archive[] archives = new Archive[2];

        WebArchive webBeansBeansXmlInterceptors = ShrinkWrap.create(WebArchive.class, "webBeansBeansXmlInterceptors.war")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlInterceptors.InterceptedBean")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlInterceptors.BasicInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlInterceptors.BasicInterceptorBinding")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlInterceptors.SimpleTestServlet")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlInterceptors.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlInterceptors.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        WebArchive webBeansBeansXmlDecorators = ShrinkWrap.create(WebArchive.class, "webBeansBeansXmlDecorators.war")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators.Bean")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators.DecoratedBean")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators.BeanDecorator")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators.SimpleTestServlet")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlDecorators.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlDecorators.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        EnterpriseArchive webBeansBeansXmlDecoratorsEar = ShrinkWrap.create(EnterpriseArchive.class,"webBeansBeansXmlDecorators.ear")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlDecorators.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(webBeansBeansXmlDecorators);

        EnterpriseArchive webBeansBeansXmlInterceptorsEar = ShrinkWrap.create(EnterpriseArchive.class,"webBeansBeansXmlInterceptors.ear")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlInterceptors.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(webBeansBeansXmlInterceptors);
       
        archives[0] = webBeansBeansXmlDecoratorsEar;
        archives[1] = webBeansBeansXmlInterceptorsEar;
        return archives;
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
    public void testInterceptorsWithWebBeansBeansXml() throws Exception {
        this.verifyResponse("/webBeansBeansXmlInterceptors/", "Last Intercepted by: BasicInterceptor");
    }

    @Test
    public void testDecoratorsWithWebBeansBeansXml() throws Exception {
        this.verifyResponse("/webBeansBeansXmlDecorators/", "decorated message");
    }

    /**
     * Stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * W WELD-001208: Error when validating
             * wsjar:file:/C:/workspaces/KateCDOpenStreamLiberty/build.image/wlp/usr/servers/cdi12WebBeansBeansXmlServer/workarea/org.eclipse.osgi
             * /65/data/cache/com.ibm.ws.app.manager_13/.cache/webBeansBeansXmlDecorators.war!/WEB-INF/beans.xml@4 against xsd. cvc-elt.1: Cannot find the declaration of element
             * 'WebBeans'.
             * W WELD-001208: Error when validating
             * wsjar:file:/C:/workspaces/KateCDOpenStreamLiberty/build.image/wlp/usr/servers/cdi12WebBeansBeansXmlServer/workarea/org.eclipse.osgi
             * /65/data/cache/com.ibm.ws.app.manager_12/.cache/webBeansBeansXmlInterceptors.war!/WEB-INF/beans.xml@4 against xsd. cvc-elt.1: Cannot find the declaration of element
             * 'WebBeans'.
             *
             * The following exception has been seen but as long as the test passes
             * then we are happy that the application did manage to start eventually
             * so we will also ignore the following exception:
             * CWWKZ0022W: Application webBeansBeansXmlInterceptors has not started in 30.001 seconds.
             */
            SHARED_SERVER.getLibertyServer().stopServer("WELD-001208", "CWWKZ0022W");
        }
    }
}
