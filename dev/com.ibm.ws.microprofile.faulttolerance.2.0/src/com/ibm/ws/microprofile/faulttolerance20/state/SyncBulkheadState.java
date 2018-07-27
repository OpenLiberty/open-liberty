/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.state;

import java.util.concurrent.Callable;

import com.ibm.ws.microprofile.faulttolerance20.impl.MethodResult;

/**
 * Implements the state and logic for a Fault Tolerance Synchronous Bulkhead
 * <p>
 * Scope: one method for the lifetime of the application
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 * MethodResult&lt;R&gt; result = syncBulkheadState.run(() -> codeToRun());
 * </code>
 * </pre>
 */
public interface SyncBulkheadState {

    /**
     * Submit a callable to be run on the bulkhead
     * <p>
     * If the callable throws an exception, this will be caught and included in the MethodResult returned from this method.
     * <p>
     * If the callable returns normally, the returned MethodResult will include the returned value.
     *
     * @param callable the callable to run
     * @return the result of running the callable
     */
    <R> MethodResult<R> run(Callable<R> callable);

}