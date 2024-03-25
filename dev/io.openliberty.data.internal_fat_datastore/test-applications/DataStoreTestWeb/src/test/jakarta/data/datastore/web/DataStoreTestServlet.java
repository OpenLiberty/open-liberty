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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataStoreTestServlet extends FATServlet {

    @Inject
    DefaultDSRepo defaultDSRepo;

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

    /**
     * Use a repository that specifies the Jakarta EE default data source by its JNDI name: java:comp/DefaultDataSource
     */
    @Test
    public void testDefaultDataSourceByJNDIName() {
        assertEquals(false, defaultDSRepo.existsByIdAndValue(25L, "twenty-five"));

        defaultDSRepo.insert(DefaultDSEntity.of(25L, "twenty-five"));

        assertEquals(true, defaultDSRepo.existsByIdAndValue(25L, "twenty-five"));
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
     * Use a repository that specifies a data source from server.xml by its id: ServerDataSource
     */
    @Test
    public void testServerDataSourceById() {
        ServerDSEntity eighty_seven = ServerDSEntity.of("eighty-seven", 87);

        assertEquals(false, serverDSIdRepo.existsById("eighty-seven"));

        serverDSJNDIRepo.insert(eighty_seven); // other repository with same data source used for the insert

        serverDSIdRepo.remove(ServerDSEntity.of("eighty-seven", 87)); // raises an error if not found
    }

    /**
     * Use a repository that specifies a data source from server.xml by its JNDI name: jdbc/ServerDataSource
     */
    @Test
    public void testServerDataSourceByJNDIName() {

        assertEquals(0, serverDSJNDIRepo.countById("forty-one"));

        serverDSJNDIRepo.insert(ServerDSEntity.of("forty-one", 41));

        assertEquals(1, serverDSJNDIRepo.countById("forty-one"));
    }
}
