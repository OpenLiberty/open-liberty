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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.apache.yoko.orb.OCI.IIOP;

import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.orb.OB.CorbalocProtocol;
import org.omg.CORBA.BAD_PARAM;
import org.omg.IIOP.ProfileBody_1_1;
import org.omg.IIOP.ProfileBody_1_1Helper;
import org.omg.IIOP.Version;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.TaggedProfile;

import static java.lang.Integer.parseInt;
import static org.apache.yoko.logging.VerboseLogging.IOR_LOG;
import static org.apache.yoko.util.MinorCodes.MinorBadAddress;
import static org.apache.yoko.util.MinorCodes.describeBadParam;
import static org.omg.CORBA.CompletionStatus.COMPLETED_NO;

/**
 * A {@link CorbalocProtocol} implementation for the {@code iiops} scheme.
 * <p>
 * When a {@code corbaloc:iiops:host:port/key} (or
 * {@code corbaname:iiops:host:port#name}) URL is resolved, this class
 * synthesises a 1.2 IIOP {@link TaggedProfile} whose host string is
 * SSL-encoded via {@link Util#encodeHost(String, String, String)}.
 */
public class IIOPSCorbalocProtocol_impl implements CorbalocProtocol {

    /** Empty string signals "use default SSL config" — passed as info to {@link Util#encodeHost}. */
    static final String DEFAULT_SSL_CONFIG = "";

    private final String sslConfigName;

    /**
     * @param sslConfigName the SSL config name passed to
     *                      {@link Util#encodeHost(String, String, String)};
     *                      {@code null} or empty string means use the default SSL config.
     */
    public IIOPSCorbalocProtocol_impl(String sslConfigName) {
        this.sslConfigName = (sslConfigName != null) ? sslConfigName : DEFAULT_SSL_CONFIG;
    }

    @Override
    public String name() {
        return "iiops";
    }

    @Override
    public TaggedProfile parseAddress(String addr, byte[] key) {
        IOR_LOG.finest(() -> "iiops: parsing address: " + '"' + addr + '"');

        // Strip the optional "1.X@" version prefix. The profile is always written as IIOP 1.2
        // so that it can carry TaggedComponents (needed by CSIv2 component processing).
        String[] parts = addr.split("@", 2);
        final String hostAndPort = (parts.length == 1) ? parts[0] : parts[1];

        // Split host from port
        parts = hostAndPort.split(":", 2);
        final String rawHost = parts[0];
        if (rawHost.isEmpty())
            throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": iiops hostname must be specified: " + addr, MinorBadAddress, COMPLETED_NO);

        final short port = getPort(parts);

        final String encodedHost = Util.encodeHost(rawHost, "ssl", sslConfigName);
        IOR_LOG.finest(() -> "iiops: encoded host: " + encodedHost + " port: " + port);

        TaggedProfile profile = new TaggedProfile();
        profile.tag = TAG_INTERNET_IOP.value;
        try (YokoOutputStream out = new YokoOutputStream()) {
            out._OB_writeEndian();
            ProfileBody_1_1Helper.write(out, new ProfileBody_1_1(new Version((byte)1, (byte)2), encodedHost, port, key, new TaggedComponent[0]));
            profile.profile_data = out.copyWrittenBytes();
        }
        return profile;
    }

    private static short getPort(String[] parts) {
        if (parts.length == 1) return (short) 2809; // default IIOP port
        final String portStr = parts[1];
        final int port;
        try {
            port = parseInt(portStr);
        } catch (NumberFormatException ex) {
            throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": iiops port is invalid", MinorBadAddress, COMPLETED_NO);
        }
        if (port < 1 || port > 65535)
            throw new BAD_PARAM(describeBadParam(MinorBadAddress) + ": iiops port must be between 1 and 65535: " + portStr, MinorBadAddress, COMPLETED_NO);
        return (short) port;
    }
}
