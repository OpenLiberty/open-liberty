/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.clientpool.test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.collector.Client;
import com.ibm.ws.collector.clientpool.DummyClient;
import com.ibm.ws.collector.clientpool.DummyClientPool;

/**
 *
 */
public class ClientPoolTest {

    @Rule
    public final static TestName testName = new TestName();

    final static int POOL_SIZE = 4;
    final static int REASONABLE_TIME_LIMIT_IN_SECONDS = 5;

    private DummyClientPool clientPool;
    private Client[] checkedOutClients = null;

    private ExecutorService executor;
    private Future<?> taskRef = null;

    @Before
    public void setup() throws SSLException {
        log("*** STARTS ***");

        executor = Executors.newFixedThreadPool(POOL_SIZE);

        clientPool = new DummyClientPool("testSslConfig", null, POOL_SIZE);
        checkedOutClients = new Client[POOL_SIZE];
    }

    @Test
    public void testAlwaysPasses() {
        Assert.assertEquals("Pool queue size incorrect", POOL_SIZE, clientPool.getClientPoolQueueSize());
    }

    @Test
    public void testCheckOutCheckIn() {
        Client clientOne, clientTwo;

        // ** CheckOut one client
        clientOne = doCheckOut_withinReasonableTime();
        log("Checked out a client : " + clientOne);
        Assert.assertNotNull("Client checkout did NOT work", clientOne);
        Assert.assertEquals("Pool queue size incorrect", (POOL_SIZE - 1), clientPool.getClientPoolQueueSize());

        clientTwo = doCheckOut_withinReasonableTime();
        log("Checked out one more client : " + clientTwo);
        Assert.assertNotNull("Client checkout did NOT work", clientTwo);
        Assert.assertEquals("Pool queue size incorrect", (POOL_SIZE - 2), clientPool.getClientPoolQueueSize());

        // ** CheckIn clientOne
        log("Checkin the first client : " + clientOne);
        doCheckIn_withinReasonableTime(clientOne);
        Assert.assertEquals("Pool queue size incorrect", POOL_SIZE - 1, clientPool.getClientPoolQueueSize());

        // ** CheckIn clientTwo
        log("Checkin the 2nd client : " + clientOne);
        doCheckIn_withinReasonableTime(clientTwo);
        Assert.assertEquals("Pool queue size incorrect", POOL_SIZE, clientPool.getClientPoolQueueSize());

        // ** Check Out all clients
        log("Checkout all clients ");
        checkOutAllClients();
        Assert.assertEquals("There should not have been any client in the pool.", 0, clientPool.getClientPoolQueueSize());

        // ** Check Out all clients
        log("Checkin all clients ");
        checkInAllClients();
        Assert.assertEquals("Something wrong, all clients not checked in.", POOL_SIZE, clientPool.getClientPoolQueueSize());
    }

    // Note: The test 'testSameClientCheckInAgain' has been disabled because of
    //       Defect-'204668: Collector ClientPool allows checking-in of same client again'.
    //       The test should be enabled, when defect is fixed.
    @Test
    public void testSameClientCheckInAgain() {
        final Client clientOne, clientTwo;

        // ** CheckOut clientOne
        clientOne = doCheckOut_withinReasonableTime();
        log("Checked out a client : " + clientOne);
        Assert.assertNotNull("Client checkout did NOT work", clientOne);
        Assert.assertEquals("Pool queue size incorrect", (POOL_SIZE - 1), clientPool.getClientPoolQueueSize());

        // ** CheckOut clientTwo
        clientTwo = doCheckOut_withinReasonableTime();
        log("Checked out one more client : " + clientTwo);
        Assert.assertNotNull("Client checkout did NOT work", clientTwo);
        Assert.assertEquals("Pool queue size incorrect", (POOL_SIZE - 2), clientPool.getClientPoolQueueSize());

        // ** Check-In clientOne
        log("CheckIn the clientOne first time");
        doCheckIn_withinReasonableTime(clientOne);
        log("Client pool Size:  " + clientPool.getClientPoolQueueSize());
        Assert.assertEquals("Pool queue size incorrect", (POOL_SIZE - 1), clientPool.getClientPoolQueueSize());

        // ** Try Check-In same clientOne again (It should not check in)
        log("CheckIn the clientOne second time ");
        doCheckIn_withinReasonableTime(clientOne);
        log("Client pool Size:  " + clientPool.getClientPoolQueueSize());
        Assert.assertEquals("Something wrong, Pool queue size incorrect, same client checked-in twice into pool",
                            (POOL_SIZE - 1), clientPool.getClientPoolQueueSize());

        // ** Check-In clientTwo
        log("CheckIn the clientTwo ");
        doCheckIn_withinReasonableTime(clientTwo);
        log("Client pool Size:  " + clientPool.getClientPoolQueueSize());
        Assert.assertEquals("Pool queue size incorrect", (POOL_SIZE), clientPool.getClientPoolQueueSize());

        // **Check-In same clientOne third time (shouldn't check in).
        log("Trying to Re-CheckIn clientOne...");
        doCheckIn_withinReasonableTime(clientOne);
        Assert.assertEquals("Something wrong, Pool queue size incorrect, same client checked-in twice into pool",
                            (POOL_SIZE), clientPool.getClientPoolQueueSize());

    }

    /**
     * Verifies that checkOut will remain blocked, till a client is available back in the pool.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testCheckOutBlockedAndUnBlocked() throws InterruptedException, ExecutionException {
        // *** Check-Out all clients
        log("CheckOut all client, empty the pool");
        checkOutAllClients();
        Assert.assertEquals("There should not have been any client in the pool.", 0, clientPool.getClientPoolQueueSize());

        boolean result = false;

        /// *** Checkout blocked
        log("Try checkOut, from a empty pool");
        final Client[] aClientHolder = new Client[1];
        taskRef = executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                aClientHolder[0] = clientPool.checkoutClient();
                log("Client checkedout client=" + aClientHolder[0]);
                return true; // success;
            }
        });

        // *** Wait for reasonable time
        try {
            result = (Boolean) taskRef.get(REASONABLE_TIME_LIMIT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log("checkout task timed out : " + e.getMessage());
        }
        log("checkOut.result[1] : " + result);
        Assert.assertFalse("Something wrong, task cancelled(" + taskRef.isCancelled() + ") or completed(" + taskRef.isDone() + ") before time, result=" + result,
                           result);
        Assert.assertNull("Something wrong, client Checked-out from pool",
                          aClientHolder[0]);
        log("As expected, NO client checked out, and task is still blocked");

        // ** Now check-in a earlier checked-out client;
        log("CheckIn all clients, to make the clients available for the blocked checkout");
        checkInAllClients();

        // ** Now the blocked checkOut, should get unblocked
        try {
            result = (Boolean) taskRef.get(REASONABLE_TIME_LIMIT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("Blocked CheckOut, did get unblocked, after the clients were available, within reasonable time");
        }
        log("checkOut.result[2] : " + result);
        Assert.assertTrue("Something wrong, task NOT completed(" + taskRef.isDone() + ") within reasonable time (" + REASONABLE_TIME_LIMIT_IN_SECONDS + " secs), result=" + result,
                          result);
        Assert.assertNotNull("Something wrong, client NOT checkout from pool within reasonable time",
                             aClientHolder[0]);
        log("As expected, blocked task completed, the client has been checked out, client: " + aClientHolder[0]);

        doCheckIn_withinReasonableTime(aClientHolder[0]);
        Assert.assertEquals("Something wrong, all clients not checked in.", POOL_SIZE, clientPool.getClientPoolQueueSize());
    }

    /**
     * Start Checkout for all clients at same time, in multi-thread, similarly do check-ins
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testCheckOutCheckInMultiThreads() throws InterruptedException, ExecutionException {
        final List<Future<?>> checkoutTaskRefs = new ArrayList<Future<?>>(POOL_SIZE);
        final CyclicBarrier barrier = new CyclicBarrier(POOL_SIZE, new Runnable() {
            @Override
            public void run() {
                log("All tasks(" + POOL_SIZE + ") reached the barrier, now lets start ...");
            }
        });

        log("** Do CheckIn all clients");

        try {
            // *** CheckOut all clients, to empty the pool
            for (int i = 0; i < POOL_SIZE; i++) {
                final int i2 = i;
                // log("Checking client no = " + i);
                Future<?> taskRef = executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        log("Task[" + i2 + "] started & will wait at barrier for other tasks...");
                        try {
                            barrier.await();
                        } catch (InterruptedException e) {
                            log("barrier for task[" + i2 + "] interrupted : " + e.getMessage());
                        } catch (BrokenBarrierException e) {
                            log("barrier for task[" + i2 + "] broken : " + e.getMessage());
                        }

                        // Do CheckOut Client
                        log("Task[" + i2 + "] : client checkout");
                        checkedOutClients[i2] = clientPool.checkoutClient();
                        log("Task[" + i2 + "] : client=" + checkedOutClients[i2]);
                    }
                });
                checkoutTaskRefs.add(taskRef);
            }

            // Wait for some reasonable time, then cancel the tasks
            long MAX_TIME = REASONABLE_TIME_LIMIT_IN_SECONDS * POOL_SIZE * 1000;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < POOL_SIZE; i++) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                Future<?> task = checkoutTaskRefs.get(i);
                if (elapsedTime < MAX_TIME) {
                    // 207252: Mimic CountDownLatch timer with MAX_TIME
                    try {
                        task.get(MAX_TIME - elapsedTime, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        log("checkout task [" + i + "] timed out : " + e.getMessage());
                    }
                }
                if (!task.isDone()) {
                    Assert.fail("Task [" + i + "] not done");
                }
            }

            log("** Do CheckIn all clients");
            barrier.reset();

            for (int i = 0; i < POOL_SIZE; i++) {
                final int i2 = i;
                Future<?> taskRef = executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        log("Task[" + i2 + "] started & will wait at barrier for other tasks...");
                        try {
                            barrier.await();
                        } catch (InterruptedException e) {
                            log("barrier for task[" + i2 + "] interrupted : " + e.getMessage());
                        } catch (BrokenBarrierException e) {
                            log("barrier for task[" + i2 + "] broken : " + e.getMessage());
                        }

                        // Do CheckOut Client
                        log("Task[" + i2 + "] client checkin ");
                        clientPool.checkinClient(checkedOutClients[i2]);
                        log("Task[" + i2 + "] Done checkIn client.id=" + ((DummyClient) checkedOutClients[i2]).getId());
                    }
                });
                checkoutTaskRefs.add(taskRef);
            }

            // Wait for some reasonable time, then cancel the tasks
            startTime = System.currentTimeMillis();

            for (int i = 0; i < POOL_SIZE; i++) {
                long elapsedTime = System.currentTimeMillis() - startTime;

                Future<?> task = checkoutTaskRefs.get(i);
                if (elapsedTime < MAX_TIME) {
                    // 207252: Mimic CountDownLatch timer with MAX_TIME
                    try {
                        task.get(MAX_TIME - elapsedTime, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        log("checkin task [" + i + "] timed out : " + e.getMessage());
                    }
                }
                if (!task.isDone()) {
                    Assert.fail("Task [" + i + "] not done");
                }
            }

        } finally {
            for (int i = 0; i < POOL_SIZE; i++) {
                Future<?> task = checkoutTaskRefs.get(i);
                if (!task.isDone()) {
                    try {
                        log("Cancel task " + i);
                        task.cancel(true);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

    }

    @After
    public void cleanUp() {
        final String testname = testName.getMethodName();
        Future<Boolean> future = null;

        try {
            if (taskRef != null) {
                if (!taskRef.isDone()) {
                    taskRef.cancel(true);
                }
                taskRef = null;
            }

            log("Closing clientPool...");
            long start = System.currentTimeMillis();
            boolean clientPoolClosed = false;

            try {
                future = executor.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        clientPool.close();
                        log("Closed clientPool.");
                        return true;
                    }
                });
                clientPoolClosed = future.get(REASONABLE_TIME_LIMIT_IN_SECONDS, TimeUnit.SECONDS);
                log("got closed result in millis=" + ((System.currentTimeMillis() - start)));
            } catch (TimeoutException e) {
                log("Close timeout in secs=" + ((System.currentTimeMillis() - start) / 1000));
                log("clientPool.close task timed out : " + e.getMessage());
            } catch (InterruptedException e) {
                log("clientPool.close task Interputed : " + e.getMessage());
            } catch (ExecutionException e) {
                log("clientPool.close task Exception : " + e.getMessage());
            }
            Assert.assertTrue("ClientPool did not close cleanly for " + testname, clientPoolClosed);

        } finally {
            if (future != null && !future.isDone()) {
                log("Close task will be cancelled ...");
                boolean result = false;
                try {
                    result = future.cancel(true);
                } catch (Exception e) {
                    log("task failed : " + e.getMessage());
                }
                log("Is close task cancelled : " + result);
            }

            // just to ensure close(loop) ends, we increment private counter, so that closed is forced.
            clientPool.incrementNumClientsClosed(POOL_SIZE);

            // shutdown
            try {
                log("shutdown executor");
                executor.shutdown();
                executor.awaitTermination(REASONABLE_TIME_LIMIT_IN_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log("There was some interruption : " + e.getMessage());
            } catch (Throwable t) {
                log("Shutdown failed : " + t.getMessage());
            }

            checkedOutClients = null;
            clientPool = null;

            log("*** ENDS ***");
        }
    }

    /**
     * @param testname
     */
    private void checkOutAllClients() {
        // *** CheckOut all clients, to empty the pool
        for (int i = 0; i < POOL_SIZE; i++) {
            // log("Checking client no = " + i);
            checkedOutClients[i] = doCheckOut_withinReasonableTime();
            log("CheckedOut client : " + checkedOutClients[i].toString());
        }
    }

    /**
     * @param testname
     */
    private void checkInAllClients() {
        // *** CheckOut all clients, to empty the pool
        for (int i = 0; i < POOL_SIZE; i++) {
            if (checkedOutClients[i] != null) {
                doCheckIn_withinReasonableTime(checkedOutClients[i]);
                log("Checked-In client.id : " + ((DummyClient) checkedOutClients[i]).getId());
                checkedOutClients[i] = null;
            }
        }
    }

    private boolean doCheckIn_withinReasonableTime(final Client client) {
        boolean checkInSuccessful = false;
        Future<Boolean> future = null;
        try {
            try {
                future = executor.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        clientPool.checkinClient(client);
                        return true;
                    }
                });
                checkInSuccessful = future.get(REASONABLE_TIME_LIMIT_IN_SECONDS, TimeUnit.SECONDS);
                log("Client checkIn result.success=" + checkInSuccessful);
            } catch (TimeoutException e) {
                log("CheckIn task timed out : " + e.getMessage());
            } catch (InterruptedException e) {
                log("CheckIn task Interputed : " + e.getMessage());
            } catch (ExecutionException e) {
                log("CheckIn Exception : " + e.getMessage());
            }

            Assert.assertTrue("Sometime wrong, Client CheckIn is blocked, did not complete in reasonable time (" + REASONABLE_TIME_LIMIT_IN_SECONDS + "secs)",
                              checkInSuccessful);
            log("Client checked-in: client.id=" + ((DummyClient) client).getId());
        } finally {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
        return checkInSuccessful;
    }

    private Client doCheckOut_withinReasonableTime() {
        Client client = null;
        Future<Client> future = null;
        try {
            try {
                future = executor.submit(new Callable<Client>() {
                    @Override
                    public Client call() {
                        return clientPool.checkoutClient();
                    }
                });
                client = future.get(REASONABLE_TIME_LIMIT_IN_SECONDS, TimeUnit.SECONDS);
                log("Client checkOut result.success");
            } catch (TimeoutException e) {
                log("CheckOut task timed out : " + e.getMessage());
            } catch (InterruptedException e) {
                log("CheckOut task Interputed : " + e.getMessage());
            } catch (ExecutionException e) {
                log("CheckOut Exception : " + e.getMessage());
            }

            Assert.assertNotNull("Sometime wrong, Client CheckOut is blocked, did not complete in reasonable time (" + REASONABLE_TIME_LIMIT_IN_SECONDS + "secs)",
                                 client);
            log("Client checked-out: client.id=" + ((DummyClient) client).getId());
        } finally {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
        return client;
    }

    // private static final Class c = ClientPoolTest.class;
    // private static final Logger logger = Logger.getLogger(c.getSimpleName());

    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static void log(String message) {
        final String testname = testName.getMethodName();
        String timestamp = dateFormat.format(new Date());
        System.out.println(timestamp + " [" + testname + "] " + message);

        // logger.logp(Level.INFO, c.getSimpleName(), testname, message);
    }
}
