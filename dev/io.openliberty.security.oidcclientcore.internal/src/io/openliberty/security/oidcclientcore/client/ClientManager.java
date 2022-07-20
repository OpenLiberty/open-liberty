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
package io.openliberty.security.oidcclientcore.client;

/**
 * Creates, stores, and retrieves OIDC clients based on the OidcClientConfig.
 */
public class ClientManager {

    public static Client getClientFor(OidcClientConfig oidcClientConfig) {
        // TODO: Lookup from a client store.
        return new Client(oidcClientConfig);
    }

}