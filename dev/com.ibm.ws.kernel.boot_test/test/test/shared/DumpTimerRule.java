/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.shared;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.ws.kernel.boot.internal.commands.JavaDumpAction;
import com.ibm.ws.kernel.boot.internal.commands.JavaDumper;

/**
 *
 */
public class DumpTimerRule implements TestRule {

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final long timeoutMillis;
    private final File outputDir;
    private volatile boolean complete;

    public DumpTimerRule(long timeoutMillis, File outputDir) {
        this.timeoutMillis = timeoutMillis;
        this.outputDir = outputDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.junit.rules.TestRule#apply(org.junit.runners.model.Statement, org.junit.runner.Description)
     */
    @Override
    public Statement apply(Statement statement, Description arg1) {
        return new DumpTimerStatement(statement);
    }

    private class DumpTimerStatement extends Statement {
        private final Statement statement;

        private DumpTimerStatement(Statement statement) {
            this.statement = statement;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.junit.runners.model.Statement#evaluate()
         */
        @Override
        public void evaluate() throws Throwable {
            DumpThreads dumper = new DumpThreads();
            ScheduledFuture<?> future = scheduler.schedule(dumper, timeoutMillis, TimeUnit.MILLISECONDS);
            try {
                statement.evaluate();
            } finally {
                complete = true;
                future.cancel(false);
            }

        }

    }

    private class DumpThreads implements Runnable {

        @Override
        public void run() {
            if (!complete) {
                JavaDumper.getInstance().dump(JavaDumpAction.THREAD, outputDir);
            }

        }

    }
}
