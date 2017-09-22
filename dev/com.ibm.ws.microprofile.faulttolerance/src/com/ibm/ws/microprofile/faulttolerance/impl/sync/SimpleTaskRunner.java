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
package com.ibm.ws.microprofile.faulttolerance.impl.sync;

import java.util.concurrent.Callable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.impl.ExecutionContextImpl;
import com.ibm.ws.microprofile.faulttolerance.impl.TaskRunner;
import com.ibm.ws.microprofile.faulttolerance.utils.FTDebug;

/**
 * SimpleTaskRunner will call the task and end the execution afterwards
 */
public class SimpleTaskRunner<R> implements TaskRunner<R> {

    private static final TraceComponent tc = Tr.register(SimpleTaskRunner.class);

    @Override
    @FFDCIgnore(InterruptedException.class)
    public R runTask(Callable<R> task, ExecutionContextImpl executionContext) throws Exception {
        R result = null;
        try {
            result = task.call();
        } catch (InterruptedException e) {
            //if the interrupt was caused by a timeout then check and throw that instead (which is what check does)
            long remaining = executionContext.check();
            FTDebug.debugTime(tc, "Task Interrupted", remaining);
            throw e;
        } finally {
            executionContext.end();
        }

        return result;
    }

}
