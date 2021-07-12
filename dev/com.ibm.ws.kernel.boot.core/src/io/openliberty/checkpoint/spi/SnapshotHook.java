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

import io.openliberty.checkpoint.spi.Checkpoint.Phase;

/**
 * A hook that gets called during a snapshot to allow the system to
 * prepare and restore during a snapshot operation.
 *
 * @see Checkpoint#snapshot(Phase, java.io.File)
 */
@ConsumerType
public interface SnapshotHook {
    /**
     * Prepare to snapshot. If the hook throws
     * an exception then the snapshot will be aborted. All hooks
     * that already had their {@link prepare()} method called
     * will then have their {@link #abortPrepare(Exception)}
     * method called.
     */
    default void prepare() {
    };

    /**
     * Abort the prepare for snapshot.
     *
     * @param snapshotResult that caused the abort
     */
    default void abortPrepare(SnapshotResult snapshotResult) {
    };

    /**
     * Restore the state after a snapshot. If the hook
     * throws an exception then the restore will be aborted. All hooks
     * that already had their {@link #restore()} method called
     * will then have their {@link #abortRestore(Exception)}
     * method called.
     */
    default void restore() {
    };

    /**
     * Abort the restore from snapshot.
     *
     * @param snapshotResult that caused the abort
     */
    default void abortRestore(SnapshotResult snapshotResult) {
    };
}
