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

import org.apache.yoko.rmi.impl.UtilImpl;
import org.omg.CORBA.portable.OutputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.util.ThreadContextAccessor;

public class WSUtilImpl extends UtilImpl {
    private static final TraceComponent tc = Tr.register(WSUtilImpl.class);
    private static final ThreadContextAccessor threadContextAccessor = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    // FIXME: remove after fixing 152839
    @Override
    @SuppressWarnings("rawtypes")
    @FFDCIgnore(ClassNotFoundException.class)
    public Class loadClass(String name, String codebase, ClassLoader loader) throws ClassNotFoundException {
        try {
            return super.loadClass(name, codebase, loader);
        } catch (ClassNotFoundException e) {
            // Fix #2: use context class loader to allow _Intf_Stub to be
            // loaded from a different loader than Intf (or to be dynamically
            // generated if needed).
            ClassLoader contextClassLoader = threadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());
            if (contextClassLoader != loader && name.endsWith("_Stub")) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "retrying with " + contextClassLoader);
                try {
                    return contextClassLoader.loadClass(name);
                } catch (ClassNotFoundException e2) {
                }
            }
            throw e;
        }
    }

    /**
     * Overridden to allow objects to be replaced prior to being written.
     */
    @Override
    public void writeRemoteObject(OutputStream out, @Sensitive Object obj) throws org.omg.CORBA.SystemException {
        WSUtilService service = WSUtilService.getInstance();
        if (service != null) {
            obj = service.replaceObject(obj);
        }
        super.writeRemoteObject(out, obj);
    }

    /**
     * Overridden to disable use of codebase, as it is arguably a security exposure.
     */
    @Override
    public String getCodebase(@SuppressWarnings("rawtypes") Class arg0) {
        return null;
    }
}
