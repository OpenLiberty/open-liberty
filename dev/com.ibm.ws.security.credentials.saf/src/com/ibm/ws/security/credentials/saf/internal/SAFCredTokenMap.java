/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.credentials.saf.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * Maps SAFCredential -> SAFCredentialToken.
 *
 * The SAFCredential is what we give out to external callers, for populating in the Subject.
 * The SAFCredentialToken contains the native token that indirectly references the RACO. Whenever
 * the SAFCredential is used for authorization, the authz service uses this map to lookup the
 * native token (RACO) to use for native services (RACROUTE).
 *
 * There's also a mapping from SAFCredentialTokenKey (a String) -> SAFCredential. This is
 * used on the setCredential() call. The serialized token key embedded in the securityId that
 * gets passed back to SAFAuthorizedRegistry callers (checkPassword/createCertificate). The
 * serialized key is placed in a Map inside the Subject and used on setCredential to lookup
 * the appropriate SAFCredential for the given Subject.
 *
 * We don't put the SAFCredentialToken directly in the SAFCredential (but instead maintain
 * the mapping of SAFCredentials to SAFCredentialTokens internally) for a few reasons:
 *
 * 1) The SAFCredential may get serialized or externalized, and we don't want the token serialized with it.
 * 2) The SAFCredential and SAFCredentialToken have independent lifecycles.
 *
 * When an external caller wishes to perform some SAF service against a credential, they
 * pass us the SAFCredential, which we use to look up the SAFCredentialToken in this Map.
 * The SAFCredentialToken is then passed to the SAF service.
 *
 * Note: extends ConcurrentHashMap as multiple threads access/update the map concurrently.
 */
public class SAFCredTokenMap extends ConcurrentHashMap<SAFCredential, SAFCredentialToken> {

    /**
     * Secondary mapping of SAFCredentialToken keys to SAFCredentials.
     */
    private final Map<String, SAFCredential> safCredTokenKeys = new ConcurrentHashMap<String, SAFCredential>();

    /**
     * The reaper manages the size of the SAFCredTokenMap. It periodically reaps
     * the oldest entries from the map.
     */
    private final SAFCredTokenMapReaper reaper;

    /**
     * CTOR.
     *
     * Takes a SAFCredentialsService, which it passes on to the SAFCredTokenMapReaper
     * (the reaper will call SAFCredentialsService.deleteCredential directly).
     */
    public SAFCredTokenMap(SAFCredentialsServiceImpl safCredentialsService) {
        reaper = new SAFCredTokenMapReaper(this, safCredentialsService);
    }

    /**
     * Find the SAFCredential with the given SAFCredentialToken key.
     */
    protected SAFCredential getCredential(String key) {
        return safCredTokenKeys.get(key);
    }

    /**
     * Map the given SAFCredential to the given SAFCredentialToken, and also
     * map the SAFCredentialToken key to the SAFCredential.
     */
    @Override
    public SAFCredentialToken put(SAFCredential safCred, SAFCredentialToken safCredToken) {

        // Reap old entries if the map gets too big.
        if (reaper.shouldRunReaper()) {
            reaper.runReaper();
        }

        safCredTokenKeys.put(safCredToken.getKey(), safCred);
        return super.put(safCred, safCredToken);
    }

    /**
     * Remove the given SAFCredential key from the map, and also remove the
     * SAFCredentialToken key from the keys map.
     */
    @Override
    public SAFCredentialToken remove(Object safCred) {
        SAFCredentialToken safCredToken = super.remove(safCred);
        if (safCredToken != null) {
            safCredTokenKeys.remove(safCredToken.getKey());
        }
        return safCredToken;
    }

}