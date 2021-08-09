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
package com.ibm.ws.channelfw.internal;

import com.ibm.websphere.channelfw.FlowType;
import com.ibm.wsspi.channelfw.InboundVirtualConnectionFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Inbound VirtualConnectionFactory implementation.
 * 
 */
public class InboundVirtualConnectionFactoryImpl implements InboundVirtualConnectionFactory {

    /**
     * Constructor for Inbound types.
     */
    public InboundVirtualConnectionFactoryImpl() {
        // Nothing needed here at this time.
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.InboundVirtualConnectionFactory#createConnection()
     */
    public VirtualConnection createConnection() {
        VirtualConnectionImpl vc = new InboundVirtualConnectionImpl();
        vc.init();

        return vc;
    }

    /*
     * @see com.ibm.wsspi.channelfw.VirtualConnectionFactory#getType()
     */
    public FlowType getType() {
        return FlowType.INBOUND;
    }

    /*
     * @see com.ibm.wsspi.channelfw.VirtualConnectionFactory#getName()
     */
    public String getName() {
        return "inbound";
    }

    /*
     * @see com.ibm.wsspi.channelfw.VirtualConnectionFactory#destroy()
     */
    public void destroy() {
        // Nothing needed here at this time.
    }

}
