/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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
import java.util.function.BiConsumer;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.EJB;
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
import test.jakarta.data.datastore.ejb.DataStoreTestEJB;
import test.jakarta.data.datastore.lib.DSDEntity;
import test.jakarta.data.datastore.lib.DSDRepo;
import test.jakarta.data.datastore.lib.ServerDSEntity;
import test.jakarta.data.datastore.web.lib.WebLibEntity;
import test.jakarta.data.datastore.web.lib.WebLibRepo;

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
    DefaultDSRepo2 defaultDSRepo2;

    @Inject
    DSDRepo dsdRepo;

    @Inject
    DSDRepoWar dsdRepoWar;

    @Inject
    DSAccessorMethodQualifiedRepo dsAccessorQualifiedRepo;

    @EJB(lookup = "java:global/DataStoreEJBApp/DataEJBAppBean!java.util.function.BiConsumer")
    BiConsumer<String, String> ejbApp;

    @Inject
    EMAccessorMethodQualifiedRepo emAccessorQualifiedRepo;

    @Inject
    MultiEntityRepo1 multiEntityRepo1;

    @Inject
    OrderingRepo orderingRepo;

    @Inject
    PersistenceUnitRepo persistenceUnitRepo;

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

    @EJB
    DataStoreTestEJB testEJB;

    @Inject
    WebLibRepo webLibRepo;

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
     * Use a repository defined in a WAR module of the application that uses
     * a java:app scoped DataSourceDefinition defined in the same WAR module.
     */
    @Test
    public void testDataSourceDefinitionWar() throws SQLException {
        dsdRepoWar.put(DSDEntityWar.of(16, "sixteen"));

        assertEquals("servletuser1", dsdRepoWar.getUser());
    }

    /**
     * Use a repository that specifies the Jakarta EE default data source by its JNDI name: java:comp/DefaultDataSource.
     * Verifies that the Table(name=...) JPA annotation can be used to specify the table name, and that no prefix is added
     * because a databaseStore is not explicitly used.
     * Also verifies that multiple repositories are supported using the same JNDI name, mostly to
     * ensure that ddlgen includes the create table commands for both in the same ddl file.
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

        // Verify a second repository in the same datastore works as well
        assertEquals(false, defaultDSRepo2.existsByIdAndValue(25L, "twenty-five"));

        defaultDSRepo2.insert(DefaultDSEntity2.of(25L, "twenty-five"));

        assertEquals(true, defaultDSRepo2.existsByIdAndValue(25L, "twenty-five"));

        try (Connection con = defaultDSRepo2.connect()) {
            assertEquals("defaultuser1",
                         con.getMetaData().getUserName().toLowerCase());

            String sql = "SELECT value FROM DefDSEntity2 WHERE id = 25";
            ResultSet result = con
                            .createStatement()
                            .executeQuery(sql);
            assertEquals(true, result.next());
            assertEquals("twenty-five", result.getString(1));
        }
    }

    /**
     * Use a repository that defaults to the default dataStore, but also has a
     * qualifier annotation on its resource accessor method for a DataSource.
     * The qualifier annotation must be ignored.
     */
    @Test
    public void testDataSourceResourceAccessorMethodQualifierIgnored() {
        dsAccessorQualifiedRepo.add(List.of(DefaultDSEntity.of(31, "DSRAMQI-3"),
                                            DefaultDSEntity.of(32, "DSRAMQI-2"),
                                            DefaultDSEntity.of(33, "DSRAMQI-3")));

        List<DefaultDSEntity> threes = dsAccessorQualifiedRepo.getAll("DSRAMQI-3");
        assertEquals(threes.toString(), 2, threes.size());
        assertEquals(31, threes.get(0).id);
        assertEquals(33, threes.get(1).id);

        // Prove it went into the expected database by accessing it from
        // another repository that uses the same DataSource
        defaultDSRepo.existsByIdAndValue(32, "DSRAMQI-2");
    }

    /**
     * Use a repository that is defined within an EJB application,
     * where the data source is the default data source.
     */
    @Test
    public void testDefaultDataSourceInEJBModule() {
        ejbApp.accept("testDefaultDataSourceInEJBModule",
                      "EJBAppDSRefRepo");

        // Prove it went into the expected database by accessing it from
        // another repository that uses the same DataSource
        defaultDSRepo.existsByIdAndValue(62, "sixty-two");
    }

    /**
     * Use a repository that is defined within an EJB application,
     * where the EJB application also defines the data source.
     */
    @Test
    public void testEJBAppDefinesAndUsesRepository() {
        ejbApp.accept("testEJBAppDefinesAndUsesRepository",
                      "EJBAppDSRefRepo");
    }

    /**
     * Verify that a qualified resource accessor method can also be used by the
     * application to obtain an EntityManager. The qualifier is ignored.
     */
    @Test
    public void testEntityManagerResourceAccessorMethodQualifierIgnored() {

        // goes into defaultdb
        emAccessorQualifiedRepo.add(List.of(PersistenceUnitEntity.of("EMRAMQI", 70)));

        // goes into serverdb
        persistenceUnitRepo.save(List.of(PersistenceUnitEntity.of("EMRAMQI", 71)));

        // reads from defaultdb
        try (EntityManager em = emAccessorQualifiedRepo.entityManager()) {
            PersistenceUnitEntity entity = em.find(PersistenceUnitEntity.class, "EMRAMQI");
            assertEquals(Integer.valueOf(70), entity.value);
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
            fail("write better test if EclipseLink ever adds this capability.");
        } catch (UnsupportedOperationException x) {
            // expected - EclipseLink does not allow unwrap as Connection
        }

        try {
            persistenceUnitRepo.dataSource();
            fail("write better test if EclipseLink ever adds this capability.");
        } catch (UnsupportedOperationException x) {
            // expected - EclipseLink does not allow unwrap as DataSource
        }

        try (EntityManager em = persistenceUnitRepo.entityManager()) {
            PersistenceUnitEntity e = em.find(PersistenceUnitEntity.class, "TestPersistenceUnit-fifty-two");
            assertEquals(Integer.valueOf(152), e.value);
        }
    }

    /**
     * Use a repository that is defined within an EJB application,
     * where the EJB application also defines the data source.
     */
    @Test
    public void testRepositoryInEJBAppDefinesAndUsesDataSourceDefinition() {
        ejbApp.accept("testRepositoryInEJBAppDefinesAndUsesDataSourceDefinition",
                      "EJBAppDSDRepo");
    }

    /**
     * Use a repository defined in a library of a web module.
     */
    @Test
    public void testRepositoryInWebModuleLibrary() throws SQLException {

        assertEquals(false, webLibRepo.request(5).isPresent());

        webLibRepo.create(WebLibEntity.of(5, "five"));

        assertEquals("servletuser1", webLibRepo.user());

        WebLibEntity e = webLibRepo.request(5).orElseThrow();
        assertEquals(5, e.id);
        assertEquals("five", e.value);
    }

    /**
     * Verify that the resource reference that is defined with a qualifier is made
     * accessible to the test application.
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

    /**
     * Use a repository, defined in an EJB, that specifies the JNDI name of a
     * DataSourceDefinition, also defined in the EJB, which has user id ejbuser1.
     * Use a resource accessor method to obtain a connection to the data source
     * and verify the user name matches and that the connection can access the
     * data that was inserted via the repository.
     */
    @Test
    public void testDataSourceDefinitionInEJBModule() throws SQLException {
        testEJB.testDataSourceDefinitionInEJBModule();
    }

    /**
     * Use a repository, defined in an EJB, that specifies a resource reference
     * to a data source, defined in server.xml,
     * where the resource reference has a container managed authentication alias
     * that is defined in server.xml, ResRefAuth3, with user resrefuser3.
     * Use a resource accessor method to obtain the same data source
     * and verify the user name matches what is configured in server.xml and that
     * the connection to the data source can access the data that was inserted
     * via the repository.
     */
    @Test
    public void testServerDataSourceByResRefInEJBModule() throws SQLException {
        testEJB.testServerDataSourceByResRefInEJBModule();
    }

    /**
     * Use a repository, defined in an EJB, that specifies the JNDI name of a
     * DataSourceDefinition, defined by a servlet in a WAR module, which has
     * user id servletuser1. Use a resource accessor method to obtain a connection
     * to the data source and verify the user name matches.
     */
    @Test
    public void testDataSourceDefinitionInWARModuleFromEJB() throws SQLException {
        testEJB.testDataSourceDefinitionInWARModuleFromEJB();
    }

    /**
     * Verify that code in an EJB application can access a Jakarta Data repository
     * from a CDI Startup event.
     */
    @Test
    public void testStartupEventObserverInEJBApplicationUsesRepository() {
        ejbApp.accept("testStartupEventObserverInEJBApplicationUsesRepository",
                      "EJBAppDSRefRepo");
    }

    /**
     * Verify that code in an EJB module can access a Jakarta Data repository from
     * a CDI Startup event.
     */
    @Test
    public void testStartupEventObserverInEJBModuleUsesRepository() {
        testEJB.testStartupEventObserverInEJBModuleUsesRepository();
    }

    /**
     * Verify that ServletContextListener can inject a Jakarta Data repository
     * and use it to initialize data.
     */
    @Test
    public void testServletContextListenerPopulatesDatabase() {
        ServerDSEntity one = serverDSResRefRepo.read("DSSCL-one").orElseThrow();
        assertEquals("DSSCL-one", one.id);
        assertEquals(1, one.value);

        ServerDSEntity two = serverDSResRefRepo.read("DSSCL-two").orElseThrow();
        assertEquals("DSSCL-two", two.id);
        assertEquals(2, two.value);
    }
}
