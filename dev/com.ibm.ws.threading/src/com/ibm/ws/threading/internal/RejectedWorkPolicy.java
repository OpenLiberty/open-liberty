/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

/**
 * The policy that determines what should happen when an executor
 * is unable to queue a piece of work for execution.
 */
public enum RejectedWorkPolicy {

    /**
     * Discard the work and raise a <code>RejectedExecutionException</code>.
     */
    ABORT,

    /**
     * Execute the work immediately on the caller's thread.
     */
    CALLER_RUNS
}
