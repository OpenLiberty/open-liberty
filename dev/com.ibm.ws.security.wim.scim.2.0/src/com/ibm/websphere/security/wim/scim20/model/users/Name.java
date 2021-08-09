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
import com.ibm.ws.security.wim.scim20.model.users.NameImpl;

/**
 * The components of a user's real name.
 */
@JsonDeserialize(as = NameImpl.class)
public interface Name {

    /**
     * Get the family name of the user, or last name in most Western languages
     * (e.g., 'Jensen' given the full name 'Ms. Barbara J Jensen, III').
     *
     * @return The family name of the user.
     */
    public String getFamilyName();

    /**
     * Get the full name, including all middle names, titles, and suffixes as
     * appropriate, formatted for display (e.g., 'Ms. Barbara J Jensen, III').
     *
     * @return The full name, formatted for display.
     */
    public String getFormatted();

    /**
     * Get the given name of the user, or first name in most Western languages
     * (e.g., 'Barbara' given the full name 'Ms. Barbara J Jensen, III').
     *
     * @return The given name of the user.
     */
    public String getGivenName();

    /**
     * Get the honorific prefix(es) of the user, or title in most Western
     * languages (e.g., 'Ms.' given the full name 'Ms. Barbara J Jensen, III').
     *
     * @return The honorific prefix(es) of the user.
     */
    public String getHonorificPrefix();

    /**
     * Get the honorific suffix(es) of the user, or suffix in most Western
     * languages (e.g., 'III' given the full name 'Ms. Barbara J Jensen, III').
     *
     * @return The honorific suffix(es) of the user.
     */
    public String getHonorificSuffix();

    /**
     * Get the middle name(s) of the User (e.g., 'Jane' given the full name 'Ms.
     * Barbara J Jensen, III').
     *
     * @return The middle name(s) of the user.
     */
    public String getMiddleName();

    /**
     * Set the family name of the user, or last name in most Western languages
     * (e.g., 'Jensen' given the full name 'Ms. Barbara J Jensen, III').
     *
     * @param familyName
     *            The family name of the user.
     */
    public void setFamilyName(String familyName);

    /**
     * Set the full name, including all middle names, titles, and suffixes as
     * appropriate, formatted for display (e.g., 'Ms. Barbara J Jensen, III').
     *
     * @param formatted
     *            The full name, formatted for display.
     */
    public void setFormatted(String formatted);

    /**
     * Set the given name of the user, or first name in most Western languages
     * (e.g., 'Barbara' given the full name 'Ms. Barbara J Jensen, III').
     *
     * @return The given name of the user.
     */
    public void setGivenName(String givenName);

    /**
     * Set the honorific prefix(es) of the user, or title in most Western
     * languages (e.g., 'Ms.' given the full name 'Ms. Barbara J Jensen, III').
     *
     * @param honorificSuffix
     *            The honorific prefix(es) of the user.
     */
    public void setHonorificPrefix(String honorificSuffix);

    /**
     * Set the honorific suffix(es) of the user, or suffix in most Western
     * languages (e.g., 'III' given the full name 'Ms. Barbara J Jensen, III').
     *
     * @param honorificPrefix
     *            The honorific suffix(es) of the user.
     */
    public void setHonorificSuffix(String honorificPrefix);

    /**
     * Set the middle name(s) of the User (e.g., 'Jane' given the full name 'Ms.
     * Barbara J Jensen, III').
     *
     * @param middleName
     *            The middle name(s) of the user.
     */
    public void setMiddleName(String middleName);
}
