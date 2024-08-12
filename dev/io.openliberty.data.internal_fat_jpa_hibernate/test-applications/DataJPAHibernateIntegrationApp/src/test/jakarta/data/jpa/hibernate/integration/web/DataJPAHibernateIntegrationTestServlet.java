/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.jpa.hibernate.integration.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataJPAHibernateIntegrationTestServlet extends FATServlet {

    @PersistenceUnit(unitName = "HibernateProvider", name = "jpa/HibernateProvider")
    private EntityManagerFactory hibernateEMF;

    @PersistenceUnit(unitName = "LibertyProvider", name = "jpa/LibertyProvider")
    private EntityManagerFactory libertyEMF;

    @Inject
    Counties counties; //Hibernate

    @Inject
    Segments segments; //Liberty

    @Resource
    private UserTransaction tx;

    @Test
    public void testPersistenceUnits() throws Exception {
        assertNotNull(hibernateEMF);
        assertNotNull(libertyEMF);
    }

    @Test
    public void testHibernateProvider() throws Exception {
        //Create entity
        int[] zipCodes = new int[] { 55009, 55018, 55026, 55027, 55066, 55089, 55946, 55963, 55983, 55992 };
        County expected = County.of("Goodhue", "Minnesota", 48013, zipCodes, "Red Wing");

        //Save entity
        counties.save(expected);

        //Ensure the Hibernate persistence unit was used
        tx.begin();
        County actual = hibernateEMF.createEntityManager().find(County.class, "Goodhue");
        tx.commit();

        assertEquals(expected, actual);

        //Ensure the Liberty persistence unit was not used
        tx.begin();
        County unexpected = libertyEMF.createEntityManager().find(County.class, "Goodhue");
        tx.commit();

        assertNull(unexpected);

        //Remove entity
        counties.remove(expected);
    }

    @Test
    public void testLibertyProvider() throws Exception {
        //Create entity
        Segment expected = Segment.of(5, 10, 2, 4);

        //Save entity
        expected = segments.save(expected);
        int expectedId = expected.id;

        //Ensure the Liberty persistence unit was used
        tx.begin();
        Segment actual = libertyEMF.createEntityManager().find(Segment.class, expectedId);
        tx.commit();

        assertEquals(expected, actual);

        //Ensure the Hibernate persistence unit was not used
        tx.begin();
        Segment unexpected = hibernateEMF.createEntityManager().find(Segment.class, expectedId);
        tx.commit();

        assertNull(unexpected);

        // Remove entity
        segments.remove(expected);

    }
}
