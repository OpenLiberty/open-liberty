/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.featureverifier.internal;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public abstract class AbstractVariableResolver {

    private static final String START = "${";
    private static final String START_IF = START + "if;";
    private static final String END = "}";

    public abstract String getValue(String name);

    public String resolve(String value) {
        String result = value;
        result = resolveSimple(value);
        result = resolveConditional(result);
        return result;
    }

    private String resolveSimple(String value) {
        String result = value;
        int start = indexOfVariableStart(result);
        int end = result.indexOf(END);

        while (start >= 0 && end >= 0) {
            String front = result.substring(0, start);
            String back = result.substring(start + 2);

            int nestedStart = indexOfVariableStart(back);
            int nestedEnd = back.indexOf(END);
            if (nestedStart >= 0 && nestedStart < nestedEnd) {
                String resolved = resolve(back);
                result = front + START + resolved;
            } else {
                int backEnd = back.indexOf(END);
                String variableName = back.substring(0, backEnd);
                String variableValue = getValue(variableName);
                result = front + variableValue + back.substring(backEnd + 1);
            }

            start = indexOfVariableStart(result);
            end = result.indexOf(END);
        }
        return result;
    }

    //${if;${releaseTypeGA};${libertyServiceVersion};${libertyBetaVersion}}
    private String resolveConditional(String value) {
        String result = value;
        int start = result.indexOf(START_IF);
        int end = result.indexOf(END);

        while (start >= 0 && end >= 0) {
            String prefix = result.substring(0, start);
            String middle = result.substring(start + START_IF.length(), end);
            String suffix = result.substring(end + 1);

            String[] args = middle.split(";", 3);
            assertEquals("Invalid If: " + middle, 3, args.length);
            boolean condition = Boolean.valueOf(args[0]);
            String then = args[1];
            String elze = args[2];

            result = prefix + (condition ? then : elze) + suffix;

            start = result.indexOf(START_IF);
            end = result.indexOf(END);
        }
        return result;
    }

    public static void debug(String message, String value) {
        debug(message, value, -1);
    }

    public static void debug(String message, String value, int index) {
        StringBuilder builder = new StringBuilder();

        if (index >= 0 && index < value.length()) {
            indent(builder, message.length() + 2);
            indent(builder, index);
            builder.append("v\n");
        }
        builder.append(message);
        builder.append(": ");
        builder.append(value);
        System.out.println(builder.toString());
    }

    private static void indent(StringBuilder builder, int size) {
        for (int i = 0; i < size; i++) {
            builder.append(" ");
        }
    }

    public static int indexOfVariableStart(String value) {
        int startIF = -1;
        int start = value.indexOf(START, startIF + 1);
        startIF = value.indexOf(START_IF, startIF + 1);
        while (startIF >= 0 && startIF == start) {
            start = value.indexOf(START, startIF + 1);
            startIF = value.indexOf(START_IF, startIF + 1);
        }
        return start;
    }

}
