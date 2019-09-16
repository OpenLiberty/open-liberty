/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.db;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;

import com.google.gson.JsonObject;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientDBModel;
import com.ibm.ws.security.oauth20.util.HashSecretUtils;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

/**
 *
 */
public class InitialContextFactoryMock implements InitialContextFactory {

    public InitialContextFactoryMock() {
    }

    private static final Mockery mockCtx = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String SCHEMA = "OAuthDBSchema";
    private static final String TABLE_NAME = "OAUTH20CLIENTCONFIG";
    private static final String SCHEMA_TABLE_NAME = SCHEMA + "." + TABLE_NAME;

    private static final String COL_CLIENTMETADATA = "CLIENTMETADATA";
    private static final String COL_COMPONENTID = "COMPONENTID";
    private static final String COL_CLIENTID = "CLIENTID";
    private static final String COL_CLIENTSECRET = "CLIENTSECRET";
    private static final String COL_DISPLAYNAME = "DISPLAYNAME";
    private static final String COL_REDIRECTURI = "REDIRECTURI";
    private static final String COL_ENABLED = "ENABLED";

    private static final String QUERY_ADD_DEFAULT_TBL_OAUTH20_CLIENT_CONFIG = "CREATE TABLE " + SCHEMA_TABLE_NAME + " ("
            + "COMPONENTID VARCHAR(256) NOT NULL,"
            + "CLIENTID VARCHAR(256) NOT NULL,"
            + "CLIENTSECRET VARCHAR(256),"
            + "DISPLAYNAME VARCHAR(256) NOT NULL,"
            + "REDIRECTURI VARCHAR(2048),"
            + "ENABLED INT,"
            + "CLIENTMETADATA CLOB NOT NULL DEFAULT '{}'"
            + ")";

    private static final String CLIENT_CONFIG_PARAMS = String.format(" (%s, %s, %s, %s, %s, %s, %s)",
            COL_COMPONENTID,
            COL_CLIENTID,
            COL_CLIENTSECRET,
            COL_DISPLAYNAME,
            COL_REDIRECTURI,
            COL_ENABLED,
            COL_CLIENTMETADATA);

    private static final String QUERY_CONSTRAINT = "ALTER TABLE " + SCHEMA_TABLE_NAME + " ADD CONSTRAINT PK_COMPIDCLIENTID PRIMARY KEY (COMPONENTID,CLIENTID)";

    // private static final String QUERY_SCHEMA_TABLE = "SELECT * FROM " + SCHEMA_TABLE_NAME;

    private static InitialContext ctx = null;
    static final DataSource dsMock = mockCtx.mock(DataSource.class);
    protected static UnclosableConnection inMemoryDbConn = null;

    static {
        try {
            inMemoryDbConn = new UnclosableConnection(getInMemoryDbConn());
            initializeDefaultOAuth20ClientConfigTable();
            // addEntryToOldOAuth20ClientConfigTable();

            mockCtx.checking(new Expectations() {
                {
                    allowing(dsMock).getConnection();
                    will(returnValue(inMemoryDbConn));
                }
            });

            ctx = new InitialContext(true) {
                Map<String, Object> bindings = new HashMap<String, Object>();

                @Override
                public void bind(String name, Object obj) throws NamingException {
                    bindings.put(name, obj);
                }

                @Override
                public Object lookup(String name) throws NamingException {
                    return dsMock;
                }
            };
        } catch (SQLException e) {
            fail("Error in InitialContextFactoryMock.class for Test Setup of In-Memory Derby DB: " + e);
        } catch (NamingException e) {
            fail("Error in InitialContextFactoryMock.class for Test Setup of JNDI context: " + e);
        } catch (ClassNotFoundException e) {
            fail("Error in InitialContextFactoryMock.class for acquirign In-Memory Derby DB Drivers: " + e);
        }

    }

    @Override
    public InitialContext getInitialContext(Hashtable<?, ?> env) throws NamingException {
        return ctx;
    }

    public static void bind(String name, Object obj) {
        try {
            ctx.bind(name, obj);
        } catch (NamingException e) { // Won't encounter
            throw new RuntimeException(e);
        }
    }

    protected static void initializeDefaultOAuth20ClientConfigTable() {
        Statement stCreateTable = null;
        Statement stConstraint = null;
        try {
            // Initialize default OAuth20 Client Config Table, which lacks the CLIENTMETADATA clob column
            stCreateTable = inMemoryDbConn.createStatement();
            stCreateTable.execute(QUERY_ADD_DEFAULT_TBL_OAUTH20_CLIENT_CONFIG);

            // Adjust constraints
            stConstraint = inMemoryDbConn.createStatement();
            stConstraint.execute(QUERY_CONSTRAINT);
        } catch (SQLException e) {
            fail("Error setting up in-memory derby database: " + e.getLocalizedMessage());
        } finally {
            closeStatement(stCreateTable);
            closeStatement(stConstraint);
        }
    }

    public static OidcBaseClientDBModel getInitializedOidcBaseClientModel() {
        OidcBaseClient tmpClient = AbstractOidcRegistrationBaseTest.getsampleOidcBaseClients(1, CachedDBOidcClientProviderTest.PROVIDER_NAME).get(0);

        JsonObject clientMetadataAsJson = OidcOAuth20Util.getJsonObj(tmpClient);

        return new OidcBaseClientDBModel(tmpClient.getComponentId(),
                tmpClient.getClientId(),
                tmpClient.getClientSecret(),
                tmpClient.getClientName(),
                tmpClient.getRedirectUris().get(0).getAsString(), // Only taking first redirect uri, for use in old DB schema
                tmpClient.isEnabled() ? 1 : 0,
                clientMetadataAsJson);
    }

    protected static void addEntryToOldOAuth20ClientConfigTable() {
        PreparedStatement stInsert = null;

        String QUERY_INSERT = "INSERT INTO " + SCHEMA_TABLE_NAME
                + CLIENT_CONFIG_PARAMS
                + " VALUES ( ?, ?, ?, ?, ?, ?, ? )";

        OidcBaseClientDBModel sampleClientForInitialization = getInitializedOidcBaseClientModel();

        String clientSecret = sampleClientForInitialization.getClientSecret();
        if (AbstractOidcRegistrationBaseTest.isHash) {
            HashSecretUtils.hashClientMetaTypeSecret(sampleClientForInitialization.getClientMetadata(), sampleClientForInitialization.getClientId(), true);
            clientSecret = HashSecretUtils.hashSecret(clientSecret, sampleClientForInitialization.getClientId(), true, sampleClientForInitialization.getClientMetadata());
        } else {
            clientSecret = PasswordUtil.passwordEncode(clientSecret);
            sampleClientForInitialization.getClientMetadata().addProperty(OAuth20Constants.CLIENT_SECRET, clientSecret);
        }

        try {
            stInsert = inMemoryDbConn.prepareStatement(QUERY_INSERT);
            stInsert.setString(1, sampleClientForInitialization.getComponentId());
            stInsert.setString(2, sampleClientForInitialization.getClientId());
            stInsert.setString(3, clientSecret);
            stInsert.setString(4, sampleClientForInitialization.getDisplayName());
            stInsert.setString(5, sampleClientForInitialization.getRedirectUri());
            stInsert.setInt(6, sampleClientForInitialization.getEnabled());
            stInsert.setString(7, sampleClientForInitialization.getClientMetadata().toString());

            stInsert.executeUpdate();
        } catch (SQLException e) {
            fail("Error adding an entry into the default OAuth20ClientConfig table: " + e.getLocalizedMessage());
        } finally {
            closeStatement(stInsert);
        }

    }

    private static void closeStatement(Statement st) {
        try {
            if (st != null) {
                st.close();
            }
        } catch (SQLException e) {
            fail("Error closing statement: " + e.getLocalizedMessage());
        }
    }

    private static Connection getInMemoryDbConn() throws ClassNotFoundException, SQLException {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String connectionURL = "jdbc:derby:memory:testOidcCachedDB;create=true";

        Connection conn = null;

        Class.forName(driver);
        conn = DriverManager.getConnection(connectionURL);

        return conn;
    }
}
