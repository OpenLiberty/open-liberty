/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.database.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ibm.websphere.simplicity.config.AuthData;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.Fileset;
import com.ibm.websphere.simplicity.config.JavaPermission;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ServerConfigurationFactory;
import com.ibm.websphere.simplicity.config.dsprops.Properties;
import com.ibm.websphere.simplicity.config.dsprops.Properties_postgresql;

import componenttest.app.JavaInfo;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests the DatabaseContainerUtil (soon to be renamed DatabaseContainerProperties) class
 */
public class DatabaseContainerPropertiesTest {

    @Mock
    LibertyServer server;

    @Mock
    PostgreSQLContainer cont;

    @Mock
    DerbyJava17PlusContainer cont2;

    private static Path driverDir;

    @BeforeClass
    public static void setup() throws Exception {
        // Create temporary mock jars to represent the jdbc drivers and
        // supporting libraries for the containers under test.
        driverDir = createJdbcDriverDirectory();

        DatabaseContainerType.Postgres.streamAllArtifacts()
                        .forEach(name -> {
                            try {
                                createFakeDrivers(driverDir, "jdbc", name);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

        DatabaseContainerType.DerbyJava17Plus.streamAllArtifacts()
                        .forEach(name -> {
                            try {
                                createFakeDrivers(driverDir, "jdbcJava17Plus", name);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
    }

    @AfterClass
    public static void teardown() throws Exception {
        // Clean up temporary files
        Files.walk(driverDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                //ignore
                            }
                        });
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        when(cont.getHost()).thenReturn("localhost");
        when(cont.getFirstMappedPort()).thenReturn(9999);
        when(cont.getDatabaseName()).thenReturn("TEST");
        when(cont.getUsername()).thenReturn("TEST_USER");
        when(cont.getPassword()).thenReturn("TEST_PASS");

        when(cont2.getUsername()).thenReturn("TEST_USER");
        when(cont2.getUsername()).thenReturn("TEST_PASS");
    }

    @Test
    public void findAuthDataLocationsTest() throws Exception {

        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("authDataServer.xml"));

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont);

        Set<AuthData> ads = (Set<AuthData>) getFindAuthDataLocations().invoke(inst, server.getServerConfiguration().getDataSources().getById("DefaultDataSource"));

        assertEquals(4, ads.size());

        for (AuthData ad : ads) {
            if (ad.getId() == null) {
                assertEquals("myUser3", ad.getUser());
                assertEquals("myPass3", ad.getPassword());
                continue;
            }

            if (ad.getId().equals("recoveryAuth")) {
                assertEquals("myUser1", ad.getUser());
                assertEquals("myPass1", ad.getPassword());
                continue;
            }

            if (ad.getId().equals("userAuth")) {
                assertEquals("myUser2", ad.getUser());
                assertEquals("myPass2", ad.getPassword());
                continue;
            }

            if (ad.getId().equals("contAuth")) {
                assertEquals("myUser4", ad.getUser());
                assertEquals("myPass4", ad.getPassword());
                continue;
            }

            fail("Found auth data that should not have been present in findAuthDataLocations: " + ad.toString());
        }
    }

    @Test
    public void withDriverReplacementTest() throws Exception {
        assumeTrue(!System.getProperty("os.name").equalsIgnoreCase("OS/400"));

        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("libraryServer.xml"));

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont)
                        .withDriverReplacement();

        ArgumentCaptor<ServerConfiguration> capturedConfig = ArgumentCaptor.forClass(ServerConfiguration.class);
        doNothing().when(server).updateServerConfiguration(capturedConfig.capture());

        inst.modify();

        ServerConfiguration result = capturedConfig.getValue();

        assertNotNull(result.getLibraries().getById("JDBCLibrary"));

        Library jdbcLib = result.getLibraries().getById("JDBCLibrary");
        assertEquals(1, jdbcLib.getFilesets().size());

        Fileset jdbcFs = jdbcLib.getFilesets().get(0);
        assertEquals("${shared.resource.dir}/jdbc", jdbcFs.getDir());
        assertEquals(DatabaseContainerType.Postgres.getDriverName(), jdbcFs.getIncludes());
    }

    @Test
    public void withDriverReplacementAdditionsTest() throws Exception {
        assumeTrue(!System.getProperty("os.name").equalsIgnoreCase("OS/400"));
        assumeTrue(JavaInfo.majorVersion() >= 17);

        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("libraryServerJava17Plus.xml"));

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont2)
                        .withDriverReplacement();

        ArgumentCaptor<ServerConfiguration> capturedConfig = ArgumentCaptor.forClass(ServerConfiguration.class);
        doNothing().when(server).updateServerConfiguration(capturedConfig.capture());

        inst.modify();

        ServerConfiguration result = capturedConfig.getValue();

        assertNotNull(result.getLibraries().getById("JDBCLibrary"));

        Library jdbcLib = result.getLibraries().getById("JDBCLibrary");
        assertEquals(1, jdbcLib.getFilesets().size());

        Fileset jdbcFs = jdbcLib.getFilesets().get(0);
        assertEquals("${shared.resource.dir}/jdbcJava17Plus", jdbcFs.getDir());
        assertEquals(3, jdbcFs.getIncludes().split(",").length); //All three jars were set
    }

    @Test
    public void withPermissionReplacementTest() throws Exception {
        assumeTrue(!System.getProperty("os.name").equalsIgnoreCase("OS/400"));

        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("libraryServer.xml"));
        when(server.isJava2SecurityEnabled()).thenReturn(true);

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont)
                        .withPermissionReplacement();

        ArgumentCaptor<ServerConfiguration> capturedConfig = ArgumentCaptor.forClass(ServerConfiguration.class);
        doNothing().when(server).updateServerConfiguration(capturedConfig.capture());

        inst.modify();

        ServerConfiguration result = capturedConfig.getValue();

        assertEquals(1, result.getJavaPermissions().size());

        JavaPermission permission = result.getJavaPermissions().get(0);
        assertEquals("java.security.AllPermission", permission.getClassName());
        assertEquals("${shared.resource.dir}/jdbc/" + DatabaseContainerType.Postgres.getDriverName(), permission.getCodeBase());
    }

    @Test
    public void withPermissionReplacementSkipped() throws Exception {
        assumeTrue(!System.getProperty("os.name").equalsIgnoreCase("OS/400"));

        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("libraryServer.xml"));
        when(server.isJava2SecurityEnabled()).thenReturn(false);

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont)
                        .withPermissionReplacement();

        ArgumentCaptor<ServerConfiguration> capturedConfig = ArgumentCaptor.forClass(ServerConfiguration.class);
        doNothing().when(server).updateServerConfiguration(capturedConfig.capture());

        inst.modify();

        ServerConfiguration result = capturedConfig.getValue();

        assertEquals(1, result.getJavaPermissions().size());

        JavaPermission permission = result.getJavaPermissions().get(0);
        assertEquals("java.security.AllPermission", permission.getClassName());
        assertEquals("${shared.resource.dir}/jdbc/${env.DB_DRIVER}", permission.getCodeBase());
    }

    @Test
    public void withPermissionAdditionsTest() throws Exception {
        assumeTrue(!System.getProperty("os.name").equalsIgnoreCase("OS/400"));
        assumeTrue(JavaInfo.majorVersion() >= 17);

        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("libraryServerJava17Plus.xml"));
        when(server.isJava2SecurityEnabled()).thenReturn(true);

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont2)
                        .withPermissionReplacement();

        ArgumentCaptor<ServerConfiguration> capturedConfig = ArgumentCaptor.forClass(ServerConfiguration.class);
        doNothing().when(server).updateServerConfiguration(capturedConfig.capture());

        inst.modify();

        ServerConfiguration result = capturedConfig.getValue();

        assertEquals(3, result.getJavaPermissions().size());

        List<String> expectedCodeBases = DatabaseContainerType.DerbyJava17Plus.streamAllArtifacts()
                        .map(name -> "${shared.resource.dir}/jdbcJava17Plus/" + name)
                        .collect(Collectors.toList());

        assertEquals(3, expectedCodeBases.size());

        for (JavaPermission permission : result.getJavaPermissions()) {
            assertEquals("java.security.AllPermission", permission.getClassName());
            assertTrue("Expected codebases did not contain: " + permission.getCodeBase(), expectedCodeBases.remove(permission.getCodeBase()));
        }

        assertTrue("Expected codebases were not present in server.xml: " + expectedCodeBases, expectedCodeBases.isEmpty());
    }

    @Test
    public void withDriverVariableTest() throws Exception {
        assumeTrue(!System.getProperty("os.name").equalsIgnoreCase("OS/400"));

        // Use null permission server to verify that we are able to correctly parse permissions
        // that lack a codeBase attribute.
        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("NullPermissionServer.xml"));

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont)
                        .withDriverVariable();

        ArgumentCaptor<ServerConfiguration> capturedConfig = ArgumentCaptor.forClass(ServerConfiguration.class);
        doNothing().when(server).updateServerConfiguration(capturedConfig.capture());

        inst.modify();

        ServerConfiguration result = capturedConfig.getValue();

        assertNotNull(result.getLibraries().getById("JDBCLibrary"));

        Library jdbcLib = result.getLibraries().getById("JDBCLibrary");
        assertEquals(1, jdbcLib.getFilesets().size());

        Fileset jdbcFs = jdbcLib.getFilesets().get(0);
        assertEquals("${shared.resource.dir}/jdbc", jdbcFs.getDir());
        assertEquals("${env.DB_DRIVER}", jdbcFs.getIncludes());

        assertEquals(2, result.getJavaPermissions().size());

        List<String> expectedCodeBases = new ArrayList<>();
        expectedCodeBases.add(null);
        expectedCodeBases.add("${shared.resource.dir}/jdbc/${env.DB_DRIVER}");

        for (JavaPermission permission : result.getJavaPermissions()) {
            assertTrue("Expected codebases did not contain: " + permission.getCodeBase(), expectedCodeBases.remove(permission.getCodeBase()));
        }

        assertTrue("Expected codebases were not present in server.xml: " + expectedCodeBases, expectedCodeBases.isEmpty());
    }

    @Test
    public void withDriverVariableAdditionsTest() throws Exception {
        assumeTrue(!System.getProperty("os.name").equalsIgnoreCase("OS/400"));
        assumeTrue(JavaInfo.JAVA_VERSION >= 17);

        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("libraryServerJava17Plus.xml"));
        when(server.isJava2SecurityEnabled()).thenReturn(true);

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont2)
                        .withDriverVariable();

        ArgumentCaptor<ServerConfiguration> capturedConfig = ArgumentCaptor.forClass(ServerConfiguration.class);
        doNothing().when(server).updateServerConfiguration(capturedConfig.capture());

        inst.modify();

        ServerConfiguration result = capturedConfig.getValue();

        assertNotNull(result.getLibraries().getById("JDBCLibrary"));

        Library jdbcLib = result.getLibraries().getById("JDBCLibrary");
        assertEquals(1, jdbcLib.getFilesets().size());

        Fileset jdbcFs = jdbcLib.getFilesets().get(0);
        assertEquals("${shared.resource.dir}/jdbcJava17Plus", jdbcFs.getDir());
        assertEquals("${env.DB_DRIVER}", jdbcFs.getIncludes());

        assertEquals(3, result.getJavaPermissions().size());

        List<String> expectedCodeBases = DatabaseContainerType.DerbyJava17Plus.streamAllArtifacts()
                        .map(name -> "${shared.resource.dir}/jdbcJava17Plus/" + name)
                        .collect(Collectors.toList());

        for (JavaPermission permission : result.getJavaPermissions()) {
            assertEquals("java.security.AllPermission", permission.getClassName());
            assertTrue("Expected codebases did not contain: " + permission.getCodeBase(), expectedCodeBases.remove(permission.getCodeBase()));
        }

        assertTrue("Expected codebases were not present in server.xml: " + expectedCodeBases, expectedCodeBases.isEmpty());
    }

    @Test
    public void withAuthVariablesTest() throws Exception {
        assumeTrue(!System.getProperty("os.name").equalsIgnoreCase("OS/400"));

        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("authDataServer.xml"));

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont)
                        .withAuthVariables("myUserCustom", "myPassCustom");

        ArgumentCaptor<ServerConfiguration> capturedConfig = ArgumentCaptor.forClass(ServerConfiguration.class);
        doNothing().when(server).updateServerConfiguration(capturedConfig.capture());

        inst.modify();

        ServerConfiguration result = capturedConfig.getValue();

        assertEquals(1, result.getDataSources().size());

        DataSource defaultDataSource = result.getDataSources().get(0);
        assertTrue(Boolean.valueOf(defaultDataSource.getFatModify()));
        assertNotNull(defaultDataSource.getProperties());
        assertEquals(1, defaultDataSource.getProperties().size());

        //Note: generic properties not properties.postgres because we did not specify .withDatabaseProperties()
        assertEquals(1, defaultDataSource.getProperties().size());
        Properties dsProps = defaultDataSource.getProperties().get(0);
        assertEquals("${env.DB_USER}", dsProps.getUser());
        assertEquals("${env.DB_PASS}", dsProps.getPassword());
    }

    @Test
    public void withGenericPropertiesTest() throws Exception {
        assumeTrue(!System.getProperty("os.name").equalsIgnoreCase("OS/400"));

        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("dataSourceServer.xml"));

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont)
                        .withGenericProperties();

        ArgumentCaptor<ServerConfiguration> capturedConfig = ArgumentCaptor.forClass(ServerConfiguration.class);
        doNothing().when(server).updateServerConfiguration(capturedConfig.capture());

        inst.modify();

        ServerConfiguration result = capturedConfig.getValue();

        AuthData recoveryAuth = result.getAuthDataElements().getById("recoveryAuth");
        assertNotNull(recoveryAuth);
        assertEquals("false", recoveryAuth.getFatModify());
        assertEquals("myUser1", recoveryAuth.getUser());
        assertEquals("myPass1", recoveryAuth.getPassword());

        AuthData userAuth = result.getAuthDataElements().getById("userAuth");
        assertNotNull(userAuth);
        assertEquals("true", userAuth.getFatModify());
        assertEquals(cont.getUsername(), userAuth.getUser());
        assertEquals(cont.getPassword(), userAuth.getPassword());

        DataSource defaultDataSource = result.getDataSources().getById("DefaultDataSource");
        assertNotNull(defaultDataSource);

        assertEquals(1, defaultDataSource.getContainerAuthDatas().size());
        AuthData containerAuthData = defaultDataSource.getContainerAuthDatas().get(0);
        assertNull(containerAuthData.getFatModify());
        assertEquals("myUser3", containerAuthData.getUser());
        assertEquals("myPass3", containerAuthData.getPassword());

        //Note: generic properties because we specified .withGenericProperties();
        assertEquals(1, defaultDataSource.getProperties().size());
        Properties defaultDataSourceProperties = defaultDataSource.getProperties().get(0);
        assertEquals(cont.getHost(), defaultDataSourceProperties.getServerName());
        assertEquals(cont.getFirstMappedPort().toString(), defaultDataSourceProperties.getPortNumber());
        assertEquals(cont.getDatabaseName(), defaultDataSourceProperties.getDatabaseName());
        assertNull("Expected null but was " + defaultDataSourceProperties.getUser(), defaultDataSourceProperties.getUser());
        assertNull("Expected null but was " + defaultDataSourceProperties.getPassword(), defaultDataSourceProperties.getPassword());

        assertEquals(1, result.getDatabaseStores().size());
        DatabaseStore dbStore = result.getDatabaseStores().get(0);

        DataSource testDataSource = dbStore.getDataSources().getById("TestDataSource");
        assertNotNull(testDataSource);

        //Note: generic properties because we specified .withGenericProperties();
        assertEquals(1, testDataSource.getProperties().size());
        Properties testDataSourceProperties = testDataSource.getProperties().get(0);
        assertEquals(cont.getHost(), testDataSourceProperties.getServerName());
        assertEquals(cont.getFirstMappedPort().toString(), testDataSourceProperties.getPortNumber());
        assertEquals(cont.getDatabaseName(), testDataSourceProperties.getDatabaseName());
        assertEquals(cont.getUsername(), testDataSourceProperties.getUser());
        assertEquals(cont.getPassword(), testDataSourceProperties.getPassword());
    }

    @Test
    public void withDatabaseProperties() throws Exception {
        when(server.getServerConfiguration()).thenReturn(getServerConfigFor("dataSourceServer.xml"));

        DatabaseContainerUtil inst = DatabaseContainerUtil.build(server, cont)
                        .withDatabaseProperties();

        ArgumentCaptor<ServerConfiguration> capturedConfig = ArgumentCaptor.forClass(ServerConfiguration.class);
        doNothing().when(server).updateServerConfiguration(capturedConfig.capture());

        inst.modify();

        ServerConfiguration result = capturedConfig.getValue();

        DataSource defaultDataSource = result.getDataSources().getById("DefaultDataSource");
        assertNotNull(defaultDataSource);

        assertEquals(1, defaultDataSource.getContainerAuthDatas().size());
        AuthData containerAuthData = defaultDataSource.getContainerAuthDatas().get(0);
        assertNull(containerAuthData.getFatModify());
        assertEquals("myUser3", containerAuthData.getUser());
        assertEquals("myPass3", containerAuthData.getPassword());

        //Note: specific properties because we specified .withDatabaseProperties()
        assertEquals(1, defaultDataSource.getProperties_postgresql().size());
        Properties_postgresql defaultDataSourceProperties = defaultDataSource.getProperties_postgresql().get(0);
        assertEquals(cont.getHost(), defaultDataSourceProperties.getServerName());
        assertEquals(cont.getFirstMappedPort().toString(), defaultDataSourceProperties.getPortNumber());
        assertEquals(cont.getDatabaseName(), defaultDataSourceProperties.getDatabaseName());
        assertNull("Expected null but was " + defaultDataSourceProperties.getUser(), defaultDataSourceProperties.getUser());
        assertNull("Expected null but was " + defaultDataSourceProperties.getPassword(), defaultDataSourceProperties.getPassword());

        assertEquals(1, result.getDatabaseStores().size());
        DatabaseStore dbStore = result.getDatabaseStores().get(0);

        DataSource testDataSource = dbStore.getDataSources().getById("TestDataSource");
        assertNotNull(testDataSource);

        //Note: specific properties because we specified .withDatabaseProperties()
        assertEquals(1, testDataSource.getProperties_postgresql().size());
        Properties_postgresql testDataSourceProperties = testDataSource.getProperties_postgresql().get(0);
        assertEquals(cont.getHost(), testDataSourceProperties.getServerName());
        assertEquals(cont.getFirstMappedPort().toString(), testDataSourceProperties.getPortNumber());
        assertEquals(cont.getDatabaseName(), testDataSourceProperties.getDatabaseName());
        assertEquals(cont.getUsername(), testDataSourceProperties.getUser());
        assertEquals(cont.getPassword(), testDataSourceProperties.getPassword());
    }

    // Helper methods to use test server.xml files

    private static ServerConfiguration getServerConfigFor(String fileName) throws Exception {
        return ServerConfigurationFactory.getInstance().unmarshal(getServerStream(fileName));
    }

    private static InputStream getServerStream(String fileName) {
        return DatabaseContainerPropertiesTest.class.getResourceAsStream(fileName);
    }

    // Helper methods to mock gradle tasks which copy dependencies to a shared resources directory
    private static Path createJdbcDriverDirectory() throws Exception {
        // Change where DatabaseContainerUtil looks for shared resources
        Field sharedResourcesDir = DatabaseContainerUtil.class.getDeclaredField("sharedResourcesDir");
        sharedResourcesDir.setAccessible(true);

        Path driverDir = Files.createTempDirectory("DatabaseContainerPropertiesTest-");
        sharedResourcesDir.set(null, driverDir.toFile());

        return driverDir;
    }

    private static void createFakeDrivers(Path driverDir, String child, String driverName) throws Exception {
        Path jdbcDir = driverDir.resolve(child);
        jdbcDir.toFile().mkdir();

        Path jdbcJar = driverDir.resolve(child).resolve(driverName);
        Files.write(jdbcJar, "mock jdbc driver jar".getBytes(), StandardOpenOption.CREATE);
    }

    // Helper methods to get private methods

    private static Method getFindAuthDataLocations() throws Exception {
        Method m = DatabaseContainerUtil.class.getDeclaredMethod("findAuthDataLocations", DataSource.class);
        m.setAccessible(true);
        return m;
    }
}
