/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.resolver.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.kernel.feature.resolver.util.VerifyData.VerifyCase;

public class VerifyUtil {
    public static List<String> compare(VerifyData expectedCases, VerifyData actualCases) {
        List<String> errors = new ArrayList<>();
        compare(expectedCases, actualCases, errors);
        return errors;
    }

    public static void compare(VerifyData expectedCases, VerifyData actualCases, List<String> errors) {
        int actualSize = actualCases.cases.size();
        int expectedSize = expectedCases.cases.size();
        if (actualSize != expectedSize) {
            errors.add("Incorrect count of cases; expected [ " + expectedSize + " ] actual [ " + actualSize + " ]");
        }

        Map<String, VerifyCase> actual = mapCases(actualCases.cases);
        Map<String, VerifyCase> expected = mapCases(expectedCases.cases);

        actual.forEach((String caseKey, VerifyCase actualCase) -> {
            VerifyCase expectedCase = expected.get(caseKey);
            if (expectedCase == null) {
                errors.add("Extra case [ " + caseKey + " ]");
            }
        });

        expected.forEach((String caseKey, VerifyCase expectedCase) -> {
            VerifyCase actualCase = actual.get(caseKey);
            if (actualCase == null) {
                errors.add("Missing case [ " + caseKey + " ]");
            }
        });

        actual.forEach((String caseKey, VerifyCase actualCase) -> {
            VerifyCase expectedCase = expected.get(caseKey);
            if (expectedCase == null) {
                return;
            }
            compare(caseKey, expectedCase, actualCase, errors);
        });
    }

    public static void compare(String caseKey, VerifyCase expectedCase, VerifyCase actualCase, List<String> errors) {
        compare(caseKey, "Resolved", expectedCase.output.resolved, actualCase.output.resolved, errors);
    }

    public static void compare(String caseKey, String tag, List<String> expected, List<String> actual, List<String> errors) {
        String prefix = "Error [ " + tag + caseKey + " ]: ";

        int actualSize = actual.size();
        int expectedSize = expected.size();
        if (actualSize != expectedSize) {
            errors.add(prefix + "Incorrect count: expected [ " + expectedSize + " ] actual [ " + actualSize + " ]");
        }

        Set<String> expectedSet = new HashSet<>(expected);
        Set<String> actualSet = new HashSet<>(actual);

        expectedSet.forEach((String expectedElement) -> {
            if (!actualSet.contains(expectedElement)) {
                errors.add(prefix + "Missing [ " + expectedElement + " ]");
            }
        });

        actualSet.forEach((String actualElement) -> {
            if (!expectedSet.contains(actualElement)) {
                errors.add(prefix + "Extra [ " + actualElement + " ]");
            }
        });

        int minSize = ((actualSize > expectedSize) ? expectedSize : actualSize);
        String orderError = null;

        for (int elementNo = 0; (orderError == null) && (elementNo < minSize); elementNo++) {
            String expectedAt = expected.get(elementNo);
            String actualAt = actual.get(elementNo);

            if (!expectedAt.contentEquals(actualAt)) {
                orderError = "Order error at [ " + elementNo + " ]" +
                             ": Expected [ " + expectedAt + " ]" +
                             " Actual [ " + actualAt + " ]";
            }
        }

        if (orderError == null) {
            errors.add(prefix + orderError);
        }
    }

    public static Map<String, VerifyCase> mapCases(List<VerifyCase> cases) {
        Map<String, VerifyCase> mappedCases = new HashMap<>();

        StringBuilder keyBuilder = new StringBuilder();
        cases.forEach((VerifyCase verifyCase) -> {
            mappedCases.put(caseKey(verifyCase, keyBuilder), verifyCase);
        });

        return mappedCases;
    }

    public static String caseKey(VerifyCase verifyCase, StringBuilder keyBuilder) {
        verifyCase.input.kernel.forEach((String name) -> {
            if (keyBuilder.length() != 0) {
                keyBuilder.append(':');
            }
            keyBuilder.append(name);
        });

        verifyCase.input.roots.forEach((String name) -> {
            if (keyBuilder.length() != 0) {
                keyBuilder.append(':');
            }
            keyBuilder.append(name);
        });

        String key = keyBuilder.toString();
        keyBuilder.setLength(0);
        return key;
    }
}
