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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import io.openliberty.data.Data;
import io.openliberty.data.Repository;
import io.openliberty.data.Select;

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

    Reservation[] findByStartLessThanOrStartGreaterThanOrderByMeetingIDDesc(OffsetDateTime startBefore, OffsetDateTime startAfter);

    Vector<Reservation> findByStartNotBetween(OffsetDateTime startBefore, OffsetDateTime startAfter);

    LinkedList<Reservation> findByStopGreaterThanEqual(OffsetDateTime minEndTime);

    Stack<Reservation> findByStopGreaterThanOrderByLocationDescOrderByHostOrderByStopAsc(OffsetDateTime endAfter);

    AbstractCollection<Reservation> findByStopLessThanEqual(OffsetDateTime maxEndTime);

    AbstractList<Reservation> findByStopLessThanOrderByHostAscOrderByLocationDescOrderByStart(OffsetDateTime endBefore);

    Stream<Reservation> findByStopOrStart(OffsetDateTime stop, OffsetDateTime start);

    @Select("location")
    Stream<String> findByStopOrStartOrStart(OffsetDateTime stop, OffsetDateTime start1, OffsetDateTime start2);

    @Select("meetingID")
    LongStream findByStopOrStartOrStartOrStart(OffsetDateTime stop, OffsetDateTime start1, OffsetDateTime start2, OffsetDateTime start3);

    // Use a stream of record as the return type
    @Select(type = ReservedTimeSlot.class, value = { "start", "stop" })
    Stream<ReservedTimeSlot> findByStopOrStopOrStop(OffsetDateTime stop1, OffsetDateTime stop2, OffsetDateTime stop3);

    // Use a record as the return type
    @Select({ "start", "stop" })
    ReservedTimeSlot[] findByLocationAndStartBetweenOrderByStart(String location, OffsetDateTime startAfter, OffsetDateTime startBefore);

}