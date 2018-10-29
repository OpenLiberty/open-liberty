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
package org.test.context.location;

/**
 * Utility class for tests that relies on a location (US state and city)
 * being associated with the current thread, which it uses to compute sales tax.
 * This can be used to validate that the custom thread context types
 * "StateContext" and "CityContext" have been correctly propagated to the
 * running thread.
 */
public class CurrentLocation {
    public static double getStateSalesTax(double purchaseAmount) {
        String stateName = StateContextProvider.stateName.get();
        return getStateTaxRate(stateName) * purchaseAmount;
    }

    public static double getTotalSalesTax(double purchaseAmount) {
        String cityName = null; // TODO CityContextProvider.cityName.get();
        String stateName = StateContextProvider.stateName.get();
        return getTotalTaxRate(cityName, stateName) * purchaseAmount;
    }

    public static void setLocation(String state) {
        StateContextProvider.stateName.set(state);
    }

    private static double getStateTaxRate(String state) {
        switch (StateContextProvider.stateName.get()) {
            case "Illinois":
                return 0.0625;
            case "Indiana":
                return 0.07;
            case "Iowa":
                return 0.06;
            case "Kansas":
                return 0.065;
            case "Michigan":
                return 0.0600;
            case "Minnesota":
                return 0.06875;
            case "Missouri":
                return 0.04225;
            case "Nebraska":
                return 0.055;
            case "North Dakota":
                return 0.05;
            case "South Dakota":
                return 0.04;
            case "Wisconsin":
                return 0.05;
        }
        throw new IllegalArgumentException(state + " is an unkown location");
    }

    private static double getTotalTaxRate(String city, String state) {
        switch (state) {
            case "Iowa":
                switch (city) {
                    case "Ames":
                    case "Cedar Rapids":
                    case "Davenport":
                    case "Dubuque":
                        return 0.07;
                    case "Des Moines":
                    case "Iowa City":
                        return 0.06;
                }
                break;
            case "Minnesota":
                switch (city) {
                    case "Duluth":
                        return 0.07875;
                    case "Minneapolis":
                        return 0.07775;
                    case "Rochester":
                        return 0.07375;
                    case "Saint Paul":
                        return 0.07625;
                    case "Byron":
                    case "Stewartville":
                    case "Winona":
                        return getStateTaxRate(state);
                }
                break;
        }
        throw new IllegalArgumentException(city + ", " + state + " is an unknown location");
    }
}
