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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.kernel.feature.resolver.FeatureResolver;

//Source restricted to java7.

public class VerifyData {

    public VerifyData add(VerifyData other) {
        Map<String, VerifyCase> mappedCases = mapCases();
        Map<String, VerifyCase> otherMappedCases = other.mapCases();

        mappedCases.putAll(otherMappedCases);

        return new VerifyData(mappedCases.values());
    }

    public VerifyData splice(VerifyData output) {
        Map<String, VerifyCase> inputCases = mapCases();
        Map<String, VerifyCase> outputCases = output.mapCases();

        int inputSize = inputCases.size();
        int outputSize = outputCases.size();
        int maxCases = ((inputSize > outputSize) ? inputSize : outputSize);
        Map<String, VerifyCase> mergedCases = new HashMap<>(maxCases);

        for (Map.Entry<String, VerifyCase> inputEntry : inputCases.entrySet()) {
            String inputKey = inputEntry.getKey();
            VerifyCase inputCase = inputEntry.getValue();

            VerifyCase outputCase = outputCases.get(inputKey);

            VerifyCase mergedCase;

            if (outputCase == null) {
                System.out.println("Stubbing [ " + inputCase.name + " ]: No output case");
                mergedCase = inputCase;
            } else {
                mergedCase = inputCase.splice(outputCase);
            }

            mergedCases.put(inputKey, mergedCase);
        }

        return new VerifyData(mergedCases.values());
    }

    //

    public VerifyData() {
        this.cases = new ArrayList<>();
    }

    public VerifyData(Collection<? extends LazySupplier<VerifyCase>> cases) {
        this.cases = new ArrayList<>(cases.size());

        for (LazySupplier<VerifyCase> verifyCase : cases) {
            this.cases.add(verifyCase.supply());
        }
    }

    public final List<VerifyCase> cases;

    public List<? extends VerifyCase> getCases() {
        return cases;
    }

    /**
     * Answer an ordered table of the contained cases.
     *
     * Keys are case keys, obtained from {@link VerifyCase#asKey(StringBuilder)}.
     *
     * Values are the cases.
     *
     * The mapping is ordered, and preserves the original ordering of the cases.
     *
     * @return An ordered table of the contained cases.
     */
    public Map<String, VerifyCase> mapCases() {
        Map<String, VerifyCase> mappedCases = new LinkedHashMap<>(cases.size());

        StringBuilder keyBuilder = new StringBuilder();
        for (VerifyCase verifyCase : cases) {
            String caseKey = verifyCase.asKey(keyBuilder);
            VerifyCase priorCase = mappedCases.put(caseKey, verifyCase);
            if (priorCase != null) {
                System.out.println("ERROR: Key duplication [ " + caseKey + " ]");
            }
        }

        return mappedCases;
    }

    private static void append(StringBuilder builder, String text, char sep) {
        if (builder.length() != 0) {
            builder.append(sep);
        }
        builder.append(text);
    }

    public VerifyCase addCase(String name, String description, boolean isMultiple) {
        VerifyCase verifyCase = new VerifyCase(name, description, isMultiple);
        cases.add(verifyCase);
        return verifyCase;
    }

    public VerifyCase addCase() {
        VerifyCase verifyCase = new VerifyCase();
        cases.add(verifyCase);
        return verifyCase;
    }

    public void addCase(VerifyCase verifyCase) {
        cases.add(verifyCase);
    }

    public static long getTimeNs() {
        return System.nanoTime();
    }

    public static class VerifyCase implements LazySupplier<VerifyCase> {
        public String name;
        public String description;
        public long durationNs;

        public final VerifyInput input;
        public final VerifyOutput output;

        public VerifyCase() {
            this.input = new VerifyInput();
            this.output = new VerifyOutput();
        }

        public VerifyCase(String name, String description, boolean isMultiple) {
            this.name = name;
            this.description = description;

            this.input = new VerifyInput(isMultiple);
            this.output = new VerifyOutput();
        }

        public VerifyCase(VerifyCase inputCase, FeatureResolver.Result result, long durationNs) {
            this.name = inputCase.name;
            this.description = inputCase.description;

            this.durationNs = durationNs;

            this.input = inputCase.input.copy();
            this.output = new VerifyOutput(result);
        }

        public void setDurationNs(long durationNs) {
            this.durationNs = durationNs;
        }

        // Multiple Kernel:kname1 Roots:rname1:rname2

        public String asKey(StringBuilder keyBuilder) {
            char spaceSep = ' ';
            char colonSep = ':';

            if (input.isMultiple) {
                append(keyBuilder, "Multiple", spaceSep);
            }

            if (!input.kernel.isEmpty()) {
                append(keyBuilder, "Kernel", spaceSep);
                for (String kernelName : input.kernel) {
                    append(keyBuilder, kernelName, colonSep);
                }
            }
            if (!input.roots.isEmpty()) {
                append(keyBuilder, "Roots", spaceSep);
                for (String rootName : input.roots) {
                    append(keyBuilder, rootName, colonSep);
                }
            }
            if (!input.platforms.isEmpty()) {
                append(keyBuilder, "Platforms", spaceSep);
                for (String platform : input.platforms) {
                    append(keyBuilder, platform, colonSep);
                }
            }
            if (!input.envMap.isEmpty()) {
                append(keyBuilder, "Environment", spaceSep);
                for (Map.Entry<String, String> envEntry : input.envMap.entrySet()) {
                    append(keyBuilder, envEntry.getKey(), colonSep);
                    append(keyBuilder, envEntry.getValue(), colonSep);
                }
            }

            String key = keyBuilder.toString();
            keyBuilder.setLength(0);

            return key;
        }

        public void kernelAdjust(boolean usedKernel,
                                 Set<String> altResolved, boolean altUsedKernel) {

            VerifyDelta.kernelAdjust(this, usedKernel, altResolved, altUsedKernel);
        }

        //

        @Override
        public VerifyCase produce() {
            return this;
        }

        @Override
        public VerifyCase supply() {
            return this;
        }

        @Override
        public VerifyCase getSupplied() {
            return this;
        }

        //

        public VerifyCase splice(VerifyCase other) {
            return new VerifyCase(this, other);
        }

        public VerifyCase(VerifyCase thisCase, VerifyCase otherCase) {
            this.name = thisCase.name;
            this.description = thisCase.description;

            this.input = thisCase.input.copy();
            this.output = otherCase.output.copy();
        }
    }

    public static class VerifyInput {
        public VerifyInput(boolean isMultiple) {
            this();

            this.isMultiple = isMultiple;
        }

        public VerifyInput() {
            this.isMultiple = false;

            this.kernel = new ArrayList<>();
            this.roots = new ArrayList<>();
            this.platforms = new ArrayList<>();
            this.envMap = new HashMap<>();
        }

        public boolean isMultiple;

        public final List<String> kernel;
        public final List<String> roots;
        public final List<String> platforms;
        public final Map<String, String> envMap;

        public void addKernel(Collection<String> features) {
            kernel.addAll(features);
        }

        public void addRoots(Collection<String> newRoots) {
            this.roots.addAll(newRoots);
        }

        public void putEnv(Map<String, String> newEnv) {
            envMap.putAll(newEnv);
        }

        public void addKernel(String feature) {
            kernel.add(feature);
        }

        public void addRoot(String name) {
            roots.add(name);
        }

        public void addPlatForms(Collection<String> newPlatforms) {
            this.platforms.addAll(newPlatforms);
        }

        public void addPlatform(String name) {
            platforms.add(name);
        }

        public void putEnv(String name, String value) {
            envMap.put(name, value);
        }

        public void putAllEnv(Map<String, String> map) {
            envMap.putAll(map);
        }

        public void putEnvironment(String name, String value) {
            envMap.put(name, value);
        }

        public void putAllEnvironment(Map<String, String> map) {
            envMap.putAll(map);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append('\n');
            sb.append("VerifyInput [");
            sb.append('\n');
            sb.append('\n');

            if (isMultiple) {
                sb.append("Multiple");
                sb.append('\n');
            }

            sb.append('\n');
            sb.append("Kernel features:");
            sb.append('\n');

            for (String kf : kernel) {
                sb.append(kf + "\n");
            }

            sb.append("\nRoot features:\n");
            for (String rf : roots) {
                sb.append(rf);
                sb.append('\n');
            }

            sb.append('\n');
            sb.append("Platforms:");
            sb.append('\n');
            for (String p : platforms) {
                sb.append(p);
                sb.append('\n');
            }

            sb.append('\n');
            sb.append("Environment:");
            sb.append('\n');
            for (Map.Entry<String, String> envEntry : envMap.entrySet()) {
                sb.append(envEntry.getKey());
                sb.append('=');
                sb.append(envEntry.getValue());
                sb.append('\n');
            }

            sb.append(']');
            sb.append('\n');

            return sb.toString();
        }

        //

        public VerifyInput copy() {
            return new VerifyInput(this);
        }

        public VerifyInput(VerifyInput other) {
            this(other.isMultiple);

            this.addKernel(other.kernel);
            this.addRoots(other.roots);
            this.addPlatForms(other.platforms);
            this.putAllEnvironment(other.envMap);
        }
    }

    public static enum ResultData {
        PLATFORM_RESOLVED("Resolved platforms"),
        PLATFORM_MISSING("Missing platforms"),
        PLATFORM_DUPLICATE("Conflicted platforms"),

        FEATURE_VERSIONLESS_RESOLVED("Versionless feature resolutions"),
        FEATURE_VERSIONLESS_NO_PLATFORM("Versionless features without platforms"),

        FEATURE_RESOLVED("Resolved features"),
        FEATURE_MISSING("Missing features"),
        FEATURE_NON_PUBLIC("Non-public features"),
        FEATURE_WRONG_PROCESS("Wrong process type features"),
        FEATURE_CONFLICT("Conflicted features");

        private ResultData(String description) {
            this.description = description;
        }

        public final String description;

        public String getDescription() {
            return description;
        }
    }

    public static class VerifyOutput {

        public VerifyOutput() {
            // EMPTY
        }

        public VerifyOutput copy() {
            return new VerifyOutput(this);
        }

        public VerifyOutput(VerifyOutput other) {
            for (ResultData valueType : ResultData.values()) {
                if (valueType == ResultData.FEATURE_VERSIONLESS_RESOLVED) {
                    this.putAllVersionlessResolved(other.getVersionlessResolved());
                } else {
                    this.addAll(valueType, other.get(valueType));
                }
            }
        }

        public EnumMap<ResultData, List<String>> resultData = new EnumMap<>(ResultData.class);

        public List<String> get(ResultData dataType) {
            List<String> result = resultData.get(dataType);
            if (result == null) {
                return Collections.emptyList();
            } else {
                return result;
            }
        }

        public void add(ResultData dataType, String value) {
            List<String> values = resultData.get(dataType);
            if (values == null) {
                values = new ArrayList<>();
                resultData.put(dataType, values);
            }
            values.add(value);
        }

        public void addAll(ResultData dataType, Collection<String> newValues) {
            List<String> values = resultData.get(dataType);
            if (values == null) {
                values = new ArrayList<>(newValues.size());
                resultData.put(dataType, values);
            }
            values.addAll(newValues);
        }

        //

        public void addResolved(String feature) {
            add(ResultData.FEATURE_RESOLVED, feature);
        }

        public List<String> getResolved() {
            return get(ResultData.FEATURE_RESOLVED);
        }

        //

        public Map<String, String> versionlessResolved = new HashMap<>();

        public Map<String, String> getVersionlessResolved() {
            return versionlessResolved;
        }

        public void putVersionlessResolved(String versionless, String versioned) {
            versionlessResolved.put(versionless, versioned);
        }

        public void putAllVersionlessResolved(Map<String, String> resolved) {
            for (Map.Entry<String, String> resolvedEntry : resolved.entrySet()) {
                System.out.println("Versionless resolution [ " + resolvedEntry.getKey() + "=" + resolvedEntry.getValue() + " ]");
            }
            versionlessResolved.putAll(resolved);
        }

        public String getVersionlessResolved(String versionless) {
            return versionlessResolved.get(versionless);
        }

        //

        public final List<String> kernelOnly = new ArrayList<>();

        public void addKernelOnly(String feature) {
            kernelOnly.add(feature);
        }

        public final List<String> kernelBlocked = new ArrayList<>();

        public void addKernelBlocked(String feature) {
            kernelBlocked.add(feature);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append('\n');
            sb.append("VerifyOutput [");

            for (ResultData valueType : ResultData.values()) {
                if (valueType == ResultData.FEATURE_VERSIONLESS_RESOLVED) {
                    list(sb, valueType.description, getVersionlessResolved());
                } else {
                    list(sb, valueType.description, get(valueType));
                }
            }

            list(sb, "Kernel only", kernelOnly);
            list(sb, "Kernel Blocked", kernelBlocked);

            sb.append(']');
            sb.append('\n');

            return sb.toString();
        }

        protected void list(StringBuilder sb, String description, Collection<?> values) {
            if (values.isEmpty()) {
                return;
            }

            sb.append('\n');

            sb.append(description);
            sb.append(':');
            sb.append('\n');

            for (Object value : values) {
                sb.append(value);
                sb.append('\n');
            }
        }

        protected void list(StringBuilder sb, String description, Map<?, ?> map) {
            if (map.isEmpty()) {
                return;
            }

            sb.append('\n');

            sb.append(description);
            sb.append(':');
            sb.append('\n');

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append(entry.getKey());
                sb.append('=');
                sb.append(entry.getValue());
                sb.append('\n');
            }
        }

        // PLATFORM_RESOLVED("Resolved platforms"),
        // Set<String> getResolvedPlatforms(); [ versioned platform ]

        // PLATFORM_MISSING("Missing platforms"),
        // Set<String> getMissingPlatforms(); [ versioned platform ]

        // PLATFORM_DUPLICATE("Duplicate platform versions"),
        // Map<String, Set<String>> getDuplicatePlatforms(); // unversioned platform -> [ versioned platform ]

        // FEATURE_VERSIONLESS_RESOLVED("Versionless feature resolutions"),
        // Map<String, String> getVersionlessFeatures(); // [ unversioned feature -> versioned feature | null ]

        // FEATURE_VERSIONLESS_NO_PLATFORM("Versionless features without platforms"),
        // Map<String, Set<String>> getNoPlatformVersionless(); // unversioned platform -> [ versionless features ]

        // FEATURE_RESOLVED("Resolved features"),
        // Set<String> getResolved(); // [ versioned ]

        // FEATURE_MISSING("Missing features"),
        // Set<String> getMissing(); [ feature ]

        // FEATURE_NON_PUBLIC("Non-public features"),
        // Set<String> getNonPublicRoots(); [ feature ]

        // FEATURE_WRONG_PROCESS("Wrong process type features"),
        // Map<String, Chain> getWrongProcessTypes(); [ feature ] -> chain

        // FEATURE_CONFLICT("Conflicted features");
        // Map<String, Collection<Chain>> getConflicts(); [ feature -> Collection<Chain> ]

        public VerifyOutput(FeatureResolver.Result result) {
            this();
            this.copy(result);
        }

        public void copy(FeatureResolver.Result result) {
            addAll(ResultData.PLATFORM_RESOLVED, result.getResolvedPlatforms());
            addAll(ResultData.PLATFORM_MISSING, result.getMissingPlatforms());

            // Versioned platforms an appear in at most one platform collection.
            // For now, don't check the versionless platform association.
            for (Set<String> platforms : result.getDuplicatePlatforms().values()) {
                addAll(ResultData.PLATFORM_DUPLICATE, platforms);
            }

            putAllVersionlessResolved(result.getVersionlessFeatures());

            // Versionless features can appear in at most one platform collection.
            // For now, don't check the platform association.
            for (Set<String> versionlessFeatures : result.getNoPlatformVersionless().values()) {
                addAll(ResultData.FEATURE_VERSIONLESS_NO_PLATFORM, versionlessFeatures);
            }

            addAll(ResultData.FEATURE_RESOLVED, result.getResolvedFeatures());
            addAll(ResultData.FEATURE_MISSING, result.getMissing());
            addAll(ResultData.FEATURE_NON_PUBLIC, result.getNonPublicRoots());

            // For now, don't check the chain that reached each problem feature.
            addAll(ResultData.FEATURE_WRONG_PROCESS, result.getWrongProcessTypes().keySet());

            // For now, don't check the chains that reach each problem feature.
            addAll(ResultData.FEATURE_CONFLICT, result.getConflicts().keySet());
        }
    }
}
