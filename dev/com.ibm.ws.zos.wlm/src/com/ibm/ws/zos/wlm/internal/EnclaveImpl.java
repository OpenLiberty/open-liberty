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

import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.zos.wlm.Enclave;

/**
 *
 */
public class EnclaveImpl implements Enclave {
    private static final TraceComponent tc = Tr.register(EnclaveImpl.class);

    /**
     * The token representing this enclave to z/OS WLM services. It is
     * immutable.
     */
    private byte[] _token = null;

    /**
     * Pre-calculated hashCode for this token This value is calulated on the
     * first hashCode() call. It is immutable from that point on since the
     * _token is also immutable.
     */
    private int _hash = -1;

    /**
     * A printable version of the enclave token for debug.
     */
    private String _stringToken = null;

    /**
     * The number of pieces of Work that are using this enclave.
     */
    private int _pendingUseCount = 0;

    /**
     * The number of threads that are currently joined to this enclave.
     */
    private int _inUseCount = 0;

    /**
     * Indicate that this enclave was created by WorkManager classification.
     */
    private boolean _createdByEnclaveManager = false;

    /**
     * The WLM registration token returned upon registration.
     */
    private byte[] _registrationToken = null;

    /**
     * Enclave deletion indicator. It is set to true by default and it prevents this enclave from
     * being deleted prematurely when the enclave manager is called for leaveEnclave.
     * This allows higher level enclave manager users to control when the enclave is deleted.
     */
    private boolean autoDelete;

    /**
     * Create an object representation of an enclave that wrappers the MVS WLM
     * enclave token.
     */
    protected EnclaveImpl(byte[] token) {
        autoDelete = true;
        setToken(token);

    }

    /**
     * Get the &quot;real&qout; WLM Enclave token that this object represents.
     */
    protected byte[] getToken() {
        return _token;
    }

    /** {@inheritDoc} */
    @Override
    public String getStringToken() {
        return toHexString(_token);
    }

    /**
     * Callback from the EnclaveManager to indicate that a thread is joining
     * this Enclave.
     */
    protected void joining() {
        _inUseCount++;
        _pendingUseCount--;
    }

    /**
     * Callback from the EnclaveManager to indicate that a thread is leaving
     * this Enclave.
     */
    protected void leaving() {
        _inUseCount--;
    }

    /**
     * Callback from the EnclaveManager to indicate that a join is pending.
     */
    protected void incrementPendingUseCount() {
        _pendingUseCount++;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInUse() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "_inUseCount", Integer.valueOf(_inUseCount));
            Tr.debug(tc, "_pendingUseCount", Integer.valueOf(_pendingUseCount));
        }

        boolean isInUse = _inUseCount > 0 || _pendingUseCount > 0;

        return isInUse;
    }

    /**
     * Set indication that this Enclave has been created by the EnclaveManager
     * and not by the runtime.
     */
    void setCreatedByEnclaveManager(boolean created) {
        _createdByEnclaveManager = created;
    }

    /**
     * Return the indication of whether or not this Enclave has been explicitly
     * created by the EnclaveManager. This will effect whether the WLM enclave
     * will be deregistered or deleted.
     */
    public boolean getCreatedByEnclaveManager() {
        return _createdByEnclaveManager;
    }

    /**
     * Determine if this enclave has been registered to avoid deletion.
     */
    boolean isRegistered() {
        boolean isRegistered = _registrationToken != null;

        return isRegistered;
    }

    /**
     * Save the token representing the registration of this Enclave.
     */
    void setRegistrationToken(byte[] token) {
        _registrationToken = token;
    }

    /**
     * Return the token representing the prior registration of this Enclave.
     */
    byte[] getRegistrationToken() {
        return _registrationToken;
    }

    /**
     * Set the &qout;real&qout; WLM enclave token for this object and update
     * dependent fields. Synchronization is not necessary since this is called
     * at construction. The _token object is immutable.
     */
    private void setToken(byte[] token) {
        _token = token;
        _stringToken = null;
        _hash = -1;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getAutoDelete() {
        return autoDelete;
    }

    /** {@inheritDoc} */
    @Override
    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    /**
     * Explicit hashCode calculation to facilitate use as key in various Map
     * Implementations.
     */
    @Override
    public synchronized int hashCode() {
        if (_hash == -1 && _token != null) {
            _hash = 0;

            // TODO: adjust to take advantage of seq# in token
            _hash |= ((_token[_token.length - 4] & 0xFF) << 24);
            _hash |= ((_token[_token.length - 3] & 0xFF) << 16);
            _hash |= ((_token[_token.length - 2] & 0xFF) << 8);
            _hash |= (_token[_token.length - 1] & 0xFF);
        }
        return _hash;
    }

    /**
     * Explicit equals implementation to facilitate use as a key in various Map
     * implementations.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof EnclaveImpl) {
            EnclaveImpl that = (EnclaveImpl) o;
            return Arrays.equals(this._token, that._token);
        }
        return false;
    }

    /**
     * Debugging and tracing aid.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nEnclaveImpl:" + super.toString());
        sb.append("\n\tpendingUseCount:" + Integer.toString(_pendingUseCount));
        sb.append("\n\tinUseCount:" + Integer.toString(_inUseCount));
        sb.append("\n\tcreatedByEnclaveManager:" + Boolean.toString(_createdByEnclaveManager));

        if (_stringToken == null) {
            _stringToken = toHexString(_token);
        }
        sb.append("\n\ttoken:" + _stringToken);
        sb.append("\n\tautoDelete:" + autoDelete);

        return sb.toString();
    }

    final static String digits = "0123456789abcdef";

    /**
     * Converts a byte array to a hexadecimal string.
     */
    public static String toHexString(byte[] b) {
        StringBuffer result = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            result.append(digits.charAt((b[i] >> 4) & 0xf));
            result.append(digits.charAt(b[i] & 0xf));
        }
        return (result.toString());
    }
}
