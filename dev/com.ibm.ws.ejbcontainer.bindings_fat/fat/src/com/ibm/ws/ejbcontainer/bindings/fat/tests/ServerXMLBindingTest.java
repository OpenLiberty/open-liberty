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
package com.ibm.ws.ejbcontainer.bindings.fat.tests;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.bindings.serverxml.bnd.web.ServerXMLBindingsTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class ServerXMLBindingTest extends FATServletClient {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            try {
                System.runFinalization();
                System.gc();
                server.serverDump("heap");
            } catch (Exception e1) {
                System.out.println("Failed to dump server");
                e1.printStackTrace();
            }
        }
    };

    private static String servlet = "ServerXMLBindingsWeb/ServerXMLBindingsTestServlet";

    @Server("com.ibm.ws.ejbcontainer.bindings.fat.serverxml.testserver")
    @TestServlet(servlet = ServerXMLBindingsTestServlet.class, contextRoot = "ServerXMLBindingsWeb")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.serverxml.testserver")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.bindings.fat.serverxml.testserver"));

    @Test
    public void testServerXMLBindings() throws Exception {

        //build app
        EnterpriseArchive ServerXMLTestApp = buildApplication(false);

        ShrinkHelper.exportAppToServer(server, ServerXMLTestApp);
        server.addInstalledAppForValidation("ServerXMLTestApp");

        server.startServer();

        //lookupServerXMLBindings
        FATServletClient.runTest(server, servlet, "lookupServerXMLBindings");

        //remove app
        server.removeAllInstalledAppsForValidation();

        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0338W");
        }

    }

    @Test
    public void testServerXMLBindingsOverride() throws Exception {

        //build app
        EnterpriseArchive ServerXMLTestApp = buildApplication(true);

        ShrinkHelper.exportAppToServer(server, ServerXMLTestApp);
        server.addInstalledAppForValidation("ServerXMLTestApp");

        server.startServer();

        //lookupServerXMLBindings
        FATServletClient.runTest(server, servlet, "lookupServerXMLBindings");

        //remove app
        server.removeAllInstalledAppsForValidation();

        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0338W");
        }

    }

    static org.jboss.shrinkwrap.api.Filter<ArchivePath> xmlFilter = new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {

        @Override
        public boolean include(ArchivePath arg0) {
            if (arg0.get().contains("ejb-jar.xml")) {
                return true;
            }
            return false;
        }

    };

    public EnterpriseArchive buildApplication(boolean override) throws Exception {

        // simple-binding-name
        JavaArchive SimpleBindingNameEJB = ShrinkHelper.buildJavaArchiveNoResources("SimpleBindingNameEJB.jar", "com.ibm.ejb3x.SimpleBindingName.ejb.");
        if (override) {
            ShrinkHelper.addDirectory(SimpleBindingNameEJB, "test-applications/SimpleBindingNameEJB.jar/resources");
        } else {
            ShrinkHelper.addDirectory(SimpleBindingNameEJB, "test-applications/SimpleBindingNameEJB.jar/resources", xmlFilter);
        }

        // component-id
        JavaArchive ComponentIDBndEJB = ShrinkHelper.buildJavaArchiveNoResources("ComponentIDBndEJB.jar", "com.ibm.ejb3x.ComponentIDBnd.ejb.");
        if (override) {
            ShrinkHelper.addDirectory(ComponentIDBndEJB, "test-applications/ComponentIDBndEJB.jar/resources");
        } else {
            ShrinkHelper.addDirectory(ComponentIDBndEJB, "test-applications/ComponentIDBndEJB.jar/resources", xmlFilter);
        }

        // local-home-binding and remote-home-binding
        JavaArchive HomeBindingNameEJB = ShrinkHelper.buildJavaArchiveNoResources("HomeBindingNameEJB.jar", "com.ibm.ejb3x.HomeBindingName.ejb.");
        if (override) {
            ShrinkHelper.addDirectory(HomeBindingNameEJB, "test-applications/HomeBindingNameEJB.jar/resources");
        } else {
            ShrinkHelper.addDirectory(HomeBindingNameEJB, "test-applications/HomeBindingNameEJB.jar/resources", xmlFilter);
        }

        // binding-name
        JavaArchive BindingNameEJB = ShrinkHelper.buildJavaArchiveNoResources("BindingNameEJB.jar", "com.ibm.ejb3x.BindingName.ejb.");
        if (override) {
            ShrinkHelper.addDirectory(BindingNameEJB, "test-applications/BindingNameEJB.jar/resources");
        } else {
            ShrinkHelper.addDirectory(BindingNameEJB, "test-applications/BindingNameEJB.jar/resources", xmlFilter);
        }

        WebArchive ServerXMLBindingsWeb = ShrinkHelper.buildDefaultAppFromPathNoResources("ServerXMLBindingsWeb.war", null, "com.ibm.ws.ejbcontainer.bindings.serverxml.bnd.web");
        if (override) {
            ShrinkHelper.addDirectory(ServerXMLBindingsWeb, "test-applications/ServerXMLBindingsWeb.war/resources");
        } else {
            ShrinkHelper.addDirectory(ServerXMLBindingsWeb, "test-applications/ServerXMLBindingsWeb.war/resources", xmlFilter);
        }

        EnterpriseArchive ServerXMLTestApp = ShrinkWrap.create(EnterpriseArchive.class, "ServerXMLTestApp.ear");
        ShrinkHelper.addDirectory(ServerXMLTestApp, "test-applications/ServerXMLTestApp.ear/resources");

        //add all modules
        ServerXMLTestApp.addAsModules(SimpleBindingNameEJB, ComponentIDBndEJB, HomeBindingNameEJB,
                                      BindingNameEJB, ServerXMLBindingsWeb);

        return ServerXMLTestApp;
    }

}
