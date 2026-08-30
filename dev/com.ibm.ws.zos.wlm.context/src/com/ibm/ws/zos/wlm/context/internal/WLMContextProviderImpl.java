/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.context.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.ws.zos.core.utils.NativeUtils;
import com.ibm.ws.zos.wlm.EnclaveManager;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;

/**
 * The z/OS WLM Context Provider
 */
public class WLMContextProviderImpl implements ThreadContextProvider {
    /**
     * The key for the policy that tells us how to propagate
     */
    final static String PROPAGATION_POLICY = "wlm";

    /**
     * The key for the default transaction class
     */
    final static String DEFAULT_TRANSACTION_CLASS = "defaultTransactionClass";

    /**
     * The key for the daemon transaction class
     */
    final static String DAEMON_TRANSACTION_CLASS = "daemonTransactionClass";

    /**
     * PropagateOrNew says I want an enclave on the asynch work. I'd prefer it to be the
     * same enclave I'm using now, but if I'm not running under an enclave, then
     * please create one using the default Transaction Class from the configuration.
     */
    final static String POLICY_PROPAGATE_OR_NEW = "PROPAGATEORNEW";

    /**
     * New says I want a new enclave created using the default Transaction Class
     * from the configuration.
     */
    final static String POLICY_NEW = "NEW";

    /**
     * Propagate says I want the asynch work to run just like I'm running now. If I have
     * an enclave, then run the asynch work under the same enclave. If I don't have an
     * enclave, then don't run the asynch work under an enclave.
     */
    final static String POLICY_PROPAGATE = "PROPAGATE";

    /**
     * Our reference to the enclave manager
     */
    private EnclaveManager enclaveManager = null;

    /**
     * Jakarta EE version if Jakarta EE 9 or higher. If 0, assume a lesser EE spec version.
     */
    volatile int eeVersion;

    /**
     * Tracks the most recently bound EE version service reference. Only use this within the set/unsetEEVersion methods.
     */
    private ServiceReference<JavaEEVersion> eeVersionRef;

    /**
     * Native Utility object reference.
     */
    private NativeUtils nativeUtils = null;

    /**
     * Constructor
     */
    public WLMContextProviderImpl() {
    }

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param properties : Map containing service & config properties
     *                       populated/provided by config admin
     */
    protected void activate(Map<String, Object> properties) {
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     */
    protected void deactivate() {
    }

    /**
     * Remember the enclave manager
     *
     * @param em the enclave manager
     */
    protected void setWlmEnclaveManager(EnclaveManager em) {
        enclaveManager = em;
    }

    /**
     * Forget the enclave manager (if we had remembered it)
     *
     * @param em The enclave manager to forget
     */
    protected void unsetWlmEnclaveManager(EnclaveManager em) {
        if (enclaveManager == em) {
            enclaveManager = null;
        }
    }

    /**
     * Remember the enclave manager
     *
     * @param em the enclave manager
     */
    protected void setNativeUtils(NativeUtils nativeUtils) {
        this.nativeUtils = nativeUtils;
    }

    /**
     * Forget the enclave manager (if we had remembered it)
     *
     * @param em The enclave manager to forget
     */
    protected void unsetNativeUtils(NativeUtils nativeUtils) {
        if (this.nativeUtils == nativeUtils) {
            this.nativeUtils = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig) {
        // Create a context from the current thread using our config
        String propagationPolicy = ((String) threadContextConfig.get(PROPAGATION_POLICY)).toUpperCase();
        String defaultTransactionClass = (String) threadContextConfig.get(DEFAULT_TRANSACTION_CLASS);
        String daemonTransactionClass = (String) threadContextConfig.get(DAEMON_TRANSACTION_CLASS);
        return new WLMContextImpl(this, execProps, propagationPolicy, defaultTransactionClass, daemonTransactionClass);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext createDefaultThreadContext(Map<String, String> execProps) {
        String propagationPolicy = POLICY_NEW;
        String defaultTransactionClass = "ASYNCBN";
        String daemonTransactionClass = "ASYNCDMN";
        return new WLMContextImpl(this, Collections.<String, String> emptyMap(), propagationPolicy, defaultTransactionClass, daemonTransactionClass);
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        WLMContextImpl context;
        try {
            context = (WLMContextImpl) in.readObject();
            // Set the transient reference to this object so the context can find the EnclaveManager
            context.setContextProviderImpl(this);
        } catch (ObjectStreamException ose) {
            // Deserialize failed, so create an empty context
            context = (WLMContextImpl) createDefaultThreadContext(Collections.<String, String> emptyMap());
        } finally {
            in.close();
        }
        return context;
    }

    /**
     * Get the enclave manager
     *
     * @return The enclave manager
     */
    public EnclaveManager enclaveManager() {
        return enclaveManager;
    }

    /**
     * Get the native utility object reference.
     *
     * @return The native utility object reference.
     */
    public NativeUtils getNativeUtils() {
        return nativeUtils;
    }

    /**
     * @see com.ibm.wsspi.threadcontext.ThreadContextProvider#getPrerequisites()
     */
    @Override
    public List<ThreadContextProvider> getPrerequisites() {
        return null;
    }

    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        if (version == null) {
            eeVersion = 0;
        } else {
            int dot = version.indexOf('.');
            String major = dot > 0 ? version.substring(0, dot) : version;
            eeVersion = Integer.parseInt(major);
        }
        eeVersionRef = ref;
    }

    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        if (eeVersionRef == ref) {
            eeVersionRef = null;
            eeVersion = 0;
        }
    }
}
