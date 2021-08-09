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
import com.ibm.ws.fat.util.browser.WebBrowser;

public class JNDILookupTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12JNDIServer");

    @BuildShrinkWrap
    public static Archive<?> buildShrinkWrap() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jndiLookup.jar"); 
        jar.addClass("com.ibm.ws.cdi12.test.jndi.observer.ObserverBean");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jndiLookup.war");
        war.addClass("com.ibm.ws.cdi12.test.jndi.LookupServlet");
        war.addClass("com.ibm.ws.cdi12.test.jndi.JNDIStrings");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jndiLookup.ear");
        ear.addAsLibrary(jar);
        ear.addAsModule(war);
        ear.add(new FileAsset(new File("test-applications/jndiLookup.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml");
        ear.add(new FileAsset(new File("test-applications/jndiLookup.ear/resources/META-INF/application.xml")), "/META-INF/application.xml");

        return ear;
    }

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testJNDILookup() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        SHARED_SERVER.verifyResponse(browser, "/jndiLookup/", new String[] { "From Config: Value from Config", "From Bind: Value from Bind" });
    }

    @Test
    public void testJNDILookupInObserverJar() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        SHARED_SERVER.verifyResponse(browser, "/jndiLookup/", new String[] { "From ObserverBean: test/passed" });
    }

}
