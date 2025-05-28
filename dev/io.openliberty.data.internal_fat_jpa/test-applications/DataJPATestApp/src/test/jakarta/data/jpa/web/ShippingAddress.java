/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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
package test.jakarta.data.jpa.web;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

/**
 * Entity for testing DiscriminatorColumn/Value.
 */
@Inheritance
@DiscriminatorColumn(name = "ADDRESS_TYPE")
@DiscriminatorValue("Standard")
@Entity
public class ShippingAddress {

    @Id
    public Long id;

    public String city;

    public String state;

    @Embedded
    public StreetAddress streetAddress;

    public int zipCode;
}
