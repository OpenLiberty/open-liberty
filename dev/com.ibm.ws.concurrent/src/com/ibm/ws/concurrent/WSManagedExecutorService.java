/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent;

import com.ibm.ws.threading.PolicyExecutor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Interface that exposes various details about a managed executor service internally to other bundles.
 */
public interface WSManagedExecutorService {
    /**
     * Returns the context service instance backing the managed executor.
     *
     * @return the context service instance backing the managed executor.
     */
    WSContextService getContextService();

    // TODO getLongRunningPolicyExecutor

    /**
     * Returns the policy executor for running tasks against the normal concurrency policy.
     *
     * @return the policy executor for running tasks against the normal concurrency policy.
     */
    PolicyExecutor getNormalPolicyExecutor();
}
