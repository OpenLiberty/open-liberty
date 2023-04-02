/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.internal;

import java.io.IOException;

import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.Stub;

import org.omg.CORBA.ORB;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.SerializedStub;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.serialization.DeserializationObjectResolver;
import com.ibm.ws.serialization.SerializationObjectReplacer;
import com.ibm.ws.transport.iiop.spi.ClientORBRef;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component
public class StubObjectResolver implements SerializationObjectReplacer, DeserializationObjectResolver {
    private static final String REFERENCE_ORB = "orb";

    private final AtomicServiceReference<ClientORBRef> orbRef = new AtomicServiceReference<ClientORBRef>(REFERENCE_ORB);

    @Activate
    protected void activate(ComponentContext cc) {
        orbRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        orbRef.deactivate(cc);
    }

    @Reference(name = REFERENCE_ORB, service = ClientORBRef.class, target = "(id=defaultOrb)")
    protected void addORBRef(ServiceReference<ClientORBRef> ref) {
        orbRef.setReference(ref);
    }

    @Override
    public Object replaceObject(@Sensitive Object object) {
        // -----------------------------------------------------------------------
        // When rmic compatibility is enabled, stubs for interfaces that are not
        // RMI/IDL abstract interfaces will extend SerializableStub, which has a
        // writeReplace method to substitute an instance of SerializedStub.
        // Unfortunately, SerializedStub holds a reference to the original stub
        // which serialization will replace with a reference to the same
        // SerializdStub returned by writeReplace causing a circular reference
        // that cannot be deserialized.
        //
        // To avoid this, the SerializedStub is replaced here with a very similar
        // SerializedStubString, using the ORB to convert the Stub to a
        // String that may be serialized, and then the ORB is used again during
        // resolveObject to convert the String back to the original Stub.
        // -----------------------------------------------------------------------
        if (object instanceof SerializedStub) {
            ORB orb = orbRef.getServiceWithException().getORB();
            SerializedStub serializedStub = (SerializedStub) object;
            String stub_string = orb.object_to_string((org.omg.CORBA.Object) serializedStub.getStub());
            return new SerializedStubString(stub_string, serializedStub.getInterfaceClass());
        }

        return null;
    }

    @Override
    public Object resolveObject(Object object) throws IOException {
        if (object instanceof Stub) {
            ((Stub) object).connect(orbRef.getServiceWithException().getORB());
            return object;
        }

        if (object instanceof SerializedStubString) {
            ORB orb = orbRef.getServiceWithException().getORB();
            SerializedStubString serializedStub = (SerializedStubString) object;
            Object stub = orb.string_to_object(serializedStub.getStub());
            stub = PortableRemoteObject.narrow(stub, serializedStub.getInterface());
            return stub;
        }

        return null;
    }
}
