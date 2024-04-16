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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Source restricted to java7.

public class VerifyData {

    public VerifyData() {
        this.cases = new ArrayList<>();
    }

    public VerifyData(List<LazySupplier<VerifyCase>> cases) {
        this.cases = new ArrayList<>(cases.size());

        for (LazySupplier<VerifyCase> verifyCase : cases) {
            this.cases.add(verifyCase.get());
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
            mappedCases.put(verifyCase.asKey(keyBuilder), verifyCase);
        }

        return mappedCases;
    }

    private static void append(StringBuilder builder, String text, char sep) {
        if (builder.length() != 0) {
            builder.append(sep);
        }
        builder.append(text);
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

    public static class VerifyCase {
        public String name;
        public String description;
        public long durationNs;

        public final VerifyInput input = new VerifyInput();
        public final VerifyOutput output = new VerifyOutput();

        public VerifyCase() {
            // EMPTY
        }

        public VerifyCase(VerifyCase inputCase, List<String> resolved, long durationNs) {
            this.name = inputCase.name;
            this.description = inputCase.description;

            this.durationNs = durationNs;

            if (inputCase.input.isMultiple) {
                this.input.setMultiple();
            }

            if (inputCase.input.isClient) {
                this.input.setClient();
            } else if (inputCase.input.isServer) {
                this.input.setServer();
            }

            for (String kernelName : inputCase.input.kernel) {
                this.input.addKernel(kernelName);
            }

            for (String rootName : inputCase.input.roots) {
                this.input.addRoot(rootName);
            }

            for (String featureName : resolved) {
                this.output.addResolved(featureName);
            }
        }

        public void setDuration(long startNs) {
            durationNs = getTimeNs() - startNs;
        }

        // Multiple Process:Client Kernel:kname1 Roots:rname1:rname2

        public String asKey(StringBuilder keyBuilder) {
            char spaceSep = ' ';
            char colonSep = ':';

            if (input.isMultiple) {
                append(keyBuilder, "Multiple", spaceSep);
            }

            append(keyBuilder, "Process", spaceSep);
            if (input.isClient) {
                append(keyBuilder, "Client", colonSep);
            }
            if (input.isServer) {
                append(keyBuilder, "Server", colonSep);
            }

            append(keyBuilder, "Kernel", spaceSep);
            for (String kernelName : input.kernel) {
                append(keyBuilder, kernelName, colonSep);
            }

            append(keyBuilder, "Roots", spaceSep);
            for (String rootName : input.roots) {
                append(keyBuilder, rootName, colonSep);
            }

            String key = keyBuilder.toString();

            keyBuilder.setLength(0);
            return key;
        }

        public void kernelAdjust(boolean usedKernel,
                                 Set<String> altResolved, boolean altUsedKernel) {

            VerifyDelta.kernelAdjust(this, usedKernel, altResolved, altUsedKernel);
        }
    }

    public static class VerifyInput {
        public boolean isMultiple;

        public boolean isClient;
        public boolean isServer;

        public final List<String> kernel = new ArrayList<>();
        public final List<String> roots = new ArrayList<>();

        public void setMultiple() {
            isMultiple = true;
        }

        public void setClient() {
            isClient = true;
        }

        public void setServer() {
            isServer = true;
        }

        public void addKernel(String feature) {
            kernel.add(feature);
        }

        public void addRoot(String name) {
            roots.add(name);
        }
    }

    public static class VerifyOutput {
        public final List<String> resolved = new ArrayList<>();

        public void addResolved(String feature) {
            resolved.add(feature);
        }

        public final List<String> kernelOnly = new ArrayList<>();

        public void addKernelOnly(String feature) {
            kernelOnly.add(feature);
        }

        public final List<String> kernelBlocked = new ArrayList<>();

        public void addKernelBlocked(String feature) {
            kernelBlocked.add(feature);
        }
    }
}
