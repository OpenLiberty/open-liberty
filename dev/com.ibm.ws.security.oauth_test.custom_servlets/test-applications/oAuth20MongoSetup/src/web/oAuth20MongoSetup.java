/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

@WebServlet(name = "oAuth20MongoSetup", urlPatterns = { "/oAuth20MongoSetup" })
public class oAuth20MongoSetup extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String CLASS_NAME = oAuth20MongoSetup.class.getName();

    final static String DEFAULT_COMPID = "OAuthConfigDerby"; // this is the correct default, it is the name used in many
                                                             // oauthProvider configs, easier to leave as 'Derby'

    // Collection types
    // Do not change these unless the CustomStoreSample is also changing
    final static String OAUTHCLIENT = "OauthClient";
    final static String OAUTHTOKEN = "OauthToken";
    final static String OAUTHCONSENT = "OauthConsent";

    // Database keys
    // Do not change these unless the CustomStoreSample is also changing
    final static String LOOKUPKEY = "LOOKUPKEY";
    final static String UNIQUEID = "UNIQUEID";
    final static String COMPONENTID = "COMPONENTID";
    final static String TYPE = "TYPE";
    final static String SUBTYPE = "SUBTYPE";
    final static String CREATEDAT = "CREATEDAT";
    final static String LIFETIME = "LIFETIME";
    final static String EXPIRES = "EXPIRES";
    final static String TOKENSTRING = "TOKENSTRING";
    final static String CLIENTID = "CLIENTID";
    final static String USERNAME = "USERNAME";
    final static String SCOPE = "SCOPE";
    final static String REDIRECTURI = "REDIRECTURI";
    final static String STATEID = "STATEID";
    final static String EXTENDEDFIELDS = "EXTENDEDFIELDS";
    final static String PROPS = "PROPS";
    final static String USER = "USER";
    final static String RESOURCE = "RESOURCE";
    final static String PROVIDERID = "PROVIDERID";
    final static String CLIENTSECRET = "CLIENTSECRET";
    final static String DISPLAYNAME = "DISPLAYNAME";
    final static String ENABLED = "ENABLED";
    final static String METADATA = "METADATA";

    String uid = "defaultUID";

    // must match strings used in MongoDBUtils
    final static String UID_WEB = "uid";
    final static String PORT = "port";
    final static String DROP_TABLE = "cleanup";
    final static String DROP_DB = "dropDB";
    final static String ADD_CLIENT = "addClient";
    final static String CLIENT_ID_WEB = "clientID";
    final static String SECRET_WEB = "secret";
    final static String CHECK_SECRET = "checkSecret";
    final static String PROVIDER_ID = "provider";
    final static String COMP_ID = "compID";
    final static String DB_NAME = "dbName";
    final static String DB_USER = "dbUser";
    final static String DB_HOST = "dbHost";
    final static String DB_PORT = "dbPort";
    final static String DB_PWD = "dbPwd";
    final static String SALT = "checkSalt";
    final static String ALGORITHM = "checkAlgorithm";
    final static String ITERATION = "checkIteration";
    final static String CLEAR_CLIENTS = "clearClients";

    // must match constants in OidcBaseClient
    public static final String HASH_SALT = "salt"; // must match constants in OidcBaseClient
    public static final String HASH_ALGORITHM = "hash_alg"; // must match constants in OidcBaseClient
    public static final String HASH_ITERATIONS = "hash_itr"; // must match constants in OidcBaseClient
    public static final String HASH_LENGTH = "hash_len"; // must match constants in OidcBaseClient

    protected DB mongoDB;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try {

            System.out.println("oAuth20MongoSetup MongoDB setup util parms: "
                               + Arrays.toString(request.getParameterMap().keySet().toArray()));
            String port = request.getParameter(PORT);
            System.out.println("oAuth20MongoSetup We are in the servlet. Port is: " + port);

            String dropTable = request.getParameter(DROP_TABLE);
            String dropDB = request.getParameter(DROP_DB);
            String addClient = request.getParameter(ADD_CLIENT);
            String checkSecret = request.getParameter(CHECK_SECRET);
            String getSalt = request.getParameter(SALT);
            String getAlgorithm = request.getParameter(ALGORITHM);
            String getIteration = request.getParameter(ITERATION);

            connect(request.getParameter(DB_NAME), request.getParameter(DB_USER), request.getParameter(DB_HOST),
                    request.getParameter(DB_PORT), request.getParameter(DB_PWD));

            String u = request.getParameter(UID_WEB);
            if (u != null) {
                uid = u;
            }
            String clearClients = request.getParameter(CLEAR_CLIENTS);
            if (clearClients != null) {
                clearAllClients();
            }

            if (Boolean.TRUE.toString().equals(addClient)) { // add a new client to the database
                String clientID = request.getParameter(CLIENT_ID_WEB);
                System.out.println("Request to add a new client " + clientID);
                String secret = request.getParameter(SECRET_WEB);
                String providerId = request.getParameter(COMP_ID);
                addCustomEntryMongo(clientID, secret, port, providerId, getSalt, getAlgorithm);
            } else if (getAlgorithm != null) {
                String clientID = request.getParameter(CLIENT_ID_WEB);
                String compID = request.getParameter(COMP_ID);
                System.out.println("Request to get algorithm for client " + clientID + " compId " + compID);
                String type = getAlgorithm(clientID, compID);
                System.out.println("Algorithm is " + type);

                PrintWriter writer = response.getWriter();
                writer.write(type);
                writer.flush();
                writer.close();
            } else if (getIteration != null) {
                String clientID = request.getParameter(CLIENT_ID_WEB);
                String compID = request.getParameter(COMP_ID);
                System.out.println("Request to get iteration for client " + clientID + " compId " + compID);
                String type = getIteration(clientID, compID);
                System.out.println("Iteration is " + type);

                PrintWriter writer = response.getWriter();
                writer.write(type);
                writer.flush();
                writer.close();
            } else if (getSalt != null) {
                String clientID = request.getParameter(CLIENT_ID_WEB);
                String compID = request.getParameter(COMP_ID);
                System.out.println("Request to get salt for client " + clientID + " compId " + compID);
                String type = getSalt(clientID, compID);
                System.out.println("Salt is " + type);

                PrintWriter writer = response.getWriter();
                writer.write(type);
                writer.flush();
                writer.close();

            } else if (checkSecret != null) { // check what kind of encoding the secret has for a client
                String clientID = request.getParameter(CLIENT_ID_WEB);
                String providerId = request.getParameter(COMP_ID);
                System.out.println("Request to check client: " + clientID + " " + providerId);

                String type = getSecretType(clientID, providerId);
                System.out.println("Secret is of type " + type);

                PrintWriter writer = response.getWriter();
                writer.write(type);
                writer.flush();
                writer.close();
            } else if ("true".equals(dropTable)) {
                dropClientConfigTableMongo();
            } else if ("true".equals(dropDB)) {
                dropDatabase();
            } else { // default setup
                checkAndDrop();
                System.out.println("Precheck on mongoDB -- ");
                queryTableMongo();
                System.out.println("Precheck complete on mongoDB -- ");
                String redirectUri = "http://localhost:" + port + "/oauthclient/redirect.jsp";
                String redirectUri2 = "http://localhost:" + port + "/oauthclient/authorize_redirect.jsp";

                createCacheTableMongo(mongoDB);

                // To make the common code easier, just leaving the
                // OAuthConfigDerby references.
                addEntryMongo(mongoDB, DEFAULT_COMPID, "dclient01", "secret", "dclient01", redirectUri, true,
                              buildClientMetaData("dclient01", redirectUri, "true"));
                addEntryMongo(mongoDB, DEFAULT_COMPID, "dclient02", "secret", "dclient02", redirectUri, true,
                              buildClientMetaData("dclient02", redirectUri, "false"));
                addEntryMongo(mongoDB, "OAuthConfigDerby2", "dclient01", "secret", "dclient01", redirectUri, true,
                              buildClientMetaData("dclient01", redirectUri, "true"));
                addEntryMongo(mongoDB, "OAuthConfigDerby2", "dclient02", "secret", "dclient02", redirectUri, true,
                              buildClientMetaData("dclient02", redirectUri, "false"));

                addEntryMongo(mongoDB, DEFAULT_COMPID, "dclient03", "secret", "dclient03", redirectUri, true,
                              buildClientMetaData("dclient03", redirectUri, "true"));
                addEntryMongo(mongoDB, DEFAULT_COMPID, "dclient04", "secret", "dclient04", redirectUri, true,
                              buildClientMetaData("dclient03", redirectUri, "true"));

                // client for OAuthGrantTypesCustomStoreTest
                addEntryMongo(mongoDB, "OAuthConfigSampleGrantTypes", "client03", "{xor}LDo8LTor", "client03",
                              redirectUri2, true, buildClientMetaData("client03", redirectUri2, "true"));

                queryTableMongo();

            }
        } catch (Throwable x) {
            System.out.println("oAuth20MongoSetup Someone threw an exception (see messages.log for stack): " + x);
            x.printStackTrace();
        }
    }

    /**
     * Create the default table used by the tests.
     *
     */
    private void addEntryMongo(DB ds, String compId, String clientID, String secret, String dispName,
                               String redirectUri, boolean enabled, JSONObject metaData) {

        System.out.println(CLASS_NAME + "Create on OauthClient: " + clientID + " " + compId);

        try {
            DBCollection col = mongoDB.getCollection(OAUTHCLIENT + uid);
            BasicDBObject d = new BasicDBObject(CLIENTID, clientID);

            d.append(PROVIDERID, compId);

            d.append(CLIENTSECRET, secret);
            d.append(DISPLAYNAME, dispName);
            d.append(REDIRECTURI, redirectUri);
            d.append(ENABLED, enabled);
            d.append(METADATA, metaData.toString());

            col.insert(d);

        } catch (Throwable e) {
            System.out.println("oAuth20MongoSetup Exception trying to add " + clientID);
            e.printStackTrace();
        }

    }

    /**
     * Query the database for all members
     *
     * @throws Exception
     */
    private void queryTableMongo() throws Exception {
        System.out.println("oAuth20MongoSetup queryTable, double check setup on " + mongoDB.getName());

        DBCollection col = mongoDB.getCollection(OAUTHCLIENT + uid);
        System.out.println("oAuth20MongoSetup Collection " + col.getName());

        DBCursor cursor = col.find();

        if (cursor != null && cursor.size() > 0) {
            while (cursor.hasNext()) {
                DBObject dbo = cursor.next();
                System.out.println("oAuth20MongoSetup Client " + dbo.get(CLIENTID) + " " + dbo.get(PROVIDERID) + " "
                                   + dbo.get("_id"));
            }
        } else {
            System.out.println("oAuth20MongoSetup Query returned no results");
        }

    }

    private String getSecretType(String clientId, String providerId) throws Exception {

        DBCollection col = mongoDB.getCollection(OAUTHCLIENT + uid);

        BasicDBObject d = new BasicDBObject(CLIENTID, clientId);
        d.append(PROVIDERID, providerId == null ? DEFAULT_COMPID : providerId);
        DBObject dbo = col.findOne(d);

        if (dbo == null) {
            System.out.println("getSecretType: Could not find client " + clientId + " providerId " + providerId);
            return "null_client";
        }

        String cs = (String) dbo.get(CLIENTSECRET);

        System.out.println("oAuth20MongoSetup " + cs);

        if (cs == null) {
            return "null_secret";
        } else if (cs.equals("")) {
            return "empty_secret";
        } else if (cs.startsWith("{xor}")) {
            return "xor";
        } else if (cs.startsWith("{hash}")) {
            return "hash";
        } else {
            return "plain";
        }

    }

    private String getAlgorithm(String clientId, String providerId) throws Exception {

        DBCollection col = mongoDB.getCollection(OAUTHCLIENT + uid);

        BasicDBObject d = new BasicDBObject(CLIENTID, clientId);
        d.append(PROVIDERID, providerId == null ? DEFAULT_COMPID : providerId);
        DBObject dbo = col.findOne(d);

        if (dbo == null) {
            System.out.println("getAlgorithm: Could not find client " + clientId + " providerId " + providerId);
            return "null_client";
        }

        String cs = (String) dbo.get(METADATA);

        JSONObject clientMetadata = JSONObject.parse(cs);
        String algorithm = (String) clientMetadata.get(HASH_ALGORITHM);

        if (algorithm == null) {
            return "null_algorithm";
        }
        return algorithm;

    }

    private String getIteration(String clientId, String providerId) throws Exception {

        DBCollection col = mongoDB.getCollection(OAUTHCLIENT + uid);

        BasicDBObject d = new BasicDBObject(CLIENTID, clientId);
        d.append(PROVIDERID, providerId == null ? DEFAULT_COMPID : providerId);
        DBObject dbo = col.findOne(d);

        if (dbo == null) {
            System.out.println("getIteration: Could not find client " + clientId + " providerId " + providerId);
            return "null_client";
        }

        String cs = (String) dbo.get(METADATA);

        JSONObject clientMetadata = JSONObject.parse(cs);
        String iteration = String.valueOf(clientMetadata.get(HASH_ITERATIONS));

        if (iteration == null) {
            return "null_iteration";
        }
        return iteration;

    }

    private String getSalt(String clientId, String providerId) throws Exception {

        DBCollection col = mongoDB.getCollection(OAUTHCLIENT + uid);

        BasicDBObject d = new BasicDBObject(CLIENTID, clientId);
        d.append(PROVIDERID, providerId == null ? DEFAULT_COMPID : providerId);
        DBObject dbo = col.findOne(d);

        if (dbo == null) {
            System.out.println("getSalt: Could not find client " + clientId + " providerId " + providerId);
            return "null_client";
        }

        String cs = (String) dbo.get(METADATA);

        JSONObject clientMetadata = JSONObject.parse(cs);
        String salt = (String) clientMetadata.get(HASH_SALT);

        if (salt == null) {
            return "null_salt";
        }
        return salt;

    }

    /**
     * Drop the client table
     *
     */
    private void dropClientConfigTableMongo() {
        mongoDB.getCollection(OAUTHCLIENT + uid).drop();
        System.out.println("oAuth20MongoSetup Dropped " + OAUTHCLIENT + uid);
    }

    private void checkAndDrop() throws SQLException {
        try {
            // Having some troubles with mystery timeouts on z/OS. Added DB tuning on the connect method and extra trace here
            System.out.println("oAuth20MongoSetup Starting checkAndDrop -- running collectionExists and, if needed, dropDatabase");
            if (mongoDB.collectionExists(OAUTHCLIENT + uid) || mongoDB.collectionExists(OAUTHCONSENT + uid)
                || mongoDB.collectionExists(OAUTHTOKEN + uid)) {
                System.out.println("oAuth20MongoSetup Drop databases for cleanup before test start");
                dropDatabase();
            }
            System.out.println("oAuth20MongoSetup Completed checkAndDrop");
        } catch (Exception e) {
            System.out.println("oAuth20MongoSetup failed doing checkAndDrop " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void dropDatabase() throws SQLException {
        String name = mongoDB.getName();
        mongoDB.getCollection(OAUTHCLIENT + uid).drop();
        mongoDB.getCollection(OAUTHCONSENT + uid).drop();
        mongoDB.getCollection(OAUTHTOKEN + uid).drop();

        // just in case, clean up generic tables
        if (mongoDB.collectionExists(OAUTHCLIENT)) {
            mongoDB.getCollection(OAUTHCLIENT).drop();
            mongoDB.getCollection(OAUTHCONSENT).drop();
            mongoDB.getCollection(OAUTHTOKEN).drop();
            System.out.println("oAuth20MongoSetup Dropped collections for " + OAUTHCLIENT);
        }

        if (mongoDB.collectionExists(OAUTHCLIENT + "defaultUID")) {
            mongoDB.getCollection(OAUTHCLIENT + "defaultUID").drop();
            mongoDB.getCollection(OAUTHCONSENT + "defaultUID").drop();
            mongoDB.getCollection(OAUTHTOKEN + "defaultUID").drop();
            System.out.println("oAuth20MongoSetup Dropped collections for " + OAUTHCLIENT + "defaultUID");
        }

        System.out.println(
                           "oAuth20MongoSetup Dropped collections in  mongoDB database " + name + ". Table uid was " + uid);

        System.out.println("oAuth20MongoSetup Collection dropped for " + OAUTHCLIENT + uid + ": "
                           + !mongoDB.collectionExists(OAUTHCLIENT + uid));
    }

    /**
     * Create the default table used by the tests.
     *
     */
    private void createCacheTableMongo(DB ds) {

        mongoDB.getCollection(OAUTHCLIENT + uid);
        mongoDB.getCollection(OAUTHCONSENT + uid);
        mongoDB.getCollection(OAUTHTOKEN + uid);

        System.out.println("oAuth20MongoSetup created tables with uid " + uid);
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

    /**
     * These settings make a user similar to dclient01
     *
     * @param clientID
     * @param secret
     * @param port
     */
    private void addCustomEntryMongo(String clientID, String secret, String port, String providerID, String salt,
                                     String algorithm) {

        System.out.println(CLASS_NAME + "Create on OauthClient: " + clientID + " provider " + " providerID "
                           + providerID + " " + secret + " " + salt + " " + algorithm);

        if (clientID == null || secret.equals(null)) {
            throw new IllegalArgumentException("Received null for " + (clientID.equals(null) ? "clientID" : "secret"));
        }

        String redirectUri = "http://localhost:" + port + "/oauthclient/redirect.jsp";
        try {
            DBCollection col = mongoDB.getCollection(OAUTHCLIENT + uid);
            BasicDBObject d = new BasicDBObject(CLIENTID, clientID);

            d.append(PROVIDERID, providerID);
            d.append(CLIENTSECRET, secret);
            d.append(DISPLAYNAME, clientID);
            d.append(REDIRECTURI, redirectUri);
            d.append(ENABLED, true);

            JSONObject metaD = new JSONObject();
            JSONArray grantTypes = new JSONArray();

            grantTypes.add("authorization_code");
            grantTypes.add("password");

            grantTypes.add("refresh_token");

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
            } else {
                System.out.println("No salt provided");
            }
            if (algorithm != null) {
                metaD.put(HASH_ALGORITHM, algorithm);
            } else {
                System.out.println("No algorithm provided");
            }

            metaD.put(HASH_ITERATIONS, "2048");
            metaD.put(HASH_LENGTH, "32");
            d.append(METADATA, metaD.toString());

            col.insert(d);

            queryTableMongo();

        } catch (Throwable e) {
            System.out.println("oAuth20MongoSetup Exception trying to add " + clientID);
            e.printStackTrace();
        }

    }

    private void connect(String dbName, String dbUser, String dbHost, String dbPort, String dbPwd) {
        MongoClient mongoClient = null;
        try {
            System.out.println("oAuth20MongoSetup connecting to the " + dbName + " database at " + dbHost + ":" + dbPort
                               + " using table modifier " + uid);
            List<MongoCredential> credentials = Collections.emptyList();
            MongoCredential credential = MongoCredential.createCredential(dbUser, dbName, dbPwd.toCharArray());
            credentials = Collections.singletonList(credential);
            MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder().connectTimeout(10000);
            optionsBuilder.socketTimeout(10000);
            optionsBuilder.socketKeepAlive(true);
            MongoClientOptions clientOptions = optionsBuilder.build();
            mongoClient = new MongoClient(new ServerAddress(dbHost, Integer.parseInt(dbPort)), credentials, clientOptions);
            mongoDB = mongoClient.getDB(dbName);
            System.out.println("oAuth20MongoSetup connected to the database");

        } catch (UnknownHostException e) {
            System.out.println("oAuth20MongoSetup failed connecting to the database " + e);
            e.printStackTrace();
            throw new IllegalStateException("oAuth20MongoSetup Database is not connected at " + dbHost + ":" + dbPort
                                            + ", cannot process requests. Failure is " + e, e);
        }
    }

    private void clearAllClients() throws Exception {
        System.out.println("clearAllClients: ");
        try {
            DBCollection col = mongoDB.getCollection(OAUTHCLIENT + uid);
            System.out.println("oAuth20MongoSetup Collection " + col.getName());

            col.remove(new BasicDBObject());

            DBCursor cursor = col.find();

            if (cursor != null && cursor.size() > 0) {
                if (cursor.hasNext()) {
                    System.out.println("clearAllClients: table not cleared as expected, future tests may have unexpected results.");
                }
            }
        } catch (Throwable x) {
            System.out.println("clearAllClients unexpected exception"
                               + x.getMessage());
        }

    }
}
