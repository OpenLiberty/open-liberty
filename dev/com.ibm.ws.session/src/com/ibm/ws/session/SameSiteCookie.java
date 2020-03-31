/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

/**
 * An Enum that contains the valid values for the <httpSession cookieSameSite/> configuration.
 */
public enum SameSiteCookie {
    
    LAX("Lax"),
    STRICT("Strict"),
    NONE("None"),
    DISABLED("Disabled"); 
    
    private final String sameSiteCookieValue;
    
    private SameSiteCookie(String value) {
        sameSiteCookieValue = value;
    }
    
    /**
     * Returns the value to be used for the Cookie SameSite attribute.
     * 
     * @return
     */
    public String getSameSiteCookieValue() {
        return sameSiteCookieValue;
    }
    
    /**
     * Any value passed to this method is compared to the SameSiteCookies ignoring case and 
     * the appropriate SameSiteCookie is returned. If no matches are found then DISABLED is
     * returned.
     * 
     * @param value - The value to check against the SameSiteCookie.
     * @return - The SameSiteCookie that matches the value.
     */
    public static SameSiteCookie get(String value) {
        if (LAX.toString().equalsIgnoreCase(value)) {
            return LAX;
        } else if (STRICT.toString().equalsIgnoreCase(value)) {
            return STRICT;
        } else if (NONE.toString().equalsIgnoreCase(value)) {
            return NONE;           
        }

        return DISABLED;
    }
}
