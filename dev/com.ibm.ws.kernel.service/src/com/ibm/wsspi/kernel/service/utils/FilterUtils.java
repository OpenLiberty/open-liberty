/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

/**
 * Create an OSGi filter from a name value pair.
 * 
 * Use {@link #createPropertyFilter(String, String)} when the name is known
 * to be valid. Use {@link #createValidPropertyFilter(String, String)} if the
 * name must be validated.
 */
public class FilterUtils {

    public static final String FORBIDDEN_NAME_CHARS = "=><~()";

    public static final boolean isForbiddenNameChar(char c) {
        return ( (c == '=') ||
                 (c == '>') || (c == '<') ||
                 (c == '~') ||
                 (c == '(') || (c == ')') );
    }    
    
    public static final boolean isValidName(String name) {
        int nameLen = name.length();
        if ( nameLen == 0 ) {
            return false;
        }
        
        for ( int nextChar = 0; nextChar < nameLen; nextChar++ ) {
            if ( isForbiddenNameChar( name.charAt(nextChar) ) ) {
                return false;
            }
        }
        
        return true;
    }

    public static final String VALUE_SPECIAL_CHARS = "\\*()";
    
    public static final boolean isSpecialValueChar(char c) {
        return ( (c == '\\') || (c == '*') || (c == '(') || (c == ')') );
    }

    /**
     * Creates a filter string that matches an attribute value exactly.
     * Characters in the value with special meaning will be escaped.
     * The filter value is:
     * <code>
     *     '(' + name + '=' + escaped_value + ')'
     * </code>
     * 
     * Where the <code>escaped_value</code> is the value parameter with
     * escape characters inserted before each special character.
     *
     * The name parameter is not validated.
     * 
     * Null and empty values are allowed.
     * 
     * @param name A valid attribute name.
     * @param value An attribute value.
     * 
     * @return A property filter composed from the name and value.
     */
    @SuppressWarnings("null")
    public static String createPropertyFilter(String name, String value) {
        int valueLen = ( (value == null) ? 0 : value.length() );

        int numEscapes = 0;

        if ( valueLen > 0 ) {
            for (int nextOffset = 0; nextOffset < valueLen; nextOffset++ ) {
                if ( isSpecialValueChar( value.charAt(nextOffset) ) ) {
                    numEscapes++;
                }
            }
        }

        // Allocate for '(' + name + '=' + value + ')'
        // plus extra for escape characters.

        StringBuilder builder = new StringBuilder( 1 + name.length() + 1 + valueLen + numEscapes + 1 );

        builder.append( '(' );
        builder.append( name );
        builder.append( '=' );

        if ( valueLen > 0 ) {
            int tailOffset = 0;

            // Insert escape characters before each special character.
            // Stop checking when all of the escapes have been processed.

            if ( numEscapes != 0 ) {
                for ( int nextOffset = 0; nextOffset < valueLen; nextOffset++ ) {
                    if ( !isSpecialValueChar( value.charAt(nextOffset) ) ) {
                        continue;
                    }

                    builder.append(value, tailOffset, nextOffset);
                    tailOffset = nextOffset;

                    builder.append('\\');

                    if ( --numEscapes == 0 ) {
                        break;
                    }
                }
            }

            builder.append(value, tailOffset, valueLen);
        }

        builder.append(')');

        return builder.toString();
    }

    /**
     * Create a property filter value. Validate the name.
     * 
     * The property filter is composed using {@link #createPropertyFilter(String, String)}.
     * 
     * See {@link #isValidName(String)} for validation details.
     * 
     * @param name A name value. Must not be empty and must not have any
     *     forbidden characters.
     * @param value A value. May be null or empty.
     * 
     * @return The filter composed from the name and value.
     *
     * @throws IllegalArgumentException Thrown if the name is not valid.
     */
    public static String createValidPropertyFilter(String name, String value) {
        if ( !FilterUtils.isValidName(name) )  {
            throw new IllegalArgumentException(
                "Non-valid filter keyword [ " + name + " ] supplied with value [ " + value + " ]" );
        }
        return createPropertyFilter(name, value);
    }
}
