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
package test.jakarta.data.experimental.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;
import junit.framework.AssertionFailedError;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DataExperimentalServlet extends FATServlet {

    @Inject
    Reservations reservations;

    @Inject
    Shipments shipments;

    @Inject
    Towns towns;

    public static <T> void assertArrayEquals(T[] expected, T[] actual, Comparator<T> comparator) {
        String errorMessage = "expected: " + Arrays.toString(expected) + " but was: " + Arrays.toString(actual);

        if (expected == actual) // covers if both are null
            return;
        if (expected == null || actual == null || expected.length != actual.length)
            throw new AssertionFailedError(errorMessage);

        for (int i = 0; i < expected.length; i++)
            assertEquals(errorMessage, 0, comparator.compare(expected[i], actual[i]));
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // Some read-only data that is prepopulated for tests:
        towns.add(new Town("Rochester", "Minnesota", 121395, Set.of(507)));
        towns.add(new Town("Rochester", "New York", 211328, Set.of(585)));
        towns.add(new Town("Springfield", "Missouri", 169176, Set.of(417)));
        towns.add(new Town("Springfield", "Illinois", 114394, Set.of(217, 447)));
        towns.add(new Town("Springfield", "Massachusetts", 155929, Set.of(413)));
        towns.add(new Town("Springfield", "Oregon", 59403, Set.of(458, 541)));
        towns.add(new Town("Springfield", "Ohio", 58662, Set.of(326, 937)));
        towns.add(new Town("Kansas City", "Missouri", 508090, Set.of(816, 975)));
        towns.add(new Town("Kansas City", "Kansas", 156607, Set.of(913)));
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

        reservations.saveAll(List.of(r1, r2, r3, r4));

        // ElementCount keyword

        assertEquals(List.of("host1@openliberty.io", "host4@openliberty.io"),
                     reservations.findByInviteesElementCount(2)
                                     .map(r -> r.host)
                                     .collect(Collectors.toList()));

        assertEquals(Collections.EMPTY_LIST,
                     reservations.findByInviteesElementCount(0)
                                     .map(r -> r.host)
                                     .collect(Collectors.toList()));

        // ElementCount Function

        assertEquals(List.of("host3@openliberty.io"),
                     reservations.withInviteeCount(3)
                                     .map(r -> r.host)
                                     .collect(Collectors.toList()));

        // WithHour, WithMinute. We cannot compare the hour without knowing which time zone the database stores it in.

        assertEquals(List.of(113001L, 213002L),
                     reservations.findMeetingIdByStartWithHourBetweenAndStartWithMinute(0, 23, 15));

        assertEquals(List.of(313003L),
                     reservations.startsWithinHoursWithMinute(0, 23, 35));

        // WithSecond

        assertEquals(List.of(313003L),
                     reservations.findMeetingIdByStopWithSecond(30));

        assertEquals(List.of(113001L, 213002L, 413004L),
                     reservations.endsAtSecond(0));

        // @Select as String

        assertEquals(List.of("050-2 B120", "050-2 G105", "050-2 G105"),
                     reservations.locationsThatStartWith("050-"));

        reservations.deleteByHostNot("nobody");
    }

    /**
     * Repository methods that use the Extract annotation with a parameter of
     * Year, Quarter, Month, and Day to compare different parts of a date.
     */
    @Test
    public void testExtractFromDateFunctions() {
        ZoneOffset CDT = ZoneOffset.ofHours(-5);

        reservations.deleteByHostNot("no one");

        Reservation r1 = new Reservation();
        r1.host = "testExtractFromDateFunctions-H1@example.org";
        r1.invitees = Set.of("testExtractFromDateFunctions-A@example.org",
                             "testExtractFromDateFunctions-B@example.org",
                             "testExtractFromDateFunctions-C@example.org");
        r1.location = "050-2 H115";
        r1.meetingID = 10100101;
        r1.start = OffsetDateTime.of(2024, 12, 18, 10, 0, 0, 0, CDT);
        r1.stop = OffsetDateTime.of(2024, 12, 18, 11, 30, 0, 0, CDT);
        r1.setLengthInMinutes(90);

        Reservation r2 = new Reservation();
        r2.host = "testExtractFromDateFunctions-H2@example.org";
        r2.invitees = Set.of("testExtractFromDateFunctions-A@example.org");
        r2.location = "050-2 B120";
        r2.meetingID = 20200202;
        r2.start = OffsetDateTime.of(2024, 7, 10, 8, 0, 0, 0, CDT);
        r2.stop = OffsetDateTime.of(2024, 7, 10, 9, 30, 0, 0, CDT);
        r2.setLengthInMinutes(90);

        Reservation r3 = new Reservation();
        r3.host = "testExtractFromDateFunctions-H1@example.org";
        r3.invitees = Set.of("testExtractFromDateFunctions-B@example.org");
        r3.location = "050-2 G105";
        r3.meetingID = 30300303;
        r3.start = OffsetDateTime.of(2023, 4, 10, 8, 0, 0, 0, CDT);
        r3.stop = OffsetDateTime.of(2023, 4, 14, 17, 0, 0, 0, CDT);
        r3.setLengthInMinutes(6300);

        Reservation r4 = new Reservation();
        r4.host = "testExtractFromDateFunctions-H4@example.org";
        r4.invitees = Set.of("testExtractFromDateFunctions-A@example.org",
                             "testExtractFromDateFunctions-B@example.org");
        r4.location = "050-2 B120";
        r4.meetingID = 40400404;
        r4.start = OffsetDateTime.of(2024, 4, 11, 16, 30, 0, 0, CDT);
        r4.stop = OffsetDateTime.of(2024, 4, 11, 17, 30, 0, 0, CDT);
        r4.setLengthInMinutes(60);

        reservations.saveAll(List.of(r1, r2, r3, r4));

        assertEquals(List.of(10100101L, 20200202L, 40400404L),
                     reservations.startsInYear(2024));

        assertEquals(List.of(10100101L, 30300303L, 40400404L),
                     reservations.startsInQuarterOtherThan(3));

        assertEquals(List.of(10100101L, 20200202L),
                     reservations.endsInMonth(List.of(7, 12)));

        assertEquals(List.of(30300303L, 40400404L),
                     Arrays.stream(reservations.endsWithinDays(11, 14))
                                     .collect(ArrayList::new, ArrayList::add, ArrayList::addAll));

        assertEquals(List.of(60, 90),
                     reservations.lengthsBelow(200));

        assertEquals(4, reservations.deleteByHostNot("no one"));
    }

    /**
     * Annotatively-defined repository operation to remove and return one or more IdClass
     * instances corresponding to the removed entities.
     */
    @Test
    public void testFindAndDeleteReturningIdClassArray(HttpServletRequest request, HttpServletResponse response) {

        towns.add(new Town("Bloomington", "Minnesota", 89987, Set.of(952)));
        towns.add(new Town("Plymouth", "Minnesota", 79828, Set.of(763)));
        towns.add(new Town("Woodbury", "Minnesota", 75102, Set.of(651)));
        towns.add(new Town("Brooklyn Park", "Minnesota", 86478, Set.of(763)));

        TownId[] removed = towns.deleteWithinPopulationRange(75000, 99999);

        assertEquals(Arrays.toString(removed), 4, removed.length);

        Arrays.sort(removed, Comparator.comparing(TownId::toString));

        assertEquals("Bloomington", removed[0].name);
        assertEquals("Minnesota", removed[0].stateName);

        assertEquals("Brooklyn Park", removed[1].name);
        assertEquals("Minnesota", removed[1].stateName);

        assertEquals("Plymouth", removed[2].name);
        assertEquals("Minnesota", removed[2].stateName);

        assertEquals("Woodbury", removed[3].name);
        assertEquals("Minnesota", removed[3].stateName);

        removed = towns.deleteWithinPopulationRange(75000, 99999);

        assertEquals(Arrays.toString(removed), 0, removed.length);

        // Ensure non-matching entities remain in the database
        assertEquals(true, towns.existsById(TownId.of("Rochester", "Minnesota")));
    }

    /**
     * Repository method with the Count keyword that counts how many matching entities there are.
     */
    @Test
    public void testIdClassCountKeyword() {
        assertEquals(2L, towns.countByStateButNotTown_Or_NotTownButWithTownName("Missouri", TownId.of("Kansas City", "Missouri"),
                                                                                TownId.of("Rochester", "New York"), "Rochester"));
    }

    /**
     * Repository method with the Query annotation with JPQL that checks if any matching entities exist.
     */
    @Test
    public void testIdClassExistsInQuery() {
        assertEquals(true, towns.areFoundIn("Minnesota"));
        assertEquals(false, towns.areFoundIn("Antarctica"));
    }

    /**
     * Repository method performing a parameter-based query on a compound entity Id which is an IdClass,
     * where the method parameter is annotated with By.
     */
    @Test
    public void testIdClassFindByAnnotatedParameter() {

        assertEquals(List.of("Springfield Massachusetts",
                             "Rochester Minnesota",
                             "Kansas City Missouri"),
                     towns.largerThan(100000, TownId.of("springfield", "missouri"), "M%s")
                                     .map(c -> c.name + ' ' + c.stateName)
                                     .collect(Collectors.toList()));
    }

    /**
     * Repository method performing a parameter-based query on a compound entity Id which is an IdClass,
     * without annotating the method parameter.
     */
    @Test
    public void testIdClassFindByParametersUnannotated() {
        assertEquals(true, towns.isBiggerThan(100000, TownId.of("Rochester", "Minnesota")));
        assertEquals(false, towns.isBiggerThan(500000, TownId.of("Rochester", "Minnesota")));
    }

    /**
     * Repository method with the Find keyword that queries based on multiple IdClass parameters.
     */
    @Test
    public void testIdClassFindKeyword() {
        assertEquals(List.of("Kansas City Missouri",
                             "Rochester Minnesota",
                             "Springfield Illinois"),
                     towns.findByIdIsOneOf(TownId.of("Rochester", "Minnesota"),
                                           TownId.of("springfield", "illinois"),
                                           TownId.of("Kansas City", "Missouri"))
                                     .map(c -> c.name + ' ' + c.stateName)
                                     .collect(Collectors.toList()));

        assertEquals(List.of("Springfield Illinois",
                             "Springfield Massachusetts",
                             "Springfield Missouri",
                             "Springfield Ohio"),
                     towns.findByNameButNotId("Springfield", TownId.of("Springfield", "Oregon"))
                                     .map(c -> c.name + ' ' + c.stateName)
                                     .collect(Collectors.toList()));
    }

    /**
     * Use keyset pagination with the OrderBy annotation on a composite id that is defined by an IdClass attribute.
     * Also use named parameters, which means the keyset portion of the query will also need to use named parameters.
     */
    @Test
    public void testIdClassOrderByAnnotationWithKeysetPaginationAndNamedParameters() {
        PageRequest pagination = PageRequest.ofSize(2);

        CursoredPage<Town> page1 = towns.sizedWithin(100000, 1000000, pagination);
        assertEquals(List.of("Springfield Missouri",
                             "Springfield Massachusetts"),
                     page1.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(4L, page1.totalPages());
        assertEquals(7L, page1.totalElements());

        CursoredPage<Town> page2 = towns.sizedWithin(100000, 1000000, page1.nextPageRequest());
        assertEquals(List.of("Springfield Illinois",
                             "Rochester New York"),
                     page2.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        CursoredPage<Town> page3 = towns.sizedWithin(100000, 1000000, page2.nextPageRequest());
        assertEquals(List.of("Rochester Minnesota",
                             "Kansas City Missouri"),
                     page3.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        CursoredPage<Town> page4 = towns.sizedWithin(100000, 1000000, page3.nextPageRequest());
        assertEquals(List.of("Kansas City Kansas"),
                     page4.stream().map(c -> c.name + ' ' + c.stateName).collect(Collectors.toList()));

        assertEquals(false, page4.hasNext());
    }

    /**
     * Repository method with the Assign annotation that makes an update by assigning the IdClass instance to something else.
     */
    @Test
    public void testIdClassUpdateAssignIdClass() {
        towns.add(new Town("La Crosse", "Wisconsin", 52680, Set.of(608)));
        try {
            assertEquals(true, towns.existsById(TownId.of("La Crosse", "Wisconsin")));

            assertEquals(1, towns.replace(TownId.of("La Crosse", "Wisconsin"),
                                          "Decorah", "Iowa", 7587, Set.of(563))); // TODO TownId.of("Decorah", "Iowa"), 7587, Set.of(563)));

            assertEquals(false, towns.existsById(TownId.of("La Crosse", "Wisconsin")));
            assertEquals(true, towns.existsById(TownId.of("Decorah", "Iowa")));

            // TODO EclipseLink bug needs to be fixed:
            // java.lang.IllegalArgumentException: Can not set java.util.Set field test.jakarta.data.experimental.web.Town.areaCodes to java.lang.Integer
            //Town town = towns.findById(TownId.of("Decorah", "Iowa")).orElseThrow();
            //assertEquals("Decorah", town.name);
            //assertEquals("Iowa", town.stateName);
            //assertEquals(7587, town.population);
            //assertEquals(Set.of(563), town.areaCodes);
        } finally {
            towns.deleteWithinPopulationRange(7580, 7590);
            towns.deleteWithinPopulationRange(52600, 52700);
        }
    }

    /**
     * Repository method with the Update annotation that makes an update by assigning the IdClass components to something else.
     */
    @Test
    public void testIdClassUpdateAssignIdClassComponents() {
        towns.add(new Town("Janesville", "Wisconsin", 65615, Set.of(608)));
        try {
            assertEquals(true, towns.existsById(TownId.of("Janesville", "Wisconsin")));

            assertEquals(1, towns.replace("Janesville", "Wisconsin",
                                          "Ames", "Iowa", Set.of(515), 66427));

            assertEquals(false, towns.existsById(TownId.of("Janesville", "Wisconsin")));
            assertEquals(true, towns.existsById(TownId.of("Ames", "Iowa")));

            // TODO EclipseLink bug needs to be fixed:
            // java.lang.IllegalArgumentException: Can not set java.util.Set field test.jakarta.data.experimental.web.Town.areaCodes to java.lang.Integer
            //Town town = cities.findById(TownId.of("Decorah", "Iowa")).orElseThrow();
            //assertEquals("Ames", town.name);
            //assertEquals("Iowa", town.stateName);
            //assertEquals(66427, town.population);
            //assertEquals(Set.of(515), town.areaCodes);
        } finally {
            assertEquals(1, towns.deleteWithinPopulationRange(65000, 67000).length);
        }
    }

    /**
     * Repository method with the Update keyword that makes an update by assigning the IdClass instance to something else.
     */
    @Test
    public void testIdClassUpdateKeyword() {
        towns.add(new Town("Madison", "Wisconsin", 269840, Set.of(608)));
        try {
            assertEquals(true, towns.existsById(TownId.of("Madison", "Wisconsin")));

            // TODO enable once IdClass is supported for @Update
            // UnsupportedOperationException: @Assign IdClass
            //assertEquals(1, cities.updateIdPopulationAndAreaCodes(TownId.of("Madison", "Wisconsin"), 269840,
            //                                                      TownId.of("Des Moines", "Iowa"), 214133, Set.of(515)));

            //assertEquals(false, cities.existsById(TownId.of("Madison", "Wisconsin")));
            //assertEquals(true, cities.existsById(TownId.of("Des Moines", "Iowa")));

            // TODO EclipseLink bug needs to be fixed:
            // java.lang.IllegalArgumentException: Can not set java.util.Set field test.jakarta.data.experimental.web.Town.areaCodes to java.lang.Integer
            //Town town = cities.findById(TownId.of("Des Moines", "Iowa")).orElseThrow();
            //assertEquals("Des Moines", town.name);
            //assertEquals("Iowa", town.stateName);
            //assertEquals(214133, town.population);
            //assertEquals(Set.of(515), town.areaCodes);
        } finally {
            assertEquals(1, towns.deleteWithinPopulationRange(214000, 270000).length);
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

        assertEquals(true, shipments.dispatch(2, "READY_FOR_PICKUP",
                                              "IN_TRANSIT", "44.036217, -92.488040", OffsetDateTime.now()));
        assertEquals("IN_TRANSIT", shipments.getStatus(2));

        // @OrderBy "destination"
        assertEquals(List.of("151 4th St SE, Rochester, MN 55904",
                             "200 1st Ave SW, Rochester, MN 55902",
                             "201 4th St SE, Rochester, MN 55904"),
                     shipments.find("IN_TRANSIT")
                                     .map(o -> o.getDestination())
                                     .collect(Collectors.toList()));

        // @OrderBy "status", then "orderedAt" descending
        assertEquals(List.of(3L, 2L, 1L, 5L, 4L),
                     Stream.of(shipments.getAll())
                                     .map(o -> o.getId())
                                     .collect(Collectors.toList()));

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
     * Use the provided methods of a Repository<T, K> interface that is a copy of Jakarta NoSQL's.
     */
    @Test
    public void testRepositoryBuiltInMethods() {
        ZoneOffset CDT = ZoneOffset.ofHours(-5);

        // remove data that other tests previously inserted to the same table
        reservations.deleteByHostNot("never-ever-used@example.org");

        assertEquals(0, reservations.countAll());

        Reservation r1 = new Reservation();
        r1.host = "testRepository-host1@example.org";
        r1.invitees = Set.of("testRepository-1a@example.org", "testRepository-1b@example.org");
        r1.location = "050-2 G105";
        r1.meetingID = 10020001;
        r1.start = OffsetDateTime.of(2022, 5, 23, 9, 0, 0, 0, CDT);
        r1.stop = OffsetDateTime.of(2022, 5, 23, 10, 0, 0, 0, CDT);
        r1.setLengthInMinutes(60);

        assertEquals(Boolean.FALSE, reservations.existsByMeetingId(r1.meetingID));

        Reservation inserted = reservations.save(r1);
        assertEquals(r1.host, inserted.host);
        assertEquals(r1.invitees, inserted.invitees);
        assertEquals(r1.location, inserted.location);
        assertEquals(r1.meetingID, inserted.meetingID);
        assertEquals(r1.start, inserted.start);
        assertEquals(r1.stop, inserted.stop);

        assertEquals(Boolean.TRUE, reservations.existsByMeetingId(r1.meetingID));

        assertEquals(1, reservations.countAll());

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

        List<Reservation> insertedOrUpdated = reservations.saveAll(new AbstractList<>() {
            List<Reservation> list = Arrays.asList(r1, r2, r3, r4);

            @Override
            public Reservation get(int index) {
                return list.get(index);
            }

            @Override
            public int size() {
                return list.size();
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

        assertEquals(true, reservations.existsByMeetingId(r3.meetingID));

        assertEquals(4, reservations.count());

        Map<Long, Reservation> found = reservations.findByMeetingIdIn(List.of(r4.meetingID, r2.meetingID))
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

        reservations.deleteByMeetingIdIn(Set.of(r1.meetingID, r4.meetingID));

        assertEquals(Boolean.FALSE, reservations.existsByMeetingId(r4.meetingID));

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
        reservations.removeByHostNotIn(Set.of("never-ever-used@example.org"));

        assertEquals(0, reservations.countAll());

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
        assertEquals(List.of(10030002L, 10030005L, 10030007L),
                     reservationList
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030005L, 10030007L, 10030009L),
                     reservations.findByLocationContainsOrderByMeetingID("-2 B1")
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030001L, 10030002L, 10030004L, 10030006L, 10030008L),
                     reservations.findByMeetingIDOrLocationLikeAndStartAndStopOrHost(10030006,
                                                                                     "050-2 %",
                                                                                     OffsetDateTime.of(2022, 5, 25, 9, 0, 0, 0, CDT),
                                                                                     OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT),
                                                                                     "testRepositoryCustom-host4@example.org")
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030004L, 10030005L),
                     reservations.findByStartBetweenAndLocationIn(OffsetDateTime.of(2022, 5, 25, 9, 30, 0, 0, CDT),
                                                                  OffsetDateTime.of(2022, 5, 25, 11, 30, 0, 0, CDT),
                                                                  List.of("050-2 H115", "050-2 B120", "050-2 B125"))
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030009L, 10030007L, 10030008L, 10030006L),
                     reservations.findByStartGreaterThanOrderByStartDescStopDesc(OffsetDateTime.of(2022, 5, 25, 0, 0, 0, 0, CDT),
                                                                                 Limit.of(4))
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030007L, 10030008L, 10030006L),
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

        assertEquals(List.of(10030001L, 10030002L, 10030003L, 10030009L),
                     reservations.findByStartNotBetween(OffsetDateTime.of(2022, 5, 25, 9, 30, 0, 0, CDT),
                                                        OffsetDateTime.of(2022, 5, 25, 13, 30, 0, 0, CDT))
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030007L, 10030008L, 10030009L),
                     reservations.findByStopGreaterThanEqual(OffsetDateTime.of(2022, 5, 25, 13, 0, 0, 0, CDT))
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030003L, 10030001L, 10030004L, 10030006L, 10030009L, 10030005L, 10030007L, 10030002L, 10030008L),
                     reservations.findByStopGreaterThanOrderByLocationDescHostAscStopAsc(OffsetDateTime.of(2022, 5, 25, 8, 0, 0, 0, CDT))
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030001L, 10030005L, 10030007L, 10030002L, 10030003L, 10030006L, 10030009L, 10030004L, 10030008L),
                     reservations.findByStopLessThanOrderByHostAscLocationDescStart(OffsetDateTime.of(2022, 5, 26, 0, 0, 0, 0, CDT))
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .collect(Collectors.toList()));

        // Reverse ordering to the above using Sort:
        assertEquals(List.of(10030008L, 10030004L, 10030009L, 10030006L, 10030003L, 10030002L, 10030007L, 10030005L, 10030001L),
                     reservations.findByStopLessThan(OffsetDateTime.of(2022, 5, 26, 0, 0, 0, 0, CDT),
                                                     Sort.desc("host"),
                                                     Sort.asc("location"),
                                                     Sort.desc("start"))
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030001L, 10030002L, 10030003L, 10030007L, 10030008L),
                     reservations.findByStopOrStart(OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT),
                                                    OffsetDateTime.of(2022, 5, 25, 13, 0, 0, 0, CDT))
                                     .map(r -> r.meetingID)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(List.of("030-2 E314", "050-2 B125", "050-2 G105"),
                     reservations.findByStopOrStartOrStart(OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT),
                                                           OffsetDateTime.of(2022, 5, 25, 11, 0, 0, 0, CDT),
                                                           OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT))
                                     .parallel()
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030004L, 10030005L, 10030006L, 10030009L),
                     reservations.findByStopOrStartOrStartOrStart(OffsetDateTime.of(2022, 5, 25, 11, 0, 0, 0, CDT),
                                                                  OffsetDateTime.of(2022, 5, 25, 7, 30, 0, 0, CDT),
                                                                  OffsetDateTime.of(2022, 5, 25, 11, 0, 0, 0, CDT),
                                                                  OffsetDateTime.of(2022, 5, 25, 14, 0, 0, 0, CDT))
                                     .parallel()
                                     .sorted()
                                     .boxed()
                                     .collect(Collectors.toList()));

        assertEquals(List.of(OffsetDateTime.of(2022, 5, 25, 10, 0, 0, 0, CDT).toInstant(),
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
        assertEquals(List.of(10030009L, 10030008L, 10030007L, 10030006L),
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
        assertEquals(List.of(10030005L, 10030004L, 10030003L, 10030002L),
                     page2
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .collect(Collectors.toList()));

        Page<Reservation> page3 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                                    page2.nextPageRequest(),
                                                                    Sort.desc("meetingID"));
        assertEquals(List.of(10030001L),
                     page3
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .collect(Collectors.toList()));
        assertEquals(false, page3.hasNext());
        assertEquals(true, page3.hasContent());
        assertEquals(1, page3.numberOfElements());

        // Paging that comes out even:
        page2 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                  PageRequest.ofPage(2).size(3),
                                                  Sort.desc("meetingID"));
        assertEquals(List.of(10030006L, 10030005L, 10030004L),
                     page2
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .collect(Collectors.toList()));
        page3 = reservations.findByHostStartsWith("testRepositoryCustom-host",
                                                  page2.nextPageRequest(),
                                                  Sort.desc("meetingID"));
        assertEquals(List.of(10030003L, 10030002L, 10030001L),
                     page3
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .collect(Collectors.toList()));
        assertEquals(false, page3.hasNext());

        // Page of nothing
        page1 = reservations.findByHostStartsWith("Not Found", PageRequest.ofSize(100), Sort.asc("meetingID"));
        assertEquals(1L, page1.pageRequest().page());
        assertEquals(false, page1.hasContent());
        assertEquals(0, page1.numberOfElements());
        assertEquals(false, page1.hasNext());
        assertEquals(0L, page1.totalElements());
        assertEquals(0L, page1.totalPages());

        // find by member of a collection
        assertEquals(List.of(10030002L, 10030007L),
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
        assertEquals(List.of(10030002L, 10030005L, 10030007L),
                     reservations.findByHostIgnoreCaseEndsWith("HOST2@EXAMPLE.ORG")
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(List.of(10030002L, 10030005L, 10030007L),
                     reservations.findByHostIgnoreCaseEndsWith("Host2@Example.org") // should match regardless of case
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .sorted()
                                     .collect(Collectors.toList()));

        // StartsWith
        assertEquals(List.of(10030005L, 10030007L, 10030009L),
                     reservations.findByLocationStartsWith("050-2 B")
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .sorted()
                                     .collect(Collectors.toList()));

        assertEquals(false, reservations.deleteByHostIn(List.of("testRepositoryCustom-host5@example.org")));

        assertEquals(true, reservations.deleteByHostIn(List.of("testRepositoryCustom-host1@example.org",
                                                               "testRepositoryCustom-host3@example.org")));

        assertEquals(List.of(10030002L, 10030004L, 10030005L, 10030008L),
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
        assertEquals(List.of(1012004L), found);

        // Update multiple by various conditions
        assertEquals(2, reservations.updateByHostAndLocationSetLocation("testRepositoryUpdateMethods-host1@example.org",
                                                                        "050-2 A101",
                                                                        "050-2 H115"));
        assertEquals(List.of(1012001L, 1012003L),
                     reservations.findByLocationContainsOrderByMeetingID("H115")
                                     .stream()
                                     .map(r -> r.meetingID)
                                     .collect(Collectors.toList()));

        reservations.deleteByHostNot("unused@example.org");
    }

    /**
     * Experiment with making a repository method return a record.
     */
    @Test
    public void testSelectAsRecord() {
        ZoneOffset CDT = ZoneOffset.ofHours(-5);

        // remove data that other tests previously inserted to the same table
        reservations.removeByHostNotIn(Set.of("never-ever-used@example.org"));

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

        reservations.saveAll(List.of(r1, r2, r3, r4));

        ReservedTimeSlot[] reserved = reservations.findTimeSlotByLocationAndStartBetweenOrderByStart("30-2 C206",
                                                                                                     OffsetDateTime.of(2022, 6, 3, 0, 0, 0, 0, CDT),
                                                                                                     OffsetDateTime.of(2022, 6, 3, 23, 59, 59, 0, CDT));
        assertArrayEquals(new ReservedTimeSlot[] { new ReservedTimeSlot(r2.start, r2.stop),
                                                   new ReservedTimeSlot(r1.start, r1.stop),
                                                   new ReservedTimeSlot(r3.start, r3.stop) },
                          reserved,
                          Comparator.<ReservedTimeSlot, Instant> comparing(o -> o.start().toInstant())
                                          .thenComparing(Comparator.<ReservedTimeSlot, Instant> comparing(o -> o.stop().toInstant())));
    }
}
