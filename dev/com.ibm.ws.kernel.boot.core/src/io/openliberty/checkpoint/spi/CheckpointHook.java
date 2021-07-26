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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A hook that gets called during a checkpoint to allow the system to
 * prepare and restore during a checkpoint operation.
 *
 */
@ConsumerType
public interface CheckpointHook {
    /**
     * Prepare to checkpoint. If the hook throws
     * an exception then the checkpoint will be aborted. All hooks
     * that already had their {@link prepare()} method called
     * will then have their {@link #abortPrepare(Exception)}
     * method called.
     */
    default void prepare() {
    };

    /**
     * Abort the prepare for checkpoint.
     *
     * @param cause the exception that caused the abort
     */
    default void abortPrepare(Exception cause) {
    };

    /**
     * Restore the state after a checkpoint. If the hook
     * throws an exception then the restore will be aborted. All hooks
     * that already had their {@link #restore()} method called
     * will then have their {@link #abortRestore(Exception)}
     * method called.
     */
    default void restore() {
    };

    /**
     * Abort the restore from checkpoint.
     *
     * @param cause the exception the caused the abort
     */
    default void abortRestore(Exception cause) {
    };
}
