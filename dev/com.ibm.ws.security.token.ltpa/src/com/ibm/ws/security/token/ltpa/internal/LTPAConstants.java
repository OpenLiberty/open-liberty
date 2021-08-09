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
package com.ibm.ws.security.token.ltpa.internal;

public class LTPAConstants {

    /**
     * Used to identify the expiration limit of the LTPA2 token.
     */
    protected static final String EXPIRATION = "expiration";

    /**
     * Used to identify the LTPA shared key.
     */
    protected static final String SECRET_KEY = "ltpa_shared_key";

    /**
     * Used to identify the LTPA private key.
     */
    protected static final String PRIVATE_KEY = "ltpa_private_key";

    /**
     * Used to identify the LTPA public key.
     */
    protected static final String PUBLIC_KEY = "ltpa_public_key";

    /**
     * Used to identify the unique identifier of a user.
     */
    protected static final String UNIQUE_ID = "unique_id";
}
