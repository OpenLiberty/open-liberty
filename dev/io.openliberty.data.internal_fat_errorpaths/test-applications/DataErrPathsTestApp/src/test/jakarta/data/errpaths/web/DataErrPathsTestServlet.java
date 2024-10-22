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
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
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
// The following is intentionally invalidly used by repositories that specify
// a different entity type that is not in the persistence unit.
@PersistenceUnit(name = "java:app/env/WrongPersistenceUnitRef",
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

    @Inject
    WrongPersistenceUnitRefRepo errWrongPersistenceUnitRef;

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
     * Verify an error is raised for a repository method that attempts to use
     * both named parameters and positional parameters in the same query.
     */
    @Test
    public void testBothNamedAndPositionalParameters() {
        try {
            List<Voter> found = voters.livingAt(701,
                                                "Silver Creek Rd NE",
                                                "Rochester",
                                                "MN",
                                                55906);
            fail("Method that mixes named parameters with positional parameters" +
                 " ought to raise an appropriate error. Instead found: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1019E:") ||
                !x.getMessage().contains("livingAt"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that defines two method
     * parameters (Param annotation) for the same named parameter.
     */
    @Test
    public void testDuplicateNamedParam() {
        try {
            List<Voter> found = voters.bornOn(1977, Month.SEPTEMBER, 9, 26);
            fail("Method with two Param annotations for the same named parameter" +
                 " ought to raise an appropriate error. Instead found: " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1083E:") ||
                !x.getMessage().contains("bornOn"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that has extra Param
     * annotations that do not correspond to any named parameters in the query.
     */
    @Test
    public void testExtraParamAnnos() {
        try {
            List<Voter> found = voters.livingOn("E River Rd NE", "Rochester", "MN");
            fail("Method with extra Param annotations ought to raise an error." +
                 " Instead found: " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1085E:") ||
                !x.getMessage().contains("livingOn"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that has extra method
     * parameters that do not correspond to any parameters in the query.
     */
    @Test
    public void testExtraParameters() {
        try {
            List<Voter> found = voters.residingAt(701,
                                                  "Silver Creek Rd NE",
                                                  "Rochester",
                                                  "MN");
            fail("Method with extra method parameters ought to raise an error." +
                 " Instead found: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1019E:") ||
                !x.getMessage().contains("residingAt"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method with a query that
     * requires 1 positional parameter, but the method supplies 3 parameters.
     */
    @Test
    public void testExtraPositionalParameters() {
        try {
            List<Voter> found = voters.withAddressLongerThan(20, 25, 30);
            fail("Method with extra positional parameters ought to raise an" +
                 " error. Instead found: " + found);
        } catch (IllegalArgumentException x) {
            // Error is detected by EclipseLink
            if (x.getMessage() == null ||
                !x.getMessage().contains("WHERE LENGTH(address) > ?1"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that has a Param annotation
     * that specifies a name value that does not match the name of a named parameter
     * from the query.
     */
    @Test
    public void testMismatchedParameterNames() {
        try {
            List<Voter> found = voters.livingIn("Rochester", "MN");
            fail("Method where the Param annotation specifies a name that does" +
                 " not match a named parameter in the query ought to raise an. " +
                 " error. Instead found: " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1084E:") ||
                !x.getMessage().contains("livingIn"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that defines two method
     * parameters (Param annotation) for the same named parameter.
     */
    @Test
    public void testMissingParamAnno() {
        try {
            List<Voter> found = voters.bornIn(1951);
            fail("Method that lacks a Param annotation and runs without the" +
                 " -parameters compile option ought to raise an error. " +
                 " Instead found: " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1084E:") ||
                !x.getMessage().contains("bornIn"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that attempts to use
     * the Param annotation (which is for named parameters only) to supply its
     * single positional parameter.
     */
    @Test
    public void testParamUsedForPositionalParameter() {
        try {
            List<Voter> found = voters.withAddressShorterThan(100);
            fail("Method that tries to use Param for a positional parameter" +
                 " ought to raise an error. Instead found: " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1086E:") ||
                !x.getMessage().contains("(maxLength)"))
                throw x;
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

    /**
     * Attempt to use a repository that has a persistence unit reference to a
     * persistence unit that lacks the entity class that is needed by the
     * repository. Expect an error.
     */
    @Test
    public void testWrongPersistenceUnitRef() {
        try {
            Page<Volunteer> page;
            page = errWrongPersistenceUnitRef.findAll(PageRequest.ofSize(5),
                                                      Order.by(Sort.asc("name")));
            fail("Should not be able to use a repository that has a persistence" +
                 " unit reference to a persistence unit that does not include the" +
                 " entity that is used by the repository. Found: " + page);
        } catch (DataException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1082E:") ||
                !x.getMessage().contains("(test.jakarta.data.errpaths.web.Volunteer)") ||
                !x.getMessage().contains("(test.jakarta.data.errpaths.web.WrongPersistenceUnitRefRepo)"))
                throw x;
        }
    }
}
