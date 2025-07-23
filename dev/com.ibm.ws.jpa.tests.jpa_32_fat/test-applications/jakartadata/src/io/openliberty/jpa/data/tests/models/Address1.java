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

import io.openliberty.jpa.data.tests.models.Business.Street;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

@Embeddable
public class Address1 {

    public String city;

    public int houseNum;

    public String state;

    @Embedded
    public Street1 street;

    @Convert(converter = ZipCodeConverter.class)
    public ZipCode zip;

    public Address1() {
    }

    public Address1(String city, String state, int zip, int houseNum, Street1 street) {
        this.city = city;
        this.state = state;
        this.zip = ZipCode.of(zip);
        this.houseNum = houseNum;
        this.street = street;
    }
}