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
 * A checkpoint hook factory provides an instance of a checkpoint hook
 * that will be used for a checkpoint and restore operation.
 *
 * @see CheckpointHook
 */
@FunctionalInterface
public interface CheckpointHookFactory {
    /**
     * Phase which a checkpoint is being taken.
     *
     */
    public enum Phase {
        /**
         * Phase of startup after all feature bundles have been started and before
         * starting any configured applications
         */
        FEATURES,
        /**
         * Phase of startup after all configured applications have been started
         * or have timed out starting. No ports are opened yet
         */
        APPLICATIONS;

        static Map<String, Phase> phases;
        static {
            phases = new HashMap<String, Phase>();
            for (Phase p : Phase.values()) {
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
        public static Phase getPhase(String p) {
            return phases.get(p.trim().toUpperCase());
        }
    }

    /**
     * Creates a checkpoint hook for the specified phase.
     * The instance of this hook will be used to prepare
     * and restore a checkpoint.
     *
     * @param phase the phase of the checkpoint
     * @return the checkpoint hook or null if this factory does
     *         not need to participate in the checkpoint prepare or restore
     */
    CheckpointHook create(Phase phase);
}
