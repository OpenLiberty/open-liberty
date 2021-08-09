/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.listeners;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface CompletionListener<T> {
    public void successfulCompletion(Future<T> future, T result);

    /**
     * The implementor is expected to FFDC if necessary for the
     * supplied {@link Throwable}, the threading utilities will not FFDC
     * for the enclosing {@link ExecutionException}.
     * 
     * @param t
     */
    public void failedCompletion(Future<T> future, Throwable t);
}