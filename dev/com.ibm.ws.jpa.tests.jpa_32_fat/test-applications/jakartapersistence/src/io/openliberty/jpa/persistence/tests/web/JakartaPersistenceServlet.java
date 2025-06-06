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
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import io.openliberty.jpa.persistence.tests.models.Organization;
import io.openliberty.jpa.persistence.tests.models.Person;
import io.openliberty.jpa.persistence.tests.models.Priority;
import io.openliberty.jpa.persistence.tests.models.Product;
import io.openliberty.jpa.persistence.tests.models.Ticket;
import io.openliberty.jpa.persistence.tests.models.TicketStatus;
import io.openliberty.jpa.persistence.tests.models.User;

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
     /**
     * Testing version function added to Jakarta Persistence QL
     *
     * @throws Exception
     */
    @Test
    public void testVersion() throws Exception {
        deleteAllEntities(User.class);

        User user1 = User.of(1, "John", "Doe");
        
        tx.begin();
        em.persist(user1);
        tx.commit();
        try {
            
	        Long versionVal =  em.createQuery("SELECT version(u) FROM User u where u.lastName = ?1",Long.class)
                                .setParameter(1, "Doe")
	            				.getSingleResult();

            assertEquals(0L, versionVal);
       
        } catch (Exception e) {
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
