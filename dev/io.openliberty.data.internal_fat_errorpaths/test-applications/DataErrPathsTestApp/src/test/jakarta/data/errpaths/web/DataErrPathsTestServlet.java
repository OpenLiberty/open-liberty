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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
import jakarta.data.page.PageRequest.Mode;
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
@Resource(name = "java:app/jdbc/env/DSForInvalidEntityRecordWithJPAAnnoRef",
          lookup = "java:module/jdbc/DataSourceForInvalidEntity")
@Resource(name = "java:comp/jdbc/env/DSForInvalidEntityClassWithoutAnnoRef",
          lookup = "java:module/jdbc/DataSourceForInvalidEntity")
@SuppressWarnings("serial")
@WebServlet("/*")
public class DataErrPathsTestServlet extends FATServlet {

    @Inject
    InvalidDatabaseRepo errDatabaseNotFound;

    @Inject
    RepoWithoutDataStore errDefaultDataSourceNotConfigured;

    @Inject
    Invitations errEntityClassMissingAnnoRepo;

    @Inject
    Inventions errEntityMissingIdRepo;

    @Inject
    InvalidNonJNDIRepo errIncorrectDataStoreName;

    @Inject
    InvalidJNDIRepo errIncorrectJNDIName;

    @Inject
    Investments errRecordEnityWithJPAAnnoRepo;

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
                                "701 Silver Creek Rd NE, Rochester, MN 55906", //
                                "vivian@openliberty.io", //
                                "vivian.voter@openliberty.io"));

                em.persist(new Voter(789001234, "Vincent", //
                                LocalDate.of(1977, Month.SEPTEMBER, 26), //
                                "770 W Silver Lake Dr NE, Rochester, MN 55906", //
                                "vincent@openliberty.io"));
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
     * Verify an error is raised when the GreaterThanEqual keyword is applied to a
     * collection of values.
     */
    @Test
    public void testCollectionGreaterThanEqual() {
        try {
            List<Voter> found = voters.findByEmailAddressesGreaterThanEqual(1);
            fail("Should not be able to compare a collection to a number." +
                 " Found " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1110E:") ||
                !x.getMessage().contains("GreaterThanEqual"))
                throw x;
        }
    }

    /**
     * Verify an error is raised when the IgnoreCase keyword is applied to a
     * collection of values.
     */
    @Test
    public void testCollectionIgnoreCase() {
        List<Voter> found;
        try {
            String mixedCaseEmail = "Vivian@OpenLiberty.io";
            found = voters.findByEmailAddressesIgnoreCaseContains(mixedCaseEmail);
            fail("Should not be able to compare a collection ignoring case." +
                 " Found " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1110E:") ||
                !x.getMessage().contains("IgnoreCase"))
                throw x;
        }
    }

    /**
     * Verify an error is raised when a value cannot be safely converted to byte.
     */
    @Test
    public void testConvertToByte() {
        try {
            byte result = voters.ssnAsByte(123445678);
            fail("Should not convert int value 123445678 to byte value " + result);
        } catch (MappingException x) {
            // expected - out of range
        }

        try {
            Optional<Byte> result = voters.ssnAsByteWrapper(987665432);
            fail("Should not convert int value 987665432 to Byte value " + result);
        } catch (MappingException x) {
            // expected - out of range
        }
    }

    /**
     * Verify an error is raised when a String cannot be safely converted to char
     * because it contains more than 1 character.
     */
    @Test
    public void testConvertToChar() {
        try {
            Optional<Character> found = voters.firstLetterOfName(987665432);
            fail("Should not be able to return a 6 character String as a" +
                 " single character: " + found);
        } catch (MappingException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1046E") &&
                x.getMessage().contains("firstLetterOfName"))
                ; // pass
            else
                throw x;
        }
    }

    /**
     * Verify an error is raised when a value cannot be safely converted to float.
     */
    @Test
    public void testConvertToFloat() {
        try {
            float[] floats = voters.minMaxSumCountAverageFloat(999999999);
            fail("Allowed unsafe conversion from integer to float: " +
                 Arrays.toString(floats));
        } catch (MappingException x) {
            if (x.getMessage().startsWith("CWWKD1046E") &&
                x.getMessage().contains("float[]"))
                ; // unsafe to convert double to float
            else
                throw x;
        }
    }

    /**
     * Repository method that returns the count as a boolean value,
     * which is not an allowed return type. This must raise an error.
     */
    @Test
    public void testCountAsBoolean() {
        try {
            boolean count = voters.countAsBooleanBySSNLessThan(420000000);
            fail("Count queries cannot have a boolean return type: " + count);
        } catch (MappingException x) {
            if (x.getMessage().startsWith("CWWKD1049E") &&
                x.getMessage().contains("boolean"))
                ; // cannot convert number to boolean
            else
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
     * Error path where a request for cursor-based pagination includes ordering on
     * VERSION(THIS) when the entity has no version. Expect an error with an
     * appropriate message.
     */
    @Test
    public void testCursoredPageInvalidOrderNoVersion() {
        PageRequest pageReq = PageRequest.ofSize(5).afterCursor(Cursor.forKey(0));
        try {
            CursoredPage<Voter> page = voters
                            .selectByBirthday(LocalDate.of(2025, 5, 23),
                                              pageReq,
                                              Order.by(Sort.asc("VERSION(THIS)")));
            fail("Should not be able to retrieve CursoredPage ordering on" +
                 " VERSION(THIS) when the entity doesn't have a version. Found: " +
                 page);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1093E:") ||
                !x.getMessage().contains("VERSION(THIS)"))
                throw x;
        }
    }

    /**
     * Error path where a request for cursor-based pagination includes ordering on
     * an attribute that the entity does not have. Expect an error with an
     * appropriate message.
     */
    @Test
    public void testCursoredPageInvalidOrderUnknownSortAttribute() {
        PageRequest pageReq = PageRequest.ofSize(6).afterCursor(Cursor.forKey(50000));
        try {
            CursoredPage<Voter> page = voters
                            .selectByBirthday(LocalDate.of(2025, 5, 22),
                                              pageReq,
                                              Order.by(Sort.asc("income")));
            fail("Should not be able to retrieve CursoredPage ordering on" +
                 " an attribute that does not exist on the entity. Found: " +
                 page);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1010E:") ||
                !x.getMessage().contains("income"))
                throw x;
        }
    }

    /**
     * Error path where a request for cursor-based pagination includes ordering on
     * a function that does not exist. Expect an error from EclipseLink.
     */
    @Test
    public void testCursoredPageInvalidOrderUnknownSortFunction() {
        PageRequest pageReq = PageRequest.ofSize(7).afterCursor(Cursor.forKey(0.22));
        try {
            CursoredPage<Voter> page = voters
                            .selectByBirthday(LocalDate.of(2025, 5, 21),
                                              pageReq,
                                              Order.by(Sort.asc("TAXBRACKET(THIS)")));
            fail("Should not be able to retrieve CursoredPage ordering on" +
                 " a function that does not exist on the entity. Found: " +
                 page);
        } catch (RuntimeException x) {
            // error is from EclipseLink
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
                !x.getMessage().startsWith("CWWKD1020E:") ||
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
                !x.getMessage().startsWith("CWWKD1020E:") ||
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
                !x.getMessage().startsWith("CWWKD1020E:") ||
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
                !x.getMessage().startsWith("CWWKD1020E:") ||
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
     * Verify MappingException is raised if attempting to supply an empty string
     * value instead of a valid entity attribute name to the By annotation.
     */
    @Test
    public void testEmptyBy() {
        try {
            List<Voter> found = voters.inPrecinct(2);
            fail("Queried on an empty string attribute name and found " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1024E:") ||
                !x.getMessage().contains("inPrecinct"))
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
     * Verify MappingException is raised if the entity attribute name is omitted
     * from a findBy comparison of a repository method name.
     */
    @Test
    public void testEmptyFindComparison() {
        try {
            List<Voter> found = voters.findByIgnoreCaseContains("Civic Center Dr");
            fail("Ordered by an empty string attribute name and returned " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1011E:") ||
                !x.getMessage().contains("findByIgnoreCaseContains"))
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
     * Verify MappingException is raised if attempting to supply an empty string
     * value instead of a valid entity attribute name to the OrderBy annotation.
     */
    @Test
    public void testEmptyOrderByAnno() {
        try {
            List<Voter> found = voters.inTownship("Haverhill");
            fail("Ordered by an empty string attribute name and returned " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1024E:") ||
                !x.getMessage().contains("inTownship"))
                throw x;
        }
    }

    /**
     * Verify MappingException is raised if the entity attribute name is omitted
     * from the OrderBy of a repository method name.
     */
    @Test
    public void testEmptyOrderByInMethodName() {
        try {
            List<Voter> found = voters.findByAddressContainsOrderByAsc("Broadway Ave");
            fail("Ordered by an empty string attribute name and returned " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1024E:") ||
                !x.getMessage().contains("findByAddressContainsOrderByAsc"))
                throw x;
        }
    }

    /**
     * Verify MappingException is raised if attempting to supply an empty string
     * value instead of a valid named parameter name to the Param annotation.
     */
    @Test
    public void testEmptyParam() {
        try {
            List<Voter> found = voters.inWard(3);
            fail("Queried with an empty string named parameter and found " + found);
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1104E:") ||
                !x.getMessage().contains("inWard"))
                throw x;
        }
    }

    /**
     * Repository methods with return types requiring a single entity must
     * raise EmptyResultException when no entity matches.
     */
    @Test
    public void testEmptyResultException() {
        try {
            Voter v = voters.findBySSNBetweenAndNameNotNull(-28, -24);
            fail("Unexpected SSN for " + v);
        } catch (EmptyResultException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1053E") &&
                x.getMessage().contains("findBySSNBetweenAndNameNotNull"))
                ; // expected
            else
                throw x;
        }

        try {
            long n = voters.findSSNAsLongBetween(-36, -32);
            fail("Unexpected SSN " + n);
        } catch (EmptyResultException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1053E") &&
                x.getMessage().contains("findSSNAsLongBetween"))
                ; // expected
            else
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
     * Verify an error is raised when an entity class has Jakarta Persistence
     * annotations on its members but lacks the Entity annotation on the
     * entity class.
     */
    @Test
    public void testEntityClassMissingAnno() {
        Invitation inv = new Invitation();
        inv.id = 50006;
        inv.place = "Rochester, MN";
        inv.time = LocalDateTime.now().plusHours(5);
        inv.invitees = Set.of("invitee1@openliberty.io",
                              "invitee2@openliberty.io");

        try {
            errEntityClassMissingAnnoRepo.invite(inv);

            fail("Used an entity that has Jakarta Persistence annotations on" +
                 " members, but lacks the Entity annotation.");
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1108E:") ||
                !x.getMessage().contains("jakarta.persistence.Entity"))
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
     * Find-and-delete repository operations that return invalid types that are neither the entity class,
     * record class, or id class.
     */
    @Test
    public void testFindAndDeleteReturnsInvalidTypes() {

        // test data includes an entity with this address:
        final String address = "4051 E River Rd NE, Rochester, MN 55906";

        Sort<Voter> sort = Sort.asc("ssn");

        try {
            char[] deleted = voters.deleteReturnCharByAddress(address,
                                                              Limit.of(3),
                                                              sort);
            fail("Deleted with return type of char[]: " + Arrays.toString(deleted) +
                 " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }

        try {
            List<String> deleted = voters.deleteReturnStringByAddress(address,
                                                                      Limit.of(4),
                                                                      sort);
            fail("Deleted with return type of List of String: " + deleted +
                 " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }

        try {
            Page<Boolean> deleted = voters.deleteReturnBooleanByAddress(address,
                                                                        Limit.of(5),
                                                                        sort);
            fail("Deleted with return type of Page of Boolean: " + deleted +
                 " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }
    }

    /**
     * Find-and-delete repository operations that return invalid types that are neither the entity class,
     * record class, or id class.
     * In this case the table is empty and no results will have been deleted,
     * we should still throw a mapping exception.
     */
    @Test
    public void testFindAndDeleteReturnsInvalidTypesEmpty() {

        // test data does not include any entities with this address:
        final String address = "2800 37th St NW, Rochester, MN 55901";

        Sort<Voter> sort = Sort.asc("ssn");

        try {
            char[] deleted = voters.deleteReturnCharByAddress(address,
                                                              Limit.of(3),
                                                              sort);
            fail("Deleted with return type of char[]: " + Arrays.toString(deleted) +
                 " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }

        try {
            List<String> deleted = voters.deleteReturnStringByAddress(address,
                                                                      Limit.of(4),
                                                                      sort);
            fail("Deleted with return type of List of String: " + deleted +
                 " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }

        try {
            Page<Boolean> deleted = voters.deleteReturnBooleanByAddress(address,
                                                                        Limit.of(5),
                                                                        sort);
            fail("Deleted with return type of Page of Boolean: " + deleted +
                 " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }
    }

    /**
     * Verify an appropriate error is raised for the invalid combination of the
     * IgnoreCase and In keywords on a Query by Method Name method.
     */
    @Test
    public void testIgnoreCaseIn() {
        Set<String> addresses = Set.of("401 9th Ave NW, Rochester, MN 55901",
                                       "88 23RD AVE SW, Rochester, MN 55902");
        try {
            List<Voter> found = voters.findByAddressIgnoreCaseIn(addresses);
            fail("Should not be able to combine IgnoreCase and In keywords." +
                 " Found: " + found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1074E:") ||
                !x.getMessage().contains("IgnoreCase") ||
                !x.getMessage().contains("In"))
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
                        "88 23rd Ave SW, Rochester, MN 55902", //
                        "valerie@openliberty.io");
        Voter v2 = new Voter(400500600, "Vinny", //
                        LocalDate.of(1988, Month.NOVEMBER, 8), //
                        "2016 45th St SE, Rochester, MN 55904", //
                        "vinny@openliberty.io");
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
     * A repository might attempt to define a method that returns a CursoredPage
     * without specifying a PageRequest and attempt to use a Limit parameter
     * instead. This is not supported by the spec.
     * Expect UnsupportedOperationException.
     */
    @Test
    public void testLacksPageRequestUseLimitInstead() {
        CursoredPage<Voter> page;
        try {
            page = voters.findBySsnBetweenAndAddressNotNull(150000000,
                                                            450000000,
                                                            Limit.of(5));
            fail("Able to obtain CursoredPage without a PageRequest: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1041E") ||
                !x.getMessage().contains("findBySsnBetweenAndAddressNotNull"))
                throw x;
        }
    }

    /**
     * A repository might attempt to define a method that returns a CursoredPage
     * without specifying a PageRequest and attempt to use a Sort parameter instead.
     * This is not supported by the spec. Expect UnsupportedOperationException.
     */
    @Test
    public void testLacksPageRequestUseSortInstead() {
        CursoredPage<Voter> page;
        try {
            page = voters.findBySsnBetweenAndBirthdayNotNull(300000000, //
                                                             400000000, //
                                                             Sort.asc(ID));
            fail("Able to obtain CursoredPage without a PageRequest: " + page);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1041E") ||
                !x.getMessage().contains("findBySsnBetweenAndBirthdayNotNull"))
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
                                        "213 13th Ave NW, Rochester, MN 55901", //
                                        "voter1@openliberty.io"),

                            new Voter(777665555, "New Voter 2", //
                                            LocalDate.of(1987, Month.NOVEMBER, 7), //
                                            "300 7th St SW, Rochester, MN 55902", //
                                            "voter2@openliberty.io"));

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
                !x.getMessage().startsWith("CWWKD1014E") ||
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
     * Repository methods with return types allowing at most a single entity must
     * raise NonUniqueResultException when multiple entities match.
     */
    @Test
    public void testNonUniqueResultException() {
        try {
            Voter v = voters.findBySSNBetweenAndNameNotNull(700000000, 999999999);
            fail("Should find more Voter entities than " + v);
        } catch (NonUniqueResultException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1054E") &&
                x.getMessage().contains("findBySSNBetweenAndNameNotNull"))
                ; // expected
            else
                throw x;
        }

        try {
            long n = voters.findSSNAsLongBetween(700000000, 999999999);
            fail("Should find more numbers than " + n);
        } catch (NonUniqueResultException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1054E") &&
                x.getMessage().contains("findSSNAsLongBetween"))
                ; // expected
            else
                throw x;
        }

        try {
            Optional<Voter> v = voters.deleteByNameStartsWith("V");
            fail("Should get NonUniqueResultException when there are multiple" +
                 " results but a singular return type. Instead, result is: " + v);
        } catch (NonUniqueResultException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1054E") &&
                x.getMessage().contains("deleteByNameStartsWith"))
                ; // expected
            else
                throw x;
        }

        try {
            Optional<Voter> v = voters.deleteFirst();
            fail("Expected voters.deleteFirst() to ignore the 'first' keyword" +
                 " and attempt to delete and not return a singular result." +
                 " Instead returned: " + v);
        } catch (NonUniqueResultException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1054E") &&
                x.getMessage().contains("deleteFirst"))
                ; // expected
            else
                throw x;
        }
    }

    /**
     * Supply a null Limit results in NullPointerException.
     */
    @Test
    public void testNullLimit() {
        try {
            List<Voter> found = voters
                            .findBySsnLessThanEqualOrderBySsnDesc(999999999, null);
            fail("Repository method with a null Limit must raise" +
                 " NullPointerException. Instead: " + found);
        } catch (NullPointerException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1087E") &&
                x.getMessage().contains(Limit.class.getName()))
                ; // expected
            else
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
     * Attempt to supply a NULL Sort parameter.
     */
    @Test
    public void testNullSortArgument() {
        Page<Voter> page;
        try {
            page = voters.selectByName("Vincent",
                                       PageRequest.ofSize(9),
                                       null);
            fail("Obtained a page sorted by NULL: " + page);
        } catch (NullPointerException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1087E") &&
                x.getMessage().contains(Sort.class.getName()))
                ; // expected
            else
                throw x;
        }
    }

    /**
     * Attempt to supply a NULL varargs Sort parameter.
     */
    @Test
    public void testNullSortArray() {
        Page<Voter> page;
        try {
            page = voters.selectAll(PageRequest.ofSize(3),
                                    (Sort[]) null);
            fail("Obtained a page sorted by NULL: " + page);
        } catch (NullPointerException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1087E") &&
                x.getMessage().contains(Sort.class.getName() + "[]"))
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
     * Supply a PageRequest that requires CURSOR_NEXT to a repository method that
     * returns an offset-based Page rather than a CursoredPage. Expect an error.
     */
    @Test
    public void testOffsetPageRequestedWithCursorNext() {
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
                !x.getMessage().contains(Mode.CURSOR_NEXT.toString()) ||
                !x.getMessage().contains("atAddress"))
                throw x;
        }
    }

    /**
     * Supply a PageRequest that requires CURSOR_PREVIOUS to a repository method that
     * returns an offset-based Page rather than a CursoredPage. Expect an error.
     */
    @Test
    public void testOffsetPageRequestedWithCursorPrevious() {
        Order<Voter> nameAsc = Order.by(Sort.asc("name"), Sort.asc("ssn"));
        PageRequest before987654321 = PageRequest.ofSize(5) //
                        .beforeCursor(Cursor.forKey("Voter Name", 987654321));
        try {
            Page<Voter> page = voters.findAll(before987654321, nameAsc);
            fail("Obtained an offset page from a PageRequest that contains a" +
                 " Cursor: " + page);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1035E:") ||
                !x.getMessage().contains(Mode.CURSOR_PREVIOUS.toString()) ||
                !x.getMessage().contains("findAll"))
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
     * Exceed the maximum offset allowed by JPA.
     */
    @Test
    public void testOverflow() {
        Limit range = Limit.range(Integer.MAX_VALUE + 5L, Integer.MAX_VALUE + 10L);
        try {
            List<Voter> found = voters.findBySsnLessThanEqualOrderBySsnDesc(999999999,
                                                                            range);
            fail("Expected an error because starting position of range exceeds" +
                 " Integer.MAX_VALUE. Found: " + found);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1073E") &&
                x.getMessage().contains("Limit[maxResults=6, startAt=2147483652]"))
                ; // expected
            else
                throw x;
        }

        try {
            Stream<Voter> found = voters.findFirst2147483648BySsnGreaterThan(1);
            fail("Expected an error because limit exceeds Integer.MAX_VALUE. Found: " +
                 found);
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1028E") &&
                x.getMessage().contains("2147483648"))
                ; // expected
            else
                throw x;
        }

        try {
            PageRequest pageReqWithInvalidOffset = PageRequest
                            .ofPage(33)
                            .size(Integer.MAX_VALUE / 30);
            CursoredPage<Voter> found = voters.selectByBirthday(LocalDate.of(2000, 3, 13),
                                                                pageReqWithInvalidOffset,
                                                                Order.by(Sort.asc("ssn")));
            fail("Expected an error because offset for pagination exceeds" +
                 " Integer.MAX_VALUE. Found: " + found);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1043E") &&
                x.getMessage().contains("page=33"))
                ; // expected
            else
                throw x;
        }

        try {
            PageRequest pageReqWithInvalidOffset = PageRequest
                            .ofPage(22)
                            .size(Integer.MAX_VALUE / 20);
            Page<Voter> found = voters.selectAll(pageReqWithInvalidOffset,
                                                 Sort.desc("ssn"));
            fail("Expected an error because offset for pagination exceeds" +
                 " Integer.MAX_VALUE. Found: " + found);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1043E") &&
                x.getMessage().contains("page=22"))
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
     * Verify an error is raised when an entity class has Jakarta Persistence
     * annotations on its members but lacks the Entity annotation on the
     * entity class.
     */
    @Test
    public void testRecordEntityWithJakartaPersistenceAnno() {
        Investment ibm = new Investment(1, 232.64f, "IBM");

        try {
            errRecordEnityWithJPAAnnoRepo.invest(ibm);

            fail("Used a record entity that has a Jakarta Persistence annotation" +
                 " on a record component.");
        } catch (MappingException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1109E:") ||
                !x.getMessage().contains("jakarta.persistence.Column"))
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
        } catch (DataException x) {
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
        } catch (DataException x) {
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
        } catch (DataException x) {
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
            Invention i = errEntityMissingIdRepo //
                            .save(new Invention(1, 2, "Perpetual Motion Machine"));
            fail("Should not be able to use a repository operation for an entity" +
                 " that is not valid because it has no Id attribute. Saved: " + i);
        } catch (DataException x) {
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
        } catch (DataException x) {
            if (x.getMessage() == null ||
                !x.getMessage().startsWith("CWWKD1077E:") ||
                !x.getMessage().contains("<dataSource id=\"DefaultDataSource\""))
                throw x;
        }
    }

    /**
     * Verify that an appropriate error is raised when a repository method name for
     * Query by Method Name includes a reserved keyword in the OrderBy part of the
     * method name.
     */
    @Test
    public void testReservedKeywordInOrderByOfMethodName() {
        try {
            List<Voter> found = voters.findByNameNotNullOrderByDescriptionAsc();
            fail("Should not be able to OrderBy an entity attribute name that" +
                 " contains a reserved keyword. Found: " + found);
        } catch (MappingException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1105E") &&
                x.getMessage().contains("findByNameNotNullOrderByDescriptionAsc"))
                ; // expected
            else
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
