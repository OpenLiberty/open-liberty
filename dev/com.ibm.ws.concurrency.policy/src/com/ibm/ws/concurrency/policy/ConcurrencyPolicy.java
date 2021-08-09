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
package com.ibm.ws.concurrency.policy;

import com.ibm.ws.threading.PolicyExecutor;

/**
 * Corresponds to a <code>concurrencyPolicy</code> configuration element.
 * Allows you to obtain the policy executor that implements the configured policy.
 */
public interface ConcurrencyPolicy {
    /**
     * Obtains the policy executor that runs tasks on the Liberty global thread pool according to the concurrency constraints
     * and other concurrency-related options that are configured for this <code>concurrencyPolicy</code>.
     *
     * @return the policy executor instance.
     */
    PolicyExecutor getExecutor();
}