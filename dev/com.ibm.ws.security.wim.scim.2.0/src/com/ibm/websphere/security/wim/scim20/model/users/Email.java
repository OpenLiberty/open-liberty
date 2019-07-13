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

package com.ibm.websphere.security.wim.scim20.model.users;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.users.EmailImpl;

/**
 * A single email address for a user.
 */
@JsonDeserialize(as = EmailImpl.class)
public interface Email {

    /**
     * Get a human-readable name, primarily used for display purposes.
     *
     * @return A human-readable name, primarily used for display purposes.
     */
    public String getDisplay();

    /**
     * Get a Boolean value indicating the 'primary' or preferred attribute value
     * for this attribute, e.g., the preferred mailing address or primary email
     * address. The primary attribute value 'true' MUST appear no more than
     * once.
     *
     * @return A Boolean value indicating the 'primary' or preferred attribute
     *         value for this attribute.
     */
    public Boolean getPrimary();

    /**
     * Get a label indicating the attribute's function, e.g., 'work' or 'home'.
     *
     * @return A label indicating the attribute's function, e.g., 'work' or
     *         'home'.
     */
    public String getType();

    /**
     * Get the email address for the user. The value SHOULD be canonicalized by
     * the service provider, e.g., 'bjensen@example.com' instead of
     * 'bjensen@EXAMPLE.COM'.
     *
     * @return The email address for the user.
     */
    public String getValue();

    /**
     * Set a human-readable name, primarily used for display purposes.
     *
     * @param display
     *            A human-readable name, primarily used for display purposes.
     */
    // TODO Description says READ-ONLY, but mutability is readWrite.
    public void setDisplay(String display);

    /**
     * Set a Boolean value indicating the 'primary' or preferred attribute value
     * for this attribute, e.g., the preferred mailing address or primary email
     * address. The primary attribute value 'true' MUST appear no more than
     * once.
     *
     * @param primary
     *            A Boolean value indicating the 'primary' or preferred
     *            attribute value for this attribute.
     */
    public void setPrimary(Boolean primary);

    /**
     * Set a label indicating the attribute's function, e.g., 'work' or 'home'.
     *
     * @param type
     *            A label indicating the attribute's function, e.g., 'work' or
     *            'home'.
     */
    public void setType(String type);

    /**
     * Set the email address for the user. The value SHOULD be canonicalized by
     * the service provider, e.g., 'bjensen@example.com' instead of
     * 'bjensen@EXAMPLE.COM'.
     *
     * @param value
     *            The email address for the user.
     */
    public void setValue(String value);
}
