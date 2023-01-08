/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.concurrent;

import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

/**
 * Marker interface that identifies a task as already contextualized,
 * such that when it is submitted to a managed executor via any of the execute/submit/invoke methods,
 * the managed executor does not capture context again, but instead uses the already captured context.
 */
public interface ContextualAction<T> {
    /**
     * Returns the action, not including the application & removal of context.
     *
     * @return the action, not including the application & removal of context.
     */
    T getAction();

    /**
     * Returns the already-captured thread context.
     *
     * @return the already captured thread context.
     */
    ThreadContextDescriptor getContextDescriptor();
}
