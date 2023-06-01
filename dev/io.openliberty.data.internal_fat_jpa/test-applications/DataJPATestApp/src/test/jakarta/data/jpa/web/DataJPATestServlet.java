/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetAwareSlice;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Pageable.Cursor;
import jakarta.data.repository.Sort;
import jakarta.inject.Inject;
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
    Orders orders;

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
        businesses.save(new Business(44.05938f, -92.50373f, "Rochester", "MN", 55901, 2800, "37st St", "NW", "IBM"));
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
        shippingAddresses.save(a1);

        ShippingAddress a2 = new ShippingAddress();
        a2.id = 1002L;
        a2.city = "Rochester";
        a2.state = "Minnesota";
        a2.streetAddress = new StreetAddress(201, "4th St SE");
        a2.zipCode = 55904;
        shippingAddresses.save(a2);

        ShippingAddress a3 = new ShippingAddress();
        a3.id = 1003L;
        a3.city = "Rochester";
        a3.state = "Minnesota";
        a3.streetAddress = new StreetAddress(200, "1st Ave SW");
        a3.zipCode = 55902;
        shippingAddresses.save(a3);

        ShippingAddress a4 = new ShippingAddress();
        a4.id = 1004L;
        a4.city = "Rochester";
        a4.state = "Minnesota";
        a4.streetAddress = new StreetAddress(151, "4th St SE");
        a4.zipCode = 55904;
        shippingAddresses.save(a4);

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

        taxpayers.save(t1, t2, t3, t4, t5, t6, t7);

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

        page = businesses.findByZipIn(zipCodes, Pageable.ofSize(4));

        assertIterableEquals(List.of(345, 1421, 1016, 1600),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        page = businesses.findByZipIn(zipCodes, page.nextPageable());

        assertIterableEquals(List.of(2800, 2960, 3100, 3428),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(2L, page.pageable().page());
        assertEquals(4, page.pageable().size());

        page = businesses.findByZipIn(zipCodes, page.nextPageable());

        assertIterableEquals(List.of(5201, 1661, 3706, 200),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(3, page.number());

        page = businesses.findByZipIn(zipCodes, page.nextPageable());

        assertIterableEquals(List.of(1402, 3008),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(2, page.numberOfElements());
        assertEquals(4, page.number());
        assertEquals(null, page.nextPageable());

        page = businesses.findByZipIn(zipCodes, page.previousPageable());

        assertIterableEquals(List.of(5201, 1661, 3706, 200),
                             page
                                             .stream()
                                             .map(b -> b.location.address.houseNum)
                                             .collect(Collectors.toList()));

        assertEquals(3, page.number());
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
                             businesses.onSouthSide());
    }

    /**
     * Repository method where the result type is the embeddable class of one of the entity attributes.
     */
    @Test
    public void testEmbeddableTypeAsResult() {
        assertIterableEquals(List.of("NW 19th St",
                                     "NW 37st St",
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

        accounts.save(new Account(1005380, 70081, "Think Bank", true, 552.18, "Ellen TestEmbeddedId"));
        accounts.save(new Account(1004470, 70081, "Think Bank", true, 443.94, "Erin TestEmbeddedId"));
        accounts.save(new Account(1006380, 70081, "Think Bank", true, 160.63, "Edward TestEmbeddedId"));
        accounts.save(new Account(1007590, 70081, "Think Bank", true, 793.30, "Elizabeth TestEmbeddedId"));
        accounts.save(new Account(1008410, 22158, "Home Federal Savings Bank", true, 829.91, "Elizabeth TestEmbeddedId"));
        accounts.save(new Account(1006380, 22158, "Home Federal Savings Bank", true, 261.66, "Elliot TestEmbeddedId"));
        accounts.save(new Account(1004470, 22158, "Home Federal Savings Bank", false, 416.14, "Emma TestEmbeddedId"));
        accounts.save(new Account(1009130, 30372, "Mayo Credit Union", true, 945.20, "Elizabeth TestEmbeddedId"));
        accounts.save(new Account(1004470, 30372, "Mayo Credit Union", true, 423.15, "Eric TestEmbeddedId"));
        accounts.save(new Account(1008200, 30372, "Mayo Credit Union", true, 103.04, "Evan TestEmbeddedId"));

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

        try {
            System.out.println("findByIdInOrOwner: " + accounts.findByIdInOrOwner(List.of(AccountId.of(1004470, 30372),
                                                                                          AccountId.of(1006380, 22158)),
                                                                                  "Emma TestEmbeddedId"));
        } catch (MappingException x) {
            // expected
        }

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

        Order o1 = new Order();
        o1.purchasedBy = "testEntitiesAsParameters-Customer1";
        o1.purchasedOn = OffsetDateTime.now();
        o1.total = 10.99f;
        o1 = orders.save(o1);
        int o1_v1 = o1.versionNum;

        Order o2 = new Order();
        o2.purchasedBy = "testEntitiesAsParameters-Customer2";
        o2.purchasedOn = OffsetDateTime.now();
        o2.total = 20.99f;
        o2 = orders.save(o2);

        Order o3 = new Order();
        o3.purchasedBy = "testEntitiesAsParameters-Customer3";
        o3.purchasedOn = OffsetDateTime.now();
        o3.total = 30.99f;
        o3 = orders.save(o3);

        Order o4 = new Order();
        o4.purchasedBy = "testEntitiesAsParameters-Customer4";
        o4.purchasedOn = OffsetDateTime.now();
        o4.total = 40.99f;
        o4 = orders.save(o4);

        Order o5 = new Order();
        o5.purchasedBy = "testEntitiesAsParameters-Customer5";
        o5.purchasedOn = OffsetDateTime.now();
        o5.total = 50.99f;
        o5 = orders.save(o5);
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
                Order o1updated = orders.findById(o1id).orElseThrow();
                o1updated.total = 11.99f;
                return orders.save(o1updated);
            }).get(30, TimeUnit.SECONDS);

            try {
                orders.delete(o1);
                fail("Deletion must be rejected when the version doesn't match.");
            } catch (DataException x) {
                System.out.println("Deletion was rejected as it ought to be when the version does not match.");
            }

            assertEquals(Status.STATUS_MARKED_ROLLBACK, tran.getStatus());
        } finally {
            tran.rollback();
        }

        Order o2old = new Order();
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
            Order unexpected = orders.save(o2old);
            fail("Should not be able to update old version of entity: " + unexpected);
        } catch (DataException x) {
            // expected
        }

        // attempt to save second entity at an old version in combination with addition of another entity
        Order o6 = new Order();
        o6.purchasedBy = "testEntitiesAsParameters-Customer6";
        o6.purchasedOn = OffsetDateTime.now();
        o6.total = 60.99f;
        try {
            Iterable<Order> unexpected = orders.saveAll(List.of(o6, o2old));
            fail("Should not be able to update old version of entity: " + unexpected);
        } catch (DataException x) {
            // expected
        }

        // verify that the second entity remains at its second version (22.99) and that the addition of the sixth entity was rolled back
        List<Float> orderTotals = orders.findTotalByPurchasedByIn(List.of("testEntitiesAsParameters-Customer2",
                                                                          "testEntitiesAsParameters-Customer6"));
        assertEquals(orderTotals.toString(), 1, orderTotals.size());
        assertEquals(22.99f, orderTotals.get(0), 0.001f);

        orders.deleteAll(List.of(o3, o2));

        Map<String, Order> map = orders.findAll()
                        .collect(Collectors.toMap(o -> o.purchasedBy, // key
                                                  o -> o)); // value

        assertEquals(map.toString(), 2, map.size());

        Order o;
        assertNotNull(o = map.get("testEntitiesAsParameters-Customer1"));
        assertEquals(11.99f, o.total, 0.001f);
        assertEquals(o1_v1 + 1, o.versionNum); // updated once

        assertNotNull(o = map.get("testEntitiesAsParameters-Customer5"));
        assertEquals(50.99f, o.total, 0.001f);
        assertEquals(o5_v1, o.versionNum); // never updated

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

        Order o1 = new Order();
        o1.purchasedBy = "testGeneratedKey-Customer1";
        o1.purchasedOn = OffsetDateTime.of(2022, 6, 1, 9, 30, 0, 0, MDT);
        o1.total = 25.99f;
        o1 = orders.save(o1);

        Order o2 = new Order();
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
     * Repository method with the Filter annotation that queries based on multiple IdClass parameters.
     */
    @Test
    public void testIdClassFilterAnnotation() {
        assertIterableEquals(List.of("Rochester Minnesota",
                                     "Rochester New York"),
                             cities.withNameOf("Rochester")
                                             .map(c -> c.name + ' ' + c.stateName)
                                             .collect(Collectors.toList()));

        assertIterableEquals(List.of("Springfield Massachusetts",
                                     "Rochester Minnesota",
                                     "Kansas City Missouri"),
                             cities.largerThan(100000, CityId.of("springfield", "missouri"), "M%s")
                                             .map(c -> c.name + ' ' + c.stateName)
                                             .collect(Collectors.toList()));
    }

    /**
     * Repository method with the Filter annotation that queries based on IdClass as a named parameter.
     */
    @Test
    public void testIdClassFilterAnnotationWithNamedParameters() {
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
        Pageable pagination = Pageable.ofSize(3).afterKeyset(CityId.of("Rochester", "Minnesota"));

        KeysetAwareSlice<City> slice1 = cities.findByStateNameNotEndsWith("o", pagination);
        assertIterableEquals(List.of("Rochester New York",
                                     "Springfield Illinois",
                                     "Springfield Massachusetts"),
                             slice1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwareSlice<City> slice2 = cities.findByStateNameNotEndsWith("o", slice1.nextPageable());
        assertIterableEquals(List.of("Springfield Missouri",
                                     "Springfield Oregon"),
                             slice2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, slice2.nextPageable());

        KeysetAwareSlice<City> slice0 = cities.findByStateNameNotEndsWith("o", slice1.previousPageable());
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota"),
                             slice0.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, slice0.previousPageable());
    }

    /**
     * Use keyset pagination with the OrderBy annotation on a composite id that is defined by an IdClass attribute.
     * Also use named parameters, which means the keyset portion of the query will also need to use named parameters.
     */
    @Test
    public void testIdClassOrderByAnnotationWithKeysetPaginationAndNamedParameters() {
        Pageable pagination = Pageable.ofSize(2);

        KeysetAwarePage<City> page1 = cities.sizedWithin(100000, 1000000, pagination);
        assertIterableEquals(List.of("Springfield Missouri",
                                     "Springfield Massachusetts"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(4L, page1.totalPages());
        assertEquals(7L, page1.totalElements());

        KeysetAwarePage<City> page2 = cities.sizedWithin(100000, 1000000, page1.nextPageable());
        assertIterableEquals(List.of("Springfield Illinois",
                                     "Rochester New York"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page3 = cities.sizedWithin(100000, 1000000, page2.nextPageable());
        assertIterableEquals(List.of("Rochester Minnesota",
                                     "Kansas City Missouri"),
                             page3.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page4 = cities.sizedWithin(100000, 1000000, page3.nextPageable());
        assertIterableEquals(List.of("Kansas City Kansas"),
                             page4.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, page4.nextPageable());
    }

    /**
     * Use keyset pagination with the OrderBy query-by-method pattern on a composite id that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderByNamePatternWithKeysetPagination() {
        Pageable pagination = Pageable.ofSize(5);

        KeysetAwareSlice<City> slice1 = cities.findByStateNameNotNullOrderById(pagination);
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota",
                                     "Rochester New York",
                                     "Springfield Illinois"),
                             slice1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwareSlice<City> slice2 = cities.findByStateNameNotNullOrderById(slice1.nextPageable());
        assertIterableEquals(List.of("Springfield Massachusetts",
                                     "Springfield Missouri",
                                     "Springfield Ohio",
                                     "Springfield Oregon"),
                             slice2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, slice2.nextPageable());

        Cursor springfieldMO = slice2.getKeysetCursor(1);
        pagination = Pageable.ofSize(3).beforeKeysetCursor(springfieldMO);

        KeysetAwareSlice<City> beforeSpringfieldMO = cities.findByStateNameNotNullOrderById(pagination);
        assertIterableEquals(List.of("Rochester New York",
                                     "Springfield Illinois",
                                     "Springfield Massachusetts"),
                             beforeSpringfieldMO.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwareSlice<City> beforeRochesterNY = cities.findByStateNameNotNullOrderById(beforeSpringfieldMO.previousPageable());
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota"),
                             beforeRochesterNY.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, beforeRochesterNY.previousPageable());
    }

    /**
     * Use keyset pagination with the OrderBy query-by-method pattern in descending direction
     * on a composite id that is defined by an IdClass attribute.
     */
    @Test
    public void testIdClassOrderByNamePatternWithKeysetPaginationDescending() {
        Pageable pagination = Pageable.ofSize(3).afterKeyset(CityId.of("Springfield", "Tennessee"));

        KeysetAwarePage<City> page1 = cities.findByStateNameNotStartsWithOrderByIdDesc("Ma", pagination);
        assertIterableEquals(List.of("Springfield Oregon",
                                     "Springfield Ohio",
                                     "Springfield Missouri"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page2 = cities.findByStateNameNotStartsWithOrderByIdDesc("Ma", page1.nextPageable());
        assertIterableEquals(List.of("Springfield Illinois",
                                     "Rochester New York",
                                     "Rochester Minnesota"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page3 = cities.findByStateNameNotStartsWithOrderByIdDesc("Ma", page2.nextPageable());
        assertIterableEquals(List.of("Kansas City Missouri",
                                     "Kansas City Kansas"),
                             page3.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, page3.nextPageable());

        page2 = cities.findByStateNameNotStartsWithOrderByIdDesc("Ma", page3.previousPageable());
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
        Pageable pagination = Pageable.ofSize(5).sortBy(Sort.asc("id"));

        KeysetAwarePage<City> page1 = cities.findByStateNameGreaterThan("Iowa", pagination);
        assertIterableEquals(List.of("Kansas City Kansas",
                                     "Kansas City Missouri",
                                     "Rochester Minnesota",
                                     "Rochester New York",
                                     "Springfield Massachusetts"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page2 = cities.findByStateNameGreaterThan("Iowa", page1.nextPageable());
        assertIterableEquals(List.of("Springfield Missouri",
                                     "Springfield Ohio",
                                     "Springfield Oregon"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, page2.nextPageable());

        // descending:
        pagination = Pageable.ofSize(4).sortBy(Sort.descIgnoreCase("id"));
        page1 = cities.findByStateNameGreaterThan("Idaho", pagination);
        assertIterableEquals(List.of("Springfield Oregon",
                                     "Springfield Ohio",
                                     "Springfield Missouri",
                                     "Springfield Massachusetts"),
                             page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        page2 = cities.findByStateNameGreaterThan("Idaho", page1.nextPageable());
        assertIterableEquals(List.of("Springfield Illinois",
                                     "Rochester New York",
                                     "Rochester Minnesota",
                                     "Kansas City Missouri"),
                             page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        KeysetAwarePage<City> page3 = cities.findByStateNameGreaterThan("Idaho", page2.nextPageable());
        assertIterableEquals(List.of("Kansas City Kansas"),
                             page3.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(null, page3.nextPageable());
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
     * Repository method with the Update annotation that makes an update by assigning the IdClass instance to something else.
     */
    @Test
    public void testIdClassUpdateAnnotation() {
        cities.save(new City("La Crosse", "Wisconsin", 52680, Set.of(608)));
        try {
            cities.findById(CityId.of("La Crosse", "Wisconsin")).orElseThrow();

            assertEquals(1, cities.replace(CityId.of("La Crosse", "Wisconsin"),
                                           CityId.of("Decorah", "Iowa"), 7587, Set.of(563)));

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
     * Repository method with the Update annotation that makes an update by assigning the IdClass instance to something else.
     * This test uses named parameters.
     */
    @Test
    public void testIdClassUpdateAnnotationWithNamedParameters() {
        cities.save(new City("Janesville", "Wisconsin", 65615, Set.of(608)));
        try {
            cities.findById(CityId.of("Janesville", "Wisconsin")).orElseThrow();

            assertEquals(1, cities.replace(CityId.of("Janesville", "Wisconsin"),
                                           CityId.of("Ames", "Iowa"), Set.of(515), 66427));

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

        employees.save(new Employee("Irene", "TestIdOnEmbeddable", (short) 2636, 'A'));
        employees.save(new Employee("Isabella", "TestIdOnEmbeddable", (short) 8171, 'B'));
        employees.save(new Employee("Ivan", "TestIdOnEmbeddable", (short) 4948, 'A'));
        employees.save(new Employee("Isaac", "TestIdOnEmbeddable", (short) 5310, 'C'));

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
    @Test
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
    @Test
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
    @Test
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
        Iterator<Tariff> it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("M", Pageable.ofSize(3));

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
        it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("M", Pageable.ofSize(2).afterKeyset(t8key));

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
        it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("M", Pageable.ofSize(2).beforeKeyset(t2key));

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
        it = tariffs.findByLeviedAgainstLessThanOrderByKeyDesc("A", Pageable.ofSize(3));
        assertEquals(false, it.hasNext());

        t = tariffs.findByLeviedByAndLeviedAgainstAndLeviedOn("USA", "Bangladesh", "Textiles");
        assertEquals(t6.rate, t.rate, 0.0001f);

        // List return type for Pagination only represents a single page, not all pages.
        // page 1:
        assertIterableEquals(List.of("China", "Germany", "India", "Japan"),
                             tariffs.findByLeviedByOrderByKey("USA", Pageable.ofSize(4))
                                             .stream()
                                             .map(o -> o.leviedAgainst)
                                             .collect(Collectors.toList()));
        // page 2:
        assertIterableEquals(List.of("Canada", "Bangladesh", "Mexico", "Canada"),
                             tariffs.findByLeviedByOrderByKey("USA", Pageable.ofSize(4).page(2))
                                             .stream()
                                             .map(o -> o.leviedAgainst)
                                             .collect(Collectors.toList()));

        // Random access to paginated list:
        List<Tariff> list = tariffs.findByLeviedByOrderByKey("USA", Pageable.ofPage(1));
        assertEquals(t4.leviedAgainst, list.get(3).leviedAgainst);
        assertEquals(t7.leviedAgainst, list.get(6).leviedAgainst);
        assertEquals(t2.leviedAgainst, list.get(1).leviedAgainst);
        assertEquals(t8.leviedAgainst, list.get(7).leviedAgainst);

        assertEquals(8, tariffs.deleteByLeviedBy("USA"));
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
    @Test
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
    @Test
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
     * Use an Entity which has an attribute which is a collection that is not annotated with the JPA ElementCollection annotation.
     */
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

        counties.save(olmsted, winona, wabasha, fillmore);

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

        // Derby does not support comparisons of BLOB values
        String jdbcJarName = request.getParameter("jdbcJarName").toLowerCase();
        if (!jdbcJarName.startsWith("derby")) {
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
                             counties.findZipCodesByNameStartsWith("W", Pageable.ofSize(10))
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
        assertEquals(0, counties.findZipCodesByNameStartsWith("Hous", Pageable.ofSize(5)).numberOfElements());

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
        assertEquals(false, cities.remove(duluth));

        duluth.changeCount = newVersion;
        assertEquals(true, cities.remove(duluth));
    }
}
