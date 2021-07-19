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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import com.ibm.ws.zos.core.utils.NativeUtils;
import com.ibm.ws.zos.wlm.AlreadyClassifiedException;
import com.ibm.ws.zos.wlm.Enclave;
import com.ibm.ws.zos.wlm.EnclaveManager;
import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * Implements the z/OS WLM Context for propagation purposes
 */
public class WLMContextImpl implements ThreadContext {

    /** Serialization version */
    private static final long serialVersionUID = 4946224900789868451L;

    /**
     * Names of serializable fields.
     * A single character is used for each to reduce the space required.
     */
    private static final String DAEMON_TRAN_CLASS = "d",
                    DEFAULT_TRAN_CLASS = "D",
                    ENCLAVE_TOKEN = "E",
                    IS_LONG_RUNNING = "L",
                    PROPAGATION_POLICY = "P";

    /**
     * Fields to serialize
     */
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {
                                                                                                new ObjectStreamField(DAEMON_TRAN_CLASS, String.class),
                                                                                                new ObjectStreamField(DEFAULT_TRAN_CLASS, String.class),
                                                                                                new ObjectStreamField(ENCLAVE_TOKEN, String.class),
                                                                                                new ObjectStreamField(IS_LONG_RUNNING, boolean.class),
                                                                                                new ObjectStreamField(PROPAGATION_POLICY, String.class)
    };

    /**
     * The transaction class name used to create an enclave for Daemon work
     */
    private String daemonTransactionClass;

    /**
     * The transaction class name used if we need to create an enclave for non-Daemon work
     */
    private String defaultTransactionClass;

    /**
     * The token for an enclave that is part of this context
     */
    private String enclave_token = null;

    /**
     * If there is already an enclave on the thread when we are supposed to put one on the thread we need
     * to remember the pre-existing one so we can put it back later. This is just to remember this between starting and stopping.
     * so it should NEVER be set when we serialize this object, so make the attribute transient so its ignored
     */
    transient private Enclave preExistingEnclave = null;

    /**
     * The rules to follow for enclave propagation for non-Daemon work (values defined in ContextProviderImpl)
     */
    private String propagationPolicy;

    /**
     * A reference to the ContextProvider that will know where the EnclaveManager is. Since the context gets serialized
     * we can't remember ourselves where the EnclaveManager is (or the ContextProvider for that matter). When we
     * get deserialized the getContext(byte []) method in ContextProviderImpl will reset this field so we'll have a current
     * reference. This way if the ContextProvider gets deactivated/reactivated across this Context we'll still be ok.
     */
    transient private WLMContextProviderImpl cpi = null;

    /**
     * Indicates if the contextual task might take a long time to complete.
     */
    transient private boolean isLongRunning = false;

    /**
     * Constructor
     *
     * @param contextProviderImpl     The Context Provider
     * @param execProps               execution properties for the contextual task
     * @param propagationPolicy       The propagation policy to follow (one of a fixed set of strings)
     * @param defaultTransactionClass the default tran class
     * @param daemonTransactionClass  The Daemon tran class
     */
    public WLMContextImpl(WLMContextProviderImpl contextProviderImpl, Map<String, String> execProps,
                          String propagationPolicy, String defaultTransactionClass, String daemonTransactionClass) {
        // Remember our config and context provider
        cpi = contextProviderImpl;
        this.propagationPolicy = propagationPolicy;
        this.defaultTransactionClass = defaultTransactionClass;
        this.daemonTransactionClass = daemonTransactionClass;

        String longRunningProp;
        if (contextProviderImpl.eeVersion < 9) {
            longRunningProp = execProps.get("javax.enterprise.concurrent.LONGRUNNING_HINT"); // Java EE ManagedTask.LONGRUNNING_HINT
            if (longRunningProp == null)
                longRunningProp = execProps.get("jakarta.enterprise.concurrent.LONGRUNNING_HINT"); // Jakarta ManagedTask.LONGRUNNING_HINT
        } else { // prefer Jakarta
            longRunningProp = execProps.get("jakarta.enterprise.concurrent.LONGRUNNING_HINT"); // Jakarta ManagedTask.LONGRUNNING_HINT
            if (longRunningProp == null)
                longRunningProp = execProps.get("javax.enterprise.concurrent.LONGRUNNING_HINT"); // Java EE ManagedTask.LONGRUNNING_HINT
        }
        isLongRunning = Boolean.parseBoolean(longRunningProp);

        // If we're going to try to use the current enclave, remember it, if there is one...
        if ((propagationPolicy.equalsIgnoreCase(WLMContextProviderImpl.POLICY_PROPAGATE)) ||
            (propagationPolicy.equalsIgnoreCase(WLMContextProviderImpl.POLICY_PROPAGATE_OR_NEW))) {
            // Get the Enclave Manager
            EnclaveManager em = cpi.enclaveManager();
            // If there is one
            if (null != em) {
                // get current enclave if possible
                Enclave enclave = em.getCurrentEnclave();
                // If we have an enclave
                enclave_token = (enclave != null) ? em.getStringToken(enclave) : null;
            }
            // Else no enclave manager therefore no enclave...do nothing
        }
    }

    /** {@inheritDoc} */
    @Override
    public ThreadContext clone() {
        try {
            WLMContextImpl copy = (WLMContextImpl) super.clone();
            copy.preExistingEnclave = null;
            return copy;
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * On deserialization we need our context provider reset
     *
     * @param contextProviderImpl the context provider
     */
    public void setContextProviderImpl(WLMContextProviderImpl contextProviderImpl) {
        cpi = contextProviderImpl;
    }

    /** {@inheritDoc} */
    @Override
    public void taskStarting() throws RejectedExecutionException {
        // Get the enclave manager if there is one
        EnclaveManager em = cpi.enclaveManager();

        // Before we do anything else, if there's an enclave already on the thread, get it off and remember it
        if (null != em) {
            preExistingEnclave = em.removeCurrentEnclaveFromThread();
        }

        // Ok, are we doing 'daemon' work?
        if (isLongRunning) {
            handleDaemonWork();
        } else // Not a Daemon, so what did we decide to do up front?  If we remembered the enclave we're supposed to propagate it
        {
            // Did we have an enclave
            if (null != enclave_token) {
                joinExistingEnclave();

                if (null == enclave_token) {
                    // We didn't like the existing enclave.  We could've been moved across a server instance and the current enclave
                    // reference is no longer good.  Check the POLICY setting to determine if we should create a new enclave.
                    if (propagationPolicy.equalsIgnoreCase(WLMContextProviderImpl.POLICY_PROPAGATE)) {
                        // POLICY_PROPAGATE -- states to only use the existing enclave if present.  Since we can't use the existing
                        // enclave we will run without one.
                    } else {
                        // We need to make a new one with the default TC
                        createNewEnclave();
                    }
                }
            } // Else no enclave...are we supposed to make a new one?
            else if ((propagationPolicy.equalsIgnoreCase(WLMContextProviderImpl.POLICY_PROPAGATE_OR_NEW)) ||
                     (propagationPolicy.equalsIgnoreCase(WLMContextProviderImpl.POLICY_NEW))) {
                // We need to make a new one with the default TC
                createNewEnclave();
            } // Else nothing to join, nothing to create..all done!
        }
    }

    /** {@inheritDoc} */
    @Override
    public void taskStopping() {

        // Find the enclave manager
        EnclaveManager em = cpi.enclaveManager();

        // If we have an enclave, leave it.  Leave will delete it if nobody else is using it
        if (null != enclave_token) {
            leaveEnclave();
        }

        // If there was an enclave on the thread when we started this, put it back now
        if ((null != em) && (null != preExistingEnclave)) {
            try {
                em.restoreEnclaveToThread(preExistingEnclave);
            } catch (AlreadyClassifiedException e) {
                // Well that didn't work..oh well
            }
            preExistingEnclave = null; // if we had an enclave, forget about it
        }

    }

    /**
     * Task Starting work when we're on a Daemon thread
     */
    protected void handleDaemonWork() {
        // Get the enclave manager if there is one
        EnclaveManager em = cpi.enclaveManager();
        NativeUtils nativeUtils = cpi.getNativeUtils();

        // create an enclave with daemon TC (if we have an enclave manager and native utility object reference)
        if (null != em && null != nativeUtils) {
            // Creating a new enclave, we need an arrival time..now sounds good
            long arrivalTime = nativeUtils.getSTCK();
            // Create a new enclave and join it
            Enclave enclave = em.joinNewEnclave(daemonTransactionClass, arrivalTime);
            // Get the token for our new enclave
            enclave_token = em.getStringToken(enclave);
        }
        // If no enclaveManager and native utility object reference then do nothing
    }

    /**
     * Handle taskStarting work when we have an enclave to join
     */
    protected void joinExistingEnclave() {
        // Get the enclave manager if there is one
        EnclaveManager em = cpi.enclaveManager();

        //We have an enclave to join, check for the enclave manager again
        if (null != em) {
            Enclave enclave = null;
            // Convert our token to an enclave object
            enclave = em.getEnclaveFromToken(enclave_token);
            // If we found it
            if (null != enclave) {
                try {
                    em.preJoinEnclave(enclave);
                    em.joinEnclave(enclave);
                } catch (AlreadyClassifiedException e) {
                    // Well that didn't work..best we just forget about the enclave and let the caller decide what to do,
                    // if anything.
                    enclave_token = null;
                }
            } else {
                // Enclave no longer known to EnclaveManager.
                enclave_token = null;
            }

        } // else no enclave manager..do nothing..

    }

    /**
     * handle taskStarting work when we need a new Enclave
     */
    protected void createNewEnclave() {
        // Get the enclave manager if there is one
        EnclaveManager em = cpi.enclaveManager();
        NativeUtils nativeUtils = cpi.getNativeUtils();

        if (null != em && null != nativeUtils) {
            // Need an arrival time...
            long arrivalTime = nativeUtils.getSTCK();
            // Create a new one and join it
            Enclave enclave = em.joinNewEnclave(defaultTransactionClass, arrivalTime);
            // And put it in the map and remember the token in this object
            enclave_token = (enclave != null) ? em.getStringToken(enclave) : null;
        } // else no enclave manager and native utility object reference so just do nothing

    }

    /**
     * Handle taskStopping work when we have an enclave
     */
    protected void leaveEnclave() {
        // Find the enclave manager
        EnclaveManager em = cpi.enclaveManager();

        if (null != em) {
            Enclave enclave = null;
            // Find it in the map
            enclave = em.getEnclaveFromToken(enclave_token);
            if (null != enclave) {
                // Leave the enclave
                em.leaveEnclave(enclave);
            }
        }
        enclave_token = null;
    }

    /**
     * Deserialize.
     *
     * @param in stream from which to deserialize.
     * @throws IOException            if an error occurs.
     * @throws ClassNotFoundException
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        GetField fields = in.readFields();
        daemonTransactionClass = (String) fields.get(DAEMON_TRAN_CLASS, "ASYNCDMN");
        defaultTransactionClass = (String) fields.get(DEFAULT_TRAN_CLASS, "ASYNCBN");
        enclave_token = (String) fields.get(ENCLAVE_TOKEN, null);
        isLongRunning = fields.get(IS_LONG_RUNNING, false);
        propagationPolicy = (String) fields.get(PROPAGATION_POLICY, WLMContextProviderImpl.POLICY_NEW);
    }

    /**
     * Serialize.
     *
     * @param out stream to which to serialize.
     *
     * @throws IOException if an error occurs.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        PutField fields = out.putFields();
        fields.put(DAEMON_TRAN_CLASS, daemonTransactionClass);
        fields.put(DEFAULT_TRAN_CLASS, defaultTransactionClass);
        fields.put(ENCLAVE_TOKEN, enclave_token);
        fields.put(IS_LONG_RUNNING, isLongRunning);
        fields.put(PROPAGATION_POLICY, propagationPolicy);
        out.writeFields();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("WLMContextImpl@" + Integer.toHexString(this.hashCode()));
        sb.append("\n\tdaemonTransactionClass  : " + daemonTransactionClass);
        sb.append("\n\tdefaultTransactionClass : " + defaultTransactionClass);
        sb.append("\n\tenclave_token           : " + enclave_token);
        sb.append("\n\tpropagationPolicy       : " + propagationPolicy);
        sb.append("\n\tpreExistingEnclave      : " + ((preExistingEnclave != null) ? Integer.toHexString(preExistingEnclave.hashCode()) : "null"));
        sb.append("\n\tcpi                     : " + ((cpi != null) ? Integer.toHexString(cpi.hashCode()) : "null"));
        sb.append("\n\tisLongRunning           : " + preExistingEnclave);

        return sb.toString();
    }
}
