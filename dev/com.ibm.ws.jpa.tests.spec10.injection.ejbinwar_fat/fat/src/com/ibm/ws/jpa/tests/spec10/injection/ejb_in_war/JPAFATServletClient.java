/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.spec10.injection.ejb_in_war;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.util.Base64.Decoder;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.testtooling.database.DatabaseVendor;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
public class JPAFATServletClient extends FATServletClient {
    private static final String dbManagementResourcePath = "test-applications/helpers/DatabaseManagement/resources/";

    private static boolean dbMetaAcquired = false;
    private static String dbProductName = "";
    private static String dbProductVersion = "";
    private static String jdbcDriverVersion = "";
    private static String jdbcURL = "";
    private static String jdbcUsername = "";

    private static DatabaseVendor dbVendor = null;

    protected static void bannerStart(Class testClass) {
        StringBuilder sb = new StringBuilder();
        sb.append("**********************************************************************\n");
        sb.append("Executing Test Bucket: ").append(testClass.getName()).append("\n");
        sb.append("**********************************************************************");
        System.out.println(sb);
    }

    protected static void bannerEnd(Class testClass, long timestart) {
        long timestop = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        sb.append("**********************************************************************\n");
        sb.append("Completed Test Bucket: ").append(testClass.getName());
        sb.append(" (total test execution time in ms: ").append(timestop - timestart).append(")");
        sb.append("\n");
        sb.append("**********************************************************************");
        System.out.println(sb);
    }

    protected static ProgramOutput setupDatabaseApplication(LibertyServer server, String ddlPath) throws Exception {
        ProgramOutput progOut = null;
        if (!server.isStarted()) {
            server.startServer();
        }

        System.out.println("Installing DatabaseManagement.war ...");
        final WebArchive webApp = ShrinkWrap.create(WebArchive.class, "DatabaseManagement.war");
        webApp.addPackages(true, "jpahelper.databasemanagement");
        ShrinkHelper.addDirectory(webApp, dbManagementResourcePath + "/databasemanagement.war");
        ShrinkHelper.addDirectory(webApp, ddlPath);

        ShrinkHelper.exportToServer(server, "apps", webApp, DeployOptions.OVERWRITE);

        Application appRecord = new Application();
        appRecord.setLocation("DatabaseManagement.war");
        appRecord.setName("DatabaseManagement");

        server.setMarkToEndOfLog();
        ServerConfiguration sc = server.getServerConfiguration();
        sc.getApplications().add(appRecord);
        server.updateServerConfiguration(sc);
        server.saveServerConfiguration();

        HashSet<String> appNamesSet = new HashSet<String>();
        appNamesSet.add("DatabaseManagement");
        server.waitForConfigUpdateInLogUsingMark(appNamesSet, "");

        System.out.println("Successfully installed DatabaseManagement.war.");

        fetchDatabaseMetadata(server);

        return progOut;
    }

    private static void fetchDatabaseMetadata(LibertyServer server) throws Exception {
        if (dbMetaAcquired) {
            return;
        }

        final String pathAndQuery = "/DatabaseManagement/DMS?command=GETINFO";
        final HttpURLConnection con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, pathAndQuery);
        final int rc = con.getResponseCode();
        if (rc == 200) {
            System.out.println("Reading encoded database information.");
            final StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String buffer;
                while ((buffer = reader.readLine()) != null) {
                    sb.append(buffer);
                }
            }

            final String base64EncodedData = sb.toString();
            final Decoder base64Decoder = java.util.Base64.getDecoder();
            final byte[] objectData = base64Decoder.decode(base64EncodedData);

            ByteArrayInputStream bais = new ByteArrayInputStream(objectData);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Properties dbProps = (Properties) ois.readObject();

            System.out.println("Acquired Database Metadata: " + dbProps);
            for (Object key : dbProps.keySet()) {
                System.out.println("   " + key + " = " + dbProps.getProperty((String) key));
            }

            dbProductName = dbProps.getProperty("dbproduct_name", "UNKNOWN");
            dbProductVersion = dbProps.getProperty("dbproduct_version", "UNKNOWN");
            jdbcDriverVersion = dbProps.getProperty("jdbcdriver_version", "UNKNOWN");
            jdbcURL = dbProps.getProperty("jdbc_url", "UNKNOWN");
            jdbcUsername = dbProps.getProperty("jdbc_username", "UNKNOWN");

            dbVendor = DatabaseVendor.resolveDBProduct(dbProductName);
        } else {
            System.out.println("Failed to acquire Database Metadata.");
        }

        con.disconnect();
        dbMetaAcquired = true;
    }

    protected static void executeDDL(LibertyServer server, Set<String> ddlScriptNames, boolean ignoreErrors) throws Exception {
        System.out.println("*****");
        for (String script : ddlScriptNames) {
            String pathAndQuery = "/DatabaseManagement/DMS?command=EXECDDL&ddl.script.name=" + script + "&swallow.errors=" + ignoreErrors;
            HttpURLConnection con = HttpUtils.getHttpConnectionWithAnyResponseCode(server, pathAndQuery);
            int rc = con.getResponseCode();
            if (rc == 200) {
                System.out.println("Results of executing " + script + " DDL script:");

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inLine = null;
                while ((inLine = br.readLine()) != null) {
                    System.out.println(inLine);
                }
                br.close();
                System.out.println("*****");
            }
            con.disconnect();
        }
    }

    protected boolean isDbMetaAcquired() {
        return dbMetaAcquired;
    }

    protected String getDbProductName() {
        return dbProductName;
    }

    protected String getDbProductVersion() {
        return dbProductVersion;
    }

    protected String getJdbcDriverVersion() {
        return jdbcDriverVersion;
    }

    protected String getJdbcURL() {
        return jdbcURL;
    }

    protected String getJdbcUsername() {
        return jdbcUsername;
    }

    protected static DatabaseVendor getDbVendor() {
        return dbVendor;
    }

    protected static final JavaArchive buildTestAPIJar() throws Exception {
        final JavaArchive testApiJar = ShrinkWrap.create(JavaArchive.class, "TestAPI.jar");
        testApiJar.addPackage("com.ibm.ws.testtooling.database");
        testApiJar.addPackage("com.ibm.ws.testtooling.msgcli");
        testApiJar.addPackage("com.ibm.ws.testtooling.msgcli.jms");
        testApiJar.addPackage("com.ibm.ws.testtooling.msgcli.msc");
        testApiJar.addPackage("com.ibm.ws.testtooling.testinfo");
        testApiJar.addPackage("com.ibm.ws.testtooling.testlogic");
        testApiJar.addPackage("com.ibm.ws.testtooling.tranjacket");
        testApiJar.addPackage("com.ibm.ws.testtooling.vehicle");
        testApiJar.addPackage("com.ibm.ws.testtooling.vehicle.ejb");
        testApiJar.addPackage("com.ibm.ws.testtooling.vehicle.execontext");
        testApiJar.addPackage("com.ibm.ws.testtooling.vehicle.resources");
        testApiJar.addPackage("com.ibm.ws.testtooling.vehicle.web");
        return testApiJar;
    }
}
