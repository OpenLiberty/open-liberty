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
package test.jakarta.data.jpa.web;

import static componenttest.annotation.SkipIfSysProp.DB_DB2;
import static componenttest.annotation.SkipIfSysProp.DB_Not_Default;
import static componenttest.annotation.SkipIfSysProp.DB_Postgres;
import static componenttest.annotation.SkipIfSysProp.DB_SQLServer;
import static jakarta.data.repository.By.ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static test.jakarta.data.jpa.web.Assertions.assertArrayEquals;
import static test.jakarta.data.jpa.web.Assertions.assertIterableEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.Direction;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.OnlyIfSysProp;
import componenttest.annotation.SkipIfSysProp;
import componenttest.app.FATServlet;
import test.jakarta.data.jpa.web.CreditCard.CardId;
import test.jakarta.data.jpa.web.CreditCard.Issuer;
import test.jakarta.data.jpa.web.Mobile.OS;
import test.jakarta.data.jpa.web.Residence.Occupant;

@DataSourceDefinition(name = "java:module/jdbc/RepositoryDataStore",
                      className = "${repository.datasource.class.name}",
                      databaseName = "${repository.database.name}",
                      user = "${repository.database.user}",
                      password = "${repository.database.password}",
                      properties = {
                                     "createDatabase=create"
                      })
@SuppressWarnings("serial")
@WebServlet("/*")
public class DataJPATestServlet extends FATServlet {

    @Inject
    Accounts accounts;

    @Inject
    Apartments apartments;

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
    Demographics demographics;

    @Inject
    Drivers drivers;

    @Inject
    ECRepo ecRepo;

    @Inject
    Employees employees;

    @Inject
    Manufacturers manufacturers;

    @Inject
    MixedRepository mixed;

    @Inject
    MobilePhones mobilePhones;

    @Inject
    Models models;

    @Inject
    Orders orders;

    @Inject
    Rebates rebates;

    @Inject
    Segments segments;

    @Inject
    ShippingAddresses shippingAddresses;

    @Inject
    Tariffs tariffs;

    @Inject
    TaxPayers taxpayers;

    @Resource
    private UserTransaction tran;

    @Inject
    Triangles triangles;

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

        demographics.write(new DemographicInfo(2024, 4, 30, 133809000, 7136033799632.56, 27480960216618.32));
        demographics.write(new DemographicInfo(2023, 4, 28, 134060000, 6852746625848.93, 24605068022566.94));
        demographics.write(new DemographicInfo(2022, 4, 29, 132250000, 6526909395140.41, 23847245116757.60));
        demographics.write(new DemographicInfo(2021, 4, 30, 127160000, 6118659345749.70, 22056055138417.67));
        demographics.write(new DemographicInfo(2020, 4, 30, 123190000, 5920553066244.38, 19053618801919.97));
        demographics.write(new DemographicInfo(2019, 4, 30, 130600000, 5843472921623.80, 16192789476576.03));
        demographics.write(new DemographicInfo(2018, 4, 30, 128570000, 5733071837291.80, 15335128360141.59));
        demographics.write(new DemographicInfo(2017, 4, 28, 125970000, 5552784531172.11, 14293344777463.61));
        demographics.write(new DemographicInfo(2016, 4, 29, 123760000, 5346192750684.33, 13841194733299.04));
        demographics.write(new DemographicInfo(2015, 4, 30, 121490000, 5098878878836.55, 13053681247951.16));
        demographics.write(new DemographicInfo(2014, 4, 30, 118720000, 5004968792143.34, 12503468335518.28));
        demographics.write(new DemographicInfo(2013, 4, 30, 116310000, 4885697098978.25, 11943148398205.65));
        demographics.write(new DemographicInfo(2012, 4, 30, 114810000, 4776297169202.55, 10916070898102.68));
        demographics.write(new DemographicInfo(2011, 4, 29, 112560000, 4632679886492.71, 9654950165830.41));
        demographics.write(new DemographicInfo(2010, 4, 30, 111710000, 4514304290243.70, 8434434625613.16));
        demographics.write(new DemographicInfo(2009, 4, 30, 112630000, 4307767198983.08, 6930824942975.56));
        demographics.write(new DemographicInfo(2008, 4, 30, 120030000, 4133362638109.27, 5244194578964.17));
        demographics.write(new DemographicInfo(2007, 4, 30, 121090000, 3833110332444.19, 5007058051986.64));
        demographics.write(new DemographicInfo(2006, 4, 30, 119690000, 3535769322660.75, 4819948752057.74));
        demographics.write(new DemographicInfo(2005, 4, 29, 117020000, 3213472491939.22, 4551064845424.92));
        demographics.write(new DemographicInfo(2004, 4, 30, 114520000, 2974811477645.08, 4158978012936.35));
        demographics.write(new DemographicInfo(2003, 4, 30, 113320000, 2757535748111.21, 3702844997678.07));
        demographics.write(new DemographicInfo(2002, 4, 30, 112700000, 2582340471146.16, 3402336886067.70));

        // TODO remove this workaround for intermittent issue triggered by test ordering once 28078 is fixed
        testLiteralDouble();
        // To quickly try reproducing the issue, remove the above line and add the following line to tearDown,
        // runTest(server, "DataJPATestApp", "testLiteralDouble");
    }

    /**
     * Test the ABS JDQL function by querying on values that could be positive or negative.
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
     * Use a repository method comparing a BigDecimal value on an entity that includes BigDecimal attributes.
     * This includes both a comparison in the query conditions as well as ordering on the BigDecimal attribute.
     */
    @Test
    public void testBigDecimal() {
        final ZoneId EASTERN = ZoneId.of("America/New_York");

        List<DemographicInfo> list = demographics.findByPublicDebtBetween(BigDecimal.valueOf(5000000000000.00), // 5 trillion
                                                                          BigDecimal.valueOf(10000000000000.00)); // 10 trillion
        assertEquals(list.toString(), 5, list.size());
        assertEquals(2007, list.get(0).collectedOn.atZone(EASTERN).get(ChronoField.YEAR));
        assertEquals(2008, list.get(1).collectedOn.atZone(EASTERN).get(ChronoField.YEAR));
        assertEquals(2009, list.get(2).collectedOn.atZone(EASTERN).get(ChronoField.YEAR));
        assertEquals(2010, list.get(3).collectedOn.atZone(EASTERN).get(ChronoField.YEAR));
        assertEquals(2011, list.get(4).collectedOn.atZone(EASTERN).get(ChronoField.YEAR));

        // Use the BigDecimal and BigInteger values in a computation.
        List<BigDecimal> debtPerFullTimeWorker = demographics.debtPerFullTimeWorker()
                        .map(DebtPerWorker::get)
                        .sorted()
                        .toList();

        assertEquals(debtPerFullTimeWorker.toString(), 23, debtPerFullTimeWorker.size());
        assertEquals(53102.72, debtPerFullTimeWorker.get(0).doubleValue(), 0.02); // 2002
        assertEquals(258704.53, debtPerFullTimeWorker.get(22).doubleValue(), 0.02); // 2024
    }

    /**
     * Use a repository method comparing a BigInteger value on an entity that includes BigInteger attributes.
     * This includes both a comparison in the query conditions as well as ordering on the BigInteger attribute.
     */
    @Test
    public void testBigInteger() {
        ZoneId ET = ZoneId.of("America/New_York");

        List<Instant> list = demographics.whenFullTimeEmploymentWithin(BigInteger.valueOf(120000000),
                                                                       BigInteger.valueOf(126000000));
        assertEquals(2008, list.get(0).atZone(ET).get(ChronoField.YEAR));
        assertEquals(2007, list.get(1).atZone(ET).get(ChronoField.YEAR));
        assertEquals(2015, list.get(2).atZone(ET).get(ChronoField.YEAR));
        assertEquals(2020, list.get(3).atZone(ET).get(ChronoField.YEAR));
        assertEquals(2016, list.get(4).atZone(ET).get(ChronoField.YEAR));
        assertEquals(2017, list.get(5).atZone(ET).get(ChronoField.YEAR));
    }

    /**
     * Use an entity that has an attribute that is a byte[], performing repository operations
     * that query by the byte[] and return the byte[] attribute in various ways.
     */
    @Test
    public void testByteArrayAttributeType() {
        // remove all data before test
        triangles.deleteByHypotenuseNot((byte) 0);

        List<Triangle> saved = triangles.saveAll(List.of(new Triangle((byte) 10, (byte) 10, (byte) 10), // keys[0]
                                                         new Triangle((byte) 13, (byte) 84, (byte) 85), // keys[1]
                                                         new Triangle((byte) 16, (byte) 63, (byte) 65), // keys[2]
                                                         new Triangle((byte) 28, (byte) 45, (byte) 53), // keys[3]
                                                         new Triangle((byte) 33, (byte) 56, (byte) 65), // keys[4]
                                                         new Triangle((byte) 39, (byte) 80, (byte) 89), // keys[5]
                                                         new Triangle((byte) 48, (byte) 55, (byte) 73))); // keys[6]

        int[] keys = new int[] { saved.get(0).distinctKey,
                                 saved.get(1).distinctKey,
                                 saved.get(2).distinctKey,
                                 saved.get(3).distinctKey,
                                 saved.get(4).distinctKey,
                                 saved.get(5).distinctKey,
                                 saved.get(6).distinctKey };

        // byte[] as return value
        assertEquals(0, Arrays.compare(new byte[] { 13, 84, 85 },
                                       triangles.getSides(keys[1])));

        // Optional byte[] as return value
        assertEquals(0, Arrays.compare(new byte[] { 39, 80, 89 },
                                       triangles.getSidesIfPresent(keys[5]).orElseThrow()));

        // updates to a byte[] attribute
        assertEquals(true, triangles.resizePreservingHypotenuse(keys[1],
                                                                new byte[] { 36, 77, 85 },
                                                                (short) (198)));

        List<Triangle> found = triangles.withPerimeter((short) (36 + 77 + 85));
        assertEquals(1, found.size());
        Triangle t = found.get(0);
        assertEquals(0, Arrays.compare(new byte[] { 36, 77, 85 }, t.sides));
        assertEquals(t.distinctKey, Integer.valueOf(keys[1]));
        assertEquals(t.hypotenuse, Byte.valueOf((byte) 85));
        assertEquals(t.perimeter, (short) (36 + 77 + 85));
        assertEquals(Short.valueOf((short) 0), t.sameLengthSides);

        // update to cause triangles 2 and 4 to have the same sides
        assertEquals(true, triangles.resizePreservingHypotenuse(keys[2],
                                                                new byte[] { 33, 56, 65 },
                                                                (short) (154)));

        // results as array of byte[], ordered by hypotenuse, then key
        byte[][] array = triangles.sidesWhereHypotenuseWithin((byte) 65, (byte) 75);
        assertEquals(Arrays.deepToString(array), 3, array.length);
        assertEquals(0, Arrays.compare(new byte[] { 33, 56, 65 }, array[0]));
        assertEquals(0, Arrays.compare(new byte[] { 33, 56, 65 }, array[1]));
        assertEquals(0, Arrays.compare(new byte[] { 48, 55, 73 }, array[2]));

        // results as a stream of byte[]
        Stream<byte[]> stream = triangles.sidesWherePerimeter((short) 30);
        byte[] sides = stream.findFirst().orElseThrow();
        assertEquals(Arrays.toString(sides), 0, Arrays.compare(new byte[] { 10, 10, 10 }, sides));

        // results as a list of byte[]
        List<byte[]> list = triangles.sidesWhereNumSidesEqual((short) 0);
        assertEquals(6, list.size());
        assertEquals(Arrays.toString(list.get(0)), 0, Arrays.compare(new byte[] { 28, 45, 53 }, list.get(0)));
        assertEquals(Arrays.toString(list.get(1)), 0, Arrays.compare(new byte[] { 33, 56, 65 }, list.get(1)));
        assertEquals(Arrays.toString(list.get(2)), 0, Arrays.compare(new byte[] { 33, 56, 65 }, list.get(2)));
        assertEquals(Arrays.toString(list.get(3)), 0, Arrays.compare(new byte[] { 48, 55, 73 }, list.get(3)));
        assertEquals(Arrays.toString(list.get(4)), 0, Arrays.compare(new byte[] { 36, 77, 85 }, list.get(4)));
        assertEquals(Arrays.toString(list.get(5)), 0, Arrays.compare(new byte[] { 39, 80, 89 }, list.get(5)));

        // select values including a function on byte[] column
        // SQLServer does not support length for IMAGE values
        // SQLServer JDBC Jar Name : mssql-jdbc.jar
        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
        if (!(jdbcJarName.startsWith("mssql-jdbc"))) {
            int[][] sidesInfo = triangles.sidesInfo((byte) 65);
            assertEquals(2, sidesInfo.length);
            assertEquals(0, sidesInfo[0][0]);
            assertEquals(3, sidesInfo[0][1]);
            assertEquals(0, sidesInfo[1][0]);
            assertEquals(3, sidesInfo[1][1]);

            sidesInfo = triangles.sidesInfo((byte) 89);
            assertEquals(sidesInfo.toString(), 1, sidesInfo.length);
            assertEquals(0, sidesInfo[0][0]);
            assertEquals(3, sidesInfo[0][1]);
        }

        // empty stream
        assertEquals(1, triangles.deleteByHypotenuseNull());
        stream = triangles.sidesWherePerimeter((short) 30);
        assertEquals(0, stream.count());

        assertEquals(4L, triangles.deleteByHypotenuseNot((byte) 65));

        assertEquals(2L, triangles.deleteByHypotenuseNot((byte) 0));

        // empty array
        array = triangles.sidesWhereHypotenuseWithin((byte) 5, (byte) 125);
        assertEquals(Arrays.deepToString(array), 0, array.length);

        // empty list
        list = triangles.sidesWhereNumSidesEqual((short) 0);
        assertEquals(List.of(), list);
    }

    /**
     * Comparison ignoring case on an entity attribute of type char.
     */
    @Test
    public void testCharIgnoreCase() {
        // Clear out data before test
        employees.deleteByLastName("TestCharIgnoreCase");

        businesses.save(new Employee(33, "Charlotte", "TestCharIgnoreCase", (short) 1033, 'C'),
                        new Employee(14, "Christina", "TestCharIgnoreCase", (short) 1014, 'c'),
                        new Employee(22, "Claudia", "TestCharIgnoreCase", (short) 1022, 'b'),
                        new Employee(54, "Cecilia", "TestCharIgnoreCase", (short) 1073, 'D'),
                        new Employee(73, "Cindy", "TestCharIgnoreCase", (short) 1054, 'c'));

        assertEquals(List.of(14, 33, 73, 54),
                     employees.findByBadgeAccessLevelIgnoreCaseGreaterThan("B")
                                     .map(e -> e.empNum)
                                     .collect(Collectors.toList()));

        employees.deleteByLastName("TestCharIgnoreCase");
    }

    /**
     * Use repository methods that return a collection attribute as a single value
     * and multiple attributes including a collection attribute via a record.
     */
    @Test
    public void testCollectionAttribute() {
        assertEquals(Set.of(507),
                     cities.areaCodes("Rochester", "Minnesota").orElseThrow());

        List<AreaInfo> list = cities.areaInfo("Missouri").collect(Collectors.toList());
        assertEquals(list.toString(), 2, list.size());

        assertEquals("Kansas City", list.get(0).name());
        assertEquals("Missouri", list.get(0).stateName());
        assertEquals(Set.of(816, 975), list.get(0).areaCodes());

        assertEquals("Springfield", list.get(1).name());
        assertEquals("Missouri", list.get(1).stateName());
        assertEquals(Set.of(417), list.get(1).areaCodes());
    }

    /**
     * Use repository methods that convert a BigInteger value to other
     * numeric types.
     */
    @SkipIfSysProp({
                     DB_DB2, //TODO Failing on DB2 due to eclipselink issue. https://github.com/OpenLiberty/open-liberty/issues/29443
    })
    @Test
    public void testConvertBigDecimalValue() {
        ZoneId ET = ZoneId.of("America/New_York");
        Instant when = ZonedDateTime.of(2024, 4, 30, 12, 0, 0, 0, ET)
                        .toInstant();

        assertEquals(27480960216618.32,
                     demographics.publicDebtAsBigDecimal(when)
                                     .doubleValue(),
                     1.0);

        try {
            Optional<BigInteger> i = demographics.publicDebtAsBigInteger(when);
            // TODO is BigDecimal.toBigIntegerExact() broken?
            // or are the fractional digits not being included?
            //fail("Should not convert BigDecimal 27480960216618.32 to BigInteger " + i);
            assertEquals(27480960216618L,
                         i.orElseThrow().longValue());
        } catch (MappingException x) {
            if (x.getCause() instanceof ArithmeticException)
                ; // expected - out of range
            else
                throw x;
        }

        try {
            byte b = demographics.publicDebtAsByte(when);
            fail("Should not convert BigDecimal 27480960216618.32 to byte " + b);
        } catch (MappingException x) {
            if (x.getCause() instanceof ArithmeticException)
                ; // expected - out of range
            else
                throw x;
        }

        try {
            Double d = demographics.publicDebtAsDouble(when);
            fail("Should not convert BigDecimal 27480960216618.32 to Double " + d);
        } catch (MappingException x) {
            // expected - out of range
        }

        try {
            Optional<Float> f = demographics.publicDebtAsFloat(when);
            fail("Should not convert BigDecimal 27480960216618.32 to Float " + f);
        } catch (MappingException x) {
            // expected - out of range
        }

        try {
            int i = demographics.publicDebtAsInt(when);
            fail("Should not convert BigDecimal 27480960216618.32 to int " + i);
        } catch (MappingException x) {
            // expected - out of range
        }

        try {
            Long l = demographics.publicDebtAsLong(when);
            // TODO is BigDecimal.longValueExact() broken?
            // or are the fractional digits not being included?
            //fail("Should not convert BigDecimal 27480960216618.32 to Long " + l);
            assertEquals(Long.valueOf(27480960216618L),
                         l);
        } catch (MappingException x) {
            // expected - out of range
        }

        try {
            Optional<Short> s = demographics.publicDebtAsShort(when);
            fail("Should not convert BigDecimal 27480960216618.32 to Short " + s);
        } catch (MappingException x) {
            // expected - out of range
        }
    }

    /**
     * Use repository methods that convert a BigInteger value to other
     * numeric types.
     */
    @SkipIfSysProp({
                     DB_DB2, //TODO Failing on DB2 due to eclipselink issue. https://github.com/OpenLiberty/open-liberty/issues/29443
    })
    @Test
    public void testConvertBigIntegerValue() {
        ZoneId ET = ZoneId.of("America/New_York");
        Instant when = ZonedDateTime.of(2024, 4, 30, 12, 0, 0, 0, ET)
                        .toInstant();

        assertEquals(133809000L,
                     demographics.numFullTimeWorkersAsBigDecimal(when)
                                     .orElseThrow()
                                     .longValueExact());

        assertEquals(133809000L,
                     demographics.numFullTimeWorkersAsBigInteger(when)
                                     .longValueExact());

        try {
            Optional<Byte> b = demographics.numFullTimeWorkersAsByte(when);
            fail("Should not convert BigInteger 133809000 to byte value " + b);
        } catch (MappingException x) {
            // expected - out of range
        }

        try {
            Double d = demographics.numFullTimeWorkersAsDouble(when);
            fail("Should not convert BigInteger 133809000 to Double value " + d);
        } catch (MappingException x) {
            // expected - not convertible
        }

        try {
            float f = demographics.numFullTimeWorkersAsFloat(when);
            fail("Should not convert BigInteger 133809000 to float value " + f);
        } catch (MappingException x) {
            // expected - not convertible
        }

        assertEquals(Integer.valueOf(133809000),
                     demographics.numFullTimeWorkersAsInteger(when)
                                     .toCompletableFuture()
                                     .join()
                                     .orElseThrow());

        assertEquals(133809000L,
                     demographics.numFullTimeWorkersAsLong(when));

        try {
            short s = demographics.numFullTimeWorkersAsShort(when);
            fail("Should not convert BigInteger 133809000 to short value " + s);
        } catch (MappingException x) {
            // expected - out of range
        }
    }

    /**
     * Use a repository method with query language for the main query and count query,
     * where the count query is JDQL consisting of the FROM clause only.
     */
    @Test
    public void testCountQueryWithFromClauseOnly() {
        Page<Business> page1 = mixed.findAll(PageRequest.ofSize(5), Order.by(Sort.desc("name")));

        assertEquals(5L, page1.numberOfElements());
        assertEquals(15L, page1.totalElements());
        assertEquals(3L, page1.totalPages());
        assertEquals(true, page1.hasNext());

        assertEquals(List.of("Think Bank", "Silver Lake Foods", "Reichel Foods", "RAC", "Olmsted Medical"),
                     page1.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        Page<Business> page2 = mixed.findAll(page1.nextPageRequest(), Order.by(Sort.desc("name")));

        assertEquals(List.of("Metafile", "Mayo Clinic", "IBM", "Home Federal Savings Bank", "HALCON"),
                     page2.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        assertEquals(true, page2.hasNext());

        Page<Business> page3 = mixed.findAll(page2.nextPageRequest(), Order.by(Sort.desc("name")));

        assertEquals(List.of("Geotek", "Custom Alarm", "Crenlo", "Cardinal", "Benike Construction"),
                     page3.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method with query language for the main query and count query,
     * where the count query is JDQL consisting of the FROM and WHERE clauses only.
     */
    @Test
    public void testCountQueryWithFromAndWhereClausesOnly() {
        Order<Business> order = Order.by(Sort.asc("name"));
        Page<Business> page1 = mixed.locatedIn("Rochester", PageRequest.ofSize(6), order);

        assertEquals(6L, page1.numberOfElements());
        assertEquals(13L, page1.totalElements());
        assertEquals(3L, page1.totalPages());
        assertEquals(true, page1.hasNext());

        assertEquals(List.of("Benike Construction", "Cardinal", "Crenlo", "Custom Alarm", "Home Federal Savings Bank", "IBM"),
                     page1.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        Page<Business> page2 = mixed.locatedIn("Rochester", page1.nextPageRequest(), order);

        assertEquals(List.of("Mayo Clinic", "Metafile", "Olmsted Medical", "RAC", "Reichel Foods", "Silver Lake Foods"),
                     page2.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        assertEquals(true, page2.hasNext());

        Page<Business> page3 = mixed.locatedIn("Rochester", page2.nextPageRequest(), order);

        assertEquals(List.of("Think Bank"),
                     page3.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Test that a query with named parameters can be used for pagination
     * where the cursor is an IdClass value.
     */
    @Test
    public void testCursoredPagesWithNamedParametersAndIdClass() {
        PageRequest page1req = PageRequest
                        .ofSize(3)
                        .afterCursor(Cursor.forKey(CityId.of("Indianapolis",
                                                             "Indiana")));

        CursoredPage<City> page1 = cities.smallerThanOrNotNamed(100000,
                                                                "Springfield",
                                                                page1req);

        assertEquals(List.of("Kansas City in Kansas",
                             "Kansas City in Missouri",
                             "Rochester in Minnesota"),
                     page1.stream()
                                     .map(c -> c.name + " in " + c.stateName)
                                     .collect(Collectors.toList()));

        CursoredPage<City> page2 = cities.smallerThanOrNotNamed(100000,
                                                                "Springfield",
                                                                page1.nextPageRequest());

        assertEquals(List.of("Rochester in New York",
                             "Springfield in Ohio",
                             "Springfield in Oregon"),
                     page2.stream()
                                     .map(c -> c.name + " in " + c.stateName)
                                     .collect(Collectors.toList()));

        assertEquals(false, page2.hasNext());
    }

    /**
     * Demonstrates inconsistency and wrong behavior in how EclipseLink returns an
     * entity attribute that is an ElementCollection vs an entity attribute of the
     * same type that is not an ElementCollection. Note that the former is not
     * supported by Jakarta Persistence, and it would be fine if EclipseLink would
     * reject it, but EclipseLink should not be running the query and producing
     * wrong data.
     */
    @Test
    public void testElementCollection() throws Exception {
        ECEntity e1 = new ECEntity();
        e1.setId("EC1");
        e1.setIntArray(new int[] { 14, 12, 1 });
        e1.setLongList(new ArrayList<>(List.of(14L, 12L, 1L)));
        e1.setLongListEC(new ArrayList<>(List.of(14L, 12L, 1L)));
        e1.setStringSet(Set.of("fourteen", "twelve", "one"));
        e1.setStringSetEC(Set.of("fourteen", "twelve", "one"));
        ecRepo.insert(e1);

        ECEntity e2 = new ECEntity();
        e2.setId("EC2");
        e2.setIntArray(new int[] { 14, 12, 2 });
        e2.setLongList(new ArrayList<>(List.of(14L, 12L, 2L)));
        e2.setLongListEC(new ArrayList<>(List.of(14L, 12L, 2L)));
        e2.setStringSet(Set.of("fourteen", "twelve", "two"));
        e2.setStringSetEC(Set.of("fourteen", "twelve", "two"));
        ecRepo.insert(e2);

        try (EntityManager em = ecRepo.getEntityManager()) {
            String jpql;
            jakarta.persistence.Query q;
            List<?> results;

            jpql = "SELECT intArray FROM ECEntity WHERE id=?1";
            q = em.createQuery(jpql);
            q.setParameter(1, "EC1");
            results = q.getResultList();
            System.out.println();
            System.out.println(jpql);
            System.out.println("getResultList returned a " + results.getClass().getTypeName());
            System.out.println("    elements are of type " + results.iterator().next().getClass().getTypeName());
            // Vector of int[] needs special handling to print:
            StringBuilder s = new StringBuilder();
            boolean first = true;
            for (Object element : results) {
                if (first)
                    first = false;
                else
                    s.append(", ");
                if (element instanceof int[])
                    s.append(Arrays.toString((int[]) element));
                else
                    s.append(element);
            }
            System.out.println("            contents are [" + s.toString() + "]");

            jpql = "SELECT longList FROM ECEntity WHERE id=?1";
            q = em.createQuery(jpql);
            q.setParameter(1, "EC1");
            results = q.getResultList();
            System.out.println();
            System.out.println(jpql);
            System.out.println("getResultList returned a " + results.getClass().getTypeName());
            System.out.println("    elements are of type " + results.iterator().next().getClass().getTypeName());
            System.out.println("            contents are " + results);

            jpql = "SELECT longListEC FROM ECEntity WHERE id=?1";
            q = em.createQuery(jpql);
            q.setParameter(1, "EC1");
            results = q.getResultList();
            System.out.println();
            System.out.println(jpql);
            System.out.println("getResultList returned a " + results.getClass().getTypeName());
            System.out.println("    elements are of type " + results.iterator().next().getClass().getTypeName());
            System.out.println("            contents are " + results);

            jpql = "SELECT stringSet FROM ECEntity WHERE id=?1";
            q = em.createQuery(jpql);
            q.setParameter(1, "EC1");
            results = q.getResultList();
            System.out.println();
            System.out.println(jpql);
            System.out.println("getResultList returned a " + results.getClass().getTypeName());
            System.out.println("    elements are of type " + results.iterator().next().getClass().getTypeName());
            System.out.println("            contents are " + results);

            jpql = "SELECT stringSetEC FROM ECEntity WHERE id=?1";
            q = em.createQuery(jpql);
            q.setParameter(1, "EC1");
            results = q.getResultList();
            System.out.println();
            System.out.println(jpql);
            System.out.println("getResultList returned a " + results.getClass().getTypeName());
            System.out.println("    elements are of type " + results.iterator().next().getClass().getTypeName());
            System.out.println("            contents are " + results);

            // with multiple results (that ElementCollection wrongly combines into one!),

            jpql = "SELECT longList FROM ECEntity WHERE id LIKE ?1";
            q = em.createQuery(jpql);
            q.setParameter(1, "EC%");
            results = q.getResultList();
            System.out.println();
            System.out.println(jpql);
            System.out.println("getResultList returned a " + results.getClass().getTypeName());
            System.out.println("    elements are of type " + results.iterator().next().getClass().getTypeName());
            System.out.println("            contents are " + results);

            jpql = "SELECT longListEC FROM ECEntity WHERE id LIKE ?1";
            q = em.createQuery(jpql);
            q.setParameter(1, "EC%");
            results = q.getResultList();
            System.out.println();
            System.out.println(jpql);
            System.out.println("getResultList returned a " + results.getClass().getTypeName());
            System.out.println("    elements are of type " + results.iterator().next().getClass().getTypeName());
            System.out.println("            contents are " + results);

            jpql = "SELECT stringSet FROM ECEntity WHERE id LIKE ?1";
            q = em.createQuery(jpql);
            q.setParameter(1, "EC%");
            results = q.getResultList();
            System.out.println();
            System.out.println(jpql);
            System.out.println("getResultList returned a " + results.getClass().getTypeName());
            System.out.println("    elements are of type " + results.iterator().next().getClass().getTypeName());
            System.out.println("            contents are " + results);

            jpql = "SELECT stringSetEC FROM ECEntity WHERE id LIKE ?1";
            q = em.createQuery(jpql);
            q.setParameter(1, "EC%");
            results = q.getResultList();
            System.out.println();
            System.out.println(jpql);
            System.out.println("getResultList returned a " + results.getClass().getTypeName());
            System.out.println("    elements are of type " + results.iterator().next().getClass().getTypeName());
            System.out.println("            contents are " + results);
        }
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
     * Verify that escape characters can be used to correctly match results.
     * TODO PostgreSQL fails due to https://github.com/OpenLiberty/open-liberty/issues/30400
     */
    @Test
    /*
     * Even without providing an "ESCAPE '\'" clause to the SQL statement
     * PostgreSQL will always escape a '\' within a string literal.
     * See documentation here: https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-STRINGS-ESCAPE
     * Which indicates that you can set `standard_conforming_strings=on` (which is default)
     * but it seems the PostgreSQL JDBC driver always provides prepared strings as
     * escape string contents. So there is no work around.
     */
    @SkipIfSysProp(DB_Postgres)
    public void testEscapeCharacters() {
        orders.deleteAll();

        orders.create(PurchaseOrder.of(21.91f, "Escape_Characters"),
                      PurchaseOrder.of(22.92f, "Escape%Characters"),
                      PurchaseOrder.of(23.93f, "Escape\\Characters"),
                      PurchaseOrder.of(24.94f, "Escape_%Characters"),
                      PurchaseOrder.of(25.95f, "Escape\\_Characters"),
                      PurchaseOrder.of(26.96f, "Escape\\%Characters"),
                      PurchaseOrder.of(27.97f, "Escape\\\\Characters"),
                      PurchaseOrder.of(28.98f, "EscapeCharacters"));

        List<Float> found;

        // The % wildcard matches any number of characters (including 0)
        found = orders.purchaseTotalsFor("Escape%Characters");
        assertEquals(found.toString(), 8, found.size());

        // The _ wildcard matches any 1 character, so expect 3 results:
        found = orders.purchaseTotalsFor("Escape_Characters");
        assertEquals(found.toString(), 3, found.size());
        assertEquals(21.91f, found.get(0).floatValue(), 0.001f);
        assertEquals(22.92f, found.get(1).floatValue(), 0.001f);
        assertEquals(23.93f, found.get(2).floatValue(), 0.001f);

        // \ is NOT an escape character unless ESCAPE '\' is specified in the JPQL.
        // and in that case, it would only apply to the _ and % wildcard characters.
        // Nothing in JPA says that an escape character can escape itself.

        // Without ESCAPE '\':
        found = orders.purchaseTotalsFor("Escape\\\\Characters");
        assertEquals(found.toString(), 1, found.size());
        assertEquals(27.97f, found.get(0).floatValue(), 0.001f);

        // Without ESCAPE '\':
        found = orders.purchaseTotalsFor("Escape\\_Characters");
        assertEquals(found.toString(), 3, found.size());
        assertEquals(25.95f, found.get(0).floatValue(), 0.001f);
        assertEquals(26.96f, found.get(1).floatValue(), 0.001f);
        assertEquals(27.97f, found.get(2).floatValue(), 0.001f);

        // With ESCAPE '\':
        try (EntityManager em = orders.entityMgr()) {
            String queryWithEscape = "SELECT total FROM Orders" +
                                     " WHERE purchasedBy LIKE ?1 ESCAPE '\\'" +
                                     " ORDER BY total";
            TypedQuery<Float> query = em.createQuery(queryWithEscape, Float.class);

            // Escaped % character is not a wildcard
            query.setParameter(1, "Escape\\%Characters");
            found = query.getResultList();
            assertEquals(found.toString(), 1, found.size());
            assertEquals(22.92f, found.get(0).floatValue(), 0.001f);

            // Escaped _ character is not a wildcard
            query.setParameter(1, "Escape\\_Characters");
            found = query.getResultList();
            assertEquals(found.toString(), 1, found.size());
            assertEquals(21.91f, found.get(0).floatValue(), 0.001f);

            // matches \ followed by any one character
            query.setParameter(1, "Escape\\\\_Characters");
            found = query.getResultList();
            assertEquals(found.toString(), 3, found.size());
            assertEquals(25.95f, found.get(0).floatValue(), 0.001f);
            assertEquals(26.96f, found.get(1).floatValue(), 0.001f);
            assertEquals(27.97f, found.get(2).floatValue(), 0.001f);

            // matches \_ where _ is not a wildcard.
            query.setParameter(1, "Escape\\\\\\_Characters");
            found = query.getResultList();
            assertEquals(found.toString(), 1, found.size());
            assertEquals(25.95f, found.get(0).floatValue(), 0.001f);
        }

        orders.deleteAll();
    }

    /**
     * Query-by-method name repository operation to remove and return one or more entities
     * where the entity has an IdClass.
     */
    @Test
    public void testFindAndDeleteEntityThatHasAnIdClass() {
        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
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

        Stream<City> stream = supportsOrderByForUpdate //
                        ? cities.removeByStateNameOrderByName("Wisconsin").stream() //
                        : cities.removeByStateName("Wisconsin").stream();

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
        Iterator<CityId> ids = cities.deleteByStateName("South Dakota", Limit.of(3), orderByCityName).iterator();
        CityId id;

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.getStateName());
        if (supportsOrderByForUpdate)
            assertEquals("Aberdeen", id.name);
        // else order is unknown, but at least must be one of the city names that we added and haven't removed yet
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.getStateName());
        if (supportsOrderByForUpdate)
            assertEquals("Brookings", id.name);
        // else order is unknown, but at least must be one of the city names that we added and haven't removed yet
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.getStateName());
        if (supportsOrderByForUpdate)
            assertEquals("Mitchell", id.name);
        // else order is unknown, but at least must be one of the city names that we added and haven't removed yet
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(false, ids.hasNext());

        Order<City> orderByPopulation = supportsOrderByForUpdate ? Order.by(Sort.asc("population")) : Order.by();
        id = cities.deleteAtMost1ByStateName("South Dakota", Limit.of(1), orderByPopulation).orElseThrow();
        assertEquals("South Dakota", id.getStateName());
        if (supportsOrderByForUpdate)
            assertEquals("Spearfish", id.name);
        // else order is unknown, but at least must be one of the city names that we added and haven't removed yet
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        id = cities.delete1ByStateName("South Dakota", Limit.of(1));
        assertEquals("South Dakota", id.getStateName());
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        List<CityId> some = cities.deleteSome("South Dakota", Limit.of(2));
        ids = some.iterator();

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.getStateName());
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.getStateName());
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(false, ids.hasNext());

        // Should only have 1 left:
        some = cities.deleteSome("South Dakota", Limit.of(5));
        ids = some.iterator();

        assertEquals(true, ids.hasNext());
        id = ids.next();
        assertEquals("South Dakota", id.getStateName());
        assertEquals("Found " + id, true, cityNames.remove(id.name));

        assertEquals(false, ids.hasNext());
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
        assertEquals("Iowa", id.getStateName());

        id = ids.next();
        assertEquals("Iowa City", id.name);
        assertEquals("Iowa", id.getStateName());

        id = ids.next();
        assertEquals("Sioux City", id.name);
        assertEquals("Iowa", id.getStateName());

        removed = cities.deleteByStateName("Iowa");

        assertEquals(removed.toString(), 0, removed.size());

        // Ensure non-matching entities remain in the database
        assertEquals(true, cities.existsByNameAndStateName("Rochester", "Minnesota"));
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
                          shippingAddresses.findByStreetAddress_streetNameOrderByStreetAddress_houseNumber("4th St SE"),
                          Comparator.<ShippingAddress, Long> comparing(o -> o.id)
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.city))
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.state))
                                          .thenComparing(Comparator.<ShippingAddress, Integer> comparing(o -> o.streetAddress.houseNumber))
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.streetAddress.streetName))
                                          .thenComparing(Comparator.<ShippingAddress, Integer> comparing(o -> o.zipCode)));

        assertIterableEquals(List.of("200 1st Ave SW", "151 4th St SE", "201 4th St SE"),
                             Stream.of(shippingAddresses.findByStreetAddress_houseNumberBetweenOrderByStreetAddress_streetNameAscStreetAddress_houseNumber(150, 250))
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

        // assertEquals(3L, shippingAddresses.countByStreetAddressRecipientInfoEmpty());

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

        List<Set<AccountId>> list;
        try {
            list = taxpayers.findBankAccountsByFilingStatus(TaxPayer.FilingStatus.HeadOfHousehold);
            assertEquals(list.toString(), 2, list.size());
            assertEquals(Set.of("AccountId:43014400:410224"),
                         list.get(0)
                                         .stream()
                                         .map(AccountId::toString)
                                         .collect(Collectors.toSet()));
            assertEquals(Set.of("AccountId:10105600:560237",
                                "AccountId:15561600:391588"),
                         list.get(1)
                                         .stream()
                                         .map(AccountId::toString)
                                         .collect(Collectors.toSet()));
        } catch (UnsupportedOperationException x) {
            if (x.getMessage() != null &&
                x.getMessage().startsWith("CWWKD1103E:"))
                // Works around bad behavior from EclipseLink (see #30575)
                // for ElementCollection:
                // SELECT o.bankAccounts FROM TaxPayer o WHERE (o.filingStatus=?1)
                //  ORDER BY o.numDependents, o.ssn
                // combines the two Set<AccountId> values that ought to be the result
                // into a single combined list of AccountId.
                ;
            else
                throw x;
        }

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
                             businesses.findByLocationLatitudeBetweenOrderByLocationLongitudeDesc(44.0f, 44.03f)
                                             .stream()
                                             .map(b -> b.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method that queries and sorts by an embeddable attribute of an embeddable attribute of an entity.
     */
    @Test
    public void testEmbeddableDepth2() {
        CursoredPage<Business> page;
        List<Integer> zipCodes = List.of(55906, 55902, 55901, 55976, 55905);

        page = businesses.findByLocationAddressZipIn(zipCodes, PageRequest.ofSize(4).withoutTotal());

        assertIterableEquals(List.of(345, 1421, 1016, 1600),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        page = businesses.findByLocationAddressZipIn(zipCodes, page.nextPageRequest());

        assertIterableEquals(List.of(2800, 2960, 3100, 3428),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(2L, page.pageRequest().page());
        assertEquals(4, page.pageRequest().size());

        page = businesses.findByLocationAddressZipIn(zipCodes, page.nextPageRequest());

        assertIterableEquals(List.of(5201, 1661, 3706, 200),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(3, page.pageRequest().page());

        page = businesses.findByLocationAddressZipIn(zipCodes, page.nextPageRequest());

        assertIterableEquals(List.of(1402, 3008),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(2, page.numberOfElements());
        assertEquals(4, page.pageRequest().page());
        assertEquals(false, page.hasNext());

        page = businesses.findByLocationAddressZipIn(zipCodes, page.previousPageRequest());

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

        assertIterableEquals(List.of("Custom Alarm", "Mayo Clinic", "Reichel Foods"),
                             businesses.onSouthSideOf("Rochester", "MN", "SW")
                                             .stream()
                                             .map(b -> b.name)
                                             .collect(Collectors.toList()));
    }

    /**
     * Use an entity with embeddable attributes that are Java records.
     */
    @Test
    public void testEmbeddableRecord() {
        Segment s1 = new Segment();
        s1.pointA = new Point(0, 0);
        s1.pointB = new Point(120, 209); // length 241
        s1 = segments.addOrModify(s1);

        Segment s2 = new Segment();
        s2.pointA = new Point(-20, 0);
        s2.pointB = new Point(120, 171); // length 221
        s2 = segments.addOrModify(s2);

        Segment s3 = new Segment();
        s3.pointA = new Point(24, 7);
        s3.pointB = new Point(180, 140); // length 205
        s3 = segments.addOrModify(s3);

        Segment s4 = new Segment();
        s4.pointA = new Point(12, 45);
        s4.pointB = new Point(180, 140); // length 193
        s4 = segments.addOrModify(s4);

        Segment s5 = new Segment();
        s5.pointA = new Point(4, 3);
        s5.pointB = new Point(180, 60); // length 185
        s5 = segments.addOrModify(s5);

        Segment s6 = new Segment();
        s6.pointA = new Point(0, 41);
        s6.pointB = new Point(180, 60); // length 181
        s6 = segments.addOrModify(s6);

        assertEquals(3, segments.countByPointAXLessThan(1));

        // TODO enable once #29460 is fixed
        //assertEquals(List.of(s3.id, s4.id, s2.id, s1.id),
        //             segments.endingSouthOf(100)
        //                             .map(s -> s.id)
        //                             .collect(Collectors.toList()));

        //assertEquals(List.of(-20, 0, 24),
        //             segments.longerThan(200, Sort.asc("pointA.x"))
        //                             .stream()
        //                             .map(s -> s.pointA.x())
        //                             .collect(Collectors.toList()));

        //s3.pointB = new Point(s3.pointB.x() - s3.pointA.x(), s3.pointB.y() - s3.pointA.y());
        //s3.pointA = new Point(0, 0);
        //s3 = segments.addOrModify(s3);

        // removes s1 and s3
        //assertEquals(2L, segments.removeStartingAt(0, 0));

        //Point s2pointB = segments.terminalPoint(s2.id).orElseThrow();
        //assertEquals(120, s2pointB.x());
        //assertEquals(171, s2pointB.y());

        assertEquals(6L, // TODO change to 4L, once #29460 is fixed
                     segments.erase());
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
                             businesses.findByLocationAddressZip(55901)
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
                             businesses.findByLocationAddressZipNotAndLocationAddressCity(55901, "Rochester")
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
                             accounts.findByAccountIdNotNull()
                                             .map(a -> a.accountId.toString())
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("AccountId:1004470:70081",
                                     "AccountId:1005380:70081",
                                     "AccountId:1006380:70081",
                                     "AccountId:1007590:70081"),
                             accounts.findByBankName("Think Bank")
                                             .map(AccountId::toString)
                                             .collect(Collectors.toList()));

        assertEquals(Collections.EMPTY_LIST, accounts.findByAccountIdEmpty());

        try {
            System.out.println("findByIdBetween: " + accounts.findByAccountIdBetween(AccountId.of(1006380, 22158), AccountId.of(1008200, 30372)));
        } catch (MappingException x) {
            // expected
        }

        try {
            System.out.println("findByIdGreaterThan: " + accounts.findByAccountIdGreaterThan(AccountId.of(1008200, 30372)));
        } catch (MappingException x) {
            // expected
        }

        // Varies by database whether MappingException or DatabaseException is raised,
        // System.out.println("findByIdInOrOwner: " + accounts.findByIdInOrOwner(List.of(AccountId.of(1004470, 30372),
        //                                                                               AccountId.of(1006380, 22158)),
        //                                                                       "Emma TestEmbeddedId"));

        try {
            System.out.println("findByIdTrue: " + accounts.findByAccountIdTrue());
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
     * Also tests compatibility with Converters using OffsetDateTimeToStringConverter
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
        o3 = orders.insert(o3);

        assertNotNull(o3.id);
        assertEquals("testEntitiesAsParameters-Customer3", o3.purchasedBy);
        assertEquals(30.99f, o3.total, 0.001f);
        int o3_v1 = o3.versionNum;

        o3 = orders.findFirstByPurchasedBy("testEntitiesAsParameters-Customer3").orElseThrow();
        assertEquals(o3_v1, o3.versionNum);

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

        // delete even though an entity attribute doesn't match
        o4.total = 44.99f;
        orders.delete(o4);

        // cannot delete when the version number doesn't match
        o1 = orders.findById(o1.id).orElseThrow();
        UUID o1id = o1.id;

        // Update on another thread:
        CompletableFuture.supplyAsync(() -> {
            PurchaseOrder o1updated = orders.findById(o1id).orElseThrow();
            o1updated.total = 11.99f;
            return orders.save(o1updated);
        }).get(2, TimeUnit.MINUTES);

        tran.begin();
        try {
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

        // TODO SQLServer throws com.microsoft.sqlserver.jdbc.SQLServerException: Violation of PRIMARY KEY constraint ...
        // which is not a subset of SQLIntegrityConstraintViolationException
        // we are not correctly parsing this exception to re-throw as EntityExistsException
        // Related issue: https://github.com/microsoft/mssql-jdbc/issues/1199
        // SQLServer JDBC Jar Name : mssql-jdbc.jar
        // TODO PostgreSQL throws org.postgresql.util.PSQLException:
        // ERROR: duplicate key value violates unique constraint "purchaseorder_pkey"
        // Detail: Key (id)=(c2383324-d0a1-4d71-be8c-b2fda27f1701) already exists.
        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
        if (!jdbcJarName.startsWith("mssql-jdbc") &&
            !jdbcJarName.startsWith("postgresql")) {
            try {

                orders.insertAll(List.of(o7, o5));
                fail("Should not be able insert an entity with an Id that is already present.");
            } catch (EntityExistsException x) {
                // expected
            }
        }

        assertEquals(false, orders.findFirstByPurchasedBy("testEntitiesAsParameters-Customer7").isPresent());

        PurchaseOrder o8 = new PurchaseOrder();
        o8.purchasedBy = "testEntitiesAsParameters-Customer8";
        o8.purchasedOn = OffsetDateTime.now();
        o8.total = 80.99f;

        List<PurchaseOrder> inserted = orders.insertAll(List.of(o7, o8));

        assertEquals(2, inserted.size());
        assertNotNull(o7 = inserted.get(0));
        assertNotNull(o7.id);
        assertEquals("testEntitiesAsParameters-Customer7", o7.purchasedBy);
        assertEquals(70.99f, o7.total, 0.001f);
        int o7_v1 = o7.versionNum;
        assertNotNull(o8 = inserted.get(1));
        assertNotNull(o8.id);
        assertEquals("testEntitiesAsParameters-Customer8", o8.purchasedBy);
        assertEquals(80.99f, o8.total, 0.001f);
        int o8_v1 = o8.versionNum;

        o7 = orders.findFirstByPurchasedBy("testEntitiesAsParameters-Customer7").orElseThrow();
        o8 = orders.findFirstByPurchasedBy("testEntitiesAsParameters-Customer8").orElseThrow();

        assertEquals(o7_v1, o7.versionNum);
        assertEquals(o8_v1, o8.versionNum);

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

        Iterable<PurchaseOrder> updates = orders.updateAll(List.of(o8, o7));

        PurchaseOrder updated;
        Iterator<PurchaseOrder> updatesIt = updates.iterator();
        assertEquals(true, updatesIt.hasNext());
        updated = updatesIt.next();
        assertEquals("testEntitiesAsParameters-Customer8", updated.purchasedBy);
        assertEquals(88.99f, updated.total, 0.001f);
        assertEquals(true, updatesIt.hasNext());
        updated = updatesIt.next();
        assertEquals("testEntitiesAsParameters-Customer7", updated.purchasedBy);
        assertEquals(77.99f, updated.total, 0.001f);
        assertEquals(false, updatesIt.hasNext());

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

        updated = orders.update(o1);

        assertEquals("testEntitiesAsParameters-Customer1", updated.purchasedBy);
        assertEquals(0.99f, updated.total, 0.001f);

        totals = orders.findTotalByPurchasedByIn(Set.of("testEntitiesAsParameters-Customer1"));
        assertEquals(totals.toString(), 1, totals.size());
        assertEquals(0.99f, totals.get(0), 0.001f);

        orders.deleteAll();
    }

    /**
     * Use a repository method that uses query language to perform an exists query
     * that returns a boolean true/false value.
     */
    @Test
    public void testExistsViaQueryLanguage() {
        assertEquals(true, businesses.isLocatedAt(2800, "37th St", "NW", "IBM"));
        assertEquals(false, businesses.isLocatedAt(200, "1st St", "SW", "IBM"));
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
    @OnlyIfSysProp(DB_Not_Default) // Derby doesn't support a WEEK function in SQL
    @Test
    public void testExtractWeekFromDateFunction() {
        // WithWeek
        List<CreditCard> results = creditCards.expiringInWeek(15);

        assertEquals(1, results.size());
        assertEquals(4000921041110001L, results.get(0).number);
    }

    /**
     * Verify WithWeek in query-by-method-name to compare the week-of-year part of a date.
     */
    @OnlyIfSysProp(DB_Not_Default) // Derby doesn't support a WEEK function in SQL
    @Test
    public void testExtractWeekFromDateKeyword() {
        // WithWeek
        List<CreditCard> results = creditCards.findByExpiresOnWithWeek(17);

        assertEquals(1, results.size());
        assertEquals(4000921042220002L, results.get(0).number);
    }

    /**
     * Verify that fetch type eager and lazy both work when using a detached entity returned by Jakarta Data
     */
    @Test
    public void testFetchType() {
        mobilePhones.removeAll();

        List<String> apps = Arrays.asList("Settings", "Camera", "Phone", "Email", "Messages",
                                          "UnoLingo", "BankApp", "SoloGame", "LocalNews");

        List<String> emails = Arrays.asList("john.smith@example.com", "JohnDSmith@example.work.com");

        // Populate database
        UUID id = mobilePhones.insert(Mobile.of(OS.ANDROID, apps, emails)).deviceId;

        // Outside of transaction, returned entity should be detached
        Mobile johnsMobile = mobilePhones.findById(id).orElseThrow();

        // Fetch type lazy should be populated when accessing apps field
        assertFalse("Expected apps to be populated when using fetch type lazy", johnsMobile.apps.isEmpty());
        assertEquals("Entity apps did not match expected apps", apps, johnsMobile.apps);

        // Fetch type eager emails field should be pre-populated
        assertFalse("Expected emails to be populated when using fetch type eager", johnsMobile.emails.isEmpty());
        assertEquals("Entity emails did not match expected apps", emails, johnsMobile.emails);

    }

    /**
     * Reproduces issue 27925.
     */
    @Test
    public void testForeignKey() {
        Manufacturer toyota = new Manufacturer();
        toyota.setName("Toyota");
        toyota.setNotes("testForeignKey-1");

        Model camry = new Model();
        camry.setName("Camry");
        camry.setYearIntroduced(1983);
        toyota.addModel(camry);

        Model corolla = new Model();
        corolla.setName("Corolla");
        corolla.setYearIntroduced(1966);
        toyota.addModel(corolla);

        Iterator<Model> saved = models.saveAll(List.of(camry, corolla)).iterator();

        assertEquals(true, saved.hasNext());
        UUID camryId = saved.next().getId();

        assertEquals(true, saved.hasNext());
        UUID corollaId = saved.next().getId();

        assertEquals(false, saved.hasNext());

        camry = models.findById(camryId).orElseThrow();

        assertEquals("Camry", camry.getName());
        assertEquals(Integer.valueOf(1983), camry.getYearIntroduced());
        assertEquals("Toyota", camry.getManufacturer().getName());

        Instant corollaLastMod;
        corollaLastMod = models.lastModified(corollaId).orElseThrow();
        List<Model> found = models.modifiedAt(corollaLastMod);
        assertEquals(false, found.isEmpty());
        corolla = null;
        for (Model model : found)
            if ("Corolla".equals(model.getName()))
                corolla = model;
        assertNotNull(corolla);
        assertEquals(Integer.valueOf(1966), corolla.getYearIntroduced());
        assertEquals("Toyota", corolla.getManufacturer().getName());

        models.deleteById(corollaId);
        assertEquals(false, models.findById(corollaId).isPresent());

        models.delete(camry);
        assertEquals(false, models.findById(camryId).isPresent());

        manufacturers.delete(toyota);
    }

    /**
     * Use a repository method with JDQL query language that includes only the FROM and ORDER BY clauses.
     */
    @Test
    public void testFromAndOrderByClausesOnly() {
        List<City> list = mixed.all(); // ORDER BY name DESC, stateName ASC

        assertEquals(List.of("Springfield:Illinois",
                             "Springfield:Massachusetts",
                             "Springfield:Missouri",
                             "Springfield:Ohio",
                             "Springfield:Oregon",
                             "Rochester:Minnesota",
                             "Rochester:New York",
                             "Kansas City:Kansas",
                             "Kansas City:Missouri"),
                     list.stream()
                                     .map(c -> c.name + ":" + c.stateName)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method with query language that includes only the
     * FROM and ORDER BY clauses and returns a Page. Verify the page count
     * and the total count of matching entities.
     */
    @Test
    public void testFromAndOrderByClausesOnlyWithPageCount() {

        Page<Business> page1 = businesses.sorted(PageRequest.ofSize(5));

        assertEquals(3l, page1.totalPages());
        assertEquals(15l, page1.totalElements());

        assertEquals(List.of("RAC",
                             "IBM",
                             "Crenlo",
                             "Home Federal Savings Bank",
                             "Benike Construction"),
                     page1.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        Page<Business> page2 = businesses.sorted(page1.nextPageRequest());

        assertEquals(List.of("Metafile",
                             "Think Bank",
                             "Reichel Foods",
                             "Custom Alarm",
                             "Olmsted Medical"),
                     page2.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        Page<Business> page3 = businesses.sorted(page2.nextPageRequest());

        assertEquals(List.of("Mayo Clinic",
                             "Silver Lake Foods",
                             "Cardinal",
                             "Geotek",
                             "HALCON"),
                     page3.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        assertEquals(false, page3.hasNext());
    }

    /**
     * Use a repository method with query language that includes only the
     * FROM and WHERE and ORDER BY clauses and returns a Page. Verify the
     * page count and the total count of matching entities.
     */
    @Test
    public void testFromAndWhereAndOrderByClausesOnly() {

        Page<Business> page1 = businesses
                        .withStreetDirection("NW", PageRequest.ofSize(4));

        assertEquals(8l, page1.totalElements());
        assertEquals(2l, page1.totalPages());

        assertEquals(List.of("Think Bank",
                             "Metafile",
                             "RAC",
                             "IBM"),
                     page1.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        Page<Business> page2 = businesses
                        .withStreetDirection("NW", page1.nextPageRequest());

        assertEquals(List.of("Crenlo",
                             "Geotek",
                             "Home Federal Savings Bank",
                             "HALCON"),
                     page2.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        assertEquals(false, page2.hasNext());
    }

    /**
     * Use a repository method with JDQL query language that includes only the FROM and WHERE clauses.
     * Omit the count query and let it be inferred from the main query.
     */
    @Test
    public void testFromAndWhereClausesOnly() {
        CursoredPage<Business> page1 = mixed.locatedIn("Rochester", "MN", PageRequest.ofSize(4), Order.by(Sort.asc("name")));

        assertEquals(4L, page1.numberOfElements());
        assertEquals(13L, page1.totalElements());
        assertEquals(4L, page1.totalPages());
        assertEquals(true, page1.hasNext());

        assertEquals(List.of("Benike Construction", "Cardinal", "Crenlo", "Custom Alarm"),
                     page1.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        CursoredPage<Business> page2 = mixed.locatedIn("Rochester", "MN", page1.nextPageRequest(), Order.by(Sort.asc("name")));

        assertEquals(List.of("Home Federal Savings Bank", "IBM", "Mayo Clinic", "Metafile"),
                     page2.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        assertEquals(true, page2.hasNext());

        CursoredPage<Business> page3 = mixed.locatedIn("Rochester", "MN", page2.nextPageRequest(), Order.by(Sort.asc("name")));

        assertEquals(List.of("Olmsted Medical", "RAC", "Reichel Foods", "Silver Lake Foods"),
                     page3.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        assertEquals(true, page3.hasNext());

        CursoredPage<Business> page4 = mixed.locatedIn("Rochester", "MN", page3.nextPageRequest(), Order.by(Sort.asc("name")));

        assertEquals(List.of("Think Bank"),
                     page4.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method with JDQL query language that includes only the FROM clause.
     */
    @Test
    public void testFromClauseOnly() {
        List<City> list = mixed.all(Order.by(Sort.asc("stateName"),
                                             Sort.desc("name")));

        assertEquals(List.of("Illinois:Springfield",
                             "Kansas:Kansas City",
                             "Massachusetts:Springfield",
                             "Minnesota:Rochester",
                             "Missouri:Springfield",
                             "Missouri:Kansas City",
                             "New York:Rochester",
                             "Ohio:Springfield",
                             "Oregon:Springfield"),
                     list.stream()
                                     .map(c -> c.stateName + ":" + c.name)
                                     .collect(Collectors.toList()));
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
     * Repository method with the Exists keyword that checks if any matching entities exist.
     */
    @Test
    public void testIdClassExistsKeyword() {
        assertEquals(true, cities.existsByNameAndStateName("Kansas City", "Kansas"));
        assertEquals(false, cities.existsByNameAndStateName("Kansas City", "Minnesota"));
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
     * Use cursor-based pagination with the OrderBy annotation on a composite id
     * that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderByAnnotationWithCursorPagination() {
        PageRequest pagination = PageRequest
                        .ofSize(3)
                        .withoutTotal()
                        .afterCursor(Cursor.forKey(CityId.of("Rochester", "Minnesota")));

        CursoredPage<City> slice1 = cities.findByStateNameNotEndsWith("o", pagination);
        assertIterableEquals(List.of("Rochester New York",
                                     "Springfield Illinois",
                                     "Springfield Massachusetts"),
                             slice1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        CursoredPage<City> slice2 = cities.findByStateNameNotEndsWith("o", slice1.nextPageRequest());
        assertIterableEquals(List.of("Springfield Missouri",
                                     "Springfield Oregon"),
                             slice2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(false, slice2.hasNext());

        CursoredPage<City> slice0 = cities.findByStateNameNotEndsWith("o", slice1.previousPageRequest());
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota"),
                             slice0.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(false, slice0.hasPrevious());
    }

    /**
     * Use cursor-based pagination with the OrderBy query-by-method pattern on a
     * composite id that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderByNamePatternWithCursorPagination() {
        PageRequest pagination = PageRequest.ofSize(5).withoutTotal();

        CursoredPage<City> slice1 = cities.findByStateNameNotNull(pagination, Order.by());
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota",
                                     "Rochester New York",
                                     "Springfield Illinois"),
                             slice1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        CursoredPage<City> slice2 = cities.findByStateNameNotNull(slice1.nextPageRequest(), Order.by());
        assertIterableEquals(List.of("Springfield Massachusetts",
                                     "Springfield Missouri",
                                     "Springfield Ohio",
                                     "Springfield Oregon"),
                             slice2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(false, slice2.hasNext());

        Cursor springfieldMO = slice2.cursor(1);
        pagination = pagination.size(3).beforeCursor(springfieldMO);

        CursoredPage<City> beforeSpringfieldMO = cities.findByStateNameNotNull(pagination, Order.by());
        assertIterableEquals(List.of("Rochester New York",
                                     "Springfield Illinois",
                                     "Springfield Massachusetts"),
                             beforeSpringfieldMO.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        CursoredPage<City> beforeRochesterNY = cities.findByStateNameNotNull(beforeSpringfieldMO.previousPageRequest(),
                                                                             Order.by());
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota"),
                             beforeRochesterNY.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(false, beforeRochesterNY.hasPrevious());
    }

    /**
     * Use cursor-based pagination with the OrderBy query-by-method pattern in
     * descending direction on a composite id that is defined by an IdClass
     * attribute.
     */
    @Test
    public void testIdClassOrderByNamePatternWithCursorPaginationDescending() {
        PageRequest pagination = PageRequest.ofSize(3)
                        .withTotal()
                        .afterCursor(Cursor.forKey(CityId.of("Springfield", "Tennessee")));

        CursoredPage<City> page1 = cities.findByStateNameNotStartsWith("Ma", pagination);
        assertIterableEquals(List.of("Springfield Oregon",
                                     "Springfield Ohio",
                                     "Springfield Missouri"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        CursoredPage<City> page2 = cities.findByStateNameNotStartsWith("Ma", page1.nextPageRequest());
        assertIterableEquals(List.of("Springfield Illinois",
                                     "Rochester New York",
                                     "Rochester Minnesota"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        CursoredPage<City> page3 = cities.findByStateNameNotStartsWith("Ma", page2.nextPageRequest());
        assertIterableEquals(List.of("Kansas City Missouri",
                                     "Kansas City Kansas"),
                             page3.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(false, page3.hasNext());

        assertEquals(true, page3.hasPrevious());
        page2 = cities.findByStateNameNotStartsWith("Ma", page3.previousPageRequest());
        assertIterableEquals(List.of("Springfield Illinois",
                                     "Rochester New York",
                                     "Rochester Minnesota"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));
    }

    /**
     * Use cursor-based pagination with the pagination sort criteria on a
     * composite id that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderByPaginationWithCursor() {
        // ascending:
        Order<City> asc = Order.by(Sort.asc(ID));
        PageRequest pagination = PageRequest.ofSize(5);

        CursoredPage<City> page1 = cities.findByStateNameGreaterThan("Iowa", pagination, asc);
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota",
                                     "Rochester New York",
                                     "Springfield Massachusetts"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        CursoredPage<City> page2 = cities.findByStateNameGreaterThan("Iowa", page1.nextPageRequest(), asc);
        assertIterableEquals(List.of("Springfield Missouri",
                                     "Springfield Ohio",
                                     "Springfield Oregon"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(false, page2.hasNext());

        // descending:
        Order<City> desc = Order.by(Sort.descIgnoreCase(ID));
        pagination = PageRequest.ofSize(4);
        page1 = cities.findByStateNameGreaterThan("Idaho", pagination, desc);
        assertIterableEquals(List.of("Springfield Oregon",
                                     "Springfield Ohio",
                                     "Springfield Missouri",
                                     "Springfield Massachusetts"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        page2 = cities.findByStateNameGreaterThan("Idaho", page1.nextPageRequest(), desc);
        assertIterableEquals(List.of("Springfield Illinois",
                                     "Rochester New York",
                                     "Rochester Minnesota",
                                     "Kansas City Missouri"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        CursoredPage<City> page3 = cities.findByStateNameGreaterThan("Idaho", page2.nextPageRequest(), desc);
        assertIterableEquals(List.of("Kansas City Kansas"),
                             page3.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(false, page3.hasNext());
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
                             cities.findByStateNameLessThan("Ohio", Sort.desc(ID))
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
        assertEquals("Missouri", cityId.getStateName());

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
     * Repository methods for an entity that has an id attribute that is not the unique identifier.
     * In this case, the id value is computed as firstName + " " + lastName and is different from
     * empNum, which is the unique identifier.
     */
    @Test
    public void testIdThatIsNotTheUniqueIdentifier() {
        // Clear out data before test
        employees.deleteByLastName("testIdThatIsNotTheUniqueIdentifier");

        Stream<Employee> added = businesses.save(new Employee(1002636, "Irene", "testIdThatIsNotTheUniqueIdentifier", (short) 2636, 'A'),
                                                 new Employee(1008171, "Isabella", "testIdThatIsNotTheUniqueIdentifier", (short) 8171, 'B'),
                                                 new Employee(1004948, "Ivan", "testIdThatIsNotTheUniqueIdentifier", (short) 4948, 'A'),
                                                 new Employee(1005310, "Isaac", "testIdThatIsNotTheUniqueIdentifier", (short) 5310, 'C'));

        assertEquals(List.of("Irene", "Isabella", "Ivan", "Isaac"),
                     added.map(e -> e.firstName).collect(Collectors.toList()));

        Employee emp4948 = employees.findByEmpNum(1004948);
        assertEquals(1004948, emp4948.empNum);
        assertEquals("Ivan", emp4948.firstName);
        assertEquals("testIdThatIsNotTheUniqueIdentifier", emp4948.lastName);
        assertEquals((short) 4948, emp4948.badge.number);
        assertEquals('A', emp4948.badge.accessLevel);

        assertEquals("Irene", employees.findByBadgeNumber(2636).firstName);

        assertIterableEquals(List.of((short) 4948, (short) 5310, (short) 8171),
                             employees.findByFirstNameLike("I_a%")
                                             .map(emp -> emp.badge.number)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of((short) 8171, (short) 5310, (short) 4948, (short) 2636),
                             employees.findByFirstNameStartsWithOrderByEmpNumDesc("I")
                                             .stream()
                                             .map(emp -> emp.badge.number)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("Badge#2636 Level A", "Badge#4948 Level A", "Badge#5310 Level C", "Badge#8171 Level B"),
                             employees.findByLastName("testIdThatIsNotTheUniqueIdentifier")
                                             .map(Badge::toString)
                                             .collect(Collectors.toList()));

        // Use @OrderBy to sort by the id attribute which is not a unique identifier:
        assertEquals(List.of("Irene", "Isaac", "Isabella", "Ivan"),
                     employees.findByFirstNameStartsWith("I")
                                     .map(e -> e.firstName)
                                     .collect(Collectors.toList()));

        // Use the OrderBy keyword to sort by the id attribute which is not a unique identifier:
        assertEquals(List.of("Ivan", "Isabella", "Isaac", "Irene"),
                     employees.findByFirstNameStartsWithOrderByIdDesc("I")
                                     .map(e -> e.firstName)
                                     .collect(Collectors.toList()));

        Optional<Employee> found = employees.withId("Ivan testIdThatIsNotTheUniqueIdentifier");
        assertEquals(true, found.isPresent());
        assertEquals(1004948, found.get().empNum);

        employees.deleteByLastName("testIdThatIsNotTheUniqueIdentifier");

        assertEquals(false, employees.withId("Ivan testIdThatIsNotTheUniqueIdentifier").isPresent());
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

        WorkAddress[] secondFloorOfficesOn37th = shippingAddresses.findByStreetAddress_streetNameAndFloorNumber("37th St NW", 2);

        assertArrayEquals(new WorkAddress[] { work }, secondFloorOfficesOn37th,
                          Comparator.<WorkAddress, Long> comparing(o -> o.id)
                                          .thenComparing(Comparator.<WorkAddress, String> comparing(o -> o.city))
                                          .thenComparing(Comparator.<WorkAddress, Integer> comparing(o -> o.floorNumber))
                                          .thenComparing(Comparator.<WorkAddress, String> comparing(o -> o.office))
                                          .thenComparing(Comparator.<WorkAddress, String> comparing(o -> o.state))
                                          .thenComparing(Comparator.<WorkAddress, String> comparing(o -> o.streetAddress.streetName))
                                          .thenComparing(Comparator.<WorkAddress, Integer> comparing(o -> o.streetAddress.houseNumber))
                                          .thenComparing(Comparator.<WorkAddress, Integer> comparing(o -> o.zipCode)));

        ShippingAddress[] found = shippingAddresses.findByStreetAddress_streetNameOrderByStreetAddress_houseNumber("37th St NW");

        assertArrayEquals(new ShippingAddress[] { work }, found,
                          Comparator.<ShippingAddress, Long> comparing(o -> o.id)
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.city))
                                          .thenComparing(Comparator.<ShippingAddress, Integer> comparing(o -> ((WorkAddress) o).floorNumber))
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> ((WorkAddress) o).office))
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.state))
                                          .thenComparing(Comparator.<ShippingAddress, String> comparing(o -> o.streetAddress.streetName))
                                          .thenComparing(Comparator.<ShippingAddress, Integer> comparing(o -> o.streetAddress.houseNumber))
                                          .thenComparing(Comparator.<ShippingAddress, Integer> comparing(o -> o.zipCode)));

        StreetAddress[] streetAddresses = shippingAddresses.findByStreetAddress_houseNumberBetweenOrderByStreetAddress_streetNameAscStreetAddress_houseNumber(1000, 3000);

        assertArrayEquals(new StreetAddress[] { work.streetAddress, home.streetAddress }, streetAddresses,
                          Comparator.<StreetAddress, Integer> comparing(o -> o.houseNumber)
                                          .thenComparing(Comparator.<StreetAddress, String> comparing(o -> o.streetName)));

        shippingAddresses.removeAll();
    }

    /**
     * Repository method that queries by an Instant attribute and retrieves an
     * entity that includes the Instant attribute.
     */
    @Test
    public void testInstant() {
        final ZoneId EASTERN = ZoneId.of("America/New_York");
        final Instant apr_28_2023 = ZonedDateTime.of(2023, 4, 28,
                                                     12, 0, 0, 0,
                                                     EASTERN)
                        .toInstant();

        DemographicInfo info = demographics.read(apr_28_2023).orElseThrow();

        assertEquals(apr_28_2023,
                     info.collectedOn);

        assertEquals(134060000L,
                     info.numFullTimeWorkers.longValue());

        assertEquals(6852746625849.0,
                     info.intragovernmentalDebt.doubleValue(),
                     1.0);

        assertEquals(24605068022567.0,
                     info.publicDebt.doubleValue(),
                     1.0);
    }

    /**
     * Repository method that queries by the year component of an Instant attribute.
     */
    @Test
    public void testInstantExtractYear() {

        assertEquals(30189.32,
                     demographics.publicDebtPerFullTimeWorker(2002)
                                     .orElseThrow()
                                     .doubleValue(),
                     0.01);

        assertEquals(102683.76,
                     demographics.publicDebtPerFullTimeWorker(2013)
                                     .orElseThrow()
                                     .doubleValue(),
                     0.01);

        assertEquals(205374.53,
                     demographics.publicDebtPerFullTimeWorker(2024)
                                     .orElseThrow()
                                     .doubleValue(),
                     0.01);
    }

    /**
     * Use a repository method with a Query that hard codes a literal for a double value in E notation,
     * as is done in an example within the spec.
     */
    // enable once 28078 is fixed @Test
    public void testLiteralDouble() {
        // Clear out data before test
        accounts.deleteByOwnerEndsWith("TestLiteralDouble");

        accounts.create(new Account(1006520, 28002, "Think Bank", true, 21.04, "Lester TestLiteralDouble"));
        accounts.create(new Account(2003291, 28002, "Think Bank", true, 331.01, "Laura TestLiteralDouble"));

        AccountId id = AccountId.of(2003291, 28002);

        assertEquals(true, accounts.addInterest(id)); // adds 15e-2, which is 0.15

        assertEquals(1L, accounts.countByOwnerAndBalanceBetween("Laura TestLiteralDouble", 331.159, 331.161));

        Account account = accounts.findByAccountId(id);
        assertEquals(331.16, account.balance, 0.001);

        assertEquals(2L, accounts.deleteByOwnerEndsWith("TestLiteralDouble"));
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
     * Verify that LOWER(ID(o)) is valid in a query.
     */
    @Test
    public void testLowerId() {
        assertEquals(0, counties.deleteByNameIn(List.of("Freeborn", "Steele")));

        int[] freebornZipCodes = new int[] { 55912, 56007, 56009, 56016, 56020, //
                                             56026, 56029, 56032, 56035, 56036 };

        int[] steeleZipCodes = new int[] { 55049, 55060, 55917, 55924, 55926, //
                                           55946, 559072 };

        County freeborn = new County("Freeborn", "Minnesota", 30895, freebornZipCodes, //
                        "Albert Lea", "Alden", "Clarks Grove", "Conger", "Emmons", //
                        "Freeborn", "Geneva", "Glenville", "Hartland", "Hayward", //
                        "Hollandale", "Manchester", "Myrtle", "Twin Lakes");

        County steele = new County("Steele", "Minnesota", 37406, steeleZipCodes, //
                        "Blooming Prairie", "Ellendale", "Medford", "Owatonna");

        counties.save(freeborn, steele);

        assertEquals(Integer.valueOf(30895),
                     counties.populationOf("freeborn").orElseThrow());

        assertEquals(Integer.valueOf(37406),
                     counties.populationOf("steele").orElseThrow());

        assertEquals(2, counties.deleteByNameIn(List.of("Freeborn", "Steele")));
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
     * Query that matches multiple entities that have a ManyToMany relation.
     * Verify that the data within the ManyToMany relations of the results
     * is correct.
     */
    @Test
    public void testManyToManyIncludedInResults() {

        List<String> addresses = customers.findByPhoneIn(List.of(5075552444L,
                                                                 5075550101L))
                        .map(cust -> cust.deliveryLocations)
                        .map(locs -> locs
                                        .stream()
                                        .map(loc -> loc.houseNum + " " +
                                                    loc.street.toString())
                                        .sorted()
                                        .collect(Collectors.toList())
                                        .toString())
                        .collect(Collectors.toList());

        // Customer 1's delivery address must come before
        // Customer 4's delivery addresses due to ordering
        // on Customer.email
        assertEquals(List.of("[1001 1st Ave SW]",
                             "[2800 37th St NW, 4004 4th Ave SE]"),
                     addresses);
    }

    /**
     * Query that matches a single entity that has a ManyToMany relation.
     * Verify that the data within the ManyToMany relation of the result
     * is correct.
     */
    @Test
    public void testManyToManyIncludedInSingleResult() {
        Set<DeliveryLocation> locations = customers //
                        .findByEmail("Maximilian@tests.openliberty.io")
                        .orElseThrow().deliveryLocations;

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
     * filters on an attribute from the many side (CreditCard),
     * orders by an attribute on the one side (Customer email),
     * returning results (CreditCard) from which the one (Customer)
     * can be obtained.
     */
    @Test
    public void testManyToOneMM11() {
        assertIterableEquals(List.of("MICHELLE@TESTS.OPENLIBERTY.IO",
                                     "Matthew@tests.openliberty.io",
                                     "Maximilian@tests.openliberty.io",
                                     "Megan@tests.openliberty.io"),
                             creditCards.findByIssuer(Issuer.MonsterCard)
                                             .map(cc -> cc.debtor)
                                             .map(c -> c.email)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("MICHELLE@TESTS.OPENLIBERTY.IO",
                                     "Matthew@tests.openliberty.io",
                                     "Megan@tests.openliberty.io",
                                     "Monica@tests.openliberty.io"),
                             creditCards.findByIssuer(Issuer.Feesa)
                                             .map(cc -> cc.debtor)
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
     * Query that matches based on an attribute of the One of a ManyToOne relation.
     */
    @Test
    public void testManyToOneSubAttribute() {

        Stream<CreditCard> cards = customers//
                        .findCardsByDebtorEmailEndsWith("an@tests.openliberty.io");
        List<Long> cardNumbers = cards
                        .map(card -> card.number)
                        .collect(Collectors.toList());

        // Customer 5's card numbers must come before Customer 4's card numbers
        // due to the ordering on Customer.phone.
        assertEquals(Set.of(5000921051110001L, 5000921052220002L),
                     new HashSet<>(cardNumbers.subList(0, 2)));

        assertEquals(Set.of(4000921041110001L, 4000921042220002L),
                     new HashSet<>(cardNumbers.subList(2, 4)));
    }

    /**
     * Add, find, and remove entities with a mapped superclass.
     * Also tests the Iterator return type with a PageRequest and list.
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

        assertEquals(false, it.hasNext());

        it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("M", PageRequest.ofPage(2).size(3));

        assertEquals(true, it.hasNext());
        assertNotNull(t = it.next());
        assertEquals(t4.leviedAgainst, t.leviedAgainst);

        assertNotNull(t = it.next());
        assertEquals(t3.leviedAgainst, t.leviedAgainst);

        assertNotNull(t = it.next());
        assertEquals(t2.leviedAgainst, t.leviedAgainst);
        Long t2key = t.key;

        assertEquals(false, it.hasNext());

        it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("M", PageRequest.ofPage(3).size(3));

        assertNotNull(t = it.next());
        assertEquals(t1.leviedAgainst, t.leviedAgainst);

        assertEquals(false, it.hasNext());
        assertEquals(false, it.hasNext());

        // Iterator with cursor-based pagination:
        try {
            it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("M", PageRequest.ofSize(2)
                            .afterCursor(Cursor.forKey(t8key)));
            fail("Did not enforce the CursoredPage return type for cursor-based pagination in the CURSOR_NEXT direction.");
        } catch (IllegalArgumentException x) {
            // expected
        }

        // Iterator with cursor-based pagination obtaining pages in the
        // previous page direction
        try {
            it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("M", PageRequest.ofSize(2)
                            .beforeCursor(Cursor.forKey(t2key)));
            fail("Did not enforce the CursoredPage return type for cursor-based pagination in the CURSOR_PREVIOUS direction.");
        } catch (IllegalArgumentException x) {
            // expected
        }

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
                             tariffs.findByLeviedByOrderByKey("USA", PageRequest.ofPage(2).size(4))
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
     * Query that matches a single entity and so returns one collection for a OneToMany association.
     */
    @Test
    public void testOneToManyReturnsOneSetOfMany() {
        Set<CreditCard> cards = customers.findCardsByDebtorCustomerId(9210005);

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
        d = drivers.findByLicense_licenseNum("T121-200-200-200");
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
                             drivers.findByDriver_fullNameEndsWith(" TestOneToOne")
                                             .map(license -> license.stateName + " " + license.licenseNum)
                                             .collect(Collectors.toList()));

        // Order by attributes of the entity to which OneToOne maps, using various formats for referring to the attributes:
        assertIterableEquals(List.of("Oscar TestOneToOne", // Iowa
                                     "Owen TestOneToOne", "Olivia TestOneToOne", // Minnesota
                                     "Ozzy TestOneToOne", "Oliver TestOneToOne"), // Wisconsin
                             drivers.findByLicenseNotNull()
                                             .map(driver -> driver.fullName)
                                             .collect(Collectors.toList()));

        drivers.setInfo(new Driver("Oscar TestOneToOne", //
                        100404000, //
                        LocalDate.of(2004, 4, 4), //
                        75, // height updated
                        242, // weight updated
                        "T121-400-400-400", //
                        "Iowa", //
                        LocalDate.of(2020, 4, 4), //
                        LocalDate.of(2024, 4, 4)));
        d4 = drivers.findById(100404000).orElseThrow();
        assertEquals("Oscar TestOneToOne", d4.fullName);
        assertEquals(75, d4.heightInInches);
        assertEquals(242, d4.weightInPounds);
        assertEquals("Iowa", d4.license.stateName);
        assertEquals(LocalDate.of(2020, 4, 4), d4.license.issuedOn);

        drivers.deleteByFullNameEndsWith(" TestOneToOne");
    }

    /**
     * Use a repository method with Page<Object> return type.
     * The primary entity type of the repository should be used.
     */
    @Test
    public void testPageOfObjectReturnType() {
        Page<Object> page1 = businesses
                        .findAsPageByNameBetween("Crenlo",
                                                 "RAC",
                                                 PageRequest.ofSize(4));

        assertEquals(List.of("Metafile", // 3428
                             "RAC", // 3100
                             "IBM", // 2800
                             "Custom Alarm"), // 1661
                     page1.stream()
                                     .map(b -> ((Business) b).name)
                                     .collect(Collectors.toList()));

        Page<Object> page2 = businesses
                        .findAsPageByNameBetween("Crenlo",
                                                 "RAC",
                                                 page1.nextPageRequest());

        assertEquals(List.of("Crenlo", // 1600
                             "Geotek", // 1421
                             "Home Federal Savings Bank", // 1016
                             "HALCON"), // 345
                     page2.stream()
                                     .map(b -> ((Business) b).name)
                                     .collect(Collectors.toList()));

        Page<Object> page3 = businesses
                        .findAsPageByNameBetween("Crenlo",
                                                 "RAC",
                                                 page2.nextPageRequest());

        assertEquals(List.of("Olmsted Medical", // 210
                             "Mayo Clinic"), // 200
                     page3.stream()
                                     .map(b -> ((Business) b).name)
                                     .collect(Collectors.toList()));

    }

    /**
     * Tests entity attribute names from embeddables and MappedSuperclass that
     * can have delimiters. Includes tests for name collisions with attributes from an
     * embeddable or superinteface.
     */
    @Test
    public void testPersistentFieldNamesAndDelimiters() {
        apartments.removeAll();

        Apartment a101 = new Apartment();
        a101.occupant = new Occupant();
        a101.occupant.firstName = "Kyle";
        a101.occupant.lastName = "Smith";
        a101.isOccupied = true;
        a101.aptId = 101L;
        a101.quarters = new Bedroom();
        a101.quarters.length = 10;
        a101.quarters.width = 10;
        a101.quartersWidth = 15;

        Apartment a102 = new Apartment();
        a102.occupant = new Occupant();
        a102.occupant.firstName = "Brent";
        a102.occupant.lastName = "Smith";
        a102.isOccupied = false;
        a102.aptId = 102L;
        a102.quarters = new Bedroom();
        a102.quarters.length = 11;
        a102.quarters.width = 11;
        a102.quartersWidth = 15;

        Apartment a103 = new Apartment();
        a103.occupant = new Occupant();
        a103.occupant.firstName = "Brian";
        a103.occupant.lastName = "Smith";
        a103.isOccupied = false;
        a103.aptId = 103L;
        a103.quarters = new Bedroom();
        a103.quarters.length = 11;
        a103.quarters.width = 12;
        a103.quartersWidth = 15;

        Apartment a104 = new Apartment();
        a104.occupant = new Occupant();
        a104.occupant.firstName = "Scott";
        a104.occupant.lastName = "Smith";
        a104.isOccupied = false;
        a104.aptId = 104L;
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
     * Use a repository method with cursor-based pagination that does not specify parenthesis
     * in the WHERE clause.
     */
    @Test
    public void testParenthesisInsertionForCursorPagination() {
        PageRequest page1Request = PageRequest.ofSize(3);
        CursoredPage<Business> page1 = mixed.withZipCodeIn(55901, 55904, page1Request, Sort.asc("name"), Sort.asc(ID));

        assertEquals(List.of("Benike Construction", "Crenlo", "Home Federal Savings Bank"),
                     page1.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        assertEquals(true, page1.hasNext());

        CursoredPage<Business> page2 = mixed.withZipCodeIn(55901, 55904, page1.nextPageRequest(), Sort.asc("name"), Sort.asc(ID));

        assertEquals(List.of("IBM", "Metafile", "Olmsted Medical"),
                     page2.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        assertEquals(true, page1.hasNext());

        CursoredPage<Business> page3 = mixed.withZipCodeIn(55901, 55904, page2.nextPageRequest(), Sort.asc("name"), Sort.asc(ID));

        assertEquals(List.of("RAC", "Think Bank"),
                     page3.stream()
                                     .map(b -> b.name)
                                     .collect(Collectors.toList()));

        assertEquals(false, page3.hasNext());
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

    /**
     * Repository method that queries for the IdClass using id(this)
     * and sorts based on the attributes of the IdClass.
     */
    // @Test // TODO enable once #29073 is fixed
    public void testSelectIdClass() {
        assertEquals(List.of("Illinois:Springfield",
                             "Kansas:Kansas City",
                             "Massachusetts:Springfield",
                             "Minnesota:Rochester",
                             "Missouri:Kansas City",
                             "Missouri:Springfield",
                             "New York:Rochester",
                             "Ohio:Springfield",
                             "Oregon:Springfield"),
                     cities.ids()
                                     .map(id -> id.getStateName() + ":" + id.name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use the JPQL version(entityVar) function as the sort attribute to perform
     * an ascending sort.
     */
    @Test
    public void testSortByVersionFunction() {
        orders.deleteAll();

        PurchaseOrder o1 = new PurchaseOrder();
        o1.purchasedBy = "testSortByVersionFunction-Customer1";
        o1.purchasedOn = OffsetDateTime.now();
        o1.total = 21.99f;
        o1 = orders.create(o1);

        PurchaseOrder o2 = new PurchaseOrder();
        o2.purchasedBy = "testSortByVersionFunction-Customer2";
        o2.purchasedOn = OffsetDateTime.now();
        o2.total = 22.99f;
        o2 = orders.create(o2);

        PurchaseOrder o3 = new PurchaseOrder();
        o3.purchasedBy = "testSortByVersionFunction-Customer3";
        o3.purchasedOn = OffsetDateTime.now();
        o3.total = 23.99f;
        o3 = orders.create(o3);

        PurchaseOrder o4 = new PurchaseOrder();
        o4.purchasedBy = "testSortByVersionFunction-Customer4";
        o4.purchasedOn = OffsetDateTime.now();
        o4.total = 24.99f;
        o4 = orders.create(o4);

        PurchaseOrder[] updated;

        o3.total = 33.39f;
        o1.total = 31.19f;
        o2.total = 32.29f;
        updated = orders.modifyAll(o3, o1, o2);
        o3 = updated[0];
        o1 = updated[1];
        o2 = updated[2];

        o3.total = 33.59f;
        o1.total = 31.59f;
        updated = orders.modifyAll(o3, o1);
        o3 = updated[0];
        o1 = updated[1];

        o3.total = 33.99f;
        updated = orders.modifyAll(o3);
        o3 = updated[0];

        assertEquals(List.of("testSortByVersionFunction-Customer4",
                             "testSortByVersionFunction-Customer2",
                             "testSortByVersionFunction-Customer1",
                             "testSortByVersionFunction-Customer3"),
                     orders.findAll(PageRequest.ofSize(10),
                                    Order.by(Sort.asc("version(this)")))
                                     .stream()
                                     .map(o -> o.purchasedBy)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(1, 2, 3, 4),
                     orders.versionsAsc());

        assertEquals(List.of(4, 3, 2, 1),
                     orders.versionsDesc());

        orders.deleteAll();
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
     * Test passing a Sort created with Sort.of, particularly the ignoreCase parameter
     */
    @SkipIfSysProp(DB_SQLServer) //SQLServer does not sort by case by default, thus ignoreCase=false will produce the same result as ignoreCase=true
    @Test
    public void testSortOf() {
        City eagan = cities.save(new City("eagan", "minnesota", 67_396, Set.of(651)));

        // With ignoreCase=true, eagan should be first
        Sort<City> of = Sort.of("name", Direction.ASC, true);
        List<City> all = cities.allSorted(of);
        City first = all.get(0);
        assertEquals("eagan", first.name);

        // With ignoreCase=false Kansas City should be first
        of = Sort.of("name", Direction.ASC, false);
        all = cities.allSorted(of);
        assertEquals("Kansas City", all.get(0).name);

        of = Sort.of("population", Direction.DESC, true);
        try {
            cities.allSorted(of);
            fail("Should not be able to applay a Sort with ignoreCase=true on a" +
                 " non-string entity attribute");
        } catch (UnsupportedOperationException x) {
            // expected
        }

        cities.remove(eagan);
    }

    /**
     * Use an Entity which has a version attribute of type LocalDateTime.
     */
    @Test
    public void testTimeAsVersion() throws Exception {
        /*
         * Reference Issue: https://github.com/eclipse-ee4j/eclipselink/issues/205
         * Without using the Eclipselink Oracle plugin the precision of Timestamp is 1 second
         * Therefore, we need to ensure 1 second has passed between queries where we expect
         * the LocalDateTime/version to be different.
         *
         * For Derby the timestamp field can hold a precision of nanoseconds,
         * but the CURRENT_TIMESTAMP function returns a timestamp with millis
         * precision and there is no configuration to change that.
         *
         * For SQLServer the timestamp field also defaults to millis and Eclipselink
         * offers no alternative query to get better percision.
         *
         * PostgreSQL - default is nanoseconds
         * DB2 - default is microseconds
         */
        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
        boolean secondPrecision = jdbcJarName.startsWith("ojdbc");
        boolean millisecondPrecision = jdbcJarName.startsWith("derby") || jdbcJarName.startsWith("mssql-jdbc");

        assertEquals(0, counties.deleteByNameIn(List.of("Dodge", "Mower")));

        int[] dodgeZipCodes = new int[] { 55924, 55927, 55940, 55944, 55955, 55985 };
        int[] mowerZipCodes = new int[] { 55912, 55917, 55918, 55926, 55933, 55936, //
                                          55950, 55951, 55961, 55953, 55967, 55970, //
                                          55973, 55975, 55982 };

        County dodge = new County("Dodge", "Minnesota", 20867, dodgeZipCodes, //
                        "Mantorville", "Blooming Prairie", "Claremont", //
                        "Dodge Center", "Hayfield", "Kasson", "West Concord");

        County mower = new County("Mower", "Minnesota", 49671, mowerZipCodes, //
                        "Austin", "Adams", "Brownsdale", "Dexter", "Elkton", //
                        "Grand Meadow", "Le Roy", "Lyle", "Mapleview", "Racine", //
                        "Rose Creek", "Sargeant", "Taopi", "Waltham");

        counties.save(dodge, mower);

        dodge = counties.findByName("Dodge").orElseThrow();

        if (secondPrecision)
            Thread.sleep(Duration.ofSeconds(1).toMillis());
        if (millisecondPrecision)
            Thread.sleep(Duration.ofMillis(1).toMillis());

        dodgeZipCodes = new int[] { 55917, 55924, 55927, 55940, 55944, 55955, 55963, 55985 };
        assertEquals(true, counties.updateByNameSetZipCodes("Dodge", dodgeZipCodes));

        // Try to update with outdated version/LocalDateTime:
        try {
            dodge.population = 20873;
            counties.save(dodge);
            fail("Should not be able to save using old version: " + dodge.lastUpdated);
        } catch (OptimisticLockingFailureException x) {
            // expected
        }

        // Update the version/LocalDateTime and retry:
        LocalDateTime lastUpdate;
        lastUpdate = dodge.lastUpdated = counties.findLastUpdatedByName("Dodge");
        dodge.population = 20981;

        if (secondPrecision)
            Thread.sleep(Duration.ofSeconds(1).toMillis());
        if (millisecondPrecision)
            Thread.sleep(Duration.ofMillis(1).toMillis());

        counties.save(dodge);

        // Try to delete by previous version/LocalDateTime,
        assertEquals(false, counties.deleteByNameAndLastUpdated("Dodge", lastUpdate));

        // Should be able to delete with latest version/LocalDateTime,
        lastUpdate = counties.findLastUpdatedByName("Dodge");
        assertEquals(true, counties.deleteByNameAndLastUpdated("Dodge", lastUpdate));

        // Try to delete with wrong version/LocalDateTime (from other entity),
        mower.lastUpdated = lastUpdate;
        try {
            counties.remove(mower);
            fail("Deletion attempt with wrong version did not raise OptimisticLockingFailureException.");
        } catch (OptimisticLockingFailureException x) {
            // pass
        }

        // Use correct version/LocalDateTime,
        mower = counties.findByName("Mower").orElseThrow();
        counties.remove(mower);
    }

    /**
     * Use an Entity which has an attribute which is a collection that is not annotated with the JPA ElementCollection annotation.
     */
    @Test
    public void testUnannotatedCollection() {
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

        // Derby, Oracle, SQLServer  does not support comparisons of BLOB (IMAGE sqlserver) values
        // Derby JDBC Jar Name : derby.jar
        // Oracle JDBC Jar Name : ojdbc8.jar
        // SQLServer JDBC Jar Name : mssql-jdbc.jar
        String jdbcJarName = System.getenv().getOrDefault("DB_DRIVER", "UNKNOWN");
        if (!(jdbcJarName.startsWith("derby") || jdbcJarName.startsWith("ojdbc8") || jdbcJarName.startsWith("mssql-jdbc"))) {
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
                     Arrays.toString(counties.findZipCodesByNameContains("llmor")));

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
        Iterator<int[]> it = counties.findZipCodesByPopulationLessThanEqual(50000);
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

        // iterator over array attribute with none found
        assertEquals(false, counties.findZipCodesByPopulationLessThanEqual(1).hasNext());

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
     * Update an entity that has an IdClass.
     * This covers update that returns the updated value
     * and update that returns no value,
     * which are different code paths.
     */
    @Test
    public void testUpdateEntityWithIdClass() {
        CreditCard original = creditCards
                        .findByIssuedOnWithMonthIn(Set.of(Month.MAY.getValue()))
                        .findFirst()
                        .orElseThrow();

        CreditCard replacement = new CreditCard( //
                        original.debtor, //
                        original.number, //
                        551, // new security code
                        LocalDate.of(2024, Month.MAY, 10), //
                        LocalDate.of(2028, Month.MAY, 10), //
                        original.issuer);

        replacement = creditCards.replace(replacement);

        assertEquals(original.debtor.customerId, replacement.debtor.customerId);
        assertEquals(original.number, replacement.number);
        assertEquals(551, replacement.securityCode);
        assertEquals(LocalDate.of(2024, Month.MAY, 10), replacement.issuedOn);
        assertEquals(LocalDate.of(2028, Month.MAY, 10), replacement.expiresOn);
        assertEquals(original.issuer, replacement.issuer);

        CreditCard card = creditCards
                        .findByIssuedOnWithMonthIn(Set.of(Month.MAY.getValue()))
                        .findFirst()
                        .orElseThrow();

        assertEquals(original.debtor.customerId, card.debtor.customerId);
        assertEquals(original.number, card.number);
        assertEquals(551, card.securityCode);
        assertEquals(LocalDate.of(2024, Month.MAY, 10), card.issuedOn);
        assertEquals(LocalDate.of(2028, Month.MAY, 10), card.expiresOn);
        assertEquals(original.issuer, card.issuer);

        // Put the original value back to avoid impacting other tests.
        // This also tests an Update method with void return.
        creditCards.revert(original);
    }

    /**
     * Update an entity that has an IdClass and Version.
     * This covers update that returns the updated value
     * and update that returns no value,
     * which are different code paths.
     */
    @Test
    public void testUpdateEntityWithIdClassAndVersion() {
        CityId mnId = CityId.of("Rochester", "Minnesota");
        CityId nyId = CityId.of("Rochester", "New York");

        long mnVer = cities.currentVersion(mnId.name, mnId.getStateName());
        long nyVer = cities.currentVersion(nyId.name, nyId.getStateName());
        // TODO
        //long mnVer = cities.currentVersion(mnId);
        //long nyVer = cities.currentVersion(nyId);

        // TODO allow this test to run once 28589 is fixed
        // and verify that EclipseLink does not corrupt the area code value
        // for the following subsequent tests:
        // testCollectionAttribute, testIdClassOrderBySorts, testIdClassOrderByAnnotationWithCursorPagination,
        // testIdClassOrderByNamePatternWithCursorPagination, testIdClassOrderByAnnotationReverseDirection
        if (true)
            return;

        City[] updated = cities.modifyData(City.of(mnId, 122413, Set.of(507, 924), mnVer),
                                           City.of(nyId, 208546, Set.of(585), nyVer));
        assertEquals(Arrays.toString(updated), 2, updated.length);
        assertEquals("Rochester", updated[0].name);
        assertEquals("Minnesota", updated[0].stateName);
        assertEquals(122413, updated[0].population);
        assertEquals(Set.of(507, 924), updated[0].areaCodes);
        assertEquals(mnVer + 1, updated[0].changeCount);

        assertEquals("Rochester", updated[1].name);
        assertEquals("New York", updated[1].stateName);
        assertEquals(208546, updated[1].population);
        assertEquals(Set.of(585), updated[1].areaCodes);
        assertEquals(nyVer + 1, updated[1].changeCount);

        // restore original data
        cities.modifyStats(City.of(mnId, 121395, Set.of(507), mnVer + 1),
                           City.of(nyId, 211328, Set.of(585), nyVer + 1));
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
            assertEquals(44.05881f, ibm.location.latitude, 0.0001f);
            assertEquals(-92.50556f, ibm.location.longitude, 0.0001f);
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
        assertEquals(originalLatitude, ibm.location.latitude, 0.0001f);
        assertEquals(originalLongitude, ibm.location.longitude, 0.0001f);
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
        UUID id = o1.id;

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
        UUID id = o1.id;

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
        PurchaseOrder updated = orders.update(o1);

        assertEquals("testVersionedUpdate-Customer1", updated.purchasedBy);
        assertEquals(10.29f, updated.total, 0.001f);
        assertEquals(id, updated.id);
        assertEquals(newVersion + 1, updated.versionNum);

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

    /**
     * Use a repository method with CursoredPage<?> return type.
     * The primary entity type of the repository should be used.
     */
    @Test
    public void testWildcardCursoredPageReturnType() {
        CursoredPage<?> page1 = businesses.findAsCursoredPage("Crenlo",
                                                              "RAC",
                                                              PageRequest.ofSize(4));

        assertEquals(List.of("Crenlo",
                             "Custom Alarm",
                             "Geotek",
                             "HALCON"),
                     page1.stream()
                                     .map(b -> ((Business) b).name)
                                     .collect(Collectors.toList()));

        CursoredPage<?> page2 = businesses.findAsCursoredPage(
                                                              "Crenlo",
                                                              "RAC",
                                                              page1.nextPageRequest());

        assertEquals(List.of("Home Federal Savings Bank",
                             "IBM",
                             "Mayo Clinic",
                             "Metafile"),
                     page2.stream()
                                     .map(b -> ((Business) b).name)
                                     .collect(Collectors.toList()));

        CursoredPage<?> page3 = businesses.findAsCursoredPage("Crenlo",
                                                              "RAC",
                                                              page2.nextPageRequest());

        assertEquals(List.of("Olmsted Medical",
                             "RAC"),
                     page3.stream()
                                     .map(b -> ((Business) b).name)
                                     .collect(Collectors.toList()));

    }

    /**
     * Use a repository method with List<?> return type.
     * The primary entity type of the repository should be used.
     */
    @Test
    public void testWildcardListReturnType() {
        assertEquals(List.of("IBM",
                             "Mayo Clinic",
                             "Metafile",
                             "Olmsted Medical",
                             "RAC"),
                     businesses.findAsListByNameBetween("IBM", "RAC")
                                     .stream()
                                     .map(b -> ((Business) b).name)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use a repository method with Optional<?> return type.
     * The primary entity type of the repository should be used.
     */
    @Test
    public void testWildcardOptionalReturnType() {
        Business found = (Business) businesses.findAsOptional("IBM")
                        .orElseThrow();
        assertEquals("IBM", found.name);
    }

    /**
     * Use a repository method with CompletableFuture<Stream<?>> return type.
     * The primary entity type of the repository should be used.
     */
    @Test
    public void testWildcardStreamReturnType() {
        assertEquals(List.of("Geotek",
                             "HALCON"),
                     businesses.findAsStreamByCity("Stewartville", "MN")
                                     .join()
                                     .map(b -> ((Business) b).name)
                                     .collect(Collectors.toList()));
    }
}
