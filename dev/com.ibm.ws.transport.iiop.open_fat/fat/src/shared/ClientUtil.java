/*
 * Copyright (c) 2023 IBM Corporation and others.
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
package shared;

import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.Stub;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CSIIOP.CompoundSecMech;
import org.omg.CSIIOP.CompoundSecMechList;
import org.omg.CSIIOP.CompoundSecMechListHelper;
import org.omg.CSIIOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.CSIIOP.TAG_TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANS;
import org.omg.CSIIOP.TLS_SEC_TRANSHelper;
import org.omg.CSIIOP.TransportAddress;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.IIOP.ProfileBody_1_1;
import org.omg.IIOP.ProfileBody_1_1Helper;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;
import org.omg.IOP.TAG_INTERNET_IOP;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.TaggedProfile;
import org.omg.PortableServer.POA;

public enum ClientUtil {
    ;

    private static final Map<ORB, TestIDLIntf> IDL_REF_CACHE = new WeakHashMap<>();

    public static TestIDLIntf getIDLObjectRef(ORB orb) throws Exception {
        return IDL_REF_CACHE.computeIfAbsent(orb, ClientUtil::createIDLObjectRef);
    }

    private static TestIDLIntf createIDLObjectRef(ORB orb) {
        POA rootPoa;
        try {
            rootPoa = (POA) orb.resolve_initial_references("RootPOA");
            // Bind a sample CORBA object
            TestIDLIntf_impl testIDLIntf_impl = new TestIDLIntf_impl();
            testIDLIntf_impl.s("wibble");
            byte[] id = rootPoa.activate_object(testIDLIntf_impl);
            rootPoa.the_POAManager().activate();
            org.omg.CORBA.Object testIDLIntfRef = rootPoa.create_reference_with_id(id, testIDLIntf_impl._all_interfaces(rootPoa, id)[0]);
            return TestIDLIntfHelper.narrow(testIDLIntfRef);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static TestRemote lookupTestBean(ORB orb) throws Exception {
        return lookup(orb, "ejb/global/test.corba/test.corba.bean/TestBean!shared.TestRemote", TestRemote.class);
    }

    public static Business lookupBusinessBean(ORB orb) throws Exception {
        return lookup(orb, "ejb/global/test.corba/test.corba.bean/BusinessBean!shared.Business", Business.class);
    }

    private static <T extends Remote> T lookup(ORB orb, String s, Class<T> type) throws Exception {
        System.out.println("### Performing naming lookup ###");
        NamingContext nameService = NamingContextHelper.narrow(orb.resolve_initial_references("NameService"));
        NameComponent[] nameComponents = Stream.of(s.split("/"))
                .map(n->new NameComponent(n,""))
                .toArray(NameComponent[]::new);
        Object o = nameService.resolve(nameComponents);
        T bean = type.cast(PortableRemoteObject.narrow(o, type));
        System.out.println("### bean = " + bean + " ###");
        return bean;
    }

    public static int getNumProfiles(Stub ejb, ORB orb) {
        return getIor(ejb, orb).profiles.length;
    }

    private static IOR getIor(Stub ejb, ORB orb) {
        OutputStream os = orb.create_output_stream();
        os.write_Object(ejb);
        InputStream is = os.create_input_stream();
        return IORHelper.read(is);
    }

    private static Codec getCodec(ORB orb) throws Exception {
        CodecFactory factory = CodecFactoryHelper.narrow(orb.resolve_initial_references("CodecFactory"));
        return factory.create_codec(new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 1, (byte) 2));
    }

    /** Decodes the first TAG_INTERNET_IOP profile of the IOR of the given stub. */
    public static ProfileBody_1_1 getIiopProfile(Stub ejb, ORB orb) throws Exception {
        Codec codec = getCodec(orb);
        for (TaggedProfile profile : getIor(ejb, orb).profiles) {
            if (profile.tag == TAG_INTERNET_IOP.value) {
                Any any = codec.decode_value(profile.profile_data, ProfileBody_1_1Helper.type());
                return ProfileBody_1_1Helper.extract(any);
            }
        }
        throw new IllegalStateException("IOR has no TAG_INTERNET_IOP profile");
    }

    /**
     * Returns the ports of every CSIv2 TLS_SEC_TRANS address advertised in the
     * TAG_CSI_SEC_MECH_LIST component of the given IIOP profile.
     */
    public static List<Integer> getCsiv2SslPorts(ProfileBody_1_1 profile, ORB orb) throws Exception {
        Codec codec = getCodec(orb);
        List<Integer> ports = new ArrayList<>();
        for (TaggedComponent component : profile.components) {
            if (component.tag != TAG_CSI_SEC_MECH_LIST.value) {
                continue;
            }
            Any any = codec.decode_value(component.component_data, CompoundSecMechListHelper.type());
            CompoundSecMechList mechList = CompoundSecMechListHelper.extract(any);
            for (CompoundSecMech mech : mechList.mechanism_list) {
                if (mech.transport_mech.tag != TAG_TLS_SEC_TRANS.value) {
                    continue;
                }
                Any tlsAny = codec.decode_value(mech.transport_mech.component_data, TLS_SEC_TRANSHelper.type());
                TLS_SEC_TRANS tls = TLS_SEC_TRANSHelper.extract(tlsAny);
                for (TransportAddress address : tls.addresses) {
                    ports.add(address.port & 0xFFFF);
                }
            }
        }
        return ports;
    }
}