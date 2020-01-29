/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.filter;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.ibm.ejs.ras.TraceNLS;

public class ValueIPAddress implements IValue {

    private InetAddress myIP;

    /**
     * @throws UnknownHostException
     */
    public ValueIPAddress(String ip) throws FilterException {
        super();
        try {
            this.myIP = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            String msg = TraceNLS.getFormattedMessage(this.getClass(),
                    TraceConstants.MESSAGE_BUNDLE,
                    "security.tai.ipstring.convert.error",
                    new Object[] { ip },
                    "CWTAI0045E: Cannot convert   the   IP string {0} to an IP address.");
            // message is a little different, more spaces, so if bundle lookup isn't working, unittest will fail.
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
