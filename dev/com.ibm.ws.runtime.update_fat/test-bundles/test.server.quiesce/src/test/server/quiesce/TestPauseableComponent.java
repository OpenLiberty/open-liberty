/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.kernel.launch.service.PauseableComponent;
import com.ibm.ws.kernel.launch.service.PauseableComponentException;
import com.ibm.ws.threading.ThreadQuiesce;

//Note: This is the test implementation of PauseableComponent for FAT testing which implements PauseableComponent interface.
@Component(immediate = true, configurationPid = "test.server.pauseable")
public class TestPauseableComponent implements PauseableComponent {

    private static final String COMPONENT_NAME = "TestPauseableComponent";

    boolean throwException = false;
    boolean takeForever = false;
    boolean startThreadsAfterPause = false;
    boolean isPaused = false;
    ExecutorService executorService;

    @Activate
    protected void activate(Map<String, Object> newConfig) {
        System.out.println("TEST CONFIGURATION: " + newConfig);

        throwException = (Boolean) newConfig.get("throwException");
        takeForever = (Boolean) newConfig.get("takeForever");
        startThreadsAfterPause = (Boolean) newConfig.get("startThreadsAfterPause");

        if ((Boolean) newConfig.get("startThreadsWhileRunning")) {
            // Start a couple of threads. These should block pause
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            executorService.submit(r);
            executorService.submit(r);
        }

    }

    @Override
    public String getName() {
        return COMPONENT_NAME;
    }

    @Override
    public void pause() throws PauseableComponentException {
        System.out.println("WHEE! THE COMPONENT IS PAUSING AND I GOT TOLD!");

        if (throwException) {
            throw new RuntimeException("WOOPS! I was told to do this, honest.");
        }

        if (takeForever) {
            System.out.println("MUAHAHA.. I will now take forever to pause (literally)!");

            // Rather than deal with slow hardware or possible timing windows, just wait forever
            // The server will still stop. But this gives it ample time to get to the timeout
            // without having to worry about failures that aren't really failures
            // This now relies on the pause thread pool to hit the timeout and shutdown
            // Make the loop interruptible so the server can forcefully terminate if needed
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                System.out.println("TestPauseableComponent: Pause operation interrupted during takeForever");
                Thread.currentThread().interrupt(); // Restore interrupt status
            }
        }

        if (startThreadsAfterPause) {
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
            // Start a couple of threads. These should not block pause
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            executorService.submit(r);
            executorService.submit(r);
        }

        isPaused = true;

    }

    @Override
    public void resume() throws PauseableComponentException {
        System.out.println("WHEE! THE COMPONENT IS RESUMING AND I GOT TOLD!");

        if (throwException) {
            throw new RuntimeException("WOOPS! I was told to throw on resume too!");
        }

        isPaused = false;
    }

    @Override
    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public HashMap<String, String> getExtendedInfo() {
        HashMap<String, String> info = new HashMap<>();
        info.put("componentName", COMPONENT_NAME);
        info.put("isPaused", String.valueOf(isPaused));
        info.put("throwException", String.valueOf(throwException));
        info.put("takeForever", String.valueOf(takeForever));
        return info;
    }

    @Reference(service = ExecutorService.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}