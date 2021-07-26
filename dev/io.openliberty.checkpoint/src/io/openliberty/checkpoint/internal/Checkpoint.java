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
package io.openliberty.checkpoint.internal;

import org.osgi.annotation.versioning.ProviderType;

import io.openliberty.checkpoint.spi.CheckpointHookFactory.Phase;
import io.openliberty.checkpoint.spi.SnapshotFailed;

@ProviderType
/**
 * Service used to perform CRIU checkpoint operations.
 */
public interface Checkpoint {
    /**
     * Asserts that this platform supports checkpoint. Depending on implementation may
     * call out to native library support.
     *
     * @throws SnapshotFailed if support for snapshot functionality not available.
     */
    default void checkpointSupported() throws SnapshotFailed {
    }

    /**
     * Perform a snapshot for the specified phase. The result of the
     * snapshot will be stored in the specified directory.
     * Before the snapshot is taken the registered {@link CheckpointHookFactory#create(Phase)}
     * methods are called to obtain the {@link CheckpointHook} instances which will participate
     * in the prepare and restore steps for the snapshot process.
     * Each checkpoint hook instance will their {@link CheckpointHook#prepare(Phase)}
     * methods are called before the snapshot is taken. After the snapshot
     * is taken each checkpoint hook instance will have their {@link CheckpointHook#restore(Phase)}
     * methods called.
     * Asserts that this platform supports checkpoint. Depending on implementation may
     * call out to native library support.
     *
     * @param phase the phase to take the snapshot
     * @throws SnapshotFailed if the snapshot fails
     */
    void snapshot(Phase phase) throws SnapshotFailed;
}
