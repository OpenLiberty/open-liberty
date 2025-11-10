/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.health.file.healthcheck.app.start.after;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;

/**
 *
 */
@Startup
@ApplicationScoped
public class StartupCheckUpAfterSecondQuery implements HealthCheck {

    private int counter = 0;

    @Override
    public HealthCheckResponse call() {

        /*
         * Return up after first check.
         */
        if (counter != 0) {
            return HealthCheckResponse.named("secondUP-startup-check").up().build();
        } else {
            counter++;
            return HealthCheckResponse.named("secondUP-startup-check").down().build();
        }

    }

}
