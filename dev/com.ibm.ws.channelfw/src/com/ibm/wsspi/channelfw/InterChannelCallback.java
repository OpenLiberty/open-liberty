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
package com.ibm.wsspi.channelfw;

/**
 * A generic callback mechanism used for asynchronous operations.
 * <p>
 * This interface is not used specifically within the framework, but is a model
 * for consistency that can be used by any channel.
 */
public interface InterChannelCallback {
    /**
     * Called when the request has completeted successfully.
     * 
     * @param vc
     */
    void complete(VirtualConnection vc);

    /**
     * Called back if an exception occurres while processing the request.
     * The implementer of this interface can then decide how to handle this
     * exception.
     * 
     * @param vc
     * @param t
     *            The Throwable that caused the error.
     */
    void error(VirtualConnection vc, Throwable t);
}
