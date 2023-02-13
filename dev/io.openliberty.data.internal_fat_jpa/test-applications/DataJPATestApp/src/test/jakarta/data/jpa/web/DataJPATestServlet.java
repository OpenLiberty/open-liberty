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

import static org.junit.Assert.assertEquals;
import static test.jakarta.data.jpa.web.Assertions.assertIterableEquals;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Resource;
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
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

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
    Employees employees;

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

        accounts.delete(new Account(1005380, 70081, "Think Bank", true, 552.18, "Ellen TestEmbeddedId"));

        // JPQL with "IN", which this needs, is not supported by EclipseLink for embeddables
        // accounts.deleteAll(List.of(new Account(1004470, 70081, "Think Bank", true, 443.94, "Erin TestEmbeddedId")));

        accounts.deleteByOwnerEndsWith("TestEmbeddedId");
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
        cities.save(winona);
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
     */
    @Test
    public void testIdOnEmbeddable() {
        // Clear out data before test
        employees.deleteByLastName("TestIdOnEmbeddable");

        employees.save(new Employee("Irene", "TestIdOnEmbeddable", (short) 2636, 'A'));
        employees.save(new Employee("Isabella", "TestIdOnEmbeddable", (short) 8171, 'B'));
        employees.save(new Employee("Ivan", "TestIdOnEmbeddable", (short) 4948, 'A'));
        employees.save(new Employee("Isaac", "TestIdOnEmbeddable", (short) 5310, 'C'));

        assertEquals("Ivan", employees.findById(4948).firstName);

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
}
