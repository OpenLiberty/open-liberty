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
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * These tests use a runtime feature to destroy a contextual object.
 * The test passes if that object exists before destroy is called
 * and is null afterwards
 */

@Mode(TestMode.FULL)
public class AlterableContextTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12AlterableContextServer");

    @BuildShrinkWrap
    public static Archive<?> buildShrinkWrap() {

        JavaArchive alterableContextExtension = ShrinkWrap.create(JavaArchive.class, "alterableContextExtension.jar");
        alterableContextExtension.addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.DirtySingleton");
        alterableContextExtension.addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextBean");
        alterableContextExtension.addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextExtension");
        alterableContextExtension.add(new FileAsset(new File("test-applications/alterableContextExtension.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                      "/META-INF/services/javax.enterprise.inject.spi.Extension");
        alterableContextExtension.add(new FileAsset(new File("test-applications/alterableContextExtension.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        WebArchive alterableContextApp = ShrinkWrap.create(WebArchive.class, "alterableContextApp.war");
        alterableContextApp.addClass("com.ibm.ws.cdi12.alterablecontext.test.AlterableContextTestServlet");
        alterableContextApp.add(new FileAsset(new File("test-applications/alterableContextApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        alterableContextApp.addAsLibrary(alterableContextExtension);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "alterableContextsApp.ear");
        ear.add(new FileAsset(new File("test-applications/alterableContextsApp.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml");
        ear.add(new FileAsset(new File("test-applications/alterableContextsApp.ear/resources/META-INF/application.xml")), "/META-INF/application.xml");
        ear.addAsModule(alterableContextApp);

        return ear;
    }

    /** {@inheritDoc} */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @Test
    public void testBeanWasFound() throws Exception {
        this.verifyResponse("/alterableContextApp/", "I got this from my alterablecontext: com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextBean");
    }

    @Test
    public void testBeanWasDestroyed() throws Exception {
        this.verifyResponse("/alterableContextApp/", "Now the command returns: null");
    }

}
