/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.ejb.mdb.internal;

import java.io.UnsupportedEncodingException;
import java.util.Dictionary;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.wlm.internal.WLMNativeServices;

/**
 * MDB Classification class representing a mdbClassification configuration element.
 */
public class MDBClassification {

    /** Trace component. */
    private static final TraceComponent tc = Tr.register(MDBClassification.class);

    /** Config attribute key indicating the activation specification id of the MDB to classify. */
    public static final String CFG_JMS_ACTIVATION_SPEC = "jmsActivationSpec";

    /** Config attribute key indicating the transaction class to use for classification. */
    public static final String CFG_TRANSACTION_CLASS = "transactionClass";

    /** Transaction class name associated with the MDB in string form. */
    private String transactionClass;

    /** Transaction class name associated with the MDB in byte (ebcdic) form. */
    private byte[] transactionClassBytes;

    /** Activation specification ID associated with the MDB. */
    private String activationSpecId;

    /** Determines the validity of the underlying mdbClassification configuration element. */
    private boolean valid;

    /**
     * Constructor.
     *
     * @param transactionClass The transaction class.
     * @param activationSpecId The activation specification.
     */
    public MDBClassification(Dictionary<?, ?> prop) {
        // Read the transactionClass attribute. Note that a mdbClassification element that does not define
        // a transactionClass or contains a transactionClass attribute with an empty string value is valid
        // and significant because it is a way for users to define a rule for requests that they
        // do not want to be classified. This is helpful in cases where a user wants certain requests to be
        // specifically tracked by WLM through an enclave and also wants to have a rule to catch all
        // other requests (jmsActivationSpec="*") while at the same time being able to prevent certain
        // requests from being processed under an enclave.
        transactionClass = (String) prop.get(CFG_TRANSACTION_CLASS);

        // Read the jmsActivationSpec attribute.
        String configActivationSpec = (String) prop.get(CFG_JMS_ACTIVATION_SPEC);
        activationSpecId = configActivationSpec.trim();

        // Validate the activation specification. If invalid, this mdbClassification will be ignored.
        if (activationSpecId.isEmpty()) {
            Tr.warning(tc, "MDB_CONFIG_INVALID_ACTIVATION_SPEC", new Object[] { configActivationSpec, transactionClass });
            return;
        }

        // Check the length of the transactionClass. If it's too long, truncate it to the max length (8 chars).
        if (transactionClass.length() > WLMNativeServices.WLM_MAXIMUM_TRANSACTIONCLASS_LENGTH) {
            String originalTclass = transactionClass;
            transactionClass = transactionClass.substring(0, WLMNativeServices.WLM_MAXIMUM_TRANSACTIONCLASS_LENGTH);
            Tr.warning(tc, "MDB_CLASSIFICATION_TRANCLASS_TRUNCATED",
                       new Object[] { originalTclass, activationSpecId, transactionClass, WLMNativeServices.WLM_MAXIMUM_TRANSACTIONCLASS_LENGTH });
        }

        // If the transaction class is not an empty string, blank-pad the transactionClass and convert it to EBCDIC for Native use.
        if (!transactionClass.equals("")) {
            String temp = transactionClass + "        ".substring(0, WLMNativeServices.WLM_MAXIMUM_TRANSACTIONCLASS_LENGTH - transactionClass.length());
            try {
                transactionClassBytes = temp.getBytes("Cp1047");
            } catch (UnsupportedEncodingException e) {
                // Issue an FFDC an return. This mdbClassification will be ignored.
                return;
            }
        }

        valid = true;
    }

    /**
     * Retrieves the transaction class in ebcdic byte array form.
     *
     * @return The transaction class in in ebcdic byte array form.
     */
    public byte[] getTransactionClassBytes() {
        return transactionClassBytes;
    }

    /**
     * Retrieves the activation specification id.
     *
     * @return The activation specification id.
     */
    public String getActivationSpecId() {
        return activationSpecId;
    }

    /**
     * Returns True if this class instance representing a mdbClassification configuration element is valid. False otherwise.
     *
     * @return True if this class instance representing a mdbClassification configuration element is valid. False otherwise.
     */
    public boolean isValid() {
        return valid;
    }
}
