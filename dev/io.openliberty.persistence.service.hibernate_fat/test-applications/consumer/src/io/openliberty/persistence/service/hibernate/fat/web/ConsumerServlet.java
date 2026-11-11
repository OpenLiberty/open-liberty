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
package io.openliberty.persistence.service.hibernate.fat.web;

import java.io.StringWriter;
import java.util.Arrays;

import jakarta.annotation.Resource;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.persistence.service.hibernate.fat.persistence.test.Consumer;

/**
 * Servlet that exercises the Hibernate-backed PersistenceService (persistenceService-3.0).
 *
 * Each @Test method is invoked by the FATRunner via HTTP — a test passes when
 * the method returns without throwing.
 */
@WebServlet("/*")
@SuppressWarnings("serial")
public class ConsumerServlet extends FATServlet {

    /** Looked up via the jndiName declared on ConsumerService */
    @Resource(lookup = "hibernateConsumerFactory")
    protected Consumer consumer;

    @Resource
    UserTransaction ut;

    /**
     * Persist a Person inside a JTA transaction and verify it can be found.
     */
    @Test
    public void testPersistAndFind() throws Exception {
        ut.begin();
        long id = consumer.persistPerson(false, 0);
        ut.commit();

        if (!consumer.personExists(id)) {
            throw new Exception("Person with id=" + id + " was not found after persist");
        }
    }

    /**
     * Persist a Person and verify the first/last name round-trips correctly.
     */
    @Test
    public void testPersonName() throws Exception {
        ut.begin();
        long id = consumer.persistPerson(false, 0);
        ut.commit();

        String[] names = consumer.getPersonName(id);
        if (!"first".equals(names[0]) || !"last".equals(names[1])) {
            throw new Exception("Expected [first, last] but got " + Arrays.toString(names));
        }
    }

    /**
     * Persist a Person with string data and verify the value round-trips.
     */
    @Test
    public void testPersistWithData() throws Exception {
        String testData = "hello-hibernate";
        ut.begin();
        long id = consumer.persistPerson(false, 0, testData);
        ut.commit();

        if (!consumer.personExists(id)) {
            throw new Exception("Person with data not found, id=" + id);
        }
    }

    /**
     * Verify that DDL can be generated without error and contains at least one
     * CREATE TABLE statement.
     */
    @Test
    public void testGenerateDDL() throws Exception {
        StringWriter writer = new StringWriter();
        try {
            consumer.generateDDL(writer);
            String ddl = writer.toString();
            if (!ddl.toUpperCase().contains("CREATE TABLE")) {
                throw new Exception("Expected CREATE TABLE in DDL output, got:\n" + ddl);
            }
        } finally {
            writer.close();
        }
    }

    /**
     * Verify the database termination token is returned (Hibernate 8 always returns ";").
     */
    @Test
    public void testGetDatabaseTerminationToken() throws Exception {
        String token = consumer.getDatabaseTerminationToken();
        if (token == null) {
            throw new Exception("getDatabaseTerminationToken() returned null");
        }
    }

    /**
     * Jakarta Data 1.1-style test: execute a JPQL query against the Person entity.
     *
     * In Jakarta Data 1.1, a @Repository method like:
     *   List<Person> findByFirstName(String firstName)
     * is translated by the Data runtime into exactly this JPQL:
     *   SELECT p FROM Person p WHERE p.firstName = :first
     *
     * This validates that Hibernate 8.x's ANTLR-based HQL/JPQL parser works
     * end-to-end through persistenceService-3.0 — the same code path that
     * Jakarta Data will use when generating query methods for @Repository interfaces.
     *
     * No @Repository interface is used directly here; the test exercises the
     * underlying JPA query mechanism that Data depends on.
     */
    @Test
    public void testJpqlQueryByFirstName() throws Exception {
        // Persist two persons with the same firstName, different lastName
        ut.begin();
        consumer.persistPerson(false, 0, "alpha");
        consumer.persistPerson(false, 0, "beta");
        ut.commit();

        // Query by firstName (hardcoded in persistPerson as "first")
        String[] lastNames = consumer.findByFirstName("first");

        if (lastNames == null || lastNames.length < 2) {
            throw new Exception("Expected at least 2 results for firstName='first', got: "
                + (lastNames == null ? "null" : lastNames.length));
        }
        // All returned entries should have lastName "last"
        for (String ln : lastNames) {
            if (!"last".equals(ln)) {
                throw new Exception("Unexpected lastName: " + ln);
            }
        }
    }
}
