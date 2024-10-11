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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

//
// Recreate from io.openliberty.data.internal_fat_jpa
//
//public record Rebate(
//                Integer id, // TODO use @GeneratedValue
//                double amount,
//                String customerId,
//                LocalTime purchaseMadeAt,
//                LocalDate purchaseMadeOn,
//                Rebate.Status status,
//                LocalDateTime updatedAt,
//                Integer version) { // TODO rename to something other than version, and use @Version
//    public static enum Status {
//        DENIED, SUBMITTED, VERIFIED, PAID
//    }
//}

@Entity
public class Rebate {

    @Id
    @GeneratedValue
    public Integer id;

    public double amount;

    public String customerId;

    public LocalTime purchaseMadeAt;

    public LocalDate purchaseMadeOn;

    public Rebate.Status status;

    public LocalDateTime updatedAt;

    @Version
    public Integer version;
    public Rebate(){}
    public static Rebate of(double amount, String customerId, LocalTime purchaseMadeAt, LocalDate purchaseMadeOn, Status status, LocalDateTime updatedAt, int version) {
        Rebate inst = new Rebate();
        inst.amount = amount;
        inst.customerId = customerId;
        inst.purchaseMadeAt = purchaseMadeAt;
        inst.purchaseMadeOn = purchaseMadeOn;
        inst.status = status;
        inst.updatedAt = updatedAt;
        inst.version = version;

        return inst;
    }
    public Rebate(Integer id, double amount, String customerId, LocalTime purchaseMadeAt, LocalDate purchaseMadeOn, Status status, LocalDateTime updatedAt, Integer version) {
        this.id = id; 
        this.amount = amount;
        this.customerId = customerId;
        this.purchaseMadeAt = purchaseMadeAt;
        this.purchaseMadeOn = purchaseMadeOn;
        this.status = status;
        this.updatedAt = updatedAt;
        this.version = version;
    }
    public static enum Status {
        DENIED, SUBMITTED, VERIFIED, PAID
    }
}
