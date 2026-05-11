/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.models;

import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity to reproduce EclipseLink conversion error with java.time.Year fields.
 */
@Entity
public class PartialDateEntity {

    @Id
    @Column(name = "YEARVALUE")
    public Year year;

    public MonthDay bestDay;

    public YearMonth bestMonth;

    public String description;

    public Year getYear() {
        return year;
    }

    public void setYear(Year year) {
        this.year = year;
    }

    public MonthDay getBestDay() {
        return bestDay;
    }

    public void setBestDay(MonthDay bestDay) {
        this.bestDay = bestDay;
    }

    public YearMonth getBestMonth() {
        return bestMonth;
    }

    public void setBestMonth(YearMonth bestMonth) {
        this.bestMonth = bestMonth;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "PartialDateEntity{" +
               "year=" + year +
               ", bestDay=" + bestDay +
               ", bestMonth=" + bestMonth +
               ", description='" + description + '\'' +
               '}';
    }
}
