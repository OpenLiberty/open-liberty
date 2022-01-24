/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.spi;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A hook that gets called during a checkpoint to allow the system to
 * prepare and restore during a checkpoint operation.
 *
 */
@ConsumerType
public interface CheckpointHook {
    /**
     * Hooks that must be run while the JVM has multi threaded execution enabled must set this property to
     * the Boolean value true. If this property is set to false or not set at all then the hook will
     * run while the JVM is in a single threaded mode of execution.
     */
    public static String MULTI_THREADED_HOOK = "io.openliberty.checkpoint.hook.multi.threaded";

    /**
     * Prepare to checkpoint. If the hook throws
     * an exception then the checkpoint will be aborted.
     */
    default void prepare() {
    };

    /**
     * Restore the state after a checkpoint. If the hook
     * throws an exception then the restore will be aborted.
     */
    default void restore() {
    };
}
