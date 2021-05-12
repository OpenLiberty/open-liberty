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

package jpahelper.databasemanagement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64.Encoder;
import java.util.Properties;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

/**
 *
 */
@WebServlet(urlPatterns = { "/DMS" })
public class DatabaseManagementServlet extends HttpServlet {
    @Resource
    private UserTransaction tx;

    @Resource(lookup = "jdbc/JPA_DS")
    private DataSource dsJta;

    @Resource(lookup = "jdbc/JPA_NJTADS")
    private DataSource dsRl;

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execRequest(req, resp);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execRequest(req, resp);
    }

    private void execRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String command = req.getParameter("command");

        if ("EXECDDL".equals(command)) {
            executeDDL(req, resp);
        } else if ("GETINFO".equals(command)) {
            getInfo(req, resp);
        } else {
            resp.sendError(400);
        }
    }

    private void getInfo(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Connection conn = null;
        try {
            System.out.println("DatabaseManagement servicing getInfo request...");
            conn = dsRl.getConnection();
            final DatabaseMetaData dbMeta = conn.getMetaData();
            Properties properties = new Properties();
            properties.put("dbproduct_name", dbMeta.getDatabaseProductName());
            properties.put("dbproduct_version", dbMeta.getDatabaseProductVersion());
            properties.put("jdbcdriver_version", dbMeta.getDatabaseProductVersion());
            properties.put("jdbc_url", dbMeta.getURL());
            properties.put("jdbc_username", dbMeta.getUserName());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(properties);
            oos.flush();
            oos.close();

            final Encoder base64Encoder = java.util.Base64.getEncoder();
            final String base64EncodedData = base64Encoder.encodeToString(baos.toByteArray());

            final PrintWriter pw = resp.getWriter();
            pw.println(base64EncodedData);
        } catch (SQLException e) {
            throw new ServletException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable t) {
                }
            }
            System.out.println("DatabaseManagement getInfo request service complete.");
        }

    }

    private void executeDDL(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String ddlScriptName = req.getParameter("ddl.script.name");
        final boolean swallowErrors = Boolean.valueOf(req.getParameter("swallow.errors"));
        final String overrideDefaultSchema = req.getParameter("override.default.schema");
        final ClassLoader cl = DatabaseManagementServlet.class.getClassLoader();
        final URL ddlScriptURL = cl.getResource(ddlScriptName);

        if (ddlScriptURL == null) {
            throw new ServletException("Unable to locate resource " + ddlScriptName);
        }

        StringBuilder sbHeader = new StringBuilder();
        sbHeader.append("\n");
        sbHeader.append("Database Management Servlet DDL Execution:\n");
        sbHeader.append("ddlScriptName = " + ddlScriptName + "\n");
        sbHeader.append("swallowErrors = " + swallowErrors + "\n");
        sbHeader.append("overrideDefaultSchema = " + overrideDefaultSchema + "\n");
        System.out.println(sbHeader);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final InputStream is = ddlScriptURL.openStream();
        final byte[] buffer = new byte[1024];
        int bytesRead = 0;

        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();

        final String ddl = baos.toString();
        final String[] commands = ddl.split(";");

        final PrintWriter pw = resp.getWriter();
        try {
            tx.begin();
            final Connection conn = dsJta.getConnection();
            final DatabaseMetaData dbMeta = conn.getMetaData();
            dumpDBMeta(dbMeta);

            final Statement stmt = conn.createStatement();
            final String defaultSchemaName = (overrideDefaultSchema == null || "".equals(overrideDefaultSchema.trim())) ? //
                            sanitize(dbMeta.getUserName()) : // Usually the default schema name
                            sanitize(overrideDefaultSchema); // But always allow for test client to override

            int totalCount = 0, successCount = 0;
            for (String command : commands) {
                final String sql = command.replace("${schemaname}", defaultSchemaName).trim();
                if ("".equals(sql)) {
                    continue;
                }

                totalCount++;
                try {
                    System.out.println("Executing: " + sql);
                    pw.println("Executing: " + sql);
                    if (stmt.execute(sql)) {
                        pw.println("Successful execution, Result Set:");

                        final ResultSet rs = stmt.getResultSet();
                        final ResultSetMetaData rsmd = rs.getMetaData();
                        final int colCount = rsmd.getColumnCount();
                        int index = 0;
                        while (rs.next()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(++index).append(": ");

                            for (int col = 1; col <= colCount; col++) {
                                if (col != 1) {
                                    sb.append(", ");
                                }
                                sb.append(rs.getObject(col));
                            }

                            pw.println(sb);
                        }
                    } else {
                        pw.println("Successful execution, update count = " + stmt.getUpdateCount());
                    }

                    successCount++;
                } catch (Exception e) {
                    if (!swallowErrors) {
                        pw.println("SQL Execution failed: " + e);
                    }
                }
            }

            System.out.println("SQL Executed: Total = " + totalCount + " Successful = " + successCount);

            tx.commit();
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            pw.close();
        }
    }

    /**
     * Sanitize input to thwart sql injection attacks.
     *
     * @param in
     * @return
     */
    private String sanitize(String in) {
        if (in == null) {
            return null;
        }

        return in.replaceAll("[^A-Za-z0-9_]", "");
    }

    private void dumpDBMeta(DatabaseMetaData dbMeta) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("################################################################################\n");
        sb.append("DBMeta Dump:\n");
        sb.append("DB Product Name: ").append(dbMeta.getDatabaseProductName()).append("\n");
        sb.append("DB Product Version: ").append(dbMeta.getDatabaseProductVersion()).append("\n");
        sb.append("JDBC Driver Version: ").append(dbMeta.getDriverVersion()).append("\n");
        sb.append("DB URL: ").append(dbMeta.getURL()).append("\n");
        sb.append("DB Username: ").append(dbMeta.getUserName()).append("\n");

        sb.append("DB Schemas:\n");
        try {
            ResultSet schemas = dbMeta.getSchemas();
            while (schemas.next()) {
                sb.append("  ").append(schemas.getString("TABLE_SCHEM")).append("\n");
            }
        } catch (Throwable t) {
            // Ouch
            sb.append("   DB SCHEMAS NOT AVAILABLE -- " + t.getMessage());
            t.printStackTrace();
        }

        sb.append("################################################################################\n");
        System.out.println(sb);
    }

}
