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
package test.jakarta.concurrency;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ConcurrencyJSPTest extends FATServletClient {

    @Server("com.ibm.ws.concurrent.fat.jakarta.jsp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive ConcurrencyJSPTestApp = ShrinkWrap.create(WebArchive.class, "ConcurrencyJSPTestApp.war");
        ShrinkHelper.addDirectory(ConcurrencyJSPTestApp, "test-applications/ConcurrencyJSPTestApp/resources");
        ShrinkHelper.exportAppToServer(server, ConcurrencyJSPTestApp);

        server.startServer();
    }

    @Test
    public void securityPropogatedTest() throws Exception {
        HttpUtils.setDefaultAuth("concurrency", "password");
        Assert.assertEquals("SUCCESS", HttpUtils.getHttpResponseAsString(server, "/ConcurrencyJSPTestApp/SecurityPropagated.jsp").trim());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
