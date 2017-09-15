/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.internal;

import java.security.AccessController;

import javax.rmi.CORBA.Stub;
import javax.rmi.CORBA.Util;

import org.apache.yoko.rmi.impl.PortableRemoteObjectImpl;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.portable.IDLEntity;
import org.omg.CORBA.portable.ObjectImpl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.util.ThreadContextAccessor;

public class WSPortableRemoteObjectImpl extends PortableRemoteObjectImpl {
    private static final TraceComponent tc = Tr.register(WSPortableRemoteObjectImpl.class);
    private static ThreadContextAccessor threadContextAccessor = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    /**
     * An implementation of narrow that always attempts to load stub classes
     * from the class loader before dynamically generating a stub class.
     */
    @Override
    @FFDCIgnore(BAD_OPERATION.class)
    public Object narrow(Object narrowFrom, @SuppressWarnings("rawtypes") Class narrowTo) throws ClassCastException {
        if (narrowFrom == null) {
            return null;
        }

        if (narrowTo.isInstance(narrowFrom)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "object is already an instance");
            return narrowFrom;
        }

        if (!!!(narrowFrom instanceof ObjectImpl))
            throw new ClassCastException(narrowTo.getName());

        if (IDLEntity.class.isAssignableFrom(narrowTo))
            return super.narrow(narrowFrom, narrowTo);

        Class<?> stubClass = loadStubClass(narrowTo);
        if (stubClass == null) {
            // The stub class could not be loaded.  Fallback to the default
            // implementation to allow it to dynamically generate one.
            return super.narrow(narrowFrom, narrowTo);
        }

        Object stubObject;
        try {
            stubObject = stubClass.newInstance();
        } catch (Throwable t) {
            // This will fail if the stub class is "invalid" (missing default
            // constructor, non-public default constructor, logic in the
            // constructor that throws, etc.).
            ClassCastException e = new ClassCastException(narrowTo.getName());
            e.initCause(t);
            throw e;
        }

        // This will fail if the loaded class does not actually extend Stub.
        Stub stub = (Stub) stubObject;

        try {
            stub._set_delegate(((ObjectImpl) narrowFrom)._get_delegate());
        } catch (org.omg.CORBA.BAD_OPERATION e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "unable to copy delegate", e);
        }

        // This will fail if the loaded stub class does not implement the
        // interface. For example, narrow was called with one version of the
        // interface, but the context class loader loaded a stub that
        // implemented a different version of the interface.
        return narrowTo.cast(stub);
    }

    @FFDCIgnore(ClassNotFoundException.class)
    private static Class<?> loadStubClass(Class<?> type) {
        String name = type.getName();
        int index = name.lastIndexOf('.');
        String stubName = index == -1 ?
                        '_' + name + "_Stub" :
                        name.substring(0, index + 1) + '_' + name.substring(index + 1) + "_Stub";

        ClassLoader loader = threadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());

        // ??? Yoko Util.loadClass returns null rather than throwing
        // ClassNotFoundException as per javadoc.
        Class<?> stubClass;
        try {
            stubClass = Util.loadClass(stubName, null, loader);
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "unable to load class " + stubName, e);
            stubClass = null;
        }

        if (stubClass == null) {
            String altStubName = "org.omg.stub." + stubName;
            try {
                stubClass = Util.loadClass(altStubName, null, loader);
            } catch (ClassNotFoundException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "unable to load class " + altStubName, e);
            }
        }

        return stubClass;
    }
}
