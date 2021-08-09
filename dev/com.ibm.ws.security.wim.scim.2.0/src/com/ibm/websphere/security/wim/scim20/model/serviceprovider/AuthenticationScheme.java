/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.wim.scim20.model.serviceprovider;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.serviceprovider.AuthenticationSchemeImpl;

/**
 * A single supported authentication scheme.
 */
@JsonDeserialize(as = AuthenticationSchemeImpl.class)
public interface AuthenticationScheme {
    /**
     * Get a description of the authentication scheme.
     *
     * @return A description of the authentication scheme.
     */
    public String getDescription();

    /**
     * Get an HTTP-addressable URL pointing to the authentication scheme's usage
     * documentation.
     *
     * @return An HTTP-addressable URL pointing to the authentication scheme's
     *         usage documentation.
     */
    public String getDocumentationUri();

    /**
     * Get the common authentication scheme name, e.g., HTTP Basic.
     *
     * @return The common authentication scheme name.
     */
    public String getName();

    /**
     * Get an HTTP-addressable URL pointing to the authentication scheme's
     * specification.
     *
     * @return An HTTP-addressable URL pointing to the authentication scheme's
     *         specification.
     */
    public String getSpecUri();

    /**
     * Get the authentication scheme.
     *
     * @return The authentication scheme.
     */
    public String getType();
}
