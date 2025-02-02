/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.web;

import static componenttest.annotation.SkipIfSysProp.DB_DB2;
import static componenttest.annotation.SkipIfSysProp.DB_Oracle;
import static componenttest.annotation.SkipIfSysProp.DB_Postgres;
import static componenttest.annotation.SkipIfSysProp.DB_SQLServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;
import io.openliberty.jpa.data.tests.models.Account;
import io.openliberty.jpa.data.tests.models.AccountId;
import io.openliberty.jpa.data.tests.models.Annuity;
import io.openliberty.jpa.data.tests.models.AsciiCharacter;
import io.openliberty.jpa.data.tests.models.Box;
import io.openliberty.jpa.data.tests.models.Business;
import io.openliberty.jpa.data.tests.models.City;
import io.openliberty.jpa.data.tests.models.CityId;
import io.openliberty.jpa.data.tests.models.Coordinate;
import io.openliberty.jpa.data.tests.models.County;
import io.openliberty.jpa.data.tests.models.DemographicInfo;
import io.openliberty.jpa.data.tests.models.DemographicInformation;
import io.openliberty.jpa.data.tests.models.ECEntity;
import io.openliberty.jpa.data.tests.models.Item;
import io.openliberty.jpa.data.tests.models.Line;
import io.openliberty.jpa.data.tests.models.Line.Point;
import io.openliberty.jpa.data.tests.models.NaturalNumber;
import io.openliberty.jpa.data.tests.models.Package;
import io.openliberty.jpa.data.tests.models.Participant;
import io.openliberty.jpa.data.tests.models.Person;
import io.openliberty.jpa.data.tests.models.Prime;
import io.openliberty.jpa.data.tests.models.Product;
import io.openliberty.jpa.data.tests.models.PurchaseOrder;
import io.openliberty.jpa.data.tests.models.Rating;
import io.openliberty.jpa.data.tests.models.Rebate;
import io.openliberty.jpa.data.tests.models.Rebate.Status;
import io.openliberty.jpa.data.tests.models.Reciept;
import io.openliberty.jpa.data.tests.models.Segment;
import io.openliberty.jpa.data.tests.models.Store;
import io.openliberty.jpa.data.tests.models.Triangle;
import io.openliberty.jpa.data.tests.models.Vehicle;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.RollbackException;
import jakarta.transaction.UserTransaction;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JakartaDataRecreate")
public class JakartaDataRecreateServlet extends FATServlet {

    @PersistenceContext(unitName = "RecreatePersistenceUnit")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void alwaysPasses() {
        assertTrue(true);
    }

    @Test
    @SkipIfSysProp({ DB_Postgres })
    public void testOLGH28912() throws Exception {
        Coordinate original = Coordinate.of("testOLGH28912", 10, 15f);
        UUID id = original.id;
        Coordinate result;

        tx.begin();

        em.persist(original);
        tx.commit();
        tx.begin();
        try {
            // FAILURE PARSING QUERY HERE
            em.createQuery("UPDATE Coordinate SET x = :newX, y = y / :yDivisor WHERE id = :id")
                            .setParameter("newX", 11)
                            .setParameter("yDivisor", 5)
                            .setParameter("id", id)
                            .executeUpdate();
            tx.commit();

        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreated
             * Exception Description: Syntax error parsing [UPDATE Coordinate SET x = :newX,
             * y = y / :yDivisor WHERE id = :id].
             * [37, 38] The left expression is not an arithmetic expression.
             */
            throw e;
        }
        tx.begin();
        result = em.createQuery("SELECT this from Coordinate WHERE id = :id", Coordinate.class)
                        .setParameter("id", id)
                        .getSingleResult();
        tx.commit();
        assertEquals(id, result.id);
        assertEquals(11, result.x, 0.001);
        assertEquals(3f, result.y, 0.001);
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28913
    public void testOLGH28913() throws Exception {
        AsciiCharacter character = AsciiCharacter.of(80); // P
        String result;

        tx.begin();

        try {
            em.persist(character);

            result = em.createQuery(
                                    "SELECT hexadecimal FROM AsciiCharacter WHERE hexadecimal IS NOT NULL AND thisCharacter = ?1",
                                    String.class) // FAILURE PARSING QUERY HERE
                            .setParameter(1, character.getThisCharacter())
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreated
             * Exception Description: Problem compiling [SELECT hexadecimal FROM
             * AsciiCharacter WHERE hexadecimal IS NOT NULL AND thisCharacter = ?1].
             * [7, 18] The identification variable 'hexadecimal' is not defined in the FROM
             * clause.
             */
            throw e;
        }

        assertEquals(character.getHexadecimal(), result);
    }

    @Test
    @Ignore("Reference issue:https://github.com/OpenLiberty/open-liberty/issues/29459")
    public void testOLGH29459() throws Exception {
        int x1 = 0, y1 = 0, x2 = 120, y2 = 209;
        Segment segment = Segment.of(x1, y1, x2, y2);
        List<Exception> exceptions = new ArrayList<>();

        tx.begin();

        try {
            em.persist(segment);
            tx.commit();
        } catch (Exception e) {
            if (tx.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                // Only rollback if it's not a RollbackException
                if (!(e instanceof jakarta.transaction.RollbackException)) {
                    tx.rollback();
                }
            }
            exceptions.add(e);
        }

        tx.begin();

        try {
            em.createNativeQuery("INSERT INTO Segment (id, pointA_x, pointA_y, pointB_x, pointB_y) VALUES (?, ?, ?, ?, ?)")
                            .setParameter(1, segment.id + 1)
                            .setParameter(2, segment.pointA.x())
                            .setParameter(3, segment.pointA.y())
                            .setParameter(4, segment.pointB.x())
                            .setParameter(5, segment.pointB.y())
                            .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
                // Only rollback if it's not a RollbackException
                if (!(e instanceof jakarta.transaction.RollbackException)) {
                    tx.rollback();
                }
            }
            exceptions.add(e);
        }

        if (!exceptions.isEmpty()) {
            throw exceptions.get(0);
        }

        Segment retrievedSegment1 = em.find(Segment.class, segment.id);
        Segment retrievedSegment2 = em.find(Segment.class, segment.id + 1);

        // Assertions for the first segment
        assertEquals(segment.id, retrievedSegment1.id);
        assertEquals(x1, retrievedSegment1.pointA.x());
        assertEquals(y1, retrievedSegment1.pointA.y());
        assertEquals(x2, retrievedSegment1.pointB.x());
        assertEquals(y2, retrievedSegment1.pointB.y());

        // Assertions for the second segment (inserted with incremented ID)
        assertEquals(Long.valueOf(segment.id + 1), Long.valueOf(retrievedSegment2.id));
        assertEquals(x1, retrievedSegment2.pointA.x());
        assertEquals(y1, retrievedSegment2.pointA.y());
        assertEquals(x2, retrievedSegment2.pointB.x());
        assertEquals(y2, retrievedSegment2.pointB.y());
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28908
    public void testOLGH28908() throws Exception {
        Person p = new Person();
        p.firstName = "John";
        p.lastName = "Jacobs";
        p.ssn_id = 111111111l;

        Person result;

        tx.begin();

        try {
            em.persist(p);
            em.createQuery("UPDATE Person SET firstName=:newFirstName WHERE id(this)=:ssn")
                            .setParameter("newFirstName", "Jack")
                            .setParameter("ssn", p.ssn_id)
                            .executeUpdate();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreated
             * Exception Description: Internal problem encountered while compiling [UPDATE
             * Person SET firstName=:newFirstName WHERE id(this)=:ssn].
             * Internal Exception: java.lang.NullPointerException: Cannot invoke
             * "org.eclipse.persistence.internal.jpa.jpql.Declaration.getDescriptor()"
             * because the return value of
             * "org.eclipse.persistence.internal.jpa.jpql.JPQLQueryContext.getDeclaration(java.lang.String)"
             * is null
             */
            throw e;
        }
        tx.begin();
        result = em.createQuery("SELECT this from Person WHERE ssn_id = :ssn", Person.class)
                        .setParameter("ssn", p.ssn_id)
                        .getSingleResult();
        tx.commit();
        assertEquals(p.ssn_id, result.ssn_id);
        assertEquals("Jack", result.firstName);
        assertEquals(p.lastName, result.lastName);
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28874
    public void testOLGH28874() throws Exception {
        NaturalNumber two = NaturalNumber.of(2);
        NaturalNumber three = NaturalNumber.of(3);
        NaturalNumber result1 = null, result2 = null;

        List<Exception> exceptions = new ArrayList<>();

        tx.begin();
        em.persist(two);
        em.persist(three);
        tx.commit();

        tx.begin();
        try {
            result1 = em.createQuery(
                                     "FROM NaturalNumber WHERE isOdd = false AND numType = io.openliberty.jpa.data.tests.models.NaturalNumber.NumberType.PRIME",
                                     NaturalNumber.class)
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            exceptions.add(e);
        }

        tx.begin();
        try {
            result2 = em.createQuery(
                                     "FROM NaturalNumber WHERE this.isOdd = false AND this.numType = io.openliberty.jpa.data.tests.models.NaturalNumber.NumberType.PRIME",
                                     NaturalNumber.class)
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            exceptions.add(e);
        }

        if (!exceptions.isEmpty()) {

            /*
             * Recreate
             * Exception Description: Object comparisons can only be used with
             * OneToOneMappings. Other mapping comparisons must be done through query keys
             * or direct attribute level
             * comparisons.
             * Mapping: [org.eclipse.persistence.mappings.DirectToFieldMapping[numType-->
             * NATURALNUMBER.NUMTYPE]]
             * Expression: [
             * Query Key numType
             * Base io.openliberty.jpa.data.tests.models.NaturalNumber]
             * Query: ReadAllQuery(referenceClass=NaturalNumber
             * jpql="FROM NaturalNumber WHERE isOdd = false AND numType = io.openliberty.jpa.data.tests.models.NaturalNumber.NumberType.PRIME"
             * )
             */
            throw exceptions.get(0);
        }

        assertEquals(2l, result1.getId(), 0.001f);
        assertEquals(2l, result2.getId(), 0.001f);
    }

    @Test //Original issue: https://github.com/OpenLiberty/open-liberty/issues/28920
    @Ignore("Additional issue: https://github.com/OpenLiberty/open-liberty/issues/28874")
    public void testOLGH28920() throws Exception {
        Rebate r1 = Rebate.of(10.00, "testOLGH28920", LocalTime.now().minusHours(1), LocalDate.now(), Status.SUBMITTED,
                              LocalDateTime.now(), 1);
        Rebate r2 = Rebate.of(12.00, "testOLGH28920", LocalTime.now().minusHours(1), LocalDate.now(), Status.PAID,
                              LocalDateTime.now(), 2);
        Rebate r3 = Rebate.of(14.00, "testOLGH28920", LocalTime.now().minusHours(1), LocalDate.now(), Status.PAID,
                              LocalDateTime.now(), 2);

        List<Rebate> paidRebates;

        tx.begin();
        em.persist(r1);
        em.persist(r2);
        em.persist(r3);
        tx.commit();

        tx.begin();
        try {

            paidRebates = em.createQuery(
                                         "SELECT NEW io.openliberty.jpa.data.tests.models.Rebate(id, amount, customerId, purchaseMadeAt, purchaseMadeOn, status, updatedAt, version) "
                                         + "FROM Rebate "
                                         + "WHERE customerId=?1 AND status=io.openliberty.jpa.data.tests.models.Rebate.Status.PAID "
                                         + "ORDER BY amount DESC, id ASC",
                                         Rebate.class)
                            .setParameter(1, "testOLGH28920")
                            .getResultList();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * Exception Description: Syntax error parsing [SELECT NEW
             * io.openliberty.jpa.data.tests.models.Rebate(id, amount, customerId,
             * purchaseMadeAt, purchaseMadeOn, status,
             * updatedAt, version) FROM Rebate WHERE customerId=?1 AND
             * status=io.openliberty.jpa.data.tests.models.Rebate.Status.PAID ORDER BY
             * amount DESC, id ASC].
             * [55, 57] The identification variable 'id' cannot be a reserved word.
             * [130, 137] The identification variable 'version' cannot be a reserved word.
             */
            throw e;
        }

        assertEquals(2, paidRebates.size());
        assertEquals(14.00, paidRebates.get(0).amount, 0.001);
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28909
    public void testOLGH28909() throws Exception {
        deleteAllEntities(Box.class);

        Box cube = Box.of("testOLGH28909", 1, 1, 1);

        Box wall; // box with no width

        tx.begin();
        em.persist(cube);
        tx.commit();

        tx.begin();
        try {
            em.createQuery("UPDATE Box SET length = length + ?1, width = width - ?1, height = height * ?2")
                            .setParameter(1, 1)
                            .setParameter(2, 2)
                            .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * Exception Description: Syntax error parsing [UPDATE Box SET length = length +
             * ?1, width = width - ?1, height = height * ?2].
             * [30, 30] The left parenthesis is missing from the LENGTH expression.
             * [45, 50] The left expression is not an arithmetic expression.
             * [66, 72] The left expression is not an arithmetic expression.
             */
            throw e;
        }
        tx.begin();
        wall = em.createQuery("SELECT this from Box WHERE boxIdentifier = :id", Box.class)
                        .setParameter("id", "testOLGH28909")
                        .getSingleResult();
        tx.commit();

        assertEquals("testOLGH28909", wall.boxIdentifier);
        assertEquals(2, wall.length); // 1+1
        assertEquals(0, wall.width); // 1-1
        assertEquals(2, wall.height); // 1*2
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28931")
    public void testOLGH28931() throws Exception {
        Business ibmRoc = Business.of(44.05887f, -92.50355f, "Rochester", "Minnesota", 55901, 2800, "37th St", "NW",
                                      "IBM Rochester");
        Business ibmRTP = Business.of(35.90481f, -78.85026f, "Durham", "North Carolina", 27703, 4204, "Miami Blvd", "S",
                                      "IBM RTP");

        Business result;

        tx.begin();
        em.persist(ibmRoc);
        em.persist(ibmRTP);
        tx.commit();

        tx.begin();
        try {
            result = em.createQuery("FROM Business WHERE location.address.city=?1 ORDER BY name", Business.class)
                            .setParameter(1, "Rochester")
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * Exception Description: Internal problem encountered while compiling [FROM
             * Business WHERE location.address.city=?1 ORDER BY name].
             * Internal Exception: java.lang.IndexOutOfBoundsException: Index 1 out of
             * bounds for length 1
             */
            throw e;
        }

        assertEquals("IBM Rochester", result.name);
        assertEquals(55901, result.location.address.zip);
    }

    @Test //Reference issue: https://github.com/eclipse-ee4j/eclipselink/issues/2234
    public void testELGH2234() throws Exception {

        Product p = Product.of("testSnapshot", "product", 10.50f);
        tx.begin();
        em.persist(p);
        tx.commit();

        tx.begin();
        try {

            em.createQuery("FROM Product WHERE (:rate * price <= :max AND :rate * price >= :min) ORDER BY name",
                           Product.class)
                            .setParameter("rate", 4)
                            .setParameter("max", 100)
                            .setParameter("min", 1)
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @Test //Original Issue: https://github.com/OpenLiberty/open-liberty/issues/29457"
    @SkipIfSysProp(DB_Oracle) //Additional Issue: https://github.com/OpenLiberty/open-liberty/issues/29440
    public void testOLGH29457() throws Exception {

        // Create a DemographicInfo instance
        DemographicInfo demographicInfo = DemographicInfo.of(2023, 8, 21, 500, 200000.00, 1000000.00);

        // Begin transaction and persist the entity
        tx.begin();
        em.persist(demographicInfo);
        tx.commit();

        // Begin a new transaction to execute the query
        tx.begin();
        try {
            // Execute the query
            BigDecimal result = em.createQuery(
                                               "SELECT publicDebt / numFullTimeWorkers FROM DemographicInfo WHERE EXTRACT(YEAR FROM collectedOn) = ?1",
                                               BigDecimal.class)
                            .setParameter(1, 2023)
                            .getSingleResult();

            // Assuming some assertion or validation
            BigDecimal expected = new BigDecimal("2000.00");
            BigDecimal actual = result;

            // Define the precision for comparison
            BigDecimal tolerance = new BigDecimal("0.01");

            assertTrue("Expected: " + expected + ", but was: " + actual, expected.subtract(actual).abs().compareTo(tolerance) < 0);

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @Test
    @Ignore("Reference Issue: https://github.com/OpenLiberty/open-liberty/issues/29319")
    // This test fails with createNamedQuery. For recreating you can uncomment the
    // @NamedQuery annotation in Annuity.java
    public void testOLGH29319() throws Exception {

        Annuity annuity = Annuity.of("holder123", 2500.00);
        tx.begin();
        em.persist(annuity);
        tx.commit();
        tx.begin();
        try {
            em.createNamedQuery("TEST_OLGH_29319", Annuity.class)
                            .setParameter("holderId", "holder123")
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @Test // Reference Issue: https://github.com/OpenLiberty/open-liberty/issues/29319
    // This test will be passing with createQuery method.
    public void testOLGH29319_2() throws Exception {

        Annuity annuity = Annuity.of("holder123", 2500.00);
        tx.begin();
        em.persist(annuity);
        tx.commit();
        tx.begin();
        try {
            em.createQuery("FROM Annuity WHERE annuityHolderId = :holderId", Annuity.class)
                            .setParameter("holderId", "holder123")
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28925")
    public void testOLGH28925() throws Exception {
        deleteAllEntities(Prime.class); // Cleanup any left over entities

        Prime two = Prime.of(2, "II", "two");
        Prime three = Prime.of(3, "III", "three");
        Prime five = Prime.of(5, "V", "five");
        Prime seven = Prime.of(7, "VII", "seven");

        List<Prime> primes;

        tx.begin();
        em.persist(two);
        em.persist(three);
        em.persist(five);
        em.persist(seven);
        tx.commit();

        tx.begin();
        try {
            primes = em.createQuery(
                                    "SELECT ID(THIS) FROM Prime o WHERE (o.name = :numberName OR :numeral=o.romanNumeral OR o.hex =:hex OR ID(THIS)=:num) ORDER BY o.numberId",
                                    Prime.class)
                            .setParameter("numberName", "two")
                            .setParameter("numeral", "III")
                            .setParameter("hex", "5")
                            .setParameter("num", 7)
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * Exception Description: Problem compiling [SELECT ID(THIS) FROM Prime o
             * WHERE (o.name = :numberName OR :numeral=o.romanNumeral OR o.hex =:hex OR
             * ID(THIS)=:num)
             * ORDER BY o.numberId].
             * [10, 14] The identification variable 'THIS' is not defined in the FROM
             * clause.
             * [108, 112] The identification variable 'THIS' is not defined in the FROM
             * clause.
             */
            throw e;
        }

        assertEquals(4, primes.size());
    }
    @Test
    // "Reference issue: https://github.com/OpenLiberty/open-liberty/issues/30093"
    public void testOLGH30093() throws Exception {
        deleteAllEntities(Prime.class); // Cleanup any left over entities

        Prime two = Prime.of(2, "II", "two");
        Prime three = Prime.of(3, "III", "three");
        Prime five = Prime.of(5, "V", "five");
        Prime seven = Prime.of(7, "VII", "seven");
        List<Long> ids;

        tx.begin();
        em.persist(two);
        em.persist(three);
        em.persist(five);
        em.persist(seven);
        tx.commit();

        tx.begin();
        try {
             ids = em.createQuery(
                                    "SELECT ID(THIS) FROM Prime WHERE ID(THIS) < ?1 ORDER BY ID(THIS) DESC",
                                    Long.class)
                            .setParameter(1, 7)
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        
        assertEquals(3, ids.size());
        assertEquals(5L, ids.get(0).longValue());
        assertEquals(3L, ids.get(1).longValue());
        assertEquals(2L, ids.get(2).longValue());
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29117")
    public void testOLGH29117() throws Exception {
        Segment unitRadius = Segment.of(0, 0, 0, 1);

        tx.begin();
        try {
            em.persist(unitRadius);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @Test
    @SkipIfSysProp(DB_Oracle) //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28545
    public void testOLGH28545_1() throws Exception {
        deleteAllEntities(Package.class); // Cleanup any left over entities

        Package p1 = Package.of(1, 1.0f, 1.0f, 1.0f, "testOLGH28545-1");
        Package p2 = Package.of(2, 1.0f, 2.0f, 1.0f, "testOLGH28545-2");

        Package result;

        tx.begin();
        em.persist(p1);
        em.persist(p2);
        tx.commit();

        tx.begin();
        try {
            result = em.createQuery("SELECT o FROM Package o ORDER BY o.width DESC", Package.class)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .setMaxResults(1)
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        /**
         * Reproduce
         * Generated SQL:
         *
         * <pre>
         *   SELECT ID AS a1, DESCRIPTION AS a2, HEIGHT AS a3, LENGTH AS a4, WIDTH AS a5
         *   FROM PACKAGE
         *   WHERE (ID) IN (
         *   SELECT a1 FROM (
         *       SELECT a1, ROWNUM rnum  FROM (
         *         SELECT ID AS a1, DESCRIPTION AS a2, HEIGHT AS a3, LENGTH AS a4, WIDTH AS a5
         *         FROM PACKAGE
         *         ORDER BY a1  // This nested order is what determines the result, instead of the outer order, when limiting the results.
         *       ) WHERE ROWNUM <= ?
         *     ) WHERE rnum > ?
         *   )  ORDER BY WIDTH DESC FOR UPDATE
         * </pre>
         */
        assertEquals(p2.description, result.description);
    }

    @Test
    @SkipIfSysProp(DB_Oracle) //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28545
    public void testOLGH28545_2() throws Exception {
        deleteAllEntities(Package.class); // Cleanup any left over entities

        Package p1 = Package.of(1, 1.0f, 1.0f, 1.0f, "testOLGH28545-1");
        Package p2 = Package.of(2, 1.0f, 2.0f, 1.0f, "testOLGH28545-2");

        List<Integer> results;

        tx.begin();
        em.persist(p1);
        em.persist(p2);
        tx.commit();

        tx.begin();
        try {
            results = em.createQuery("SELECT o.id FROM Package o ORDER BY o.width DESC", Integer.class)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .setMaxResults(1)
                            .getResultList();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        /**
         * Reproduce
         * Generated SQL:
         *
         * <pre>
         *   SELECT ID AS a1
         *   FROM PACKAGE
         *   WHERE (ID) IN (
         *     SELECT null FROM (  // ID will never be IN the result of a SELECT NULL statement therefore no results will ever be returned
         *       SELECT null, ROWNUM rnum  FROM (
         *         SELECT ID AS a1
         *         FROM PACKAGE
         *         ORDER BY null
         *       ) WHERE ROWNUM <= ?
         *     ) WHERE rnum > ?
         *   )  ORDER BY WIDTH DESC FOR UPDATE
         * </pre>
         */
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).intValue());

    }

    @Test
    @Ignore
    //Reference issue : https://github.com/OpenLiberty/open-liberty/issues/30444
    public void testOLGH30444() throws Exception {
        deleteAllEntities(Package.class); 

        Package p1 = Package.of(1, 1.0f, 1.0f, 1.0f, "testOLGH28545-1");
        Package p2 = Package.of(2, 1.0f, 2.0f, 1.0f, "testOLGH28545-2");

        List<Integer> results;

        tx.begin();
        em.persist(p1);
        em.persist(p2);
        tx.commit();

        tx.begin();
        try {
            results = em.createQuery("SELECT ID FROM Package ORDER BY WIDTH DESC", Integer.class)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .setMaxResults(1)
                            .getResultList();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).intValue());

    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28545
    @SkipIfSysProp({ DB_Postgres, DB_Oracle })
    public void testOLGH28545_3() throws Exception {
        deleteAllEntities(Prime.class);

        Prime two = Prime.of(2, "II", "two");
        Prime three = Prime.of(3, "III", "three");
        Prime five = Prime.of(5, "V", "five");
        Prime seven = Prime.of(7, "VII", "seven");

        List<Integer> lengths;

        tx.begin();
        em.persist(two);
        em.persist(three);
        em.persist(five);
        em.persist(seven);
        tx.commit();

        tx.begin();
        try {
            lengths = em.createQuery("SELECT DISTINCT LENGTH(p.romanNumeral) FROM Prime p "
                                     + "WHERE p.numberId <= ?1 ORDER BY LENGTH(p.romanNumeral) DESC", Integer.class)
                            .setParameter(1, 5)
                            .setMaxResults(4)
                            .getResultList();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /**
             * Recreate
             *
             * Internal Exception: java.sql.SQLSyntaxErrorException: ORA-00918:
             * LENGTH(ROMANNUMERAL): column ambiguously specified - appears in and
             * https://docs.oracle.com/error-help/db/ora-00918/
             * Error Code: 918
             * Call:
             *
             * <pre>
             *   SELECT * FROM (
             *     SELECT a.*, ROWNUM rnum  FROM (
             *       SELECT DISTINCT LENGTH(ROMANNUMERAL), LENGTH(ROMANNUMERAL)
             *       FROM PRIME
             *       WHERE (NUMBERID <= ?)
             *       ORDER BY LENGTH(ROMANNUMERAL) DESC
             *     ) a WHERE ROWNUM <= ?
             *   ) WHERE rnum > ?
             * </pre>
             *
             * bind => [3 parameters bound]
             */
            throw e;
        }

        assertEquals(3, lengths.size());
        assertEquals(3, lengths.get(0).intValue()); // III
        assertEquals(2, lengths.get(1).intValue()); // II
        assertEquals(1, lengths.get(2).intValue()); // V
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29073")
    public void testOLGH29073() throws Exception {
        deleteAllEntities(City.class);

        City RochesterMN = City.of("Rochester", "Minnesota", 121878, Set.of(55901, 55902, 55903, 55904, 55906));
        City RochesterNY = City.of("Rochester", "New York", 209352, Set.of(14601, 14602, 14603, 14604, 14606));

        List<CityId> rochesters;

        tx.begin();
        em.persist(RochesterMN);
        em.persist(RochesterNY);
        tx.commit();

        tx.begin();
        try {
            rochesters = em
                            .createQuery("SELECT ID(THIS) FROM City WHERE (name=?1) ORDER BY population DESC", CityId.class)
                            .setParameter(1, "Rochester")
                            .getResultList();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * io.openliberty.jpa.data.tests.web.JakartaDataRecreateServlet
             * java.lang.ClassCastException: java.lang.String incompatible with
             * io.openliberty.jpa.data.tests.models.CityId
             */
            throw e;
        }

        assertEquals(2, rochesters.size());
        assertEquals("New York", rochesters.get(0).getStateName());
        assertEquals("Minnesota", rochesters.get(1).getStateName());
    }

    @Test
    @SkipIfSysProp(DB_Postgres) //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28368
    public void testOLGH28368() throws Exception {
        deleteAllEntities(PurchaseOrder.class, "Orders");

        PurchaseOrder order1 = PurchaseOrder.of("testOLGH28368-1", 12.55f);
        PurchaseOrder order2 = PurchaseOrder.of("testOLGH28368-2", 12.55f);

        tx.begin();
        em.persist(order1);
        em.persist(order2);
        tx.commit();

        assertNotNull(order1.id); // order1 is now detached

        tx.begin();
        try {
            List<PurchaseOrder> results = em.createQuery("SELECT p FROM Orders p WHERE p.id=?1", PurchaseOrder.class)
                            .setParameter(1, order1.id)
                            .getResultList();

            assertEquals(1, results.size());
            assertEquals(order1.purchasedBy, results.get(0).purchasedBy);

            em.remove(results.get(0));

            tx.commit();
        } catch (RollbackException x) {
            /*
             * Recreate
             * Internal Exception: org.postgresql.util.PSQLException: ERROR: operator does
             * not exist: character varying = uuid
             * Hint: No operator matches the given name and argument types. You might need
             * to add explicit type casts.
             * Position: 31
             * Error Code: 0
             * Call: DELETE FROM ORDERS WHERE ((ID = ?) AND (VERSIONNUM = ?))
             * bind => [2 parameters bound]
             * Query: DeleteObjectQuery(io.openliberty.jpa.data.tests.models.PurchaseOrder@
             * b659d1ca)
             */
            throw x;
        } catch (Exception e) {
            tx.rollback();
            throw e; // Unexpected
        }
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28813
    public void testOLGH28813() throws Exception {
        deleteAllEntities(DemographicInfo.class);

        final ZoneId EASTERN = ZoneId.of("America/New_York");

        DemographicInfo US2024 = DemographicInfo.of(2024, 4, 30, 133809000, 7136033799632.56, 27480960216618.32);
        DemographicInfo US2023 = DemographicInfo.of(2023, 4, 28, 134060000, 6852746625848.93, 24605068022566.94);
        DemographicInfo US2022 = DemographicInfo.of(2022, 4, 29, 132250000, 6526909395140.41, 23847245116757.60);
        DemographicInfo US2007 = DemographicInfo.of(2007, 4, 30, 121090000, 3833110332444.19, 5007058051986.64);

        tx.begin();
        em.persist(US2024);
        em.persist(US2023);
        em.persist(US2022);
        em.persist(US2007);
        tx.commit();

        List<DemographicInfo> results;

        tx.begin();
        try {
            results = em.createQuery(
                                     "SELECT o FROM DemographicInfo o WHERE (o.publicDebt BETWEEN ?1 AND ?2) ORDER BY o.publicDebt",
                                     DemographicInfo.class)
                            .setParameter(1, BigDecimal.valueOf(5000000000000.00))
                            .setParameter(2, BigDecimal.valueOf(10000000000000.00))
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * TODO unable to recreate issue
             * Exception Description: The object [2007-04-30 11:00:00.0], of class [class
             * java.lang.String],
             * from mapping
             * [org.eclipse.persistence.mappings.DirectToFieldMapping[collectedOn-->
             * WLPDemographicInfo.COLLECTEDON]]
             * with descriptor
             * [RelationalDescriptor(test.jakarta.data.jpa.web.DemographicInfo -->
             * [DatabaseTable(WLPDemographicInfo)])],
             * could not be converted to [class java.time.Instant].
             * Internal Exception: java.time.format.DateTimeParseException: Text '2007-04-30
             * 11:00:00.0' could not be parsed at index 10
             */
            throw e;
        }

        assertEquals(1, results.size());
        assertEquals(2007, results.get(0).collectedOn.atZone(EASTERN).get(ChronoField.YEAR));

        System.out.println(results.get(0).toString());
    }

    @Test
    //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28928
    public void testOLGH28928() throws Exception {
        Item apple = Item.of("testOLGH28928-a", "apple", 7.00f);
        Item ball = Item.of("testOLGH28928-b", "ball", 10.00f);
        Item carrot = Item.of("testOLGH28928-c", "carrot", 0.50f);

        Double maxPrice;
        Double minPrice;
        Double avgPrice;

        tx.begin();
        em.persist(apple);
        em.persist(ball);
        em.persist(carrot);
        tx.commit();

        tx.begin();
        try {

            maxPrice = em.createQuery("SELECT MAX(price) FROM Item", Double.class)
                            .getSingleResult();

            minPrice = em.createQuery("SELECT MIN(price) FROM Item", Double.class)
                            .getSingleResult();

            avgPrice = em.createQuery("SELECT AVG(price) FROM Item", Double.class)
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * java.lang.IllegalArgumentException: An exception occurred while creating a
             * query in EntityManager:
             * Exception Description: Syntax error parsing [SELECT MAX(price) FROM Item].
             * [11, 16] The encapsulated expression is not a valid expression.
             */
            throw e;
        }

        assertEquals(10.00, maxPrice, 0.01);
        assertEquals(0.50, minPrice, 0.01);
        assertEquals(5.833, avgPrice, 0.01);
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28589")
    public void testOLGH28589_1() throws Exception {
        deleteAllEntities(City.class);

        City RochesterMN = City.of("Rochester", "Minnesota", 121878, Set.of(55901, 55902, 55903, 55904, 55906));
        City RochesterNY = City.of("Rochester", "New York", 209352, Set.of(14601, 14602, 14603, 14604, 14606));

        List<Set> RochesterAreaCodes;

        tx.begin();
        em.persist(RochesterMN);
        em.persist(RochesterNY);
        tx.commit();

        tx.begin();
        try {
            RochesterAreaCodes = em.createQuery("SELECT o.areaCodes FROM City o WHERE (o.name=?1)", Set.class)
                            .setParameter(1, "Rochester")
                            .getResultList();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        System.out.println(RochesterAreaCodes);
        System.out.println(RochesterAreaCodes.get(0));

        /**
         * Recreate
         * expected: <2> but was:<10>
         * Expected: [[55901, 55902, 55903, 55904, 55906], [14601, 14602, 14603, 14604,
         * 14606]]
         * Actual: [[55901], [55902], [55903], [55904], [55906], [14601], [14602],
         * [14603], [14604], [14606]]
         */
        assertEquals(2, RochesterAreaCodes.size());
        assertTrue(RochesterAreaCodes.contains(Set.of(55901, 55902, 55903, 55904, 55906)));
        assertTrue(RochesterAreaCodes.contains(Set.of(14601, 14602, 14603, 14604, 14606)));
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28589")
    public void testOLGH28589_2() throws Exception {
        deleteAllEntities(City.class);

        City RedWingMN = City.of("Red Wing", "Minnesota", 16672, Set.of(55066));

        Set<Integer> RedWingAreaCodes;

        tx.begin();
        em.persist(RedWingMN);
        tx.commit();

        tx.begin();
        try {
            RedWingAreaCodes = em.createQuery("SELECT o.areaCodes FROM City o WHERE (o.name=?1)", Set.class)
                            .setParameter(1, "Red Wing")
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * java.lang.ClassCastException: java.lang.Integer incompatible with
             * java.util.Set
             */
            throw e;
        }

        assertEquals(1, RedWingAreaCodes.size());
        assertEquals(55066, RedWingAreaCodes.stream().findFirst());
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28589")
    public void testOLGH28589_3() throws Exception {
        deleteAllEntities(City.class);

        City RochesterMN = City.of("Rochester", "Minnesota", 121878, Set.of(55901, 55902, 55903, 55904, 55906));

        Set<Integer> RochesterAreaCodes;

        tx.begin();
        em.persist(RochesterMN);
        tx.commit();

        tx.begin();
        try {
            RochesterAreaCodes = em.createQuery("SELECT o.areaCodes FROM City o WHERE (o.name=?1)", Set.class)
                            .setParameter(1, "Rochester")
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            /*
             * Recreate
             * jakarta.persistence.NonUniqueResultException: More than one result was
             * returned from Query.getSingleResult()
             */
            throw e;
        }

        assertEquals(5, RochesterAreaCodes.size());
        assertTrue(RochesterAreaCodes.containsAll(Set.of(55901, 55902, 55903, 55904, 55906)));
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/24926")
    public void testOLGH24926() throws Exception {
        Line unitRadius = Line.of(0, 0, 1, 1);

        Line origin;

        tx.begin();
        em.persist(unitRadius);
        tx.commit();

        tx.begin();
        em.createQuery("UPDATE Line o SET o.pointB = ?1 WHERE (o.id=?2)")
                        .setParameter(1, null)
                        .setParameter(2, unitRadius.id)
                        .executeUpdate(); // UPDATE LINE SET x_B = ? WHERE (ID = ?) bind => [null, 5]
        tx.commit();

        tx.begin();
        try {
            origin = em.createQuery("SELECT o FROM Line o WHERE (o.id=?1)", Line.class)
                            .setParameter(1, unitRadius.id)
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        assertEquals(Point.of(0, 0), origin.pointA);

        /*
         * Recreate
         * Expected: null
         * Actual: Point [x=0, y=1]
         */
        assertNull("PointB was not null, instead: " + origin.pointB, origin.pointB);
    }

    @Test // Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28737
    @SkipIfSysProp({ DB_Postgres, DB_SQLServer })
    public void testOLGH28737() throws Exception {
        deleteAllEntities(Box.class);

        Box cube = Box.of("testOLGH28737", 1, 1, 1);
        Box wall = Box.of("testOLGH28737", 1, 0, 1);

        tx.begin();
        try {
            em.persist(cube);
            em.persist(wall);
            tx.commit();
        } catch (RollbackException e) {
            assertTrue(e.getCause() instanceof PersistenceException);
            Throwable cause = e.getCause();

            // Ensure PersistenceException was caused by
            // SQLIntegrityConstraintViolationException
            while (cause != null) {
                if (cause instanceof SQLIntegrityConstraintViolationException) {
                    return; // passing result
                }
                cause = cause.getCause();
            }

            /*
             * Recreate - !! NOT AN ECLIPSELINK ISSUE !!
             * Caused by the PostgreSQL and Microsoft SQLServer drivers not throwing
             * SQLIntegrityConstraintViolationException
             * PostgreSQL: https://github.com/pgjdbc/pgjdbc/issues/963
             * SQLServer: https://github.com/microsoft/mssql-jdbc/issues/1199
             */
            fail("Caught PersistenceException, but it was not caused by SQLIntegrityConstraintViolationException");
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28289
    @SkipIfSysProp({ DB_Oracle })
    // DB2 resolved (Oracle outstanding): https://github.com/eclipse-ee4j/eclipselink/issues/2282
    public void testOLGH28289() throws Exception {
        deleteAllEntities(Package.class);

        Package p1 = Package.of(70071, 17.0f, 17.1f, 7.7f, "testOLGH28289#70071"); // tallest and smallest length
        Package p2 = Package.of(70077, 77.0f, 17.7f, 7.7f, "testOLGH28289#70077"); // tallest and largest length
        Package p3 = Package.of(70007, 70.0f, 10.7f, 0.7f, "testOLGH28289#70007");

        List<Package> tallToShort;

        tx.begin();
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);
        tx.commit();

        tx.begin();
        try {
            tallToShort = em
                            .createQuery("SELECT o FROM Package o WHERE (o.height<?1) ORDER BY o.height DESC, o.length",
                                         Package.class)
                            .setParameter(1, 8.0)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE) // Cause of issue
                            .setMaxResults(2)
                            .getResultList();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        System.out.println(tallToShort);

        assertEquals(2, tallToShort.size());

        /**
         * Recreate
         *
         * <pre>
         * SELECT * FROM (
         *   SELECT * FROM (
         *     SELECT EL_TEMP.*, ROWNUMBER() OVER() AS EL_ROWNM
         *     FROM (
         *       SELECT ID AS a1, DESCRIPTION AS a2, HEIGHT AS a3, LENGTH AS a4, WIDTH AS a5
         *       FROM PACKAGE
         *       WHERE (HEIGHT < ?)
         *       ORDER BY HEIGHT DESC, LENGTH
         *     ) AS EL_TEMP
         *   ) AS EL_TEMP2 WHERE EL_ROWNM <= ?
         * ) AS EL_TEMP3 WHERE EL_ROWNM > ?
         * FOR READ ONLY WITH RS USE AND KEEP UPDATE LOCKS // When this is added to query the ordering is ignored
         *
         * bind => [8.0, 2, 0]
         * </pre>
         */
        assertEquals(70071, tallToShort.get(0).id);
        assertEquals(70077, tallToShort.get(1).id);
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28078
    public void testOLGH28078() throws Exception {
        deleteAllEntities(Account.class);

        Account Checking = Account.of(123456, 123456, "Wells Fargo", true, 1000.00, "Jimmy Cricket");
        Account Savings = Account.of(654321, 123456, "Wells Fargo", false, 8569.15, "Jimmy Cricket");

        List<Account> results;

        tx.begin();
        em.persist(Checking);
        em.persist(Savings);
        tx.commit();

        tx.begin();
        try {
            results = em.createQuery("SELECT o FROM Account o WHERE (o.accountId=?1)", Account.class)
                            .setParameter(1, AccountId.of(123456, 123456))
                            .getResultList(); // Unable to recreate
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        assertEquals(1, results.size());
        assertEquals(1000.00, results.get(0).balance, 0.01);

    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/27696")
    public void testOLGH27696() throws Exception {
        deleteAllEntities(Account.class);

        Account a1 = Account.of(1005380, 70081, "Think Bank", true, 552.18, "Aaron testOLGH27696");
        Account a2 = Account.of(1004470, 70081, "Think Bank", true, 443.94, "Brian testOLGH27696");
        Account a3 = Account.of(1006380, 70081, "Think Bank", true, 160.63, "Cole testOLGH27696");
        Account a4 = Account.of(1007590, 70081, "Think Bank", true, 793.30, "Dean testOLGH27696");

        List<Account> accounts;

        tx.begin();
        em.persist(a1);
        em.persist(a2);
        em.persist(a3);
        em.persist(a4);
        tx.commit();

        tx.begin();
        try {
            accounts = em
                            .createQuery(
                                         "SELECT o FROM Account o WHERE (o.accountId IN ?1 OR o.owner=?2) ORDER BY o.owner DESC",
                                         Account.class)
                            .setParameter(1,
                                          Set.of(AccountId.of(1005380, 70081), AccountId.of(1004470, 70081),
                                                 AccountId.of(1006380, 70081)))
                            .setParameter(2, "Elizabeth testOLGH27696")
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * Internal Exception: java.sql.SQLSyntaxErrorException: Syntax error:
             * Encountered "," at line 1, column 99.
             * Error Code: 20000
             * Call: SELECT BALANCE, BANKNAME, CHECKING, OWNER, ACCOUNTNUM, ROUTINGNUM FROM
             * ACCOUNT WHERE (((ACCOUNTNUM, ROUTINGNUM) IN (AccountId:1006380:70081,
             * AccountId:1005380:70081, AccountId:1004470:70081)) OR (OWNER = 'Elizabeth
             * testOLGH27696')) ORDER BY OWNER DESC
             * Query: ReadAllQuery(referenceClass=Account
             * sql="SELECT BALANCE, BANKNAME, CHECKING, OWNER, ACCOUNTNUM, ROUTINGNUM FROM ACCOUNT WHERE (((ACCOUNTNUM, ROUTINGNUM) IN ?) OR (OWNER = ?)) ORDER BY OWNER DESC"
             * )
             */
            throw e;
        }

        assertEquals(4, accounts.size());
        assertEquals(a4.owner, accounts.get(0).owner);
        assertEquals(a3.owner, accounts.get(1).owner);
        assertEquals(a2.owner, accounts.get(2).owner);
        assertEquals(a1.owner, accounts.get(3).owner);
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28905")
    public void testOLGH28905() throws Exception {
        Triangle t1_0 = Triangle.of((byte) 13, (byte) 84, (byte) 85);

        Triangle t1_1;

        tx.begin();
        em.persist(t1_0);
        tx.commit();

        assertNotNull(t1_0.distinctKey); // t1_0 is detached

        tx.begin();
        try {
            em.createQuery("UPDATE Triangle SET this.sides=?2, this.perimeter=?3 WHERE this.distinctKey=?1")
                            .setParameter(1, t1_0.distinctKey)
                            .setParameter(2, new byte[] { 36, 77, 85 })
                            .setParameter(3, (short) (198))
                            .executeUpdate();

            t1_1 = em.createQuery("SELECT o FROM Triangle o WHERE o.distinctKey=?1", Triangle.class)
                            .setParameter(0, t1_0.distinctKey)
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            /*
             * Recreate
             * java.lang.IllegalArgumentException: An exception occurred while creating a
             * query in EntityManager:
             * Exception Description: Problem compiling [UPDATE Triangle SET this.sides=?2,
             * this.perimeter=?3 WHERE this.distinctKey=?1].
             * [20, 30] The state field cannot be resolved.
             * [37, 51] The state field cannot be resolved.
             */
            throw e;
        }

        assertEquals(198, t1_1.perimeter);
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28898
    public void testOLGH28898() throws Exception {
        Reciept r1 = Reciept.of(00012, "Billy", 12.5f);
        Reciept r2 = Reciept.of(00013, "Bobby", 9.75f);

        int count;

        tx.begin();
        em.persist(r1);
        em.persist(r2);
        tx.commit();

        tx.begin();
        try {
            count = em.createQuery("DELETE FROM Reciept WHERE this.total < :max")
                            .setParameter("max", 10.00f)
                            .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * org.eclipse.persistence.exceptions.QueryException
             * Exception Description: Object comparisons can only use the equal() or
             * notEqual() operators. Other comparisons must be done through query keys or
             * direct attribute
             * level comparisons.
             * Expression: [
             * Relation operator [ < ]
             * Base io.openliberty.jpa.data.tests.models.Reciept
             * Parameter max]
             * Query: DeleteAllQuery(referenceClass=Reciept
             * jpql="DELETE FROM Reciept WHERE this.total < :max")
             */
            throw e;
        }

        assertEquals(1, count);
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29781
    public void testOLGH29781() throws Exception {
        ZoneId ET = ZoneId.of("America/New_York");
        Instant when = ZonedDateTime.of(2022, 4, 29, 12, 0, 0, 0, ET)
                        .toInstant();
        Store s1 = Store.of(2022, 4, 29, "Billy", 12L);
        Store s2 = Store.of(2024, 5, 12, "Bobby", 9L);

        int count;

        tx.begin();
        em.persist(s1);
        em.persist(s2);
        tx.commit();

        tx.begin();
        try {
            count = em.createQuery("DELETE FROM Store WHERE this.time>:when")
                            .setParameter("when", when)
                            .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        assertEquals(1, count);
    }

    @Test // Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28895
    public void testOLGH28895() throws Exception {
        Product p1 = Product.of("testOLGH28895-1", "Ball", 12.50f);
        Product p2 = Product.of("testOLGH28895-2", "Skate", 15.50f);

        tx.begin();
        em.persist(p1);
        em.persist(p2);
        tx.commit();

        int count;

        tx.begin();
        try {
            count = em.createQuery("DELETE FROM Product WHERE this.name LIKE ?1")
                            .setParameter(1, "B%")
                            .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * org.eclipse.persistence.exceptions.DatabaseException
             * Internal Exception: java.sql.SQLSyntaxErrorException: Syntax error:
             * Encountered "LIKE" at line 1, column 28.
             * Error Code: 20000
             * Call: DELETE FROM PRODUCT WHERE LIKE ?
             * bind => [B%]
             * Query: DeleteAllQuery(referenceClass=Product
             * sql="DELETE FROM PRODUCT WHERE LIKE ?")
             */
            throw e;
        }

        assertEquals(1, count);
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29440
    @SkipIfSysProp({ DB_Postgres, DB_Oracle })
    public void testOLGH29440() throws Exception {
        deleteAllEntities(DemographicInfo.class);

        DemographicInfo US2024 = DemographicInfo.of(2024, 4, 30, 133809000, 7136033799632.56, 27480960216618.32);
        DemographicInfo US2023 = DemographicInfo.of(2023, 4, 28, 134060000, 6852746625848.93, 24605068022566.94);

        tx.begin();
        em.persist(US2024);
        em.persist(US2023);
        tx.commit();

        BigDecimal result;

        tx.begin();
        try {

            result = em.createQuery(
                                    "SELECT this.publicDebt / this.numFullTimeWorkers FROM DemographicInfo WHERE EXTRACT (YEAR FROM this.collectedOn) = ?1",
                                    BigDecimal.class)
                            .setParameter(1, 2024)
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * Exception [EclipseLink-4002] (Eclipse Persistence Services -
             * 5.0.0.v202408071314-43356e84b79e71022b1656a5462b0a72d70787a4):
             * org.eclipse.persistence.exceptions.DatabaseException
             * Internal Exception: org.postgresql.util.PSQLException: ERROR: current
             * transaction is aborted, commands ignored until end of transaction block
             * Error Code: 0
             * Call: SELECT (PUBLICDEBT / NUMFULLTIMEWORKERS) FROM DEMOGRAPHICINFO WHERE
             * (EXTRACT(YEAR FROM COLLECTEDON) = ?)
             * bind => [2024]
             * Query: ReportQuery(referenceClass=DemographicInfo
             * sql="SELECT (PUBLICDEBT / NUMFULLTIMEWORKERS) FROM DEMOGRAPHICINFO WHERE (EXTRACT(YEAR FROM COLLECTEDON) = ?)"
             * )
             */
            throw e;
        }

        MathContext ctx = new MathContext(8, RoundingMode.UP); // Expect actual and expected result to be 205374.53
        assertEquals(US2024.publicDebt.divide(new BigDecimal(US2024.numFullTimeWorkers), ctx), result.round(ctx));
    }

    @Test
    @SkipIfSysProp(DB_DB2) //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29443
    public void testOLGH29443() throws Exception {
        deleteAllEntities(DemographicInfo.class);

        ZoneId ET = ZoneId.of("America/New_York");
        Instant when = ZonedDateTime.of(2022, 4, 29, 12, 0, 0, 0, ET)
                        .toInstant();

        DemographicInfo US2022 = DemographicInfo.of(2022, 4, 29, 132250000, 6526909395140.41, 23847245116757.60);
        DemographicInfo US2007 = DemographicInfo.of(2007, 4, 30, 121090000, 3833110332444.19, 5007058051986.64);

        List<BigInteger> results;

        tx.begin();
        em.persist(US2022);
        em.persist(US2007);
        tx.commit();

        List<Error> errors = new ArrayList<>();

        Thread.sleep(Duration.ofSeconds(1).toMillis());

        for (int i = 0; i < 10; i++) {
            System.out.println("Executing SELECT query, iteration: " + i);

            tx.begin();
            results = em
                            .createQuery("SELECT this.numFullTimeWorkers FROM DemographicInfo WHERE this.collectedOn=:when",
                                         BigInteger.class)
                            .setParameter("when", when)
                            .getResultList();
            tx.commit();

            try {
                assertNotNull("Query should not have returned null after iteration " + i, results);
                // Recreate - an empty list is returned
                assertFalse("Query should not have returned an empty list after iteration " + i, results.isEmpty());
                assertEquals("Query should not have returned more than one result after iteration " + i, 1,
                             results.size());
                assertEquals(US2022.numFullTimeWorkers, results.get(0));
            } catch (AssertionError e) {
                errors.add(e);
            }
        }

        if (!errors.isEmpty()) {
            throw new AssertionError("Executing the same query returned incorrect results " + errors.size() + " out of 10 executions", errors.get(0));
        }
    }

    @Test //Original issue: https://github.com/OpenLiberty/open-liberty/issues/29443
    @Ignore("Additional Issue: ZonedDateTime stored as blob, cannot do comparison of blobs on most databases")
    public void testOLGH29443ZonedDateTime() throws Exception {
        deleteAllEntities(DemographicInformation.class);

        ZoneId ET = ZoneId.of("America/New_York");
        ZonedDateTime when = ZonedDateTime.of(2022, 4, 29, 12, 0, 0, 0, ET);

        DemographicInformation US2022 = DemographicInformation.of(2022, 4, 29, 132250000, 6526909395140.41, 23847245116757.60);
        DemographicInformation US2007 = DemographicInformation.of(2007, 4, 30, 121090000, 3833110332444.19, 5007058051986.64);

        List<BigInteger> results;

        tx.begin();
        em.persist(US2022);
        em.persist(US2007);
        tx.commit();

        List<Error> errors = new ArrayList<>();

        Thread.sleep(Duration.ofSeconds(1).toMillis());

        for (int i = 0; i < 10; i++) {
            System.out.println("Executing SELECT query, iteration: " + i);

            tx.begin();
            results = em
                            .createQuery("SELECT this.numFullTimeWorkers FROM DemographicInformation WHERE this.collectedOn=:when",
                                         BigInteger.class)
                            .setParameter("when", when)
                            .getResultList();
            tx.commit();

            try {
                assertNotNull("Query should not have returned null after iteration " + i, results);
                // Recreate - an empty list is returned
                assertFalse("Query should not have returned an empty list after iteration " + i, results.isEmpty());
                assertEquals("Query should not have returned more than one result after iteration " + i, 1,
                             results.size());
                assertEquals(US2022.numFullTimeWorkers, results.get(0));
            } catch (AssertionError e) {
                errors.add(e);
            }
        }

        if (!errors.isEmpty()) {
            throw new AssertionError("Executing the same query returned incorrect results " + errors.size() + " out of 10 executions", errors.get(0));
        }
    }

    @Test
    //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29893
    public void testOLGH29893() throws Exception {
        String vehicleId = "V1234";
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setModel("Toyota Corolla");
        vehicle.setColor("Blue");

        tx.begin();
        em.persist(vehicle);
        tx.commit();

        Vehicle result;

        tx.begin();
        try {
            result = em.createQuery("FROM Vehicle WHERE LOWER(ID(THIS)) = ?1", Vehicle.class)
                            .setParameter(1, vehicleId.toLowerCase())
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        assertNotNull(result);
        assertEquals(vehicleId, result.getId());
        assertEquals("Toyota Corolla", result.getModel());
        assertEquals("Blue", result.getColor());
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29475")
    public void testOLGH29475() throws Exception {
        Rating.Reviewer jimmy = Rating.Reviewer.of("Jimothy", "Scramble", "J.Scramble@example.com");
        Rating.Item blueBerry = Rating.Item.of("BlueBerry 10", 299.99f);
        Rating rating = Rating.of(1001, blueBerry, 4, jimmy,
                                  "The buttons are nice for quick typing",
                                  "The power button could have been in a better place",
                                  "Poor screen lighting");

        Rating result;

        tx.begin();
        em.persist(rating);
        tx.commit();

        tx.begin();
        List<String> comments = em.createQuery("SELECT o.comments FROM Rating o WHERE o.id = :id", String.class)
                        .setParameter("id", 1001)
                        .getResultList();
        tx.commit();

        assertEquals(3, comments.size());

        tx.begin();
        try {
            result = em.createQuery("SELECT NEW io.openliberty.jpa.data.tests.models.Rating( "
                                    + " o.id, o.item, o.numStars, o.reviewer, o.comments ) "
                                    + "FROM Rating o WHERE o.id = :id", Rating.class)
                            .setParameter("id", 1001)
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /**
             * Recreate
             * Exception [EclipseLink-0] (Eclipse Persistence Services -
             * 5.0.0.v202408071314-43356e84b79e71022b1656a5462b0a72d70787a4):
             * org.eclipse.persistence.exceptions.JPQLException
             * Exception Description: Problem compiling
             * [SELECT NEW io.openliberty.jpa.data.tests.models.Rating( o.id, o.item,
             * o.numStars, o.reviewer, o.comments ) FROM Rating o WHERE o.id = :id].
             * [93, 103] The state field path 'o.comments' cannot be resolved to a
             * collection type.
             * (SELECT NEW io.openliberty.jpa.data.tests.models.Rating(o.id, o.item,
             * o.numStars, o.reviewer, [ o.comments ] ...
             */
            throw e;
        }

        assertNotNull(result.comments);
        assertEquals(3, result.comments.size());

    }

    @Test
    //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29475 .This test includes issues in ElementCollection
    public void test_29475_ElementCollection() throws Exception {
        ECEntity e1 = new ECEntity();
        e1.setId("EC1");
        e1.setIntArray(new int[] { 14, 12, 1 });
        e1.setLongList(new ArrayList<>(List.of(14L, 12L, 1L)));
        e1.setLongListEC(new ArrayList<>(List.of(14L, 12L, 1L)));
        e1.setStringSet(Set.of("fourteen", "twelve", "one"));
        e1.setStringSetEC(Set.of("fourteen", "twelve", "one"));
       
        ECEntity e2 = new ECEntity();
        e2.setId("EC2");
        e2.setIntArray(new int[] { 14, 12, 2 });
        e2.setLongList(new ArrayList<>(List.of(14L, 12L, 2L)));
        e2.setLongListEC(new ArrayList<>(List.of(14L, 12L, 2L)));
        e2.setStringSet(Set.of("fourteen", "twelve", "two"));
        e2.setStringSetEC(Set.of("fourteen", "twelve", "two"));


        tx.begin();
        em.persist(e1);
        em.persist(e2);
        tx.commit();
         // Test JPQL queries
    String jpql;
    List<?> results;
    // Query for intArray
    tx.begin();
    try {
        jpql = "SELECT intArray FROM ECEntity WHERE id=?1";
        results = em.createQuery(jpql)
                    .setParameter(1, "EC1")
                    .getResultList();
                    logQueryResults(jpql,results);
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }

    // Query for longList
    tx.begin();
    try {
        jpql = "SELECT longList FROM ECEntity WHERE id=?1";
        results = em.createQuery(jpql)
                    .setParameter(1, "EC1")
                    .getResultList();
                    logQueryResults(jpql,results);
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }
    // Query for stringSet
    tx.begin();
    try {
        jpql = "SELECT stringSet FROM ECEntity WHERE id=?1";
        results = em.createQuery(jpql)
                    .setParameter(1, "EC1")
                    .getResultList();
        logQueryResults(jpql,results);
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }
    tx.begin();
    try {
        jpql = "SELECT longListEC FROM ECEntity WHERE id=?1";
        results = em.createQuery(jpql)
                    .setParameter(1, "EC1")
                    .getResultList();
                    logQueryResults(jpql,results);
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }
    // Query for longListEC
    tx.begin();
    try {
        jpql = "SELECT longListEC FROM ECEntity WHERE id LIKE ?1";
        results = em.createQuery(jpql)
                    .setParameter(1, "EC%")
                    .getResultList();
                    logQueryResults(jpql,results);
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }
    tx.begin();
    try {
        jpql = "SELECT longList FROM ECEntity WHERE id LIKE ?1";
        results = em.createQuery(jpql)
                    .setParameter(1, "EC%")
                    .getResultList();
                    logQueryResults(jpql,results);
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }
    // Query for stringSetEC
    tx.begin();
    try {
        jpql = "SELECT stringSetEC FROM ECEntity WHERE id LIKE ?1";
        results = em.createQuery(jpql)
                    .setParameter(1, "EC%")
                    .getResultList();
                    logQueryResults(jpql,results);
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }
    tx.begin();
    try {
        jpql = "SELECT stringSet FROM ECEntity WHERE id LIKE ?1";
        results = em.createQuery(jpql)
                    .setParameter(1, "EC%")
                    .getResultList();
                    logQueryResults(jpql,results);
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }

    tx.begin();
    try {
        jpql = "SELECT stringSetEC FROM ECEntity WHERE id=?1";
        results = em.createQuery(jpql)
                    .setParameter(1, "EC1")
                    .getResultList();
                    logQueryResults(jpql,results);
        tx.commit();
    } catch (Exception e) {
        tx.rollback();
        throw e;
    }

    }
    public void logQueryResults(String jpql, Collection<?> results) {
        System.out.println(jpql);
        System.out.println("getResultList returned a " + results.getClass().getTypeName());
        if (!results.isEmpty()) {
            System.out.println("    elements are of type " + results.iterator().next().getClass().getTypeName());
        } else {
            System.out.println("    elements are of type <empty>");
        }
        StringBuilder s = new StringBuilder();
            boolean first = true;
            for (Object element : results) {
                if (first)
                    first = false;
                else
                    s.append(", ");
                if (element instanceof int[])
                    s.append(Arrays.toString((int[]) element));
                else
                    s.append(element);
            }
            System.out.println("            contents are [" + s.toString() + "]");
    }

    @Test //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29460
    public void testOLGH29460() throws Exception {
        // Setup test data using the factory method
        Participant p1 = Participant.of("John", "Doe", 1);
        Participant p2 = Participant.of("Jane", "Smith", 2);
        Participant p3 = Participant.of("Emily", "Doe", 3);

        // Persisting the participants
        tx.begin();
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);
        tx.commit();

        // Test the JPQL query
        List<Participant> results;
        tx.begin();
        try {
            results = em.createQuery("SELECT o FROM Participant o WHERE (o.name.last = ?1) ORDER BY o.name.first, o.id", Participant.class)
                            .setParameter(1, "Doe")
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        // Verify the results
        assertEquals(2, results.size());
        assertEquals("Doe", results.get(0).getName().getLast());
        assertEquals("Emily", results.get(0).getName().getFirst());
        assertEquals("Doe", results.get(1).getName().getLast());
        assertEquals("John", results.get(1).getName().getFirst());

    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/30534")
    public void testOLGH30534() throws Exception {

        County county1 = new County("CountyA");
        County county2 = new County("CountyB");
        County county3 = new County("CountyC");

        tx.begin();
        em.persist(county1);
        em.persist(county2);
        em.persist(county3);
        tx.commit();

        List<County> results;
        tx.begin();
        try {
            results = em.createQuery("SELECT o FROM County o WHERE o.name = ?1 ORDER BY o.name", County.class)
                            .setParameter(1, "CountyA")
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        assertEquals(1, results.size());
        assertEquals("CountyA", results.get(0).getName());
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/30351")
    public void testOLGH30351() throws Exception {

        Business business1 = Business.of(43.1566f, -77.6109f, "Rochester", "NY", 14623, 123, "Main St", "N", "Acme Corp");
        Business business2 = Business.of(43.1578f, -77.6110f, "Rochester", "NY", 14623, 456, "Broadway", "S", "Beta LLC");
        Business business3 = Business.of(42.8864f, -78.8784f, "Buffalo", "NY", 14202, 789, "Elm St", "E", "Gamma Inc");

        tx.begin();
        em.persist(business1);
        em.persist(business2);
        em.persist(business3);
        tx.commit();

        List<Business> results;
        tx.begin();
        try {

            results = em.createQuery("FROM Business WHERE location.address.city=?1 ORDER BY name", Business.class)
                            .setParameter(1, "Rochester")
                            .getResultList();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Acme Corp", results.get(0).name);
        assertEquals("Beta LLC", results.get(1).name);
    }

    @Test
    @SkipIfSysProp(DB_Postgres) //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/30400
    public void testOLGH30400() throws Exception {
        deleteAllEntities(PurchaseOrder.class, "Orders");

        /*
         * Expect the following columns in the database after escaping \ from in java string
         *
         * | id | ____purchaseBy____ | total | v |
         * | -- | ------------------ | ----- | - |
         * | XX | Escape\Characters _| 23.93 | 1 |
         * | YY | Escape\\Characters | 27.97 | 1 |
         */
        PurchaseOrder order1 = PurchaseOrder.of("Escape\\Characters", 23.93f);
        PurchaseOrder order2 = PurchaseOrder.of("Escape\\\\Characters", 27.97f);

        tx.begin();
        em.persist(order1);
        em.persist(order2);
        tx.commit();

        tx.begin();
        List<Float> totals = em.createQuery("SELECT total FROM Orders WHERE purchasedBy LIKE ?1 ORDER BY total", Float.class)
                        .setParameter(1, "Escape\\\\Characters") //attempt to find `Escape\\Characters` in the database
                        .getResultList();
        tx.commit();

        assertEquals(1, totals.size());

        // Failure here, because PostgreSQL automatically escaped `Escape\\Characters` and instead found `Escape\Characters` in the database.
        // This cannot be avoided even when setting standard_conforming_strings=on
        // EclipseLink should handle this case by adding in additional escapes to the bound parameters where PostgreSQL uses the default escape character `\`
        assertEquals(27.97f, totals.get(0), 0.01);

    }

    /**
     * Utility method to drop all entities from table.
     *
     * Order to tests is not guaranteed and thus we should be pessimistic and
     * delete all entities when we reuse an entity between tests.
     *
     * @param clazz - the entity class
     * @param aka   - "also known as" if the table has a different name than the
     *                  entity
     */
    private void deleteAllEntities(Class<?> clazz, String aka) throws Exception {
        tx.begin();
        em.createQuery("DELETE FROM " + aka)
                        .executeUpdate();
        tx.commit();
    }

    /**
     * Utility method to drop all entities from table.
     *
     * Order to tests is not guaranteed and thus we should be pessimistic and
     * delete all entities when we reuse an entity between tests.
     *
     * @param clazz - the entity class
     */
    private void deleteAllEntities(Class<?> clazz) throws Exception {
        tx.begin();
        em.createQuery("DELETE FROM " + clazz.getSimpleName())
                        .executeUpdate();
        tx.commit();
    }

}
