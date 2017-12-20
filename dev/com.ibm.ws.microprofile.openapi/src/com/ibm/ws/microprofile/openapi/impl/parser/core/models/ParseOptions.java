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

package com.ibm.ws.microprofile.openapi.impl.parser.core.models;

public class ParseOptions {
    private boolean resolve;
    private boolean resolveCombinators = true;
    private boolean resolveFully;
    private boolean flatten;

    public boolean isResolve() {
        return resolve;
    }

    public void setResolve(boolean resolve) {
        this.resolve = resolve;
    }

    public boolean isResolveCombinators() {
        return resolveCombinators;
    }

    public void setResolveCombinators(boolean resolveCombinators) {
        this.resolveCombinators = resolveCombinators;
    }

    public boolean isResolveFully() {
        return resolveFully;
    }

    public void setResolveFully(boolean resolveFully) {
        this.resolveFully = resolveFully;
    }

    public boolean isFlatten() {
        return flatten;
    }

    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
    }
}
