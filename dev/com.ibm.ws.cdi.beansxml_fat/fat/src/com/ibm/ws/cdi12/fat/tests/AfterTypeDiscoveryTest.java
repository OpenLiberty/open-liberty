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
import java.util.HashMap;
import java.util.Map;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AfterTypeDiscoveryTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12AfterTypeDiscoveryServer");

    @BuildShrinkWrap
    public static Map<Archive<?>, String> buildShrinkWrap() {
        Map<Archive<?>, String> archives = new HashMap<Archive<?>, String>();
        WebArchive afterTypeDiscoveryApp = ShrinkWrap.create(WebArchive.class, "afterTypeDiscoveryApp.war");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.GlobalState");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeNotAlternative");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.InterceptedAfterType");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeBeanDecorator");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeBean");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.InterceptedBean");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeExtension");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeAlternativeTwo");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeServlet");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeAlternativeInterface");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.UseAlternative");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeInterceptorImpl");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeAlternativeOne");
        afterTypeDiscoveryApp.addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeInterface");
        afterTypeDiscoveryApp.add(new FileAsset(new File("test-applications/afterTypeDiscoveryApp.war/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml");
        afterTypeDiscoveryApp.add(new FileAsset(new File("test-applications/afterTypeDiscoveryApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        afterTypeDiscoveryApp.add(new FileAsset(new File("test-applications/afterTypeDiscoveryApp.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                  "/META-INF/services/javax.enterprise.inject.spi.Extension");
        archives.put(afterTypeDiscoveryApp, "/apps");
        return archives;
    }

    /** {@inheritDoc} */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testAfterTypeDecoratorAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", "New msg: decorated");
    }

    @Test
    public void testAfterTypeInterceptorAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", "intercepted");
    }

    @Test
    public void testAfterTypeBeanAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", "hello world");
    }

    @Test
    public void testAfterTypeAlternativeAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", new String[] { "expecting one: alternative one", "expecting two: alternative two" });
    }

}
