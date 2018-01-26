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

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

public class PassivationBeanTests extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12PassivationServer");

    private static boolean hasSetup = false;

    @BuildShrinkWrap
    public static Archive[] buildShrinkWrap() {

        if (hasSetup) {
          return null;
        }

        Archive[] archives = new Archive[2];

        WebArchive transientReferenceInSessionPersist = ShrinkWrap.create(WebArchive.class, "transientReferenceInSessionPersist.war")
                        .addClass("cdi12.transientpassivationtest.GlobalState")
                        .addClass("cdi12.transientpassivationtest.BeanWithInjectionPointMetadata")
                        .addClass("cdi12.transientpassivationtest.MyStatefulSessionBean")
                        .addClass("cdi12.transientpassivationtest.ConstructorInjectionPointBean")
                        .addClass("cdi12.transientpassivationtest.MethodInjectionPointBean")
                        .addClass("cdi12.transientpassivationtest.AnimalStereotype")
                        .addClass("cdi12.transientpassivationtest.BeanHolder")
                        .addClass("cdi12.transientpassivationtest.FieldInjectionPointBean")
                        .addClass("cdi12.transientpassivationtest.PassivationBean")
                        .add(new FileAsset(new File("test-applications/transientReferenceInSessionPersist.war/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .add(new FileAsset(new File("test-applications/transientReferenceInSessionPersist.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/transientReferenceInSessionPersist.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/transientReferenceInSessionPersist.war/resources/PassivationCapability.jsp")), "/PassivationCapability.jsp");

        WebArchive passivationBeanWar = ShrinkWrap.create(WebArchive.class, "passivationBean.war")
                        .addClass("com.ibm.ws.cdi12.test.passivation.GlobalState")
                        .addClass("com.ibm.ws.cdi12.test.passivation.TransiantDependentScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.passivation.PassivationServlet")
                        .addClass("com.ibm.ws.cdi12.test.passivation.BeanHolder")
                        .addClass("com.ibm.ws.cdi12.test.passivation.TransiantDependentScopedBeanTwo")
                        .add(new FileAsset(new File("test-applications/passivationBean.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/passivationBean.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");

        EnterpriseArchive passivationBean = ShrinkWrap.create(EnterpriseArchive.class,"passivationBean.ear")
                        .add(new FileAsset(new File("test-applications/passivationBean.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .add(new FileAsset(new File("test-applications/passivationBean.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(passivationBeanWar);

        archives[0] = transientReferenceInSessionPersist;
        archives[1] = passivationBean;
        hasSetup = true;
        return archives;
    }

    @Test
    public void testTransientReference() throws Exception {
        //Enable when 158940 is complete.
        this.verifyResponse("/passivaiton/", new String[] { "destroyed-one", "doNothing2destroyed-two" });
        this.verifyResponse("/passivaiton/", new String[] { "destroyed-one" });
    }

    /**
     * Passivation Capability FVT Test Group
     *
     * @throws Exception
     *             if validation fails, or if an unexpected error occurs
     */
    @Test
    public void testTransientReferenceInPassivation() throws Exception {

        //I don't know why adding a shared web browser is nessacary here. It wasn't in the CDI1.0 test I'm copying.
        WebBrowser wb = createWebBrowserForTestCase();

        this.verifyResponse(wb, "/transientReferenceInSessionPersist/PassivationCapability.jsp",
                            new String[] { "Initialized", "PASSED", "MyStatefulSessionBean was destroyed", "injected into PassivatationBean and it has been destroyed" });

        SHARED_SERVER.getApplicationMBean("transientReferenceInSessionPersist").restart();

        this.verifyResponse(wb, "/transientReferenceInSessionPersist/PassivationCapability.jsp",
                            new String[] { "Reused", "PASSED", "destroy" });
    }

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {

        return SHARED_SERVER;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (SHARED_SERVER.getLibertyServer() != null) {
            SHARED_SERVER.getLibertyServer().stopServer();
        }
    }

}
