/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package security.custom.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.ibm.websphere.security.oauth20.store.OAuthClient;
import com.ibm.websphere.security.oauth20.store.OAuthConsent;
import com.ibm.websphere.security.oauth20.store.OAuthStore;
import com.ibm.websphere.security.oauth20.store.OAuthStoreException;
import com.ibm.websphere.security.oauth20.store.OAuthToken;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;

/**
 * The main purpose of this sample is to demonstrate the use of a CustomStore
 * for an OAuth Provider. It is provided as-is.
 *
 * It uses a MongoDB back end.
 **/
public class CustomStoreSample implements OAuthStore {

    public final static String MONGO_PROPS_FILE = "mongoDB.props";
    private String dbName = "default";
    private String dbHost = "localhost";
    private String dbUser = "user";
    private String dbPwd = "password";
    private int dbPort = 27017;
    String uid = "defaultUID";

    private DB db = null;
    private DBCollection clientCollection = null;
    private DBCollection tokenCollection = null;
    private DBCollection consentCollection = null;

    // Collection types in the database
    static String OAUTHCLIENT = "OauthClient";
    static String OAUTHTOKEN = "OauthToken";
    static String OAUTHCONSENT = "OauthConsent";

    // Keys in the database
    final static String LOOKUPKEY = "LOOKUPKEY";
    final static String UNIQUEID = "UNIQUEID";
    final static String COMPONENTID = "COMPONENTID";
    final static String TYPE = "TYPE";
    final static String SUBTYPE = "SUBTYPE";
    final static String CREATEDAT = "CREATEDAT";
    final static String LIFETIME = "LIFETIME";
    final static String EXPIRES = "EXPIRES"; // long
    final static String TOKENSTRING = "TOKENSTRING";
    final static String CLIENTID = "CLIENTID";
    final static String USERNAME = "USERNAME";
    final static String SCOPE = "SCOPE";
    final static String REDIRECTURI = "REDIRECTURI";
    final static String STATEID = "STATEID";
    final static String EXTENDEDFIELDS = "EXTENDEDFIELDS";
    final static String PROPS = "PROPS";
    final static String RESOURCE = "RESOURCE";
    final static String PROVIDERID = "PROVIDERID";
    final static String CLIENTSECRET = "CLIENTSECRET";
    final static String DISPLAYNAME = "DISPLAYNAME";
    final static String ENABLED = "ENABLED";
    final static String METADATA = "METADATA";

    private final static int RETRY_COUNT = 3;

    public CustomStoreSample() {
        System.out.println("CustomStoreSample init");
    }

    /**
     * Simple helper method to get a connection to mongoDB.
     *
     * It is not a general recommendation for all CustomStore implementations.
     *
     * @return
     */
    private synchronized DB getDB() {
        if (db == null) {
            System.out.println("CustomStoreSample DB reference is null, connect to database.");
            getDatabaseConfig();

            MongoClient mongoClient = null;
            try {
                System.out.println("CustomStoreSample connecting to the " + dbName + " database at " + dbHost + ":"
                                   + dbPort + " using table modifier " + uid);
                List<MongoCredential> credentials = Collections.emptyList();
                MongoCredential credential = MongoCredential.createCredential(dbUser, dbName, dbPwd.toCharArray());
                credentials = Collections.singletonList(credential);
                MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder().connectTimeout(30000);
                optionsBuilder.socketTimeout(10000);
                optionsBuilder.socketKeepAlive(true);
                optionsBuilder.maxWaitTime(30000);
                MongoClientOptions clientOptions = optionsBuilder.build();
                mongoClient = new MongoClient(new ServerAddress(dbHost, dbPort), credentials, clientOptions);
                db = mongoClient.getDB(dbName);
                System.out.println("CustomStoreSample connected to the database");
                OAUTHCLIENT = OAUTHCLIENT + uid;
                OAUTHTOKEN = OAUTHTOKEN + uid;
                OAUTHCONSENT = OAUTHCONSENT + uid;

                // for test purposes, double check the starting state of the database
                DBCollection col = getClientCollection();
                DBCursor result = col.find();
                if (result.hasNext()) {
                    System.out.println("Database is pre-populated with clients: " + OAUTHCLIENT);
                    while (result.hasNext()) {
                        DBObject dbo = result.next();
                        System.out.println("Client: " + (String) dbo.get(PROVIDERID) + " " + (String) dbo.get(CLIENTID));
                    }
                } else {
                    System.out.println("Database is not pre-populated with clients: " + OAUTHCLIENT);
                }
                result.close();
            } catch (UnknownHostException e) {
                System.out.println("CustomStoreSample failed connecting to the database " + e);
                e.printStackTrace();
                throw new IllegalStateException("CustomStoreSample Database is not connected at " + dbHost + ":" + dbPort
                                                + ", cannot process requests. Failure is " + e, e);
            }
        }

        return db;

    }

    private DBCollection getClientCollection() {
        if (clientCollection == null) {
            System.out.println("CustomStoreSample getting collection for OAUTHCLIENT");
            clientCollection = getDB().getCollection(OAUTHCLIENT);
        }
        return clientCollection;
    }

    private DBCollection getTokenCollection() {
        if (tokenCollection == null) {
            tokenCollection = getDB().getCollection(OAUTHTOKEN);
        }
        return tokenCollection;
    }

    private DBCollection getConsentCollection() {
        if (consentCollection == null) {
            consentCollection = getDB().getCollection(OAUTHCONSENT);
        }
        return consentCollection;
    }

    /**
     * This helper method uses a properties file to get the database connection
     * parameters. It was done this way to support local testing of both Bell
     * and User Feature configurations.
     *
     * It is not a general recommendation for all CustomStore implementations.
     *
     */
    private void getDatabaseConfig() {
        System.out.println("CustomStoreSample entering getDatabaseConfig");
        File f = new File(MONGO_PROPS_FILE);
        if (!f.exists()) {
            throw new IllegalStateException("CustomStoreSample Database config file " + MONGO_PROPS_FILE
                                            + " was not found. This may be normal during server startup");
        }
        try {
            System.out.println("CustomStoreSample start reading props file");
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.forName("UTF8")));
            System.out.println("CustomStoreSample BufferedReader created, get props from file.");
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("CustomStoreSample line to read is: " + line);
                    if (line.startsWith("#") || line.trim().equals("")) {
                        continue;
                    }
                    String[] prop = line.split(":");
                    if (prop.length != 2) {
                        System.out.println("CustomStoreSample Exception key:value syntax of properties in " + MONGO_PROPS_FILE
                                           + ", line is: " + line);
                    } else {
                        if (prop[0].equals("DBNAME")) {
                            dbName = prop[1];
                        } else if (prop[0].equals("HOST")) {
                            dbHost = prop[1];
                        } else if (prop[0].equals("PWD")) {
                            dbPwd = prop[1];
                        } else if (prop[0].equals("PORT")) {
                            dbPort = Integer.parseInt(prop[1]);
                        } else if (prop[0].equals("USER")) {
                            dbUser = prop[1];
                        } else if (prop[0].equals("UID")) {
                            uid = prop[1];
                        } else {
                            System.out.println("CustomStoreSample Unexpected property in " + MONGO_PROPS_FILE + ": " + prop[0]);
                        }
                    }
                }

                System.out.println("CustomStoreSample Table mod is " + uid);
            } finally {
                br.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Database config could not be retrieved from " + MONGO_PROPS_FILE, e);
        }
    }

    @Override
    public void create(OAuthClient oauthClient) throws OAuthStoreException {
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                DBCollection col = getClientCollection();
                col.insert(createClientDBObjectHelper(oauthClient));
            } catch (Exception e) {
                if (i < RETRY_COUNT && isNetworkFailure(e)) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                    }
                } else {
                    throw new OAuthStoreException("Failed to process create on OAuthClient " + oauthClient.getClientId(), e);
                }
            }
        }
    }

    private BasicDBObject createClientDBObjectHelper(OAuthClient oauthClient) {
        BasicDBObject d = new BasicDBObject(CLIENTID, oauthClient.getClientId());

        d.append(PROVIDERID, oauthClient.getProviderId());
        d.append(CLIENTSECRET, oauthClient.getClientSecret());
        d.append(DISPLAYNAME, oauthClient.getDisplayName());
        d.append(ENABLED, oauthClient.isEnabled());
        d.append(METADATA, oauthClient.getClientMetadata());
        return d;
    }

    @Override
    public void create(OAuthToken oauthToken) throws OAuthStoreException {
        try {
            DBCollection col = getTokenCollection();
            col.insert(createTokenDBObjectHelper(oauthToken));

            System.out.println("CustomStoreSample create Token " + oauthToken.getTokenString());
        } catch (Exception e) {
            throw new OAuthStoreException("Failed to process create on OAuthToken " + oauthToken.getClientId(), e);
        }
    }

    private BasicDBObject createTokenDBObjectHelper(OAuthToken oauthToken) {
        BasicDBObject d = new BasicDBObject(LOOKUPKEY, oauthToken.getLookupKey());
        d.append(UNIQUEID, oauthToken.getUniqueId());
        d.append(PROVIDERID, oauthToken.getProviderId());
        d.append(TYPE, oauthToken.getType());
        d.append(SUBTYPE, oauthToken.getSubType());
        d.append(CREATEDAT, oauthToken.getCreatedAt());
        d.append(LIFETIME, oauthToken.getLifetimeInSeconds());
        d.append(EXPIRES, oauthToken.getExpires());
        d.append(TOKENSTRING, /* PasswordUtil.passwordEncode( */oauthToken.getTokenString()/* ) */);
        d.append(CLIENTID, oauthToken.getClientId());
        d.append(USERNAME, oauthToken.getUsername());
        d.append(SCOPE, oauthToken.getScope());
        d.append(REDIRECTURI, oauthToken.getRedirectUri());
        d.append(STATEID, oauthToken.getStateId());
        d.append(PROPS, oauthToken.getTokenProperties());
        return d;
    }

    @Override
    public void create(OAuthConsent oauthConsent) throws OAuthStoreException {
        try {
            DBCollection col = getConsentCollection();
            col.insert(createConsentDBObjectHelper(oauthConsent));
        } catch (Exception e) {
            throw new OAuthStoreException("Failed to process create on OAuthConsent " + oauthConsent.getClientId(), e);
        }
    }

    private BasicDBObject createConsentDBObjectHelper(OAuthConsent oauthConsent) {
        BasicDBObject d = new BasicDBObject(CLIENTID, oauthConsent.getClientId());
        d.append(USERNAME, oauthConsent.getUser());
        d.append(SCOPE, oauthConsent.getScope());
        d.append(RESOURCE, oauthConsent.getResource());
        d.append(PROVIDERID, oauthConsent.getProviderId());
        d.append(EXPIRES, oauthConsent.getExpires());
        d.append(PROPS, oauthConsent.getConsentProperties());
        return d;
    }

    @Override
    public OAuthClient readClient(String providerId, String clientId) throws OAuthStoreException {
        System.out.println("CustomStoreSample entering readClient for " + clientId);
        OAuthClient client = null;
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                client = innerReadClient(providerId, clientId);
                break;
            } catch (Exception e) {
                if (i < RETRY_COUNT && isNetworkFailure(e)) {
                    try {
                        System.out.println("CustomStoreSample readClient hit a failure, trying again " + e.getMessage());

                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                    }
                } else {
                    throw e;
                }
            }
        }
        return client;
    }

    private OAuthClient innerReadClient(String providerId, String clientId) throws OAuthStoreException {
        System.out.println("CustomStoreSample entering innerReadClient for " + clientId);
        try {
            DBCollection col = getClientCollection();
            BasicDBObject d = new BasicDBObject(CLIENTID, clientId);
            d.append(PROVIDERID, providerId);
            DBObject dbo = col.findOne(d);
            if (dbo == null) {
                System.out.println("CustomStoreSample readClient Did not find clientId " + clientId + " under " + providerId);
                return null;
            }

            System.out.println("CustomStoreSample readClient Found clientId " + clientId + " under " + providerId + " _id " + dbo.get("_id"));
            return createOAuthClientHelper(dbo);

        } catch (Exception e) {
            throw new OAuthStoreException("Failed to readClient " + clientId + " under " + providerId, e);
        }
    }

    private OAuthClient createOAuthClientHelper(DBObject dbo) {
        return new OAuthClient((String) dbo.get(PROVIDERID), (String) dbo.get(CLIENTID), (String) dbo.get(CLIENTSECRET), (String) dbo.get(DISPLAYNAME), (boolean) dbo
                        .get(ENABLED), (String) dbo.get(METADATA));
    }

    @Override
    public Collection<OAuthClient> readAllClients(String providerId, String attribute) throws OAuthStoreException {
        Collection<OAuthClient> results = null;

        try {
            DBCollection col = getClientCollection();

            DBCursor cursor = null;
            if (attribute == null || attribute.isEmpty()) {
                cursor = col.find(new BasicDBObject(PROVIDERID, providerId));
            } else {
                System.out.println("CustomStoreSample Attribute on readAllClients not implemented");
                // TODO Need to create query to check for all clients that
                // contain 'attribute' in metadata.
            }

            if (cursor != null && cursor.size() > 0) {
                results = new HashSet<OAuthClient>();
                while (cursor.hasNext()) {
                    DBObject dbo = cursor.next();
                    results.add(createOAuthClientHelper(dbo));
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            throw new OAuthStoreException("Failed on readAllClients found under " + providerId, e);
        }
        return results;
    }

    @Override
    public OAuthToken readToken(String providerId, String lookupKey) throws OAuthStoreException {
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                DBCollection col = getTokenCollection();
                DBObject dbo = col.findOne(createTokenKeyHelper(providerId, lookupKey));
                if (dbo == null) {
                    System.out.println("CustomStoreSample readToken Did not find lookupKey " + lookupKey);
                    return null;
                }
                System.out.println("CustomStoreSample readToken Found lookupKey " + lookupKey + " under " + providerId + " _id " + dbo.get("_id"));
                return createOAuthTokenHelper(dbo);
            } catch (Exception e) {
                if (i < RETRY_COUNT && isNetworkFailure(e)) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                    }
                } else {
                    throw new OAuthStoreException("CustomStoreSample Failed to readToken " + lookupKey, e);
                }
            }
        }
        throw new OAuthStoreException("CustomStoreSample Failed to readToken after " + RETRY_COUNT + "tries: " + lookupKey);
    }

    private OAuthToken createOAuthTokenHelper(DBObject dbo) {
        return new OAuthToken((String) dbo.get(LOOKUPKEY), (String) dbo.get(UNIQUEID), (String) dbo.get(PROVIDERID), (String) dbo.get(TYPE), (String) dbo.get(SUBTYPE), (long) dbo
                        .get(CREATEDAT), (int) dbo.get(LIFETIME), (long) dbo.get(EXPIRES), (String) dbo.get(TOKENSTRING), (String) dbo.get(CLIENTID), (String) dbo
                                        .get(USERNAME), (String) dbo.get(SCOPE), (String) dbo.get(REDIRECTURI), (String) dbo.get(STATEID), (String) dbo.get(PROPS));
    }

    @Override
    public Collection<OAuthToken> readAllTokens(String providerId, String username) throws OAuthStoreException {
        try {
            DBCollection col = getTokenCollection();
            BasicDBObject d = new BasicDBObject(USERNAME, username);
            d.append(PROVIDERID, providerId);
            DBCursor result = col.find(d);
            Collection<OAuthToken> collection = null;

            while (result.hasNext()) {
                DBObject dbo = result.next();
                if (collection == null) {
                    collection = new ArrayList<OAuthToken>();
                }
                collection.add(createOAuthTokenHelper(dbo));
            }
            result.close();
            return collection;
        } catch (Exception e) {
            throw new OAuthStoreException("Failed to readAllTokens for " + username + " under " + providerId, e);
        }
    }

    @Override
    public int countTokens(String providerId, String username, String clientId) throws OAuthStoreException {
        try {
            DBCollection col = getTokenCollection();
            BasicDBObject d = new BasicDBObject(USERNAME, username);
            d.append(PROVIDERID, providerId);
            d.append(CLIENTID, clientId);
            return (int) col.count(d); // mongoDB returns as a long
        } catch (Exception e) {
            throw new OAuthStoreException("Failed on countTokens for " + username, e);
        }
    }

    @Override
    public OAuthConsent readConsent(String providerId, String username, String clientId, String resource) throws OAuthStoreException {
        try {
            DBCollection col = getConsentCollection();
            DBObject dbo = col.findOne(createConsentKeyHelper(providerId, username, clientId, resource));
            if (dbo == null) {
                System.out.println("CustomStoreSample readConsent Did not find username " + username);
                return null;
            }
            System.out.println("CustomStoreSample readConsent Found clientId " + clientId + " under " + providerId + " _id " + dbo.get("_id"));
            return new OAuthConsent(clientId, (String) dbo.get(USERNAME), (String) dbo.get(SCOPE), resource, providerId, (long) dbo.get(EXPIRES), (String) dbo.get(PROPS));
        } catch (Exception e) {
            throw new OAuthStoreException("Failed on readConsent for " + username, e);
        }
    }

    @Override
    public void update(OAuthClient oauthClient) throws OAuthStoreException {
        try {
            DBCollection col = getClientCollection();
            col.update(createClientKeyHelper(oauthClient), createClientDBObjectHelper(oauthClient), false, false);
            System.out.println("CustomStoreSample update on " + oauthClient.getClientId());
        } catch (Exception e) {
            throw new OAuthStoreException("Failed on update for OAuthClient for " + oauthClient.getClientId(), e);
        }
    }

    @Override
    public void update(OAuthToken oauthToken) throws OAuthStoreException {
        try {
            DBCollection col = getTokenCollection();
            col.update(createTokenKeyHelper(oauthToken), createTokenDBObjectHelper(oauthToken), false, false);
        } catch (Exception e) {
            throw new OAuthStoreException("Failed on update for OAuthToken for " + oauthToken.getClientId(), e);
        }
    }

    @Override
    public void update(OAuthConsent oauthConsent) throws OAuthStoreException {
        try {
            DBCollection col = getConsentCollection();
            col.update(createConsentKeyHelper(oauthConsent), createConsentDBObjectHelper(oauthConsent), false, false);
        } catch (Exception e) {
            throw new OAuthStoreException("Failed on update for OAuthConsent for " + oauthConsent.getClientId(), e);
        }
    }

    @Override
    public void deleteClient(String providerId, String clientId) throws OAuthStoreException {
        try {
            DBCollection col = getClientCollection();
            WriteResult wr = col.remove(createClientKeyHelper(providerId, clientId));
            System.out.println("CustomStoreSample deleteClient requested on clientId " + clientId + " under " + providerId);
        } catch (Exception e) {
            throw new OAuthStoreException("Failed on delete for OAuthClient for " + clientId, e);
        }
    }

    @Override
    public void deleteToken(String providerId, String lookupKey) throws OAuthStoreException {
        try {
            DBCollection col = getTokenCollection();
            col.remove(createTokenKeyHelper(providerId, lookupKey));
        } catch (Exception e) {
            throw new OAuthStoreException("Failed on delete for OAuthToken for " + lookupKey, e);
        }
    }

    @Override
    public void deleteTokens(String providerId, long timestamp) throws OAuthStoreException {
        try {
            System.out.println("CustomStoreSample deleteTokens request for " + providerId + " expiring before " + timestamp);
            DBCollection col = getTokenCollection();
            System.out.println("CustomStoreSample deleteTokens before " + col.count());
            BasicDBObject query = new BasicDBObject();
            query.put(EXPIRES, new BasicDBObject("$lt", timestamp));
            query.put(PROVIDERID, providerId);
            col.remove(query);
            System.out.println("CustomStoreSample deleteTokens after " + col.count());
        } catch (Exception e) {
            throw new OAuthStoreException("Failed on deleteTokens for time after " + timestamp, e);
        }
    }

    @Override
    public void deleteConsent(String providerId, String username, String clientId, String resource) throws OAuthStoreException {
        try {
            DBCollection col = getConsentCollection();
            BasicDBObject db = new BasicDBObject(CLIENTID, clientId);
            db.put(USERNAME, username);
            db.put(PROVIDERID, providerId);
            db.put(RESOURCE, resource);
            col.remove(db);

        } catch (Exception e) {
            throw new OAuthStoreException("Failed on delete for Consent for " + username, e);
        }

    }

    @Override
    public void deleteConsents(String providerId, long timestamp) throws OAuthStoreException {
        try {
            DBCollection col = getConsentCollection();
            BasicDBObject query = new BasicDBObject();
            query.put(EXPIRES, new BasicDBObject("$lt", timestamp));
            query.put(PROVIDERID, providerId);
            col.remove(query);
        } catch (Exception e) {
            throw new OAuthStoreException("Failed on deleteConsents for time after " + timestamp, e);
        }
    }

    private BasicDBObject createClientKeyHelper(OAuthClient oauthClient) {
        return createClientKeyHelper(oauthClient.getProviderId(), oauthClient.getClientId());
    }

    private BasicDBObject createClientKeyHelper(String providerId, String clientId) {
        BasicDBObject d = new BasicDBObject(CLIENTID, clientId);
        d.append(PROVIDERID, providerId);
        return d;
    }

    private BasicDBObject createTokenKeyHelper(OAuthToken oauthToken) {
        return createTokenKeyHelper(oauthToken.getProviderId(), oauthToken.getLookupKey());
    }

    private BasicDBObject createTokenKeyHelper(String providerId, String lookupKey) {
        BasicDBObject d = new BasicDBObject(LOOKUPKEY, lookupKey);
        d.append(PROVIDERID, providerId);
        return d;
    }

    private BasicDBObject createConsentKeyHelper(OAuthConsent oauthConsent) {
        return createConsentKeyHelper(oauthConsent.getClientId(), oauthConsent.getUser(), oauthConsent.getResource(),
                                      oauthConsent.getProviderId());
    }

    private BasicDBObject createConsentKeyHelper(String providerId, String username, String clientId, String resource) {
        BasicDBObject d = new BasicDBObject(CLIENTID, clientId);
        d.append(USERNAME, username);
        d.append(RESOURCE, resource);
        d.append(PROVIDERID, providerId);
        return d;
    }

    private boolean isNetworkFailure(Exception e) {
        System.out.println("CustomStoreSample isNetworkFailure processing for " + e);
        Throwable causeBy = e;
        while (causeBy != null) {
            if (causeBy instanceof IOException) {
                System.out.println("Hit an IOException: " + causeBy);
                return true;
            } else {
                System.out.println("Not an IOException: " + causeBy);
                causeBy = causeBy.getCause();
            }
        }
        return false;
    }
}
