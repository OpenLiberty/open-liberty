/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities;

/**
 * Embeddable class with a non-fully qualified classname in the XML ORM, relying on the package mapping default setting
 * for the JPA implementation to find it.
 *
 */
@SuppressWarnings("serial")
public class MFDNFQEmbeddable implements java.io.Serializable {
    private String street;
    private String city;
    private String state;
    private String zip;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    @Override
    public String toString() {
        return "MFDNFQEmbeddable [street=" + street + ", city=" + city + ", state=" + state + ", zip=" + zip + "]";
    }
}
