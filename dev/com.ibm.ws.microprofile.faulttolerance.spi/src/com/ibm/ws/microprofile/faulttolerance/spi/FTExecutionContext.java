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

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

/**
 * The Execution Context keeps track of execution state. It may only be used once.
 */
public interface FTExecutionContext extends ExecutionContext {

    /**
     * Close the ExecutionContext and release all resources used by the execution.
     */
    public void close();

}
