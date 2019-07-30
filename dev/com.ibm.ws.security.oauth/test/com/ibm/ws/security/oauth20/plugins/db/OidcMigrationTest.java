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

import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.TestCase;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

/**
 *
 */
public class OidcMigrationTest extends TestCase {
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

    private static final String QUERY_ADD_DEFAULT_TBL_OAUTH20_CLIENT_CONFIG =
                    "CREATE TABLE " + SCHEMA_TABLE_NAME + " ("
                                    + "COMPONENTID VARCHAR(256) NOT NULL,"
                                    + "CLIENTID VARCHAR(256) NOT NULL,"
                                    + "CLIENTSECRET VARCHAR(256),"
                                    + "DISPLAYNAME VARCHAR(256) NOT NULL,"
                                    + "REDIRECTURI VARCHAR(2048),"
                                    + "ENABLED INT"
                                    + ")";

    private static final String CLIENT_CONFIG_PARAMS = String.format(" (%s, %s, %s, %s, %s, %s)",
                                                                     COL_COMPONENTID,
                                                                     COL_CLIENTID,
                                                                     COL_CLIENTSECRET,
                                                                     COL_DISPLAYNAME,
                                                                     COL_REDIRECTURI,
                                                                     COL_ENABLED);

    private static final String QUERY_CONSTRAINT =
                    "ALTER TABLE " + SCHEMA_TABLE_NAME + " ADD CONSTRAINT PK_COMPIDCLIENTID PRIMARY KEY (COMPONENTID,CLIENTID)";

    private static final String QUERY_SCHEMA_TABLE = "SELECT * FROM " + SCHEMA_TABLE_NAME;

    private static final Connection inMemoryDbConn = getInMemoryDbConn();

    @Test
    public void testMigration() {
        executeSchemaUpdate();
        verifyUpdatedSchemaContainsEmptyJsonObjectInClientMetadata();
        verifyExceptionEvent();

    }

    private void executeSchemaUpdate() {
        //Create Empty Table
        initializeDefaultOAuth20ClientConfigTable();

        //Add One Client Entry
        addEntryToOldOAuth20ClientConfigTable();

        //Assert not CLIENTMETADATA field already exists
        assertFalse("Column CLIENTMETADATA should not exist. Test setup failure.", dbHasClientMetadataColumn());

        //Attempt migration, which adds a CLIENTMETDATA column
        DynamicDBMigrator migration = new DynamicDBMigrator(SCHEMA_TABLE_NAME);
        migration.execute(inMemoryDbConn);
        assertTrue("The migration implementation code did not upgrade the database correctly. ", dbHasClientMetadataColumn());

    }

    public void verifyUpdatedSchemaContainsEmptyJsonObjectInClientMetadata() {
        Statement stQueryTable = null;
        try {
            stQueryTable = inMemoryDbConn.createStatement();
            ResultSet rset = stQueryTable.executeQuery(QUERY_SCHEMA_TABLE);

            boolean emptyClientMetadataJsonCheckOccurred = false;
            if (rset != null && rset.next()) {
                Clob clob = rset.getClob(COL_CLIENTMETADATA);
                JsonObject clientMetadata = (new JsonParser()).parse(clob.getCharacterStream()).getAsJsonObject();
                assertTrue(clientMetadata.entrySet().size() == 0);
                emptyClientMetadataJsonCheckOccurred = true;
            }

            assertTrue("Test setup failure: No existing client entry was found.", emptyClientMetadataJsonCheckOccurred);
        } catch (SQLException e) {
            fail("Error trying to detect presence of column " + COL_CLIENTMETADATA + ": " + e.getLocalizedMessage());
        } finally {
            closeStatement(stQueryTable);
        }
    }

    public void verifyExceptionEvent() {
        try {
            //Attempt migration, which adds a CLIENTMETDATA column
            DynamicDBMigrator migration = new DynamicDBMigrator(SCHEMA_TABLE_NAME);
            inMemoryDbConn.close();
            migration.execute(inMemoryDbConn);
        } catch (SQLException e) {
            fail("Test failure, unable to close inMemoryDbConn.");
        }
    }

    private boolean dbHasClientMetadataColumn() {
        Statement stQueryTable = null;
        try {
            stQueryTable = inMemoryDbConn.createStatement();
            ResultSet rset = stQueryTable.executeQuery(QUERY_SCHEMA_TABLE);
            ResultSetMetaData md = rset.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++)
            {
                if (md.getColumnLabel(i).equals(COL_CLIENTMETADATA)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            fail("Error trying to detect presence of column " + COL_CLIENTMETADATA + ": " + e.getLocalizedMessage());
        } finally {
            closeStatement(stQueryTable);
        }

        return false;
    }

    private void initializeDefaultOAuth20ClientConfigTable() {
        Statement stCreateTable = null;
        Statement stConstraint = null;
        try {
            //Initialize default OAuth20 Client Config Table, which lacks the CLIENTMETADATA clob column
            stCreateTable = inMemoryDbConn.createStatement();
            stCreateTable.execute(QUERY_ADD_DEFAULT_TBL_OAUTH20_CLIENT_CONFIG);

            //Adjust constraints
            stConstraint = inMemoryDbConn.createStatement();
            stConstraint.execute(QUERY_CONSTRAINT);
        } catch (SQLException e) {
            fail("Error setting up in-memory derby database: " + e.getLocalizedMessage());
        } finally {
            closeStatement(stCreateTable);
            closeStatement(stConstraint);
        }
    }

    private void addEntryToOldOAuth20ClientConfigTable() {
        PreparedStatement stInsert = null;

        String QUERY_INSERT = "INSERT INTO " + SCHEMA_TABLE_NAME
                              + CLIENT_CONFIG_PARAMS
                              + " VALUES ( ?, ?, ?, ?, ?, ? )";

        try {
            stInsert = inMemoryDbConn.prepareStatement(QUERY_INSERT);
            stInsert.setString(1, AbstractOidcRegistrationBaseTest.COMPONENT_ID);
            stInsert.setString(2, AbstractOidcRegistrationBaseTest.CLIENT_ID);
            stInsert.setString(3, AbstractOidcRegistrationBaseTest.CLIENT_SECRET);
            stInsert.setString(4, AbstractOidcRegistrationBaseTest.CLIENT_NAME);
            stInsert.setString(5, AbstractOidcRegistrationBaseTest.REDIRECT_URI_1);
            stInsert.setInt(6, AbstractOidcRegistrationBaseTest.IS_ENABLED ? 1 : 0);

            stInsert.executeUpdate();
        } catch (SQLException e) {
            fail("Error adding an entry into the defailt OAuth20ClientConfig table: " + e.getLocalizedMessage());
        } finally {
            closeStatement(stInsert);
        }

    }

    private void closeStatement(Statement st) {
        try {
            if (st != null) {
                st.close();
            }
        } catch (SQLException e) {
            fail("Error closing statement: " + e.getLocalizedMessage());
        }
    }

    private static Connection getInMemoryDbConn() {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String connectionURL = "jdbc:derby:memory:testDB;create=true";

        Connection conn = null;

        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(connectionURL);
        } catch (Exception e) {
            fail("Error setting up in-memory derby database");
        }

        return conn;
    }
}