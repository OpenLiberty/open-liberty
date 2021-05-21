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

import io.openliberty.checkpoint.spi.Checkpoint.Phase;

/**
 * A snapshot hook factory provides an instance of a snapshot hook
 * that will be used for a checkpoint and restore operation.
 *
 * @see Checkpoint#snapshot(Phase, java.io.File)
 * @see SnapshotHook
 */
@FunctionalInterface
public interface SnapshotHookFactory {
    /**
     * Creates a snapshot hook for the specified phase.
     * The instance of this hook will be used to prepare
     * and restore a snapshot.
     *
     * @param phase the phase of the snapshot
     * @return the snapshot hook or null if this factory does
     *         not need to participate in the snapshot prepare or restore
     */
    SnapshotHook create(Phase phase);
}
