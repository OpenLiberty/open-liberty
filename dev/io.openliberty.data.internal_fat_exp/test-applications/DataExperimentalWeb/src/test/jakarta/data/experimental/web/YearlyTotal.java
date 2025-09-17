/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.experimental.web;

import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * An entity that tries out some temporal and CharSequence types that aren't
 * mentioned in the Jakarta Persistence specification (except for Year, which
 * is mentioned but is not indicated whether or not it can be an Id).
 */
@Entity
public class YearlyTotal {

    public MonthDay bestDay;

    public YearMonth bestMonth;

    public StringBuffer buffer;

    public StringBuilder builder;

    // This allows a String value to be written, but not read, at which point
    // EclipseLink thinks the value needs to be converted to a byte[] and fails.
    //@Column(columnDefinition = "VARCHAR(50)")
    public CharSequence comments;

    // avoid collision with Derby reserved word YEAR
    @Column(name = "YEARVALUE")
    @Id
    public Year year;

    public static YearlyTotal of(Year year,
                                 MonthDay bestDay,
                                 YearMonth bestMonth,
                                 String buffer,
                                 String builder,
                                 String comments) {
        YearlyTotal y = new YearlyTotal();
        y.year = year;
        y.bestDay = bestDay;
        y.bestMonth = bestMonth;
        y.buffer = new StringBuffer(buffer);
        y.builder = new StringBuilder(builder);
        y.comments = comments;
        return y;
    }

    @Override
    public String toString() {
        return "YearInfo(" + year + "): " + bestMonth + " " + bestDay +
               ", buffer: " + buffer +
               ", builder: " + builder +
               ", comments: " + comments;
    }

}