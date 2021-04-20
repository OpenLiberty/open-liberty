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

import java.io.File;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
/**
 * Service used to perform CRIU checkpoint operations.
 */
public interface Checkpoint {
    /**
     * Phase which a snapshot is being taken.
     *
     */
    public enum Phase {
        FEATURE,
        APP,
        SERVER
    }

    /**
     * Perform a snapshot for the specified phase. The result of the
     * snapshot will be stored in the specified directory.
     * Before the snapshot is taken the registered {@link SnapshotHookFactory#create(Phase)}
     * methods are called to obtain the {@link SnapshotHook} instances which will participate
     * in the prepare and restore steps for the snapshot process.
     * Each snapshot hook instance will their {@link SnapshotHook#prepare(Phase)}
     * methods are called before the snapshot is taken. After the snapshot
     * is taken each snapshot hook instance will have their {@link SnapshotHook#restore(Phase)}
     * methods called.
     *
     * @param phase     the phase to take the snapshot
     * @param directory the directory to store the snapshot
     * @throws SnapshotFailed if the snapshot fails
     */
    void snapshot(Phase phase, File directory) throws SnapshotFailed;
}
