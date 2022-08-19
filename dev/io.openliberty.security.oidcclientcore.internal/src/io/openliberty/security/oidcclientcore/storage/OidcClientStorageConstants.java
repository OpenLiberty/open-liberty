/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.storage;

public class OidcClientStorageConstants {

    public static final String WAS_OIDC_STATE_KEY = "WASOidcState";
    public static final String WAS_OIDC_NONCE = "WASOidcNonce";
    public static final String WAS_REQ_URL_OIDC = "WASReqURLOidc";

    public static final int DEFAULT_STATE_STORAGE_LIFETIME_SECONDS = 420;
    public static final int DEFAULT_REQ_URL_STORAGE_LIFETIME_SECONDS = DEFAULT_STATE_STORAGE_LIFETIME_SECONDS;

}
