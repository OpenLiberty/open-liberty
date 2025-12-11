/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * This entity has multiple levels of embeddables.
 */
@Entity
public class Business {

    public String name;

    @GeneratedValue
    @Id
    public int id;

    @Embedded
    public Location location;

    public Business() {
    }

    Business(float latitude, float longitude, String city, String state, int zip,
             int houseNum, String streetName, String streetDir, String name) {
        Street street = new Street(streetName, streetDir);
        Address address = new Address(city, state, zip, houseNum, street);
        this.name = name;
        this.location = new Location(address, latitude, longitude);
    }

    @Override
    public String toString() {
        return "Business id=" + id + " " + name + " @ " + location;
    }
}
