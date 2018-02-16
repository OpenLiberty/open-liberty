/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threadContext;

//import com.ibm.ejs.csi.DefaultComponentMetaData; //122727
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.runtime.metadata.ComponentMetaData;

/**
 * 
 * <p>Accessor for ComponentMetaData. It provides static methods for easy access.
 * It is a singleton.
 * 
 * @ibm-private-in-use
 */
public final class ComponentMetaDataAccessorImpl {
    private static final TraceComponent tc =
                    Tr.register(ComponentMetaDataAccessorImpl.class,
                                "Runtime", "com.ibm.ws.runtime.runtime"); // d143991

    private static ComponentMetaDataAccessorImpl cmdai =
                    new ComponentMetaDataAccessorImpl();

    private ThreadContext<ComponentMetaData> threadContext = null;

    // this default CMD is used in cases like the client container where there is only one possible
    // active component - so that container is responsible for setting the defaultCMD via the
    // DefaultCMD service.
    private ComponentMetaData defaultCMD;

    private ComponentMetaDataAccessorImpl() {
        //        threadContext = new ThreadContextImpl<ComponentMetaData>(DefaultComponentMetaData.getInstance()); //122727
        threadContext = new ThreadContextImpl<ComponentMetaData>();
    }

    /**
     * 
     * @return ComponentMetaDataAccessorImpl
     */
    public static ComponentMetaDataAccessorImpl getComponentMetaDataAccessor() {
        return cmdai;
    }

    /**
     * @return ComponentMetaData
     */
    public ComponentMetaData getComponentMetaData() {
        ComponentMetaData cmd = threadContext.getContext();
        return cmd == null ? defaultCMD : cmd;
    }

    /**
     * @return ComponentMetaData which matches a passed in class
     *         if there is no match, return null.
     */
    public ComponentMetaData getComponentMetaData(Class clz) {
        return threadContext.peekContext(clz);
    }

    /**
     * @return ThreadContext
     * @deprecated use beginContext and endContext methods provided by ComponentMetaDataImpl
     */
    @Deprecated
    public ThreadContext<ComponentMetaData> getThreadContext() {
        return threadContext;
    }

    /**
     * Begin the context for the ComponentMetaData provided.
     * 
     * @param ComponentMetaData It Must not be null. Tr.error will be logged if it is null.
     * @return Previous Object, which was on the stack. It can be null.
     */
    public Object beginContext(ComponentMetaData cmd) { // modified to return object d131914
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (cmd != null)
                Tr.debug(tc, "begin context " + cmd.getJ2EEName());
            else
                Tr.debug(tc, "NULL was passed.");
        }

        if (cmd == null) {
            Tr.error(tc, "WSVR0603E"); // d143991
            throw new IllegalArgumentException(Tr.formatMessage(tc, "WSVR0603E"));
        }

        return threadContext.beginContext(cmd); //131914
    }

    /**
     * Establish default context for when there should be no ComponentMetaData on the thread.
     * 
     * @return Previous component metadata which was on the thread. It can be null.
     */
    public ComponentMetaData beginDefaultContext() {
        return threadContext.beginContext(null);
    }

    /**
     * End the context for the current ComponentMetaData
     * 
     * @return Object which was removed (pop) from the stack.
     */
    public Object endContext() { // modified to return object d131914
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            ComponentMetaData cmd = getComponentMetaData();
            Tr.debug(tc, "end context " + (cmd == null ? null : cmd.getJ2EEName()));
        }
        return threadContext.endContext(); //d131914
    }

    /**
     * @return The index of ComponentMetaData
     */
    public int getComponentMetaDataIndex() {
        return threadContext.getContextIndex();
    }

    public void setDefaultCMD(ComponentMetaData defaultCMD) {
        if (isClient()) {
            this.defaultCMD = defaultCMD;
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setDefaultCMD called in non-client process - ignoring " + defaultCMD, new Throwable("StackTrace"));
        }

    }

    private boolean isClient() {
        Bundle b = FrameworkUtil.getBundle(ComponentMetaDataAccessorImpl.class);
        BundleContext bc = b == null ? null : b.getBundleContext();
        ServiceReference<LibertyProcess> sr = bc == null ? null : bc.getServiceReference(LibertyProcess.class);
        return BootstrapConstants.LOC_PROCESS_TYPE_CLIENT.equals((sr == null ? null : sr.getProperty(BootstrapConstants.LOC_PROPERTY_PROCESS_TYPE)));
    }
} //ComponentMetaDataAccessorImpl end
