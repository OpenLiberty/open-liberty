/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
package test.jakarta.data.jpa.web.eclipselink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.jakarta.data.jpa.web.PurchaseTime;

/**
 * For tests that only run on the built-in Persistence provider, which is
 * EclipseLink. The built-in provider is needed for record entities and
 * any repositories that specify a DataSource as their dataStore.
 */
@DataSourceDefinition(name = "java:module/jdbc/EclipseLinkDataStore",
                      className = "${repository.datasource.class.name}",
                      databaseName = "${repository.database.name}",
                      user = "${repository.database.user}",
                      password = "${repository.database.password}",
                      properties = {
                                     "createDatabase=create"
                      })
@Resource(name = "java:app/env/data/DataStoreRef",
          lookup = "java:comp/DefaultDataSource")
@SuppressWarnings("serial")
@WebServlet("/DataJPAEclipseLinkServlet")
public class DataJPAEclipseLinkServlet extends FATServlet {

    @Inject
    Rebates rebates;

    /**
     * Prepopulate read only data, if any, that is needed by the tests here:
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
    }

    /**
     * Verify that JPQL can be used to EXTRACT the DATE from a LocalDateTime.
     */
    @Test
    public void testExtractDate() {
        rebates.reset();

        Rebate r1 = new Rebate(54001, //
                        1.40f, //
                        "TestExtractDate-1", //
                        LocalTime.now(), //
                        LocalDate.of(2025, Month.MAY, 21), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2025, Month.JUNE, 11, 13, 06, 00), //
                        1);

        Rebate r2 = new Rebate(54002, //
                        2.30f, //
                        "TestExtractDate-2", //
                        LocalTime.now(), //
                        LocalDate.of(2025, Month.MAY, 14), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2025, Month.JUNE, 12, 12, 30, 00), //
                        1);

        Rebate r3 = new Rebate(54003, //
                        3.20f, //
                        "TestExtractDate-3", //
                        LocalTime.now(), //
                        LocalDate.of(2025, Month.MAY, 15), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2025, Month.JUNE, 11, 8, 45, 00), //
                        1);

        rebates.addAll(r1, r2, r3);

        assertEquals(List.of("TestExtractDate-3", "TestExtractDate-1"),
                     rebates.updatedOn(LocalDate.of(2025, Month.JUNE, 11))
                                     .map(Rebate::customerId)
                                     .collect(Collectors.toList()));

        rebates.reset();
    }

    /**
     * Verify that JPQL can be used to EXTRACT the TIME from a LocalDateTime.
     */
    @Test
    public void testExtractTime() {
        rebates.reset();

        Rebate r1 = new Rebate(520001, //
                        10.00f, //
                        "TestExtractTime-1", //
                        LocalTime.now(), //
                        LocalDate.of(2025, Month.MAY, 10), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2025, Month.JUNE, 6, 11, 34, 30), //
                        3);

        Rebate r2 = new Rebate(520002, //
                        2.50f, //
                        "TestExtractTime-2", //
                        LocalTime.now(), //
                        LocalDate.of(2025, Month.MAY, 12), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2025, Month.JUNE, 6, 12, 38, 00), //
                        1);

        Rebate r3 = new Rebate(520003, //
                        3.75f, //
                        "TestExtractTime-3", //
                        LocalTime.now(), //
                        LocalDate.of(2025, Month.MAY, 14), //
                        Rebate.Status.DENIED, //
                        LocalDateTime.of(2025, Month.JUNE, 7, 9, 55, 20), //
                        2);

        rebates.addAll(r1, r2, r3);

        assertEquals(List.of(LocalTime.of(12, 38, 00),
                             LocalTime.of(9, 55, 20),
                             LocalTime.of(11, 34, 30)),
                     rebates.timeUpdated());

        rebates.reset();
    }

    /**
     * Verify that JPQL SELECT and ORDER BY clauses can EXTRACT the YEAR from a
     * LocalDateTime.
     */
    @Test
    public void testExtractYearInSelectAndOrderBy() {
        rebates.reset();

        Rebate r1 = new Rebate(525001, //
                        10.00f, //
                        "testExtractYearInSelectAndOrderBy-1", //
                        LocalTime.now(), //
                        LocalDate.of(2024, Month.APRIL, 5), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2024, Month.JULY, 10, 9, 15, 00), //
                        3);

        Rebate r2 = new Rebate(525002, //
                        2.50f, //
                        "TestExtractYearInSelectAndOrderBy-2", //
                        LocalTime.now(), //
                        LocalDate.of(2022, Month.SEPTEMBER, 22), //
                        Rebate.Status.DENIED, //
                        LocalDateTime.of(2022, Month.OCTOBER, 5, 15, 01, 00), //
                        4);

        Rebate r3 = new Rebate(525003, //
                        3.75f, //
                        "TestExtractYearInSelectAndOrderBy-3", //
                        LocalTime.now(), //
                        LocalDate.of(2025, Month.MAY, 18), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2025, Month.JUNE, 11, 14, 59, 53), //
                        2);

        rebates.addAll(r1, r2, r3);

        assertEquals(List.of(2025,
                             2024,
                             2022),
                     rebates.yearUpdated());

        rebates.reset();
    }

    /**
     * Use repository methods with JDQL that specifies LOCAL DATE, LOCAL DATETIME,
     * and LOCAL TIME.
     */
    @Test
    public void testLocalDateAndTimeFunctions() {

        Rebate r1 = new Rebate(21, 1.01, "testLocalDateAndTimeFunctions-CustomerA", //
                        LocalTime.of(10, 51, 0), //
                        LocalDate.of(2024, Month.JULY, 19), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2024, Month.JULY, 19, 13, 10, 0), //
                        null);

        Rebate r2 = new Rebate(22, 2.02, "testLocalDateAndTimeFunctions-CustomerB", //
                        LocalTime.of(14, 28, 52), //
                        LocalDate.of(2024, Month.JULY, 18), //
                        Rebate.Status.VERIFIED, //
                        LocalDateTime.of(2024, Month.JULY, 20, 8, 2, 59), //
                        null);

        Rebate r3 = new Rebate(23, 1.23, "testLocalDateAndTimeFunctions-CustomerB", //
                        LocalTime.of(16, 33, 53), //
                        LocalDate.of(2024, Month.JUNE, 30), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2024, Month.JULY, 20, 13, 3, 31), //
                        null);

        Rebate r4 = new Rebate(24, 1.44, "testLocalDateAndTimeFunctions-CustomerA", //
                        LocalTime.of(16, 4, 44), //
                        LocalDate.of(2024, Month.JULY, 13), //
                        Rebate.Status.VERIFIED, //
                        LocalDateTime.of(2024, Month.JULY, 16, 18, 42, 0), //
                        null);

        Rebate[] all = rebates.addAll(r1, r2, r3, r4);

        assertEquals(List.of(r2.id(), r4.id(), r3.id(), r1.id()),
                     rebates.notRecentlyUpdated("testLocalDateAndTimeFunctions-%"));

        assertEquals(List.of(r4.id(), r1.id(), r2.id(), r3.id()),
                     rebates.purchasedInThePast("testLocalDateAndTimeFunctions-%"));

        LocalDateTime lastUpdate = rebates.lastUpdated(r3.id()).orElseThrow();
        assertEquals(2024, lastUpdate.getYear());
        assertEquals(Month.JULY, lastUpdate.getMonth());
        assertEquals(20, lastUpdate.getDayOfMonth());
        assertEquals(13, lastUpdate.getHour());
        assertEquals(3, lastUpdate.getMinute());
        assertEquals(31, lastUpdate.getSecond());

        LocalDate dayOfPurchase = (LocalDate) rebates.dayOfPurchase(r2.id())
                        .orElseThrow();
        assertEquals(2024, dayOfPurchase.getYear());
        assertEquals(Month.JULY, dayOfPurchase.getMonth());
        assertEquals(18, dayOfPurchase.getDayOfMonth());

        rebates.removeAll(all);
    }

    /**
     * Use a repository method that runs a query without specifying an entity type
     * and returns a record entity. The repository must be able to infer the record type
     * to use from the return value and generate the proper select clause so that the
     * generated entity type is converted to the record type.
     */
    @Test
    public void testRecordQueryInfersSelectClause() {

        Rebate r1 = new Rebate(10, 10.00, "testRecordEntityInferredFromReturnType-CustomerA", //
                        LocalTime.of(15, 40, 0), //
                        LocalDate.of(2024, Month.MAY, 1), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2024, Month.MAY, 1, 15, 40, 0), //
                        null);

        Rebate r2 = new Rebate(12, 12.00, "testRecordEntityInferredFromReturnType-CustomerA", //
                        LocalTime.of(12, 46, 30), //
                        LocalDate.of(2024, Month.APRIL, 5), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2024, Month.MAY, 2, 10, 18, 0), //
                        null);

        Rebate r3 = new Rebate(13, 3.00, "testRecordEntityInferredFromReturnType-CustomerB", //
                        LocalTime.of(9, 15, 0), //
                        LocalDate.of(2024, Month.MAY, 2), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2024, Month.MAY, 2, 9, 15, 0), //
                        null);

        Rebate r4 = new Rebate(14, 4.00, "testRecordEntityInferredFromReturnType-CustomerA", //
                        LocalTime.of(10, 55, 0), //
                        LocalDate.of(2024, Month.MAY, 1), //
                        Rebate.Status.VERIFIED, //
                        LocalDateTime.of(2024, Month.MAY, 2, 14, 27, 45), //
                        null);

        Rebate r5 = new Rebate(15, 5.00, "testRecordEntityInferredFromReturnType-CustomerA", //
                        LocalTime.of(17, 50, 0), //
                        LocalDate.of(2024, Month.MAY, 1), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2024, Month.MAY, 5, 15, 5, 0), //
                        null);

        Rebate[] all = rebates.addAll(r1, r2, r3, r4, r5);

        List<Rebate> paid = rebates.paidTo("testRecordEntityInferredFromReturnType-CustomerA");

        assertEquals(paid.toString(), 3, paid.size());
        Rebate r;
        r = paid.get(0);
        assertEquals(12.0f, r.amount(), 0.001);
        r = paid.get(1);
        assertEquals(10.0f, r.amount(), 0.001);
        r = paid.get(2);
        assertEquals(5.0f, r.amount(), 0.001);

        List<Double> amounts = rebates.amounts("testRecordEntityInferredFromReturnType-CustomerA");

        assertEquals(4.0f, amounts.get(0), 0.001);
        assertEquals(5.0f, amounts.get(1), 0.001);
        assertEquals(10.0f, amounts.get(2), 0.001);
        assertEquals(12.0f, amounts.get(3), 0.001);

        assertEquals(Rebate.Status.VERIFIED, rebates.status(all[4 - 1].id()).orElseThrow());
        assertEquals(Rebate.Status.PAID, rebates.status(all[3 - 1].id()).orElseThrow());

        List<LocalDate> purchaseDates = rebates.findByCustomerIdOrderByPurchaseMadeOnDesc("testRecordEntityInferredFromReturnType-CustomerA");

        assertEquals(LocalDate.of(2024, Month.MAY, 1), purchaseDates.get(0));
        assertEquals(LocalDate.of(2024, Month.MAY, 1), purchaseDates.get(1));
        assertEquals(LocalDate.of(2024, Month.MAY, 1), purchaseDates.get(2));
        assertEquals(LocalDate.of(2024, Month.APRIL, 5), purchaseDates.get(3));

        PurchaseTime time = rebates.purchaseTime(all[3 - 1].id()).orElseThrow();
        assertEquals(LocalDate.of(2024, Month.MAY, 2), time.purchaseMadeOn());
        assertEquals(LocalTime.of(9, 15, 0), time.purchaseMadeAt());

        PurchaseTime[] times = rebates.findTimeOfPurchaseByCustomerId("testRecordEntityInferredFromReturnType-CustomerA");
        assertEquals(Arrays.toString(times), 4, times.length);

        assertEquals(LocalDate.of(2024, Month.APRIL, 5), times[0].purchaseMadeOn());
        assertEquals(LocalTime.of(12, 46, 30), times[0].purchaseMadeAt());

        assertEquals(LocalDate.of(2024, Month.MAY, 1), times[1].purchaseMadeOn());
        assertEquals(LocalTime.of(10, 55, 0), times[1].purchaseMadeAt());

        assertEquals(LocalDate.of(2024, Month.MAY, 1), times[2].purchaseMadeOn());
        assertEquals(LocalTime.of(15, 40, 0), times[2].purchaseMadeAt());

        assertEquals(LocalDate.of(2024, Month.MAY, 1), times[3].purchaseMadeOn());
        assertEquals(LocalTime.of(17, 50, 0), times[3].purchaseMadeAt());

        rebates.removeAll(all);

        assertEquals(false, rebates.status(all[3 - 1].id()).isPresent());
    }

    /**
     * Tests lifecycle methods returning a single record.
     */
    @Test
    public void testRecordReturnedByLifecycleMethods() {
        // Insert
        Rebate r1 = new Rebate(1, 1.00, "TestRecordReturned-Customer1", //
                        LocalTime.of(11, 31, 0), //
                        LocalDate.of(2023, Month.OCTOBER, 16), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 16, 11, 32, 0), //
                        null);
        r1 = rebates.add(r1);
        assertEquals(Integer.valueOf(1), r1.id());
        assertEquals(1.00, r1.amount(), 0.001f);
        assertEquals(LocalTime.of(11, 31, 0), r1.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 16), r1.purchaseMadeOn());
        assertEquals(Rebate.Status.SUBMITTED, r1.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 16, 11, 32, 0), r1.updatedAt());
        Integer initialVersion = r1.version();
        assertNotNull(initialVersion);

        // Update
        r1 = new Rebate(r1.id(), r1.amount(), r1.customerId(), //
                        r1.purchaseMadeAt(), //
                        r1.purchaseMadeOn(), //
                        Rebate.Status.VERIFIED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 16, 11, 41, 0), //
                        r1.version());
        r1 = rebates.modify(r1);
        assertEquals(Integer.valueOf(1), r1.id());
        assertEquals(1.00, r1.amount(), 0.001f);
        assertEquals(LocalTime.of(11, 31, 0), r1.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 16), r1.purchaseMadeOn());
        assertEquals(Rebate.Status.VERIFIED, r1.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 16, 11, 41, 0), r1.updatedAt());
        assertEquals(Integer.valueOf(initialVersion + 1), r1.version());

        // Save
        r1 = new Rebate(r1.id(), r1.amount(), r1.customerId(), //
                        r1.purchaseMadeAt(), //
                        r1.purchaseMadeOn(), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2023, Month.OCTOBER, 16, 11, 44, 0), //
                        r1.version());
        r1 = rebates.process(r1);
        assertEquals(Integer.valueOf(1), r1.id());
        assertEquals(1.00, r1.amount(), 0.001f);
        assertEquals(LocalTime.of(11, 31, 0), r1.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 16), r1.purchaseMadeOn());
        assertEquals(Rebate.Status.PAID, r1.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 16, 11, 44, 0), r1.updatedAt());
        assertEquals(Integer.valueOf(initialVersion + 2), r1.version());

        // Delete
        rebates.remove(r1);
    }

    /**
     * Tests lifecycle methods returning multiple records as an array.
     */
    @Test
    public void testRecordsArrayReturnedByLifecycleMethods() {
        // Insert
        Rebate r2 = new Rebate(2, 2.00, "TestRecordsArrayReturned-Customer2", //
                        LocalTime.of(8, 22, 0), //
                        LocalDate.of(2023, Month.OCTOBER, 12), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 12, 8, 22, 0), //
                        null);

        Rebate r3 = new Rebate(3, 3.00, "TestRecordsArrayReturned-Customer3", //
                        LocalTime.of(9, 33, 0), //
                        LocalDate.of(2023, Month.OCTOBER, 13), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 13, 9, 33, 0), //
                        null);

        Rebate r4 = new Rebate(4, 4.00, "TestRecordsArrayReturned-Customer4", //
                        LocalTime.of(7, 44, 0), //
                        LocalDate.of(2023, Month.OCTOBER, 14), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 14, 7, 44, 0), //
                        null);

        // r5 is intentionally not inserted into the database yet so that we can test non-matching
        Rebate r5 = new Rebate(5, 5.00, "TestRecordsArrayReturned-Customer5", //
                        LocalTime.of(6, 55, 0), //
                        LocalDate.of(2023, Month.OCTOBER, 15), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 15, 6, 55, 0), //
                        null);

        Rebate[] r = rebates.addAll(r4, r3, r2);
        assertEquals(3, r.length);
        r2 = r[2];
        r3 = r[1];
        r4 = r[0];

        assertEquals(Integer.valueOf(2), r2.id());
        assertEquals(2.00, r2.amount(), 0.001f);
        assertEquals("TestRecordsArrayReturned-Customer2", r2.customerId());
        assertEquals(LocalTime.of(8, 22, 0), r2.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 12), r2.purchaseMadeOn());
        assertEquals(Rebate.Status.SUBMITTED, r2.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 12, 8, 22, 0), r2.updatedAt());
        Integer r2_initialVersion = r2.version();
        assertNotNull(r2_initialVersion);

        assertEquals(Integer.valueOf(3), r3.id());
        assertEquals("TestRecordsArrayReturned-Customer3", r3.customerId());
        assertEquals(3.00, r3.amount(), 0.001f);
        assertEquals(LocalTime.of(9, 33, 0), r3.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 13), r3.purchaseMadeOn());
        assertEquals(Rebate.Status.SUBMITTED, r3.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 13, 9, 33, 0), r3.updatedAt());
        Integer r3_initialVersion = r3.version();
        assertNotNull(r3_initialVersion);

        assertEquals(Integer.valueOf(4), r4.id());
        assertEquals("TestRecordsArrayReturned-Customer4", r4.customerId());
        assertEquals(4.00, r4.amount(), 0.001f);
        assertEquals(LocalTime.of(7, 44, 0), r4.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 14), r4.purchaseMadeOn());
        assertEquals(Rebate.Status.SUBMITTED, r4.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 14, 7, 44, 0), r4.updatedAt());
        Integer r4_initialVersion = r4.version();
        assertNotNull(r4_initialVersion);

        // Update
        r2 = new Rebate(r2.id(), r2.amount(), r2.customerId(), //
                        r2.purchaseMadeAt(), //
                        r2.purchaseMadeOn(), //
                        Rebate.Status.VERIFIED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 17, 8, 45, 0), //
                        r2.version());

        r4 = new Rebate(r4.id(), r4.amount(), r4.customerId(), //
                        r4.purchaseMadeAt(), //
                        r4.purchaseMadeOn(), //
                        Rebate.Status.DENIED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 17, 8, 47, 0), //
                        r4.version());

        try {
            r = rebates.modifyAll(r2, r5, r4);
            fail("An attempt to update multiple entities where one does not exist in the database " +
                 "must raise OptimisticLockingFailureException. Instead: " + Arrays.toString(r));
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        r = rebates.modifyAll(r2, r4);

        assertEquals(2, r.length);
        Rebate r4_old = r4;
        r2 = r[0];
        r4 = r[1];

        assertEquals(Integer.valueOf(2), r2.id());
        assertEquals("TestRecordsArrayReturned-Customer2", r2.customerId());
        assertEquals(2.00, r2.amount(), 0.001f);
        assertEquals(LocalTime.of(8, 22, 0), r2.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 12), r2.purchaseMadeOn());
        assertEquals(Rebate.Status.VERIFIED, r2.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 17, 8, 45, 0), r2.updatedAt());
        assertEquals(Integer.valueOf(r2_initialVersion + 1), r2.version());

        assertEquals(Integer.valueOf(4), r4.id());
        assertEquals("TestRecordsArrayReturned-Customer4", r4.customerId());
        assertEquals(4.00, r4.amount(), 0.001f);
        assertEquals(LocalTime.of(7, 44, 0), r4.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 14), r4.purchaseMadeOn());
        assertEquals(Rebate.Status.DENIED, r4.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 17, 8, 47, 0), r4.updatedAt());
        assertEquals(Integer.valueOf(r4_initialVersion + 1), r4.version());

        // Save

        r2 = new Rebate(r2.id(), r2.amount(), r2.customerId(), //
                        r2.purchaseMadeAt(), //
                        r2.purchaseMadeOn(), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2023, Month.OCTOBER, 22, 10, 28, 0), //
                        r2.version()); // valid update

        r3 = new Rebate(r3.id(), r3.amount(), r3.customerId(), //
                        r3.purchaseMadeAt(), //
                        r3.purchaseMadeOn(), //
                        Rebate.Status.VERIFIED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 22, 10, 36, 0), //
                        r3.version()); // valid update

        r = rebates.processAll(r5, r3, r2); // new, update, update

        assertEquals(3, r.length);
        r5 = r[0];
        r3 = r[1];
        r2 = r[2];

        assertEquals(Integer.valueOf(2), r2.id());
        assertEquals("TestRecordsArrayReturned-Customer2", r2.customerId());
        assertEquals(2.00, r2.amount(), 0.001f);
        assertEquals(LocalTime.of(8, 22, 0), r2.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 12), r2.purchaseMadeOn());
        assertEquals(Rebate.Status.PAID, r2.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 22, 10, 28, 0), r2.updatedAt());
        assertEquals(Integer.valueOf(r2_initialVersion + 2), r2.version());

        assertEquals(Integer.valueOf(3), r3.id());
        assertEquals("TestRecordsArrayReturned-Customer3", r3.customerId());
        assertEquals(3.00, r3.amount(), 0.001f);
        assertEquals(LocalTime.of(9, 33, 0), r3.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 13), r3.purchaseMadeOn());
        assertEquals(Rebate.Status.VERIFIED, r3.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 22, 10, 36, 0), r3.updatedAt());
        assertEquals(Integer.valueOf(r3_initialVersion + 1), r3.version());

        assertEquals(Integer.valueOf(5), r5.id());
        assertEquals("TestRecordsArrayReturned-Customer5", r5.customerId());
        assertEquals(5.00, r5.amount(), 0.001f);
        assertEquals(LocalTime.of(6, 55, 0), r5.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 15), r5.purchaseMadeOn());
        assertEquals(Rebate.Status.SUBMITTED, r5.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 15, 6, 55, 0), r5.updatedAt());
        assertNotNull(r5.version());

        Rebate r4_nonMatching = new Rebate(r4_old.id(), r4_old.amount(), r4_old.customerId(), //
                        r4_old.purchaseMadeAt(), //
                        r4_old.purchaseMadeOn(), //
                        Rebate.Status.VERIFIED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 22, 10, 49, 0), //
                        r4_old.version()); // invalid update due to old version

        try {
            r = rebates.processAll(r4_nonMatching);
            fail("Did not raise OptimisticLockingFailureException when saving a record with an old version. Instead: " +
                 Arrays.toString(r));
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        // Delete
        try {
            rebates.removeAll(r3, r4_old, r2);
            fail("Attempt to delete multiple where one has an outdated version must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        rebates.removeAll(r2, r3, r4, r5);

        try {
            rebates.removeAll(r2, r5);
            fail("Attempt to delete multiple where at least one is not found must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }
    }

    /**
     * Tests lifecycle methods returning multiple records as various types of Iterable.
     */
    @Test
    public void testRecordsIterableReturnedByLifecycleMethods() {
        // Insert
        Rebate r6 = new Rebate(6, 6.00, "TestRecordsIterableReturned-Customer6", //
                        LocalTime.of(6, 36, 0), //
                        LocalDate.of(2023, Month.OCTOBER, 16), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 16, 6, 36, 0), //
                        null);

        Rebate r7 = new Rebate(7, 7.00, "TestRecordsIterableReturned-Customer7", //
                        LocalTime.of(7, 37, 0), //
                        LocalDate.of(2023, Month.OCTOBER, 17), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 17, 7, 37, 0), //
                        null);

        Rebate r8 = new Rebate(8, 8.00, "TestRecordsIterableReturned-Customer8", //
                        LocalTime.of(8, 38, 0), //
                        LocalDate.of(2023, Month.OCTOBER, 18), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 18, 8, 38, 0), //
                        null);

        // r9 is intentionally not inserted into the database yet so that we can test non-matching
        Rebate r9 = new Rebate(9, 9.00, "TestRecordsIterableReturned-Customer9", //
                        LocalTime.of(9, 39, 0), //
                        LocalDate.of(2023, Month.OCTOBER, 19), //
                        Rebate.Status.SUBMITTED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 19, 9, 39, 0), //
                        null);

        Iterator<Rebate> it = rebates.addMultiple(List.of(r6, r7, r8)).iterator();

        assertEquals(true, it.hasNext());
        r6 = it.next();
        assertEquals(Integer.valueOf(6), r6.id());
        assertEquals(6.00, r6.amount(), 0.001f);
        assertEquals("TestRecordsIterableReturned-Customer6", r6.customerId());
        assertEquals(LocalTime.of(6, 36, 0), r6.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 16), r6.purchaseMadeOn());
        assertEquals(Rebate.Status.SUBMITTED, r6.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 16, 6, 36, 0), r6.updatedAt());
        Integer r6_initialVersion = r6.version();
        assertNotNull(r6_initialVersion);

        assertEquals(true, it.hasNext());
        r7 = it.next();
        assertEquals(Integer.valueOf(7), r7.id());
        assertEquals("TestRecordsIterableReturned-Customer7", r7.customerId());
        assertEquals(7.00, r7.amount(), 0.001f);
        assertEquals(LocalTime.of(7, 37, 0), r7.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 17), r7.purchaseMadeOn());
        assertEquals(Rebate.Status.SUBMITTED, r7.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 17, 7, 37, 0), r7.updatedAt());
        Integer r7_initialVersion = r7.version();
        assertNotNull(r7_initialVersion);

        assertEquals(true, it.hasNext());
        r8 = it.next();
        assertEquals(Integer.valueOf(8), r8.id());
        assertEquals("TestRecordsIterableReturned-Customer8", r8.customerId());
        assertEquals(8.00, r8.amount(), 0.001f);
        assertEquals(LocalTime.of(8, 38, 0), r8.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 18), r8.purchaseMadeOn());
        assertEquals(Rebate.Status.SUBMITTED, r8.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 18, 8, 38, 0), r8.updatedAt());
        Integer r8_initialVersion = r8.version();
        assertNotNull(r8_initialVersion);

        assertEquals(false, it.hasNext());

        // Save
        r6 = new Rebate(r6.id(), r6.amount(), r6.customerId(), //
                        r6.purchaseMadeAt(), //
                        r6.purchaseMadeOn(), //
                        Rebate.Status.VERIFIED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 26, 6, 46, 0), //
                        r6.version());

        r8 = new Rebate(r8.id(), r8.amount(), r8.customerId(), //
                        r8.purchaseMadeAt(), //
                        r8.purchaseMadeOn(), //
                        Rebate.Status.DENIED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 28, 8, 48, 0), //
                        r8.version());

        Collection<Rebate> collection = rebates.processMultiple(List.of(r6, r8, r9)); // update, update, new
        it = collection.iterator();

        assertEquals(true, it.hasNext());
        r6 = it.next();
        assertEquals(Integer.valueOf(6), r6.id());
        assertEquals("TestRecordsIterableReturned-Customer6", r6.customerId());
        assertEquals(6.00, r6.amount(), 0.001f);
        assertEquals(LocalTime.of(6, 36, 0), r6.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 16), r6.purchaseMadeOn());
        assertEquals(Rebate.Status.VERIFIED, r6.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 26, 6, 46, 0), r6.updatedAt());
        assertEquals(Integer.valueOf(r6_initialVersion + 1), r6.version());

        assertEquals(true, it.hasNext());
        Rebate r8_old = r8;
        r8 = it.next();
        assertEquals(Integer.valueOf(8), r8.id());
        assertEquals("TestRecordsIterableReturned-Customer8", r8.customerId());
        assertEquals(8.00, r8.amount(), 0.001f);
        assertEquals(LocalTime.of(8, 38, 0), r8.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 18), r8.purchaseMadeOn());
        assertEquals(Rebate.Status.DENIED, r8.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 28, 8, 48, 0), r8.updatedAt());
        assertEquals(Integer.valueOf(r8_initialVersion + 1), r8.version());

        assertEquals(true, it.hasNext());
        r9 = it.next();
        assertEquals(Integer.valueOf(9), r9.id());
        assertEquals("TestRecordsIterableReturned-Customer9", r9.customerId());
        assertEquals(9.00, r9.amount(), 0.001f);
        assertEquals(LocalTime.of(9, 39, 0), r9.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 19), r9.purchaseMadeOn());
        assertEquals(Rebate.Status.SUBMITTED, r9.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 19, 9, 39, 0), r9.updatedAt());
        assertNotNull(r9.version());

        assertEquals(false, it.hasNext());

        // Update

        r6 = new Rebate(r6.id(), r6.amount(), r6.customerId(), //
                        r6.purchaseMadeAt(), //
                        r6.purchaseMadeOn(), //
                        Rebate.Status.PAID, //
                        LocalDateTime.of(2023, Month.OCTOBER, 30, 12, 56, 0), //
                        r6.version()); // valid update

        r7 = new Rebate(r7.id(), r7.amount(), r7.customerId(), //
                        r7.purchaseMadeAt(), //
                        r7.purchaseMadeOn(), //
                        Rebate.Status.VERIFIED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 30, 12, 57, 0), //
                        r7.version()); // valid update

        Rebate r8_nonMatching = new Rebate(r8_old.id(), r8_old.amount(), r8_old.customerId(), //
                        r8_old.purchaseMadeAt(), //
                        r8_old.purchaseMadeOn(), //
                        Rebate.Status.VERIFIED, //
                        LocalDateTime.of(2023, Month.OCTOBER, 30, 12, 58, 0), //
                        r8_old.version()); // invalid update due to old version

        try {
            List<Rebate> list = rebates.modifyMultiple(List.of(r7, r8_nonMatching, r6));
            fail("An attempt to update multiple entities where one does not match the version in the database " +
                 "must raise OptimisticLockingFailureException. Instead: " + list);
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        List<Rebate> list = rebates.modifyMultiple(List.of(r7, r6));

        assertEquals(2, list.size());
        r7 = list.get(0);
        r6 = list.get(1);

        assertEquals(Integer.valueOf(7), r7.id());
        assertEquals("TestRecordsIterableReturned-Customer7", r7.customerId());
        assertEquals(7.00, r7.amount(), 0.001f);
        assertEquals(LocalTime.of(7, 37, 0), r7.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 17), r7.purchaseMadeOn());
        assertEquals(Rebate.Status.VERIFIED, r7.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 30, 12, 57, 0), r7.updatedAt());
        assertEquals(Integer.valueOf(r7_initialVersion + 1), r7.version());

        assertEquals(Integer.valueOf(6), r6.id());
        assertEquals("TestRecordsIterableReturned-Customer6", r6.customerId());
        assertEquals(6.00, r6.amount(), 0.001f);
        assertEquals(LocalTime.of(6, 36, 0), r6.purchaseMadeAt());
        assertEquals(LocalDate.of(2023, Month.OCTOBER, 16), r6.purchaseMadeOn());
        assertEquals(Rebate.Status.PAID, r6.status());
        assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 30, 12, 56, 0), r6.updatedAt());
        assertEquals(Integer.valueOf(r6_initialVersion + 2), r6.version());

        // Delete
        try {
            rebates.removeMultiple(new ArrayList<>(List.of(r9, r8_old, r7, r6)));
            fail("Attempt to delete multiple where one has an outdated version must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        rebates.removeMultiple(new ArrayList<>(List.of(r6, r9, r7, r8)));

        try {
            rebates.removeMultiple(new ArrayList<>(List.of(r9, r7)));
            fail("Attempt to delete multiple where at leaset one is not found must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }
    }

}
