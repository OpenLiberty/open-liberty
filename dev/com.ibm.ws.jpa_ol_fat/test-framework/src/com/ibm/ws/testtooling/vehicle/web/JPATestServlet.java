/**
 *
 */
package com.ibm.ws.testtooling.vehicle.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.JEEExecutionContextHelper;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

import componenttest.app.FATServlet;

public abstract class JPATestServlet extends FATServlet {
    private static final long serialVersionUID = -4038309130483462162L;

    private static int portNumber = 0;

    @Resource
    protected UserTransaction tx;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        portNumber = request.getLocalPort();

        super.doGet(request, response);
    }

    protected void executeTestVehicle(TestExecutionContext ctx) {
        TestExecutionResources testExecResources = null;

        try {
            JEEExecutionContextHelper.printBeginTestInfo(ctx);

            // Create resources needed by the test
            testExecResources = JEEExecutionContextHelper.processTestExecutionResources(ctx, this, tx);

            // Execute the test
            JEEExecutionContextHelper.executeTestLogic(ctx, testExecResources, this);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            Assert.fail("TestServlet Caught Exception: " + t);
        } finally {
            // Cleanup Resources
            try {
                JEEExecutionContextHelper.destroyExecutionResources(testExecResources);
            } catch (Throwable t) {
                Assert.fail("TestServlet Cleanup Caught Exception: " + t);
            }

            JEEExecutionContextHelper.printEndTestInfo(ctx);
        }
    }

    private static boolean dbMetaAcquired = false;
    private static String dbProductName = "";
    private static String dbProductVersion = "";
    private static String jdbcDriverVersion = "";
    private static String jdbcURL = "";
    private static String jdbcUsername = "";

    protected static synchronized void fetchDatabaseMetadata() throws Exception {
        if (dbMetaAcquired) {
            return;
        }

        final StringBuilder sb = new StringBuilder();

        final URL dmURL = new URL("http://localhost:" + portNumber + "/DatabaseManagement/DMS?command=GETINFO");
        final HttpURLConnection conn = (HttpURLConnection) dmURL.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                sb.append(buffer);
            }
        }

        System.out.println("DBMeta Request Result: ");
        System.out.println(sb);

        Properties dbProps = new Properties();
        dbProps.load(new ByteArrayInputStream(sb.toString().getBytes()));
        System.out.println("Acquired Database Metadata: " + dbProps);

        dbProductName = dbProps.getProperty("dbproduct_name");
        dbProductVersion = dbProps.getProperty("dbproduct_version");
        jdbcDriverVersion = dbProps.getProperty("jdbcdriver_version");
        jdbcURL = dbProps.getProperty("jdbc_url");
        jdbcUsername = dbProps.getProperty("jdbc_username");

        dbMetaAcquired = true;
    }

    protected static void executeDDL(String scriptName) throws Exception {
        fetchDatabaseMetadata();

        String productName = "";
        if (dbProductName.toLowerCase().contains("derby")) {
            productName = "DERBY";
        } else if (dbProductName.toLowerCase().contains("db2")) {
            productName = "DB2";
        } else if (dbProductName.toLowerCase().contains("informix")) {
            productName = "INFORMIX";
        } else if (dbProductName.toLowerCase().contains("hsql")) {
            productName = "HSQL";
        } else if (dbProductName.toLowerCase().contains("mysql")) {
            productName = "MYSQL";
        } else if (dbProductName.toLowerCase().contains("oracle")) {
            productName = "ORACLE";
        } else if (dbProductName.toLowerCase().contains("postgres")) {
            productName = "POSTGRES";
        } else if (dbProductName.toLowerCase().contains("sqlserver")) {
            productName = "SQLSERVER";
        } else if (dbProductName.toLowerCase().contains("sybase")) {
            productName = "SYBASE";
        }

        scriptName = scriptName.replace("${dbvendor}", productName);

        System.out.println("*****");

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

        System.out.println("DBMeta DDL Exec Result: ");
        System.out.println(sb);

        System.out.println("*****");
    }
}
