/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.meta;

import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;

/**
 * Additional metadata sent from the client to the server.
 * <p>
 * All feature methods can accept this class as a parameter. It will be automatically injected before the
 * method is invoked.
 */
public interface Meta {

    /**
     *
     * @param key
     * @return the value for the given key, or {@code null}
     */
    Object getValue(MetaKey key, Jsonb jsonb);

    /**
     * If {@code _meta} is not present then an empty {@link JsonObject} is returned.
     *
     * @return the JSON representation of {@code _meta}, never {@code null}
     */
    JsonObject asJsonObject();

}