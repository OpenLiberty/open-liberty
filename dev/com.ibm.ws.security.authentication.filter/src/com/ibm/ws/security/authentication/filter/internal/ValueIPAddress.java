/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.filter.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.ibm.ejs.ras.TraceNLS;

public class ValueIPAddress implements IValue {
    InetAddress myIP;

    /**
     * @throws UnknownHostException
     * 
     */
    public ValueIPAddress(String ip) throws FilterException {
        super();
        try {
            this.myIP = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            String msg = TraceNLS.getFormattedMessage(this.getClass(),
                                                      TraceConstants.MESSAGE_BUNDLE,
                                                      "AUTH_FILTER_IP_STRING_CONVERT_ERROR",
                                                      new Object[] { ip },
                                                      "CWWKS1756E: Cannot convert the IP string {0} to an IP address.");

            throw new FilterException(msg, e);
        }
    }

    @Override
    public boolean equals(IValue ip) {
        if (ip.getClass() != ValueIPAddress.class)
            return false;
        else
            return ((ValueIPAddress) ip).getIP().equals(getIP());
    }

    @Override
    public boolean greaterThan(IValue ip) {
        if (ip.getClass() != ValueIPAddress.class)
            return false;

        return (IPAddressRange.greaterThan(getIP(), ((ValueIPAddress) ip).getIP()));
    }

    @Override
    public boolean lessThan(IValue ip) {
        if (ip.getClass() != ValueIPAddress.class)
            return false;
        return (IPAddressRange.lessThan(getIP(), ((ValueIPAddress) ip).getIP()));
    }

    @Override
    public boolean containedBy(IValue ip) {
        return this.equals(ip);
    }

    @Override
    public String toString() {
        return getIP().toString();
    }

    protected InetAddress getIP() {
        return myIP;
    }
}
