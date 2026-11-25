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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.wlm.WLMConfigManager;

/**
 * Provides access to the LE WLM interfaces relating to WLM Enclaves
 */
public class UnauthorizedWLMNativeServices implements WLMNativeServices {
    /**
     * Native method manager DS injected reference.
     */
    protected NativeMethodManager nativeMethodManager = null;

    /**
     * WLM configuration manager DS injected reference.
     */
    protected WLMConfigManager wlmConfigManager = null;

    /**
     * The token representing a successful connection to WLM.
     * The LE routine called for connect is ConnectWorkMgr. It is equivalent to the
     * following WLM service call:
     * IWMCONN WORK_MANAGER=YES
     * ROUTER=NO
     * QUEUE_MANAGER=YES
     * SERVER_MANAGER=NO
     * EXPTIMPT=YES
     * Reference: z/OS MVS Programming: Workload Management Services:
     * Appendix D. C Language Interfaces for Workload Management Services
     */
    private Integer connectToken;

    /**
     * WLM Connect is associated with the thread that issued it. If the thread
     * terminates then the WLM resources created under that Connect Token are cleaned up. So,
     * any WLM Enclaves created are deleted, and future create attempts using the connect token
     * will fail.
     * Solution: We need to perform the WLM Connect under a thread that wont go away until we
     * say. So, create are own thread and do the WLM Connect under it.
     */
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * DS method to activate this component.
     */
    protected void activate() throws Exception {

        // Attempt to load native code via the method manager.
        Object[] callbackData = new Object[] { WLMServiceResults.class, "setResults" };
        nativeMethodManager.registerNatives(UnauthorizedWLMNativeServices.class, callbackData);

        // Attempt to connect.
        connectAsWorkMgr();
    }

    /**
     * DS method to deactivate this component.
     */
    protected void deactivate() {
        try {
            if (connectToken != null) {
                // This method calls LE's DisconnectServer. It is equivalent to
                // to a WLM service call IWMDISC.
                // Reference: z/OS MVS Programming: Workload Management Services:
                // Appendix D. C Language Interfaces for Workload Management Services
                ntv_le_disconnectAsWorkMgr(connectToken);
            }
        } finally {
            this.cleanupExecutorService();
        }
    }

    /**
     * Sets the reference to a NativeMethodManager instance. Called by DS.
     *
     * @param nativeMethodManager The NativeMethodManager instance to unset.
     */
    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    /**
     * Unsets the reference to a NativeMethodManager instance. Called by DS.
     *
     * @param nativeMethodManager The NativeMethodManager instance to unset.
     */
    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    /**
     * Sets the reference to a WLMConfigManager instance. Called by DS.
     *
     * @param wlmConfigManager The WLMConfigManager instance to unset.
     */
    protected void setWlmConfig(WLMConfigManager wlmConfigManager) {
        this.wlmConfigManager = wlmConfigManager;
    }

    /**
     * Unsets the reference to a WLMConfigManager instance. Called by DS.
     *
     * @param wlmConfigManager The WLMConfigManager instance to unset.
     */
    protected void unsetWlmConfig(WLMConfigManager wlmConfigManager) {
        if (this.wlmConfigManager == wlmConfigManager) {
            this.wlmConfigManager = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public byte[] createWorkUnit(byte[] classificationInfo, long arrivalTime) {
        byte[] enclaveToken = ntv_le_createWorkUnit(connectToken,
                                                    classificationInfo,
                                                    wlmConfigManager.getCreateFunctionName(),
                                                    wlmConfigManager.getClassifyCollectionName(),
                                                    arrivalTime);

        // If failed to call service
        if (enclaveToken == null) {
            // returning null after a message
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }

        return enclaveToken;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] deleteWorkUnit(byte[] etoken) {
        byte[] deleteDataBytes = new byte[WLM_DELETE_DATA_LENGTH];
        int rc = ntv_le_deleteWorkUnit(etoken);

        // If failed to call service
        if (rc != 0) {
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }
        System.arraycopy(etoken, 0, deleteDataBytes, WLM_DELETE_DATA_ENCLAVE_OFFSET, WLM_DELETE_DATA_ENCLAVE_LENGTH);
        return deleteDataBytes;
    }

    /** {@inheritDoc} */
    @Override
    public boolean joinWorkUnit(byte[] token) {
        boolean rc = ntv_le_joinWorkUnit(token);

        // If failed to call service
        if (rc == false) {
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }

        return rc;
    }

    /** {@inheritDoc} */
    @Override
    public boolean leaveWorkUnit(byte[] token) {
        boolean rc = ntv_le_leaveWorkUnit(token);

        // If failed to call service
        if (rc == false) {
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }

        return rc;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] createJoinWorkUnit(byte[] transactionClass, long arrivalTime) {

        byte[] enclaveToken = ntv_le_createJoinWorkUnit(connectToken,
                                                        transactionClass,
                                                        arrivalTime);

        // If failed to call service
        if (enclaveToken == null) {
            // Returning null after a message unless threw of course
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }
        return enclaveToken;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] leaveDeleteWorkUnit(byte[] etoken) {
        byte[] deleteDataBytes = new byte[WLM_DELETE_DATA_LENGTH];
        int rc = ntv_le_leaveDeleteWorkUnit(etoken);

        // If failed to call service
        if (rc != 0) {
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }
        System.arraycopy(etoken, 0, deleteDataBytes, WLM_DELETE_DATA_ENCLAVE_OFFSET, WLM_DELETE_DATA_ENCLAVE_LENGTH);
        return deleteDataBytes;
    }

    /** {@inheritDoc} */
    @Override
    public void writeDiagnostics(OutputStream out) throws IOException {
        StringBuilder sbuilder = new StringBuilder();

        // Write WLMNativeServices info
        sbuilder.append("\nUnauthorized WLM -- Introspection\n");
        sbuilder.append("\tWLM Connection Token: ");
        sbuilder.append(Integer.toHexString(connectToken));
        sbuilder.append("\n");

        // Write diagnostics from config data
        if (wlmConfigManager != null) {
            wlmConfigManager.writeDiagnostics(out);
        }

        out.write(sbuilder.toString().getBytes());
    }

    /**
     * Connects to WLM as a work manager. Invoked during activatation.
     */
    private void connectAsWorkMgr() throws Exception {
        // Drive connect.
        Future<Integer> future = this.executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return ntv_le_connectAsWorkMgr(wlmConfigManager.getSubSystem(),
                                               wlmConfigManager.getSubSystemName(),
                                               wlmConfigManager.getCreateFunctionName(),
                                               wlmConfigManager.getClassifyCollectionName());
            }
        });

        // Wait for and get the result.
        Integer rc = future.get();

        if (rc == -1) {
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        } else {
            connectToken = rc;
        }
    }

    /**
     * Shuts down the executor service.
     */
    private void cleanupExecutorService() {
        // reject new tasks
        this.executorService.shutdown();

        // wait for termination
        try {
            this.executorService.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
    }

    //
    // Native Services
    //
    protected native int ntv_le_connectAsWorkMgr(String subSystem,
                                                 String subSystemName,
                                                 String createFunctionName,
                                                 String classifyCollectionNam);

    protected native int ntv_le_disconnectAsWorkMgr(int connectToken);

    protected native boolean ntv_le_joinWorkUnit(byte[] token);

    protected native boolean ntv_le_leaveWorkUnit(byte[] token);

    protected native byte[] ntv_le_createWorkUnit(int connectToken,
                                                  byte[] classificationInfo,
                                                  String createFunctionName,
                                                  String classifyCollectionName,
                                                  long arrivalTime);

    protected native int ntv_le_deleteWorkUnit(byte[] etoken);

    protected native byte[] ntv_le_createJoinWorkUnit(int connectToken,
                                                      byte[] transactionClass,
                                                      long arrivalTime);

    protected native int ntv_le_leaveDeleteWorkUnit(byte[] etoken);
}
