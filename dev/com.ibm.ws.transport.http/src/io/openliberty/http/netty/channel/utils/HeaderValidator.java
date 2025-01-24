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

import java.util.Objects;
import java.util.regex.Pattern;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;

/**
 * Processes and validates HTTP header names and values in compliance with 
 * RFC 7230, "Hypertext Transfer Protocol (HTTP/1.1): Message Syntax 
 * and Routing"
 */
public class HeaderValidator {

    /**
     * Defines a pattern for valid header names (token characters or "tchars") as specified in 
     * RFC 7230, Section 3.2.6, "Field Value Components".
     */
    private static final Pattern TCHAR_PATTERN = Pattern.compile("^[!#$%&'*+\\-\\.\\^_`|~0-9a-zA-Z]+$");

    public enum FieldType{NAME, VALUE}

    private HeaderValidator() {
        //Utility Singleton
    }

    public static String process(String token, FieldType type, HttpChannelConfig config){

        if(type == FieldType.NAME && token == null){
            throw new IllegalArgumentException("Header name must not be null");
        }
        String normalized = (token == null) ? "": token.trim();

        if(type == FieldType.NAME){
            normalized = normalized.toLowerCase();
        }

        validate(normalized, type, config);

        return normalized;

    }

    private static void validate(String token, FieldType type, HttpChannelConfig config){

        final int MAX_FIELD_SIZE = config.getLimitOfFieldSize();

        // Check for length limit
        if (token.length() > MAX_FIELD_SIZE) {
            throw new IllegalArgumentException(token +
                                               " exceeds the maximum allowed length of " + MAX_FIELD_SIZE + " characters");
        }

        // Additional validation for header names
        if (type == FieldType.NAME && !TCHAR_PATTERN.matcher(token).matches()) {
            throw new IllegalArgumentException("Invalid header name: " + token);
        }

        // Validate header values for control characters and non-ASCII characters
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);

            // Reject control characters (0x00-0x1F, except horizontal tab (0x09)) and DEL (0x7F)
            if (c == '\r' || c == '\n' || (c >= 0 && c < 32 && c != '\t') || c == 127) {
                throw new IllegalArgumentException("Invalid control character in Field Token [" + type + "]: " + token);
            }

            // Reject non-ASCII characters (0x80 and above)
            if (c > 127) {
                throw new IllegalArgumentException("Non-ASCII character found in Field Token [" + type + ":] " + token);
            }
        }
    }

}
