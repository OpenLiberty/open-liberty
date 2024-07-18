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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.kernel.feature.internal.util.VerifyData.ResultData;
import com.ibm.ws.kernel.feature.internal.util.VerifyData.VerifyCase;

public class VerifyDelta {
    public static Map<String, List<String>> compare(FeatureSupplier repo,
                                                    VerifyData expectedCases,
                                                    VerifyData actualCases, boolean actualUsedKernel) {
        VerifyDelta delta = new VerifyDelta();
        delta.doCompare(repo, expectedCases, actualCases, actualUsedKernel);
        return delta.getErrors();
    }

    public VerifyDelta() {
        this.errors = new LinkedHashMap<>();
        this.warnings = new LinkedHashMap<>();
        this.info = new LinkedHashMap<>();
    }

    public void clear() {
        clearErrors();
        clearWarnings();
        clearInfo();
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

    public static List<String> addMessage(List<String> messages, String message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        return messages;
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

    private final Map<String, List<String>> info;

    public void clearInfo() {
        info.clear();
    }

    public int getInfoCount() {
        return info.size();
    }

    public int totalInfoCount() {
        int total = 0;

        for (List<String> caseWarnings : info.values()) {
            total += caseWarnings.size();
        }

        return total;
    }

    public Map<String, List<String>> getInfo() {
        return info;
    }

    private void setInfo(String name, List<String> caseInfo) {
        info.put(name, caseInfo);
    }

    //

    private void setMessages(String caseKey, ChangeMessages messages) {
        if (messages.errors != null) {
            setErrors(caseKey, messages.errors);
        }
        if (messages.warnings != null) {
            setWarnings(caseKey, messages.warnings);
        }
        if (messages.info != null) {
            setInfo(caseKey, messages.info);
        }
    }

    //

    public static final String GLOBAL_CASE_KEY = "global results";

    public static final boolean USED_KERNEL = true;

    public void doCompare(FeatureSupplier repo,
                          VerifyData expectedCases,
                          VerifyData actualCases, boolean actualUsedKernel) {

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

            ChangeMessages caseMessages = compare(repo,
                                                  expectedCase,
                                                  actualCase, actualUsedKernel,
                                                  null, null,
                                                  null);
            setMessages(caseKey, caseMessages);
        }
    }

    public static final int ORIGINAL_FEATURE_OFFSET = 0;
    public static final int VERSIONLESS_FEATURE_OFFSET = 1;
    public static final int PLATFORM_OFFSET = 2;
    public static final int OLD_FEATURE_OFFSET = 3;
    public static final int NEW_FEATURE_OFFSET = 4;

    public static String substitutionKey(String vFeature, String platform) {
        String sKey = vFeature + ":" + platform;
        System.out.println("Substitution key [ " + sKey + " ]");
        return sKey;
    }

    public static void putSubstitution(String[] substitution, Map<String, String[]> substitutions) {
        String vFeature = substitution[VERSIONLESS_FEATURE_OFFSET];
        String platform = substitution[PLATFORM_OFFSET];
        substitutions.put(substitutionKey(vFeature, platform), substitution);
    }

    public static String[] getSubstitution(String vFeature, String platform, Map<String, String[]> substitutions) {
        return substitutions.get(substitutionKey(vFeature, platform));
    }

    public static ChangeMessages compare(FeatureSupplier repo,
                                         VerifyCase expectedCase,
                                         VerifyCase actualCase, boolean actualUsedKernel,
                                         List<String> extra, List<String> missing,
                                         String[] allowedSubstitution) {

        ChangeMessages messages = new ChangeMessages();

        for (ResultData resultType : VerifyData.ResultData.values()) {
            if (resultType == ResultData.FEATURE_RESOLVED) {
                continue;
            }
            compare(resultType.description,
                    expectedCase.output.get(resultType),
                    actualCase.output.get(resultType),
                    messages);
        }

        compare("Versionless resolutions",
                expectedCase.output.getVersionlessResolved(),
                actualCase.output.getVersionlessResolved(),
                messages);

        compareResolved(repo,
                        ResultData.FEATURE_RESOLVED.description,
                        expectedCase.output.getResolved(),
                        expectedCase.output.kernelOnly,
                        expectedCase.output.kernelBlocked,
                        actualCase.output.getResolved(), actualUsedKernel,
                        extra, missing,
                        allowedSubstitution,
                        messages);

        return messages;
    }

    public static void compare(String description,
                               Map<String, String> expectedResolved,
                               Map<String, String> actualResolved,
                               ChangeMessages messages) {

        for (Map.Entry<String, String> expectedEntry : expectedResolved.entrySet()) {
            String expectedKey = expectedEntry.getKey();
            String expectedValue = expectedEntry.getValue();

            if (!actualResolved.containsKey(expectedKey)) {
                messages.addError("Missing [ " + description + " ]: Key [ " + expectedKey + " ] Expected value [ " + expectedValue + " ]");
            } else {
                String actualValue = actualResolved.get(expectedKey);

                boolean failed;
                if (expectedValue == null) {
                    if (actualValue != null) {
                        failed = true;
                    } else {
                        failed = false;
                    }
                } else if (actualValue == null) {
                    failed = true;
                } else if (!expectedValue.equals(actualValue)) {
                    failed = true;
                } else {
                    failed = false;
                }

                if (failed) {
                    messages.addError("Incorrect [ " + description + " ]:" +
                                      " Key [ " + expectedKey + " ]" +
                                      " Expected value [ " + expectedValue + " ]" +
                                      " Actual value [ " + actualValue + " ]");
                }
            }
        }
    }

    public static void compare(String description,
                               List<String> expected,
                               List<String> actual,
                               ChangeMessages messages) {

        Set<String> expectedSet = new HashSet<>(expected);
        Set<String> actualSet = new HashSet<>(actual);

        for (String expectedElement : expectedSet) {
            if (!actualSet.contains(expectedElement)) {
                messages.addError("Missing [ " + description + " ]: [ " + expectedElement + " ]");
            }
        }

        for (String actualElement : actualSet) {
            if (!expectedSet.contains(actualElement)) {
                messages.addError("Extra [ " + description + " ]: [ " + actualElement + " ]");
            }
        }
    }

    public static interface FeatureSupplier {
        String getVisibility(String featureName);

        boolean isNoShip(String featureName);

        boolean dependsOnNoShip(String featureName);
    }

    private static void add(List<String> storage, String element) {
        if (storage != null) {
            storage.add(element);
        }
    }

    private static <T> T getAny(Set<T> set) {
        for (T elem : set) {
            return elem;
        }
        return null;
    }

    private static Set<String> compactMap(Map<String, Set<String>> map, String key, String value) {
        Set<String> values = map.get(key);
        if (values == null) {
            values = Collections.singleton(value);
            map.put(key, values);
        } else if (values.size() == 1) {
            Set<String> newValues = new HashSet<>(2);
            newValues.add(getAny(values));
            newValues.add(value);
            values = newValues;
            map.put(key, values);
        } else {
            values.add(value);
        }

        return values;
    }

    private static Map<String, Set<String>> mapVersions(Collection<String> features) {
        Map<String, Set<String>> featureVersions = new HashMap<>(features.size());

        for (String feature : features) {
            int versionOffset = feature.lastIndexOf('-');
            if (versionOffset == -1) {
                continue;
            }

            String base = feature.substring(0, versionOffset);
            String version = feature.substring(versionOffset + 1);

            @SuppressWarnings("unused")
            Set<String> versionsOfFeature = compactMap(featureVersions, base, version);
        }

        return featureVersions;
    }

    public static class ChangeMessages {
        public final List<String> errors;
        public final List<String> warnings;
        public final List<String> info;

        public ChangeMessages() {
            this(new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>());
        }

        public ChangeMessages(List<String> errors, List<String> warnings, List<String> info) {
            this.errors = errors;
            this.warnings = warnings;
            this.info = info;
        }

        public void addError(String message) {
            errors.add(message);
        }

        public boolean hasErrors() {
            return ((errors != null) && !errors.isEmpty());
        }

        public void addWarning(String message) {
            warnings.add(message);
        }

        public boolean hasWarnings() {
            return ((warnings != null) && !warnings.isEmpty());
        }

        public void addInfo(String message) {
            info.add(message);
        }

        public boolean hasInfo() {
            return ((info != null) && !info.isEmpty());
        }
    }

    public static void compareResolved(FeatureSupplier repo,
                                       String description,
                                       List<String> expected,
                                       List<String> expectedKernelOnly,
                                       List<String> expectedKernelBlocked,
                                       List<String> actual,
                                       boolean actualUsedKernel,
                                       List<String> extra, List<String> missing,
                                       String[] allowedSubstitution,
                                       ChangeMessages messages) {

        // Don't do this: Rely on the extra/missing checks.
        // The sizes are allowed to be different if the differences are all no-ship features.

        // int actualSize = actual.size();
        //
        // int expectedSize = expected.size();
        // expectedSize += (actualUsedKernel ? expectedKernelOnly.size() : expectedKernelBlocked.size());
        // if (actualSize != expectedSize) {
        //     messages.addError("Incorrect count: expected [ " + expectedSize + " ] actual [ " + actualSize + " ]");
        // }

        Set<String> actualSet = new HashSet<>(actual);
        Set<String> expectedSet = new HashSet<>(expected);
        Set<String> expectedExtraSet = new HashSet<>(actualUsedKernel ? expectedKernelOnly : expectedKernelBlocked);

        String expectedMissing = ((allowedSubstitution != null) ? allowedSubstitution[OLD_FEATURE_OFFSET] : null);
        String actualMissing = null;
        String expectedExtra = ((allowedSubstitution != null) ? allowedSubstitution[NEW_FEATURE_OFFSET] : null);
        String actualExtra = null;

        if (expectedMissing != null) {
            System.out.print("Expected missing [ " + expectedMissing + " ]");
        }
        if (expectedExtra != null) {
            System.out.print("Expected extra [ " + expectedExtra + " ]");
        }

        for (String expectedElement : expectedSet) {
            if (!actualSet.contains(expectedElement)) {
                if (repo.isNoShip(expectedElement) || repo.dependsOnNoShip(expectedElement)) {
                    messages.addWarning("Missing no-ship [ " + description + " ]: [ " + expectedElement + " ]");
                } else {
                    if ((expectedMissing != null) && expectedElement.equals(expectedMissing)) {
                        actualMissing = expectedElement;
                    } else {
                        add(missing, expectedElement);
                        messages.addError("Missing [ " + description + " ]: [ " + expectedElement + " ]");
                    }
                }
            }
        }

        String usedKernelTag = (actualUsedKernel ? "Kernel Only" : "Kernel Blocked");

        for (String expectedElement : expectedExtraSet) {
            if (!actualSet.contains(expectedElement)) {
                add(missing, expectedElement);

                if (repo.isNoShip(expectedElement) || repo.dependsOnNoShip(expectedElement)) {
                    messages.addWarning("Missing no-ship [ " + description + " ]: [ " + expectedElement + " ]" + usedKernelTag);
                } else {
                    messages.addError("Missing [ " + description + " ]: [ " + expectedElement + " ]" + usedKernelTag);
                }
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
                if (repo.isNoShip(actualElement) || repo.dependsOnNoShip(actualElement)) {
                    messages.addWarning(extraTag + " no-ship [ " + description + " ]: [ " + actualElement + " ]");
                } else {
                    if ((expectedExtra != null) && actualElement.equals(expectedExtra)) {
                        actualExtra = actualElement;
                    } else {
                        add(extra, actualElement);
                        messages.addError(extraTag + " [ " + description + " ]: [ " + actualElement + " ]");
                    }
                }
            }
        }

        if ((expectedExtra != null) && (expectedMissing != null)) {
            String substitutionError;

            if ((actualExtra == null) && (actualMissing == null)) {
                substitutionError = "Missing substitution: [ " + expectedMissing + " ] with [ " + expectedExtra + " ]";
            } else if (actualExtra == null) {
                substitutionError = "Broken substitution: Removed [ " + expectedMissing + " ] but did not add [ " + expectedExtra + " ]";
            } else if (actualMissing == null) {
                substitutionError = "Broken substitution: Did not remove [ " + expectedMissing + " ] but did add [ " + expectedExtra + " ]";
            } else {
                substitutionError = null;
            }
            if (substitutionError != null) {
                messages.addError(substitutionError);
            }

        } else if (expectedExtra != null) {
            if (actualExtra == null) {
                messages.addError("Did not add [ " + expectedExtra + " ]");
            }

        } else if (expectedMissing != null) {
            if (actualMissing == null) {
                messages.addError("Did not remove [ " + expectedMissing + " ]");
            }
        }

        if (((missing != null) && !missing.isEmpty()) && ((extra != null) && !extra.isEmpty())) {
            Map<String, Set<String>> missingVersions = mapVersions(missing);
            Map<String, Set<String>> extraVersions = mapVersions(extra);

            for (Map.Entry<String, Set<String>> missingEntry : missingVersions.entrySet()) {
                String missingBase = missingEntry.getKey();
                Set<String> missingVersionsOfBase = missingEntry.getValue();
                if (missingVersionsOfBase.size() != 1) {
                    continue;
                }

                Set<String> extraVersionsOfBase = extraVersions.get(missingBase);
                if (extraVersionsOfBase == null) {
                    continue;
                }
                if (extraVersionsOfBase.size() != 1) {
                    continue;
                }

                String oldVersion = getAny(missingVersionsOfBase);
                String newVersion = getAny(extraVersionsOfBase);

                messages.addInfo("Feature [ " + missingBase + " ] changed from [ " + oldVersion + " ] to [ " + newVersion + " ]");
            }
        }

        // Don't bother with the order if the resolved are incorrect.  The order
        // is likely wildly off because of omissions.

        if (!messages.hasErrors()) {
            int actualSize = actual.size();
            int expectedSize = expected.size();
            int minSize = ((actualSize > expectedSize) ? expectedSize : actualSize);

            String orderMsg = null;

            // Only test the order of elements which are unaffected
            // by the presence of kernel features.
            //
            // Always consume this actual.
            //
            // Only consume the expected if the actual is in the
            // unaffected features.
            //
            // Skip features which are no-ship.

            int actualNo = 0;
            int expectedNo = 0;
            while ((orderMsg == null) && (actualNo < minSize) && (expectedNo < minSize)) {
                String actualAt = actual.get(actualNo);
                boolean skipActual = (expectedExtraSet.contains(actualAt) ||
                                      (repo.isNoShip(actualAt) || repo.dependsOnNoShip(actualAt)));

                String expectedAt = expected.get(expectedNo);
                boolean skipExpected = (repo.isNoShip(expectedAt) || repo.dependsOnNoShip(expectedAt));

                // !skipA, !skipB: a++, b++;  test: Consume both; do test
                //  skipA, !skipB: a++;      !test: Consume just A; don't test
                // !skipA,  skipB: b++,      !test: Consume just B; don't test
                //  skipA,  skipB: a++, b++, !test: Consume both; don't test.

                if (skipActual || !skipExpected) {
                    actualNo++;
                }
                if (skipExpected || !skipActual) {
                    expectedNo++;
                }
                if (skipActual || skipExpected) {
                    continue;
                }

                if (!expectedAt.equals(actualAt)) {
                    orderMsg = "Order error at [ " + (actualNo - 1) + " ]" +
                               ": Expected [ " + expectedAt + " ]" +
                               " Actual [ " + actualAt + " ]";
                }
            }

            if (orderMsg != null) {
                messages.addWarning(orderMsg);
            }
        }
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

        moveDifference(original.output.getResolved(), updatedResolved, added, removed);
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
