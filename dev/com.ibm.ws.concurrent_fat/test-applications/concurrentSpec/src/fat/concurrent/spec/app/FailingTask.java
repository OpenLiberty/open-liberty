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
package fat.concurrent.spec.app;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A simple task that fails to run.
 */
class FailingTask implements Callable<Integer>, Runnable {
    static final TraceComponent tc = Tr.register(FailingTask.class);

    static final int FAIL_IF_CALL_INVOKED = -101, FAIL_IF_RUN_INVOKED = -102;

    boolean failByInterrupt;
    final AtomicInteger numExecutions = new AtomicInteger();
    final int numExecutionToFailOn;

    FailingTask(int numExecutionToFailOn) {
        this.numExecutionToFailOn = numExecutionToFailOn;
    }

    @Override
    public void run() {
        int i = numExecutions.incrementAndGet();
        if (i == numExecutionToFailOn || numExecutionToFailOn == FAIL_IF_RUN_INVOKED) {
            Tr.debug(this, tc, "run and fail");
            if (failByInterrupt)
                try {
                    Thread.currentThread().interrupt();
                    Thread.sleep(1000); // this will fail with InterruptedException
                } catch (InterruptedException x) {
                    throw new RuntimeException("intentionally caused interruption", x);
                }
            else
                throw new ArithmeticException("intentionally caused failure");
        }
        Tr.debug(this, tc, "run " + i);
    }

    @Override
    public Integer call() throws IllegalAccessException, InterruptedException {
        int i = numExecutions.incrementAndGet();
        if (i == numExecutionToFailOn || numExecutionToFailOn == FAIL_IF_CALL_INVOKED) {
            Tr.debug(this, tc, "call and fail");
            if (failByInterrupt) {
                Thread.currentThread().interrupt();
                Thread.sleep(1000); // this will fail with InterruptedException
            } else
                throw new IllegalAccessException("intentionally caused failure");
        }
        Tr.debug(this, tc, "call " + i);
        return i;
    };
}