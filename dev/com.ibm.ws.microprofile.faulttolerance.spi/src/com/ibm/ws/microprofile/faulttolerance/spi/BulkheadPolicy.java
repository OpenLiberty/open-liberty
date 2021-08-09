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
package com.ibm.ws.microprofile.faulttolerance.spi;

/**
 * Wrap the execution and invoke it, limiting the number of threads used.
 */
public interface BulkheadPolicy {
    /**
     * The maximum number of threads which may execute the method at any one time.
     *
     * @return the maximum number of threads to use.
     *
     */
    public int getMaxThreads();

    public void setMaxThreads(int maxThreads);

    public int getQueueSize();

    public void setQueueSize(int queueSize);
}
