/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.channel.utils;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 * Processes and validates HTTP header names and values in compliance with 
 * RFC 7230, "Hypertext Transfer Protocol (HTTP/1.1): Message Syntax 
 * and Routing"
 * 
 * This utility ensures:
 *      Header names are composed of valid "tchar" characters.
 *      Header names and values do not exceed a configured maximum field length.
 *      Control characters (except for valid folding) and non-ASCII characters
 *          are properly handled.
 *      When following a folding sequence, CR must be followed by LF, and LF 
 *          must be followed by a whitespace.
 *      Trailing CR of LF is disallowed. 
 * 
 * The class also normalizes header names by trimming leading/trailing
 * whitespaces from both names and values.
 */
public class HeaderValidator {
    
        private static final char CR = '\r';
        private static final char LF = '\n';
        private static final char TAB = '\t';
        private static final char SPACE = ' ';
    
        /** 
         * Enumerates if the field token being processed is a header name or header value.
        */
        public enum FieldType{NAME, VALUE}
    
        private HeaderValidator() {
            //Utility Singleton
        }
    
    
        /**
         * Peforms processing of a header field (name or value).
         * 
         * This method normalizes a header field:
         *      Ensures a non-null input if field type is {@link FieldType#NAME}.
         *      Trims the input if it is non-null.
         *      Substitutes a null input with an empty string if the field type is
         *          {@link FieldType#VALUE}.
         *      
         * NOTE -> Should seek approval for:
         *  Lowercases the token if the field type is {@link FieldType#NAME}
         * 
         * @param token a raw header field token; may be {@code null} for values, 
         *      but not for names.
         * @param type whether this token is a header name or value
         * @param config the HTTP configuration object
         * @return processed and possibly normalized header field, ensuring to 
         *      comply with the configured validation requirements
         * @throws IllegalArgumentException if the field is invalid (too long, 
         *      contains illegal characters, or null name)
         */
        public static String process(String token, FieldType type, HttpChannelConfig config){
    
            if(type == FieldType.NAME && token == null){
                throw new IllegalArgumentException("Header name must not be null");
            }
            
            // For addHeader (appendHeader), null values should throw IllegalArgumentException
            // to match CHFW behavior - but only when validation is enabled
            if(type == FieldType.VALUE && token == null && config.isHeaderValidationEnabled()){
                throw new IllegalArgumentException("Null input provided: " + token);
            }
            
            String normalized = (token == null) ? "": token.trim();
            
            // For header names with validation enabled, we need to call normalizeHeaderName
            // to match CHFW behavior which validates through HttpHeaderKeys.find()
            if(type == FieldType.NAME && token != null && config.isHeaderValidationEnabled()){
               if(token.isEmpty() || normalized.isEmpty()){
                   // Empty or whitespace-only name - validate the original to get CHFW exception
                   // This will throw StringIndexOutOfBoundsException for empty strings
                   // or IllegalArgumentException for whitespace-only strings
                   normalizeHeaderName(token);
               } else {
                   // Normal case - normalize the trimmed name
                   normalized = normalizeHeaderName(normalized);
               }
            }
        
            return validate(normalized, type, config);
    
    }

    /**
     * Validates and cleans a header field:
     *  Returns token as-is if validation is disabled.
     *  Checks that a header name token is complaint with the RFC "tchar" pattern.
     *  Disallows trailing CR or LF characters.
     *  Ensures that an inline CR is followed by LF, and LF is followed by space or tab.
     *  Replaces valid CR or LF (folding sequence) with space character.
     *  For character outside of the printable ASCII range, if the masked code point 
     *      is CR or LF, it is replaced with '?'.
     * @param token the header field test after it has been normalized
     * @param type  whether this token is a header name or a header value
     * @param config the HTTP configuration object
     * @return the validated (and clean) header field
     * @throws IllegalArgumentException if the field fails one of the checks
     */
    private static String validate(String token, FieldType type, HttpChannelConfig config){

        if(!config.isHeaderValidationEnabled()){
            return token;
        }
        
        if(!token.isEmpty()){
            char lastChar = token.charAt(token.length()-1);
            if(lastChar == CR || lastChar == LF){
                throw new IllegalArgumentException("Illegal trailing EOL in header field: " + token);
            }
        }

        StringBuilder sb = new StringBuilder(token.length());
        String error = null;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);

            if (type == FieldType.NAME && !HttpHeaderKeys.isValidTchar(c)) {
                error = "Invalid header name: " + token;
            } else if (c == CR) {
                if (i + 1 >= token.length() || token.charAt(i + 1) != LF) {
                    error = "Invalid CR not followed by LF in header " + token;
                }
            }
            else if (c == LF) {
                if (i + 1 < token.length()) {
                    char next = token.charAt(i + 1);
                    if (next != SPACE && next != TAB) {
                        error = "Invalid LF not followed by whitespace";
                    }
                } else {
                    // LF at end of string (already checked for trailing LF above, but this handles edge case)
                    // This matches CHFW behavior
                    error = "Invalid LF not followed by whitespace";
                }
            }
            if (c >= 32 && c < 127) {
                sb.append(c);
            } 
            else if (c == CR || c == LF) {
                sb.append(SPACE);
            } 
            else {
                int maskedCode = c & 0xFF;
                if (maskedCode == CR || maskedCode == LF) {
                    sb.append('?');
                } else {
                    sb.append(c);
                }
            }
            if (error != null) {
                break;
            }
        }
        if (error != null) {
            throw new IllegalArgumentException(error);
        }

        return sb.toString();
    }

    /**
     * Normalize a header name to match the standard casing defined in HttpHeaderKeys.
     * This ensures consistency between CHFW and Netty header handling.
     * 
     * For standard headers (e.g., "content-type", "CONTENT-TYPE"), this returns the 
     * properly cased version ("Content-Type"). For custom headers, it returns the 
     * casing from the first occurrence, matching CHFW behavior.
     * 
     * @param headerName The header name to normalize (case-insensitive)
     * @return The normalized header name with proper casing, or the original name if null
     */
    private static String normalizeHeaderName(String headerName) {
        if (headerName == null) {
            return null;
        }
        
        // Use HttpHeaderKeys.find to get the normalized header name
        // This leverages the same matcher logic that CHFW uses
        // Pass false for returnNullForInvalidName to throw exceptions for:
        // - Empty strings (StringIndexOutOfBoundsException from KeyMatcher.add)
        // - Invalid characters like whitespace and special chars (IllegalArgumentException)
        HttpHeaderKeys key = HttpHeaderKeys.find(headerName, false);
        
        if (key != null) {
            // Return the properly cased name from the HttpHeaderKeys constant
            return key.getName();
        }
        
        // If not found (shouldn't happen with returnNullForInvalidName=false),
        // return the original name
        return headerName;
    }
    
}
