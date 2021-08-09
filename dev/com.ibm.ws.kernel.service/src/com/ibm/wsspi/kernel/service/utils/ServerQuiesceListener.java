/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

/**
 * A server quiesce listener. All {@code ServerQuiesceListener}s registered
 * in the service registry are called when the server has been stopped without
 * the {@code --force} option.
 */
public interface ServerQuiesceListener {

    /**
     * Called when the server is stopped without the {@code --force} option
     * to allow the registered service to perform pre-stop quiesce activities
     * to facilitate a clean server stop, like canceling pending scheduled executors,
     * or stopping inbound traffic to the server. This method should not be used to
     * register any new services, nor should it prematurely remove services that
     * other services depend on.
     * <p>
     * This method must complete and return to its caller in a timely manner and can be
     * called concurrently with other {@code ServerQuiesceListener}s in no specific
     * order.
     * </p><p>
     * Note that when this method is called, {@link FrameworkState#isStopping()} will
     * return true, and {@link FrameworkState#isValid()} will return false.
     * </p>
     */
    void serverStopping();
}
