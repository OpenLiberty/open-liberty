/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.error;

/**
 * Service used to match error page providers against error conditions on
 * different listening ports.
 * 
 */
public interface HttpErrorPageService {

    /**
     * Register the given page provider for the input listening port.
     * This may fail if another provider is already registered.
     * 
     * @param port
     * @param provider
     * @return boolean - true means it successfully registered
     * @throws IllegalArgumentException
     *             if provider is null or port is non-positive
     */
    boolean register(int port, HttpErrorPageProvider provider);

    /**
     * Attempt to deregister whatever provider is registered for the input
     * port.
     * 
     * @param port
     * @return HttpErrorPageProvider - null if none registered for this port
     * @throws IllegalArgumentException
     *             if the port is non-positive
     */
    HttpErrorPageProvider deregister(int port);

    /**
     * Access the current page provider that is registered for this port. It
     * may return null if no provider exists.
     * 
     * @param port
     * @return HttpErrorPageProvider
     */
    HttpErrorPageProvider access(int port);

}
