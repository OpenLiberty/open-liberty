/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.template.web.TemplateTestServlet;
import test.jakarta.data.web.DataTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11) // TODO 17
public class TemplateTest extends FATServletClient {

    @Server("io.openliberty.data.internal.template.fat")
    @TestServlet(servlet = TemplateTestServlet.class, contextRoot = "TemplateTestApp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("TemplateTestApp", "test.jakarta.data.template.web");
        ShrinkHelper.exportAppToServer(server, war);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
