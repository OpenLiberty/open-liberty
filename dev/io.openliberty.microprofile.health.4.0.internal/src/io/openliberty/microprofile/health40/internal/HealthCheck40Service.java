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
package io.openliberty.microprofile.health40.internal;

import java.io.File;

import org.eclipse.microprofile.health.HealthCheckResponse.Status;

import io.openliberty.microprofile.health31.internal.HealthCheck31Service;

/**
 * Microprofile Health Check Service
 */
public interface HealthCheck40Service extends HealthCheck31Service {

    /**
     * Start the file-based health check processes
     */
    public void startFileHealthCheckProcesses();

    /**
     * Performs a query on the statuses of the given health check procedure and updates the last modified time of the file if the status is UP.
     *
     * @param file                 The file for reporting the health status.
     * @param healthCheckProcedure the health check procedure
     * @return The final status of this health check.
     */
    public Status performFileHealthCheck(File file, String healthCheckProcedure);

}