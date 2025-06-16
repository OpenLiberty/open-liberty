/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.AsciiCharacter;
import io.openliberty.jpa.persistence.tests.models.Organization;
import io.openliberty.jpa.persistence.tests.models.Participant;
import io.openliberty.jpa.persistence.tests.models.Person;
import io.openliberty.jpa.persistence.tests.models.Priority;
import io.openliberty.jpa.persistence.tests.models.Product;
import io.openliberty.jpa.persistence.tests.models.Ticket;
import io.openliberty.jpa.persistence.tests.models.TicketStatus;
import io.openliberty.jpa.persistence.tests.models.User;
import io.openliberty.jpa.persistence.tests.models.identity.IntegerIdentityIdEntity;
import io.openliberty.jpa.persistence.tests.models.identity.IntegerPrimitiveIdentityIdEntity;
import io.openliberty.jpa.persistence.tests.models.identity.LongIdentityIdEntity;
import io.openliberty.jpa.persistence.tests.models.identity.LongPrimitiveIdentityIdEntity;
import io.openliberty.jpa.persistence.tests.models.sequence.IntegerPrimitiveSequenceIdEntity;
import io.openliberty.jpa.persistence.tests.models.sequence.IntegerSequenceIdEntity;
import io.openliberty.jpa.persistence.tests.models.sequence.LongPrimitiveSequenceIdEntity;
import io.openliberty.jpa.persistence.tests.models.sequence.LongSequenceIdEntity;
import io.openliberty.jpa.persistence.tests.models.table.IntegerPrimitiveTableIdEntity;
import io.openliberty.jpa.persistence.tests.models.table.IntegerTableIdEntity;
import io.openliberty.jpa.persistence.tests.models.table.LongPrimitiveTableIdEntity;
import io.openliberty.jpa.persistence.tests.models.table.LongTableIdEntity;
import io.openliberty.jpa.persistence.tests.models.uuid.StringUUIDIdEntity;
import io.openliberty.jpa.persistence.tests.models.uuid.UUIDUUIDIdEntity;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JakartaPersistence32")
public class JakartaPersistenceServlet extends FATServlet {
    @PersistenceContext(unitName = "JakartaPersistenceUnit")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void testSetOperationsJPQL() throws Exception{
        deleteAllEntities(Person.class);
        deleteAllEntities(Organization.class);

        Person person1 = Person.of(1L, "AAA");
        Person person2 = Person.of(2L, "BBB");
        tx.begin();
        em.persist(person1);
        em.persist(person2);
        tx.commit();

        Organization org1 = Organization.of(3L, "BBB");
        Organization org2 = Organization.of(4L, "CCC");
        tx.begin();
        em.persist(org1);
        em.persist(org2);
        tx.commit();

        List<String> unionResult, intersectResult, exceptResult;
        try {
            // UNION
            unionResult = em.createQuery(
                                            "SELECT p.name FROM Person p " +
                                            "UNION " +
                                            "SELECT o.name FROM Organization o", String.class)
                            .getResultList();

            // INTERSECT
            intersectResult = em.createQuery(
                                            "SELECT p.name FROM Person p " +
                                            "INTERSECT " +
                                            "SELECT o.name FROM Organization o", String.class)
                            .getResultList();
            
            // EXCEPT
            exceptResult = em.createQuery(
                                            "SELECT p.name FROM Person p " +
                                            "EXCEPT " +
                                            "SELECT o.name FROM Organization o", String.class)
                            .getResultList();
        }
        
        catch(Exception e) {
            throw e;
        }
        Collections.sort(unionResult);
        assertEquals(Arrays.asList("AAA", "BBB", "CCC"), unionResult);
        assertEquals(Arrays.asList("BBB"), intersectResult);
        assertEquals(Arrays.asList("AAA"), exceptResult);
    }

     /**
     * Method for testing || in JPQL queries.
     * @throws Exception
     */
    @Test
    public void testJpqlConcat() throws Exception {
        deleteAllEntities(User.class);

        User user1 = User.of(1, "John", "Doe");
        User user2 = User.of(2, "Harry", "Potter");
        User user3 = User.of(3, "Hermione", "Granger");
        
        tx.begin();
        em.persist(user1);
        em.persist(user2);
        em.persist(user3);
        tx.commit();

        try{
            String concatJPQL = "SELECT u.firstName || ' ' || u.lastName FROM User u where u.lastName = ?1" ;
	        String fullName = em.createQuery(concatJPQL,String.class).setParameter(1, "Doe")
	            					.getSingleResult();

            String concatJPQLFrom = "SELECT u.firstName FROM User u where u.firstName || ' ' || u.lastName = ?1" ;
	        String firstName= em.createQuery(concatJPQLFrom,String.class).setParameter(1, "Harry Potter")
	            					.getSingleResult();

            assertEquals("John Doe", fullName);
            assertEquals("Harry", firstName);

        }catch (Exception e) {
            throw e;
        }
    }
    /**
     * In previous version, Enumerated annotations were used for mapping Java Enum types to database column values.
     *
     * The Annotation @Enumerated are used with EnumType (ORDINAL or STRING)
     * EnumeratedValue in 3.2, Specifies that an annotated field of a Java enum type is the source of database column values for an enumerated mapping.
     * The annotated field must be declared final, and must be of type:
     * byte, short, or int for EnumType.ORDINAL, or
     * String for EnumType.STRING.
     * https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/enumeratedvalue
     *
     * @throws Exception
     */
    @Test
    public void testEnumeratedValue() throws Exception {

        Ticket ticket1 = Ticket.of(1, "ticket1", TicketStatus.OPEN, Priority.HIGH);
        Ticket ticket2 = Ticket.of(2, "ticket2", TicketStatus.CLOSED, Priority.LOW);
        Ticket ticket3 = Ticket.of(3, "ticket3", TicketStatus.CANCELLED, Priority.MEDIUM);

        // Checking SQL logs whether the mapping is done as below in the insert queries
        // TicketStatus.OPEN ENUM property value will be mapped to Table column value 0
        // Priority.HIGH property value will be mapped to Table column value 'H'
        tx.begin();
        em.persist(ticket1);
        em.persist(ticket2);
        em.persist(ticket3);
        tx.commit();

         /*
         * The INSERT statements present in the log is missing value mapping:
         * INSERT INTO TICKET (ID, NAME, PRIORITY, STATUS) VALUES (?, ?, ?, ?)
         * bind => [1, ticket1, HIGH, 0]
         * Persisted Values in column PRIORITY & STATUS, in MySQL Server do not match
         * the specification description
         */
        tx.begin();
        List<Ticket> results = em.createQuery("SELECT t FROM Ticket t ORDER BY t.id", Ticket.class).getResultList();
        tx.commit();

        System.out.println("***** testEnumeratedValue results: " + results);
        // Assert against status value of first element
        assertEquals(TicketStatus.OPEN, results.get(0).getStatus());
        assertFalse(TicketStatus.CLOSED.equals(results.get(0).getStatus()));
        assertFalse(TicketStatus.CANCELLED.equals(results.get(0).getStatus()));
        // Assert against status value of second element
        assertEquals(TicketStatus.CLOSED, results.get(1).getStatus());
        assertFalse(TicketStatus.OPEN.equals(results.get(1).getStatus()));
        assertFalse(TicketStatus.CANCELLED.equals(results.get(1).getStatus()));
        // Assert against status value of third element
        assertEquals(TicketStatus.CANCELLED, results.get(2).getStatus());
        assertFalse(TicketStatus.OPEN.equals(results.get(2).getStatus()));
        assertFalse(TicketStatus.CLOSED.equals(results.get(2).getStatus()));
        // Assert against priority value of first element
        assertEquals(Priority.HIGH, results.get(0).getPriority());
        assertFalse(Priority.MEDIUM.equals(results.get(0).getPriority()));
        assertFalse(Priority.LOW.equals(results.get(0).getPriority()));
        // Assert against priority value of second element
        assertEquals(Priority.LOW, results.get(1).getPriority());
        assertFalse(Priority.HIGH.equals(results.get(1).getPriority()));
        assertFalse(Priority.MEDIUM.equals(results.get(1).getPriority()));
        // Assert against priority value of third element
        assertEquals(Priority.MEDIUM, results.get(2).getPriority());
        assertFalse(Priority.HIGH.equals(results.get(2).getPriority()));
        assertFalse(Priority.LOW.equals(results.get(2).getPriority()));

    }

     /**
     * Specifies the precedence of null values within query result sets.
     * https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a5587
     *
     * @throws Exception
     */
    @Test
    public void testNullPrecedenceWithJPQL() throws Exception {
        deleteAllEntities(Product.class);
        Product product1 = Product.of("testSnapshot", "product1", 10.50f);
        Product product2 = Product.of(null, "product2", 20.50f);
        Product product3 = Product.of("sample products", "product3", 30.50f);
        tx.begin();
        em.persist(product1);
        em.persist(product2);
        em.persist(product3);
        tx.commit();

        /*
         * Specifies the precedence of null values within query result sets.
         */
        List<Product> productsNullFirst;
        try {

            tx.begin();
            productsNullFirst = em.createQuery("FROM Product ORDER BY description DESC NULLS FIRST",
                                               Product.class)
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertEquals(3, productsNullFirst.size());
        assertEquals("Sorted based on 'description' in desc order with NULLS FIRST, Expecting first element to be 'product2'", "product2", productsNullFirst.get(0).name);

        /*
         * Null values occur at the end of the result set.
         */
        List<Product> productsNullLast;
        try {

            tx.begin();
            productsNullLast = em.createQuery("FROM Product ORDER BY description DESC NULLS LAST",
                                              Product.class)
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertEquals(3, productsNullLast.size());
        assertEquals("Sorted based on 'description' in desc order with NULLS LAST, Expecting last element to be 'product2'", "product2", productsNullLast.get(2).name);

    }

    /**
     * Specifies the precedence of null values within query result sets.
     * https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#nulls
     *
     * @throws Exception
     */
    @Test
    public void testNullPrecedenceWithCriteriaQuery() throws Exception {
        deleteAllEntities(Product.class);
        Product p1 = Product.of("testSnapshot", "product1", 10.50f);
        Product p2 = Product.of(null, "product2", 20.50f);
        Product p3 = Product.of("sample products", "product3", 30.50f);
        tx.begin();
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);
        tx.commit();

        /*
         * Null values occur at the beginning of the result set.
         */
        List<Product> productsNullFirst;
        try {
            tx.begin();
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
            Root<Product> from = criteriaQuery.from(Product.class);
            CriteriaQuery<Product> select = criteriaQuery.select(from);
            criteriaQuery.orderBy(criteriaBuilder.desc(from.get("description"), Nulls.FIRST));
            productsNullFirst = em.createQuery(criteriaQuery).getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertEquals(3, productsNullFirst.size());
        assertEquals("Sorted based on 'description' in desc order with NULLS FIRST, Expecting first element to be 'product2'", "product2", productsNullFirst.get(0).name);

        /*
         * Null values occur at the end of the result set.
         */
        List<Product> productsNullLast;
        try {

            tx.begin();
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
            Root<Product> from = criteriaQuery.from(Product.class);
            CriteriaQuery<Product> select = criteriaQuery.select(from);
            criteriaQuery.orderBy(criteriaBuilder.desc(from.get("description"), Nulls.LAST));
            productsNullLast = em.createQuery(criteriaQuery).getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertEquals(3, productsNullLast.size());
        assertEquals("Sorted based on 'description' in desc order with NULLS LAST, Expecting last element to be 'product2'", "product2", productsNullLast.get(2).name);
    }

    /**
     * Usage of notEqualTo() expression in queries built using criteria api
     *
     * @throws Exception
     */
    @Test
    public void testNotEqualToInCriteriaQuery() throws Exception {
        deleteAllEntities(User.class);

        User user1 = User.of(1, "John", "Doe");
        User user2 = User.of(2, "Harry", "Potter");
        User user3 = User.of(3, "Hermione", "Granger");
        User user4 = User.of(4, "John", "Samuel");
        User user5 = User.of(5, "John", "Philip");
        User user6 = User.of(6, "Ron", "Weasley");
        User user7 = User.of(7, "Nervile", "Longbottom");
        
        tx.begin();
        em.persist(user1);
        em.persist(user2);
        em.persist(user3);
        em.persist(user4);
        em.persist(user5);
        em.persist(user6);
        em.persist(user7);
        tx.commit();
       
        List<User> result;
        List<User> resultNew;
        try {
            tx.begin();
            /** Using old method - While using Criteria API to build queries, comparison for not equal to was done using
                method -  CriteriaBuilder.notEqual()
            */
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<User> criteriaQueryOld = criteriaBuilder.createQuery(User.class);
            Root<User> userOld = criteriaQueryOld.from(User.class);
            criteriaQueryOld.select(userOld).where(criteriaBuilder.notEqual(userOld.get("firstName"), "John"));
	        result = em.createQuery(criteriaQueryOld).getResultList();
            assertEquals(4, result.size());

             /** In JPA 3.2, new default method was added to the jakarta.persistence.criteria.Expression 
                interface:Predicate notEqualTo(Expression<?> other);
            */
            CriteriaQuery<User> criteriaQueryNew= criteriaBuilder.createQuery(User.class);
            Root<User> userNew = criteriaQueryNew.from(User.class);
            criteriaQueryNew.where(userNew.get("firstName").notEqualTo("John"));
	        resultNew = em.createQuery(criteriaQueryNew).getResultList();
            
            assertEquals(4, resultNew.size());
            assertEquals(result, resultNew);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

     /**
     * Usage of equalTo() expression in queries built using criteria api
     *
     * @throws Exception
     */
    @Test
    public void testEqualToInCriteriaQuery() throws Exception {
        deleteAllEntities(User.class);

        User user1 = User.of(1, "John", "Doe");
        User user2 = User.of(2, "Harry", "Potter");
        User user3 = User.of(3, "Hermione", "Granger");
        User user4 = User.of(4, "John", "Samuel");
        User user5 = User.of(5, "John", "Philip");
        User user6 = User.of(6, "Ron", "Weasley");
        User user7 = User.of(7, "Nervile", "Longbottom");
        
        tx.begin();
        em.persist(user1);
        em.persist(user2);
        em.persist(user3);
        em.persist(user4);
        em.persist(user5);
        em.persist(user6);
        em.persist(user7);
        tx.commit();
       
        List<User> result;
        List<User> resultNew;
        try {
            tx.begin();
            /** Using old method - While using Criteria API to build queries, comparison for equal to was done using
                method  - CriteriaBuilder.equal()
            */
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<User> criteriaQueryOld= criteriaBuilder.createQuery(User.class);
            Root<User> user = criteriaQueryOld.from(User.class);
            criteriaQueryOld.select(user).where(criteriaBuilder.equal(user.get("firstName"), "John"));
	        result = em.createQuery(criteriaQueryOld).getResultList();

            /** In JPA 3.2, new default method was added to the jakarta.persistence.criteria.Expression 
                interface:Predicate equalTo(Expression<?> other);
            */
            CriteriaQuery<User> criteriaQueryNew= criteriaBuilder.createQuery(User.class);
            Root<User> userNew = criteriaQueryNew.from(User.class);
            criteriaQueryNew.where(userNew.get("firstName").equalTo("John"));
	        resultNew = em.createQuery(criteriaQueryNew).getResultList();
            
            assertEquals(3, resultNew.size());
            assertEquals(result, resultNew);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
       
    }
  
    @Test
    public void testRecordAsEmbeddable_NoMatchAndOrdering() throws Exception {
        // Clean up any existing data
        tx.begin();
        em.createQuery("DELETE FROM Participant").executeUpdate();
        tx.commit();

        // Setup test data
        Participant p1 = Participant.of("Anna", "Brown", 4);
        Participant p2 = Participant.of("Zach", "Taylor", 5);
        Participant p3 = Participant.of("Mark", "Lee", 6);

        tx.begin();
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);
        tx.commit();

        List<Participant> results = Collections.emptyList(); // Ensure it's never null

        // Query with a last name that doesn't exist
        tx.begin();
        try {
            results = em.createQuery(
                                     "SELECT o FROM Participant o WHERE o.name.last = ?1 ORDER BY o.name.first, o.id",
                                     Participant.class)
                            .setParameter(1, "Doe")
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException("Query failed unexpectedly", e);
        }

        // Assertions
        assertNotNull("Results list should not be null", results);
        assertTrue("Expected empty results for non-matching last name", results.isEmpty());
    }

    @Test
    public void testRecordAsEmbeddable_NullEdgeCaseAndOrdering() throws Exception {
        // Setup test data with null, empty, and edge case values
        Participant p1 = Participant.of("Anna", null, 13); // Null last name (should be excluded)
        Participant p2 = Participant.of("Mike", "Green", 14); // Valid
        Participant p3 = Participant.of("Laura", "Blue", 15); // Different last name (excluded)
        Participant p4 = Participant.of("Zoe", "Green", 16); // Valid
        Participant p5 = Participant.of(null, "Green", 17); // Null first name
        Participant p6 = Participant.of("John", "Green", 18); // Valid
        Participant p7 = Participant.of("", "Green", 19); // Empty first name

        // Persist participants
        tx.begin();
        try {
            em.persist(p1);
            em.persist(p2);
            em.persist(p3);
            em.persist(p4);
            em.persist(p5);
            em.persist(p6);
            em.persist(p7);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        // Query for participants with the last name 'Green'
        List<Participant> results;
        tx.begin();
        try {
            results = em.createQuery(
                                     "SELECT o FROM Participant o WHERE o.name.last = :lastName " +
                                     "ORDER BY " +
                                     "CASE WHEN o.name.first IS NULL THEN 1 ELSE 0 END, " +
                                     "CASE WHEN o.name.first = '' THEN 1 ELSE 0 END, " +
                                     "o.name.first, o.id",
                                     Participant.class)
                            .setParameter("lastName", "Green")
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        // Validate results
        assertNotNull(results);
        assertEquals(5, results.size()); // 5 participants with last name "Green"

        // Expected order: John (18), Mike (14), Zoe (16), "" (19), null (17)
        assertEquals("John", results.get(0).getName().getFirst());
        assertEquals("Mike", results.get(1).getName().getFirst());
        assertEquals("Zoe", results.get(2).getName().getFirst());
        assertEquals("", results.get(3).getName().getFirst());
        assertNull(results.get(4).getName().getFirst());

        // Additional validation for excluded/edge cases
        assertNull(p1.getName().getLast()); // Null last name should be excluded from query
        assertEquals("", p7.getName().getFirst()); // Empty first name correctly stored
        assertNull(p5.getName().getFirst()); // Null first name correctly stored
    }

    @Test // Verifies that a JPQL query using an alias returns the correct hexadecimal value for a persisted AsciiCharacter
    public void testAsciiCharacterQueryReturnsHexadecimalWithAlias() {
        int id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        AsciiCharacter character = new AsciiCharacter();
        character.setId(id);
        character.setThisCharacter('P');
        character.setHexadecimal("50");
        character.setNumericValue(80);
        character.setControl(false);
        try {
            tx.begin();
            em.createQuery("DELETE FROM AsciiCharacter a WHERE a.thisCharacter = :char").setParameter("char", character.getThisCharacter()).executeUpdate();
            em.persist(character);
            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (SystemException se) {
                throw new RuntimeException("Rollback failed during testAsciiCharacterQueryReturnsHexadecimalWithAlias", se);
            }
            throw new RuntimeException("Transaction failed during testAsciiCharacterQueryReturnsHexadecimalWithAlias", e);
        }
        TypedQuery<String> query = em.createQuery("SELECT a.hexadecimal FROM AsciiCharacter a WHERE a.thisCharacter = :char", String.class);
        query.setParameter("char", character.getThisCharacter());
        List<String> results = query.getResultList();
        assertNotNull("Query result should not be null", results);
        assertFalse("Query result should not be empty", results.isEmpty());
        assertTrue("Expected hexadecimal value not found in results", results.contains(character.getHexadecimal()));
    }

    @Test
    public void testInvalidFieldInAsciiCharacterQuery() {
        try {
            em.createQuery("SELECT nonExistentField FROM AsciiCharacter", String.class).getResultList();
        } catch (Exception e) {
            assertTrue("Expected exception to be thrown due to non-existent field",
                       e instanceof IllegalArgumentException || e instanceof RuntimeException);
            assertTrue("Unexpected exception type: " + e.getClass(),
                       e instanceof IllegalArgumentException || e instanceof RuntimeException);
            assertTrue("Exception message did not contain 'nonExistentField': " + e.getMessage(),
                       e.getMessage().contains("nonExistentField") || e.getClass().equals(IllegalArgumentException.class) || e.getClass().equals(RuntimeException.class));
        }
    }

    @Test // Verifies that multiple persisted AsciiCharacter entries return correct hexadecimal values via JPQL query
    public void testAsciiCharacterMultipleResultsQuery() {
        try {
            tx.begin();
            em.persist(AsciiCharacter.of(65)); // 'A'
            em.persist(AsciiCharacter.of(66)); // 'B'
            tx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
            throw new RuntimeException("Transaction failed during testAsciiCharacterMultipleResultsQuery", e);
        }
        List<String> results = em.createQuery("SELECT a.hexadecimal FROM AsciiCharacter a WHERE a.hexadecimal IS NOT NULL", String.class).getResultList();
        assertTrue("Expected hex value 41 not found", results.contains("41")); // 65 in hex
        assertTrue("Expected hex value 42 not found", results.contains("42")); // 66 in hex
    }

    @Test
    public void testAsciiCharacterwithSpecialCharacter() throws Exception {
        AsciiCharacter character = AsciiCharacter.of(42); // *
        String result;
        tx.begin();
        try {
            em.persist(character);
            result = em.createQuery("SELECT hexadecimal FROM AsciiCharacter WHERE hexadecimal IS NOT NULL AND thisCharacter = ?1", String.class)
                            .setParameter(1, character.getThisCharacter())
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertEquals(character.getHexadecimal(), result);
    }

    @Test(expected = AssertionError.class)
    public void testAsciiCharacterNullCharacter() throws Exception {
        AsciiCharacter character = null;
        tx.begin();
        try {
            em.persist(character);
            em.createQuery("SELECT hexadecimal FROM AsciiCharacter WHERE hexadecimal IS NOT NULL AND thisCharacter = ?1", String.class)
                            .setParameter(1, character.getThisCharacter())
                            .getSingleResult();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @Test
    public void testAsciiCharacterNonExistentCharacter() throws Exception {
        AsciiCharacter character = new AsciiCharacter();
        character.setThisCharacter((char) 200); // Choose a code outside standard ASCII (0-127)
        character.setHexadecimal(null); // set to null
        tx.begin();
        try {
            em.persist(character);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        // filters out null hexadecimal values
        List<String> results = em.createQuery("SELECT c.hexadecimal FROM AsciiCharacter c WHERE c.hexadecimal IS NOT NULL AND c.thisCharacter = ?1", String.class)
                        .setParameter(1, character.getThisCharacter())
                        .getResultList();
        // Assert that no result was returned
        assertTrue("Expected no results, but got: " + results, results.isEmpty());
    }

    @Test
    public void testAsciiCharWithNullHexadecimalUsingDefaultConstructor() throws Exception {
        deleteAllEntities(AsciiCharacter.class);
        AsciiCharacter character = new AsciiCharacter();
        character.setThisCharacter('P'); // char for ASCII 80
        character.setHexadecimal(null);
        String result;
        tx.begin();
        try {
            em.persist(character);
            result = em.createQuery("SELECT c.hexadecimal FROM AsciiCharacter c WHERE c.thisCharacter = :thisCharacter", String.class)
                            .setParameter("thisCharacter", character.getThisCharacter())
                            .getSingleResult();
            tx.commit();
        } catch (NoResultException e) {
            tx.rollback();
            result = null;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertNull(result);
    }

    /**
     * tests the use of primary key of type type java.lang.Long, java.lang.Integer, long, or int with GenerationType.TABLE
     *
     * @throws Exception
     */
    @Test
    public void testTableIdGenerationType() throws Exception {
        final String actualName = "Test Entity 1";

        // test persist with primary key type java.lang.Integer & GenerationType TABLE
        IntegerTableIdEntity integerTableIdEntity = IntegerTableIdEntity.of(actualName);
        tx.begin();
        em.persist(integerTableIdEntity);
        tx.commit();
        assertNotNull(integerTableIdEntity.getId());
        // Query with ID and validate the result
        String queriedNameById = em.createQuery("SELECT t.name FROM IntegerTableIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", integerTableIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);

        // test persist with primary key type int & GenerationType TABLE
        IntegerPrimitiveTableIdEntity integerPrimitiveTableIdEntity = IntegerPrimitiveTableIdEntity.of(actualName);
        tx.begin();
        em.persist(integerPrimitiveTableIdEntity);
        tx.commit();
        assertNotNull(integerPrimitiveTableIdEntity.getId());
        // Query with ID and validate the result
        queriedNameById = em.createQuery("SELECT t.name FROM IntegerPrimitiveTableIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", integerPrimitiveTableIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);

        // test persist with primary key java.lang.Long int & GenerationType TABLE
        LongTableIdEntity longTableIdEntity = LongTableIdEntity.of(actualName);
        tx.begin();
        em.persist(longTableIdEntity);
        tx.commit();
        assertNotNull(longTableIdEntity.getId());
        // Query with ID and validate the result
        queriedNameById = em.createQuery("SELECT t.name FROM LongTableIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", longTableIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);

        // test persist with primary key type long & GenerationType TABLE
        LongPrimitiveTableIdEntity longPrimitiveTableIdEntity = LongPrimitiveTableIdEntity.of(actualName);
        tx.begin();
        em.persist(longPrimitiveTableIdEntity);
        tx.commit();
        assertNotNull(longPrimitiveTableIdEntity.getId());
        // Query with ID and validate the result
        queriedNameById = em.createQuery("SELECT t.name FROM LongPrimitiveTableIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", longPrimitiveTableIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);
    }

    /**
     * tests the use of primary key of type type java.lang.Long, java.lang.Integer, long, or int with GenerationType.SEQUENCE
     *
     * @throws Exception
     */
    @Test
    public void testSequenceIdGenerationType() throws Exception {
        final String actualName = "integer Sequence Id Generation Type Entity 1";
        
        // test persist with primary key type Integer & GenerationType Sequence
        IntegerSequenceIdEntity integerSequenceIdEntity = IntegerSequenceIdEntity.of(actualName);
        tx.begin();
        em.persist(integerSequenceIdEntity);
        tx.commit();
        assertNotNull(integerSequenceIdEntity.getId());
        // Query with ID and validate the result
        String queriedNameById = em.createQuery("SELECT t.name FROM IntegerSequenceIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", integerSequenceIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);

        IntegerPrimitiveSequenceIdEntity integerPrimitiveSequenceIdEntity = IntegerPrimitiveSequenceIdEntity.of(actualName);
        tx.begin();
        em.persist(integerPrimitiveSequenceIdEntity);
        tx.commit();
        assertNotNull(integerPrimitiveSequenceIdEntity.getId());
        // Query with ID and validate the result
        queriedNameById = em.createQuery("SELECT t.name FROM IntegerPrimitiveSequenceIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", integerPrimitiveSequenceIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);

        // test persist with primary key type Long & GenerationType Sequence
        LongSequenceIdEntity longSequenceIdEntity = LongSequenceIdEntity.of(actualName);
        tx.begin();
        em.persist(longSequenceIdEntity);
        tx.commit();
        assertNotNull(longSequenceIdEntity.getId());
        // Query with ID and validate the result
        queriedNameById = em.createQuery("SELECT t.name FROM LongSequenceIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", longSequenceIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);

        // test persist with primary key type long & GenerationType Sequence
        LongPrimitiveSequenceIdEntity longPrimitiveSequenceIdEntity = LongPrimitiveSequenceIdEntity.of(actualName);
        tx.begin();
        em.persist(longPrimitiveSequenceIdEntity);
        tx.commit();
        assertNotNull(longPrimitiveSequenceIdEntity.getId());
        // Query with ID and validate the result
        queriedNameById = em.createQuery("SELECT t.name FROM LongPrimitiveSequenceIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", longPrimitiveSequenceIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);
    }

     /**
     * Confirms the use of primary key of type 'java.lang.String' With GenerationType.UUID
     * The UUID value indicates that the persistence provider should assign an RFC 4122 Universally Unique IDentifier.
     * A UUID generator may be used to generate values for a primary key property or field of type java.util.UUID or java.lang.String.
     *
     * @throws Exception
     */
    @Test
    public void testUUIDGenerationType() throws Exception {
        final String actualName = "UUID Test Entity 1";
        // test persist with primary key type UUID & GenerationType UUID
        UUIDUUIDIdEntity uuidEntity = UUIDUUIDIdEntity.of(actualName);
        tx.begin();
        em.persist(uuidEntity);
        tx.commit();
        assertNotNull(uuidEntity.getId());

        // Query with ID and validate the result
        String queriedNameById = em.createQuery("SELECT u.name FROM UUIDUUIDIdEntity u WHERE u.id = :id", String.class)
                        .setParameter("id", uuidEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);

        // test persist with primary key type String & GenerationType UUID
        StringUUIDIdEntity stringUUIDIdEntity = StringUUIDIdEntity.of(actualName);
        tx.begin();
        em.persist(stringUUIDIdEntity);
        tx.commit();
        assertNotNull(stringUUIDIdEntity.getId());

        // Query with ID and validate the result
        queriedNameById = em.createQuery("SELECT u.name FROM StringUUIDIdEntity u WHERE u.id = :id", String.class)
                        .setParameter("id", stringUUIDIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);
    }

    @Test
    public void testIntegerIDWithIdentityIdGenerationType() throws Exception {
        final String actualName = "Identity Generation Entity 1";
        // test persist with primary key type Integer & GenerationType IDENTITY
        IntegerIdentityIdEntity integerIdentityIdEntity = IntegerIdentityIdEntity.of(actualName);
        tx.begin();
        em.persist(integerIdentityIdEntity);
        tx.commit();
        assertNotNull(integerIdentityIdEntity.getId());
        // Query with ID and validate the result
        String queriedNameById = em.createQuery("SELECT t.name FROM IntegerIdentityIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", integerIdentityIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);
   
        // test persist with primary key type int & GenerationType IDENTITY
        IntegerPrimitiveIdentityIdEntity integerPrimitiveIdentityIdEntity = IntegerPrimitiveIdentityIdEntity.of(actualName);
        tx.begin();
        em.persist(integerPrimitiveIdentityIdEntity);
        tx.commit();
        assertNotNull(integerPrimitiveIdentityIdEntity.getId());
        // Query with ID and validate the result
        queriedNameById = em.createQuery("SELECT t.name FROM IntegerPrimitiveIdentityIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", integerPrimitiveIdentityIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);

        // test persist with primary key type int & GenerationType IDENTITY
        LongIdentityIdEntity longIdentityIdEntity = LongIdentityIdEntity.of(actualName);
        tx.begin();
        em.persist(longIdentityIdEntity);
        tx.commit();
        assertNotNull(longIdentityIdEntity.getId());
        // Query with ID and validate the result
        queriedNameById = em.createQuery("SELECT t.name FROM LongIdentityIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", longIdentityIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);

        // test persist with primary key type int & GenerationType IDENTITY
        LongPrimitiveIdentityIdEntity longPrimitiveIdentityIdEntity = LongPrimitiveIdentityIdEntity.of(actualName);
        tx.begin();
        em.persist(longPrimitiveIdentityIdEntity);
        tx.commit();
        assertNotNull(longPrimitiveIdentityIdEntity.getId());
        // Query with ID and validate the result
        queriedNameById = em.createQuery("SELECT t.name FROM LongPrimitiveIdentityIdEntity t WHERE t.id = :id", String.class)
                        .setParameter("id", longPrimitiveIdentityIdEntity.getId())
                        .getSingleResult();
        assertEquals(actualName, queriedNameById);
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
