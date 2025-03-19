/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health40.internal;

import java.io.File;
import java.util.Set;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class FileHealthCheckBuilder {
    private Status overallStatus = Status.UP;

    private final File file;

    private static final TraceComponent tc = Tr.register(AppTracker40Impl.class);

    /**
     * Never used.
     * Ensure the other constructor with File param is used.
     */
    private FileHealthCheckBuilder() {
        file = null;
    }

    public FileHealthCheckBuilder(File file) {
        this.file = file;
    }

    /**
     * Ultimately checks if there is a DOWN and sets overallStatus as down.
     *
     * @param hcResponseSet Set of queried health checks.
     */
    public void addResponses(Set<HealthCheckResponse> hcResponseSet) {
        for (HealthCheckResponse hcr : hcResponseSet) {
            if (hcr.getStatus().equals(Status.DOWN)) {
                overallStatus = Status.DOWN;
                return;
            }
        }
    }

    /**
     * No information, means down.
     */
    public void handleUndeterminedResponse() {
        overallStatus = Status.DOWN;
    }

    public void setOverallStatus(Status status) {
        overallStatus = status;
    }

    public void updateFile() {

        if (overallStatus.equals(Status.DOWN)) {
            return;
        }

        /*
         * !Exist -> create
         * Exist -> last modified
         */

        if (!file.exists()) {
            if (!HealthFileUtils.createFile(file)) {
                //Shouldn't happen, we already validated write perms on system if we got here.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to create file " + file.getAbsolutePath());
                }
                return;
            }
        } else {
            if (!HealthFileUtils.setLastModified(file)) {
                //Shouldn't happen, we already validated write perms on system if we got here.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to update last modified time for  file " + file.getAbsolutePath());
                }
                return;
            }
        }

    }

    public Status getOverallStatus() {
        return overallStatus;
    }
}
