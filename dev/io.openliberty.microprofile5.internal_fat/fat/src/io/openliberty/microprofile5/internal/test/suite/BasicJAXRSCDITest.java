/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile5.internal.test.suite;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile5.internal.test.helloworld.HelloWorldApplication;
import io.openliberty.microprofile5.internal.test.helloworld.basic.BasicHelloWorldBean;

@RunWith(FATRunner.class)
public class BasicJAXRSCDITest {

    private static final String SERVER_NAME = "MP5Server";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String APP_NAME = "helloworld";

    private static final String MESSAGE = BasicHelloWorldBean.MESSAGE;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(HelloWorldApplication.class.getPackage())
                                   .addPackage(BasicHelloWorldBean.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    public void testSimple() throws IOException {
        runGetMethod(200, "/helloworld/helloworld", MESSAGE);
    }

    private StringBuilder runGetMethod(int exprc, String requestUri, String testOut) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br
                                                                     .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf(testOut) < 0)
                fail("Missing success message in output. " + lines);

            if (retcode != exprc)
                fail("Bad return Code from Get. Expected " + exprc + "Got"
                     + retcode);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    private int getPort() {
        return server.getHttpDefaultPort();
    }

    private String getHost() {
        return server.getHostname();
    }

}
