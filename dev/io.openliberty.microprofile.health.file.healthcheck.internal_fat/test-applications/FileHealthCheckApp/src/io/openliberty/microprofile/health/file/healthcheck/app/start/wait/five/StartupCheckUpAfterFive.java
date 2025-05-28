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
package io.openliberty.microprofile.health.file.healthcheck.app.start.wait.five;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Startup;

import io.openliberty.microprofile.health.file.healthcheck.app.HealthAppServlet;

/**
 *
 */
@Startup
@ApplicationScoped
public class StartupCheckUpAfterFive implements HealthCheck {

    @Override
    public HealthCheckResponse call() {

        if (HealthAppServlet.isFiveSeconds()) {
            return HealthCheckResponse.named("fiveSec-startup-check").up().build();
        } else {
            return HealthCheckResponse.named("fiveSec-startup-check").down().build();
        }

    }

}
