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

/**
 * Dummy protocol-type connection link object.
 */
@SuppressWarnings("unused")
public class ProtocolDummyLink extends InboundProtocolLink {
    private ProtocolDummyContext context = null;

    /**
     * Constructor.
     */
    public ProtocolDummyLink() {
        this.context = new ProtocolDummyContext();
    }

    public Object getChannelAccessor() {
        return this.context;
    }

    public void ready(VirtualConnection inVC) {
        //
    }

}
