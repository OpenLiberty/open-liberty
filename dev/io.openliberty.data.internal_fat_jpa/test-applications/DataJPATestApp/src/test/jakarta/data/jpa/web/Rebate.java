/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Java record that is used as an entity where record/entity includes a version.
 */
public record Rebate(
                Integer id, // record entities cannot use @GeneratedValue
                double amount,
                String customerId,
                LocalTime purchaseMadeAt,
                LocalDate purchaseMadeOn,
                Rebate.Status status,
                LocalDateTime updatedAt,
                Integer version) {
    public static enum Status {
        DENIED, SUBMITTED, VERIFIED, PAID
    }
}
