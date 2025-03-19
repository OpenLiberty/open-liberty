/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

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
 * Recreate from io.openliberty.data.internal_fat_jpa
 */
@Entity
public class DemographicInfo {

    @Column
    public Instant collectedOn;

    @GeneratedValue
    @Id
    public BigInteger id;

    @Column
    public BigDecimal publicDebt;

    @Column
    public BigDecimal intragovernmentalDebt;

    @Column
    public BigInteger numFullTimeWorkers;

    public static DemographicInfo of(int year, int month, int day,
                                     long numFullTimeWorkers,
                                     double intragovernmentalDebt, double publicDebt) {
        DemographicInfo inst = new DemographicInfo();
        inst.collectedOn = ZonedDateTime.of(year, month, day, 12, 0, 0, 0, ZoneId.of("America/New_York")).toInstant();
        inst.numFullTimeWorkers = BigInteger.valueOf(numFullTimeWorkers);
        inst.intragovernmentalDebt = BigDecimal.valueOf(intragovernmentalDebt);
        inst.publicDebt = BigDecimal.valueOf(publicDebt);
        return inst;
    }

    @Override
    public String toString() {
        return "DemographicInfo from " + collectedOn;
    }
}