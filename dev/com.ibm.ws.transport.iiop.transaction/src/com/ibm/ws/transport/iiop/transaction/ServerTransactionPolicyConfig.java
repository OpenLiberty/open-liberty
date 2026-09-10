/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.transaction;

import java.io.Serializable;

/**
 * Lightweight, truly serializable configuration for server transaction policy.
 *
 * <p>This class contains only configuration data (primitives and immutable objects),
 * making it genuinely serializable as required by the CORBA specification.
 *
 * <p>Services (TransactionManager, RemoteTransactionController, import handlers) are
 * accessed at request time via {@code ServiceCaller<TransactionHandlerContext>} in the
 * interceptors — not stored in this config.
 *
 * <p>This design provides clean separation of concerns:
 * <ul>
 *   <li>Policy configuration (this class) — what to do</li>
 *   <li>Service access ({@code ServiceCaller}) — how to reach the runtime services</li>
 * </ul>
 */
public class ServerTransactionPolicyConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final boolean transactionImportEnabled;
    private final int timeoutSeconds;
    
    /**
     * Creates a new server transaction policy configuration.
     * 
     * @param transactionImportEnabled whether transaction import is enabled
     * @param timeoutSeconds timeout for transaction operations in seconds
     */
    public ServerTransactionPolicyConfig(boolean transactionImportEnabled, int timeoutSeconds) {
        this.transactionImportEnabled = transactionImportEnabled;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    /**
     * Creates a default configuration with transaction import enabled.
     */
    public ServerTransactionPolicyConfig() {
        this(true, 30);  // Default: enabled, 30 second timeout
    }
    
    /**
     * Checks if transaction import is enabled.
     * 
     * @return true if transaction import is enabled, false otherwise
     */
    public boolean isTransactionImportEnabled() {
        return transactionImportEnabled;
    }
    
    /**
     * Gets the timeout for transaction operations.
     * 
     * @return timeout in seconds
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    @Override
    public String toString() {
        return "ServerTransactionPolicyConfig[enabled=" + transactionImportEnabled + 
               ", timeout=" + timeoutSeconds + "s]";
    }
}

// Made with Bob
