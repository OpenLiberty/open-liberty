/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.concurrent.persistent.jca;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * General tests that don't involve updating configuration while the server is running.
 */
@RunWith(FATRunner.class)
public class PersistRATest {

    private static final String APP = "PersistRAApp";
    private static final String RAR_NAME = "PersistRA";
    private static final String WAR_NAME = "fvtweb";
    
    private static LibertyServer server = FATSuite.server;

    /**
     * Utility method to run a test in a servlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String test, String webmodule) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + webmodule + "?test=" + test);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
    	//Creating application as an EAR
    	WebArchive web = ShrinkHelper.buildDefaultApp(WAR_NAME, "web");
    	EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP + ".ear")
    			.addAsModule(web);
    	
    	ShrinkHelper.exportAppToServer(server, ear);
    			
    	//Creating resource adapter as a RAR
    	ResourceAdapterArchive rar = ShrinkHelper.buildDefaultRar(RAR_NAME, "fat.persistra.resourceadapter");
    	
    	ShrinkHelper.exportToServer(server, "connectors", rar);
    	
    	//Start Server
    	server.startServer();
    			
    	//Check everything went okay
        server.waitForStringInLog("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testResourceAdapterSchedulesTaskWhenStarted() throws Exception {
        runInServlet("testResourceAdapterSchedulesTaskWhenStarted", WAR_NAME);
    }

    @Test
    public void testServletSchedulesTaskFromResourceAdapter() throws Exception {
        runInServlet("testServletSchedulesTaskFromResourceAdapter", WAR_NAME);
    }

    @Test
    public void testServletSchedulesSerializableTaskFromResourceAdapter() throws Exception {
        runInServlet("testServletSchedulesSerializableTaskFromResourceAdapter", WAR_NAME);
    }
}
