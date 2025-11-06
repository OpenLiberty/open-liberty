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
import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.In;
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotEqualTo;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Is;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;
import jakarta.data.repository.Update;

import io.openliberty.data.repository.function.ElementCount;
import io.openliberty.data.repository.function.Extract;
import io.openliberty.data.repository.update.Assign;

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
    List<Long> endsInMonth(@By("stop") @Extract(MONTH) @Is(In.class) Iterable<Integer> months);

    @Find
    @Select("meetingID")
    @OrderBy("meetingID")
    long[] endsWithinDays(@By("stop") @Extract(DAY) @Is(AtLeast.class) int minDayOfMonth,
                          @By("stop") @Extract(DAY) @Is(AtMost.class) int maxDayOfMonth);

    @Find
    @Select("meetingId")
    @OrderBy(ID)
    List<Long> endsAtSecond(@By("stop") @Extract(SECOND) int second);

    Boolean existsByMeetingId(long meetingID);

    Iterable<Reservation> findByHost(String host);

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

    @Select("location")
    Stream<String> findByStopOrStartOrStart(OffsetDateTime stop,
                                            OffsetDateTime start1,
                                            OffsetDateTime start2);

    @Select("meetingID")
    LongStream findByStopOrStartOrStartOrStart(OffsetDateTime stop,
                                               OffsetDateTime start1,
                                               OffsetDateTime start2,
                                               OffsetDateTime start3);

    // Use a stream of record as the return type
    @Select("start")
    @Select("stop")
    Stream<ReservedTimeSlot> findByStopOrStopOrStop(OffsetDateTime stop1,
                                                    OffsetDateTime stop2,
                                                    OffsetDateTime stop3);

    Page<Reservation> findByHostStartsWith(String hostPrefix, PageRequest pagination, Sort<Reservation> sort);

    LinkedHashSet<Reservation> findByInviteesContainsOrderByMeetingID(String invitee);

    HashSet<Reservation> findByLocationAndInviteesNotContains(String location, String noninvitee);

    @Find
    @Select("meetingId")
    List<Long> findMeetingIdStoppingAtSecond(@By("stop") @Extract(SECOND) int second);

    // Use a record as the return type
    @Find
    @Select("start")
    @Select("stop")
    @OrderBy("start")
    ReservedTimeSlot[] findTimeSlotWithin(String location,
                                          @By("start") @Is(AtLeast.class) OffsetDateTime startAfter,
                                          @By("start") @Is(AtMost.class) OffsetDateTime startBefore);

    ArrayDeque<Reservation> findByLocationStartsWith(String locationPrefix);

    CopyOnWriteArrayList<Reservation> findByHostIgnoreCaseEndsWith(String hostPostfix);

    @Query("SELECT DISTINCT r.lengthInMinutes FROM Reservation r WHERE r.lengthInMinutes<:lengthInMinutes")
    @OrderBy("lengthInMinutes")
    List<Long> lengthsBelow(@Param("lengthInMinutes") int max);

    @Find
    @Select("location")
    @OrderBy("location")
    List<String> locations(@By("location") Like locationNamePrefix);

    int removeByHostNotIn(Collection<String> hosts);

    @Update
    boolean setHost(@By("meetingID") long id,
                    @Assign("host") String newHost);

    @Update
    int setLocation(@By("host") String host,
                    @By("location") String currentLocation,
                    @Assign("location") String newLocation);

    @Find
    @Select("meetingId")
    @OrderBy("host")
    List<Long> startingWithin(@By("start") @Extract(HOUR) @Is(AtLeast.class) int minHour,
                              @By("start") @Extract(HOUR) @Is(AtMost.class) int maxHour,
                              @By("start") @Extract(MINUTE) @Is int minute);

    @Find
    @Select("meetingID")
    @OrderBy("meetingID")
    List<Long> startsInQuarterOtherThan(@By("start") @Extract(QUARTER) @Is(NotEqualTo.class) int quarterToExclude);

    @Find
    @Select("meetingID")
    @OrderBy("meetingID")
    List<Long> startsInYear(@By("start") @Extract(YEAR) int year);

    @Find
    @Select("meetingId")
    @OrderBy("host")
    List<Long> startsWithinHoursWithMinute(@By("start") @Extract(HOUR) @Is(AtLeast.class) int minHour,
                                           @By("start") @Extract(HOUR) @Is(AtMost.class) int maxHour,
                                           @By("start") @Extract(MINUTE) int minute);

    @Find
    @OrderBy(ID)
    Stream<Reservation> withInviteeCount(@By("invitees") @ElementCount int size);
}