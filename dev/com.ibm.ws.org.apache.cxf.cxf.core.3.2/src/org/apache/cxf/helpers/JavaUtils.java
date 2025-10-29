/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.helpers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.common.util.SystemPropertyAction;

public final class JavaUtils {

    /** Use this character as suffix */
    static final char KEYWORD_PREFIX = '_';

    /**
     * These are java keywords as specified at the following URL.
     * http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.9
     * Note that false, true, and null are not strictly keywords; they are
     * literal values, but for the purposes of this array, they can be treated
     * as literals.
     */
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch",
        "char", "class", "const", "continue", "default", "do", "double",
        "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto",
        "if", "implements", "import", "instanceof", "int", "interface", "long",
        "native", "new", "null", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch",
        "synchronized", "this", "throw", "throws", "transient", "true", "try",
        "void", "volatile", "while"
    ));

    private static boolean isJava11Compatible;
    private static boolean isJava9Compatible;
    private static boolean isJava8Compatible;
    private static boolean isJava8Before161;
    private static Integer javaMajorVersion;
    private static Integer java8patch;
    // Liberty Change Start: backport fips support
    private static boolean isFIPSEnabled;
    private static final String FIPS_ENABLED = "fips.enabled"; 

    static {
        String version = SystemPropertyAction.getProperty("java.version");
        isFIPSEnabled = Boolean.valueOf(SystemPropertyAction.getProperty(FIPS_ENABLED)); 
        // Liberty Change End
        try {
            if (version.startsWith("1.8.0_")) {
                isJava8Compatible = true;
                java8patch = Integer.parseInt(version.substring(6));
                isJava8Before161 = java8patch  < 161;
            }
        } catch (NumberFormatException ex) {
            isJava8Before161 = false;
        }

        if (version.indexOf('.') > 0) {
            version = version.substring(0, version.indexOf('.'));
        }            
        if (version.indexOf('-') > 0) {
            version = version.substring(0, version.indexOf('-'));
        }

        final Integer javaVersion = Integer.valueOf(version);
        setJava9Compatible(javaVersion >= 9);
        setJava11Compatible(javaVersion >= 11);
        setJavaMajorVersion(javaVersion);
    }

    private JavaUtils() {
    }

    /**
     * checks if the input string is a valid java keyword.
     *
     * @return boolean true/false
     */
    public static boolean isJavaKeyword(String keyword) {
        return KEYWORDS.contains(keyword);
    }

    /**
     * Turn a java keyword string into a non-Java keyword string. (Right now
     * this simply means appending an underscore.)
     */
    public static String makeNonJavaKeyword(String keyword) {
        return KEYWORD_PREFIX + keyword;
    }

    public static boolean isJava8Compatible() {
        return isJava8Compatible;
    }

    public static boolean isJava9Compatible() {
        return isJava9Compatible;
    }
    
    public static boolean isJava11Compatible() {
        return isJava11Compatible;
    }
    
    private static void setJava9Compatible(boolean java9Compatible) {
        JavaUtils.isJava9Compatible = java9Compatible;
    }
    
    private static void setJava11Compatible(boolean java11Compatible) {
        JavaUtils.isJava11Compatible = java11Compatible;
    }

    public static boolean isJava8Before161() {
        return isJava8Before161;
    }

    public static boolean isJava8Before(int patch) {
        return isJava8Compatible && java8patch < patch;
    }

    public static void setJavaMajorVersion(Integer javaMajorVersion) {
        JavaUtils.javaMajorVersion = javaMajorVersion;
    }
    
    public static Integer getJavaMajorVersion() {
        return javaMajorVersion;
    }
    
    // Liberty Change Start
    public static boolean isFIPSEnabled() {
        return isFIPSEnabled;
    }
    // Liberty Change End
}
