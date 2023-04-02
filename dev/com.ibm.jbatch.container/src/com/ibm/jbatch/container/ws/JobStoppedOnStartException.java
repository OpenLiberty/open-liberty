/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.ws;

import javax.batch.operations.JobStartException;

public class JobStoppedOnStartException extends JobStartException {
    
    private static final long serialVersionUID = 1L;

    public JobStoppedOnStartException() {
    }

    public JobStoppedOnStartException(String message) {
        super(message);
    }

    public JobStoppedOnStartException(Throwable cause) {
        super(cause);
    }

    public JobStoppedOnStartException(String message, Throwable cause) {
        super(message, cause);
    }
 
}
