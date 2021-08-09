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
package com.ibm.ws.tcpchannel.internal;

/**
 * When a TCP channel factory is being stopped, this termination handle is
 * called for each channel owned by that factory. The various channel types
 * must implement this interface and perform appropriate action during
 * termination, whatever that means for each type.
 */
public interface ChannelTermination {

    /**
     * Signal to the individual TCP channel to terminate.
     */
    void terminate();
}
