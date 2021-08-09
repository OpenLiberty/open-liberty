/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.test.api.testobjects;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpObjectFactory;
import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Testable version of the inbound service context that does not required an
 * underlying socket connection.
 */
public class MockInboundSC extends HttpInboundServiceContextImpl {
    private HttpObjectFactory factory = new HttpObjectFactory();
    private HttpRequestMessageImpl req;

    /**
     * Constructor.
     * 
     * @param vc
     * @param cfg
     */
    public MockInboundSC(VirtualConnection vc, HttpChannelConfig cfg) {
        super(null, null, vc, cfg);
    }

    protected HttpRequestMessageImpl getRequestImpl() {
        if (null == req) {
            req = new MockRequestMessage(this);
        }
        return req;
    }

    public HttpObjectFactory getObjectFactory() {
        return this.factory;
    }

}
