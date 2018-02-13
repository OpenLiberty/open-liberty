/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.jsonb.fat;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Example Java object that can be converted to/from JSON.
 */
public class Location {
    private String cityName;
    private String countyName;
    private String stateName;
    private int zipCode;

    public String getCity() {
        return cityName;
    }

    public String getCounty() {
        return countyName;
    }

    public String getState() {
        return stateName;
    }

    @JsonbProperty("zip")
    public int getZipCode() {
        return zipCode;
    }

    public void setCity(String cityName) {
        this.cityName = cityName;
    }

    public void setCounty(String countyName) {
        this.countyName = countyName;
    }

    public void setState(String stateName) {
        this.stateName = stateName;
    }

    @JsonbProperty("zip")
    public void setZipCode(int zipCode) {
        this.zipCode = zipCode;
    }
}
