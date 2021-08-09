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
package web;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

/**
 * TimerTask that schedules another timer task, that schedules another timer task... and so forth.
 */
class RecursiveTimerTask extends TimerTask {
    private final AtomicInteger numExecutionsRemaining;
    private final AtomicLong partialResult;
    private final LinkedBlockingQueue<Object> result;
    private final Timer timer;
    private final UserTransaction tran;

    RecursiveTimerTask(AtomicInteger numExecutionsRemaining, AtomicLong partialResult, LinkedBlockingQueue<Object> result, Timer timer, UserTransaction tran) {
        this.numExecutionsRemaining = numExecutionsRemaining;
        this.partialResult = partialResult;
        this.result = result;
        this.timer = timer;
        this.tran = tran;
    }

    @Override
    public void run() {
        try {
            int count = numExecutionsRemaining.getAndDecrement();
            long sum = partialResult.addAndGet(count);
            System.out.println("RecursiveTimerTask.run countdown: " + count + ", sum: " + sum);

            // load a class from the application
            Thread.currentThread().getContextClassLoader().loadClass("web.DerbyRAAnnoServlet");

            // start a new transaction
            tran.begin();
            tran.commit();

            if (count <= 1) {
                // JCA defaults to the DefaultContextService, which includes jeeMetadataContext
                DataSource ds = (DataSource) new InitialContext().lookup("java:module/env/eis/ds1ref");
                ds.getConnection().close();

                result.add(sum);
            } else
                timer.schedule(new RecursiveTimerTask(numExecutionsRemaining, partialResult, result, timer, tran), new Date());

            cancel();
        } catch (Throwable x) {
            x.printStackTrace(System.out);
            result.add(x);
            cancel();
        }
    }
}
