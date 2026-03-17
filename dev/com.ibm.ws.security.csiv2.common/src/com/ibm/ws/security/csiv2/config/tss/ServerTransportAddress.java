/*
 * Copyright 2017,2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.security.csiv2.config.tss;

import java.io.Serializable;
import java.util.Objects;

import org.omg.CSIIOP.TransportAddress;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Wrapper around TransportAddress to be able to uniquely insert into collections that use hash codes.
 */
@Trivial
public class ServerTransportAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    private final TransportAddress transportAddress;

    public ServerTransportAddress(TransportAddress transportAddress) { this.transportAddress = transportAddress; }

    public TransportAddress getTransportAddress() { return transportAddress; }

    public String getHost() { return transportAddress.host_name; }

    public short getPort() { return transportAddress.port; }

    @Override
    public int hashCode() { return transportAddress.host_name.hashCode() + transportAddress.port; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ServerTransportAddress)) return false;
        ServerTransportAddress that = (ServerTransportAddress) other;
        return Objects.equals(this.transportAddress.host_name, that.transportAddress.host_name)
                && this.transportAddress.port == that.transportAddress.port;
    }

    public String toString() { return String.format("ServerTransportAddress{%s:%d}", getHost(), 0xFFFF & getPort()); }
}
