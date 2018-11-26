/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.quiesce;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.threading.ThreadQuiesce;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 *
 */
@Component(immediate = true, configurationPid = "test.server.quiesce")
public class TestQuiesceListener implements ServerQuiesceListener {

    boolean throwException = false;
    boolean takeForever = false;
    boolean startThreadsAfterStop = false;
    ExecutorService executorService;

    @Activate
    protected void activate(Map<String, Object> newConfig) {
        System.out.println("TEST CONFIGURATION: " + newConfig);

        throwException = (Boolean) newConfig.get("throwException");
        takeForever = (Boolean) newConfig.get("takeForever");
        startThreadsAfterStop = (Boolean) newConfig.get("startThreadsAfterStop");

        if ((Boolean) newConfig.get("startThreadsWhileRunning")) {
            // Start a couple of threads. These should block shutdown
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    while (true) {
                    }

                }
            };
            executorService.submit(r);
            executorService.submit(r);
        }

    }

    @Override
    public void serverStopping() {

        System.out.println("WHEE! THE SERVER IS STOPPING AND I GOT TOLD!");

        if (throwException) {
            throw new RuntimeException("WOOPS! I was told to do this, honest.");
        }

        if (takeForever) {
            System.out.println("MUAHAHA.. I will now take forever to quiesce (literally)!");

            //Rather than deal with slow hardware or possible timing windows, just wait forever
            //The server will still stop. But this gives it ample time to get to the timeout
            //without having to worry about failures that aren't really failures
            //This now relies on the quiesce thread pool to hit the timeout and shutdown
            while (true) {
            }
        }

        if (startThreadsAfterStop) {
            // Normally the executor service will already be quiescing at this point, but wait just in case
            for (int i = 0; i < 20; i++) {
                if (((ThreadQuiesce) executorService).quiesceStarted()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // Start a couple of threads. These should not block shutdown
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    while (true) {
                    }

                }
            };
            executorService.submit(r);
            executorService.submit(r);
        }
    }

    @Reference(service = ExecutorService.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

}
