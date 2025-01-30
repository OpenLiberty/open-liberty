/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.cookie;

/**
 * Represents the allowable values for the SameSite attribute for HTTP
 * cookies. This defines three valid values:
 * 
 * {@link LAX}
 * {@link NONE} 
 * {@link STRICT}
 */
public enum SameSite {

    /**
     * Cookies are withheld on most cross-site requests, but could 
     * be sent when navigating between sites. 
     */
    LAX("Lax"),
    /** 
     * No restrictions on cross-site requests. However, most browsers
     * require the cookie to also be flagged as Secure. This enum
     * will return true for {@link #requiresSecure()}
     */
    NONE("None"),
    /**
     * Cookies are only sent in first-party context and not 
     * on cross-site requests.
     */
    STRICT("Strict");

    private final String name;

    SameSite(String name){
        this.name = name;
    }

    /**
     * Convert a String into a {@code SameSite} enum. This is treated
     * in a case-insenstive fashion. 
     * 
     * @param value the string to parse
     * @return the corresponding {@code SameSite} enum
     * @throws IllegalArgumentException if the input is null, empty, or
     * or doesn't match a supported SameSite value
     */
    public static SameSite from(String value){
        if(value == null || value.trim().isEmpty()){
            throw new IllegalArgumentException("SameSite value cannot be null or empty");
        }
        String normalized = value.trim().toLowerCase();
        switch(normalized){
            case "lax": return LAX;
            case "strict": return STRICT;
            case "none": return NONE;
            default: throw new IllegalArgumentException("Unknown SameSite value: " + value);
        }
    }

    /**
     * Converts a String into a {@code SameSite} enum. This is treated
     * in a case-insensitive fashion. If the value is null, empty, or
     * unrecognized, the default input is returned.
     * 
     * @param value the string to parse
     * @param defaultValue fallback enum if parsing fails
     * @return the parsed enum or the default if parsing failed
     */
    public static SameSite fromOrDefault(String value, SameSite defaultValue){
        if(value == null || value.trim().isEmpty()){
            return defaultValue;
        }
        try{
            return from(value);
        }catch(IllegalArgumentException e){
            return defaultValue;
        }
    }

    /**
     * Returns true if this SameSite enumeration requires the "Secure" cookie flag.
     * @return
     */
    public boolean requiresSecure(){
        return (this == NONE);
    }

    @Override
    public String toString(){
        return name;
    }
}
