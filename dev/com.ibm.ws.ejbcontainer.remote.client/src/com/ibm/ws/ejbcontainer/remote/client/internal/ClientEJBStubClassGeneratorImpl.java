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
package com.ibm.ws.ejbcontainer.remote.client.internal;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.ClassGenerator;
import com.ibm.ws.clientcontainer.remote.common.ClientSupportFactory;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ejbcontainer.jitdeploy.JIT_Stub;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.wsspi.ejbcontainer.JITDeploy;

@Component(service = { ApplicationStateListener.class, ClassGenerator.class })
public class ClientEJBStubClassGeneratorImpl implements ClassGenerator, ApplicationStateListener {
    private static final TraceComponent tc = Tr.register(ClientEJBStubClassGeneratorImpl.class);
    private static final String ORG_OMG_STUB_PREFIX = "org.omg.stub.";
    private static final int RMIC_COMPATIBLE_ALL = -1;

    /**
     * Client application name.
     */
    private String appName;

    /**
     * Indication that an attempt should be made to connect to the server process through
     * the ClientSupport service to obtain the set of RMIC compatible classes.
     */
    private boolean attemptConnectionToServer = true;

    /**
     * Set of classes that should be generated with maximum RMIC compatibility.
     * This is used to simulate traditional WAS, which would normally run rmic.
     */
    private Set<Class<?>> rmicCompatibleClasses;

    /**
     * ClientSupportFactory provides a mechanism to connect to a server process
     * and obtain the set of RMIC compatible classes for the application.
     */
    private ClientSupportFactory clientSupportFactory;

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=client)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    @Reference
    protected void setClientSupportFactory(ClientSupportFactory clientSupportFactory) {
        this.clientSupportFactory = clientSupportFactory;
    }

    protected void unsetClientSupportFactory(ClientSupportFactory clientSupportFactory) {
        this.clientSupportFactory = null;
    }

    @Override
    @Trivial
    @FFDCIgnore(ClassNotFoundException.class)
    public byte[] generateClass(String name, ClassLoader loader) throws ClassNotFoundException {
        if (!name.startsWith(ORG_OMG_STUB_PREFIX)) {
            String remoteInterfaceName = JIT_Stub.getRemoteInterfaceName(name);
            if (remoteInterfaceName != null) {
                try {
                    // The ORB will try without the org.omg.stub prefix first,
                    // so we don't want to generate a stub if a stub with the
                    // org.omg.stub prefix would be subsequently found.
                    //
                    // Alternatively, we could generate org.omg.stub classes,
                    // but traditional WAS does it this way, so it's less confusing if we
                    // generate stubs the same way.
                    loader.loadClass(ORG_OMG_STUB_PREFIX + name);
                } catch (ClassNotFoundException e) {
                    // The org.omg.stub class does not exist, so we can generate
                    // the stub without the org.omg.stub prefix.
                    return generateStubClass(remoteInterfaceName, loader);
                }
            }
        }
        return null;
    }

    @FFDCIgnore(ClassNotFoundException.class)
    @Sensitive
    private byte[] generateStubClass(String remoteInterfaceName, ClassLoader loader) {
        try {
            Class<?> remoteInterface = loader.loadClass(remoteInterfaceName);
            int rmicCompatible = isRMICCompatibleClass(remoteInterface, loader) ? RMIC_COMPATIBLE_ALL : JITDeploy.RMICCompatible;
            return JITDeploy.generateStubBytes(remoteInterface, rmicCompatible);
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "unable to load remote interface class: " + e);
        } catch (EJBConfigurationException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "failed to process " + remoteInterfaceName + " as a remote interface", e);
        }
        return null;
    }

    @FFDCIgnore({ ClassNotFoundException.class, RemoteException.class })
    private synchronized boolean isRMICCompatibleClass(Class<?> c, ClassLoader loader) {

        if (attemptConnectionToServer && appName != null) {

            // Only attempt to get the set of RMIC compatible classes one time;
            // if the attempt fails then the first stub generated may already
            // be generated incorrectly. Keep in mind that this method is only
            // called when a _Stub classes is definitely going to be generated.
            attemptConnectionToServer = false;

            try {
                Set<String> classNames = clientSupportFactory.getRemoteClientSupport().getEJBRmicCompatibleClasses(appName);

                rmicCompatibleClasses = new HashSet<Class<?>>(classNames.size());
                for (String className : classNames) {
                    try {
                        rmicCompatibleClasses.add(loader.loadClass(className));
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "isRMICCompatibleClass: added " + className);
                    } catch (ClassNotFoundException ex) {
                        // No requirement for all EJB remote interfaces to be
                        // available to the client.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "isRMICCompatibleClass: not added " + className + ", " + ex);
                    }
                }
                if (rmicCompatibleClasses.size() == 0) {
                    rmicCompatibleClasses = null;
                }
            } catch (RemoteException rex) {
                // Failed to obtain classes from server; this is normal for scenarios
                // where client container is running standalone, but could be problematic
                // if the server just hasn't started yet.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "isRMICCompatibleClass: failed to connect to server:" + rex);
            }
        }
        return rmicCompatibleClasses != null && rmicCompatibleClasses.contains(c);
    }

    @Override
    public synchronized void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        appName = appInfo.getName();
    }

    @Trivial
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {}

    @Trivial
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {}

    @Trivial
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {}
}
