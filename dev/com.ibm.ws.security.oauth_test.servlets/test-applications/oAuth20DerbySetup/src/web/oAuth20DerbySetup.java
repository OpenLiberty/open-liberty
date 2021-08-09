/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

@WebServlet(name = "oAuth20DerbySetup", urlPatterns = { "/oAuth20DerbySetup" })
public class oAuth20DerbySetup extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String DB_USER = "dbuser";
    private static final String DB_PWD = "dbpwd";
    @Resource(name = "jdbc/OAuth2DB")
    DataSource OAuthFvtDataSource;
    private String schemaName = "OAuthDBSchema";

    // these need to match the strings used in DerbyUtils
    final static String PORT = "port";
    final static String DROP_TABLE = "cleanup";
    final static String DROP_DB = "dropDB";
    final static String ADD_CLIENT = "addClient";
    final static String CLIENT_ID_WEB = "clientID";
    final static String SECRET_WEB = "secret";
    final static String CHECK_SECRET = "checkSecret";
    final static String COMP_ID = "compID";
    final static String SALT = "checkSalt";
    final static String ALGORITHM = "checkAlgorithm";
    final static String ITERATION = "checkIteration";
    final static String ACCESS_TOKEN = "checkAccessToken";
    final static String CLEAR_CLIENTS = "clearClients";

    final static String DEFAULT_COMPID = "OAuthConfigDerby";

    public static final String HASH_SALT = "salt"; // must match constants in OidcBaseClient
    public static final String HASH_ALGORITHM = "hash_alg"; // must match constants in OidcBaseClient
    public static final String HASH_ITERATIONS = "hash_itr"; // must match constants in OidcBaseClient
    public static final String HASH_LENGTH = "hash_len"; // must match constants in OidcBaseClient

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        try {
            System.out.println("Derby setup util parms: " + Arrays.toString(request.getParameterMap().keySet().toArray()));
            String port = request.getParameter("port");
            System.out.println("We are in the servlet. Port is: " + port);
            String dropTable = request.getParameter("cleanup");
            schemaName = request.getParameter("schemaName");
            if (schemaName == null || schemaName.trim().equals("")) {
                schemaName = "OAuthDBSchema";
            }
            String clearClients = request.getParameter(CLEAR_CLIENTS);
            if (clearClients != null) {
                clearAllClients(OAuthFvtDataSource);
            }

            String addClient = request.getParameter(ADD_CLIENT);
            String checkSecret = request.getParameter(CHECK_SECRET);
            String getSalt = request.getParameter(SALT);
            String getAlgorithm = request.getParameter(ALGORITHM);
            String getIteration = request.getParameter(ITERATION);
            String checkAccessToken_plain = request.getParameter(ACCESS_TOKEN + "_plain");
            String checkAccessToken_hashed = request.getParameter(ACCESS_TOKEN + "_hashed");
            String checkAccessToken = setCheckAccessToken(checkAccessToken_plain, checkAccessToken_hashed);

            if (Boolean.TRUE.toString().equals(addClient)) { // add a new client to the database
                String clientID = request.getParameter(CLIENT_ID_WEB);
                System.out.println("Request to add a new client " + clientID);
                String secret = request.getParameter(SECRET_WEB);
                String compID = request.getParameter(COMP_ID);
                addCustomEntry(OAuthFvtDataSource, clientID, secret, port, compID, getSalt, getAlgorithm);
            } else if (getAlgorithm != null) {
                String clientID = request.getParameter(CLIENT_ID_WEB);
                String compID = request.getParameter(COMP_ID);
                System.out.println("Request to get algorithm for client " + clientID + " compId " + compID);
                String type = getAlgorithm(OAuthFvtDataSource, clientID, compID);
                System.out.println("Algorithm is " + type);

                PrintWriter writer = response.getWriter();
                writer.write(type);
                writer.flush();
                writer.close();
            } else if (getIteration != null) {
                String clientID = request.getParameter(CLIENT_ID_WEB);
                String compID = request.getParameter(COMP_ID);
                System.out.println("Request to get iteration for client " + clientID + " compId " + compID);
                String type = getIteration(OAuthFvtDataSource, clientID, compID);
                System.out.println("Iteration is " + type);

                PrintWriter writer = response.getWriter();
                writer.write(type);
                writer.flush();
                writer.close();
            } else if (getSalt != null) {
                String clientID = request.getParameter(CLIENT_ID_WEB);
                String compID = request.getParameter(COMP_ID);
                System.out.println("Request to get salt for client " + clientID + " compId " + compID);
                String type = getSalt(OAuthFvtDataSource, clientID, compID);
                System.out.println("Salt is" + type);

                PrintWriter writer = response.getWriter();
                writer.write(type);
                writer.flush();
                writer.close();
            } else if (checkSecret != null) { // check what kind of encoding the secret has for a client
                String clientID = request.getParameter(CLIENT_ID_WEB);
                String compID = request.getParameter(COMP_ID);
                System.out.println("Request to check client " + clientID + " compId " + compID);
                String type = getSecretType(OAuthFvtDataSource, clientID, compID);
                System.out.println("secret is of type " + type);

                PrintWriter writer = response.getWriter();
                writer.write(type);
                writer.flush();
                writer.close();
            } else if (checkAccessToken != null) { // check what kind of encoding used for the access_token
                System.out.println("Request to check accessToken ");
                String accessToken = request.getParameter(CLIENT_ID_WEB); // reuse client_id parm so we don't have to change interface
                String encodingType = getAccessTokenType(OAuthFvtDataSource, accessToken, checkAccessToken);
                System.out.println("accessTokens should be: " + encodingType);

                PrintWriter writer = response.getWriter();
                writer.write(encodingType);
                writer.flush();
                writer.close();
            } else if ("true".equals(dropTable)) {
                dropClientConfigTable(OAuthFvtDataSource);
            } else {
                String redirectUri = "http://localhost:" + port + "/oauthclient/redirect.jsp";
                String redirectUri2 = "http://localhost:" + port + "/oauthclient/authorize_redirect.jsp";
                createClientConfigTable(OAuthFvtDataSource);
                createCacheTable(OAuthFvtDataSource);
                addEntry(OAuthFvtDataSource, DEFAULT_COMPID, "dclient01",
                         "secret", "dclient01", redirectUri, 1,
                         buildClientMetaData("dclient01", redirectUri, "true"));
                addEntry(OAuthFvtDataSource, DEFAULT_COMPID, "dclient02",
                         "secret", "dclient02", redirectUri, 1,
                         buildClientMetaData("dclient02", redirectUri, "false"));
                addEntry(OAuthFvtDataSource, "OAuthConfigDerby2", "dclient01",
                         "secret", "dclient01", redirectUri, 1,
                         buildClientMetaData("dclient01", redirectUri, "true"));
                addEntry(OAuthFvtDataSource, "OAuthConfigDerby2", "dclient02",
                         "secret", "dclient02", redirectUri, 1,
                         buildClientMetaData("dclient02", redirectUri, "false"));

                addEntry(OAuthFvtDataSource, DEFAULT_COMPID, "dclient03",
                         "secret", "dclient03", redirectUri, 1,
                         buildClientMetaData("dclient03", redirectUri, "true"));
                addEntry(OAuthFvtDataSource, DEFAULT_COMPID, "dclient04",
                         "secret", "dclient04", redirectUri, 1,
                         buildClientMetaData("dclient03", redirectUri, "true"));

                // client for OAuthGrantTypesDerbyTest
                addEntry(OAuthFvtDataSource, "OAuthConfigSampleGrantTypes", "client03", "{xor}LDo8LTor", "client03", redirectUri2, 1,
                         buildClientMetaData("client03", redirectUri2, "true"));

                queryTable(OAuthFvtDataSource);
            }
        } catch (Throwable x) {
            System.out.println("Someone threw an exception (see messages.log for stack): " + x.getMessage());
            x.printStackTrace();
        }
    }

    private String setCheckAccessToken(String checkAccessToken_plain, String checkAccessToken_hashed) {

        if (checkAccessToken_plain == null) {
            if (checkAccessToken_hashed == null) {
                return null;
            } else {
                return "{hash}";
            }
        } else {
            return "plain";
        }
    }

    /**
     * Create the default table used by the tests.
     *
     * @param ds
     *               the data source
     * @throws SQLException
     *                          if it fails
     */
    private void addEntry(DataSource ds, String compId, String clientID, String secret,
                          String dispName, String redirectUri, int enabled, JSONObject metaData) throws SQLException {
        Connection con = ds.getConnection(DB_USER, DB_PWD);
        try {

            PreparedStatement pstmt = con.prepareStatement("insert into " + schemaName + ".OAUTH20CLIENTCONFIG values (?, ?, ?,?, ?, ?,?)");

            pstmt.setString(1, compId);
            pstmt.setString(2, clientID);
            pstmt.setString(3, secret);
            pstmt.setString(4, dispName);
            pstmt.setString(5, redirectUri);
            pstmt.setInt(6, enabled);
            pstmt.setString(7, metaData.toString());
            //setClob(7, metaData);
            //(7, metaData.serialize().getBytes());
            //setInt(7, metaData)
            pstmt.executeUpdate();
            System.out.println("We finished the update");
        } catch (Throwable x) {
            System.out.println("addEntry unexpected exception" + x.getMessage());
        } finally {
            try {
                con.close();
                System.out.println("We closed the connection");
            } catch (Throwable x) {
                System.out.println("addEntry unexpected exception"
                                   + x.getMessage());
            }
        }
    }

    /**
     * Create the default table used by the tests.
     *
     * @param ds
     *               the data source
     * @throws Exception
     */
    private void queryTable(DataSource ds) throws Exception {
        Connection con = ds.getConnection(DB_USER, DB_PWD);
        System.out.println("queryTable: " + schemaName);
        try {
            Statement stmt = con.createStatement();
            String query = "select * from " + schemaName + ".OAUTH20CLIENTCONFIG";
            ResultSet rs = stmt.executeQuery(query);
            // System.out.println("Id Name    Job");
            while (rs.next()) {

                System.out.println("db entry : " + rs.getString("COMPONENTID")
                                   + " " + rs.getString("CLIENTID") + " "
                                   + rs.getString("CLIENTSECRET") + " "
                                   + rs.getString("DISPLAYNAME") + " "
                                   + rs.getString("REDIRECTURI") + " "
                                   + rs.getInt("Enabled") + " "
                                   + rs.getString(7));
            }
        } catch (Throwable x) {
            System.out.println("queryTable unexpected exception"
                               + x.getMessage());
        } finally {
            System.out.println("queryTable - time to close");
            con.close();
        }

    }

    /**
     * Create the default table used by the tests.
     *
     * @param ds
     *               the data source
     * @throws SQLException
     *                          if it fails
     */
    private void dropClientConfigTable(DataSource ds) throws SQLException {
        System.out.println("Drop Table");
        Connection con = ds.getConnection(DB_USER, DB_PWD);
        try {
            Statement st = con.createStatement();
            try {
                st.executeUpdate("drop table " + schemaName + ".OAUTH20CLIENTCONFIG");
            } catch (SQLException x) {
                System.out.println("dropClientConfigTable 1  unexpected exception"
                                   + x.getMessage());
            }
        } catch (Throwable x) {
            System.out.println("dropClientConfigTable 2 unexpected exception"
                               + x.getMessage());
        } finally {
            con.close();
        }
    }

    /**
     * Create the default table used by the tests.
     *
     * @param ds
     *               the data source
     * @throws SQLException
     *                          if it fails
     */
    private void createClientConfigTable(DataSource ds) throws SQLException {
        System.out.println("Create Table " + schemaName);
        Connection con = ds.getConnection(DB_USER, DB_PWD);
        try {
            Statement st = con.createStatement();
            try {
                st.executeUpdate("drop table " + schemaName + ".OAUTH20CLIENTCONFIG");
            } catch (SQLException x) {
                System.out.println("createClientConfigTable 1 unexpected exception"
                                   + x.getMessage());
            }
            st.executeUpdate("create table "
                             + schemaName
                             + ".OAUTH20CLIENTCONFIG (COMPONENTID VARCHAR(256) NOT NULL, CLIENTID VARCHAR(256) NOT NULL, CLIENTSECRET VARCHAR(256), DISPLAYNAME VARCHAR(256) NOT NULL, REDIRECTURI VARCHAR(2048), ENABLED INT, CLIENTMETADATA CLOB NOT NULL DEFAULT '{}')");
        } catch (Throwable x) {
            System.out.println("createClientConfigTable 2 unexpected exception"
                               + x.getMessage());
        } finally {
            con.close();
        }
    }

    /**
     * Create the default table used by the tests.
     *
     * @param ds
     *               the data source
     * @throws SQLException
     *                          if it fails
     */
    private void createCacheTable(DataSource ds) throws SQLException {
        Connection con = ds.getConnection(DB_USER, DB_PWD);
        try {
            Statement st = con.createStatement();
            try {
                st.executeUpdate("drop table " + schemaName + ".OAUTH20CACHE");
            } catch (SQLException x) {
                System.out.println("createCacheTable 1  unexpected exception"
                                   + x.getMessage());
            }
            st.executeUpdate("CREATE TABLE "
                             + schemaName
                             + ".OAUTH20CACHE ( LOOKUPKEY VARCHAR(256) NOT NULL,UNIQUEID VARCHAR(128) NOT NULL, COMPONENTID VARCHAR(256) NOT NULL, TYPE VARCHAR(64) NOT NULL, SUBTYPE VARCHAR(64), CREATEDAT BIGINT,  LIFETIME INT, EXPIRES BIGINT, TOKENSTRING VARCHAR(2048) NOT NULL, CLIENTID VARCHAR(64) NOT NULL, USERNAME VARCHAR(64) NOT NULL, SCOPE VARCHAR(512) NOT NULL,  REDIRECTURI VARCHAR(2048), STATEID VARCHAR(64) NOT NULL, EXTENDEDFIELDS CLOB NOT NULL DEFAULT '{}')");
        } catch (Throwable x) {
            System.out.println("createCacheTable 2 unexpected exception"
                               + x.getMessage());
        } finally {
            con.close();
        }
    }

    private JSONObject buildClientMetaData(String clientId, String redirectUri, String introspectTokens) throws Exception {

        JSONObject metaD = new JSONObject();
        JSONArray grantTypes = new JSONArray();

        if ("dclient01".equals(clientId) || "dclient02".equals(clientId)) {
            grantTypes.add("authorization_code");
            grantTypes.add("refresh_token");
            grantTypes.add("password");
        } else if ("dclient03".equals(clientId)) {
            grantTypes.add("authorization_code");
            grantTypes.add("password");
        } else if ("client03".equals(clientId)) {
            grantTypes.add("authorization_code");
            grantTypes.add("refresh_token");
        }

        metaD.put("scope", "ALL_SCOPES");

        JSONArray redirectUris = new JSONArray();
        redirectUris.add(redirectUri);
        metaD.put("redirect_uris", redirectUris);

        if (grantTypes.size() > 0) {
            metaD.put("grant_types", grantTypes);
        }

        metaD.put("token_endpoint_auth_method", "client_secret_basic");
        metaD.put("client_id", clientId);
        metaD.put("client_secret", "secret");
        metaD.put("client_name", clientId);
        metaD.put("introspect_tokens", introspectTokens);
        return metaD;
    }

    private String getSecretType(DataSource ds, String clientId, String compID) throws Exception {
        if (clientId == null) {
            System.out.println("oAuth20DerbySetup No clientId provided for getSecretType");
            return "No clientId provided";
        }
        if (compID == null) {
            System.out.println("oAuth20DerbySetup No compID provided for getSecretTypeusing default: OAuthConfigDerby");
            compID = DEFAULT_COMPID;
        }

        Connection con = ds.getConnection(DB_USER, DB_PWD);
        System.out.println("getSecretType for " + clientId + " schema " + schemaName);

        String cs = null;
        try {
            Statement stmt = con.createStatement();
            String query = "select * from " + schemaName + ".OAUTH20CLIENTCONFIG";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                // Search on everything for debug
                System.out.println("db entry : " + rs.getString("COMPONENTID")
                                   + " " + rs.getString("CLIENTID") + " "
                                   //   + rs.getString("CLIENTSECRET") + " "
                                   + rs.getString("DISPLAYNAME") + " "
                                   + rs.getString("REDIRECTURI") + " "
                                   + rs.getInt("Enabled") + " "
                                   + rs.getString(7));

                if (rs.getString("CLIENTID").equals(clientId) && rs.getString("COMPONENTID").equals(compID)) {
                    System.out.println("Found client " + rs.getString("CLIENTSECRET"));
                    cs = rs.getString("CLIENTSECRET");
                    if (cs == null) {
                        JSONObject clientMetadata = JSONObject.parse(rs.getString(7));
                        cs = (String) clientMetadata.get("client_secret");
                    }
                    break;
                }
            }
        } catch (Throwable x) {
            System.out.println("getSecretType unexpected exception"
                               + x.getMessage());
        } finally {
            System.out.println("getSecretType - time to close");
            con.close();
        }

        if (cs == null) {
            System.out.println("getSecretType: Could not find client " + clientId + " " + compID);
            return "null_client";
        }

        if (cs.equals("")) {
            return "empty_secret";
        } else if (cs.startsWith("{xor}")) {
            return "xor";
        } else if (cs.startsWith("{hash}")) {
            return "hash";
        } else {
            return "plain";
        }
    }

    /**
     * Create the default table used by the tests.
     *
     * @param ds
     *               the data source
     * @throws SQLException
     *                          if it fails
     */
    private void addCustomEntry(DataSource ds, String clientID, String secret, String port, String compID, String salt, String algorithm) throws SQLException {
        String redirectUri = "http://localhost:" + port + "/oauthclient/redirect.jsp";
        Connection con = ds.getConnection(DB_USER, DB_PWD);
        try {
            JSONObject metaD = new JSONObject();
            JSONArray grantTypes = new JSONArray();

            grantTypes.add("authorization_code");
            grantTypes.add("refresh_token");
            grantTypes.add("password");

            metaD.put("scope", "ALL_SCOPES");

            JSONArray redirectUris = new JSONArray();
            redirectUris.add(redirectUri);
            metaD.put("redirect_uris", redirectUris);

            if (grantTypes.size() > 0) {
                metaD.put("grant_types", grantTypes);
            }

            metaD.put("token_endpoint_auth_method", "client_secret_basic");
            metaD.put("client_id", clientID);
            metaD.put("client_secret", secret);
            metaD.put("client_name", clientID);
            metaD.put("introspect_tokens", "false");
            if (salt != null) {
                metaD.put(HASH_SALT, salt);
            }
            if (algorithm != null) {
                metaD.put(HASH_ALGORITHM, algorithm);
            }

            metaD.put(HASH_ITERATIONS, "2048");
            metaD.put(HASH_LENGTH, "32");

            PreparedStatement pstmt = con.prepareStatement("insert into " + schemaName + ".OAUTH20CLIENTCONFIG values (?, ?, ?,?, ?, ?,?)");

            pstmt.setString(1, compID);
            pstmt.setString(2, clientID);
            pstmt.setString(3, secret);
            pstmt.setString(4, clientID);
            pstmt.setString(5, redirectUri);
            pstmt.setInt(6, 1);
            pstmt.setString(7, metaD.toString());

            pstmt.executeUpdate();
            System.out.println("We finished the update");
        } catch (Throwable x) {
            System.out.println("addCustomEntry unexpected exception" + x.getMessage());
        } finally {
            try {
                con.close();
                System.out.println("We closed the connection");
            } catch (Throwable x) {
                System.out.println("addCustomEntry close unexpected exception"
                                   + x.getMessage());
            }
        }
    }

    private String getSalt(DataSource ds, String clientId, String compID) throws Exception {
        if (clientId == null) {
            System.out.println("oAuth20DerbySetup No clientId provided for getSalt");
            return "No clientId provided";
        }
        if (compID == null) {
            System.out.println("oAuth20DerbySetup No compID provided for getSalt, using default: OAuthConfigDerby");
            compID = DEFAULT_COMPID;
        }

        Connection con = ds.getConnection(DB_USER, DB_PWD);
        System.out.println("getSecretType for " + clientId + " schema " + schemaName);

        String salt = null;
        try {
            Statement stmt = con.createStatement();
            String query = "select * from " + schemaName + ".OAUTH20CLIENTCONFIG";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                // Search on everything for debug
                System.out.println("db entry : " + rs.getString("COMPONENTID")
                                   + " " + rs.getString("CLIENTID") + " "
                                   //   + rs.getString("CLIENTSECRET") + " "
                                   + rs.getString("DISPLAYNAME") + " "
                                   + rs.getString("REDIRECTURI") + " "
                                   + rs.getInt("Enabled") + " "
                                   + rs.getString(7));

                if (rs.getString("CLIENTID").equals(clientId) && rs.getString("COMPONENTID").equals(compID)) {
                    System.out.println("Found client " + rs.getString("CLIENTSECRET"));

                    JSONObject clientMetadata = JSONObject.parse(rs.getString(7));
                    salt = (String) clientMetadata.get(HASH_SALT);

                    if (salt == null) {
                        System.out.println("getSalt: salt is null");
                    }

                    break;
                }
            }
        } catch (Throwable x) {
            System.out.println("getSalt unexpected exception"
                               + x.getMessage());
        } finally {
            System.out.println("getSalt - time to close");
            con.close();
        }

        if (salt == null) {
            System.out.println("getSalt: Could not find client " + clientId);
            return "null_client";
        }

        return salt;
    }

    private String getIteration(DataSource ds, String clientId, String compID) throws Exception {
        if (clientId == null) {
            System.out.println("oAuth20DerbySetup No clientId provided for getIteration");
            return "No clientId provided";
        }
        if (compID == null) {
            System.out.println("oAuth20DerbySetup No compID provided for getIteration, using default: OAuthConfigDerby");
            compID = DEFAULT_COMPID;
        }

        Connection con = ds.getConnection(DB_USER, DB_PWD);
        System.out.println("getIteration for " + clientId + " schema " + schemaName);

        String iteration = null;
        try {
            Statement stmt = con.createStatement();
            String query = "select * from " + schemaName + ".OAUTH20CLIENTCONFIG";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                // Search on everything for debug
                System.out.println("db entry : " + rs.getString("COMPONENTID")
                                   + " " + rs.getString("CLIENTID") + " "
                                   //   + rs.getString("CLIENTSECRET") + " "
                                   + rs.getString("DISPLAYNAME") + " "
                                   + rs.getString("REDIRECTURI") + " "
                                   + rs.getInt("Enabled") + " "
                                   + rs.getString(7));

                if (rs.getString("CLIENTID").equals(clientId) && rs.getString("COMPONENTID").equals(compID)) {
                    System.out.println("Found client " + rs.getString("CLIENTSECRET"));

                    JSONObject clientMetadata = JSONObject.parse(rs.getString(7));
                    iteration = String.valueOf(clientMetadata.get(HASH_ITERATIONS));

                    if (iteration == null) {
                        System.out.println("getIteration: iteration is null");
                    }

                    break;
                }
            }
        } catch (Throwable x) {
            System.out.println("getIteration unexpected exception"
                               + x.getMessage());
        } finally {
            System.out.println("getIteration - time to close");
            con.close();
        }

        if (iteration == null) {
            System.out.println("getIteration: Could not find client " + clientId);
            return "null_client";
        }

        return iteration;
    }

    private String getAlgorithm(DataSource ds, String clientId, String compID) throws Exception {
        if (clientId == null) {
            System.out.println("oAuth20DerbySetup No clientId provided for getAlgorithm");
            return "No clientId provided";
        }
        if (compID == null) {
            System.out.println("oAuth20DerbySetup No compID provided for getAlgorithm, using deafult: OAuthConfigDerby");
            compID = DEFAULT_COMPID;
        }

        Connection con = ds.getConnection(DB_USER, DB_PWD);
        System.out.println("getAlgorithm for " + clientId + " schema " + schemaName);

        String algorithm = null;
        try {
            Statement stmt = con.createStatement();
            String query = "select * from " + schemaName + ".OAUTH20CLIENTCONFIG";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                // Search on everything for debug
                System.out.println("db entry : " + rs.getString("COMPONENTID")
                                   + " " + rs.getString("CLIENTID") + " "
                                   //   + rs.getString("CLIENTSECRET") + " "
                                   + rs.getString("DISPLAYNAME") + " "
                                   + rs.getString("REDIRECTURI") + " "
                                   + rs.getInt("Enabled") + " "
                                   + rs.getString(7));

                if (rs.getString("CLIENTID").equals(clientId) && rs.getString("COMPONENTID").equals(compID)) {
                    System.out.println("Found client " + clientId);

                    JSONObject clientMetadata = JSONObject.parse(rs.getString(7));
                    algorithm = (String) clientMetadata.get(HASH_ALGORITHM);

                    if (algorithm == null) {
                        System.out.println("getAlgorithm: hash_alg is null");
                    }

                    break;
                }
            }
        } catch (Throwable x) {
            System.out.println("getAlgorithm unexpected exception"
                               + x.getMessage());
        } finally {
            System.out.println("getAlgorithm - time to close");
            con.close();
        }

        if (algorithm == null) {
            System.out.println("getAlgorithm: Could not find client " + clientId);
            return "null_client";
        }

        return algorithm;
    }

    private String getAccessTokenType(DataSource ds, String accessToken, String tokenType) throws Exception {

        if (accessToken == null) {
            System.out.println("getAccessTokenType request is missing a valid accessToken value");
            return "accessToken missing";
        }
        Connection con = ds.getConnection(DB_USER, DB_PWD);
        System.out.println("getAccessTokenType for schema " + schemaName);

        String accessTokenEncodingType = "hashed";
        try {
            Statement stmt = con.createStatement();
            String query = "select * from " + schemaName + ".OAUTH20CACHE";
            ResultSet rs = stmt.executeQuery(query);
            System.out.println("Have connection");
            System.out.println("db entry : " + rs.toString());
            System.out.println("ResultSet size: " + rs.getFetchSize());

            System.out.println("rs is: " + (rs != null));
            while (rs.next()) {

                System.out.println("db entry: LOOKUPKEY: " + rs.getString("LOOKUPKEY") + " UNIQUEID: " + rs.getString("UNIQUEID") + " TOKENSTRING: " + rs.getString("TOKENSTRING"));

                // we can't really create the same hash as the runtime, so, we'll just return a found/not found flag
                // not found can be because the db has he hashed value, or the access_token isn't there
                // the test cases will make sure that we can use the access_token (found/not found and being able to use the token should show that the behavior was correct for the config)
                if (tokenType.equals("plain")) {
                    System.out.println("access_token: " + accessToken);
                    System.out.println("UNIQUEID: " + rs.getString("UNIQUEID"));
                    if (accessToken.equals(rs.getString("UNIQUEID"))) {
                        accessTokenEncodingType = "plain";
                        break;
                    }
                } else {
                    // the hashed tokens will show up as {hash}<some hashed value} - so, for hashed tokens, we should never be able to match {hash}<access_token>
                    if ((tokenType + accessToken).equals(rs.getString("UNIQUEID"))) {
                        accessTokenEncodingType = "invalid";
                        break;
                    } else {
                        // let's just make sure they start with hashed  - UNIQUEID should always exist, the catch below should be enough
                        if (!rs.getString("UNIQUEID").startsWith(tokenType)) {
                            accessTokenEncodingType = "invalid";
                            break;
                        }
                    }
                }

            }
        } catch (Throwable x) {
            System.out.println("getAccessTokenType unexpected exception" + x.getMessage());
        } finally {
            System.out.println("getAccessTokenType - time to close");
            con.close();
        }

        return accessTokenEncodingType;

    }

    private void clearAllClients(DataSource ds) throws Exception {
        Connection con = ds.getConnection(DB_USER, DB_PWD);
        System.out.println("clearAllClients: " + schemaName);
        try {
            Statement stmt = con.createStatement();
            String query = "delete from " + schemaName + ".OAUTH20CLIENTCONFIG";
            stmt.executeUpdate(query);

            stmt = con.createStatement();
            query = "select * from " + schemaName + ".OAUTH20CLIENTCONFIG";
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                System.out.println("clearAllClients: table not cleared as expected, future tests may have unexpected results.");
            }
        } catch (Throwable x) {
            System.out.println("clearAllClients unexpected exception"
                               + x.getMessage());
        } finally {
            System.out.println("clearAllClients - time to close");
            con.close();
        }

    }
}
