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

import java.util.Map;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a city name with a thread, such that the applicable
 * sales tax rate for the corresponding city (and state that it is in) is included
 * when the getTotalSalesTax() methods is invoked from the thread.
 */
public class CityContextProvider implements ThreadContextProvider {
    static ThreadLocal<String> cityName = ThreadLocal.withInitial(() -> "");

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return new CityContextSnapshot("");
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return new CityContextSnapshot(cityName.get());
    }

    @Override
    public String getThreadContextType() {
        return TestContextTypes.CITY;
    }
}
