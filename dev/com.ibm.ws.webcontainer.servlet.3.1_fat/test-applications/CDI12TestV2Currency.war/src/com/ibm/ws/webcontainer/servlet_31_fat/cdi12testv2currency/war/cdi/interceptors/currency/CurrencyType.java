/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2currency.war.cdi.interceptors.currency;

/**
 * Type used to represent a currency. Includes a country name.
 */
public enum CurrencyType {
    US_Dollar("USA"),
    Yuan("China"),
    Pound("UK"),
    Euro("Germany");

    private CurrencyType(String countryName) {
        this.countryName = countryName;
    }

    private final String countryName;

    public String getCountryName() {
        return countryName;
    }

    public static CurrencyType getCurrencyType(String countryName) {
        for (CurrencyType currencyType : CurrencyType.values()) {
            if (currencyType.getCountryName().equals(countryName)) {
                return currencyType;
            }
        }
        return null;
    }
}