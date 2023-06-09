/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.jca.mbean.fat.app;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JCA_JDBC_JSR77_MBean_MultipleTest {
    @Server("com.ibm.ws.jca.jdbc.mbean.fat.app.mBeanMultiple")
    public static LibertyServer server;

    private static final String WAR_NAME = "fvtweb";
    private static final String APP_NAME = "fvtapp";

    @Rule
    public TestName testName = new TestName();

    private StringBuilder runInServlet(String test, String webmodule) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + webmodule + "?test=" + test);
        for (int numRetries = 2;; numRetries--) {
            Log.info(getClass(), "runInServlet", "URL is " + url);
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
            } catch (FileNotFoundException x) {
                if (numRetries > 0)
                    try {
                        Log.info(getClass(), "runInServlet", x + " occurred - will retry after 10 seconds");
                        Thread.sleep(10000);
                    } catch (InterruptedException interruption) {
                    }
                else
                    throw x;
            } finally {
                con.disconnect();
            }
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        // Build jar that will be in the RAR
        JavaArchive JCAFAT1_jar = ShrinkWrap.create(JavaArchive.class, "JCAFAT1.jar");
        JCAFAT1_jar.addPackage("fat.jca.resourceadapter.jar1");

        JavaArchive JCAFAT2_jar = ShrinkWrap.create(JavaArchive.class, "JCAFAT2.jar");
        JCAFAT2_jar.addPackage("fat.jca.resourceadapter.jar2");
        JCAFAT2_jar.add(JCAFAT1_jar, "/", ZipExporter.class);

        // Build the resource adapter
        ResourceAdapterArchive JCAFAT1_rar = ShrinkWrap.create(ResourceAdapterArchive.class, "JCAFAT1.rar");
        JCAFAT1_rar.as(JavaArchive.class).addPackage("fat.jca.resourceadapter");
        JCAFAT1_rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF/ra.xml"));
        JCAFAT1_rar.addAsLibrary(JCAFAT2_jar);
        ShrinkHelper.exportToServer(server, "connectors", JCAFAT1_rar);

        // Build the resource adapter
        ResourceAdapterArchive JCAFAT2_rar = ShrinkWrap.create(ResourceAdapterArchive.class, "JCAFAT2.rar");
        JCAFAT2_rar.as(JavaArchive.class).addPackage("fat.jca.resourceadapter");
        JCAFAT2_rar.addAsManifestResource(new File("test-resourceadapters/fvt-resourceadapter/resources/META-INF2/ra.xml"));
        JCAFAT2_rar.addAsLibrary(JCAFAT2_jar);
        ShrinkHelper.exportToServer(server, "connectors", JCAFAT2_rar);

        // Build the web module and application
        WebArchive fvtweb_war = ShrinkWrap.create(WebArchive.class, WAR_NAME + ".war");
        fvtweb_war.addPackage("web");
        fvtweb_war.addPackage("web.mdb");
        fvtweb_war.addPackage("web.mdb.bindings");
        fvtweb_war.addAsWebInfResource(new File("test-applications/" + WAR_NAME + "/resources/WEB-INF/ibm-ejb-jar-bnd.xml"));
        fvtweb_war.addAsWebInfResource(new File("test-applications/" + WAR_NAME + "/resources/WEB-INF/ibm-web-bnd.xml"));
        fvtweb_war.addAsWebInfResource(new File("test-applications/" + WAR_NAME + "/resources/WEB-INF/web.xml"));

        EnterpriseArchive fvtapp_ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        fvtapp_ear.addAsModule(fvtweb_war);
        ShrinkHelper.addDirectory(fvtapp_ear, "lib/LibertyFATTestFiles/" + APP_NAME);
        ShrinkHelper.exportAppToServer(server, fvtapp_ear);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKG0007W");
    }

    /**
     * test JCA ConnectionFactory MBean registration.
     */
    @Test
    public void testJCAConnectionFactoryMBeanRegistrationMultiple() throws Exception {
        String fvtweb = "fvtweb/JCAConnectionFactoryMBeanServlet";
        runInServlet(testName.getMethodName(), fvtweb);
    }

    /**
     * test JCA ManagedConnectionFactory MBean registration.
     */
    @Test
    public void testJCAManagedConnectionFactoryMBeanRegistrationMultiple() throws Exception {
        String fvtweb = "fvtweb/JCAManagedConnectionFactoryMBeanServlet";
        runInServlet(testName.getMethodName(), fvtweb);
    }

    /**
     * test JCA Resource MBean registration with multiple resources defined on the server.xml.
     */
    @Test
    public void testJCAResourceMBeanRegistrationMultiple() throws Exception {
        String fvtweb = "fvtweb/JCAResourceMBeanServlet";
        runInServlet(testName.getMethodName(), fvtweb);
    }

    /**
     * test JCA Resource Adapter MBean registration.
     */
    @Test
    public void testResourceAdapterMBeanRegistrationMultiple() throws Exception {
        String fvtweb = "fvtweb/ResourceAdapterMBeanServlet";
        runInServlet(testName.getMethodName(), fvtweb);
    }

    /**
     * test JCA Resource Adapter MBean registration.
     */
    @Test
    public void testResourceAdapterModuleMBeanRegistrationMultiple() throws Exception {
        String fvtweb = "fvtweb/ResourceAdapterModuleMBeanServlet";
        runInServlet(testName.getMethodName(), fvtweb);
    }

    /**
     * test JCA Resource Adapter Module MBean getdeploymentDescriptor() function with multiple unique
     * resource adapters defined on the server.xml.
     */
    @Test
    public void testResourceAdapterModuleMBeanMultipleDD() throws Exception {
        String fvtweb = "fvtweb/ResourceAdapterModuleMBeanServlet";
        runInServlet(testName.getMethodName(), fvtweb);
    }

    /**
     * test JDBC Datasource MBean registration.
     */
    @Test
    public void testJDBCDatasourceMBeanRegistrationMultiple() throws Exception {
        String fvtweb = "fvtweb/JDBCDataSourceMBeanServlet";
        runInServlet(testName.getMethodName(), fvtweb);
    }

    /**
     * test JDBC Driver MBean registration.
     */
    @Test
    public void testJDBCDriverMBeanRegistrationMultiple() throws Exception {
        String fvtweb = "fvtweb/JDBCDriverMBeanServlet";
        runInServlet(testName.getMethodName(), fvtweb);
    }

    /**
     * test JDBC Resource MBean registration.
     */
    @Test
    public void testJDBCResourceMBeanRegistrationMultiple() throws Exception {
        String fvtweb = "fvtweb/JDBCResourceMBeanServlet";
        runInServlet(testName.getMethodName(), fvtweb);
    }

}
