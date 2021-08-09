/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.remote.common.internal;

import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.rmi.PortableRemoteObject;

import org.omg.CORBA.ORB;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.clientcontainer.remote.common.ClientSupport;
import com.ibm.ws.clientcontainer.remote.common.ClientSupportFactory;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.transport.iiop.spi.ClientORBRef;

/**
 * Provides access to the ClientSupport remote interface of a singleton class bound in CosNaming
 * to allow remote clients (the client container in particular) to access objects bound in the
 * server's namespace.
 */
@Component(service = ClientSupportFactory.class)
public class ClientSupportFactoryImpl implements ClientSupportFactory, Runnable {
    private static final TraceComponent tc = Tr.register(ClientSupportFactoryImpl.class);
    private static final String nsURL = "corbaname:rir:#" + ClientSupport.SERVICE_NAME;

    private ClientORBRef clientORBRef;
    private ClientSupport clientSupport;
    private ScheduledExecutorService executorService;

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=client)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    @Reference
    protected void setClientORBRef(ClientORBRef clientORBRef) {
        this.clientORBRef = clientORBRef;
    }

    protected void unsetClientORBRef(ClientORBRef clientORB) {
        this.clientORBRef = null;
    }

    @Reference
    protected void setExecutorService(ScheduledExecutorService executor) {
        executorService = executor;
    }

    protected void unsetExecutorService(ScheduledExecutorService executor) {
        executorService = null;
    }

    @Override
    public synchronized ClientSupport getRemoteClientSupport() throws RemoteException {
        if (clientSupport == null) {
            try {
                clientSupport = obtainReferenceFromORB();

                // If successfully obtained, schedule to clear it out after 10 seconds,
                // so it will be acquired again if the server restarts.
                executorService.schedule(this, 10, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new RemoteException("Unable to obtain " + nsURL + " from corbaloc NameService", ex);
            }
        }
        return clientSupport;
    }

    private ClientSupport obtainReferenceFromORB() throws Exception {
        // This code looks up the ClientSupportImpl bound under the NameService in
        // the server's CosNaming namespace.  The connection URL should be specified
        // in the client.xml (or included config) and should look like this (which is
        // the default configuration:
        //    <iiopClient nameService="corbaloc::localhost:2809/NameService" />
        ORB orb = clientORBRef.getORB();
        Object obj = orb.string_to_object(nsURL);

        ClassLoader oldTCCL = AccessController.doPrivileged(new GetCL("tccl"));
        try {
            AccessController.doPrivileged(new SetTCCL(this.getClass().getClassLoader()));
            return (ClientSupport) PortableRemoteObject.narrow(obj, ClientSupport.class);
        } finally {
            AccessController.doPrivileged(new SetTCCL(oldTCCL));
        }
    }

    private static class GetCL implements PrivilegedAction<ClassLoader> {
        String whichLoader;

        GetCL(String which) {
            whichLoader = which;
        }

        @Override
        public ClassLoader run() {
            if ("this".equals(whichLoader)) {
                return ClientSupportFactoryImpl.class.getClassLoader();
            }
            return Thread.currentThread().getContextClassLoader();
        }
    }

    private static class SetTCCL implements PrivilegedAction<Object> {
        private final ClassLoader classLoader;

        SetTCCL(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public Object run() {
            Thread.currentThread().setContextClassLoader(classLoader);
            return null;
        }
    }

    /**
     * Scheduled Runnable implementation to reset the ClientSupport
     * so that it will be obtained again later, in case the server
     * is restarted.
     */
    @Trivial
    @Override
    public synchronized void run() {
        clientSupport = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "run : cached ClientSupport reference cleared");
        }
    }
}
