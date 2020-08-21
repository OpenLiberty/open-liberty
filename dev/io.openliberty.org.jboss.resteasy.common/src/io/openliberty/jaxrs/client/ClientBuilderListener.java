/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jaxrs.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Registered ClientBuilderListeners (via DS) will be called when a
 * {@code javax.ws.rs.client.ClientBuilder} is invoked allowing
 * customization of the resulting {@code javax.ws.rs.client.Client}
 * instance.
 */
@Sensitive
public interface ClientBuilderListener {
    static final TraceComponent tc = Tr.register(ClientBuilderListener.class);

    default void building(ClientBuilder clientBuilder) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "building - no-op");
        }
    }

    default void built(Client client) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "built - no-op");
        }
    }
}
