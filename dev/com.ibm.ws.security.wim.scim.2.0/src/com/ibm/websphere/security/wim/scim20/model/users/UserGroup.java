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
import com.ibm.ws.security.wim.scim20.model.users.UserGroupImpl;

/**
 * A single group to which the user belongs, either through direct membership,
 * through nested groups, or dynamically calculated.
 */
@JsonDeserialize(as = UserGroupImpl.class)
public interface UserGroup {

    /**
     * Get a human-readable name, primarily used for display purposes.
     *
     * @return A human-readable name, primarily used for display purposes
     */
    public String getDisplay();

    /**
     * Get the URI of the corresponding 'Group' resource to which the user
     * belongs.
     *
     * @return The URI of the corresponding 'Group' resource to which the user
     *         belongs.
     */
    public String getRef();

    /**
     * Get a label indicating the attribute's function, e.g., 'direct' or
     * 'indirect'.
     *
     * @return A label indicating the attribute's function.
     */
    public String getType();

    /**
     * Get the identifier of the user's group.
     *
     * @return The identifier of the user's group.
     */
    public String getValue();
}
