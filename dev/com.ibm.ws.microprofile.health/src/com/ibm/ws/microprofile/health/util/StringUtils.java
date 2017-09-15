/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.microprofile.health.util;

/**
 * A lite-weight impl of apache.commons.lang3.StringUtils/ObjectUtils.
 */
public class StringUtils {

    /**
     * @return true if the string is null or "" or only whitespace
     */
    public static boolean isEmpty(String str) {
        return (str == null) || str.trim().length() == 0;
    }

}
