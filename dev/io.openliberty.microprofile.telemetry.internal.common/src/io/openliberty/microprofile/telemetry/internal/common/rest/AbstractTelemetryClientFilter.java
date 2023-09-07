/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.common.rest;

import jakarta.enterprise.inject.spi.CDI;

public abstract class AbstractTelemetryClientFilter {
	
    /**
     * Retrieve the TelemetryClientFilter for the current application using CDI
     * <p>
     * Implementation note: It's important that there's a class which is registered as a CDI bean on the stack from this bundle when {@code CDI.current()} is called so that CDI
     * finds the correct BDA and bean manager.
     * <p>
     * Calling it from this static method ensures that {@code TelemetryClientFilter} is the first thing on the stack and CDI will find the right BDA.
     *
     * @return the TelemetryClientFilter for the current application
     */
	//This is here so there is only one entry point via static methods regardless of which mpTelemetry version is in use.
    public static AbstractTelemetryClientFilter getCurrent() {
        return CDI.current().select(AbstractTelemetryClientFilter.class).get();
    }

    //This is here because it is called by other classes in the common package
	protected abstract boolean isEnabled();

}
