/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.embeddable;

/**
 * A {@link ServerEvent} listener. <code>ServerEventListener</code> is a listener
 * interface that may be implemented by the embedding runtime.
 * <p>
 * When a <code>ServerEvent</code> is fired, it is asynchronously delivered to
 * a <code>ServerEventListener</code>. The server delivers <code>ServerEvent</code> objects
 * to a <code>ServerEventListener</code> in order and will not concurrently call a <code>ServerEventListener</code>.
 * <p>
 * A <code>ServerEventListener</code> object is registered with a <code>Server</code> using
 * the {@link ServerBuilder#registerServerEventListener(ServerEventListener)} method.
 * <code>ServerEventListener</code> objects
 * are called with a <code>ServerEvent</code> object when the <code>Server</code> created
 * by the <code>ServerBuilder</code> starts or stops.
 */
public interface ServerEventListener {

    /**
     * A <code>ServerEvent</code> is passed to a registered <code>ServerEventListener</code>
     * <p>
     * Consumers of this SPI must not implement this interface.
     */
    static interface ServerEvent {

        /** The type of server event */
        enum Type {
            STARTING,
            STARTED,
            STOPPED,
            FAILED; // used to ensure notification when configuration prevents the server from starting
        }

        /**
         * @return the Server associated with the event.
         */
        Server getServer();

        /**
         * The type of event
         */
        Type getType();

        /**
         * @return a ServerException, or null.
         */
        ServerException getException();
    }

    /**
     * Receives notification of a general {@link ServerEvent} object.
     * 
     * @param event The ServerEvent object
     */
    void serverEvent(ServerEvent event);
}
