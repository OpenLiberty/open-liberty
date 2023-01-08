/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.testtooling.vehicle.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.Properties;

import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;

public abstract class JPADBTestServlet extends JPATestServlet {
    private static final long serialVersionUID = 1542586929371075141L;

    private static boolean dbMetaAcquired = false;
    private static String dbMajorVersion = "";
    private static String dbMinorVersion = "";
    private static String dbProductName = "";
    private static String dbProductVersion = "";
    private static String jdbcDriverVersion = "";
    private static String jdbcURL = "";
    private static String jdbcUsername = "";

    @Override
    protected void executeTestVehicle(TestExecutionContext ctx) {
        setDatabaseInformationOnContext(ctx);
        super.executeTestVehicle(ctx);
    }

    protected void setDatabaseInformationOnContext(TestExecutionContext ctx) {
        try {
            HashMap<String, java.io.Serializable> properties = ctx.getProperties();
            properties.put("dbMajorVersion", getDbMajorVersion());
            properties.put("dbMinorVersion", getDbMinorVersion());
            properties.put("dbProductName", getDbProductName());
            properties.put("dbProductVersion", getDbProductVersion());
            properties.put("jdbcDriverVersion", getJdbcDriverVersion());
        } catch (Throwable t) {
            logException(t, ctx);
        }
    }

    protected static synchronized void fetchDatabaseMetadata() throws Exception {
        if (dbMetaAcquired) {
            return;
        }

        final StringBuilder sb = new StringBuilder();

        final URL dmURL = new URL("http://localhost:" + portNumber + "/DatabaseManagement/DMS?command=GETINFO");
        final HttpURLConnection conn = (HttpURLConnection) dmURL.openConnection();
        conn.setRequestMethod("GET");
        System.out.println("JPATestServlet sending request for database information...");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                sb.append(buffer);
            }
        }

        System.out.println("JPATestServlet Reading encoded database information.");

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

        dbMajorVersion = dbProps.getProperty("dbmajor_version", "UNKNOWN");
        dbMinorVersion = dbProps.getProperty("dbminor_version", "UNKNOWN");
        dbProductName = dbProps.getProperty("dbproduct_name", "UNKNOWN");
        dbProductVersion = dbProps.getProperty("dbproduct_version", "UNKNOWN");
        jdbcDriverVersion = dbProps.getProperty("jdbcdriver_version", "UNKNOWN");
        jdbcURL = dbProps.getProperty("jdbc_url", "UNKNOWN");
        jdbcUsername = dbProps.getProperty("jdbc_username", "UNKNOWN");

        dbMetaAcquired = true;
    }

    protected static void executeDDL(String scriptName) throws Exception {
        fetchDatabaseMetadata();

        DatabaseVendor vendor = DatabaseVendor.resolveDBProduct(dbProductName, dbProductVersion);
        scriptName = scriptName.replace("${dbvendor}", vendor.toString());

        System.out.println("*** start execution: " + scriptName + " ***");

        final StringBuilder sb = new StringBuilder();

        final URL dmURL = new URL("http://localhost:" + portNumber + "/DatabaseManagement/DMS?command=EXECDDL&ddl.script.name="
                                  + scriptName + "&swallow.errors=true");
        final HttpURLConnection conn = (HttpURLConnection) dmURL.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                sb.append(buffer);
            }
        }

//        System.out.println("DBMeta DDL Exec Result: ");
//        System.out.println(sb);

        System.out.println("*** end execution ***");
    }

    /**
     * @return the dbMajorVersion
     */
    public static String getDbMajorVersion() throws Exception {
        fetchDatabaseMetadata();
        return dbMajorVersion;
    }

    /**
     * @return the dbMinorVersion
     */
    public static String getDbMinorVersion() throws Exception {
        fetchDatabaseMetadata();
        return dbMinorVersion;
    }

    /**
     * @return the dbProductName
     */
    public static String getDbProductName() throws Exception {
        fetchDatabaseMetadata();
        return dbProductName;
    }

    /**
     * @return the dbProductVersion
     */
    public static String getDbProductVersion() throws Exception {
        fetchDatabaseMetadata();
        return dbProductVersion;
    }

    /**
     * @return the jdbcDriverVersion
     */
    public static String getJdbcDriverVersion() throws Exception {
        fetchDatabaseMetadata();
        return jdbcDriverVersion;
    }

    public static boolean isDB2ForZOS() throws Exception {
        String prodName = getDbProductName();
        String prodVers = getDbProductVersion();
        return DatabaseVendor.checkDBProductName(prodName, prodVers, DatabaseVendor.DB2ZOS);
    }

    public static boolean isDB2ForLUW() throws Exception {
        String prodName = getDbProductName();
        String prodVers = getDbProductVersion();
        return DatabaseVendor.checkDBProductName(prodName, prodVers, DatabaseVendor.DB2LUW);
    }

    public static boolean isDB2ForISeries() throws Exception {
        String prodName = getDbProductName();
        String prodVers = getDbProductVersion();
        return DatabaseVendor.checkDBProductName(prodName, prodVers, DatabaseVendor.DB2I);
    }

    public static boolean isDB2ForVM_VSE() throws Exception {
        String prodName = getDbProductName();
        String prodVers = getDbProductVersion();
        return DatabaseVendor.checkDBProductName(prodName, prodVers, DatabaseVendor.DB2VMVSE);
    }

    public static boolean isDerby() throws Exception {
        String prodName = getDbProductName();
        String prodVers = getDbProductVersion();
        return DatabaseVendor.checkDBProductName(prodName, prodVers, DatabaseVendor.DERBY);
    }
}
