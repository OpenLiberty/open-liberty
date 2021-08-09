/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature;

/**
 * Service called by feature manager before registering the {@link ServerStarted} service.
 * This allows a sub system to delay the registration of the {@code ServerStarted} service
 * until the sub system is ready to provide service.
 */
public interface ServerReadyStatus {
    /**
     * A check and possible delay to wait until the check is ready for the server to claim it has started.
     * This check allows a sub system to hold up the {@code ServerStarted} service registration
     * until the sub system is ready to provide service.
     * <p>
     * All logic to timeout must be implemented by the check method itself and any errors or warnings
     * must be logged by the check method as appropriate. This method does not provide a way for
     * the sub system to prevent the server from starting based on success or failure, it only provides
     * an opportunity for the sub system to delay until it is ready, or timeout itself. This method
     * must not block indefinitely as that will block the server from starting.
     */
    void check();
}
