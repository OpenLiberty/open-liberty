/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static test.jakarta.data.web.Assertions.assertArrayEquals;
import static test.jakarta.data.web.Assertions.assertIterableEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
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
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.Streamable;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.KeysetAwarePage;
import jakarta.data.page.KeysetAwareSlice;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.Slice;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

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
    Houses houses;

    @Inject
    MultiRepository multi;

    @Inject
    Packages packages;

    @Inject
    People people;

    @Inject
    Personnel personnel;

    @Inject
    PersonRepo persons;

    // Only add to this repository within the Servlet.init method so that all tests can rely on its data:
    @Inject
    Primes primes;

    @Inject
    Products products;

    @Inject
    Receipts receipts;

    @Inject
    Reservations reservations;

    @Inject
    Shipments shipments;

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
     * Use repository methods with aggregate functions in the select clause.
     */
    @Test
    public void testAggregateFunctions() {
        // Remove data from previous test:
        Product[] allProducts = products.findByVersionGreaterThanEqualOrderByPrice(-1);
        if (allProducts.length > 0)
            products.discontinueProducts(Arrays.stream(allProducts).map(p -> p.pk).collect(Collectors.toSet()));

        // Add data for this test to use:
        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("AF-006E905-LE".getBytes());
        prod1.name = "TestAggregateFunctions Lite Edition";
        prod1.price = 104.99f;
        products.save(prod1);

        Product prod2 = new Product();
        prod2.pk = UUID.nameUUIDFromBytes("AF-006E005-RK".getBytes());
        prod2.name = "TestAggregateFunctions Repair Kit";
        prod2.price = 104.99f;
        products.save(prod2);

        Product prod3 = new Product();
        prod3.pk = UUID.nameUUIDFromBytes("AF-006E905-CE".getBytes());
        prod3.name = "TestAggregateFunctions Classic Edition";
        prod3.price = 306.99f;
        products.save(prod3);

        Product prod4 = new Product();
        prod4.pk = UUID.nameUUIDFromBytes("AF-006E205-CE".getBytes());
        prod4.name = "TestAggregateFunctions Classic Edition";
        prod4.description = "discontinued";
        prod4.price = 286.99f;
        products.save(prod4);

        assertEquals(306.99f, products.highestPrice(), 0.001f);

        assertEquals(104.99f, products.lowestPrice(), 0.001f);

        assertEquals(200.99f, products.meanPrice(), 0.001f);

        assertEquals(698.97f, products.totalOfDistinctPrices(), 0.001f);

        // EclipseLink says that multiple distinct attribute are not support at this time,
        // so we are testing this with distinct=false
        ProductCount stats = products.stats();
        assertEquals(4, stats.totalNames);
        assertEquals(1, stats.totalDescriptions);
        assertEquals(4, stats.totalPrices);
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

        // Find with Streamable and various Collectors

        Streamable<String> aNames = personnel.namesThatStartWith("A");

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

        Streamable<String> bNames = personnel.namesThatStartWith("B");

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
                             primes.findByEvenFalseAndIdLessThan(10L)
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(7L, 5L, 3L),
                             primes.findByEvenNotTrueAndIdLessThan(10L)
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(2L),
                             primes.findByEvenTrueAndIdLessThan(10L)
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(2L),
                             primes.findByEvenNotFalseAndIdLessThan(10L)
                                             .stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));
    }

    /**
     * Test the CharCount Function to query based on string length.
     */
    @Test
    public void testCharCountFunction() {
        assertIterableEquals(List.of("eleven", "five", "seven", "three"),
                             primes.whereNameLengthWithin(4, 6)
                                             .map(p -> p.name)
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
     * Asynchronous repository method that returns a CompletionStage of KeysetAwarePage.
     */
    @Test
    public void testCompletionStageOfPage() throws ExecutionException, InterruptedException, TimeoutException {
        LinkedBlockingQueue<Long> sums = new LinkedBlockingQueue<Long>();

        primes.findByNumberIdLessThanOrderByIdDesc(42L, PageRequest.ofSize(6)).thenCompose(page1 -> {
            sums.add(page1.stream().mapToLong(p -> p.numberId).sum());
            return primes.findByNumberIdLessThanOrderByIdDesc(42L, page1.nextPageRequest());
        }).thenCompose(page2 -> {
            sums.add(page2.stream().mapToLong(p -> p.numberId).sum());
            return primes.findByNumberIdLessThanOrderByIdDesc(42L, page2.nextPageRequest());
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
     * Count the number of matching entries in the database using query by method name.
     */
    @Test
    public void testCount() throws ExecutionException, InterruptedException, TimeoutException {

        assertEquals(15, primes.countByIdLessThan(50));

        assertEquals(Integer.valueOf(0), primes.countByNumberIdBetween(32, 36));

        assertEquals(Integer.valueOf(3), primes.countByNumberIdBetween(40, 50));

        assertEquals(Short.valueOf((short) 14), primes.countByIdBetweenAndEvenNot(0, 50, true).get(TIMEOUT_MINUTES, TimeUnit.MINUTES));
    }

    /**
     * Parameter-based query with the Count annotation to indicate that it performs a count rather than a find operation.
     */
    @Test
    public void testCountAnnoParameterBasedQuery() {
        assertEquals(1, primes.numEvenWithSumOfBits(1, true));
        assertEquals(0, primes.numEvenWithSumOfBits(1, false));
        assertEquals(0, primes.numEvenWithSumOfBits(2, true));
        assertEquals(3, primes.numEvenWithSumOfBits(2, false));
    }

    /**
     * Count the number of matching entries in the database using annotatively defined queries.
     */
    @Test
    public void testCountAnnotation() throws ExecutionException, InterruptedException, TimeoutException {

        assertEquals(6, primes.howManyIn(17L, 37L));
        assertEquals(0, primes.howManyIn(24L, 28L));

        assertEquals(Long.valueOf(5), primes.howManyBetweenExclusive(5, 20));
        assertEquals(Long.valueOf(0), primes.howManyBetweenExclusive(19, 20));
        assertEquals(Long.valueOf(0), primes.howManyBetweenExclusive(21, 20));
    }

    /**
     * Use a repository that inherits from a custom repository interface with type parameters indicating the entity and key types.
     */
    @Test
    public void testCustomRepositoryInterface() {
        people.deleteByIdBetween(400000000L, 499999999L);

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

        assertEquals(3L, people.countByIdBetween(400000000L, 499999999L));

        Person p4 = new Person();
        p4.firstName = "Cathy";
        p4.lastName = "TestCustomRepositoryInterface";
        p4.ssn_id = p1.ssn_id;
        people.updateOrAdd(List.of(p4));

        assertEquals(List.of("Cathy", "Charles", "Claire"),
                     Arrays.stream(people.findByLastName("TestCustomRepositoryInterface"))
                                     .map(p -> p.firstName)
                                     .collect(Collectors.toList()));

        assertEquals(1L, people.deleteByIdBetween(400000000L, 449999999L));

        assertEquals(2L, people.deleteByIdBetween(400000000L, 499999999L));
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

            assertEquals(false, products.findById(prod1.pk).isPresent());
            assertEquals(true, products.findById(prod3.pk).isPresent());
        }

        products.clear();
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
     * Query for distinct values of an attribute.
     */
    @Test
    public void testDistinctAttribute() {
        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("TDA-T-L1".getBytes());
        prod1.name = "TestDistinctAttribute T-Shirt Size Large";
        prod1.price = 7.99f;
        products.save(prod1);

        Product prod2 = new Product();
        prod2.pk = UUID.nameUUIDFromBytes("TDA-T-M1".getBytes());
        prod1.name = "TestDistinctAttribute T-Shirt Size Medium";
        prod2.price = 7.89f;
        products.save(prod2);

        Product prod3 = new Product();
        prod3.pk = UUID.nameUUIDFromBytes("TDA-T-S1".getBytes());
        prod3.name = "TestDistinctAttribute T-Shirt Size Small";
        prod3.price = 7.79f;
        products.save(prod3);

        Product prod4 = new Product();
        prod4.pk = UUID.nameUUIDFromBytes("TDA-T-M2".getBytes());
        prod4.name = "TestDistinctAttribute T-Shirt Size Medium";
        prod4.price = 7.49f;
        products.save(prod4);

        Product prod5 = new Product();
        prod5.pk = UUID.nameUUIDFromBytes("TDA-T-XS1".getBytes());
        prod5.name = "TestDistinctAttribute T-Shirt Size Extra Small";
        prod5.price = 7.59f;
        products.save(prod5);

        Product prod6 = new Product();
        prod6.pk = UUID.nameUUIDFromBytes("TDA-T-L2".getBytes());
        prod6.name = "TestDistinctAttribute T-Shirt Size Large";
        prod6.price = 7.49f;
        products.save(prod6);

        List<String> uniqueProductNames = products.findByNameLike("TestDistinctAttribute %");

        // only 4 of the 6 names are unique
        assertIterableEquals(List.of("TestDistinctAttribute T-Shirt Size Extra Small",
                                     "TestDistinctAttribute T-Shirt Size Large",
                                     "TestDistinctAttribute T-Shirt Size Medium",
                                     "TestDistinctAttribute T-Shirt Size Small"),
                             uniqueProductNames);
    }

    /**
     * Test the ElementCount keyword by querying against a collection attribute with different sizes.
     * Also covers WithMinute and WithSecond.
     */
    @Test
    public void testElementCountAndExtract() throws Exception {
        reservations.deleteByHostNot("nobody");

        final ZoneOffset MDT = ZoneOffset.ofHours(-6);

        Reservation r1 = new Reservation();
        r1.host = "host1@openliberty.io";
        r1.invitees = Set.of("invitee1@openliberty.io", "invitee3@openliberty.io");
        r1.location = "050-2 G105";
        r1.meetingID = 113001;
        r1.start = OffsetDateTime.of(2023, 5, 1, 10, 15, 0, 0, MDT);
        r1.stop = OffsetDateTime.of(2023, 5, 1, 10, 45, 0, 0, MDT);
        r1.setLengthInMinutes(30);

        Reservation r2 = new Reservation();
        r2.host = "host2.openliberty.io";
        r2.invitees = Set.of("invitee2@openliberty.io");
        r2.location = "050-2 B120";
        r2.meetingID = 213002;
        r2.start = OffsetDateTime.of(2023, 5, 1, 10, 15, 0, 0, MDT);
        r2.stop = OffsetDateTime.of(2023, 5, 1, 11, 00, 0, 0, MDT);
        r2.setLengthInMinutes(45);

        Reservation r3 = new Reservation();
        r3.host = "host3@openliberty.io";
        r3.invitees = Set.of("invitee1@openliberty.io", "invitee2@openliberty.io", "invitee3@openliberty.io");
        r3.location = "030-2 A312";
        r3.meetingID = 313003;
        r3.start = OffsetDateTime.of(2022, 5, 24, 9, 35, 30, 0, MDT);
        r3.stop = OffsetDateTime.of(2022, 5, 24, 9, 59, 30, 0, MDT);
        r3.setLengthInMinutes(24);

        Reservation r4 = new Reservation();
        r4.host = "host4@openliberty.io";
        r4.invitees = Set.of("invitee2@openliberty.io", "invitee4@openliberty.io");
        r4.location = "050-2 G105";
        r4.meetingID = 413004;
        r4.start = OffsetDateTime.of(2023, 5, 1, 9, 00, 0, 0, MDT);
        r4.stop = OffsetDateTime.of(2023, 5, 1, 9, 30, 0, 0, MDT);
        r4.setLengthInMinutes(30);

        reservations.saveAll(Set.of(r1, r2, r3, r4));

        // ElementCount keyword

        assertIterableEquals(List.of("host1@openliberty.io", "host4@openliberty.io"),
                             reservations.findByInviteesElementCount(2)
                                             .map(r -> r.host)
                                             .collect(Collectors.toList()));

        assertIterableEquals(Collections.EMPTY_LIST,
                             reservations.findByInviteesElementCount(0)
                                             .map(r -> r.host)
                                             .collect(Collectors.toList()));

        // ElementCount Function

        assertIterableEquals(List.of("host3@openliberty.io"),
                             reservations.withInviteeCount(3)
                                             .map(r -> r.host)
                                             .collect(Collectors.toList()));

        // WithHour, WithMinute. We cannot compare the hour without knowing which time zone the database stores it in.

        assertIterableEquals(List.of(113001L, 213002L),
                             reservations.findMeetingIdByStartWithHourBetweenAndStartWithMinute(0, 23, 15));

        assertIterableEquals(List.of(313003L),
                             reservations.startsWithinHoursWithMinute(0, 23, 35));

        // WithSecond

        assertIterableEquals(List.of(313003L),
                             reservations.findMeetingIdByStopWithSecond(30));

        assertIterableEquals(List.of(113001L, 213002L, 413004L),
                             reservations.endsAtSecond(0));

        reservations.deleteByHostNot("nobody");
    }

    /**
     * Unannotated entity with an attribute that is an embeddable type.
     */
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
                                                  Order.by(_House.id.ascIgnoreCase()))
                                     .map(house -> house.parcelId)
                                     .collect(Collectors.toList()));

        // Find a single attribute type

        assertArrayEquals(new int[] { 180, 200, 220, 220 },
                          houses.findGarageAreaByGarageNotNull());

        // Find a DoubleStream for a single attribute type

        double[] prices = houses.findPurchasePriceByLotSizeGreaterThan(0.15f).toArray();
        assertEquals(Arrays.toString(prices), 4, prices.length);
        assertEquals(153000.0, prices[0], 0.001);
        assertEquals(162000.0, prices[1], 0.001);
        assertEquals(188000.0, prices[2], 0.001);
        assertEquals(204000.0, prices[3], 0.001);

        assertEquals(2L, houses.deleteByKitchenWidthGreaterThan(12));

        // Find a subset of attributes

        Object[] tuple = houses.findGarageDoorAndKitchenLengthAndKitchenWidthById("TestEmbeddable-304-3655-30").orElseThrow();
        GarageDoor door = (GarageDoor) tuple[0];
        assertEquals(8, door.getHeight());
        assertEquals(9, door.getWidth());
        assertEquals(Integer.valueOf(14), tuple[1]); // kitchen length
        assertEquals(Integer.valueOf(12), tuple[2]); // kitchen width

        assertIterableEquals(List.of("[14, 12, 180, 1700]", "[15, 12, 200, 1800]"),
                             houses.findKitchenLengthAndKitchenWidthAndGarageAreaAndAreaByAreaLessThan(2000)
                                             .map(Arrays::toString)
                                             .collect(Collectors.toList()));

        // Update embeddable attributes

        assertEquals(true, houses.updateByIdSetGarageAddAreaAddKitchenLengthSetNumBedrooms("TestEmbeddable-304-3655-30", null, 180, 2, 4));

        h = houses.findById("TestEmbeddable-304-3655-30");
        assertEquals("TestEmbeddable-304-3655-30", h.parcelId);
        assertEquals(1880, h.area);
        // Null embeddables aren't required by JPA, but EclipseLink claims to support it as the default behavior.
        // See https://wiki.eclipse.org/EclipseLink/UserGuide/JPA/Basic_JPA_Development/Entities/Embeddable#Nullable_embedded_values
        // But it looks like EclipseLink has a bug here in that it only nulls out 1 of the fields of Garage, not all,
        // JPQL: UPDATE House o SET o.garage=?2, o.area=o.area+?3, o.kitchen.length=o.kitchen.length+?4, o.numBedrooms=?5 WHERE (o.parcelId=?1)
        // SQL:  UPDATE WLPHouse SET NUMBEDROOMS = 4, AREA = (AREA + 180), GARAGEAREA = NULL, KITCHENLENGTH = (KITCHENLENGTH + 2) WHERE (PARCELID = 'TestEmbeddable-304-3655-30')
        // This causes the following assertion to fail:
        // assertEquals(null, h.garage);
        // TODO re-enable the above if fixed
        assertNotNull(h.kitchen);
        assertEquals(16, h.kitchen.length);
        assertEquals(12, h.kitchen.width);
        assertEquals(0.17f, h.lotSize, 0.001f);
        assertEquals(4, h.numBedrooms);
        assertEquals(153000f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2018), h.sold);

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
     * Identify whether elements exist in the database using query by method name.
     */
    @Test
    public void testExists() {
        assertEquals(true, primes.existsByNumberId(19));
        assertEquals(false, primes.existsByNumberId(9));

        assertEquals(Boolean.TRUE, primes.existsByIdBetween(Long.valueOf(14), Long.valueOf(18)));
        assertEquals(Boolean.FALSE, primes.existsByIdBetween(Long.valueOf(24), Long.valueOf(28)));
    }

    /**
     * Identify whether elements exist in the database using an annotatively defined query.
     */
    @Test
    public void testExistsAnnotation() {
        assertEquals(true, primes.anyLessThanEndingWithBitPattern(25L, "1101"));
        assertEquals(false, primes.anyLessThanEndingWithBitPattern(25L, "1111"));
        assertEquals(false, primes.anyLessThanEndingWithBitPattern(12L, "1101"));
    }

    /**
     * Parameter-based query with the Exists annotation.
     */
    @Test
    public void testExistsAnnotationWithParameterBasedQuery() {
        assertEquals(true, primes.isFoundWith(47, "2F"));
        assertEquals(false, primes.isFoundWith(41, "2F")); // 2F is not hex for 41 decimal
        assertEquals(false, primes.isFoundWith(15, "F")); // not prime
    }

    /**
     * Define a parameter-based find operation with IgnoreCase, Like, and Contains annotations.
     */
    @Test
    public void testFind() {
        assertIterableEquals(List.of(37L, 17L, 7L, 5L), // 11 has no V in the roman numeral and 47 is too big
                             primes.inRangeHavingNumeralLikeAndSubstringOfName(5L, 45L, "%v%", "ve"));

        assertIterableEquals(List.of(),
                             primes.inRangeHavingNumeralLikeAndSubstringOfName(1L, 18L, "%v%", "nine"));
    }

    /**
     * Query-by-method name repository operation to remove and return one or more entities.
     */
    // Test annotation is present on corresponding method in DataTest
    public void testFindAndDelete(HttpServletRequest request, HttpServletResponse response) {
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

        String jdbcJarName = request.getParameter("jdbcJarName").toLowerCase();
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
    // Test annotation is present on corresponding method in DataTest
    public void testFindAndDeleteMultipleAnnotated(HttpServletRequest request, HttpServletResponse response) {
        packages.save(new Package(60001, 61.0f, 41.0f, 26.0f, "testFindAndDeleteMultipleAnnotated#60001"));
        packages.save(new Package(60002, 62.0f, 42.0f, 25.0f, "testFindAndDeleteMultipleAnnotated#60002"));

        String jdbcJarName = request.getParameter("jdbcJarName").toLowerCase();
        boolean supportsOrderByForUpdate = !jdbcJarName.startsWith("derby");

        List<Package> list = supportsOrderByForUpdate //
                        ? packages.takeWithinOrdered(60.0f, 65.0f) //
                        : packages.takeWithin(60.0f, 65.0f);
        assertEquals(list.toString(), 2, list.size());

        if (!supportsOrderByForUpdate) {
            System.out.println("Sorting results in test code.");
            list.sort(Comparator.comparing(p -> p.id));
        }

        Package p0 = list.get(0);
        Package p1 = list.get(1);

        assertEquals(60001, p0.id);
        assertEquals(61.0f, p0.length, 0.01f);
        assertEquals(41.0f, p0.width, 0.01f);
        assertEquals(26.0f, p0.height, 0.01f);
        assertEquals("testFindAndDeleteMultipleAnnotated#60001", p0.description);

        assertEquals(60002, p1.id);
        assertEquals(62.0f, p1.length, 0.01f);
        assertEquals(42.0f, p1.width, 0.01f);
        assertEquals(25.0f, p1.height, 0.01f);
        assertEquals("testFindAndDeleteMultipleAnnotated#60002", p1.description);

        assertEquals(Collections.EMPTY_LIST, packages.takeWithin(60.0f, 65.0f));
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
    // Test annotation is present on corresponding method in DataTest
    public void testFindAndDeleteReturnsIds(HttpServletRequest request, HttpServletResponse response) {
        String jdbcJarName = request.getParameter("jdbcJarName").toLowerCase();
        boolean supportsOrderByForUpdate = !jdbcJarName.startsWith("derby");

        packages.deleteAll();

        packages.save(new Package(80081, 18.0f, 18.1f, 8.8f, "testFindAndDeleteReturnsIds#80081"));
        packages.save(new Package(80080, 80.0f, 80.0f, 8.0f, "testFindAndDeleteReturnsIds#80080"));
        packages.save(new Package(80088, 88.0f, 18.8f, 8.8f, "testFindAndDeleteReturnsIds#80088"));
        packages.save(new Package(80008, 80.0f, 10.8f, 0.8f, "testFindAndDeleteReturnsIds#80008"));

        Set<Integer> remaining = new TreeSet<>();
        remaining.addAll(Set.of(80008, 80080, 80081, 80088));

        Sort<Package> sort = supportsOrderByForUpdate ? Sort.desc("width") : null;
        Integer id = packages.deleteFirstBy(sort).orElseThrow();
        if (supportsOrderByForUpdate)
            assertEquals(Integer.valueOf(80080), id);
        assertEquals("Found " + id + "; expected one of " + remaining, true, remaining.remove(id));

        Sort<?>[] sorts = supportsOrderByForUpdate ? new Sort[] { Sort.desc("height"), Sort.asc("length") } : null;
        int[] ids = packages.deleteFirst2By(sorts);
        assertEquals(Arrays.toString(ids), 2, ids.length);
        if (supportsOrderByForUpdate) {
            assertEquals(80081, ids[0]);
            assertEquals(80088, ids[1]);
        }
        assertEquals("Found " + ids[0] + "; expected one of " + remaining, true, remaining.remove(ids[0]));
        assertEquals("Found " + ids[1] + "; expected one of " + remaining, true, remaining.remove(ids[1]));

        // should have only 1 remaining
        ids = packages.deleteFirst2By(sorts);
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
            long[] deleted = packages.deleteFirst3By(sort);
            fail("Deleted with return type of long[]: " + Arrays.toString(deleted) + " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }

        try {
            List<String> deleted = packages.deleteFirst4By(sort);
            fail("Deleted with return type of List<String>: " + deleted + " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }

        try {
            Collection<Number> deleted = packages.deleteFirst5By(sort);
            fail("Deleted with return type of Collection<Number>: " + deleted + " even though the id type is int.");
        } catch (MappingException x) {
            // expected
        }
    }

    /**
     * Find-and-delete repository operations that return one or more objects, corresponding to removed entities.
     */
    // Test annotation is present on corresponding method in DataTest
    public void testFindAndDeleteReturnsObjects(HttpServletRequest request, HttpServletResponse response) {
        String jdbcJarName = request.getParameter("jdbcJarName").toLowerCase();
        boolean supportsOrderByForUpdate = !jdbcJarName.startsWith("derby");

        packages.deleteAll();

        packages.save(new Package(70071, 17.0f, 17.1f, 7.7f, "testFindAndDeleteReturnsObjects#70071"));
        packages.save(new Package(70070, 70.0f, 70.0f, 7.0f, "testFindAndDeleteReturnsObjects#70070"));
        packages.save(new Package(70077, 77.0f, 17.7f, 7.7f, "testFindAndDeleteReturnsObjects#70077"));
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

        Sort<?>[] sorts = supportsOrderByForUpdate ? new Sort[] { Sort.desc("height"), Sort.asc("length") } : null;
        LinkedList<?> deletesList = packages.deleteFirst2ByHeightLessThan(8.0f, sorts);
        assertEquals("Deleted " + deletesList, 2, deletesList.size());
        Package p0 = (Package) deletesList.get(0);
        Package p1 = (Package) deletesList.get(1);
        if (supportsOrderByForUpdate) {
            assertEquals(70071, p0);
            assertEquals(17.0f, p.length, 0.001f);
            assertEquals(17.1f, p.width, 0.001f);
            assertEquals(7.7f, p.height, 0.001f);
            assertEquals("testFindAndDeleteReturnsObjects#70071", p.description);
            assertEquals(70077, p1);
            assertEquals(77.0f, p.length, 0.001f);
            assertEquals(17.7f, p.width, 0.001f);
            assertEquals(7.7f, p.height, 0.001f);
            assertEquals("testFindAndDeleteReturnsObjects#70077", p.description);
        }
        assertEquals("Found " + p0.id + "; expected one of " + remaining, true, remaining.remove(p0.id));
        assertEquals("Found " + p1.id + "; expected one of " + remaining, true, remaining.remove(p1.id));

        // should have only 1 remaining
        deleted = packages.destroy(Limit.of(4), sort);
        assertEquals("Deleted " + Arrays.toString(deleted), 1, deleted.length);
        assertEquals(remaining.iterator().next(), Integer.valueOf(((Package) deleted[0]).id));
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

        Prime[] p = primes.findFirst5ByIdLessThanEqual(25); // descending order by name
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

        assertIterableEquals(List.of("2% TestFindLike",
                                     "200 TestFindLike",
                                     "TestFindLike  1",
                                     "TestFindLike 1",
                                     "TestFindLike_1"),
                             products.findByNameLike("%TestFindLike%"));

        // _ wildcard matches any single character
        assertIterableEquals(List.of("TestFindLike 1", "TestFindLike_1"),
                             products.findByNameLike("TestFindLike_1"));

        // % wildcard matches 0 or more characters
        assertIterableEquals(List.of("2% TestFindLike", "200 TestFindLike"),
                             products.findByNameLike("2% TestFindLike"));

        // Escape characters are not possible for the repository Like keyword, however,
        // consider using JPQL escape characters and ESCAPE '\' clause for StartsWith, EndsWith, and Contains
    }

    /**
     * Search for multiple entries.
     */
    @Test
    public void testFindMultiple() throws Exception {
        assertEquals(Collections.EMPTY_LIST, persons.find("TestFindMultiple"));

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
            persons.save(List.of(jane, joe));
            persons.save(List.of(jude));
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }

        List<Person> found = persons.find("TestFindMultiple");
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

        found = persons.find("Test-FindMultiple");
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
        List<Object[]> all = primes.findIdAndNameBy(Sort.asc("numberId"));

        Object[] idAndName = all.get(5);
        assertEquals(13L, idAndName[0]);
        assertEquals("thirteen", idAndName[1]);

        idAndName = all.get(8);
        assertEquals(23L, idAndName[0]);
        assertEquals("twenty-three", idAndName[1]);
    }

    /**
     * Verify that ORDER BY can be generated, taking into account the entity variable name of a custom query.
     * The custom query in this case has no WHERE clause.
     * Other tests cover similar scenarios in which a WHERE clause is present.
     */
    @Test
    public void testGeneratedOrderAppendedToCustomQuery() {

        PageRequest<?> page2request = PageRequest.ofPage(2)
                        .size(5)
                        .sortBy(Sort.asc("numberId"));

        assertIterableEquals(List.of("thirteen", "seventeen", "nineteen", "twenty-three", "twenty-nine"),
                             primes.all(page2request));
    }

    /**
     * Inherited repository method with a generic array return type.
     */
    @Test
    public void testGenericArrayReturnType() {
        people.deleteByIdBetween(100101001l, 100101004l);

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

        assertEquals(4l, people.deleteByIdBetween(100101001l, 100101004l));
    }

    /**
     * Keyset pagination with ignoreCase in the sort criteria.
     */
    @Test
    public void testIgnoreCaseInKeysetPagination() {
        PageRequest<Prime> pagination = Order.by(_Prime.sumOfBits.asc(), _Prime.name.descIgnoreCase()).pageSize(3);
        KeysetAwareSlice<Prime> page1 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L, pagination);
        assertIterableEquals(List.of("four thousand one", "four thousand three", "Four Thousand Thirteen"),
                             page1
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        KeysetAwareSlice<Prime> page2 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L, page1.nextPageRequest());
        assertIterableEquals(List.of("four thousand seven", "four thousand nineteen"),
                             page2
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        pagination = Order.by(_Prime.name.ascIgnoreCase()).pageSize(4);
        page1 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L, pagination);
        assertIterableEquals(List.of("four thousand nineteen", "four thousand one", "four thousand seven", "Four Thousand Thirteen"),
                             page1
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        page2 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L, page1.nextPageRequest());
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
        PageRequest<?> pagination = PageRequest.ofSize(4).sortBy(Sort.asc("sumOfBits"), Sort.ascIgnoreCase("name"));
        Page<Prime> page1 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L, pagination);
        assertIterableEquals(List.of("four thousand one", "four thousand three", "four thousand nineteen", "four thousand seven"),
                             page1
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        Page<Prime> page2 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L, page1.nextPageRequest());
        assertIterableEquals(List.of("Four Thousand Thirteen"),
                             page2
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        pagination = PageRequest.ofSize(3).sortBy(Sort.descIgnoreCase("hex"));
        page1 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L, pagination);
        assertIterableEquals(List.of("FB3", "FAD", "Fa7"),
                             page1
                                             .stream()
                                             .map(p -> p.hex)
                                             .collect(Collectors.toList()));

        page2 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L, page1.nextPageRequest());
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
                             primes.findByNameIgnoreCaseNotAndIdLessThanOrderByIdAsc("Three", 10)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        // StartsWith
        assertIterableEquals(List.of("thirteen", "thirty-one", "thirty-seven"),
                             primes.findByNameIgnoreCaseStartsWithAndIdLessThanOrderByIdAsc("Thirt%n", 1000)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        // Like
        assertIterableEquals(List.of("thirteen", "thirty-seven"),
                             primes.findByNameIgnoreCaseLikeAndIdLessThanOrderByIdAsc("Thirt%n", 1000)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        // Contains
        assertIterableEquals(List.of("twenty-three", "seventeen"),
                             primes.findByNameIgnoreCaseContainsAndIdLessThanOrderByIdDesc("ent%ee", 1000)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        // Between
        assertIterableEquals(List.of("nineteen", "seventeen", "seven"),
                             primes.findByNameIgnoreCaseBetweenAndIdLessThanOrderByIdDesc("Nine", "SEVENTEEN", 50)
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        // GreaterThan, LessThanEqual
        assertIterableEquals(List.of("XLVII", "XLIII", "XIII", "XI", "VII", "V", "III"),
                             primes.findByHexIgnoreCaseGreaterThanAndRomanNumeralIgnoreCaseLessThanEqualAndIdLessThan("2a", "xlvII", 50)
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
    @Test
    public void testInsert() throws Exception {
        people.deleteByIdBetween(0L, 999999999L);

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
            persons.insertAll(List.of(ian, ike, isabelle));
            fail("Did not detect duplicate insert of id within collection.");
        } catch (EntityExistsException x) {
            // pass
        }

        persons.insertAll(List.of(ian, isabelle));

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

        assertEquals(5, people.deleteByIdBetween(0L, 999999999L));
    }

    /**
     * Insert and delete multiple entities.
     */
    // @AllowedFFDC("jakarta.data.exceptions.EntityExistsException")
    @Test
    public void testInsertAndDeleteMultiple() throws Exception {
        people.deleteByIdBetween(0L, 999999999L);

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
                     persons.findFirstNames("TestInsertAndDeleteMultiple"));

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
                     persons.findFirstNames("TestInsertAndDeleteMultiple"));

        // delete multiple entities at once

        assertEquals(null, personnel.deleteMultiple(daniel, david).join());

        assertEquals(List.of("Dianne", "Dominic", "Dorothy"),
                     persons.findFirstNames("TestInsertAndDeleteMultiple"));

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
                     persons.findFirstNames("TestInsertAndDeleteMultiple"));

        // delete remaining:

        assertEquals(Integer.valueOf(3), personnel.deleteSeveral(Stream.of(dianne, dorothy, dominic)).join());

        assertEquals(List.of(),
                     persons.findFirstNames("TestInsertAndDeleteMultiple"));
    }

    /**
     * Repository method that returns IntStream.
     */
    @Test
    public void testIntStreamResult() {
        assertIterableEquals(List.of(5, 4, 3, 3, 5, 4, 4),
                             primes.findSumOfBitsByIdBetween(20, 49)
                                             .mapToObj(i -> Integer.valueOf(i))
                                             .collect(Collectors.toList()));
    }

    /**
     * Page and KeysetAwarePage are Iterable.
     */
    @Test
    public void testIterablePages() {
        // KeysetAwarePage:
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
     * Supply a PageRequest with a keyset cursor to a repository method that returns an Iterator.
     * Specify the sort criteria statically via the OrderBy annotation.
     */
    @Test
    public void testIteratorWithKeysetPagination_OrderBy() {
        PageRequest<?> p = PageRequest.ofSize(5).afterKeyset(false, 2, 3);
        Iterator<Prime> it = primes.findByNameStartsWithAndIdLessThanOrNameContainsAndIdLessThan("t", 50L, "n", 50L, p);

        assertEquals("seventeen", it.next().name);
        assertEquals("seven", it.next().name);
        assertEquals("eleven", it.next().name);
        assertEquals("thirteen", it.next().name);
        assertEquals("nineteen", it.next().name);

        assertEquals("thirty-seven", it.next().name);
        assertEquals("forty-one", it.next().name);
        assertEquals("twenty-three", it.next().name);
        assertEquals("twenty-nine", it.next().name);
        assertEquals("thirty-one", it.next().name);

        assertEquals("forty-seven", it.next().name);
        assertEquals("two", it.next().name);
        assertEquals(false, it.hasNext());

        p = PageRequest.ofSize(4).beforeKeyset(true, 1, 2);
        it = primes.findByNameStartsWithAndIdLessThanOrNameContainsAndIdLessThan("t", 45L, "n", 45L, p);

        assertEquals("forty-one", it.next().name);
        assertEquals("twenty-three", it.next().name);
        assertEquals("twenty-nine", it.next().name);
        assertEquals("thirty-one", it.next().name);

        assertEquals("eleven", it.next().name);
        assertEquals("thirteen", it.next().name);
        assertEquals("nineteen", it.next().name);
        assertEquals("thirty-seven", it.next().name);

        assertEquals("three", it.next().name);
        assertEquals("seventeen", it.next().name);
        assertEquals("seven", it.next().name);
        assertEquals(false, it.hasNext());
    }

    /**
     * Supply a PageRequest with a keyset cursor to a repository method that returns an Iterator.
     * Specify the sort criteria dynamically via Sort.
     */
    @Test
    public void testIteratorWithKeysetPagination_Sorts() {
        PageRequest<?> p = PageRequest.ofSize(6) //
                        .sortBy(Sort.asc("sumOfBits"), Sort.asc("name")) //
                        .afterKeyset(1, "a prime number");
        Iterator<Prime> it = primes.findByNumberIdNotGreaterThan(40L, p);

        assertEquals("two", it.next().name);
        assertEquals("five", it.next().name);
        assertEquals("seventeen", it.next().name);
        assertEquals("three", it.next().name);
        assertEquals("eleven", it.next().name);
        assertEquals("nineteen", it.next().name);

        assertEquals("seven", it.next().name);
        assertEquals("thirteen", it.next().name);
        assertEquals("thirty-seven", it.next().name);
        assertEquals("twenty-nine", it.next().name);
        assertEquals("twenty-three", it.next().name);
        assertEquals("thirty-one", it.next().name);

        assertEquals(false, it.hasNext());

        p = PageRequest.ofSize(4) //
                        .sortBy(Sort.asc("sumOfBits"), Sort.asc("name")) //
                        .beforeKeyset(5, "forty-seven");
        it = primes.findByNumberIdNotGreaterThan(50L, p);

        assertEquals("thirty-seven", it.next().name);
        assertEquals("forty-three", it.next().name);
        assertEquals("twenty-nine", it.next().name);
        assertEquals("twenty-three", it.next().name);

        assertEquals("forty-one", it.next().name);
        assertEquals("nineteen", it.next().name);
        assertEquals("seven", it.next().name);
        assertEquals("thirteen", it.next().name);

        assertEquals("five", it.next().name);
        assertEquals("seventeen", it.next().name);
        assertEquals("three", it.next().name);
        assertEquals("eleven", it.next().name);

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
     * Access pages in a forward direction while entities are being added and removed,
     * using a keyset to avoid duplicates.
     */
    @Test
    public void testKeysetForwardPagination() {
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

        KeysetAwareSlice<Package> page;

        // Page 1
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, PageRequest.ofSize(3));

        assertIterableEquals(List.of(114, 116, 118),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // should not appear on next page because we already read up to length 18.0:
        packages.save(new Package(117, 17.0f, 23.0f, 12.0f, "package#117"));

        // should appear on next page because length 20.0 is beyond the keyset value of 18.0:
        packages.save(new Package(120, 20.0f, 23.0f, 12.0f, "package#120"));

        // Page 2
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, page.nextPageRequest());

        assertIterableEquals(List.of(120, 122, 124),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // remove some entries that we already read:
        packages.deleteByIdIn(List.of(116, 118, 120, 122, 124));

        // should appear on next page because length 22.0 matches the keyset value and width 70.0 is beyond the keyset value:
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
        assertEquals(null, page.nextPageRequest());

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

        page = packages.whereHeightNotWithin(20.0f, 40.0f,
                                             PageRequest.ofSize(5)
                                                             .sortBy(Sort.asc("width"), Sort.desc("height"), Sort.asc("id"))
                                                             .afterKeyset(23.0f, 12.0f, 117));

        assertIterableEquals(List.of(148, 150, 151, 133, 144),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // Switch to pages of size 4.

        // Page 1
        page = packages.findByHeightGreaterThan(10.0f, PageRequest.ofSize(4));

        assertEquals(1L, page.pageRequest().page());

        assertIterableEquals(List.of(114, 144, 133, 151),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.saveAll(List.of(new Package(128, 28.0f, 45.0f, 53.0f, "package#128"), // comes after the keyset values, should be included in next page
                                 new Package(153, 53.0f, 45.0f, 28.0f, "package#153") // comes before the keyset values, should not be on next page
        ));

        // Page 2
        page = packages.findByHeightGreaterThan(10.0f, page.nextPageRequest());

        assertEquals(2L, page.pageRequest().page());

        assertIterableEquals(List.of(150, 148, 128, 117),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // No more pages
        assertEquals(null, page.nextPageRequest());

        PageRequest<?> previous = page.previousPageRequest();
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
        assertEquals(null, page.nextPageRequest());

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
     * Access pages in a forward direction, but delete the remaining entries before accessing the next page.
     * Expect the next page to be empty, and next/previous PageRequest from the empty page to be null.
     */
    @Test
    public void testKeysetForwardPaginationNextPageEmptyAfterDeletion() {
        packages.deleteAll();

        packages.saveAll(List.of(new Package(440, 40.0f, 44.0f, 40.0f, "package#440"), // page1
                                 new Package(441, 41.0f, 41.0f, 41.0f, "package#441"))); // will be deleted

        KeysetAwareSlice<Package> page;

        // Page 1
        page = packages.findByHeightGreaterThan(4.0f, PageRequest.ofSize(1));

        assertIterableEquals(List.of(440),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(true, page.hasContent());

        PageRequest<?> next = page.nextPageRequest();
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

        // An empty page lacks keyset values from which to request next/previous pages
        assertEquals(null, page.nextPageRequest());
        assertEquals(null, page.previousPageRequest());
    }

    /**
     * Obtain keyset cursors from a page of results and use them to obtain pages in forward
     * and reverse directions.
     */
    @Test
    public void testKeysetPaginationWithCursor() {
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

        PageRequest<?> initialPagination = PageRequest.ofPage(2).size(8).afterKeyset(false, 4, 23L);
        KeysetAwarePage<Prime> page2 = primes.findByNumberIdBetweenOrderByEvenDescSumOfBitsDescIdAsc(0L, 45L, initialPagination);

        assertIterableEquals(List.of(29L, 43L, 7L, 11L, 13L, 19L, 37L, 41L),
                             page2.stream().map(p -> p.numberId).collect(Collectors.toList()));

        PageRequest.Cursor cursor7 = page2.getKeysetCursor(2);
        PageRequest<?> paginationBefore7 = PageRequest.ofSize(8).beforeKeysetCursor(cursor7);

        KeysetAwarePage<Prime> page1 = primes.findByNumberIdBetweenOrderByEvenDescSumOfBitsDescIdAsc(0L, 45L, paginationBefore7);

        assertIterableEquals(List.of(2L, 31L, 23L, 29L, 43L),
                             page1.stream().map(p -> p.numberId).collect(Collectors.toList()));

        PageRequest.Cursor cursor13 = page2.getKeysetCursor(4);
        PageRequest<?> paginationAfter13 = PageRequest.ofPage(3).afterKeysetCursor(cursor13);

        KeysetAwarePage<Prime> page3 = primes.findByNumberIdBetweenOrderByEvenDescSumOfBitsDescIdAsc(0L, 45, paginationAfter13);

        assertIterableEquals(List.of(19L, 37L, 41L, 3L, 5L, 17L),
                             page3.stream().map(p -> p.numberId).collect(Collectors.toList()));

        // test .equals method
        assertEquals(cursor13, cursor13);
        assertEquals(cursor13, page2.getKeysetCursor(4));
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
     * Access pages in reverse direction using a keyset.
     */
    @Test
    public void testKeysetReversePagination() {
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

        KeysetAwareSlice<Package> page;

        // Page 3
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(20.0f, PageRequest.ofSize(3).page(3).beforeKeyset(40.0f, 94.0f, 42.0f, 240));

        assertEquals(3L, page.pageRequest().page());

        assertIterableEquals(List.of(230, 233, 236),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // Page 2
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(20.0f, page.previousPageRequest());

        assertEquals(2L, page.pageRequest().page());

        assertIterableEquals(List.of(220, 224, 228),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // Page 1
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(20.0f, page.previousPageRequest());

        assertEquals(1L, page.pageRequest().page());

        assertIterableEquals(List.of(210, 215),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(null, page.previousPageRequest());

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
            private final List<Object> keysetValues = List.of(21.0f, 42.0f, 240);

            @Override
            public List<?> elements() {
                return keysetValues;
            }

            @Override
            public Object getKeysetElement(int index) {
                return keysetValues.get(index);
            }

            @Override
            public int size() {
                return keysetValues.size();
            }

            @Override
            public String toString() {
                return "Custom cursor of " + keysetValues;
            }
        };

        page = packages.findByHeightGreaterThan(20.0f, PageRequest.ofSize(2).page(3).beforeKeysetCursor(cursor));

        assertEquals(3L, page.pageRequest().page());

        assertIterableEquals(List.of(233, 220),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        page = packages.findByHeightGreaterThan(20.0f, page.previousPageRequest());

        assertEquals(2L, page.pageRequest().page());

        assertIterableEquals(List.of(236, 224),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        page = packages.findByHeightGreaterThan(20.0f, page.previousPageRequest());

        assertEquals(1L, page.pageRequest().page());

        assertIterableEquals(List.of(215, 210),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        page = packages.findByHeightGreaterThan(20.0f, page.previousPageRequest());

        assertEquals(1L, page.pageRequest().page()); // page numbers cannot go to 0 or negative

        assertIterableEquals(List.of(230, 228),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(null, page.previousPageRequest());

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
        // 230, 30.0f, 81.0f, 88.0f // starting point for beforeKeyset

        Package p230 = packages.findById(230).orElseThrow();

        PageRequest<?> pagination = PageRequest.ofSize(4)
                        .page(5)
                        .sortBy(Sort.asc("width"), Sort.desc("length"), Sort.asc("id"))
                        .beforeKeyset(p230.width, p230.length, p230.id);
        page = packages.whereHeightNotWithin(20.0f, 38.5f, pagination);

        assertEquals(5L, page.pageRequest().page());

        assertIterableEquals(List.of(215, 216, 210, 228),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(4, page.numberOfElements());

        page = packages.whereHeightNotWithin(20.0f, 38.5f, page.previousPageRequest());

        assertEquals(4L, page.pageRequest().page());

        assertIterableEquals(List.of(233, 224, 219, 236),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(4, page.numberOfElements());

        page = packages.whereHeightNotWithin(20.0f, 38.5f, page.previousPageRequest());

        assertEquals(3L, page.pageRequest().page());

        assertIterableEquals(List.of(240),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(1, page.numberOfElements());

        assertEquals(null, page.previousPageRequest());
        PageRequest<?> next = page.nextPageRequest();
        assertNotNull(next);
        assertEquals(4L, next.page());

        page = packages.whereHeightNotWithin(20.0f, 38.5f, next);

        assertEquals(4L, page.pageRequest().page());

        assertIterableEquals(List.of(233, 224, 219, 236),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

    }

    /**
     * Access pages in reverse direction while entities are being added and removed,
     * using a keyset to avoid duplicates.
     */
    @Test
    public void testKeysetReversePaginationWithUpdates() {
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

        KeysetAwareSlice<Package> page;

        // Page 3
        page = packages.findByHeightGreaterThan(20.0f, PageRequest.ofPage(3).size(3).beforeKeyset(10.0f, 31.0f, 310));

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

        assertEquals(null, page.previousPageRequest());

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
        page = packages.whereHeightNotWithin(32.0f, 35.5f,
                                             PageRequest.ofSize(2)
                                                             .page(5)
                                                             .sortBy(Sort.asc("height"), Sort.desc("length"), Sort.asc("id"))
                                                             .beforeKeyset(40.0f, 0.0f, 0));

        assertEquals(5L, page.pageRequest().page());

        assertIterableEquals(List.of(315, 373),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteByIdIn(List.of(373, 315, 376));

        page = packages.whereHeightNotWithin(32.0f, 35.5f, page.previousPageRequest());

        assertEquals(4L, page.pageRequest().page());

        assertIterableEquals(List.of(351, 370),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.save(new Package(331, 33.0f, 41.0f, 31.0f, "package#351"));

        page = packages.whereHeightNotWithin(32.0f, 35.5f, page.previousPageRequest());

        assertEquals(3L, page.pageRequest().page());

        assertIterableEquals(List.of(379, 331),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        page = packages.whereHeightNotWithin(32.0f, 35.5f, page.previousPageRequest());

        assertEquals(2L, page.pageRequest().page());

        assertIterableEquals(List.of(330, 310),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        PageRequest<?> previous = page.previousPageRequest();
        assertNotNull(previous);

        // delete the only previous entry and visit the empty previous page

        packages.deleteById(336);

        page = packages.whereHeightNotWithin(32.0f, 35.5f, page.previousPageRequest());

        assertEquals(1L, page.pageRequest().page());

        assertIterableEquals(Collections.EMPTY_LIST, page.content());

        // attempt next after an empty page
        assertEquals(null, page.nextPageRequest());
        assertEquals(null, page.previousPageRequest());
    }

    /**
     * A repository might define a method that returns a keyset-aware page with a Limit parameter.
     */
    @Test
    public void testKeysetWithLimit() {
        // This is not a recommended pattern. Testing to see how it is handled.
        KeysetAwarePage<Prime> page = primes.findByNumberIdBetween(15L, 45L, Limit.of(5));

        assertEquals(1L, page.pageRequest().page());
        assertEquals(5L, page.numberOfElements());
        assertEquals(5L, page.pageRequest().size());
        assertEquals(1L, page.pageRequest().page());
        assertEquals(2L, page.totalPages());
        assertEquals(8L, page.totalElements());

        assertIterableEquals(List.of(17L, 19L, 23L, 29L, 31L),
                             page.stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertIterableEquals(Collections.EMPTY_LIST,
                             primes.findByNumberIdBetween(15L, 45L, page.previousPageRequest()));

        page = primes.findByNumberIdBetween(15L, 45L, page.nextPageRequest());

        assertEquals(3L, page.numberOfElements());
        assertEquals(5L, page.pageRequest().size());
        assertEquals(2L, page.pageRequest().page());
        assertEquals(2L, page.totalPages());
        assertEquals(8L, page.totalElements());
        assertEquals(null, page.nextPageRequest());

        assertIterableEquals(List.of(37L, 41L, 43L),
                             page.stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));
    }

    /**
     * A repository might define a method that returns a keyset-aware page without specifying a PageRequest,
     * specifying the sort criteria separately.
     */
    @Test
    public void testKeysetWithoutPageRequest() {
        // This is not a recommended pattern. Testing to see how it is handled.
        KeysetAwarePage<Prime> page = primes.findByNumberIdBetweenAndBinaryDigitsNotNull(30L, 40L, Sort.asc("id"));
        assertEquals(31L, page.content().get(0).numberId);

        // Obtain PageRequest for previous entries from the KeysetAwarePage
        PageRequest<?> pagination = page.previousPageRequest().size(5);
        page = primes.findByNumberIdBetween(0L, 40L, pagination);
        assertIterableEquals(List.of(13L, 17L, 19L, 23L, 29L),
                             page.stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        pagination = page.previousPageRequest();
        page = primes.findByNumberIdBetween(0L, 40L, pagination);
        assertIterableEquals(List.of(2L, 3L, 5L, 7L, 11L),
                             page.stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertEquals(null, page.previousPageRequest());
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
            assertEquals(true, vehicles.updateByIdAddPrice("TME09876543210001", 200f));
        } finally {
            tran.rollback();
        }

        // Ensure all updates were rolled back
        h = houses.findById("111-222-333");
        v = vehicles.findById("TME09876543210001").get();

        assertEquals(219000f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2019), h.sold);
        assertEquals(24000f, v.price, 0.001f);

        // Make updates and commit
        tran.begin();
        try {
            h1.purchasePrice = 241000f;
            h1.sold = Year.of(2021);
            houses.save(h1);
            assertEquals(true, vehicles.updateByIdAddPrice("TME09876543210001", 2000f));
        } finally {
            tran.commit();
        }

        // Ensure all updates were committed
        h = houses.findById("111-222-333");
        v = vehicles.findById("TME09876543210001").get();

        assertEquals(241000f, h.purchasePrice, 0.001f);
        assertEquals(Year.of(2021), h.sold);
        assertEquals(26000f, v.price, 0.001f);

        houses.dropAll();
        vehicles.removeAll();
    }

    /**
     * Various return types for a repository query that performs multiple aggregate functions.
     */
    @Test
    public void testMultipleAggregates() {
        Object[] objects = primes.minMaxSumCountAverageObject(50);
        assertEquals(Long.valueOf(2L), objects[0]); // minimum
        assertEquals(Long.valueOf(47L), objects[1]); // maximum
        assertEquals(Long.valueOf(328L), objects[2]); // sum
        assertEquals(Long.valueOf(15L), objects[3]); // count
        assertEquals(true, objects[4] instanceof Number); // average
        assertEquals(21.0, Math.floor(((Number) objects[4]).doubleValue()), 0.01);

        Number[] numbers = primes.minMaxSumCountAverageNumber(45);
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

        float[] floats = primes.minMaxSumCountAverageFloat(35);
        assertEquals(2.0f, floats[0], 0.01f); // minimum
        assertEquals(31.0f, floats[1], 0.01f); // maximum
        assertEquals(160.0f, floats[2], 0.01f); // sum
        assertEquals(11.0f, floats[3], 0.01f); // count
        assertEquals(14.0f, Math.floor(floats[4]), 0.01f); // average

        List<Long> list = primes.minMaxSumCountAverageList(30);
        assertEquals(Long.valueOf(2L), list.get(0)); // minimum
        assertEquals(Long.valueOf(29L), list.get(1)); // maximum
        assertEquals(Long.valueOf(129L), list.get(2)); // sum
        assertEquals(Long.valueOf(10L), list.get(3)); // count
        assertEquals(Long.valueOf(12L), list.get(4)); // average

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
        assertEquals(Integer.valueOf(9), it.next()); // average

        Deque<Double> deque = primes.minMaxSumCountAverageDeque(18);
        assertEquals(2.0, deque.removeFirst(), 0.01); // minimum
        assertEquals(17.0, deque.removeFirst(), 0.01); // maximum
        assertEquals(58.0, deque.removeFirst(), 0.01); // sum
        assertEquals(7.0, deque.removeFirst(), 0.01); // count
        assertEquals(8.0, Math.floor(deque.removeFirst()), 0.01); // average
    }

    /**
     * Use a repository where methods are for different entities.
     */
    @Test
    public void testMultipleEntitiesInARepository() {
        // Remove any pre-existing data that could interfere with the test:
        products.clear();
        packages.deleteAll();
        multi.deleteByIdIn(List.of(908070605l, 807060504l, 706050403l));

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

        Person[] deleted = multi.deleteByIdIn(List.of(908070605l, 807060504l, 706050403l));
        assertEquals(2, deleted.length);

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
     * Use a repository query with both named parameters and positional parameters. Expect this to be rejected.
     */
    @Test
    public void testNamedParametersMixedWithPositionalParameters() {
        try {
            Collection<Long> found = primes.matchAnyWithMixedUsageOfPositionalAndNamed("three", 23);
            fail("Should not be able to mix positional and named parameters. Found: " + found);
        } catch (MappingException x) {
            // expected
        }
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
                                     .stream()
                                     .sorted()
                                     .collect(Collectors.toList()));
    }

    /**
     * Test the Not annotation on a parameter-based query.
     */
    @Test
    public void testNot() {
        assertEquals(List.of("thirteen"),
                     primes.withRomanNumeralSuffixAndWithoutNameSuffix("III", "three", 50));

        assertEquals(List.of("seventeen"),
                     primes.withRomanNumeralSuffixAndWithoutNameSuffix("VII", "seven", 50));
    }

    /**
     * BasicRepository.findAll(PageRequest) must raise NullPointerException.
     */
    @Test
    public void testNullPagination() {
        try {
            Page<Package> page = packages.findAll(null);
            fail("BasicRepository.findAll(PageRequest) must raise NullPointerException. Instead: " + page);
        } catch (NullPointerException x) {
            // expected
        }
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
     * Test the Or annotation on a parameter-based query.
     */
    @Test
    public void testOr() {
        assertIterableEquals(List.of(2L, 3L, 5L, 7L, 41L, 43L, 47L),
                             primes.notWithinButBelow(10, 40, 50));
    }

    /**
     * Verify that a repository method with return type of Set preserves the order of iteration,
     * (in this case descending sort on id) which is possible by using LinkedHashSet.
     */
    @Test
    public void testOrderedSet() {
        assertIterableEquals(List.of(47L, 43L, 41L, 37L, 31L, 29L, 23L),
                             primes.findIdByIdBetween(20, 49));
    }

    /**
     * Exceed the maximum offset allowed by JPA.
     */
    @Test
    public void testOverflow() {
        Limit range = Limit.range(Integer.MAX_VALUE + 5L, Integer.MAX_VALUE + 10L);
        try {
            Streamable<Prime> found = primes.findByNumberIdLessThanEqualOrderByIdDesc(9L, range);
            fail("Expected an error because starting position of range exceeds Integer.MAX_VALUE. Found: " + found);
        } catch (DataException x) {
            if (x.getCause() instanceof IllegalArgumentException)
                ; // expected
            else
                throw x;
        }

        try {
            Stream<Prime> found = primes.findFirst2147483648ByIdGreaterThan(1L);
            fail("Expected an error because limit exceeds Integer.MAX_VALUE. Found: " + found);
        } catch (DataException x) {
            boolean expected = false;
            for (Throwable cause = x; !expected && cause != null; cause = cause.getCause())
                expected = cause instanceof UnsupportedOperationException;
            if (!expected)
                throw x;
        }

        try {
            KeysetAwarePage<Prime> found = primes.findByNumberIdBetween(5L, 15L, PageRequest.ofSize(Integer.MAX_VALUE / 30).page(33));
            fail("Expected an error because when offset for pagination exceeds Integer.MAX_VALUE. Found: " + found);
        } catch (DataException x) {
            if (x.getCause() instanceof IllegalArgumentException)
                ; // expected
            else
                throw x;
        }

        try {
            Page<Prime> found = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(52L, PageRequest.ofSize(Integer.MAX_VALUE / 20).page(22));
            fail("Expected an error because when offset for pagination exceeds Integer.MAX_VALUE. Found: " + found);
        } catch (DataException x) {
            if (x.getCause() instanceof IllegalArgumentException)
                ; // expected
            else
                throw x;
        }
    }

    /**
     * Repository method where the page request type (Prime entity) differs
     * from the data type of the slice that is returned (String) due to the use
     * of query language that asks for results to be returned a String
     * (one component of the entity).
     */
    @Test
    public void testPageRequestTypeDiffersFromResultType() {
        PageRequest<Prime> page1Request = Order.by(_Prime.numberId.desc()).page(1).size(4);
        Slice<String> page1 = primes.namesBelow(35, page1Request);

        assertEquals(List.of("thirty-one", "twenty-nine", "twenty-three", "nineteen"),
                     page1.content());

        PageRequest<Prime> page2Request = page1.nextPageRequest(Prime.class);
        Slice<String> page2 = primes.namesBelow(35, page2Request);

        assertEquals(List.of("seventeen", "thirteen", "eleven", "seven"),
                     page2.content());

        PageRequest<Prime> page3Request = page2.nextPageRequest(Prime.class);
        Slice<String> page3 = primes.namesBelow(35, page3Request);

        assertEquals(List.of("five", "three", "two"),
                     page3.content());

        assertEquals(page3Request, page3.pageRequest(Prime.class));
        assertEquals(page2Request, page2.pageRequest(Prime.class));
        assertEquals(page1Request, page1.pageRequest(Prime.class));

        // Re-request the second page
        assertEquals(List.of("seventeen", "thirteen", "eleven", "seven"),
                     primes.namesBelow(35, page2.pageRequest(Prime.class))
                                     .content());
    }

    /***
     * Covers the slice.nextPageRequest(EntityClass) and slice.pageRequest(EntityClass) methods
     * when the result type matches the entity class.
     */
    @Test
    public void testPageRequestTypeMatchesResultType() {
        PageRequest<Prime> page1Request = Order.by(_Prime.id.desc()).pageSize(5);
        KeysetAwareSlice<Prime> page1 = primes.findByNumberIdBetweenAndEvenFalse(20, 50, page1Request);

        assertEquals(List.of(47L, 43L, 41L, 37L, 31L),
                     page1.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        // Specifying the entity type here is unnecessary, but should still work
        PageRequest<Prime> page2Request = page1.nextPageRequest(Prime.class);
        KeysetAwareSlice<Prime> page2 = primes.findByNumberIdBetweenAndEvenFalse(20, 50, page2Request);

        assertEquals(List.of(29L, 23L),
                     page2.stream()
                                     .map(p -> p.numberId)
                                     .collect(Collectors.toList()));

        // Specifying the entity type here is unnecessary, but should still work
        assertEquals(page2Request, page2.pageRequest(Prime.class));

        // Specifying the entity type here is unnecessary, but should still work
        assertEquals(null, page2.nextPageRequest(Prime.class));
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
     * Invoke methods that are annotated with the Select, Where, Update, and Delete annotations.
     */
    @Test
    public void testPartialQueryAnnotations() {
        Shipment s1 = new Shipment();
        s1.setDestination("200 1st Ave SW, Rochester, MN 55902");
        s1.setLocation("44.027354, -92.468482");
        s1.setId(1);
        s1.setOrderedAt(OffsetDateTime.now().minusMinutes(45));
        s1.setStatus("IN_TRANSIT");
        shipments.save(s1);

        Shipment s2 = new Shipment();
        s2.setDestination("201 4th St SE, Rochester, MN 55904");
        s2.setLocation("2800 37th St NW, Rochester, MN 55901");
        s2.setId(2);
        s2.setOrderedAt(OffsetDateTime.now().minusMinutes(20));
        s2.setStatus("READY_FOR_PICKUP");
        shipments.save(s2);

        Shipment s3 = new Shipment();
        s3.setDestination("151 4th St SE, Rochester, MN 55904");
        s3.setLocation("44.057840, -92.496301");
        s3.setId(3);
        s3.setOrderedAt(OffsetDateTime.now().minusMinutes(13));
        s3.setStatus("IN_TRANSIT");
        shipments.save(s3);

        Shipment s4 = new Shipment();
        s4.setDestination("151 4th St SE, Rochester, MN 55904");
        s4.setLocation("2800 37th St NW, Rochester, MN 55901 ");
        s4.setId(4);
        s4.setOrderedAt(OffsetDateTime.now().minusMinutes(4));
        s4.setStatus("READY_FOR_PICKUP");
        shipments.save(s4);

        Shipment s5 = new Shipment();
        s5.setDestination("201 4th St SE, Rochester, MN 55904");
        s5.setLocation("2800 37th St NW, Rochester, MN 55901");
        s5.setId(5);
        s5.setOrderedAt(OffsetDateTime.now().minusSeconds(50));
        s5.setStatus("PREPARING");
        shipments.save(s5);

        assertEquals(true, shipments.dispatch(2, "READY_FOR_PICKUP",
                                              "IN_TRANSIT", "44.036217, -92.488040", OffsetDateTime.now()));
        assertEquals("IN_TRANSIT", shipments.getStatus(2));

        // @OrderBy "destination"
        assertIterableEquals(List.of("151 4th St SE, Rochester, MN 55904",
                                     "200 1st Ave SW, Rochester, MN 55902",
                                     "201 4th St SE, Rochester, MN 55904"),
                             shipments.find("IN_TRANSIT")
                                             .map(o -> o.getDestination())
                                             .collect(Collectors.toList()));

        // @OrderBy "status", then "orderedAt" descending
        assertIterableEquals(List.of(3L, 2L, 1L, 5L, 4L),
                             Stream.of(shipments.getAll()).map(o -> o.getId()).collect(Collectors.toList()));

        Shipment s = shipments.find(3);
        String previousLocation = s.getLocation();

        assertEquals(true, shipments.updateLocation(3, previousLocation, "44.029468, -92.483191"));
        assertEquals(false, shipments.updateLocation(3, previousLocation, "44.029406, -92.489553"));

        s = shipments.find(3);
        assertEquals("44.029468, -92.483191", s.getLocation());

        assertEquals(true, shipments.cancel(4, Set.of("PREPARING", "READY_FOR_PICKUP"),
                                            "CANCELED", OffsetDateTime.now()));
        assertEquals(true, shipments.cancel(5, Set.of("PREPARING", "READY_FOR_PICKUP"),
                                            "CANCELED", OffsetDateTime.now()));
        assertEquals(false, shipments.cancel(10, Set.of("PREPARING", "READY_FOR_PICKUP"),
                                             "CANCELED", OffsetDateTime.now()));

        assertEquals(2, shipments.statusBasedRemoval("CANCELED"));

        assertEquals(3, shipments.removeEverything());
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
     * Use a repository method that has both AND and OR keywords.
     * The AND keywords should take precedence over OR and be computed first.
     */
    @Test
    public void testPrecedenceOfAndOverOr() {
        assertIterableEquals(List.of(41L, 37L, 31L, 11L, 7L),
                             primes.lessThanWithSuffixOrBetweenWithSuffix(40L, "even", 30L, 50L, "one")
                                             .map(p -> p.numberId)
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

        assertEquals(true, receipts.existsById(300L));
        assertEquals(5L, receipts.countBy());

        Receipt receipt = receipts.findById(200L).orElseThrow();
        assertEquals(202.40f, receipt.total(), 0.001f);

        assertIterableEquals(List.of("C0013-00-031:300", "C0022-00-022:200", "C0045-00-054:500"),
                             receipts.findByIdIn(List.of(200L, 300L, 500L))
                                             .map(r -> r.customer() + ":" + r.purchaseId())
                                             .sorted()
                                             .collect(Collectors.toList()));

        receipts.deleteByIdIn(List.of(200L, 500L));

        assertIterableEquals(List.of("C0013-00-031:100", "C0013-00-031:300", "C0045-00-054:400"),
                             receipts.findAll()
                                             .map(r -> r.customer() + ":" + r.purchaseId())
                                             .sorted()
                                             .collect(Collectors.toList()));

        receipts.deleteById(100L);

        assertEquals(2L, receipts.countBy());

        receipts.delete(new Receipt(400L, "C0045-00-054", 44.49f));

        assertEquals(false, receipts.existsById(400L));

        receipts.saveAll(List.of(new Receipt(600L, "C0067-00-076", 266.80f),
                                 new Receipt(700L, "C0067-00-076", 17.99f),
                                 new Receipt(800L, "C0088-00-088", 88.98f)));

        receipts.deleteAll(List.of(new Receipt(300L, "C0013-00-031", 33.99f),
                                   new Receipt(700L, "C0067-00-076", 17.99f)));

        assertEquals(2L, receipts.countBy());

        assertEquals(true, receipts.deleteByTotalLessThan(1000000.0f));

        assertEquals(0L, receipts.countBy());
    }

    /**
     * Tests all CrudRepository methods (apart from those inherited from BasicRepository) with a record as the entity.
     */
    @Test
    public void testRecordCrudRepositoryMethods() {
        receipts.deleteByTotalLessThan(1000000.0f);

        receipts.insert(new Receipt(1200L, "C0002-12-002", 102.20f));

        receipts.insertAll(Set.of(new Receipt(1300L, "C0033-13-003", 130.13f),
                                  new Receipt(1400L, "C0040-14-004", 14.40f),
                                  new Receipt(1500L, "C0005-15-005", 105.50f),
                                  new Receipt(1600L, "C0006-16-006", 600.16f)));

        try {
            receipts.insert(new Receipt(1200L, "C0002-10-002", 22.99f));
            fail("Inserted an entity with an Id that already exists.");
        } catch (EntityExistsException x) {
            // expected
        }

        // Ensure that the entity that already exists was not modified by insert
        Receipt r = receipts.findById(1200L).orElseThrow();
        assertEquals(1200L, r.purchaseId());
        assertEquals("C0002-12-002", r.customer());
        assertEquals(102.20f, r.total(), 0.001f);

        try {
            receipts.insertAll(Set.of(new Receipt(1700L, "C0017-17-007", 177.70f),
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
        receipts.update(new Receipt(1600L, "C0060-16-006", 600.16f));

        // Update multiple entities, if they exist
        try {
            receipts.updateAll(List.of(new Receipt(1400L, "C0040-14-044", 14.49f),
                                       new Receipt(1900L, "C0009-19-009", 199.99f),
                                       new Receipt(1200L, "C0002-12-002", 112.29f)));
            fail("Attempt to update multiple entities where one does not exist must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        receipts.updateAll(List.of(new Receipt(1400L, "C0040-14-044", 14.41f),
                                   new Receipt(1200L, "C0002-12-002", 112.20f)));

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

        assertEquals(0L, receipts.countBy());
    }

    /**
     * Use the provided methods of a Repository<T, K> interface that is a copy of Jakarta NoSQL's.
     */
    @Test
    public void testRepositoryBuiltInMethods() {
        ZoneOffset CDT = ZoneOffset.ofHours(-5);

        // remove data that other tests previously inserted to the same table
        reservations.deleteByHostNot("never-ever-used@example.org");

        assertEquals(0, reservations.countBy());

        Reservation r1 = new Reservation();
        r1.host = "testRepository-host1@example.org";
        r1.invitees = Set.of("testRepository-1a@example.org", "testRepository-1b@example.org");
        r1.location = "050-2 G105";
        r1.meetingID = 10020001;
        r1.start = OffsetDateTime.of(2022, 5, 23, 9, 0, 0, 0, CDT);
        r1.stop = OffsetDateTime.of(2022, 5, 23, 10, 0, 0, 0, CDT);
        r1.setLengthInMinutes(60);

        assertEquals(false, reservations.existsById(r1.meetingID));

        Reservation inserted = reservations.save(r1);
        assertEquals(r1.host, inserted.host);
        assertEquals(r1.invitees, inserted.invitees);
        assertEquals(r1.location, inserted.location);
        assertEquals(r1.meetingID, inserted.meetingID);
        assertEquals(r1.start, inserted.start);
        assertEquals(r1.stop, inserted.stop);

        assertEquals(true, reservations.existsById(r1.meetingID));

        assertEquals(1, reservations.countBy());

        Reservation r2 = new Reservation();
        r2.host = "testRepository-host2@example.org";
        r2.invitees = Set.of("testRepository-2a@example.org", "testRepository-2b@example.org");
        r2.location = "050-2 B120";
        r2.meetingID = 10020002;
        r2.start = OffsetDateTime.of(2022, 5, 23, 9, 0, 0, 0, CDT);
        r2.stop = OffsetDateTime.of(2022, 5, 23, 10, 0, 0, 0, CDT);
        r2.setLengthInMinutes(60);

        Reservation r3 = new Reservation();
        r3.host = "testRepository-host2@example.org";
        r3.invitees = Set.of("testRepository-3a@example.org");
        r3.location = "030-2 A312";
        r3.meetingID = 10020003;
        r3.start = OffsetDateTime.of(2022, 5, 24, 8, 30, 0, 0, CDT);
        r3.stop = OffsetDateTime.of(2022, 5, 24, 10, 00, 0, 0, CDT);
        r3.setLengthInMinutes(90);

        Reservation r4 = new Reservation();
        r4.host = "testRepository-host1@example.org";
        r4.invitees = Collections.emptySet();
        r4.location = "050-2 G105";
        r4.meetingID = 10020004;
        r4.start = OffsetDateTime.of(2022, 5, 24, 9, 0, 0, 0, CDT);
        r4.stop = OffsetDateTime.of(2022, 5, 24, 10, 0, 0, 0, CDT);
        r4.setLengthInMinutes(60);

        r1.invitees = Set.of("testRepository-1a@example.org", "testRepository-1b@example.org", "testRepository-1c@example.org");

        Iterable<Reservation> insertedOrUpdated = reservations.saveAll(new Iterable<>() {
            @Override
            public Iterator<Reservation> iterator() {
                return Arrays.asList(r1, r2, r3, r4).iterator();
            }
        });

        Iterator<Reservation> it = insertedOrUpdated.iterator();
        Reservation r;
        assertEquals(true, it.hasNext());
        assertNotNull(r = it.next());
        assertEquals(r1.meetingID, r.meetingID);
        assertEquals(r1.invitees, r.invitees);

        assertEquals(true, it.hasNext());
        assertNotNull(r = it.next());
        assertEquals(r2.meetingID, r.meetingID);

        assertEquals(true, it.hasNext());
        assertNotNull(r = it.next());
        assertEquals(r3.meetingID, r.meetingID);

        assertEquals(true, it.hasNext());
        assertNotNull(r = it.next());
        assertEquals(r4.meetingID, r.meetingID);

        assertEquals(false, it.hasNext());

        assertEquals(true, reservations.existsById(r3.meetingID));

        assertEquals(4, reservations.countBy());

        Map<Long, Reservation> found = reservations.findByIdIn(List.of(r4.meetingID, r2.meetingID))
                        .collect(Collectors.toMap(rr -> rr.meetingID, Function.identity()));
        assertEquals(found.toString(), 2, found.size());
        assertEquals(true, found.containsKey(r2.meetingID));
        assertEquals(true, found.containsKey(r4.meetingID));
        assertEquals(r2.location, found.get(r2.meetingID).location);
        assertEquals(r4.location, found.get(r4.meetingID).location);
        assertEquals(60, found.get(r2.meetingID).getLengthInMinutes());
        assertEquals(60, found.get(r4.meetingID).getLengthInMinutes());

        reservations.deleteById(r2.meetingID);

        Optional<Reservation> r2optional = reservations.findById(r2.meetingID);
        assertNotNull(r2optional);
        assertEquals(true, r2optional.isEmpty());

        assertEquals(3, reservations.countBy());

        reservations.deleteByIdIn(Set.of(r1.meetingID, r4.meetingID));

        assertEquals(false, reservations.existsById(r4.meetingID));

        assertEquals(1, reservations.countBy());

        Optional<Reservation> r3optional = reservations.findById(r3.meetingID);
        assertNotNull(r3optional);
        Reservation r3found = r3optional.get();
        assertEquals(r3.host, r3found.host);
        assertEquals(r3.invitees, r3found.invitees);
        assertEquals(r3.location, r3found.location);
        assertEquals(r3.meetingID, r3found.meetingID);
        assertEquals(r3.start.toInstant(), r3found.start.toInstant());
        assertEquals(r3.stop.toInstant(), r3found.stop.toInstant());
        assertEquals(90, r3found.getLengthInMinutes());
    }

    /**
     * Use custom repository interface methods modeled after Jakarta NoSQL's Repository class.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRepositoryCustomMethods() throws InterruptedException {
        ZoneOffset CDT = ZoneOffset.ofHours(-5);

        // remove data that other tests previously inserted to the same table
        reservations.removeByHostNotIn(Set.of("never-ever-used@example.org"));

        assertEquals(0, reservations.countBy());

        // set up some data for the test to use
        Reservation r1 = new Reservation();
        r1.host = "testRepositoryCustom-host1@example.org";
        r1.invitees = Set.of("testRepositoryCustom-1a@example.org", "testRepositoryCustom-1b@example.org", "testRepositoryCustom-1c@example.org");
        r1.location = "050-2 H115";
        r1.meetingID = 10030001;
        r1.start = OffsetDateTime.of(2022, 5, 25, 9, 0, 0, 0, CDT);
        r1.stop = OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT);
        r1.setLengthInMinutes(60);

        Reservation r2 = new Reservation();
        r2.host = "testRepositoryCustom-host2@example.org";
        r2.invitees = Set.of("testRepositoryCustom-2a@example.org", "testRepositoryCustom-2b@example.org");
        r2.location = "050-2 A101";
        r2.meetingID = 10030002;
        r2.start = OffsetDateTime.of(2022, 5, 25, 9, 0, 0, 0, CDT);
        r2.stop = OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT);
        r2.setLengthInMinutes(60);

        Reservation r3 = new Reservation();
        r3.host = "testRepositoryCustom-host3@example.org";
        r3.invitees = Set.of("testRepositoryCustom-3a@example.org", "testRepositoryCustom-3b@example.org");
        r3.location = "050-3 H103";
        r3.meetingID = 10030003;
        r3.start = OffsetDateTime.of(2022, 5, 25, 9, 0, 0, 0, CDT);
        r3.stop = OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT);
        r3.setLengthInMinutes(60);

        Reservation r4 = new Reservation();
        r4.host = "testRepositoryCustom-host4@example.org";
        r4.invitees = Set.of("testRepositoryCustom-4a@example.org", "testRepositoryCustom-4b@example.org");
        r4.location = "050-2 H115";
        r4.meetingID = 10030004;
        r4.start = OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT);
        r4.stop = OffsetDateTime.of(2022, 5, 25, 11, 0, 0, 0, CDT);
        r4.setLengthInMinutes(60);

        Reservation r5 = new Reservation();
        r5.host = "testRepositoryCustom-host2@example.org";
        r5.invitees = Set.of("testRepositoryCustom-5a@example.org", "testRepositoryCustom-5b@example.org");
        r5.location = "050-2 B120";
        r5.meetingID = 10030005;
        r5.start = OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT);
        r5.stop = OffsetDateTime.of(2022, 5, 25, 11, 0, 0, 0, CDT);
        r5.setLengthInMinutes(60);

        Reservation r6 = new Reservation();
        r6.host = "testRepositoryCustom-host3@example.org";
        r6.invitees = Set.of("testRepositoryCustom-3c@example.org");
        r6.location = "050-2 G105";
        r6.meetingID = 10030006;
        r6.start = OffsetDateTime.of(2022, 5, 25, 11, 0, 0, 0, CDT);
        r6.stop = OffsetDateTime.of(2022, 5, 25, 12, 0, 0, 0, CDT);
        r6.setLengthInMinutes(60);

        Reservation r7 = new Reservation();
        r7.host = "testRepositoryCustom-host2@example.org";
        r7.invitees = Set.of("testRepositoryCustom-2a@example.org", "testRepositoryCustom-2b@example.org");
        r7.location = "050-2 B120";
        r7.meetingID = 10030007;
        r7.start = OffsetDateTime.of(2022, 5, 25, 13, 0, 0, 0, CDT);
        r7.stop = OffsetDateTime.of(2022, 5, 25, 15, 0, 0, 0, CDT);
        r7.setLengthInMinutes(120);

        Reservation r8 = new Reservation();
        r8.host = "testRepositoryCustom-host4@example.org";
        r8.invitees = Set.of("testRepositoryCustom-8a@example.org");
        r8.location = "030-2 E314";
        r8.meetingID = 10030008;
        r8.start = OffsetDateTime.of(2022, 5, 25, 13, 0, 0, 0, CDT);
        r8.stop = OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT);
        r8.setLengthInMinutes(60);

        Reservation r9 = new Reservation();
        r9.host = "testRepositoryCustom-host3@example.org";
        r9.invitees = Collections.emptySet();
        r9.location = "050-2 B125";
        r9.meetingID = 10030009;
        r9.start = OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT);
        r9.stop = OffsetDateTime.of(2022, 5, 25, 15, 0, 0, 0, CDT);
        r9.setLengthInMinutes(60);

        reservations.saveAll(List.of(r1, r2, r3, r4, r5, r6, r7, r8, r9));

        List<Reservation> reservationList = new ArrayList<Reservation>();
        reservations.findByHost("testRepositoryCustom-host2@example.org").forEach(reservationList::add);
        assertIterableEquals(List.of(10030002L, 10030005L, 10030007L),
                             reservationList
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(10030005L, 10030007L, 10030009L),
                             reservations.findByLocationContainsOrderByMeetingID("-2 B1")
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));

        // TODO This fails on SQL Server, with: 10030009L, 10030004L, 10030006L, 10030008L, considering 14:00-15:00 CDT a match for 9:00-10:00 CDT. Why is it off by 5 hours?
        assertIterableEquals(List.of(10030001L, 10030002L, 10030004L, 10030006L, 10030008L),
                             reservations.findByMeetingIDOrLocationLikeAndStartAndStopOrHost(10030006,
                                                                                             "050-2 %",
                                                                                             OffsetDateTime.of(2022, 5, 25, 9, 0, 0, 0, CDT),
                                                                                             OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT),
                                                                                             "testRepositoryCustom-host4@example.org")
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(10030004L, 10030005L),
                             reservations.findByStartBetweenAndLocationIn(OffsetDateTime.of(2022, 5, 25, 9, 30, 0, 0, CDT),
                                                                          OffsetDateTime.of(2022, 5, 25, 11, 30, 0, 0, CDT),
                                                                          List.of("050-2 H115", "050-2 B120", "050-2 B125"))
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(10030009L, 10030007L, 10030008L, 10030006L),
                             reservations.findByStartGreaterThanOrderByStartDescStopDesc(OffsetDateTime.of(2022, 5, 25, 0, 0, 0, 0, CDT),
                                                                                         Limit.of(4))
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(10030007L, 10030008L, 10030006L),
                             reservations.findByStartGreaterThanOrderByStartDescStopDesc(OffsetDateTime.of(2022, 5, 25, 0, 0, 0, 0, CDT),
                                                                                         Limit.range(2, 4))
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));

        Reservation[] array = reservations.findByStartLessThanOrStartGreaterThanOrderByMeetingIDDesc(OffsetDateTime.of(2022, 5, 25, 9, 30, 0, 0, CDT),
                                                                                                     OffsetDateTime.of(2022, 5, 25, 13, 30, 0, 0, CDT));
        assertArrayEquals(new Reservation[] { r9, r3, r2, r1 }, array,
                          Comparator.<Reservation, Long> comparing(o -> o.meetingID)
                                          .thenComparing(Comparator.<Reservation, String> comparing(o -> o.host))
                                          .thenComparing(Comparator.<Reservation, String> comparing(o -> new TreeSet<String>(o.invitees).toString()))
                                          .thenComparing(Comparator.<Reservation, String> comparing(o -> o.location))
                                          .thenComparing(Comparator.<Reservation, Instant> comparing(o -> o.start.toInstant()))
                                          .thenComparing(Comparator.<Reservation, Instant> comparing(o -> o.stop.toInstant())));

        assertIterableEquals(List.of(10030001L, 10030002L, 10030003L, 10030009L),
                             reservations.findByStartNotBetween(OffsetDateTime.of(2022, 5, 25, 9, 30, 0, 0, CDT),
                                                                OffsetDateTime.of(2022, 5, 25, 13, 30, 0, 0, CDT))
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(10030007L, 10030008L, 10030009L),
                             reservations.findByStopGreaterThanEqual(OffsetDateTime.of(2022, 5, 25, 13, 0, 0, 0, CDT))
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(10030003L, 10030001L, 10030004L, 10030006L, 10030009L, 10030005L, 10030007L, 10030002L, 10030008L),
                             reservations.findByStopGreaterThanOrderByLocationDescHostAscStopAsc(OffsetDateTime.of(2022, 5, 25, 8, 0, 0, 0, CDT))
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(10030001L, 10030005L, 10030007L, 10030002L, 10030003L, 10030006L, 10030009L, 10030004L, 10030008L),
                             reservations.findByStopLessThanOrderByHostAscLocationDescStart(OffsetDateTime.of(2022, 5, 26, 0, 0, 0, 0, CDT))
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));

        // Reverse ordering to the above using Sort:
        assertIterableEquals(List.of(10030008L, 10030004L, 10030009L, 10030006L, 10030003L, 10030002L, 10030007L, 10030005L, 10030001L),
                             reservations.findByStopLessThan(OffsetDateTime.of(2022, 5, 26, 0, 0, 0, 0, CDT),
                                                             Sort.desc("host"),
                                                             Sort.asc("location"),
                                                             Sort.desc("start"))
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(10030001L, 10030002L, 10030003L, 10030007L, 10030008L),
                             reservations.findByStopOrStart(OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT),
                                                            OffsetDateTime.of(2022, 5, 25, 13, 0, 0, 0, CDT))
                                             .map(r -> r.meetingID)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("030-2 E314", "050-2 B125", "050-2 G105"),
                             reservations.findByStopOrStartOrStart(OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT),
                                                                   OffsetDateTime.of(2022, 5, 25, 11, 0, 0, 0, CDT),
                                                                   OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT))
                                             .parallel()
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(10030004L, 10030005L, 10030006L, 10030009L),
                             reservations.findByStopOrStartOrStartOrStart(OffsetDateTime.of(2022, 5, 25, 11, 0, 0, 0, CDT),
                                                                          OffsetDateTime.of(2022, 5, 25, 7, 30, 0, 0, CDT),
                                                                          OffsetDateTime.of(2022, 5, 25, 11, 0, 0, 0, CDT),
                                                                          OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT))
                                             .parallel()
                                             .sorted()
                                             .boxed()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT).toInstant(),
                                     OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT).toInstant(),
                                     OffsetDateTime.of(2022, 5, 25, 13, 0, 0, 0, CDT).toInstant(),
                                     OffsetDateTime.of(2022, 5, 25, 13, 0, 0, 0, CDT).toInstant(),
                                     OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT).toInstant()),
                             reservations.findByStopOrStopOrStop(OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT),
                                                                 OffsetDateTime.of(2022, 5, 25, 15, 0, 0, 0, CDT),
                                                                 OffsetDateTime.of(2022, 5, 25, 11, 0, 0, 0, CDT))
                                             .map(r -> r.start().toInstant())
                                             .sorted()
                                             .collect(Collectors.toList()));

        // Pagination where the final page includes less than the maximum page size,
        Page<Reservation> page1 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                                    PageRequest.ofSize(4),
                                                                    Sort.desc("meetingID"));
        assertIterableEquals(List.of(10030009L, 10030008L, 10030007L, 10030006L),
                             page1
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));
        assertEquals(true, page1.hasContent());
        assertEquals(4, page1.numberOfElements());
        assertEquals(9L, page1.totalElements());
        assertEquals(3L, page1.totalPages());

        Page<Reservation> page2 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                                    page1.nextPageRequest(),
                                                                    Sort.desc("meetingID"));
        assertIterableEquals(List.of(10030005L, 10030004L, 10030003L, 10030002L),
                             page2
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));

        Page<Reservation> page3 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                                    page2.nextPageRequest(),
                                                                    Sort.desc("meetingID"));
        assertIterableEquals(List.of(10030001L),
                             page3
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));
        assertEquals(null, page3.nextPageRequest());
        assertEquals(true, page3.hasContent());
        assertEquals(1, page3.numberOfElements());

        // Paging that comes out even:
        page2 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                  PageRequest.ofPage(2).size(3),
                                                  Sort.desc("meetingID"));
        assertIterableEquals(List.of(10030006L, 10030005L, 10030004L),
                             page2
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));
        page3 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                  page2.nextPageRequest(),
                                                  Sort.desc("meetingID"));
        assertIterableEquals(List.of(10030003L, 10030002L, 10030001L),
                             page3
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));
        assertEquals(null, page3.nextPageRequest());

        // Page of nothing
        page1 = reservations.findByHostStartsWith("Not Found", PageRequest.ofSize(100), Sort.asc("meetingID"));
        assertEquals(1L, page1.pageRequest().page());
        assertEquals(false, page1.hasContent());
        assertEquals(0, page1.numberOfElements());
        assertEquals(null, page1.nextPageRequest());
        assertEquals(0L, page1.totalElements());
        assertEquals(0L, page1.totalPages());

        // find by member of a collection
        assertIterableEquals(List.of(10030002L, 10030007L),
                             reservations.findByInviteesContainsOrderByMeetingID("testRepositoryCustom-2b@example.org")
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));

        // find by not a member of a collection
        Set<Reservation> set = reservations.findByLocationAndInviteesNotContains("050-2 B120", "testRepositoryCustom-2b@example.org");
        assertNotNull(set);
        assertEquals(set.toString(), 1, set.size());
        Reservation found = set.iterator().next();
        assertEquals(10030005L, found.meetingID);

        // EndsWith, IgnoreCase
        assertIterableEquals(List.of(10030002L, 10030005L, 10030007L),
                             reservations.findByHostIgnoreCaseEndsWith("HOST2@EXAMPLE.ORG")
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of(10030002L, 10030005L, 10030007L),
                             reservations.findByHostIgnoreCaseEndsWith("Host2@Example.org") // should match regardless of case
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .sorted()
                                             .collect(Collectors.toList()));

        // StartsWith
        assertIterableEquals(List.of(10030005L, 10030007L, 10030009L),
                             reservations.findByLocationStartsWith("050-2 B")
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertEquals(false, reservations.deleteByHostIn(List.of("testRepositoryCustom-host5@example.org")));

        assertEquals(true, reservations.deleteByHostIn(List.of("testRepositoryCustom-host1@example.org",
                                                               "testRepositoryCustom-host3@example.org")));

        assertIterableEquals(List.of(10030002L, 10030004L, 10030005L, 10030008L),
                             reservations.findByStopLessThanEqual(OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT))
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .sorted()
                                             .collect(Collectors.toList()));
    }

    /**
     * Use repository updateBy methods.
     */
    @Test
    public void testRepositoryUpdateMethods() {
        ZoneOffset CDT = ZoneOffset.ofHours(-5);

        // remove data that other tests previously inserted to the same table
        reservations.deleteByHostNot("unused@example.org");

        Reservation r1 = new Reservation();
        r1.host = "testRepositoryUpdateMethods-host1@example.org";
        r1.invitees = Set.of("testRepositoryUpdateMethods-1a@example.org");
        r1.location = "050-2 A101";
        r1.meetingID = 1012001;
        r1.start = OffsetDateTime.of(2022, 8, 25, 8, 0, 0, 0, CDT);
        r1.stop = OffsetDateTime.of(2022, 8, 25, 9, 30, 0, 0, CDT);
        r1.setLengthInMinutes(90);

        Reservation r2 = new Reservation();
        r2.host = "testRepositoryUpdateMethods-host1@example.org";
        r2.invitees = Set.of("testRepositoryUpdateMethods-2a@example.org", "testRepositoryUpdateMethods-2b@example.org");
        r2.location = "050-2 B120";
        r2.meetingID = 1012002;
        r2.start = OffsetDateTime.of(2022, 8, 25, 9, 0, 0, 0, CDT);
        r2.stop = OffsetDateTime.of(2022, 8, 25, 10, 0, 0, 0, CDT);
        r2.setLengthInMinutes(60);

        Reservation r3 = new Reservation();
        r3.host = "testRepositoryUpdateMethods-host1@example.org";
        r3.invitees = Set.of("testRepositoryUpdateMethods-3a@example.org", "testRepositoryUpdateMethods-3b@example.org", "testRepositoryUpdateMethods-3c@example.org");
        r3.location = "050-2 A101";
        r3.meetingID = 1012003;
        r3.start = OffsetDateTime.of(2022, 8, 25, 10, 0, 0, 0, CDT);
        r3.stop = OffsetDateTime.of(2022, 8, 25, 11, 0, 0, 0, CDT);
        r3.setLengthInMinutes(60);

        Reservation r4 = new Reservation();
        r4.host = "testRepositoryUpdateMethods-host4@example.org";
        r4.invitees = Set.of("testRepositoryUpdateMethods-1a@example.org");
        r4.location = "050-2 A101";
        r4.meetingID = 1012004;
        r4.start = OffsetDateTime.of(2022, 8, 25, 13, 0, 0, 0, CDT);
        r4.stop = OffsetDateTime.of(2022, 8, 25, 14, 30, 0, 0, CDT);
        r4.setLengthInMinutes(90);

        reservations.saveAll(List.of(r1, r2, r3, r4));

        // Update by primary key
        assertEquals(true, reservations.updateByMeetingIDSetHost(1012004, "testRepositoryUpdateMethods-host2@example.org"));

        // See if the updated entry is found
        List<Long> found = new ArrayList<>();
        reservations.findByHost("testRepositoryUpdateMethods-host2@example.org").forEach(r -> found.add(r.meetingID));
        assertIterableEquals(List.of(1012004L), found);

        // Update multiple by various conditions
        assertEquals(2, reservations.updateByHostAndLocationSetLocation("testRepositoryUpdateMethods-host1@example.org",
                                                                        "050-2 A101",
                                                                        "050-2 H115"));
        assertIterableEquals(List.of(1012001L, 1012003L),
                             reservations.findByLocationContainsOrderByMeetingID("H115")
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));

        reservations.deleteByHostNot("unused@example.org");

        ;
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

        // divide height and append to description via annotatively defined method with positional parameters
        assertEquals(1, packages.reduceBy(990003, 1.02f, " and slightly shortened"));

        p = packages.findById(990003).orElseThrow();
        assertEquals(11.4f, p.length, 0.01f);
        assertEquals(11.3f, p.width, 0.01f);
        assertEquals(10.0f, p.height, 0.01f);
        assertEquals("Tissue box halved and slightly shortened", p.description);

        // subtract from height and append to description via annotatively defined method
        assertEquals(true, packages.shorten(990003, 1.0f, " and shortened 1 cm"));

        p = packages.findById(990003).orElseThrow();
        assertEquals(11.4f, p.length, 0.01f);
        assertEquals(11.3f, p.width, 0.01f);
        assertEquals(9.0f, p.height, 0.01f);
        assertEquals("Tissue box halved and slightly shortened and shortened 1 cm", p.description);

        // subtract from height and append to description via annotatively defined method with named parameters
        packages.shortenBy(2, " and shortened 2 cm", 990003);

        p = packages.findById(990003).orElseThrow();
        assertEquals(11.4f, p.length, 0.01f);
        assertEquals(11.3f, p.width, 0.01f);
        assertEquals(7.0f, p.height, 0.01f);
        assertEquals("Tissue box halved and slightly shortened and shortened 1 cm and shortened 2 cm", p.description);

        packages.delete(p3);

        Page<Package> page = packages.findAll(PageRequest.of(Package.class).size(3).sortBy(Sort.desc("id")));
        assertIterableEquals(List.of(990006, 990005, 990004),
                             page.stream().map(pack -> pack.id).collect(Collectors.toList()));

        page = packages.findAll(page.nextPageRequest());
        assertIterableEquals(List.of(990002, 990001),
                             page.stream().map(pack -> pack.id).collect(Collectors.toList()));

        packages.deleteAll(List.of(p1, p6));

        assertIterableEquals(List.of(990002, 990004, 990005),
                             packages.findAll().map(pack -> pack.id).sorted().collect(Collectors.toList()));

        packages.deleteAll();

        assertEquals(0, packages.countBy());
    }

    /**
     * Experiment with reserved keywords in entity property names.
     */
    @Test
    public void testReservedKeywordsInEntityPropertyNames() {
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

        // "like" is allowed at end of entity property name because the capitalization differs.
        assertIterableEquals(List.of("Fireside", "Haralson", "Honeycrisp", "Honeygold"),
                             things.findByAlike(true)
                                             .map(o -> o.brand)
                                             .sorted()
                                             .collect(Collectors.toList()));

        // "Like" is used as a reserved keyword here.
        assertIterableEquals(List.of("A101"),
                             things.findByALike("A1%") // include second character so that databases that compare independent of case don't match "apple"
                                             .map(o -> o.a)
                                             .collect(Collectors.toList()));

        // "Or" in middle of entity property name is possible to to use of @Query.
        assertIterableEquals(List.of("Honeycrisp"),
                             things.forPurchaseOrder(20)
                                             .map(o -> o.brand)
                                             .collect(Collectors.toList()));

        // "Or" is allowed at the beginning of an entity property name because "find...By" immediately precedes it.
        assertIterableEquals(List.of("Honeygold"),
                             things.findByOrderNumber(100201L)
                                             .map(o -> o.brand)
                                             .collect(Collectors.toList()));

        // "And" is allowed at the beginning of an entity property name because "find...By" immediately precedes it.
        assertIterableEquals(List.of("android"),
                             things.findByAndroid(true)
                                             .map(o -> o.a)
                                             .collect(Collectors.toList()));

        // "and" is allowed at end of entity property name "brand" because the capitalization differs.
        // "Not" is allowed at the beginning of an entity property name "Notes" because the reserved word "Not" never appears prior to the property name.
        // "And" is allowed at the beginning of an entity property name because "And" or "Or" immediately precedes it.
        assertIterableEquals(List.of(2L, 3L, 5L, 6L),
                             things.findByBrandOrNotesContainsOrAndroid("IBM", "October", true)
                                             .map(o -> o.thingId)
                                             .sorted()
                                             .collect(Collectors.toList()));

        // "or" is allowed at end of entity property name "floor" because the capitalization differs.
        // "In" is allowed at the beginning of an entity property name "Info" because the reserved word "In" never appears prior to the property name.
        // "Or" is allowed at the beginning of an entity property name because "And" or "Or" immediately precedes it.
        assertIterableEquals(List.of("2nd floor conference room", "Golden Delicious x Haralson"),
                             things.findByFloorNotAndInfoLikeAndOrderNumberLessThan(3, "%o%", 300000L)
                                             .map(o -> o.info)
                                             .sorted()
                                             .collect(Collectors.toList()));

        // TODO is "Desc" allowed in an entity property name in the OrderBy clause?
        assertIterableEquals(List.of("A101", "android", "apple"),
                             things.findByIdGreaterThan(3L)
                                             .map(o -> o.a)
                                             .sorted()
                                             .collect(Collectors.toList()));
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

        assertIterableEquals(List.of(605, 607),
                             packages.withLengthFloored(18.0f));

        assertIterableEquals(List.of(607),
                             packages.withWidthCeiling(9.0f));

        assertIterableEquals(List.of(601, 603),
                             packages.withHeightAbout(5.0f));

        packages.deleteAll();
    }

    /**
     * Insert, update, find, and delete entities.
     */
    @Test
    public void testSaveAndUpdateMultiple() {
        // find none
        houses.dropAll();
        assertEquals(false, houses.existsById("001-203-401"));

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
        assertEquals(false, houses.existsById(h1.parcelId));

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
        assertEquals(true, vehicles.removeById(v1.vinId));

        // find none
        Optional<Vehicle> found = vehicles.findById(v1.vinId);
        assertEquals(false, found.isPresent());

        // update
        assertEquals(true, vehicles.updateByIdAddPrice("TE201234567890003", 500.0f));

        // find
        found = vehicles.findById("TE201234567890003");
        assertEquals(true, found.isPresent());
        assertEquals(25500f, found.get().price, 0.001f);

        vehicles.removeAll();
    }

    /**
     * Experiment with making a repository method return a record-like type.
     */
    @Test
    public void testSelectAsRecord() {
        ZoneOffset CDT = ZoneOffset.ofHours(-5);

        // remove data that other tests previously inserted to the same table
        reservations.removeByHostNotIn(Set.of("never-ever-used@example.org"));

        assertEquals(0, reservations.countBy());

        // set up some data for the test to use
        Reservation r1 = new Reservation();
        r1.host = "testSelectAsRecord-host1@example.org";
        r1.invitees = Set.of("testSelectAsRecord-1a@example.org", "testSelectAsRecord-1b@example.org");
        r1.location = "30-2 C206";
        r1.meetingID = 10040001;
        r1.start = OffsetDateTime.of(2022, 6, 3, 13, 30, 0, 0, CDT);
        r1.stop = OffsetDateTime.of(2022, 6, 3, 15, 0, 0, 0, CDT);
        r1.setLengthInMinutes(90);

        Reservation r2 = new Reservation();
        r2.host = "testSelectAsRecord-host2@example.org";
        r2.invitees = Set.of("testSelectAsRecord-2a@example.org", "testSelectAsRecord-2b@example.org");
        r2.location = "30-2 C206";
        r2.meetingID = 10040002;
        r2.start = OffsetDateTime.of(2022, 6, 3, 9, 0, 0, 0, CDT);
        r2.stop = OffsetDateTime.of(2022, 6, 3, 10, 0, 0, 0, CDT);
        r2.setLengthInMinutes(60);

        Reservation r3 = new Reservation();
        r3.host = "testSelectAsRecord-host3@example.org";
        r3.invitees = Set.of("testSelectAsRecord-3a@example.org");
        r3.location = "30-2 C206";
        r3.meetingID = 10040003;
        r3.start = OffsetDateTime.of(2022, 6, 3, 15, 0, 0, 0, CDT);
        r3.stop = OffsetDateTime.of(2022, 6, 3, 16, 0, 0, 0, CDT);
        r3.setLengthInMinutes(60);

        Reservation r4 = new Reservation();
        r4.host = "testSelectAsRecord-host3@example.org";
        r4.invitees = Set.of("testSelectAsRecord-3a@example.org", "testSelectAsRecord-3b@example.org");
        r4.location = "30-2 C220";
        r4.meetingID = 10040004;
        r4.start = OffsetDateTime.of(2022, 6, 3, 9, 0, 0, 0, CDT);
        r4.stop = OffsetDateTime.of(2022, 6, 3, 10, 0, 0, 0, CDT);
        r4.setLengthInMinutes(60);

        reservations.saveAll(Set.of(r1, r2, r3, r4));

        ReservedTimeSlot[] reserved = reservations.findStartAndStopByLocationAndStartBetweenOrderByStart("30-2 C206",
                                                                                                         OffsetDateTime.of(2022, 6, 3, 0, 0, 0, 0, CDT),
                                                                                                         OffsetDateTime.of(2022, 6, 3, 23, 59, 59, 0, CDT));
        assertArrayEquals(new ReservedTimeSlot[] { new ReservedTimeSlot(r2.start, r2.stop),
                                                   new ReservedTimeSlot(r1.start, r1.stop),
                                                   new ReservedTimeSlot(r3.start, r3.stop) },
                          reserved,
                          Comparator.<ReservedTimeSlot, Instant> comparing(o -> o.start().toInstant())
                                          .thenComparing(Comparator.<ReservedTimeSlot, Instant> comparing(o -> o.stop().toInstant())));
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
     * A repository might define a method that returns a Slice with a Limit parameter.
     */
    @Test
    public void testSliceWithLimit() {
        // This is not a recommended pattern. Testing to see how it is handled.
        Slice<Prime> slice = primes.findByRomanNumeralEndsWithAndIdLessThan("II", 50L, Limit.of(4), Sort.desc("id"));

        assertEquals(1L, slice.pageRequest().page());
        assertEquals(4L, slice.numberOfElements());
        assertEquals(4L, slice.pageRequest().size());
        assertEquals(1L, slice.pageRequest().page());

        assertIterableEquals(List.of("XLVII", "XLIII", "XXXVII", "XXIII"),
                             slice.stream()
                                             .map(p -> p.romanNumeral)
                                             .collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndIdLessThan("II", 50L, slice.nextPageRequest(), Sort.desc("id"));

        assertEquals(2L, slice.pageRequest().page());
        assertEquals(4L, slice.numberOfElements());
        assertEquals(4L, slice.pageRequest().size());
        assertEquals(2L, slice.pageRequest().page());

        assertIterableEquals(List.of("XVII", "XIII", "VII", "III"),
                             slice.stream()
                                             .map(p -> p.romanNumeral)
                                             .collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndIdLessThan("II", 50L, slice.nextPageRequest(), Sort.desc("id"));

        assertEquals(3L, slice.pageRequest().page());
        assertEquals(1L, slice.numberOfElements());
        assertEquals(4L, slice.pageRequest().size());
        assertEquals(3L, slice.pageRequest().page());
        assertEquals(null, slice.nextPageRequest());

        assertIterableEquals(List.of("II"),
                             slice.stream()
                                             .map(p -> p.romanNumeral)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method that returns a Slice with the sort criteria provided as Sort parameters
     */
    @Test
    public void testSliceWithSortCriteriaAsSortParameters() {
        Slice<Prime> slice = primes.findByRomanNumeralEndsWithAndIdLessThan("I", 50L,
                                                                            PageRequest.ofSize(5),
                                                                            Sort.asc("sumOfBits"), Sort.desc("id"));
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(5, slice.numberOfElements());
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(5, slice.pageRequest().size());

        assertIterableEquals(List.of(2L, 17L, 3L, 41L, 37L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndIdLessThan("I", 50L,
                                                               slice.nextPageRequest(),
                                                               Sort.asc("sumOfBits"), Sort.desc("id"));
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(5, slice.numberOfElements());
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(5, slice.pageRequest().size());

        assertIterableEquals(List.of(13L, 11L, 7L, 43L, 23L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndIdLessThan("I", 50L,
                                                               slice.nextPageRequest(),
                                                               Sort.asc("sumOfBits"), Sort.desc("id"));
        assertEquals(3L, slice.pageRequest().page());
        assertEquals(2, slice.numberOfElements());
        assertEquals(3L, slice.pageRequest().page());
        assertEquals(5, slice.pageRequest().size());

        assertIterableEquals(List.of(47L, 31L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(null, slice.nextPageRequest());
    }

    /**
     * Repository method that returns a Slice with the sort criteria provided by the OrderBy annotation.
     */
    @Test
    public void testSliceWithSortCriteriaInOrderByAnnotation() {
        Slice<Prime> slice = primes.findByRomanNumeralStartsWithAndIdLessThan("X", 50L, PageRequest.ofSize(4));
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(4, slice.numberOfElements());
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(4, slice.pageRequest().size());

        assertIterableEquals(List.of("forty-seven", "thirty-one", "forty-three", "twenty-nine"),
                             slice.stream().map(p -> p.name).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralStartsWithAndIdLessThan("X", 50L, slice.nextPageRequest());
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(4, slice.numberOfElements());
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(4, slice.pageRequest().size());

        assertIterableEquals(List.of("twenty-three", "eleven", "forty-one", "nineteen"),
                             slice.stream().map(p -> p.name).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralStartsWithAndIdLessThan("X", 50L, slice.nextPageRequest());
        assertEquals(3L, slice.pageRequest().page());
        assertEquals(3, slice.numberOfElements());
        assertEquals(3L, slice.pageRequest().page());
        assertEquals(4, slice.pageRequest().size());

        assertIterableEquals(List.of("thirteen", "thirty-seven", "seventeen"),
                             slice.stream().map(p -> p.name).collect(Collectors.toList()));

        assertEquals(null, slice.nextPageRequest());
    }

    /**
     * Repository method that returns a Slice with the sort criteria provided in the PageRequest
     */
    @Test
    public void testSliceWithSortCriteriaInPageRequest() {
        Slice<Prime> slice = primes.findByRomanNumeralEndsWithAndIdLessThan("II", 50L,
                                                                            PageRequest.ofSize(6).sortBy(Sort.desc("numberId")));
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(6, slice.numberOfElements());
        assertEquals(1L, slice.pageRequest().page());
        assertEquals(6, slice.pageRequest().size());

        assertIterableEquals(List.of(47L, 43L, 37L, 23L, 17L, 13L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndIdLessThan("II", 50L,
                                                               slice.nextPageRequest());
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(3, slice.numberOfElements());
        assertEquals(2L, slice.pageRequest().page());
        assertEquals(6, slice.pageRequest().size());

        assertIterableEquals(List.of(7L, 3L, 2L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(null, slice.nextPageRequest());
    }

    /**
     * When sort criteria is specified statically via the OrderBy annotation and
     * dynamically via Sorts from pagination, the static sort criteria is applied
     * before the dynamic sort criteria.
     */
    @Test
    public void testSortCriteriaOfOrderByAnnoTakesPrecedenceOverPaginationSorts() {

        PageRequest<?> pagination = PageRequest.ofSize(9).sortBy(Sort.desc("numberId"));
        Page<Prime> page1 = primes.findByNumberIdLessThan(49L, pagination);

        assertIterableEquals(List.of("17(2)", "5(2)", "3(2)",
                                     "41(3)", "37(3)", "19(3)", "13(3)", "11(3)", "7(3)"),
                             page1.stream()
                                             .map(p -> p.numberId + "(" + p.sumOfBits + ")")
                                             .collect(Collectors.toList()));

        Page<Prime> page2 = primes.findByNumberIdLessThan(49L, page1.nextPageRequest());

        assertIterableEquals(List.of("43(4)", "29(4)", "23(4)",
                                     "47(5)", "31(5)",
                                     "2(1)"),
                             page2.stream()
                                             .map(p -> p.numberId + "(" + p.sumOfBits + ")")
                                             .collect(Collectors.toList()));
    }

    /**
     * When sort criteria is specified statically via the Query annotation and
     * dynamically via Sorts from keyset pagination, the static sort criteria is applied
     * before the dynamic sort criteria.
     */
    @Test
    public void testSortCriteriaOfOrderByAnnoTakesPrecedenceOverPaginationSortsOnCustomQueryUsingKeysetPagination() {

        PageRequest<?> pagination = PageRequest.ofSize(7).sortBy(Sort.asc("binaryDigits"));
        KeysetAwarePage<Prime> page1 = primes.upTo(47L, pagination);

        assertEquals(7, page1.numberOfElements());
        assertEquals(15L, page1.totalElements());

        assertIterableEquals(List.of("10",
                                     "101111", "11111",
                                     "101011", "10111", "11101",
                                     "100101"),
                             page1.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        KeysetAwarePage<Prime> page2 = primes.upTo(47L, page1.nextPageRequest());

        assertIterableEquals(List.of("10011", "101001", "1011", "1101", "111",
                                     "10001", "101"),
                             page2.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        KeysetAwarePage<Prime> page3 = primes.upTo(47L, page2.nextPageRequest());

        assertIterableEquals(List.of("11"),
                             page3.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page2 = primes.upTo(47L, page3.previousPageRequest());

        assertIterableEquals(List.of("10011", "101001", "1011", "1101", "111",
                                     "10001", "101"),
                             page2.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page1 = primes.upTo(47L, page2.previousPageRequest());

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
     * dynamically via Sorts from keyset pagination, the static sort criteria is applied
     * before the dynamic sort criteria.
     */
    @Test
    public void testSortCriteriaOfOrderByKeywordTakesPrecedenceOverKeysetPaginationSorts() {

        PageRequest<?> pagination = PageRequest.ofSize(6).sortBy(Sort.desc("binaryDigits"));
        KeysetAwareSlice<Prime> page1 = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, pagination);

        assertIterableEquals(List.of("11", "101", "10001",
                                     "111", "1101", "1011"),
                             page1.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        KeysetAwareSlice<Prime> page2 = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page1.nextPageRequest());

        assertIterableEquals(List.of("101001", "10011", "100101",
                                     "11101", "10111", "101011"),
                             page2.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        KeysetAwareSlice<Prime> page3 = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page2.nextPageRequest());

        assertIterableEquals(List.of("11111", "101111",
                                     "10"),
                             page3.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        pagination = PageRequest.ofSize(6)
                        .sortBy(Sort.desc("binaryDigits"))
                        .beforeKeysetCursor(page3.getKeysetCursor(1)); // before the middle element of page 3

        KeysetAwareSlice<Prime> page = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, pagination);

        assertIterableEquals(List.of("10011", "100101",
                                     "11101", "10111", "101011",
                                     "11111"),
                             page.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page.previousPageRequest());

        assertIterableEquals(List.of("101", "10001",
                                     "111", "1101", "1011", "101001"),
                             page.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page.previousPageRequest());

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
                             primes.findByNumberIdLessThanOrderByEven(50L, Sort.asc("sumOfBits"), Sort.asc("id"))
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
     * Repository method that returns a streamable and obtains streams from it twice, and also obtains an iterable from it.
     */
    @Test
    public void testStreamable() {
        Streamable<Prime> streamable = primes.findByNumberIdLessThanEqualOrderByIdDesc(49L, Limit.of(14));
        Long total = streamable.stream().parallel().reduce(0L, (sum, p) -> sum + p.numberId, (sum1, sum2) -> sum1 + sum2);
        assertEquals(Long.valueOf(326), total);

        assertIterableEquals(List.of(47L, 43L, 41L, 37L, 31L, 29L, 23L, 19L, 17L, 13L, 11L, 7L, 5L, 3L),
                             streamable.stream().map(p -> p.numberId).collect(Collectors.toList()));

        AtomicLong sumRef = new AtomicLong();
        streamable.iterator().forEachRemaining(p -> sumRef.addAndGet(p.numberId));
        assertEquals(326L, sumRef.get());
    }

    /**
     * Repository method that supplies pagination information and returns a streamable.
     */
    @Test
    public void testStreamableWithPagination() {
        PageRequest<?> p1 = PageRequest.ofSize(9);
        Streamable<Prime> streamable1 = primes.findByNumberIdLessThanEqualOrderByIdAsc(44L, p1);

        assertIterableEquals(List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L),
                             streamable1.stream().map(p -> p.numberId).collect(Collectors.toList()));

        PageRequest<?> p2 = p1.next();
        Streamable<Prime> streamable2 = primes.findByNumberIdLessThanEqualOrderByIdAsc(44L, p2);

        assertIterableEquals(List.of(29L, 31L, 37L, 41L, 43L),
                             streamable2.stream().map(p -> p.numberId).collect(Collectors.toList()));

        AtomicLong sumRef = new AtomicLong();
        streamable2.forEach(p -> sumRef.addAndGet(p.numberId));
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
     * Obtain total counts of number of elements and pages when JPQL is supplied via the Query annotation
     * and a special count query is also provided by the Query annotation.
     */
    @Test
    public void testTotalCountsForCountQuery() {
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

        assertEquals(null, page3.nextPageRequest());
    }

    /**
     * Obtain total counts of number of elements and pages when JPQL is supplied via the Query annotation
     * where a count query is inferred from the Query annotation value, which has an ORDER BY clause.
     */
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

        assertEquals(null, page2.nextPageRequest());
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

        assertEquals(null, page3.nextPageRequest());
    }

    /**
     * Obtain total counts of number of elements and pages when keyset pagination is used.
     */
    @Test
    public void testTotalCountsWithKeysetPagination() {
        KeysetAwarePage<Prime> page3 = primes.findByNumberIdBetween(3L, 50L, PageRequest.ofPage(3).size(5).beforeKeyset(47L));
        assertEquals(14L, page3.totalElements());
        assertEquals(3L, page3.totalPages());

        assertIterableEquals(List.of(29L, 31L, 37L, 41L, 43L),
                             page3.stream().map(p -> p.numberId).collect(Collectors.toList()));

        KeysetAwarePage<Prime> page2 = primes.findByNumberIdBetween(3L, 50L, page3.previousPageRequest());
        assertEquals(3L, page2.totalPages());
        assertEquals(14L, page2.totalElements());

        assertIterableEquals(List.of(11L, 13L, 17L, 19L, 23L),
                             page2.stream().map(p -> p.numberId).collect(Collectors.toList()));

        KeysetAwarePage<Prime> page1 = primes.findByNumberIdBetween(3L, 50L, page2.previousPageRequest());
        assertEquals(3L, page1.totalPages());
        assertEquals(14L, page1.totalElements());

        assertIterableEquals(List.of(3L, 5L, 7L),
                             page1.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(null, page1.previousPageRequest());

        KeysetAwarePage<Prime> page4 = primes.findByNumberIdBetween(3L, 50L, page3.nextPageRequest());
        // In this case, the 14 elements are across 4 pages, not 3,
        // because the first and last pages ended up being partial.
        // But that doesn't become known until the first or last page is read.
        // This is one of many reasons why keyset pagination documents that
        // page counts are inaccurate and cannot be relied upon.
        assertEquals(3L, page4.totalPages());
        assertEquals(14L, page4.totalElements());

        assertIterableEquals(List.of(47L),
                             page4.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(null, page4.nextPageRequest());
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

        persons.save(List.of(p1, p2, p3));

        System.out.println("TxType.SUPPORTS in transaction");

        tran.begin();
        try {
            assertEquals(true, persons.setFirstNameInCurrentTransaction(p3.ssn_id, "Ty")); // update with MANDATORY
            assertEquals("Ty", persons.getFirstNameInCurrentOrNoTransaction(p3.ssn_id)); // read value with SUPPORTS
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Thomas", "Timothy", "Tyler"),
                             persons.findFirstNames("TestTransactional"));

        System.out.println("TxType.SUPPORTS from no transaction");

        assertEquals("Tyler", persons.getFirstNameInCurrentOrNoTransaction(p3.ssn_id));

        System.out.println("TxType.REQUIRED in transaction");

        tran.begin();
        try {
            assertEquals(true, persons.setFirstNameInCurrentOrNewTransaction(p1.ssn_id, "Tommy"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Thomas", "Timothy", "Tyler"),
                             persons.findFirstNames("TestTransactional"));

        System.out.println("TxType.REQUIRED from no transaction");

        assertEquals(true, persons.setFirstNameInCurrentOrNewTransaction(p1.ssn_id, "Tom"));

        assertIterableEquals(List.of("Timothy", "Tom", "Tyler"),
                             persons.findFirstNames("TestTransactional"));

        System.out.println("TxType.MANDATORY in transaction");

        tran.begin();
        try {
            assertEquals(true, persons.setFirstNameInCurrentTransaction(p3.ssn_id, "Ty"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Timothy", "Tom", "Tyler"),
                             persons.findFirstNames("TestTransactional"));

        System.out.println("TxType.MANDATORY from no transaction is an error");

        try {
            boolean result = persons.setFirstNameInCurrentTransaction(p3.ssn_id, "Ty");
            fail("Invoked TxType.MANDATORY operation with no transaction on thread. Result: " + result);
        } catch (TransactionalException x) {
            if (!(x.getCause() instanceof TransactionRequiredException))
                throw x;
        }

        System.out.println("TxType.REQUIRES_NEW in transaction");

        tran.begin();
        try {
            assertEquals(true, persons.setFirstNameInNewTransaction(p2.ssn_id, "Timmy"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Timmy", "Tom", "Tyler"),
                             persons.findFirstNames("TestTransactional"));

        System.out.println("TxType.REQUIRES_NEW from no transaction");

        assertEquals(true, persons.setFirstNameInCurrentOrNewTransaction(p2.ssn_id, "Tim"));

        assertIterableEquals(List.of("Tim", "Tom", "Tyler"),
                             persons.findFirstNames("TestTransactional"));

        System.out.println("TxType.NEVER in transaction");

        tran.begin();
        try {
            boolean result = persons.setFirstNameWhenNoTransactionIsPresent(p3.ssn_id, "Ty");
            fail("Invoked TxType.NEVER operation with transaction on thread. Result: " + result);
        } catch (TransactionalException x) {
            if (!(x.getCause() instanceof InvalidTransactionException))
                throw x;
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Tim", "Tom", "Tyler"),
                             persons.findFirstNames("TestTransactional"));

        System.out.println("TxType.NEVER from no transaction");

        assertEquals(true, persons.setFirstNameWhenNoTransactionIsPresent(p3.ssn_id, "Ty"));

        assertIterableEquals(List.of("Tim", "Tom", "Ty"),
                             persons.findFirstNames("TestTransactional"));

        System.out.println("TxType.NOT_SUPPORTED in transaction");

        tran.begin();
        try {
            assertEquals(true, persons.setFirstNameWithCurrentTransactionSuspended(p3.ssn_id, "Tyler"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Tim", "Tom", "Tyler"),
                             persons.findFirstNames("TestTransactional"));

        System.out.println("TxType.NOT_SUPPORTED from no transaction");

        assertEquals("Tyler", persons.getFirstNameInCurrentOrNoTransaction(p3.ssn_id));

        personnel.removeAll().get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Test the Trimmed Function by querying against data that has leading and trailing blank space.
     */
    @Test
    public void testTrimmedFunction() {
        List<Prime> found = primes.withNameLengthAndWithin(24, 4000L, 4025L);
        assertNotNull(found);
        assertEquals("Found: " + found, 1, found.size());
        assertEquals(4021L, found.get(0).numberId);
        assertEquals(" Four thousand twenty-one ", found.get(0).name);

        Prime prime = primes.withAnyCaseName("FOUR THOUSAND TWENTY-ONE").orElseThrow();
        assertEquals(4021L, prime.numberId);
        assertEquals(" Four thousand twenty-one ", prime.name);
    }

    /**
     * Test the Trimmed keyword by querying against data that has leading and trailing blank space.
     */
    @Test
    public void testTrimmedKeyword() {
        List<Prime> found = primes.findByNameTrimmedCharCountAndIdBetween(24, 4000L, 4025L);
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
    public void testUpdateAnnotation() {
        products.clear();

        Product prod1 = new Product();
        prod1.pk = UUID.nameUUIDFromBytes("UPD-ANNO-1".getBytes());
        prod1.name = "Fairly Priced TestUpdateAnnotation Item";
        prod1.price = 5.00f;
        products.save(prod1);

        Product prod2 = new Product();
        prod2.pk = UUID.nameUUIDFromBytes("UPD-ANNO-2".getBytes());
        prod2.name = "Highly Priced TestUpdateAnnotation Item";
        prod2.price = 100.00f;
        products.save(prod2);

        Product prod3 = new Product();
        prod3.pk = UUID.nameUUIDFromBytes("UPD-ANNO-3".getBytes());
        prod3.name = "Middle Priced TestUpdateAnnotation Item";
        prod3.price = 40.00f;
        products.save(prod3);

        Product prod4 = new Product();
        prod4.pk = UUID.nameUUIDFromBytes("UPD-ANNO-4".getBytes());
        prod4.name = "Inexpensive TestUpdateAnnotation Item";
        prod4.price = 2.00f;
        products.save(prod4);

        Product prod5 = new Product();
        prod5.pk = UUID.nameUUIDFromBytes("UPD-ANNO-5".getBytes());
        prod5.name = "Ridiculously High Priced TestUpdateAnnotation Item";
        prod5.price = 500.00f;
        products.save(prod5);

        Product prod6 = new Product();
        prod6.pk = UUID.nameUUIDFromBytes("UPD-ANNO-6".getBytes());
        prod6.name = "Lowest Priced TestUpdateAnnotation Item";
        prod6.price = 1.00f;
        products.save(prod6);

        assertEquals(true, products.isNotEmpty());
        assertEquals(6, products.total());

        assertEquals(5, products.inflatePrices("Priced TestUpdateAnnotation Item", 1.07f)); // prod4 does not match

        Product[] found = products.findByVersionGreaterThanEqualOrderByPrice(2);

        assertEquals(Stream.of(found).map(p -> p.pk).collect(Collectors.toList()).toString(),
                     5, found.length);

        assertEquals(1.07f, found[0].price, 0.001f);
        assertEquals(5.35f, found[1].price, 0.001f);
        assertEquals(42.80f, found[2].price, 0.001f);
        assertEquals(107.00f, found[3].price, 0.001f);
        assertEquals(535.00f, found[4].price, 0.001f);

        Product item = products.findItem(prod4.pk);
        assertEquals(2.00f, item.price, 0.001f);

        products.undoPriceIncrease(Set.of(prod5.pk, prod2.pk, prod1.pk), 1.07f);

        found = products.findByVersionGreaterThanEqualOrderByPrice(1);

        assertEquals(Stream.of(found).map(p -> p.pk).collect(Collectors.toList()).toString(),
                     6, found.length);

        assertEquals(1.07f, found[0].price, 0.001f); // update remains in place
        assertEquals(2.00f, found[1].price, 0.001f); // never updated
        assertEquals(5.00f, found[2].price, 0.001f); // reverted
        assertEquals(42.80f, found[3].price, 0.001f); // update remains in place
        assertEquals(100.00f, found[4].price, 0.001f); // reverted
        assertEquals(500.00f, found[5].price, 0.001f); // reverted

        assertEquals(2, found[0].version); // update remains in place
        assertEquals(1, found[1].version); // never updated
        assertEquals(1, found[2].version); // reverted
        assertEquals(2, found[3].version); // update remains in place
        assertEquals(1, found[4].version); // reverted
        assertEquals(1, found[5].version); // reverted

        assertEquals(6, products.inflateAllPrices(1.05f));

        found = products.findByVersionGreaterThanEqualOrderByPrice(2);

        assertEquals(1.12f, found[0].price, 0.01f);
        assertEquals(2.10f, found[1].price, 0.01f);
        assertEquals(5.25f, found[2].price, 0.01f);
        assertEquals(44.94f, found[3].price, 0.01f);
        assertEquals(105.00f, found[4].price, 0.01f);
        assertEquals(525.00f, found[5].price, 0.01f);

        products.clear();

        assertEquals(0, products.total());
        assertEquals(false, products.isNotEmpty());
    }

    /**
     * Update multiple entries.
     */
    @Test
    public void testUpdateMultiple() {
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
    }

    /**
     * Use update methods with an entity parameter to make updates.
     */
    @AllowedFFDC("jakarta.data.exceptions.OptimisticLockingFailureException")
    @Test
    public void testUpdateWithEntityParameter() {
        people.deleteByIdBetween(0L, 999999999L);

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

        assertEquals(0, persons.updateSome());

        assertEquals(4, people.deleteByIdBetween(0L, 999999999L));
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
}
