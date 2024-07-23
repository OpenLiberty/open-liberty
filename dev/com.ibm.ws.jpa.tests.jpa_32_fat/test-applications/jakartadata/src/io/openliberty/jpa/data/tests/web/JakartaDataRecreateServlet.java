/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.web;

import static componenttest.annotation.SkipIfSysProp.DB_Oracle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;
import io.openliberty.jpa.data.tests.models.AsciiCharacter;
import io.openliberty.jpa.data.tests.models.Box;
import io.openliberty.jpa.data.tests.models.Business;
import io.openliberty.jpa.data.tests.models.City;
import io.openliberty.jpa.data.tests.models.CityId;
import io.openliberty.jpa.data.tests.models.Coordinate;
import io.openliberty.jpa.data.tests.models.NaturalNumber;
import io.openliberty.jpa.data.tests.models.Package;
import io.openliberty.jpa.data.tests.models.Person;
import io.openliberty.jpa.data.tests.models.Prime;
import io.openliberty.jpa.data.tests.models.Rebate;
import io.openliberty.jpa.data.tests.models.Rebate.Status;
import io.openliberty.jpa.data.tests.models.Segment;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
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
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28912")
    public void testOLGH28912() throws Exception {
        Coordinate original = Coordinate.of("testOLGH28912", 10, 15f);
        UUID id = original.id;
        Coordinate result;

        tx.begin();

        try {
            em.persist(original);

            em.createQuery("UPDATE Coordinate SET x = :newX, y = y / :yDivisor WHERE id = :id") //FAILURE PARSING QUERY HERE
                            .setParameter("newX", 11)
                            .setParameter("yDivisor", 5)
                            .setParameter("id", id)
                            .executeUpdate();

            result = em.createQuery("SELECT Coordinate WHERE id = :id", Coordinate.class)
                            .setParameter("id", id)
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreated
             * Exception Description: Syntax error parsing [UPDATE Coordinate SET x = :newX, y = y / :yDivisor WHERE id = :id].
             * [37, 38] The left expression is not an arithmetic expression.
             */
            throw e;
        }

        assertEquals(id, result.id);
        assertEquals(11, result.x, 0.001);
        assertEquals(5f, result.y, 0.001);
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28913")
    public void testOLGH28913() throws Exception {
        AsciiCharacter character = AsciiCharacter.of(80); //P
        String result;

        tx.begin();

        try {
            em.persist(character);

            result = em.createQuery("SELECT hexadecimal FROM AsciiCharacter WHERE hexadecimal IS NOT NULL AND thisCharacter = ?1", String.class) //FAILURE PARSING QUERY HERE
                            .setParameter(1, character.getThisCharacter())
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreated
             * Exception Description: Problem compiling [SELECT hexadecimal FROM AsciiCharacter WHERE hexadecimal IS NOT NULL AND thisCharacter = ?1].
             * [7, 18] The identification variable 'hexadecimal' is not defined in the FROM clause.
             */
            throw e;
        }

        assertEquals(character.getHexadecimal(), result);
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28908")
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

            result = em.createQuery("SELECT Person WHERE ssn_id = :ssn", Person.class)
                            .setParameter("ssn", p.ssn_id)
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreated
             * Exception Description: Internal problem encountered while compiling [UPDATE Person SET firstName=:newFirstName WHERE id(this)=:ssn].
             * Internal Exception: java.lang.NullPointerException: Cannot invoke "org.eclipse.persistence.internal.jpa.jpql.Declaration.getDescriptor()"
             * because the return value of "org.eclipse.persistence.internal.jpa.jpql.JPQLQueryContext.getDeclaration(java.lang.String)" is null
             */
            throw e;
        }

        assertEquals(p.ssn_id, result.ssn_id);
        assertEquals("Jack", result.firstName);
        assertEquals(p.lastName, result.lastName);
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28874")
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
            result1 = em.createQuery("FROM NaturalNumber WHERE isOdd = false AND numType = io.openliberty.jpa.data.tests.models.NaturalNumber.NumberType.PRIME",
                                     NaturalNumber.class)
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            exceptions.add(e);
        }

        tx.begin();
        try {
            result2 = em.createQuery("FROM NaturalNumber WHERE this.isOdd = false AND this.numType = io.openliberty.jpa.data.tests.models.NaturalNumber.NumberType.PRIME",
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
             * Exception Description: Object comparisons can only be used with OneToOneMappings. Other mapping comparisons must be done through query keys or direct attribute level
             * comparisons.
             * Mapping: [org.eclipse.persistence.mappings.DirectToFieldMapping[numType-->NATURALNUMBER.NUMTYPE]]
             * Expression: [
             * Query Key numType
             * Base io.openliberty.jpa.data.tests.models.NaturalNumber]
             * Query: ReadAllQuery(referenceClass=NaturalNumber
             * jpql="FROM NaturalNumber WHERE isOdd = false AND numType = io.openliberty.jpa.data.tests.models.NaturalNumber.NumberType.PRIME")
             */
            throw exceptions.get(0);
        }

        assertEquals(2l, result1.getId(), 0.001f);
        assertEquals(2l, result2.getId(), 0.001f);
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28920")
    public void testOLGH28920() throws Exception {
        Rebate r1 = Rebate.of(10.00, "testOLGH28920", LocalTime.now().minusHours(1), LocalDate.now(), Status.SUBMITTED, LocalDateTime.now(), 1);
        Rebate r2 = Rebate.of(12.00, "testOLGH28920", LocalTime.now().minusHours(1), LocalDate.now(), Status.PAID, LocalDateTime.now(), 2);
        Rebate r3 = Rebate.of(14.00, "testOLGH28920", LocalTime.now().minusHours(1), LocalDate.now(), Status.PAID, LocalDateTime.now(), 2);

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
                                         + "ORDER BY amount DESC, id ASC", Rebate.class)
                            .setParameter(1, "testOLGH28920")
                            .getResultList();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * Exception Description: Syntax error parsing [SELECT NEW io.openliberty.jpa.data.tests.models.Rebate(id, amount, customerId, purchaseMadeAt, purchaseMadeOn, status,
             * updatedAt, version) FROM Rebate WHERE customerId=?1 AND status=io.openliberty.jpa.data.tests.models.Rebate.Status.PAID ORDER BY amount DESC, id ASC].
             * [55, 57] The identification variable 'id' cannot be a reserved word.
             * [130, 137] The identification variable 'version' cannot be a reserved word.
             */
            throw e;
        }

        assertEquals(2, paidRebates.size());
        assertEquals(14.00, paidRebates.get(0).amount, 0.001);
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28909")
    public void testOLGH28909() throws Exception {
        Box cube = Box.of("testOLGH28909", 1, 1, 1);

        Box wall; //box with no width

        tx.begin();
        em.persist(cube);
        tx.commit();

        tx.begin();
        try {
            em.createQuery("UPDATE Box SET length = length + ?1, width = width - ?1, height = height * ?2")
                            .setParameter(1, 1)
                            .setParameter(2, 2)
                            .executeUpdate();

            wall = em.createQuery("SELECT Box WHERE boxIdentifier = :id", Box.class)
                            .setParameter("id", "testOLGH28909")
                            .getSingleResult();

            tx.commit();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * Exception Description: Syntax error parsing [UPDATE Box SET length = length + ?1, width = width - ?1, height = height * ?2].
             * [30, 30] The left parenthesis is missing from the LENGTH expression.
             * [45, 50] The left expression is not an arithmetic expression.
             * [66, 72] The left expression is not an arithmetic expression.
             */
            throw e;
        }

        assertEquals("testOLGH28909", wall.boxIdentifier);
        assertEquals(2, wall.length); // 1+1
        assertEquals(0, wall.length); // 1-1
        assertEquals(2, wall.height); // 1*2
    }

    @Test
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28931")
    public void testOLGH28931() throws Exception {
        Business ibmRoc = Business.of(44.05887f, -92.50355f, "Rochester", "Minnesota", 55901, 2800, "37th St", "NW", "IBM Rochester");
        Business ibmRTP = Business.of(35.90481f, -78.85026f, "Durham", "North Carolina", 27703, 4204, "Miami Blvd", "S", "IBM RTP");

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
             * Exception Description: Internal problem encountered while compiling [FROM Business WHERE location.address.city=?1 ORDER BY name].
             * Internal Exception: java.lang.IndexOutOfBoundsException: Index 1 out of bounds for length 1
             */
            throw e;
        }

        assertEquals("IBM Rochester", result.name);
        assertEquals(55901, result.location.address.zip);
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
            primes = em.createQuery("SELECT ID(THIS) FROM Prime o WHERE (o.name = :numberName OR :numeral=o.romanNumeral OR o.hex =:hex OR ID(THIS)=:num) ORDER BY o.numberId",
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
             * WHERE (o.name = :numberName OR :numeral=o.romanNumeral OR o.hex =:hex OR ID(THIS)=:num)
             * ORDER BY o.numberId].
             * [10, 14] The identification variable 'THIS' is not defined in the FROM clause.
             * [108, 112] The identification variable 'THIS' is not defined in the FROM clause.
             */
            throw e;
        }

        assertEquals(4, primes.size());
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
    @SkipIfSysProp(DB_Oracle) // Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28545
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
    @SkipIfSysProp(DB_Oracle) // Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28545
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
    @SkipIfSysProp(DB_Oracle) // Reference issue: https://github.com/OpenLiberty/open-liberty/issues/28545
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
             * Internal Exception: java.sql.SQLSyntaxErrorException: ORA-00918: LENGTH(ROMANNUMERAL): column ambiguously specified - appears in and
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
        City RochesterMN = City.of("Rochester", "Minnesota", 121878, Set.of(55901, 55902, 55903, 55904, 55906));
        City RochesterNY = City.of("Rochester", "New York", 209352, Set.of(14601, 14602, 14603, 14604, 14606));

        List<CityId> rochesters;

        tx.begin();
        em.persist(RochesterMN);
        em.persist(RochesterNY);
        tx.commit();

        tx.begin();
        try {
            rochesters = em.createQuery("SELECT ID(THIS) FROM City WHERE (name=?1) ORDER BY population DESC", CityId.class)
                            .setParameter(1, "Rochester")
                            .getResultList();
        } catch (Exception e) {
            tx.rollback();

            /*
             * Recreate
             * io.openliberty.jpa.data.tests.web.JakartaDataRecreateServlet
             * java.lang.ClassCastException: java.lang.String incompatible with io.openliberty.jpa.data.tests.models.CityId
             */
            throw e;
        }

        assertEquals(2, rochesters.size());
        assertEquals("New York", rochesters.get(0).getStateName());
        assertEquals("Minnesota", rochesters.get(1).getStateName());

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
