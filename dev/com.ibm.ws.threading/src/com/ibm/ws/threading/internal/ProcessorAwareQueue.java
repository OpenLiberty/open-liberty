/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.threading.internal;

/**
 * A mix-in interface to allow for ExecutorService Queue implementations to
 * remove themselves from CPUInfo if it is registered as a AvailableProcessorsListener
 */
public interface ProcessorAwareQueue {

    /**
     * If registered as a AvailableProcessorsListener with CpuInfo, call
     * CpuInfo to unregister.
     */
    public void removeFromAvailableProcessors();
}
