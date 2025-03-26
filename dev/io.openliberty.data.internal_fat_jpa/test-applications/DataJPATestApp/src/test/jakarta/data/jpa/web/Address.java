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

import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

/**
 *
 */
@Embeddable
public class Address {

    public String city;

    public int houseNum;

    public String state;

    @Embedded
    public Street street;

    @Convert(converter = ZipCodeConverter.class)
    public ZipCode zip;

    public Address() {
    }

    Address(String city, String state, int zip, int houseNum, Street street) {
        this.city = city;
        this.state = state;
        this.zip = ZipCode.of(zip);
        this.houseNum = houseNum;
        this.street = street;
    }

    @Override
    public String toString() {
        return houseNum + " " + street + " | " + city + ", " + state + " " + zip;
    }
}
