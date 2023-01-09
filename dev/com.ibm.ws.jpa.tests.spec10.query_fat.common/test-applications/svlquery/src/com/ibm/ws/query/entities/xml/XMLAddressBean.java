/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.query.entities.xml;

import java.io.Serializable;

import com.ibm.ws.query.entities.interfaces.IAddressBean;
import com.ibm.ws.query.entities.interfaces.IAddressPK;

public class XMLAddressBean implements IAddressBean, Serializable {
    private XMLAddressPK street;
    private String city;
    private String state;
    private String zip;

    public XMLAddressBean() {
    }

    public XMLAddressBean(String street, String city, String state, String zip) {
        this.street = new XMLAddressPK(street);
        this.city = city;
        this.state = state;
        this.zip = zip;
    }

    @Override
    public String toString() {
        return street + " " + " " + city + "," + state + " " + zip;
    }

    @Override
    public XMLAddressPK getStreet() {
        return street;
    }

    @Override
    public void setStreet(IAddressPK street) {
        this.street = (XMLAddressPK) street;
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
