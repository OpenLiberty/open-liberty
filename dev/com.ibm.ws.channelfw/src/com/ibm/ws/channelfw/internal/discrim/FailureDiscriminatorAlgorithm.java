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
import com.ibm.wsspi.channelfw.DiscriminationProcess;

/**
 * 
 * This is the Failure algorithm. Fail all the time
 */
public class FailureDiscriminatorAlgorithm implements DiscriminationAlgorithm {

    /**
     * Constructor
     */
    FailureDiscriminatorAlgorithm() {
        // Nothing needed here at this time.
    }

    /**
     * @see com.ibm.ws.channelfw.internal.discrim.DiscriminationAlgorithm#discriminate(InboundVirtualConnection,Object, ConnectionLink)
     */
    public int discriminate(InboundVirtualConnection vc, Object discrimData, ConnectionLink prevChannelLink) {
        return DiscriminationProcess.FAILURE;
    }

}
