/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal.fat.response;

public class JsonData {
    public String id;
    public String productEdition;
    public String productVersion;
    public String productName;
    public String[] features;
    public String javaVendor;
    public String javaVersion;
    public String os;
    public String osArch;
    public String[] iFixes;
}