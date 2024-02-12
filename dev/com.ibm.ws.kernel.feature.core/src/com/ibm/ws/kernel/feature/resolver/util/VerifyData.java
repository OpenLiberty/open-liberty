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
import java.util.List;
import java.util.stream.Stream;

public class VerifyData {
    public final List<VerifyCase> cases = new ArrayList<>();

    public VerifyData() {
        // Empty
    }

    public VerifyData(Stream<VerifyCase> cases) {
        cases.forEach((VerifyCase verifyCase) -> this.cases.add(verifyCase));
    }

    public VerifyCase addCase() {
        VerifyCase Case = new VerifyCase();
        cases.add(Case);
        return Case;
    }

    public static class VerifyCase {
        public String name;
        public String description;

        public final VerifyInput input = new VerifyInput();
        public final VerifyOutput output = new VerifyOutput();
    }

    public static class VerifyInput {
        public boolean isClient;
        public boolean isServer;

        public final List<String> kernel = new ArrayList<>();
        public final List<String> roots = new ArrayList<>();

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
    }
}
