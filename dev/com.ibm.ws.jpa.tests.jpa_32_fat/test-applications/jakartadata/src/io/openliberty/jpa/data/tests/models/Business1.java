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

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Recreate from io.openliberty.data.internal_fat_jpa
 */
@Entity
public class Business1 {

    public String name;

    @GeneratedValue
    @Id
    public int id;

    @Embedded
    public Location1 location;

    public Business1() {
    }

    public Business1(float latitude, float longitude, String city, String state, int zip,
             int houseNum, String streetName, String streetDir, String name) {
        Street1 street = new Street1(streetName, streetDir);
        Address1 address = new Address1(city, state, zip, houseNum, street);
        this.name = name;
        this.location = new Location1(address, latitude, longitude);
    }
}