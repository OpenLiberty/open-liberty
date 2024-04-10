/*******************************************************************************
 * Copyright (c) 2010, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.File;

/**
 * Interface for cached kernel bundle elements.
 */
public interface KernelBundleElement {
    String getSymbolicName();

    String getLocation();

    String getRangeString();

    File getCachedBestMatch();

    void setBestMatch(File bestMatch);

    int getStartLevel();

    String toNameVersionString();
}
