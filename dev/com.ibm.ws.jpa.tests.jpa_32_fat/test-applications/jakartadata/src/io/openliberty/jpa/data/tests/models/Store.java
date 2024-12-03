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

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Store {

    @Id
    public long purchaseId;

    public String customer;

    public Instant time;

    public static Store of(int year, int month, int day,String customer,long purchaseId) {
        Store inst = new Store();
        inst.purchaseId = purchaseId;
        inst.customer = customer;
        inst.time = ZonedDateTime.of(year, month, day, 12, 0, 0, 0, ZoneId.of("America/New_York")).toInstant();
        return inst;
    }
}