/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.dns.impl.netty;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import io.netty.buffer.ByteBuf;

/**
 * Class to represent a DNS A resource record
 * <p>
 * See RFC 1035
 * <p>
 * The field members of this class represent the RDATA portion of a resource
 * record
 */
public class ARecord extends ResourceRecord {

    private Inet4Address _address;

    protected ARecord() {
        _address = null;
    }

    protected ARecord(ByteBuf buffer) {

        super(buffer);
        byte[] ba = new byte[4];
        ba[0] = buffer.readByte();
        ba[1] = buffer.readByte();
        ba[2] = buffer.readByte();
        ba[3] = buffer.readByte();
        try {
            _address = (Inet4Address) Inet4Address.getByAddress(ba);
        } catch (UnknownHostException uhe) {
        }

    }

    protected void toBuffer(ByteBuf buffer) {
        super.toBuffer(buffer);
        buffer.writeBytes(_address.getAddress());
    }

    public void setAddress(Inet4Address address) {
        _address = address;
    }

    public Inet4Address getAddress() {
        return _address;
    }

    public short calcrdLength() {
        return (short) 4;
    }

    public String toString() {
        String s = new String();
        s = super.toString();
        s += "      address: " + _address.getHostAddress() + "\n";
        return s;
    }

}
