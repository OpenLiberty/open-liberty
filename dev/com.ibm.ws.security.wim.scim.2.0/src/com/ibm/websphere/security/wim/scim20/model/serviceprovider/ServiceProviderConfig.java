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

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.serviceprovider.ServiceProviderConfigImpl;

/**
 * The service provider's configuration.
 */
@JsonDeserialize(as = ServiceProviderConfigImpl.class)
public interface ServiceProviderConfig {
    /**
     * Get the list of supported authentication schemes.
     *
     * @return The list of supported authentication schemes.
     */
    public List<AuthenticationScheme> getAuthenticationSchemes();

    /**
     * Get bulk configuration options.
     *
     * @return Bulk configuration options
     */
    public Bulk getBulk();

    /**
     * Get configuration options related to changing a password.
     *
     * @return Configuration options related to changing a password.
     */
    public ChangePassword getChangePassword();

    /**
     * Get an HTTP-addressable URL pointing to the service provider's
     * human-consumable help documentation.
     *
     * @return An HTTP-addressable URL pointing to the service provider's
     *         human-consumable help documentation.
     */
    public String getDocumentationUri();

    /**
     * Get ETag configuration options.
     *
     * @return ETag configuration options.
     */
    public ETag getEtag();

    /**
     * Get filter options
     *
     * @return Filter options
     */
    public Filter getFilter();

    /**
     * Get PATCH configuration options.
     *
     * @return PATCH configuration options.
     */
    public Patch getPatch();

    /**
     * Get sort configuration options.
     *
     * @return Sort configuration options.
     */
    public Sort getSort();
}
