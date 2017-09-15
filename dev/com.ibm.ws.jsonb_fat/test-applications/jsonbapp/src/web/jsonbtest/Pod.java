/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.jsonbtest;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

/**
 * Application class that can be marshalled/unmarshalled to/from JSON.
 */
@JsonbPropertyOrder({ "building", "floor", "podNumber", "streetAddress",
                      "address", // temporary workaround for Johnzon
                      "city", "state", "zipCode" })
public class Pod implements Location {
    private String building;
    private String city;
    private short floor;
    private String pod;
    private String state;
    private String streetAddress;
    private int zip;

    @Override
    public String getBuilding() {
        return building;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public short getFloor() {
        return floor;
    }

    public String getPodNumber() {
        return pod;
    }

    @Override
    public String getState() {
        return state;
    }

    @JsonbProperty("address")
    @Override
    public String getStreetAddress() {
        return streetAddress;
    }

    @Override
    public int getZipCode() {
        return zip;
    }

    @Override
    public void setBuilding(String b) {
        building = b;
    }

    @Override
    public void setCity(String c) {
        city = c;
    }

    @Override
    public void setFloor(short f) {
        floor = f;
    }

    public void setPodNumber(String p) {
        pod = p;
    }

    @Override
    public void setState(String s) {
        state = s;
    }

    @JsonbProperty("address")
    @Override
    public void setStreetAddress(String s) {
        streetAddress = s;
    }

    @Override
    public void setZipCode(int z) {
        zip = z;
    }

    @Override
    public String toString() {
        return building + '-' + floor + ' ' + pod + " | " + streetAddress + " | " + city + ", " + state + ' ' + zip;
    }
}