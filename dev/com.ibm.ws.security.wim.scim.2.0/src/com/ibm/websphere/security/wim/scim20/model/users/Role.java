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
import com.ibm.ws.security.wim.scim20.model.users.RoleImpl;

/**
 * A single role for a user that represents who the user is.
 */
@JsonDeserialize(as = RoleImpl.class)
public interface Role {

    /**
     * Get the value of a role.
     *
     * @return The value of a role.
     */
    public String getValue();

    /**
     * Set the value of a role.
     *
     * @param value
     *            The value of a role.
     */
    public void setValue(String value);

    /**
     * Get a human-readable name, primarily used for display purposes.
     *
     * @return A human-readable name, primarily used for display purposes.
     */
    public String getDisplay();

    /**
     * Set a human-readable name, primarily used for display purposes.
     *
     * @param display
     *            A human-readable name, primarily used for display purposes.
     */
    // TODO Description says READ-ONLY, but mutability is readWrite.
    public void setDisplay(String display);

    /**
     * Get a label indicating the attribute's function.
     *
     * @return A label indicating the attribute's function.
     */
    public String getType();

    /**
     * Set a label indicating the attribute's function.
     *
     * @param type
     *            A label indicating the attribute's function.
     */
    public void setType(String type);

    /**
     * Get a Boolean value indicating the 'primary' or preferred attribute value
     * for this attribute. The primary attribute value 'true' MUST appear no
     * more than once.
     *
     * @return A Boolean value indicating the 'primary' or preferred attribute
     *         value for this attribute
     */
    public Boolean getPrimary();

    /**
     * Set a Boolean value indicating the 'primary' or preferred attribute value
     * for this attribute. The primary attribute value 'true' MUST appear no
     * more than once.
     *
     * @param primary
     *            A Boolean value indicating the 'primary' or preferred
     *            attribute value for this attribute
     */
    public void setPrimary(Boolean primary);
}
