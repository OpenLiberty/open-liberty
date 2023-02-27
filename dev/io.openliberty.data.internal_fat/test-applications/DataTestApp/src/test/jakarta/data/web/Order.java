/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package test.jakarta.data.web;

import java.time.OffsetDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * An entity with a generated id value.
 */
@Entity(name = "Orders") // overrides the default name Order, which happens to be a keyword in JPQL and the database
public class Order {

    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Id
    public Long id;

    public String purchasedBy;

    public OffsetDateTime purchasedOn;

    public float total;
}
