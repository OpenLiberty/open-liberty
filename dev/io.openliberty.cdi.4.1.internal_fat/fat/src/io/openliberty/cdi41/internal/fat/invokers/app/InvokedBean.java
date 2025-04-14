/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi41.internal.fat.invokers.app;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InvokedBean {

    public String basicMethod(int i) {
        return "Hello: " + i;
    }

    public String referenceMethod(CharSequence name) {
        return "Hello: " + name;
    }

    public String varargsMethod(int i, String... strings) {
        return "Hello: " + i + " " + String.join(",", strings);
    }

    public String noargMethod() {
        return "Hello";
    }
}
