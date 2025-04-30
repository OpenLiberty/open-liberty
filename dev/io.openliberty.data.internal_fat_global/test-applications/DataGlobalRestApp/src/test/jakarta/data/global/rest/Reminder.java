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
import java.time.MonthDay;
import java.time.Year;

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
                              MonthDay monthDayCreated) {
        Reminder r = new Reminder();
        r.id = id;
        r.message = message;
        r.forDayOfWeek = forDayOfWeek;
        r.yearCreated = yearCreated;
        r.monthDayCreated = monthDayCreated;
        return r;
    }

    @Override
    public String toString() {
        return "Reminder#" + id + ":" + message + " on " + forDayOfWeek +
               " created " + yearCreated + " " + monthDayCreated;
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
}
