/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;

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
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@Mode(TestMode.FULL)
public class JaxWithNoInjectTest extends LoggingTest {

    public static LibertyServer server;

    protected SharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive jaxwsResourceInjectionApp = ShrinkWrap.create(WebArchive.class, "jaxrsResourceInjection.war")
                        .addPackage("com.ibm.ws.cdi12.test.jax.resource")
                        .add(new FileAsset(new File("test-applications/jaxrsResourceInjection.war/resources/WEB-INF/ibm-web-bnd.xml")), "/WEB-INF/ibm-web-bnd.xml")
                        .add(new FileAsset(new File("test-applications/jaxrsResourceInjection.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");

        server = LibertyServerFactory.getLibertyServer("cdi12JaxNoInjectServer");
        ShrinkHelper.exportDropinAppToServer(server, jaxwsResourceInjectionApp);
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application jaxrsResourceInjectionApp started");
    }

    @Test
    public void testResourceInjectionIntoJaxClassWithNoInjectAnnotation() throws Exception {
        HttpUtils.findStringInUrl(server, "/jaxrsResourceInjection/starter/resource", "DS =com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
