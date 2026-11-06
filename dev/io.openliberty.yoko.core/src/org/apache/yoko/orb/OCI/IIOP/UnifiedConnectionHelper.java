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
import java.util.Collections;
import java.util.Set;

public interface UnifiedConnectionHelper {
    void init(ORB orb, String params);
    Socket createSocket(String host, int port, IOR ior, Policy... policies) throws IOException;
    Socket createSelfConnection(InetAddress address, int port) throws IOException;
    ServerSocket createServerSocket(int port, int backlog, String... params)  throws IOException;
    ServerSocket createServerSocket(int port, int backlog, InetAddress address, String... params) throws IOException;
    default Set<Integer> tags() { return Collections.emptySet(); }
    default TransportAddress[] getEndpoints(TaggedComponent taggedComponent, Policy... policies) { return new TransportAddress[0]; }
    default boolean isExtended() { return false; }
    /** Returns the default SSL config name to use for iiops:// corbaloc addresses, or null if SSL is not configured. */
    default String getDefaultSslConfigName() { return null; }
}
