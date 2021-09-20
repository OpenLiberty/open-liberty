/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.war.fat.tests;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.fat.ejbinwar.EJBInWARServlet;
import com.ibm.ws.ejbcontainer.fat.ejbinwarbnd.EJBInWARBndServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import junit.framework.Assert;

@RunWith(FATRunner.class)
public class EjbInWarTest extends FATServletClient {
    @Server("com.ibm.ws.ejbcontainer.war.fat.EjbInWarServer")
    @TestServlets({ @TestServlet(servlet = EJBInWARServlet.class, contextRoot = "EJBInWAR"),
                    @TestServlet(servlet = EJBInWARBndServlet.class, contextRoot = "EJBInWARBnd") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.war.fat.EjbInWarServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.war.fat.EjbInWarServer")).andWith(FeatureReplacementAction.EE9_FEATURES().forServers("com.ibm.ws.ejbcontainer.war.fat.EjbInWarServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the EJBInWAR war application
        WebArchive EJBInWAR = ShrinkHelper.buildDefaultApp("EJBInWAR.war", "com.ibm.ws.ejbcontainer.fat.ejbinwar.", "com.ibm.ws.ejbcontainer.fat.ejbinwar.ejb.");
        ShrinkHelper.exportDropinAppToServer(server, EJBInWAR, DeployOptions.SERVER_ONLY);

        // Use ShrinkHelper to build the EJBInWARBnd war application
        WebArchive EJBInWARBnd = ShrinkHelper.buildDefaultApp("EJBInWARBnd.war", "com.ibm.ws.ejbcontainer.fat.ejbinwarbnd.*");
        ShrinkHelper.exportDropinAppToServer(server, EJBInWARBnd, DeployOptions.SERVER_ONLY);

        // Use ShrinkHelper to build the EJBInWARPackagingWeb war application
        JavaArchive BeanInterfaceHolderLib = ShrinkHelper.buildJavaArchive("BeanInterfaceHolderLib.jar", "com.ibm.ws.ejbcontainer.fat.beaninterfaceholderlib.");
        WebArchive EJBInWARPackagingWeb = ShrinkHelper.buildDefaultApp("EJBInWARPackagingWeb.war", "com.ibm.ws.ejbcontainer.fat.ejbinwarpackaging.*");
        EJBInWARPackagingWeb.addAsLibrary(BeanInterfaceHolderLib);
        ShrinkHelper.exportDropinAppToServer(server, EJBInWARPackagingWeb, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }

    @Test
    public void testStandaloneModuleCNTR0167I() throws Exception {
        final String regex = "CNTR0167I:.*java:global/EJBInWAR/EJBInWARStatelessBean!com\\.ibm\\.ws\\.ejbcontainer\\.fat\\.ejbinwar\\.ejb\\.EJBInWARLocal";
        Assert.assertFalse("Should have found CNTR0167I with java:global name without app name for a standalone module",
                           server.findStringsInLogs(regex).isEmpty());
    }

    /**
     * Ensure different ways of packaging an EJB in a WAR,
     * eg. Within the WEB-INF/classes directory or in a .jar file within WEB-INF/lib
     */
    @Test
    public void verifyBeanName() throws Exception {
        runTest(server, "EJBInWARPackagingWeb/EJBInWARPackagingServlet", "verifyBeanName");
    }

    /**
     * Ensure interceptors can be packaged in a separate location than the bean class
     */
    @Test
    public void verifyInterceptor() throws Exception {
        runTest(server, "EJBInWARPackagingWeb/EJBInWARPackagingServlet", "verifyInterceptor");
    }

    /**
     * Ensure EJBs can be packaged within/used by web fragments
     */
    @Test
    public void verifyWebFragment() throws Exception {
        runTest(server, "EJBInWARPackagingWeb/FragmentServlet", "verifyWebFragment");
    }
}
