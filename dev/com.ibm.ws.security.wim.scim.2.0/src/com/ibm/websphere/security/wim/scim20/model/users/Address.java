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
import com.ibm.ws.security.wim.scim20.model.users.AddressImpl;

/**
 * A single physical mailing address for a user.
 */
@JsonDeserialize(as = AddressImpl.class)
public interface Address {

    /**
     * Get the country name component.
     *
     * @return The country name component.
     */
    public String getCountry();

    /**
     * Get the full mailing address, formatted for display or use with a mailing
     * label. This attribute MAY contain newlines.
     *
     * @return The full mailing address.
     */
    public String getFormatted();

    /**
     * Get the city or locality component.
     *
     * @return The city or locality component.
     */
    public String getLocality();

    /**
     * Get the zip code or postal code component.
     *
     * @return The zip code or postal code component.
     */
    public String getPostalCode();

    /**
     * Get the state or region component.
     *
     * @return The state or region component.
     */
    public String getRegion();

    /**
     * Get the full street address component, which may include house number,
     * street name, P.O. box, and multi-line extended street address
     * information. This attribute MAY contain newlines.
     *
     * @return The full street address component.
     */
    public String getStreetAddress();

    /**
     * Get the label indicating the address' function, e.g., 'work' or 'home'.
     *
     * @return The label indicating the address' function.
     */
    public String getType();

    /**
     * Set the country name component.
     *
     * @param country
     *            The country name component.
     */
    public void setCountry(String country);

    /**
     * Set the full mailing address.
     *
     * @param formatted
     *            The fully-formatted mailing address.
     */
    public void setFormatted(String formatted);

    /**
     * Set the city or locality component.
     *
     * @param locality
     *            The city or locality component.
     */
    public void setLocality(String locality);

    /**
     * Set the zip code or postal code component.
     *
     * @param postalCode
     *            The zip code or postal code component.
     */
    public void setPostalCode(String postalCode);

    /**
     * Set the state or region component.
     *
     * @param region
     *            The state or region component.
     */
    public void setRegion(String region);

    /**
     * Set the full street address component, which may include house number,
     * street name, P.O. box, and multi-line extended street address
     * information. This attribute MAY contain newlines.
     *
     * @param streetAddress
     *            The full street address component.
     */
    public void setStreetAddress(String streetAddress);

    /**
     * Set the label indicating the address' function, e.g., 'work' or 'home'.
     *
     * @param type
     *            The label indicating the address' function.
     */
    public void setType(String type);
}
