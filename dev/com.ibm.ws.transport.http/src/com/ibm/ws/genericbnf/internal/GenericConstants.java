/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.genericbnf.internal;

/**
 * Constants used throughout the generic BNF package.
 */
public interface GenericConstants {

    /** RAS trace name for the genericbnf package */
    String GENERIC_TRACE_NAME = "GenericBNF";

    /** Mask used for known/defined values */
    int KNOWN_MASK = 0x20000; // 128 * 1024
    /** Mask used for unknown/undefined values */
    int UNKNOWN_MASK = 0x1FFFF; // KNOWN-1

    /** ID representing the end of headers marker */
    int END_OF_HEADERS = 0;
    /** ID representing a known header is next */
    int KNOWN_HEADER = 1;
    /** ID representing an unknown header is next */
    int UNKNOWN_HEADER = 2;

    /** Default non-parsing state */
    int PARSING_NOTHING = 0;
    /** Parsing the flag on what type of header is coming next */
    int PARSING_HDR_FLAG = 1;
    /** Parsing the known header ordinal */
    int PARSING_HDR_KNOWN = 2;
    /** Parsing the unknown header name length */
    int PARSING_HDR_NAME_LEN = 3;
    /** Parsing the unknown header string */
    int PARSING_HDR_NAME_VALUE = 4;
    /** Parsing the length of the header value string */
    int PARSING_HDR_VALUE_LEN = 5;
    /** Parsing the header value string */
    int PARSING_HDR_VALUE = 6;

}
