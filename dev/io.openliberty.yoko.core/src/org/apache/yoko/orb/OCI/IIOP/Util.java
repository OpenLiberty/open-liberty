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

import org.apache.yoko.orb.CORBA.YokoInputStream;
import org.apache.yoko.orb.CORBA.YokoOutputStream;
import org.apache.yoko.util.Assert;
import org.apache.yoko.orb.OB.Net;
import org.apache.yoko.orb.OCI.ProfileInfo;
import org.apache.yoko.orb.OCI.ProfileInfoHolder;
import org.apache.yoko.orb.OCI.ProfileInfoSeqHolder;
import org.omg.CORBA.Any;
import org.omg.CSIIOP.CompoundSecMech;
import org.omg.CSIIOP.CompoundSecMechList;
import org.omg.CSIIOP.CompoundSecMechListHelper;
import org.omg.CSIIOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.CSIIOP.TAG_TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANSHelper;
import org.omg.CSIIOP.TransportAddress;
import org.omg.IIOP.ProfileBody_1_0;
import org.omg.IIOP.ProfileBody_1_0Helper;
import org.omg.IIOP.ProfileBody_1_1;
import org.omg.IIOP.ProfileBody_1_1Helper;
import org.omg.IIOP.Version;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.IOP.IOR;
import org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.TaggedComponentHelper;
import org.omg.IOP.TaggedProfile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final public class Util {
    static public IOR createIOR(String host, int port, String id, ProfileInfo profileInfo) {
        IOR ior = new IOR();
        ior.type_id = id;
        ior.profiles = new TaggedProfile[1];
        ior.profiles[0] = new TaggedProfile();
        ior.profiles[0].tag = TAG_INTERNET_IOP.value;

        if (profileInfo.major == 1 && profileInfo.minor == 0) {
            ProfileBody_1_0 body = new ProfileBody_1_0();
            body.iiop_version = new Version((byte) 1, (byte) 0);
            body.host = host;
            if (port >= 0x8000)
                body.port = (short) (port - 0xffff - 1);
            else
                body.port = (short) port;
            body.object_key = profileInfo.key;
            try (YokoOutputStream out = new YokoOutputStream()) {
                out._OB_writeEndian();
                ProfileBody_1_0Helper.write(out, body);
                ior.profiles[0].profile_data = out.copyWrittenBytes();
            }
        } else {
            ProfileBody_1_1 body = new ProfileBody_1_1();
            body.iiop_version = new Version(profileInfo.major,
                    profileInfo.minor);
            body.host = host;
            if (port >= 0x8000)
                body.port = (short) (port - 0xffff - 1);
            else
                body.port = (short) port;
            body.object_key = profileInfo.key;
            body.components = profileInfo.components;
            try (YokoOutputStream out = new YokoOutputStream()) {
                out._OB_writeEndian();
                ProfileBody_1_1Helper.write(out, body);
                ior.profiles[0].profile_data = out.copyWrittenBytes();
            }
        }

        return ior;
    }

    static public IOR createIOR(IOR ior, String id, byte[] key) {
        //
        // Extract the IIOP profile information from the provided IOR
        //
        int profile;
        for (profile = 0; profile < ior.profiles.length; profile++)
            if (ior.profiles[profile].tag == TAG_INTERNET_IOP.value)
                break;

        // TODO: Internal error?
        Assert.ensure(profile < ior.profiles.length);

        YokoInputStream in = new YokoInputStream(ior.profiles[profile].profile_data);
        in._OB_readEndian();
        ProfileBody_1_0 body = ProfileBody_1_0Helper
                .read(in);

        ProfileInfo profileInfo = new ProfileInfo();
        profileInfo.key = key;
        profileInfo.major = body.iiop_version.major;
        profileInfo.minor = body.iiop_version.minor;
        profileInfo.components = new TaggedComponent[0];
        return createIOR(body.host, body.port, id, profileInfo);
    }

    static public boolean extractProfileInfo(IOR ior, ProfileInfoHolder profileInfo) {
        ProfileInfoSeqHolder profileInfoSeq = new ProfileInfoSeqHolder();
        profileInfoSeq.value = new ProfileInfo[0];
        extractAllProfileInfos(ior, profileInfoSeq, false, null, 0, false, null);
        if (profileInfoSeq.value.length > 0) {
            profileInfo.value = profileInfoSeq.value[0];
            return true;
        }

        return false;
    }

    static public boolean hostMatch(String host1, String host2, boolean matchLoopback) {
        return Net.CompareHosts(host1, host2, matchLoopback);
    }

    static public void extractAllProfileInfos(IOR ior, ProfileInfoSeqHolder profileInfoSeq,
            boolean performMatch, String host, int port, boolean matchLoopback, Codec codec) {
        short portNo = (short) port;

        List<ProfileInfo> list = new ArrayList<>();

        for (int i = 0; i < ior.profiles.length; i++) {
            if (ior.profiles[i].tag != TAG_INTERNET_IOP.value) continue;
            //
            // Get the IIOP profile body
            //
            YokoInputStream in = new YokoInputStream(ior.profiles[i].profile_data);
            in._OB_readEndian();
            ProfileBody_1_0 body = ProfileBody_1_0Helper.read(in);

            //
            // Read components if the IIOP version is > 1.0
            //
            TaggedComponent[] components;
            if (body.iiop_version.major > 1 || body.iiop_version.minor > 0) {
                int len = in.read_ulong();
                components = new TaggedComponent[len];
                for (int j = 0; j < len; j++) {
                    components[j] = TaggedComponentHelper.read(in);
                }
            } else {
                components = new TaggedComponent[0];
            }

            // add this profile to the list unless
            // A) the caller requested matching
            // B) the profile doesn't match the supplied host and port
            // The profile host may be encoded (e.g. by the iiops corbaloc protocol), so decode it
            // before comparing it with the (already decoded) connector host.
            if (!!!performMatch
                    || hostAndPortMatch(host, portNo, isEncodedHost(body.host) ? decodeHost(body.host) : body.host, body.port, matchLoopback)
                    || taggedComponentsMatch(components, host, portNo, codec, matchLoopback)) {
                ProfileInfo profileInfo = new ProfileInfo();
                profileInfo.key = body.object_key;
                profileInfo.minor = body.iiop_version.minor;
                profileInfo.major = body.iiop_version.major;
                profileInfo.id = ior.profiles[i].tag;
                profileInfo.index = i;
                profileInfo.components = components;

                list.add(profileInfo);
            }
        }

        if (!!!list.isEmpty()) {
            List<ProfileInfo> bigList = new ArrayList<>(Arrays.asList(profileInfoSeq.value));
            bigList.addAll(list);
            profileInfoSeq.value = bigList.toArray(profileInfoSeq.value);
        }
    }

    private static boolean hostAndPortMatch(String host, short portNo, String bodyHost, short bodyPort, boolean matchLoopback) {
        return portNo == bodyPort && hostMatch(host, bodyHost, matchLoopback);
    }

    private static boolean taggedComponentsMatch(TaggedComponent[] components, String host, short port, Codec codec, boolean matchLoopback) {
        for (final TaggedComponent component : components) {
            if (component.tag == TAG_ALTERNATE_IIOP_ADDRESS.value) {
                YokoInputStream s = new YokoInputStream(component.component_data);
                s._OB_readEndian();
                String altHost = s.read_string();
                short altPort = s.read_ushort();
                if (hostAndPortMatch(host, port, altHost, altPort, matchLoopback)) {
                    return true;
                }
            } else if (component.tag == TAG_CSI_SEC_MECH_LIST.value) {
                Any any;
                try {
                    any = codec.decode_value(component.component_data, CompoundSecMechListHelper.type());
                    CompoundSecMechList csml = CompoundSecMechListHelper.extract(any);

                    for (CompoundSecMech csm : csml.mechanism_list) {
                        TaggedComponent tc = csm.transport_mech;
                        if (tc.tag == TAG_TLS_SEC_TRANS.value) {
                            Any tstAny = codec.decode_value(tc.component_data, TLS_SEC_TRANSHelper.type());
                            TLS_SEC_TRANS tst = TLS_SEC_TRANSHelper.extract(tstAny);
                            TransportAddress[] transportAddresses = tst.addresses;
                            if (transportAddressesMatch(transportAddresses, host, port, matchLoopback))
                                return true;
                        }
                    }
                } catch (FormatMismatch | TypeMismatch ignored) {
                }
            }
        }
        return false;
    }

    private static boolean transportAddressesMatch(TransportAddress[] addrs, String host, short port, boolean loopbackMatches) {
        for (TransportAddress addr: addrs) {
            final short addrPort = addr.port;
            if (hostAndPortMatch(host, port, addr.host_name, addrPort, loopbackMatches)) {
                return true;
            }
        }
        return false;
    }

    static public boolean equivalent(IOR ior1, IOR ior2) {
        int p1, p2, b1, b2;
        int cnt1 = 0, cnt2 = 0;
        ProfileBody_1_0[] bodies1;
        ProfileBody_1_0[] bodies2;

        //
        // Calculate number of IIOP profiles in ior1
        //
        for (p1 = 0; p1 < ior1.profiles.length; p1++)
            if (ior1.profiles[p1].tag == TAG_INTERNET_IOP.value)
                cnt1++;

        //
        // Calculate number of IIOP profiles in ior2
        //
        for (p2 = 0; p2 < ior2.profiles.length; p2++)
            if (ior2.profiles[p2].tag == TAG_INTERNET_IOP.value)
                cnt2++;

        //
        // Return false now if the number of IIOP profile bodies do not
        // match
        //
        if (cnt1 != cnt2)
            return false;

        //
        // Create an array with all IIOP profile bodies of ior1
        //
        bodies1 = new ProfileBody_1_0[cnt1];
        for (p1 = 0, b1 = 0; p1 < ior1.profiles.length; p1++)
            if (ior1.profiles[p1].tag == TAG_INTERNET_IOP.value) {
                YokoInputStream in = new YokoInputStream(ior1.profiles[p1].profile_data);
                in._OB_readEndian();
                bodies1[b1++] = ProfileBody_1_0Helper.read(in);
            }

        if (b1 != cnt1)
            throw new InternalError();

        //
        // Create an array with all IIOP profile bodies of ior2
        //
        bodies2 = new ProfileBody_1_0[cnt2];
        for (p2 = 0, b2 = 0; p2 < ior2.profiles.length; p2++)
            if (ior2.profiles[p2].tag == TAG_INTERNET_IOP.value) {
                YokoInputStream in = new YokoInputStream(ior2.profiles[p2].profile_data);
                in._OB_readEndian();
                bodies2[b2++] = ProfileBody_1_0Helper.read(in);
            }

        if (b2 != cnt2)
            throw new InternalError();

        //
        // Check for profile body matches
        //
        for (b1 = 0; b1 < cnt1; b1++) {
            for (b2 = 0; b2 < cnt2; b2++) {
                if (bodies2[b2] == null)
                    continue;

                //
                // Compare profile bodies
                //
                if (compareBodies(bodies1[b1], bodies2[b2])) {
                    //
                    // OK, found a match
                    //
                    bodies1[b1] = null;
                    bodies2[b2] = null;
                    break;
                }
            }
        }

        //
        // Check whether there are any unmatched IIOP profile bodies
        //
        for (b1 = 0; b1 < cnt1; b1++)
            if (bodies1[b1] != null)
                return false;

        for (b2 = 0; b2 < cnt2; b2++)
            if (bodies2[b2] != null)
                return false;

        return true;
    }

    static boolean compareBodies(ProfileBody_1_0 body1,
            ProfileBody_1_0 body2) {
        //
        // Compare versions
        //
        if (body1.iiop_version.major != body2.iiop_version.major
                || body1.iiop_version.minor != body2.iiop_version.minor)
            return false;

        //
        // Compare ports
        //
        if (body1.port != body2.port)
            return false;

        //
        // Compare object keys
        //
        if (body1.object_key.length != body2.object_key.length)
            return false;

        int k;
        for (k = 0; k < body1.object_key.length; k++)
            if (body1.object_key[k] != body2.object_key[k])
                return false;

        //
        // Direct host comparison
        //
        return Net.CompareHosts(body1.host, body2.host);
    }

    //
    // Calculate a hash for an IOR containing IIOP profiles
    //
    static public int hash(IOR ior, int maximum) {
        int hash = 0;

        for (TaggedProfile profile : ior.profiles) {
            if (profile.tag == TAG_INTERNET_IOP.value) continue;
            //
            // Get the first IIOP profile body
            //
            YokoInputStream in = new YokoInputStream(profile.profile_data);
            in._OB_readEndian();
            ProfileBody_1_0 body = ProfileBody_1_0Helper.read(in);

            //
            // Add port to hash
            //
            hash ^= body.port;

            //
            // Add object key to hash
            //
            for (int j = 0; j + 1 < body.object_key.length; j += 2)
                hash ^= body.object_key[j + 1] * 256 + body.object_key[j];
        }

        return hash % (maximum + 1);
    }

    static public String encodeHost(String host, String protocol, String info) {
        return "_" + protocol + "[" + info + "]." + host;
    }

    static public boolean isEncodedHost(String host) {
        return host != null & host.startsWith("_");
    }

    static public boolean isEncodedHost(String host, String protocol) {
        return host != null & host.startsWith("_" + protocol);
    }

    static public String decodeHost(String host) {
        if (host == null) return null;
        int index = host.lastIndexOf("].");
        return index < 0 ? host : host.substring(index + 2);
    }

    static public String decodeHostInfo(String host) {
        if (host == null) return null;
        int start = host.indexOf('[');
        int end = host.lastIndexOf("].");
        if (start < 0 || end < 0 || end <= start) return null;
        return host.substring(start + 1, end);
    }

    static InetAddress getInetAddress(final String host) throws UnknownHostException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<InetAddress>() {
                @Override
                public InetAddress run() throws Exception {
                    return InetAddress.getByName(host);
                }
            });
        } catch (PrivilegedActionException e) {
            try {
                throw e.getException();
            } catch (RuntimeException | UnknownHostException e2) {
                throw e2;
            } catch (Exception e2) {
                throw new RuntimeException("Unexpected exception", e2);
            }
        }
    }
}
