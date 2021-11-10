/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.xml;

import java.io.Serializable;

import com.ibm.ws.query.entities.interfaces.IAddressBean;

public class AddressBean implements IAddressBean, Serializable {
    private AddressPK street;
    private String city;
    private String state;
    private String zip;

    public AddressBean() {
    }

    public AddressBean(String street, String city, String state, String zip) {
        this.street = new AddressPK(street);
        this.city = city;
        this.state = state;
        this.zip = zip;
    }

    @Override
    public String toString() {
        return street + " " + " " + city + "," + state + " " + zip;
    }

    @Override
    public String getStreet() {
        return street.getName();
    }

    @Override
    public void setStreet(String street) {
        this.street = new AddressPK(street);
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getZip() {
        return zip;
    }

    @Override
    public void setZip(String zip) {
        this.zip = zip;
    }
}
