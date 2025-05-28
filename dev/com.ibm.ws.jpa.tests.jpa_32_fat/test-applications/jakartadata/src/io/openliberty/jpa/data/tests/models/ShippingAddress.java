/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Embedded;

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