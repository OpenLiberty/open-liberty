/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
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
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetAwareSlice;
import jakarta.data.repository.Limit;
import jakarta.data.repository.Page;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Slice;
import jakarta.data.repository.Sort;
import jakarta.data.repository.Streamable;
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

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataTestServlet extends FATServlet {
    private final long TIMEOUT_MINUTES = 2;

    @Inject
    Houses houses;

    @Inject
    Packages packages;

    @Inject
    PersonRepo people;

    @Inject
    Personnel personnel;

    // Only add to this repository within the Servlet.init method so that all tests can rely on its data:
    @Inject
    Primes primes;

    @Inject
    ProductRepo products;

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
        primes.save(new Prime(2, "2", "10", 1, "II", "two"),
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
                    new Prime(4019, "FB3", "111110110011", 9, "", "four thousand nineteen")); // empty list of romanNumeralSymbols
    }

    /**
     * Use repository methods with aggregate functions in the select clause.
     */
    @Test
    public void testAggregateFunctions() {
        // Remove data from previous test:
        Product[] allProducts = products.findByVersionGreaterThanEqualOrderByPrice(-1);
        if (allProducts.length > 0)
            products.discontinueProducts(Arrays.stream(allProducts).map(p -> p.id).collect(Collectors.toSet()));

        // Add data for this test to use:
        Product prod1 = new Product();
        prod1.id = "AF-006E905-LE";
        prod1.name = "TestAggregateFunctions Lite Edition";
        prod1.price = 104.99f;
        products.save(prod1);

        Product prod2 = new Product();
        prod2.id = "AF-006E005-RK";
        prod2.name = "TestAggregateFunctions Repair Kit";
        prod2.price = 104.99f;
        products.save(prod2);

        Product prod3 = new Product();
        prod3.id = "AF-006E905-CE";
        prod3.name = "TestAggregateFunctions Classic Edition";
        prod3.price = 306.99f;
        products.save(prod3);

        Product prod4 = new Product();
        prod4.id = "AF-006E205-CE";
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
     * Asynchronous repository method that returns a CompletionStage of KeysetAwarePage.
     */
    @Test
    public void testCompletionStageOfPage() throws ExecutionException, InterruptedException, TimeoutException {
        LinkedBlockingQueue<Long> sums = new LinkedBlockingQueue<Long>();

        primes.findByNumberIdLessThanOrderByIdDesc(42L, Pageable.ofSize(6)).thenCompose(page1 -> {
            sums.add(page1.stream().mapToLong(p -> p.numberId).sum());
            return primes.findByNumberIdLessThanOrderByIdDesc(42L, page1.nextPageable());
        }).thenCompose(page2 -> {
            sums.add(page2.stream().mapToLong(p -> p.numberId).sum());
            return primes.findByNumberIdLessThanOrderByIdDesc(42L, page2.nextPageable());
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

        assertEquals(Integer.valueOf(0), primes.countNumberIdBetween(32, 36));

        assertEquals(Integer.valueOf(3), primes.countNumberIdBetween(40, 50));

        assertEquals(Short.valueOf((short) 14), primes.countByIdBetweenAndEvenNot(0, 50, true).get(TIMEOUT_MINUTES, TimeUnit.MINUTES));
    }

    /**
     * Count the number of matching entries in the database using annotatively defined queries.
     */
    @Test
    public void testCountAnnotation() throws ExecutionException, InterruptedException, TimeoutException {

        assertEquals(6, primes.howManyIn(17L, 37L));
        assertEquals(0, primes.howManyIn(24L, 28L));

        assertEquals(Long.valueOf(5), primes.howManyLessThan20StartingAfter(5));
        assertEquals(Long.valueOf(0), primes.howManyLessThan20StartingAfter(19));
        assertEquals(Long.valueOf(0), primes.howManyLessThan20StartingAfter(21));
    }

    /**
     * Delete multiple entries and use a default method to atomically remove and return a removed entity.
     */
    @Test
    public void testDefaultRepositoryMethod() {
        products.clear();

        Product prod1 = new Product();
        prod1.id = "TDM-SE";
        prod1.name = "TestDefaultRepositoryMethod Standard Edition";
        prod1.price = 115.99f;
        products.save(prod1);

        Product prod2 = new Product();
        prod2.id = "TDM-AE";
        prod2.name = "TestDefaultRepositoryMethod Advanced Edition";
        prod2.price = 197.99f;
        products.save(prod2);

        Product prod3 = new Product();
        prod3.id = "TDM-EE";
        prod3.name = "TestDefaultRepositoryMethod Expanded Edition";
        prod3.price = 153.99f;
        products.save(prod3);

        Product prod4 = new Product();
        prod4.id = "TDM-NFE";
        prod4.name = "TestDefaultRepositoryMethod Nearly Free Edition";
        prod4.price = 1.99f;
        products.save(prod4);

        assertEquals(2, products.discontinueProducts(Set.of("TDM-AE", "TDM-NFE", "TDM-NOT-FOUND")));

        // expect that 2 remain
        assertNotNull(products.findItem("TDM-SE"));
        assertNotNull(products.findItem("TDM-EE"));

        // In the future it will only be possible to run this on Java 21+
        // and then the following condition can be removed so that this part of the test always runs:
        if (Runtime.version().feature() >= 17) {
            // Use custom method:
            Product removed = products.remove("TDM-SE");
            assertEquals("TestDefaultRepositoryMethod Standard Edition", removed.name);

            assertEquals(false, products.findById("TDM-SE").isPresent());
            assertEquals(true, products.findById("TDM-EE").isPresent());
        }

        products.clear();
    }

    /**
     * Query for distinct values of an attribute.
     */
    @Test
    public void testDistinctAttribute() {
        Product prod1 = new Product();
        prod1.id = "TDA-T-L1";
        prod1.name = "TestDistinctAttribute T-Shirt Size Large";
        prod1.price = 7.99f;
        products.save(prod1);

        Product prod2 = new Product();
        prod2.id = "TDA-T-M1";
        prod1.name = "TestDistinctAttribute T-Shirt Size Medium";
        prod2.price = 7.89f;
        products.save(prod2);

        Product prod3 = new Product();
        prod3.id = "TDA-T-S1";
        prod3.name = "TestDistinctAttribute T-Shirt Size Small";
        prod3.price = 7.79f;
        products.save(prod3);

        Product prod4 = new Product();
        prod4.id = "TDA-T-M2";
        prod4.name = "TestDistinctAttribute T-Shirt Size Medium";
        prod4.price = 7.49f;
        products.save(prod4);

        Product prod5 = new Product();
        prod5.id = "TDA-T-XS1";
        prod5.name = "TestDistinctAttribute T-Shirt Size Extra Small";
        prod5.price = 7.59f;
        products.save(prod5);

        Product prod6 = new Product();
        prod6.id = "TDA-T-L2";
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
     * Unannotated entity with an attribute that is an embeddable type.
     */
    @Test
    public void testEmbeddable() {
        houses.deleteById("TestEmbeddable-304-2288-60");

        House h = new House();
        h.area = 1800;
        h.garage = new Garage();
        h.garage.area = 200;
        h.garage.door = new GarageDoor();
        h.garage.door.setHeight(8);
        h.garage.door.setWidth(10);
        h.garage.type = Garage.Type.Attached;
        h.kitchen = new Kitchen();
        h.kitchen.length = 15;
        h.kitchen.width = 12;
        h.lotSize = 0.19f;
        h.numBedrooms = 4;
        h.parcelId = "TestEmbeddable-304-2288-60";
        h.purchasePrice = 162000;
        h.sold = Year.of(2018);

        houses.save(h);

        h = houses.findById("TestEmbeddable-304-2288-60");

        assertNotNull(h.kitchen);
        assertEquals(15, h.kitchen.length);
        assertEquals(12, h.kitchen.width);

        assertNotNull(h.garage);
        assertEquals(200, h.garage.area);
        assertEquals(Garage.Type.Attached, h.garage.type);

        assertNotNull(h.garage.door);
        assertEquals(8, h.garage.door.getHeight());
        assertEquals(10, h.garage.door.getWidth());

        assertEquals(1L, houses.deleteById("TestEmbeddable-304-2288-60"));
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

        assertEquals(Boolean.TRUE, primes.existsIdBetween(Long.valueOf(14), Long.valueOf(18)));
        assertEquals(Boolean.FALSE, primes.existsIdBetween(Long.valueOf(24), Long.valueOf(28)));
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
     * Define a query with Filter annotations.
     */
    @Test
    public void testFilterAnnotation() {
        assertIterableEquals(List.of(37L, 17L, 7L, 5L), // 11 has no V in the roman numeral and 47 is too big
                             primes.inRangeHavingVNumeralAndSubstringOfName(5L, 45L, "ve"));

        assertIterableEquals(List.of(),
                             primes.inRangeHavingVNumeralAndSubstringOfName(1L, 18L, "nine"));
    }

    /**
     * Search for missing item. Insert it. Search again.
     */
    @Test
    public void testFindCreateFind() {
        try {
            Product prod = products.findItem("OL306-233F");
            fail("Should not find " + prod);
        } catch (EmptyResultException x) {
            // expected
        }

        Product prod = new Product();
        prod.id = "OL306-233F";
        prod.name = "Something";
        prod.price = 3.99f;
        prod.description = "An item for sale.";

        products.save(prod);

        Product p = products.findItem("OL306-233F");
        assertEquals(prod.id, p.id);
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
     * Use the % and _ characters, which are wildcards in JPQL, within query parameters.
     */
    @Test
    public void testFindLike() throws Exception {
        // Remove data from previous tests:
        Product[] allProducts = products.findByVersionGreaterThanEqualOrderByPrice(-1);
        if (allProducts.length > 0)
            products.discontinueProducts(Arrays.stream(allProducts).map(p -> p.id).collect(Collectors.toSet()));

        Product p1 = new Product();
        p1.id = "TFL-1";
        p1.name = "TestFindLike_1";
        p1.price = 1.00f;
        products.save(p1);

        Product p2 = new Product();
        p2.id = "TFL-2";
        p2.name = "2% TestFindLike";
        p2.price = 2.00f;
        products.save(p2);

        Product p10 = new Product();
        p10.id = "TFL-10";
        p10.name = "TestFindLike 1";
        p10.price = 10.00f;
        products.save(p10);

        Product p100 = new Product();
        p100.id = "TFL-100";
        p100.name = "TestFindLike  1";
        p100.price = 100.00f;
        products.save(p100);

        Product p200 = new Product();
        p200.id = "TFL-200";
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
        assertEquals(Collections.EMPTY_LIST, people.find("TestFindMultiple"));

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
            people.save(List.of(jane, joe));
            people.save(List.of(jude));
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }

        List<Person> found = people.find("TestFindMultiple");
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

        found = people.find("Test-FindMultiple");
        assertNotNull(found);
        assertEquals(1, found.size());
        assertEquals(jude.ssn_id, found.get(0).ssn_id);
    }

    /**
     * Verify that ORDER BY can be generated, taking into account the entity variable name of a custom query.
     * The custom query in this case has no WHERE clause.
     * Other tests cover similar scenarios in which a WHERE clause is present.
     */
    @Test
    public void testGeneratedOrderAppendedToCustomQuery() {

        Pageable page2request = Pageable.ofPage(2)
                        .size(5)
                        .sortBy(Sort.asc("numberId"));

        assertIterableEquals(List.of("thirteen", "seventeen", "nineteen", "twenty-three", "twenty-nine"),
                             primes.all(page2request));
    }

    /**
     * Keyset pagination with ignoreCase in the sort criteria.
     */
    @Test
    public void testIgnoreCaseInKeysetPagination() {
        Pageable pagination = Pageable.ofSize(3).sortBy(Sort.asc("sumOfBits"), Sort.descIgnoreCase("name"));
        KeysetAwareSlice<Prime> page1 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L, pagination);
        assertIterableEquals(List.of("four thousand one", "four thousand three", "Four Thousand Thirteen"),
                             page1
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        KeysetAwareSlice<Prime> page2 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L, page1.nextPageable());
        assertIterableEquals(List.of("four thousand seven", "four thousand nineteen"),
                             page2
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        pagination = Pageable.ofSize(4).sortBy(Sort.ascIgnoreCase("name"));
        page1 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L, pagination);
        assertIterableEquals(List.of("four thousand nineteen", "four thousand one", "four thousand seven", "Four Thousand Thirteen"),
                             page1
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        page2 = primes.findByNumberIdBetweenAndEvenFalse(4000L, 4020L, page1.nextPageable());
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
        Pageable pagination = Pageable.ofSize(4).sortBy(Sort.asc("sumOfBits"), Sort.ascIgnoreCase("name"));
        Page<Prime> page1 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L, pagination);
        assertIterableEquals(List.of("four thousand one", "four thousand three", "four thousand nineteen", "four thousand seven"),
                             page1
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        Page<Prime> page2 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L, page1.nextPageable());
        assertIterableEquals(List.of("Four Thousand Thirteen"),
                             page2
                                             .stream()
                                             .map(p -> p.name)
                                             .collect(Collectors.toList()));

        pagination = Pageable.ofSize(3).sortBy(Sort.descIgnoreCase("hex"));
        page1 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L, pagination);
        assertIterableEquals(List.of("FB3", "FAD", "Fa7"),
                             page1
                                             .stream()
                                             .map(p -> p.hex)
                                             .collect(Collectors.toList()));

        page2 = primes.findByNumberIdBetweenAndSumOfBitsNotNull(4000L, 4020L, page1.nextPageable());
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
     * Page and KeysetAwarePage are Iterable.
     */
    @Test
    public void testIterablePages() {
        // KeysetAwarePage:
        Page<Prime> page = primes.findByNumberIdBetween(20L, 40L, Pageable.ofSize(3));
        List<Long> results = new ArrayList<>();
        for (Prime p : page)
            results.add(p.numberId);
        assertIterableEquals(List.of(23L, 29L, 31L), results);
        assertEquals(3, page.content().size());

        page = primes.findByNumberIdBetween(0L, 1L, Pageable.ofSize(5));
        Iterator<Prime> it = page.iterator();
        assertEquals(false, it.hasNext());
        try {
            Prime p = it.next();
            fail("Obtained next from iterator: " + p);
        } catch (NoSuchElementException x) {
            // expected
        }
        assertEquals(Collections.EMPTY_LIST, page.content());

        page = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(23L, Pageable.ofSize(4));
        results = new ArrayList<>();
        for (Prime p : page)
            results.add(p.numberId);
        assertIterableEquals(List.of(23L, 19L, 17L, 13L), results);

        page = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(1L, Pageable.ofSize(6));
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
     * Supply a Pageable with a keyset cursor to a repository method that returns an Iterator.
     * Specify the sort criteria statically via the OrderBy annotation.
     */
    @Test
    public void testIteratorWithKeysetPagination_OrderBy() {
        Pageable p = Pageable.ofSize(5).afterKeyset(false, 2, 3);
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

        p = Pageable.ofSize(4).beforeKeyset(true, 1, 2);
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
     * Supply a Pageable with a keyset cursor to a repository method that returns an Iterator.
     * Specify the sort criteria dynamically via Sort.
     */
    @Test
    public void testIteratorWithKeysetPagination_Sorts() {
        Pageable p = Pageable.ofSize(6) //
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

        p = Pageable.ofSize(4) //
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
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, Pageable.ofSize(3));

        assertIterableEquals(List.of(114, 116, 118),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // should not appear on next page because we already read up to length 18.0:
        packages.save(new Package(117, 17.0f, 23.0f, 12.0f, "package#117"));

        // should appear on next page because length 20.0 is beyond the keyset value of 18.0:
        packages.save(new Package(120, 20.0f, 23.0f, 12.0f, "package#120"));

        // Page 2
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, page.nextPageable());

        assertIterableEquals(List.of(120, 122, 124),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // remove some entries that we already read:
        packages.deleteAllById(List.of(116, 118, 120, 122, 124));

        // should appear on next page because length 22.0 matches the keyset value and width 70.0 is beyond the keyset value:
        packages.save(new Package(130, 22.0f, 70.0f, 67.0f, "package#130"));

        // Page 3
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, page.nextPageable());

        assertIterableEquals(List.of(130, 132, 133),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteById(130);

        // Page 4
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, page.nextPageable());

        assertIterableEquals(List.of(140, 144, 148),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteAllById(List.of(132, 140));

        // Page 5
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(10.0f, page.nextPageable());

        assertIterableEquals(List.of(150, 151),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // No more pages
        assertEquals(null, page.nextPageable());

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
                                             Pageable.ofSize(5)
                                                             .sortBy(Sort.asc("width"), Sort.desc("height"), Sort.asc("id"))
                                                             .afterKeyset(23.0f, 12.0f, 117));

        assertIterableEquals(List.of(148, 150, 151, 133, 144),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // Switch to pages of size 4.

        // Page 1
        page = packages.findByHeightGreaterThan(10.0f, Pageable.ofSize(4));

        assertEquals(1L, page.number());

        assertIterableEquals(List.of(114, 144, 133, 151),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.saveAll(List.of(new Package(128, 28.0f, 45.0f, 53.0f, "package#128"), // comes after the keyset values, should be included in next page
                                 new Package(153, 53.0f, 45.0f, 28.0f, "package#153") // comes before the keyset values, should not be on next page
        ));

        // Page 2
        page = packages.findByHeightGreaterThan(10.0f, page.nextPageable());

        assertEquals(2L, page.number());

        assertIterableEquals(List.of(150, 148, 128, 117),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // No more pages
        assertEquals(null, page.nextPageable());

        Pageable previous = page.previousPageable();
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
        page = packages.whereVolumeWithin(5000.0f, 123456.0f, Pageable.ofSize(6));

        assertIterableEquals(List.of(114, 133, 144, 128, 148, 150),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteAllById(List.of(144, 148, 150));

        packages.save(new Package(152, 48.0f, 45.0f, 52.0f, "package#152"));

        // Page 2
        page = packages.whereVolumeWithin(5000.0f, 123456.0f, page.nextPageable());

        assertIterableEquals(List.of(151, 152, 153),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // No more pages
        assertEquals(null, page.nextPageable());

        // Previous page
        previous = page.previousPageable();
        assertNotNull(previous);
        assertEquals(1L, previous.page());

        page = packages.whereVolumeWithin(5000.0f, 123456.0f, previous);

        assertEquals(1L, page.number());

        assertIterableEquals(List.of(114, 133, 128),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));
    }

    /**
     * Access pages in a forward direction, but delete the remaining entries before accessing the next page.
     * Expect the next page to be empty, and next/previous Pageable from the empty page to be null.
     */
    @Test
    public void testKeysetForwardPaginationNextPageEmptyAfterDeletion() {
        packages.deleteAll();

        packages.saveAll(List.of(new Package(440, 40.0f, 44.0f, 40.0f, "package#440"), // page1
                                 new Package(441, 41.0f, 41.0f, 41.0f, "package#441"))); // will be deleted

        KeysetAwareSlice<Package> page;

        // Page 1
        page = packages.findByHeightGreaterThan(4.0f, Pageable.ofSize(1));

        assertIterableEquals(List.of(440),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(true, page.hasContent());

        Pageable next = page.nextPageable();
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
        assertEquals(null, page.nextPageable());
        assertEquals(null, page.previousPageable());
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

        Pageable initialPagination = Pageable.ofPage(2).size(8).afterKeyset(false, 4, 23L);
        KeysetAwarePage<Prime> page2 = primes.findByNumberIdBetweenOrderByEvenDescSumOfBitsDescIdAsc(0L, 45L, initialPagination);

        assertIterableEquals(List.of(29L, 43L, 7L, 11L, 13L, 19L, 37L, 41L),
                             page2.stream().map(p -> p.numberId).collect(Collectors.toList()));

        Pageable.Cursor cursor7 = page2.getKeysetCursor(2);
        Pageable paginationBefore7 = Pageable.ofSize(8).beforeKeysetCursor(cursor7);

        KeysetAwarePage<Prime> page1 = primes.findByNumberIdBetweenOrderByEvenDescSumOfBitsDescIdAsc(0L, 45L, paginationBefore7);

        assertIterableEquals(List.of(2L, 31L, 23L, 29L, 43L),
                             page1.stream().map(p -> p.numberId).collect(Collectors.toList()));

        Pageable.Cursor cursor13 = page2.getKeysetCursor(4);
        Pageable paginationAfter13 = Pageable.ofPage(3).afterKeysetCursor(cursor13);

        KeysetAwarePage<Prime> page3 = primes.findByNumberIdBetweenOrderByEvenDescSumOfBitsDescIdAsc(0L, 45, paginationAfter13);

        assertIterableEquals(List.of(19L, 37L, 41L, 3L, 5L, 17L),
                             page3.stream().map(p -> p.numberId).collect(Collectors.toList()));

        // test .equals method
        assertEquals(cursor13, cursor13);
        assertEquals(cursor13, page2.getKeysetCursor(4));
        assertEquals(false, cursor13.equals(cursor7));

        // test .hashCode method
        Map<Pageable.Cursor, Integer> map = new HashMap<>();
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
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(20.0f, Pageable.ofSize(3).page(3).beforeKeyset(40.0f, 94.0f, 42.0f, 240));

        assertEquals(3L, page.number());

        assertIterableEquals(List.of(230, 233, 236),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // Page 2
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(20.0f, page.previousPageable());

        assertEquals(2L, page.number());

        assertIterableEquals(List.of(220, 224, 228),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        // Page 1
        page = packages.findByHeightGreaterThanOrderByLengthAscWidthDescHeightDescIdAsc(20.0f, page.previousPageable());

        assertEquals(1L, page.number());

        assertIterableEquals(List.of(210, 215),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(null, page.previousPageable());

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

        Pageable.Cursor cursor = new Pageable.Cursor() {
            private final List<Object> keysetValues = List.of(21.0f, 42.0f, 240);

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

        page = packages.findByHeightGreaterThan(20.0f, Pageable.ofSize(2).page(3).beforeKeysetCursor(cursor));

        assertEquals(3L, page.number());

        assertIterableEquals(List.of(233, 220),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        page = packages.findByHeightGreaterThan(20.0f, page.previousPageable());

        assertEquals(2L, page.number());

        assertIterableEquals(List.of(236, 224),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        page = packages.findByHeightGreaterThan(20.0f, page.previousPageable());

        assertEquals(1L, page.number());

        assertIterableEquals(List.of(215, 210),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        page = packages.findByHeightGreaterThan(20.0f, page.previousPageable());

        assertEquals(1L, page.number()); // page numbers cannot go to 0 or negative

        assertIterableEquals(List.of(230, 228),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(null, page.previousPageable());

        // Switch directions and expect a page 2 next

        page = packages.findByHeightGreaterThan(20.0f, page.nextPageable());

        assertEquals(2L, page.number());

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

        Pageable pagination = Pageable.ofSize(4)
                        .page(5)
                        .sortBy(Sort.asc("width"), Sort.desc("length"), Sort.asc("id"))
                        .beforeKeyset(p230.width, p230.length, p230.id);
        page = packages.whereHeightNotWithin(20.0f, 38.5f, pagination);

        assertEquals(5L, page.number());

        assertIterableEquals(List.of(215, 216, 210, 228),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(4, page.numberOfElements());

        page = packages.whereHeightNotWithin(20.0f, 38.5f, page.previousPageable());

        assertEquals(4L, page.number());

        assertIterableEquals(List.of(233, 224, 219, 236),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(4, page.numberOfElements());

        page = packages.whereHeightNotWithin(20.0f, 38.5f, page.previousPageable());

        assertEquals(3L, page.number());

        assertIterableEquals(List.of(240),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(1, page.numberOfElements());

        assertEquals(null, page.previousPageable());
        Pageable next = page.nextPageable();
        assertNotNull(next);
        assertEquals(4L, next.page());

        page = packages.whereHeightNotWithin(20.0f, 38.5f, next);

        assertEquals(4L, page.number());

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
        page = packages.findByHeightGreaterThan(20.0f, Pageable.ofPage(3).size(3).beforeKeyset(10.0f, 31.0f, 310));

        assertEquals(3L, page.number());

        assertIterableEquals(List.of(355, 333, 330),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.saveAll(List.of(new Package(351, 22.0f, 50.0f, 31.f, "package#351"),
                                 new Package(336, 66.0f, 33.0f, 30.f, "package#336"),
                                 new Package(315, 66.0f, 31.0f, 37.f, "package#315")));

        // Page 2
        page = packages.findByHeightGreaterThan(20.0f, page.previousPageable());

        assertEquals(2L, page.number());

        assertIterableEquals(List.of(370, 350, 351),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteAllById(List.of(350, 333));

        // Page 1
        page = packages.findByHeightGreaterThan(20.0f, page.previousPageable());

        assertEquals(1L, page.number());

        assertIterableEquals(List.of(379, 376, 373),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        assertEquals(null, page.previousPageable());

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
                                             Pageable.ofSize(2)
                                                             .page(5)
                                                             .sortBy(Sort.asc("height"), Sort.desc("length"), Sort.asc("id"))
                                                             .beforeKeyset(40.0f, 0.0f, 0));

        assertEquals(5L, page.number());

        assertIterableEquals(List.of(315, 373),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.deleteAllById(List.of(373, 315, 376));

        page = packages.whereHeightNotWithin(32.0f, 35.5f, page.previousPageable());

        assertEquals(4L, page.number());

        assertIterableEquals(List.of(351, 370),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        packages.save(new Package(331, 33.0f, 41.0f, 31.0f, "package#351"));

        page = packages.whereHeightNotWithin(32.0f, 35.5f, page.previousPageable());

        assertEquals(3L, page.number());

        assertIterableEquals(List.of(379, 331),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        page = packages.whereHeightNotWithin(32.0f, 35.5f, page.previousPageable());

        assertEquals(2L, page.number());

        assertIterableEquals(List.of(330, 310),
                             page.stream().map(pkg -> pkg.id).collect(Collectors.toList()));

        Pageable previous = page.previousPageable();
        assertNotNull(previous);

        // delete the only previous entry and visit the empty previous page

        packages.deleteById(336);

        page = packages.whereHeightNotWithin(32.0f, 35.5f, page.previousPageable());

        assertEquals(1L, page.number());

        assertIterableEquals(Collections.EMPTY_LIST, page.content());

        // attempt next after an empty page
        assertEquals(null, page.nextPageable());
        assertEquals(null, page.previousPageable());
    }

    /**
     * A repository might define a method that returns a keyset-aware page with a Limit parameter.
     */
    @Test
    public void testKeysetWithLimit() {
        // This is not a recommended pattern. Testing to see how it is handled.
        KeysetAwarePage<Prime> page = primes.findByNumberIdBetween(15L, 45L, Limit.of(5));

        assertEquals(1L, page.number());
        assertEquals(5L, page.numberOfElements());
        assertEquals(5L, page.pageable().size());
        assertEquals(1L, page.pageable().page());
        assertEquals(2L, page.totalPages());
        assertEquals(8L, page.totalElements());

        assertIterableEquals(List.of(17L, 19L, 23L, 29L, 31L),
                             page.stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertIterableEquals(Collections.EMPTY_LIST,
                             primes.findByNumberIdBetween(15L, 45L, page.previousPageable()));

        page = primes.findByNumberIdBetween(15L, 45L, page.nextPageable());

        assertEquals(2L, page.number());
        assertEquals(3L, page.numberOfElements());
        assertEquals(5L, page.pageable().size());
        assertEquals(2L, page.pageable().page());
        assertEquals(2L, page.totalPages());
        assertEquals(8L, page.totalElements());
        assertEquals(null, page.nextPageable());

        assertIterableEquals(List.of(37L, 41L, 43L),
                             page.stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));
    }

    /**
     * A repository might define a method that returns a keyset-aware page without specifying a Pageable,
     * specifying the sort criteria separately.
     */
    @Test
    public void testKeysetWithoutPageable() {
        // This is not a recommended pattern. Testing to see how it is handled.
        KeysetAwarePage<Prime> page = primes.findByNumberIdBetweenAndBinaryDigitsNotNull(30L, 40L, Sort.asc("id"));
        assertEquals(31L, page.content().get(0).numberId);

        // Obtain Pageable for previous entries from the KeysetAwarePage
        Pageable pagination = page.previousPageable().size(5);
        page = primes.findByNumberIdBetween(0L, 40L, pagination);
        assertIterableEquals(List.of(13L, 17L, 19L, 23L, 29L),
                             page.stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        pagination = page.previousPageable();
        page = primes.findByNumberIdBetween(0L, 40L, pagination);
        assertIterableEquals(List.of(2L, 3L, 5L, 7L, 11L),
                             page.stream()
                                             .map(p -> p.numberId)
                                             .collect(Collectors.toList()));

        assertEquals(null, page.previousPageable());
    }

    /**
     * Intermix two different types of entities in the same transaction.
     */
    @Test
    public void testMixedEntitiesInTransaction() throws HeuristicMixedException, HeuristicRollbackException, //
                    IllegalStateException, NotSupportedException, RollbackException, SecurityException, SystemException {
        houses.deleteAll();
        vehicles.deleteAll();

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

        houses.deleteAll();
        vehicles.deleteAll();
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
     * Test NotBetween in a filter.
     */
    @Test
    public void testNotBetweenFromFilter() {
        assertIterableEquals(List.of(2L, 3L, 5L, 7L, 41L, 43L, 47L),
                             primes.notWithinButBelow(10, 40, 50));
    }

    /**
     * PageableRepository.findAll(Pageable) must raise NullPointerException.
     */
    @Test
    public void testNullPagination() {
        try {
            Page<Package> page = packages.findAll(null);
            fail("PageableRepository.findAll(Pageable) must raise NullPointerException. Instead: " + page);
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
            KeysetAwarePage<Prime> found = primes.findByNumberIdBetween(5L, 15L, Pageable.ofSize(Integer.MAX_VALUE / 30).page(33));
            fail("Expected an error because when offset for pagination exceeds Integer.MAX_VALUE. Found: " + found);
        } catch (DataException x) {
            if (x.getCause() instanceof IllegalArgumentException)
                ; // expected
            else
                throw x;
        }

        try {
            Page<Prime> found = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(52L, Pageable.ofSize(Integer.MAX_VALUE / 20).page(22));
            fail("Expected an error because when offset for pagination exceeds Integer.MAX_VALUE. Found: " + found);
        } catch (DataException x) {
            if (x.getCause() instanceof IllegalArgumentException)
                ; // expected
            else
                throw x;
        }
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

        assertEquals(true, shipments.dispatch(2, "44.036217, -92.488040", OffsetDateTime.now()));
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

        assertEquals(true, shipments.cancel(4, OffsetDateTime.now()));
        assertEquals(true, shipments.cancel(5, OffsetDateTime.now()));
        assertEquals(false, shipments.cancel(10, OffsetDateTime.now()));

        assertEquals(2, shipments.removeCanceled());

        assertEquals(3, shipments.removeEverything());
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
     * Use the provided methods of a Repository<T, K> interface that is a copy of Jakarta NoSQL's.
     */
    @Test
    public void testRepositoryBuiltInMethods() {
        ZoneOffset CDT = ZoneOffset.ofHours(-5);

        // remove data that other tests previously inserted to the same table
        reservations.deleteByHostNot("never-ever-used@example.org");

        assertEquals(0, reservations.count());

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

        assertEquals(1, reservations.count());

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

        assertEquals(4, reservations.count());

        Map<Long, Reservation> found = reservations.findAllById(List.of(r4.meetingID, r2.meetingID))
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

        assertEquals(3, reservations.count());

        reservations.deleteAllById(Set.of(r1.meetingID, r4.meetingID));

        assertEquals(false, reservations.existsById(r4.meetingID));

        assertEquals(1, reservations.count());

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
        reservations.deleteByHostNotIn(Set.of("never-ever-used@example.org"));

        assertEquals(0, reservations.count());

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
                                                                    Pageable.ofSize(4),
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
                                                                    page1.nextPageable(),
                                                                    Sort.desc("meetingID"));
        assertIterableEquals(List.of(10030005L, 10030004L, 10030003L, 10030002L),
                             page2
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));

        Page<Reservation> page3 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                                    page2.nextPageable(),
                                                                    Sort.desc("meetingID"));
        assertIterableEquals(List.of(10030001L),
                             page3
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));
        assertEquals(null, page3.nextPageable());
        assertEquals(true, page3.hasContent());
        assertEquals(1, page3.numberOfElements());

        // Paging that comes out even:
        page2 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                  Pageable.ofPage(2).size(3),
                                                  Sort.desc("meetingID"));
        assertIterableEquals(List.of(10030006L, 10030005L, 10030004L),
                             page2
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));
        page3 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                  page2.nextPageable(),
                                                  Sort.desc("meetingID"));
        assertIterableEquals(List.of(10030003L, 10030002L, 10030001L),
                             page3
                                             .stream()
                                             .map(r -> r.meetingID)
                                             .collect(Collectors.toList()));
        assertEquals(null, page3.nextPageable());

        // Page of nothing
        page1 = reservations.findByHostStartsWith("Not Found", Pageable.ofSize(100), Sort.asc("meetingID"));
        assertEquals(1L, page1.number());
        assertEquals(false, page1.hasContent());
        assertEquals(0, page1.numberOfElements());
        assertEquals(null, page1.nextPageable());
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

        // subtract from height and append to description via annotatively defined method with fixed values
        assertEquals(true, packages.shorten(990003));

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

        Page<Package> page = packages.findAll(Pageable.ofSize(3).sortBy(Sort.desc("id")));
        assertIterableEquals(List.of(990006, 990005, 990004),
                             page.stream().map(pack -> pack.id).collect(Collectors.toList()));

        page = packages.findAll(page.nextPageable());
        assertIterableEquals(List.of(990002, 990001),
                             page.stream().map(pack -> pack.id).collect(Collectors.toList()));

        packages.deleteAll(List.of(p1, p6));

        assertIterableEquals(List.of(990002, 990004, 990005),
                             packages.findAll().map(pack -> pack.id).sorted().collect(Collectors.toList()));

        packages.deleteAll();

        assertEquals(0, packages.count());
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
     * Insert, update, find, and delete entities.
     */
    @Test
    public void testSaveAndUpdateMultiple() {
        // find none
        houses.deleteAll();
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

        assertEquals(2L, houses.deleteAll());
    }

    /**
     * Insert, update, find, and delete an unannotated entity.
     */
    @Test
    public void testSaveMultipleAndUpdate() {
        vehicles.deleteAll();

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
        assertEquals(true, vehicles.deleteById(v1.vinId));

        // find none
        Optional<Vehicle> found = vehicles.findById(v1.vinId);
        assertEquals(false, found.isPresent());

        // update
        assertEquals(true, vehicles.updateByIdAddPrice("TE201234567890003", 500.0f));

        // find
        found = vehicles.findById("TE201234567890003");
        assertEquals(true, found.isPresent());
        assertEquals(25500f, found.get().price, 0.001f);

        vehicles.deleteAll();
    }

    /**
     * Experiment with making a repository method return a record-like type.
     */
    @Test
    public void testSelectAsRecord() {
        ZoneOffset CDT = ZoneOffset.ofHours(-5);

        // remove data that other tests previously inserted to the same table
        reservations.deleteByHostNotIn(Set.of("never-ever-used@example.org"));

        assertEquals(0, reservations.count());

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

        ReservedTimeSlot[] reserved = reservations.findByLocationAndStartBetweenOrderByStart("30-2 C206",
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

        assertEquals(1L, slice.number());
        assertEquals(4L, slice.numberOfElements());
        assertEquals(4L, slice.pageable().size());
        assertEquals(1L, slice.pageable().page());

        assertIterableEquals(List.of("XLVII", "XLIII", "XXXVII", "XXIII"),
                             slice.stream()
                                             .map(p -> p.romanNumeral)
                                             .collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndIdLessThan("II", 50L, slice.nextPageable(), Sort.desc("id"));

        assertEquals(2L, slice.number());
        assertEquals(4L, slice.numberOfElements());
        assertEquals(4L, slice.pageable().size());
        assertEquals(2L, slice.pageable().page());

        assertIterableEquals(List.of("XVII", "XIII", "VII", "III"),
                             slice.stream()
                                             .map(p -> p.romanNumeral)
                                             .collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndIdLessThan("II", 50L, slice.nextPageable(), Sort.desc("id"));

        assertEquals(3L, slice.number());
        assertEquals(1L, slice.numberOfElements());
        assertEquals(4L, slice.pageable().size());
        assertEquals(3L, slice.pageable().page());
        assertEquals(null, slice.nextPageable());

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
                                                                            Pageable.ofSize(5),
                                                                            Sort.asc("sumOfBits"), Sort.desc("id"));
        assertEquals(1L, slice.number());
        assertEquals(5, slice.numberOfElements());
        assertEquals(1L, slice.pageable().page());
        assertEquals(5, slice.pageable().size());

        assertIterableEquals(List.of(2L, 17L, 3L, 41L, 37L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndIdLessThan("I", 50L,
                                                               slice.nextPageable(),
                                                               Sort.asc("sumOfBits"), Sort.desc("id"));
        assertEquals(2L, slice.number());
        assertEquals(5, slice.numberOfElements());
        assertEquals(2L, slice.pageable().page());
        assertEquals(5, slice.pageable().size());

        assertIterableEquals(List.of(13L, 11L, 7L, 43L, 23L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndIdLessThan("I", 50L,
                                                               slice.nextPageable(),
                                                               Sort.asc("sumOfBits"), Sort.desc("id"));
        assertEquals(3L, slice.number());
        assertEquals(2, slice.numberOfElements());
        assertEquals(3L, slice.pageable().page());
        assertEquals(5, slice.pageable().size());

        assertIterableEquals(List.of(47L, 31L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(null, slice.nextPageable());
    }

    /**
     * Repository method that returns a Slice with the sort criteria provided by the OrderBy annotation.
     */
    @Test
    public void testSliceWithSortCriteriaInOrderByAnnotation() {
        Slice<Prime> slice = primes.findByRomanNumeralStartsWithAndIdLessThan("X", 50L, Pageable.ofSize(4));
        assertEquals(1L, slice.number());
        assertEquals(4, slice.numberOfElements());
        assertEquals(1L, slice.pageable().page());
        assertEquals(4, slice.pageable().size());

        assertIterableEquals(List.of("forty-seven", "thirty-one", "forty-three", "twenty-nine"),
                             slice.stream().map(p -> p.name).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralStartsWithAndIdLessThan("X", 50L, slice.nextPageable());
        assertEquals(2L, slice.number());
        assertEquals(4, slice.numberOfElements());
        assertEquals(2L, slice.pageable().page());
        assertEquals(4, slice.pageable().size());

        assertIterableEquals(List.of("twenty-three", "eleven", "forty-one", "nineteen"),
                             slice.stream().map(p -> p.name).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralStartsWithAndIdLessThan("X", 50L, slice.nextPageable());
        assertEquals(3L, slice.number());
        assertEquals(3, slice.numberOfElements());
        assertEquals(3L, slice.pageable().page());
        assertEquals(4, slice.pageable().size());

        assertIterableEquals(List.of("thirteen", "thirty-seven", "seventeen"),
                             slice.stream().map(p -> p.name).collect(Collectors.toList()));

        assertEquals(null, slice.nextPageable());
    }

    /**
     * Repository method that returns a Slice with the sort criteria provided in the Pageable
     */
    @Test
    public void testSliceWithSortCriteriaInPageable() {
        Slice<Prime> slice = primes.findByRomanNumeralEndsWithAndIdLessThan("II", 50L,
                                                                            Pageable.ofSize(6).sortBy(Sort.desc("numberId")));
        assertEquals(1L, slice.number());
        assertEquals(6, slice.numberOfElements());
        assertEquals(1L, slice.pageable().page());
        assertEquals(6, slice.pageable().size());

        assertIterableEquals(List.of(47L, 43L, 37L, 23L, 17L, 13L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        slice = primes.findByRomanNumeralEndsWithAndIdLessThan("II", 50L,
                                                               slice.nextPageable());
        assertEquals(2L, slice.number());
        assertEquals(3, slice.numberOfElements());
        assertEquals(2L, slice.pageable().page());
        assertEquals(6, slice.pageable().size());

        assertIterableEquals(List.of(7L, 3L, 2L),
                             slice.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(null, slice.nextPageable());
    }

    /**
     * When sort criteria is specified statically via the OrderBy annotation and
     * dynamically via Sorts from pagination, the static sort criteria is applied
     * before the dynamic sort criteria.
     */
    @Test
    public void testSortCriteriaOfOrderByAnnoTakesPrecedenceOverPaginationSorts() {

        Pageable pagination = Pageable.ofSize(9).sortBy(Sort.desc("numberId"));
        Page<Prime> page1 = primes.findByNumberIdLessThan(49L, pagination);

        assertIterableEquals(List.of("17(2)", "5(2)", "3(2)",
                                     "41(3)", "37(3)", "19(3)", "13(3)", "11(3)", "7(3)"),
                             page1.stream()
                                             .map(p -> p.numberId + "(" + p.sumOfBits + ")")
                                             .collect(Collectors.toList()));

        Page<Prime> page2 = primes.findByNumberIdLessThan(49L, page1.nextPageable());

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

        Pageable pagination = Pageable.ofSize(7).sortBy(Sort.asc("binaryDigits"));
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

        KeysetAwarePage<Prime> page2 = primes.upTo(47L, page1.nextPageable());

        assertIterableEquals(List.of("10011", "101001", "1011", "1101", "111",
                                     "10001", "101"),
                             page2.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        KeysetAwarePage<Prime> page3 = primes.upTo(47L, page2.nextPageable());

        assertIterableEquals(List.of("11"),
                             page3.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page2 = primes.upTo(47L, page3.previousPageable());

        assertIterableEquals(List.of("10011", "101001", "1011", "1101", "111",
                                     "10001", "101"),
                             page2.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page1 = primes.upTo(47L, page2.previousPageable());

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

        Pageable pagination = Pageable.ofSize(6).sortBy(Sort.desc("binaryDigits"));
        KeysetAwareSlice<Prime> page1 = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, pagination);

        assertIterableEquals(List.of("11", "101", "10001",
                                     "111", "1101", "1011"),
                             page1.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        KeysetAwareSlice<Prime> page2 = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page1.nextPageable());

        assertIterableEquals(List.of("101001", "10011", "100101",
                                     "11101", "10111", "101011"),
                             page2.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        KeysetAwareSlice<Prime> page3 = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page2.nextPageable());

        assertIterableEquals(List.of("11111", "101111",
                                     "10"),
                             page3.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        pagination = Pageable.ofSize(6)
                        .sortBy(Sort.desc("binaryDigits"))
                        .beforeKeysetCursor(page3.getKeysetCursor(1)); // before the middle element of page 3

        KeysetAwareSlice<Prime> page = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, pagination);

        assertIterableEquals(List.of("10011", "100101",
                                     "11101", "10111", "101011",
                                     "11111"),
                             page.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page.previousPageable());

        assertIterableEquals(List.of("101", "10001",
                                     "111", "1101", "1011", "101001"),
                             page.stream()
                                             .map(p -> p.binaryDigits)
                                             .collect(Collectors.toList()));

        page = primes.findByNumberIdLessThanOrderByEvenAscSumOfBitsAsc(52L, page.previousPageable());

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
        Pageable p1 = Pageable.ofSize(9);
        Streamable<Prime> streamable1 = primes.findByNumberIdLessThanEqualOrderByIdAsc(44L, p1);

        assertIterableEquals(List.of(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L),
                             streamable1.stream().map(p -> p.numberId).collect(Collectors.toList()));

        Pageable p2 = p1.next();
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
        Page<Prime> page1 = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(43L, Pageable.ofSize(6));

        assertEquals(3L, page1.totalPages());
        assertEquals(14L, page1.totalElements());

        assertIterableEquals(List.of(43L, 41L, 37L, 31L, 29L, 23L),
                             page1.content().stream().map(p -> p.numberId).collect(Collectors.toList()));

        Page<Prime> page2 = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(43L, page1.nextPageable());

        assertEquals(14L, page2.totalElements());
        assertEquals(3L, page2.totalPages());

        assertIterableEquals(List.of(19L, 17L, 13L, 11L, 7L, 5L),
                             page2.stream().map(p -> p.numberId).collect(Collectors.toList()));

        Page<Prime> page3 = primes.findByNumberIdLessThanEqualOrderByNumberIdDesc(43L, page2.nextPageable());

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
        Page<Map.Entry<Long, String>> page1 = primes.namesByNumber(47L, Pageable.ofSize(5));

        assertEquals(15L, page1.totalElements());
        assertEquals(3L, page1.totalPages());

        assertIterableEquals(List.of("eleven", "five", "forty-one", "forty-seven", "forty-three"),
                             page1.stream().map(e -> e.getValue()).collect(Collectors.toList()));

        Page<Map.Entry<Long, String>> page2 = primes.namesByNumber(47L, page1.nextPageable());

        assertEquals(3L, page2.totalPages());
        assertEquals(15L, page2.totalElements());

        assertIterableEquals(List.of("nineteen", "seven", "seventeen", "thirteen", "thirty-one"),
                             page2.stream().map(e -> e.getValue()).collect(Collectors.toList()));

        Page<Map.Entry<Long, String>> page3 = primes.namesByNumber(47L, page2.nextPageable());

        assertEquals(3L, page2.totalPages());
        assertEquals(15L, page2.totalElements());

        assertIterableEquals(List.of("thirty-seven", "three", "twenty-nine", "twenty-three", "two"),
                             page3.stream().map(e -> e.getValue()).collect(Collectors.toList()));

        assertEquals(null, page3.nextPageable());
    }

    /**
     * Obtain total counts of number of elements and pages when JPQL is supplied via the Query annotation
     * where a count query is inferred from the Query annotation value, which has an ORDER BY clause.
     */
    @Test
    public void testTotalCountsForQueryWithOrderBy() {
        Page<Integer> page1 = primes.romanNumeralLengths(41L, Pageable.ofSize(4));

        assertEquals(6L, page1.totalElements());
        assertEquals(2L, page1.totalPages());

        assertIterableEquals(List.of(6, 5, 4, 3), page1.content());

        Page<Integer> page2 = primes.romanNumeralLengths(41L, page1.nextPageable());

        assertEquals(2L, page2.totalPages());
        assertEquals(6L, page2.totalElements());

        assertIterableEquals(List.of(2, 1), page2.content());

        assertEquals(null, page2.nextPageable());
    }

    /**
     * Obtain total counts of number of elements and pages when JPQL is supplied via the Query annotation
     * where a count query is inferred from the Query annotation value, which lacks an ORDER BY clause
     * because the OrderBy annotation is used.
     */
    @Test
    public void testTotalCountsForQueryWithSeparateOrderBy() {
        Page<Object[]> page1 = primes.namesWithHex(40L, Pageable.ofSize(4));

        assertEquals(12L, page1.totalElements());
        assertEquals(3L, page1.totalPages());

        assertIterableEquals(List.of("two", "three", "five", "seven"),
                             page1.stream().map(o -> (String) o[0]).collect(Collectors.toList()));

        Page<Object[]> page2 = primes.namesWithHex(40L, page1.nextPageable());

        assertEquals(3L, page2.totalPages());
        assertEquals(12L, page2.totalElements());

        assertIterableEquals(List.of("eleven", "thirteen", "seventeen", "nineteen"),
                             page2.stream().map(o -> (String) o[0]).collect(Collectors.toList()));

        Page<Object[]> page3 = primes.namesWithHex(40L, page2.nextPageable());

        assertEquals(3L, page3.totalPages());
        assertEquals(12L, page3.totalElements());

        assertIterableEquals(List.of("twenty-three", "twenty-nine", "thirty-one", "thirty-seven"),
                             page3.stream().map(o -> (String) o[0]).collect(Collectors.toList()));

        assertEquals(null, page3.nextPageable());
    }

    /**
     * Obtain total counts of number of elements and pages when keyset pagination is used.
     */
    @Test
    public void testTotalCountsWithKeysetPagination() {
        KeysetAwarePage<Prime> page3 = primes.findByNumberIdBetween(3L, 50L, Pageable.ofPage(3).size(5).beforeKeyset(47L));
        assertEquals(14L, page3.totalElements());
        assertEquals(3L, page3.totalPages());

        assertIterableEquals(List.of(29L, 31L, 37L, 41L, 43L),
                             page3.stream().map(p -> p.numberId).collect(Collectors.toList()));

        KeysetAwarePage<Prime> page2 = primes.findByNumberIdBetween(3L, 50L, page3.previousPageable());
        assertEquals(3L, page2.totalPages());
        assertEquals(14L, page2.totalElements());

        assertIterableEquals(List.of(11L, 13L, 17L, 19L, 23L),
                             page2.stream().map(p -> p.numberId).collect(Collectors.toList()));

        KeysetAwarePage<Prime> page1 = primes.findByNumberIdBetween(3L, 50L, page2.previousPageable());
        assertEquals(3L, page1.totalPages());
        assertEquals(14L, page1.totalElements());

        assertIterableEquals(List.of(3L, 5L, 7L),
                             page1.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(null, page1.previousPageable());

        KeysetAwarePage<Prime> page4 = primes.findByNumberIdBetween(3L, 50L, page3.nextPageable());
        // In this case, the 14 elements are across 4 pages, not 3,
        // because the first and last pages ended up being partial.
        // But that doesn't become known until the first or last page is read.
        // This is one of many reasons why keyset pagination documents that
        // page counts are inaccurate and cannot be relied upon.
        assertEquals(3L, page4.totalPages());
        assertEquals(14L, page4.totalElements());

        assertIterableEquals(List.of(47L),
                             page4.stream().map(p -> p.numberId).collect(Collectors.toList()));

        assertEquals(null, page4.nextPageable());
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

        people.save(List.of(p1, p2, p3));

        System.out.println("TxType.SUPPORTS in transaction");

        tran.begin();
        try {
            assertEquals(true, people.setFirstNameInCurrentTransaction(p3.ssn_id, "Ty")); // update with MANDATORY
            assertEquals("Ty", people.getFirstNameInCurrentOrNoTransaction(p3.ssn_id)); // read value with SUPPORTS
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Thomas", "Timothy", "Tyler"),
                             people.findFirstNames("TestTransactional"));

        System.out.println("TxType.SUPPORTS from no transaction");

        assertEquals("Tyler", people.getFirstNameInCurrentOrNoTransaction(p3.ssn_id));

        System.out.println("TxType.REQUIRED in transaction");

        tran.begin();
        try {
            assertEquals(true, people.setFirstNameInCurrentOrNewTransaction(p1.ssn_id, "Tommy"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Thomas", "Timothy", "Tyler"),
                             people.findFirstNames("TestTransactional"));

        System.out.println("TxType.REQUIRED from no transaction");

        assertEquals(true, people.setFirstNameInCurrentOrNewTransaction(p1.ssn_id, "Tom"));

        assertIterableEquals(List.of("Timothy", "Tom", "Tyler"),
                             people.findFirstNames("TestTransactional"));

        System.out.println("TxType.MANDATORY in transaction");

        tran.begin();
        try {
            assertEquals(true, people.setFirstNameInCurrentTransaction(p3.ssn_id, "Ty"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Timothy", "Tom", "Tyler"),
                             people.findFirstNames("TestTransactional"));

        System.out.println("TxType.MANDATORY from no transaction is an error");

        try {
            boolean result = people.setFirstNameInCurrentTransaction(p3.ssn_id, "Ty");
            fail("Invoked TxType.MANDATORY operation with no transaction on thread. Result: " + result);
        } catch (TransactionalException x) {
            if (!(x.getCause() instanceof TransactionRequiredException))
                throw x;
        }

        System.out.println("TxType.REQUIRES_NEW in transaction");

        tran.begin();
        try {
            assertEquals(true, people.setFirstNameInNewTransaction(p2.ssn_id, "Timmy"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Timmy", "Tom", "Tyler"),
                             people.findFirstNames("TestTransactional"));

        System.out.println("TxType.REQUIRES_NEW from no transaction");

        assertEquals(true, people.setFirstNameInCurrentOrNewTransaction(p2.ssn_id, "Tim"));

        assertIterableEquals(List.of("Tim", "Tom", "Tyler"),
                             people.findFirstNames("TestTransactional"));

        System.out.println("TxType.NEVER in transaction");

        tran.begin();
        try {
            boolean result = people.setFirstNameWhenNoTransactionIsPresent(p3.ssn_id, "Ty");
            fail("Invoked TxType.NEVER operation with transaction on thread. Result: " + result);
        } catch (TransactionalException x) {
            if (!(x.getCause() instanceof InvalidTransactionException))
                throw x;
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Tim", "Tom", "Tyler"),
                             people.findFirstNames("TestTransactional"));

        System.out.println("TxType.NEVER from no transaction");

        assertEquals(true, people.setFirstNameWhenNoTransactionIsPresent(p3.ssn_id, "Ty"));

        assertIterableEquals(List.of("Tim", "Tom", "Ty"),
                             people.findFirstNames("TestTransactional"));

        System.out.println("TxType.NOT_SUPPORTED in transaction");

        tran.begin();
        try {
            assertEquals(true, people.setFirstNameWithCurrentTransactionSuspended(p3.ssn_id, "Tyler"));
        } finally {
            tran.rollback();
        }

        assertIterableEquals(List.of("Tim", "Tom", "Tyler"),
                             people.findFirstNames("TestTransactional"));

        System.out.println("TxType.NOT_SUPPORTED from no transaction");

        assertEquals("Tyler", people.getFirstNameInCurrentOrNoTransaction(p3.ssn_id));

        personnel.removeAll().get(TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Update multiple entries.
     */
    @Test
    public void testUpdateAnnotation() {
        products.clear();

        Product prod1 = new Product();
        prod1.id = "UPD-ANNO-1";
        prod1.name = "Fairly Priced TestUpdateAnnotation Item";
        prod1.price = 5.00f;
        products.save(prod1);

        Product prod2 = new Product();
        prod2.id = "UPD-ANNO-2";
        prod2.name = "Highly Priced TestUpdateAnnotation Item";
        prod2.price = 100.00f;
        products.save(prod2);

        Product prod3 = new Product();
        prod3.id = "UPD-ANNO-3";
        prod3.name = "Middle Priced TestUpdateAnnotation Item";
        prod3.price = 40.00f;
        products.save(prod3);

        Product prod4 = new Product();
        prod4.id = "UPD-ANNO-4";
        prod4.name = "Inexpensive TestUpdateAnnotation Item";
        prod4.price = 2.00f;
        products.save(prod4);

        Product prod5 = new Product();
        prod5.id = "UPD-ANNO-5";
        prod5.name = "Ridiculously High Priced TestUpdateAnnotation Item";
        prod5.price = 500.00f;
        products.save(prod5);

        Product prod6 = new Product();
        prod6.id = "UPD-ANNO-6";
        prod6.name = "Lowest Priced TestUpdateAnnotation Item";
        prod6.price = 1.00f;
        products.save(prod6);

        assertEquals(true, products.isNotEmpty());
        assertEquals(6, products.total());

        assertEquals(5, products.inflatePrices("Priced TestUpdateAnnotation Item", 1.07f)); // prod4 does not match

        Product[] found = products.findByVersionGreaterThanEqualOrderByPrice(2);

        assertEquals(Stream.of(found).map(p -> p.id).collect(Collectors.toList()).toString(),
                     5, found.length);

        assertEquals(1.07f, found[0].price, 0.001f);
        assertEquals(5.35f, found[1].price, 0.001f);
        assertEquals(42.80f, found[2].price, 0.001f);
        assertEquals(107.00f, found[3].price, 0.001f);
        assertEquals(535.00f, found[4].price, 0.001f);

        Product item = products.findItem("UPD-ANNO-4");
        assertEquals(2.00f, item.price, 0.001f);

        products.undoPriceIncrease(Set.of("UPD-ANNO-5", "UPD-ANNO-2", "UPD-ANNO-1"), 1.07f);

        found = products.findByVersionGreaterThanEqualOrderByPrice(1);

        assertEquals(Stream.of(found).map(p -> p.id).collect(Collectors.toList()).toString(),
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
        prod1.id = "800-2024-S";
        prod1.name = "Small size TestUpdateMultiple-matched item";
        prod1.price = 10.00f;
        products.save(prod1);

        Product prod2 = new Product();
        prod2.id = "800-3024-M";
        prod2.name = "Medium size TestUpdateMultiple-matched item";
        prod2.price = 15.00f;
        products.save(prod2);

        Product prod3 = new Product();
        prod3.id = "C6000-814BH0003Y";
        prod3.name = "Medium size TestUpdateMultiple non-matching item";
        prod3.price = 18.00f;
        products.save(prod3);

        Product prod4 = new Product();
        prod4.id = "800-4024-L";
        prod4.name = "Large size TestUpdateMultiple-matched item";
        prod4.price = 20.00f;
        products.save(prod4);

        Product[] p = products.findByVersionGreaterThanEqualOrderByPrice(0);

        // JPA knows to update the version even through the JPQL didn't explicitly tell it to
        assertEquals(3, products.putOnSale("TestUpdateMultiple-match", .20f));

        Product p1 = products.findItem(prod1.id);
        assertEquals(8.00f, p1.price, 0.001f);
        assertEquals(p[0].version + 1, p1.version); // updated

        Product p2 = products.findItem(prod2.id);
        assertEquals(12.00f, p2.price, 0.001f);
        assertEquals(p[1].version + 1, p2.version); // updated

        Product p3 = products.findItem(prod3.id);
        assertEquals(18.00f, p3.price, 0.001f);
        assertEquals(p[2].version, p3.version); // not updated, version remains the same

        Product p4 = products.findItem(prod4.id);
        assertEquals(16.00f, p4.price, 0.001f);
        assertEquals(p[3].version + 1, p4.version); // updated
    }

    /**
     * Use JPQL query to update based on version.
     */
    @Test
    public void testVersionedUpdateViaQuery() {
        Product prod1 = new Product();
        prod1.id = "Q6008-U8-21001";
        prod1.name = "testVersionedUpdateViaQuery Product 1";
        prod1.price = 82.99f;
        products.save(prod1);

        Product p = products.findItem(prod1.id);
        long initialVersion = p.version;

        assertEquals(true, products.setPrice(prod1.id, initialVersion, 84.99f));
        assertEquals(false, products.setPrice(prod1.id, initialVersion, 83.99f));
        assertEquals(true, products.setPrice(prod1.id, initialVersion + 1, 88.99f));

        p = products.findItem(prod1.id);
        assertEquals(88.99f, p.price, 0.001f);
        assertEquals(initialVersion + 2, p.version);
    }

    /**
     * Use repository save method to update based on version.
     */
    @Test
    public void testVersionedUpdateViaRepository() {
        Product prod1 = new Product();
        prod1.id = "3400R-6120-1";
        prod1.name = "TestVersionedUpdateViaRepository Product 1";
        prod1.price = 139.99f;
        products.save(prod1);

        Product prod1a = products.findItem(prod1.id);
        Product prod1b = products.findItem(prod1.id);

        long version;
        assertEquals(version = prod1a.version, prod1b.version);

        prod1a.price += 15.00f;
        prod1b.price += 10.00f;

        products.save(prod1b);

        try {
            products.save(prod1a);
            fail("Able to update using old version.");
        } catch (DataException x) {
            if (x.getCause() != null && "jakarta.persistence.OptimisticLockException".equals(x.getCause().getClass().getName()))
                ; // expected;
            else
                throw x;
        }

        Product p = products.findItem(prod1.id);
        assertEquals(149.99f, p.price, 0.001f);
        assertEquals(version + 1, p.version);
    }
}
