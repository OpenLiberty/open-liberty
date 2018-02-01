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
import java.util.Map;
import java.util.HashMap;

import org.junit.ClassRule;
import org.junit.Test;

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

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
public class AfterTypeDiscoveryTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12AfterTypeDiscoveryServer");

    @BuildShrinkWrap
    public static Map<Archive,String> buildShrinkWrap() {
       Map<Archive,String> archives = new HashMap<Archive,String>();
       WebArchive afterTypeDiscoveryApp = ShrinkWrap.create(WebArchive.class, "afterTypeDiscoveryApp.war")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.GlobalState")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeNotAlternative")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.InterceptedAfterType")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeBeanDecorator")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeBean")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.InterceptedBean")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeExtension")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeAlternativeTwo")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeServlet")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeAlternativeInterface")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.UseAlternative")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeInterceptorImpl")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeAlternativeOne")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeInterface")
                        .add(new FileAsset(new File("test-applications/afterTypeDiscoveryApp.war/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                        .add(new FileAsset(new File("test-applications/afterTypeDiscoveryApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/afterTypeDiscoveryApp.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");
      archives.put(afterTypeDiscoveryApp, "publish/servers/cdi12AfterTypeDiscoveryServer/apps");
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
