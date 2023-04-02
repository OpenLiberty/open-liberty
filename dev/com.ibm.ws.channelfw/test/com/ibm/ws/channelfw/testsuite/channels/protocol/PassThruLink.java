/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.channelfw.testsuite.channels.protocol;

import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.InboundProtocolLink;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Test channel that sits above TCP and looks like TCP to those above it.
 */
@SuppressWarnings("unused")
public class PassThruLink extends InboundProtocolLink {
    /**
     * Constructor.
     */
    public PassThruLink() {
        // nothing
    }

    public Object getChannelAccessor() {
        return TCPConnectionContext.class;
    }

    public void ready(VirtualConnection inVC) {
        // nothing
    }

}
