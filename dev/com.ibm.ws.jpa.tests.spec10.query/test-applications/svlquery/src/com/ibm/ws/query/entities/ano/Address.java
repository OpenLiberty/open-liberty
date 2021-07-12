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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.ibm.ws.query.entities.interfaces.IAddress;
import com.ibm.ws.query.entities.interfaces.IEmbZipCode;

@Entity
@Table(name = "Jpa2Address")
public class Address implements IAddress, Serializable {
    @Id
    @Column(length = 30)
    protected String street;
    protected String city;
    protected String state;
    @Embedded
    protected EmbZipCode zipcode;

    public Address() {

    }

    public Address(String street, String city, String state,
                   String zip, String plusFour) {
        super();
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = new EmbZipCode(zip, plusFour);
    }

    public Address(String street, String city, String state,
                   EmbZipCode zipcode) {
        super();
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
    }

    @Override
    public String toString() {
        return "( Address: Street=" + getStreet() + " City =" + getCity() + " State =" + getState() + " Zipcode =" + getZipcode().getZip() + ")";
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
