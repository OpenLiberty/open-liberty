/*
 * Copyright 2025 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.orb.OCI.IIOP;

import org.apache.yoko.orb.OCI.ConnectCB;
import org.omg.CORBA.LocalObject;
import org.omg.IOP.TAG_INTERNET_IOP;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.yoko.orb.OCI.IIOP.Exceptions.asCommFailure;

/**
 * Immutable memo of the endpoint details for a connection. The InetAddress
 * is looked up when first needed but never changed again. It is used in
 * hashcode() and equals().
 */
public final class ConnectorInfo_impl extends LocalObject implements ConnectorInfo {
    private final String host;
    private final short port;
    private volatile InetAddress addr; // initialised lazily

    //
    // All connect callback objects
    //
    private final List<ConnectCB> callbacks;

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public String id() {return PLUGIN_ID.value;}

    public int tag() {return TAG_INTERNET_IOP.value;}

    public String describe() {return String.format("%s -> %s:%d", id(),  remote_addr(), getPort());}

    public String remote_addr() {return getInetAddress().getHostAddress();}

    public short remote_port() {return port;}

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    ConnectorInfo_impl(String host, int port, ConnectCB...cb) {
        this.host = host;
        this.port = (short)port;
        if (cb == null || cb.length == 0)
            callbacks = emptyList();
        else
            callbacks = unmodifiableList(new ArrayList<ConnectCB>(asList(cb)));
    }

    String getHost() { return host; }

    int getPort() { return (char)port; }

    private InetAddress getInetAddress() {
        if (addr == null) synchronized (this) {
            if (addr == null) try {
                String h = Util.decodeHost(this.host);
                addr = Util.getInetAddress(h);
            } catch (UnknownHostException ex) {
                throw asCommFailure(ex);
            }
        }
        return addr;
    }

    synchronized void _OB_callConnectCB(org.apache.yoko.orb.OCI.TransportInfo info) {
        for (ConnectCB cb : callbacks) cb.connect_cb(info);
    }

    /**
     * Returns the transport protocol prefix encoded in the host string (e.g. {@code "_ssl"}),
     * or an empty string for a plain (unencoded) host.
     * Two connectors to the same host and port but with different protocols must not be
     * considered equal, otherwise Yoko's connector cache could reuse an SSL connection
     * for a plain request (or vice versa).
     */
    String protocol() {
        if (!Util.isEncodedHost(host)) return "";
        int end = host.indexOf('[');
        return end < 0 ? host : host.substring(0, end);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!!! (other instanceof ConnectorInfo_impl)) return false;

        ConnectorInfo_impl that = (ConnectorInfo_impl) other;

        return (this.port == that.port)
                && this.protocol().equals(that.protocol())
                && this.getInetAddress().equals(that.getInetAddress());
    }

    @Override
    public int hashCode() {return 31*(31*port + protocol().hashCode()) + getInetAddress().hashCode();}

    @Override
    public String toString() {
        return "[" + host + ":" + (0xFFFF & port) + "]";
    }
}
