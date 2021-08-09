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
package com.ibm.ws.channelfw.internal.discrim;

import com.ibm.ws.channelfw.internal.InboundVirtualConnection;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.exception.DiscriminationProcessException;

/**
 * An algorithm to choose a channel on the application side given some data.
 */
public interface DiscriminationAlgorithm {

    /**
     * Discriminate is called when a channel is ready to hand off a connection to
     * another
     * channel above it in the chain. This involves handing context specific
     * discrimination
     * data to the various channels that may exist above and have each of them
     * respond
     * after reviewing that data. The various instance types of the algorithm
     * handle the
     * logic for passing data to those channels.
     * 
     * @param vcx
     * @param discrimData
     * @param cl
     * @return int
     * @throws DiscriminationProcessException
     */
    int discriminate(InboundVirtualConnection vcx, Object discrimData, ConnectionLink cl) throws DiscriminationProcessException;

}
