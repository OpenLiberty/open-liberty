/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.oidcclientcore.discovery;

public class OidcDiscoveryConstants {

    public static final String METADATA_KEY_AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    public static final String METADATA_KEY_TOKEN_ENDPOINT = "token_endpoint";
    public static final String METADATA_KEY_USERINFO_ENDPOINT = "userinfo_endpoint";
    public static final String METADATA_KEY_JWKS_URI = "jwks_uri";
    public static final String METADATA_KEY_ENDSESSION_ENDPOINT = "end_session_endpoint";
    public static final String METADATA_KEY_ISSUER = "issuer";
    public static final String METADATA_KEY_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = "id_token_signing_alg_values_supported";
    public static final String METADATA_KEY_USER_INFO_SIGNING_ALG_VALUES_SUPPORTED = "userinfo_signing_alg_values_supported";

    public static final String WELL_KNOWN_SUFFIX = ".well-known/openid-configuration";

}
