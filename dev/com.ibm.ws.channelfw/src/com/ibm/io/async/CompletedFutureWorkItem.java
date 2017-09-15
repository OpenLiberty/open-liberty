/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

/**
 * An item that represents a completed IO request.
 */
public class CompletedFutureWorkItem {

    // the future that was completed
    protected AsyncFuture future;
    // the number of bytes read/wrote
    protected int numBytes;
    // the return code from the operation
    protected int returnCode;

    // the result handler that should process this item

    /**
     * Constructor.
     * 
     * @param _future
     * @param _numBytes
     * @param _returnCode
     */
    public CompletedFutureWorkItem(AsyncFuture _future, int _numBytes, int _returnCode) {
        this.future = _future;
        this.numBytes = _numBytes;
        this.returnCode = _returnCode;
    }
}
