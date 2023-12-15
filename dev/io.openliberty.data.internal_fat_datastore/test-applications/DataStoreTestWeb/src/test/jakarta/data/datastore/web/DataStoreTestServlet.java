/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.annotation.WebServlet;

import javax.naming.InitialContext;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataStoreTestServlet extends FATServlet {

    @Inject
    DefaultDSRepo defaultDSRepo;

    // A prefix of java:comp/env/ is implied for name
    @PersistenceUnit(name = "persistence/MyPersistenceUnitRef", unitName = "MyPersistenceUnit")
    EntityManagerFactory emf;

    @Inject
    PersistenceUnitRepo persistenceUnitRepo;

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
     * Use a repository that specifies a data source from server.xml by its id: ServerDataSource
     */
    @Test
    public void testServerDataSourceById() {
        ServerDSEntity eighty_seven = ServerDSEntity.of("eighty-seven", 87);

        assertEquals(false, serverDSIdRepo.remove(eighty_seven));

        serverDSJNDIRepo.insert(eighty_seven); // other repository with same data source used for the insert

        assertEquals(true, serverDSIdRepo.remove(ServerDSEntity.of("eighty-seven", 87)));
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
