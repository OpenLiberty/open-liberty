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
package test.jakarta.data.errpaths.web;

import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;

import org.junit.Test;

import componenttest.app.FATServlet;

@DataSourceDefinition(name = "java:app/jdbc/DerbyDataSource",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      user = "dbuser1",
                      password = "dbpwd1",
                      properties = "createDatabase=create")
@PersistenceUnit(name = "java:app/env/VoterPersistenceUnitRef",
                 unitName = "VoterPersistenceUnit")
@SuppressWarnings("serial")
@WebServlet("/*")
public class DataErrPathsTestServlet extends FATServlet {

    @Inject
    RepoWithoutDataStore errDefaultDataSourceNotConfigured;

    @Resource
    UserTransaction tx;

    @Inject
    Voters voters;

    /**
     * Initialize the database with some data that other tests can try to read.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            EntityManagerFactory emf = InitialContext //
                            .doLookup("java:app/env/VoterPersistenceUnitRef");

            tx.begin();
            try (EntityManager em = emf.createEntityManager()) {
                em.persist(new Voter(123445678, "Veronica", //
                                LocalDate.of(1951, Month.SEPTEMBER, 25), //
                                "4051 E River Rd NE, Rochester, MN 55906"));

                em.persist(new Voter(987665432, "Vivian", //
                                LocalDate.of(1971, Month.OCTOBER, 1), //
                                "701 Silver Creek Rd NE, Rochester, MN 55906"));

                em.persist(new Voter(789001234, "Vincent", //
                                LocalDate.of(1977, Month.SEPTEMBER, 26), //
                                "770 W Silver Lake Dr NE, Rochester, MN 55906"));
            } finally {
                tx.commit();
            }
        } catch (Exception x) {
            throw new ServletException(x);
        }
    }

    /**
     * Tests a basic error path that is very likely to occur where a Repository
     * lets the dataStore default to java:comp/DefaultDataSource, but the
     * default data source is not configured. This tests for the error message
     * that explains how to correct the problem.
     */
    @Test
    public void testRequiresDefaultDataSourceButNotConfigured() {
        try {
            Optional<Voter> found;
            found = errDefaultDataSourceNotConfigured.findById(123445678);
            fail("Should not be able to use repository without DefaultDataSource " +
                 "being configured. Found: " + found);
        } catch (CompletionException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1077E:") ||
                !x.getMessage().contains("<dataSource id=\"DefaultDataSource\""))
                throw x;
        }
    }
}
