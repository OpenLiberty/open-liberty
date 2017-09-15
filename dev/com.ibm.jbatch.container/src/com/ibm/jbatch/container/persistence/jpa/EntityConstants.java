/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.persistence.jpa;

/**
 * @author skurz
 *
 */
// Hide this for now internally to the package to see how far it bleeds out.
public interface EntityConstants {

	// Partition numbering begins at '0', so '-1' is a natural choice to denote the top-level thread, in contrast.
	final int TOP_LEVEL_THREAD = -1;

	// Since '0' is a maybe-valid (if seemingly useless boundary value) for plan size, let's distinguish with -1 the state
	// where the value hasn't been initialized.
	public final int PARTITION_PLAN_SIZE_UNINITIALIZED = -1;

	final int MAX_EXIT_STATUS_LENGTH = 512;

	final int MAX_STEP_NAME = 128;
}
