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

package com.ibm.websphere.security.wim.scim20.model.extensions;

import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMGroupImpl;

// TODO Support all static properties
@JsonDeserialize(as = WIMGroupImpl.class)
public interface WIMGroup {
    public String getCn();

    public WIMIdentifier getIdentifier();

    public void setCn(String cn);

    /**
     * Returns an unmodifiable view of the extended properties and values for
     * the WIM group.
     *
     * @return The unmodifiable map of the WIM group's extended properties.
     */
    public Map<String, Object> getExtendedProperties();

    /**
     * Set a WIM group extended property's value.
     *
     * TODO List and test types that 'value' can be.
     *
     * @param property
     *            The extended property name to set the value for.
     * @param value
     *            The value of the extended property.
     */
    public void setExtendedProperty(String property, Object value);
}
