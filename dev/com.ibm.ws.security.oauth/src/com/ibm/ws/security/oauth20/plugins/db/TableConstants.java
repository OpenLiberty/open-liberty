/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.db;

public class TableConstants {

    /*
     * Table name and columns for the OAuth 1.0 token cache
     */
    final static String TABLE_OC1 = "OAuthDBSchema.OAUTH10CACHE";

    final static String COL_OC1_LOOKUPKEY = "LOOKUPKEY";
    final static String COL_OC1_COMPONENTID = "COMPONENTID";
    final static String COL_OC1_TYPE = "TYPE";
    final static String COL_OC1_CREATEDAT = "CREATEDAT";
    final static String COL_OC1_LIFETIME = "LIFETIME";
    final static String COL_OC1_EXPIRES = "EXPIRES";
    final static String COL_OC1_TOKENSTRING = "TOKENSTRING";
    final static String COL_OC1_CLIENTID = "CLIENTID";
    final static String COL_OC1_SECRET = "SECRET";
    final static String COL_OC1_USERNAME = "USERNAME";
    final static String COL_OC1_SCOPE = "SCOPE";
    final static String COL_OC1_CALLBACK = "CALLBACK";
    final static String COL_OC1_STATEID = "STATEID";
    final static String COL_OC1_VERIFIER = "VERIFIER";
    final static String COL_OC1_REALM = "REALM";

    /*
     * Table name and columns for the OAuth 1.0 nonce cache
     */
    final static String TABLE_ON1 = "OAuthDBSchema.OAUTH10NONCE";

    final static String COL_ON1_LOOKUPKEY = "LOOKUPKEY";
    final static String COL_ON1_CREATEDAT = "CREATEDAT";
    final static String COL_ON1_LIFETIME = "LIFETIME";
    final static String COL_ON1_EXPIRES = "EXPIRES";

    /*
     * Table name and columns for the OAuth 1.0 external client configuration
     * data
     */
    final static String TABLE_CC1 = "OAuthDBSchema.OAUTH10CLIENTCONFIG";

    final static String COL_CC1_COMPONENTID = "COMPONENTID";
    final static String COL_CC1_CLIENTID = "CLIENTID";
    final static String COL_CC1_CLIENTSECRET = "CLIENTSECRET";
    final static String COL_CC1_DISPLAYNAME = "DISPLAYNAME";
    final static String COL_CC1_CALLBACKURI = "CALLBACKURI";
    final static String COL_CC1_ALLOWCALLBACKOVERRIDE = "ALLOWCALLBACKOVERRIDE";
    final static String COL_CC1_ENABLED = "ENABLED";

    final static String COL_CC2_COMPONENTID = "COMPONENTID";
    final static String COL_CC2_CLIENTID = "CLIENTID";
    final static String COL_CC2_CLIENTSECRET = "CLIENTSECRET";
    final static String COL_CC2_DISPLAYNAME = "DISPLAYNAME";
    final static String COL_CC2_REDIRECTURI = "REDIRECTURI";
    final static String COL_CC2_ENABLED = "ENABLED";
    final static String COL_CC2_CLIENTMETADATA = "CLIENTMETADATA";

    final static String COL_OC2_LOOKUPKEY = "LOOKUPKEY";
    final static String COL_OC2_UNIQUEID = "UNIQUEID";
    final static String COL_OC2_COMPONENTID = "COMPONENTID";
    final static String COL_OC2_TYPE = "TYPE";
    final static String COL_OC2_SUBTYPE = "SUBTYPE";
    final static String COL_OC2_CREATEDAT = "CREATEDAT";
    final static String COL_OC2_LIFETIME = "LIFETIME";
    final static String COL_OC2_EXPIRES = "EXPIRES";
    final static String COL_OC2_TOKENSTRING = "TOKENSTRING";
    final static String COL_OC2_CLIENTID = "CLIENTID";
    final static String COL_OC2_USERNAME = "USERNAME";
    final static String COL_OC2_SCOPE = "SCOPE";
    final static String COL_OC2_REDIRECTURI = "REDIRECTURI";
    final static String COL_OC2_STATEID = "STATEID";
    final static String COL_OC2_EXTENDEDFIELDS = "EXTENDEDFIELDS";

    /*
     * Table name and columns for the consent cache data
     */
    final static String TABLE_CONSENT = "OAuthDBSchema.OAUTH20CONSENTCACHE";

    /**
     * The client id
     */
    final static String COL_CONSENT_CLIENT_ID = "CLIENTID";
    /**
     * The redirect URI
     */
    final static String COL_CONSENT_USER = "USERID";
    /**
     * The duration of time (in milliseconds) to exist in the cache
     */
    final static String COL_CONSENT_EXPIRES = "EXPIRES";
    /**
     * The time in which the entry was added to the cache
     */
    final static String COL_CONSENT_TIMESTAMP = "tc_TIMESTAMP";

    final static String COL_CONSENT_SCOPE = "SCOPE";

    // final static String COL_CONSENT_RESOURCE_ID = "RESOURCEID";

    final static String COL_CONSENT_PROVIDER_ID = "PROVIDERID";

    final static String COL_CONSENT_EXTENDEDFIELDS = "EXTENDEDFIELDS";
}
