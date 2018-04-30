/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FatStringUtils {

    /**
     * Extracts the first matching group in the provided content, if the regex includes at least one group. An exception is
     * thrown if the regex does not include a group, or if a matching group cannot be found in the content.
     */
    public static String extractRegexGroup(String fromContent, String regex) throws Exception {
        return extractRegexGroup(fromContent, regex, 1);
    }

    /**
     * Extracts the first matching group in the provided content, if the pattern includes at least one group. An exception is
     * thrown if the pattern does not include a group, or if a matching group cannot be found in the content.
     */
    public static String extractRegexGroup(String fromContent, Pattern regex) throws Exception {
        return extractRegexGroup(fromContent, regex, 1);
    }

    /**
     * Extracts the specified matching group in the provided content. An exception is thrown if the group number is invalid
     * (negative or greater than the number of groups found in the content), or if a matching group cannot be found in the
     * content.
     */
    public static String extractRegexGroup(String fromContent, String regex, int groupNumber) throws Exception {
        if (regex == null) {
            throw new Exception("Cannot extract regex group because the provided regular expression is null.");
        }
        Pattern expectedPattern = Pattern.compile(regex);
        return extractRegexGroup(fromContent, expectedPattern, groupNumber);
    }

    /**
     * Extracts the specified matching group in the provided content. An exception is thrown if the group number is invalid
     * (negative or greater than the number of groups found in the content), or if a matching group cannot be found in the
     * content.
     */
    public static String extractRegexGroup(String fromContent, Pattern regex, int groupNumber) throws Exception {
        if (fromContent == null) {
            throw new Exception("Cannot extract regex group because the provided content string is null.");
        }
        if (regex == null) {
            throw new Exception("Cannot extract regex group because the provided regular expression is null.");
        }
        if (groupNumber < 0) {
            throw new Exception("Group number to extract must be non-negative. Provided group number was " + groupNumber);
        }
        Matcher matcher = regex.matcher(fromContent);
        if (!matcher.find()) {
            throw new Exception("Did not find any matches for regex [" + regex + "] in the provided content: [" + fromContent + "].");
        }
        if (matcher.groupCount() < groupNumber) {
            throw new Exception("Found " + matcher.groupCount() + " matching groups in the content, but expected at least " + groupNumber + ". Regex was [" + regex + "] and content was [" + fromContent + "].");
        }
        return matcher.group(groupNumber);
    }

}
