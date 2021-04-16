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
 */
@ConsumerType
public interface SnapshotHook {
	/**
	 * Prepare for the specified phase to snapshot.  If the hook throws
	 * an exception then the snapshot will be aborted.  All hooks
	 * that already had their {@link apply} method called
	 * will then have their {@link #abortPrepare(Phase, Exception)}
	 * method called for the specified phase.
	 * @param phase the phase to prepare
	 * @return an exception to abort the prepare or {@code null}.
	 */
	void prepare(Phase phase);

	/**
	 * Abort the prepare for the specified phase.
	 * @param phase the phase being aborted
	 * @param cause the exception that caused the abort
	 */
	void abortPrepare(Phase phase, Exception cause);

	/**
	 * Restore for the specified phase after a snapshot.  If the hook
	 * throws an exception then the restore will be aborted. All hooks
	 * that already had their {@link #restore(Phase)} method called
	 * will then have their {@link #abortRestore(Phase, Exception)}
	 * method called for the specified phase.
	 * @param phase the phase to restore
	 */
	Exception restore(Phase phase);

	/**
	 * Abort the restore for the specified phase.
	 * @param phase the phase being aborted
	 * @param cause the exception the caused the abort
	 */
	void abortRestore(Phase phase, Exception cause);
}
