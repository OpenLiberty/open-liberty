/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.spi;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase which a checkpoint is being taken. This enum is registered as a
 * service when a checkpoint operation is being done.
 *
 */
public enum CheckpointPhase {
    /**
     * Phase of startup after all feature bundles have been started and before
     * starting any configured applications
     */
    FEATURES,
    /**
     * Phase of startup after all configured applications have been started
     * or have timed out starting. No ports are opened yet
     */
    APPLICATIONS,
    /**
     * Phase of startup after the applications have been discovered but no
     * code from the application has been executed.
     */
    DEPLOYMENT;

    /**
     * The checkpoint service property used to store the checkpoint phase when registered as a service.
     */
    public static final String CHECKPOINT_PROPERTY = "io.openliberty.checkpoint";

    private static Map<String, CheckpointPhase> phases;
    static {
        phases = new HashMap<String, CheckpointPhase>();
        for (CheckpointPhase p : CheckpointPhase.values()) {
            phases.put(p.toString(), p);
        }
    }

    /**
     * Convert a String to a Phase
     *
     * @param p The string value
     * @return The matching phase or null if there is no match. String comparison
     *         is case insensitive.
     */
    public static CheckpointPhase getPhase(String p) {
        return phases.get(p.trim().toUpperCase());
    }
}