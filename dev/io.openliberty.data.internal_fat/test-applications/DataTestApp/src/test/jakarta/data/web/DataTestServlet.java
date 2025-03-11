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
package test.jakarta.data.web;

import static componenttest.annotation.SkipIfSysProp.DB_Oracle;
import static componenttest.annotation.SkipIfSysProp.DB_Postgres;
import static componenttest.annotation.SkipIfSysProp.DB_SQLServer;
import static jakarta.data.repository.By.ID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static test.jakarta.data.web.Assertions.assertIterableEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
import jakarta.data.repository.By;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionRequiredException;
import jakarta.transaction.TransactionalException;
import jakarta.transaction.UserTransaction;

import org.junit.Ignore;
import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;
import test.jakarta.data.web.Animal.ScientificName;
import test.jakarta.data.web.Residence.Occupant;

@DataSourceDefinition(name = "java:app/jdbc/DerbyDataSource",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      properties = "createDatabase=create")
@Resource(name = "java:module/jdbc/env/DerbyDataSourceRef", lookup = "java:app/jdbc/DerbyDataSource")
@SuppressWarnings("serial")
@WebServlet("/*")
public class DataTestServlet extends FATServlet {
    private final long TIMEOUT_MINUTES = 2;

    @Inject
    Animals animals;

    @Inject
    Apartments apartments;

    @Inject
    Cylinders cylinders;

    @Inject
    EmptyRepository emptyRepo;

    @Inject
    Houses houses;

    @Inject
    MultiRepository multi;

    @Inject
    Packages packages;

    @Inject
    Participants participants;

    @Inject
    People people;

    @Inject
    Personnel personnel;

    @Inject
    PersonRepo personRepo;

    @Inject
    Persons persons;

    // Only add to this repository within the Servlet.init method so that all tests can rely on its data:
    @Inject
    Primes primes;

    @Inject
    Products products;

    @Inject
    Purchases purchases;

    @Inject
    Ratings ratings;

    @Inject
    Receipts receipts;

    @Inject
    Things things;

    @Resource
    private UserTransaction tran;

    @Inject
    Vehicles vehicles;

    @Override
    public void init(ServletConfig config) throws ServletException {
        // Do not add or remove from this data in tests.
        // Tests must be able to rely on this data always being present.
        primes.persist(new Prime(2, "2", "10", 1, "II", "two"),
                       new Prime(3, "3", "11", 2, "III", "three"),
                       new Prime(5, "5", "101", 2, "V", "five"),
                       new Prime(7, "7", "111", 3, "VII", "seven"),
                       new Prime(11, "B", "1011", 3, "XI", "eleven"),
                       new Prime(13, "D", "1101", 3, "XIII", "thirteen"),
                       new Prime(17, "11", "10001", 2, "XVII", "seventeen"),
                       new Prime(19, "13", "10011", 3, "XIX", "nineteen"),
                       new Prime(23, "17", "10111", 4, "XXIII", "twenty-three"),
                       new Prime(29, "1D", "11101", 4, "XXIX", "twenty-nine"),
                       new Prime(31, "1F", "11111", 5, "XXXI", "thirty-one"),
                       new Prime(37, "25", "100101", 3, "XXXVII", "thirty-seven"),
                       new Prime(41, "29", "101001", 3, "XLI", "forty-one"),
                       new Prime(43, "2B", "101011", 4, "XLIII", "forty-three"),
                       new Prime(47, "2F", "101111", 5, "XLVII", "forty-seven"),
                       new Prime(4001, "FA1", "111110100001", 7, null, "four thousand one"), // romanNumeralSymbols null
                       new Prime(4003, "FA3", "111110100011", 8, null, "four thousand three"), // romanNumeralSymbols null
                       new Prime(4007, "Fa7", "111110100111", 9, null, "four thousand seven"), // romanNumeralSymbols null
                       new Prime(4013, "FAD", "111110101101", 9, "", "Four Thousand Thirteen"), // empty list of romanNumeralSymbols
                       new Prime(4019, "FB3", "111110110011", 9, "", "four thousand nineteen"), // empty list of romanNumeralSymbols
                       new Prime(4021, "FB5", "111110110101", 9, "", " Four thousand twenty-one ")); // extra blank space at beginning and end
    }

    /**
     * Use repository methods that are designated as asynchronous by the Concurrency Asynchronous annotation.
     */
    @Test
    public void testAsynchronous() throws ExecutionException, InterruptedException, TimeoutException {
        // Clear out old data before running test
        CompletableFuture<Long> deleted = personnel.removeAll();
        deleted.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);

        // Data for test to use:
        Person p1 = new Person();
        p1.firstName = "Aaron";
        p1.lastName = "TestAsynchronous";
        p1.ssn_id = 1002003001;

        Person p2 = new Person();
        p2.firstName = "Amy";
        p2.lastName = "TestAsynchronous";
        p2.ssn_id = 1002003002;

        Person p3 = new Person();
        p3.firstName = "Alice";
        p3.lastName = "TestAsynchronous";
        p3.ssn_id = 1002003003;

        Person p4 = new Person();
        p4.firstName = "Alexander";
        p4.lastName = "TestAsynchronous";
        p4.ssn_id = 1002003004;

        Person p5 = new Person();
        p5.firstName = "Andrew";
        p5.lastName = "TestAsynchronous";
        p5.ssn_id = 1002003005;

        Person p6 = new Person();
        p6.firstName = "Brian";
        p6.lastName = "TestAsynchronous";
        p6.ssn_id = 1002003006;

        Person p7 = new Person();
        p7.firstName = "Betty";
        p7.lastName = "TestAsynchronous";
        p7.ssn_id = 1002003007;

        Person p8 = new Person();
        p8.firstName = "Bob";
        p8.lastName = "TestAsynchronous";
        p8.ssn_id = 1002003008;

        Person p9 = new Person();
        p9.firstName = "Albert";
        p9.lastName = "TestAsynchronous";
        p9.ssn_id = 1002003009;

        Person p10 = new Person();
        p10.firstName = "Ben";
        p10.lastName = "TestAsynchronous";
        p10.ssn_id = 1002003010;

        // Async multiple insert
        CompletableFuture<List<Person>> added = personnel.save(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);

        assertIterableEquals(List.of("Aaron", "Amy", "Alice", "Alexander", "Andrew", "Brian", "Betty", "Bob", "Albert", "Ben"),
                             added.get(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                                             .stream()
                                             .map(p -> p.firstName)
                                             .collect(Collectors.toList()));

        // Async update
        CompletionStage<List<Person>> updated = personnel.changeSurnames("TestAsynchronous",
                                                                         List.of(1002003009L, 1002003008L, 1002003005L,
                                                                                 1002003003L, 1002003002L, 1002003001L),
                                                                         "TestAAsynchronous") // use only alphanumeric characters to ensure consistent sorting across databases
                        .thenCompose(updateCount -> {
                            assertEquals(Integer.valueOf(6), updateCount);

                            return personnel.findByLastNameOrderByFirstName("TestAAsynchronous");
                        });

        assertIterableEquals(List.of("Aaron", "Albert", "Alice", "Amy", "Andrew", "Bob"),
                             updated.toCompletableFuture()
                                             .get(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                                             .stream()
                                             .map(p -> p.firstName)
                                             .collect(Collectors.toList()));

        // Async find as CompletableFuture<Stream<String>>
        CompletableFuture<Stream<String>> futureStream = personnel.firstNames("TestAsynchronous");
        assertIterableEquals(List.of("Alexander", "Ben", "Betty", "Brian"),
                             futureStream.get(TIMEOUT_MINUTES, TimeUnit.MINUTES).collect(Collectors.toList()));

        // Async find as CompletionStage<String[]>
        LinkedBlockingQueue<String> names = new LinkedBlockingQueue<>();
        personnel.lastNames().thenAccept(lastNames -> {
            for (String lastName : lastNames)
                names.add(lastName);
        }).toCompletableFuture().get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        assertEquals("TestAAsynchronous", names.poll());
        assertEquals("TestAsynchronous", names.poll());
        assertEquals(null, names.poll());

        // Async find single item
        CompletableFuture<Person> future = personnel.findBySSN_Id(p4.ssn_id);

        Person p = future.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        assertNotNull(p);
        assertEquals(p4.ssn_id, p.ssn_id);
        assertEquals(p4.firstName, p.firstName);
        assertEquals(p4.lastName, p.lastName);

        // Async count

        CompletableFuture<Long> nameCount = personnel.countByFirstNameStartsWith("A");

        assertEquals(Long.valueOf(6), nameCount.get(TIMEOUT_MINUTES, TimeUnit.MINUTES));

        // Find with List and various Collectors

        List<String> aNames = personnel.namesThatStartWith("A");

        // Have a collector reduce the results to a count of names.
        // The database could have done this instead, but it makes a nice, simple example.

        assertEquals(Long.valueOf(6), aNames.stream().collect(Collectors.counting()));

        // Have a collector reduce the results to the length of the longest name found.
        Collector<String, ?, Long> maxLengthFinder = Collectors.collectingAndThen(
                                                                                  Collectors.maxBy(Comparator.<String, Integer> comparing(n -> n.length())),
                                                                                  n -> n.isPresent() ? n.get().length() : -1L);

        assertEquals(Long.valueOf(9), aNames.stream().collect(maxLengthFinder));

        // Have a collector reduce the results to an average name length,
        final int COUNT = 0, SUM = 1;
        Collector<String, ?, Long> lengthAverager = Collector.of(() -> new long[2],
                                                                 (len, name) -> {
                                                                     len[COUNT]++;
                                                                     len[SUM] += name.length();
                                                                 },
                                                                 (left, right) -> new long[] {
                                                                                               left[COUNT] + right[COUNT],
                                                                                               left[SUM] + right[SUM]
                                                                 },
                                                                 len -> len[COUNT] == 0 ? 0 : (len[SUM] / len[COUNT]));

        List<String> bNames = personnel.namesThatStartWith("B");

        assertEquals(Long.valueOf(4), bNames.stream().collect(lengthAverager));

        assertEquals(Boolean.TRUE, personnel.setSurnameAsync(1002003008L, "TestAsynchronously").get(TIMEOUT_MINUTES, TimeUnit.MINUTES));

        // Async delete with void return type
        personnel.deleteByFirstName("Andrew");

        long timeout_ns = Duration.ofMinutes(TIMEOUT_MINUTES).toNanos();
        boolean found = true;
        for (long start = System.nanoTime(); System.nanoTime() - start < timeout_ns && found; Thread.sleep(200))
            found = personnel.namesThatStartWith("And").stream().findFirst().isPresent();
        assertEquals(false, found);

        // Async delete with CompletableFuture<Void> return type
        CompletableFuture<Void> cf = personnel.deleteById(1002003008L);
        assertEquals(null, cf.get(TIMEOUT_MINUTES, TimeUnit.MINUTES));

        found = personnel.namesThatStartWith("Bob").stream().findFirst().isPresent();
        assertEquals(false, found);

        deleted = personnel.removeAll();
        assertEquals(Long.valueOf(8), deleted.get(TIMEOUT_MINUTES, TimeUnit.MINUTES));
    }

    /**
     * Verify that repository methods that are designated to run asynchronously
     * actually do run asynchronously by performing database operations that would
     * deadlock if not run asynchronously.
     */
    @Test
    public void testAsyncPreventsDeadlock() throws ExecutionException, InterruptedException, NotSupportedException, SystemException, TimeoutException {
        // Clear out old data before running test
        CompletableFuture<Long> deleted = personnel.removeAll();
        deleted.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);

        // Data for test to use:
        Person p1 = new Person();
        p1.firstName = "Christopher";
        p1.lastName = "TestAsyncPreventsDeadlock";
        p1.ssn_id = 1001001001;

        Person p2 = new Person();
        p2.firstName = "Chad";
        p2.lastName = "TestAsyncPreventsDeadlock";
        p2.ssn_id = 2002002002;

        // Async multiple insert
        CompletableFuture<List<Person>> added = personnel.save(p1, p2);

        assertEquals(2, added.get(TIMEOUT_MINUTES, TimeUnit.MINUTES).size());

        CompletableFuture<Long> updated2Then1;
        CompletableFuture<Boolean> updated2;

        tran.begin();
        try {
            // main thread obtains lock on p1
            assertEquals(1L, personnel.setSurname(p1.ssn_id, "Test-AsyncPreventsDeadlock"));

            CountDownLatch locked2 = new CountDownLatch(1);

            // second thread obtains lock on p2 and then attempts lock on p1
            updated2Then1 = added.thenApplyAsync(a -> {
                try {
                    tran.begin();
                    try {
                        // lock on p2
                        long updateCount2 = personnel.setSurname(p2.ssn_id, "TestAsync-PreventsDeadlock");

                        locked2.countDown();

                        // lock on p1
                        return updateCount2 + personnel.setSurname(p1.ssn_id, "TestAsync-PreventsDeadlock");
                    } finally {
                        tran.rollback();
                    }
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });

            assertEquals(true, locked2.await(TIMEOUT_MINUTES, TimeUnit.MINUTES));

            // If this runs on a third thread as expected, it will be blocked until the second thread releases the lock.
            // If it runs inline (unexpected) deadlock will occur.
            updated2 = personnel.setSurnameAsync(p2.ssn_id, "TestAsyncPrevents-Deadlock");

            try {
                Boolean wasUpdated = updated2.get(1, TimeUnit.SECONDS);
                fail("Third thread ought to be blocked by second thread. Instead, was updated? " + wasUpdated);
            } catch (TimeoutException x) {
                // expected
            }
        } finally {
            // release lock on p1
            tran.rollback();
        }

        // With the lock on p1 released, the second thread can obtain that lock and complete
        assertEquals(Long.valueOf(2), updated2Then1.get(TIMEOUT_MINUTES, TimeUnit.MINUTES));

        // With the second thread completing, it releases both locks, allowing the third thread to obtain the lock on 2 and complete
        assertEquals(Boolean.TRUE, updated2.get(TIMEOUT_MINUTES, TimeUnit.MINUTES));
    }

    /**
     * Test True, False, NotTrue, and NotFalse on repository methods.
     */
    @Test
    public void testBooleanConditions() {
        assertIterableEquals(List.of(3L, 5L, 7L),
                             primes.findByEvenFalseAndNumberIdLessThan(10L)
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(7L, 5L, 3L),
                             primes.findByEvenNotTrueAndNumberIdLessThan(10L)
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(2L),
                             primes.findByEvenTrueAndNumberIdLessThan(10L)
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(2L),
                             primes.findByEvenNotFalseAndNumberIdLessThan(10L)
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));
    }

    /**
     * Test the CharCount keyword to query based on string length.
     */
    @Test
    public void testCharCountKeyword() {
        assertIterableEquals(List.of("eleven", "nineteen", "seven", "thirteen", "three"),
                             primes.findByNameCharCountBetween(5, 8)
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Asynchronous repository method that returns a CompletableFuture of Page.
     */
    @Test
    public void testCompletableFutureOfPage() throws ExecutionException, //
                    InterruptedException, TimeoutException {
        PageRequest page1req = PageRequest.ofPage(1).size(4);
        PageRequest page3req = PageRequest.ofPage(3).size(4);

        Order<Prime> asc = Order.by(Sort.asc(ID));

        CompletableFuture<Page<Long>> cf1 = //
                        primes.divisibleByTwo(false, page1req, asc);

        CompletableFuture<Page<Long>> cf3 = //
                        primes.divisibleByTwo(false, page3req, asc);

        Page<Long> page1 = cf1.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        Page<Long> page3 = cf3.get(TIMEOUT_MINUTES, TimeUnit.MINUTES);

        assertEquals(List.of(3L, 5L, 7L, 11L),
                     page1.content());

        assertEquals(List.of(29L, 31L, 37L, 41L),
                     page3.content());

        PageRequest page2req = page3.previousPageRequest();
        assertEquals(page2req, page1.nextPageRequest());

        assertEquals(List.of(13L, 17L, 19L, 23L),
                     primes.divisibleByTwo(false, page2req, asc)
                                     .thenApply(Page::content)
                                     .get(TIMEOUT_MINUTES, TimeUnit.MINUTES));
    }

    /**
     * Asynchronous repository method that returns a CompletionStage of CursoredPage.
     */
    @Test
    public void testCompletionStageOfPage() throws ExecutionException, InterruptedException, TimeoutException {
        LinkedBlockingQueue<Long> sums = new LinkedBlockingQueue<Long>();

        primes.findByNumberIdLessThanOrderByNumberIdDesc(42L, PageRequest.ofSize(6)).thenCompose(page1 -> {
            sums.add(page1.stream().mapToLong(p -> p.numberId).sum());
            return primes.findByNumberIdLessThanOrderByNumberIdDesc(42L, page1.nextPageRequest());
        }).thenCompose(page2 -> {
            sums.add(page2.stream().mapToLong(p -> p.numberId).sum());
            return primes.findByNumberIdLessThanOrderByNumberIdDesc(42L, page2.nextPageRequest());
        }).thenAccept(page3 -> {
            sums.add(page3.stream().mapToLong(p -> p.numberId).sum());
        }).toCompletableFuture().get(TIMEOUT_MINUTES, TimeUnit.MINUTES);

        Long sum;
        assertNotNull(sum = sums.poll());
        assertEquals(Long.valueOf(41L + 37L + 31L + 29L + 23L + 19L), sum);

        assertNotNull(sum = sums.poll());
        assertEquals(Long.valueOf(17L + 13L + 11L + 7L + 5L + 3L), sum);

        assertNotNull(sum = sums.poll());
        assertEquals(Long.valueOf(2L), sum);
    }

    /**
     * Use a repository method that performs a JDQL query using the String
     * concatenation operator ||.
     */
    @Test
    public void testConcatenationOperator() {
        assertEquals(List.of("thirty-one", "twenty-three", "thirteen", "three", "two"),
                     primes.concatAndMatch("%It%", Sort.desc(ID)));
    }

    /**
     * Use repository methods that convert a long value to other numeric types.
     */
    @Test
    public void testConvertLongValue() throws Exception {
        assertEquals(47L,
                     primes.numberAsBigDecimal(47).longValue());

        assertEquals(43L,
                     primes.numberAsBigInteger(43).orElseThrow().longValue());

        assertEquals((byte) 41,
                     primes.numberAsByte(41));

        try {
            byte result = primes.numberAsByte(4021);
            fail("Should not convert long value 4021 to byte value " + result);
        } catch (MappingException x) {
            // expected - out of range
        }

        assertEquals((byte) 37,
                     primes.numberAsByteWrapper(37).orElseThrow().byteValue());

        try {
            Optional<Byte> result = primes.numberAsByteWrapper(4019);
            fail("Should not convert long value 4019 to Byte value " + result);
        } catch (MappingException x) {
            // expected - out of range
        }

        assertEquals(4003.0, primes.numberAsDouble(4003), 0.01);

        assertEquals(4001f,
                     primes.numberAsFloatWrapper(4001)
                                     .get(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                                     .orElseThrow()
                                     .floatValue(),
                     0.01f);

        assertEquals(31,
                     primes.numberAsInt(31));

        assertEquals(29,
                     primes.numberAsInteger(29L).orElseThrow().intValue());

        assertEquals(23L,
                     primes.numberAsLong(23));

        assertEquals(19L,
                     primes.numberAsLongWrapper(19).orElseThrow().longValue());

        assertEquals((short) 4013,
                     primes.numberAsShort(4013));

        assertEquals((short) 4007,
                     primes.numberAsShortWrapper(4007).orElseThrow().shortValue());

        assertEquals(false,
                     primes.numberAsShortWrapper(27).isPresent());

    }

    /**
     * Repository method that converts a length 1 String attribute to a
     * single character.
     */
    @Test
    public void testConvertToChar() {
        assertEquals(Character.valueOf('D'),
                     primes.singleHexDigit(13).orElseThrow());

        assertEquals(false,
                     primes.singleHexDigit(12).isPresent());

        try {
            Optional<Character> found = primes.singleHexDigit(29);
            fail("Should not be able to return hex 1D as a single character: " +
                 found);
        } catch (MappingException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1046E") &&
                x.getMessage().contains("singleHexDigit"))
                ; // pass
            else
                throw x;
        }
    }

    /**
     * Repository method that returns the count as a BigDecimal value.
     */
    @Test
    public void testCountAsBigDecimal() {
        assertEquals(BigDecimal.valueOf(14L), primes.countAsBigDecimalByNumberIdLessThan(45));
    }

    /**
     * Repository method that returns the count as a BigInteger value.
     */
    @Test
    public void testCountAsBigInteger() {
        assertEquals(BigInteger.valueOf(13L), primes.countAsBigIntegerByNumberIdLessThan(43));
    }

    /**
     * Repository method that returns the count as a boolean value,
     * which is not an allowed return type. This must raise an error.
     */
    @Test
    public void testCountAsBoolean() {
        try {
            boolean count = primes.countAsBooleanByNumberIdLessThan(42);
            fail("Count queries cannot have a boolean return type: " + count);
        } catch (MappingException x) {
            // expected
        }
    }

    /**
     * Repository method that returns the count as a byte value.
     */
    @Test
    public void testCountAsByte() {
        assertEquals((byte) 12, primes.countAsβyteByNumberIdLessThan(40));
    }

    /**
     * Repository method that returns the count as a Byte value.
     */
    @Test
    public void testCountAsByteWrapper() {
        assertEquals(Byte.valueOf((byte) 10),
                     primes.countAsβyteWrapperByNumberIdLessThan(30));
    }

    /**
     * Repository method that returns the count as an int value.
     */
    @Test
    public void testCountAsInt() {
        assertEquals(9, primes.countAsIntByNumberIdLessThan(25));
    }

    /**
     * Count the number of matching entries in the database using query by method name.
     */
    @Test
    public void testCountAsInteger() throws ExecutionException, InterruptedException, TimeoutException {

        assertEquals(15L, primes.countAsLongByNumberIdLessThan(50));

        assertEquals(Integer.valueOf(0), primes.countAsIntegerByNumberIdBetween(32, 36));

        assertEquals(Integer.valueOf(3), primes.countAsIntegerByNumberIdBetween(40, 50));

        assertEquals(Short.valueOf((short) 14), primes.countByNumberIdBetweenAndEvenNot(0, 50, true).get(TIMEOUT_MINUTES, TimeUnit.MINUTES));
    }

    /**
     * Repository method that returns the count as a Long value.
     */
    @Test
    public void testCountAsLongWrapper() {
        assertEquals(Long.valueOf(8L), primes.countAsLongWrapperByNumberIdLessThan(20));
    }

    /**
     * Repository method that returns the count as a Number.
     */
    @Test
    public void testCountAsNumber() {
        assertEquals(7L, primes.countAsNumberByNumberIdLessThan(18).longValue());
    }

    /**
     * Repository method that returns the count as a short value.
     */
    @Test
    public void testCountAsShort() {
        assertEquals((short) 6, primes.countAsShortByNumberIdLessThan(15));
    }

    /**
     * Repository method that returns the count as a Short value.
     */
    @Test
    public void testCountAsShortWrapper() {
        assertEquals(Short.valueOf((short) 5), primes.countAsShortWrapperByNumberIdLessThan(12));
    }

    /**
     * Tests a repository method that returns pages of a DISTINCT single value
     * that can sometimes be null. Verify that the count of pages and total
     * elements is correct.
     */
    @Test
    public void testCountPagesWithDistinctValues() {
        Page<String> page1 = primes.romanNumeralsDistinct(30L, 49L,
                                                          4000L, 4009L,
                                                          PageRequest.ofSize(3));

        assertEquals(2, page1.totalPages());

        // page1.totalElements();
        // The above returns 5 instead of 6 because COUNT omits the
        // NULL value that comes from (DISTINCT romanNumeral) when
        // invoking SELECT COUNT(DISTINCT romanNumeral)

        assertEquals(List.of("XXXI", "XXXVII", "XLI"),
                     page1.content());

        Page<String> page2 = primes.romanNumeralsDistinct(30L, 49L,
                                                          4000L, 4009L,
                                                          page1.nextPageRequest());

        assertEquals(2, page2.totalPages());

        assertEquals(Arrays.asList("XLIII", "XLVII", null),
                     page2.content());
    }

    /**
     * Tests a repository method that returns pages of a single value that can
     * sometimes be null. Verify that the count of pages and total elements is
     * correct.
     */
    @Test
    public void testCountPagesWithNullValues() {
        Page<String> page1 = primes.romanNumeralsWithin(30L, 49L,
                                                        4000L, 4009L,
                                                        PageRequest.ofSize(3));

        assertEquals(8, page1.totalElements());
        assertEquals(3, page1.totalPages());

        assertEquals(List.of("XXXI", "XXXVII", "XLI"),
                     page1.content());

        Page<String> page2 = primes.romanNumeralsWithin(30L, 49L,
                                                        4000L, 4009L,
                                                        page1.nextPageRequest());

        assertEquals(8, page2.totalElements());
        assertEquals(3, page2.totalPages());

        assertEquals(Arrays.asList("XLIII", "XLVII", null),
                     page2.content());

        Page<String> page3 = primes.romanNumeralsWithin(30L, 49L,
                                                        4000L, 4009L,
                                                        page2.nextPageRequest());

        assertEquals(8, page3.totalElements());
        assertEquals(3, page3.totalPages());

        assertEquals(Arrays.asList(null, null),
                     page3.content());
    }

    /**
     * Repository method that uses cursor-based pagination but does not specify
     * any sort criteria, which should default to ascending by Id.
     */
    @Test
    public void testCursoredPageRequestWithImplicitSort() {
        PageRequest page1req = PageRequest.ofSize(4);
        CursoredPage<Prime> page;
        page = primes.findByRomanNumeralIgnoreCaseEndsWith("I", page1req);

        assertEquals(List.of(2L, 3L, 7L, 11L),
                     page.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        PageRequest page2req = page.nextPageRequest();

        page = primes.findByRomanNumeralIgnoreCaseEndsWith("i", page2req);

        assertEquals(List.of(13L, 17L, 23L, 31L),
                     page.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        PageRequest page3req = page.nextPageRequest();

        page = primes.findByRomanNumeralIgnoreCaseEndsWith("i", page3req);

        assertEquals(List.of(37L, 41L, 43L, 47L),
                     page.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        page2req = page.previousPageRequest();

        page = primes.findByRomanNumeralIgnoreCaseEndsWith("i", page2req);

        assertEquals(List.of(13L, 17L, 23L, 31L),
                     page.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));
    }

    /**
     * Verify that cursor-based pagination can be used with an empty string Query,
     * which the spec allows as a valid query. It will need to generate a WHERE
     * clause when appending conditions for the cursor.
     */
    @Test
    public void testCursorPaginationForEmptyQuery() {

        PageRequest page3request = PageRequest.ofPage(3)
                        .size(5)
                        .afterCursor(Cursor.forKey(12));
        CursoredPage<Prime> page3 = primes.all(page3request,
                                               Sort.asc("numberId"));

        assertIterableEquals(List.of("thirteen",
                                     "seventeen",
                                     "nineteen",
                                     "twenty-three",
                                     "twenty-nine"),
                             page3.stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        CursoredPage<Prime> page4 = primes.all(page3.nextPageRequest(),
                                               Order.by(Sort.asc("numberId")));

        assertIterableEquals(List.of("thirty-one",
                                     "thirty-seven",
                                     "forty-one",
                                     "forty-three",
                                     "forty-seven"),
                             page4.stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Verify that cursor-based pagination can be used on a Find method without
     * conditions that retrieves all entities. It will need to generate the WHERE
     * clause when appending conditions for the cursor.
     */
    @Test
    public void testCursorPaginationForFindAll() {

        CursoredPage<Prime> page3 = primes.all(Order.by(Sort.asc("numberId")),
                                               PageRequest.ofPage(3).size(3));

        assertIterableEquals(List.of("seventeen", "nineteen", "twenty-three"),
                             page3.stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        CursoredPage<Prime> page4 = primes.all(page3.nextPageRequest(),
                                               Order.by(Sort.asc("numberId")));

        assertIterableEquals(List.of("twenty-nine", "thirty-one", "thirty-seven"),
                             page4.stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Verify that cursor-based pagination can append conditions for a WHERE clause
     * when the base query only consists of a FROM clause.
     */
    @Test
    public void testCursorPaginationWithFromClauseOnly() {

        CursoredPage<Prime> page4 = primes.all(PageRequest.ofPage(4).size(3),
                                               Order.by(Sort.asc("numberId")));

        assertIterableEquals(List.of("twenty-nine", "thirty-one", "thirty-seven"),
                             page4.stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        CursoredPage<Prime> page3 = primes.all(page4.previousPageRequest(),
                                               Order.by(Sort.asc("numberId")));

        assertIterableEquals(List.of("seventeen", "nineteen", "twenty-three"),
                             page3.stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Use a repository method that specifies query language consisting only of a WHERE clause
     * and requests cursor-based pagination.
     */
    @Test
    public void testCursorPaginationWithWhereClauseOnly() {
        // The prime numbers between 1 and 50 that do not end in 7 or 3 are:
        // 2, 5, 11, 19, 29, 31, 41
        CursoredPage<Prime> page1 = primes.withinButNotEndingIn7or3(1, 50, PageRequest.ofSize(4));

        assertEquals(List.of("two", "eleven", "five", "forty-one"),
                     page1.stream()
                                     .map(p -> p.name)
                                     .collect(Collectors.toList()));

        assertEquals(4, page1.numberOfElements());
        assertEquals(7L, page1.totalElements());
        assertEquals(2L, page1.totalPages());

        assertEquals(true, page1.hasNext());

        CursoredPage<Prime> page2 = primes.withinButNotEndingIn7or3(1, 50, page1.nextPageRequest());

        assertEquals(List.of("nineteen", "thirty-one", "twenty-nine"),
                     page2.stream()
                                     .map(p -> p.name)
                                     .collect(Collectors.toList()));

        assertEquals(false, page2.hasNext());
    }

    /**
     * Use a repository that inherits from a custom repository interface with type parameters indicating the entity and key types.
     */
    @Test
    public void testCustomRepositoryInterface() {
        people.deleteBySSN_IdBetween(400000000L, 499999999L);

        Person p1 = new Person();
        p1.firstName = "Catherine";
        p1.lastName = "TestCustomRepositoryInterface";
        p1.ssn_id = 456778910;

        Person p2 = new Person();
        p2.firstName = "Claire";
        p2.lastName = "TestCustomRepositoryInterface";
        p2.ssn_id = 468998765;

        Person p3 = new Person();
        p3.firstName = "Charles";
        p3.lastName = "TestCustomRepositoryInterface";
        p3.ssn_id = 432446688;

        people.updateOrAdd(Set.of(p1, p2, p3));

        assertEquals(3L, people.countBySSN_IdBetween(400000000L, 499999999L));

        Person p4 = new Person();
        p4.firstName = "Cathy";
        p4.lastName = "TestCustomRepositoryInterface";
        p4.ssn_id = p1.ssn_id;
        people.updateOrAdd(List.of(p4));

        assertEquals(List.of("Cathy", "Charles", "Claire"),
                     Arrays.stream(people.findByLastName("TestCustomRepositoryInterface"))
                                     .map(p -> p.firstName)
                                     .collect(Collectors.toList()));

        assertEquals(1L, people.deleteBySSN_IdBetween(400000000L, 449999999L));

        assertEquals(2L, people.deleteBySSN_IdBetween(400000000L, 499999999L));
    }

    /**
     * Delete multiple entries and use a default method to atomically remove and return a removed entity.
     */
    @Test
    public void testDefaultRepositoryMethod() {
        products.clear();

        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("TDM-SE".getBytes());
        prod1.name = "TestDefaultRepositoryMethod Standard Edition";
        prod1.price = 115.99f;
        products.save(prod1);

        Product prod2 = new Product();
        prod2.pk = UUID.nameUUIDFromBytes("TDM-AE".getBytes());
        prod2.name = "TestDefaultRepositoryMethod Advanced Edition";
        prod2.price = 197.99f;
        products.save(prod2);

        Product prod3 = new Product();
        prod3.pk = UUID.nameUUIDFromBytes("TDM-EE".getBytes());
        prod3.name = "TestDefaultRepositoryMethod Expanded Edition";
        prod3.price = 153.99f;
        products.save(prod3);

        Product prod4 = new Product();
        prod4.pk = UUID.nameUUIDFromBytes("TDM-NFE".getBytes());
        prod4.name = "TestDefaultRepositoryMethod Nearly Free Edition";
        prod4.price = 1.99f;
        products.save(prod4);

        assertEquals(2, products.discontinueProducts(Set.of(prod2.pk, prod4.pk, UUID.nameUUIDFromBytes("TDM-NOT-FOUND".getBytes()))));

        // expect that 2 remain
        assertNotNull(products.findItem(prod1.pk));
        assertNotNull(products.findItem(prod3.pk));

        // In the future it will only be possible to run this on Java 21+
        // and then the following condition can be removed so that this part of the test always runs:
        if (Runtime.version().feature() >= 17) {
            // Use custom method:
            Product removed = products.remove(prod1.pk);
            assertEquals("TestDefaultRepositoryMethod Standard Edition", removed.name);

            assertEquals(false, products.findByPK(prod1.pk).isPresent());
            assertEquals(true, products.findByPK(prod3.pk).isPresent());
        }

        products.clear();
    }

    /**
     * Query by method name with deleteFirstX prefix - ensure First keywork is ignored
     */
    @Test
    public void testDeleteIgnoresFirstKeywork() {
        packages.deleteAll(); //cleanup before test

        packages.save(new Package(10001, 10.0f, 13.0f, 4.0f, "testDeleteIgnoresFirstKeywork#10001"));
        packages.save(new Package(10002, 11.0f, 12.4f, 4.0f, "testDeleteIgnoresFirstKeywork#10002"));
        packages.save(new Package(10003, 12.0f, 11.0f, 4.0f, "testDeleteIgnoresFirstKeywork#10003"));
        packages.save(new Package(10004, 13.0f, 10.0f, 4.0f, "testDeleteIgnoresFirstKeywork#10004"));

        try {
            Optional<Package> pkg = packages.deleteFirst();
            fail("Expected packages.deleteFirst() to ignore the 'first' keyword" +
                 " and fail to return a signular result. Instead returned: " + pkg);
        } catch (NonUniqueResultException e) {
            // pass
        }

        Package pkg = packages.deleteFirst5ByWidthLessThan(10.5f);
        assertEquals(10004, pkg.id);

        List<Package> pkgs = packages.deleteFirst2();
        assertEquals(3, pkgs.size());

        assertEquals(0, packages.deleteAll()); //cleanup after test
    }

    /**
     * Parameter-based query with the Delete annotation.
     */
    @Test
    public void testDeleteQueryByParameters() {
        houses.dropAll();

        House h1 = new House();
        h1.area = 1600;
        h1.garage = new Garage();
        h1.garage.area = 190;
        h1.garage.door = new GarageDoor();
        h1.garage.door.setHeight(9);
        h1.garage.door.setWidth(9);
        h1.garage.type = Garage.Type.Detached;
        h1.kitchen = new Kitchen();
        h1.kitchen.length = 14;
        h1.kitchen.width = 13;
        h1.lotSize = 0.16f;
        h1.numBedrooms = 2;
        h1.parcelId = "TestDeleteQueryByParameters-1";
        h1.purchasePrice = 116000;
        h1.sold = Year.of(2016);
        houses.insert(h1);

        House h2 = new House();
        h2.area = 2200;
        h2.garage = new Garage();
        h2.garage.area = 230;
        h2.garage.door = new GarageDoor();
        h2.garage.door.setHeight(9);
        h2.garage.door.setWidth(10);
        h2.garage.type = Garage.Type.Attached;
        h2.kitchen = new Kitchen();
        h2.kitchen.length = 14;
        h2.kitchen.width = 18;
        h2.lotSize = 0.22f;
        h2.numBedrooms = 5;
        h2.parcelId = "TestDeleteQueryByParameters-2";
        h2.purchasePrice = 212000;
        h2.sold = Year.of(2022);
        houses.insert(h2);

        House h3 = new House();
        h3.area = 1300;
        h3.kitchen = new Kitchen();
        h3.kitchen.length = 13;
        h2.kitchen.width = 12;
        h3.lotSize = 0.13f;
        h3.numBedrooms = 2;
        h3.parcelId = "TestDeleteQueryByParameters-3";
        h3.purchasePrice = 83000;
        h3.sold = Year.of(2013);
        houses.insert(h3);

        House h4 = new House();
        h4.area = 2400;
        h4.garage = new Garage();
        h4.garage.area = 240;
        h4.garage.door = new GarageDoor();
        h4.garage.door.setHeight(9);
        h4.garage.door.setWidth(12);
        h4.garage.type = Garage.Type.Detached;
        h4.kitchen = new Kitchen();
        h4.kitchen.length = 17;
        h4.kitchen.width = 14;
        h4.lotSize = 0.24f;
        h4.numBedrooms = 5;
        h4.parcelId = "TestDeleteQueryByParameters-4";
        h4.purchasePrice = 144000;
        h4.sold = Year.of(2014);
        houses.insert(h4);

        assertEquals(2, houses.discardBasedOnGarage(Garage.Type.Detached, 9));

        House h = houses.remove("TestDeleteQueryByParameters-2").orElseThrow();
        assertEquals(2200, h.area);
        assertEquals(230, h.garage.area);
        assertEquals(9, h.garage.door.getHeight());
        assertEquals(10, h.garage.door.getWidth());
        assertEquals(Garage.Type.Attached, h.garage.type);
        assertEquals(14, h.kitchen.length);
        assertEquals(18, h.kitchen.width);
        assertEquals(0.22f, h.lotSize, 0.001f);
        assertEquals(5, h.numBedrooms);
        assertEquals("TestDeleteQueryByParameters-2", h.parcelId);
        assertEquals(212000, h.purchasePrice, 0.001);
        assertEquals(Year.of(2022), h.sold);

        assertEquals(1, houses.dropAll());
    }

    /**
     * Repository delete method with query language (JPQL) that contains
     * an entity identifier variable.
     */
    @Test
    public void testDeleteQueryWithEntityIdentifierVariable() {
        products.purge("TestDeleteQueryWithEntityIdentifierVariable-Product-%");

        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("TestDeleteQueryWithEntityIdentifierVariable-1".getBytes());
        prod1.name = "TestDeleteQueryWithEntityIdentifierVariable-Product-1";
        prod1.price = 1.99f;

        Product prod2 = new Product();
        prod2.pk = UUID.nameUUIDFromBytes("TestDeleteQueryWithEntityIdentifierVariable-2".getBytes());
        prod2.name = "TestDeleteQueryWithEntityIdentifierVariable-Product-2";
        prod2.price = 2.39f;

        Product prod3 = new Product();
        prod3.pk = UUID.nameUUIDFromBytes("TestDeleteQueryWithEntityIdentifierVariable-3".getBytes());
        prod3.name = "TestDeleteQueryWithEntityIdentifierVariable-Product-3";
        prod3.price = 3.89f;

        products.saveMultiple(prod1, prod2, prod3);

        assertEquals(3, products.purge("TestDeleteQueryWithEntityIdentifierVariable-Product-%"));

        assertEquals(0, products.purge("TestDeleteQueryWithEntityIdentifierVariable-Product-%"));
    }

    /**
     * Query-by-Method-Name query with a Contains restriction applied to an
     * ElementCollection.
     */
    @Test
    public void testElementCollectionContains() {
        assertEquals(List.of(5L, 7L, 17L, 37L, 47L),
                     primes.findByRomanNumeralSymbolsContainsAndNumberIdLessThan("V",
                                                                                 50)
                                     .map(prime -> prime.numberId)
                                     .collect(Collectors.toList()));
    }

    /**
     * Unannotated entity with an attribute that is an embeddable type.
     */
    @SkipIfSysProp(DB_Postgres) //Failing on Postgres due to eclipselink issue.  https://github.com/OpenLiberty/open-liberty/issues/28380
    @Test
    public void testEmbeddable() {
        houses.dropAll();

        House h1 = new House();
        h1.area = 1800;
        h1.garage = new Garage();
        h1.garage.area = 200;
        h1.garage.door = new GarageDoor();
        h1.garage.door.setHeight(8);
        h1.garage.door.setWidth(10);
        h1.garage.type = Garage.Type.Attached;
        h1.kitchen = new Kitchen();
        h1.kitchen.length = 15;
        h1.kitchen.width = 12;
        h1.lotSize = 0.19f;
        h1.numBedrooms = 4;
        h1.parcelId = "TestEmbeddable-104-2288-60";
        h1.purchasePrice = 162000;
        h1.sold = Year.of(2018);
        houses.save(h1);

        House h2 = new House();
        h2.area = 2000;
        h2.garage = new Garage();
        h2.garage.area = 220;
        h2.garage.door = new GarageDoor();
        h2.garage.door.setHeight(8);
        h2.garage.door.setWidth(12);
        h2.garage.type = Garage.Type.Detached;
        h2.kitchen = new Kitchen();
        h2.kitchen.length = 16;
        h2.kitchen.width = 13;
        h2.lotSize = 0.18f;
        h2.numBedrooms = 4;
        h2.parcelId = "TestEmbeddable-204-2992-20";
        h2.purchasePrice = 188000;
        h2.sold = Year.of(2020);
        houses.save(h2);

        House h3 = new House();
        h3.area = 1700;
        h3.garage = new Garage();
        h3.garage.area = 180;
        h3.garage.door = new GarageDoor();
        h3.garage.door.setHeight(8);
        h3.garage.door.setWidth(9);
        h3.garage.type = Garage.Type.TuckUnder;
        h3.kitchen = new Kitchen();
        h3.kitchen.length = 14;
        h3.kitchen.width = 12;
        h3.lotSize = 0.17f;
        h3.numBedrooms = 3;
        h3.parcelId = "TestEmbeddable-304-3655-30";
        h3.purchasePrice = 153000;
        h3.sold = Year.of(2018);
        houses.save(h3);

        House h4 = new House();
        h4.area = 2400;
        h4.garage = new Garage();
        h4.garage.area = 220;
        h4.garage.door = new GarageDoor();
        h4.garage.door.setHeight(9);
        h4.garage.door.setWidth(13);
        h4.garage.type = Garage.Type.Detached;
        h4.kitchen = new Kitchen();
        h4.kitchen.length = 16;
        h4.kitchen.width = 14;
        h4.lotSize = 0.24f;
        h4.numBedrooms = 5;
        h4.parcelId = "TestEmbeddable-404-4418-40";
        h4.purchasePrice = 204000;
        h4.sold = Year.of(2022);
        houses.save(h4);

        House h = houses.findById("TestEmbeddable-104-2288-60");

        assertNotNull(h.kitchen);
        assertEquals(15, h.kitchen.length);
        assertEquals(12, h.kitchen.width);

        assertNotNull(h.garage);
        assertEquals(200, h.garage.area);
        assertEquals(Garage.Type.Attached, h.garage.type);

        assertNotNull(h.garage.door);
        assertEquals(8, h.garage.door.getHeight());
        assertEquals(10, h.garage.door.getWidth());

        // Query and order by embeddable attributes

        List<House> list = houses.findWithGarageDoorDimensions(12, 8);
        assertEquals(1, list.size());
        h = list.get(0);
        assertEquals("TestEmbeddable-204-2992-20", h.parcelId);
        assertEquals(2000, h.area);
        assertNotNull(h.garage);
        assertEquals(220, h.garage.area);
        assertEquals(Garage.Type.Detached, h.garage.type);
        assertNotNull(h.garage.door);
        assertEquals(8, h.garage.door.getHeight());
        assertEquals(12, h.garage.door.getWidth());
        assertNotNull(h.kitchen);
        assertEquals(16, h.kitchen.length);
        assertEquals(13, h.kitchen.width);
        assertEquals(0.18f, h.lotSize, 0.001f);
        assertEquals(4, h.numBedrooms);
        assertEquals(188000f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2020), h.sold);

        List<House> found = houses.findByGarageTypeOrderByGarageDoorWidthDesc(Garage.Type.Detached);
        assertEquals(found.toString(), 2, found.size());

        h = found.get(0);
        assertEquals("TestEmbeddable-404-4418-40", h.parcelId);
        assertEquals(2400, h.area);
        assertNotNull(h.garage);
        assertEquals(220, h.garage.area);
        assertEquals(Garage.Type.Detached, h.garage.type);
        assertNotNull(h.garage.door);
        assertEquals(9, h.garage.door.getHeight());
        assertEquals(13, h.garage.door.getWidth());
        assertNotNull(h.kitchen);
        assertEquals(16, h.kitchen.length);
        assertEquals(14, h.kitchen.width);
        assertEquals(0.24f, h.lotSize, 0.001f);
        assertEquals(5, h.numBedrooms);
        assertEquals(204000f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2022), h.sold);

        h = found.get(1);
        assertEquals("TestEmbeddable-204-2992-20", h.parcelId);
        assertEquals(2000, h.area);
        assertNotNull(h.garage);
        assertEquals(220, h.garage.area);
        assertEquals(Garage.Type.Detached, h.garage.type);
        assertNotNull(h.garage.door);
        assertEquals(8, h.garage.door.getHeight());
        assertEquals(12, h.garage.door.getWidth());
        assertNotNull(h.kitchen);
        assertEquals(16, h.kitchen.length);
        assertEquals(13, h.kitchen.width);
        assertEquals(0.18f, h.lotSize, 0.001f);
        assertEquals(4, h.numBedrooms);
        assertEquals(188000f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2020), h.sold);

        found = houses.findByGarage_door_heightOrderByGarage_door_heightDesc(9);
        assertEquals(1, found.size());

        h = found.get(0);
        assertEquals("TestEmbeddable-404-4418-40", h.parcelId);
        assertEquals(2400, h.area);
        assertNotNull(h.garage);
        assertEquals(220, h.garage.area);
        assertEquals(Garage.Type.Detached, h.garage.type);
        assertNotNull(h.garage.door);
        assertEquals(9, h.garage.door.getHeight());
        assertEquals(13, h.garage.door.getWidth());
        assertNotNull(h.kitchen);
        assertEquals(16, h.kitchen.length);
        assertEquals(14, h.kitchen.width);
        assertEquals(0.24f, h.lotSize, 0.001f);
        assertEquals(5, h.numBedrooms);
        assertEquals(204000f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2022), h.sold);

        // Query by attributes on base entity that could conflict with embedded attributes

        List<House> hs = houses.findByArea(2400);
        assertEquals(1, hs.size());

        h = hs.get(0);
        assertEquals("TestEmbeddable-404-4418-40", h.parcelId);
        assertEquals(2400, h.area);
        assertNotNull(h.garage);
        assertEquals(220, h.garage.area);
        assertEquals(Garage.Type.Detached, h.garage.type);
        assertNotNull(h.garage.door);
        assertEquals(9, h.garage.door.getHeight());
        assertEquals(13, h.garage.door.getWidth());
        assertNotNull(h.kitchen);
        assertEquals(16, h.kitchen.length);
        assertEquals(14, h.kitchen.width);
        assertEquals(0.24f, h.lotSize, 0.001f);
        assertEquals(5, h.numBedrooms);
        assertEquals(204000f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2022), h.sold);

        // Sorting with type-safe StaticMetamodel constant-like fields

        assertEquals(List.of("TestEmbeddable-404-4418-40",
                             "TestEmbeddable-204-2992-20",
                             "TestEmbeddable-104-2288-60",
                             "TestEmbeddable-304-3655-30"),
                     houses.findByAreaGreaterThan(1500,
                                                  Order.by(Sort.desc(_House.numBedrooms.name()),
                                                           Sort.asc(_House.LotSize.name())))
                                     .map(house -> house.parcelId)
                                     .collect(Collectors.toList()));

        assertEquals(_House.NUM_BEDROOMS, _House.numBedrooms.name());

        assertEquals(List.of("TestEmbeddable-404-4418-40",
                             "TestEmbeddable-304-3655-30",
                             "TestEmbeddable-104-2288-60",
                             "TestEmbeddable-204-2992-20"),
                     houses.findByAreaGreaterThan(1400,
                                                  Order.by(_House.garage_door_height.desc(),
                                                           _House.kitchen_width.asc(),
                                                           _House.AREA.asc()))
                                     .map(house -> house.parcelId)
                                     .collect(Collectors.toList()));

        assertEquals(List.of("TestEmbeddable-404-4418-40",
                             "TestEmbeddable-304-3655-30",
                             "TestEmbeddable-204-2992-20",
                             "TestEmbeddable-104-2288-60"),
                     houses.findByAreaGreaterThan(1300,
                                                  Order.by(_House.parcelid.descIgnoreCase()))
                                     .map(house -> house.parcelId)
                                     .collect(Collectors.toList()));

        assertEquals(List.of("TestEmbeddable-104-2288-60",
                             "TestEmbeddable-204-2992-20",
                             "TestEmbeddable-304-3655-30",
                             "TestEmbeddable-404-4418-40"),
                     houses.findByAreaGreaterThan(1200,
                                                  Order.by(_House.parcelid.ascIgnoreCase()))
                                     .map(house -> house.parcelId)
                                     .collect(Collectors.toList()));

        // Find a single attribute type

        assertArrayEquals(new int[] { 180, 200, 220, 220 },
                          houses.findGarageAreas());

        // Find a DoubleStream for a single attribute type

        double[] prices = houses.findPurchasePriceByLotSizeGreaterThan(0.15f).toArray();
        assertEquals(Arrays.toString(prices), 4, prices.length);
        assertEquals(153000.0, prices[0], 0.001);
        assertEquals(162000.0, prices[1], 0.001);
        assertEquals(188000.0, prices[2], 0.001);
        assertEquals(204000.0, prices[3], 0.001);

        assertEquals(2L, houses.deleteByKitchenWidthGreaterThan(12));

        // Find a subset of attributes

        Object[] tuple = houses.findGarageDoorAndKitchenLengthAndKitchenWidthByParcelId("TestEmbeddable-304-3655-30").orElseThrow();
        GarageDoor door = (GarageDoor) tuple[0];
        assertEquals(8, door.getHeight());
        assertEquals(9, door.getWidth());
        assertEquals(Integer.valueOf(14), tuple[1]); // kitchen length
        assertEquals(Integer.valueOf(12), tuple[2]); // kitchen width

        assertIterableEquals(List.of("[14, 12, 180, 1700]", "[15, 12, 200, 1800]"),
                             houses.findKitchenLengthAndKitchenWidthAndGarageAreaAndAreaByAreaLessThan(2000)
                                             .map(Arrays::toString)
                                             .collect(Collectors.toList()));

        /*
         * Update embeddable attributes
         * TODO enable once #30789 is fixed
         */

        //assertEquals(true, houses.updateByParcelIdSetGarageAddAreaAddKitchenLengthSetNumBedrooms("TestEmbeddable-304-3655-30", null, 180, 2, 4));

        //h = houses.findById("TestEmbeddable-304-3655-30");
        //assertEquals("TestEmbeddable-304-3655-30", h.parcelId);
        //assertEquals(1880, h.area);
        // Null embeddables aren't required by JPA, but EclipseLink claims to support it as the default behavior.
        // See https://wiki.eclipse.org/EclipseLink/UserGuide/JPA/Basic_JPA_Development/Entities/Embeddable#Nullable_embedded_values
        // But it looks like EclipseLink has a bug here in that it only nulls out 1 of the fields of Garage, not all,
        // JPQL: UPDATE House o SET o.garage=?2, o.area=o.area+?3, o.kitchen.length=o.kitchen.length+?4, o.numBedrooms=?5 WHERE (o.parcelId=?1)
        // SQL:  UPDATE WLPHouse SET NUMBEDROOMS = 4, AREA = (AREA + 180), GARAGEAREA = NULL, KITCHENLENGTH = (KITCHENLENGTH + 2) WHERE (PARCELID = 'TestEmbeddable-304-3655-30')
        // This causes the following assertion to fail:
        // assertEquals(null, h.garage);
        // TODO re-enable the above if EclipseLink bug #24926 is fixed
        //assertNotNull(h.kitchen);
        //assertEquals(16, h.kitchen.length);
        //assertEquals(12, h.kitchen.width);
        //assertEquals(0.17f, h.lotSize, 0.001f);
        //assertEquals(4, h.numBedrooms);
        //assertEquals(153000f, h.purchasePrice, 0.001f);
        //assertEquals(Year.of(2018), h.sold);

        assertEquals(2, houses.dropAll());
    }

    /**
     * Test Empty and NotEmpty on repository methods.
     */
    @Test
    public void testEmpty() {
        assertIterableEquals(List.of(4007L, 4013L, 4019L),
                             primes.findByNumberIdInAndRomanNumeralSymbolsEmpty(Set.of(7L, 4007L, 13L, 4013L, 19L, 4019L))
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        Stack<Long> list = new Stack<>();
        list.addAll(Set.of(7L, 4007L, 13L, 4013L, 19L, 4019L));

        assertIterableEquals(List.of(7L, 13L, 19L),
                             primes.findByNumberIdInAndRomanNumeralSymbolsNotEmpty(list)
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(4003L),
                             primes.findByNumberIdInAndRomanNumeralEmpty(List.of(43L, 4003L))
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));
        assertIterableEquals(List.of(43L),
                             primes.findByNumberIdInAndRomanNumeralNotEmpty(List.of(43L, 4003L))
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));
    }

    /**
     * Tests whether a user can write an empty repository class.
     * This is only useful when just starting out developing and you don't have methods yet.
     */
    @Test
    public void testEmptyRepository() {
        assertNotNull(emptyRepo);
        String str = emptyRepo.toString();
        assertTrue(str, str.startsWith("test.jakarta.data.web.EmptyRepository(Proxy)@"));
    }

    /**
     * Identify whether elements exist in the database using query by method name.
     */
    @Test
    public void testExists() {
        assertEquals(true, primes.existsByNumberId(19));
        assertEquals(false, primes.existsByNumberId(9));

        assertEquals(Boolean.TRUE, primes.existsByNumberIdBetween(Long.valueOf(14), Long.valueOf(18)));
        assertEquals(Boolean.FALSE, primes.existsByNumberIdBetween(Long.valueOf(24), Long.valueOf(28)));
    }

    /**
     * Use exists methods in the Query by Method Name pattern where the return type
     * is a CompletionStage or CompletableFuture and the repository method is
     * annotated to run Asynchronous.
     */
    @Test
    public void testExistsAsync() throws ExecutionException, //
                    InterruptedException, TimeoutException {
        CompletionStage<Boolean> stage1 = primes.existsByNameIgnoreCase("thirty-one");
        CompletionStage<Boolean> stage2 = primes.existsByNameIgnoreCase("thirty-two");

        assertEquals(Boolean.TRUE,
                     stage1.toCompletableFuture()
                                     .get(TIMEOUT_MINUTES, TimeUnit.MINUTES));

        assertEquals(Boolean.FALSE,
                     stage2.toCompletableFuture()
                                     .get(TIMEOUT_MINUTES, TimeUnit.MINUTES));

        CompletableFuture<Boolean> cf1 = primes.existsByRomanNumeralIgnoreCase("XLI");
        CompletableFuture<Boolean> cf2 = primes.existsByRomanNumeralIgnoreCase("XLII");

        assertEquals(Boolean.TRUE,
                     cf1.get(TIMEOUT_MINUTES, TimeUnit.MINUTES));

        assertEquals(Boolean.FALSE,
                     cf2.get(TIMEOUT_MINUTES, TimeUnit.MINUTES));
    }

    /**
     * Query-by-method name repository operation to remove and return one or more entities.
     */
    @Test
    public void testFindAndDelete() {
        packages.save(new Package(40001, 41.0f, 14.0f, 4.0f, "testFindAndDelete#40001"));
        packages.save(new Package(40004, 44.0f, 40.4f, 4.4f, "testFindAndDelete#40004"));
        packages.save(new Package(40012, 42.0f, 12.0f, 2.0f, "testFindAndDelete#4001x"));
        packages.save(new Package(40013, 43.0f, 13.0f, 3.0f, "testFindAndDelete#4001x"));

        Optional<Package> none = packages.deleteByDescription("testFindAndDelete#40000");
        assertEquals(true, none.isEmpty());

        Package p1 = packages.deleteByDescription("testFindAndDelete#40001").orElseThrow();
        assertEquals(40001, p1.id);
        assertEquals(41.0f, p1.length, 0.01f);
        assertEquals(14.0f, p1.width, 0.01f);
        assertEquals(4.0f, p1.height, 0.01f);
        assertEquals("testFindAndDelete#40001", p1.description);

        try {
            Optional<Package> p = packages.deleteByDescription("testFindAndDelete#4001x");
            fail("Should get NonUniqueResultException when there are multiple results but a singular return type. Instead, result is: " + p);
        } catch (NonUniqueResultException x) {
            // expected
        }

        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
        boolean supportsOrderByForUpdate = !jdbcJarName.startsWith("derby");
        Sort<?>[] sorts = supportsOrderByForUpdate ? new Sort[] { Sort.asc("id") } : null;

        Package[] p = packages.deleteByDescriptionEndsWith("#4001x", sorts);
        assertEquals(Arrays.toString(p), 2, p.length);

        if (!supportsOrderByForUpdate) {
            System.out.println("Sorting results in test code.");
            p = Stream.of(p)
                            .sorted(Comparator.comparing(pkg -> pkg.id))
                            .collect(Collectors.toList())
                            .toArray(new Package[2]);
        }

        assertEquals(40012, p[0].id);
        assertEquals(42.0f, p[0].length, 0.01f);
        assertEquals(12.0f, p[0].width, 0.01f);
        assertEquals(2.0f, p[0].height, 0.01f);
        assertEquals("testFindAndDelete#4001x", p[0].description);

        assertEquals(40013, p[1].id);
        assertEquals(43.0f, p[1].length, 0.01f);
        assertEquals(13.0f, p[1].width, 0.01f);
        assertEquals(3.0f, p[1].height, 0.01f);
        assertEquals("testFindAndDelete#4001x", p[1].description);

        p = packages.deleteByDescriptionEndsWith("#40000");
        assertEquals(Arrays.toString(p), 0, p.length);

        p = packages.deleteByDescriptionEndsWith("#40004");
        assertEquals(Arrays.toString(p), 1, p.length);

        assertEquals(40004, p[0].id);
        assertEquals(44.0f, p[0].length, 0.01f);
        assertEquals(40.4f, p[0].width, 0.01f);
        assertEquals(4.4f, p[0].height, 0.01f);
        assertEquals("testFindAndDelete#40004", p[0].description);
    }

    /**
     * Annotated repository operation to remove and return a single entity.
     */
    @Test
    public void testFindAndDeleteAnnotated() {
        packages.save(new Package(50001, 51.0f, 31.0f, 21.0f, "testFindAndDeleteAnnotated#50001"));
        packages.save(new Package(50002, 52.0f, 32.0f, 22.0f, "testFindAndDeleteAnnotated#50002"));

        Package p1 = packages.take(50001);
        assertEquals(50001, p1.id);
        assertEquals(51.0f, p1.length, 0.01f);
        assertEquals(31.0f, p1.width, 0.01f);
        assertEquals(21.0f, p1.height, 0.01f);
        assertEquals("testFindAndDeleteAnnotated#50001", p1.description);

        try {
            p1 = packages.take(50001);
            fail("Should get EmptyResultException when there is no result. Instead, result is: " + p1);
        } catch (EmptyResultException x) {
            // expected
        }

        Package p2 = packages.take(50002);
        assertEquals(50002, p2.id);
        assertEquals(52.0f, p2.length, 0.01f);
        assertEquals(32.0f, p2.width, 0.01f);
        assertEquals(22.0f, p2.height, 0.01f);
        assertEquals("testFindAndDeleteAnnotated#50002", p2.description);
    }

    /**
     * Annotated repository operation to remove and return a single entity.
     */
    @Test
    public void testFindAndDeleteMultipleAnnotated() {
        packages.save(new Package(60001, 61.0f, 41.0f, 26.0f, "testFindAndDeleteMultipleAnnotated"));
        packages.save(new Package(60002, 62.0f, 42.0f, 25.0f, "testFindAndDeleteMultipleAnnotated"));
        packages.save(new Package(60003, 59.0f, 39.0f, 24.0f, "testFindAndDeleteMultipleAnnotated"));

        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
        boolean supportsOrderByForUpdate = !jdbcJarName.startsWith("derby");

        List<Package> list = supportsOrderByForUpdate //
                        ? packages.takeOrdered("testFindAndDeleteMultipleAnnotated") //
                        : packages.take("testFindAndDeleteMultipleAnnotated");
        assertEquals(list.toString(), 3, list.size());

        if (!supportsOrderByForUpdate) {
            System.out.println("Sorting results in test code.");
            list.sort(Comparator.comparing(p -> p.width));
        }

        Package p0 = list.get(0);
        Package p1 = list.get(1);
        Package p2 = list.get(2);

        assertEquals(60003, p0.id);
        assertEquals(59.0f, p0.length, 0.01f);
        assertEquals(39.0f, p0.width, 0.01f);
        assertEquals(24.0f, p0.height, 0.01f);
        assertEquals("testFindAndDeleteMultipleAnnotated", p0.description);

        assertEquals(60001, p1.id);
        assertEquals(61.0f, p1.length, 0.01f);
        assertEquals(41.0f, p1.width, 0.01f);
        assertEquals(26.0f, p1.height, 0.01f);
        assertEquals("testFindAndDeleteMultipleAnnotated", p1.description);

        assertEquals(60002, p2.id);
        assertEquals(62.0f, p2.length, 0.01f);
        assertEquals(42.0f, p2.width, 0.01f);
        assertEquals(25.0f, p2.height, 0.01f);
        assertEquals("testFindAndDeleteMultipleAnnotated", p2.description);

        assertEquals(Collections.EMPTY_LIST, packages.take("testFindAndDeleteMultipleAnnotated"));
    }

    /**
     * Find-and-delete returning a record.
     */
    @Test
    public void testFindAndDeleteRecord() {
        assertEquals(false, receipts.deleteByPurchaseId(600L).isPresent());

        receipts.save(new Receipt(600L, "C1510-13-600", 6.89f));

        Receipt r = receipts.deleteByPurchaseId(600L).orElseThrow();
        assertEquals(600L, r.purchaseId());
        assertEquals("C1510-13-600", r.customer());
        assertEquals(6.89f, r.total(), 0.001f);
    }

    /**
     * Find-and-delete returning multiple records.
     */
    @Test
    public void testFindAndDeleteRecords() {
        assertIterableEquals(Collections.EMPTY_SET, receipts.discardFor("C1510-13-999"));

        receipts.save(new Receipt(909L, "C1510-13-999", 9.09f));
        receipts.save(new Receipt(900L, "C1510-13-900", 9.00f));
        receipts.save(new Receipt(999L, "C1510-13-999", 9.99f));
        receipts.save(new Receipt(990L, "C1510-13-999", 9.90f));

        Collection<Receipt> deleted = receipts.discardFor("C1510-13-999");

        assertEquals(deleted.toString(), 3, deleted.size());

        List<Receipt> list = deleted.stream()
                        .sorted(Comparator.comparing(Receipt::purchaseId))
                        .toList();

        Receipt r = list.get(0);
        assertEquals(909, r.purchaseId());
        assertEquals("C1510-13-999", r.customer());
        assertEquals(9.09f, r.total(), 0.001f);

        r = list.get(1);
        assertEquals(990, r.purchaseId());
        assertEquals("C1510-13-999", r.customer());
        assertEquals(9.90f, r.total(), 0.001f);

        r = list.get(2);
        assertEquals(999, r.purchaseId());
        assertEquals("C1510-13-999", r.customer());
        assertEquals(9.99f, r.total(), 0.001f);

        deleted = receipts.discardFor("C1510-13-900");

        assertEquals(deleted.toString(), 1, deleted.size());

        r = deleted.iterator().next();
        assertEquals(900, r.purchaseId());
        assertEquals("C1510-13-900", r.customer());
        assertEquals(9.00f, r.total(), 0.001f);
    }

    /**
     * Find-and-delete repository operations that return one or more IDs, corresponding to removed entities.
     */
    @Test
    public void testFindAndDeleteReturnsIds() throws Exception {
        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
        boolean supportsOrderByForUpdate = !jdbcJarName.startsWith("derby");

        packages.deleteAll();

        packages.save(new Package(80081, 18.0f, 18.1f, 8.8f, "testFindAndDeleteReturnsIds#80081"));
        packages.save(new Package(80080, 80.0f, 80.0f, 8.0f, "testFindAndDeleteReturnsIds#80080"));
        packages.save(new Package(80088, 88.0f, 18.8f, 8.8f, "testFindAndDeleteReturnsIds#80088"));
        packages.save(new Package(80008, 80.0f, 10.8f, 0.8f, "testFindAndDeleteReturnsIds#80008"));

        Set<Integer> remaining = new TreeSet<>();
        remaining.addAll(Set.of(80008, 80080, 80081, 80088));

        Sort<Package> sort = supportsOrderByForUpdate ? Sort.desc("width") : null;
        Integer id = packages.delete1(Limit.of(1), sort).orElseThrow();
        if (supportsOrderByForUpdate)
            assertEquals(Integer.valueOf(80080), id);
        assertEquals("Found " + id + "; expected one of " + remaining, true, remaining.remove(id));

        Sort<?>[] sorts = supportsOrderByForUpdate ? new Sort[] { Sort.desc("height"), Sort.asc("length") } : null;
        int[] ids = packages.delete2(Limit.of(2), sorts);
        assertEquals(Arrays.toString(ids), 2, ids.length);
        if (supportsOrderByForUpdate) {
            assertEquals(80081, ids[0]);
            assertEquals(80088, ids[1]);
        }
        assertEquals("Found " + ids[0] + "; expected one of " + remaining, true, remaining.remove(ids[0]));
        assertEquals("Found " + ids[1] + "; expected one of " + remaining, true, remaining.remove(ids[1]));

        // should have only 1 remaining
        ids = packages.delete2(Limit.of(2), sorts);
        assertEquals(Arrays.toString(ids), 1, ids.length);
        assertEquals(remaining.iterator().next(), Integer.valueOf(ids[0]));
    }

    /**
     * Find-and-delete repository operations that return invalid types that are neither the entity class,
     * record class, or id class.
     */
    @Test
    public void testFindAndDeleteReturnsInvalidTypes() {
        packages.deleteAll();

        packages.save(new Package(60006, 16.0f, 61.1f, 6.0f, "testFindAndDeleteReturnsInvalidTypes#60006"));

        Sort<Package> sort = Sort.asc("id");

        try {
            long[] deleted = packages.delete3(Limit.of(3), sort);
            fail("Deleted with return type of long[]: " + Arrays.toString(deleted) + " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }

        try {
            List<String> deleted = packages.delete4(Limit.of(4), sort);
            fail("Deleted with return type of List<String>: " + deleted + " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }

        try {
            Collection<Number> deleted = packages.delete5(Limit.of(5), sort);
            fail("Deleted with return type of Collection<Number>: " + deleted + " even though the id type is int.");
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
        packages.deleteAll();

        Sort<Package> sort = Sort.asc("id");

        try {
            long[] deleted = packages.delete3(Limit.of(3), sort);
            fail("Deleted with return type of long[]: " + Arrays.toString(deleted) + " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }

        try {
            List<String> deleted = packages.delete4(Limit.of(4), sort);
            fail("Deleted with return type of List<String>: " + deleted + " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }

        try {
            Collection<Number> deleted = packages.delete5(Limit.of(5), sort);
            fail("Deleted with return type of Collection<Number>: " + deleted + " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }
    }

    /**
     * Find-and-delete repository operations that return one or more objects, corresponding to removed entities.
     */
    @Test
    public void testFindAndDeleteReturnsObjects() {
        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
        boolean supportsOrderByForUpdate = !jdbcJarName.startsWith("derby");

        packages.deleteAll();

        //                        id     length width height description
        packages.save(new Package(70071, 17.0f, 17.1f, 7.7f, "testFindAndDeleteReturnsObjects#multi"));
        packages.save(new Package(70070, 70.0f, 70.0f, 7.0f, "testFindAndDeleteReturnsObjects#70070"));
        packages.save(new Package(70077, 77.0f, 17.7f, 7.7f, "testFindAndDeleteReturnsObjects#multi"));
        packages.save(new Package(70007, 70.0f, 10.7f, 0.7f, "testFindAndDeleteReturnsObjects#70007"));

        Set<Integer> remaining = new TreeSet<>();
        remaining.addAll(Set.of(70007, 70070, 70071, 70077));

        Sort<Package> sort = supportsOrderByForUpdate ? Sort.desc("width") : null;
        Object[] deleted = packages.destroy(Limit.of(1), sort);
        assertEquals("Deleted " + Arrays.toString(deleted), 1, deleted.length);
        Package p = (Package) deleted[0];
        if (supportsOrderByForUpdate) {
            assertEquals(70070, p.id);
            assertEquals(70.0f, p.length, 0.001f);
            assertEquals(70.0f, p.width, 0.001f);
            assertEquals(7.0f, p.height, 0.001f);
            assertEquals("testFindAndDeleteReturnsObjects#70070", p.description);
        }
        assertEquals("Found " + p.id + "; expected one of " + remaining, true, remaining.remove(p.id));

        // It is not deterministic to order on height when multiple entities
        // have a floating point value that looks the same (7.7), but which is
        // not exact and can vary slightly, intermittently ordering the entities
        // differently. Instead, we are using the description column, which can
        // be reliably compared.
        // It is okay to order on length because the other entity with length=70.0
        // was previously removed.
        Sort<?>[] sorts = supportsOrderByForUpdate //
                        ? new Sort[] { Sort.desc("description"), Sort.asc("length") } //
                        : null;
        LinkedList<?> deletesList = packages.delete2ByHeightLessThan(8.0f, Limit.of(2), sorts);
        assertEquals("Deleted " + deletesList, 2, deletesList.size());
        Package p0 = (Package) deletesList.get(0);
        Package p1 = (Package) deletesList.get(1);
        if (supportsOrderByForUpdate) {
            assertEquals(70071, p0.id);
            assertEquals(17.0f, p0.length, 0.001f);
            assertEquals(17.1f, p0.width, 0.001f);
            assertEquals(7.7f, p0.height, 0.001f);
            assertEquals("testFindAndDeleteReturnsObjects#multi", p0.description);
            assertEquals(70077, p1.id);
            assertEquals(77.0f, p1.length, 0.001f);
            assertEquals(17.7f, p1.width, 0.001f);
            assertEquals(7.7f, p1.height, 0.001f);
            assertEquals("testFindAndDeleteReturnsObjects#multi", p1.description);
        }
        assertEquals("Found " + p0.id + "; expected one of " + remaining, true, remaining.remove(p0.id));
        assertEquals("Found " + p1.id + "; expected one of " + remaining, true, remaining.remove(p1.id));

        // should have only 1 remaining
        deleted = packages.destroy(Limit.of(4), sort);
        assertEquals("Deleted " + Arrays.toString(deleted), 1, deleted.length);
        assertEquals(remaining.iterator().next(), Integer.valueOf(((Package) deleted[0]).id));
    }

    /**
     * Find-and-delete repository operation that sorts entities according to the
     * OrderBy method name keyword. Also cover the same scenario, but with the
     * OrderBy annotation.
     */
    @Test
    public void testFindAndDeleteWithOrderBy() {
        String testName = "TestFindAndDeleteWithOrderByKeyword";
        //                        id   length   width   height  description
        packages.save(new Package(517, 1165.0f, 1044.0f, 517.0f, testName));
        packages.save(new Package(527, 625.0f, 336.0f, 527.0f, testName));
        packages.save(new Package(533, 925.0f, 756.0f, 533.0f, testName));
        packages.save(new Package(551, 601.0f, 240.0f, 551.0f, testName));
        packages.save(new Package(559, 1009.0f, 840.0f, 559.0f, testName));
        packages.save(new Package(583, 1465.0f, 1344.0f, 583.0f, testName));
        packages.save(new Package(589, 661.0f, 300.0f, 589.0f, testName));

        Package[] removed = packages.deleteByDescriptionOrderByWidthDesc(testName,
                                                                         Limit.of(3));
        assertEquals(List.of(583, 517, 559),
                     Stream.of(removed)
                                     .map(pkg -> pkg.id)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(551, 589),
                     packages.deleteByDescriptionOrderByWidthAsc(testName,
                                                                 Limit.of(2)));

        // remaining entities are:
        //         id   length  width   height  description
        // Package(527, 625.0f, 336.0f, 527.0f, testName))
        // Package(533, 925.0f, 756.0f, 533.0f, testName))

        assertEquals(List.of(533),
                     packages.removeIfDescriptionMatches(testName, Limit.of(1)));

        assertEquals(List.of(527),
                     packages.removeIfDescriptionMatches(testName, Limit.of(10)));
    }

    /**
     * Find a record entity based on its embedded id, which is also a record.
     * This test covers finding and returning an entity in addition to
     * finding an entity to save/update/delete.
     */
    @Test
    public void testFindByEmbeddedId() {
        List<Animal> found = animals.findAll().toList();
        if (!found.isEmpty())
            animals.deleteAll(found);

        Animal redFox = animals.insert(Animal.of("red fox", "Vulpes", "vulpes"));
        Animal grayFox = animals.insert(Animal.of("gray fox", "Urocyon", "cinereoargenteus"));
        Animal foxSquirrel = animals.insert(Animal.of("Fox squirrel", "Sciurus", "niger"));
        Animal graySquirrel = animals.insert(Animal.of("gray squirrel", "Sciurus", "carolinensis"));
        Animal redSquirrel = animals.insert(Animal.of("red squirrel", "Tamiasciurus", "hudsonicus"));

        assertEquals("red fox", redFox.commonName());
        assertEquals("Vulpes", redFox.id().genus());
        assertEquals("vulpes", redFox.id().species());
        assertEquals(1, redFox.version());

        assertEquals("Fox squirrel", foxSquirrel.commonName());
        assertEquals("Sciurus", foxSquirrel.id().genus());
        assertEquals("niger", foxSquirrel.id().species());
        assertEquals(1, foxSquirrel.version());

        // TODO enable once #29460 is fixed
        //assertEquals(List.of("Sciurus carolinensis",
        //                     "Sciurus niger"),
        //             animals.ofGenus("Sciurus")
        //                             .map(n -> n.genus() + ' ' + n.species())
        //                             .collect(Collectors.toList()));

        //ScientificName grayFoxId = new ScientificName("Urocyon", "cinereoargenteus");
        //grayFox = animals.findById(grayFoxId).orElseThrow();
        //assertEquals("gray fox", grayFox.commonName());
        //assertEquals("Urocyon", grayFox.id().genus());
        //assertEquals("cinereoargenteus", grayFox.id().species());

        //ScientificName graySquirrelId = new ScientificName("Sciurus", "carolinensis");
        //graySquirrel = animals.findById(graySquirrelId).orElseThrow();
        //assertEquals("gray squirrel", graySquirrel.commonName());
        //assertEquals("Sciurus", graySquirrel.id().genus());
        //assertEquals("carolinensis", graySquirrel.id().species());

        //foxSquirrel = foxSquirrel.withCommonName("FOX SQUIRREL");
        //foxSquirrel = animals.save(foxSquirrel);
        //assertEquals("FOX SQUIRREL", foxSquirrel.commonName());
        //assertEquals("Sciurus", foxSquirrel.id().genus());
        //assertEquals("niger", foxSquirrel.id().species());
        //assertEquals(2, foxSquirrel.version());

        //foxSquirrel = foxSquirrel.withCommonName("fox squirrel");
        //foxSquirrel = animals.update(foxSquirrel);
        //assertEquals("fox squirrel", foxSquirrel.commonName());
        //assertEquals("Sciurus", foxSquirrel.id().genus());
        //assertEquals("niger", foxSquirrel.id().species());
        //assertEquals(3, foxSquirrel.version());

        animals.deleteById(new ScientificName("Sciurus", "niger"));

        assertEquals(4L, animals.countByIdNotNull());

        animals.delete(redSquirrel);

        assertEquals(false, animals.existsById(redSquirrel.id()));

        //found = animals.findAll().toList(); TODO replace next line
        found = List.of(redFox, grayFox, graySquirrel);
        assertEquals(found.toString(), 3, found.size());
        animals.deleteAll(found);
    }

    /**
     * Search for missing item. Insert it. Search again.
     */
    @Test
    public void testFindCreateFind() {
        UUID id = UUID.nameUUIDFromBytes("OL306-233F".getBytes());

        try {
            Product prod = products.findItem(id);
            fail("Should not find " + prod);
        } catch (EmptyResultException x) {
            // expected
        }

        Product prod = new Product();
        prod.pk = id;
        prod.name = "Something";
        prod.price = 3.99f;
        prod.description = "An item for sale.";

        products.save(prod);

        Product p = products.findItem(id);
        assertEquals(prod.pk, p.pk);
        assertEquals(prod.name, p.name);
        assertEquals(prod.price, p.price, 0.001f);
        assertEquals(prod.description, p.description);
    }

    /**
     * Find the first or first 3 of a list of sorted results.
     */
    @Test
    public void testFindFirst() throws Exception {
        Prime prime = primes.findFirstByNameLikeOrderByNumberId("%ven");
        assertNotNull(prime);
        assertEquals(7, prime.numberId);

        Prime[] p = primes.findFirst5ByNumberIdLessThanEqual(25); // descending order by name
        assertNotNull(p);
        assertEquals(Arrays.toString(p), 5, p.length);
        assertEquals("two", p[0].name);
        assertEquals("twenty-three", p[1].name);
        assertEquals("three", p[2].name);
        assertEquals("thirteen", p[3].name);
        assertEquals("seventeen", p[4].name);
    }

    /**
     * Find method that includes "First" but doesn't include "By" because there are no conditions.
     */
    @Test
    public void testFindFirstWithoutBy() {
        Prime first = primes.findFirst(Sort.asc("numberId"), Limit.of(1));
        assertEquals(2, first.numberId);
    }

    /**
     * Use the % and _ characters, which are wildcards in JPQL, within query parameters.
     */
    @Test
    public void testFindLike() throws Exception {
        // Remove data from previous tests:
        Product[] allProducts = products.findByVersionGreaterThanEqualOrderByPrice(-1);
        if (allProducts.length > 0)
            products.discontinueProducts(Arrays.stream(allProducts).map(p -> p.pk).collect(Collectors.toSet()));

        Product p1 = new Product();
        p1.pk = UUID.nameUUIDFromBytes("TFL-1".getBytes());
        p1.name = "TestFindLike_1";
        p1.price = 1.00f;
        products.save(p1);

        Product p2 = new Product();
        p2.pk = UUID.nameUUIDFromBytes("TFL-2".getBytes());
        p2.name = "2% TestFindLike";
        p2.price = 2.00f;
        products.save(p2);

        Product p10 = new Product();
        p10.pk = UUID.nameUUIDFromBytes("TFL-10".getBytes());
        p10.name = "TestFindLike 1";
        p10.price = 10.00f;
        products.save(p10);

        Product p100 = new Product();
        p100.pk = UUID.nameUUIDFromBytes("TFL-100".getBytes());
        p100.name = "TestFindLike  1";
        p100.price = 100.00f;
        products.save(p100);

        Product p200 = new Product();
        p200.pk = UUID.nameUUIDFromBytes("TFL-200".getBytes());
        p200.name = "200 TestFindLike";
        p200.price = 200.00f;
        products.save(p200);

        assertEquals(List.of("2% TestFindLike",
                             "200 TestFindLike",
                             "TestFindLike  1",
                             "TestFindLike 1",
                             "TestFindLike_1"),
                     products.findByNameLike("%TestFindLike%")
                                     .stream()
                                     .map(p -> p.name)
                                     .collect(Collectors.toList()));

        // _ wildcard matches any single character
        assertEquals(List.of("TestFindLike 1", "TestFindLike_1"),
                     products.findByNameLike("TestFindLike_1")
                                     .stream()
                                     .map(p -> p.name)
                                     .collect(Collectors.toList()));

        // % wildcard matches 0 or more characters
        assertEquals(List.of("2% TestFindLike", "200 TestFindLike"),
                     products.findByNameLike("2% TestFindLike")
                                     .stream()
                                     .map(p -> p.name)
                                     .collect(Collectors.toList()));

        // Escape characters are not possible for the repository Like keyword, however,
        // consider using JPQL escape characters and ESCAPE '\' clause for StartsWith, EndsWith, and Contains
    }

    /**
     * Search for multiple entries.
     */
    @Test
    public void testFindMultiple() throws Exception {
        assertEquals(Collections.EMPTY_LIST, personRepo.find("TestFindMultiple"));

        Person jane = new Person();
        jane.firstName = "Jane";
        jane.lastName = "TestFindMultiple";
        jane.ssn_id = 123456789;

        Person joe = new Person();
        joe.firstName = "Joe";
        joe.lastName = "TestFindMultiple";
        joe.ssn_id = 987654321;

        Person jude = new Person();
        jude.firstName = "Jude";
        jude.lastName = "Test-FindMultiple";
        jude.ssn_id = 11235813;

        tran.begin();
        try {
            personRepo.save(List.of(jane, joe));
            personRepo.save(List.of(jude));
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }

        List<Person> found = personRepo.find("TestFindMultiple");
        assertNotNull(found);
        assertEquals(2, found.size());

        Person p1 = found.get(0);
        Person p2expected;
        assertEquals("TestFindMultiple", p1.lastName);
        if (jane.firstName.equals(p1.firstName)) {
            assertEquals(jane.ssn_id, p1.ssn_id);
            p2expected = joe;
        } else {
            assertEquals(joe.ssn_id, p1.ssn_id);
            p2expected = jane;
        }

        Person p2 = found.get(1);
        assertEquals(p2expected.lastName, p2.lastName);
        assertEquals(p2expected.firstName, p2.firstName);
        assertEquals(p2expected.ssn_id, p2.ssn_id);

        found = personRepo.find("Test-FindMultiple");
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals(jude.ssn_id, found.get(0).ssn_id);
    }

    /**
     * Find method that uses the query-by-parameter name pattern.
     */
    @Test
    public void testFindQueryByParameters() {
        Prime prime = primes.findHexadecimal("2B").orElseThrow();
        assertEquals(43, prime.numberId);
        assertEquals(false, prime.even);
        assertEquals("2B", prime.hex);
        assertEquals("forty-three", prime.name);
        assertEquals("XLIII", prime.romanNumeral);
        assertEquals(List.of("X", "L", "I", "I", "I"), prime.romanNumeralSymbols);
        assertEquals(4, prime.sumOfBits);

        assertEquals(false, primes.findHexadecimal("2A").isPresent());

        assertEquals(List.of("thirty-seven", "thirteen", "seven", "nineteen"),
                     primes.find(false, 3, Limit.of(4), Sort.asc("even"), Sort.desc("name"))
                                     .map(p -> p.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Find a subset of attributes of the entity.
     */
    @Test
    public void testFindSubsetOfAttributes() {
        List<Object[]> all = primes.findNumberIdAndName(Sort.asc("numberId"));

        Object[] idAndName = all.get(5);
        assertEquals(13L, idAndName[0]);
        assertEquals("thirteen", idAndName[1]);

        idAndName = all.get(8);
        assertEquals(23L, idAndName[0]);
        assertEquals("twenty-three", idAndName[1]);
    }

    /**
     * Repository methods where the FROM clause identifies the entity.
     */
    @Test
    public void testFromClauseIdentifiesEntity() {
        products.clear();

        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("TestFromClauseIdentifiesEntity-1".getBytes());
        prod1.name = "TestFromClauseIdentifiesEntity-Product-1";
        prod1.price = 8.99f;
        prod1 = multi.create(prod1);

        Product prod2 = new Product();
        prod2.pk = UUID.nameUUIDFromBytes("TestFromClauseIdentifiesEntity-2".getBytes());
        prod2.name = "TestFromClauseIdentifiesEntity-Product-2";
        prod2.price = 12.99f;
        prod2 = multi.create(prod2);

        Product prod3 = new Product();
        prod3.pk = UUID.nameUUIDFromBytes("TestFromClauseIdentifiesEntity-3".getBytes());
        prod3.name = "TestFromClauseIdentifiesEntity-Product-3";
        prod3.price = 16.99f;
        prod3 = multi.create(prod3);

        assertEquals(3L, multi.countEverything());

        assertEquals(1L, multi.discount("TestFromClauseIdentifiesEntity-Product-3", 0.30f));
        assertEquals(3L, multi.discount("TestFromClauseIdentifiesEntity-Product-_", 0.20f));

        assertEquals(8.79f, products.findByPK(prod1.pk).orElseThrow().price, 0.001f);
        assertEquals(12.79f, products.findByPK(prod2.pk).orElseThrow().price, 0.001f);
        assertEquals(16.49f, products.findByPK(prod3.pk).orElseThrow().price, 0.001f);

        assertEquals(3L, multi.destroy("TestFromClauseIdentifiesEntity-%"));

        assertEquals(0L, multi.countEverything());
    }

    /**
     * Verify a repository method can use JPQL query language that supplies
     * id(this) as an argument to another function.
     */
    @Test
    public void testFunctionWithIdThisArg() {
        vehicles.delete();

        Vehicle v1 = new Vehicle();
        v1.make = "Chevrolet";
        v1.model = "Silverado";
        v1.numSeats = 3;
        v1.price = 38000f;
        v1.vinId = "CS102030405060708";
        vehicles.save(List.of(v1));

        v1 = vehicles.withVINLowerCase("cs102030405060708").orElseThrow();
        assertEquals("Chevrolet", v1.make);
        assertEquals("Silverado", v1.model);
        assertEquals(3, v1.numSeats);
        assertEquals(38000f, v1.price, 0.001f);
        assertEquals("CS102030405060708", v1.vinId);

        assertEquals(1L, vehicles.delete());
    }

    /**
     * Verify that ORDER BY can be generated, taking into account the entity variable name of a custom query.
     * The custom query in this case has no WHERE clause.
     * Other tests cover similar scenarios in which a WHERE clause is present.
     */
    @Test
    public void testGeneratedOrderAppendedToCustomQuery() {

        assertIterableEquals(List.of("thirteen", "seventeen", "nineteen", "twenty-three", "twenty-nine"),
                             primes.all(Sort.asc("numberId"),
                                        PageRequest.ofPage(2).size(5)));
    }

    /**
     * Inherited repository method with a generic array return type.
     */
    @Test
    public void testGenericArrayReturnType() {
        people.deleteBySSN_IdBetween(100101001l, 100101004l);

        Person p1 = new Person();
        p1.firstName = "George";
        p1.lastName = "TestGenericArrayReturnType";
        p1.ssn_id = 100101001l;

        Person p2 = new Person();
        p2.firstName = "Gordon";
        p2.lastName = "TestGenericArrayReturnType";
        p2.ssn_id = 100101002l;

        Person p3 = new Person();
        p3.firstName = "Gary";
        p3.lastName = "TestGenericArrayReturnType-NonMatching";
        p3.ssn_id = 100101003l;

        Person p4 = new Person();
        p4.firstName = "Gerald";
        p4.lastName = "TestGenericArrayReturnType";
        p4.ssn_id = 100101004l;

        people.updateOrAdd(List.of(p1, p2, p3, p4));

        Person[] found = people.findByLastName("TestGenericArrayReturnType");

        assertEquals(Arrays.toString(found), 3, found.length);
        assertEquals("George", found[0].firstName);
        assertEquals("Gerald", found[1].firstName);
        assertEquals("Gordon", found[2].firstName);

        assertEquals(4l, people.deleteBySSN_IdBetween(100101001l, 100101004l));
    }

    /**
     * Cursor pagination with ignoreCase in the sort criteria.
     */
    @Test
    public void testIgnoreCaseInCursorPagination() {
        Order<Prime> order = Order.by(_Prime.sumOfBits.asc(), _Prime.name.descIgnoreCase());
        PageRequest pagination = PageRequest.ofSize(3).withoutTotal();
        CursoredPage<Prime> page1 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L,
                                                                             pagination,
                                                                             order);
        assertIterableEquals(List.of("four thousand one", "four thousand three", "Four Thousand Thirteen"),
                             page1
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        assertEquals(true, page1.hasNext());
        CursoredPage<Prime> page2 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L,
                                                                             page1.nextPageRequest(),
                                                                             order);
        assertIterableEquals(List.of("four thousand seven", "four thousand nineteen"),
                             page2
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        Order<Prime> ascName = Order.by(_Prime.name.ascIgnoreCase());
        pagination = PageRequest.ofSize(4);
        page1 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L,
                                                         pagination,
                                                         ascName);
        assertIterableEquals(List.of("four thousand nineteen", "four thousand one", "four thousand seven", "Four Thousand Thirteen"),
                             page1
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        assertEquals(true, page1.hasNext());
        page2 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L,
                                                         page1.nextPageRequest(),
                                                         ascName);
        assertIterableEquals(List.of("four thousand three"),
                             page2
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Offset pagination with ignoreCase in the sort criteria.
     */
    @Test
    public void testIgnoreCaseInOffsetPagination() {
        Order<Prime> ascSumOfBitsName = Order.by(Sort.asc("sumOfBits"), Sort.ascIgnoreCase("name"));
        PageRequest pagination = PageRequest.ofSize(4);
        Page<Prime> page1 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L,
                                                                            ascSumOfBitsName,
                                                                            pagination);
        assertIterableEquals(List.of("four thousand one", "four thousand three", "four thousand nineteen", "four thousand seven"),
                             page1
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        assertEquals(true, page1.hasNext());
        Page<Prime> page2 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L,
                                                                            ascSumOfBitsName,
                                                                            page1.nextPageRequest());
        assertIterableEquals(List.of("Four Thousand Thirteen"),
                             page2
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        Order<Prime> descHex = Order.by(Sort.descIgnoreCase("hex"));
        pagination = PageRequest.ofSize(3);
        page1 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L,
                                                                descHex,
                                                                pagination);
        assertIterableEquals(List.of("FB3", "FAD", "Fa7"),
                             page1
                                             .stream()
                                             .map(p -> p.hex)
                                             .collect(Collectors.toList()));

        assertEquals(true, page1.hasNext());
        page2 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L,
                                                                descHex,
                                                                page1.nextPageRequest());
        assertIterableEquals(List.of("FA3", "FA1"),
                             page2
                                             .stream()
                                             .map(p -> p.hex)
                                             .collect(Collectors.toList()));
    }

    /**
     * Specify IgnoreCase keyword in OrderBy.
     */
    @Test
    public void testIgnoreCaseInOrderByPatternOfMethodName() {
        assertIterableEquals(List.of("four thousand three", "Four Thousand Thirteen", "four thousand seven", "four thousand one", "four thousand nineteen"),
                             primes.findByNumberIdBetweenOrderByNameIgnoreCaseDesc(4000L, 4020L)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Specify IgnoreCase on various conditions.
     */
    @Test
    public void testIgnoreCaseInQueryConditions() {
        // Equals
        assertEquals("twenty-nine", primes.findByNameIgnoreCase("Twenty-Nine").name);

        // Not
        assertIterableEquals(List.of("two", "five", "seven"),
                             primes.findByNameIgnoreCaseNotAndNumberIdLessThanOrderByNumberIdAsc("Three", 10)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        // StartsWith
        assertIterableEquals(List.of("thirteen", "thirty-one", "thirty-seven"),
                             primes.findByNameIgnoreCaseStartsWithAndNumberIdLessThanOrderByNumberIdAsc("Thirt%n", 1000)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        // Like
        assertIterableEquals(List.of("thirteen", "thirty-seven"),
                             primes.findByNameIgnoreCaseLikeAndNumberIdLessThanOrderByNumberIdAsc("Thirt%n", 1000)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        // Contains
        assertIterableEquals(List.of("twenty-three", "seventeen"),
                             primes.findByNameIgnoreCaseContainsAndNumberIdLessThanOrderByNumberIdDesc("ent%ee", 1000)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        // Between
        assertIterableEquals(List.of("nineteen", "seventeen", "seven"),
                             primes.findByNameIgnoreCaseBetweenAndNumberIdLessThanOrderByNumberIdDesc("Nine", "SEVENTEEN", 50)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        // GreaterThan, LessThanEqual
        assertIterableEquals(List.of("XLVII", "XLIII", "XIII", "XI", "VII", "V", "III"),
                             primes.findByHexIgnoreCaseGreaterThanAndRomanNumeralIgnoreCaseLessThanEqualAndNumberIdLessThan("2a", "xlvII", 50)
                                             .stream()
                                             .map(p -> p.romanNumeral)
                                             .collect(Collectors.toList()));
    }

    /**
     * Specify Sorts with ignoreCase.
     */
    @Test
    public void testIgnoreCaseInSorts() {
        assertIterableEquals(List.of("FA1", "FA3", "FB3", "FAD", "Fa7"),
                             primes.findByNumberIdBetween(4000L, 4020L, Sort.asc("sumOfBits"), Sort.descIgnoreCase("hex"))
                                             .stream()
                                             .map(p -> p.hex)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("FA1", "FA3", "Fa7", "FAD", "FB3"),
                             primes.findByNumberIdBetween(4000L, 4020L, Sort.ascIgnoreCase("hex"), Sort.desc("sumOfBits"))
                                             .stream()
                                             .map(p -> p.hex)
                                             .collect(Collectors.toList()));
    }

    /**
     * Tests repository insert methods.
     */
    @AllowedFFDC("jakarta.data.exceptions.EntityExistsException")
    @SkipIfSysProp({
                     DB_Postgres, //Failing on Postgres due to eclipselink issue:  https://github.com/OpenLiberty/open-liberty/issues/28380
                     DB_SQLServer //Failing on SQLServer due to eclipselink issue: https://github.com/OpenLiberty/open-liberty/issues/28737
    })
    @Test
    public void testInsert() throws Exception {
        people.deleteBySSN_IdBetween(0L, 999999999L);

        // insert single:

        Person isaac = new Person();
        isaac.firstName = "Isaac";
        isaac.lastName = "TestInsert";
        isaac.ssn_id = 999009001;
        persons.insert(isaac);

        Person ike = new Person();
        ike.firstName = "Ike";
        ike.lastName = "TestInsert";
        ike.ssn_id = 999009001;
        try {
            persons.insert(ike);
            fail("Did not detect duplicate insert of id.");
        } catch (EntityExistsException x) {
            // pass
        }

        Person p = personnel.findBySSN_Id(999009001).join();
        assertEquals("Isaac", p.firstName);

        // insert collection:

        Person ian = new Person();
        ian.firstName = "Ian";
        ian.lastName = "TestInsert";
        ian.ssn_id = 999009002;

        Person isabelle = new Person();
        isabelle.firstName = "Isabelle";
        isabelle.lastName = "TestInsert";
        isabelle.ssn_id = 999009003;

        try {
            personRepo.insertAll(List.of(ian, ike, isabelle));
            fail("Did not detect duplicate insert of id within collection.");
        } catch (EntityExistsException x) {
            // pass
        }

        personRepo.insertAll(List.of(ian, isabelle));

        // insert varargs array:
        Person irene = new Person();
        irene.firstName = "Irene";
        irene.lastName = "TestInsert";
        irene.ssn_id = 999009004;

        Person isaiah = new Person();
        isaiah.firstName = "Isaiah";
        isaiah.lastName = "TestInsert";
        isaiah.ssn_id = 999009005;

        persons.insertAll(irene, isaiah);

        Person ivan = new Person();
        ivan.firstName = "Ivan";
        ivan.lastName = "TestInsert";
        ivan.ssn_id = 999009006;

        Person iris = new Person();
        iris.firstName = "Iris";
        iris.lastName = "TestInsert";
        iris.ssn_id = ivan.ssn_id;

        try {
            persons.insertAll(ivan, iris);
            fail("Did not detect duplicate insert of id within array.");
        } catch (EntityExistsException x) {
            // pass
        }

        tran.begin();
        try {
            Person ivy = new Person();
            ivy.firstName = "Ivy";
            ivy.lastName = "TestInsert";
            ivy.ssn_id = 999009007;
            persons.insert(ivy);

            try {
                persons.insert(ike);
                fail("Did not detect duplicate insert of id within transaction.");
            } catch (EntityExistsException x) {
                // pass
            }
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }

        assertEquals(5, people.deleteBySSN_IdBetween(0L, 999999999L));
    }

    /**
     * Insert and delete multiple entities.
     */
    // @AllowedFFDC("jakarta.data.exceptions.EntityExistsException")
    @SkipIfSysProp({
                     DB_Postgres, //Failing on Postgres due to eclipselink issue:  https://github.com/OpenLiberty/open-liberty/issues/28380
                     DB_SQLServer //Failing on SQLServer due to eclipselink issue: https://github.com/OpenLiberty/open-liberty/issues/28737
    })
    @Test
    public void testInsertAndDeleteMultiple() throws Exception {
        people.deleteBySSN_IdBetween(0L, 999999999L);

        // insert multiple:

        Person david = new Person();
        david.firstName = "David";
        david.lastName = "TestInsertAndDeleteMultiple";
        david.ssn_id = 999009999;

        Person daniel = new Person();
        daniel.firstName = "Daniel";
        daniel.lastName = "TestInsertAndDeleteMultiple";
        daniel.ssn_id = 999009998;

        Person dorothy = new Person();
        dorothy.firstName = "Dorothy";
        dorothy.lastName = "TestInsertAndDeleteMultiple";
        dorothy.ssn_id = 999009997;

        Person dianne = new Person();
        dianne.firstName = "Dianne";
        dianne.lastName = "TestInsertAndDeleteMultiple";
        dianne.ssn_id = 999009996;

        Person dominic = new Person();
        dominic.firstName = "Dominic";
        dominic.lastName = "TestInsertAndDeleteMultiple";
        dominic.ssn_id = 999009995;

        assertEquals(null, personnel.insertAll(david, daniel, dorothy, dianne, dominic).join());

        assertEquals(List.of("Daniel", "David", "Dianne", "Dominic", "Dorothy"),
                     personRepo.findFirstNames("TestInsertAndDeleteMultiple"));

        Person dennis = new Person();
        dennis.firstName = "Dennis";
        dennis.lastName = "TestInsertAndDeleteMultiple";
        dennis.ssn_id = 999009994;

        // attempted insert where one is duplicate:

        CompletableFuture<Void> future = personnel.insertAll(dennis, daniel);

        try {
            future.join();
            fail("Did not detect duplicate insert of id within varags array.");
        } catch (CompletionException x) {
            if ((x.getCause() instanceof EntityExistsException))
                ; // pass
            else
                throw x;
        }

        assertEquals(List.of("Daniel", "David", "Dianne", "Dominic", "Dorothy"),
                     personRepo.findFirstNames("TestInsertAndDeleteMultiple"));

        // delete multiple entities at once

        assertEquals(null, personnel.deleteMultiple(daniel, david).join());

        assertEquals(List.of("Dianne", "Dominic", "Dorothy"),
                     personRepo.findFirstNames("TestInsertAndDeleteMultiple"));

        // attempt deletion where one is not found:

        future = personnel.deleteMultiple(dianne, dorothy, david);

        try {
            future.join();
            fail("Deletion did not detect missing entity.");
        } catch (CompletionException x) {
            if ((x.getCause() instanceof OptimisticLockingFailureException))
                ; // pass
            else
                throw x;
        }

        assertEquals(List.of("Dianne", "Dominic", "Dorothy"),
                     personRepo.findFirstNames("TestInsertAndDeleteMultiple"));

        // delete remaining:

        assertEquals(Integer.valueOf(3), personnel.deleteSeveral(Stream.of(dianne, dorothy, dominic)).join());

        assertEquals(List.of(),
                     personRepo.findFirstNames("TestInsertAndDeleteMultiple"));
    }

    /**
     * Repository method that returns IntStream.
     */
    @Test
    public void testIntStreamResult() {
        assertIterableEquals(List.of(5, 4, 3, 3, 5, 4, 4),
                             primes.findSumOfBitsByNumberIdBetween(20, 49)
                                             .mapToObj(i -> Integer.valueOf(i))
                                             .collect(Collectors.toList()));
    }

    /**
     * Page and CursoredPage are Iterable.
     */
    @Test
    public void testIterablePages() {
        // CursoredPage:
        Page<Prime> page = primes.findByNumberIdBetween(20L, 40L, PageRequest.ofSize(3));
        List<Long> results = new ArrayList<>();
        for (Prime p : page)
            results.add(p.numberId);
        assertIterableEquals(List.of(23L, 29L, 31L), results);
        assertEquals(3, page.content().size());

        page = primes.findByNumberIdBetween(0L, 1L, PageRequest.ofSize(5));
        Iterator<Prime> it = page.iterator();
        assertEquals(false, it.hasNext());
        try {
            Prime p = it.next();
            fail("Obtained next from iterator: " + p);
        } catch (NoSuchElementException x) {
            // expected
        }
        assertEquals(Collections.EMPTY_LIST, page.content());

        page = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(23L, PageRequest.ofSize(4));
        results = new ArrayList<>();
        for (Prime p : page)
            results.add(p.numberId);
        assertIterableEquals(List.of(23L, 19L, 17L, 13L), results);

        page = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(1L, PageRequest.ofSize(6));
        it = page.iterator();
        assertEquals(false, it.hasNext());
        try {
            Prime p = it.next();
            fail("Obtained next from iterator: " + p);
        } catch (NoSuchElementException x) {
            // expected
        }
    }

    /**
     * Supply a PageRequest to a repository method that returns an Iterator.
     * Specify the sort criteria statically via the OrderBy annotation.
     * This test also covers precedence of the Or keyword.
     */
    @Test
    public void testIteratorWithOffsetPagination() {
        PageRequest p = PageRequest.ofSize(5);
        Iterator<Prime> it = primes.findByNameStartsWithAndNumberIdLessThanOrNameContainsAndNumberIdLessThan("t", 50L, "n", 50L, p);

        assertEquals("three", it.next().name);
        assertEquals("seventeen", it.next().name);
        assertEquals("seven", it.next().name);
        assertEquals("eleven", it.next().name);
        assertEquals("thirteen", it.next().name);
        assertEquals(false, it.hasNext());

        p = PageRequest.ofPage(2).size(p.size());
        it = primes.findByNameStartsWithAndNumberIdLessThanOrNameContainsAndNumberIdLessThan("t", 50L, "n", 50L, p);
        assertEquals("nineteen", it.next().name);
        assertEquals("thirty-seven", it.next().name);
        assertEquals("forty-one", it.next().name);
        assertEquals("twenty-three", it.next().name);
        assertEquals("twenty-nine", it.next().name);
        assertEquals(false, it.hasNext());

        p = PageRequest.ofPage(3).size(p.size());
        it = primes.findByNameStartsWithAndNumberIdLessThanOrNameContainsAndNumberIdLessThan("t", 50L, "n", 50L, p);
        assertEquals("thirty-one", it.next().name);
        assertEquals("forty-seven", it.next().name);
        assertEquals("two", it.next().name);
        assertEquals(false, it.hasNext());
    }

    /**
     * Iterator with Sort only and no pagination.
     */
    @Test
    public void testIteratorWithoutPagination() {
        Iterator<Prime> it = primes.findByNumberIdNotGreaterThan(41L, Sort.desc("sumOfBits"), Sort.desc("romanNumeral"));

        assertEquals("XXXI", it.next().romanNumeral);

        assertEquals("XXIX", it.next().romanNumeral);
        assertEquals("XXIII", it.next().romanNumeral);

        assertEquals("XXXVII", it.next().romanNumeral);
        assertEquals("XLI", it.next().romanNumeral);
        assertEquals("XIX", it.next().romanNumeral);
        assertEquals("XIII", it.next().romanNumeral);
        assertEquals("XI", it.next().romanNumeral);
        assertEquals("VII", it.next().romanNumeral);

        assertEquals("XVII", it.next().romanNumeral);
        assertEquals("V", it.next().romanNumeral);
        assertEquals("III", it.next().romanNumeral);

        assertEquals("II", it.next().romanNumeral);

        assertEquals(false, it.hasNext());
    }

    /**
     * Access pages in the next page direction while entities are being added and
     * removed, using a cursor to avoid duplicates.
     */
    @Test
    public void testCursorNext() {
        packages.deleteAll();

        packages.saveAll(List.of(new Package(114, 14.0f, 90.0f, 15.0f, "package#114"), // page1
                                 new Package(116, 16.0f, 88.0f, 36.0f, "package#116"),
                                 new Package(118, 18.0f, 95.0f, 22.0f, "package#118"),
                                 // will add 117: 17.0f, ... between page requests     // not on any page because it is added after
                                 // will add 120: 20.0f, ... between page requests     // page2
                                 new Package(122, 22.0f, 90.0f, 60.0f, "package#122"),
                                 new Package(124, 22.0f, 80.0f, 62.0f, "package#124"),
                                 // will add 130: 22.0f, 70.0f, ... between page requests // page 3
                                 new Package(132, 22.0f, 60.0f, 66.0f, "package#132"),
                                 new Package(133, 33.0f, 56.0f, 65.0f, "package#133"),
                                 new Package(140, 33.0f, 56.0f, 64.0f, "package#140"), // page 4
                                 new Package(144, 33.0f, 56.0f, 63.0f, "package#144"),
                                 new Package(148, 48.0f, 45.0f, 50.0f, "package#148"),
                                 new Package(150, 48.0f, 45.0f, 50.0f, "package#150"), // page 5
                                 new Package(151, 48.0f, 45.0f, 41.0f, "package#151")));

        CursoredPage<Package> page;

        // Page 1
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, PageRequest
                        .ofSize(3)
                        .withoutTotal());

        assertIterableEquals(List.of(114, 116, 118),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // should not appear on next page because we already read up to length 18.0:
        packages.save(new Package(117, 17.0f, 23.0f, 12.0f, "package#117"));

        // should appear on next page because length 20.0 is beyond the cursor value of 18.0:
        packages.save(new Package(120, 20.0f, 23.0f, 12.0f, "package#120"));

        // Page 2
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, page.nextPageRequest());

        assertIterableEquals(List.of(120, 122, 124),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // remove some entries that we already read:
        packages.deleteByIdIn(List.of(116, 118, 120, 122, 124));

        // should appear on next page because length 22.0 matches the cursor value
        // and width 70.0 is beyond the cursor value:
        packages.save(new Package(130, 22.0f, 70.0f, 67.0f, "package#130"));

        // Page 3
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, page.nextPageRequest());

        assertIterableEquals(List.of(130, 132, 133),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteById(130);

        // Page 4
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, page.nextPageRequest());

        assertIterableEquals(List.of(140, 144, 148),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteByIdIn(List.of(132, 140));

        // Page 5
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, page.nextPageRequest());

        assertIterableEquals(List.of(150, 151),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // No more pages
        assertEquals(false, page.hasNext());

        // At this point, the following should remain (sorted by width descending, height ascending, id descending):
        // 114: 14.0f, 90.0f, 15.0f
        // 144: 33.0f, 56.0f, 63.0f
        // 133: 33.0f, 56.0f, 65.0f
        // 151: 48.0f, 45.0f, 41.0f
        // 150: 48.0f, 45.0f, 50.0f
        // 148: 48.0f, 45.0f, 50.0f
        // 117: 17.0f, 23.0f, 12.0f

        // Dynamically request forward sorting where the criteria is the reverse of the above,
        // starting after (but not including) the first position

        page = packages.findByHeightLessThanOrHeightGreaterThan(20.0f, 40.0f,
                                                                Order.by(Sort.asc("width"), Sort.desc("height"), Sort.asc("id")),
                                                                PageRequest.ofSize(5).afterCursor(Cursor.forKey(23.0f, 12.0f, 117)));

        assertIterableEquals(List.of(148, 150, 151, 133, 144),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // Switch to pages of size 4.

        // Page 1
        page = packages.findByHeightGreaterThan(10.0f, PageRequest.ofSize(4));

        assertEquals(1L, page.pageRequest().page());

        assertIterableEquals(List.of(114, 144, 133, 151),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.saveAll(List.of(// comes after the cursor values, should be included in next page
                                 new Package(128, 28.0f, 45.0f, 53.0f, "package#128"),
                                 // comes before the cursor values, should not be on next page
                                 new Package(153, 53.0f, 45.0f, 28.0f, "package#153")));

        // Page 2
        page = packages.findByHeightGreaterThan(10.0f, page.nextPageRequest());

        assertEquals(2L, page.pageRequest().page());

        assertIterableEquals(List.of(150, 148, 128, 117),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // No more pages
        assertEquals(false, page.hasNext());

        PageRequest previous = page.previousPageRequest();
        assertNotNull(previous);
        assertEquals(1L, previous.page());

        // At this point, the following should remain (sorted by width descending, length ascending, id ascending):
        // 114: 14.0f, 90.0f, 15.0f
        // 133: 33.0f, 56.0f, 65.0f
        // 144: 33.0f, 56.0f, 63.0f
        // 128: 28.0f, 45.0f, 53.0f
        // 148: 48.0f, 45.0f, 50.0f
        // 150: 48.0f, 45.0f, 50.0f
        // 151: 48.0f, 45.0f, 41.0f
        // 153: 53.0f, 45.0f, 28.0f
        // 117: 17.0f, 23.0f, 12.0f (will not match query condition)

        // Page 1
        page = packages.whereVolumeWithin(5000.0f, 123456.0f, PageRequest.ofSize(6));

        assertIterableEquals(List.of(114, 133, 144, 128, 148, 150),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteByIdIn(List.of(144, 148, 150));

        packages.save(new Package(152, 48.0f, 45.0f, 52.0f, "package#152"));

        // Page 2
        page = packages.whereVolumeWithin(5000.0f, 123456.0f, page.nextPageRequest());

        assertIterableEquals(List.of(151, 152, 153),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // No more pages
        assertEquals(false, page.hasNext());

        // Previous page
        previous = page.previousPageRequest();
        assertNotNull(previous);
        assertEquals(1L, previous.page());

        page = packages.whereVolumeWithin(5000.0f, 123456.0f, previous);

        assertEquals(1L, page.pageRequest().page());

        assertIterableEquals(List.of(114, 133, 128),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));
    }

    /**
     * Access pages in the next page direction, but delete the remaining entries
     * before accessing the next page. Expect the next page to be empty, and the
     * next/previous PageRequest from the empty page to be null.
     */
    @Test
    public void testCursorNextPageEmptyAfterDeletion() {
        packages.deleteAll();

        packages.saveAll(List.of(new Package(440, 40.0f, 44.0f, 40.0f, "package#440"), // page1
                                 new Package(441, 41.0f, 41.0f, 41.0f, "package#441"))); // will be deleted

        CursoredPage<Package> page;

        // Page 1
        page = packages.findByHeightGreaterThan(4.0f, PageRequest.ofSize(1).withoutTotal());

        assertIterableEquals(List.of(440),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(true, page.hasContent());

        PageRequest next = page.nextPageRequest();
        assertNotNull(next);
        assertEquals(2L, next.page());

        // Delete what would be the contents of page 2
        packages.deleteById(441);

        // Page 2 (should be empty)
        page = packages.findByHeightGreaterThan(4.0f, next);

        assertIterableEquals(Collections.EMPTY_LIST,
                             page.content());

        assertEquals(0, page.numberOfElements());
        assertEquals(false, page.hasContent());

        // An empty page lacks cursor values from which to request next/previous pages
        assertEquals(false, page.hasNext());
        assertEquals(false, page.hasPrevious());
    }

    /**
     * Obtain cursors from a page of results and use them to obtain pages in the
     * next page and previous page directions.
     */
    @Test
    public void testCursorPagination() {
        // Expected order for OrderByEvenDescSumOfBitsDescNumberAsc:
        // num binary sum even?
        // 2,  10,     1, true
        // 31, 11111,  5, false
        // 23, 10111,  4, false
        // 29, 11101,  4, false
        // 43, 101011, 4, false
        // 7,  111,    3, false
        // 11, 1011,   3, false
        // 13, 1101,   3, false
        // 19, 10011,  3, false
        // 37, 100101, 3, false
        // 41, 101001, 3, false
        // 3,  11,     2, false
        // 5,  101,    2, false
        // 17, 10001,  2, false

        PageRequest initialPagination = PageRequest.ofPage(2).size(8).afterCursor(Cursor.forKey(false, 4, 23L));
        CursoredPage<Prime> page2 = primes.findByNumberIdBetweenOrderByEvenDescSumOfBitsDescNumberIdAsc(0L, 45L, initialPagination);

        assertIterableEquals(List.of(29L, 43L, 7L, 11L, 13L, 19L, 37L, 41L),
                             page2.stream().map(p -> p.numberId).collect(Collectors.toList()));

        PageRequest.Cursor cursor7 = page2.cursor(2);
        PageRequest paginationBefore7 = PageRequest.ofSize(8).beforeCursor(cursor7);

        CursoredPage<Prime> page1 = primes.findByNumberIdBetweenOrderByEvenDescSumOfBitsDescNumberIdAsc(0L, 45L, paginationBefore7);

        assertIterableEquals(List.of(2L, 31L, 23L, 29L, 43L),
                             page1.stream().map(p -> p.numberId).collect(Collectors.toList()));

        PageRequest.Cursor cursor13 = page2.cursor(4);
        PageRequest paginationAfter13 = PageRequest.ofPage(3).afterCursor(cursor13);

        CursoredPage<Prime> page3 = primes.findByNumberIdBetweenOrderByEvenDescSumOfBitsDescNumberIdAsc(0L, 45, paginationAfter13);

        assertIterableEquals(List.of(19L, 37L, 41L, 3L, 5L, 17L),
                             page3.stream().map(p -> p.numberId).collect(Collectors.toList()));

        // test .equals method
        assertEquals(cursor13, cursor13);
        assertEquals(cursor13, page2.cursor(4));
        assertEquals(false, cursor13.equals(cursor7));

        // test .hashCode method
        Map<PageRequest.Cursor, Integer> map = new HashMap<>();
        map.put(cursor13, 13);
        map.put(cursor7, 7);
        assertEquals(Integer.valueOf(7), map.get(cursor7));
        assertEquals(Integer.valueOf(13), map.get(cursor13));

        assertEquals(false, cursor7.toString().equals(cursor13.toString()));
    }

    /**
     * Access pages in the previous page direction using a cursor.
     */
    @Test
    public void testCursorPrevious() {
        packages.deleteAll();

        packages.saveAll(List.of(new Package(210, 10.0f, 50.0f, 55.0f, "package#210"), // page 1
                                 new Package(215, 15.0f, 50.0f, 55.0f, "package#215"), // page 1
                                 new Package(219, 19.0f, 39.0f, 19.0f, "package#219"), // non-matching
                                 new Package(220, 20.0f, 22.0f, 38.0f, "package#220"), // page 2
                                 new Package(224, 24.0f, 32.0f, 39.0f, "package#224"), // page 2
                                 new Package(228, 28.0f, 62.0f, 87.0f, "package#228"), // page 2
                                 new Package(230, 30.0f, 81.0f, 88.0f, "package#230"), // page 3
                                 new Package(233, 33.0f, 32.0f, 43.0f, "package#233"), // page 3
                                 new Package(236, 36.0f, 50.0f, 93.0f, "package#236"), // page 3
                                 new Package(240, 40.0f, 21.0f, 42.0f, "package#240")));

        CursoredPage<Package> page;

        // Page 3
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(20.0f, PageRequest
                        .ofPage(3)
                        .size(3)
                        .withoutTotal()
                        .beforeCursor(Cursor.forKey(40.0f, 94.0f, 42.0f, 240)));

        assertEquals(3L, page.pageRequest().page());

        assertIterableEquals(List.of(230, 233, 236),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // Page 2
        assertEquals(true, page.hasPrevious());
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(20.0f, page.previousPageRequest());

        assertEquals(2L, page.pageRequest().page());

        assertIterableEquals(List.of(220, 224, 228),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // Page 1
        assertEquals(true, page.hasPrevious());
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(20.0f, page.previousPageRequest());

        assertEquals(1L, page.pageRequest().page());

        assertIterableEquals(List.of(210, 215),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(false, page.hasPrevious());

        // using @OrderBy width descending, height ascending, id descending:
        // id   length width  height
        // 230, 30.0f, 81.0f, 88.0f // second page 1
        // 228, 28.0f, 62.0f, 87.0f // second page 1
        // 215, 15.0f, 50.0f, 55.0f // page 1
        // 210, 10.0f, 50.0f, 55.0f // page 1
        // 236, 36.0f, 50.0f, 93.0f // page 2
        // 219, 19.0f, 39.0f, 19.0f // non-matching
        // 224, 24.0f, 32.0f, 39.0f // page 2
        // 233, 33.0f, 32.0f, 43.0f // page 3
        // 220, 20.0f, 22.0f, 38.0f // page 3
        // 240, 40.0f, 21.0f, 42.0f

        PageRequest.Cursor cursor = new PageRequest.Cursor() {
            private final List<Object> cursorElements = List.of(21.0f, 42.0f, 240);

            @Override
            public List<?> elements() {
                return cursorElements;
            }

            @Override
            public Object get(int index) {
                return cursorElements.get(index);
            }

            @Override
            public int size() {
                return cursorElements.size();
            }

            @Override
            public String toString() {
                return "Custom cursor of " + cursorElements;
            }
        };

        page = packages.findByHeightGreaterThan(20.0f, PageRequest
                        .ofPage(3)
                        .size(2)
                        .withoutTotal()
                        .beforeCursor(cursor));

        assertEquals(3L, page.pageRequest().page());

        assertIterableEquals(List.of(233, 220),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(true, page.hasPrevious());
        page = packages.findByHeightGreaterThan(20.0f, page.previousPageRequest());

        assertEquals(2L, page.pageRequest().page());

        assertIterableEquals(List.of(236, 224),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(true, page.hasPrevious());
        page = packages.findByHeightGreaterThan(20.0f, page.previousPageRequest());

        assertEquals(1L, page.pageRequest().page());

        assertIterableEquals(List.of(215, 210),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(true, page.hasPrevious());
        page = packages.findByHeightGreaterThan(20.0f, page.previousPageRequest());

        assertEquals(1L, page.pageRequest().page()); // page numbers cannot go to 0 or negative

        assertIterableEquals(List.of(230, 228),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(false, page.hasPrevious());

        // Switch directions and expect a page 2 next

        page = packages.findByHeightGreaterThan(20.0f, page.nextPageRequest());

        assertEquals(2L, page.pageRequest().page());

        assertIterableEquals(List.of(215, 210),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.save(new Package(216, 15.0f, 50.0f, 61.0f, "package#216"));

        // With dynamically specified Sorts: width ascending, length descending, id ascending

        // id   length width  height
        // 240, 40.0f, 21.0f, 42.0f // page 3
        // 220, 20.0f, 22.0f, 38.0f // non-matching
        // 233, 33.0f, 32.0f, 43.0f // page 4
        // 224, 24.0f, 32.0f, 39.0f // page 4
        // 219, 19.0f, 39.0f, 19.0f // page 4
        // 236, 36.0f, 50.0f, 93.0f // page 4
        // 215, 15.0f, 50.0f, 55.0f // page 5
        // 216, 15.0f, 50.0f, 61.0f // page 5
        // 210, 10.0f, 50.0f, 55.0f // page 5
        // 228, 28.0f, 62.0f, 87.0f // page 5
        // 230, 30.0f, 81.0f, 88.0f // starting point for beforeKey

        Package p230 = packages.findById(230).orElseThrow();

        Order<Package> sorts = Order.by(Sort.asc("width"), Sort.desc("length"), Sort.asc("id"));
        PageRequest pagination = PageRequest
                        .ofPage(5)
                        .size(4)
                        .withTotal()
                        .beforeCursor(Cursor.forKey(p230.width, p230.length, p230.id));
        page = packages.findByHeightLessThanOrHeightGreaterThan(20.0f, 38.5f, sorts, pagination);

        assertEquals(5L, page.pageRequest().page());

        assertIterableEquals(List.of(215, 216, 210, 228),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(4, page.numberOfElements());

        page = packages.findByHeightLessThanOrHeightGreaterThan(20.0f, 38.5f, sorts, page.previousPageRequest());

        assertEquals(4L, page.pageRequest().page());

        assertIterableEquals(List.of(233, 224, 219, 236),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(4, page.numberOfElements());

        page = packages.findByHeightLessThanOrHeightGreaterThan(20.0f, 38.5f, sorts, page.previousPageRequest());

        assertEquals(3L, page.pageRequest().page());

        assertIterableEquals(List.of(240),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(1, page.numberOfElements());

        assertEquals(false, page.hasPrevious());
        PageRequest next = page.nextPageRequest();
        assertNotNull(next);
        assertEquals(4L, next.page());

        page = packages.findByHeightLessThanOrHeightGreaterThan(20.0f, 38.5f, sorts, next);

        assertEquals(4L, page.pageRequest().page());

        assertIterableEquals(List.of(233, 224, 219, 236),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

    }

    /**
     * Access pages in the previous page direction while entities are being added
     * and removed, using a cursor to avoid duplicates.
     */
    @Test
    public void testCursorPreviousWithUpdates() {
        packages.deleteAll();

        // using @OrderBy width descending, height ascending, id descending:

        packages.saveAll(List.of(new Package(379, 44.0f, 70.0f, 31.0f, "package#379"), // page 1
                                 new Package(376, 33.0f, 70.0f, 36.0f, "package#376"), // page 1
                                 new Package(373, 22.0f, 70.0f, 37.0f, "package#373"), // page 1
                                 new Package(370, 88.0f, 70.0f, 37.0f, "package#370"), // page 2
                                 new Package(350, 55.0f, 50.0f, 30.0f, "package#350"), // page 2, then deleted after page 2 read
                                 // will add 351, 22.0f, 50.0f, 31.0f, "package#351"   // page 2
                                 new Package(355, 44.0f, 50.0f, 35.0f, "package#355"), // page 3
                                 // will add 336, 66.0f, 33.0f, 30.0f, "package#336"
                                 new Package(333, 36.0f, 33.0f, 30.0f, "package#333"), // page 3, then deleted after page 2 read
                                 new Package(330, 33.0f, 33.0f, 30.0f, "package#330"), // page 3
                                 // will add 315, 66.0f, 31.0f, 37.0f, "package#315"
                                 new Package(310, 55.0f, 10.0f, 31.0f, "package#310")));

        CursoredPage<Package> page;

        // Page 3
        page = packages.findByHeightGreaterThan(20.0f, PageRequest
                        .ofPage(3)
                        .size(3)
                        .withoutTotal()
                        .beforeCursor(Cursor.forKey(10.0f, 31.0f, 310)));

        assertEquals(3L, page.pageRequest().page());

        assertIterableEquals(List.of(355, 333, 330),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.saveAll(List.of(new Package(351, 22.0f, 50.0f, 31.f, "package#351"),
                                 new Package(336, 66.0f, 33.0f, 30.f, "package#336"),
                                 new Package(315, 66.0f, 31.0f, 37.f, "package#315")));

        // Page 2
        page = packages.findByHeightGreaterThan(20.0f, page.previousPageRequest());

        assertEquals(2L, page.pageRequest().page());

        assertIterableEquals(List.of(370, 350, 351),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteByIdIn(List.of(350, 333));

        // Page 1
        page = packages.findByHeightGreaterThan(20.0f, page.previousPageRequest());

        assertEquals(1L, page.pageRequest().page());

        assertIterableEquals(List.of(379, 376, 373),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(false, page.hasPrevious());

        // With dynamically specified Sorts: height ascending, length descending, id ascending

        // id   length width  height
        // 336, 66.0f, 33.0f, 30.0f // will be deleted
        // 330, 33.0f, 33.0f, 30.0f // page 2
        // 310, 55.0f, 10.0f, 31.0f // page 2
        // 379, 44.0f, 70.0f, 31.0f // page 3
        // 331, 33.0f, 41.0f, 31.0f // will be added (page 3)
        // 351, 22.0f, 50.0f, 31.0f // page 4
        // 355, 44.0f, 50.0f, 35.0f // non-matching
        // 376, 33.0f, 70.0f, 36.0f // will be deleted
        // 370, 88.0f, 70.0f, 37.0f // page 4
        // 315, 66.0f, 31.0f, 37.0f // page 5, then deleted
        // 373, 22.0f, 70.0f, 37.0f // page 5, then deleted

        // Page 5
        Order<Package> sorts = Order.by(Sort.asc("height"), Sort.desc("length"), Sort.asc("id"));
        page = packages.findByHeightLessThanOrHeightGreaterThan(32.0f, 35.5f, sorts,
                                                                PageRequest.ofPage(5)
                                                                                .size(2)
                                                                                .beforeCursor(Cursor.forKey(40.0f, 0.0f, 0)));

        assertEquals(5L, page.pageRequest().page());

        assertIterableEquals(List.of(315, 373),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteByIdIn(List.of(373, 315, 376));

        page = packages.findByHeightLessThanOrHeightGreaterThan(32.0f, 35.5f, sorts, page.previousPageRequest());

        assertEquals(4L, page.pageRequest().page());

        assertIterableEquals(List.of(351, 370),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.save(new Package(331, 33.0f, 41.0f, 31.0f, "package#351"));

        page = packages.findByHeightLessThanOrHeightGreaterThan(32.0f, 35.5f, sorts, page.previousPageRequest());

        assertEquals(3L, page.pageRequest().page());

        assertIterableEquals(List.of(379, 331),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        page = packages.findByHeightLessThanOrHeightGreaterThan(32.0f, 35.5f, sorts, page.previousPageRequest());

        assertEquals(2L, page.pageRequest().page());

        assertIterableEquals(List.of(330, 310),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        PageRequest previous = page.previousPageRequest();
        assertNotNull(previous);

        // delete the only previous entry and visit the empty previous page

        packages.deleteById(336);

        page = packages.findByHeightLessThanOrHeightGreaterThan(32.0f, 35.5f, sorts, page.previousPageRequest());

        assertEquals(1L, page.pageRequest().page());

        assertIterableEquals(Collections.EMPTY_LIST, page.content());

        // attempt next after an empty page
        assertEquals(false, page.hasNext());
        assertEquals(false, page.hasPrevious());

        try {
            previous = page.previousPageRequest();
            fail("Page.previousPageRequest must raise NoSuchElementException when hasPrevious returns false. Instead: " + previous);
        } catch (NoSuchElementException x) {
            // expected
        }
    }

    @Test //TODO
    @Ignore("Reference issue: https://github.com/OpenLiberty/open-liberty/issues/29475")
    public void testFetchTypeDefault() {
        ratings.clear();

        Rating.Reviewer.Name name1 = new Rating.Reviewer.Name("Rex", "TestFetchTypeDefault");
        Rating.Reviewer user1 = new Rating.Reviewer(name1, "rex@openliberty.io");
        Rating.Item toaster = new Rating.Item("toaster", 28.98f);
        Set<String> comments = Set.of("Burns everything.", "Often gets stuck.", "Bagels don't fit.");

        ratings.add(new Rating(1000, toaster, 2, user1, comments));

        Rating user1Rating = ratings.get(1000).orElseThrow();

        assertFalse("Expected comments to be populated when using fetch type eager", user1Rating.comments().isEmpty());
        assertEquals("Expected comments to be populated when using fetch type eager", comments, user1Rating.comments());
    }

    /**
     * A repository might attempt to define a method that returns a CursoredPage
     * without specifying a PageRequest and attempt to use a Limit parameter
     * instead. This is not supported by the spec.
     * Expect UnsupportedOperationException.
     */
    @Test
    public void testLacksPageRequestUseLimitInstead() {
        CursoredPage<Prime> page;
        try {
            page = primes.findByNumberIdBetween(15L, 45L, Limit.of(5));
            fail("Able to obtain CursoredPage without a PageRequest: " + page);
        } catch (UnsupportedOperationException x) {
            // pass
        }
    }

    /**
     * A repository might attempt to define a method that returns a CursoredPage
     * without specifying a PageRequest and attempt to use a Sort parameter instead.
     * This is not supported by the spec. Expect UnsupportedOperationException.
     */
    @Test
    public void testLacksPageRequestUseSortInstead() {
        CursoredPage<Prime> page;
        try {
            page = primes.findByNumberIdBetweenAndBinaryDigitsNotNull(30L, //
                                                                      40L, //
                                                                      Sort.asc(ID));
            fail("Able to obtain CursoredPage without a PageRequest: " + page);
        } catch (UnsupportedOperationException x) {
            // pass
        }
    }

    /**
     * Use a repository method that performs a JDQL query using LEFT function
     * to obtain the beginning of a String value.
     */
    @Test
    public void testLeftFunction() {
        assertEquals(List.of("seven", "seventeen"),
                     primes.matchLeftSideOfName("seven"));
    }

    /**
     * Repository method with return type of LongStream, involving type conversion.
     */
    @Test
    public void testLongStream() {
        assertEquals(List.of(10L, 11L, 101L, 111L, 1011L, 1101L, 10001L, 10011L),
                     primes.binaryDigitsAsDecimal(20)
                                     .boxed()
                                     .collect(Collectors.toList()));
    }

    /**
     * Intermix two different types of entities in the same transaction.
     */
    @Test
    public void testMixedEntitiesInTransaction() throws HeuristicMixedException, HeuristicRollbackException, //
                    IllegalStateException, NotSupportedException, RollbackException, SecurityException, SystemException {
        houses.dropAll();
        vehicles.removeAll();

        House h1 = new House();
        h1.area = 1900;
        h1.lotSize = 0.19f;
        h1.numBedrooms = 4;
        h1.parcelId = "111-222-333";
        h1.purchasePrice = 219000.00f;
        h1.sold = Year.of(2019);

        Vehicle v1 = new Vehicle();
        v1.make = "Nissan";
        v1.model = "Altima";
        v1.numSeats = 5;
        v1.price = 24000f;
        v1.vinId = "TME09876543210001";

        House h;
        Vehicle v;
        tran.begin();
        try {
            h = houses.save(h1).get(0);
            v = vehicles.save(List.of(v1)).iterator().next();
        } finally {
            tran.commit();
        }

        assertEquals(Year.of(2019), h.sold);
        assertEquals(24000f, v.price, 0.001f);

        // Make updates and roll back
        tran.begin();
        try {
            h1.purchasePrice = 222000f;
            h1.sold = Year.of(2020);
            houses.save(h1);
            assertEquals(true, vehicles.updateByVinIdAddPrice("TME09876543210001", 200f));
        } finally {
            tran.rollback();
        }

        // Ensure all updates were rolled back
        h = houses.findById("111-222-333");
        v = vehicles.findByVinId("TME09876543210001").get();

        assertEquals(219000f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2019), h.sold);
        assertEquals(24000f, v.price, 0.001f);

        // Make updates and commit
        tran.begin();
        try {
            h1.purchasePrice = 241000f;
            h1.sold = Year.of(2021);
            houses.save(h1);
            assertEquals(true, vehicles.updateByVinIdAddPrice("TME09876543210001", 2000f));
        } finally {
            tran.commit();
        }

        // Ensure all updates were committed
        h = houses.findById("111-222-333");
        v = vehicles.findByVinId("TME09876543210001").get();

        assertEquals(241000f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2021), h.sold);
        assertEquals(26000f, v.price, 0.001f);

        houses.dropAll();
        vehicles.removeAll();
    }

    /**
     * Verify a repository method can intermix Sort parameters and Order with Sort parameters.
     */
    @Test
    public void testMixSortParamsAndOrder() {

        Page<Prime> page = primes.findByRomanNumeralEndsWithAndNumberIdLessThan("I", 45L,
                                                                                PageRequest.ofSize(6),
                                                                                Order.by(Sort.asc("sumOfBits")),
                                                                                Sort.desc("name"));
        assertEquals(List.of("two", // sum of bits: 1
                             "three", "seventeen", // sum of bits: 2
                             "thirty-seven", "thirteen", "seven"), // sum of bits: 3
                     page.stream()
                                     .map(p -> p.name)
                                     .collect(Collectors.toList()));

        page = primes.findByRomanNumeralEndsWithAndNumberIdLessThan("I", 45L,
                                                                    page.nextPageRequest(),
                                                                    Order.by(Sort.asc("sumOfBits")),
                                                                    Sort.desc("name"));

        assertEquals(List.of("forty-one", "eleven", // sum of bits: 3
                             "twenty-three", "forty-three", // sum of bits: 4
                             "thirty-one"), // sum of bits: 5
                     page.stream()
                                     .map(p -> p.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Various return types for a repository query that performs multiple aggregate functions.
     */
    @Test
    public void testMultipleAggregates() {
        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
        boolean databaseRounds = jdbcJarName.startsWith("ojdbc") || jdbcJarName.startsWith("postgre");

        Object[] objects = primes.minMaxSumCountAverageObjectArray(50);
        assertEquals(Long.valueOf(2L), objects[0]); // minimum
        assertEquals(Long.valueOf(47L), objects[1]); // maximum
        assertEquals(Long.valueOf(328L), objects[2]); // sum
        assertEquals(Long.valueOf(15L), objects[3]); // count
        assertEquals(true, objects[4] instanceof Number); // average
        assertEquals(21.0, Math.floor(((Number) objects[4]).doubleValue()), 0.01);

        Number[] numbers = primes.minMaxSumCountAverageNumberArray(45);
        assertEquals(Long.valueOf(2L), numbers[0]); // minimum
        assertEquals(Long.valueOf(43L), numbers[1]); // maximum
        assertEquals(Long.valueOf(281L), numbers[2]); // sum
        assertEquals(Long.valueOf(14L), numbers[3]); // count
        assertEquals(20.0, Math.floor(numbers[4].doubleValue()), 0.01);

        Long[] longs = primes.minMaxSumCountAverageLong(42);
        assertEquals(Long.valueOf(2L), longs[0]); // minimum
        assertEquals(Long.valueOf(41L), longs[1]); // maximum
        assertEquals(Long.valueOf(238L), longs[2]); // sum
        assertEquals(Long.valueOf(13L), longs[3]); // count
        assertEquals(Long.valueOf(18L), longs[4]); // average

        int[] ints = primes.minMaxSumCountAverageInt(40);
        assertEquals(2, ints[0]); // minimum
        assertEquals(37, ints[1]); // maximum
        assertEquals(197, ints[2]); // sum
        assertEquals(12, ints[3]); // count
        assertEquals(16, ints[4]); // average

        try {
            float[] floats = primes.minMaxSumCountAverageFloat(35);
            fail("Allowed unsafe conversion from double to float: " +
                 Arrays.toString(floats));
        } catch (MappingException x) {
            if (x.getMessage().startsWith("CWWKD1046E") &&
                x.getMessage().contains("float[]"))
                ; // unsafe to convert double to float
            else
                throw x;
        }

        List<Long> list = primes.minMaxSumCountAverageList(30);
        assertEquals(Long.valueOf(2L), list.get(0)); // minimum
        assertEquals(Long.valueOf(29L), list.get(1)); // maximum
        assertEquals(Long.valueOf(129L), list.get(2)); // sum
        assertEquals(Long.valueOf(10L), list.get(3)); // count
        if (databaseRounds)
            assertEquals(Long.valueOf(13L), list.get(4)); // average - 12.9 -> 13
        else
            assertEquals(Long.valueOf(12L), list.get(4)); // average - 12.9 -> 12

        Stack<String> stack = primes.minMaxSumCountAverageStack(25);
        assertEquals("2", stack.get(0)); // minimum
        assertEquals("23", stack.get(1)); // maximum
        assertEquals("100", stack.get(2)); // sum
        assertEquals("9", stack.get(3)); // count
        assertEquals(stack.get(4), true, stack.get(4).startsWith("11.")); // average

        Iterable<Integer> iterable = primes.minMaxSumCountAverageIterable(20);
        Iterator<Integer> it = iterable.iterator();
        assertEquals(Integer.valueOf(2), it.next()); // minimum
        assertEquals(Integer.valueOf(19), it.next()); // maximum
        assertEquals(Integer.valueOf(77), it.next()); // sum
        assertEquals(Integer.valueOf(8), it.next()); // count
        if (databaseRounds)
            assertEquals(Integer.valueOf(10), it.next()); // average - 9.625 -> 10
        else
            assertEquals(Integer.valueOf(9), it.next()); // average - 9.625 -> 9

        Deque<Double> deque = primes.minMaxSumCountAverageDeque(18);
        assertEquals(2.0, deque.removeFirst(), 0.01); // minimum
        assertEquals(17.0, deque.removeFirst(), 0.01); // maximum
        assertEquals(58.0, deque.removeFirst(), 0.01); // sum
        assertEquals(7.0, deque.removeFirst(), 0.01); // count
        assertEquals(8.0, Math.floor(deque.removeFirst()), 0.01); // average

        List<Number> numberList = primes.minMaxSumCountAverageNumberList(15);
        assertEquals(Long.valueOf(2L), numberList.get(0)); // minimum
        assertEquals(Long.valueOf(13L), numberList.get(1)); // maximum
        assertEquals(Long.valueOf(41L), numberList.get(2)); // sum
        assertEquals(Long.valueOf(6L), numberList.get(3)); // count
        assertEquals(6.0, Math.floor(numberList.get(4).doubleValue()), 0.01);

        List<Object> objectList = primes.minMaxSumCountAverageObjectList(10);
        assertEquals(Long.valueOf(2L), objectList.get(0)); // minimum
        assertEquals(Long.valueOf(7L), objectList.get(1)); // maximum
        assertEquals(Long.valueOf(17L), objectList.get(2)); // sum
        assertEquals(Long.valueOf(4L), objectList.get(3)); // count
        assertEquals(true, objectList.get(4) instanceof Number); // average
        assertEquals(4.0, Math.floor(((Number) objectList.get(4)).doubleValue()), 0.01);
    }

    /**
     * Use a repository that has multiple embeddable attributes of the same type.
     */
    @Test
    public void testMultipleEmbeddableAttributesOfSameType() {
        Cylinder cyl1, cyl2, cyl3, cyl4, cyl5;

        //                                    Id     a.x, a.y, b.x, b.y, c.x, c.y
        cylinders.upsert(cyl1 = new Cylinder("CYL1", 100, 287, 372, 833, 509, 424),
                         cyl2 = new Cylinder("CYL2", 790, 857, 942, 143, 509, 424),
                         cyl3 = new Cylinder("CYL3", 340, 101, 100, 919, 629, 630),
                         cyl4 = new Cylinder("CYL4", 100, 684, 974, 516, 453, 163),
                         cyl5 = new Cylinder("CYL5", 412, 983, 276, 413, 629, 630));

        assertEquals(5, cylinders.countValid());

        assertEquals(List.of(cyl5.toString(), cyl3.toString()),
                     cylinders.centeredAt(629, 630)
                                     .map(Object::toString)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(cyl2.toString(), cyl1.toString()),
                     cylinders.centeredAt(509, 424)
                                     .map(Object::toString)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(cyl3.toString(), cyl1.toString(), cyl4.toString()),
                     cylinders.findBySideAXOrSideBXOrderBySideBYDesc(100, 100)
                                     .map(Object::toString)
                                     .collect(Collectors.toList()));

        assertEquals(Long.valueOf(5), cylinders.eraseAll());
    }

    /**
     * Use a repository where methods are for different entities.
     */
    @Test
    public void testMultipleEntitiesInARepository() {
        // Remove any pre-existing data that could interfere with the test:
        products.clear();
        packages.deleteAll();
        personnel.removeAll().join();

        Product prod = new Product();
        prod.pk = UUID.nameUUIDFromBytes("TestMultipleEntitiesInARepository".getBytes());
        prod.name = "TestMultipleEntitiesInARepository-Product";
        prod.price = 30.99f;

        prod = multi.create(prod);

        Person p1 = new Person();
        p1.firstName = "Michael";
        p1.lastName = "TestMultipleEntitiesInARepository";
        p1.ssn_id = 908070605;

        Person p2 = new Person();
        p2.firstName = "Mark";
        p2.lastName = "TestMultipleEntitiesInARepository";
        p2.ssn_id = 807060504;

        Person p3 = new Person();
        p3.firstName = "Maria";
        p3.lastName = "TestMultipleEntitiesInARepository";
        p3.ssn_id = 706050403;

        List<Person> added = multi.add(p1, p2, p3);
        assertEquals(List.of("Michael", "Mark", "Maria"),
                     added.stream()
                                     .map(p -> p.firstName)
                                     .collect(Collectors.toList()));

        Package pkg = multi.upsert(new Package(60504, 30.0f, 20.0f, 10.0f, "TestMultipleEntitiesInARepository-Package"));
        assertEquals(60504, pkg.id);
        assertEquals(30.0f, pkg.length, 0.001f);
        assertEquals(20.0f, pkg.width, 0.001f);
        assertEquals(10.0f, pkg.height, 0.001f);
        assertEquals("TestMultipleEntitiesInARepository-Package", pkg.description);

        pkg.length = 33.0f;
        pkg.width = 22.0f;
        pkg = multi.upsert(pkg);
        assertEquals(33.0f, pkg.length, 0.001f);
        assertEquals(22.0f, pkg.width, 0.001f);

        pkg = multi.findById(60504).orElseThrow();
        assertEquals(60504, pkg.id);
        assertEquals(33.0f, pkg.length, 0.001f);
        assertEquals(22.0f, pkg.width, 0.001f);
        assertEquals(10.0f, pkg.height, 0.001f);
        assertEquals("TestMultipleEntitiesInARepository-Package", pkg.description);

        assertEquals(true, multi.remove(added.get(2)));

        assertEquals(true, multi.deleteById(908070605l).isPresent());
        assertEquals(true, multi.deleteById(807060504l).isPresent());
        assertEquals(false, multi.deleteById(706050403l).isPresent());

        prod.name = "Test-Multiple-Entities-In-A-Repository-Product";
        assertEquals(1, multi.modify(prod));

        prod = products.remove(prod.pk);
        assertEquals("Test-Multiple-Entities-In-A-Repository-Product", prod.name);

        packages.delete(pkg);
    }

    /**
     * Use a repository query with named parameters, where the parameters are obtained from
     * the corresponding method parameters based on the method's parameter names.
     */
    @Test
    public void testNamedParametersFromMethodParameterNames() {
        assertArrayEquals(new long[] { 19, 29, 43, 47 },
                          primes.matchAny(19, "XLVII", "2B", "twenty-nine"));
    }

    /**
     * Use a repository query with named parameters, where the parameters are
     * sometimes obtained from the Param annotation and other times obtained from
     * the corresponding method parameters based on the method's parameter names.
     */
    @Test
    public void testNamedParametersMixingAnnotationAndParameterNames() {
        assertEquals(List.of(5L, 13L, 29L, 31L),
                     primes.matchAnyWithMixedUsageOfParamAnnotation(31, "thirteen", "V", "1D")
                                     .sorted()
                                     .collect(Collectors.toList()));
    }

    /**
     * Test Null and NotNull on repository methods.
     */
    @Test
    public void testNulls() {
        assertIterableEquals(List.of(4001L, 4003L, 4007L),
                             primes.findByNumberIdInAndRomanNumeralNull(Set.of(41L, 4001L, 43L, 4003L, 47L, 4007L))
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(41L, 43L, 47L),
                             primes.findByNumberIdInAndRomanNumeralNotNull(Set.of(41L, 4001L, 43L, 4003L, 47L, 4007L))
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));
    }

    /**
     * Verify a repository method that supplies id(this) as the sort criteria
     * hard coded within a JDQL query.
     */
    @Test
    public void testOrderByIdFunction() {
        assertIterableEquals(List.of(19L, 17L, 13L, 11L, 7L, 5L, 3L, 2L),
                             primes.below(20L));
    }

    /**
     * Verify that a repository method with return type of Set preserves the order of iteration,
     * (in this case descending sort on id) which is possible by using LinkedHashSet.
     */
    @Test
    public void testOrderedSet() {
        assertIterableEquals(List.of(47L, 43L, 41L, 37L, 31L, 29L, 23L),
                             primes.findNumberIdByNumberIdBetween(20, 49));
    }

    /**
     * Exceed the maximum offset allowed by JPA.
     */
    @Test
    public void testOverflow() {
        Limit range = Limit.range(Integer.MAX_VALUE + 5L, Integer.MAX_VALUE + 10L);
        try {
            List<Prime> found = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(9L, range);
            fail("Expected an error because starting position of range exceeds Integer.MAX_VALUE. Found: " + found);
        } catch (IllegalArgumentException x) {
            // expected
        }

        try {
            Stream<Prime> found = primes.findFirst2147483648ByNumberIdGreaterThan(1L);
            fail("Expected an error because limit exceeds Integer.MAX_VALUE. Found: " + found);
        } catch (UnsupportedOperationException x) {
            // expected
        }

        try {
            CursoredPage<Prime> found = primes.findByNumberIdBetween(5L, 15L, PageRequest.ofPage(33).size(Integer.MAX_VALUE / 30));
            fail("Expected an error because when offset for pagination exceeds Integer.MAX_VALUE. Found: " + found);
        } catch (IllegalArgumentException x) {
            // expected
        }

        try {
            Page<Prime> found = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(52L, PageRequest.ofPage(22).size(Integer.MAX_VALUE / 20));
            fail("Expected an error because when offset for pagination exceeds Integer.MAX_VALUE. Found: " + found);
        } catch (IllegalArgumentException x) {
            // expected
        }
    }

    /**
     * Repository method where the page request type (Prime entity) differs
     * from the data type of the page that is returned (String) due to the use
     * of query language that asks for results to be returned a String
     * (one component of the entity).
     */
    @Test
    public void testPageRequestTypeDiffersFromResultType() {
        PageRequest page1Request = PageRequest.ofPage(1).size(4).withoutTotal();
        Page<String> page1 = primes.namesBelow(35, _Prime.numberId.desc(), page1Request);

        assertEquals(List.of("thirty-one", "twenty-nine", "twenty-three", "nineteen"),
                     page1.content());

        PageRequest page2Request = page1.nextPageRequest();
        Page<String> page2 = primes.namesBelow(35, _Prime.numberId.desc(), page2Request);

        assertEquals(List.of("seventeen", "thirteen", "eleven", "seven"),
                     page2.content());

        PageRequest page3Request = page2.nextPageRequest();
        Page<String> page3 = primes.namesBelow(35, _Prime.numberId.desc(), page3Request);

        assertEquals(List.of("five", "three", "two"),
                     page3.content());

        assertEquals(page3Request, page3.pageRequest());
        assertEquals(page2Request, page2.pageRequest());
        assertEquals(page1Request, page1.pageRequest());

        // Re-request the second page
        assertEquals(List.of("seventeen", "thirteen", "eleven", "seven"),
                     primes.namesBelow(35, _Prime.numberId.desc(), page2.pageRequest())
                                     .content());
    }

    /***
     * Covers the page.nextPageRequest() and page.pageRequest() methods
     * when the result type matches the entity class.
     */
    @Test
    public void testPageRequestTypeMatchesResultType() {
        Order<Prime> descId = Order.by(_Prime.numberId.desc());
        PageRequest page1Request = PageRequest.ofSize(5).withoutTotal();
        CursoredPage<Prime> page1 = primes.findByNumberIdBetweenAndEvenFalse(20, 50, page1Request, descId);

        assertEquals(List.of(47L, 43L, 41L, 37L, 31L),
                     page1.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        // Specifying the entity type here is unnecessary, but should still work
        PageRequest page2Request = page1.nextPageRequest();
        CursoredPage<Prime> page2 = primes.findByNumberIdBetweenAndEvenFalse(20, 50, page2Request, descId);

        assertEquals(List.of(29L, 23L),
                     page2.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        assertEquals(page2Request, page2.pageRequest());

        assertEquals(false, page2.hasNext());

        try {
            PageRequest page3Request = page2.nextPageRequest();
            fail("Page.nextPageRequest must raise NoSuchElementException when hasNext returns false. Instead: " + page3Request);
        } catch (NoSuchElementException x) {
            // expected
        }
    }

    /**
     * Repository method that uses offset pagination but does not specify
     * any sort criteria, which should default to ascending by Id.
     */
    @Test
    public void testPageRequestWithImplicitSort() {
        PageRequest page1req = PageRequest.ofSize(4);
        Page<Prime> page;
        page = primes.findByRomanNumeralIgnoreCaseStartsWith("X", page1req);

        assertEquals(List.of(11L, 13L, 17L, 19L),
                     page.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        PageRequest page2req = page.nextPageRequest();

        page = primes.findByRomanNumeralIgnoreCaseStartsWith("x", page2req);

        assertEquals(List.of(23L, 29L, 31L, 37L),
                     page.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a method with a prefix for Query by Method Name and a parameter annotation
     * that indicates Parameter-based Query. Verify the latter takes precedence when
     * interpreting the method.
     */
    @Test
    public void testParameterAnnotationTakesPrecedenceOverMethodPrefix() {
        Prime nine = primes.findByBinary("10011").orElseThrow();
        assertEquals(19L, nine.numberId);
    }

    /**
     * Tests entity attribute names from embeddables and MappedSuperclass that
     * can have delimiters. Includes tests for name collisions with attributes from an
     * embeddable or superinteface. This test uses unannotated entities.
     */
    @Test
    public void testPersistentFieldNamesAndDelimiters() {
        apartments.removeAll();

        Apartment a101 = new Apartment();
        a101.occupant = new Occupant();
        a101.occupant.firstName = "Kyle";
        a101.occupant.lastName = "Smith";
        a101.isOccupied = true;
        a101.APTID = 101L;
        a101.quarters = new Bedroom();
        a101.quarters.length = 10;
        a101.quarters.width = 10;
        a101.quartersWidth = 15;

        Apartment a102 = new Apartment();
        a102.occupant = new Occupant();
        a102.occupant.firstName = "Brent";
        a102.occupant.lastName = "Smith";
        a102.isOccupied = false;
        a102.APTID = 102L;
        a102.quarters = new Bedroom();
        a102.quarters.length = 11;
        a102.quarters.width = 11;
        a102.quartersWidth = 15;

        Apartment a103 = new Apartment();
        a103.occupant = new Occupant();
        a103.occupant.firstName = "Brian";
        a103.occupant.lastName = "Smith";
        a103.isOccupied = false;
        a103.APTID = 103L;
        a103.quarters = new Bedroom();
        a103.quarters.length = 11;
        a103.quarters.width = 12;
        a103.quartersWidth = 15;

        Apartment a104 = new Apartment();
        a104.occupant = new Occupant();
        a104.occupant.firstName = "Scott";
        a104.occupant.lastName = "Smith";
        a104.isOccupied = false;
        a104.APTID = 104L;
        a104.quarters = new Bedroom();
        a104.quarters.length = 12;
        a104.quarters.width = 11;
        a104.quartersWidth = 15;

        apartments.saveAll(List.of(a101, a102, a103, a104));

        List<Apartment> results;

        results = apartments.findApartmentsByBedroomWidth(12);
        assertEquals(1, results.size());
        assertEquals("Brian", results.get(0).occupant.firstName);

        results = apartments.findApartmentsByBedroom(11, 11);
        assertEquals(1, results.size());
        assertEquals("Brent", results.get(0).occupant.firstName);

        results = apartments.findAllOrderByBedroomLength();
        assertEquals(4, results.size());
        assertEquals("Kyle", results.get(0).occupant.firstName);
        assertEquals("Scott", results.get(3).occupant.firstName);

        results = apartments.findAllOrderByBedroomWidth();
        assertEquals(4, results.size());
        assertEquals("Kyle", results.get(0).occupant.firstName);
        assertEquals("Brian", results.get(3).occupant.firstName);

        results = apartments.findApartmentsByBedroomLength(10);
        assertEquals(1, results.size());
        assertEquals("Kyle", results.get(0).occupant.firstName);

        results = apartments.findByQuarters_Width(12);
        assertEquals(1, results.size());
        assertEquals("Brian", results.get(0).occupant.firstName);

        results = apartments.findByQuartersLength(12);
        assertEquals(1, results.size());
        assertEquals("Scott", results.get(0).occupant.firstName);

        results = apartments.findAllSorted(Sort.asc("quarters.length"));
        assertEquals(4, results.size());
        assertEquals("Kyle", results.get(0).occupant.firstName);
        assertEquals("Scott", results.get(3).occupant.firstName);

        results = apartments.findAllSorted(Sort.asc("quarters_width"));
        assertEquals(4, results.size());
        assertEquals("Kyle", results.get(0).occupant.firstName);
        assertEquals("Brian", results.get(3).occupant.firstName);

        results = apartments.findByOccupied(true);
        assertEquals(1, results.size());
        assertEquals("Kyle", results.get(0).occupant.firstName);

        results = apartments.findByOccupantLastNameOrderByFirstName("Smith");
        assertEquals(4, results.size());
        assertEquals("Brent", results.get(0).occupant.firstName);
        assertEquals("Brian", results.get(1).occupant.firstName);
        assertEquals("Kyle", results.get(2).occupant.firstName);
        assertEquals("Scott", results.get(3).occupant.firstName);

        // Colliding non-delimited attribute name quartersWidth, ensure we use entity attribute and not embedded attribute for query
        results = apartments.findByQuartersWidth(15);
        assertEquals(4, results.size());
        assertEquals("Brent", results.get(0).occupant.firstName);
        assertEquals("Brian", results.get(1).occupant.firstName);
        assertEquals("Kyle", results.get(2).occupant.firstName);
        assertEquals("Scott", results.get(3).occupant.firstName);

        try {
            apartments.findAllCollidingEmbeddable();
            fail("Should not have been able to execute query on an entity with colliding attibute name from embeddable");
        } catch (MappingException e) {
            //expected
        }

        try {
            apartments.findAllCollidingSuperclass();
            fail("Should not have been able to execute query on an entity with colliding attibute name from superclass");
        } catch (MappingException e) {
            // expected
        }
    }

    /**
     * Use a repository query with positional parameters and a literal value that looks like a named parameter, but isn't.
     */
    @Test
    public void testPositionalParameterWithLiteralThatLooksLikeANamedParameter() {
        assertEquals(List.of("thirty-seven", "forty-one"), // ordered by numeric value
                     primes.matchAnyExceptLiteralValueThatLooksLikeANamedParameter(41, "thirty-seven"));

        assertEquals(List.of("forty-one", "thirty-seven"), // ordered by name
                     primes.matchAnyExceptLiteralValueThatLooksLikeANamedParameter("thirty-seven", 41));
    }

    /**
     * Use Query by Method Name repository methods that lack the By keyword.
     */
    @Test
    public void testQueryByMethodNameWithoutBy() {
        vehicles.delete();

        Vehicle v1 = new Vehicle();
        v1.make = "Ford";
        v1.model = "Explorer";
        v1.numSeats = 7;
        v1.price = 37000f;
        v1.vinId = "QBYM0000101010101";

        Vehicle v2 = new Vehicle();
        v2.make = "Subaru";
        v2.model = "Outback";
        v2.numSeats = 5;
        v2.price = 29000f;
        v2.vinId = "QBYM0000202020202";

        Vehicle v3 = new Vehicle();
        v3.make = "Honda";
        v3.model = "CR-V";
        v3.numSeats = 5;
        v3.price = 30000f;
        v3.vinId = "QBYM0000303030303";

        Vehicle v4 = new Vehicle();
        v4.make = "Honda";
        v4.model = "HR-V";
        v4.numSeats = 5;
        v4.price = 24000f;
        v4.vinId = "QBYM0000404040404";

        Vehicle v5 = new Vehicle();
        v5.make = "Subaru";
        v5.model = "Impreza";
        v5.numSeats = 5;
        v5.price = 23000f;
        v5.vinId = "QBYM0000505050505";

        assertEquals(0L, vehicles.count());
        assertEquals(false, vehicles.exists());
        assertEquals(0L, vehicles.find().count());
        assertEquals(false, vehicles.findFirstOrderByVinId().isPresent());
        assertEquals(List.of(), vehicles.findOrderByMakeAscModelAscVinIdAsc());

        vehicles.save(List.of(v1, v2, v3, v4, v5));

        assertEquals(5L, vehicles.count());
        assertEquals(true, vehicles.exists());
        assertEquals(true, vehicles.existsAny());

        assertEquals("Explorer", vehicles.findFirstOrderByVinId().orElseThrow().model);

        assertEquals(List.of("CR-V", "Explorer", "HR-V", "Impreza", "Outback"),
                     vehicles.find()
                                     .map(v -> v.model)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(List.of("CR-V", "Explorer", "HR-V", "Impreza", "Outback"),
                     vehicles.findAll()
                                     .map(v -> v.model)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(List.of("Ford Explorer", "Honda CR-V", "Honda HR-V", "Subaru Impreza", "Subaru Outback"),
                     vehicles.findOrderByMakeAscModelAscVinIdAsc()
                                     .stream()
                                     .map(v -> v.make + " " + v.model)
                                     .collect(Collectors.toList()));

        assertEquals(List.of("Explorer", "CR-V", "Outback", "HR-V", "Impreza"),
                     vehicles.findAllOrderByPriceDescVinIdAsc()
                                     .stream()
                                     .map(v -> v.model)
                                     .collect(Collectors.toList()));

        assertEquals(List.of("Impreza", "HR-V"),
                     vehicles.deleteFoundOrderByPriceAscVinIdAsc(Limit.of(2))
                                     .stream()
                                     .map(v -> v.model)
                                     .collect(Collectors.toList()));

        assertEquals(3L, vehicles.countEverything());

        assertEquals(List.of("CR-V", "Explorer", "Outback"),
                     vehicles.deleteAll()
                                     .stream()
                                     .map(v -> v.model)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(false, vehicles.existsAny());
        assertEquals(0L, vehicles.findAll().count());
        assertEquals(List.of(), vehicles.findAllOrderByPriceDescVinIdAsc());
    }

    /**
     * Repository Query method that selects and returns a single ArrayList attribute
     */
    @Test
    public void testQueryReturnsArrayListAttribute() {
        assertEquals(new ArrayList<String>(List.of("X", "L", "I")),
                     primes.romanNumeralSymbolsAsArrayList(41).orElseThrow());
    }

    /**
     * Repository Query method that selects a single attribute of type ArrayList
     * and returns it as a Collection.
     */
    @Test
    public void testQueryReturnsArrayListAttributeAsCollection() {
        assertEquals(List.of("X", "X", "X", "V", "I", "I"),
                     primes.romanNumeralSymbolsAsCollection(37).orElseThrow());
    }

    /**
     * Repository Query method that selects and returns a multiple results of an
     * ArrayList attribute
     */
    @Test
    public void testQueryReturnsArrayListAttributeAsListOfArrayList() {
        List<ArrayList<String>> results;
        try {
            results = primes.romanNumeralSymbolsAsListOfArrayList("XL%");
        } catch (UnsupportedOperationException x) {
            if (x.getMessage().startsWith("CWWKD1103E"))
                // work around for bad behavior from EclipseLink when selecting
                // ElementCollection attributes (see #30575)
                return;
            else
                throw x;
        }
        assertEquals(List.of(new ArrayList<String>(List.of("X", "L", "I")),
                             new ArrayList<String>(List.of("X", "L", "I", "I", "I")),
                             new ArrayList<String>(List.of("X", "L", "V", "I", "I"))),
                     results);
    }

    /**
     * Repository Query method that selects and returns a multiple results of an
     * ArrayList attribute as a Set.
     */
    @Test
    public void testQueryReturnsArrayListAttributeAsSetOfArrayList() {
        LinkedHashSet<ArrayList<String>> results;
        try {
            results = primes.romanNumeralSymbolsAsSetOfArrayList("XL%");
        } catch (UnsupportedOperationException x) {
            if (x.getMessage().startsWith("CWWKD1103E"))
                // work around for bad behavior from EclipseLink when selecting
                // ElementCollection attributes (see #30575)
                return;
            else
                throw x;
        }
        assertEquals(Set.of(new ArrayList<String>(List.of("X", "L", "I")),
                            new ArrayList<String>(List.of("X", "L", "I", "I", "I")),
                            new ArrayList<String>(List.of("X", "L", "V", "I", "I"))),
                     results);
    }

    /**
     * Use a Repository method that has the Query annotation and has a return type
     * that uses a Java record indicating to select a subset of entity attributes.
     */
    @Test
    public void testQueryWithRecordResult() {
        assertEquals(List.of("eleven XI ( X I )",
                             "five V ( V )",
                             "nineteen XIX ( X I X )",
                             "seven VII ( V I I )",
                             "seventeen XVII ( X V I I )",
                             "thirteen XIII ( X I I I )",
                             "three III ( I I I )",
                             "two II ( I I )"),
                     primes.romanNumeralsLessThanEq(20)
                                     .stream()
                                     .map(RomanNumeral::toString)
                                     .collect(Collectors.toList()));
    }

    /**
     * Tests all BasicRepository methods with a record as the entity.
     */
    @Test
    public void testRecordBasicRepositoryMethods() {
        receipts.deleteByTotalLessThan(1000000.0f);

        receipts.save(new Receipt(100L, "C0013-00-031", 101.90f));
        receipts.saveAll(List.of(new Receipt(200L, "C0022-00-022", 202.40f),
                                 new Receipt(300L, "C0013-00-031", 33.99f),
                                 new Receipt(400L, "C0045-00-054", 44.49f),
                                 new Receipt(500L, "C0045-00-054", 155.00f)));

        assertEquals(true, receipts.existsByPurchaseId(300L));
        assertEquals(5L, receipts.count());

        Receipt receipt = receipts.findById(200L).orElseThrow();
        assertEquals(202.40f, receipt.total(), 0.001f);

        assertIterableEquals(List.of("C0013-00-031:300", "C0022-00-022:200", "C0045-00-054:500"),
                             receipts.findByPurchaseIdIn(List.of(200L, 300L, 500L))
                                             .map(r -> r.customer() + ":" + r.purchaseId())
                                             .sorted()
                                             .collect(Collectors.toList()));

        receipts.deleteByPurchaseIdIn(List.of(200L, 500L));

        assertIterableEquals(List.of("C0013-00-031:100", "C0013-00-031:300", "C0045-00-054:400"),
                             receipts.findAll()
                                             .map(r -> r.customer() + ":" + r.purchaseId())
                                             .sorted()
                                             .collect(Collectors.toList()));

        receipts.deleteById(100L);

        assertEquals(2L, receipts.count());

        receipts.delete(new Receipt(400L, "C0045-00-054", 44.49f));

        assertEquals(false, receipts.existsByPurchaseId(400L));

        receipts.saveAll(List.of(new Receipt(600L, "C0067-00-076", 266.80f),
                                 new Receipt(700L, "C0067-00-076", 17.99f),
                                 new Receipt(800L, "C0088-00-088", 88.98f)));

        receipts.deleteAll(List.of(new Receipt(300L, "C0013-00-031", 33.99f),
                                   new Receipt(700L, "C0067-00-076", 17.99f)));

        assertEquals(2L, receipts.count());

        assertEquals(true, receipts.deleteByTotalLessThan(1000000.0f));

        assertEquals(0L, receipts.count());
    }

    /**
     * Tests all CrudRepository methods (apart from those inherited from BasicRepository) with a record as the entity.
     */
    @SkipIfSysProp({
                     DB_Postgres, //Failing on Postgres due to eclipselink issue:  https://github.com/OpenLiberty/open-liberty/issues/28380
                     DB_SQLServer //Failing on SQLServer due to eclipselink issue: https://github.com/OpenLiberty/open-liberty/issues/28737
    })
    @Test
    public void testRecordCrudRepositoryMethods() {
        receipts.deleteByTotalLessThan(1000000.0f);

        Receipt r = receipts.insert(new Receipt(1200L, "C0002-12-002", 102.20f));
        assertNotNull(r);
        assertEquals(1200L, r.purchaseId());
        assertEquals("C0002-12-002", r.customer());
        assertEquals(102.20f, r.total(), 0.001f);

        List<Receipt> inserted = receipts.insertAll(List.of(new Receipt(1300L, "C0033-13-003", 130.13f),
                                                            new Receipt(1400L, "C0040-14-004", 14.40f),
                                                            new Receipt(1500L, "C0005-15-005", 105.50f),
                                                            new Receipt(1600L, "C0006-16-006", 600.16f)));

        assertEquals(4, inserted.size());
        assertNotNull(r = inserted.get(0));
        assertEquals(1300L, r.purchaseId());
        assertEquals("C0033-13-003", r.customer());
        assertEquals(130.13f, r.total(), 0.001f);
        assertNotNull(r = inserted.get(1));
        assertEquals(1400L, r.purchaseId());
        assertEquals("C0040-14-004", r.customer());
        assertEquals(14.40f, r.total(), 0.001f);
        assertNotNull(r = inserted.get(2));
        assertEquals(1500L, r.purchaseId());
        assertEquals("C0005-15-005", r.customer());
        assertEquals(105.50f, r.total(), 0.001f);
        assertNotNull(r = inserted.get(3));
        assertEquals(1600L, r.purchaseId());
        assertEquals("C0006-16-006", r.customer());
        assertEquals(600.16f, r.total(), 0.001f);

        try {
            receipts.insert(new Receipt(1200L, "C0002-10-002", 22.99f));
            fail("Inserted an entity with an Id that already exists.");
        } catch (EntityExistsException x) {
            // expected
        }

        // Ensure that the entity that already exists was not modified by insert
        r = receipts.findById(1200L).orElseThrow();
        assertEquals(1200L, r.purchaseId());
        assertEquals("C0002-12-002", r.customer());
        assertEquals(102.20f, r.total(), 0.001f);

        try {
            receipts.insertAll(List.of(new Receipt(1700L, "C0017-17-007", 177.70f),
                                       new Receipt(1500L, "C0055-15-005", 55.55f),
                                       new Receipt(1800L, "C0008-18-008", 180.18f)));
            fail("insertAll must fail when one of the entities has an Id that already exists.");
        } catch (EntityExistsException x) {
            // expected
        }

        // Ensure that insertAll inserted no entities when one had an Id that already exists
        assertEquals(false, receipts.findById(1700L).isPresent());
        assertEquals(false, receipts.findById(1800L).isPresent());

        // Ensure that the entity that already exists was not modified by insertAll
        r = receipts.findById(1500L).orElseThrow();
        assertEquals(1500L, r.purchaseId());
        assertEquals("C0005-15-005", r.customer());
        assertEquals(105.50f, r.total(), 0.001f);

        // Update single entity that exists
        Receipt updated = receipts.update(new Receipt(1600L, "C0060-16-006", 600.16f));

        assertEquals("C0060-16-006", updated.customer());
        assertEquals(1600L, updated.purchaseId());
        assertEquals(600.16f, updated.total(), 0.001f);

        // Update multiple entities, if they exist
        try {
            receipts.updateAll(List.of(new Receipt(1400L, "C0040-14-044", 14.49f),
                                       new Receipt(1900L, "C0009-19-009", 199.99f),
                                       new Receipt(1200L, "C0002-12-002", 112.29f)));
            fail("Attempt to update multiple entities where one does not exist must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        List<Receipt> updates = receipts.updateAll(List.of(new Receipt(1400L, "C0040-14-044", 14.41f),
                                                           new Receipt(1200L, "C0002-12-002", 112.20f)));
        Iterator<Receipt> updatesIt = updates.iterator();
        assertEquals(true, updatesIt.hasNext());
        updated = updatesIt.next();
        assertEquals(1400L, updated.purchaseId());
        assertEquals("C0040-14-044", updated.customer());
        assertEquals(14.41f, updated.total(), 0.001f);
        assertEquals(true, updatesIt.hasNext());
        updated = updatesIt.next();
        assertEquals(1200L, updated.purchaseId());
        assertEquals("C0002-12-002", updated.customer());
        assertEquals(112.20f, updated.total(), 0.001f);
        assertEquals(false, updatesIt.hasNext());

        // Verify the updates
        assertEquals(List.of(new Receipt(1200L, "C0002-12-002", 112.20f), // updated by updateAll
                             new Receipt(1300L, "C0033-13-003", 130.13f),
                             new Receipt(1400L, "C0040-14-044", 14.41f), // updated by updateAll
                             new Receipt(1500L, "C0005-15-005", 105.50f),
                             new Receipt(1600L, "C0060-16-006", 600.16f)), // updated by update
                     receipts.findAll()
                                     .sorted(Comparator.comparing(Receipt::purchaseId))
                                     .collect(Collectors.toList()));

        assertEquals(true, receipts.deleteByTotalLessThan(1000000.0f));

        assertEquals(0L, receipts.count());
    }

    /**
     * Test an unannotated entity that has an attribute which is a Java record,
     * which should be inferred to be an embeddable, such that queries and sorting
     * can be performed on the attributes of the embeddable.
     */
    @Test
    public void testRecordAsEmbeddable() {
        participants.remove("TestRecordAsEmbeddable");

        participants.add(Participant.of("Steve", "TestRecordAsEmbeddable", 1),
                         Participant.of("Sarah", "TestRecordAsEmbeddable", 2),
                         Participant.of("Simon", "TestRecordAsEmbeddable", 3),
                         Participant.of("Samantha", "TestRecordAsEmbeddable", 4));

        assertEquals("Simon", participants.getFirstName(3).orElseThrow());

        // TODO enable once #29460 is fixed
        //assertEquals(new Participant.Name("Samantha", "TestRecordAsEmbeddable"),
        //             participants.findNameById(4).orElseThrow());

        // TODO enable once #29460 is fixed
        //assertEquals(List.of("Samantha", "Sarah", "Simon", "Steve"),
        //             participants.withSurname("TestRecordAsEmbeddable")
        //                             .map(p -> p.name.first())
        //                             .collect(Collectors.toList()));

        assertEquals(4L, participants.remove("TestRecordAsEmbeddable"));
    }

    /**
     * Tests that a record entity can be specified in the FROM clause of JDQL.
     */
    @Test
    public void testRecordInFromClause() {
        receipts.deleteByTotalLessThan(2000.0f);

        receipts.saveAll(List.of(new Receipt(2000L, "C2000-00-123", 20.98f),
                                 new Receipt(2001L, "C2000-00-123", 15.99f)));

        assertEquals(20.98f, receipts.totalOf(2000L), 0.001f);
        assertEquals(15.99f, receipts.totalOf(2001L), 0.001f);

        assertEquals(true, receipts.addTax(2001L, 0.0813f));

        assertEquals(17.29f, receipts.totalOf(2001L), 0.001f);

        assertEquals(2, receipts.removeIfTotalUnder(2000.0f));
    }

    /**
     * Verify that a record return type (per spec) takes precedence over an
     * entity attribute that is a record.
     */
    @Test
    public void testRecordReturnTypePrecedence() {
        purchases.clearAll();

        Purchase p1 = new Purchase();
        p1.total = 105.19f;
        p1.customer = "TestRecordReturnTypePrecedence";
        p1.purchaseId = 1L;
        // the following does not match on purpose
        p1.receipt = new Receipt(1001L, "Customer1", 1.99f);

        purchases.buy(p1);

        Receipt r1 = purchases.receiptFor(1L).orElseThrow();
        assertEquals("TestRecordReturnTypePrecedence", r1.customer());
        assertEquals(1L, r1.purchaseId());
        assertEquals(105.19f, r1.total(), 0.001f);

        purchases.clearAll();
    }

    /**
     * Use repository methods that have various return types for a record entity.
     */
    @Test
    public void testRecordReturnTypes() throws Exception {
        receipts.removeIfTotalUnder(1000000.0f);

        receipts.insertAll(List.of(new Receipt(3000L, "RRT10155", 100.98f),
                                   new Receipt(3001L, "RRT10155", 48.99f),
                                   new Receipt(3002L, "RRT20618", 12.98f),
                                   new Receipt(3003L, "RRT10155", 34.97f),
                                   new Receipt(3004L, "RRT10155", 4.15f),
                                   new Receipt(3005L, "RRT10155", 51.95f),
                                   new Receipt(3006L, "RRT20618", 629.99f),
                                   new Receipt(3007L, "RRT10155", 71.79f),
                                   new Receipt(3008L, "RRT20618", 8.98f),
                                   new Receipt(3009L, "RRT10155", 99.94f),
                                   new Receipt(3010L, "RRT10155", 10.49f),
                                   new Receipt(3011L, "RRT10155", 101.92f),
                                   new Receipt(3012L, "RRT20618", 12.99f),
                                   new Receipt(3013L, "RRT30033", 31.99f),
                                   new Receipt(3014L, "RRT10155", 434.99f),
                                   new Receipt(3015L, "RRT10155", 55.59f)));

        // various forms of completion stage results
        CompletableFuture<Receipt> futureResult = receipts.findByPurchaseId(3013L);
        CompletionStage<Optional<Receipt>> futureOptionalPresent = //
                        receipts.findIfPresentByPurchaseId(3014L);
        CompletionStage<Optional<Receipt>> futureOptionalMissing = //
                        receipts.findIfPresentByPurchaseId(3116L);
        CompletableFuture<List<Receipt>> futureList = //
                        receipts.forCustomer("RRT20618",
                                             Order.by(Sort.desc("total")));

        // single record
        Receipt receipt = receipts.withPurchaseNum(3015L);
        assertEquals("RRT10155", receipt.customer());
        assertEquals(55.59f, receipt.total(), 0.001f);

        // array of record
        Receipt[] array = receipts.forCustomer("RRT20618");
        assertEquals(Arrays.toString(array), 4, array.length);
        assertEquals(3002L, array[0].purchaseId());
        assertEquals(3006L, array[1].purchaseId());
        assertEquals(3008L, array[2].purchaseId());
        assertEquals(3012L, array[3].purchaseId());

        // page of record
        PageRequest pageReq = PageRequest.ofSize(5);

        Page<Receipt> page1 = receipts.forCustomer("RRT10155", pageReq, Sort.asc("total"));
        assertEquals(List.of(3004L, 3010L, 3003L, 3001L, 3005L),
                     page1.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        Page<Receipt> page2 = receipts.forCustomer("RRT10155", page1.nextPageRequest(), Sort.asc("total"));
        assertEquals(List.of(3015L, 3007L, 3009L, 3000L, 3011L),
                     page2.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        Page<Receipt> page3 = receipts.forCustomer("RRT10155", page2.nextPageRequest(), Sort.asc("total"));
        assertEquals(List.of(3014L),
                     page3.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        // cursored page of record
        PageRequest above3006 = PageRequest.ofSize(3).afterCursor(Cursor.forKey(3006L));

        CursoredPage<Receipt> pageAbove3006 = receipts.forCustomer("RRT10155", above3006, Sort.asc("purchaseId"));
        assertEquals(List.of(3007L, 3009L, 3010L),
                     pageAbove3006.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        CursoredPage<Receipt> pageAbove3010 = receipts.forCustomer("RRT10155", pageAbove3006.nextPageRequest(), Sort.asc("purchaseId"));
        assertEquals(List.of(3011L, 3014L, 3015L),
                     pageAbove3010.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        CursoredPage<Receipt> pageBelow3007 = receipts.forCustomer("RRT10155", pageAbove3006.previousPageRequest(), Sort.asc("purchaseId"));
        assertEquals(List.of(3003L, 3004L, 3005L),
                     pageBelow3007.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        CursoredPage<Receipt> pageBelow3003 = receipts.forCustomer("RRT10155", pageBelow3007.previousPageRequest(), Sort.asc("purchaseId"));
        assertEquals(List.of(3000L, 3001L),
                     pageBelow3003.stream()
                                     .map(Receipt::purchaseId)
                                     .toList());

        // completable future single result that was requested earlier
        assertEquals(31.99f, futureResult.get(TIMEOUT_MINUTES, TimeUnit.MINUTES).total(), 0.001f);

        // completable future list of results that were requested earlier
        assertEquals(List.of(3006L, 3012L, 3002L, 3008L),
                     futureList.get(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                                     .stream()
                                     .map(Receipt::purchaseId)
                                     .collect(Collectors.toList()));

        // completion stage optional result that was requested earlier
        Receipt r3014 = futureOptionalPresent.toCompletableFuture().get(TIMEOUT_MINUTES, TimeUnit.MINUTES).orElseThrow();
        assertEquals(3014L, r3014.purchaseId());
        assertEquals("RRT10155", r3014.customer());
        assertEquals(434.99f, r3014.total(), 0.001f);

        assertEquals(false, futureOptionalMissing.toCompletableFuture().get(TIMEOUT_MINUTES, TimeUnit.MINUTES).isPresent());

        assertEquals(1L, receipts.removeByPurchaseId(3000L));

        assertEquals(Set.of(3002L, 3010L, 3012L),
                     receipts.removeByTotalBetween(10.00f, 20.00f));

        // remove data to avoid interference with other tests
        assertEquals(12, receipts.removeIfTotalUnder(1000000.0f));
    }

    /**
     * Use a record entity that has embeddable attributes.
     */
    @Test
    public void testRecordWithEmbeddables() {
        ratings.clear();

        Rating.Reviewer.Name name1 = new Rating.Reviewer.Name("Rex", "TestRecordWithEmbeddables");
        Rating.Reviewer.Name name2 = new Rating.Reviewer.Name("Rhonda", "TestRecordWithEmbeddables");
        Rating.Reviewer.Name name3 = new Rating.Reviewer.Name("Rachel", "TestRecordWithEmbeddables");
        Rating.Reviewer.Name name4 = new Rating.Reviewer.Name("Ryan", "TestRecordWithEmbeddables");

        Rating.Reviewer user1 = new Rating.Reviewer(name1, "rex@openliberty.io");
        Rating.Reviewer user2 = new Rating.Reviewer(name2, "rhonda@openliberty.io");
        Rating.Reviewer user3 = new Rating.Reviewer(name3, "rachel@openliberty.io");
        Rating.Reviewer user4 = new Rating.Reviewer(name4, "ryan@openliberty.io");

        Rating.Item blender = new Rating.Item("blender", 41.99f);
        Rating.Item toaster = new Rating.Item("toaster", 28.98f);
        Rating.Item microwave = new Rating.Item("microwave", 63.89f);

        ratings.add(new Rating(1000, toaster, 2, user4, Set.of("Burns everything.", "Often gets stuck.", "Bagels don't fit.")));
        ratings.add(new Rating(1001, blender, 0, user4, Set.of("Broke after first use.")));
        ratings.add(new Rating(1002, microwave, 2, user4, Set.of("Uneven cooking.", "Too noisy.")));
        ratings.add(new Rating(1003, microwave, 4, user3, Set.of("Good at reheating leftovers.")));
        ratings.add(new Rating(1004, microwave, 5, user2, Set.of()));
        ratings.add(new Rating(1005, microwave, 3, user1, Set.of("It works okay.")));
        ratings.add(new Rating(1006, toaster, 4, user1, Set.of("It toasts things.")));
        ratings.add(new Rating(1007, blender, 3, user1, Set.of("Too noisy.", "It blends things. Sometimes.")));
        ratings.add(new Rating(1008, blender, 5, user2, Set.of("Nice product!")));
        ratings.add(new Rating(1009, toaster, 5, user2, Set.of("Nice product!")));
        ratings.add(new Rating(1010, toaster, 3, user3, Set.of("Timer malfunctions on occasion, but it otherwise works.")));

        assertEquals(Set.of("Uneven cooking.", "Too noisy."),
                     ratings.getComments(1002));

        // TODO enable once EclipseLink bug #28589 is fixed
        // java.lang.IllegalArgumentException: An exception occurred while creating a query in EntityManager:
        // Exception Description: Problem compiling
        // [SELECT NEW test.jakarta.data.web.Rating(o.id, o.item, o.numStars, o.reviewer, o.comments)
        //  FROM RatingEntity o WHERE (o.item.price BETWEEN ?1 AND ?2) ORDER BY o.reviewer.email]. [78, 88]
        // The state field path 'o.comments' cannot be resolved to a collection type.
        //assertEquals(List.of("Rachel", "Rex", "Ryan"),
        //             ratings.findByItemPriceBetween(40.00f, 50.00f,
        //                                            Sort.asc("reviewer.email"))
        //                             .map(r -> r.reviewer().firstName())
        //                             .collect(Collectors.toList()));

        //assertEquals(List.of(1007, 1002),
        //             ratings.findByCommentsContainsOrderByIdDesc("Too noisy.")
        //                             .map(Rating::id)
        //                             .collect(Collectors.toList()));

        //assertEquals(List.of("toaster", "blender", "microwave"),
        //             ratings.search(3)
        //                             .map(r -> r.item().name)
        //                             .collect(Collectors.toList()));

        assertEquals(11L, ratings.clear());
    }

    /**
     * Use repository updateBy methods with multiplication and division,
     */
    @Test
    public void testRepositoryUpdateMethodsMultiplyAndDivide() {
        packages.deleteAll();

        Package p1 = new Package();
        p1.description = "Cereal Box";
        p1.length = 7.0f;
        p1.width = 19.1f;
        p1.height = 28.2f;
        p1.id = 990001;

        Package p2 = new Package();
        p2.description = "Pasta Noodle Box";
        p2.length = 7.0f;
        p2.width = 10.0f;
        p2.height = 18.5f;
        p2.id = 990002;

        Package p3 = new Package();
        p3.description = "Tissue box";
        p3.length = 12.0f;
        p3.width = 23.73f;
        p3.height = 9.2f;
        p3.id = 990003;

        Package p4 = new Package();
        p4.description = "Large Cereal Box";
        p4.length = 8.2f;
        p4.width = 21.0f;
        p4.height = 29.5f;
        p4.id = 990004;

        Package p5 = new Package();
        p5.description = "Small Cereal Box";
        p5.length = 4.8f;
        p5.width = 18.4f;
        p5.height = 27.3f;
        p5.id = 990005;

        Package p6 = new Package();
        p6.description = "Crackers Box";
        p6.length = 6.0f;
        p6.width = 11.5f;
        p6.height = 19.2f;
        p6.id = 990006;

        packages.saveAll(List.of(p1, p2, p3, p4, p5, p6));

        // multiply, divide, and add within same update
        assertEquals(true, packages.updateByIdAddHeightMultiplyLengthDivideWidth(990003, 1.0f, 0.95f, 1.05f));

        Package p = packages.findById(990003).get();
        assertEquals(11.4f, p.length, 0.01f);
        assertEquals(22.6f, p.width, 0.01f);
        assertEquals(10.2f, p.height, 0.01f);

        // perform same type of update to multiple columns
        packages.updateByIdDivideLengthDivideWidthDivideHeight(990005, 1.2f, 1.15f, 1.1375f);

        p = packages.findById(990005).get();
        assertEquals(4.0f, p.length, 0.01f);
        assertEquals(16.0f, p.width, 0.01f);
        assertEquals(24.0f, p.height, 0.01f);

        // multiple conditions and multiple updates
        assertEquals(2L, packages.updateByLengthLessThanEqualAndHeightBetweenMultiplyLengthMultiplyWidthSetHeight(7.1f, 18.4f, 19.4f, 1.1f, 1.2f, 19.5f));

        List<Package> results = packages.findByHeightBetween(19.4999f, 19.5001f);
        assertEquals(results.toString(), 2, results.size());

        p = packages.findById(990002).get();
        assertEquals(7.7f, p.length, 0.01f);
        assertEquals(12.0f, p.width, 0.01f);
        assertEquals(19.5f, p.height, 0.01f);

        p = packages.findById(990006).get();
        assertEquals(6.6f, p.length, 0.01f);
        assertEquals(13.8f, p.width, 0.01f);
        assertEquals(19.5f, p.height, 0.01f);

        // divide width and append to description via query by method name
        assertEquals(true, packages.updateByIdDivideWidthAddDescription(990003, 2, " halved"));

        p = packages.findById(990003).orElseThrow();
        assertEquals(11.4f, p.length, 0.01f);
        assertEquals(11.3f, p.width, 0.01f);
        assertEquals(10.2f, p.height, 0.01f);
        assertEquals("Tissue box halved", p.description);

        packages.delete(p3);

        Order<Package> descId = Order.by(Sort.desc("id"));
        Page<Package> page = packages.findAll(PageRequest.ofSize(3), descId);
        assertIterableEquals(List.of(990006, 990005, 990004),
                             page.stream().map(pack -> pack.id).collect(Collectors.toList()));

        page = packages.findAll(page.nextPageRequest(), descId);
        assertIterableEquals(List.of(990002, 990001),
                             page.stream().map(pack -> pack.id).collect(Collectors.toList()));

        packages.deleteAll(List.of(p1, p6));

        assertIterableEquals(List.of(990002, 990004, 990005),
                             packages.findAll().map(pack -> pack.id).sorted().collect(Collectors.toList()));

        assertEquals(3l, packages.deleteAll());

        assertEquals(0, packages.countAll());
    }

    /**
     * Experiment with reserved keywords in entity attribute names.
     */
    @Test
    public void testReservedKeywordsInEntityAttributeNames() {
        // clear out old data before test
        things.deleteAll();

        things.save(new Thing(1, "apple", true, false, "Honeycrisp", "a type of apple", 1, "Keepsake x MN 1627", "September", 20, 210550L));
        things.save(new Thing(2, "apple", true, false, "Haralson", "a type of apple", 1, "Malinda x Wealthy", "September/October", 5, 143005L));
        things.save(new Thing(3, "apple", true, false, "Fireside", "a type of apple", 1, "McIntosh x Longfield", "October", 3, 321004L));
        things.save(new Thing(4, "apple", true, false, "Honeygold", "a type of apple", 1, "Golden Delicious x Haralson", "September", 10, 100201L));
        things.save(new Thing(5, "A101", false, false, "IBM", "050-2 A101 conference room", 2, "2nd floor conference room", "capacity 20", 16, 163L));
        things.save(new Thing(6, "android", false, true, null, "a robot that looks like a person", 3, "not alive", "not alive", 4, 40661L));

        Thing thing = things.findById(2);
        assertEquals("Haralson", thing.brand);

        // "like" is allowed at end of entity attribute name because the capitalization differs.
        assertEquals(List.of("Fireside", "Haralson", "Honeycrisp", "Honeygold"),
                     things.findByAlike(true)
                                     .map(o -> o.brand)
                                     .sorted()
                                     .collect(Collectors.toList()));

        // "Like" is used as a reserved keyword here.
        assertEquals(List.of("A101"),
                     things.findByALike("A1%") // include second character so that databases that compare independent of case don't match "apple"
                                     .map(o -> o.a)
                                     .collect(Collectors.toList()));

        // "Or" in middle of entity attribute name is possible when using @Query.
        assertEquals(List.of("Honeycrisp"),
                     things.forPurchaseOrder(20)
                                     .map(o -> o.brand)
                                     .collect(Collectors.toList()));

        // "Or" is allowed at the beginning of an entity attribute name
        // because "find...By" immediately precedes it.
        assertEquals(List.of("Honeygold"),
                     things.findByOrderNumber(100201L)
                                     .map(o -> o.brand)
                                     .collect(Collectors.toList()));

        // "And" is allowed at the beginning of an entity attribute name
        // because "find...By" immediately precedes it.
        assertEquals(List.of("android"),
                     things.findByAndroid(true)
                                     .map(o -> o.a)
                                     .collect(Collectors.toList()));

        // "and" is allowed at end of entity attribute name "brand"
        // because the capitalization differs.
        // "Not" is allowed at the beginning of an entity attribute name "Notes"
        // because the reserved word "Not" never appears prior to the attribute name.
        // "And" is allowed at the beginning of an entity attribute name
        // because "And" or "Or" immediately precedes it.
        assertEquals(List.of(2L, 3L, 5L, 6L),
                     things.findByBrandOrNotesContainsOrAndroid("IBM", "October", true)
                                     .map(o -> o.thingId)
                                     .sorted()
                                     .collect(Collectors.toList()));

        // "or" is allowed at end of entity attribute name "floor"
        // because the capitalization differs.
        // "In" is allowed at the beginning of an entity attribute name "Info"
        // because the reserved word "In" never appears prior to the attribute name.
        // "Or" is allowed at the beginning of an entity attribute name
        // because "And" or "Or" immediately precedes it.
        assertEquals(List.of("2nd floor conference room", "Golden Delicious x Haralson"),
                     things.findByFloorNotAndInfoLikeAndOrderNumberLessThan(3, "%o%", 300000L)
                                     .map(o -> o.info)
                                     .sorted()
                                     .collect(Collectors.toList()));

        // The OrderBy annotation can include entity attributes with "Desc" in the name
        assertEquals(List.of("A101", "android", "apple"),
                     things.findByThingIdGreaterThan(3L)
                                     .map(o -> o.a)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method that performs a JDQL query using RIGHT function
     * to obtain the end of a String value.
     */
    @Test
    public void testRightFunction() {
        assertEquals(List.of("thirty-seven", "thirteen", "seventeen",
                             "seven", "nineteen", "eleven"),
                     primes.matchRightSideOfName("en"));
    }

    /**
     * Use repository methods with Rounded, RoundedUp, and RoundedDown keywords.
     */
    @Test
    public void testRounding() {
        packages.deleteAll();

        packages.saveAll(List.of(new Package(601, 19.2f, 5.8f, 5.4f, "package#601"),
                                 new Package(603, 12.4f, 10.125f, 4.8f, "package#603"),
                                 new Package(605, 18.75f, 9.75f, 3.31f, "package#605"),
                                 new Package(607, 18.01f, 8.33f, 4.046f, "package#607")));

        assertIterableEquals(List.of(605, 607),
                             packages.findIdByLengthRoundedUp(19));

        assertIterableEquals(List.of(603, 605),
                             packages.findIdByWidthRounded(10));

        assertIterableEquals(List.of(603, 607),
                             packages.findIdByHeightRoundedDown(4));

        assertEquals(4, packages.deleteEverything());
    }

    /**
     * Insert, update, find, and delete entities.
     */
    @Test
    public void testSaveAndUpdateMultiple() {
        // find none
        houses.dropAll();
        assertEquals(false, houses.existsByParcelId("001-203-401"));

        // insert
        House h1 = new House();
        h1.area = 1500;
        h1.lotSize = 0.18f;
        h1.numBedrooms = 3;
        h1.parcelId = "001-203-401";
        h1.purchasePrice = 125000.00f;
        h1.sold = Year.of(2015);

        List<House> saved = houses.save(h1);

        assertEquals(saved.toString(), 1, saved.size());
        assertEquals("001-203-401", saved.get(0).parcelId);

        // update
        h1.numBedrooms = 4;
        h1.purchasePrice = 136000.00f;
        h1.sold = Year.of(2016);

        saved = houses.save(h1);
        assertEquals(saved.toString(), 1, saved.size());
        House h = saved.get(0);

        assertEquals(1500, h.area);
        assertEquals(0.18f, h.lotSize, 0.001f);
        assertEquals(4, h.numBedrooms);
        assertEquals("001-203-401", h.parcelId);
        assertEquals(136000.00f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2016), h.sold);

        // insert multiple
        House h2 = new House();
        h2.area = 1200;
        h2.lotSize = 0.21f;
        h2.numBedrooms = 2;
        h2.parcelId = "001-203-402";
        h2.purchasePrice = 112000.00f;
        h2.sold = Year.of(2012);

        House h3 = new House();
        h3.area = 1300;
        h3.lotSize = 0.13f;
        h3.numBedrooms = 3;
        h3.parcelId = "001-203-403";
        h3.purchasePrice = 113000.00f;
        h3.sold = Year.of(2013);

        Iterable<House> inserted = houses.save(h2, h3);

        Iterator<House> i = inserted.iterator();
        assertEquals(true, i.hasNext());
        assertEquals(h2.parcelId, i.next().parcelId);
        assertEquals(true, i.hasNext());
        assertEquals(h3.parcelId, i.next().parcelId);
        assertEquals(false, i.hasNext());

        // find
        h = houses.findById(h1.parcelId);
        assertEquals(h1.area, h.area);
        assertEquals(h1.lotSize, h.lotSize, 0.001f);
        assertEquals(h1.numBedrooms, h.numBedrooms);
        assertEquals(h1.parcelId, h.parcelId);
        assertEquals(h1.purchasePrice, h.purchasePrice, 0.001f);
        assertEquals(h1.sold, h.sold);

        // update multiple
        h2.purchasePrice = 152000.00f;
        h2.sold = Year.of(2022);

        h1.purchasePrice = 191000.00f;
        h1.sold = Year.of(2019);

        Iterable<House> updated = houses.save(h1, h2);

        Iterator<House> u = updated.iterator();
        assertEquals(true, u.hasNext());
        assertEquals(191000.00f, u.next().purchasePrice, 0.001f);
        assertEquals(true, u.hasNext());
        assertEquals(Year.of(2022), u.next().sold);
        assertEquals(false, u.hasNext());

        // delete
        assertEquals(1L, houses.deleteById(h1.parcelId));

        // find none
        assertEquals(false, houses.existsByParcelId(h1.parcelId));

        // delete nothing
        assertEquals(0L, houses.deleteById(h1.parcelId));

        assertEquals(2L, houses.dropAll());
    }

    /**
     * Insert, update, find, and delete an unannotated entity.
     */
    @Test
    public void testSaveMultipleAndUpdate() {
        vehicles.removeAll();

        Vehicle v1 = new Vehicle();
        v1.make = "Honda";
        v1.model = "Accord";
        v1.numSeats = 5;
        v1.price = 26000f;
        v1.vinId = "TE201234567890001";

        Vehicle v2 = new Vehicle();
        v2.make = "Ford";
        v2.model = "F-150";
        v2.numSeats = 3;
        v2.price = 32000f;
        v2.vinId = "TE201234567890002";

        Vehicle v3 = new Vehicle();
        v3.make = "Toyota";
        v3.model = "Camry";
        v3.numSeats = 5;
        v3.price = 25000f;
        v3.vinId = "TE201234567890003";

        // insert multiple
        Iterable<Vehicle> inserted = vehicles.save(List.of(v1, v2, v3));
        Iterator<Vehicle> i = inserted.iterator();
        assertEquals(true, i.hasNext());
        assertEquals("Honda", i.next().make);
        assertEquals(true, i.hasNext());
        assertEquals(3, i.next().numSeats);
        assertEquals(true, i.hasNext());
        assertEquals(25000f, i.next().price, 0.001f);
        assertEquals(false, i.hasNext());

        // delete
        assertEquals(true, vehicles.removeByVinId(v1.vinId));

        // find none
        Optional<Vehicle> found = vehicles.findByVinId(v1.vinId);
        assertEquals(false, found.isPresent());

        // update
        assertEquals(true, vehicles.updateByVinIdAddPrice("TE201234567890003", 500.0f));

        // find
        found = vehicles.findByVinId("TE201234567890003");
        assertEquals(true, found.isPresent());
        assertEquals(25500f, found.get().price, 0.001f);

        vehicles.removeAll();
    }

    /**
     * Repository method having only a SELECT clause.
     */
    @Test
    public void testSelectClauseOnly() {
        products.clear();

        assertEquals(List.of(),
                     List.of(products.names()));

        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("TestSelectClauseOnly-1".getBytes());
        prod1.name = "TestSelectClauseOnly-1";
        prod1.description = "TestSelectClauseOnly description 1";
        prod1.price = 18.99f;

        Product prod2 = new Product();
        prod2.pk = UUID.nameUUIDFromBytes("TestSelectClauseOnly-2".getBytes());
        prod2.name = "TestSelectClauseOnly-2";
        prod2.description = "TestSelectClauseOnly description 2";
        prod2.price = 27.99f;

        products.saveMultiple(prod1, prod2);

        assertEquals(Set.of("TestSelectClauseOnly-1", "TestSelectClauseOnly-2"),
                     Set.of(products.names()));

        products.clear();
    }

    /**
     * Repository method with a SELECT clause and WHERE clause, but no FROM clause.
     */
    @Test
    public void testSelectClauseWithoutFromClause() {
        assertEquals("1D", primes.toHexadecimal(29).orElseThrow());
        assertEquals("2B", primes.toHexadecimal(43).orElseThrow());
        assertEquals(false, primes.toHexadecimal(18).isPresent());
    }

    /**
     * Repository method returns a single result or raises the specification-defined exceptions for none or too many.
     */
    @Test
    public void testSingleResult() {
        // With entity class as return type:

        // Single result is fine:
        Prime p = primes.findByNumberIdBetween(14L, 18L);
        assertEquals(17L, p.numberId);

        // No result must raise EmptyResultException:
        try {
            p = primes.findByNumberIdBetween(24L, 28L);
            fail("Unexpected prime " + p);
        } catch (EmptyResultException x) {
            // expected
        }

        // Multiple results must raise NonUniqueResultException:
        try {
            p = primes.findByNumberIdBetween(34L, 48L);
            fail("Should find more primes than " + p);
        } catch (NonUniqueResultException x) {
            // expected
        }

        // With custom return type:

        // Single result is fine:
        long n = primes.findAsLongBetween(12L, 16L);
        assertEquals(13L, n);

        // No result must raise EmptyResultException:
        try {
            n = primes.findAsLongBetween(32L, 36L);
            fail("Unexpected prime number " + n);
        } catch (EmptyResultException x) {
            // expected
        }

        // Multiple results must raise NonUniqueResultException:
        try {
            n = primes.findAsLongBetween(22L, 42L);
            fail("Should find more prime numbers than " + n);
        } catch (NonUniqueResultException x) {
            // expected
        }
    }

    /**
     * Tests a repository method that returns a Page with a single boolean
     * result on it.
     */
    @Test
    public void testSingularResultPageOfBoolean() {
        PageRequest pageReq = PageRequest.ofSize(6);
        Page<Boolean> page = primes.pageOfExists(pageReq);

        assertEquals(List.of(true), page.content());
        assertEquals(false, page.hasNext());
        assertEquals(false, page.hasPrevious());
        assertEquals(1L, page.totalElements());
        assertEquals(1L, page.totalPages());
    }

    /**
     * Tests a repository method that returns a Page with a single numeric
     * result on it.
     */
    @Test
    public void testSingularResultPageNumeric() {
        Page<Long> page = primes.pageOfCountUpTo(20L, PageRequest.ofSize(4));

        assertEquals(List.of(8L), page.content());
        assertEquals(false, page.hasNext());
        assertEquals(false, page.hasPrevious());
        assertEquals(1L, page.totalElements());
        assertEquals(1L, page.totalPages());
    }

    /**
     * A repository might define a method that returns a Page with a Limit parameter.
     */
    @Test
    public void testSliceWithLimit() {
        // This is not a recommended pattern. Testing to see how it is handled.
        Page<Prime> slice = primes.findByRomanNumeralEndsWithAndNumberIdLessThan("II", 50L, Limit.of(4), Sort.desc(ID));

        assertEquals(1L, slice.pageRequest().page());
        assertEquals(4L, slice.numberOfElements());
        assertEquals(4L, slice.pageRequest().size());
        assertEquals(1L, slice.pageRequest().page());

        assertIterableEquals(List.of("XLVII", "XLIII", "XXXVII", "XXIII"),
                             slice.stream()
                                             .map(p -> p.romanNumeral)
                                             .collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndNumberIdLessThan("II", 50L, slice.nextPageRequest(), Order.by(Sort.desc(ID)));

        assertEquals(2L, slice.pageRequest().page());
        assertEquals(4L, slice.numberOfElements());
        assertEquals(4L, slice.pageRequest().size());
        assertEquals(2L, slice.pageRequest().page());

        assertIterableEquals(List.of("XVII", "XIII", "VII", "III"),
                             slice.stream()
                                             .map(p -> p.romanNumeral)
                                             .collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndNumberIdLessThan("II", 50L, slice.nextPageRequest(), Order.by(Sort.desc(ID)));

        assertEquals(3L, slice.pageRequest().page());
        assertEquals(1L, slice.numberOfElements());
        assertEquals(4L, slice.pageRequest().size());
        assertEquals(3L, slice.pageRequest().page());
        assertEquals(false, slice.hasNext());

        assertIterableEquals(List.of("II"),
                             slice.stream()
                                             .map(p -> p.romanNumeral)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method that returns a Page with the sort criteria provided as Sort parameters
     */
    @Test
    public void testSliceWithSortCriteriaAsSortParameters() {
        Page<Prime> slice = primes.findByRomanNumeralEndsWithAndNumberIdLessThan("I", 50L,
                                                                                 PageRequest.ofSize(5).withoutTotal(),
                                                                                 Order.by(),
                                                                                 Sort.asc("sumOfBits"), Sort.desc(ID));
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(5, slice.numberOfElements());
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(5, slice.pageRequest().size());

        assertIterableEquals(List.of(2L, 17L, 3L, 41L, 37L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndNumberIdLessThan("I", 50L,
                                                                     slice.nextPageRequest(),
                                                                     Order.by(),
                                                                     Sort.asc("sumOfBits"), Sort.desc(ID));
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(5, slice.numberOfElements());
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(5, slice.pageRequest().size());

        assertIterableEquals(List.of(13L, 11L, 7L, 43L, 23L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndNumberIdLessThan("I", 50L,
                                                                     slice.nextPageRequest(),
                                                                     Order.by(),
                                                                     Sort.asc("sumOfBits"), Sort.desc(ID));
        assertEquals(3L, slice.pageRequest().page());
        assertEquals(2, slice.numberOfElements());
        assertEquals(3L, slice.pageRequest().page());
        assertEquals(5, slice.pageRequest().size());

        assertIterableEquals(List.of(47L, 31L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(false, slice.hasNext());
    }

    /**
     * Repository method that returns a Page with the sort criteria provided by the OrderBy annotation.
     */
    @Test
    public void testSliceWithSortCriteriaInOrderByAnnotation() {
        Page<Prime> slice = primes.findByRomanNumeralStartsWithAndNumberIdLessThan("X", 50L,
                                                                                   PageRequest.ofSize(4).withoutTotal());
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(4, slice.numberOfElements());
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(4, slice.pageRequest().size());

        assertIterableEquals(List.of("forty-seven", "thirty-one", "forty-three", "twenty-nine"),
                             slice.stream().map(p -> p.name).collect(Collectors.toList()));

        assertEquals(true, slice.hasNext());
        slice = primes.findByRomanNumeralStartsWithAndNumberIdLessThan("X", 50L, slice.nextPageRequest());
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(4, slice.numberOfElements());
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(4, slice.pageRequest().size());

        assertIterableEquals(List.of("twenty-three", "eleven", "forty-one", "nineteen"),
                             slice.stream().map(p -> p.name).collect(Collectors.toList()));

        assertEquals(true, slice.hasNext());
        slice = primes.findByRomanNumeralStartsWithAndNumberIdLessThan("X", 50L, slice.nextPageRequest());
        assertEquals(3L, slice.pageRequest().page());
        assertEquals(3, slice.numberOfElements());
        assertEquals(3L, slice.pageRequest().page());
        assertEquals(4, slice.pageRequest().size());

        assertIterableEquals(List.of("thirteen", "thirty-seven", "seventeen"),
                             slice.stream().map(p -> p.name).collect(Collectors.toList()));

        assertEquals(false, slice.hasNext());
    }

    /**
     * Repository method that returns a Page with the sort criteria provided in an Order parameter
     */
    @Test
    public void testSliceWithSortCriteriaInOrderClass() {
        Order<Prime> order = Order.by(Sort.desc("numberId"));
        Page<Prime> slice = primes.findByRomanNumeralEndsWithAndNumberIdLessThan("II", 50L,
                                                                                 PageRequest.ofSize(6).withoutTotal(),
                                                                                 order);
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(6, slice.numberOfElements());
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(6, slice.pageRequest().size());

        assertIterableEquals(List.of(47L, 43L, 37L, 23L, 17L, 13L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndNumberIdLessThan("II", 50L,
                                                                     slice.nextPageRequest(),
                                                                     order);
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(3, slice.numberOfElements());
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(6, slice.pageRequest().size());

        assertIterableEquals(List.of(7L, 3L, 2L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(false, slice.hasNext());
    }

    /**
     * Use the JDQL id(entityVar) function as the sort attribute to perform a
     * descending sort.
     */
    @Test
    public void testSortByIdFunction() {
        assertEquals(List.of(31L, 29L, 23L, 19L, 17L, 13L),
                     primes.findByNumberIdBetween(12, 36, Sort.desc(By.ID))
                                     .stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));
    }

    /**
     * When sort criteria is specified statically via the OrderBy annotation and
     * dynamically via Sorts from pagination, the static sort criteria is applied
     * before the dynamic sort criteria.
     */
    @Test
    public void testSortCriteriaOfOrderByAnnoTakesPrecedenceOverPaginationSorts() {

        PageRequest pagination = PageRequest.ofSize(9);
        Page<Prime> page1 = primes.findByNumberIdLessThan(49L, Sort.desc("numberId"), pagination);

        assertIterableEquals(List.of("17(2)", "5(2)", "3(2)",
                                     "41(3)", "37(3)", "19(3)", "13(3)", "11(3)", "7(3)"),
                             page1.stream()
                                             .map(p -> p.numberId + "(" + p.sumOfBits + ")")
                                             .collect(Collectors.toList()));

        Page<Prime> page2 = primes.findByNumberIdLessThan(49L, Sort.desc("numberId"), page1.nextPageRequest());

        assertIterableEquals(List.of("43(4)", "29(4)", "23(4)",
                                     "47(5)", "31(5)",
                                     "2(1)"),
                             page2.stream()
                                             .map(p -> p.numberId + "(" + p.sumOfBits + ")")
                                             .collect(Collectors.toList()));
    }

    /**
     * When sort criteria is specified statically via the Query annotation and
     * dynamically via Sorts from cursor-based pagination, the static sort criteria
     * is applied before the dynamic sort criteria.
     */
    @Test
    public void testSortCriteriaOfOrderByAnnoTakesPrecedenceOverPaginationSortsOnCustomQueryUsingCursorPagination() {

        Order<Prime> order = Order.by(Sort.asc("binaryDigits"));

        PageRequest pagination = PageRequest.ofSize(7);
        CursoredPage<Prime> page1 = primes.upTo(47L, pagination, order);

        assertEquals(7, page1.numberOfElements());
        assertEquals(15L, page1.totalElements());

        assertIterableEquals(List.of("10",
                                     "101111", "11111",
                                     "101011", "10111", "11101",
                                     "100101"),
                             page1.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        CursoredPage<Prime> page2 = primes.upTo(47L, page1.nextPageRequest(), order);

        assertIterableEquals(List.of("10011", "101001", "1011", "1101", "111",
                                     "10001", "101"),
                             page2.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        CursoredPage<Prime> page3 = primes.upTo(47L, page2.nextPageRequest(), order);

        assertIterableEquals(List.of("11"),
                             page3.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page2 = primes.upTo(47L, page3.previousPageRequest(), order);

        assertIterableEquals(List.of("10011", "101001", "1011", "1101", "111",
                                     "10001", "101"),
                             page2.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page1 = primes.upTo(47L, page2.previousPageRequest(), order);

        assertIterableEquals(List.of("10",
                                     "101111", "11111",
                                     "101011", "10111", "11101",
                                     "100101"),
                             page1.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));
    }

    /**
     * When sort criteria is specified statically via the OrderBy keyword and
     * dynamically via Sorts from cursor-based pagination, the static sort criteria
     * is applied before the dynamic sort criteria.
     */
    @Test
    public void testSortCriteriaOfOrderByKeywordTakesPrecedenceOverCursorPaginationSorts() {

        PageRequest pagination = PageRequest.ofSize(6).withoutTotal();
        CursoredPage<Prime> page1 = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, pagination,
                                                                                            Sort.desc("binaryDigits"));

        assertIterableEquals(List.of("11", "101", "10001",
                                     "111", "1101", "1011"),
                             page1.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        CursoredPage<Prime> page2 = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page1.nextPageRequest(),
                                                                                            Sort.desc("binaryDigits"));

        assertIterableEquals(List.of("101001", "10011", "100101",
                                     "11101", "10111", "101011"),
                             page2.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        CursoredPage<Prime> page3 = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page2.nextPageRequest(),
                                                                                            Sort.desc("binaryDigits"));

        assertIterableEquals(List.of("11111", "101111",
                                     "10"),
                             page3.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        pagination = PageRequest.ofSize(6)
                        .withoutTotal()
                        .beforeCursor(page3.cursor(1)); // before the middle element of page 3

        CursoredPage<Prime> page = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, pagination,
                                                                                           Sort.desc("binaryDigits"));

        assertIterableEquals(List.of("10011", "100101",
                                     "11101", "10111", "101011",
                                     "11111"),
                             page.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page.previousPageRequest(),
                                                                       Sort.desc("binaryDigits"));

        assertIterableEquals(List.of("101", "10001",
                                     "111", "1101", "1011", "101001"),
                             page.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page.previousPageRequest(),
                                                                       Sort.desc("binaryDigits"));

        assertIterableEquals(List.of("11"),
                             page.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));
    }

    /**
     * When sort criteria is specified statically via the OrderBy keyword and
     * dynamically via Sorts, the static sort criteria is applied
     * before the dynamic sort criteria.
     */
    @Test
    public void testSortCriteriaOfOrderByKeywordTakesPrecedenceOverSorts() {

        assertIterableEquals(List.of("3(2)", "5(2)", "17(2)",
                                     "7(3)", "11(3)", "13(3)", "19(3)", "37(3)", "41(3)",
                                     "23(4)", "29(4)", "43(4)",
                                     "31(5)", "47(5)",
                                     "2(1)"),
                             primes.findByNumberIdLessThanOrderByEven(50L, Sort.asc("sumOfBits"), Sort.asc(ID))
                                             .map(p -> p.numberId + "(" + p.sumOfBits + ")")
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method that returns a stream and uses it as a parallel stream.
     */
    @Test
    public void testStream() {
        Stream<Prime> stream = primes.findByNumberIdLessThan(49L);
        Long total = stream.parallel().reduce(0L, (sum, p) -> sum + p.numberId, (sum1, sum2) -> sum1 + sum2);
        assertEquals(Long.valueOf(328), total);
    }

    /**
     * Repository method that returns a List and obtains streams from it twice, and also obtains an iterable from it.
     */
    @Test
    public void testStreamFromList() {
        List<Prime> streamable = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(49L, Limit.of(14));
        Long total = streamable.stream().parallel().reduce(0L, (sum, p) -> sum + p.numberId, (sum1, sum2) -> sum1 + sum2);
        assertEquals(Long.valueOf(326), total);

        assertIterableEquals(List.of(47L, 43L, 41L, 37L, 31L, 29L, 23L, 19L, 17L, 13L, 11L, 7L, 5L, 3L),
                             streamable.stream().map(p -> p.numberId).collect(Collectors.toList()));

        AtomicLong sumRef = new AtomicLong();
        streamable.iterator().forEachRemaining(p -> sumRef.addAndGet(p.numberId));
        assertEquals(326L, sumRef.get());
    }

    /**
     * Repository method that supplies pagination information and returns a list.
     */
    @Test
    public void testSupplyPageRequestAndReturnList() {
        PageRequest p1 = PageRequest.ofSize(9);
        List<Prime> list1 = primes.findByNumberIdLessThanEqualOrderByNumberIdAsc(44L, p1);

        assertIterableEquals(List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L),
                             list1.stream().map(p -> p.numberId).collect(Collectors.toList()));

        PageRequest p2 = PageRequest.ofPage(2, 9, false);
        List<Prime> list2 = primes.findByNumberIdLessThanEqualOrderByNumberIdAsc(44L, p2);

        assertIterableEquals(List.of(29L, 31L, 37L, 41L, 43L),
                             list2.stream().map(p -> p.numberId).collect(Collectors.toList()));

        AtomicLong sumRef = new AtomicLong();
        list2.forEach(p -> sumRef.addAndGet(p.numberId));
        assertEquals(181L, sumRef.get());
    }

    /**
     * Obtain total counts of number of elements and pages.
     */
    @Test
    public void testTotalCounts() {
        Page<Prime> page1 = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(43L, PageRequest.ofSize(6));

        assertEquals(3L, page1.totalPages());
        assertEquals(14L, page1.totalElements());

        assertIterableEquals(List.of(43L, 41L, 37L, 31L, 29L, 23L),
                             page1.content().stream().map(p -> p.numberId).collect(Collectors.toList()));

        Page<Prime> page2 = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(43L, page1.nextPageRequest());

        assertEquals(14L, page2.totalElements());
        assertEquals(3L, page2.totalPages());

        assertIterableEquals(List.of(19L, 17L, 13L, 11L, 7L, 5L),
                             page2.stream().map(p -> p.numberId).collect(Collectors.toList()));

        Page<Prime> page3 = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(43L, page2.nextPageRequest());

        assertEquals(3L, page3.totalPages());
        assertEquals(14L, page3.totalElements());

        assertIterableEquals(List.of(3L, 2L),
                             page3.stream().map(p -> p.numberId).collect(Collectors.toList()));
    }

    /**
     * Obtain total counts of number of elements and pages when JPQL with 4 clauses
     * (SELECT, FROM, WHERE, ORDER BY) is supplied via the Query annotation, with a
     * count query automatically generated from the primary query.
     */
    @Test
    public void testTotalCountsForFullQuery() {
        Page<Map.Entry<Long, String>> page1 = primes.namesByNumber(47L, PageRequest.ofSize(5));

        assertEquals(15L, page1.totalElements());
        assertEquals(3L, page1.totalPages());

        assertIterableEquals(List.of("eleven", "five", "forty-one", "forty-seven", "forty-three"),
                             page1.stream().map(e -> e.getValue()).collect(Collectors.toList()));

        Page<Map.Entry<Long, String>> page2 = primes.namesByNumber(47L, page1.nextPageRequest());

        assertEquals(3L, page2.totalPages());
        assertEquals(15L, page2.totalElements());

        assertIterableEquals(List.of("nineteen", "seven", "seventeen", "thirteen", "thirty-one"),
                             page2.stream().map(e -> e.getValue()).collect(Collectors.toList()));

        Page<Map.Entry<Long, String>> page3 = primes.namesByNumber(47L, page2.nextPageRequest());

        assertEquals(3L, page2.totalPages());
        assertEquals(15L, page2.totalElements());

        assertIterableEquals(List.of("thirty-seven", "three", "twenty-nine", "twenty-three", "two"),
                             page3.stream().map(e -> e.getValue()).collect(Collectors.toList()));

        assertEquals(false, page3.hasNext());
    }

    /**
     * Obtain total counts of elements when various JDQL queries are supplied
     * that lack different optional clauses.
     */
    @Test
    public void testTotalCountsForQueriesWithSomeClausesOmitted() {
        receipts.removeIfTotalUnder(Float.MAX_VALUE);

        receipts.insertAll(List.of(new Receipt(5001, "TCFQWSCO-1", 51.01f),
                                   new Receipt(5002, "TCFQWSCO-2", 52.42f),
                                   new Receipt(5003, "TCFQWSCO-3", 50.33f),
                                   new Receipt(5004, "TCFQWSCO-2", 52.24f),
                                   new Receipt(5005, "TCFQWSCO-5", 56.95f)));

        Page<Receipt> page1;
        PageRequest page1req = PageRequest.ofSize(3).withTotal();

        // query with no clauses
        page1 = receipts.all(page1req, Order.by(Sort.asc(ID)));
        assertEquals(5, page1.totalElements());

        receipts.insert(new Receipt(5006, "TCFQWSCO-5", 56.56f));

        // query with FROM clause only
        page1 = receipts.all(page1req, Sort.desc(ID));
        assertEquals(6, page1.totalElements());

        receipts.insert(new Receipt(5007, "TCFQWSCO-7", 57.17f));

        // query with FROM clause only
        page1 = receipts.sortedByTotalIncreasing(page1req);
        assertEquals(7, page1.totalElements());
        assertEquals(5003, page1.iterator().next().purchaseId());

        receipts.insert(new Receipt(5008, "TCFQWSCO-8", 58.88f));

        // query with SELECT clause only
        Page<Float> amountsPage1;
        amountsPage1 = receipts.totals(page1req, Sort.asc(ID));
        assertEquals(8, amountsPage1.totalElements());

        receipts.insert(new Receipt(5009, "TCFQWSCO-9", 59.09f));

        // query with SELECT and ORDER BY clauses only
        amountsPage1 = receipts.totalsDecreasing(page1req);
        assertEquals(9, amountsPage1.totalElements());

        assertEquals(9, receipts.removeIfTotalUnder(Float.MAX_VALUE));
    }

    /**
     * Obtain total counts of number of elements and pages when JDQL is supplied via
     * the Query annotation and the provided query lacks a FROM clause.
     */
    @Test
    public void testTotalCountsForQueryWithInferredFromClause() {
        // The prime numbers less than 50 where the roman numeral length is at least
        // twice the name length are:
        // 2, 3, 7, 13, 37

        Page<String> page1 = primes.lengthBasedQuery(PageRequest.ofSize(3));

        assertEquals(List.of("two", "three", "thirty-seven"),
                     page1.content());

        assertEquals(3, page1.numberOfElements());
        assertEquals(5L, page1.totalElements());
        assertEquals(2L, page1.totalPages());
        assertEquals(true, page1.hasNext());

        Page<String> page2 = primes.lengthBasedQuery(page1.nextPageRequest());

        assertEquals(List.of("thirteen", "seven"),
                     page2.content());

        assertEquals(false, page2.hasNext());
    }

    /**
     * Obtain total counts of number of elements and pages when JPQL is supplied via the Query annotation
     * where a count query is inferred from the Query annotation value, which has an ORDER BY clause.
     */
    @SkipIfSysProp(DB_Oracle) //SQLSyntaxErrorException ORA-00918: LENGTH(ROMANNUMERAL): column ambiguously specified - appears in  and
    // Call: SELECT * FROM (SELECT a.*, ROWNUM rnum  FROM (SELECT DISTINCT LENGTH(ROMANNUMERAL), LENGTH(ROMANNUMERAL) FROM WLPPrime WHERE (NUMBERID <= ?) ORDER BY LENGTH(ROMANNUMERAL) DESC) a WHERE ROWNUM <= ?) WHERE rnum > ?
    @Test
    public void testTotalCountsForQueryWithOrderBy() {
        Page<Integer> page1 = primes.romanNumeralLengths(41L, PageRequest.ofSize(4));

        assertEquals(6L, page1.totalElements());
        assertEquals(2L, page1.totalPages());

        assertIterableEquals(List.of(6, 5, 4, 3), page1.content());

        Page<Integer> page2 = primes.romanNumeralLengths(41L, page1.nextPageRequest());

        assertEquals(2L, page2.totalPages());
        assertEquals(6L, page2.totalElements());

        assertIterableEquals(List.of(2, 1), page2.content());

        assertEquals(false, page2.hasNext());
    }

    /**
     * Obtain total counts of number of elements and pages when JPQL is supplied via the Query annotation
     * where a count query is inferred from the Query annotation value, which lacks an ORDER BY clause
     * because the OrderBy annotation is used.
     */
    @Test
    public void testTotalCountsForQueryWithSeparateOrderBy() {
        Page<Object[]> page1 = primes.namesWithHex(40L, PageRequest.ofSize(4));

        assertEquals(12L, page1.totalElements());
        assertEquals(3L, page1.totalPages());

        assertIterableEquals(List.of("two", "three", "five", "seven"),
                             page1.stream().map(o -> (String) o[0]).collect(Collectors.toList()));

        Page<Object[]> page2 = primes.namesWithHex(40L, page1.nextPageRequest());

        assertEquals(3L, page2.totalPages());
        assertEquals(12L, page2.totalElements());

        assertIterableEquals(List.of("eleven", "thirteen", "seventeen", "nineteen"),
                             page2.stream().map(o -> (String) o[0]).collect(Collectors.toList()));

        Page<Object[]> page3 = primes.namesWithHex(40L, page2.nextPageRequest());

        assertEquals(3L, page3.totalPages());
        assertEquals(12L, page3.totalElements());

        assertIterableEquals(List.of("twenty-three", "twenty-nine", "thirty-one", "thirty-seven"),
                             page3.stream().map(o -> (String) o[0]).collect(Collectors.toList()));

        assertEquals(false, page3.hasNext());
    }

    /**
     * Obtain total counts of number of elements and pages when cursor-based
     * pagination is used.
     */
    @Test
    public void testTotalCountsWithCursorPagination() {
        CursoredPage<Prime> page3 = primes.findByNumberIdBetween(3L, 50L, PageRequest.ofPage(3)
                        .size(5)
                        .beforeCursor(Cursor.forKey(47L)));
        assertEquals(14L, page3.totalElements());
        assertEquals(3L, page3.totalPages());

        assertIterableEquals(List.of(29L, 31L, 37L, 41L, 43L),
                             page3.stream().map(p -> p.numberId).collect(Collectors.toList()));

        CursoredPage<Prime> page2 = primes.findByNumberIdBetween(3L, 50L, page3.previousPageRequest());
        assertEquals(3L, page2.totalPages());
        assertEquals(14L, page2.totalElements());

        assertIterableEquals(List.of(11L, 13L, 17L, 19L, 23L),
                             page2.stream().map(p -> p.numberId).collect(Collectors.toList()));

        CursoredPage<Prime> page1 = primes.findByNumberIdBetween(3L, 50L, page2.previousPageRequest());
        assertEquals(3L, page1.totalPages());
        assertEquals(14L, page1.totalElements());

        assertIterableEquals(List.of(3L, 5L, 7L),
                             page1.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(false, page1.hasPrevious());

        CursoredPage<Prime> page4 = primes.findByNumberIdBetween(3L, 50L, page3.nextPageRequest());
        // In this case, the 14 elements are across 4 pages, not 3,
        // because the first and last pages ended up being partial.
        // But that doesn't become known until the first or last page is read.
        // This is one of many reasons why CursoredPage documents that
        // page counts are inaccurate and cannot be relied upon.
        assertEquals(3L, page4.totalPages());
        assertEquals(14L, page4.totalElements());

        assertIterableEquals(List.of(47L),
                             page4.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(false, page4.hasNext());
    }

    /**
     * Use a repository interface that uses the Transactional annotation to manage transactions.
     */
    @Test
    public void testTransactional() throws ExecutionException, IllegalStateException, InterruptedException, //
                    NotSupportedException, SecurityException, SystemException, TimeoutException {
        personnel.removeAll().get(TIMEOUT_MINUTES, TimeUnit.MINUTES);

        Person p1 = new Person();
        p1.firstName = "Thomas";
        p1.lastName = "TestTransactional";
        p1.ssn_id = 300201001;

        Person p2 = new Person();
        p2.firstName = "Timothy";
        p2.lastName = "TestTransactional";
        p2.ssn_id = 300201002;

        Person p3 = new Person();
        p3.firstName = "Tyler";
        p3.lastName = "TestTransactional";
        p3.ssn_id = 300201003;

        personRepo.save(List.of(p1, p2, p3));

        System.out.println("TxType.SUPPORTS in transaction");

        tran.begin();
        try {
            assertEquals(true, personRepo.setFirstNameInCurrentTransaction(p3.ssn_id, "Ty")); // update with MANDATORY
            assertEquals("Ty", personRepo.getPersonInCurrentOrNoTransaction(p3.ssn_id).firstName); // read value with SUPPORTS
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Thomas", "Timothy", "Tyler"),
                             personRepo.findFirstNames("TestTransactional"));

        System.out.println("TxType.SUPPORTS from no transaction");

        assertEquals("Tyler", personRepo.getPersonInCurrentOrNoTransaction(p3.ssn_id).firstName);

        System.out.println("TxType.REQUIRED in transaction");

        tran.begin();
        try {
            assertEquals(true, personRepo.setFirstNameInCurrentOrNewTransaction(p1.ssn_id, "Tommy"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Thomas", "Timothy", "Tyler"),
                             personRepo.findFirstNames("TestTransactional"));

        System.out.println("TxType.REQUIRED from no transaction");

        assertEquals(true, personRepo.setFirstNameInCurrentOrNewTransaction(p1.ssn_id, "Tom"));

        assertIterableEquals(List.of("Timothy", "Tom", "Tyler"),
                             personRepo.findFirstNames("TestTransactional"));

        System.out.println("TxType.MANDATORY in transaction");

        tran.begin();
        try {
            assertEquals(true, personRepo.setFirstNameInCurrentTransaction(p3.ssn_id, "Ty"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Timothy", "Tom", "Tyler"),
                             personRepo.findFirstNames("TestTransactional"));

        System.out.println("TxType.MANDATORY from no transaction is an error");

        try {
            boolean result = personRepo.setFirstNameInCurrentTransaction(p3.ssn_id, "Ty");
            fail("Invoked TxType.MANDATORY operation with no transaction on thread. Result: " + result);
        } catch (TransactionalException x) {
            if (!(x.getCause() instanceof TransactionRequiredException))
                throw x;
        }

        System.out.println("TxType.REQUIRES_NEW in transaction");

        tran.begin();
        try {
            assertEquals(true, personRepo.setFirstNameInNewTransaction(p2.ssn_id, "Timmy"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Timmy", "Tom", "Tyler"),
                             personRepo.findFirstNames("TestTransactional"));

        System.out.println("TxType.REQUIRES_NEW from no transaction");

        assertEquals(true, personRepo.setFirstNameInCurrentOrNewTransaction(p2.ssn_id, "Tim"));

        assertIterableEquals(List.of("Tim", "Tom", "Tyler"),
                             personRepo.findFirstNames("TestTransactional"));

        System.out.println("TxType.NEVER in transaction");

        tran.begin();
        try {
            boolean result = personRepo.setFirstNameWhenNoTransactionIsPresent(p3.ssn_id, "Ty");
            fail("Invoked TxType.NEVER operation with transaction on thread. Result: " + result);
        } catch (TransactionalException x) {
            if (!(x.getCause() instanceof InvalidTransactionException))
                throw x;
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Tim", "Tom", "Tyler"),
                             personRepo.findFirstNames("TestTransactional"));

        System.out.println("TxType.NEVER from no transaction");

        assertEquals(true, personRepo.setFirstNameWhenNoTransactionIsPresent(p3.ssn_id, "Ty"));

        assertIterableEquals(List.of("Tim", "Tom", "Ty"),
                             personRepo.findFirstNames("TestTransactional"));

        System.out.println("TxType.NOT_SUPPORTED in transaction");

        tran.begin();
        try {
            assertEquals(true, personRepo.setFirstNameWithCurrentTransactionSuspended(p3.ssn_id, "Tyler"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Tim", "Tom", "Tyler"),
                             personRepo.findFirstNames("TestTransactional"));

        System.out.println("TxType.NOT_SUPPORTED from no transaction");

        assertEquals("Tyler", personRepo.getPersonInCurrentOrNoTransaction(p3.ssn_id).firstName);

        personnel.removeAll().get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Test that a repository interface can be annotated as Transactional, and the
     * methods honor the specified transaction type of REQUIRES_NEW.
     */
    @Test
    public void testTransactionalRepository() throws Exception {
        people.deleteBySSN_IdBetween(0L, 999999999L);

        Person p1 = new Person();
        p1.firstName = "Tabitha";
        p1.lastName = "TestTransactionalRepository";
        p1.ssn_id = 555331111;

        Person p2 = new Person();
        p2.firstName = "Todd";
        p2.lastName = "TestTransactionalRepository";
        p2.ssn_id = 444332222;

        Person p3 = new Person();
        p3.firstName = "Ted";
        p3.lastName = "TestTransactionalRepository";
        p3.ssn_id = 111223333;

        tran.begin();
        try {
            persons.insert(p1); // runs in current transaction

            persons.insertAll(p2, p3); // runs in its own transaction
        } finally {
            tran.rollback();
        }

        // insert again, because the previous should have rolled back
        persons.insert(p1);

        tran.begin();
        try {
            p1.lastName = "Test-TransactionalRepository";
            assertEquals(true, persons.updateOne(p1));

            p2.lastName = "TestTransactional-Repository";
            p3.firstName = "Theodore";
            assertEquals(2L, persons.updateSome(p2, p3));
        } finally {
            // The above must run in their own separate transactions,
            // so they must not roll back along with the following:
            tran.rollback();
        }

        p1 = personRepo.getPersonInCurrentOrNoTransaction(p1.ssn_id);
        assertEquals("Tabitha", p1.firstName);
        assertEquals("Test-TransactionalRepository", p1.lastName);

        p2 = personRepo.getPersonInCurrentOrNoTransaction(p2.ssn_id);
        assertEquals("Todd", p2.firstName);
        assertEquals("TestTransactional-Repository", p2.lastName);

        p3 = personRepo.getPersonInCurrentOrNoTransaction(p3.ssn_id);
        assertEquals("Theodore", p3.firstName);
        assertEquals("TestTransactionalRepository", p3.lastName);

        assertEquals(3L, people.deleteBySSN_IdBetween(0L, 999999999L));
    }

    /**
     * Test the Trimmed keyword by querying against data that has leading and trailing blank space.
     */
    @Test
    public void testTrimmedKeyword() {
        List<Prime> found = primes.findByNameTrimmedCharCountAndNumberIdBetween(24, 4000L, 4025L);
        assertNotNull(found);
        assertEquals("Found: " + found, 1, found.size());
        assertEquals(4021L, found.get(0).numberId);
        assertEquals(" Four thousand twenty-one ", found.get(0).name);

        Prime prime = primes.findByNameTrimmedIgnoreCase("FOUR THOUSAND TWENTY-ONE").orElseThrow();
        assertEquals(4021L, prime.numberId);
        assertEquals(" Four thousand twenty-one ", prime.name);
    }

    /**
     * Update multiple entries.
     */
    @Test
    public void testUpdateMultiple() {
        products.clear();

        assertEquals(0, products.putOnSale("TestUpdateMultiple-match", .10f));

        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("800-2024-S".getBytes());
        prod1.name = "Small size TestUpdateMultiple-matched item";
        prod1.price = 10.00f;
        products.save(prod1);

        Product prod2 = new Product();
        prod2.pk = UUID.nameUUIDFromBytes("800-3024-M".getBytes());
        prod2.name = "Medium size TestUpdateMultiple-matched item";
        prod2.price = 15.00f;
        products.save(prod2);

        Product prod3 = new Product();
        prod3.pk = UUID.nameUUIDFromBytes("C6000-814BH0003Y".getBytes());
        prod3.name = "Medium size TestUpdateMultiple non-matching item";
        prod3.price = 18.00f;
        products.save(prod3);

        Product prod4 = new Product();
        prod4.pk = UUID.nameUUIDFromBytes("800-4024-L".getBytes());
        prod4.name = "Large size TestUpdateMultiple-matched item";
        prod4.price = 20.00f;
        products.save(prod4);

        Product[] p = products.findByVersionGreaterThanEqualOrderByPrice(0);

        // JPA knows to update the version even through the JPQL didn't explicitly tell it to
        assertEquals(3, products.putOnSale("TestUpdateMultiple-match", .20f));

        Product p1 = products.findItem(prod1.pk);
        assertEquals(8.00f, p1.price, 0.001f);
        assertEquals(p[0].version + 1, p1.version); // updated

        Product p2 = products.findItem(prod2.pk);
        assertEquals(12.00f, p2.price, 0.001f);
        assertEquals(p[1].version + 1, p2.version); // updated

        Product p3 = products.findItem(prod3.pk);
        assertEquals(18.00f, p3.price, 0.001f);
        assertEquals(p[2].version, p3.version); // not updated, version remains the same

        Product p4 = products.findItem(prod4.pk);
        assertEquals(16.00f, p4.price, 0.001f);
        assertEquals(p[3].version + 1, p4.version); // updated

        products.clear();
    }

    /**
     * Use update methods with an entity parameter to make updates.
     */
    @AllowedFFDC("jakarta.data.exceptions.OptimisticLockingFailureException")
    @Test
    public void testUpdateWithEntityParameter() {
        people.deleteBySSN_IdBetween(0L, 999999999L);

        Person ursula = new Person();
        ursula.firstName = "Ursula";
        ursula.lastName = "TestUpdateWithEntityParameter";
        ursula.ssn_id = 987001001;

        Person ulysses = new Person();
        ulysses.firstName = "Ulysses";
        ulysses.lastName = "TestUpdateWithEntityParameter";
        ulysses.ssn_id = 987001002;

        Person uriel = new Person();
        uriel.firstName = "Uriel";
        uriel.lastName = "TestUpdateWithEntityParameter";
        uriel.ssn_id = 987001003;

        Person uriah = new Person();
        uriah.firstName = "Uriah";
        uriah.lastName = "TestUpdateWithEntityParameter";
        uriah.ssn_id = 987001004;

        Person urban = new Person();
        urban.firstName = "Urban";
        urban.lastName = "TestUpdateWithEntityParameter";
        urban.ssn_id = 987001005;

        people.updateOrAdd(List.of(ursula, ulysses, uriel, uriah));

        // update single entity:

        ulysses.lastName = "Test-UpdateWithEntityParameter";
        assertEquals(true, persons.updateOne(ulysses));

        try {
            persons.updateOne(urban); // not in database
            fail("An attempt to update an entity that is not in the database must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        // update multiple entities:

        ulysses.lastName = "TestUpdate-WithEntityParameter";
        ursula.lastName = "TestUpdate-WithEntityParameter";
        uriah.lastName = "TestUpdate-WithEntityParameter";

        try {
            assertEquals(3, persons.updateSome(ulysses, urban, ursula, uriah)); // one is not in the database
            fail("An attempt to update multiple entities where one is not in the database must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        assertEquals(3, persons.updateSome(ulysses, ursula, uriah));

        assertEquals(4, people.deleteBySSN_IdBetween(0L, 999999999L));
    }

    /**
     * Use update methods with a versioned entity parameter to make updates.
     */
    @Test
    public void testUpdateWithVersionedEntityParameter() {
        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("UPD-VER-EP-1".getBytes());
        prod1.name = "testUpdateWithVersionedEntityParameter Product 1";
        prod1.price = 10.99f;

        Product prod2 = new Product();
        prod2.pk = UUID.nameUUIDFromBytes("UPD-VER-EP-2".getBytes());
        prod2.name = "testUpdateWithVersionedEntityParameter Product 2";
        prod2.price = 12.99f;

        Product prod3 = new Product();
        prod3.pk = UUID.nameUUIDFromBytes("UPD-VER-EP-3".getBytes());
        prod3.name = "testUpdateWithVersionedEntityParameter Product 3";
        prod3.price = 13.99f;

        Product[] p = products.saveMultiple(prod1, prod2, prod3);
        prod1 = p[0];
        prod2 = p[1];
        prod3 = p[2];

        // versioned update to 1 entity:

        prod1.price = 10.79f;
        assertEquals(Boolean.TRUE, products.update(prod1)); // current version

        prod1.price = 10.89f;
        try {
            products.update(prod1); // old version
            fail("An attempt to update an entity with an outdated version must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        // versioned update to multiple entities:

        prod2.price = 12.89f;
        prod3.price = 13.89f;
        try {
            assertEquals(Long.valueOf(2), products.update(Stream.of(prod2, prod3, prod1))); // 1 with old version
            fail("An attempt to update multiple entities where one has an outdated version must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        assertEquals(Long.valueOf(2), products.update(Stream.of(prod2, prod3)));
    }

    /**
     * Use JPQL query to update based on version.
     */
    @Test
    public void testVersionedUpdateViaQuery() {
        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("Q6008-U8-21001".getBytes());
        prod1.name = "testVersionedUpdateViaQuery Product 1";
        prod1.price = 82.99f;
        prod1 = products.upsert(prod1);

        long initialVersion = prod1.version;

        assertEquals(true, products.setPrice(prod1.pk, initialVersion, 84.99f));
        assertEquals(false, products.setPrice(prod1.pk, initialVersion, 83.99f));
        assertEquals(true, products.setPrice(prod1.pk, initialVersion + 1, 88.99f));

        prod1 = products.findItem(prod1.pk);
        assertEquals(88.99f, prod1.price, 0.001f);
        assertEquals(initialVersion + 2, prod1.version);

        prod1.version = initialVersion + 1;
        prod1.price = 89.99f;
        try {
            prod1 = products.upsert(prod1);
            fail("Should not be able to save an update at an old version.");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        prod1.version = initialVersion + 2;
        prod1.price = 90.99f;
        prod1 = products.upsert(prod1);
        assertEquals(90.99f, prod1.price, 0.001f);
        assertEquals(initialVersion + 3, prod1.version);
    }

    /**
     * Use repository save method to update based on version.
     */
    @Test
    public void testVersionedUpdateViaRepository() {
        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("3400R-6120-1".getBytes());
        prod1.name = "TestVersionedUpdateViaRepository Product 1";
        prod1.price = 139.99f;
        products.save(prod1);

        Product prod1a = products.findItem(prod1.pk);
        Product prod1b = products.findItem(prod1.pk);

        long version;
        assertEquals(version = prod1a.version, prod1b.version);

        prod1a.price += 15.00f;
        prod1b.price += 10.00f;

        products.save(prod1b);

        try {
            products.save(prod1a);
            fail("Able to update using old version.");
        } catch (OptimisticLockingFailureException x) {
            if (x.getCause() != null && "jakarta.persistence.OptimisticLockException".equals(x.getCause().getClass().getName()))
                ; // expected;
            else
                throw x;
        }

        Product p = products.findItem(prod1.pk);
        assertEquals(149.99f, p.price, 0.001f);
        assertEquals(version + 1, p.version);
    }

    /**
     * Use a repository method that specifies query language consisting only of a WHERE clause and ORDER BY clause
     * and requests offset pagination with a total count of pages.
     */
    @Test
    public void testWhereAndOrderByClauseOnly() {
        // The prime numbers between 10 and 43:
        // 11, 13, 17, 19, 23, 29, 31, 37, 41, 43
        Page<Prime> page1 = primes.within10toXAndSortedByName(43, PageRequest.ofSize(5));

        assertEquals(List.of("eleven", "forty-one", "forty-three", "nineteen", "seventeen"),
                     page1.stream()
                                     .map(p -> p.name)
                                     .collect(Collectors.toList()));

        assertEquals(5, page1.numberOfElements());
        assertEquals(10L, page1.totalElements());
        assertEquals(2L, page1.totalPages());
        assertEquals(true, page1.hasNext());

        Page<Prime> page2 = primes.within10toXAndSortedByName(43, page1.nextPageRequest());

        assertEquals(List.of("thirteen", "thirty-one", "thirty-seven", "twenty-nine", "twenty-three"),
                     page2.stream()
                                     .map(p -> p.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method that specifies query language consisting only of a WHERE clause
     * and requests offset pagination with a total count of pages.
     */
    @Test
    public void testWhereClauseOnly() {
        // The prime numbers between 6 and 36:
        // 7, 11, 13, 17, 19, 23, 29, 31
        Page<Prime> page1 = primes.within(6, 36, PageRequest.ofSize(3), Sort.desc(ID));

        assertEquals(List.of(31L, 29L, 23L),
                     page1.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        assertEquals(3, page1.numberOfElements());
        assertEquals(8L, page1.totalElements());
        assertEquals(3L, page1.totalPages());
        assertEquals(true, page1.hasNext());

        Page<Prime> page2 = primes.within(6, 36, page1.nextPageRequest(), Sort.desc(ID));

        assertEquals(List.of(19L, 17L, 13L),
                     page2.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        assertEquals(true, page2.hasNext());

        Page<Prime> page3 = primes.within(6, 36, page2.nextPageRequest(), Sort.desc(ID));

        assertEquals(List.of(11L, 7L),
                     page3.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        assertEquals(false, page3.hasNext());
    }
}
