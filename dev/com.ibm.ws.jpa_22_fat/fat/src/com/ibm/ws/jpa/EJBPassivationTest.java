/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
public class EJBPassivationTest extends FATServletClient {
    public static final String APP_NAME = "ejbpassivation";
    public static final String CTX_ROOT = "ejbpassivation";
    public static final String SERVLET = "JpaPassivationServlet";

    @Server("EJBPassivationServer")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {
        final String resPath = "test-applications/" + APP_NAME + "/resources/";
        PrivHelper.generateCustomPolicy(server1, FATSuite.JAXB_PERMS);

        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        app.addPackage("ejbpassivation.data");
        app.addPackage("ejbpassivation.ejb");
        app.addPackage("ejbpassivation.web");
        ShrinkHelper.addDirectory(app, resPath);
        ShrinkHelper.exportDropinAppToServer(server1, app);

        server1.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWWJP9991W");
    }

    @Test
    public void testBasicPassivation() throws Exception {
        runTest("testBasicPassivation");
    }

    // Disabled: EJB Container doesn't permit passivating SFSB with @PersistenceUnit injections.
    //@Test
    public void testBasicPUPassivation() throws Exception {
        runTest("testBasicPUPassivation");
    }

    private void runTest(String test) throws Exception {
        FATServletClient.runTest(server1, CTX_ROOT + "/" + SERVLET, test);

    }
}
