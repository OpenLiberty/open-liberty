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

package com.ibm.ws.query.entities.ano;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import com.ibm.ws.query.entities.interfaces.IEmbAddress;
import com.ibm.ws.query.entities.interfaces.IEmbZipCode;

@Embeddable
public class EmbAddress implements IEmbAddress {
    @Column(length = 40)
    protected String street;
    @Column(length = 30)
    protected String city;
    @Column(length = 20)
    protected String state;
    @Embedded
    protected EmbZipCode zipcode;

    public EmbAddress() {

    }

    public EmbAddress(String street, String city, String state,
                      String zip, String plusFour) {
        super();
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = new EmbZipCode(zip, plusFour);
    }

    public EmbAddress(String street, String city, String state,
                      EmbZipCode zipcode) {
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
    public EmbZipCode getZipcode() {
        return zipcode;
    }

    @Override
    public void setZipcode(IEmbZipCode zipcode) {
        this.zipcode = (EmbZipCode) zipcode;
    }

}
