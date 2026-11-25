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
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.jni.NativeMethodManager;
import com.ibm.ws.zos.wlm.WLMConfigManager;

/**
 * Provides access to the Authorized WLM interfaces relating to WLM Enclaves
 */
public class AuthorizedWLMNativeServices implements WLMNativeServices {
    private static final TraceComponent tc = Tr.register(AuthorizedWLMNativeServices.class);

    protected NativeMethodManager nativeMethodManager = null;
    protected WLMConfigManager wlmConfigManager = null;

    /**
     * WLM Connection Token
     */
    private int connectToken = 0;

    /**
     * DS method to activate this component.
     *
     * @param properties
     *
     * @throws Exception
     */
    protected void activate(Map<String, Object> properties) throws Exception {

        // Attempt to load native code via the method manager.
        Object[] callbackData = new Object[] { WLMServiceResults.class, "setResults" };
        nativeMethodManager.registerNatives(AuthorizedWLMNativeServices.class, callbackData);

        // Attempt to connect -- will throw if unable
        connectAsWorkMgr();
    }

    /**
     * DS method to deactivate this component.
     *
     * @param reason
     *                   int representation of reason the component is stopping
     */
    protected void deactivate(int reason) {
        // Disconnect from WLM
        disconnectAsWorkMgr();
    }

    protected void setNativeMethodManager(NativeMethodManager nativeMethodManager) {
        this.nativeMethodManager = nativeMethodManager;
    }

    protected void unsetNativeMethodManager(NativeMethodManager nativeMethodManager) {
        if (this.nativeMethodManager == nativeMethodManager) {
            this.nativeMethodManager = null;
        }
    }

    protected void setWlmConfig(WLMConfigManager wlmConfigManager) {
        this.wlmConfigManager = wlmConfigManager;
    }

    protected void unsetWlmConfig(WLMConfigManager wlmConfigManager) {
        if (this.wlmConfigManager == wlmConfigManager) {
            this.wlmConfigManager = null;
        }
    }

    /**
     * Set the current value of the WLM Connect Token
     */
    protected int getConnectToken() {
        return connectToken;
    }

    protected void setConnectToken(int inConnectToken) {
        this.connectToken = inConnectToken;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] createWorkUnit(byte[] classificationInfo, long arrivalTime) {
        byte[] serviceClassTokenBytes = new byte[4];
        ClassificationInfoImpl classificationInfoImpl = new ClassificationInfoImpl(classificationInfo);
        String transactionClass = classificationInfoImpl.getTransactionClass();
        String transactionName = classificationInfoImpl.getTransactionName();
        String classifyCollectionName = wlmConfigManager.getClassifyCollectionName();
        String serviceClassTokenKey = classifyCollectionName + transactionClass + transactionName;
        int serviceClassToken = wlmConfigManager.getServiceClassToken(serviceClassTokenKey);
        byte[] enclaveToken = ntv_createWorkUnit(getConnectToken(),
                                                 classificationInfo,
                                                 wlmConfigManager.getCreateFunctionName(),
                                                 classifyCollectionName,
                                                 arrivalTime,
                                                 serviceClassToken,
                                                 serviceClassTokenBytes);

        // If failed to call service
        if (enclaveToken == null) {
            // returning null after a message
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        } else {
            IntBuffer ibuff = ByteBuffer.wrap(serviceClassTokenBytes).asIntBuffer();
            int outputServiceClassToken = ibuff.get();
            if (outputServiceClassToken != 0) {
                wlmConfigManager.putServiceClassToken(serviceClassTokenKey, outputServiceClassToken);
            }
        }

        return enclaveToken;
    }

    /**
     * {@inheritDoc}
     *
     * @param etoken WLM Enclave token to delete
     * @return
     *
     */
    @Override
    public byte[] deleteWorkUnit(byte[] etoken) {
        byte[] deleteDataBytes = new byte[WLM_DELETE_DATA_LENGTH];
        int rc = ntv_deleteWorkUnit(etoken, deleteDataBytes);

        // If failed to call service
        if (rc != 0) {
            // Issue appropriate message
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }
        return deleteDataBytes;
    }

    /** {@inheritDoc} */
    @Override
    public boolean joinWorkUnit(byte[] token) {

        boolean rc = ntv_joinWorkUnit(token);

        // If join was not successful
        if (rc == false) {
            // Issue appropriate message
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }

        return rc;
    }

    /** {@inheritDoc} */
    @Override
    public boolean leaveWorkUnit(byte[] token) {
        boolean rc = ntv_leaveWorkUnit(token);

        // If failed to call service
        if (rc == false) {
            // Issue appropriate message
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }

        return rc;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] createJoinWorkUnit(byte[] transactionClass, long arrivalTime) {

        byte[] serviceClassTokenBytes = new byte[4];
        int serviceClassToken = 0;
        String serviceClassTokenKey = null;

        try {
            // transaction name not set in this path so use blanks.
            serviceClassTokenKey = getServiceClassTokenKey(transactionClass, "        ");
        } catch (UnsupportedEncodingException e) {
            // This should not happen. If it does, service class token will be zero
            // so WLM will do a full classification like before.
        }
        if (serviceClassTokenKey != null) {
            serviceClassToken = wlmConfigManager.getServiceClassToken(serviceClassTokenKey);
        }
        byte[] enclaveToken = ntv_createJoinWorkUnit(getConnectToken(),
                                                     transactionClass,
                                                     arrivalTime,
                                                     serviceClassToken,
                                                     serviceClassTokenBytes);

        // If failed to call service
        if (enclaveToken == null) {
            // Returning null after a message unless threw of course
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        } else {
            IntBuffer ibuff = ByteBuffer.wrap(serviceClassTokenBytes).asIntBuffer();
            int outputServiceClassToken = ibuff.get();
            if ((outputServiceClassToken != 0) && (serviceClassTokenKey != null)) {
                wlmConfigManager.putServiceClassToken(serviceClassTokenKey, outputServiceClassToken);
            }
        }
        return enclaveToken;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public byte[] leaveDeleteWorkUnit(byte[] etoken) {
        byte[] deleteDataBytes = new byte[WLM_DELETE_DATA_LENGTH];
        int rc = ntv_leaveDeleteWorkUnit(etoken, deleteDataBytes);

        // If failed to call service
        if (rc != 0) {
            // Issue appropriate message
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }
        return deleteDataBytes;
    }

    /**
     * Connect to WLM to allow access to enclave management services.
     *
     * Problem: WLM Connect is associated with the thread that issued it. If the thread
     * terminates then the WLM resources created under that Connect Token are cleaned up. So,
     * any WLM Enclaves created are deleted, and future create attempts using the connect token
     * will fail.
     *
     * Solution: We need to perform the WLM Connect under a thread that wont go away until we
     * say. The native implementation for the authorized WLM connect is using the BPX4IPT service
     * to associate the WLM Connect with the IPT thread.
     *
     * @return
     */
    protected void connectAsWorkMgr() throws SecurityException {
        // Drive connect on a thread that wont go away (using BPX4IPT)
        int rc = ntv_connectAsWorkMgr(wlmConfigManager.getSubSystem(),
                                      wlmConfigManager.getSubSystemName(),
                                      wlmConfigManager.getCreateFunctionName(),
                                      wlmConfigManager.getClassifyCollectionName());

        if (rc == -1) {
            // Connect failed ... issue message.
            WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
        }

        // Connect Token from results
        setConnectToken(rc);

        if (rc == -1) {
            throw new java.lang.RuntimeException("failed connect to WLM");
        }
    }

    /**
     * Disconnect from WLM.
     *
     * @return
     */
    protected void disconnectAsWorkMgr() {
        // Drive connect on a thread that wont go away (using BPX4IPT)
        int connToken = this.getConnectToken();
        if (connToken != 0) {
            int rc = ntv_disconnectAsWorkMgr(connToken);
            this.setConnectToken(0);

            if (rc != 0) {
                // Disconnect failed ... issue message.
                WLMServiceResults.getWLMServiceResult().issueWLMServiceMessage();
            }
        }
    }

    /**
     * Write diagnostic information
     */
    @Override
    public void writeDiagnostics(OutputStream out) throws IOException {
        StringBuilder sbuilder = new StringBuilder();

        // Write WLMNativeServices info
        sbuilder.append("\nAuthorized WLM -- Introspection\n");
        sbuilder.append("\tWLM Connection Token: ");
        sbuilder.append(Integer.toHexString(getConnectToken()));
        sbuilder.append("\n");

        // Write diagnostics from config data
        if (wlmConfigManager != null) {
            wlmConfigManager.writeDiagnostics(out);
        }

        out.write(sbuilder.toString().getBytes());
    }

    /***
     * Create service class token key based on collection name, transaction class and transaction name.
     *
     * @param transactionClass
     * @param transactionName
     * @return service class token key.
     *
     */
    String getServiceClassTokenKey(byte[] transactionClass, String transactionName) throws UnsupportedEncodingException {
        // Convert from EBCDIC to ASCII.
        String transactionClassString = new String(transactionClass, "Cp1047");
        String serviceClassTokenKey = wlmConfigManager.getClassifyCollectionName() + transactionClassString + transactionName;
        return serviceClassTokenKey;
    }

    //
    // Native Services
    //
    protected native int ntv_connectAsWorkMgr(String subSystem,
                                              String subSystemName,
                                              String createFunctionName,
                                              String classifyCollectionName);

    protected native int ntv_disconnectAsWorkMgr(int connectToken);

    protected native boolean ntv_joinWorkUnit(byte[] token);

    protected native boolean ntv_leaveWorkUnit(byte[] token);

    protected native byte[] ntv_createWorkUnit(int connectToken,
                                               byte[] classificationInfo,
                                               String createFunctionName,
                                               String classifyCollectionName,
                                               long arrivalTime,
                                               int serviceClassToken,
                                               byte[] outputServiceClassToken);

    protected native byte[] ntv_createJoinWorkUnit(int connectToken,
                                                   byte[] transactionClass,
                                                   long arrivalTime,
                                                   int serviceClassToken,
                                                   byte[] outputServiceClassToken);

    protected native int ntv_deleteWorkUnit(byte[] etoken, byte[] deleteData);

    protected native int ntv_leaveDeleteWorkUnit(byte[] etoken, byte[] deleteData);

}
