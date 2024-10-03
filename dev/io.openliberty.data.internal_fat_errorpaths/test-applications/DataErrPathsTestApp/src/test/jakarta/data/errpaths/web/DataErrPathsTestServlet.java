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
import java.util.List;
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
import javax.naming.NamingException;

import org.junit.Test;

import componenttest.app.FATServlet;

@DataSourceDefinition(name = "java:app/jdbc/DerbyDataSource",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      user = "dbuser1",
                      password = "dbpwd1",
                      properties = "createDatabase=create")
@DataSourceDefinition(name = "java:module/jdbc/DataSourceForInvalidEntity",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      user = "dbuser1",
                      password = "dbpwd1",
                      properties = "createDatabase=create")
@DataSourceDefinition(name = "java:comp/jdbc/InvalidDatabase",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "notfounddb",
                      user = "dbuser1",
                      password = "dbpwd1")
@PersistenceUnit(name = "java:app/env/VoterPersistenceUnitRef",
                 unitName = "VoterPersistenceUnit")
@SuppressWarnings("serial")
@WebServlet("/*")
public class DataErrPathsTestServlet extends FATServlet {

    @Inject
    InvalidDatabaseRepo errDatabaseNotFound;

    @Inject
    RepoWithoutDataStore errDefaultDataSourceNotConfigured;

    @Inject
    InvalidNonJNDIRepo errIncorrectDataStoreName;

    @Inject
    InvalidJNDIRepo errIncorrectJNDIName;

    @Resource
    UserTransaction tx;

    @Inject
    Voters voters;

    /**
     * Preemptively cause errors that will result in FFDC to keep them from
     * failing test cases.
     */
    public void forceFFDC() throws Exception {
        try {
            InitialContext.doLookup("java:comp/jdbc/InvalidDataSource");
        } catch (NamingException x) {
            // expected; the database doesn't exist
        }
    }

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
     * Tests an error path where the application specifies the repository dataStore
     * to be a JNDI name that does not exist.
     */
    @Test
    public void testRepositoryWithIncorrectDataStoreJNDIName() {
        try {
            List<Voter> found = errIncorrectJNDIName //
                            .bornOn(LocalDate.of(1977, Month.SEPTEMBER, 26));
            fail("Should not be able to use repository that sets the dataStore " +
                 "to a JNDI name that does not exist. Found: " + found);
        } catch (CompletionException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1079E:") ||
                !x.getMessage().contains("<persistence-unit name=\"MyPersistenceUnit\">"))
                throw x;
        }
    }

    /**
     * Tests an error path where the application specifies the repository dataStore
     * to be a name that does not exist as a dataSource id, a databaseStore id, or
     * a JNDI name.
     */
    @Test
    public void testRepositoryWithIncorrectDataStoreName() {
        try {
            Voter added = errIncorrectDataStoreName //
                            .addNew(new Voter(876554321, "Vanessa", //
                                            LocalDate.of(1955, Month.JULY, 5), //
                                            "5455 W River Rd NW, Rochester, MN 55901"));
            fail("Should not be able to use repository that sets the dataStore " +
                 "to a name that does not exist. Added: " + added);
        } catch (CompletionException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1078E:") ||
                !x.getMessage().contains("<dataSource id=\"MyDataSource\" jndiName=\"jdbc/ds\""))
                throw x;
        }
    }

    /**
     * Tests an error path where the application specifies the repository dataStore
     * to be a DataSource that is configured to use a database that does not exist.
     */
    @Test
    public void testRepositoryWithInvalidDatabaseName() {
        try {
            List<Voter> found = errDatabaseNotFound //
                            .livesAt("2800 37th St NW, Rochester, MN 55901");
            fail("Should not be able to use repository that sets the dataStore" +
                 " to a DataSource that is configured to use a database that does" +
                 " not exist. Found: " + found);
        } catch (CompletionException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1080E:") ||
                !x.getMessage().contains(InvalidDatabaseRepo.class.getName()))
                throw x;
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
