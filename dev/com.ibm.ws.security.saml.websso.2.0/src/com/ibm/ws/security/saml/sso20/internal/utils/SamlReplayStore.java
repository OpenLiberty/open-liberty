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
package com.ibm.ws.security.saml.sso20.internal.utils;

/**
 * A store of consumed SAML assertion identifiers. Implementations that are shared
 * between JVMs MUST make {@link #registerIfAbsent(String, long)} atomic across all participants.
 */
public interface SamlReplayStore {

    /**
     * Atomically record the key if it has not been recorded before.
     *
     * @param key opaque, already scoped key (provider + issuer + assertion id, hashed)
     * @param expiryMs epoch millis after which the entry is no longer needed
     *            (SubjectConfirmationData NotOnOrAfter; skew is applied by the caller)
     * @return true if the key was ALREADY present (a replay); false if it was newly registered
     * @throws SamlReplayStoreException if the store could not be consulted
     */
    boolean registerIfAbsent(String key, long expiryMs) throws SamlReplayStoreException;
}
