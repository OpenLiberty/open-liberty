/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health.fat;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class HealthCheckExceptionTest {

    @Server("CDIHealthException")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive hcexceptionApp = ShrinkWrap.create(WebArchive.class,
                                                      "HealthCheckNoAPIImplApp.war")
                        .addPackages(true, "com.ibm.ws.microprofile.health.exception.testapp");
        ShrinkHelper.exportToServer(server1, "dropins", hcexceptionApp);

        server1.startServer();
        server1.waitForStringInLog("CWWKT0016I: Web application available.*health*");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }

    @Test
    @ExpectedFFDC({ "java.lang.reflect.InvocationTargetException",
                    "com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException", "java.lang.RuntimeException" })
    public void testHealthCheckException() throws Exception {

        URL healthURL = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/health");
        HttpURLConnection con = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, "/health");
        assertEquals(500, con.getResponseCode());

        assertEquals(null, con.getHeaderField("Content-Type"));
    }

    /**
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(CDIHealthCheckTest.class, method, msg);
    }

}
