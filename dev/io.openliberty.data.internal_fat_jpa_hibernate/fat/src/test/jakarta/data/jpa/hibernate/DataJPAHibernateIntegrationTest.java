/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.hibernate;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.jpa.hibernate.integration.web.DataJPAHibernateIntegrationTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
@MaximumJavaLevel(javaLevel = 25) // TODO remove once Hibernate upgrades their ByteBuddy dependency to a version that supports java 26+
public class DataJPAHibernateIntegrationTest extends FATServletClient {
    private static final String APP_NAME = "DataJPAHibernateIntegrationApp";

    @Server("io.openliberty.data.internal.fat.jpa.hibernate.integrate")
    @TestServlet(servlet = DataJPAHibernateIntegrationTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp(APP_NAME, "test.jakarta.data.jpa.hibernate.integration.web");
        ShrinkHelper.exportAppToServer(server, war);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
