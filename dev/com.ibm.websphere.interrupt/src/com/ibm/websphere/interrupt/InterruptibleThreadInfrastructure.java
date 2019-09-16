/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.interrupt;

/**
 * A WebSphere programming model extension which allows connectors or
 * application components register an object that may be driven if the thread
 * which registered the object exceeds a time threshold defined by the
 * application server.
 *
 * @ibm-api
 * @ibm-was-base
 */
public interface InterruptibleThreadInfrastructure {
    /**
     * The location in JNDI where this object can be obtained. The Liberty
     * feature <code>jndi-1.0</code> needs to be enabled to use JNDI to lookup this object.
     * This object can also be obtained by resource injection as follows:
     *
     * <pre>
     * &#64;Resource(InterruptibleThreadInfrastructure.ITI_LOC)
     * private InterruptibleThreadInfrastructure _iti;
     * </pre>
     */
    public static String ITI_LOC = "interrupt/iti";

    /**
     * Registers an
     * {@link com.ibm.websphere.interrupt.InterruptObject InterruptObject}
     * with the request running on the current thread of execution. The
     * {@link com.ibm.websphere.interrupt.InterruptObject InterruptObject}
     * is placed on a stack, and should be removed by calling
     * <code>deregister</code> when the caller has finished processing.
     *
     * @param odi The
     *            {@link com.ibm.websphere.interrupt.InterruptObject InterruptObject} to
     *            register.
     *
     * @exception InterruptRegistrationException Thrown if the interrupt object
     *                could not be registered. The cause (if known) will be linked to this
     *                exception.
     */
    public void register(InterruptObject odi) throws InterruptRegistrationException;

    /**
     * Deregisters an
     * {@link com.ibm.websphere.interrupt.InterruptObject InterruptObject}
     * from the current thread of execution. No errors are generated if the
     * {@link com.ibm.websphere.interrupt.InterruptObject InterruptObject}
     * is not found in the stack for this thread.
     *
     * @param odi The
     *            {@link com.ibm.websphere.interrupt.InterruptObject InterruptObject} to
     *            deregister.
     */
    public void deregister(InterruptObject odi);

    /**
     * Indicates if the InterruptibleThreadInfrastructure function is supported
     * within the current runtime environment.
     *
     * @return <code>true</code> if
     *         {@link com.ibm.websphere.interrupt.InterruptObject InterruptObject}
     *         registration is supported on the
     *         current thread, <code>false</code> if
     *         {@link com.ibm.websphere.interrupt.InterruptObject InterruptObject}
     *         registration is not supported
     *         on the current thread.
     *
     */
    public boolean isODISupported(); /* @690237A */
}
