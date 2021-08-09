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

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

/**
 * These tests verify that you can look up the bean manager as per http://docs.jboss.org/cdi/spec/1.1/cdi-spec.html#provider
 */
public class BeanManagerLookupTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12BasicServer");

    @BuildShrinkWrap
    public static Archive<?> buildShrinkWrap() {

        WebArchive beanManagerLookupApp = ShrinkWrap.create(WebArchive.class, "beanManagerLookupApp.war");
        beanManagerLookupApp.addClass("cdi12.beanmanagerlookup.test.BeanManagerLookupServlet");
        beanManagerLookupApp.addClass("cdi12.beanmanagerlookup.test.MyBean");

        beanManagerLookupApp.add(new FileAsset(new File("test-applications/beanManagerLookupApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "beanManagerLookupApp.ear");
        ear.add(new FileAsset(new File("test-applications/beanManagerLookupApp.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml");
        ear.add(new FileAsset(new File("test-applications/beanManagerLookupApp.ear/resources/META-INF/application.xml")), "/META-INF/application.xml");
        ear.addAsModule(beanManagerLookupApp);

        return ear;
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
    public void testbeanManagerLookup() throws Exception {
        this.verifyResponse("/beanManagerLookupApp/",
                            new String[] { "CDI.current().getBeanManager: true",
                                           "BeanManager from CDI.current().getBeanManager found a Bean." });
    }

    @Test
    public void testbeanManagerLookupJndi() throws Exception {
        this.verifyResponse("/beanManagerLookupApp/",
                            new String[] { "BeanManager from jndi found a Bean.",
                                           "Bean manager from JNDI: true" });
    }

    @Test
    public void testbeanManagerLookupInject() throws Exception {
        this.verifyResponse("/beanManagerLookupApp/",
                            new String[] { "BeanManager from injection found a Bean.",
                                           "Bean manager from inject: true" });
    }
}
