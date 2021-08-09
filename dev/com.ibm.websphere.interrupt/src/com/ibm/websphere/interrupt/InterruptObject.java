/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
 * application components to be notified if the thread which registered the
 * object exceeds a time threshold defined by the application server.
 *
 * @ibm-api
 * @ibm-was-base
 */
public interface InterruptObject {
    /**
     * Called by the application server when the request running on the thread
     * which registered this object exceeds a time threshold. This method will
     * be driven on a separate thread, and should attempt to interrupt the work
     * running on the thread which registered this object.
     *
     * The goal of this method is to stop the hung and/or looping work from
     * executing, so that control can return to the application server. The
     * application server can then respond appropriately to the client, and
     * issue a new request on this thread.
     *
     * @return <code>true</code> if the work was successfully interrupted,
     *         <code>false</code> if not.
     */
    public boolean interrupt();

    /**
     * Called by the application server to determine whether or not this object
     * has been driven to interrupt the work running on the thread of execution.
     *
     * @return <code>true</code> if the interrupt method was called,
     *         <code>false</code> if not.
     */
    public boolean queryTried();

    /**
     * Displays the name of this interrupt object. The text will identify this
     * object in logs. Often this will be the component name for which the
     * object is registered (for example, the Object Transaction Service might
     * return "OTS").
     *
     * @return The name of this interrupt object.
     */
    public String getName();

    /**
     * Displays additional details about the state of this interrupt object.
     * The contents of the message are implementation specific, but they may
     * be displayed in the server log, so they should be as concise as possible.
     * It is usually useful to display the state of the object, as well as any
     * information about what operations are under its control (such as which
     * outbound connection, or which transaction).
     *
     * @return A string describing the state of this interrupt object, or
     *         null if no additional information is available.
     */
    public String getDisplayInfo();
}
