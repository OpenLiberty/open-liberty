/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.request.logging.data.DataProvider;
import com.ibm.ws.zos.wlm.AlreadyClassifiedException;
import com.ibm.ws.zos.wlm.ClassificationInfo;
import com.ibm.ws.zos.wlm.Enclave;
import com.ibm.ws.zos.wlm.WLMHealthConstants;

/**
 * A declarative services component can be completely POJO based (no
 * awareness/use of OSGi services).
 *
 * OSGi methods (activate/deactivate) should be protected.
 */
public class EnclaveManagerImpl implements com.ibm.ws.zos.wlm.EnclaveManager, DataProvider, com.ibm.wsspi.logging.IntrospectableService {

    protected NativeMethodManager nativeMethodManager = null;

    protected WLMNativeServices wlmNativeServices = null;

    /**
     * Data Provider name.
     */
    private final String ENCLAVE_MGR_PROVIDER_NAME = "com.ibm.ws.zos.wlm.EnclaveManager";

    /**
     * Total number of WLM Enclaves created for current activation
     */
    protected final AtomicLong totalEnclaveCreateCount = new AtomicLong();

    /**
     * Total number of WLM Enclaves deleted for current activation
     */
    protected final AtomicLong totalEnclaveDeleteCount = new AtomicLong();

    /**
     * A thread local to let us easily find the enclave currently joined to a thread
     */
    private static final ThreadLocal<Enclave> threadEnclave = new ThreadLocal<Enclave>() {
        @Override
        protected Enclave initialValue() {
            return null;
        }
    };

    /**
     * DS method to activate this component.
     *
     * @param properties
     *                       : Map containing service & config properties
     *                       populated/provided by config admin
     *
     * @throws Exception
     */
    protected void activate(Map<String, Object> properties) throws Exception {
    }

    /**
     * DS method to deactivate this component.
     *
     * @param reason
     *                   int representation of reason the component is stopping
     */
    protected void deactivate(int reason) {

    }

    protected void setWlmNativeService(WLMNativeServices wlmNativeServices) {
        this.wlmNativeServices = wlmNativeServices;
    }

    protected void unsetWlmNativeService(WLMNativeServices wlmNativeServices) {
        if (this.wlmNativeServices == wlmNativeServices) {
            this.wlmNativeServices = null;
        }
    }

    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preJoinEnclave(Enclave enclave) {
        EnclaveImpl enclaveImpl = (EnclaveImpl) enclave;
        synchronized (enclaveImpl) {
            // not in use, but don't  let it go away
            enclaveImpl.incrementPendingUseCount();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void joinEnclave(Enclave enclave) throws AlreadyClassifiedException {
        EnclaveImpl enclaveImpl = (EnclaveImpl) enclave;
        boolean joinSucceeded = false;

        synchronized (enclaveImpl) {
            // Increment in use, decrement pending use
            enclaveImpl.joining();

            // Drive join synchronized. We do not want a leave to happen as a join is taking place.
            joinSucceeded = wlmNativeServices.joinWorkUnit(enclaveImpl.getToken());

            // remember which enclave is on this thread
            if (joinSucceeded) {
                threadEnclave.set(enclaveImpl);
            }
        }

        // If join didn't work
        if (!joinSucceeded) {
            // decrement in use and maybe delete, don't really leave
            leaveEnclave(enclaveImpl, true);
            throw new AlreadyClassifiedException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] leaveEnclave(Enclave enclave) {
        return leaveEnclave(enclave, false);
    }

    /**
     * Leave the enclave
     *
     * @param enclave        The enclave to remove from the thread
     * @param joinIncomplete true if the join failed so we're just trying to delete the enclave
     *
     * @return data from enclave delete
     */
    protected byte[] leaveEnclave(Enclave enclave, boolean joinIncomplete) {
        EnclaveImpl enclaveImpl = (EnclaveImpl) enclave;
        boolean inUse = true;
        byte[] deleteData = null;

        synchronized (enclaveImpl) {
            enclaveImpl.leaving(); // decrement in use count
            inUse = enclaveImpl.isInUse(); // is it still in use?

            if (!joinIncomplete) { // join worked, so we should leave
                // We need to leave, can we also delete?.
                if (!inUse && enclaveImpl.getCreatedByEnclaveManager() && enclaveImpl.getAutoDelete()) {
                    // increment the overall delete counter
                    totalEnclaveDeleteCount.getAndIncrement();
                    // leave and delete
                    deleteData = wlmNativeServices.leaveDeleteWorkUnit(enclaveImpl.getToken());
                    HashMap<String, Object> wlmData = new HashMap<String, Object>();
                    wlmData.put(WLMNativeServices.WLM_DATA_ENCLAVE_FORCED_DELETION, Boolean.valueOf(false));
                    wlmData.put(WLMNativeServices.WLM_DATA_DELETE_DATA, deleteData);
                    wlmData.put(WLMNativeServices.WLM_DATA_ENCLAVE_TOKEN, enclaveImpl.getStringToken());

                    // Remove the enclave from the map
                    EnclaveMap.forgetEnclave(enclaveImpl);
                    // Enclave no longer tied to this thread
                    threadEnclave.remove();

                    processPostEnclaveDelete(wlmData);
                } else {
                    // just leave
                    wlmNativeServices.leaveWorkUnit(enclaveImpl.getToken());
                    // enclave no longer tied to this thread
                    threadEnclave.remove();
                }
            } else {
                // Join failed, maybe we can delete
                if (!inUse && enclaveImpl.getCreatedByEnclaveManager() && enclaveImpl.getAutoDelete()) {
                    // Increment the overall delete counter
                    totalEnclaveDeleteCount.getAndIncrement();
                    // Forget we created it
                    enclaveImpl.setCreatedByEnclaveManager(false);
                    // Go delete it
                    deleteData = wlmNativeServices.deleteWorkUnit(enclaveImpl.getToken());
                    // Remove the enclave from the map
                    EnclaveMap.forgetEnclave(enclaveImpl);
                }
            }
        }
        return deleteData;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassificationInfo createClassificationInfo(String txClass) {
        return new ClassificationInfoImpl(txClass, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enclave create(ClassificationInfo info, long arrivalTime) {
        ClassificationInfoImpl infoImpl = (ClassificationInfoImpl) info;
        byte[] token = wlmNativeServices.createWorkUnit(infoImpl.getRawClassificationData(), arrivalTime);
        EnclaveImpl enclave = null;

        if (token != null) {
            enclave = new EnclaveImpl(token);
            enclave.incrementPendingUseCount();
            enclave.setCreatedByEnclaveManager(true);

            EnclaveMap.rememberEnclave(enclave);
            totalEnclaveCreateCount.getAndIncrement();
        }

        return enclave;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteEnclave(Enclave enclave, boolean force) {
        EnclaveImpl enclaveImpl = (EnclaveImpl) enclave;
        byte[] deleteData = null;
        synchronized (enclaveImpl) {
            if (enclaveImpl.getCreatedByEnclaveManager() && (!enclaveImpl.isInUse() || force)) {
                enclaveImpl.setCreatedByEnclaveManager(false);
                totalEnclaveDeleteCount.getAndIncrement();
                // Remove the enclave from the map
                EnclaveMap.forgetEnclave(enclaveImpl);
                deleteData = wlmNativeServices.deleteWorkUnit(enclaveImpl.getToken());
                HashMap<String, Object> wlmData = new HashMap<String, Object>();
                wlmData.put(WLMNativeServices.WLM_DATA_ENCLAVE_FORCED_DELETION, Boolean.valueOf(force));
                wlmData.put(WLMNativeServices.WLM_DATA_DELETE_DATA, deleteData);
                wlmData.put(WLMNativeServices.WLM_DATA_ENCLAVE_TOKEN, enclaveImpl.getStringToken());
                processPostEnclaveDelete(wlmData);
            }
        }
    }

    /**
     * Allows the probe framework used by the request logging support to get a hold of the data being passed
     * to this method. At the moment this method is a no-op.
     * Any changes to this method's signature or name MUST be reflected in:
     * com.ibm.ws.request.probe.zoswlm.EnclaveManagerProcessPostEnclaveDelete.java
     *
     * @param wlmData The Map containing data to be written to SMF.
     */
    public void processPostEnclaveDelete(HashMap<String, Object> wlmData) {
        /* DO NOT DELETE METHOD. */}

    /** {@inheritDoc} */
    @Override
    public Enclave joinNewEnclave(String tclass, long arrivalTime) {
        Enclave enclave = null;

        // Blank-pad TransactionClass and convert to EBCDIC for Native use.
        byte[] tclassNative;
        if (!tclass.equals("")) {
            String temp = tclass + "        ".substring(0, WLMNativeServices.WLM_MAXIMUM_TRANSACTIONCLASS_LENGTH - tclass.length());
            try {
                tclassNative = temp.getBytes("Cp1047");
            } catch (UnsupportedEncodingException uee) {
                throw new RuntimeException("code page conversion error", uee);
            }

            enclave = joinNewEnclave(tclassNative, arrivalTime);
        }
        return enclave;
    }

    /** {@inheritDoc} */
    @Override
    public Enclave joinNewEnclave(byte[] transactionClass, long arrivalTime) {
        EnclaveImpl enclave = null;

        byte[] token = wlmNativeServices.createJoinWorkUnit(transactionClass, arrivalTime);

        if (token != null) {
            enclave = new EnclaveImpl(token);
            // Fix up the usage counters & Ownership flag
            enclave.incrementPendingUseCount();
            enclave.joining();
            threadEnclave.set(enclave);

            enclave.setCreatedByEnclaveManager(true);

            totalEnclaveCreateCount.getAndIncrement();
            EnclaveMap.rememberEnclave(enclave);
        }

        return enclave;
    }

    private final static String INTROSPECTION_NAME = "WlmIntrospection";
    private final static String INTROSPECTION_DESC = "Introspect zosWlm's state.";

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return INTROSPECTION_DESC;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return INTROSPECTION_NAME;
    }

    /** {@inheritDoc) */
    @Override
    public String getStringToken(Enclave e) {
        if (null == e) {
            return null;
        }
        EnclaveImpl ei = (EnclaveImpl) e;
        return ei.getStringToken();
    }

    /** {@inheritDoc) */
    @Override
    public Enclave getEnclaveFromToken(String s) {
        Enclave e = null;
        if (null != s) {
            try {
                e = EnclaveMap.findEnclave(s);
            } catch (EnclaveNotFoundException enfe) {
                // Just return null
            }
        }
        return e;
    }

    /** {@inheritDoc} */
    @Override
    public void introspect(OutputStream out) throws IOException {
        StringBuilder sbuilder = new StringBuilder();

        sbuilder.append("\nWLM -- Introspector \n");

        sbuilder.append("\tTotal number of WLM Enclaves created for current activation (");
        sbuilder.append(totalEnclaveCreateCount.get()).append(")\n");

        sbuilder.append("\tTotal number of WLM Enclaves destroyed for current activation (");
        sbuilder.append(totalEnclaveDeleteCount.get()).append(")\n");

        sbuilder.append("\tConfigured WLM Health increment ");
        sbuilder.append(WLMHealthImpl.zosHealthConfig.get(WLMHealthConstants.zosHealthIncrement)).append("\n");

        sbuilder.append("\tConfigured WLM Health increment interval ");
        sbuilder.append(WLMHealthImpl.zosHealthConfig.get(WLMHealthConstants.zosHealthInterval)).append("\n");

        sbuilder.append("\tCurrent WLM Health level percentage ");
        sbuilder.append(WLMHealthImpl.currentHealth.toString()).append("\n");

        out.write(sbuilder.toString().getBytes());

        // Write diagnostics from Active WLMNativeServices
        if (wlmNativeServices != null) {
            wlmNativeServices.writeDiagnostics(out);
        }
    }

    /** (@inheritDoc) */
    @Override
    public Enclave removeCurrentEnclaveFromThread() {
        //  Get the current enclave
        Enclave enclave = getCurrentEnclave();
        // Do we have one?
        if (null != enclave) {
            EnclaveImpl enclaveImpl = (EnclaveImpl) enclave;
            // Get it off the thread
            wlmNativeServices.leaveWorkUnit(enclaveImpl.getToken());
            // Forget it was on the thread ourselves
            threadEnclave.remove();
        }
        // Return the enclave that was on the thread (or null if there wasn't one)
        return enclave;
    }

    /** {@inheritDoc) */
    @Override
    public void restoreEnclaveToThread(Enclave enclave) throws AlreadyClassifiedException {
        // Did you give us an enclave?
        if (null != enclave) {
            EnclaveImpl enclaveImpl = (EnclaveImpl) enclave;
            // Try to put it on the thread..if it doesn't work, there's probably already an enclave
            // on the thread.  You messed up..throw an exception
            if (wlmNativeServices.joinWorkUnit(enclaveImpl.getToken()) == false) {
                throw new AlreadyClassifiedException();
            }
            // Remember we put it on the thread
            threadEnclave.set(enclaveImpl);
        }
    }

    /** {@inheritDoc) */
    @Override
    public Enclave getCurrentEnclave() {
        return threadEnclave.get();
    }

    /** {@inheritDoc) */
    @Override
    public String getProviderName() {
        return ENCLAVE_MGR_PROVIDER_NAME;
    }

    /** {@inheritDoc) */
    @Override
    public String getDataString() {
        Enclave currentEnclave = getCurrentEnclave();
        return (currentEnclave != null) ? currentEnclave.getStringToken() : null;
    }
}
