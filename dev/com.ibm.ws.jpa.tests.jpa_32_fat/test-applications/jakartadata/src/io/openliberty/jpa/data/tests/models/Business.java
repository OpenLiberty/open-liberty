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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * Recreate from io.openliberty.data.internal_fat_jpa
 */
@Entity
public class Business {

    public String name;

    @GeneratedValue
    @Id
    public int id;

    @Embedded
    public Location location;

    public static Business of(float latitude, float longitude, String city, String state, int zip,
                              int houseNum, String streetName, String streetDir, String name) {
        Business inst = new Business();
        Street street = new Street(streetName, streetDir);
        Address address = new Address(city, state, zip, houseNum, street);

        inst.name = name;
        inst.location = new Location(address, latitude, longitude);

        return inst;
    }

    @Embeddable
    public static class Location {

        @Embedded
        public Address address;

        @Column(columnDefinition = "DECIMAL(8,5) NOT NULL")
        public float latitude;

        @Column(columnDefinition = "DECIMAL(8,5) NOT NULL")
        public float longitude;

        public Location() {
        }

        public Location(Address address, float latitude, float longitude) {
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    @Embeddable
    public static class Address {

        public String city;

        public int houseNum;

        public String state;

        @Embedded
        public Street street;

        public int zip;

        public Address() {
        }

        Address(String city, String state, int zip, int houseNum, Street street) {
            this.city = city;
            this.state = state;
            this.zip = zip;
            this.houseNum = houseNum;
            this.street = street;
        }
    }

    @Embeddable
    public static class Street {

        public String direction;

        @Column(name = "STREETNAME")
        public String name;

        public Street() {
        }

        public Street(String name, String direction) {
            this.name = name;
            this.direction = direction;
        }
    }
}
