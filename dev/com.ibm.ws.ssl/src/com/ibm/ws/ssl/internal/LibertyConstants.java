/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.internal;

import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 *
 */
public interface LibertyConstants {

    static final String KEY_ID = "id";
    static final String KEY_DEFAULT_REPERTOIRE = "sslRef";
    static final Object KEY_OUTBOUND_DEFAULT_REPERTOIRE = "outboundSSLRef";
    static final String DEFAULT_SSL_CONFIG_ID = "defaultSSLConfig";

    static final String KEY_KEYSTORE_REF = "keyStoreRef";
    static final String DEFAULT_KEYSTORE_REF_ID = "defaultKeyStore";

    static final String KEY_TRUSTSTORE_REF = "trustStoreRef";

    static final String DEFAULT_KEY_STORE_FILE = "key.jks";
    static final String DEFAULT_CONFIG_LOCATION = WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR + "resources/security/";
    static final String DEFAULT_OUTPUT_LOCATION = WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR + "resources/security/";
    static final String DEFAULT_TYPE = "JKS";
    static final String DEFAULT_KEY_STORE_NAME = "defaultKeyStore";

    static final String KEY_KEYSTORE_LOCATION = "location";
    static final String KEY_DEFAULT_KEYSTORE_LOCATION = "defaultLocation";

    static final String SSLPROP_OUTBOUND_DEFAULT_ALIAS = "com.ibm.ws.ssl.outboundDefaultAlias";
}
