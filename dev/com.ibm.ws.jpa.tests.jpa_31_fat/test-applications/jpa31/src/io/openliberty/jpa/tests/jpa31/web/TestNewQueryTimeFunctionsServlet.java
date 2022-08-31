/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.jpa.tests.jpa31.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

import io.openliberty.jpa.tests.jpa31.models.QueryDateTimeEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;
import junit.framework.Assert;

@WebServlet(urlPatterns = "/TestNewQueryTimeFunctionsServlet")
public class TestNewQueryTimeFunctionsServlet extends JPATestServlet {
    private static final long serialVersionUID = 3866874746696149833L;

    private final static String PUNAME = "QueryFeatures";

    @PersistenceUnit(unitName = PUNAME + "_JTA")
    private EntityManagerFactory emfJta;

    @PersistenceUnit(unitName = PUNAME + "_RL")
    private EntityManagerFactory emfRl;

    @PersistenceContext(unitName = PUNAME + "_JTA")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    private LocalDateTime startLDT = null;
    private LocalTime startLT = null;
    private boolean startLDTBeforeNoon = false;
    private boolean skipNowTests = true;

    @PostConstruct
    private void initializeFAT() {
        startLDT = LocalDateTime.now();
        startLT = startLDT.toLocalTime();
        startLDTBeforeNoon = startLT.isBefore(LocalTime.of(12, 0));

        // Now tests are tricky, especially if tests are run close to midnight.  Just skip them if the test is being run too close to midnight.
        // We'll treat 11pm (23:00 hours) as "too close to midnight".
        if (startLDT.getHour() != 23) {
            skipNowTests = false;
        }

        try (EntityManager iem = emfRl.createEntityManager()) {
            try {
                iem.getTransaction().begin();
                iem.createNativeQuery("DELETE FROM QUERYDATETIMEENTITY").executeUpdate();
            } finally {
                iem.getTransaction().commit();
            }

            try {
                iem.getTransaction().begin();
                // QueryDateTimeEntity(int id, LocalDate localDateData, LocalTime localTimeData, LocalDateTime localDateTimeData)
                iem.persist(new QueryDateTimeEntity(1, LocalDate.of(2022, 06, 07), LocalTime.of(12, 0), LocalDateTime.of(2022, 06, 07, 12, 0)));
                iem.persist(new QueryDateTimeEntity(2, LocalDate.of(2020, 01, 01), LocalTime.of(00, 0), LocalDateTime.of(2020, 01, 01, 00, 0)));
                iem.persist(new QueryDateTimeEntity(3, LocalDate.of(2120, 01, 01), LocalTime.of(00, 0), LocalDateTime.of(2120, 01, 01, 00, 0))); // Far Future
                iem.persist(new QueryDateTimeEntity(10000)); // For getting a LocalTime from the database, which may not be hosted on the same system.
            } finally {
                iem.getTransaction().commit();
            }
        }
    }

    @Test
    public void testLocalDateFunction_JPQL() throws Exception {
        // Verify that the LOCAL DATE operation can be used in the WHERE clause in a comparator operation
        try {
            tx.begin();
            em.joinTransaction();

            TypedQuery<QueryDateTimeEntity> q = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE qdte.localDateData < LOCAL DATE AND qdte.id <  100",
                                                               QueryDateTimeEntity.class);
            List<QueryDateTimeEntity> resList = q.getResultList();
            Assert.assertNotNull(resList);
            Assert.assertEquals(2, resList.size());

            boolean[] foundEntities = { false, false };
            for (QueryDateTimeEntity entity : resList) {
                if (entity.getId() == 1)
                    foundEntities[0] = true;
                if (entity.getId() == 2)
                    foundEntities[1] = true;
            }

            Assert.assertTrue(foundEntities[0]);
            Assert.assertTrue(foundEntities[1]);
            tx.rollback();

        } finally {
            if (isTxActive()) {
                tx.rollback();
            }
        }

        // Verify that the LOCAL DATE operation can be used in a bulk update operation
        try (EntityManager iem = emfRl.createEntityManager()) {
            try {
                iem.getTransaction().begin();
                iem.persist(new QueryDateTimeEntity(100));
            } finally {
                iem.getTransaction().commit();
            }
        }
        em.clear();
        try {
            tx.begin();
            em.joinTransaction();

            Query q = em.createQuery("UPDATE QueryDateTimeEntity qdte SET qdte.localDateData = LOCAL DATE WHERE qdte.id = 100");
            q.executeUpdate();
            tx.commit();

            em.clear();

            tx.begin();
            QueryDateTimeEntity findEntity = em.find(QueryDateTimeEntity.class, 100);
            Assert.assertNotNull(findEntity);
            Assert.assertNotNull(findEntity.getLocalDateData());
            tx.rollback();

        } finally {
            if (isTxActive()) {
                tx.rollback();
            }
        }
    }

    @Test
    public void testLocalTimeFunction_JPQL() throws Exception {
        em.clear();

        // Verify that the LOCAL TIME operation can be used in the WHERE clause in a comparator operation
        // This one is tricky, because the time of the test's execution affects the expected query output.
        // Also, this is dependent on the test machine and database being in the same timezone and having
        // decently synchronized clocks.  Best if the database is on the same machine as the liberty server.
        try {
            LocalTime now = getDatabaseServerLocalTime(); // LocalTime.now();
            if (now == null) {
                System.out.println("Failed to get a LocalTime from the database, falling back to LocalTime.now()");
                now = LocalTime.now();
            }
            int nowSecondOfDay = now.toSecondOfDay();
            final int twelveZeroSecondOfDay = LocalTime.of(12, 0).toSecondOfDay();
            final int secondsPerDay = 60 * 60 * 24;

            int secToTwelve = twelveZeroSecondOfDay - nowSecondOfDay;
            int secToMidnight = secondsPerDay - nowSecondOfDay;
            if ((secToTwelve > 0 && secToTwelve < 60) && (secToMidnight < 60)) {
                // If the current time is within 60 seconds of either of the time markers, then it's better to wait until current time
                // elapses past the time marker.  This is to eliminate race conditions.  30 seconds window for slow test systems.
                long waitTime = (secToTwelve > secToTwelve) ? (long) (secToTwelve + 1) * 1000 : (long) (secToMidnight + 1) * 1000;
                Thread.sleep(waitTime);

                // After sleeping, recalculate now.
                now = getDatabaseServerLocalTime(); // LocalTime.now();
                if (now == null) {
                    System.out.println("Failed to get a recalculated LocalTime from the database, falling back to LocalTime.now()");
                    now = LocalTime.now();
                }
            }

            tx.begin();
            em.joinTransaction();

            System.out.println("Executing query ...");
            TypedQuery<QueryDateTimeEntity> q = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE qdte.localTimeData < LOCAL TIME AND qdte.id <  100",
                                                               QueryDateTimeEntity.class);
            List<QueryDateTimeEntity> resList = q.getResultList();
            Assert.assertNotNull(resList);

            System.out.println("Examining results: " + resList);
            boolean[] foundList = { false, false, false };
            boolean[] expectedList = { false, true, true };
            if (now.isAfter(LocalTime.of(12, 0))) {
                expectedList[0] = true;
            }

            System.out.println("Expected Results: " + expectedList[0] + ", " + expectedList[1] + ", " + expectedList[2]);

            for (QueryDateTimeEntity o : resList) {
                if (o.getId() == 1)
                    foundList[0] = true;
                if (o.getId() == 2)
                    foundList[1] = true;
                if (o.getId() == 3)
                    foundList[2] = true;
            }

            Assert.assertEquals(expectedList[0], foundList[0]);
            Assert.assertEquals(expectedList[1], foundList[1]);
            Assert.assertEquals(expectedList[2], foundList[2]);

            tx.rollback();

        } finally {
            if (isTxActive()) {
                tx.rollback();
            }
        }

        // Verify that the LOCAL DATE operation can be used in a bulk update operation
        try (EntityManager iem = emfRl.createEntityManager()) {
            try {
                iem.getTransaction().begin();
                iem.persist(new QueryDateTimeEntity(200));
            } finally {
                iem.getTransaction().commit();
            }
        }
        try {
            tx.begin();
            em.joinTransaction();

            Query q = em.createQuery("UPDATE QueryDateTimeEntity qdte SET qdte.localDateData = LOCAL DATE WHERE qdte.id = 200");
            q.executeUpdate();
            tx.commit();

            em.clear();

            tx.begin();
            QueryDateTimeEntity findEntity = em.find(QueryDateTimeEntity.class, 200);
            Assert.assertNotNull(findEntity);
            Assert.assertNotNull(findEntity.getLocalDateData());
            tx.rollback();

        } finally {
            if (isTxActive()) {
                tx.rollback();
            }
        }
    }

    @Test
    public void testLocalDateTimeFunction_JPQL() throws Exception {
        em.clear();

        // Verify that the LOCAL DATETIME operation can be used in the WHERE clause in a comparator operation
        try {
            tx.begin();
            em.joinTransaction();

            Query q = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE qdte.localDateTimeData < LOCAL DATETIME AND qdte.id <  100");
            List resList = q.getResultList();
            Assert.assertNotNull(resList);
            Assert.assertEquals(2, resList.size()); // Pretty much everything preloaded is going to be in the past.
            tx.rollback();

        } finally {
            if (isTxActive()) {
                tx.rollback();
            }
        }

        // Verify that the LOCAL DATETIME operation can be used in a bulk update operation
        try (EntityManager iem = emfRl.createEntityManager()) {
            try {
                iem.getTransaction().begin();
                iem.persist(new QueryDateTimeEntity(300));
            } finally {
                iem.getTransaction().commit();
            }
        }
        try {
            tx.begin();
            em.joinTransaction();

            Query q = em.createQuery("UPDATE QueryDateTimeEntity qdte SET qdte.localDateTimeData = LOCAL DATETIME WHERE qdte.id = 300");
            q.executeUpdate();
            tx.commit();

            em.clear();

            tx.begin();
            QueryDateTimeEntity findEntity = em.find(QueryDateTimeEntity.class, 300);
            Assert.assertNotNull(findEntity);
            Assert.assertNotNull(findEntity.getLocalDateTimeData());
            tx.rollback();

        } finally {
            if (isTxActive()) {
                tx.rollback();
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testLocalDateFunction_Criteria() throws Exception {
        em.clear();

        // Verify that the localDate() operation can be used in the WHERE clause in a < comparator operation
        try {
            tx.begin();
            em.joinTransaction();

            // SELECT qdte FROM QueryDateTimeEntity qdte WHERE qdte.localDateData < LOCAL DATE AND qdte.id <  100"
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(QueryDateTimeEntity.class);
            Root<QueryDateTimeEntity> root = cq.from(QueryDateTimeEntity.class);
            cq.select(root);
            cq.where(cb.and(cb.lessThan(root.get("localDateData"), cb.localDate()), cb.lessThan(root.get("id"), 100)));
            TypedQuery<QueryDateTimeEntity> q = em.createQuery(cq);

            List<QueryDateTimeEntity> resList = q.getResultList();
            Assert.assertNotNull(resList);
            Assert.assertEquals(2, resList.size());

            boolean[] foundEntities = { false, false };
            for (QueryDateTimeEntity entity : resList) {
                System.out.println("Entity Returned = " + entity);
                if (entity.getId() == 1)
                    foundEntities[0] = true;
                if (entity.getId() == 2)
                    foundEntities[1] = true;
            }

            Assert.assertTrue(foundEntities[0]);
            Assert.assertTrue(foundEntities[1]);
            tx.rollback();

        } finally {
            if (isTxActive()) {
                tx.rollback();
            }
        }

        // Verify that the localDate() operation can be used in the WHERE clause in a > comparator operation
        try (EntityManager iem = emfRl.createEntityManager()) {
            try {
                iem.getTransaction().begin();
                iem.persist(new QueryDateTimeEntity(150));
            } finally {
                iem.getTransaction().commit();
            }
        }
        try {
            tx.begin();
            em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(QueryDateTimeEntity.class);
            Root<QueryDateTimeEntity> root = cq.from(QueryDateTimeEntity.class);
            cq.select(root);
            cq.where(cb.greaterThan(root.get("localDateData"), cb.localDate()));
            Query q = em.createQuery(cq);

            List resList = q.getResultList();
            Assert.assertNotNull(resList);
            Assert.assertTrue(resList.size() > 0);

            boolean foundID3 = false;
            for (Object o : resList) {
                QueryDateTimeEntity findEntity = (QueryDateTimeEntity) o;
                Assert.assertNotNull(findEntity);
                if (findEntity.getId() == 3) {
                    foundID3 = true;
                    break;
                }
            }
            Assert.assertTrue(foundID3);
            tx.rollback();

        } finally {
            if (isTxActive()) {
                tx.rollback();
            }
        }

        // Verify that the localDate() operation can be used in a bulk update operation
        try {
            tx.begin();
            em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaUpdate<QueryDateTimeEntity> cu = cb.createCriteriaUpdate(QueryDateTimeEntity.class);
            Root<QueryDateTimeEntity> entity = cu.from(QueryDateTimeEntity.class);
            cu.set("localDateData", cb.localDate());
            cu.where(cb.equal(entity.get("id"), 150));
            em.createQuery(cu).executeUpdate();

            tx.commit();

            em.clear();

            tx.begin();
            QueryDateTimeEntity findEntity = em.find(QueryDateTimeEntity.class, 150);
            Assert.assertNotNull(findEntity);
            Assert.assertNotNull(findEntity.getLocalDateData());
            tx.rollback();

        } finally {
            if (isTxActive()) {
                tx.rollback();
            }
        }
    }

    /*
     * The EXTRACT function takes a datetime argument and one of the following field type identifiers: YEAR, QUARTER, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND, DATE, TIME.
     * EXTRACT returns the value of the corresponding field or part of the datetime.
     */

    @Test
    public void testEXTRACTFunction_OnQueryResults_JPQL() throws Exception {
        Query q = null;
        Object result = null;

        // Verify EXTRACT(YEAR) from a LocalDate field
        q = em.createQuery("SELECT EXTRACT(YEAR FROM qdte.localDateData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(2022, result);

        // Verify EXTRACT(YEAR) from a LocalDateTime field
        q = em.createQuery("SELECT EXTRACT(YEAR FROM qdte.localDateTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(2022, result);

        // Verify EXTRACT(QUARTER) from a LocalDate field
        q = em.createQuery("SELECT EXTRACT(QUARTER FROM qdte.localDateData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result);

        // Verify EXTRACT(QUARTER) from a LocalDateTime field
        q = em.createQuery("SELECT EXTRACT(QUARTER FROM qdte.localDateTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result);

        // Verify EXTRACT(MONTH) from a LocalDate field
        q = em.createQuery("SELECT EXTRACT(MONTH FROM qdte.localDateData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(6, result);

        // Verify EXTRACT(MONTH) from a LocalDateTime field
        q = em.createQuery("SELECT EXTRACT(MONTH FROM qdte.localDateTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(6, result);

        /*
         * TODO: WEEK fails with
         *
         * java.sql.SQLSyntaxErrorException: 'WEEK' is not recognized as a function or procedure. Error Code: 30000 Call: SELECT WEEK(LOCALDATEDATA) FROM
         * QUERYDATETIMEENTITY WHERE (ID = ?) bind => [1 parameter bound] Query: ReportQuery(referenceClass=QueryDateTimeEntity sql="SELECT WEEK(LOCALDATEDATA) FROM
         * QUERYDATETIMEENTITY WHERE (ID = ?)")
         *
         * Could be a derby-specific issue, given:
         * Caused by: ERROR 42Y03: 'WEEK' is not recognized as a function or procedure. at org.apache.derby.iapi.error.StandardException.newException
         */
        if (!isDerby()) {
            // Verify EXTRACT(WEEK) from a LocalDate field
            // WEEK means the ISO-8601 week number.
            q = em.createQuery("SELECT EXTRACT(WEEK FROM qdte.localDateData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
            result = q.getSingleResult();
            Assert.assertNotNull(result);
            Assert.assertEquals(23, result);

            // Verify EXTRACT(WEEK) from a LocalDateTime field
            q = em.createQuery("SELECT EXTRACT(WEEK FROM qdte.localDateTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
            result = q.getSingleResult();
            Assert.assertNotNull(result);
            Assert.assertEquals(23, result);
        }

        // Verify EXTRACT(DAY) from a LocalDate field
        // DAY means the calendar day of the month, numbered from 1.
        q = em.createQuery("SELECT EXTRACT(DAY FROM qdte.localDateData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(7, result);

        // Verify EXTRACT(DAY) from a LocalDateTime field
        q = em.createQuery("SELECT EXTRACT(DAY FROM qdte.localDateTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(7, result);

        // Verify EXTRACT(HOUR) from a LocalTime field
        // HOUR means the hour of the day in 24-hour time, numbered from 0 to 23.
        q = em.createQuery("SELECT EXTRACT(HOUR FROM qdte.localTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(12, result);

        // Verify EXTRACT(HOUR) from a LocalDateTime field
        q = em.createQuery("SELECT EXTRACT(HOUR FROM qdte.localDateTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(12, result);

        // Verify EXTRACT(MINUTE) from a LocalTime field
        // MINUTE means the minute of the hour, numbered from 0 to 59.
        q = em.createQuery("SELECT EXTRACT(MINUTE FROM qdte.localTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result);

        // Verify EXTRACT(MINUTE) from a LocalDateTime field
        q = em.createQuery("SELECT EXTRACT(MINUTE FROM qdte.localDateTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result);

        // Verify EXTRACT(SECOND) from a LocalTime field
        // SECOND means the second of the minute, numbered from 0 to 59, including a fractional part representing fractions of a second.
        q = em.createQuery("SELECT EXTRACT(SECOND FROM qdte.localTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);

        // Verify EXTRACT(SECOND) from a LocalDateTime field
        q = em.createQuery("SELECT EXTRACT(SECOND FROM qdte.localDateTimeData) FROM QueryDateTimeEntity qdte WHERE qdte.id = 1");
        result = q.getSingleResult();
        Assert.assertNotNull(result);
        Assert.assertEquals(0.0d, result);
    }

    @Test
    public void testEXTRACTFunction_InWhereClause_JPQL() throws Exception {
        Query q = null;
        TypedQuery<QueryDateTimeEntity> tq = null;

        Object result = null;
        List resList = null;
        List<QueryDateTimeEntity> tResList = null;
        boolean found = false;

        // EXTRACT(YEAR) with LocalDate field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(YEAR FROM qdte.localDateData) = 2022", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(YEAR) with LocalDateTime field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(YEAR FROM qdte.localDateTimeData) = 2022", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(QUARTER) with LocalDate field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(QUARTER FROM qdte.localDateData) = 2", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(QUARTER) with LocalDateTime field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(QUARTER FROM qdte.localDateTimeData) = 2", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(MONTH) with LocalDate field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(MONTH FROM qdte.localDateData) = 6", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(MONTH) with LocalDateTime field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(MONTH FROM qdte.localDateTimeData) = 6", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(DAY) with LocalDate field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(DAY FROM qdte.localDateData) = 7", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(DAY) with LocalDateTime field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(DAY FROM qdte.localDateTimeData) = 7", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(HOUR) with LocalTime field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(HOUR FROM qdte.localTimeData) = 12", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(HOUR) with LocalDateTime field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(HOUR FROM qdte.localDateTimeData) = 12", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(MINUTE) with LocalTime field
        if (!this.isDB2ForLUW()) {
            // TODO: https://github.com/eclipse-ee4j/eclipselink/issues/1575
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(MINUTE FROM qdte.localTimeData) = 0", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(MINUTE) with LocalDateTime field
        if (!isDB2ForLUW())
            https: //github.com/eclipse-ee4j/eclipselink/issues/1575
            {
                tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(MINUTE FROM qdte.localDateTimeData) = 0", QueryDateTimeEntity.class);
                tResList = tq.getResultList();
                Assert.assertNotNull(tResList);
                Assert.assertTrue(tResList.size() > 0);

                found = false;
                for (QueryDateTimeEntity entity : tResList) {
                    if (entity.getId() == 1) {
                        found = true;
                        break;
                    }
                }
                Assert.assertTrue(found);
            }

        // EXTRACT(SECOND) with LocalTime field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(SECOND FROM qdte.localTimeData) = 0.0", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

        // EXTRACT(SECOND) with LocalDateTime field
        {
            tq = em.createQuery("SELECT qdte FROM QueryDateTimeEntity qdte WHERE EXTRACT(SECOND FROM qdte.localDateTimeData) = 0.0", QueryDateTimeEntity.class);
            tResList = tq.getResultList();
            Assert.assertNotNull(tResList);
            Assert.assertTrue(tResList.size() > 0);

            found = false;
            for (QueryDateTimeEntity entity : tResList) {
                if (entity.getId() == 1) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }

    }

    private boolean isTxActive() {
        try {
            int status = tx.getStatus();
            if (status == Status.STATUS_NO_TRANSACTION || status == Status.STATUS_UNKNOWN) {
                return false;
            }
            return true;
        } catch (SystemException e) {
            return false;
        }

    }

    private LocalTime getDatabaseServerLocalTime() {
        LocalTime lt = null;

        final EntityManager dtem = emfRl.createEntityManager();
        try (dtem) {
            System.out.println("Getting Local Time from database...");
            dtem.getTransaction().begin();
            dtem.createQuery("UPDATE QueryDateTimeEntity qdte SET qdte.localTimeData = LOCAL TIME WHERE qdte.id = 10000").executeUpdate();
            dtem.flush();
            lt = dtem.createQuery("SELECT qdte.localTimeData FROM QueryDateTimeEntity qdte WHERE qdte.id = 10000", LocalTime.class).getSingleResult();
            dtem.getTransaction().rollback();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {

        }

        System.out.println("Returning Local Time from database = " + lt);
        return lt;
    }

}
