/*******************************************************************************
 * Copyright (c) 20178 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.test;

public class UnitTestUtils {

    public static final String CONTENT_TO_VALIDATE_NULL = "Content to validate is null";
    public static final String STRING_TO_SEARCH_FOR_NULL = "String to search for is null";
    public static final String ERR_COMPARISON_TYPE_UNKNOWN = "comparison type.+" + "%s" + ".+unknown";
    public static final String ERR_STRING_NOT_NULL = ".*Expected value to be null.+received.+\\[" + "%s" + "\\]";
    public static final String ERR_STRING_NULL = ".*Expected value not to be null";
    public static final String ERR_STRING_DOES_NOT_EQUAL = ".*Was expecting content to equal \\[" + "%s" + "\\].+received.+\\[" + "%s" + "\\]";
    public static final String ERR_STRING_NOT_FOUND = ".*Was expecting .*" + "%s" + ".+received.+\\[" + "%s" + "\\]";
    public static final String ERR_STRING_FOUND = ".*Was not expecting .*" + "%s" + ".+received.+\\[" + "%s" + "\\]";
    public static final String ERR_REGEX_NOT_FOUND = ".*Did not find.* regex.+" + "%s" + ".+ content.+\\[" + "%s" + "\\]";
    public static final String ERR_REGEX_FOUND = ".+unexpected regex.+" + "%s" + ".+ content.+\\[" + "%s" + "\\]";

    public static final String ERR_UNKNOWN_RESPONSE_TYPE = "Unknown response type: " + "%s";

}
