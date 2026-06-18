/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package test.server.quiesce;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 *
 */
@Component(immediate = true, configurationPid = "test.server.quiesce")
public class TestQuiesceListener {

    final boolean throwException;
    final boolean takeForever;
    final boolean startThreadsAfterStop;
    final ExecutorService executorService;

    @Activate
    public TestQuiesceListener(BundleContext context, Map<String, Object> newConfig,
                               @Reference QuiesceListenerOrderRecordingService recorder,
                               @Reference ExecutorService executorService) {
        System.out.println("TEST CONFIGURATION: " + newConfig);

        this.executorService = executorService;
        throwException = (Boolean) newConfig.get("throwException");
        takeForever = (Boolean) newConfig.get("takeForever");
        startThreadsAfterStop = (Boolean) newConfig.get("startThreadsAfterStop");

        if ((Boolean) newConfig.get("startThreadsWhileRunning")) {
            // Start a couple of threads. These should block shutdown
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            };
            executorService.submit(r);
            executorService.submit(r);
        }
        context.registerService(ServerQuiesceListener.class, recorder.createQuiesceListener("DEFAULT", this::serverStopping), null);
    }

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
            // Start a couple of threads. These SHOULD block shutdown
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                }
            };
            executorService.submit(r);
            executorService.submit(r);
        }
    }

}
