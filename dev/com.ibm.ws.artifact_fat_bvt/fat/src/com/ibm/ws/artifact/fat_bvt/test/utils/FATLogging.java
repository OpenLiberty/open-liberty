/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.test.utils;

import java.util.List;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Consolidated logging utility. Isolates access to the
 * Simplicity logger, {@link Log}.
 */
public class FATLogging {
    public static String asText(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for ( String line : lines ) {
            if ( isFirst ) {
                isFirst = false;
            } else {
                builder.append("\n");
            }
            builder.append(line);
        }
        return builder.toString();
    }

    public static void info(
        Class<?> sourceClass, String sourceMethodName,
        String text) {

        Log.info(
            sourceClass, sourceMethodName,
            text);
    }

    public static void info(
        Class<?> sourceClass, String sourceMethodName,
        String text, Object value) {

        Log.info(
            sourceClass, sourceMethodName,
            text + " [ " + value + " ]");
    }

    public static void info(
        Class<?> sourceClass, String sourceMethodName,
        String prefix,
        String text, Object value) {

        Log.info(
            sourceClass, sourceMethodName,
            prefix + ": " +
            text + " [ " + value + " ]");
    }

    public static void info(
        Class<?> sourceClass, String sourceMethodName,
        String text1, Object value1,
        String text2, Object value2) {

        Log.info(
            sourceClass, sourceMethodName,
            text1 + " [ " + value1 + " ] " +
            text2 + " [ " + value2 + " ]");
    }

    public static void info(
        Class<?> sourceClass, String sourceMethodName,
        String prefix,
        String text1, Object value1,
        String text2, Object value2) {

        Log.info(
            sourceClass, sourceMethodName,
            prefix + ": " +
            text1 + " [ " + value1 + " ] " +
            text2 + " [ " + value2 + " ]");
    }
}
