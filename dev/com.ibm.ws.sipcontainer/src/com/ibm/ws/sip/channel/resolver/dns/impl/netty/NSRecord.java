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
 * Class to represent a DNS CNAME resource record
 * <p>
 * See RFC 1035
 * <p>
 * The field members of this class represent the RDATA portion of a resource
 * record
 */
public class NSRecord extends ResourceRecord {

    private Name _NSname;

    protected NSRecord() {
        _NSname = null;
    }

    protected NSRecord(ByteBuf buffer) {

        super(buffer);
        _NSname = new Name(buffer);

    }

    protected void toBuffer(ByteBuf buffer) {
        super.toBuffer(buffer);
        _NSname.toBuffer(buffer);
    }

    public void setNSname(Name name) {
        _NSname = name;
    }

    public Name getNSname() {
        return _NSname;
    }

    public short calcrdLength() {
        int length = 0;
        length = _NSname.length();
        return (short) length;
    }

    public String toString() {
        return super.toString() + "      name: " + _NSname.toString() + "\n";
    }

}
