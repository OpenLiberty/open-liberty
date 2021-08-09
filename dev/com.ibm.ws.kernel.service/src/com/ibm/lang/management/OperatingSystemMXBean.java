/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Mirror APIs on IBM Java OperatingSystemMXBean for compile
 *******************************************************************************/
package com.ibm.lang.management;

public interface OperatingSystemMXBean extends java.lang.management.OperatingSystemMXBean {
    public long getProcessCpuTime();
    public double getSystemCpuLoad();
}