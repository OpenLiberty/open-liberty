/*
 * Copyright 2026 IBM Corporation and others.
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

import org.apache.yoko.orb.OB.PROTOCOL_POLICY_ID;
import org.apache.yoko.orb.OB.ProtocolPolicy;
import org.apache.yoko.orb.OB.ProtocolPolicyHelper;
import org.apache.yoko.orb.OCI.ConnectCB;
import org.apache.yoko.orb.OCI.Connector;
import org.apache.yoko.orb.OCI.ProfileInfo;
import org.apache.yoko.orb.OCI.ProfileInfoHolder;
import org.apache.yoko.orb.OCI.ProfileInfoSeqHolder;
import org.apache.yoko.orb.OCI.Transport;
import org.omg.CORBA.Policy;
import org.omg.CORBA.SystemException;
import org.omg.CSIIOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.IOP.Codec;
import org.omg.IOP.IOR;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.IOP.TaggedComponent;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.FINE;
import static java.util.logging.Logger.getLogger;
import static org.apache.yoko.logging.VerboseLogging.CONN_LOG;
import static org.apache.yoko.logging.VerboseLogging.CONN_OUT_LOG;
import static org.apache.yoko.logging.VerboseLogging.logged;
import static org.apache.yoko.logging.VerboseLogging.wrapped;
import static org.apache.yoko.orb.OB.Net.CompareHosts;
import static org.apache.yoko.orb.OCI.IIOP.Exceptions.asCommFailure;
import static org.apache.yoko.orb.OCI.IIOP.Util.extractProfileInfo;
import static org.apache.yoko.orb.exceptions.Transients.CONNECT_FAILED;
import static org.apache.yoko.util.HexConverter.toHex;
import static org.apache.yoko.util.MinorCodes.MinorSocket;

final class Connector_impl extends org.omg.CORBA.LocalObject implements Connector {
    static final Logger logger = getLogger(Connector_impl.class.getName());

    private final IOR ior_;    // the target IOR we're connecting with

    private final Policy[] policies_;    // the policies used for the connection.

    private final boolean keepAlive_; // The keepalive flag

    private final ConnectorInfo_impl info_; // Connector information

    private Socket socket_; // The socket

    private final ListenerMap listenMap_;

    private final UnifiedConnectionHelper connectionHelper;

    private final byte[] transportInfo;

    private final Codec codec_;


    // ------------------------------------------------------------------
    // Private and protected functions
    // ------------------------------------------------------------------

    private void close() {
        logger.fine(() -> "Closing connection to host=" + this.info_.getHost() + ", port=" + this.info_.getPort());

        //
        // Close the socket
        //
        try {
            socket_.close();
            socket_ = null;
        } catch (IOException ex) {
            //ignore
        }
    }

    // ------------------------------------------------------------------
    // Standard IDL to Java Mapping
    // ------------------------------------------------------------------

    public String id() {
        return PLUGIN_ID.value;
    }

    public int tag() {
        return TAG_INTERNET_IOP.value;
    }

    public Transport connect() {
        if (socket_ != null)
            close();

        final String targetDesc = ("host=" + info_.getHost() + ", port=" + info_.getPort());
        try {
            logger.fine(() -> "Connecting to " + targetDesc);
            socket_ = connectionHelper.createSocket(info_.getHost(), info_.getPort(), ior_, policies_);
            logger.fine(() -> "Connection created with socket " + socket_);
        } catch (ConnectException ex) {
            throw wrapped(CONN_LOG, ex, "Error connecting to " + targetDesc, CONNECT_FAILED);
        } catch (IOException ex) {
            throw logged(CONN_LOG, asCommFailure(ex, MinorSocket), "Error connecting to " + targetDesc);
        }

        //
        // Set TCP_NODELAY and SO_KEEPALIVE options
        //
        try {
            socket_.setTcpNoDelay(true);

            if (keepAlive_)
                socket_.setKeepAlive(true);
        } catch (java.net.SocketException ex) {
            logger.log(FINE, ex, () -> "Socket setup error");
            try {
                socket_.close();
            } catch (IOException ignored) {
            }
            throw asCommFailure(ex);
        }

        //
        // Create new transport
        //
        Transport tr;
        try {
            tr = new Transport_impl(socket_, listenMap_);
            socket_ = null;
        } catch (SystemException ex) {
            logger.log(FINE, ex, () -> "Transport creation error");
            try {
                socket_.close();
            } catch (IOException ignored) {
            }
            throw ex;
        }

        //
        // Call callbacks
        //
        org.apache.yoko.orb.OCI.TransportInfo trInfo = tr.get_info();
        try {
            info_._OB_callConnectCB(trInfo);
        } catch (SystemException ex) {
            logger.log(FINE, ex, () -> "Connection callback error");
            tr.close();
            throw ex;
        }

        //
        // Return new transport
        //
        return tr;
    }

    //
    // Helper class for connect_timeout()
    //
    private class ConnectTimeout extends Thread {
        private final CompletableFuture<Socket> socketFuture = new CompletableFuture<>();

        public void run() {
            final Socket so_;
            try {
                so_ = connectionHelper.createSocket(info_.getHost(), info_.getPort(), ior_, policies_);
                if (socketFuture.complete(so_)) return;
                try {
                    so_.close();
                } catch (IOException ignored) {}
            } catch (IOException e) {
                socketFuture.completeExceptionally(e);
            }
        }

        synchronized Socket waitForConnect(int t) throws IOException {
            for (;;) {
                try {
                    return socketFuture.get(t, MILLISECONDS);
                } catch (InterruptedException ignored) {
                } catch (TimeoutException e) {
                    if (socketFuture.cancel(false)) return null;
                } catch (ExecutionException e) {
                    throw (IOException) e.getCause();
                }
            }
        }
    }

    public Transport connect_timeout(int t) {
        if (null != socket_) close();

        // Create socket and connect
        try {
            final ConnectTimeout connectTimeout = new ConnectTimeout();
            connectTimeout.start();

            socket_ = connectTimeout.waitForConnect(t);

            if (null == socket_) return null;
        } catch (ConnectException ex) {
            throw wrapped(CONN_OUT_LOG, ex, "Socket connection error", CONNECT_FAILED);
        } catch (IOException ex) {
            logger.log(FINE, ex, () -> "Socket I/O error");
            throw asCommFailure(ex, MinorSocket);
        }

        //
        // Set TCP_NODELAY and SO_KEEPALIVE options
        //
        try {
            socket_.setTcpNoDelay(true);

            if (keepAlive_)
                socket_.setKeepAlive(true);
        } catch (java.net.SocketException ex) {
            logger.log(FINE, ex, () -> "Socket setup error");
            try {
                socket_.close();
            } catch (IOException ignored) {
            }
            throw asCommFailure(ex);
        }

        //
        // Create new transport
        //
        Transport tr;
        try {
            tr = new Transport_impl(socket_, listenMap_);
            socket_ = null;
        } catch (SystemException ex) {
            logger.log(FINE, ex, () -> "Transport setup error");
            try {
                socket_.close();
            } catch (IOException ignored) {
            }
            throw ex;
        }

        //
        // Call callbacks
        //
        org.apache.yoko.orb.OCI.TransportInfo trInfo = tr.get_info();
        try {
            info_._OB_callConnectCB(trInfo);
        } catch (SystemException ex) {
            logger.log(FINE, ex, () -> "Callback setup error");
            tr.close();
            throw ex;
        }

        //
        // Return new transport
        //
        return tr;
    }

    public ProfileInfo[] get_usable_profiles(IOR ior, Policy[] policies) {
        //
        // Make sure that the set of policies is met
        //
        for (Policy policy : policies) {
            if (policy.policy_type() == PROTOCOL_POLICY_ID.value) {
                ProtocolPolicy protocolPolicy = ProtocolPolicyHelper.narrow(policy);
                if (!protocolPolicy.contains(PLUGIN_ID.value)) {
                    logger.fine(() -> "Protocol policy exists but does not allow expected transport. policy = " + Arrays.toString(protocolPolicy.value()) + "\t expected transport = " + PLUGIN_ID.value);
                    return new ProfileInfo[0];
                }
            }
        }

        ProfileInfoSeqHolder profileInfoSeq = new ProfileInfoSeqHolder();
        profileInfoSeq.value = new ProfileInfo[0];
        String host = info_.getHost();
        if (Util.isEncodedHost(host)) host = Util.decodeHost(host);
        Util.extractAllProfileInfos(ior, profileInfoSeq, true, host, info_.getPort(), false, codec_);

        //check that the transport info matches ours.
        //we could return just the profiles that match rather than bailing if one doesn't match.
        for (ProfileInfo profileInfo : profileInfoSeq.value) {
            byte[] otherTransportInfo = Stream.of(profileInfo.components)
                    .filter(c -> TAG_CSI_SEC_MECH_LIST.value == c.tag)
                    .map(c -> c.component_data)
                    .peek(data -> logger.fine(() -> "Found CSI_SEC_MECH_LIST: " + toHex(data)))
                    .findFirst()
                    .orElse(new byte[0]);
            if (!Arrays.equals(transportInfo, otherTransportInfo)) {
                logger.fine(() -> "Transport info does not match CSI_SEC_MECH_LIST: " + toHex(otherTransportInfo));
                return new ProfileInfo[0];
            }
        }
        return profileInfoSeq.value;
    }

    public boolean equal(Connector con) {
        return (con instanceof Connector_impl) && equal0((Connector_impl)con);
    }

    private boolean equal0(Connector_impl that) {
        if (this.info_.getPort() != that.info_.getPort()) return false;
        // an SSL connector must never match a plain one for the same host:port
        if (!this.info_.protocol().equals(that.info_.protocol())) return false;
        return (CompareHosts(this.info_.getHost(), that.info_.getHost()) && Arrays.equals(transportInfo, that.transportInfo));
    }

    private byte[] extractTransportInfo(IOR ior) {
        ProfileInfoHolder holder = new ProfileInfoHolder();
        // we need to extract the profile information from the IOR to see if this connection has
        // any transport-level security defined.
        if (extractProfileInfo(ior, holder)) {
            ProfileInfo profileInfo = holder.value;
            for (TaggedComponent component : profileInfo.components) {
                // we're looking for the security mechanism items.
                if (component.tag == TAG_CSI_SEC_MECH_LIST.value) {
                    return component.component_data;
                }
            }
        }
        return new byte[0];
    }

    public org.apache.yoko.orb.OCI.ConnectorInfo get_info() {
        return info_;
    }

    // ------------------------------------------------------------------
    // Yoko internal functions
    // Application programs must not use these functions directly
    // ------------------------------------------------------------------

    Connector_impl(IOR ior, Policy[] policies, String host, int port, boolean keepAlive, ConnectCB[] cb, ListenerMap lm, UnifiedConnectionHelper helper, Codec codec) {
        ior_ = ior;
        policies_ = policies;
        keepAlive_ = keepAlive;
        info_ = new ConnectorInfo_impl(host, port, cb);
        listenMap_ = lm;
        connectionHelper = requireNonNull(helper);
        codec_ = codec;
        transportInfo = extractTransportInfo(ior);
    }

    protected void finalize() throws Throwable {
        if (socket_ != null)
            close();

        super.finalize();
    }

    @Override
    public String toString() {
        return "-> " + info_;
    }
}
