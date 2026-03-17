/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.nosql.integration.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Optional;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataNoSQLIntegrationServlet extends FATServlet {

    @PersistenceUnit(unitName = "LibertyProvider", name = "jpa/LibertyProvider")
    private EntityManagerFactory libertyEMF;

    @Inject
    DocumentTemplate jNoSQLtemplate;

    @Inject
    Counties counties; //JNoSQL

    //TODO Re-enable after future JNoSQL Release
    //@Inject
    //Segments segments; //Liberty

    @Resource
    private UserTransaction tx;

    @Test
    public void testPersistenceUnits() throws Exception {
        assertNotNull(libertyEMF);
    }

    @Test
    public void testJNoSQLProvider() throws Exception {
        //Create entity
        Integer[] zipCodes = new Integer[] { 55009, 55018, 55026, 55027, 55066, 55089, 55946, 55963, 55983, 55992 };
        County expected = County.of("Goodhue", "Minnesota", 48013, zipCodes, "Red Wing");

        counties.save(expected);

        //Ensure JNoSQL data provider was used
        County actual = jNoSQLtemplate.find(County.class, "Goodhue").get();//hibernateEMF.createEntityManager().find(County.class, "Goodhue");

        assertEquals(expected, actual);

        //Ensure the Liberty persistence unit was not used
        try {
            tx.begin();
            County unexpected = libertyEMF.createEntityManager().find(County.class, "Goodhue");
            tx.commit();
            fail("Should not be able to access County with Liberty's EMF since it's annotated with JNoSQL's @Entity");
        } catch (IllegalArgumentException e) {
            //pass
        }

        counties.remove(expected);
    }

    //TODO Re-enable after future JNoSQL Release
    //@Test
    public void testLibertyProvider() throws Exception {

        Segment expected = Segment.of(5, 10, 2, 4);

        //TODO Re-enable after future JNoSQL Release
        //expected = segments.save(expected);
        int expectedId = expected.id;

        //Ensure the Liberty persistence unit was used
        tx.begin();
        Segment actual = libertyEMF.createEntityManager().find(Segment.class, expectedId);
        tx.commit();

        assertEquals(expected, actual);

        //Ensure the JNoSQL data provider was not used
        tx.begin();
        Optional<Segment> unexpected = jNoSQLtemplate.find(Segment.class, expectedId);
        tx.commit();

        assertTrue(unexpected.isEmpty());

        //TODO Re-enable after future JNoSQL Release
        //segments.remove(expected);

    }

}
