/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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
package test.jakarta.data.jpa.web;

import static com.ibm.websphere.simplicity.config.DataSourceProperties.DERBY_EMBEDDED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static test.jakarta.data.jpa.web.Assertions.assertArrayEquals;
import static test.jakarta.data.jpa.web.Assertions.assertIterableEquals;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.Streamable;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.KeysetAwarePage;
import jakarta.data.page.KeysetAwareSlice;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.simplicity.config.dsprops.testrules.SkipIfDataSourceProperties;

import componenttest.app.FATServlet;
import test.jakarta.data.jpa.web.CreditCard.CardId;
import test.jakarta.data.jpa.web.CreditCard.Issuer;

@DataSourceDefinition(name = "java:module/jdbc/RepositoryDataStore",
                      className = "${repository.datasource.class.name}",
                      databaseName = "${repository.database.name}",
                      user = "${repository.database.user}",
                      password = "${repository.database.password}",
                      properties = {
                                     "createDatabase=create",
                                     "data.createTables=${repository.database.tables.create}",
                                     "data.dropTables=${repository.database.tables.drop}",
                                     "data.tablePrefix=${repository.database.tables.prefix}"
                      })
@SuppressWarnings("serial")
@WebServlet("/*")
public class DataJPATestServlet extends FATServlet {

    @Inject
    Accounts accounts;

    @Inject
    Businesses businesses;

    @Inject
    Cities cities;

    @Inject
    Counties counties;

    @Inject
    CreditCards creditCards;

    @Inject
    Customers customers;

    @Inject
    Drivers drivers;

    @Inject
    Employees employees;

    @Inject
    MixedRepository mixed;

    @Inject
    Orders orders;

    @Inject
    Rebates rebates;

    @Inject
    ShippingAddresses shippingAddresses;

    @Inject
    Tariffs tariffs;

    @Inject
    TaxPayers taxpayers;

    @Resource
    private UserTransaction tran;

    @Override
    public void init(ServletConfig config) throws ServletException {
        // Some read-only data that is prepopulated for tests:
        businesses.save(new Business(44.02288f, -92.46706f, "Rochester", "MN", 55905, 200, "1st St", "SW", "Mayo Clinic"));
        businesses.save(new Business(44.01225f, -92.46060f, "Rochester", "MN", 55904, 210, "9th St", "SE", "Olmsted Medical"));
        businesses.save(new Business(44.05938f, -92.50373f, "Rochester", "MN", 55901, 2800, "37th St", "NW", "IBM"));
        businesses.save(new Business(44.03876f, -92.46785f, "Rochester", "MN", 55901, 1600, "4th Ave", "NW", "Crenlo"));
        businesses.save(new Business(44.00001f, -92.48735f, "Rochester", "MN", 55902, 1661, "Greenview Dr", "SW", "Custom Alarm"));
        businesses.save(new Business(44.02760f, -92.47810f, "Rochester", "MN", 55901, 1016, "Civic Center Dr", "NW", "Home Federal Savings Bank"));
        businesses.save(new Business(44.03368f, -92.50604f, "Rochester", "MN", 55901, 2960, "Highway 14", "W", "Benike Construction"));
        businesses.save(new Business(44.05520f, -92.45250f, "Rochester", "MN", 55906, 3008, "Wellner Dr", "NE", "Cardinal"));
        businesses.save(new Business(44.04227f, -92.50800f, "Rochester", "MN", 55901, 3100, "19th St", "NW", "RAC"));
        businesses.save(new Business(44.03216f, -92.51595f, "Rochester", "MN", 55901, 3428, "Lakeridge Pl", "NW", "Metafile"));
        businesses.save(new Business(43.96788f, -92.46620f, "Rochester", "MN", 55902, 3706, "Enterprise Dr", "SW", "Reichel Foods"));
        businesses.save(new Business(44.03780f, -92.46140f, "Rochester", "MN", 55906, 1402, "Broadway Ave", "N", "Silver Lake Foods"));
        businesses.save(new Business(44.07725f, -92.52096f, "Rochester", "MN", 55901, 5201, "Members Parkway", "NW", "Think Bank"));
        businesses.save(new Business(43.86613f, -92.49040f, "Stewartville", "MN", 55976, 1421, "2nd Ave", "NW", "Geotek"));
        businesses.save(new Business(43.86788f, -92.49300f, "Stewartville", "MN", 55976, 345, "Rochester Medical Dr", "NW", "HALCON"));

        cities.save(new City("Rochester", "Minnesota", 121395, Set.of(507)));
        cities.save(new City("Rochester", "New York", 211328, Set.of(585)));
        cities.save(new City("Springfield", "Missouri", 169176, Set.of(417)));
        cities.save(new City("Springfield", "Illinois", 114394, Set.of(217, 447)));
        cities.save(new City("Springfield", "Massachusetts", 155929, Set.of(413)));
        cities.save(new City("Springfield", "Oregon", 59403, Set.of(458, 541)));
        cities.save(new City("Springfield", "Ohio", 58662, Set.of(326, 937)));
        cities.save(new City("Kansas City", "Missouri", 508090, Set.of(816, 975)));
        cities.save(new City("Kansas City", "Kansas", 156607, Set.of(913)));

        Customer c1 = new Customer(9210001, "Matthew@tests.openliberty.io", 5075550101L);
        Customer c2 = new Customer(9210002, "martin@tests.openliberty.io", 5075552222L);
        Customer c3 = new Customer(9210003, "MICHELLE@TESTS.OPENLIBERTY.IO", 5075553333L);
        Customer c4 = new Customer(9210004, "Megan@tests.openliberty.io", 5075552444L);
        Customer c5 = new Customer(9210005, "Maximilian@tests.openliberty.io", 5075550055L);
        Customer c6 = new Customer(9210006, "Monica@tests.openliberty.io", 5075550066L);
        Customer c7 = new Customer(9210007, "Molly@tests.openliberty.io", 5075552277L);

        CreditCard card1a = new CreditCard(c1, 1000921011110001L, 101, LocalDate.of(2021, 1, 10), LocalDate.of(2025, 1, 10), Issuer.AmericanExtravagance);
        CreditCard card1m = new CreditCard(c1, 1000921011120002L, 102, LocalDate.of(2021, 1, 21), LocalDate.of(2025, 1, 21), Issuer.MonsterCard);
        CreditCard card1v = new CreditCard(c1, 1000921011130003L, 103, LocalDate.of(2021, 1, 30), LocalDate.of(2025, 1, 30), Issuer.Feesa);

        c2.addCard(new CreditCard(c2, 2000921021110001L, 201, LocalDate.of(2022, 2, 10), LocalDate.of(2026, 2, 10), Issuer.AmericanExtravagance));
        c2.addCard(new CreditCard(c2, 2000921022220002L, 222, LocalDate.of(2022, 2, 22), LocalDate.of(2026, 2, 22), Issuer.Discrooger));

        c3.addCard(new CreditCard(c3, 3000921031110001L, 301, LocalDate.of(2023, 3, 10), LocalDate.of(2027, 3, 10), Issuer.Discrooger));
        c3.addCard(new CreditCard(c3, 3000921032220002L, 222, LocalDate.of(2023, 3, 23), LocalDate.of(2027, 3, 23), Issuer.MonsterCard));
        c3.addCard(new CreditCard(c3, 3000921033330003L, 303, LocalDate.of(2023, 3, 30), LocalDate.of(2027, 3, 30), Issuer.Feesa));

        c4.addCard(new CreditCard(c4, 4000921041110001L, 401, LocalDate.of(2020, 4, 10), LocalDate.of(2024, 4, 10), Issuer.MonsterCard));
        c4.addCard(new CreditCard(c4, 4000921042220002L, 222, LocalDate.of(2020, 4, 24), LocalDate.of(2024, 4, 24), Issuer.Feesa));

        c5.addCard(new CreditCard(c5, 5000921051110001L, 501, LocalDate.of(2021, 5, 10), LocalDate.of(2025, 5, 10), Issuer.Discrooger));
        c5.addCard(new CreditCard(c5, 5000921052220002L, 502, LocalDate.of(2021, 5, 25), LocalDate.of(2025, 5, 25), Issuer.MonsterCard));

        c6.addCard(new CreditCard(c6, 6000921061110001L, 601, LocalDate.of(2022, 6, 10), LocalDate.of(2026, 6, 10), Issuer.AmericanExtravagance));
        c6.addCard(new CreditCard(c6, 6000921062220002L, 222, LocalDate.of(2022, 6, 26), LocalDate.of(2026, 6, 26), Issuer.Feesa));

        // These are implicitly saved when the corresponding Customer entities are saved.
        new DeliveryLocation(10001L, 1001, new Street("1st Ave", "SW"), DeliveryLocation.Type.HOME, c1);
        new DeliveryLocation(20002L, 2002, new Street("2nd Ave", "NE"), DeliveryLocation.Type.HOME, c2, c3);
        new DeliveryLocation(30003L, 2800, new Street("37th St", "NW"), DeliveryLocation.Type.BUSINESS, c3, c4, c5, c6);
        new DeliveryLocation(40004L, 4004, new Street("4th Ave", "SE"), DeliveryLocation.Type.HOME, c4, c5, c7);

        creditCards.save(card1a, card1m, card1v);
        creditCards.save(c2, c3, c4);
        customers.save(c5, c6, c7);
    }

    /**
     * Test the AbsoluteValue Function by querying on values that could be positive or negative.
     */
    @Test
    public void testAbsoluteValueFunction() {
        List<Business> found = businesses.withLongitudeIgnoringSignWithin(92.503f, 92.504f);
        assertNotNull(found);
        assertEquals("Found " + found.toString(), 1, found.size());
        assertEquals("IBM", found.get(0).name);
    }

    /**
     * Test the AbsoluteValue keyword by querying on values that could be positive or negative.
     */
    @Test
    public void testAbsoluteValueKeyword() {
        List<Business> found = businesses.findByLocationLongitudeAbsoluteValueBetween(92.503f, 92.504f);
        assertNotNull(found);
        assertEquals("Found " + found.toString(), 1, found.size());
        assertEquals("IBM", found.get(0).name);
    }

    /**
     * Verify that an EntityManager can be obtained for a repository and used to perform database operations.
     */
    @Test
    public void testEntityManager() throws Exception {
        counties.deleteByNameIn(List.of("Houston"));

        int[] houstonZipCodes = new int[] { 55919, 55921, 55931, 55941, 55943, 55947, 55971, 55974 };

        County houston = new County("Houston", "Minnesota", 18843, houstonZipCodes, "Caledonia", "Brownsville", "Eitzen", "Hokah", "Houston", "La Crescent", "Spring Grove");

        counties.insert(houston);

        // Unlike save, which uses em.merge, the custom insert method uses em.persist and raises an error if the entity already exists,
        try {
            counties.insert(houston);
        } catch (PersistenceException x) {
            // expected
        }

        County c = counties.findByName("Houston").orElseThrow();

        assertEquals("Houston", c.name);
        assertEquals(18843, c.population);
        assertEquals(Arrays.toString(houstonZipCodes), Arrays.toString(c.zipcodes));

        assertEquals(1, counties.deleteByNameIn(List.of("Houston")));
    }

    /**
     * Verify that an EntityManager is automatically closed when it goes out of scope of the default method
     * where it was obtained.
     */
    @Test
    public void testEntityManagerAutomaticallyClosed() {
        EntityManager em = counties.getAutoClosedEntityManager();
        assertEquals(false, em.isOpen());
    }

    /**
     * Verify that the EntityManager can be obtained outside the scope of a default method.
     */
    @Test
    public void testEntityManagerOutsideOfDefaultMethod() {
        try (EntityManager em = counties.getEntityManager()) {
            assertEquals(true, em.isOpen());
            System.out.println("Entity manager properties: " + em.getProperties());
        }
    }

    /**
     * Verify that an EntityManager remains open when a default method invokes another default method,
     * and is closed after the outer default method ends.
     */
    @Test
    public void testEntityManagerRemainsOpenAfterNestedDefaultMethod() {
        Object[] isOpenFromTopLevelDefaultMethod = counties.topLevelDefaultMethod();
        EntityManager emOuter1 = (EntityManager) isOpenFromTopLevelDefaultMethod[0];
        EntityManager emOuter2 = (EntityManager) isOpenFromTopLevelDefaultMethod[1];
        assertEquals(false, emOuter1.isOpen()); // must be closed after default method ends
        assertEquals(false, emOuter2.isOpen()); // must be closed after default method ends
        assertEquals(Boolean.TRUE, isOpenFromTopLevelDefaultMethod[2]); // outer1
        assertEquals(Boolean.TRUE, isOpenFromTopLevelDefaultMethod[3]); // outer2
        assertEquals(Boolean.FALSE, isOpenFromTopLevelDefaultMethod[4]); // inner
    }

    /**
     * Query-by-method name repository operation to remove and return one or more entities
     * where the entity has an IdClass.
     */
    // Test annotation is present on corresponding method in DataTest
    public void testFindAndDeleteEntityThatHasAnIdClass(HttpServletRequest request, HttpServletResponse response) {
        String jdbcJarName = request.getParameter("jdbcJarName").toLowerCase();
        boolean supportsOrderByForUpdate = !jdbcJarName.startsWith("derby");

        cities.save(new City("Milwaukee", "Wisconsin", 577222, Set.of(414)));
        cities.save(new City("Green Bay", "Wisconsin", 107395, Set.of(920)));
        cities.save(new City("Superior", "Wisconsin", 26751, Set.of(534, 715)));

        cities.save(new City("Sioux Falls", "South Dakota", 192517, Set.of(605)));
        cities.save(new City("Rapid City", "South Dakota", 74703, Set.of(605)));
        cities.save(new City("Brookings", "South Dakota", 23377, Set.of(605)));
        cities.save(new City("Watertown", "South Dakota", 22655, Set.of(605)));
        cities.save(new City("Spearfish", "South Dakota", 12193, Set.of(605)));
        cities.save(new City("Aberdeen", "South Dakota", 28324, Set.of(605)));
        cities.save(new City("Mitchell", "South Dakota", 15660, Set.of(605)));
        cities.save(new City("Pierre", "South Dakota", 14091, Set.of(605)));

        Streamable<City> removed = supportsOrderByForUpdate //
                        ? cities.removeByStateNameOrderByName("Wisconsin") //
                        : cities.removeByStateName("Wisconsin");

        Stream<City> stream = removed.stream();
        if (!supportsOrderByForUpdate)
            stream = stream.sorted(Comparator.comparing(c -> c.name));

        List<City> list = stream.collect(Collectors.toList());
        assertEquals(list.toString(), 3, list.size());

        assertEquals("Green Bay", list.get(0).name);
        assertEquals("Wisconsin", list.get(0).stateName);
        assertEquals(107395, list.get(0).population);
        assertIterableEquals(Set.of(920), list.get(0).areaCodes);

        assertEquals("Milwaukee", list.get(1).name);
        assertEquals("Wisconsin", list.get(1).stateName);
        assertEquals(577222, list.get(1).population);
        assertIterableEquals(Set.of(414), list.get(1).areaCodes);

        assertEquals("Superior", list.get(2).name);
        assertEquals("Wisconsin", list.get(2).stateName);
        assertEquals(26751, list.get(2).population);
        assertIterableEquals(List.of(534, 715), new TreeSet<Integer>(list.get(2).areaCodes));

        Set<String> cityNames = new TreeSet<>();
        cityNames.add("Sioux Falls");
        cityNames.add("Rapid City");
        cityNames.add("Brookings");
        cityNames.add("Watertown");
        cityNames.add("Spearfish");
        cityNames.add("Aberdeen");
        cityNames.add("Mitchell");
        cityNames.add("Pierre");

        Order<City> orderByCityName = supportsOrderByForUpdate ? Order.by(Sort.asc("name")) : Order.by();
        Iterator<CityId> ids = cities.deleteFirst3ByStateName("South Dakota", orderByCityName).iterator();
        CityId id;

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.stateName);
        if (supportsOrderByForUpdate)
            assertEquals("Aberdeen", id.name);
        // else order is unknown, but at least must be one of the city names that we added and haven't removed yet
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.stateName);
        if (supportsOrderByForUpdate)
            assertEquals("Brookings", id.name);
        // else order is unknown, but at least must be one of the city names that we added and haven't removed yet
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.stateName);
        if (supportsOrderByForUpdate)
            assertEquals("Mitchell", id.name);
        // else order is unknown, but at least must be one of the city names that we added and haven't removed yet
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(false, ids.hasNext());

        Order<City> orderByPopulation = supportsOrderByForUpdate ? Order.by(Sort.asc("population")) : Order.by();
        id = cities.deleteFirstByStateName("South Dakota", orderByPopulation).orElseThrow();
        assertEquals("South Dakota", id.stateName);
        if (supportsOrderByForUpdate)
            assertEquals("Pierre", id.name);
        // else order is unknown, but at least must be one of the city names that we added and haven't removed yet
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        id = cities.deleteByStateName("South Dakota", Limit.of(1));
        assertEquals("South Dakota", id.stateName);
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        Streamable<CityId> some = cities.deleteSome("South Dakota", Limit.of(2));
        ids = some.iterator();

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.stateName);
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.stateName);
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(false, ids.hasNext());

        // Should only have 1 left:
        some = cities.deleteSome("South Dakota", Limit.of(5));
        ids = some.iterator();

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.stateName);
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(false, ids.hasNext());
    }

    /**
     * Annotatively-defined repository operation to remove and return one or more IdClass
     * instances corresponding to the removed entities.
     */
    @Test
    public void testFindAndDeleteReturningIdClassArray(HttpServletRequest request, HttpServletResponse response) {

        cities.save(new City("Bloomington", "Minnesota", 89987, Set.of(952)));
        cities.save(new City("Plymouth", "Minnesota", 79828, Set.of(763)));
        cities.save(new City("Woodbury", "Minnesota", 75102, Set.of(651)));
        cities.save(new City("Brooklyn Park", "Minnesota", 86478, Set.of(763)));

        CityId[] removed = cities.deleteWithinPopulationRange(75000, 99999);

        assertEquals(Arrays.toString(removed), 4, removed.length);

        Arrays.sort(removed, Comparator.comparing(CityId::toString));

        assertEquals("Bloomington", removed[0].name);
        assertEquals("Minnesota", removed[0].stateName);

        assertEquals("Brooklyn Park", removed[1].name);
        assertEquals("Minnesota", removed[1].stateName);

        assertEquals("Plymouth", removed[2].name);
        assertEquals("Minnesota", removed[2].stateName);

        assertEquals("Woodbury", removed[3].name);
        assertEquals("Minnesota", removed[3].stateName);

        removed = cities.deleteWithinPopulationRange(75000, 99999);

        assertEquals(Arrays.toString(removed), 0, removed.length);

        // Ensure non-matching entities remain in the database
        assertEquals(true, cities.existsById(CityId.of("Rochester", "Minnesota")));
    }

    /**
     * Query-by-method name repository operation to remove and return one or more IdClass
     * instances corresponding to the removed entities.
     */
    @Test
    public void testFindAndDeleteReturningIdClassList(HttpServletRequest request, HttpServletResponse response) {

        cities.save(new City("Davenport", "Iowa", 101724, Set.of(563)));
        cities.save(new City("Sioux City", "Iowa", 85797, Set.of(712)));
        cities.save(new City("Iowa City", "Iowa", 74828, Set.of(319)));

        LinkedList<CityId> removed = cities.deleteByStateName("Iowa");

        assertEquals(removed.toString(), 3, removed.size());

        Collections.sort(removed, Comparator.comparing(CityId::toString));

        Iterator<CityId> ids = removed.iterator();

        CityId id = ids.next();
        assertEquals("Davenport", id.name);
        assertEquals("Iowa", id.stateName);

        id = ids.next();
        assertEquals("Iowa City", id.name);
        assertEquals("Iowa", id.stateName);

        id = ids.next();
        assertEquals("Sioux City", id.name);
        assertEquals("Iowa", id.stateName);

        removed = cities.deleteByStateName("Iowa");

        assertEquals(removed.toString(), 0, removed.size());

        // Ensure non-matching entities remain in the database
        assertEquals(true, cities.existsById(CityId.of("Rochester", "Minnesota")));
    }

    /**
     * Add, search, and remove entities with Embeddable fields.
     */
    @Test
    public void testEmbeddable() {
        shippingAddresses.removeAll();

        ShippingAddress a1 = new ShippingAddress();
        a1.id = 1001L;
        a1.city = "Rochester";
        a1.state = "Minnesota";
        a1.streetAddress = new StreetAddress(2800, "37th St NW", List.of("Receiving Dock", "Building 040-1"));
        a1.zipCode = 55901;

        ShippingAddress a2 = new ShippingAddress();
        a2.id = 1002L;
        a2.city = "Rochester";
        a2.state = "Minnesota";
        a2.streetAddress = new StreetAddress(201, "4th St SE");
        a2.zipCode = 55904;

        ShippingAddress a3 = new ShippingAddress();
        a3.id = 1003L;
        a3.city = "Rochester";
        a3.state = "Minnesota";
        a3.streetAddress = new StreetAddress(200, "1st Ave SW");
        a3.zipCode = 55902;

        ShippingAddress a4 = new ShippingAddress();
        a4.id = 1004L;
        a4.city = "Rochester";
        a4.state = "Minnesota";
        a4.streetAddress = new StreetAddress(151, "4th St SE");
        a4.zipCode = 55904;

        Set<ShippingAddress> added = shippingAddresses.save(Set.of(a1, a2, a3, a4));

        assertEquals(Set.of(1001L, 1002L, 1003L, 1004L),
                     added.stream().map(a -> a.id).collect(Collectors.toSet()));

        assertArrayEquals(new ShippingAddress[] { a4, a2 },
                          shippingAddresses.findByStreetNameOrderByHouseNumber("4th St SE"),
                          Comparator.<ShippingAddress, Long> comparing(o -> o.id)
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.city))
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.state))
                                          .thenComparing(Comparator.<ShippingAddress, Integer> comparing(o -> o.streetAddress.houseNumber))
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.streetAddress.streetName))
                                          .thenComparing(Comparator.<ShippingAddress, Integer> comparing(o -> o.zipCode)));

        assertIterableEquals(List.of("200 1st Ave SW", "151 4th St SE", "201 4th St SE"),
                             Stream.of(shippingAddresses.findByHouseNumberBetweenOrderByStreetNameAscHouseNumber(150, 250))
                                             .map(a -> a.houseNumber + " " + a.streetName)
                                             .collect(Collectors.toList()));

        // [EclipseLink-6002] Aggregated objects cannot be written/deleted/queried independently from their owners.
        //                    Descriptor: [RelationalDescriptor(test.jakarta.data.web.StreetAddress --> [])]
        //                    Query: ReportQuery(referenceClass=StreetAddress )
        // TODO uncomment the following to reproduce the above error:
        // List<ShippingAddress> found = shippingAddresses.findByRecipientInfoNotEmpty();
        // assertEquals(1, found.size());
        // ShippingAddress a = found.get(0);
        // assertEquals(a1.id, a.id);
        // assertEquals(a1.city, a.city);
        // assertEquals(a1.state, a.state);
        // assertEquals(a1.zipCode, a.zipCode);
        // assertEquals(a1.streetAddress.houseNumber, a.streetAddress.houseNumber);
        // assertEquals(a1.streetAddress.streetName, a.streetAddress.streetName);
        // assertEquals(a1.streetAddress.recipientInfo, a.streetAddress.recipientInfo);

        // assertEquals(3L, shippingAddresses.countByRecipientInfoEmpty());

        // [EclipseLink-4002] Internal Exception: java.sql.SQLIntegrityConstraintViolationException:
        //                    DELETE on table 'SHIPPINGADDRESS' caused a violation of foreign key constraint 'SHPPNGSHPPNGDDRSSD' for key (1001)
        // TODO Entity removal fails without the above error unless we add the following lines to first remove the rows from the collection attribute's table,
        a1.streetAddress.recipientInfo = new ArrayList<>();
        shippingAddresses.save(a1);

        assertEquals(4, shippingAddresses.removeAll());
    }

    /**
     * Use an Entity which has an attribute which is a collection of embeddables, as permitted by the JPA ElementCollection annotation.
     */
    @Test
    public void testEmbeddableCollection() {
        taxpayers.delete();

        AccountId a1 = AccountId.of(15561600, 391588);
        AccountId a2 = AccountId.of(26122300, 410224);
        AccountId a3 = AccountId.of(60212900, 391588);
        AccountId a4 = AccountId.of(43014400, 410224);
        AccountId a5 = AccountId.of(55435500, 560237);
        AccountId a6 = AccountId.of(66320100, 410224);
        AccountId a7 = AccountId.of(77512000, 705030);
        AccountId a8 = AccountId.of(88191200, 410224);
        AccountId a9 = AccountId.of(99105300, 391588);
        AccountId a10 = AccountId.of(10105600, 560237);

        TaxPayer t1 = new TaxPayer(123001230L, TaxPayer.FilingStatus.HeadOfHousehold, 3, 54000.0f, a1, a10);
        TaxPayer t2 = new TaxPayer(234002340L, TaxPayer.FilingStatus.MarriedFilingJointly, 2, 212000.0f, a6, a7, a8);
        TaxPayer t3 = new TaxPayer(345003450L, TaxPayer.FilingStatus.MarriedFilingSeparately, 0, 95000.0f, a2, a3);
        TaxPayer t4 = new TaxPayer(456004560L, TaxPayer.FilingStatus.HeadOfHousehold, 1, 41000.0f, a4);
        TaxPayer t5 = new TaxPayer(567005670L, TaxPayer.FilingStatus.Single, 0, 133000.0f, a5, a9);
        TaxPayer t6 = new TaxPayer(678006780L, TaxPayer.FilingStatus.MarriedFilingSeparately, 3, 126000.0f, a2);
        TaxPayer t7 = new TaxPayer(789007890L, TaxPayer.FilingStatus.Single, 0, 37000.0f);

        Iterable<TaxPayer> added = taxpayers.save(List.of(t1, t2, t3, t4));

        assertEquals(List.of(123001230L, 234002340L, 345003450L, 456004560L),
                     StreamSupport.stream(added.spliterator(), false)
                                     .map(t -> t.ssn)
                                     .collect(Collectors.toList()));

        Iterator<TaxPayer> addedIt = taxpayers.save(t5, t6, t7);

        assertEquals(List.of(567005670L, 678006780L, 789007890L),
                     StreamSupport.stream(Spliterators.spliteratorUnknownSize(addedIt, Spliterator.ORDERED), false)
                                     .map(t -> t.ssn)
                                     .collect(Collectors.toList()));

        assertIterableEquals(List.of("AccountId:66320100:410224", "AccountId:77512000:705030", "AccountId:88191200:410224"),
                             taxpayers.findAccountsBySSN(234002340L)
                                             .stream()
                                             .map(AccountId::toString)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("AccountId:10105600:560237", "AccountId:15561600:391588", "AccountId:43014400:410224"),
                             taxpayers.findBankAccountsByFilingStatus(TaxPayer.FilingStatus.HeadOfHousehold)
                                             .map(AccountId::toString)
                                             .sorted()
                                             .collect(Collectors.toList()));

        // TODO report EclipseLink bug that occurs on the following
        if (false)
            assertIterableEquals(List.of(345003450L, 678006780L),
                                 taxpayers.findByBankAccountsContains(AccountId.of(26122300, 410224))
                                                 .map(t -> t.ssn)
                                                 .collect(Collectors.toList()));

        // TODO also fails with EclipseLink error
        if (false)
            assertIterableEquals(List.of(789007890L),
                                 taxpayers.findByBankAccountsNotEmpty()
                                                 .map(t -> t.ssn)
                                                 .collect(Collectors.toList()));

        taxpayers.delete();
    }

    /**
     * Repository method that queries and sorts by an embeddable attribute of an entity.
     */
    @Test
    public void testEmbeddableDepth1() {
        assertIterableEquals(List.of("Olmsted Medical", "Mayo Clinic", "Home Federal Savings Bank", "Custom Alarm"),
                             businesses.findByLatitudeBetweenOrderByLongitudeDesc(44.0f, 44.03f)
                                             .stream()
                                             .map(b -> b.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method that queries and sorts by an embeddable attribute of an embeddable attribute of an entity.
     */
    @Test
    public void testEmbeddableDepth2() {
        KeysetAwareSlice<Business> page;
        List<Integer> zipCodes = List.of(55906, 55902, 55901, 55976, 55905);

        page = businesses.findByZipIn(zipCodes, PageRequest.ofSize(4));

        assertIterableEquals(List.of(345, 1421, 1016, 1600),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        page = businesses.findByZipIn(zipCodes, page.nextPageRequest());

        assertIterableEquals(List.of(2800, 2960, 3100, 3428),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(2L, page.pageRequest().page());
        assertEquals(4, page.pageRequest().size());

        page = businesses.findByZipIn(zipCodes, page.nextPageRequest());

        assertIterableEquals(List.of(5201, 1661, 3706, 200),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(3, page.pageRequest().page());

        page = businesses.findByZipIn(zipCodes, page.nextPageRequest());

        assertIterableEquals(List.of(1402, 3008),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(2, page.numberOfElements());
        assertEquals(4, page.pageRequest().page());
        assertEquals(null, page.nextPageRequest());

        page = businesses.findByZipIn(zipCodes, page.previousPageRequest());

        assertIterableEquals(List.of(5201, 1661, 3706, 200),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(3, page.pageRequest().page());
    }

    /**
     * Repository method that queries and sorts by an embeddable attribute
     * of an embeddable attribute of an embeddable attribute of an entity.
     */
    @Test
    public void testEmbeddableDepth3() {
        Business[] found = businesses.findByLocation_Address_Street_NameIgnoreCaseEndsWithOrderByLocation_Address_Street_DirectionIgnoreCaseAscNameAsc(" AVE");

        assertIterableEquals(List.of("Silver Lake Foods", "Crenlo", "Geotek"),
                             Stream.of(found)
                                             .map(b -> b.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Intermix different name patterns for embeddable attributes.
     */
    @Test
    public void testEmbeddableIntermixNamePatterns() {
        assertIterableEquals(List.of("HALCON", "Geotek"),
                             businesses.in("Stewartville", "MN")
                                             .map(b -> b.name)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("Custom Alarm", "Mayo Clinic", "Olmsted Medical", "Reichel Foods"),
                             businesses.onSouthSideOf("Rochester", "MN", "s"));
    }

    /**
     * Repository method where the result type is the embeddable class of one of the entity attributes.
     */
    @Test
    public void testEmbeddableTypeAsResult() {
        assertIterableEquals(List.of("NW 19th St",
                                     "NW 37th St",
                                     "NW 4th Ave",
                                     "NW Civic Center Dr",
                                     "NW Lakeridge Pl",
                                     "NW Members Parkway",
                                     "W Highway 14"),
                             businesses.findByZip(55901)
                                             .map(loc -> loc.address.street.direction + " " + loc.address.street.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method where the result type is an embeddable class 3 levels deep on the entity.
     */
    @Test
    public void testEmbeddableTypeAsResultDepth3() {
        assertIterableEquals(List.of("N Broadway Ave",
                                     "NE Wellner Dr",
                                     "SE 9th St",
                                     "SW 1st St",
                                     "SW Enterprise Dr",
                                     "SW Greenview Dr"),
                             businesses.findByZipNotAndCity(55901, "Rochester")
                                             .map(street -> street.direction + " " + street.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository methods for an entity where an embeddable is the id.
     */
    @Test
    public void testEmbeddedId() {
        // Clear out data before test
        accounts.deleteByOwnerEndsWith("TestEmbeddedId");

        accounts.create(new Account(1005380, 70081, "Think Bank", true, 552.18, "Ellen TestEmbeddedId"));
        accounts.create(new Account(1004470, 70081, "Think Bank", true, 443.94, "Erin TestEmbeddedId"));
        accounts.create(new Account(1006380, 70081, "Think Bank", true, 160.63, "Edward TestEmbeddedId"));
        accounts.create(new Account(1007590, 70081, "Think Bank", true, 793.30, "Elizabeth TestEmbeddedId"));
        accounts.save(new Account(1008410, 22158, "Home Federal Savings Bank", true, 829.91, "Elizabeth TestEmbeddedId"));
        accounts.save(new Account(1006380, 22158, "Home Federal Savings Bank", true, 261.66, "Elliot TestEmbeddedId"));
        accounts.save(new Account(1004470, 22158, "Home Federal Savings Bank", false, 416.14, "Emma TestEmbeddedId"));
        accounts.createAll(new Account(1009130, 30372, "Mayo Credit Union", true, 945.20, "Elizabeth TestEmbeddedId"),
                           new Account(1004470, 30372, "Mayo Credit Union", true, 423.15, "Eric TestEmbeddedId"),
                           new Account(1008200, 30372, "Mayo Credit Union", true, 103.04, "Evan TestEmbeddedId"));

        assertIterableEquals(List.of("Emma TestEmbeddedId", "Eric TestEmbeddedId", "Erin TestEmbeddedId"),
                             accounts.findByAccountIdAccountNum(1004470)
                                             .map(a -> a.owner)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("Edward TestEmbeddedId", "Elizabeth TestEmbeddedId", "Ellen TestEmbeddedId", "Erin TestEmbeddedId"),
                             accounts.findByAccountIdRoutingNum(70081)
                                             .map(a -> a.owner)
                                             .collect(Collectors.toList()));

        assertEquals("Emma TestEmbeddedId", accounts.findByAccountId(AccountId.of(1004470, 22158)).owner);

        assertEquals("Erin TestEmbeddedId", accounts.findById(AccountId.of(1004470, 70081)).owner);

        assertIterableEquals(List.of("Home Federal Savings Bank", "Mayo Credit Union"),
                             accounts.findByAccountIdNotAndOwner(AccountId.of(1007590, 70081), "Elizabeth TestEmbeddedId")
                                             .map(a -> a.bankName)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("AccountId:1004470:22158",
                                     "AccountId:1004470:30372",
                                     "AccountId:1004470:70081",
                                     "AccountId:1005380:70081",
                                     "AccountId:1006380:22158",
                                     "AccountId:1006380:70081",
                                     "AccountId:1007590:70081",
                                     "AccountId:1008200:30372",
                                     "AccountId:1008410:22158",
                                     "AccountId:1009130:30372"),
                             accounts.findByIdNotNull()
                                             .map(a -> a.accountId.toString())
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("AccountId:1004470:70081",
                                     "AccountId:1005380:70081",
                                     "AccountId:1006380:70081",
                                     "AccountId:1007590:70081"),
                             accounts.findByBankName("Think Bank")
                                             .map(AccountId::toString)
                                             .collect(Collectors.toList()));

        assertEquals(Collections.EMPTY_LIST, accounts.findByIdEmpty());

        try {
            System.out.println("findByIdBetween: " + accounts.findByIdBetween(AccountId.of(1006380, 22158), AccountId.of(1008200, 30372)));
        } catch (MappingException x) {
            // expected
        }

        try {
            System.out.println("findByIdGreaterThan: " + accounts.findByIdGreaterThan(AccountId.of(1008200, 30372)));
        } catch (MappingException x) {
            // expected
        }

        // Varies by database whether MappingException or DatabaseException is raised,
        // System.out.println("findByIdInOrOwner: " + accounts.findByIdInOrOwner(List.of(AccountId.of(1004470, 30372),
        //                                                                               AccountId.of(1006380, 22158)),
        //                                                                       "Emma TestEmbeddedId"));

        try {
            System.out.println("findByIdTrue: " + accounts.findByIdTrue());
        } catch (MappingException x) {
            // expected
        }

        accounts.remove(new Account(1005380, 70081, "Think Bank", true, 552.18, "Ellen TestEmbeddedId"));

        // JPQL with "IN", which this needs, is not supported by EclipseLink for embeddables
        // accounts.deleteAll(List.of(new Account(1004470, 70081, "Think Bank", true, 443.94, "Erin TestEmbeddedId")));

        accounts.deleteByOwnerEndsWith("TestEmbeddedId");
    }

    /**
     * Tests CrudRepository methods that supply entities as parameters.
     */
    @Test
    public void testEntitiesAsParameters() throws Exception {
        orders.deleteAll();

        PurchaseOrder o1 = new PurchaseOrder();
        o1.purchasedBy = "testEntitiesAsParameters-Customer1";
        o1.purchasedOn = OffsetDateTime.now();
        o1.total = 10.99f;

        PurchaseOrder o2 = new PurchaseOrder();
        o2.purchasedBy = "testEntitiesAsParameters-Customer2";
        o2.purchasedOn = OffsetDateTime.now();
        o2.total = 20.99f;

        PurchaseOrder[] created = orders.create(o1, o2);
        o1 = created[0];
        o2 = created[1];
        int o1_v1 = o1.versionNum;

        PurchaseOrder o3 = new PurchaseOrder();
        o3.purchasedBy = "testEntitiesAsParameters-Customer3";
        o3.purchasedOn = OffsetDateTime.now();
        o3.total = 30.99f;
        orders.insert(o3);
        o3 = orders.findFirstByPurchasedBy("testEntitiesAsParameters-Customer3").orElseThrow();

        PurchaseOrder o4 = new PurchaseOrder();
        o4.purchasedBy = "testEntitiesAsParameters-Customer4";
        o4.purchasedOn = OffsetDateTime.now();
        o4.total = 40.99f;
        o4 = orders.create(o4);

        PurchaseOrder o5 = new PurchaseOrder();
        o5.purchasedBy = "testEntitiesAsParameters-Customer5";
        o5.purchasedOn = OffsetDateTime.now();
        o5.total = 50.99f;
        o5 = orders.create(o5);
        int o5_v1 = o5.versionNum;

        // delete even though a property doesn't match
        o4.total = 44.99f;
        orders.delete(o4);

        // cannot delete when the version number doesn't match
        tran.begin();
        try {
            o1 = orders.findById(o1.id).orElseThrow();
            Long o1id = o1.id;

            // Update in separate transaction:
            CompletableFuture.supplyAsync(() -> {
                PurchaseOrder o1updated = orders.findById(o1id).orElseThrow();
                o1updated.total = 11.99f;
                return orders.save(o1updated);
            }).get(30, TimeUnit.SECONDS);

            try {
                orders.delete(o1);
                fail("Deletion must be rejected when the version doesn't match.");
            } catch (OptimisticLockingFailureException x) {
                System.out.println("Deletion was rejected as it ought to be when the version does not match.");
            }

            assertEquals(Status.STATUS_MARKED_ROLLBACK, tran.getStatus());
        } finally {
            tran.rollback();
        }

        PurchaseOrder o2old = new PurchaseOrder();
        o2old.id = o2.id;
        o2old.purchasedBy = o2.purchasedBy;
        o2old.purchasedOn = o2.purchasedOn;
        o2old.total = o2.total;
        o2old.versionNum = o2.versionNum;

        // increment version of second entity
        o2.total = 22.99f;
        o2 = orders.save(o2);

        // attempt to save second entity at an old version
        o2old.total = 99.22f;
        try {
            PurchaseOrder unexpected = orders.save(o2old);
            fail("Should not be able to update old version of entity: " + unexpected);
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        // attempt to save second entity at an old version in combination with addition of another entity
        PurchaseOrder o6 = new PurchaseOrder();
        o6.purchasedBy = "testEntitiesAsParameters-Customer6";
        o6.purchasedOn = OffsetDateTime.now();
        o6.total = 60.99f;
        try {
            Iterable<PurchaseOrder> unexpected = orders.saveAll(List.of(o6, o2old));
            fail("Should not be able to update old version of entity: " + unexpected);
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        // verify that the second entity remains at its second version (22.99) and that the addition of the sixth entity was rolled back
        List<Float> orderTotals = orders.findTotalByPurchasedByIn(List.of("testEntitiesAsParameters-Customer2",
                                                                          "testEntitiesAsParameters-Customer6"));
        assertEquals(orderTotals.toString(), 1, orderTotals.size());
        assertEquals(22.99f, orderTotals.get(0), 0.001f);

        orders.deleteAll(List.of(o3, o2));

        Map<String, PurchaseOrder> map = orders.findAll()
                        .collect(Collectors.toMap(o -> o.purchasedBy, // key
                                                  o -> o)); // value

        assertEquals(map.toString(), 2, map.size());

        PurchaseOrder o;
        assertNotNull(o = map.get("testEntitiesAsParameters-Customer1"));
        assertEquals(11.99f, o.total, 0.001f);
        assertEquals(o1_v1 + 1, o.versionNum); // updated once

        assertNotNull(o = map.get("testEntitiesAsParameters-Customer5"));
        assertEquals(50.99f, o.total, 0.001f);
        assertEquals(o5_v1, o.versionNum); // never updated

        PurchaseOrder o7 = new PurchaseOrder();
        o7.purchasedBy = "testEntitiesAsParameters-Customer7";
        o7.purchasedOn = OffsetDateTime.now();
        o7.total = 70.99f;

        try {
            orders.insertAll(List.of(o7, o5));
            fail("Should not be able insert an entity with an Id that is already present.");
        } catch (EntityExistsException x) {
            // expected
        }

        assertEquals(false, orders.findFirstByPurchasedBy("testEntitiesAsParameters-Customer7").isPresent());

        PurchaseOrder o8 = new PurchaseOrder();
        o8.purchasedBy = "testEntitiesAsParameters-Customer8";
        o8.purchasedOn = OffsetDateTime.now();
        o8.total = 80.99f;

        orders.insertAll(Set.of(o7, o8));

        o7 = orders.findFirstByPurchasedBy("testEntitiesAsParameters-Customer7").orElseThrow();
        o8 = orders.findFirstByPurchasedBy("testEntitiesAsParameters-Customer8").orElseThrow();

        o7.total = 77.99f;
        o8.total = 88.99f;
        o1.total = 1.99f;
        o1.versionNum = o1_v1;

        try {
            orders.updateAll(List.of(o8, o1, o7));
            fail("Attempt to update multiple entities where one has an outdated version must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        orders.updateAll(List.of(o8, o7));

        List<Float> totals = orders.findTotalByPurchasedByIn(Set.of("testEntitiesAsParameters-Customer8",
                                                                    "testEntitiesAsParameters-Customer7",
                                                                    "testEntitiesAsParameters-Customer1"),
                                                             Sort.desc("total"));
        assertEquals(totals.toString(), 3, totals.size());
        assertEquals(88.99f, totals.get(0), 0.001f);
        assertEquals(77.99f, totals.get(1), 0.001f);
        assertEquals(11.99f, totals.get(2), 0.001f); // not updated due to version mismatch

        try {
            orders.update(o1);
            fail("Attempt to update an outdated version of an entity must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        assertEquals(11.99f, totals.get(2), 0.001f); // still not updated due to version mismatch

        // use correct version for update:
        o1 = orders.findFirstByPurchasedBy("testEntitiesAsParameters-Customer1").orElseThrow();
        o1.total = 0.99f;

        orders.update(o1);

        totals = orders.findTotalByPurchasedByIn(Set.of("testEntitiesAsParameters-Customer1"));
        assertEquals(totals.toString(), 1, totals.size());
        assertEquals(0.99f, totals.get(0), 0.001f);

        orders.deleteAll();
    }

    /**
     * Verify WithYear, WithQuarter, WithMonth, and WithDay Functions to compare different parts of a date.
     */
    @Test
    public void testExtractFromDateFunctions() {
        // WithYear
        assertEquals(List.of(4000921041110001L, 4000921042220002L),
                     creditCards.expiringInOrBefore(2024));

        // WithQuarter
        assertEquals(List.of(1000921011110001L, 1000921011120002L, 1000921011130003L,
                             2000921021110001L, 2000921022220002L,
                             3000921031110001L, 3000921032220002L, 3000921033330003L),
                     creditCards.expiresInQuarterOtherThan(2)
                                     .map(cc -> cc.number)
                                     .collect(Collectors.toList()));

        // WithMonth
        assertEquals(List.of(1000921011110001L, 1000921011120002L, 1000921011130003L, 4000921041110001L, 4000921042220002L),
                     creditCards.issuedInMonth(List.of(Month.APRIL.getValue(), Month.AUGUST.getValue(), Month.JANUARY.getValue()))
                                     .map(cc -> cc.number)
                                     .collect(Collectors.toList()));

        // WithDay
        assertEquals(List.of(1000921011110001L, 2000921021110001L, 3000921031110001L, 4000921041110001L, 5000921051110001L, 6000921061110001L),
                     creditCards.issuedBetween(5, 15)
                                     .map(cc -> cc.number)
                                     .collect(Collectors.toList()));
    }

    /**
     * Verify WithYear, WithQuarter, WithMonth, and WithDay in query-by-method-name to compare different parts of a date.
     */
    @Test
    public void testExtractFromDateKeywords() {
        // WithYear
        assertEquals(List.of(1000921011110001L, 1000921011120002L, 1000921011130003L, 4000921041110001L, 4000921042220002L, 5000921051110001L, 5000921052220002L),
                     creditCards.findNumberByExpiresOnWithYearLessThanEqual(2025));

        // WithQuarter
        assertEquals(List.of(4000921041110001L, 4000921042220002L, 5000921051110001L, 5000921052220002L, 6000921061110001L, 6000921062220002L),
                     creditCards.findByExpiresOnWithQuarterNot(1)
                                     .map(cc -> cc.number)
                                     .collect(Collectors.toList()));

        // WithMonth
        assertEquals(List.of(2000921021110001L, 2000921022220002L, 5000921051110001L, 5000921052220002L),
                     creditCards.findByIssuedOnWithMonthIn(List.of(Month.FEBRUARY.getValue(), Month.MAY.getValue(), Month.SEPTEMBER.getValue()))
                                     .map(cc -> cc.number)
                                     .collect(Collectors.toList()));

        // WithDay
        assertEquals(List.of(1000921011120002L, 2000921022220002L, 3000921032220002L, 4000921042220002L, 5000921052220002L, 6000921062220002L),
                     creditCards.findByIssuedOnWithDayBetween(20, 29)
                                     .map(cc -> cc.number)
                                     .collect(Collectors.toList()));
    }

    /**
     * Verify WithWeek Function to compare the week-of-year part of a date.
     */
    @SkipIfDataSourceProperties(DERBY_EMBEDDED) // Derby doesn't support a WEEK function in SQL
    @Test
    public void testExtractWeekFromDateFunction() {

        // WithWeek
        assertEquals(List.of(4000921041110001L),
                     creditCards.expiringInWeek(15));
    }

    /**
     * Verify WithWeek in query-by-method-name to compare the week-of-year part of a date.
     */
    @SkipIfDataSourceProperties(DERBY_EMBEDDED) // Derby doesn't support a WEEK function in SQL
    @Test
    public void testExtractWeekFromDateKeyword() {

        // WithWeek
        assertEquals(List.of(4000921042220002L),
                     creditCards.findByExpiresOnWithWeek(17));
    }

    /**
     * Avoid specifying a primary key value and let it be generated.
     */
    @Test
    public void testGeneratedKey() {
        ZoneOffset MDT = ZoneOffset.ofHours(-6);

        PurchaseOrder o1 = new PurchaseOrder();
        o1.purchasedBy = "testGeneratedKey-Customer1";
        o1.purchasedOn = OffsetDateTime.of(2022, 6, 1, 9, 30, 0, 0, MDT);
        o1.total = 25.99f;
        o1 = orders.save(o1);

        PurchaseOrder o2 = new PurchaseOrder();
        o2.purchasedBy = "testGeneratedKey-Customer2";
        o2.purchasedOn = OffsetDateTime.of(2022, 6, 1, 14, 0, 0, 0, MDT);
        o2.total = 148.98f;
        o2 = orders.save(o2);

        assertNotNull(o1.id);
        assertNotNull(o2.id);
        assertEquals(false, o1.id.equals(o2.id));

        assertEquals(true, orders.addTaxAndShipping(o2.id, 1.08f, 7.99f));

        o2 = orders.findById(o2.id).get();

        assertEquals(168.89f, o2.total, 0.01f);
        assertEquals(o2.purchasedOn, OffsetDateTime.of(2022, 6, 1, 14, 0, 0, 0, MDT));
    }

    /**
     * Use the IdClass annotation to define a composite id.
     */
    @Test
    public void testIdClass() {
        assertIterableEquals(List.of("Minnesota", "New York"),
                             cities.findByName("Rochester")
                                             .map(c -> c.stateName)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("Kansas City", "Springfield"),
                             cities.findByStateName("Missouri")
                                             .map(c -> c.name)
                                             .collect(Collectors.toList()));

        // TODO JPA doesn't allow querying by IdClass. This would need to be interpreted as (c.name=?1 AND c.state=?2)
        // The current error is confusing: You have attempted to set a value of type class test.jakarta.data.jpa.web.CityId
        // for parameter 1 with expected type of class java.lang.String from query string SELECT o FROM City o WHERE (o.state=?1)
        //cities.findById(CityId.of("Rochester", "Minnesota"));
    }

    /**
     * Repository method with the Count keyword that counts how many matching entities there are.
     */
    @Test
    public void testIdClassCountKeyword() {
        assertEquals(2L, cities.countByStateNameAndIdNotOrIdNotAndName("Missouri", CityId.of("Kansas City", "Missouri"),
                                                                       CityId.of("Rochester", "New York"), "Rochester"));
    }

    /**
     * Use CrudRepository-style delete(entity) operation where entity has a composite ID that is defined by IdClass.
     */
    @Test
    public void testIdClassDelete() {
        City winona = new City("Winona", "Minnesota", 25948, Set.of(507));
        winona = cities.save(winona); // must use updated copy of entity now that we have added a version to it
        cities.delete(winona);
        assertEquals(true, cities.findById(CityId.of("Winona", "Minnesota")).isEmpty());
    }

    /**
     * Repository method with the Exists annotation that checks if any matching entities exist.
     */
    @Test
    public void testIdClassExistsAnnotation() {
        assertEquals(true, cities.areFoundIn("Minnesota"));
        assertEquals(false, cities.areFoundIn("Antarctica"));
    }

    /**
     * Repository method with the Exists keyword that checks if any matching entities exist.
     */
    @Test
    public void testIdClassExistsKeyword() {
        assertEquals(true, cities.existsByNameAndStateName("Kansas City", "Kansas"));
        assertEquals(false, cities.existsByNameAndStateName("Kansas City", "Minnesota"));

        assertEquals(true, cities.existsById(CityId.of("Kansas City", "Missouri")));
        assertEquals(false, cities.existsById(CityId.of("Kansas City", "Nebraska")));
    }

    /**
     * Repository method performing a parameter-based query on a compound entity Id which is an IdClass,
     * where the method parameter is annotated with By.
     */
    @Test
    public void testIdClassFindByAnnotatedParameter() {

        assertIterableEquals(List.of("Springfield Massachusetts",
                                     "Rochester Minnesota",
                                     "Kansas City Missouri"),
                             cities.largerThan(100000, CityId.of("springfield", "missouri"), "M%s")
                                             .map(c -> c.name + ' ' + c.stateName)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method performing a parameter-based query on one component of the compound entity Id which is an IdClass.
     */
    @Test
    public void testIdClassFindByComponentOfIdClass() {
        assertIterableEquals(List.of("Rochester Minnesota",
                                     "Rochester New York"),
                             cities.withNameOf("Rochester")
                                             .map(c -> c.name + ' ' + c.stateName)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method performing a parameter-based query on a compound entity Id which is an IdClass,
     * without annotating the method parameter.
     */
    @Test
    public void testIdClassFindByParametersUnannotated() {
        assertEquals(true, cities.isBiggerThan(100000, CityId.of("Rochester", "Minnesota")));
        assertEquals(false, cities.isBiggerThan(500000, CityId.of("Rochester", "Minnesota")));
    }

    /**
     * Repository method with the Find keyword that queries based on multiple IdClass parameters.
     */
    @Test
    public void testIdClassFindKeyword() {
        assertIterableEquals(List.of("Kansas City Missouri",
                                     "Rochester Minnesota",
                                     "Springfield Illinois"),
                             cities.findByIdOrIdIgnoreCaseOrId(CityId.of("Rochester", "Minnesota"),
                                                               CityId.of("springfield", "illinois"),
                                                               CityId.of("Kansas City", "Missouri"))
                                             .map(c -> c.name + ' ' + c.stateName)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("Springfield Illinois",
                                     "Springfield Massachusetts",
                                     "Springfield Missouri",
                                     "Springfield Ohio"),
                             cities.findByNameAndIdNot("Springfield", CityId.of("Springfield", "Oregon"))
                                             .map(c -> c.name + ' ' + c.stateName)
                                             .collect(Collectors.toList()));
    }

    /**
     * Use the OrderBy annotation on a composite id that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderByAnnotationReverseDirection() {
        assertIterableEquals(List.of("Springfield Oregon",
                                     "Springfield Ohio",
                                     "Springfield Missouri",
                                     "Springfield Illinois",
                                     "Rochester New York",
                                     "Rochester Minnesota",
                                     "Kansas City Missouri",
                                     "Kansas City Kansas"),
                             cities.findByStateNameNot("Massachusetts")
                                             .map(c -> c.name + ' ' + c.stateName)
                                             .collect(Collectors.toList()));
    }

    /**
     * Use keyset pagination with the OrderBy annotation on a composite id that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderByAnnotationWithKeysetPagination() {
        PageRequest<?> pagination = PageRequest.ofSize(3).afterKeyset(CityId.of("Rochester", "Minnesota"));

        KeysetAwareSlice<City> slice1 = cities.findByStateNameNotEndsWith("o", pagination);
        assertIterableEquals(List.of("Rochester New York",
                                     "Springfield Illinois",
                                     "Springfield Massachusetts"),
                             slice1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwareSlice<City> slice2 = cities.findByStateNameNotEndsWith("o", slice1.nextPageRequest());
        assertIterableEquals(List.of("Springfield Missouri",
                                     "Springfield Oregon"),
                             slice2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, slice2.nextPageRequest());

        KeysetAwareSlice<City> slice0 = cities.findByStateNameNotEndsWith("o", slice1.previousPageRequest());
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota"),
                             slice0.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, slice0.previousPageRequest());
    }

    /**
     * Use keyset pagination with the OrderBy annotation on a composite id that is defined by an IdClass attribute.
     * Also use named parameters, which means the keyset portion of the query will also need to use named parameters.
     */
    @Test
    public void testIdClassOrderByAnnotationWithKeysetPaginationAndNamedParameters() {
        PageRequest<City> pagination = PageRequest.ofSize(2);

        KeysetAwarePage<City> page1 = cities.sizedWithin(100000, 1000000, pagination);
        assertIterableEquals(List.of("Springfield Missouri",
                                     "Springfield Massachusetts"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(4L, page1.totalPages());
        assertEquals(7L, page1.totalElements());

        KeysetAwarePage<City> page2 = cities.sizedWithin(100000, 1000000, page1.nextPageRequest());
        assertIterableEquals(List.of("Springfield Illinois",
                                     "Rochester New York"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page3 = cities.sizedWithin(100000, 1000000, page2.nextPageRequest());
        assertIterableEquals(List.of("Rochester Minnesota",
                                     "Kansas City Missouri"),
                             page3.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page4 = cities.sizedWithin(100000, 1000000, page3.nextPageRequest());
        assertIterableEquals(List.of("Kansas City Kansas"),
                             page4.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, page4.nextPageRequest());
    }

    /**
     * Use keyset pagination with the OrderBy query-by-method pattern on a composite id that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderByNamePatternWithKeysetPagination() {
        PageRequest<City> pagination = PageRequest.ofSize(5);

        KeysetAwareSlice<City> slice1 = cities.findByStateNameNotNullOrderById(pagination);
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota",
                                     "Rochester New York",
                                     "Springfield Illinois"),
                             slice1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwareSlice<City> slice2 = cities.findByStateNameNotNullOrderById(slice1.nextPageRequest());
        assertIterableEquals(List.of("Springfield Massachusetts",
                                     "Springfield Missouri",
                                     "Springfield Ohio",
                                     "Springfield Oregon"),
                             slice2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, slice2.nextPageRequest());

        Cursor springfieldMO = slice2.getKeysetCursor(1);
        pagination = pagination.size(3).beforeKeysetCursor(springfieldMO);

        KeysetAwareSlice<City> beforeSpringfieldMO = cities.findByStateNameNotNullOrderById(pagination);
        assertIterableEquals(List.of("Rochester New York",
                                     "Springfield Illinois",
                                     "Springfield Massachusetts"),
                             beforeSpringfieldMO.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwareSlice<City> beforeRochesterNY = cities.findByStateNameNotNullOrderById(beforeSpringfieldMO.previousPageRequest());
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota"),
                             beforeRochesterNY.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, beforeRochesterNY.previousPageRequest());
    }

    /**
     * Use keyset pagination with the OrderBy query-by-method pattern in descending direction
     * on a composite id that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderByNamePatternWithKeysetPaginationDescending() {
        PageRequest<?> pagination = PageRequest.ofSize(3).afterKeyset(CityId.of("Springfield", "Tennessee"));

        KeysetAwarePage<City> page1 = cities.findByStateNameNotStartsWithOrderByIdDesc("Ma", pagination);
        assertIterableEquals(List.of("Springfield Oregon",
                                     "Springfield Ohio",
                                     "Springfield Missouri"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page2 = cities.findByStateNameNotStartsWithOrderByIdDesc("Ma", page1.nextPageRequest());
        assertIterableEquals(List.of("Springfield Illinois",
                                     "Rochester New York",
                                     "Rochester Minnesota"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page3 = cities.findByStateNameNotStartsWithOrderByIdDesc("Ma", page2.nextPageRequest());
        assertIterableEquals(List.of("Kansas City Missouri",
                                     "Kansas City Kansas"),
                             page3.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, page3.nextPageRequest());

        page2 = cities.findByStateNameNotStartsWithOrderByIdDesc("Ma", page3.previousPageRequest());
        assertIterableEquals(List.of("Springfield Illinois",
                                     "Rochester New York",
                                     "Rochester Minnesota"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));
    }

    /**
     * Use keyset pagination with the pagination sort criteria on a composite id that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderByPaginationWithKeyset() {
        // ascending:
        PageRequest<City> pagination = PageRequest.of(City.class).size(5).sortBy(Sort.asc("id"));

        KeysetAwarePage<City> page1 = cities.findByStateNameGreaterThan("Iowa", pagination);
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota",
                                     "Rochester New York",
                                     "Springfield Massachusetts"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page2 = cities.findByStateNameGreaterThan("Iowa", page1.nextPageRequest());
        assertIterableEquals(List.of("Springfield Missouri",
                                     "Springfield Ohio",
                                     "Springfield Oregon"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, page2.nextPageRequest());

        // descending:
        pagination = PageRequest.of(City.class).size(4).sortBy(Sort.descIgnoreCase("id"));
        page1 = cities.findByStateNameGreaterThan("Idaho", pagination);
        assertIterableEquals(List.of("Springfield Oregon",
                                     "Springfield Ohio",
                                     "Springfield Missouri",
                                     "Springfield Massachusetts"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        page2 = cities.findByStateNameGreaterThan("Idaho", page1.nextPageRequest());
        assertIterableEquals(List.of("Springfield Illinois",
                                     "Rochester New York",
                                     "Rochester Minnesota",
                                     "Kansas City Missouri"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page3 = cities.findByStateNameGreaterThan("Idaho", page2.nextPageRequest());
        assertIterableEquals(List.of("Kansas City Kansas"),
                             page3.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, page3.nextPageRequest());
    }

    /**
     * Sort on a composite id that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderBySorts() {
        assertIterableEquals(List.of("Springfield Missouri",
                                     "Springfield Massachusetts",
                                     "Springfield Illinois",
                                     "Rochester New York",
                                     "Rochester Minnesota",
                                     "Kansas City Missouri",
                                     "Kansas City Kansas"),
                             cities.findByStateNameLessThan("Ohio", Sort.desc("id"))
                                             .map(c -> c.name + ' ' + c.stateName)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method where the result is the entity's IdClass.
     */
    @Test
    public void testIdClassResult() {
        // single result
        CityId cityId = cities.findFirstByNameOrderByPopulationDesc("Springfield");
        assertEquals("Springfield", cityId.name);
        assertEquals("Missouri", cityId.stateName);

        // Stream result
        assertIterableEquals(List.of("Springfield, Oregon",
                                     "Springfield, Ohio",
                                     "Springfield, Missouri",
                                     "Springfield, Massachusetts",
                                     "Springfield, Illinois"),
                             cities.findByNameStartsWith("Spring")
                                             .map(CityId::toString)
                                             .collect(Collectors.toList()));

        // array result
        assertIterableEquals(List.of("Springfield, Illinois",
                                     "Kansas City, Kansas",
                                     "Springfield, Massachusetts"),
                             Stream.of(cities.findByStateNameEndsWith("s"))
                                             .map(CityId::toString)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method with the Assign annotation that makes an update by assigning the IdClass instance to something else.
     */
    @Test
    public void testIdClassUpdateAssignIdClass() {
        cities.save(new City("La Crosse", "Wisconsin", 52680, Set.of(608)));
        try {
            cities.findById(CityId.of("La Crosse", "Wisconsin")).orElseThrow();

            assertEquals(1, cities.replace(CityId.of("La Crosse", "Wisconsin"),
                                           "Decorah", "Iowa", 7587, Set.of(563))); // TODO CityId.of("Decorah", "Iowa"), 7587, Set.of(563)));

            assertEquals(true, cities.findById(CityId.of("La Crosse", "Wisconsin")).isEmpty());
            assertEquals(true, cities.existsById(CityId.of("Decorah", "Iowa")));

            // TODO EclipseLink bug needs to be fixed:
            // java.lang.IllegalArgumentException: Can not set java.util.Set field test.jakarta.data.jpa.web.City.areaCodes to java.lang.Integer
            //City city = cities.findById(CityId.of("Decorah", "Iowa")).orElseThrow();
            //assertEquals("Decorah", city.name);
            //assertEquals("Iowa", city.stateName);
            //assertEquals(7587, city.population);
            //assertEquals(Set.of(563), city.areaCodes);
        } finally {
            cities.deleteByIdOrId(CityId.of("La Crosse", "Wisconsin"), CityId.of("Decorah", "Iowa"));
        }
    }

    /**
     * Repository method with the Update annotation that makes an update by assigning the IdClass components to something else.
     */
    @Test
    public void testIdClassUpdateAssignIdClassComponents() {
        cities.save(new City("Janesville", "Wisconsin", 65615, Set.of(608)));
        try {
            cities.findById(CityId.of("Janesville", "Wisconsin")).orElseThrow();

            assertEquals(1, cities.replace("Janesville", "Wisconsin",
                                           "Ames", "Iowa", Set.of(515), 66427));

            assertEquals(true, cities.findById(CityId.of("Janesville", "Wisconsin")).isEmpty());
            assertEquals(true, cities.existsById(CityId.of("Ames", "Iowa")));

            // TODO EclipseLink bug needs to be fixed:
            // java.lang.IllegalArgumentException: Can not set java.util.Set field test.jakarta.data.jpa.web.City.areaCodes to java.lang.Integer
            //City city = cities.findById(CityId.of("Decorah", "Iowa")).orElseThrow();
            //assertEquals("Ames", city.name);
            //assertEquals("Iowa", city.stateName);
            //assertEquals(66427, city.population);
            //assertEquals(Set.of(515), city.areaCodes);
        } finally {
            cities.deleteByIdOrId(CityId.of("Janesville", "Wisconsin"), CityId.of("Ames", "Iowa"));
        }
    }

    /**
     * Repository method with the Update keyword that makes an update by assigning the IdClass instance to something else.
     */
    @Test
    public void testIdClassUpdateKeyword() {
        cities.save(new City("Madison", "Wisconsin", 269840, Set.of(608)));
        try {
            cities.findById(CityId.of("Madison", "Wisconsin")).orElseThrow();

            assertEquals(1, cities.updateByIdAndPopulationSetIdSetPopulationSetAreaCodes(CityId.of("Madison", "Wisconsin"), 269840,
                                                                                         CityId.of("Des Moines", "Iowa"), 214133, Set.of(515)));

            assertEquals(true, cities.findById(CityId.of("Madison", "Wisconsin")).isEmpty());
            assertEquals(true, cities.existsById(CityId.of("Des Moines", "Iowa")));

            // TODO EclipseLink bug needs to be fixed:
            // java.lang.IllegalArgumentException: Can not set java.util.Set field test.jakarta.data.jpa.web.City.areaCodes to java.lang.Integer
            //City city = cities.findById(CityId.of("Des Moines", "Iowa")).orElseThrow();
            //assertEquals("Des Moines", city.name);
            //assertEquals("Iowa", city.stateName);
            //assertEquals(214133, city.population);
            //assertEquals(Set.of(515), city.areaCodes);
        } finally {
            cities.deleteByIdOrId(CityId.of("Madison", "Wisconsin"), CityId.of("Des Moines", "Iowa"));
        }
    }

    /**
     * Repository methods for an entity where the id is on the embeddable.
     * EclipseLink allows this but it is not part of the JPA spec.
     */
    @Test
    public void testIdOnEmbeddable() {
        // Clear out data before test
        employees.deleteByLastName("TestIdOnEmbeddable");

        Streamable<Employee> added = businesses.save(new Employee("Irene", "TestIdOnEmbeddable", (short) 2636, 'A'),
                                                     new Employee("Isabella", "TestIdOnEmbeddable", (short) 8171, 'B'),
                                                     new Employee("Ivan", "TestIdOnEmbeddable", (short) 4948, 'A'),
                                                     new Employee("Isaac", "TestIdOnEmbeddable", (short) 5310, 'C'));

        assertEquals(List.of("Irene", "Isabella", "Ivan", "Isaac"),
                     added.stream().map(e -> e.firstName).collect(Collectors.toList()));

        Employee emp4948 = employees.findById(4948);
        assertEquals("Ivan", emp4948.firstName);
        assertEquals("TestIdOnEmbeddable", emp4948.lastName);
        assertEquals((short) 4948, emp4948.badge.number);
        assertEquals('A', emp4948.badge.accessLevel);

        assertEquals("Irene", employees.findByBadgeNumber(2636).firstName);

        assertIterableEquals(List.of((short) 4948, (short) 5310, (short) 8171),
                             employees.findByFirstNameLike("I_a%")
                                             .map(emp -> emp.badge.number)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of((short) 8171, (short) 5310, (short) 4948, (short) 2636),
                             employees.findByFirstNameStartsWithOrderByIdDesc("I")
                                             .stream()
                                             .map(emp -> emp.badge.number)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("Badge#2636 Level A", "Badge#4948 Level A", "Badge#5310 Level C", "Badge#8171 Level B"),
                             employees.findByLastName("TestIdOnEmbeddable")
                                             .map(Badge::toString)
                                             .collect(Collectors.toList()));

        employees.deleteByLastName("TestIdOnEmbeddable");
    }

    /**
     * Use an entity that inherits from another where both are kept in the same table.
     */
    @Test
    public void testInheritance() {
        shippingAddresses.removeAll();

        ShippingAddress home = new ShippingAddress();
        home.id = 10L;
        home.city = "Rochester";
        home.state = "Minnesota";
        home.streetAddress = new StreetAddress(1234, "5th St SW");
        home.zipCode = 55902;

        WorkAddress work = new WorkAddress();
        work.id = 20L;
        work.city = "Rochester";
        work.floorNumber = 2;
        work.office = "H115";
        work.state = "Minnesota";
        work.streetAddress = new StreetAddress(2800, "37th St NW");
        work.zipCode = 55901;

        shippingAddresses.save(home);
        shippingAddresses.save(work);

        WorkAddress a = shippingAddresses.forOffice("H115");
        assertEquals(Long.valueOf(20), a.id);
        assertEquals("Rochester", a.city);
        assertEquals(2, a.floorNumber);
        assertEquals("H115", a.office);
        assertEquals("Minnesota", a.state);
        assertEquals("37th St NW", a.streetAddress.streetName);
        assertEquals(55901, a.zipCode);

        WorkAddress[] secondFloorOfficesOn37th = shippingAddresses.findByStreetNameAndFloorNumber("37th St NW", 2);

        assertArrayEquals(new WorkAddress[] { work }, secondFloorOfficesOn37th,
                          Comparator.<WorkAddress, Long> comparing(o -> o.id)
                                          .thenComparing(Comparator.<WorkAddress, String> comparing(o -> o.city))
                                          .thenComparing(Comparator.<WorkAddress, Integer> comparing(o -> o.floorNumber))
                                          .thenComparing(Comparator.<WorkAddress, String> comparing(o -> o.office))
                                          .thenComparing(Comparator.<WorkAddress, String> comparing(o -> o.state))
                                          .thenComparing(Comparator.<WorkAddress, String> comparing(o -> o.streetAddress.streetName))
                                          .thenComparing(Comparator.<WorkAddress, Integer> comparing(o -> o.streetAddress.houseNumber))
                                          .thenComparing(Comparator.<WorkAddress, Integer> comparing(o -> o.zipCode)));

        ShippingAddress[] found = shippingAddresses.findByStreetNameOrderByHouseNumber("37th St NW");

        assertArrayEquals(new ShippingAddress[] { work }, found,
                          Comparator.<ShippingAddress, Long> comparing(o -> o.id)
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.city))
                                          .thenComparing(Comparator.<ShippingAddress, Integer> comparing(o -> ((WorkAddress) o).floorNumber))
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> ((WorkAddress) o).office))
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.state))
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.streetAddress.streetName))
                                          .thenComparing(Comparator.<ShippingAddress, Integer> comparing(o -> o.streetAddress.houseNumber))
                                          .thenComparing(Comparator.<ShippingAddress, Integer> comparing(o -> o.zipCode)));

        StreetAddress[] streetAddresses = shippingAddresses.findByHouseNumberBetweenOrderByStreetNameAscHouseNumber(1000, 3000);

        assertArrayEquals(new StreetAddress[] { work.streetAddress, home.streetAddress }, streetAddresses,
                          Comparator.<StreetAddress, Integer> comparing(o -> o.houseNumber)
                                          .thenComparing(Comparator.<StreetAddress, String> comparing(o -> o.streetName)));

        shippingAddresses.removeAll();
    }

    /**
     * Use a custom join query so that a ManyToMany association can query by attributes of the many side of the relationship.
     */
    @Test
    public void testManyToManyCustomJoinQuery() {

        assertIterableEquals(List.of("4th Ave SE",
                                     "4th Ave SE",
                                     "4th Ave SE",
                                     "2nd Ave NE",
                                     "2nd Ave NE",
                                     "1st Ave SW"),
                             customers.withLocationType(DeliveryLocation.Type.HOME)
                                             .map(Street::toString)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("37th St NW",
                                     "37th St NW",
                                     "37th St NW",
                                     "37th St NW"),
                             customers.withLocationType(DeliveryLocation.Type.BUSINESS)
                                             .map(Street::toString)
                                             .collect(Collectors.toList()));
    }

    /**
     * Query that matches multiple entities and returns a combined collection of results across matches for the ManyToMany association.
     */
    //@Test
    // This test is currently incorrect because Phone is an attribute of Customer (primary entity type), not DeliveryLocation (result type).
    // This would require a way to indicate that a projection is desired, meaning the return type indicates
    // an attribute of the entity rather than the entity class itself.
    // TODO Could this be achieved with @Select?
    public void testManyToManyReturnsCombinedCollectionFromMany() {

        List<String> addresses = customers.findLocationsByPhoneIn(List.of(5075552444L, 5075550101L))
                        .map(loc -> loc.houseNum + " " + loc.street.toString())
                        .collect(Collectors.toList());

        // Customer 1's delivery addresses must come before Customer 4's card numbers due to the ordering on Customer.email.
        assertEquals(addresses.toString(),
                     true, List.of("1001 1st Ave SW", "2800 37th St NW", "4004 4th Ave SE").equals(addresses) ||
                           List.of("1001 1st Ave SW", "4004 4th Ave SE", "2800 37th St NW").equals(addresses));
    }

    /**
     * Query that matches a single entity and returns the corresponding collection from its ManyToMany association.
     */
    //@Test
    // This test is currently incorrect because Email is an attribute of Customer (primary entity type), not DeliveryLocation (result type)
    // This would require a way to indicate that a projection is desired, meaning the return type indicates
    // an attribute of the entity rather than the entity class itself.
    // TODO Could this be achieved with @Select?
    public void testManyToManyReturnsOneSetOfMany() {
        Set<DeliveryLocation> locations = customers.findLocationsByEmail("Maximilian@tests.openliberty.io");

        assertEquals(locations.toString(), 2, locations.size());

        DeliveryLocation loc3 = null;
        DeliveryLocation loc4 = null;
        for (DeliveryLocation loc : locations) {
            if (loc.locationId == 30003L)
                loc3 = loc;
            else if (loc.locationId == 40004L)
                loc4 = loc;
            else
                fail("Unexpected delivery location: " + loc);
        }

        assertNotNull(locations.toString(), loc3);
        assertEquals(DeliveryLocation.Type.BUSINESS, loc3.type);
        assertNotNull(loc3.street);
        assertEquals(2800, loc3.houseNum);
        assertEquals("37th St", loc3.street.name);
        assertEquals("NW", loc3.street.direction);

        assertNotNull(locations.toString(), loc4);
        assertEquals(DeliveryLocation.Type.HOME, loc4.type);
        assertNotNull(loc4.street);
        assertEquals(4004, loc4.houseNum);
        assertEquals("4th Ave", loc4.street.name);
        assertEquals("SE", loc4.street.direction);
    }

    /**
     * Many-to-one entity mapping, where ordering is done based on a composite IdClass.
     */
    @Test
    public void testManyToOneIdClass() {
        assertIterableEquals(List.of("Discrooger card #2000921022220002",
                                     "MonsterCard card #3000921032220002",
                                     "Feesa card #4000921042220002",
                                     "Feesa card #6000921062220002"),
                             creditCards.findBySecurityCode(222)
                                             .map(CardId::toString)
                                             .collect(Collectors.toList()));

    }

    /**
     * Many-to-one entity mapping, where a repository query from the many side
     * filters on and orders by an attribute on the one side,
     * returning results from the many side.
     */
    @Test
    public void testManyToOneM11M() {
        assertIterableEquals(List.of(5000921051110001L, 5000921052220002L,
                                     1000921011110001L, 1000921011120002L, 1000921011130003L,
                                     2000921021110001L, 2000921022220002L),
                             creditCards.findByDebtorEmailIgnoreCaseStartsWith("ma")
                                             .map(card -> card.number)
                                             .collect(Collectors.toList()));
    }

    /**
     * Many-to-one entity mapping, where a repository query from the many side
     * filters on an attribute from the many side,
     * orders by an attribute on the one side,
     * returning results from the one side.
     */
    //@Test
    // This test is currently incorrect because Issuer is an attribute of CreditCard (primary entity type), not Customer (result type).
    // This would require a way to indicate that a projection is desired, meaning the return type indicates
    // an attribute of the entity rather than the entity class itself.
    // TODO Could this be achieved with @Select?
    public void testManyToOneMM11() {
        assertIterableEquals(List.of("MICHELLE@TESTS.OPENLIBERTY.IO",
                                     "Matthew@tests.openliberty.io",
                                     "Maximilian@tests.openliberty.io",
                                     "Megan@tests.openliberty.io"),
                             creditCards.findByIssuer(Issuer.MonsterCard)
                                             .map(c -> c.email)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("MICHELLE@TESTS.OPENLIBERTY.IO",
                                     "Matthew@tests.openliberty.io",
                                     "Megan@tests.openliberty.io",
                                     "Monica@tests.openliberty.io"),
                             creditCards.findByIssuer(Issuer.Feesa)
                                             .map(c -> c.email)
                                             .collect(Collectors.toList()));
    }

    /**
     * Many-to-one entity mapping, where a repository query from the many side
     * filters on an attribute from the many side,
     * orders by an attribute on the one side,
     * returning distinct results from the one side.
     */
    @Test
    public void testManyToOneMM11D() {
        assertIterableEquals(List.of("Monica@tests.openliberty.io",
                                     "martin@tests.openliberty.io"),
                             creditCards.findByExpiresOnBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
    }

    /**
     * Add, find, and remove entities with a mapped superclass.
     * Also tests automatically paginated iterator and list.
     */
    @Test
    public void testMappedSuperclass() {
        tariffs.deleteByLeviedBy("USA");

        Tariff t1 = new Tariff();
        t1.leviedAgainst = "China";
        t1.leviedBy = "USA";
        t1.leviedOn = "Solar Panels";
        t1.rate = 0.15f;
        tariffs.save(t1);

        Tariff t2 = new Tariff();
        t2.leviedAgainst = "Germany";
        t2.leviedBy = "USA";
        t2.leviedOn = "Steel";
        t2.rate = 0.25f;
        tariffs.save(t2);

        Tariff t3 = new Tariff();
        t3.leviedAgainst = "India";
        t3.leviedBy = "USA";
        t3.leviedOn = "Aluminum";
        t3.rate = 0.1f;
        tariffs.save(t3);

        Tariff t4 = new Tariff();
        t4.leviedAgainst = "Japan";
        t4.leviedBy = "USA";
        t4.leviedOn = "Cars";
        t4.rate = 0.025f;
        tariffs.save(t4);

        Tariff t5 = new Tariff();
        t5.leviedAgainst = "Canada";
        t5.leviedBy = "USA";
        t5.leviedOn = "Lumber";
        t5.rate = 0.1799f;
        tariffs.save(t5);

        Tariff t6 = new Tariff();
        t6.leviedAgainst = "Bangladesh";
        t6.leviedBy = "USA";
        t6.leviedOn = "Textiles";
        t6.rate = 0.158f;
        tariffs.save(t6);

        Tariff t7 = new Tariff();
        t7.leviedAgainst = "Mexico";
        t7.leviedBy = "USA";
        t7.leviedOn = "Trucks";
        t7.rate = 0.25f;
        tariffs.save(t7);

        Tariff t8 = new Tariff();
        t8.leviedAgainst = "Canada";
        t8.leviedBy = "USA";
        t8.leviedOn = "Copper";
        t8.rate = 0.0194f;
        tariffs.save(t8);

        assertIterableEquals(List.of("Copper", "Lumber"),
                             tariffs.findByLeviedAgainst("Canada").map(o -> o.leviedOn).sorted().collect(Collectors.toList()));

        // Iterator with offset pagination:
        Iterator<Tariff> it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("M", PageRequest.ofSize(3));

        Tariff t;
        assertEquals(true, it.hasNext());
        assertNotNull(t = it.next());
        assertEquals(t8.leviedAgainst, t.leviedAgainst);
        Long t8key = t.key;

        assertEquals(true, it.hasNext());
        assertNotNull(t = it.next());
        assertEquals(t6.leviedAgainst, t.leviedAgainst);

        assertEquals(true, it.hasNext());
        assertNotNull(t = it.next());
        assertEquals(t5.leviedAgainst, t.leviedAgainst);

        assertEquals(true, it.hasNext());
        assertEquals(true, it.hasNext());
        assertNotNull(t = it.next());
        assertEquals(t4.leviedAgainst, t.leviedAgainst);

        assertNotNull(t = it.next());
        assertEquals(t3.leviedAgainst, t.leviedAgainst);

        assertNotNull(t = it.next());
        assertEquals(t2.leviedAgainst, t.leviedAgainst);
        Long t2key = t.key;

        assertNotNull(t = it.next());
        assertEquals(t1.leviedAgainst, t.leviedAgainst);

        assertEquals(false, it.hasNext());
        assertEquals(false, it.hasNext());

        // Iterator with keyset pagination:
        it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("M", PageRequest.ofSize(2).afterKeyset(t8key));

        assertNotNull(t = it.next());
        assertEquals(t6.leviedAgainst, t.leviedAgainst);

        assertNotNull(t = it.next());
        assertEquals(t5.leviedAgainst, t.leviedAgainst);

        assertNotNull(t = it.next());
        assertEquals(t4.leviedAgainst, t.leviedAgainst);

        assertEquals(true, it.hasNext());
        assertNotNull(t = it.next());
        assertEquals(t3.leviedAgainst, t.leviedAgainst);

        assertNotNull(t = it.next());
        assertEquals(t2.leviedAgainst, t.leviedAgainst);

        assertEquals(true, it.hasNext());
        assertNotNull(t = it.next());
        assertEquals(t1.leviedAgainst, t.leviedAgainst);

        assertEquals(false, it.hasNext());

        // Iterator with keyset pagination obtaining pages in reverse direction
        it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("M", PageRequest.ofSize(2).beforeKeyset(t2key));

        assertEquals(true, it.hasNext());
        assertNotNull(t = it.next());
        assertEquals(t4.leviedAgainst, t.leviedAgainst);

        assertNotNull(t = it.next());
        assertEquals(t3.leviedAgainst, t.leviedAgainst);

        assertNotNull(t = it.next());
        assertEquals(t6.leviedAgainst, t.leviedAgainst);

        assertEquals(true, it.hasNext());
        assertNotNull(t = it.next());
        assertEquals(t5.leviedAgainst, t.leviedAgainst);

        assertNotNull(t = it.next());
        assertEquals(t8.leviedAgainst, t.leviedAgainst);

        assertEquals(false, it.hasNext());

        // Paginated iterator with no results:
        it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("A", PageRequest.ofSize(3));
        assertEquals(false, it.hasNext());

        t = tariffs.findByLeviedByAndLeviedAgainstAndLeviedOn("USA", "Bangladesh", "Textiles");
        assertEquals(t6.rate, t.rate, 0.0001f);

        // List return type for Pagination only represents a single page, not all pages.
        // page 1:
        assertIterableEquals(List.of("China", "Germany", "India", "Japan"),
                             tariffs.findByLeviedByOrderByKey("USA", PageRequest.ofSize(4))
                                             .stream()
                                             .map(o -> o.leviedAgainst)
                                             .collect(Collectors.toList()));
        // page 2:
        assertIterableEquals(List.of("Canada", "Bangladesh", "Mexico", "Canada"),
                             tariffs.findByLeviedByOrderByKey("USA", PageRequest.ofSize(4).page(2))
                                             .stream()
                                             .map(o -> o.leviedAgainst)
                                             .collect(Collectors.toList()));

        // Random access to paginated list:
        List<Tariff> list = tariffs.findByLeviedByOrderByKey("USA", PageRequest.ofPage(1));
        assertEquals(t4.leviedAgainst, list.get(3).leviedAgainst);
        assertEquals(t7.leviedAgainst, list.get(6).leviedAgainst);
        assertEquals(t2.leviedAgainst, list.get(1).leviedAgainst);
        assertEquals(t8.leviedAgainst, list.get(7).leviedAgainst);

        assertEquals(8, tariffs.deleteByLeviedBy("USA"));
    }

    /**
     * Use a repository that has no primary entity class, and no lifecyle methods, but allows find operations
     * for a mixture of different entity classes.
     */
    @Test
    public void testMixedRepository() {

        Business[] found = mixed.findByLocationAddressCity("Stewartville");
        assertEquals(List.of("Geotek", "HALCON"),
                     Stream.of(found)
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        LinkedList<Unpopulated> nothing = mixed.findBySomethingStartsWith("TestMixedRepository");
        assertEquals(0, nothing.size());

        assertEquals(List.of("Minnesota", "New York"),
                     mixed.findByName("Rochester")
                                     .map(c -> c.stateName)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a custom join query so that a OneToMany association can query by attributes of the many side of the relationship.
     */
    @Test
    public void testOneToManyCustomJoinQuery() {

        assertIterableEquals(List.of("MICHELLE@TESTS.OPENLIBERTY.IO",
                                     "Matthew@tests.openliberty.io",
                                     "Maximilian@tests.openliberty.io",
                                     "Megan@tests.openliberty.io"),
                             customers.withCardIssuer(Issuer.MonsterCard));
    }

    /**
     * Query that matches multiple entities and returns a combined collection of results across matches for the OneToMany association.
     */
    //@Test
    // This test is currently incorrect because Email is an attribute of Customer (primary entity type), not CreditCard (result type).
    // This would require a way to indicate that a projection is desired, meaning the return type indicates
    // an attribute of the entity rather than the entity class itself.
    // TODO Could this be achieved with @Select?
    public void testOneToManyReturnsCombinedCollectionFromMany() {

        List<Long> cardNumbers = customers.findCardsByEmailEndsWith("an@tests.openliberty.io")
                        .map(card -> card.number)
                        .collect(Collectors.toList());

        // Customer 5's card numbers must come before Customer 4's card numbers due to the ordering on Customer.phone.
        assertEquals(cardNumbers.toString(),
                     true, cardNumbers.equals(List.of(5000921051110001L, 5000921052220002L, 4000921041110001L, 4000921042220002L)) ||
                           cardNumbers.equals(List.of(5000921051110001L, 5000921052220002L, 4000921042220002L, 4000921041110001L)) ||
                           cardNumbers.equals(List.of(5000921052220002L, 5000921051110001L, 4000921042220002L, 4000921041110001L)) ||
                           cardNumbers.equals(List.of(5000921052220002L, 5000921051110001L, 4000921041110001L, 4000921042220002L)));
    }

    /**
     * Query that matches a single entity and so returns one collection for a OneToMany association.
     */
    //@Test
    // Test is currently incorrect because the text expects to query on the Id of Customer (primary entity type),
    // not the Id of CreditCard (result type).
    // This would require a way to indicate that a projection is desired, meaning the return type indicates
    // an attribute of the entity rather than the entity class itself.
    // TODO Could this be achieved with @Select?
    public void testOneToManyReturnsOneSetOfMany() {
        Set<CreditCard> cards = customers.findCardsById(9210005);

        assertEquals(cards.toString(), 2, cards.size());

        CreditCard card1 = null;
        CreditCard card2 = null;
        for (CreditCard c : cards) {
            if (c.number == 5000921051110001L)
                card1 = c;
            else if (c.number == 5000921052220002L)
                card2 = c;
            else
                fail("Unexpected card: " + c);
        }

        assertNotNull(cards.toString(), card1);
        assertEquals(501, card1.securityCode);
        assertEquals(Issuer.Discrooger, card1.issuer);
        assertEquals(LocalDate.of(2021, 5, 10), card1.issuedOn);
        assertEquals(LocalDate.of(2025, 5, 10), card1.expiresOn);
        assertEquals("Maximilian@tests.openliberty.io", card1.debtor.email);
        assertEquals(5075550055L, card1.debtor.phone);
        assertEquals(9210005, card1.debtor.customerId);

        assertNotNull(cards.toString(), card2);
        assertEquals(502, card2.securityCode);
        assertEquals(Issuer.MonsterCard, card2.issuer);
        assertEquals(LocalDate.of(2021, 5, 25), card2.issuedOn);
        assertEquals(LocalDate.of(2025, 5, 25), card2.expiresOn);
        assertEquals("Maximilian@tests.openliberty.io", card2.debtor.email);
        assertEquals(5075550055L, card2.debtor.phone);
        assertEquals(9210005, card2.debtor.customerId);
    }

    /**
     * One-to-one entity mapping.
     */
    @Test
    public void testOneToOne() {
        drivers.deleteByFullNameEndsWith(" TestOneToOne");

        Driver d1 = new Driver("Owen TestOneToOne", 100101000, LocalDate.of(2000, 1, 1), 71, 210, //
                        "T121-100-100-100", "Minnesota", LocalDate.of(2021, 1, 1), LocalDate.of(2025, 1, 1));

        Driver d2 = new Driver("Oliver TestOneToOne", 100202000, LocalDate.of(2002, 2, 2), 72, 220, //
                        "T121-200-200-200", "Wisconsin", LocalDate.of(2022, 2, 2), LocalDate.of(2026, 2, 2));

        Driver d3 = new Driver("Olivia TestOneToOne", 100303000, LocalDate.of(2000, 3, 3), 63, 130, //
                        "T121-300-300-300", "Minnesota", LocalDate.of(2023, 3, 3), LocalDate.of(2027, 3, 3));

        Driver d4 = new Driver("Oscar TestOneToOne", 100404000, LocalDate.of(2004, 4, 4), 74, 240, //
                        "T121-400-400-400", "Iowa", LocalDate.of(2020, 4, 4), LocalDate.of(2024, 4, 4));

        Driver d5 = new Driver("Ozzy TestOneToOne", 100505000, LocalDate.of(2000, 5, 5), 65, 150, //
                        "T121-500-500-500", "Wisconsin", LocalDate.of(2021, 5, 5), LocalDate.of(2025, 5, 5));

        drivers.saveAll(List.of(d1, d2, d3, d4, d5));

        // Query by the entity to which OneToOne maps:
        Driver d = drivers.findByLicense(d4.license);
        assertEquals("Oscar TestOneToOne", d.fullName);

        // Query by an attribute of the entity to which OneToOne maps:
        d = drivers.findByLicenseNum("T121-200-200-200");
        assertEquals("Oliver TestOneToOne", d.fullName);

        // Query by and order by attributes of the entity to which OneToOne maps:
        assertIterableEquals(List.of("Owen TestOneToOne", "Ozzy TestOneToOne", "Oliver TestOneToOne"),
                             drivers.findByLicenseExpiresOnBetween(LocalDate.of(2024, 5, 1), LocalDate.of(2026, 5, 1))
                                             .map(driver -> driver.fullName)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("Olivia TestOneToOne", "Owen TestOneToOne"),
                             drivers.findByLicenseStateNameOrderByLicenseExpiresOnDesc("Minnesota")
                                             .map(driver -> driver.fullName)
                                             .collect(Collectors.toList()));

        // Query that returns a collection of the entity type to which OneToOne maps:
        assertIterableEquals(List.of("Minnesota T121-100-100-100", "Minnesota T121-300-300-300",
                                     "Wisconsin T121-500-500-500", "Wisconsin T121-200-200-200",
                                     "Iowa T121-400-400-400"),
                             drivers.findByFullNameEndsWith(" TestOneToOne")
                                             .map(license -> license.stateName + " " + license.licenseNum)
                                             .collect(Collectors.toList()));

        // Order by attributes of the entity to which OneToOne maps, using various formats for referring to the attributes:
        assertIterableEquals(List.of("Oscar TestOneToOne", // Iowa
                                     "Owen TestOneToOne", "Olivia TestOneToOne", // Minnesota
                                     "Ozzy TestOneToOne", "Oliver TestOneToOne"), // Wisconsin
                             drivers.findByLicenseNotNull()
                                             .map(driver -> driver.fullName)
                                             .collect(Collectors.toList()));

        drivers.deleteByFullNameEndsWith(" TestOneToOne");
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
        // TODO allow entity return type on delete?
        //r1 = rebates.remove(r1);
        //assertEquals(Integer.valueOf(1), r1.id());
        //assertEquals(1.00, r1.amount(), 0.001f);
        //assertEquals(LocalTime.of(11, 31, 0), r1.purchaseMadeAt());
        //assertEquals(LocalDate.of(2023, Month.OCTOBER, 16), r1.purchaseMadeOn());
        //assertEquals(Rebate.Status.PAID, r1.status());
        //assertEquals(LocalDateTime.of(2023, Month.OCTOBER, 16, 11, 44, 0), r1.updatedAt());
        //assertEquals(Integer.valueOf(initialVersion + 2), r1.version());
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

    /**
     * Tests direct usage of StaticMetamodel auto-populated fields.
     */
    @Test
    public void testStaticMetamodel() {
        assertEquals("name", CityAttrNames1.name.name());
        assertEquals(Sort.asc("population"), CityAttrNames1.population.asc());
        assertEquals(Sort.ascIgnoreCase("stateName"), CityAttrNames1.stateName.ascIgnoreCase());

        assertEquals("areaCodes", CityAttrNames2.areaCodes.name());
        assertEquals(Sort.desc("changeCount"), CityAttrNames2.changeCount.desc());
        assertEquals(0L, CityAttrNames2.population);
        assertEquals(null, CityAttrNames2.name);

        // Metamodel should not initialize an id field when the entity has a compound unique identifier (IdClass)
        assertEquals(null, CityAttrNames2.id);

        // Metamodel should not initialize fields that do not correspond to entity attributes
        assertEquals(null, CityAttrNames2.ignore);
    }

    /**
     * Tests direct usage of StaticMetamodel auto-populated Attribute field for a collection type.
     */
    @Test
    public void testStaticMetamodelCollectionAttribute() {
        assertEquals("bankAccounts", _TaxPayer.bankAccounts.name());
    }

    /**
     * Tests that the StaticMetamodel annotation on a non-JPA entity is not populated by or overwritten by
     * the Jakarta Persistence-based Jakarta Data provider.
     */
    @Test
    public void testStaticMetamodelIgnoresNonJPAEntity() {
        // Must have same values that were set by the user:

        Sort<Period> desc = _EntityModelUnknown.days.desc();
        assertEquals("Days", desc.property());

        Sort<Period> asc = _EntityModelUnknown.months.asc();
        assertEquals("Mon", asc.property());

        assertEquals(null, _EntityModelUnknown.years);
    }

    /**
     * Use an Entity which has a version attribute of type Timestamp.
     */
    @Test
    public void testTimestampAsVersion(HttpServletRequest request, HttpServletResponse response) {
        assertEquals(0, counties.deleteByNameIn(List.of("Dodge", "Mower")));

        int[] dodgeZipCodes = new int[] { 55924, 55927, 55940, 55944, 55955, 55985 };
        int[] mowerZipCodes = new int[] { 55912, 55917, 55918, 55926, 55933, 55936, 55950, 55951, 55961, 55953, 55967, 55970, 55973, 55975, 55982 };

        County dodge = new County("Dodge", "Minnesota", 20867, dodgeZipCodes, "Mantorville", "Blooming Prairie", "Claremont", "Dodge Center", "Hayfield", "Kasson", "West Concord");
        County mower = new County("Mower", "Minnesota", 49671, mowerZipCodes, "Austin", "Adams", "Brownsdale", "Dexter", "Elkton", "Grand Meadow", "Le Roy", "Lyle", "Mapleview", "Racine", "Rose Creek", "Sargeant", "Taopi", "Waltham");

        counties.save(dodge, mower);

        dodge = counties.findByName("Dodge").orElseThrow();

        assertEquals(true, counties.updateByNameSetZipCodes("Dodge",
                                                            dodgeZipCodes = new int[] { 55917, 55924, 55927, 55940, 55944, 55955, 55963, 55985 }));

        // Try to update with outdated version/timestamp:
        try {
            dodge.population = 20873;
            counties.save(dodge);
            fail("Should not be able to save using old version: " + dodge.lastUpdated);
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        // Update the version/timestamp and retry:
        Timestamp timestamp = dodge.lastUpdated = counties.findLastUpdatedByName("Dodge");
        dodge.population = 20981;
        counties.save(dodge);

        // Try to delete by previous version/timestamp,
        assertEquals(false, counties.deleteByNameAndLastUpdated("Dodge", timestamp));

        // Should be able to delete with latest version/timestamp,
        timestamp = counties.findLastUpdatedByName("Dodge");
        assertEquals(true, counties.deleteByNameAndLastUpdated("Dodge", timestamp));

        // Try to delete with wrong version/timestamp (from other entity),
        mower.lastUpdated = timestamp;
        try {
            counties.remove(mower);
            fail("Deletion attempt with wrong version did not raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        // Use correct version/timestamp,
        mower = counties.findByName("Mower").orElseThrow();
        counties.remove(mower);
    }

    /**
     * Use an Entity which has an attribute which is a collection that is not annotated with the JPA ElementCollection annotation.
     */
    // Test annotation is present on corresponding method in DataJPATest
    public void testUnannotatedCollection(HttpServletRequest request, HttpServletResponse response) {
        assertEquals(0, counties.deleteByNameIn(List.of("Olmsted", "Fillmore", "Winona", "Wabasha")));

        int[] olmstedZipCodes = new int[] { 55901, 55902, 55903, 55904, 55905, 55906, 55920, 55923, 55929, 55932, 55934, 55940, 55960, 55963, 55964, 55972, 55976 };
        int[] winonaZipCodes = new int[] { 55910, 55925, 55942, 55943, 55947, 55952, 55959, 55964, 55969, 55971, 55972, 55979, 55987, 55988 };
        int[] wabashaZipCodes = new int[] { 55041, 55910, 55932, 55945, 55956, 55957, 55964, 55968, 55981, 55991 };
        int[] fillmoreZipCodes = new int[] { 55922, 55923, 55935, 55939, 55949, 55951, 55954, 55961, 55962, 55965, 55971, 55975, 55976, 55990 };

        County olmsted = new County("Olmsted", "Minnesota", 162847, olmstedZipCodes, "Rochester", "Byron", "Chatfield", "Dover", "Eyota", "Oronoco", "Pine Island", "Stewartville");
        County winona = new County("Winona", "Minnesota", 49671, winonaZipCodes, "Winona", "Altura", "Dakota", "Elba", "Goodview", "La Crescent", "Lewiston", "Minneiska", "Minnesota City", "Rollingstone", "St. Charles", "Stockton", "Utica");
        County wabasha = new County("Wabasha", "Minnesota", 21387, wabashaZipCodes, "Wabasha", "Bellechester", "Elgin", "Hammond", "Kellogg", "Lake City", "Mazeppa", "Millville", "Minneiska", "Plainview", "Zumbro Falls");
        County fillmore = new County("Fillmore", "Minnesota", 21228, fillmoreZipCodes, "Preston", "Canton", "Chatfield", "Fountain", "Harmony", "Lanesboro", "Mabel", "Ostrander", "Peterson", "Rushford", "Rushford Village", "Spring Valley", "Whalen", "Wykoff");

        Stream<County> saved = counties.save(olmsted, winona, wabasha, fillmore);

        assertEquals(List.of("Olmsted", "Winona", "Wabasha", "Fillmore"),
                     saved.map(s -> s.name).collect(Collectors.toList()));

        // find one entity by id as Optional
        County c = counties.findByName("Olmsted").orElseThrow();
        assertEquals("Olmsted", c.name);
        assertEquals(162847, c.population);
        assertEquals(Arrays.toString(olmstedZipCodes), Arrays.toString(c.zipcodes));

        assertIterableEquals(List.of("Byron", "Chatfield", "Dover", "Eyota", "Oronoco", "Pine Island", "Rochester", "Stewartville"),
                             c.cities.stream()
                                             .map(city -> city.name)
                                             .sorted()
                                             .collect(Collectors.toList()));

        // Derby & Oracle  does not support comparisons of BLOB values
        // Derby JDBC Jar Name : derby.jar
        // Oracle JDBC Jar Name : ojdbc8_g.jar
        // This value is passed as HTTP request Parameter(eg: http://{host}/DataJPATestApp?testMethod=testUnannotatedCollection&jdbcJarName=ojdbc8_g.jar)
        String jdbcJarName = request.getParameter("jdbcJarName").toLowerCase();
        if (!(jdbcJarName.startsWith("derby") || jdbcJarName.startsWith("ojdbc8_g"))) {
            // find one entity by zipcodes as Optional
            c = counties.findByZipCodes(wabashaZipCodes).orElseThrow();
            assertEquals("Wabasha", c.name);
            assertEquals(21387, c.population);
        }

        // find multiple collection attributes as List
        List<Set<CityId>> cityLists = counties.findCitiesByNameStartsWith("W");
        assertEquals(cityLists.toString(), 2, cityLists.size());

        assertIterableEquals(List.of("Bellechester", "Elgin", "Hammond", "Kellogg", "Lake City", "Mazeppa", "Millville", "Minneiska", "Plainview", "Wabasha", "Zumbro Falls"),
                             cityLists.get(0)
                                             .stream()
                                             .map(city -> city.name)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("Altura", "Dakota", "Elba", "Goodview", "La Crescent", "Lewiston", "Minneiska", "Minnesota City", "Rollingstone", "St. Charles", "Stockton",
                                     "Utica", "Winona"),
                             cityLists.get(1)
                                             .stream()
                                             .map(city -> city.name)
                                             .sorted()
                                             .collect(Collectors.toList()));

        // find multiple entities
        List<County> found = counties.findByPopulationLessThanEqual(25000);
        assertEquals(found.toString(), 2, found.size());

        assertIterableEquals(List.of("Canton", "Chatfield", "Fountain", "Harmony", "Lanesboro", "Mabel", "Ostrander", "Peterson", "Preston", "Rushford", "Rushford Village",
                                     "Spring Valley", "Whalen", "Wykoff"),
                             found.get(0).cities.stream()
                                             .map(city -> city.name)
                                             .sorted()
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("Bellechester", "Elgin", "Hammond", "Kellogg", "Lake City", "Mazeppa", "Millville", "Minneiska", "Plainview", "Wabasha", "Zumbro Falls"),
                             found.get(1).cities.stream()
                                             .map(city -> city.name)
                                             .sorted()
                                             .collect(Collectors.toList()));

        // find single array
        assertEquals(Arrays.toString(fillmoreZipCodes),
                     Arrays.toString(counties.findZipCodesById("Fillmore")));

        // stream of array attribute
        assertIterableEquals(List.of(Arrays.toString(wabashaZipCodes), Arrays.toString(winonaZipCodes)),
                             counties.findZipCodesByNameEndsWith("a")
                                             .map(Arrays::toString)
                                             .collect(Collectors.toList()));

        // list of array attribute
        assertIterableEquals(List.of(Arrays.toString(fillmoreZipCodes), Arrays.toString(olmstedZipCodes)),
                             counties.findZipCodesByNameNotStartsWith("W")
                                             .stream()
                                             .map(Arrays::toString)
                                             .collect(Collectors.toList()));

        // page of array attribute
        assertIterableEquals(List.of(Arrays.toString(wabashaZipCodes), Arrays.toString(winonaZipCodes)),
                             counties.findZipCodesByNameStartsWith("W", PageRequest.ofSize(10))
                                             .stream()
                                             .map(Arrays::toString)
                                             .collect(Collectors.toList()));

        // optional iterator of array attribute
        Iterator<int[]> it = counties.findZipCodesByPopulationLessThanEqual(50000).orElseThrow();
        assertIterableEquals(List.of(Arrays.toString(fillmoreZipCodes), Arrays.toString(wabashaZipCodes), Arrays.toString(winonaZipCodes)),
                             StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED), false)
                                             .map(Arrays::toString)
                                             .collect(Collectors.toList()));

        // optional for single array with none found
        counties.findZipCodesByName("Dodge") //
                        .ifPresent(array -> fail("Unexpected value: " + Arrays.toString(array)));

        // stream of array attribute with none found
        assertEquals(0, counties.findZipCodesByNameEndsWith("ower").count());

        // list of array attribute with none found
        assertEquals(0, counties.findZipCodesByNameNotStartsWith("_").size());

        // page of array attribute with none found
        assertEquals(0, counties.findZipCodesByNameStartsWith("Hous", PageRequest.ofSize(5)).numberOfElements());

        // optional for iterator over array attribute with none found
        counties.findZipCodesByPopulationLessThanEqual(1) //
                        .ifPresent(i -> fail("Unexpected iterator: " + (i.hasNext() ? Arrays.toString(i.next()) : "(empty)")));

        // update array value to empty
        assertEquals(true, counties.updateByNameSetZipCodes("Wabasha", new int[0]));

        // query on array value
        assertEquals(Arrays.toString(new int[0]),
                     Arrays.toString(counties.findZipCodesByName("Wabasha").orElseThrow()));

        // update array value to non-empty
        int[] wabashaZipCodesDescending = new int[] { 55991, 55981, 55968, 55964, 55957, 55956, 55945, 55932, 55910, 55041 };
        assertEquals(true, counties.updateByNameSetZipCodes("Wabasha", wabashaZipCodesDescending));

        // query on array value
        assertEquals(Arrays.toString(wabashaZipCodesDescending),
                     Arrays.toString(counties.findZipCodesByName("Wabasha").orElseThrow()));

        assertEquals(4, counties.deleteByNameIn(List.of("Olmsted", "Fillmore", "Winona", "Wabasha")));
    }

    /**
     * Use an update method with an entity parameter, where the entity has embedded classes.
     */
    @Test
    public void testUpdateMethodWithEntityParamWithEmbeddedClasses() {
        Business ibm = businesses.findFirstByName("IBM");

        // save these to restore when test completes, so we don't interfere with data used by other tests
        float originalLatitude = ibm.location.latitude;
        float originalLongitude = ibm.location.longitude;
        int originalHouseNum = ibm.location.address.houseNum;
        String originalStreetName = ibm.location.address.street.name;
        String originalStreetDir = ibm.location.address.street.direction;

        boolean updated;
        try {
            // TODO Uncomment the following 3 lines of code to reproduce this EclipseLink error:
            // jakarta.persistence.PersistenceException: Exception [EclipseLink-4002] ...
            // Call: UPDATE WLPBusiness SET LATITUDE = ?, NAME = ? WHERE (ID = ?)
            // ...
            // Caused by: java.sql.SQLDataException: An attempt was made to get a data value of type 'DECIMAL' from a data value of type 'test.jakarta.data.jpa.web.Location'.
            //   ...
            //   at org.apache.derby.iapi.jdbc.BrokeredPreparedStatement.setObject(Unknown Source)
            //   at com.ibm.ws.rsadapter.jdbc.WSJdbcPreparedStatement.setObject(WSJdbcPreparedStatement.java:1687)
            //   at org.eclipse.persistence.internal.databaseaccess.DatabasePlatform.setParameterValueInDatabaseCall(DatabasePlatform.java:2462)
            //   at org.eclipse.persistence.platform.database.DerbyPlatform.setParameterValueInDatabaseCall(DerbyPlatform.java:985)
            //   at org.eclipse.persistence.internal.databaseaccess.DatabaseCall.prepareStatement(DatabaseCall.java:799)
            //   at org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor.basicExecuteCall(DatabaseAccessor.java:630)
            //Address newAddress = new Address("Rochester", "MN", 55901, 3605, new Street("US 52", "N"));
            //Location newLocation = new Location(newAddress, 44.05881f, -92.50556f);
            //assertEquals(true, businesses.updateWithJPQL(newLocation, "IBM", ibm.id));

            // Jakarta Data was able to avoid the above error by generating a query to set each attribute individually,

            ibm.location.latitude = 44.05881f;
            ibm.location.longitude = -92.50556f;
            ibm.location.address.houseNum = 3605;
            ibm.location.address.street = new Street("US 52", "N");

            assertEquals(true, businesses.update(ibm));

            ibm = businesses.findFirstByName("IBM");

            assertEquals("IBM", ibm.name);
            assertEquals(44.05881f, ibm.location.latitude, 0.00001f);
            assertEquals(-92.50556f, ibm.location.longitude, 0.00001f);
            assertEquals(3605, ibm.location.address.houseNum);
            assertEquals("US 52", ibm.location.address.street.name);
            assertEquals("N", ibm.location.address.street.direction);
            assertEquals("Rochester", ibm.location.address.city);
            assertEquals("MN", ibm.location.address.state);
            assertEquals(55901, ibm.location.address.zip);
        } finally {
            // restore original values
            ibm.location.latitude = originalLatitude;
            ibm.location.longitude = originalLongitude;
            ibm.location.address.houseNum = originalHouseNum;
            ibm.location.address.street.name = originalStreetName;
            ibm.location.address.street.direction = originalStreetDir;

            updated = businesses.update(ibm);
        }
        assertEquals(true, updated);

        ibm = businesses.findFirstByName("IBM");

        assertEquals("IBM", ibm.name);
        assertEquals(originalLatitude, ibm.location.latitude, 0.00001f);
        assertEquals(originalLongitude, ibm.location.longitude, 0.00001f);
        assertEquals(2800, ibm.location.address.houseNum);
        assertEquals("37th St", ibm.location.address.street.name);
        assertEquals("NW", ibm.location.address.street.direction);
        assertEquals("Rochester", ibm.location.address.city);
        assertEquals("MN", ibm.location.address.state);
        assertEquals(55901, ibm.location.address.zip);
    }

    /**
     * Test that a method that is annotated with the Update annotation can return entity results,
     * and the resulting entities match the updated values that were written to the database.
     */
    @Test
    public void testUpdateWithEntityResults() {
        orders.deleteAll();

        PurchaseOrder o1 = new PurchaseOrder();
        o1.purchasedBy = "testUpdateWithEntityResults-Customer1";
        o1.purchasedOn = OffsetDateTime.now();
        o1.total = 1.00f;
        o1 = orders.create(o1);

        PurchaseOrder o2 = new PurchaseOrder();
        o2.purchasedBy = "testUpdateWithEntityResults-Customer2";
        o2.purchasedOn = OffsetDateTime.now();
        o2.total = 2.00f;
        o2 = orders.create(o2);

        PurchaseOrder o3 = new PurchaseOrder();
        o3.purchasedBy = "testUpdateWithEntityResults-Customer3";
        o3.purchasedOn = OffsetDateTime.now();
        o3.total = 3.00f;
        o3 = orders.create(o3);

        PurchaseOrder o4 = new PurchaseOrder();
        o4.purchasedBy = "testUpdateWithEntityResults-Customer4";
        o4.purchasedOn = OffsetDateTime.now();
        o4.total = 4.00f;
        o4 = orders.create(o4);

        PurchaseOrder o5 = new PurchaseOrder();
        o5.purchasedBy = "testUpdateWithEntityResults-Customer5";
        o5.purchasedOn = OffsetDateTime.now();
        o5.total = 5.00f;
        o5 = orders.create(o5);

        PurchaseOrder o6 = new PurchaseOrder();
        o6.purchasedBy = "testUpdateWithEntityResults-Customer6";
        o6.purchasedOn = OffsetDateTime.now();
        o6.total = 6.00f;
        // o6 is intentionally not written to the database so that it will not be found for update

        int o1_initialVersion = o1.versionNum;
        int o2_initialVersion = o2.versionNum;
        int o3_initialVersion = o3.versionNum;
        int o4_initialVersion = o4.versionNum;
        int o5_initialVersion = o5.versionNum;

        // update multiple in a variable arguments array
        o1.total = 1.01f;
        o3.total = 3.01f;
        PurchaseOrder[] modified = orders.modifyAll(o3, o1);
        assertEquals("testUpdateWithEntityResults-Customer3", modified[0].purchasedBy);
        assertEquals(3.01f, modified[0].total, 0.001f);
        assertEquals(o3_initialVersion + 1, modified[0].versionNum);
        // o3 is intentionally left at its original version so that it will not be found for update

        o1 = modified[1];
        assertEquals("testUpdateWithEntityResults-Customer1", o1.purchasedBy);
        assertEquals(1.01f, o1.total, 0.001f);
        assertEquals(o1_initialVersion + 1, o1.versionNum);

        // attempt to update multiple in an Iterable where the first entity is non-matching due to its version
        o1.total = 1.02f;
        o3.versionNum = o3_initialVersion;
        o3.total = 3.02f;
        o5.total = 5.02f;
        Vector<PurchaseOrder> results;
        try {
            results = orders.modifyMultiple(List.of(o3, o5, o1));
            fail("An attempt to update multiple where the version of the first entity does not match the database " +
                 "must raise OptimisticLockingFailureException");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        results = orders.modifyMultiple(List.of(o5, o1));
        assertEquals(2, results.size());

        o5 = results.get(0);
        assertEquals("testUpdateWithEntityResults-Customer5", o5.purchasedBy);
        assertEquals(5.02f, o5.total, 0.001f);
        assertEquals(o5_initialVersion + 1, o5.versionNum);

        o1 = results.get(1);
        assertEquals("testUpdateWithEntityResults-Customer1", o1.purchasedBy);
        assertEquals(1.02f, o1.total, 0.001f);
        assertEquals(o1_initialVersion + 2, o1.versionNum);

        // update multiple in a variable arguments array where the second entry is not found in the database
        o2.total = 2.03f;
        o4.total = 4.03f;
        o5.total = 5.03f;
        o6.total = 6.03f;
        try {
            modified = orders.modifyAll(o5, o6, o4, o2);
            fail("An attempt to update multiple where the second entity is not found in the database " +
                 "must raise OptimisticLockingFailureException");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        modified = orders.modifyAll(o5, o4, o2);
        assertEquals(3, modified.length);

        o5 = modified[0];
        assertEquals("testUpdateWithEntityResults-Customer5", o5.purchasedBy);
        assertEquals(5.03f, o5.total, 0.001f);
        assertEquals(o5_initialVersion + 2, o5.versionNum);

        o4 = modified[1];
        assertEquals("testUpdateWithEntityResults-Customer4", o4.purchasedBy);
        assertEquals(4.03f, o4.total, 0.001f);
        assertEquals(o4_initialVersion + 1, o4.versionNum);

        o2 = modified[2];
        assertEquals("testUpdateWithEntityResults-Customer2", o2.purchasedBy);
        assertEquals(2.03f, o2.total, 0.001f);
        assertEquals(o1_initialVersion + 1, o2.versionNum);

        // update returning one entity
        o4.total = 4.04f;
        o4 = orders.modifyOne(o4);
        assertEquals("testUpdateWithEntityResults-Customer4", o4.purchasedBy);
        assertEquals(4.04f, o4.total, 0.001f);
        assertEquals(o4_initialVersion + 2, o4.versionNum);

        // update where no entities match, with varargs array
        try {
            modified = orders.modifyAll(o3, o6);
            fail("An attempt to update a varargs array of multiple with a mixture of entities where either the version does not match " +
                 " or the entity is not found in the database must raise OptimisticLockingFailureException");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        // update where no entities match, with Iterable
        try {
            results = orders.modifyMultiple(List.of(o6, o3));
            fail("An attempt to update an Iterable of multiple with a mixture of entities where either the version does not match " +
                 " or the entity is not found in the database must raise OptimisticLockingFailureException");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        // update where the only entity does not match
        try {
            orders.modifyOne(o6);
            fail("An attempt to update a single entity that does not exist in the database " +
                 "must raise OptimisticLockingFailureException");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        // update where the only entity does not match
        try {
            orders.modifyIfMatching(o3);
            fail("Another attempt to update a single entity that does not exist in the database " +
                 "must raise OptimisticLockingFailureException");
        } catch (OptimisticLockingFailureException x) {
            // expected
        }
    }

    /**
     * Test that @Delete requires the entity to exist with the same version as the database for successful removal.
     */
    @Test
    public void testVersionedDelete() {
        orders.deleteAll();

        PurchaseOrder o1 = new PurchaseOrder();
        o1.purchasedBy = "testVersionedDelete-Customer1";
        o1.purchasedOn = OffsetDateTime.now();
        o1.total = 1.09f;
        try {
            orders.cancel(o1); // doesn't exist yet
            fail("Attempt to delete an entity that doesn't exist yet, must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        o1 = orders.create(o1);

        int oldVersion = o1.versionNum;

        o1.total = 1.19f;
        orders.modify(o1);

        o1 = orders.findById(o1.id).orElseThrow();
        int newVersion = o1.versionNum;
        Long id = o1.id;

        // Attempt deletion at old version
        o1 = new PurchaseOrder();
        o1.id = id;
        o1.purchasedBy = "testVersionedDelete-Customer1";
        o1.purchasedOn = OffsetDateTime.now();
        o1.total = 1.19f;
        o1.versionNum = oldVersion;
        try {
            orders.cancel(o1);
            fail("Attempt to delete an outdated version must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        PurchaseOrder o2 = new PurchaseOrder();
        o2.purchasedBy = "testVersionedDelete-Customer2";
        o2.purchasedOn = OffsetDateTime.now();
        o2.total = 2.09f;

        PurchaseOrder o3 = new PurchaseOrder();
        o3.purchasedBy = "testVersionedDelete-Customer3";
        o3.purchasedOn = OffsetDateTime.now();
        o3.total = 3.09f;

        LinkedList<PurchaseOrder> created = orders.create(List.of(o2, o3));
        o2 = created.get(0);
        o3 = created.get(1);

        // Attempt deletion at correct version
        o1.versionNum = newVersion;
        orders.cancel(o1, o2);

        // Entities o1 and o2 should no longer be in the database:
        try {
            orders.cancel(o1, o2);
            fail("Attempt to delete multiple where entities are no longer in the database must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        // Entity o3 should still be there:
        o3 = orders.findById(o3.id).orElseThrow();
        assertEquals(3.09f, o3.total, 0.001f);

        // Deletion where only 1 is found:
        try {
            orders.cancel(o3, o1, o2);
            fail("Attempt to delete multiple where at least one is no longer in the database must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        orders.cancel(o3);
    }

    /**
     * Test that remove(entity) requires the entity to be at the same version as the database for successful removal.
     * This tests covers an entity type with an IdClass.
     */
    @Test
    public void testVersionedRemoval() {
        City duluth = cities.save(new City("Duluth", "Minnesota", 86697, Set.of(218)));
        long oldVersion = duluth.changeCount;

        duluth.population = 86372;
        duluth = cities.save(duluth);
        long newVersion = duluth.changeCount;

        duluth = new City("Duluth", "Minnesota", 86697, Set.of(218));
        duluth.changeCount = oldVersion;
        try {
            cities.remove(duluth);
            fail("Attempt to delete with an outdated version must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        duluth.changeCount = newVersion;
        cities.remove(duluth);
    }

    /**
     * Test that @Update requires the entity to exist with the same version as the database for successful update.
     * This tests covers an entity type with an IdClass.
     */
    @Test
    public void testVersionedUpdate() {
        orders.deleteAll();

        PurchaseOrder o1 = new PurchaseOrder();
        o1.purchasedBy = "testVersionedUpdate-Customer1";
        o1.purchasedOn = OffsetDateTime.now();
        o1.total = 10.09f;
        try {
            orders.modify(o1); // doesn't exist yet
            fail("Attempt to modify an entity that does not exist in the database must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        o1 = orders.create(o1);

        int oldVersion = o1.versionNum;

        o1.total = 10.19f;
        orders.modify(o1);

        o1 = orders.findById(o1.id).orElseThrow();
        assertEquals(10.19f, o1.total, 0.001f);
        int newVersion = o1.versionNum;
        Long id = o1.id;

        o1 = new PurchaseOrder();
        o1.id = id;
        o1.purchasedBy = "testVersionedUpdate-Customer1";
        o1.purchasedOn = OffsetDateTime.now();
        o1.total = 10.29f;
        o1.versionNum = oldVersion;
        try {
            orders.update(o1);
            fail("Attempt to update an outdated version of an entity must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        o1.versionNum = newVersion;
        orders.update(o1);
        o1 = orders.findById(o1.id).orElseThrow();
        assertEquals(10.29f, o1.total, 0.001f);

        orders.delete(o1);

        o1.total = 10.39f;
        try {
            orders.modify(o1); // doesn't exist anymore
            fail("Attempt to update an entity that no longer exists in the database must raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }
    }
}
