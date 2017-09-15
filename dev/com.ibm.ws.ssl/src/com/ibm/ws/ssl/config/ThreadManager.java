/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ssl.config;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;

/**
 * SSL config can be set in threadlocal. This manager controls that logic.
 * <p>
 * Thread manager class manages the SSL properties set on the thread. This
 * ThreadLocal storage does not automatically get cleared. It's the
 * responsibility of the API caller that set the properties to clear the
 * properties themselves.
 * </p>
 * 
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class ThreadManager {
    private static final TraceComponent tc = Tr.register(ThreadManager.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    private static ThreadManager thisClass = null;
    // for clients or servers
    private static ThreadLocal<ThreadContext> threadLocStorage = new ThreadLocal<ThreadContext>();
    // for clients (when configured)
    private static InheritableThreadLocal<ThreadContext> inheritableThreadLocStorage = new InheritableThreadLocal<ThreadContext>();

    private static boolean useInheritableThreadLocal = false;

    private ThreadManager() {
        // do nothing
    }

    /**
     * Access the singleton instance of this class.
     * 
     * @return ThreadManager
     */
    public static ThreadManager getInstance() {
        if (thisClass == null) {
            thisClass = new ThreadManager();

            String useInheritableThreadLocalString = SSLConfigManager.getInstance().getGlobalProperty(Constants.SSLPROP_USE_INHERITABLE_THREAD_LOCAL);
            if (useInheritableThreadLocalString != null && (useInheritableThreadLocalString.equalsIgnoreCase("true") || useInheritableThreadLocalString.equalsIgnoreCase("yes"))) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "useInheritableThreadLocal is enabled.");
                useInheritableThreadLocal = true;
            }
        }

        return thisClass;
    }

    /**
     * Access the current thread context.
     * 
     * @return ThreadContext
     */
    public ThreadContext getThreadContext() {
        if (useInheritableThreadLocal && !SSLConfigManager.getInstance().isServerProcess()) {
            ThreadContext context = inheritableThreadLocStorage.get();

            if (context == null) {
                context = new ThreadContext();
                inheritableThreadLocStorage.set(context);
            }
            return context;
        }
        ThreadContext context = threadLocStorage.get();

        if (context == null) {
            context = new ThreadContext();
            threadLocStorage.set(context);
        }

        return context;
    }

    /**
     * Access the current properties for this context.
     * 
     * @return Properties - null if not set
     */
    public Properties getPropertiesOnThread() {
        return getThreadContext().getProperties();
    }

    /**
     * Set the properties for this thread context.
     * 
     * @param sslProps
     */
    public void setPropertiesOnThread(Properties sslProps) {
        getThreadContext().setProperties(sslProps);
    }

    /**
     * Query whether the signer flag is set on this context.
     * 
     * @return boolean
     */
    public boolean getSetSignerOnThread() {
        return getThreadContext().getSetSignerOnThread();
    }

    /**
     * Set the signer flag on this context to the input value.
     * 
     * @param flag
     */
    public void setSetSignerOnThread(boolean flag) {
        getThreadContext().setSetSignerOnThread(flag);
    }

    /**
     * Query whether the autoaccept bootstrap signer flag is set on this context.
     * 
     * @return boolean
     */
    public boolean getAutoAcceptBootstrapSigner() {
        return getThreadContext().getAutoAcceptBootstrapSigner();
    }

    /**
     * Set the autoaccept bootstrap signer flag on this context to the input
     * value.
     * 
     * @param flag
     */
    public void setAutoAcceptBootstrapSigner(boolean flag) {
        getThreadContext().setAutoAcceptBootstrapSigner(flag);
    }

    /**
     * Query whether the autoaccept bootstrap signer without storage flag is set
     * on this context.
     * 
     * @return boolean
     */
    public boolean getAutoAcceptBootstrapSignerWithoutStorage() {
        return getThreadContext().getAutoAcceptBootstrapSignerWithoutStorage();
    }

    /**
     * Set the autoaccept bootstrap signer without storage flag to the input
     * value.
     * 
     * @param flag
     */
    public void setAutoAcceptBootstrapSignerWithoutStorage(boolean flag) {
        getThreadContext().setAutoAcceptBootstrapSignerWithoutStorage(flag);
    }

    /**
     * Query the signer chain set on this context.
     * 
     * @return X509Certificate[] - null if not set
     */
    public X509Certificate[] getSignerChain() {
        return getThreadContext().getSignerChain();
    }

    /**
     * Set the signer chain on this context to the input value.
     * 
     * @param signerChain
     */
    public void setSignerChain(X509Certificate[] signerChain) {
        getThreadContext().setSignerChain(signerChain);
    }

    /**
     * Access the inbound connection info object for this context.
     * 
     * @return Map<String,Object> - null if not set
     */
    public Map<String, Object> getInboundConnectionInfo() {
        return getThreadContext().getInboundConnectionInfo();
    }

    /**
     * Set the inbound connection info on this context to the input value.
     * 
     * @param connectionInfo
     */
    public void setInboundConnectionInfo(Map<String, Object> connectionInfo) {
        getThreadContext().setInboundConnectionInfo(connectionInfo);
    }

    /**
     * Query the outbound connection info map of this context.
     * 
     * @return Map<String,Object> - null if not set
     */
    public Map<String, Object> getOutboundConnectionInfo() {
        return getThreadContext().getOutboundConnectionInfo();
    }

    /**
     * Set the outbound connection info of this context to the input value.
     * 
     * @param connectionInfo
     */
    public void setOutboundConnectionInfo(Map<String, Object> connectionInfo) {
        getThreadContext().setOutboundConnectionInfo(connectionInfo);
    }

    /**
     * Get the internal outbound connection info object for this context.
     * 
     * @return Map<String,Object> - null if not set
     */
    public Map<String, Object> getOutboundConnectionInfoInternal() {
        return getThreadContext().getOutboundConnectionInfoInternal();
    }

    /**
     * Set the internal outbound connection info object for this context.
     * 
     * @param connectionInfo
     */
    public void setOutboundConnectionInfoInternal(Map<String, Object> connectionInfo) {
        getThreadContext().setOutboundConnectionInfoInternal(connectionInfo);
    }
}
