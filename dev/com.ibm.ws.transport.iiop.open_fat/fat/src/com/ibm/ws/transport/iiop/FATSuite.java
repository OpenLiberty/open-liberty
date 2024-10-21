/*
 * Copyright (c) 2020,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop;

import static componenttest.topology.impl.LibertyServerFactory.getLibertyServer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import junit.framework.AssertionFailedError;

@RunWith(Suite.class)
@SuiteClasses({
    CosNamingViaStringToObjectFatTest.class,
    IIOPClientEJBTest.class,
    IIOPClientServletTest.class,
    InterceptorTest.class,
    IORTest.class,
})
public class FATSuite {
    static final EnterpriseArchive TEST_CORBA_EAR;
    static final WebArchive TEST_CORBA_WEB_WAR;
    static final WebArchive TEST_CORBA_REMOTE_WAR;

    static {
        try {
            final JavaArchive interfacesJar = ShrinkHelper.buildJavaArchive("interfaces.jar", "shared", "test.iiop.common");
            final JavaArchive beanJar = ShrinkHelper.buildJavaArchive("test.corba.bean.jar", "test.corba.bean.jar");
            ShrinkHelper.addDirectory(beanJar, "test-applications/test.corba.bean.jar.resources");

            TEST_CORBA_EAR = ShrinkWrap.create(EnterpriseArchive.class, "test.corba.ear");
            TEST_CORBA_EAR.addAsModule(beanJar);
            TEST_CORBA_EAR.addAsLibraries(interfacesJar);
            ShrinkHelper.addDirectory(TEST_CORBA_EAR, "test-applications/test.corba.ear.resources");

            TEST_CORBA_WEB_WAR = ShrinkHelper.buildDefaultApp("test.corba.web.war", "test.corba.web.war");
            TEST_CORBA_WEB_WAR.addAsLibrary(interfacesJar);
            ShrinkHelper.addDirectory(TEST_CORBA_WEB_WAR, "test-applications/test.corba.web.war.resources");

            TEST_CORBA_REMOTE_WAR = ShrinkHelper.buildDefaultApp("test.corba.remote.war", "test.corba.remote.war");
            TEST_CORBA_REMOTE_WAR.addAsLibrary(interfacesJar);
            ShrinkHelper.addDirectory(TEST_CORBA_REMOTE_WAR, "test-applications/test.corba.remote.war.resources");
        } catch (Exception e) {
            throw (AssertionFailedError)new AssertionFailedError("Could not assemble test applications").initCause(e);
        }
    }

    static final List<? extends Archive<?>> SERVER_APPS = Collections.unmodifiableList(Arrays.asList(TEST_CORBA_WEB_WAR, TEST_CORBA_EAR));

    @BeforeClass
    public static void installTestFeatures() throws Exception {
        // Use ANY server to install bundles and features
        LibertyServer server = getLibertyServer("bandyball");
        server.installUserBundle("test.user.feature");
        server.installUserFeature("test.user.feature-1.0");
        server.installSystemBundle("test.iiop");
        server.installSystemFeature("test.iiop-1.0");
        server.installSystemFeature("test.iiop.client-1.0");
        server.installSystemBundle("test.iiop.interceptor");
        server.installSystemFeature("test.iiop.interceptor-1.0");
   }

    @AfterClass
    public static void uninstallTestFeatures() throws Exception {
        // Use ANY server to uninstall bundles and features
        LibertyServer server = getLibertyServer("buckyball");
        server.uninstallUserBundle("test.user.feature");
        server.uninstallUserFeature("test.user.feature-1.0");
        server.uninstallSystemBundle("test.iiop");
        server.uninstallSystemFeature("test.iiop-1.0");
        server.uninstallSystemFeature("test.iiop.client-1.0");
        server.uninstallSystemBundle("test.iiop.interceptor");
        server.uninstallSystemFeature("test.iiop.interceptor-1.0");
    }

}