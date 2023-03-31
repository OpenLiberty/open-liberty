/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

/**
 * Encapsulation of steps for starting/stopping an http chain in a controlled/predictable
 * manner with a minimum of synchronization.
 */
public interface HttpChain {

    /**
     * Enable this chain: this happens automatically for the http chain,
     * but is delayed on the ssl chain until ssl support becomes available.
     * This does not change the chain's state. The caller should
     * make subsequent calls to perform actions on the chain.
     */
    public void enable(boolean enabled);

    /**
     * Stop this chain. The chain will have to be recreated when port is updated
     * notification/follow-on of stop operation is in the chainStopped listener method.
     */
    public void stop();

    /**
     * Update/start the chain configuration.
     */
    public void update();

}
