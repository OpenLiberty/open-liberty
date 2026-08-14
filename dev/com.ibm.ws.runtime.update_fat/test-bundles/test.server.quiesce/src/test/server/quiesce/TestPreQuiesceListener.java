/*******************************************************************************n * Copyright (c) 2026 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License 2.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0n *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package test.server.quiesce;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.threading.ThreadQuiesce;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 *
 */
@Component(immediate = true, configurationPid = "test.server.pre.quiesce", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TestPreQuiesceListener {
    private static final AtomicInteger nextId = new AtomicInteger();

    private final QuiesceListenerOrderRecordingService recorder;
    private final ExecutorService executorService;

    abstract class AbstractTestHook implements Runnable {
        private final int id;

        public AbstractTestHook() {
            this.id = nextId.getAndIncrement();
        }

        @Override
        public String toString() {
            return getClass().getName() + ": id=" + id;
        }

        public void register(BundleContext context) {
            context.registerService(ServerQuiesceListener.class, recorder.createQuiesceListener("PRE", this),
                                    FrameworkUtil.asDictionary(Collections.singletonMap("quiesce.phase", "PRE")));
        }

    }

    class ThrowExceptionHook extends AbstractTestHook {
        @Override
        public void run() {
            System.out.println("PRE quiesce listener throwing an exception: " + this);
            throw new RuntimeException("WOOPS! I was told to do this, honest." + this);
        }
    }

    class TakeForeverHook extends AbstractTestHook {
        @Override
        public void run() {
            System.out.println("PRE quiesce listener taking forever! " + this);

            //Rather than deal with slow hardware or possible timing windows, just wait forever
            //The server will still stop. But this gives it ample time to get to the timeout
            //without having to worry about failures that aren't really failures
            //This now relies on the quiesce thread pool to hit the timeout and shutdown
            while (true) {
            }
        }
    }

    class TestShutdownHook extends AbstractTestHook {
        @Override
        public void run() {
            System.out.println("Running: " + this);
        }

    }

    class StartThreadsAfterStopHook extends AbstractTestHook {
        @Override
        public void run() {
            System.out.println("PRE quiesce listener start threads after stop! " + this);
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

    @Activate
    public TestPreQuiesceListener(Map<String, Object> config, BundleContext context,
                                  @Reference QuiesceListenerOrderRecordingService recorder,
                                  @Reference ExecutorService executorService) {
        boolean throwException = (Boolean) config.get("throwException");
        boolean takeForever = (Boolean) config.get("takeForever");
        boolean startThreadsAfterStop = (Boolean) config.get("startThreadsAfterStop");
        boolean startThreadsWhileRunning = (Boolean) config.get("startThreadsWhileRunning");
        this.recorder = recorder;
        this.executorService = executorService;

        System.out.println("TEST PRE CONFIGURATION: " + config);
        AbstractTestHook hook1 = null;
        AbstractTestHook hook2 = null;
        AbstractTestHook hook3 = null;
        if (throwException) {
            hook1 = new ThrowExceptionHook();
            hook2 = new ThrowExceptionHook();
            hook3 = new ThrowExceptionHook();
        } else if (takeForever) {
            hook1 = new TakeForeverHook();
            hook2 = new TakeForeverHook();
            hook3 = new TakeForeverHook();
        } else if (startThreadsAfterStop) {
            hook1 = new StartThreadsAfterStopHook();
            hook2 = new StartThreadsAfterStopHook();
            hook3 = new StartThreadsAfterStopHook();
        } else {
            // everything else
            hook1 = new TestShutdownHook();
            hook2 = new TestShutdownHook();
            hook3 = new TestShutdownHook();
        }

        hook1.register(context);
        hook2.register(context);
        hook3.register(context);

        if (startThreadsWhileRunning) {
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
    }
}
