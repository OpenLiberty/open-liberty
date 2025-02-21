/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
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

import static jakarta.data.repository.By.ID;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
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
import test.jakarta.data.errpaths.web.Voters.NameAndZipCode;

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
    Inventions errInvalidEntityRepo;

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
     * Verify an error is raised when a count Query by Method Name method
     * tries to return a long value as a Page of Long.
     */
    @Test
    public void testCountAsPage() {
        try {
            LocalDate today = LocalDate.of(2025, Month.FEBRUARY, 18);
            Page<Long> page = voters.countByBirthday(today);
            fail("Should not be able to use a count query that returns a" +
                 " Page rather than long. Page is: " + page);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1049E:") ||
                !x.getMessage().contains("Page<java.lang.Long>"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that attempts to
     * return a CursoredPage of a record, rather than of the entity.
     */
    @Test
    public void testCursoredPageOfRecord() {
        try {
            CursoredPage<VoterRegistration> page = //
                            voters.registrations(LocalDate.of(2024, 12, 17),
                                                 PageRequest.ofSize(7));
            fail("Should not be able to retrieve CursoredPage of a non-entity." +
                 " Found: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1037E:") ||
                !x.getMessage().contains("VoterRegistration"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that attempts to
     * return a CursoredPage of an entity attribute, rather than of the entity.
     */
    @Test
    public void testCursoredPageOfString() {
        LocalDate date = LocalDate.of(2024, 12, 18);
        try {
            CursoredPage<Integer> page = //
                            voters.findByBirthdayOrderBySSN(date,
                                                            PageRequest.ofSize(8));
            fail("Should not be able to retrieve CursoredPage of a non-entity." +
                 " Found: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1037E:") ||
                !x.getMessage().contains("CursoredPage<java.lang.Integer>"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that attempts to
     * return a CursoredPage with the OrderBy annotation omitted
     * and no other sort criteria present.
     */
    @Test
    public void testCursoredPageOrderByOmitted() {
        try {
            CursoredPage<Voter> page = //
                            voters.selectByLastName("TestCursoredPageOrderByOmitted",
                                                    PageRequest.ofSize(23));
            fail("Should not be able to retrieve a CursoredPage when OrderBy is" +
                 " omitted from the Query method and there is no other sort." +
                 " criteria. Found: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1100E:") ||
                !x.getMessage().contains("Sort[]"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that attempts to
     * return a CursoredPage with an ORDER BY clause present.
     */
    @Test
    public void testCursoredPageOrderClauseIncluded() {
        String firstName = "TestCursoredPageOrderClauseIncluded";
        try {
            CursoredPage<Voter> page = //
                            voters.selectByFirstName(firstName,
                                                     PageRequest.ofSize(24),
                                                     Order.by(Sort.desc(ID)));
            fail("Should not be able to retrieve a CursoredPage when ORDER BY is" +
                 " included in the Query value. Found: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1033E:") ||
                !x.getMessage().contains("ORDER BY"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that attempts to
     * return a CursoredPage with NULL ordering.
     */
    @Test
    public void testCursoredPageOrderNull() {
        LocalDate date = LocalDate.of(2024, 12, 20);
        try {
            CursoredPage<Voter> page = //
                            voters.selectByBirthday(date,
                                                    PageRequest.ofSize(20),
                                                    null);
            fail("Should not be able to retrieve a CursoredPage with results that" +
                 " have a NULL order: " + page);
        } catch (NullPointerException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1087E:") ||
                !x.getMessage().contains("jakarta.data.Order"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that attempts to
     * return a CursoredPage with NULL sorting.
     */
    @Test
    public void testCursoredPageSortNull() {
        try {
            CursoredPage<Voter> page = //
                            voters.selectByName("Vincent",
                                                PageRequest.ofSize(33),
                                                null);
            fail("Should not be able to retrieve a CursoredPage with results that" +
                 " have a NULL value for the Sort parameter: " + page);
        } catch (NullPointerException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1087E:") ||
                !x.getMessage().contains("jakarta.data.Sort"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that attempts to
     * return an unordered CursoredPage.
     */
    @Test
    public void testCursoredPageUnordered() {
        LocalDate date = LocalDate.of(2024, 12, 19);
        try {
            CursoredPage<Voter> page = //
                            voters.selectByBirthday(date,
                                                    PageRequest.ofSize(19),
                                                    Order.by());
            fail("Should not be able to retrieve a CursoredPage with results that" +
                 " are not ordered: " + page);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1088E:") ||
                !x.getMessage().contains("jakarta.data.Order"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that attempts to
     * return an unsorted CursoredPage.
     */
    @Test
    public void testCursoredPageUnsorted() {
        String address = "701 Silver Creek Rd NE, Rochester, MN 55906";
        try {
            CursoredPage<Voter> page = //
                            voters.selectByAddress(address, PageRequest.ofSize(3));
            fail("Should not be able to retrieve a CursoredPage with results that" +
                 " are not sorted: " + page);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1088E:") ||
                !x.getMessage().contains("Sort[]"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that attempts to
     * delete a page of results by specifying a PageRequest on a Delete operation.
     */
    @Test
    public void testDeletePageOfResults() {
        try {
            voters.discardPage("701 Silver Creek Rd NE, Rochester, MN 55906",
                               PageRequest.ofSize(15));
            fail("Should not be able to perform a delete operation by supplying"
                 + " a PageRequest.");
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1022E:") ||
                !x.getMessage().contains("discardPage"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that specifies a
     * Limit parameter on a Delete operation with void return type.
     */
    @Test
    public void testDeleteWithLimitParameterButNoResult() {
        try {
            voters.discardLimited("701 Silver Creek Rd NE, Rochester, MN 55906",
                                  Limit.of(3));
            fail("Should not be able to define an Limit parameter on a method that" +
                 " deletes entities but does not return them");
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1097E:") ||
                !x.getMessage().contains("discardLimited"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that specifies an
     * OrderBy annotation on a Delete operation with void return type.
     */
    @Test
    public void testDeleteWithOrderByAnnotationButNoResult() {
        try {
            voters.discardInOrder("701 Silver Creek Rd NE, Rochester, MN 55906");
            fail("Should not be able to define an Order parameter on a method that" +
                 " deletes entities but does not return them");
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1096E:") ||
                !x.getMessage().contains("discardInOrder"))
                throw x;
        }
    }

    @Test
    public void testDeleteWithOrderByKeywordButNoResult() {
        try {
            voters.deleteByAddressOrderByName("701 Silver Creek Rd NE, Rochester, MN 55906");
            fail("Should not be able to define an OrderBy keyword on a method that" +
                 " deletes entities but does not return them");
        } catch (MappingException x) {
            // expected
        }
    }

    /**
     * Verify an error is raised for a repository method that specifies an
     * Order parameter on a Delete operation with void return type.
     */
    @Test
    public void testDeleteWithOrderParameterButNoResult() {
        try {
            voters.discardOrdered("701 Silver Creek Rd NE, Rochester, MN 55906",
                                  Order.by(Sort.desc(ID)));
            fail("Should not be able to define an Order parameter on a method that" +
                 " deletes entities but does not return them");
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1097E:") ||
                !x.getMessage().contains("discardOrdered"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository method that specifies a
     * Sort parameter on a Delete operation with int return type.
     */
    @Test
    public void testDeleteWithSortParameterButNoResult() {
        try {
            int count = voters.discardSorted("701 Silver Creek Rd NE, Rochester, MN 55906",
                                             Sort.asc("ssn"));
            fail("Should not be able to define a Sort parameter on a method that" +
                 " deletes entities and returns an update count: " + count);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1097E:") ||
                !x.getMessage().contains("discardSorted"))
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
     * Verify IllegalArgumentException is raised if you attempt to delete an
     * empty list of entities.
     */
    @Test
    public void testEmptyDelete() {
        try {
            voters.deleteAll(List.of());
            fail("Deleted an empty list of entities.");
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1092E:") ||
                !x.getMessage().contains("deleteAll"))
                throw x;
        }
    }

    /**
     * Verify IllegalArgumentException is raised if you attempt to delete an
     * empty array of entities.
     */
    @Test
    public void testEmptyInsert() {
        try {
            voters.register(new Voter[] {});
            fail("Inserted an empty array of entities.");
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1092E:") ||
                !x.getMessage().contains("register"))
                throw x;
        }
    }

    /**
     * Verify IllegalArgumentException is raised if you attempt to save an
     * empty list of entities.
     */
    @Test
    public void testEmptySave() {
        try {
            List<Voter> saved = voters.saveAll(List.of());
            fail("Saved an empty list of entities. Result: " + saved);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1092E:") ||
                !x.getMessage().contains("saveAll"))
                throw x;
        }
    }

    /**
     * Verify IllegalArgumentException is raised if you attempt to update an
     * empty stream of entities.
     */
    @Test
    public void testEmptyUpdate() {
        try {
            List<Voter> updated = voters.changeAll(Stream.of());
            fail("Updated an empty stream of entities. Result: " + updated);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1092E:") ||
                !x.getMessage().contains("changeAll"))
                throw x;
        }
    }

    /**
     * Verify an error is raised when an exists Query by Method Name method
     * tries to return a true/false value as int.
     */
    @Test
    public void testExistsAsInt() {
        try {
            int found = voters.existsByAddress("4051 E River Rd NE, Rochester, MN 55906");
            fail("Should not be able to use an exists query that returns a" +
                 " numeric value rather than boolean. Result: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1003E:") ||
                !x.getMessage().contains("boolean")) // recommended type
                throw x;
        }
    }

    /**
     * Verify an error is raised when an exists Query by Method Name method
     * tries to return a true/false value as a Long value that is wrapped in
     * a CompletableFuture.
     */
    @Test
    public void testExistsAsLong() {
        try {
            CompletableFuture<Long> cf = voters.existsByName("Vincent");
            fail("Should not be able to use an exists query that returns a" +
                 " numeric value rather than boolean. Future: " + cf);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1003E:") ||
                !x.getMessage().contains("CompletableFuture<java.lang.Long>"))
                throw x;
        }
    }

    /**
     * Verify an error is raised when an exists Query by Method Name method
     * tries to return a true/false value as a Page.
     */
    @Test
    public void testExistsAsPage() {
        try {
            LocalDate today = LocalDate.of(2025, Month.FEBRUARY, 18);
            Page<Boolean> page = voters.existsByBirthday(today,
                                                         PageRequest.ofSize(5));
            fail("Should not be able to use an exists query that returns a" +
                 " Page rather than boolean. Page is: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1003E:") ||
                !x.getMessage().contains("Page<java.lang.Boolean>"))
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
     * Verify an error is raised for a repository insert method with a parameter
     * that can insert multiple entities and a return type that can only return
     * one inserted entity.
     */
    @Test
    public void testInsertMultipleEntitiesButOnlyReturnOne() {
        Voter v1 = new Voter(100200300, "Valerie", //
                        LocalDate.of(1947, Month.NOVEMBER, 7), //
                        "88 23rd Ave SW, Rochester, MN 55902");
        Voter v2 = new Voter(400500600, "Vinny", //
                        LocalDate.of(1988, Month.NOVEMBER, 8), //
                        "2016 45th St SE, Rochester, MN 55904");
        try {
            Voter inserted = voters.register(v1, v2);
            fail("Insert method with singular return type should not be able to " +
                 "insert two entities. Instead returned: " + inserted);
        } catch (ClassCastException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1094E:") ||
                !x.getMessage().contains("register") ||
                !x.getMessage().contains("Voter[]"))
                throw x;
        }
    }

    /**
     * Verify an appropriate error is raised when attempting to insert a null
     * record entity.
     */
    @Test
    public void testInsertNullRecordEntity() {
        try {
            voters.addPollingLocation(null);
            fail("Should not be able to insert a null entity.");
        } catch (NullPointerException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1015E") ||
                !x.getMessage().contains("addPollingLocation"))
                throw x;
        }
    }

    /**
     * A repository method with the First keyword and a Limit parameter
     * must raise an error.
     */
    @Test
    public void testIntermixFirstAndLimit() {
        try {
            Voter[] found = voters.findFirst2(Limit.of(3));

            fail("Did not reject repository method that has both a First keyword" +
                 " and a Limit parameter. Instead found: " + Arrays.toString(found));
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1099E") ||
                !x.getMessage().contains("Limit"))
                throw x;
        }
    }

    /**
     * A repository method with the First keyword and a PageRequest parameter
     * must raise an error.
     */
    @Test
    public void testIntermixFirstAndPageRequest() {
        try {
            Page<Voter> page = voters.findFirst3(PageRequest.ofSize(2));

            fail("Did not reject repository method that has both a First keyword" +
                 " and a PageRequest parameter. Instead found: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1099E") ||
                !x.getMessage().contains("PageRequest"))
                throw x;
        }
    }

    /**
     * A repository method with both Limit and PageRequest parameters
     * must raise an error.
     */
    @Test
    public void testIntermixLimitAndPage() {
        try {
            List<Voter> found = voters
                            .inhabiting("4051 E River Rd NE, Rochester, MN 55906",
                                        Limit.of(8),
                                        Order.by(Sort.asc(ID)),
                                        PageRequest.ofSize(13));

            fail("Did not reject repository method that has both a Limit and" +
                 " PageRequest. Instead found: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1018E") ||
                !x.getMessage().contains("inhabiting"))
                throw x;
        }
    }

    /**
     * A repository method returning Page, with both PageRequest and Limit
     * parameters must raise an error.
     */
    @Test
    public void testIntermixPageAndLimit() {
        try {
            Page<Voter> page = voters
                            .occupying("4051 E River Rd NE, Rochester, MN 55906",
                                       PageRequest.ofPage(4),
                                       Order.by(Sort.asc(ID)),
                                       Limit.of(14));

            fail("Did not reject repository method that has both a PageReaquest" +
                 " and Limit. Instead found: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1018E") ||
                !x.getMessage().contains("occupying"))
                throw x;
        }
    }

    /**
     * Use a repository method with multiple entity parameters, which is not
     * allowed for life cycle methods such as Insert.
     */
    @Test
    public void testLifeCycleInsertMethodWithMultipleParameters() {
        List<Voter> list = List //
                        .of(new Voter(999887777, "New Voter 1", //
                                        LocalDate.of(1999, Month.DECEMBER, 9), //
                                        "213 13th Ave NW, Rochester, MN 55901"),

                            new Voter(777665555, "New Voter 2", //
                                            LocalDate.of(1987, Month.NOVEMBER, 7), //
                                            "300 7th St SW, Rochester, MN 55902"));

        try {
            list = voters.addSome(list, Limit.of(1));
            fail("Did not reject Insert method with multiple parameters. Result: " +
                 list);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1009E") ||
                !x.getMessage().contains("addSome"))
                throw x;
        }
    }

    /**
     * Use a repository method with no parameters, which is not
     * allowed for life cycle methods such as Insert.
     */
    @Test
    public void testLifeCycleInsertMethodWithoutParameters() {
        try {
            Voter[] added = voters.addNothing();
            fail("Did not reject Insert method without parameters. Result: " +
                 Arrays.toString(added));

        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1009E") ||
                !x.getMessage().contains("addNothing"))
                throw x;
        }
    }

    /**
     * Use a repository method with multiple entity parameters, which is not
     * allowed for life cycle methods such as Save.
     */
    @Test
    public void testLifeCycleSaveMethodWithMultipleParameters() {
        Voter v = new Voter(123445678, "Updated Name", //
                        LocalDate.of(1951, Month.SEPTEMBER, 25), //
                        "4051 E River Rd NE, Rochester, MN 55906");
        try {
            Voter saved = voters.storeInDatabase(v, PageRequest.ofSize(5));

            fail("Did not reject Save life cycle method that has" +
                 " multiple parameters. Result: " + saved);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1009E") ||
                !x.getMessage().contains("storeInDatabase"))
                throw x;
        }
    }

    /**
     * Use a repository method with no parameters, which is not
     * allowed for life cycle methods such as Save.
     */
    @Test
    public void testLifeCycleSaveMethodWithoutParameters() {
        try {
            voters.storeNothing();
            fail("Did not reject Save method without parameters.");

        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1009E") ||
                !x.getMessage().contains("storeNothing"))
                throw x;
        }
    }

    /**
     * Use a repository method with multiple entity parameters, which are not
     * allowed for life cycle methods such as Update.
     */
    @Test
    public void testLifeCycleUpdateMethodWithMultipleParameters() {
        Voter v1 = new Voter(123445678, "New Name 1", //
                        LocalDate.of(1951, Month.SEPTEMBER, 25), //
                        "4051 E River Rd NE, Rochester, MN 55906");
        Voter v2 = new Voter(987665432, "New Name 2", //
                        LocalDate.of(1971, Month.OCTOBER, 1), //
                        "701 Silver Creek Rd NE, Rochester, MN 55906");
        try {
            List<Voter> list = voters.changeBoth(v1, v2);

            fail("Did not reject Update life cycle method that has" +
                 " multiple parameters. Result: " + list);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1009E") ||
                !x.getMessage().contains("changeBoth"))
                throw x;
        }
    }

    /**
     * Use a repository method with no parameters, which is not
     * allowed for life cycle methods such as Update.
     */
    @Test
    public void testLifeCycleUpdateMethodWithoutParameters() {
        try {
            voters.changeNothing();

            fail("Did not reject Update life cycle method that has" +
                 " no parameters.");
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1009E") ||
                !x.getMessage().contains("changeNothing"))
                throw x;
        }
    }

    /**
     * Tests an error path where a Query by Method Name repository method
     * attempts to place the special parameters ahead of the query parameters.
     */
    @Test
    public void testMethodNameQueryWithSpecialParametersFirst() {
        try {
            List<Voter> found = voters
                            .findFirst5ByAddress(Order.by(Sort.asc(ID)),
                                                 "4051 E River Rd NE, Rochester, MN 55906");
            fail("Should fail when special parameters are positioned elsewhere" +
                 " than at the end. Instead: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1098E:") ||
                !x.getMessage().contains("findFirst5ByAddress"))
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
     * Verify an error is raised for a repository find method that defines two
     * Limit parameters.
     */
    @Test
    public void testMultipleLimits() {
        try {
            List<Voter> found = voters
                            .livesAt("701 Silver Creek Rd NE, Rochester, MN 55906",
                                     Limit.of(2),
                                     Order.by(Sort.asc(ID)),
                                     Limit.range(5, 9));
            fail("Find method with multiple Limits must raise error." +
                 " Instead found: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1017E:") ||
                !x.getMessage().contains("livesAt"))
                throw x;
        }
    }

    /**
     * Verify an error is raised for a repository find method that defines two
     * PageRequest parameters.
     */
    @Test
    public void testMultiplePageRequests() {
        try {
            Page<Voter> page = voters
                            .residesAt("701 Silver Creek Rd NE, Rochester, MN 55906",
                                       PageRequest.ofSize(7),
                                       Order.by(Sort.asc(ID)),
                                       PageRequest.ofPage(3));
            fail("Find method with multiple PageRequests must raise error." +
                 " Instead found: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1017E:") ||
                !x.getMessage().contains("residesAt"))
                throw x;
        }
    }

    /**
     * BasicRepository.findAll(PageRequest, null) must raise NullPointerException.
     */
    @Test
    public void testNullOrder() {
        try {
            Page<Voter> page = voters.findAll(PageRequest.ofSize(15), null);
            fail("BasicRepository.findAll(PageRequest, null) must raise" +
                 " NullPointerException. Instead: " + page);
        } catch (NullPointerException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1087E") &&
                x.getMessage().contains(Order.class.getName()))
                ; // expected
            else
                throw x;
        }
    }

    /**
     * BasicRepository.findAll(null, Order) must raise NullPointerException.
     */
    @Test
    public void testNullPageRequest() {
        try {
            Page<Voter> page = voters.findAll(null, Order.by(Sort.asc("id")));
            fail("BasicRepository.findAll(null, Order) must raise" +
                 " NullPointerException. Instead: " + page);
        } catch (NullPointerException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1087E") &&
                x.getMessage().contains(PageRequest.class.getName()))
                ; // expected
            else
                throw x;
        }
    }

    /**
     * Attempt to supply a single NULL Sort to a repository method that retrieves
     * results as CursoredPage.
     */
    @Test
    public void testNullSortForCursoredPage() {
        CursoredPage<Voter> page;
        Cursor ssn0 = Cursor.forKey(0);
        try {
            page = voters.selectByAddress("4051 E River Rd NE, Rochester, MN 55906",
                                          PageRequest.ofSize(5).afterCursor(ssn0),
                                          new Sort<?>[] { null });
            fail("Obtained a cursored page sorted by NULL: " + page);
        } catch (IllegalArgumentException x) {
            // expected
        }
    }

    /**
     * Attempt to supply a single NULL Sort to a repository method that retrieves
     * results as a Page.
     */
    @Test
    public void testNullSortForPage() {
        Page<Voter> page;
        try {
            page = voters.selectAll(PageRequest.ofSize(6),
                                    new Sort<?>[] { null });
            fail("Obtained a page sorted by NULL: " + page);
        } catch (IllegalArgumentException x) {
            // expected
        }
    }

    /**
     * Attempt to supply multiple Sorts, with one of them NULL, to a
     * repository method that retrieves results as CursoredPage.
     */
    @Test
    public void testNullSortWithOtherSortsValidForCursoredPage() {
        CursoredPage<Voter> page;
        Cursor ssn0 = Cursor.forKey(0,
                                    "Val",
                                    "4051 E River Rd NE, Rochester, MN 55906");
        try {
            page = voters.selectByAddress("4051 E River Rd NE, Rochester, MN 55906",
                                          PageRequest.ofSize(4).afterCursor(ssn0),
                                          Sort.asc(ID),
                                          null,
                                          Sort.desc("address"));
            fail("Obtained a cursored page sorted by NULL: " + page);
        } catch (IllegalArgumentException x) {
            // expected
        }
    }

    /**
     * Attempt to supply multiple Sorts, with one of them NULL, to a
     * repository method that retrieves results as a Page.
     */
    @Test
    public void testNullSortWithOtherSortsValidForPage() {
        Page<Voter> page;
        try {
            page = voters.selectAll(PageRequest.ofSize(7),
                                    Sort.asc(ID),
                                    null,
                                    Sort.desc("name"));
            fail("Obtained a page sorted by NULL: " + page);
        } catch (IllegalArgumentException x) {
            // expected
        }
    }

    /**
     * Supply a PageRequest that has a Cursor to a repository method that returns
     * an offset-based Page rather than a CursoredPage. Expect an error.
     */
    @Test
    public void testOffsetPageRequestedWithCursor() {
        Order<Voter> nameAsc = Order.by(Sort.asc("name"), Sort.asc("ssn"));
        PageRequest after123456789 = PageRequest.ofSize(4) //
                        .afterCursor(Cursor.forKey("Voter Name", 123456789));
        try {
            Page<Voter> page = voters //
                            .atAddress("770 W Silver Lake Dr NE, Rochester, MN 55906",
                                       after123456789,
                                       nameAsc);
            fail("Obtained an offset page from a PageRequest that contains a" +
                 " Cursor: " + page);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1035E:") ||
                !x.getMessage().contains("atAddress"))
                throw x;
        }
    }

    /**
     * Verify an error is raised when a repository method specifies both an
     * OrderBy annotation and the method's name includes the OrderBy keyword.
     */
    @Test
    public void testOrderByConflict() {
        String address = "701 Silver Creek Rd NE, Rochester, MN 55906";
        try {
            List<Voter> found = voters.findByAddressOrderByName(address);
            fail("Conflicting OrderBy annotation and method name keyword mut" +
                 " raise UnsupportedOperationException. Instead: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1090E") &&
                x.getMessage().contains("findByAddressOrderByName"))
                ; // expected
            else
                throw x;
        }
    }

    /**
     * Verify an error is raised when a repository method has an OrderBy annotation
     * that attempts to sort by an invalid, non-existent function.
     */
    @Test
    public void testOrderByInvalidFunction() {
        try {
            List<Voter> found = voters.sortedByEndOfAddress();
            fail("OrderBy annotation with invalid function must cause an error." +
                 " Instead, the repository method returned: " + found);
        } catch (MappingException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1010E") &&
                x.getMessage().contains("last5DigitsOf(address)"))
                ; // expected
            else
                throw x;
        }
    }

    /**
     * Verify an error is raised when a repository method has an OrderBy annotation
     * that attempts to sort by an invalid, non-existent function.
     */
    @Test
    public void testOrderByUnkownEntityAttribute() {
        try {
            List<Voter> found = voters.sortedByZipCode();
            fail("OrderBy annotation with invalid entity attribute must cause an" +
                 " error. Instead, the repository method returned: " + found);
        } catch (MappingException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1010E") &&
                x.getMessage().contains("sortedByZipCode"))
                ; // expected
            else
                throw x;
        }
    }

    /**
     * Tests an error path where a paremeter-based query method attempts to place
     * the special parameters ahead of the query parameters.
     */
    @Test
    public void testParameterBasedQueryWithSpecialParametersFirst() {
        try {
            Page<Voter> page = voters
                            .occupantsOf(PageRequest.ofSize(9),
                                         Order.by(Sort.asc(ID)),
                                         "4051 E River Rd NE, Rochester, MN 55906");
            fail("Should fail when special parameters are positioned elsewhere" +
                 " than at the end. Instead: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1098E:") ||
                !x.getMessage().contains("occupantsOf"))
                throw x;
        }
    }

    /**
     * Verify an error is raised when a repository method specifies both an
     * OrderBy annotation and the method's name includes the OrderBy keyword.
     * The method also has a Sort parameter.
     */
    @Test
    public void testOrderByConflictPlusSortParam() {
        Sort<Voter> sort = Sort.asc("birthday");
        try {
            List<Voter> found = voters.findByAddressOrderBySSN(123456789, sort);
            fail("Conflicting OrderBy annotation and method name keyword mut" +
                 " raise UnsupportedOperationException. Instead: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1090E") &&
                x.getMessage().contains("findByAddressOrderBySSN"))
                ; // expected
            else
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
     * Tests an error path where a Query by Method Name repository method
     * attempts to place the special parameters ahead of the query parameters.
     */
    @Test
    public void testQueryWithSpecialParameterAheadOfQueryNamedParameter() {
        try {
            List<Voter> found = voters.withNameLongerThan(Limit.of(16),
                                                          5);
            fail("Should fail when special parameters are positioned elsewhere" +
                 " than at the end. Instead: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1098E:") ||
                !x.getMessage().contains("withNameLongerThan"))
                throw x;
        }
    }

    /**
     * Tests an error path where a Query by Method Name repository method
     * attempts to place the special parameters ahead of the query parameters.
     */
    @Test
    public void testQueryWithSpecialParameterAheadOfQueryPositionalParameter() {
        try {
            List<Voter> found = voters.withNameShorterThan(Sort.asc(ID),
                                                           17);
            fail("Should fail when special parameters are positioned elsewhere" +
                 " than at the end. Instead: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1098E:") ||
                !x.getMessage().contains("withNameShorterThan"))
                throw x;
        }
    }

    /**
     * Tests an error path where a repository method attempts to remove an entity
     * but return it as a record instead.
     */
    @Test
    public void testRemoveAsSubsetOfEntity() {
        try {
            NameAndZipCode removed = voters.removeBySSN(789001234).orElseThrow();
            fail("Should not be able to remove an entity as a record: " +
                 removed);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1006E") ||
                !x.getMessage().contains("NameAndZipCode"))
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
     * Tests an error path where the repository specifies an entity that is not a
     * valid JPA entity because it has no Id attribute.
     */
    @Test
    public void testRepositoryWithInvalidEntity() {
        try {
            Invention i = errInvalidEntityRepo //
                            .save(new Invention(1, 2, "Perpetual Motion Machine"));
            fail("Should not be able to use a repository operation for an entity" +
                 " that is not valid because it has no Id attribute. Saved: " + i);
        } catch (CompletionException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1080E:") ||
                !x.getMessage().contains(Invention.class.getName()))
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
     * Tests an error path where a repository method attempts to return a subset of
     * an entity as a record where the record component names do not all match the
     * entity attribute names.
     */
    @Test
    public void testReturnInvalidSubsetOfEntity() {
        try {
            NameAndZipCode result = voters.nameAndZipCode(789001234).orElseThrow();
            fail("Should not be able to obtain result as a record with" +
                 " component names that do not all match entity attributes: " +
                 result);
        } catch (MappingException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1101E") &&
                x.getMessage().contains("Voters$NameAndZipCode"))
                ; // expected
            else
                throw x;
        }
    }

    /**
     * Verify an appropriate error is raised when attempting to save a list of
     * record entities in which one is null.
     */
    @Test
    public void testSaveListWithNullRecordEntity() {
        PollingLocation loc1 = PollingLocation
                        .of(42, "201 4th St SE, Rochester, MN 55904", 4, 2, //
                            LocalTime.of(7, 0), LocalTime.of(20, 0));
        try {
            voters.addOrUpdate(Arrays.asList(loc1, null));
            fail("Should not be able to save a list containing a null entity.");
        } catch (NullPointerException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1015E") ||
                !x.getMessage().contains("addOrUpdate"))
                throw x;
        }
    }

    /**
     * Request a page of unordered results, which should raise an error because
     * pagination only works with deterministically ordered results.
     */
    @Test
    public void testUnorderedPage() {
        try {
            Page<Voter> page = voters.findAll(PageRequest.ofSize(5), Order.by());
            fail("Retrieved a page without any ordering: " + page);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1088E") &&
                x.getMessage().contains(Order.class.getName()))
                ; // expected
            else
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
