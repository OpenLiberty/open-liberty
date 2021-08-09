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
package com.ibm.websphere.management.j2ee;

/**
 * The StateManageable model specifies the operations and attributes that must be
 * implemented by a managed object that supports state management. A managed
 * object that implements the StateManageable model is termed a State Manageable
 * Object (SMO). An SMO generates events when its state changes.
 */
public interface StateManageableMBean {

    /**
     * The current state of this SMO.
     * The SMO can be in one of the following states:
     * STARTING(0), RUNNING(1), STOPPING(2), STOPPED(3), FAILED(4)
     */
    int getstate();

    /**
     * The time that the managed object was started (i.e. entered the RUNNING
     * state) represented as a long, which value is the number of milliseconds since
     * January 1, 1970, 00:00:00.
     */
    long getstartTime();

    /**
     * Starts the SMO. This operation can be invoked only when the SMO is in the
     * STOPPED or FAILED state. It causes the SMO to go into the STARTING state
     * initially, and if it completes successfully, the SMO will be in the RUNNING state.
     * Note that start() is not called on any of the child SMOs that are registered with
     * this SMO; it is the responsibility of the calling application to start the child SMO
     * if this is required
     */
    void start();

    /**
     * Starts the SMO. This operation can only be invoked when the SMO is in the
     * STOPPED or FAILED state. It causes the SMO to go into the STARTING state
     * initially, and if it completes successfully, the SMO will be in the RUNNING state.
     * The operation startRecursive() is called on all the child SMOs registered
     * with this SMO that are in the STOPPED or FAILED state.
     */
    void startRecursive();

    /**
     * Stops the SMO. This operation can only be invoked when the SMO is in the
     * RUNNING or STARTING state. It causes stop() to be called on all the child
     * SMOs registered with this SMO that are in the RUNNING or STARTING state. It
     * is mandatory if an SMO is in the STOPPED or FAILED state, that all its child
     * SMOs must also be in the STOPPED or FAILED state, therefore there is no stopRecursive()
     * operation. Invoking stop() causes the SMO to go into the
     * STOPPING state initially, and if it completes successfully, the SMO and all the
     * child SMOs will be in the STOPPED state.
     */
    void stop();
}
