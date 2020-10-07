/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.oauth20.client;

import com.google.gson.JsonArray;

/**
 * This interface represents a registered OAuth 2.0 client.
 */
public interface OAuth20Client {
    /**
     * @return the client's unique identifier
     */
    public String getClientId();

    /**
     * @return the client's secret
     */
    public String getClientSecret();

    /**
     * @return the client's client name (display)
     */
    public String getClientName();

    /**
     * @return the client's registered redirect URI
     */
    public JsonArray getRedirectUris();

    /**
     * @return TRUE if the client is enabled, FALSE otherwise
     */
    public boolean isEnabled();

    /**
     * This is an extension function which returns an array of property values.
     *
     * A client provider should make available the following extension
     * properties:
     *
     * - At time of release of V1.0 no extension properties are expected.
     * Your implementation should return null.
     *
     * @return the property values of the client that match the property name,
     *         or null if there are no property values for the specified
     *         property name.
     */
    public String[] getExtensionProperty(String propertyName);

    /**
     * @return a string representation of this client, used to trace the
     *         client's member variables
     */
    @Override
    public String toString();

    /**
     * set to allow regular expressions in redirect uris'
     * @param value
     */
    public void setAllowRegexpRedirects(boolean value);

    /**
     * return if regular expression in redirect uri's are allowed.
     * @return
     */
    public boolean getAllowRegexpRedirects();

}
