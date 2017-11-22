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
package com.ibm.ws.threading;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Exception that indicates a task was rejected or aborted due to exceeding its start timeout.
 */
public class StartTimeoutException extends IllegalStateException {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(StartTimeoutException.class, "concurrencyPolicy", "com.ibm.ws.threading.internal.resources.ThreadingMessages");

    public StartTimeoutException(String executorId, String taskName, long elapsedNS, long startTimeoutNS) {
        super(Tr.formatMessage(tc, "CWWKE1205.start.timeout", executorId, taskName, elapsedNS, startTimeoutNS));
        Tr.error(tc, "CWWKE1205.start.timeout", executorId, taskName, elapsedNS, startTimeoutNS);
    }
}
