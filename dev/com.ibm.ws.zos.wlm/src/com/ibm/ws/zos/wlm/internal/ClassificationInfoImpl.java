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

import java.io.UnsupportedEncodingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.wlm.ClassificationInfo;

/**
 * Collection of Classification informationi
 *
 * Note: Offsets and lengths must be kept in sync with native code server_wlm_services_jni.c
 */
public class ClassificationInfoImpl implements ClassificationInfo, java.io.Serializable {

    private static final TraceComponent tc = Tr.register(ClassificationInfo.class);

    private static final long serialVersionUID = 3390951690050916415L;

    /**
     * Version 1 data length. Until versions change, the byte arrays we play
     * with should be this long.
     */
    private final static int EXPECTED_DATA_LENGTH = 44;
    /**
     * Transaction Class offset
     */
    private final static int TX_CLASS_DATA_OFFSET = 20;
    /**
     * Transaction Class length
     */
    private final static int TX_CLASS_DATA_LENGTH = 8;
    /**
     * Transaction Name offset
     */
    private final static int TX_NAME_DATA_OFFSET = 36;
    /**
     * Transaction Name length
     */
    private final static int TX_NAME_DATA_LENGTH = 8;

    /**
     * The flattened version of some of the pieces from the IWMECD data that was
     * returned from IWMECQRY.
     */
    private byte[] _classificationData = null;

    /**
     * The ASCII transaction class used to create this Classification data
     */
    private String _transactionClass = null;

    /**
     * The ASCII transaction name used to create this Classification data
     */
    private String _transactionName = null;

    /**
     * Create a classification based on the specified transaction class.
     */
    public ClassificationInfo create(String txClass, String txName) {
        return new ClassificationInfoImpl(txClass, txName);
    }

    /**
     * Create a classification based on the specified transaction class.
     */
    protected ClassificationInfoImpl(String txClass, String txName) {
        // Removed uppercase of transaction class.  We want to pass whatever the config
        // set.  WLM Panels can support mixed case.  tWAS folds it to uppercase, but we
        // think that may be a future APAR.  So, in Liberty just take what they gave us
        // for now and if they want a switch to uppercase it, we will entertain that
        // later.

        _classificationData = new byte[EXPECTED_DATA_LENGTH];
        java.util.Arrays.fill(_classificationData, (byte) 0);
        byte[] txClassBytes = null;
        byte[] txNameBytes = null;

        // Version number
        _classificationData[0] = 1;
        try {
            // Cp1047 / EBCDIC should always be available on z/OS
            txClassBytes = txClass.getBytes("cp1047");

            if (txName != null) {
                txNameBytes = txName.getBytes("cp1047");
            }
        } catch (UnsupportedEncodingException ee) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "potential conversion problem?" + ee);
            }
            // Just in case it's not...
            if (txClassBytes == null) {
                txClassBytes = txClass.getBytes();
            }
            if (txNameBytes == null) {
                txNameBytes = txName.getBytes();
            }
        }

        myArrayCopy(txClassBytes, _classificationData, TX_CLASS_DATA_OFFSET, TX_CLASS_DATA_LENGTH);

        myArrayCopy(txNameBytes, _classificationData, TX_NAME_DATA_OFFSET, TX_NAME_DATA_LENGTH);

        // Save ASCII Transaction class
        _transactionClass = txClass;
        _transactionName = txName;
    }

    /**
     * Save the classification information for the enclave in a form that can be
     * serialized as a context.
     */
    protected ClassificationInfoImpl(byte[] info) {
        _classificationData = info;
    }

    /**
     * Return the classification data structure used in native code to create an
     * enclave for work execution.
     */
    protected final byte[] getRawClassificationData() {
        return _classificationData;
    }

    /**
     * @return the Transaction string used to create this ClassificationInfo
     *
     *         Note: just putting this in now ... anticipating SMF code in future needed
     *         it.
     */
    protected String getTransactionClass() {
        if (_transactionClass == null) {
            if (_classificationData != null) {
                byte[] txClassBytes = new byte[TX_CLASS_DATA_LENGTH];

                System.arraycopy(_classificationData, TX_CLASS_DATA_OFFSET,
                                 txClassBytes, 0, TX_CLASS_DATA_LENGTH);

                try {
                    // Convert from EBCDIC to ASCII.
                    _transactionClass = new String(txClassBytes, "Cp1047");
                } catch (UnsupportedEncodingException ee) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "potential conversion problem?" + ee);
                    }
                    // Just in case it's not...
                    _transactionClass = new String(txClassBytes);
                }
            }
        }
        return _transactionClass;
    }

    /**
     *
     * @return the Transaction namestring used to create this ClassificationInfo
     */
    protected String getTransactionName() {
        if (_transactionName == null) {
            if (_classificationData != null) {
                byte[] txNameBytes = new byte[TX_NAME_DATA_LENGTH];

                System.arraycopy(_classificationData, TX_NAME_DATA_OFFSET,
                                 txNameBytes, 0, TX_NAME_DATA_LENGTH);

                try {
                    // Convert from EBCDIC to ASCII.
                    _transactionName = new String(txNameBytes, "Cp1047");
                } catch (UnsupportedEncodingException ee) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "potential conversion problem?" + ee);
                    }
                    // Just in case it's not...
                    _transactionName = new String(txNameBytes);
                }
            }
        }
        return _transactionName;
    }

    /**
     * Use System.arraycopy to move bytes between byte arrays.
     *
     * @param copyOut     source of bytes to move
     * @param copyIn      target of moving bytes
     * @param copyInStart starting index in copyIn
     * @param copyMax     maximum number of bytes to move
     */
    protected void myArrayCopy(byte[] copyOut, byte[] copyIn, int copyInStart, int copyMax) {

        if ((copyOut != null) && (copyIn != null)) {
            System.arraycopy(copyOut,
                             0,
                             copyIn,
                             copyInStart,
                             (copyOut.length > copyMax) ? copyMax : copyOut.length);
        }
    }
}
