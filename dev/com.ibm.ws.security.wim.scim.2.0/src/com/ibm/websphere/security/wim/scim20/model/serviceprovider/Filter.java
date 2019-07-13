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
import com.ibm.ws.security.wim.scim20.model.serviceprovider.FilterImpl;

/**
 * Filter configuration options.
 */
@JsonDeserialize(as = FilterImpl.class)
public interface Filter {

    /**
     * Get an integer value specifying the maximum number of resources returned
     * in a response.
     *
     * @return An integer value specifying the maximum number of resources
     *         returned in a response.
     */
    public Integer getMaxResults();

    /**
     * Get a Boolean value specifying whether or not the operation is supported.
     *
     * @return A Boolean value specifying whether or not the operation is
     *         supported.
     */
    public Boolean getSupported();
}
