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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth.test.MessageConstants;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientDBModel;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

import test.common.SharedOutputManager;

/**
 * Common Test
 */
public abstract class CachedDBOidcClientProviderTest extends AbstractOidcRegistrationBaseTest {
    protected static SharedOutputManager outputMgr;

    public static final String PROVIDER_NAME = "CachedOidcOP";
    private static final String JDBC_PROVIDER = "jdbc/OAuth2DB";
    private static final String SCHEMA = "OAuthDBSchema";
    private static final String TABLE_NAME = "OAUTH20CLIENTCONFIG";
    protected static final String SCHEMA_TABLE_NAME = SCHEMA + "." + TABLE_NAME;
    private static final String REQUEST_URL_STRING = "https://localhost:8020/oidc/endpoint/" + PROVIDER_NAME + "/registration";
    protected static final String[] EMPTY_STRING_ARR = new String[0];
    protected static List<OidcBaseClient> SAMPLE_CLIENTS = null;

    public interface MockInterface {
        void addClientToDB() throws SQLException, OidcServerException;

        OidcBaseClient getClientFromDB() throws SQLException, OidcServerException;

        OidcBaseClient getClientFromDBModel() throws SQLException;

        OidcBaseClient setDefaultFacade();

        OidcBaseClientDBModel getDBModelOfClient() throws SQLException;

        Collection<OidcBaseClient> findAllClientsFromDB() throws SQLException, OidcServerException;

        int update() throws OidcServerException, SQLException;

        boolean deleteClientFromDB() throws SQLException;

        Connection getInitializedConnection() throws OidcServerException;

        void encodeClientSecretInClientMetadata();
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    CachedDBOidcClientProvider mockProvider;

    final Connection connection = mockery.mock(Connection.class);
    final PreparedStatement preparedStatement = mockery.mock(PreparedStatement.class);
    final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    final OidcBaseClient baseClient = mockery.mock(OidcBaseClient.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setHash(false);
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InitialContextFactoryMock.class.getName());

        SAMPLE_CLIENTS = getsampleOidcBaseClients(5, PROVIDER_NAME);
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();

        InitialContextFactoryMock.inMemoryDbConn.close();
    }

    @Before
    abstract public void setupBefore();

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        System.out.println("Exiting test: " + _testName);
    }

    protected void instantiateMockProvider() {
        mockProvider = new CachedDBOidcClientProvider(PROVIDER_NAME, InitialContextFactoryMock.dsMock, SCHEMA_TABLE_NAME, null, null, EMPTY_STRING_ARR) {
            @Override
            void addClientToDB(Connection conn, OidcBaseClientDBModel clientDbModel) throws SQLException, OidcServerException {
                mockInterface.addClientToDB();
            }

            @Override
            OidcBaseClient getClientFromDB(Connection conn, String clientId, boolean removeHashInfo) throws SQLException, OidcServerException {
                return mockInterface.getClientFromDB();
            }

            @Override
            OidcBaseClient getClientFromDBModel(Connection conn, ResultSet queryResults, boolean removeHashInfo) throws SQLException {
                return mockInterface.getClientFromDBModel();
            }

            @Override
            OidcBaseClient setDefaultFacade(OidcBaseClientDBModel clientDBModel, OidcBaseClient client) {
                return mockInterface.setDefaultFacade();
            }

            @Override
            OidcBaseClientDBModel getDBModelOfClient(Connection conn, ResultSet queryResults, boolean removeHashInfo) throws SQLException {
                return mockInterface.getDBModelOfClient();
            }

            @Override
            Collection<OidcBaseClient> findAllClientsFromDB(Connection conn, HttpServletRequest request) throws SQLException, OidcServerException {
                return mockInterface.findAllClientsFromDB();
            }

            @Override
            int update(Connection conn, OidcBaseClientDBModel clientDbModel) throws OidcServerException, SQLException {
                return mockInterface.update();
            }

            @Override
            boolean deleteClientFromDB(Connection conn, String clientId) throws SQLException {
                return mockInterface.deleteClientFromDB();
            }

            @Override
            Connection getInitializedConnection() throws OidcServerException {
                return mockInterface.getInitializedConnection();
            }

            @Override
            void encodeClientSecretInClientMetadata(OidcBaseClientDBModel clientDbModel) {
                mockInterface.encodeClientSecretInClientMetadata();
            }
        };
    }

    protected void deleteAllClientsInDB(CachedDBOidcClientProvider oidcBaseClientProvider) throws OidcServerException {
        Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll();
        if (clients.size() > 0) {
            for (OidcBaseClient client : clients) {
                oidcBaseClientProvider.delete(client.getClientId());
            }
        }

        assertEquals("Task to clean db of all clients failed.", 0, oidcBaseClientProvider.getAll().size());
    }

    protected void insertSampleClientsToDb(CachedDBOidcClientProvider oidcBaseClientProvider) throws OidcServerException {
        for (int i = 0; i < SAMPLE_CLIENTS.size(); i++) {
            if (i == 0) {
                InitialContextFactoryMock.addEntryToOldOAuth20ClientConfigTable();
                OidcBaseClientDBModel initialClient = InitialContextFactoryMock.getInitializedOidcBaseClientModel();
                OidcBaseClient retrievedOidcBaseClient = oidcBaseClientProvider.get(initialClient.getClientId());
                assertClientEquals(initialClient, retrievedOidcBaseClient);
                continue;
            }

            OidcBaseClient storedOidcBaseClient = oidcBaseClientProvider.put(SAMPLE_CLIENTS.get(i));

            assertNotNull(storedOidcBaseClient);
            assertEqualsOidcBaseClients(SAMPLE_CLIENTS.get(i), storedOidcBaseClient);

            OidcBaseClient retrievedOidcBaseClient = oidcBaseClientProvider.get(SAMPLE_CLIENTS.get(i).getClientId());
            assertNotNull(retrievedOidcBaseClient);
            assertEqualsOidcBaseClients(SAMPLE_CLIENTS.get(i), retrievedOidcBaseClient);
        }
        assertEquals(SAMPLE_CLIENTS.size(), oidcBaseClientProvider.getAll().size());

    }

    private void assertEqualsSampleClients(Collection<OidcBaseClient> clients) {
        assertEquals(SAMPLE_CLIENTS.size(), clients.size());

        Map<String, OidcBaseClient> clientsMap = getOidcBaseClientMap(clients);

        int i = 0;
        for (OidcBaseClient client : clients) {
            if (i == 0) {
                assertClientEquals(InitialContextFactoryMock.getInitializedOidcBaseClientModel(), clientsMap.get(String.valueOf(i)));
                continue;
            }

            assertEqualsOidcBaseClients(SAMPLE_CLIENTS.get(i), client);
            i++;
        }
    }

    /**
     * Test initialization, loadClients and getAll via the 'initialize' function
     */
    @Test
    public void testInitializeAndGetAllClients() {
        CachedDBOidcClientProvider oidcBaseClientProvider = invokeConstructorAndInitialize();
        try {
            Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll();

            assertEquals(SAMPLE_CLIENTS.size(), clients.size());

            assertEqualsSampleClients(clients);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    /**
     * Test initialization, loadClients and getAll via the 'init(OAuthComponentConfiguration)' function
     */
    @Test
    public void testInitAndGetAllClients() {
        CachedDBOidcClientProvider oidcBaseClientProvider = new CachedDBOidcClientProvider(PROVIDER_NAME, InitialContextFactoryMock.dsMock, SCHEMA_TABLE_NAME, null, null, EMPTY_STRING_ARR);
        final String[] emptyStringArr = new String[0];
        final OAuthComponentConfiguration config = mockery.mock(OAuthComponentConfiguration.class);
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(config).getConfigPropertyValue(OAuthJDBCImpl.CONFIG_JDBC_PROVIDER);
                    will(returnValue(JDBC_PROVIDER));
                    allowing(config).getConfigPropertyValue(OAuthJDBCImpl.CONFIG_PROVIDER_NAME);
                    will(returnValue(PROVIDER_NAME));
                    allowing(config).getConfigPropertyValue(OAuthJDBCImpl.CONFIG_CLIENT_TABLE);
                    will(returnValue(SCHEMA_TABLE_NAME));
                    allowing(config).getUniqueId();
                    will(returnValue(PROVIDER_NAME));
                    allowing(config).getConfigPropertyValues(Constants.CLIENT_URI_SUBSTITUTIONS);
                    will(returnValue(emptyStringArr));
                }
            });

            oidcBaseClientProvider.init(config);

            Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll();
            assertEquals(SAMPLE_CLIENTS.size(), clients.size());
            assertEqualsSampleClients(clients);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testPutAndGet() {
        CachedDBOidcClientProvider oidcBaseClientProvider = invokeConstructorAndInitialize();
        try {
            deleteAllClientsInDB(oidcBaseClientProvider);

            OidcBaseClient newClient = SAMPLE_CLIENTS.get(1);
            OidcBaseClient storedOidcBaseClient = oidcBaseClientProvider.put(newClient);

            assertNotNull(storedOidcBaseClient);
            assertEqualsOidcBaseClients(newClient, storedOidcBaseClient);

            OidcBaseClient retrievedOidcBaseClient = oidcBaseClientProvider.get(newClient.getClientId());
            assertNotNull(retrievedOidcBaseClient);
            assertEqualsOidcBaseClients(newClient, retrievedOidcBaseClient);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testPut_SQLException() {
        try {
            final String clientId = "someClientId";
            dBMigratorExecuteExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getInitializedConnection();
                    will(returnValue(connection));
                    allowing(mockInterface).addClientToDB();
                    will(throwException(new SQLException(defaultExceptionMsg)));
                    allowing(baseClient).getClientId();
                    will(returnValue(clientId));
                    one(connection).getAutoCommit();
                    will(returnValue(true));
                    one(connection).close();
                }
            });

            try {
                OidcBaseClient result = mockProvider.put(baseClient);
                fail("Should have thrown exception but did not. Result was " + result);
            } catch (OidcServerException e) {
                String operation = "INSERT";
                verifyExceptionPerformingDBOperationOidc(e, operation, clientId);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testGet() {
        try {
            final String clientId = "someClientId";
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getInitializedConnection();
                    will(returnValue(connection));
                    one(mockInterface).getClientFromDB();
                    will(returnValue(baseClient));
                    one(connection).getAutoCommit();
                    will(returnValue(true));
                    one(connection).close();
                }
            });

            OidcBaseClient result = mockProvider.get(clientId);
            assertEquals("Result did not match expected value.", baseClient, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testGet_SQLException() {
        try {
            final String clientId = "someClientId";
            dBMigratorExecuteExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getInitializedConnection();
                    will(returnValue(connection));
                    allowing(mockInterface).getClientFromDB();
                    will(throwException(new SQLException(defaultExceptionMsg)));
                    allowing(baseClient).getClientId();
                    will(returnValue(clientId));
                    one(connection).getAutoCommit();
                    will(returnValue(true));
                    one(connection).close();
                }
            });

            try {
                OidcBaseClient result = mockProvider.get(clientId);
                fail("Should have thrown exception but did not. Result was " + result);
            } catch (OidcServerException e) {
                String operation = "SELECT";
                verifyExceptionPerformingDBOperationOidc(e, operation, clientId);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testGetAll() {
        CachedDBOidcClientProvider oidcBaseClientProvider = invokeConstructorAndInitialize();
        try {
            // The @Before test should have hydrated the DB with SAMPLE_CLIENTS
            assertEqualsSampleClients(oidcBaseClientProvider.getAll());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testGetAll_SQLException() {
        try {
            dBMigratorExecuteExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getInitializedConnection();
                    will(returnValue(connection));
                    allowing(mockInterface).findAllClientsFromDB();
                    will(throwException(new SQLException(defaultExceptionMsg)));
                    one(connection).getAutoCommit();
                    will(returnValue(true));
                    one(connection).close();
                }
            });

            try {
                Collection<OidcBaseClient> result = mockProvider.getAll();
                fail("Should have thrown exception but did not. Result was " + result);
            } catch (OidcServerException e) {
                verifyExceptionGettingAllClientsOidc(e);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testGetAllRequest() {
        CachedDBOidcClientProvider oidcBaseClientProvider = invokeConstructorAndInitialize();

        try {
            mockery.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(new StringBuffer(REQUEST_URL_STRING)));
                }
            });

            Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll(request);

            assertEquals(SAMPLE_CLIENTS.size(), clients.size());

            Map<String, OidcBaseClient> clientsMap = getOidcBaseClientMap(clients);

            // Ensure the clients from the client provider match what was expected (the sample clients)
            // while adding the modification to check for expected registration uri based on mocked request
            for (int i = 0; i < SAMPLE_CLIENTS.size(); i++) {
                OidcBaseClient clientProviderClient = clientsMap.remove(String.valueOf(i));

                if (i > 0) {
                    assertEqualsOidcBaseClients(SAMPLE_CLIENTS.get(i), clientProviderClient);
                } else {
                    assertClientEquals(InitialContextFactoryMock.getInitializedOidcBaseClientModel(), clientProviderClient);
                }

                // Add check for checking registration URI which isn't in assertEqualsOidcBaseClients
                assertEquals(clientProviderClient.getRegistrationClientUri(), REQUEST_URL_STRING + "/" + clientProviderClient.getClientId());
            }

            assertEquals(clientsMap.size(), 0);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testGetAllRequest_SQLException() {
        try {
            dBMigratorExecuteExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getInitializedConnection();
                    will(returnValue(connection));
                    allowing(mockInterface).findAllClientsFromDB();
                    will(throwException(new SQLException(defaultExceptionMsg)));
                    one(connection).getAutoCommit();
                    will(returnValue(true));
                    one(connection).close();
                }
            });

            try {
                Collection<OidcBaseClient> result = mockProvider.getAll(request);
                fail("Should have thrown exception but did not. Result was " + result);
            } catch (OidcServerException e) {
                verifyExceptionGettingAllClientsOidc(e);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testExists() {
        CachedDBOidcClientProvider oidcBaseClientProvider = invokeConstructorAndInitialize();
        try {
            for (OidcBaseClient expectedClient : SAMPLE_CLIENTS) {
                assertTrue(oidcBaseClientProvider.exists(expectedClient.getClientId()));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testExists_SQLException() {
        try {
            dBMigratorExecuteExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getInitializedConnection();
                    will(returnValue(connection));
                    allowing(mockInterface).getClientFromDB();
                    will(throwException(new SQLException(defaultExceptionMsg)));
                    one(connection).getAutoCommit();
                    will(returnValue(true));
                    one(connection).close();
                }
            });

            final String clientId = "someClientId";
            try {
                boolean result = mockProvider.exists(clientId);
                fail("Should have thrown exception but did not. Result was " + result);
            } catch (OidcServerException e) {
                String operation = "SELECT";
                verifyExceptionPerformingDBOperationOidc(e, operation, clientId);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testValidateClient() {
        CachedDBOidcClientProvider oidcBaseClientProvider = invokeConstructorAndInitialize();
        try {
            for (OidcBaseClient sampleClient : SAMPLE_CLIENTS) {
                assertTrue("The client must be valid.",
                        oidcBaseClientProvider.validateClient(sampleClient.getClientId(), sampleClient.getClientSecret()));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testValidateClient_SQLException() {
        try {
            dBMigratorExecuteExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getInitializedConnection();
                    will(returnValue(connection));
                    allowing(mockInterface).getClientFromDB();
                    will(throwException(new SQLException(defaultExceptionMsg)));
                    one(connection).getAutoCommit();
                    will(returnValue(true));
                    one(connection).close();
                }
            });

            final String clientId = "someClientId";
            final String clientPwd = "someClientIdPwd";
            try {
                boolean result = mockProvider.validateClient(clientId, clientPwd);
                fail("Should have thrown exception but did not. Result was " + result);
            } catch (OidcServerException e) {
                String operation = "SELECT";
                verifyExceptionPerformingDBOperationOidc(e, operation, clientId);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testUpdate() {
        CachedDBOidcClientProvider oidcBaseClientProvider = invokeConstructorAndInitialize();

        try {
            Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll();

            // New scope must contain the preauthorized scope or validator will clear the preauthorized scope value
            String newScopes = "profile picture email";

            // Update each client with new scope...
            for (OidcBaseClient client : clients) {
                client.setScope(newScopes);
                oidcBaseClientProvider.update(client);
            }

            Collection<OidcBaseClient> updatedClients = oidcBaseClientProvider.getAll();
            Map<String, OidcBaseClient> updatedClientsMap = getOidcBaseClientMap(updatedClients);
            Map<String, OidcBaseClient> sampleClientMap = getOidcBaseClientMap(SAMPLE_CLIENTS);

            for (int i = 0; i < SAMPLE_CLIENTS.size(); i++) {
                OidcBaseClient updatedClientProviderClient = updatedClientsMap.remove(String.valueOf(i));
                OidcBaseClient sampleClient = sampleClientMap.remove(String.valueOf(i)).getDeepCopy();

                // Set new scope on sampleClient before comparison
                sampleClient.setScope(newScopes);

                if (i > 0) {
                    assertEqualsOidcBaseClients(sampleClient, updatedClientProviderClient);
                } else {
                    assertEquals(sampleClient.getScope(), updatedClientProviderClient.getScope());
                    assertClientEquals(InitialContextFactoryMock.getInitializedOidcBaseClientModel(), updatedClientProviderClient);
                }
            }

            assertEquals(updatedClientsMap.size(), 0);
            assertEquals(sampleClientMap.size(), 0);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testUpdate_SQLException() {
        try {
            final String clientId = "someClientId";
            dBMigratorExecuteExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getInitializedConnection();
                    will(returnValue(connection));
                    allowing(mockInterface).update();
                    will(throwException(new SQLException(defaultExceptionMsg)));
                    allowing(baseClient).getClientId();
                    will(returnValue(clientId));
                    one(connection).getAutoCommit();
                    will(returnValue(true));
                    one(connection).close();
                }
            });

            try {
                OidcBaseClient result = mockProvider.update(baseClient);
                fail("Should have thrown exception but did not. Result was " + result);
            } catch (OidcServerException e) {
                String operation = "UPDATE";
                verifyExceptionPerformingDBOperationOidc(e, operation, clientId);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testDelete() {
        CachedDBOidcClientProvider oidcBaseClientProvider = invokeConstructorAndInitialize();

        try {
            Collection<OidcBaseClient> clients = oidcBaseClientProvider.getAll();
            assertEquals(SAMPLE_CLIENTS.size(), clients.size());

            for (OidcBaseClient client : clients) {
                assertTrue(oidcBaseClientProvider.delete(client.getClientId()));
            }

            Collection<OidcBaseClient> reRetrievedClients = oidcBaseClientProvider.getAll();
            assertEquals(0, reRetrievedClients.size());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    @Test
    public void testDelete_SQLException() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getInitializedConnection();
                    will(returnValue(connection));
                    one(mockInterface).deleteClientFromDB();
                    will(throwException(new SQLException(defaultExceptionMsg)));
                    one(connection).getAutoCommit();
                    will(returnValue(true));
                    one(connection).close();
                }
            });

            final String clientId = "someClientId";
            try {
                boolean result = mockProvider.delete(clientId);
                fail("Should have thrown exception but did not. Result was " + result);
            } catch (OidcServerException e) {
                String operation = "DELETE";
                verifyExceptionPerformingDBOperationOidc(e, operation, clientId);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(_testName, t);
        }
    }

    /*************************************** Helper methods ***************************************/

    abstract protected CachedDBOidcClientProvider invokeConstructorAndInitialize();

    private void assertClientEquals(OidcBaseClientDBModel expectedClient, OidcBaseClient retrievedClient) {
        // Ensure client retrieve is client expected
        assertEquals(expectedClient.getComponentId(), retrievedClient.getComponentId());
        assertEquals(expectedClient.getClientId(), retrievedClient.getClientId());
        assertEquals(expectedClient.getClientSecret(), retrievedClient.getClientSecret());
        assertEquals(expectedClient.getDisplayName(), retrievedClient.getClientName());
        assertEquals(expectedClient.getRedirectUri(), retrievedClient.getRedirectUris().get(0).getAsString()); // We know only 1 redirect URI was set
        assertEquals(expectedClient.getEnabled(), retrievedClient.isEnabled() ? 1 : 0);
    }

    private void dBMigratorExecuteExpectations() throws SQLException {
        mockery.checking(new Expectations() {
            {
                one(connection).prepareStatement(with(any(String.class)));
                will(returnValue(preparedStatement));
                one(preparedStatement).execute();
                one(preparedStatement).close();
            }
        });
    }

    /**
     * Verifies that CWWKS1460E message appears in the exception message and messages.log.
     */
    void verifyExceptionPerformingDBOperationOidc(OidcServerException e, String operation, final String clientId) {
        String msgRegex = MessageConstants.CWWKS1460E_ERROR_PERFORMING_DB_OPERATION + ".+" + operation + ".+" + clientId;
        verifyExceptionAndLogMessagesOidc(e, msgRegex);
    }

    /**
     * Verifies that CWWKS1461E message appears in the exception message and messages.log.
     */
    void verifyExceptionGettingAllClientsOidc(OidcServerException e) {
        String msgRegex = MessageConstants.CWWKS1461E_ERROR_GETTING_CLIENTS_FROM_DB;
        verifyExceptionAndLogMessagesOidc(e, msgRegex);
    }

    /**
     * Verifies that the exception contains the expected NLS message, but without the exception message. Also verifies that the
     * appropriate NLS message is logged and includes the exception message.
     */
    void verifyExceptionAndLogMessages(Exception e, String msgRegex) {
        verifyException(e, msgRegex);
        assertFalse("Exception message should not have contained SQL exception message but did.", e.getLocalizedMessage().contains(defaultExceptionMsg));
        verifyLogMessage(outputMgr, msgRegex + ".+" + Pattern.quote(defaultExceptionMsg));
    }

    void verifyExceptionAndLogMessagesOidc(OidcServerException e, String msgRegex) {
        verifyExceptionString(e.getErrorDescription(), msgRegex);
        assertFalse("Exception message should not have contained SQL exception message but did.", e.getErrorDescription().contains(defaultExceptionMsg));
        verifyLogMessage(outputMgr, msgRegex + ".+" + Pattern.quote(defaultExceptionMsg));
    }

}
