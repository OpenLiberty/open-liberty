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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.kernel.feature.resolver.util.VerifyData.VerifyCase;

public class VerifyDelta {
    public static Map<String, List<String>> compare(VerifyData expectedCases, VerifyData actualCases) {
        VerifyDelta delta = new VerifyDelta();
        delta.doCompare(expectedCases, actualCases);
        return delta.getErrors();
    }

    public VerifyDelta() {
        this.errors = new LinkedHashMap<>();
    }

    private final Map<String, List<String>> errors;

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public int size() {
        return errors.size();
    }

    public int totalSize() {
        int total = 0;

        for (List<String> caseErrors : errors.values()) {
            total += caseErrors.size();
        }

        return total;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }

    public void clear() {
        errors.clear();
    }

    private void addError(String name, String error) {
        List<String> caseErrors = errors.computeIfAbsent(name, (String useName) -> new ArrayList<>());
        caseErrors.add(error);
    }

    public static final String GLOBAL_CASE_KEY = "global results";

    public void doCompare(VerifyData expectedCases, VerifyData actualCases) {
        int actualSize = actualCases.cases.size();
        int expectedSize = expectedCases.cases.size();
        if (actualSize != expectedSize) {
            addError(GLOBAL_CASE_KEY, "Incorrect case count; expected [ " + expectedSize + " ] actual [ " + actualSize + " ]");
        }

        // The actual and expected case mappings are kept in the original case order.

        Map<String, VerifyCase> actual = actualCases.mapCases();
        Map<String, VerifyCase> expected = expectedCases.mapCases();

        actual.forEach((String caseKey, VerifyCase actualCase) -> {
            VerifyCase expectedCase = expected.get(caseKey);
            if (expectedCase == null) {
                addError(GLOBAL_CASE_KEY, "Extra case [ " + caseKey + " ]");
            }
        });

        expected.forEach((String caseKey, VerifyCase expectedCase) -> {
            VerifyCase actualCase = actual.get(caseKey);
            if (actualCase == null) {
                addError(GLOBAL_CASE_KEY, "Missing case [ " + caseKey + " ]");
            }
        });

        actual.forEach((String caseKey, VerifyCase actualCase) -> {
            VerifyCase expectedCase = expected.get(caseKey);
            if (expectedCase == null) {
                return;
            }
            compare(caseKey, expectedCase, actualCase);
        });
    }

    public void compare(String caseKey, VerifyCase expectedCase, VerifyCase actualCase) {
        compare(caseKey, "Resolved", expectedCase.output.resolved, actualCase.output.resolved);
    }

    public void compare(String caseKey, String tag, List<String> expected, List<String> actual) {
        String prefix = "Error [ " + tag + caseKey + " ]: ";

        int actualSize = actual.size();
        int expectedSize = expected.size();
        if (actualSize != expectedSize) {
            addError(caseKey, "Incorrect count: expected [ " + expectedSize + " ] actual [ " + actualSize + " ]");
        }

        Set<String> expectedSet = new HashSet<>(expected);
        Set<String> actualSet = new HashSet<>(actual);

        expectedSet.forEach((String expectedElement) -> {
            if (!actualSet.contains(expectedElement)) {
                addError(caseKey, prefix + "Missing [ " + expectedElement + " ]");
            }
        });

        actualSet.forEach((String actualElement) -> {
            if (!expectedSet.contains(actualElement)) {
                addError(caseKey, prefix + "Extra       [ " + actualElement + " ]");
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
            addError(caseKey, prefix + orderError);
        }
    }
}
