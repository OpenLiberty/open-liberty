/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.util.List;

import com.ibm.wsspi.classloading.ClassLoaderConfiguration;

/**
 * This is an internal until we can agree on the attribute name for override libraries
 */
public interface ClassLoaderConfigurationExtended {
    ClassLoaderConfiguration setOverrideLibraries(List<String> libs);
    List<String> getOverrideLibraries();
}
