/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package org.test.context.location;

import org.eclipse.microprofile.context.spi.ThreadContextController;

/**
 * Example third-party thread context restorer, to be used for testing purposes.
 * This context associates a city name with a thread, such that the applicable
 * sales tax rate (also including the state reported by the State context)
 * is included when the getTotalSalesTax() method is invoked from the thread.
 */
public class CityContextRestorer implements ThreadContextController {
    private boolean restored = false;
    private final String cityNameToRestore;

    CityContextRestorer(String cityNameToRestore) {
        this.cityNameToRestore = cityNameToRestore;
    }

    @Override
    public void endContext() {
        if (restored)
            throw new IllegalStateException("thread context was already restored");
        CityContextProvider.cityName.set(cityNameToRestore);
        restored = true;
    }

    @Override
    public String toString() {
        return "CityContextRestorer@" + Integer.toHexString(hashCode()) + "(" + cityNameToRestore + ")";
    }
}
