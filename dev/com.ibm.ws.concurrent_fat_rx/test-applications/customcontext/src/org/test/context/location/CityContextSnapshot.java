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

import org.eclipse.microprofile.concurrent.spi.ThreadContextController;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context snapshot, to be used for testing purposes.
 * This context associates a city name with a thread, such that the applicable
 * sales tax rate (also including the state reported by the State context)
 * is included when the getTotalSalesTax() method is invoked from the thread.
 */
public class CityContextSnapshot implements ThreadContextSnapshot {
    private final String cityName;

    CityContextSnapshot(String cityName) {
        this.cityName = cityName;
    }

    @Override
    public ThreadContextController begin() {
        ThreadContextController cityContextRestorer = new CityContextRestorer(CityContextProvider.cityName.get());

        // Validate that the city/state combination is valid before applying context.
        // This introduces a dependency on the State context, so that we can test prerequisites.
        if (cityName.length() > 0)
            CurrentLocation.getTotalTaxRate(cityName, StateContextProvider.stateName.get());

        CityContextProvider.cityName.set(cityName);
        return cityContextRestorer;
    }

    @Override
    public String toString() {
        return "CityContextSnapshot@" + Integer.toHexString(hashCode()) + "(" + cityName + ")";
    }
}
