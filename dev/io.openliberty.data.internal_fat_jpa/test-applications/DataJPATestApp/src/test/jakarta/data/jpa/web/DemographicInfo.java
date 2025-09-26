/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Entity that has the Instant, BigDecimal, and BigInteger basic types
 * which are not used anywhere else in tests.
 */
@Entity
public class DemographicInfo {
    static final ZoneId TIMEZONE = ZoneId.of("America/New_York");

    @Column
    public Instant collectedOn;

    @GeneratedValue
    @Id
    public BigInteger id;

    @Column(precision = 20, scale = 2)
    public BigDecimal publicDebt;

    @Column(precision = 20, scale = 2)
    public BigDecimal intragovernmentalDebt;

    @Column
    public BigInteger numFullTimeWorkers;

    public DemographicInfo() {
    }

    public DemographicInfo(int year, int month, int day,
                           long numFullTimeWorkers,
                           double intragovernmentalDebt, double publicDebt) {
        this.collectedOn = ZonedDateTime.of(year, month, day, 12, 0, 0, 0, TIMEZONE)
                        .toInstant();
        this.numFullTimeWorkers = BigInteger.valueOf(numFullTimeWorkers);
        this.intragovernmentalDebt = BigDecimal.valueOf(intragovernmentalDebt);
        this.publicDebt = BigDecimal.valueOf(publicDebt);
    }

    @Override
    public String toString() {
        return "DemographicInfo from " + collectedOn;
    }
}
