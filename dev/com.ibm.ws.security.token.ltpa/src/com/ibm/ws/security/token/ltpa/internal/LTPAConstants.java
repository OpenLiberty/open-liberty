/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa.internal;

/**
 * Constants used for LTPA token configuration and factory initialization.
 *
 * <p>This class defines configuration keys for both classical LTPA tokens
 * and hybrid PQC-enhanced tokens.</p>
 *
 * @since 1.0
 */
public class LTPAConstants {

    // ========================================================================
    // Classical LTPA Configuration Keys
    // ========================================================================

    /**
     * Used to identify the expiration limit of the LTPA2 token.
     */
    protected static final String EXPIRATION = "expiration";

    /**
     * Used to identify the primary LTPA shared key.
     */
    protected static final String PRIMARY_SECRET_KEY = "primary_ltpa_shared_key";

    /**
     * Used to identify the primary LTPA private key.
     */
    protected static final String PRIMARY_PRIVATE_KEY = "primary_ltpa_private_key";

    /**
     * Used to identify the primary LTPA public key.
     */
    protected static final String PRIMARY_PUBLIC_KEY = "primary_ltpa_public_key";

    /**
     * Used to identify the validation LTPA keys
     */
    public static final String VALIDATION_KEYS = "ltpa_validation_keys";

    /**
     * Used to identify the unique identifier of a user.
     */
    protected static final String UNIQUE_ID = "unique_id";
    
    // ========================================================================
    // PQC Configuration Keys
    // ========================================================================
    
    /**
     * Used to identify whether PQC is enabled for LTPA tokens.
     * Value type: Boolean
     * Default: false
     */
    protected static final String PQC_ENABLED = "pqc_enabled";
    
    /**
     * Used to identify the PQC algorithm for hybrid LTPA tokens.
     * Value type: String
     * Valid values: "ML-DSA-65", "ML-DSA-87"
     * Default: "ML-DSA-65"
     */
    protected static final String PQC_ALGORITHM = "pqc_algorithm";
    
    /**
     * Used to identify the primary PQC private key.
     * Value type: java.security.PrivateKey
     */
    protected static final String PQC_PRIVATE_KEY = "pqc_private_key";
    
    /**
     * Used to identify the primary PQC public key.
     * Value type: java.security.PublicKey
     */
    protected static final String PQC_PUBLIC_KEY = "pqc_public_key";
}
