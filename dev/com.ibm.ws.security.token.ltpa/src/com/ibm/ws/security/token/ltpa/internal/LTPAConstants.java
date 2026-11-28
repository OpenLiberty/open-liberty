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

public class LTPAConstants {

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
     * Used to identify the primary PQC keys (RSA + ML-KEM) for LTPA3 tokens.
     * @deprecated Use {@link #PRIMARY_HYBRID_KEYS} instead
     */
    @Deprecated
    protected static final String PRIMARY_PQC_KEYS = "primary_pqc_keys";

    /**
     * Used to identify the primary hybrid keys (RSA + ML-DSA + ML-KEM) for LTPA3 tokens.
     */
    protected static final String PRIMARY_HYBRID_KEYS = "primary_hybrid_keys";

    /**
     * Used to identify the unique identifier of a user.
     */
    protected static final String UNIQUE_ID = "unique_id";
}
