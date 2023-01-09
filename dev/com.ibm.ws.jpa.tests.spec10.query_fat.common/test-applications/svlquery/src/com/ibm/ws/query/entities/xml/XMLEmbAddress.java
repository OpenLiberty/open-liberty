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

import com.ibm.ws.query.entities.interfaces.IEmbAddress;
import com.ibm.ws.query.entities.interfaces.IEmbZipCode;

public class XMLEmbAddress implements IEmbAddress {
    protected String street;
    protected String city;
    protected String state;
    protected XMLEmbZipCode zipcode;

    public XMLEmbAddress(String street, String city, String state,
                         String zip, String plusFour) {
        super();
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = new XMLEmbZipCode(zip, plusFour);
    }

    public XMLEmbAddress(String street, String city, String state,
                         XMLEmbZipCode zipcode) {
        super();
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
    }

    @Override
    public String toString() {
        return "( EmbAddress: Street=" + getStreet() + " City =" + getCity() + " State =" + getState() + " Zipcode =" + getZipcode().getZip() + ")";
    }

    @Override
    public String getStreet() {
        return street;
    }

    @Override
    public void setStreet(String street) {
        this.street = street;
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
    public XMLEmbZipCode getZipcode() {
        return zipcode;
    }

    @Override
    public void setZipcode(IEmbZipCode zipcode) {
        this.zipcode = (XMLEmbZipCode) zipcode;
    }

}
