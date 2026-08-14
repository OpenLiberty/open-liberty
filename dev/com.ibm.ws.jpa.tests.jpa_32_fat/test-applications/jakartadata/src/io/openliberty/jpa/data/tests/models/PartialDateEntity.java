/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity to test EclipseLink's handling of partial date types (Year, MonthDay, YearMonth).
 * This replicates the issue where EclipseLink stores Year as NUMBER(10) in H2
 * and fails to convert them properly when reading, throwing:
 * ConversionException: The object [2,022], of class [class java.math.BigDecimal],
 * could not be converted to [class java.time.OffsetTime].
 */
@Entity
public class PartialDateEntity {

    @Id
    @Column(name = "YEARVALUE")
    public Year year;

    public MonthDay bestDay;

    public YearMonth bestMonth;

    public String description;

    public PartialDateEntity() {
    }

    public PartialDateEntity(Year year, MonthDay bestDay, YearMonth bestMonth, String description) {
        this.year = year;
        this.bestDay = bestDay;
        this.bestMonth = bestMonth;
        this.description = description;
    }

    @Override
    public String toString() {
        return "PartialDateEntity(year=" + year + ", bestDay=" + bestDay +
               ", bestMonth=" + bestMonth + ", description=" + description + ")";
    }
}
