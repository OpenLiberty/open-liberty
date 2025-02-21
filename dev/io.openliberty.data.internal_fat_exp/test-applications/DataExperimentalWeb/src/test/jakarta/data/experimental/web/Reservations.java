/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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

import static io.openliberty.data.repository.Is.Op.GreaterThanEqual;
import static io.openliberty.data.repository.Is.Op.In;
import static io.openliberty.data.repository.Is.Op.LessThanEqual;
import static io.openliberty.data.repository.Is.Op.Not;
import static io.openliberty.data.repository.Is.Op.Prefixed;
import static io.openliberty.data.repository.function.Extract.Field.DAY;
import static io.openliberty.data.repository.function.Extract.Field.HOUR;
import static io.openliberty.data.repository.function.Extract.Field.MINUTE;
import static io.openliberty.data.repository.function.Extract.Field.MONTH;
import static io.openliberty.data.repository.function.Extract.Field.QUARTER;
import static io.openliberty.data.repository.function.Extract.Field.SECOND;
import static io.openliberty.data.repository.function.Extract.Field.YEAR;
import static jakarta.data.repository.By.ID;

import java.time.OffsetDateTime;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;

import io.openliberty.data.repository.Is;
import io.openliberty.data.repository.Or;
import io.openliberty.data.repository.function.ElementCount;
import io.openliberty.data.repository.function.Extract;

/**
 * Covers various patterns that are extensions to Jakarta Data, such as
 * a variety of return types for collections, an Extract annotation,
 * various other annotations for conditions, and a Select annotation.
 */
@Repository
public interface Reservations extends BasicRepository<Reservation, Long> {

    @Query("SELECT COUNT(o) FROM Reservation o") // JPQL
    int count();

    int countAll();

    boolean deleteByHostIn(List<String> hosts);

    long deleteByHostNot(String host);

    void deleteByMeetingIdIn(Iterable<Long> ids);

    @Find
    @Select("meetingID")
    @OrderBy("meetingID")
    List<Long> endsInMonth(@By("stop") @Extract(MONTH) @Is(In) Iterable<Integer> months);

    @Find
    @Select("meetingID")
    @OrderBy("meetingID")
    long[] endsWithinDays(@By("stop") @Extract(DAY) @Is(GreaterThanEqual) int minDayOfMonth,
                          @By("stop") @Extract(DAY) @Is(LessThanEqual) int maxDayOfMonth);

    @Find
    @Select("meetingId")
    @OrderBy(ID)
    List<Long> endsAtSecond(@By("stop") @Extract(SECOND) int second);

    Boolean existsByMeetingId(long meetingID);

    Iterable<Reservation> findByHost(String host);

    @OrderBy(ID)
    Stream<Reservation> findByInviteesElementCount(int size);

    Collection<Reservation> findByLocationContainsOrderByMeetingID(String locationSubstring);

    Stream<Reservation> findByMeetingIdIn(Iterable<Long> ids);

    List<Reservation> findByMeetingIDOrLocationLikeAndStartAndStopOrHost(long meetingID,
                                                                         String location,
                                                                         OffsetDateTime start,
                                                                         OffsetDateTime stop,
                                                                         String host);

    ArrayList<Reservation> findByStartBetweenAndLocationIn(OffsetDateTime minStart, OffsetDateTime maxStart, List<String> locations);

    CopyOnWriteArrayList<Reservation> findByStartGreaterThanOrderByStartDescStopDesc(OffsetDateTime startAfter, Limit maxResults);

    Reservation[] findByStartLessThanOrStartGreaterThanOrderByMeetingIDDesc(OffsetDateTime startBefore, OffsetDateTime startAfter);

    Vector<Reservation> findByStartNotBetween(OffsetDateTime startBefore, OffsetDateTime startAfter);

    LinkedList<Reservation> findByStopGreaterThanEqual(OffsetDateTime minEndTime);

    Stack<Reservation> findByStopGreaterThanOrderByLocationDescHostAscStopAsc(OffsetDateTime endAfter);

    UserDefinedCollection<Reservation> findByStopLessThan(OffsetDateTime maxEndTime, Sort<?>... sortBy);

    AbstractCollection<Reservation> findByStopLessThanEqual(OffsetDateTime maxEndTime);

    AbstractList<Reservation> findByStopLessThanOrderByHostAscLocationDescStart(OffsetDateTime endBefore);

    Stream<Reservation> findByStopOrStart(OffsetDateTime stop, OffsetDateTime start);

    @Find
    @Select("location")
    Stream<String> findByStopOrStartAtAnyOf(OffsetDateTime stop,
                                            @Or @By("start") OffsetDateTime start1,
                                            @Or @By("start") OffsetDateTime start2);

    @Find
    @Select("meetingID")
    LongStream findByStopOrStartAtAnyOf(OffsetDateTime stop,
                                        @Or @By("start") OffsetDateTime start1,
                                        @Or @By("start") OffsetDateTime start2,
                                        @Or @By("start") OffsetDateTime start3);

    // Use a stream of record as the return type
    @Find
    @Select({ "start", "stop" })
    Stream<ReservedTimeSlot> findByStoppingAtAnyOf(@By("stop") OffsetDateTime stop1,
                                                   @Or @By("stop") OffsetDateTime stop2,
                                                   @Or @By("stop") OffsetDateTime stop3);

    Page<Reservation> findByHostStartsWith(String hostPrefix, PageRequest pagination, Sort<Reservation> sort);

    LinkedHashSet<Reservation> findByInviteesContainsOrderByMeetingID(String invitee);

    HashSet<Reservation> findByLocationAndInviteesNotContains(String location, String noninvitee);

    @OrderBy("host")
    List<Long> findMeetingIdByStartWithHourBetweenAndStartWithMinute(int minHour, int maxHour, int minute);

    List<Long> findMeetingIdByStopWithSecond(int second);

    // Use a record as the return type
    @Find
    @Select({ "start", "stop" })
    @OrderBy("start")
    ReservedTimeSlot[] findTimeSlotWithin(String location,
                                          @By("start") @Is(GreaterThanEqual) OffsetDateTime startAfter,
                                          @By("start") @Is(LessThanEqual) OffsetDateTime startBefore);

    ArrayDeque<Reservation> findByLocationStartsWith(String locationPrefix);

    CopyOnWriteArrayList<Reservation> findByHostIgnoreCaseEndsWith(String hostPostfix);

    @Query("SELECT DISTINCT r.lengthInMinutes FROM Reservation r WHERE r.lengthInMinutes<:lengthInMinutes")
    @OrderBy("lengthInMinutes")
    List<Long> lengthsBelow(@Param("lengthInMinutes") int max);

    @Find
    @Select("location")
    @OrderBy("location")
    List<String> locationsThatStartWith(@By("location") @Is(Prefixed) String beginningOfLocationName);

    int removeByHostNotIn(Collection<String> hosts);

    @Find
    @Select("meetingID")
    @OrderBy("meetingID")
    List<Long> startsInQuarterOtherThan(@By("start") @Extract(QUARTER) @Is(Not) int quarterToExclude);

    @Find
    @Select("meetingID")
    @OrderBy("meetingID")
    List<Long> startsInYear(@By("start") @Extract(YEAR) int year);

    @Find
    @Select("meetingId")
    @OrderBy("host")
    List<Long> startsWithinHoursWithMinute(@By("start") @Extract(HOUR) @Is(GreaterThanEqual) int minHour,
                                           @By("start") @Extract(HOUR) @Is(LessThanEqual) int maxHour,
                                           @By("start") @Extract(MINUTE) int minute);

    int updateByHostAndLocationSetLocation(String host, String currentLocation, String newLocation);

    boolean updateByMeetingIDSetHost(long meetingID, String newHost);

    @Find
    @OrderBy(ID)
    Stream<Reservation> withInviteeCount(@By("invitees") @ElementCount int size);
}