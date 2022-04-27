/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.dns.impl.netty;

import io.netty.buffer.ByteBuf;

/**
 * Class to represent a DNS Naming Authority pointer (NAPTR) resource record
 * <p>
 * See RFC 2915
 * <p>
 * The field members of this class represent the rdata portion of a resource
 * record
 * 
 */
public class NAPTRRecord extends ResourceRecord {

    private short _order;
    private short _preference;
    private String _flags;
    private String _service;
    private String _regexp;
    private Name _replacement;

    protected NAPTRRecord() {
        _order = 0;
        _preference = 0;
        _flags = new String();
        _service = new String();
        _regexp = new String();
        _replacement = null;
    }

    protected NAPTRRecord(ByteBuf buffer) {

        super(buffer);
        _order = buffer.readShort();
        _preference = buffer.readShort();

        byte length = buffer.readByte();
        byte[] b = new byte[length];
        buffer.readBytes(b);
        _flags = new String(b);

        length = buffer.readByte();
        b = new byte[length];
        buffer.readBytes(b);
        _service = new String(b);

        length = buffer.readByte();
        b = new byte[length];
        buffer.readBytes(b);

        _regexp = new String(b);
        _replacement = new Name(buffer);
    }

    protected void toBuffer(ByteBuf buffer) {
        super.toBuffer(buffer);
        buffer.writeShort(_order);
        buffer.writeShort(_preference);
        buffer.writeByte((byte) _flags.length());
        buffer.writeBytes(_flags.getBytes());
        buffer.writeByte((byte) _service.length());
        buffer.writeBytes(_service.getBytes());
        buffer.writeByte((byte) _regexp.length());
        buffer.writeBytes(_regexp.getBytes());
        _replacement.toBuffer(buffer);
    }

    public void setOrder(short s) {
        _order = s;
    }

    public Short getOrder() {
        return _order;
    }

    public void setPreference(short s) {
        _preference = s;
    }

    public Short getPreference() {
        return _preference;
    }

    public void setFlags(String s) {
        _flags = s;
    }

    public void setService(String s) {
        _service = s;
    }

    public String getService() {
        return _service;
    }

    public void setRegexp(String s) {
        _regexp = s;
    }

    public void setReplacement(Name name) {
        _replacement = name;
    }

    public Name getReplacement() {
        return _replacement;
    }

    public short calcrdLength() {
        int length = 0;

        length = Short.SIZE / 8 + Short.SIZE / 8 + 1 + _flags.length() + 1 + _service.length() + 1 + _regexp.length()
                + _replacement.length();
        return (short) length;
    }

    public String toString() {

        String s = new String();

        s += super.toString();

        s += "        order: " + (new Short(_order)).toString() + "\n" + "        preference: "
                + (new Short(_preference)).toString() + "\n" + "        flags: " + _flags.toString() + "\n"
                + "        service: " + _service.toString() + "\n" + "        regexp: " + _regexp.toString() + "\n"
                + "        replacement: " + _replacement.toString() + "\n";

        return s;
    }
}
