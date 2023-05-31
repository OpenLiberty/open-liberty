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
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.Stub;

import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;
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
        OutputStream os = orb.create_output_stream();
        os.write_Object(ejb);
        InputStream is = os.create_input_stream();
        IOR ior = IORHelper.read(is);
        return ior.profiles.length;
    }
}