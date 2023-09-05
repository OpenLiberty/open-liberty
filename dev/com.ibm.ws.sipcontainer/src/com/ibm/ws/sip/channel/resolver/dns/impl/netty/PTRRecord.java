/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.dns.impl.netty;

import io.netty.buffer.ByteBuf;

/**
 * Class to represent a DNS PTR resource record
 * <p>
 * See RFC 1035
 * <p>
 * The field members of this class represent the rdata portion of a resource
 * record
 * 
 */
public class PTRRecord extends ResourceRecord {

    private Name _ptrDname;

    protected PTRRecord() {
        _ptrDname = null;
    }

    protected PTRRecord(ByteBuf buffer) {

        super(buffer);
        _ptrDname = new Name(buffer);

    }

    public void setPtrDname(Name name) {
        _ptrDname = name;

    }

    public Name getPtrDname() {
        return _ptrDname;
    }

    protected void toBuffer(ByteBuf buffer) {
        super.toBuffer(buffer);
        _ptrDname.toBuffer(buffer);
    }

    public short calcrdLength() {
        int length = 0;
        length = _ptrDname.length();
        return (short) length;
    }

    public String toString() {
        String s = new String();
        s = super.toString();
        s += "      ptrDname: " + _ptrDname.toString() + "\n";
        return s;
    }

}
