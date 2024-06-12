/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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
package test.jakarta.data.datastore.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.jakarta.data.datastore.lib.DSDEntity;
import test.jakarta.data.datastore.lib.DSDRepo;
import test.jakarta.data.datastore.lib.ServerDSEntity;

@DataSourceDefinition(name = "java:app/jdbc/DataSourceDef",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      user = "servletuser1",
                      password = "servletpwd1",
                      properties = "createDatabase=create")
@SuppressWarnings("serial")
@WebServlet("/*")
public class DataStoreTestServlet extends FATServlet {

    @Inject
    DefaultDSRepo defaultDSRepo;

    @Inject
    DSDRepo dsdRepo;

    @Inject
    PersistenceUnitRepo persistenceUnitRepo;

    //TODO enable if we can figure out how to obtain the WSJdbcDataSource instance from the Weld proxy
    //@Inject
    QualifiedDSRepo qualifiedDSRepo;

    @Inject
    QualifiedPersistenceUnitRepo qualifiedPersistenceUnitRepo;

    @Inject
    ServerDSIdRepo serverDSIdRepo;

    @Inject
    ServerDSJNDIRepo serverDSJNDIRepo;

    // also exists in other web module, but with different
    // container managed auth alias user id
    @Resource(name = "java:module/env/jdbc/ServerDataSourceRef",
              lookup = "jdbc/ServerDataSource")
    DataSource serverDSResRef;

    @Inject
    ServerDSResRefRepo serverDSResRefRepo;

    /**
     * Use a repository defined in a library of the application that uses
     * a java:app scoped DataSourceDefinition.
     */
    @Test
    public void testDataSourceDefinition1() throws SQLException {
        dsdRepo.put(DSDEntity.of(15, "fifteen"));

        assertEquals("servletuser1", dsdRepo.getUser());
    }

    /**
     * Use a repository that specifies the Jakarta EE default data source by its JNDI name: java:comp/DefaultDataSource.
     * Verifies that the Table(name=...) JPA annotation can be used to specify the table name, and that no prefix is added
     * because a databaseStore is not explicitly used.
     */
    @Test
    public void testDefaultDataSourceByJNDIName() throws SQLException {
        assertEquals(false, defaultDSRepo.existsByIdAndValue(25L, "twenty-five"));

        defaultDSRepo.insert(DefaultDSEntity.of(25L, "twenty-five"));

        assertEquals(true, defaultDSRepo.existsByIdAndValue(25L, "twenty-five"));

        try (Connection con = defaultDSRepo.connect()) {
            assertEquals("defaultuser1",
                         con.getMetaData().getUserName().toLowerCase());

            String sql = "SELECT value FROM DefDSEntity WHERE id = 25";
            ResultSet result = con
                            .createStatement()
                            .executeQuery(sql);
            assertEquals(true, result.next());
            assertEquals("twenty-five", result.getString(1));
        }
    }

    /**
     * Verify that the EntityManagerFactory for the PersistenceUnit reference can be looked up by its JNDI name:
     */
    @Test
    public void testPersistenceUnitRefIsAvailable() throws Exception {
        EntityManagerFactory emf = InitialContext.doLookup("java:comp/env/persistence/MyPersistenceUnitRef");
        EntityManager em = emf.createEntityManager();
        em.close();
    }

    /**
     * Use a repository that specifies the a persistence unit reference, persistence/MyPersistenceUnitRef,
     * without explicitly including java:comp.
     *
     * Verify that a resource accessor method can return an EntityManager that can operate on the
     * same data that was inserted by the repository.
     */
    @Test
    public void testPersistenceUnitRefWithoutJavaComp() {
        PersistenceUnitEntity e51 = PersistenceUnitEntity.of("TestPersistenceUnit-fifty-one", 51);
        PersistenceUnitEntity e52 = PersistenceUnitEntity.of("TestPersistenceUnit-fifty-two", 52);
        PersistenceUnitEntity e56 = PersistenceUnitEntity.of("TestPersistenceUnit-fifty-six", 56);
        List<PersistenceUnitEntity> inserted = persistenceUnitRepo.save(List.of(e51, e52, e56));
        assertEquals(inserted.toString(), 3, inserted.size());

        assertEquals(3, persistenceUnitRepo.countByIdStartsWith("TestPersistenceUnit-"));

        e52.value = 152;
        e56.value = 156;

        List<PersistenceUnitEntity> updated = persistenceUnitRepo.save(List.of(e52, e56));
        assertEquals(updated.toString(), 2, updated.size());

        try {
            persistenceUnitRepo.connection();
            fail("TODO write better test if EclipseLink ever adds this capability.");
        } catch (UnsupportedOperationException x) {
            // expected - EclipseLink does not allow unwrap as Connection
        }

        try {
            persistenceUnitRepo.dataSource();
            fail("TODO write better test if EclipseLink ever adds this capability.");
        } catch (UnsupportedOperationException x) {
            // expected - EclipseLink does not allow unwrap as DataSource
        }

        try (EntityManager em = persistenceUnitRepo.entityManager()) {
            PersistenceUnitEntity e = em.find(PersistenceUnitEntity.class, "TestPersistenceUnit-fifty-two");
            assertEquals(Integer.valueOf(152), e.value);
        }
    }

    /**
     * Use a repository that specifies the data source to use via a qualifier on the
     * resource accessor method.
     */
    //TODO enable if we can figure out how to obtain the WSJdbcDataSource instance from the Weld proxy
    //@Test
    public void testQualifiedDataSource() {
        qualifiedDSRepo.add(List.of(ServerDSEntity.of("TestQualifiedDataSource-1", 31),
                                    ServerDSEntity.of("TestQualifiedDataSource-2", 32),
                                    ServerDSEntity.of("TestQualifiedDataSource-3", 31)));

        List<ServerDSEntity> thirtyOnes = qualifiedDSRepo.getAll(31);
        assertEquals(thirtyOnes.toString(), 2, thirtyOnes.size());
        assertEquals("TestQualifiedDataSource-1", thirtyOnes.get(0).id);
        assertEquals("TestQualifiedDataSource-3", thirtyOnes.get(1).id);

        // Prove it went into the expected database by accessing it from another repository that uses ServerDataSource
        serverDSIdRepo.remove(ServerDSEntity.of("TestQualifiedDataSource-1", 31)); // raises an error if not found
    }

    /**
     * Use a repository that specifies the data source to use via a qualifier on the
     * resource accessor method.
     */
    @Test
    public void testQualifiedPersistenceUnit() {
        qualifiedPersistenceUnitRepo.add(List.of(PersistenceUnitEntity.of("TestQualifiedPersistenceUnit-1", 71),
                                                 PersistenceUnitEntity.of("TestQualifiedPersistenceUnit-2", 72),
                                                 PersistenceUnitEntity.of("TestQualifiedPersistenceUnit-3", 71)));

        List<PersistenceUnitEntity> seventyOnes = qualifiedPersistenceUnitRepo.getAll(71);
        assertEquals(seventyOnes.toString(), 2, seventyOnes.size());
        assertEquals("TestQualifiedPersistenceUnit-1", seventyOnes.get(0).id);
        assertEquals("TestQualifiedPersistenceUnit-3", seventyOnes.get(1).id);

        // Prove it went into the expected database by accessing it from another repository that uses ServerDataSource
        assertEquals(3, persistenceUnitRepo.countByIdStartsWith("TestQualifiedPersistenceUnit-"));
    }

    /**
     * Verify that the qualified resource accessor method that is used to configure the EntityManagerFactory
     * can also be used by the application to obtain an EntityManager.
     */
    @Test
    public void testQualifiedEntityManagerResourceAccessorMethodUsedByApp() throws SQLException {

        qualifiedPersistenceUnitRepo.add(List.of(PersistenceUnitEntity.of("testPersistenceUnitRefWithQualifier", 70)));

        try (EntityManager em = qualifiedPersistenceUnitRepo.entityManager()) {
            PersistenceUnitEntity entity = em.find(PersistenceUnitEntity.class, "testPersistenceUnitRefWithQualifier");
            assertEquals(Integer.valueOf(70), entity.value);
        }
    }

    /**
     * Verify that the resource reference that is defined with a qualifier is made
     * accessible to the test application as a prerequisite for being able to make it
     * available to a repository in another test.
     */
    @Test
    public void testResourceReferenceWithQualifier() throws SQLException {

        Instance<DataSource> instance = CDI.current().select(DataSource.class, ResourceQualifier.Literal.INSTANCE);
        DataSource ds = instance.get();
        try (Connection con = ds.getConnection()) {
            assertEquals("SERVERUSER1", con.getMetaData().getUserName().toUpperCase());
        }
    }

    /**
     * Use a repository that specifies a data source from server.xml by its id: ServerDataSource.
     * Use a resource accessor method to obtain the same data source
     * and verify the user name matches what is configured in server.xml and that
     * the connection to the data source can access the data that was inserted
     * via the repository.
     */
    @Test
    public void testServerDataSourceById() throws SQLException {
        ServerDSEntity eighty_seven = ServerDSEntity.of("eighty-seven", 87);

        assertEquals(false, serverDSIdRepo.existsById("eighty-seven"));

        serverDSJNDIRepo.insert(eighty_seven); // other repository with same data source used for the insert

        DataSource ds = serverDSIdRepo.findById();
        try (Connection con = ds.getConnection()) {
            assertEquals("serveruser1",
                         con.getMetaData().getUserName().toLowerCase());

            String sql = "SELECT value FROM ServerDSEntity WHERE id = 'eighty-seven'";
            ResultSet result = con
                            .createStatement()
                            .executeQuery(sql);
            assertEquals(true, result.next());
            assertEquals(87, result.getInt(1));
        }

        serverDSIdRepo.remove(ServerDSEntity.of("eighty-seven", 87)); // raises an error if not found
    }

    /**
     * Use a repository that specifies a data source from server.xml by its JNDI name: jdbc/ServerDataSource.
     * Use a resource accessor method to create a connection to the same data source
     * and verify the user name matches what is configured in server.xml and that
     * the connection can access the data that was inserted via the repository.
     */
    @Test
    public void testServerDataSourceByJNDIName() throws SQLException {

        assertEquals(0, serverDSJNDIRepo.countById("forty-one"));

        serverDSJNDIRepo.insert(ServerDSEntity.of("forty-one", 41));

        assertEquals(1, serverDSJNDIRepo.countById("forty-one"));

        try (Connection con = serverDSJNDIRepo.createConnection()) {
            assertEquals("serveruser1",
                         con.getMetaData().getUserName().toLowerCase());

            String sql = "SELECT value FROM ServerDSEntity WHERE id = 'forty-one'";
            ResultSet result = con
                            .createStatement()
                            .executeQuery(sql);
            assertEquals(true, result.next());
            assertEquals(41, result.getInt(1));
        }
    }

    /**
     * Use a repository that specifies a resource reference to a data source,
     * where the resource reference has a container managed authentication alias
     * that is defined in server.xml, ResRefAuth1, with user resrefuser1.
     * Use a resource accessor method to obtain the same data source
     * and verify the user name matches what is configured in server.xml and that
     * the connection to the data source can access the data that was inserted
     * via the repository.
     */
    @Test
    public void testServerDataSourceByResRef() throws SQLException {
        ServerDSEntity ninety_three = ServerDSEntity.of("ninety-three", 93);

        assertEquals(false, serverDSResRefRepo.read("ninety-three").isPresent());

        ninety_three = serverDSResRefRepo.write(ninety_three);

        DataSource ds = serverDSResRefRepo.getDataStore();
        try (Connection con = ds.getConnection()) {
            assertEquals("resrefuser1",
                         con.getMetaData().getUserName().toLowerCase());

            String sql = "SELECT value FROM ServerDSEntity WHERE id='ninety-three'";
            ResultSet result = con
                            .createStatement()
                            .executeQuery(sql);
            assertEquals(true, result.next());
            assertEquals(93, result.getInt(1));
        }

        ninety_three = serverDSResRefRepo.read("ninety-three").orElseThrow();
        assertEquals(93, ninety_three.value);
        assertEquals("ninety-three", ninety_three.id);
    }
}
