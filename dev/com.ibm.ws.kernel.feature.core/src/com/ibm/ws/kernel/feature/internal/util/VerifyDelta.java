/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;

public class VerifyDelta {
    public static Map<String, List<String>> compare(VerifyData expectedCases,
                                                    VerifyData actualCases, boolean actualUsedKernel) {
        VerifyDelta delta = new VerifyDelta();
        delta.doCompare(expectedCases, actualCases, actualUsedKernel);
        return delta.getErrors();
    }

    public VerifyDelta() {
        this.errors = new LinkedHashMap<>();
        this.warnings = new LinkedHashMap<>();
    }

    public void clear() {
        clearErrors();
        clearWarnings();
    }

    //

    private final Map<String, List<String>> errors;

    public void clearErrors() {
        errors.clear();
    }

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

    private void setErrors(String name, List<String> caseErrors) {
        errors.put(name, caseErrors);
    }

    private void addError(String name, String error) {
        List<String> caseErrors = errors.get(name);
        if (caseErrors == null) {
            errors.put(name, caseErrors = new ArrayList<>());
        }
        caseErrors.add(error);
    }

    private static List<String> addError(List<String> errors, String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
        return errors;
    }

    //

    private final Map<String, List<String>> warnings;

    public void clearWarnings() {
        warnings.clear();
    }

    public int getWarningCount() {
        return warnings.size();
    }

    public int totalWarningCount() {
        int total = 0;

        for (List<String> caseWarnings : warnings.values()) {
            total += caseWarnings.size();
        }

        return total;
    }

    public Map<String, List<String>> getWarnings() {
        return warnings;
    }

    private void setWarnings(String name, List<String> caseWarnings) {
        warnings.put(name, caseWarnings);
    }

    //

    public static final String GLOBAL_CASE_KEY = "global results";

    public static final boolean USED_KERNEL = true;

    public void doCompare(VerifyData expectedCases, VerifyData actualCases, boolean actualUsedKernel) {

        int actualSize = actualCases.cases.size();
        int expectedSize = expectedCases.cases.size();
        if (actualSize != expectedSize) {
            addError(GLOBAL_CASE_KEY, "Incorrect case count; expected [ " + expectedSize + " ] actual [ " + actualSize + " ]");
        }

        // The actual and expected case mappings are kept in the original case order.

        Map<String, VerifyCase> actual = actualCases.mapCases();
        Map<String, VerifyCase> expected = expectedCases.mapCases();

        for (Map.Entry<String, VerifyCase> caseEntry : actual.entrySet()) {
            String caseKey = caseEntry.getKey();
            if (!expected.containsKey(caseKey)) {
                addError(GLOBAL_CASE_KEY, "Extra case [ " + caseKey + " ]");
            }
        }

        for (Map.Entry<String, VerifyCase> caseEntry : expected.entrySet()) {
            String caseKey = caseEntry.getKey();
            if (!actual.containsKey(caseKey)) {
                addError(GLOBAL_CASE_KEY, "Missing case [ " + caseKey + " ]");
            }
        }

        for (Map.Entry<String, VerifyCase> caseEntry : actual.entrySet()) {
            String caseKey = caseEntry.getKey();
            VerifyCase actualCase = caseEntry.getValue();

            VerifyCase expectedCase = expected.get(caseKey);
            if (expectedCase == null) {
                return;
            }

            List<String> caseWarnings = new ArrayList<>(0);

            List<String> caseErrors = compare(null, caseWarnings,
                                              expectedCase,
                                              actualCase, actualUsedKernel,
                                              null, null);

            if (caseErrors != null) {
                setErrors(caseKey, caseErrors);
            }
            if (!caseWarnings.isEmpty()) {
                setWarnings(caseKey, caseWarnings);
            }
        }
    }

    public static List<String> compare(List<String> caseErrors, List<String> caseWarnings,
                                       VerifyCase expectedCase,
                                       VerifyCase actualCase, boolean actualUsedKernel,
                                       List<String> extra, List<String> missing) {

        return compare(null,
                       caseErrors, caseWarnings,
                       expectedCase.output.resolved,
                       expectedCase.output.kernelOnly,
                       expectedCase.output.kernelBlocked,
                       actualCase.output.resolved, actualUsedKernel,
                       extra, missing);
    }

    public static List<String> compare(VisibilitySupplier repo,
                                       List<String> caseErrors, List<String> caseWarnings,
                                       VerifyCase expectedCase,
                                       VerifyCase actualCase, boolean actualUsedKernel,
                                       List<String> extra, List<String> missing) {

        return compare(repo,
                       caseErrors, caseWarnings,
                       expectedCase.output.resolved,
                       expectedCase.output.kernelOnly,
                       expectedCase.output.kernelBlocked,
                       actualCase.output.resolved, actualUsedKernel,
                       extra, missing);
    }

    public static interface VisibilitySupplier {
        String getVisibility(String featureName);
    }

    protected static String getType(VisibilitySupplier repo, String featureName) {
        return repo.getVisibility(featureName);
    }

    protected static String addType(VisibilitySupplier repo, String featureName) {
        if (repo == null) {
            return featureName;
        } else {
            return featureName + " " + getType(repo, featureName);
        }
    }

    public static List<String> compare(VisibilitySupplier repo,
                                       List<String> caseErrors, List<String> caseWarnings,
                                       List<String> expected,
                                       List<String> expectedKernelOnly,
                                       List<String> expectedKernelBlocked,
                                       List<String> actual,
                                       boolean actualUsedKernel,
                                       List<String> extra, List<String> missing) {

        int actualSize = actual.size();

        int expectedSize = expected.size();
        expectedSize += (actualUsedKernel ? expectedKernelOnly.size() : expectedKernelBlocked.size());
        if (actualSize != expectedSize) {
            caseErrors = addError(caseErrors, "Incorrect count: expected [ " + expectedSize + " ] actual [ " + actualSize + " ]");
        }

        Set<String> actualSet = new HashSet<>(actual);
        Set<String> expectedSet = new HashSet<>(expected);
        Set<String> expectedExtraSet = new HashSet<>(actualUsedKernel ? expectedKernelOnly : expectedKernelBlocked);

        for (String expectedElement : expectedSet) {
            if (!actualSet.contains(expectedElement)) {
                if (missing != null) {
                    missing.add(expectedElement);
                }
                caseErrors = addError(caseErrors, "Missing [ " + addType(repo, expectedElement) + " ]");
            }
        }

        String usedKernelTag = (actualUsedKernel ? "Kernel Only" : "Kernel Blocked");

        for (String expectedElement : expectedExtraSet) {
            if (!actualSet.contains(expectedElement)) {
                if (missing != null) {
                    missing.add(expectedElement);
                }
                caseErrors = addError(caseErrors, "Missing [ " + addType(repo, expectedElement) + " ]" + usedKernelTag);
            }
        }

        for (String actualElement : actualSet) {
            String extraTag;
            if (expectedSet.contains(actualElement)) {
                extraTag = null;
            } else if (expectedKernelOnly.contains(actualElement)) {
                if (actualUsedKernel) {
                    extraTag = null;
                } else {
                    extraTag = "Extra kernel only";
                }
            } else if (expectedKernelBlocked.contains(actualElement)) {
                if (actualUsedKernel) {
                    extraTag = "Extra kernel blocked";
                } else {
                    extraTag = null;
                }
            } else {
                extraTag = "Extra";
            }

            if (extraTag != null) {
                if (extra != null) {
                    extra.add(actualElement);
                }
                caseErrors = addError(caseErrors, extraTag + " [ " + addType(repo, actualElement) + " ]");
            }
        }

        // Don't bother with the order if the resolved are incorrect.  The order
        // is likely wildly off because of omissions.

        if (caseErrors == null) {
            int minSize = ((actualSize > expectedSize) ? expectedSize : actualSize);

            String orderError = null;

            // Only test the order of elements which are unaffected
            // by the presence of kernel features.

            // Always consume this actual.
            //
            // Only consume the expected if the actual is in the
            // unaffected features.

            int actualNo = 0;
            int expectedNo = 0;
            while ((orderError == null) && (actualNo < minSize)) {
                String actualAt = actual.get(actualNo);
                actualNo++;
                if (expectedExtraSet.contains(actualAt)) {
                    continue;
                }

                String expectedAt = expected.get(expectedNo);
                expectedNo++;

                if (!expectedAt.contentEquals(actualAt)) {
                    orderError = "Order error at [ " + (actualNo - 1) + " ]" +
                                 ": Expected [ " + expectedAt + " ]" +
                                 " Actual [ " + actualAt + " ]";
                }
            }

            if (orderError != null) {
                if (caseWarnings != null) {
                    caseWarnings.add(orderError);
                } else {
                    caseErrors = addError(caseErrors, orderError);
                }
            }
        }

        return caseErrors;
    }

    public static final boolean ORIGINAL_USED_KERNEL = true;
    public static final boolean UPDATED_USED_KERNEL = true;

    public static void kernelAdjust(VerifyCase original, boolean originalUsedKernel,
                                    Set<String> updatedResolved, boolean updatedUsedKernel) {

        if (originalUsedKernel == updatedUsedKernel) {
            return; // Unexpected; nothing to do.
        }

        // If the original used kernel features, and the updated did not use kernel features,
        // then features added by the updated resolved features kernel blocked, and the
        // the features moved by the updated resolved features are kernel only.
        //
        // Conversely, if the updated used kernel features, the added features are kernel only
        // and the removed features are kernel blocked.

        List<String> added;
        List<String> removed;
        if (originalUsedKernel) {
            added = original.output.kernelBlocked;
            removed = original.output.kernelOnly;
        } else {
            added = original.output.kernelBlocked;
            removed = original.output.kernelOnly;
        }

        moveDifference(original.output.resolved, updatedResolved, added, removed);
    }

    protected static void moveDifference(List<String> original, Set<String> updated,
                                         List<String> added, List<String> removed) {

        int numOriginal = original.size();
        for (int originalNo = 0; originalNo < numOriginal; originalNo++) {
            String originalElement = original.get(originalNo);
            if (!updated.contains(originalElement)) {
                removed.add(originalElement);
                original.remove(originalNo);
                numOriginal--;
            }
        }

        Set<String> originalSet = new HashSet<>(original);

        for (String updatedElement : updated) {
            if (!originalSet.contains(updatedElement)) {
                added.add(updatedElement);
            }
        }
    }
}
