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
 * Class to represent a DNS SRV resource record
 * <p>
 * See RFC 2782
 * <p>
 * The field members of this class represent the rdata portion of a resource
 * record
 *
 */
public class SRVRecord extends ResourceRecord {

    private String _service;
    private String _protocol;
    private String _name;
    private short _priority;
    private short _weight;
    private short _port;
    private Name _target;

    protected SRVRecord() {
        _service = null;
        _protocol = null;
        _name = null;
        _priority = 0;
        _weight = 0;
        _port = 0;
        _target = null;
    }

    protected SRVRecord(ByteBuf buffer, Name name) {

        super(buffer);

        String[] sa = name.getString().split("\\.");
        _service = sa[0];
        _protocol = sa[1];

        _name = "";

        for (int i = 2; i < sa.length; i++) {
            _name = _name + sa[i] + ".";
        }

        _priority = buffer.readShort();
        _weight = buffer.readShort();
        _port = buffer.readShort();
        _target = new Name(buffer);

    }

    public void setService(String s) {
        _service = s;

    }

    public void setProtocol(String s) {
        _protocol = s;

    }

    public void setName(String s) {
        _name = s;
    }

    public void setPriority(short s) {
        _priority = s;
    }

    public Short getPriority() {
        return _priority;
    }

    public void setWeight(short s) {
        _weight = s;
    }

    public Short getWeight() {
        return _weight;
    }

    public void setPort(short s) {
        _port = s;
    }

    public short getPort() {
        return _port;
    }

    public String getService() {
        return _service;
    }

    public String getProtocol() {
        return _protocol;
    }

    public String getSrvName() {
        return _name;
    }

    public void setTarget(Name name) {
        _target = name;
    }

    public Name getTarget() {
        return _target;
    }

    protected void toBuffer(ByteBuf buffer) {
        super.toBuffer(buffer);
        buffer.writeShort(_priority);
        buffer.writeShort(_weight);
        buffer.writeShort(_port);
        _target.toBuffer(buffer);
    }

    public short calcrdLength() {
        int length = 0;
        length = Short.SIZE / 8 + Short.SIZE / 8 + Short.SIZE / 8 + _target.length();
        return (short) length;
    }

    public String toString() {
        String s = new String();
        s = super.toString();
        s += "      service: " + _service.toString() + "\n" + "      protocol: " + _protocol.toString() + "\n"
                + "      name: " + _name.toString() + "\n" + "      priority: " + (new Short(_priority)).toString()
                + "\n" + "      weight: " + (new Short(_weight)).toString() + "\n" + "      port: "
                + (new Short(_port)).toString() + "\n" + "      target: " + _target.toString() + "\n";

        return s;
    }
}
