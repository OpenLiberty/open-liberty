/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.web;

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
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import io.openliberty.data.Data;
import io.openliberty.data.Limit;
import io.openliberty.data.Page;
import io.openliberty.data.Pagination;
import io.openliberty.data.Repository;
import io.openliberty.data.Result;
import io.openliberty.data.Select;
import io.openliberty.data.Sort;
import io.openliberty.data.Sorts;

/**
 * Uses the Repository interface that is copied from Jakarta NoSQL
 */
@Data(Reservation.class)
public interface Reservations extends Repository<Reservation, Long> {
    boolean deleteByHostIn(List<String> hosts);

    long deleteByHostNot(String host);

    int deleteByHostNotIn(Collection<String> hosts);

    Iterable<Reservation> findByHost(String host);

    Collection<Reservation> findByLocationLikeOrderByMeetingID(String locationSubstring);

    List<Reservation> findByMeetingIDOrLocationLikeAndStartAndStopOrHost(long meetingID,
                                                                         String location,
                                                                         OffsetDateTime start,
                                                                         OffsetDateTime stop,
                                                                         String host);

    ArrayList<Reservation> findByStartBetweenAndLocationIn(OffsetDateTime minStart, OffsetDateTime maxStart, List<String> locations);

    @Limit(4)
    CopyOnWriteArrayList<Reservation> findByStartGreaterThanOrderByStartDescOrderByStopDesc(OffsetDateTime startAfter);

    Reservation[] findByStartLessThanOrStartGreaterThanOrderByMeetingIDDesc(OffsetDateTime startBefore, OffsetDateTime startAfter);

    Vector<Reservation> findByStartNotBetween(OffsetDateTime startBefore, OffsetDateTime startAfter);

    LinkedList<Reservation> findByStopGreaterThanEqual(OffsetDateTime minEndTime);

    Stack<Reservation> findByStopGreaterThanOrderByLocationDescOrderByHostOrderByStopAsc(OffsetDateTime endAfter);

    UserDefinedCollection<Reservation> findByStopLessThan(OffsetDateTime maxEndTime, Sorts sorts);

    AbstractCollection<Reservation> findByStopLessThanEqual(OffsetDateTime maxEndTime);

    AbstractList<Reservation> findByStopLessThanOrderByHostAscOrderByLocationDescOrderByStart(OffsetDateTime endBefore);

    Stream<Reservation> findByStopOrStart(OffsetDateTime stop, OffsetDateTime start);

    @Select("location")
    Stream<String> findByStopOrStartOrStart(OffsetDateTime stop, OffsetDateTime start1, OffsetDateTime start2);

    @Select("meetingID")
    LongStream findByStopOrStartOrStartOrStart(OffsetDateTime stop, OffsetDateTime start1, OffsetDateTime start2, OffsetDateTime start3);

    // Use a stream of record as the return type
    @Result(ReservedTimeSlot.class)
    @Select({ "start", "stop" })
    Stream<ReservedTimeSlot> findByStopOrStopOrStop(OffsetDateTime stop1, OffsetDateTime stop2, OffsetDateTime stop3);

    // Possibly better way of doing the above?
    // @Result(ReservedTimeSlot.class)
    // @Select({ "start", "stop" })
    // Stream<ReservedTimeSlot> findByStopOrStopOrStart(OffsetDateTime stop1, OffsetDateTime stop2, OffsetDateTime stop3);

    Publisher<Reservation> findByHostLikeOrderByMeetingID(String hostSubstring);

    Page<Reservation> findByHostLike(String hostSubstring, Pagination pagination, Sort sort);

    LinkedHashSet<Reservation> findByInviteesContainsOrderByMeetingID(String invitee);

    HashSet<Reservation> findByLocationAndInviteesNotContains(String location, String noninvitee);

    // Use a record as the return type
    @Select({ "start", "stop" })
    ReservedTimeSlot[] findByLocationAndStartBetweenOrderByStart(String location, OffsetDateTime startAfter, OffsetDateTime startBefore);

    LinkedBlockingQueue<Reservation> findByLowerLocationIn(List<String> locations);

    ArrayDeque<Reservation> findByLocationStartsWith(String locationPrefix);

    CopyOnWriteArrayList<Reservation> findByUpperHostEndsWith(String hostPostfix);

    int updateByHostAndLocationSetLocation(String host, String currentLocation, String newLocation);

    boolean updateByMeetingIDSetHost(long meetingID, String newHost);
}