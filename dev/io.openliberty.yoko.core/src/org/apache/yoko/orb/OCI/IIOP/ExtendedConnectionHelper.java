/*
 * Copyright 2022 IBM Corporation and others.
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

import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CSIIOP.TransportAddress;
import org.omg.IOP.IOR;
import org.omg.IOP.TaggedComponent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;

public interface ExtendedConnectionHelper extends UnifiedConnectionHelperProvider
{
    void init(ORB orb, String params);

    /**
     * The host may be encoded as described in {@link #getEndpoints(TaggedComponent, Policy[])}.
     * Implementors should use and {@link Util#isEncodedHost(String)}, {@link Util#decodeHost(String)},
     * and {@link Util#decodeHostInfo(String)} to retrieve the encoded information.
     */
    Socket createSocket(String host, int port) throws IOException;

    Socket createSelfConnection(InetAddress address, int port) throws IOException;

    ServerSocket createServerSocket(int port, int backlog, String[] params)  throws IOException;

    ServerSocket createServerSocket(int port, int backlog, InetAddress address, String[] params) throws IOException;

    /**
     * The component tags this helper knows about, e.g. TAG_CSI_SEC_MECH_LIST.
     *
     * @return an array of known tags, possibly empty but not null
     */
    int[] tags();

    /**
     * The policy-compliant endpoints from the specified tagged component.
     * <br>
     * Note that the host strings in the endpoints may encode additional information.
     * Implementors should use {@link Util#encodeHost(String, String, String)} to encode the information.
     *
     * @param taggedComponent the tagged component to examine for endpoints:
     *                        note that <code>taggedComponent.tag</code> must be in <code>this.tags()</code>
     *
     * @param policies the policies against which to filter the possible endpoints
     *
     * @return a possibly empty but non-null array of endpoints
     */
    TransportAddress[] getEndpoints(TaggedComponent taggedComponent, Policy[] policies);

    default UnifiedConnectionHelper getUnifiedConnectionHelper() {
        return new UnifiedConnectionHelper() {
            @Override
            public void init(ORB orb, String params) {
                ExtendedConnectionHelper.this.init(orb, params);
            }

            @Override
            public Socket createSocket(String host, int port, IOR ior, Policy... policies) throws IOException {
                return ExtendedConnectionHelper.this.createSocket(host, port);
            }

            @Override
            public Socket createSelfConnection(InetAddress address, int port) throws IOException {
                return ExtendedConnectionHelper.this.createSelfConnection(address, port);
            }

            @Override
            public ServerSocket createServerSocket(int port, int backlog, String... params) throws IOException {
                return ExtendedConnectionHelper.this.createServerSocket(port, backlog, params);
            }

            @Override
            public ServerSocket createServerSocket(int port, int backlog, InetAddress address, String... params) throws IOException {
                return ExtendedConnectionHelper.this.createServerSocket(port, backlog, address, params);
            }

            @Override
            public Set<Integer> tags() {
                final int[] aTags = ExtendedConnectionHelper.this.tags();
                if ((null == aTags) || (0 == aTags.length)) return emptySet();
                return unmodifiableSet(Arrays.stream(aTags).boxed().collect(toSet()));
            }

            @Override
            public TransportAddress[] getEndpoints(TaggedComponent taggedComponent, Policy... policies) {
                return ExtendedConnectionHelper.this.getEndpoints(taggedComponent, policies);
            }

            @Override
            public boolean isExtended() { return true; }

            @Override
            public String getDefaultSslConfigName() {
                return ExtendedConnectionHelper.this.getDefaultSslConfigName();
            }
        };
    }

    /** Returns the default SSL config name for iiops: corbaloc addresses; override to provide one. */
    default String getDefaultSslConfigName() { return null; }
}

