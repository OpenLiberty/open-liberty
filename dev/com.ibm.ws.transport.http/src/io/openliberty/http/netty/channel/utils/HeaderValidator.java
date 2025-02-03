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

import java.util.regex.Pattern;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;

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

    private static boolean disabledUntilRFE = true;
    
        /**
         * Defines a pattern for valid header names (token characters or "tchars") as specified in 
         * RFC 7230, Section 3.2.6, "Field Value Components".
         */
        private static final Pattern TCHAR_PATTERN = Pattern.compile("^[!#$%&'*+\\-\\.\\^_`|~0-9a-zA-Z]+$");
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
            String normalized = (token == null) ? "": token.trim();
    
            if(!disabledUntilRFE && type == FieldType.NAME){
            normalized = normalized.toLowerCase();
        }
        
        return validate(normalized, type, config);

    }

    /**
     * Validates and cleans a header field:
     *  Enforces the maximum allowed length as specified by {@link HttpChannelConfig}
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

        final int MAX_FIELD_SIZE = config.getLimitOfFieldSize();

        // Check for length limit
        if (token.length() > MAX_FIELD_SIZE) {
            throw new IllegalArgumentException(token +
                                               " exceeds the maximum allowed length of " + MAX_FIELD_SIZE + " characters");
        }

        if(!config.isHeaderValidationEnabled()){
            return token;
        }
        if (type == FieldType.NAME && !TCHAR_PATTERN.matcher(token).matches()) {
            throw new IllegalArgumentException("Invalid header name: " + token);
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

            if (c == CR) {
                if (i + 1 >= token.length() || token.charAt(i + 1) != LF) {
                    error = "Invalid CR not followed by LF in header " + token;
                }
            }
            else if (c == LF) {
                if (i + 1 < token.length()) {
                    char next = token.charAt(i + 1);
                    if (next != SPACE && next != TAB) {
                        error = "Invalid LF not followed by whitespace in header " + token;
                    }
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

        String validated = sb.toString();
        if (validated.length() > MAX_FIELD_SIZE) {
            throw new IllegalArgumentException(validated +" (rewritten) exceeds the maximum allowed length of " + MAX_FIELD_SIZE + " characters");
        }

        return validated;
    }

    
}
