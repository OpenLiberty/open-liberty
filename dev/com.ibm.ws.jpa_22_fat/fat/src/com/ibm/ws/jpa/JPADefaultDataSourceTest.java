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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.JPA;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.PrivHelper;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JPADefaultDataSourceTest {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";",
                                                "permission java.lang.RuntimePermission \"accessDeclaredMembers\";" };

    @Server("JPADefaultDataSourceServer_JTANJTA")
    public static LibertyServer server_JTA_NJTA;

    @Server("JPADefaultDataSourceServer_JTA")
    public static LibertyServer server_JTA;

    @Server("JPADefaultDataSourceServer_NJTA")
    public static LibertyServer server_NJTA;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server_JTA_NJTA, JAXB_PERMS);
        PrivHelper.generateCustomPolicy(server_JTA, JAXB_PERMS);
        PrivHelper.generateCustomPolicy(server_NJTA, JAXB_PERMS);

        final WebArchive jdbcwebapp = ShrinkWrap.create(WebArchive.class, "jdbcwebapp.war");
        jdbcwebapp.addPackage("jpadds.web.jdbc");
        ShrinkHelper.exportDropinAppToServer(server_JTA_NJTA, jdbcwebapp);
        ShrinkHelper.exportDropinAppToServer(server_JTA, jdbcwebapp);
        ShrinkHelper.exportDropinAppToServer(server_NJTA, jdbcwebapp);
    }

    @AfterClass
    public static void tearDown() throws Exception {

    }

    @Before
    public void setupTest() throws Exception {

    }

    private JPA getJPAConfigElement(ServerConfiguration sc) {
        ConfigElementList<JPA> jpaElements = sc.getJPAs();
        JPA jpaElement = null;
        if (jpaElements.isEmpty()) {
            jpaElement = new JPA();
            jpaElements.add(jpaElement);
        } else {
            jpaElement = jpaElements.get(0);
        }
        return jpaElement;
    }

    @Test
    public void testApplicationRestartOnDefaultJTADatasourceChange() throws Exception {
        String testName = "testApplicationRestartOnDefaultJTADatasourceChange";

        try {
            server_JTA.startServer();
            server_JTA.waitForStringInLogUsingMark("CWWKZ0001I: .*jdbcwebapp");

            // Set up database tables and rows
            final String response = HttpUtils.getHttpResponseAsString(server_JTA, "jdbcwebapp/JDBCServlet");
            System.out.println(response);
            Assert.assertNotNull(response);
            Assert.assertTrue(response.contains("CREATE TABLES GOOD."));

            final WebArchive jpaApp = ShrinkWrap.create(WebArchive.class, "jpawebapp.war");
            jpaApp.addPackage("jpadds.entity");
            jpaApp.addClass(jpadds.web.jpa.JPAServlet.class);
            ShrinkHelper.addDirectory(jpaApp, "test-applications/jpadefaultdatasource/resources/JTA");
            ShrinkHelper.exportDropinAppToServer(server_JTA, jpaApp);
            server_JTA.waitForStringInLogUsingMark("CWWKZ0001I: .*jpawebapp");

            // Check that the default datasource points to database #1
            String jpa_response1 = HttpUtils.getHttpResponseAsString(server_JTA, "jpawebapp/JPAServlet?targetId=1&testName=" + testName);
            System.out.println(jpa_response1);
            Assert.assertNotNull(jpa_response1);
            Assert.assertTrue(jpa_response1.contains("TEST GOOD."));

            // Switch default datasource to point to database #2

            ServerConfiguration sc = server_JTA.getServerConfiguration();
            JPA jpaElement = getJPAConfigElement(sc);
            jpaElement.setDefaultJtaDataSourceJndiName("jdbc/JTA_DS2");

            server_JTA.updateServerConfiguration(sc);
            server_JTA.saveServerConfiguration();

            // The config chang should cause jpawebapp to restart.
            server_JTA.waitForStringInLogUsingMark("CWWKG0017I:");
            server_JTA.waitForStringInLogUsingMark("CWWKZ0003I: .*jpawebapp");

            //  Check that the default datasource points to database #2
            String jpa_response2 = HttpUtils.getHttpResponseAsString(server_JTA, "jpawebapp/JPAServlet?targetId=2&testName=" + testName);
            System.out.println(jpa_response2);
            Assert.assertNotNull(jpa_response2);
            Assert.assertTrue(jpa_response2.contains("TEST GOOD."));
        } finally {
            if (server_JTA.isStarted()) {
                server_JTA.stopServer(null);
                server_JTA.waitForStringInLogUsingMark("CWWKE0036I: .*JPADefaultDataSourceServer");
            }
        }

    }

    @Test
    public void testApplicationRestartOnDefaultNJTADatasourceChange() throws Exception {
        String testName = "testApplicationRestartOnDefaultNJTADatasourceChange";

        try {
            server_NJTA.startServer();
            server_NJTA.waitForStringInLogUsingMark("CWWKZ0001I: .*jdbcwebapp");

            // Set up database tables and rows
            final String response = HttpUtils.getHttpResponseAsString(server_NJTA, "jdbcwebapp/JDBCServlet");
            System.out.println(response);
            Assert.assertNotNull(response);
            Assert.assertTrue(response.contains("CREATE TABLES GOOD."));

            final WebArchive jpaApp = ShrinkWrap.create(WebArchive.class, "jpawebapp.war");
            jpaApp.addPackage("jpadds.entity");
            jpaApp.addClass(jpadds.web.jpa.JPARLServlet.class);
            ShrinkHelper.addDirectory(jpaApp, "test-applications/jpadefaultdatasource/resources/NJTA");
            ShrinkHelper.exportDropinAppToServer(server_NJTA, jpaApp);
            server_NJTA.waitForStringInLogUsingMark("CWWKZ0001I: .*jpawebapp");

            // Check that the default datasource points to database #1
            String jparl_response1 = HttpUtils.getHttpResponseAsString(server_NJTA, "jpawebapp/JPARLServlet?targetId=1&testName=" + testName);
            System.out.println(jparl_response1);
            Assert.assertNotNull(jparl_response1);
            Assert.assertTrue(jparl_response1.contains("TEST GOOD."));

            // Switch default datasource to point to database #2

            ServerConfiguration sc = server_NJTA.getServerConfiguration();
            JPA jpaElement = getJPAConfigElement(sc);
            jpaElement.setDefaultNonJtaDataSourceJndiName("jdbc/NJTA_DS2");

            server_NJTA.updateServerConfiguration(sc);
            server_NJTA.saveServerConfiguration();

            // The config chang should cause jpawebapp to restart.
            server_NJTA.waitForStringInLogUsingMark("CWWKG0017I:");
            server_NJTA.waitForStringInLogUsingMark("CWWKZ0003I: .*jpawebapp");

            //  Check that the default datasource points to database #2
            String jparl_response2 = HttpUtils.getHttpResponseAsString(server_NJTA, "jpawebapp/JPARLServlet?targetId=2&testName=" + testName);
            System.out.println(jparl_response2);
            Assert.assertNotNull(jparl_response2);
            Assert.assertTrue(jparl_response2.contains("TEST GOOD."));
        } finally {
            if (server_NJTA.isStarted()) {
                server_NJTA.stopServer(null);
                server_NJTA.waitForStringInLogUsingMark("CWWKE0036I: .*JPADefaultDataSourceServer");
            }
        }
    }

    @Test
    public void testApplicationRestartOnDefaultJTAAndNJTADatasourceChange() throws Exception {
        String testName = "testApplicationRestartOnDefaultJTAAndNJTADatasourceChange";

        try {
            server_JTA_NJTA.startServer();
            server_JTA_NJTA.waitForStringInLogUsingMark("CWWKZ0001I: .*jdbcwebapp");

            // Set up database tables and rows
            final String response = HttpUtils.getHttpResponseAsString(server_JTA_NJTA, "jdbcwebapp/JDBCServlet");
            System.out.println(response);
            Assert.assertNotNull(response);
            Assert.assertTrue(response.contains("CREATE TABLES GOOD."));

            final WebArchive jpaApp = ShrinkWrap.create(WebArchive.class, "jpawebapp.war");
            jpaApp.addPackage("jpadds.entity");
            jpaApp.addPackage("jpadds.web.jpa");
            ShrinkHelper.addDirectory(jpaApp, "test-applications/jpadefaultdatasource/resources/JTANJTA");
            ShrinkHelper.exportDropinAppToServer(server_JTA_NJTA, jpaApp);
            server_JTA_NJTA.waitForStringInLogUsingMark("CWWKZ0001I: .*jpawebapp");

            // Check that the default datasource points to database #1
            String jpa_response1 = HttpUtils.getHttpResponseAsString(server_JTA_NJTA, "jpawebapp/JPAServlet?targetId=1&testName=" + testName);
            System.out.println(jpa_response1);
            Assert.assertNotNull(jpa_response1);
            Assert.assertTrue(jpa_response1.contains("TEST GOOD."));

            String jparl_response1 = HttpUtils.getHttpResponseAsString(server_JTA_NJTA, "jpawebapp/JPARLServlet?targetId=1&testName=" + testName);
            System.out.println(jparl_response1);
            Assert.assertNotNull(jparl_response1);
            Assert.assertTrue(jparl_response1.contains("TEST GOOD."));

            // Switch default datasource to point to database #2

            ServerConfiguration sc = server_JTA_NJTA.getServerConfiguration();
            JPA jpaElement = getJPAConfigElement(sc);
            jpaElement.setDefaultJtaDataSourceJndiName("jdbc/JTA_DS2");
            jpaElement.setDefaultNonJtaDataSourceJndiName("jdbc/NJTA_DS2");

            server_JTA_NJTA.updateServerConfiguration(sc);
            server_JTA_NJTA.saveServerConfiguration();

            // The config chang should cause jpawebapp to restart.
            server_JTA_NJTA.waitForStringInLogUsingMark("CWWKG0017I:");
            server_JTA_NJTA.waitForStringInLogUsingMark("CWWKZ0003I: .*jpawebapp");

            //  Check that the default datasource points to database #2
            String jpa_response2 = HttpUtils.getHttpResponseAsString(server_JTA_NJTA, "jpawebapp/JPAServlet?targetId=2&testName=" + testName);
            System.out.println(jpa_response2);
            Assert.assertNotNull(jpa_response2);
            Assert.assertTrue(jpa_response2.contains("TEST GOOD."));

            String jparl_response2 = HttpUtils.getHttpResponseAsString(server_JTA_NJTA, "jpawebapp/JPARLServlet?targetId=2&testName=" + testName);
            System.out.println(jparl_response2);
            Assert.assertNotNull(jparl_response2);
            Assert.assertTrue(jparl_response2.contains("TEST GOOD."));
        } finally {
            if (server_JTA_NJTA.isStarted()) {
                server_JTA_NJTA.stopServer("CWWJP9991W:");
                server_JTA_NJTA.waitForStringInLogUsingMark("CWWKE0036I: .*JPADefaultDataSourceServer");
            }
        }
    }
}
