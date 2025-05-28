/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.global.rest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * A simple entity for a repository that relies on a DataSource with a
 * java:global JNDI name that is defined in this application.
 */
@Entity
public class Reminder {

    @Column
    @Convert(converter = ZonedDateTimeConverter.class)
    public ZonedDateTime expiresAt;

    @Column(nullable = false)
    public DayOfWeek forDayOfWeek;

    @Id
    public long id;

    @Column(nullable = false)
    public String message;

    @Column(nullable = false)
    @Convert(converter = MonthDayConverter.class)
    public MonthDay monthDayCreated;

    @Column(nullable = false)
    @JsonbTypeAdapter(YearAdapter.class)
    public Year yearCreated;

    public static Reminder of(long id,
                              String message,
                              DayOfWeek forDayOfWeek,
                              Year yearCreated,
                              MonthDay monthDayCreated,
                              ZonedDateTime expiresAt) {
        Reminder r = new Reminder();
        r.id = id;
        r.message = message;
        r.forDayOfWeek = forDayOfWeek;
        r.yearCreated = yearCreated;
        r.monthDayCreated = monthDayCreated;
        // limit to milliseconds precision
        r.expiresAt = expiresAt.withNano(expiresAt.getNano() / 1000000 * 100000);
        return r;
    }

    @Override
    public String toString() {
        return "Reminder#" + id + ":" + message + " on " + forDayOfWeek +
               " created " + yearCreated + " " + monthDayCreated +
               " expires " + expiresAt;
    }

    /**
     * Adds an unused year to MondayDay's representation in the database
     * so that EclipseLink will allow us to use the EXTRACT operation on it.
     * If support is ever added to Jakarta Persistence, we can remove this
     * converter.
     */
    static class MonthDayConverter //
                    implements AttributeConverter<MonthDay, LocalDate> {

        @Override
        public LocalDate convertToDatabaseColumn(MonthDay md) {
            return md.atYear(0);
        }

        @Override
        public MonthDay convertToEntityAttribute(LocalDate date) {
            return MonthDay.of(date.getMonth(), date.getDayOfMonth());
        }
    }

    /**
     * JSON-B doesn't have support for java.time.Year, so we are converting
     * it to int. If support is ever added to JSON-B or Yasson, we can
     * remove this adapter.
     */
    static class YearAdapter implements JsonbAdapter<Year, Integer> {

        @Override
        public Year adaptFromJson(Integer value) {
            return Year.of(value);
        }

        @Override
        public Integer adaptToJson(Year year) {
            return year.getValue();
        }
    }

    /**
     * Converts ZonedDateTime to a LocalDateTime, storing the zone information
     * as the first two characters of the key value from ZoneId.SHORT_IDS in the
     * unused fractional microseconds.
     * If support is ever added to Jakarta Persistence, we can remove this
     * converter.
     */
    static class ZonedDateTimeConverter //
                    implements AttributeConverter<ZonedDateTime, LocalDateTime> {

        @Override
        public LocalDateTime convertToDatabaseColumn(ZonedDateTime zoned) {
            if (zoned == null)
                return null;

            String id = zoned.getZone().getId();
            LocalDateTime local = zoned.toLocalDateTime();
            int nanos = local.getNano();
            int last6 = nanos % 1000000; // last 6 digits beyond milliseconds
            if (last6 == 0) {
                for (Map.Entry<String, String> entry : ZoneId.SHORT_IDS.entrySet())
                    if (entry.getValue().equals(id)) {
                        String shortId = entry.getKey();
                        char c0 = shortId.charAt(0);
                        char c1 = shortId.charAt(1);
                        last6 = 1000 * (26 * (c0 - 'A') + (c1 - 'A'));
                    }
            } else {
                throw new UnsupportedOperationException("microseconds are not allowed");
            }

            if (last6 == 0)
                throw new UnsupportedOperationException("zone id: " + id);

            local = local.withNano(nanos + last6);
            return local;
        }

        @Override
        public ZonedDateTime convertToEntityAttribute(LocalDateTime local) {
            if (local == null)
                return null;

            int nanos = local.getNano();
            int last6 = nanos % 1000000;
            int mid3 = last6 / 1000;
            char c0 = (char) ('A' + mid3 / 26);
            char c1 = (char) ('A' + mid3 % 26);
            String shortId = String.valueOf(new char[] { c0, c1, 'T' });
            String id = ZoneId.SHORT_IDS.get(shortId);
            local = local.withNano(nanos / 1000000 * 1000000); // keep milliseconds only
            return ZonedDateTime.of(local, ZoneId.of(id));
        }
    }
}
