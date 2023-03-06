/*
 * =============================================================================
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
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
                IORTest.class,
                IIOPSimpleFatTest.class,
                CosNamingViaStringToObjectFatTest.class,
                CosNamingViaORBInitRefFatTest.class,
                IIOPClientEJBTest.class, //TODO: Check if this really is working
                IIOPClientServletTest.class,
                InterceptorFatTest.class
})
public class FATSuite {

    private static final JavaArchive INTERFACES_JAR;
    private static final JavaArchive TEST_CORBA_BEAN_JAR;
    private static final EnterpriseArchive TEST_CORBA_EAR;
    private static final WebArchive TEST_CORBA_WEB_WAR;

    static {
        try {
            INTERFACES_JAR = ShrinkHelper.buildJavaArchive("interfaces.jar", "shared");
            System.out.println(INTERFACES_JAR);

            TEST_CORBA_BEAN_JAR = ShrinkHelper.buildJavaArchive("test.corba.bean.jar", "test.corba.bean.jar");
            ShrinkHelper.addDirectory(TEST_CORBA_BEAN_JAR, "test-applications/test.corba.bean.jar.resources");
            System.out.println(TEST_CORBA_BEAN_JAR);

            TEST_CORBA_EAR = ShrinkWrap.create(EnterpriseArchive.class, "test.corba.ear");
            TEST_CORBA_EAR.addAsModule(TEST_CORBA_BEAN_JAR);
            TEST_CORBA_EAR.addAsLibraries(INTERFACES_JAR);
            ShrinkHelper.addDirectory(TEST_CORBA_EAR, "test-applications/test.corba.ear.resources");

            TEST_CORBA_WEB_WAR = ShrinkHelper.buildDefaultApp("test.corba.web.war", "test.corba.web.war");
            TEST_CORBA_WEB_WAR.addAsLibrary(INTERFACES_JAR);
            ShrinkHelper.addDirectory(TEST_CORBA_WEB_WAR, "test-applications/test.corba.web.war.resources");
        } catch (Exception e) {
            throw (AssertionFailedError)new AssertionFailedError("Could not assemble test applications").initCause(e);
        }
    }

    static final List<? extends Archive<?>> SERVER_APPS = Collections.unmodifiableList(Arrays.asList(TEST_CORBA_WEB_WAR, TEST_CORBA_EAR));

    @BeforeClass
    public static void installTestFeatures() throws Exception {
	//The server used does not matter, it can be any to install the features
	LibertyServer server = getLibertyServer("buckyball");
	server.installSystemBundle("test.user.feature");
		server.installSystemFeature("test.user.feature-1.0");
		server.installSystemBundle("test.iiop");
		server.installSystemFeature("test.iiop-1.0");
    }

    @AfterClass
    public static void uninstallTestFeatures() throws Exception {
	LibertyServer server = getLibertyServer("buckyball");
	server.uninstallSystemBundle("test.user.feature");
		server.uninstallSystemFeature("test.user.feature-1.0");
		server.uninstallSystemBundle("test.iiop");
		server.uninstallSystemFeature("test.iiop-1.0");
    }

}