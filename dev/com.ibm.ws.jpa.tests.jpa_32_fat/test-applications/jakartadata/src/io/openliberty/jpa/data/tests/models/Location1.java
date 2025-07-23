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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

@Embeddable
public class Location1 {

    @Embedded
    public Address1 address;

    @Column(columnDefinition = "DECIMAL(8,5) NOT NULL")
    public float latitude;

    @Column(columnDefinition = "DECIMAL(8,5) NOT NULL")
    public float longitude;

    public Location1() {
    }

    public Location1(Address1 address, float latitude, float longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}