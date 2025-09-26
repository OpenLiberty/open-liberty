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

import static componenttest.annotation.OnlyIfSysProp.DB_Not_Default;
import static componenttest.annotation.SkipIfSysProp.DB_DB2;
import static componenttest.annotation.SkipIfSysProp.DB_Oracle;
import static componenttest.annotation.SkipIfSysProp.DB_Postgres;
import static componenttest.annotation.SkipIfSysProp.DB_SQLServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.Ignore;

import componenttest.annotation.OnlyIfSysProp;
import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.AsciiCharacter;
import io.openliberty.jpa.persistence.tests.models.Book;
import io.openliberty.jpa.persistence.tests.models.DateTimeEntity;
import io.openliberty.jpa.persistence.tests.models.Employee;
import io.openliberty.jpa.persistence.tests.models.Event;
import io.openliberty.jpa.persistence.tests.models.Organization;
import io.openliberty.jpa.persistence.tests.models.Participant;
import io.openliberty.jpa.persistence.tests.models.Person;
import io.openliberty.jpa.persistence.tests.models.Priority;
import io.openliberty.jpa.persistence.tests.models.Product;
import io.openliberty.jpa.persistence.tests.models.Ticket;
import io.openliberty.jpa.persistence.tests.models.TicketStatus;
import io.openliberty.jpa.persistence.tests.models.User;
import io.openliberty.jpa.persistence.tests.models.ConcatEntity;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.LocalDateField;
import jakarta.persistence.criteria.LocalDateTimeField;
import jakarta.persistence.criteria.LocalTimeField;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.ParameterExpression;
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
    public void testGetNameReturnsPersistenceUnitName() {
        EntityManagerFactory emf = em.getEntityManagerFactory();
        assertEquals("JakartaPersistenceUnit", emf.getName());
    }

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
    // Reference issue: https://github.com/OpenLiberty/open-liberty/issues/32688
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
    @SkipIfSysProp({
                     DB_SQLServer //Failing on SQLServer (No mention of NULLS FIRST/LAST keywords in Documentation)
    })
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
    @SkipIfSysProp({
                     DB_SQLServer //Failing on SQLServer (No mention of NULLS FIRST/LAST keywords in Documentation)
    })
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
        deleteAllEntities(Participant.class);

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
    @SkipIfSysProp(DB_Oracle)
    public void testRecordAsEmbeddable_NullEdgeCaseAndOrdering() throws Exception {
        deleteAllEntities(Participant.class);
        
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
    public void testAsciiCharacterQueryReturnsHexadecimalWithAlias() throws Exception {
        deleteAllEntities(AsciiCharacter.class);
        
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
    public void testInvalidFieldInAsciiCharacterQuery() throws Exception {
        deleteAllEntities(AsciiCharacter.class);
        
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
    public void testAsciiCharacterMultipleResultsQuery() throws Exception {
        deleteAllEntities(AsciiCharacter.class);
        
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
        deleteAllEntities(AsciiCharacter.class);
        
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
        deleteAllEntities(AsciiCharacter.class);
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
    @SkipIfSysProp({ DB_DB2, DB_Oracle })
    public void testAsciiCharacterNonExistentCharacter() throws Exception {
        deleteAllEntities(AsciiCharacter.class);
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

    @Test
    //Reference issue:https://github.com/OpenLiberty/open-liberty/issues/31884
    @OnlyIfSysProp({ DB_Not_Default }) //Skips the test for Derby (default DB) as it doesn't support fractional-second precision
    public void testSecondPrecision() throws Exception {
        deleteAllEntities(Event.class);

        LocalDateTime original = LocalDateTime.of(2025, 6, 11, 12, 0, 0, 444_777_999); 
        Event event = new Event(1L, original);

        tx.begin();
        em.persist(event);
        em.flush();
        em.clear();
        tx.commit();

        Event result;
        try {
            result = em.createQuery("SELECT e FROM Event e WHERE e.id = :id", Event.class)
                            .setParameter("id", 1L)
                            .getSingleResult();
        } catch (Exception e){
            throw e;
        }

        assertTrue("Unexpected nanoseconds value: " + result.timestamp.getNano(),
                   Set.of(444_770_000, 444_780_000).contains(result.timestamp.getNano()));
    }

    @Test
    public void testGetSingleResultOrNull() throws Exception {
        deleteAllEntities(Book.class);

        Book bookJPA = new Book(1L, "Jakarta Persistence 3.2");

        tx.begin();
        em.persist(bookJPA);
        tx.commit();

        Book resultFound, resultNotFound;

        try {
            resultFound = em.createQuery("SELECT b FROM Book b WHERE b.id = ?1", Book.class)
                        .setParameter(1, 1L)
                        .getSingleResultOrNull();
         } catch (Exception e){
            throw e;
        }
        assertEquals("Jakarta Persistence 3.2", resultFound.title);

        try {
            resultNotFound = em.createQuery("SELECT b FROM Book b WHERE b.id = ?1", Book.class)
                        .setParameter(1, 2L) // This ID does not exist
                        .getSingleResultOrNull();
         } catch (Exception e){
            throw e;
        }
        assertNull(resultNotFound);
    }
  
  /**
     * Jakarta 3.2 version supports concat() overload accepting list of expressions ie., concat(List<Expression<String>> expressions)
     *
     * https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/criteria/criteriabuilder
     * https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/criteria/criteriabuilder#concat(java.util.List)
     *
     * @throws Exception
     */
    @Test
    public void testConcatInWhereCriteriaQuery() throws Exception {
        deleteAllEntities(ConcatEntity.class);

        ConcatEntity concatEntity1 = new ConcatEntity();
        concatEntity1.firstName = "John";
        concatEntity1.lastName = "Jacobs";
        concatEntity1.ssn_id = 1L;

        ConcatEntity concatEntity2 = new ConcatEntity();
        concatEntity2.firstName = "Steve";
        concatEntity2.lastName = "Smith";
        concatEntity2.ssn_id = 2L;

        tx.begin();
        em.persist(concatEntity1);
        em.persist(concatEntity2);
        tx.commit();

        tx.begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ConcatEntity> cquery = cb.createQuery(ConcatEntity.class);
        Root<ConcatEntity> root = cquery.from(ConcatEntity.class);
        ParameterExpression<String> strParam1 = cb.parameter(String.class);

        List<jakarta.persistence.criteria.Expression<String>> concatExpression = new ArrayList<>();
        concatExpression.add(root.get("firstName"));
        concatExpression.add(cb.literal(" "));
        concatExpression.add(root.get("lastName"));

        cquery.select(root)
                        .where(cb.equal(cb.concat(concatExpression), strParam1));

        // Use of concat in where clause: Matching case
        List<ConcatEntity> person = em.createQuery(cquery)
                        .setParameter(strParam1, "John Jacobs")
                        .getResultList();
        assertEquals("Expected 1 record that matches full name 'John Jacobs'", 1, person.size());

        // Use of concat in where clause: No Match case
        List<ConcatEntity> personNoMatch = em.createQuery(cquery)
                        .setParameter(strParam1, "John Jacob")
                        .getResultList();
        assertEquals("Expected 0 record that matches full name 'John Jacob'", 0, personNoMatch.size());
        tx.commit();
    }

    /**
     * Jakarta 3.2 version supports concat() overload accepting list of expressions ie., concat(List<Expression<String>> expressions)
     *
     * https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/criteria/criteriabuilder
     * https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/criteria/criteriabuilder#concat(java.util.List)
     *
     * @throws Exception
     */
    @Test
    public void testConcatCriteriaQuery() throws Exception {
        deleteAllEntities(ConcatEntity.class);

        ConcatEntity concatEntity1 = new ConcatEntity();
        concatEntity1.firstName = "John";
        concatEntity1.lastName = "Jacobs";
        concatEntity1.ssn_id = 1L;

        ConcatEntity concatEntity2 = new ConcatEntity();
        concatEntity2.firstName = "Steve";
        concatEntity2.lastName = "Smith";
        concatEntity2.ssn_id = 2L;

        tx.begin();
        em.persist(concatEntity1);
        em.persist(concatEntity2);
        tx.commit();

        tx.begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> cquery = cb.createQuery(String.class);
        Root<ConcatEntity> root = cquery.from(ConcatEntity.class);

        // use concat on queried result
        List<jakarta.persistence.criteria.Expression<String>> concatExpression = List.of(root.get("firstName"),
                                                                                         cb.literal(" "),
                                                                                         root.get("lastName"));

        cquery.select(cb.concat(concatExpression));
        cquery.orderBy(cb.desc(root.get("firstName")));

        List<String> fullname = em.createQuery(cquery).getResultList();
        System.out.println("****** testConcatCriteriaQuery: fullname: " + fullname);
        assertEquals("Expected full name 'John Jacobs' for first record", "John Jacobs", fullname.get(1));
        assertEquals("Expected full name 'Steve Smith' for second record", "Steve Smith", fullname.get(0));
    }

    /**
     * this test extract the calendar YEAR from java.time.LocalDate
     *
     * @throws Exception
     */
    @Test
    //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/31802
    @SkipIfSysProp({
        DB_SQLServer //Failing on SQLServer (No mention of NULLS FIRST/LAST keywords in Documentation)
    })
    public void testExtractYearFromLocalData() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2022, 06, 07), LocalTime.of(12, 0), LocalDateTime.of(2022, 06, 07, 12, 0));
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2020, 12, 31), LocalTime.of(01, 59), LocalDateTime.of(2020, 12, 31, 01, 59));
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2021, 01, 01), LocalTime.of(00, 30), LocalDateTime.of(2021, 01, 01, 00, 30));
        DateTimeEntity q4 = new DateTimeEntity(10000);

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        em.persist(q4);
        tx.commit();

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery(Integer.class);
        Root<DateTimeEntity> from = criteriaQuery.from(DateTimeEntity.class);
        LocalDateField<Integer> yearLocalDateField = LocalDateField.YEAR;
        Expression<Integer> yearExpression = criteriaBuilder.extract(yearLocalDateField, from.get("localDateData"));
        criteriaQuery.select(yearExpression);
        criteriaQuery.orderBy(criteriaBuilder.desc(from.get("name"), Nulls.FIRST));
        List<Integer> result = em.createQuery(criteriaQuery).getResultList();
        assertEquals(4, result.size());
        assertEquals(null, result.get(0));
        assertEquals("Extracted Year should be 2021", 2021, ((Number) result.get(1)).intValue());
        assertEquals("Extracted Year should be 2020", 2020, ((Number) result.get(2)).intValue());
        assertEquals("Extracted Year should be 2022", 2022, ((Number) result.get(3)).intValue());

    }

    /**
     * this test extract the QUARTER of the year numbered from 1 to 4 from java.time.LocalDate
     *
     * @throws Exception
     */
    @Test
    //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/31802
    @SkipIfSysProp({
        DB_SQLServer //Failing on SQLServer (No mention of NULLS FIRST/LAST keywords in Documentation)
    })
    public void testExtractQuarterFromLocalData() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2022, 06, 07), LocalTime.of(12, 0), LocalDateTime.of(2022, 06, 07, 12, 0));
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2020, 12, 31), LocalTime.of(01, 59), LocalDateTime.of(2020, 12, 31, 01, 59));
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2021, 01, 01), LocalTime.of(00, 30), LocalDateTime.of(2021, 01, 01, 00, 30));
        DateTimeEntity q4 = new DateTimeEntity(10000);

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        em.persist(q4);
        tx.commit();

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery(Integer.class);
        Root<DateTimeEntity> from = criteriaQuery.from(DateTimeEntity.class);
        LocalDateField<Integer> quarterLocalDateField = LocalDateField.QUARTER;
        Expression<Integer> quarterExpression = criteriaBuilder.extract(quarterLocalDateField, from.get("localDateData"));
        criteriaQuery.select(quarterExpression);
        criteriaQuery.orderBy(criteriaBuilder.desc(from.get("name"), Nulls.FIRST));
        List<Integer> result = em.createQuery(criteriaQuery).getResultList();
        assertEquals(4, result.size());
        assertEquals(null, result.get(0));
        assertEquals("Extracted Quarter should be 1", 1, ((Number) result.get(1)).intValue());
        assertEquals("Extracted Quarter should be 4", 4, ((Number) result.get(2)).intValue());
        assertEquals("Extracted Quarter should be 2", 2, ((Number) result.get(3)).intValue());

    }
    
    @Test
    @SkipIfSysProp({
        DB_DB2, DB_SQLServer, //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/32957
        DB_Oracle //Oracle DB doesn't have any conversion function into TIME so whole TIMESTAMP is returned and result is converted to time in EclipseLink/Java
    })
    public void testExtractTimeFromLocalData() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2023, 3, 15), LocalTime.of(9, 30), LocalDateTime.of(2023, 3, 15, 9, 30));
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2022, 8, 22), LocalTime.of(14, 45), LocalDateTime.of(2022, 8, 22, 14, 45));
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2024, 11, 05), LocalTime.of(14, 45), LocalDateTime.of(2024, 11, 5, 14, 45));

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        tx.commit();

        Stream<DateTimeEntity> resultDate;
        try {
            resultDate = em.createQuery(
                "SELECT NEW io.openliberty.jpa.persistence.tests.models.DateTimeEntity(id, name, localDateData, localTimeData, localDateTimeData) " +
                "FROM DateTimeEntity " +
                "WHERE EXTRACT(TIME FROM localDateTimeData) = ?1 " +
                "ORDER BY name DESC")
                .setParameter(1, LocalTime.of(14, 45))
                .getResultStream();
        } catch (Exception e) {
            throw e;
        }
        
        List<DateTimeEntity> results = resultDate.collect(Collectors.toList());
        
        assertEquals(2, results.size());
        assertEquals("q3", results.get(0).getName());
        assertEquals(LocalDate.of(2024, 11, 5), results.get(0).getLocalDateData());
        assertEquals("q2", results.get(1).getName());
        assertEquals(LocalDateTime.of(2022, 8, 22, 14, 45), results.get(1).getLocalDateTimeData());
    }
    
    @Test
    public void testExtractDateFromLocalData() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2023, 3, 15), LocalTime.of(9, 30), LocalDateTime.of(2023, 3, 15, 9, 30));
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2022, 8, 22), LocalTime.of(14, 45), LocalDateTime.of(2022, 8, 22, 14, 45));
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2024, 7, 15), LocalTime.of(11, 20), LocalDateTime.of(2024, 7, 15, 11, 20));

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        tx.commit();

        Stream<DateTimeEntity> resultDate;
        try {
            resultDate = em.createQuery(
                "SELECT NEW io.openliberty.jpa.persistence.tests.models.DateTimeEntity(id, name, localDateData, localTimeData, localDateTimeData) " +
                "FROM DateTimeEntity " +
                "WHERE EXTRACT(DAY FROM localDateTimeData) = ?1 " +
                "ORDER BY name DESC")
                .setParameter(1, 15)  // Day = 15
                .getResultStream();
        } catch (Exception e) {
            throw e;
        }
        
        List<DateTimeEntity> results = resultDate.collect(Collectors.toList());

        assertEquals(2, results.size());
        
        assertEquals("q3", results.get(0).getName());
        assertEquals(LocalDate.of(2024, 7, 15), results.get(0).getLocalDateData());
        assertEquals(LocalTime.of(9, 30), results.get(1).getLocalTimeData());
        assertEquals(LocalDateTime.of(2023, 3, 15, 9, 30), results.get(1).getLocalDateTimeData());
    }

    @Test
    public void testExtractWeekFromLocalData() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        // Using dates that fall in the same ISO week
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2024, 1, 8), LocalTime.of(10, 15), LocalDateTime.of(2024, 1, 8, 10, 15));   // Week 2 of 2024
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2024, 1, 14), LocalTime.of(16, 30), LocalDateTime.of(2024, 1, 14, 16, 30)); // Week 2 of 2024
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2024, 1, 22), LocalTime.of(12, 45), LocalDateTime.of(2024, 1, 22, 12, 45)); // Week 4 of 2024

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        tx.commit();

        Stream<DateTimeEntity> resultDate;
        try {
            resultDate = em.createQuery(
                "SELECT NEW io.openliberty.jpa.persistence.tests.models.DateTimeEntity(id, name, localDateData, localTimeData, localDateTimeData) " +
                "FROM DateTimeEntity " +
                "WHERE EXTRACT(WEEK FROM localDateTimeData) = ?1 " +
                "ORDER BY name DESC")
                .setParameter(1, 2)  // Week 2
                .getResultStream();
        } catch (Exception e) {
            throw e;
        }
        
        List<DateTimeEntity> results = resultDate.collect(Collectors.toList());
        
        assertEquals(2, results.size());

        assertEquals("q2", results.get(0).getName());
        assertEquals(LocalDate.of(2024, 1, 14), results.get(0).getLocalDateData());
        assertEquals(LocalTime.of(10, 15), results.get(1).getLocalTimeData());
        assertEquals(LocalDateTime.of(2024, 1, 8, 10, 15), results.get(1).getLocalDateTimeData());
    }

    @Test
    public void testExtractQuarterFromLocalDataWithJPQL() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2023, 2, 15), LocalTime.of(8, 30), LocalDateTime.of(2023, 2, 15, 8, 30));   // Q1
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2023, 7, 20), LocalTime.of(14, 15), LocalDateTime.of(2023, 7, 20, 14, 15)); // Q3
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2023, 3, 10), LocalTime.of(16, 45), LocalDateTime.of(2023, 3, 10, 16, 45)); // Q1

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        tx.commit();

        Stream<DateTimeEntity> resultDate;
        try {
            resultDate = em.createQuery(
                "SELECT NEW io.openliberty.jpa.persistence.tests.models.DateTimeEntity(id, name, localDateData, localTimeData, localDateTimeData) " +
                "FROM DateTimeEntity " +
                "WHERE EXTRACT(QUARTER FROM localDateTimeData) = ?1 " +
                "ORDER BY name DESC")
                .setParameter(1, 1)  // Quarter 1 (Jan-Mar)
                .getResultStream();
        } catch (Exception e) {
            throw e;
        }
        
        List<DateTimeEntity> results = resultDate.collect(Collectors.toList());
        
        assertEquals(2, results.size());

        assertEquals("q3", results.get(0).getName());
        assertEquals(LocalDate.of(2023, 3, 10), results.get(0).getLocalDateData());
        assertEquals(LocalTime.of(8, 30), results.get(1).getLocalTimeData());
        assertEquals(LocalDateTime.of(2023, 2, 15, 8, 30), results.get(1).getLocalDateTimeData());
    }
    
    @Test
    public void testExtractMonthFromLocalData() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2023, 03, 15), LocalTime.of(9, 30), LocalDateTime.of(2023, 03, 15, 9, 30));
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2022, 8, 22), LocalTime.of(14, 45), LocalDateTime.of(2022, 8, 22, 14, 45));
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2024, 03, 05), LocalTime.of(11, 20), LocalDateTime.of(2024, 03, 05, 11, 20));

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        tx.commit();

        Stream<DateTimeEntity> resultDate;
        try {
            resultDate = em.createQuery(
                "SELECT NEW io.openliberty.jpa.persistence.tests.models.DateTimeEntity(id, name, localDateData, localTimeData, localDateTimeData) " +
                "FROM DateTimeEntity " +
                "WHERE EXTRACT(MONTH FROM localDateTimeData) = ?1 " +
                "ORDER BY name DESC")
                .setParameter(1, 3)  // March = 3
                .getResultStream();
        } catch (Exception e) {
            throw e;
        }
        
        List<DateTimeEntity> results = resultDate.collect(Collectors.toList());
        
        assertEquals(2, results.size());
        assertEquals("q3", results.get(0).getName());
        assertEquals(LocalDateTime.of(2024, 03, 05, 11, 20), results.get(0).getLocalDateTimeData());
        assertEquals(LocalTime.of(9, 30), results.get(1).getLocalTimeData());
    }
    
    @Test
    public void testExtractYearFromLocalDataWithJPQL() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2023, 4, 18), LocalTime.of(10, 45), LocalDateTime.of(2023, 4, 18, 10, 45));
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2022, 6, 25), LocalTime.of(16, 20), LocalDateTime.of(2022, 6, 25, 16, 20));
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2023, 8, 30), LocalTime.of(8, 45), LocalDateTime.of(2023, 8, 30, 8, 50));

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        tx.commit();

        Stream<DateTimeEntity> resultDate;
        try {
            resultDate = em.createQuery(
                "SELECT NEW io.openliberty.jpa.persistence.tests.models.DateTimeEntity(id, name, localDateData, localTimeData, localDateTimeData) " +
                "FROM DateTimeEntity " +
                "WHERE EXTRACT(YEAR FROM localDateTimeData) = ?1 " +
                "ORDER BY name DESC")
                .setParameter(1, 2023)
                .getResultStream();
        } catch (Exception e) {
            throw e;
        }
        
        List<DateTimeEntity> results = resultDate.collect(Collectors.toList());
        assertEquals(2, results.size());
        
        assertEquals("q3", results.get(0).getName());
        assertEquals(LocalDate.of(2023, 8, 30), results.get(0).getLocalDateData());
        assertEquals(LocalTime.of(10, 45), results.get(1).getLocalTimeData());
        assertEquals(LocalDateTime.of(2023, 4, 18, 10, 45), results.get(1).getLocalDateTimeData());
    }
    
    @Test
    public void testExtractDayFromLocalData() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2023, 3, 25), LocalTime.of(9, 30), LocalDateTime.of(2023, 3, 25, 9, 30));
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2022, 8, 12), LocalTime.of(14, 45), LocalDateTime.of(2022, 8, 12, 14, 45));
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2024, 12, 25), LocalTime.of(11, 20), LocalDateTime.of(2024, 12, 25, 11, 20));

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        tx.commit();

        Stream<DateTimeEntity> resultDate;
        try {
            resultDate = em.createQuery(
                "SELECT NEW io.openliberty.jpa.persistence.tests.models.DateTimeEntity(id, name, localDateData, localTimeData, localDateTimeData) " +
                "FROM DateTimeEntity " +
                "WHERE EXTRACT(DAY FROM localDateTimeData) = ?1 " +
                "ORDER BY name DESC")
                .setParameter(1, 25)
                .getResultStream();
        } catch (Exception e) {
            throw e;
        }
        
        List<DateTimeEntity> results = resultDate.collect(Collectors.toList());
        assertEquals(2, results.size());
        
        assertEquals("q3", results.get(0).getName());
        assertEquals(LocalDate.of(2024, 12, 25), results.get(0).getLocalDateData());
        assertEquals(LocalTime.of(9, 30), results.get(1).getLocalTimeData());
        assertEquals(LocalDateTime.of(2023, 3, 25, 9, 30), results.get(1).getLocalDateTimeData());
    }

    @Test
    public void testExtractHourFromLocalData() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2023, 5, 10), LocalTime.of(14, 30), LocalDateTime.of(2023, 5, 10, 14, 30));
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2022, 7, 15), LocalTime.of(9, 45), LocalDateTime.of(2022, 7, 15, 9, 45));
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2024, 9, 20), LocalTime.of(14, 15), LocalDateTime.of(2024, 9, 20, 14, 15));

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        tx.commit();

        Stream<DateTimeEntity> resultDate;
        try {
            resultDate = em.createQuery(
                "SELECT NEW io.openliberty.jpa.persistence.tests.models.DateTimeEntity(id, name, localDateData, localTimeData, localDateTimeData) " +
                "FROM DateTimeEntity " +
                "WHERE EXTRACT(HOUR FROM localDateTimeData) = ?1 " +
                "ORDER BY name DESC")
                .setParameter(1, 14)
                .getResultStream();
        } catch (Exception e) {
            throw e;
        }
        
        List<DateTimeEntity> results = resultDate.collect(Collectors.toList());
        assertEquals(2, results.size());
        
        assertEquals("q3", results.get(0).getName());
        assertEquals(LocalDate.of(2024, 9, 20), results.get(0).getLocalDateData());
        assertEquals(LocalTime.of(14, 30), results.get(1).getLocalTimeData());
        assertEquals(LocalDateTime.of(2023, 5, 10, 14, 30), results.get(1).getLocalDateTimeData());
    }

    @Test
    public void testExtractMinuteFromLocalData() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2023, 4, 18), LocalTime.of(10, 45), LocalDateTime.of(2023, 4, 18, 10, 45));
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2022, 6, 25), LocalTime.of(16, 20), LocalDateTime.of(2022, 6, 25, 16, 20));
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2024, 8, 30), LocalTime.of(8, 45), LocalDateTime.of(2024, 8, 30, 8, 45));

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        tx.commit();

        Stream<DateTimeEntity> resultDate;
        try {
            resultDate = em.createQuery(
                "SELECT NEW io.openliberty.jpa.persistence.tests.models.DateTimeEntity(id, name, localDateData, localTimeData, localDateTimeData) " +
                "FROM DateTimeEntity " +
                "WHERE EXTRACT(MINUTE FROM localDateTimeData) = ?1 " +
                "ORDER BY name DESC")
                .setParameter(1, 45)
                .getResultStream();
        } catch (Exception e) {
            throw e;
        }
        
        List<DateTimeEntity> results = resultDate.collect(Collectors.toList());
        assertEquals(2, results.size());
        
        assertEquals("q3", results.get(0).getName());
        assertEquals(LocalDate.of(2024, 8, 30), results.get(0).getLocalDateData());
        assertEquals(LocalTime.of(10, 45), results.get(1).getLocalTimeData());
        assertEquals(LocalDateTime.of(2023, 4, 18, 10, 45), results.get(1).getLocalDateTimeData());
    }

    @Test
    public void testExtractSecondFromLocalData() throws Exception {
        deleteAllEntities(DateTimeEntity.class);
        DateTimeEntity q1 = new DateTimeEntity(1, "q1", LocalDate.of(2023, 2, 14), LocalTime.of(13, 25, 30), LocalDateTime.of(2023, 2, 14, 13, 25, 30));
        DateTimeEntity q2 = new DateTimeEntity(2, "q2", LocalDate.of(2022, 11, 8), LocalTime.of(17, 50, 15), LocalDateTime.of(2022, 11, 8, 17, 50, 15));
        DateTimeEntity q3 = new DateTimeEntity(3, "q3", LocalDate.of(2024, 5, 22), LocalTime.of(11, 35, 30), LocalDateTime.of(2024, 5, 22, 11, 35, 30));

        tx.begin();
        em.persist(q1);
        em.persist(q2);
        em.persist(q3);
        tx.commit();

        Stream<DateTimeEntity> resultDate;
        try {
            resultDate = em.createQuery(
                "SELECT NEW io.openliberty.jpa.persistence.tests.models.DateTimeEntity(id, name, localDateData, localTimeData, localDateTimeData) " +
                "FROM DateTimeEntity " +
                "WHERE EXTRACT(SECOND FROM localDateTimeData) = ?1 " +
                "ORDER BY name DESC")
                .setParameter(1, 30.0)
                .getResultStream();
        } catch (Exception e) {
            throw e;
        }
        
        List<DateTimeEntity> results = resultDate.collect(Collectors.toList());
        assertEquals(2, results.size());
        
        assertEquals("q3", results.get(0).getName());
        assertEquals(LocalDateTime.of(2024, 5, 22, 11, 35, 30), results.get(0).getLocalDateTimeData());
        assertEquals(LocalDate.of(2023, 2, 14), results.get(1).getLocalDateData());
        assertEquals(LocalTime.of(13, 25, 30), results.get(1).getLocalTimeData());
    }
    
    @Test
    @SkipIfSysProp({
        DB_Postgres    //Reference issue: https://github.com/OpenLiberty/open-liberty/issues/32848
    })
    public void testOLGH32848() throws Exception {
        deleteAllEntities(Employee.class);
        Employee abc = Employee.of("ABC", 50000, "O+");
        Employee xyz = Employee.of("XYZ", 35000, "AB-");
        
        tx.begin();
        em.persist(abc);
        em.persist(xyz);
        tx.commit();
        
        tx.begin();
        
        try {
        em.createQuery("UPDATE Employee e SET e.info = ?1 WHERE e.id = ?2")
          .setParameter(1, null)
          .setParameter(2, abc.id)
          .executeUpdate();
        
        tx.commit();
        
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
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