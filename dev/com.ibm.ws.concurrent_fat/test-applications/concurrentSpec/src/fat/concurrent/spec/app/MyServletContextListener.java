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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.transaction.UserTransaction;

/**
 * Servlet context listener that schedules a task and expects Java EE Metadata context to be propagated to the task.
 */
@WebListener
public class MyServletContextListener implements ServletContextListener {
    static Throwable failure;
    static Future<ExecutorService> futureForTaskScheduledDuringContextInitialized;
    static LinkedBlockingQueue<Object> resultQueueForThreadStartedDuringContextInitialized = new LinkedBlockingQueue<Object>();

    @Override
    public void contextDestroyed(ServletContextEvent event) {}

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            ExecutorService executor = (ExecutorService) new InitialContext().lookup("concurrent/jeeMetadataContextSvc");
            futureForTaskScheduledDuringContextInitialized = executor.submit(new Callable<ExecutorService>() {
                @Override
                public ExecutorService call() throws Exception {
                    System.out.println("Running task scheduled from ServletContextListener");
                    UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                    System.out.println("The transaction is: " + tran);
                    tran.begin();
                    tran.commit();
                    ExecutorService xsvc = (ExecutorService) new InitialContext().lookup("java:module/env/schedxsvc-cl-ref");
                    System.out.println("The executor is: " + xsvc);
                    return xsvc;
                }
            });

            ThreadFactory threadFactory = (ThreadFactory) new InitialContext().lookup("java:comp/DefaultManagedThreadFactory");
            threadFactory.newThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ManagedScheduledExecutorService executor = (ManagedScheduledExecutorService) new InitialContext().lookup("java:module/env/schedxsvc-cl-ref");
                        System.out.println("Thread created by ServletContextListener.contextIntialized method looked up resource ref defined by the component and got: "
                                           + executor.toString());
                        resultQueueForThreadStartedDuringContextInitialized.add(executor);
                    } catch (Throwable x) {
                        x.printStackTrace();
                        resultQueueForThreadStartedDuringContextInitialized.add(x);
                    }
                }
            }).start();
        } catch (Throwable x) {
            x.printStackTrace(System.out);
            failure = x;
        }
    }

}
