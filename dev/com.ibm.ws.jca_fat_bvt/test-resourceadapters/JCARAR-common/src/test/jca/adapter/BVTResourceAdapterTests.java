/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package test.jca.adapter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.IndexedRecord;
import jakarta.resource.cci.ResultSet;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.work.ExecutionContext;
import jakarta.resource.spi.work.HintsContext;
import jakarta.resource.spi.work.SecurityContext;
import jakarta.resource.spi.work.WorkCompletedException;
import jakarta.resource.spi.work.WorkContext;
import jakarta.resource.spi.work.WorkContextErrorCodes;
import jakarta.resource.spi.work.WorkEvent;
import jakarta.resource.spi.work.WorkManager;
import jakarta.resource.spi.work.WorkRejectedException;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;

/**
 * Test cases that run from the resource adapter
 */
public class BVTResourceAdapterTests {

    // Interval (in milliseconds) up to which tests should wait for a single task to run
    private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(2);

    /**
     * Tests the JCA timer.
     * Schedule a repeating timer task that cancels itself on the third run.
     * Schedule a one-shot timer task and cancel the timer before it starts.
     */
    static void testTimer(BootstrapContext bootstrapContext) throws Throwable {

        final Timer timer = bootstrapContext.createTimer();
        final WorkManager workManager = bootstrapContext.getWorkManager();

        final BVTWork<String> work = new BVTWork<String>() {
            @Override
            public String call() throws Exception {
                return "running " + this;
            }
        };

        final BlockingQueue<Object> timerResults = new LinkedBlockingQueue<Object>();
        TimerTask task = new TimerTask() {

            private int runs;

            @Override
            public void run() {
                ++runs;
                System.out.println("Entry:run #" + runs);
                try {
                    switch (runs) {
                        case 1:
                            workManager.doWork(work);
                            break;
                        case 2:
                            workManager.scheduleWork(work);
                            break;
                        case 3:
                            long elapsedTime = workManager.startWork(work);
                            if (elapsedTime < -1)
                                throw new Exception("Negative start duration: " + elapsedTime);
                        default:
                            System.out.println("canceling timer task");
                            cancel();
                    }
                    timerResults.add("Successful run #" + runs + " of " + this);
                    System.out.println("Exit:run # " + runs);
                } catch (Throwable x) {
                    System.out.println("Exit:run #" + runs + ":" + x);
                    timerResults.add(x);
                }
            }
        };

        Object result;
        try {
            timer.schedule(task, 0, 100);

            result = timerResults.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (result == null)
                throw new Exception("Timer did not fire once.");
            else if (result instanceof Throwable)
                throw (Throwable) result;

            result = work.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (result == null)
                throw new Exception("Work did not run once.");

            result = timerResults.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (result == null)
                throw new Exception("Timer did not fire twice.");
            else if (result instanceof Throwable)
                throw (Throwable) result;

            result = work.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (result == null)
                throw new Exception("Work did not run twice.");

            result = timerResults.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (result == null)
                throw new Exception("Timer did not fire 3 times.");
            else if (result instanceof Throwable)
                throw (Throwable) result;

            result = work.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (result == null)
                throw new Exception("Work did not run 3 times.");

            result = timerResults.poll(1000, TimeUnit.MILLISECONDS);
            if (result instanceof Throwable)
                throw (Throwable) result;
            else if (result != null)
                throw new Exception("Timer cancel not honored. Extra result is: " + result);

            timer.purge();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timerResults.add("running " + this + " which should have been canceled");
                }
            }, 2000);
        } finally {
            timer.cancel();
        }

        result = timerResults.poll(4000, TimeUnit.MILLISECONDS);
        if (result instanceof Throwable)
            throw (Throwable) result;
        else if (result != null)
            throw new Exception("Task should not fire after timer is canceled");
    }

    /**
     * Tests thread context propagation for the JCA work manager.
     */
    static void testWorkContext(BootstrapContext bootstrapContext) throws Exception {

        WorkManager workManager = bootstrapContext.getWorkManager();

        // requires component context
        BVTWork<ConnectionFactory> lookupConnectionFactory = new BVTWork<ConnectionFactory>() {
            @Override
            public ConnectionFactory call() throws Exception {
                return (ConnectionFactory) new InitialContext().lookup("java:comp/env/eis/cf1");
            }
        };

        long startDuration = workManager.startWork(lookupConnectionFactory);
        if (startDuration < WorkManager.UNKNOWN || startDuration > TIMEOUT)
            throw new Exception("It took an unreasonably long time to start the work: " + startDuration + " ms");

        final ConnectionFactory cf = lookupConnectionFactory.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        cf.getRecordFactory();

        // requires component context and transaction context
        BVTWork<?> getSubString = new BVTWork<String>() {
            @SuppressWarnings("unchecked")
            @Override
            public String call() throws Exception {
                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    Connection con = cf.getConnection();
                    IndexedRecord input = cf.getRecordFactory().createIndexedRecord("inputParams");
                    input.add("VALUES SUBSTR(?, ?, ?)");
                    input.add("Running testWorkContext");
                    input.add(9);
                    input.add(15);
                    ResultSet result = (ResultSet) con.createInteraction().execute(null, input);
                    String substring = result.next() ? result.getString(1) : null;
                    con.close();
                    return substring;
                } finally {
                    tran.commit();
                }
            }
        };

        workManager.scheduleWork(getSubString);

        Object result = getSubString.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result instanceof Throwable)
            throw new Exception("Work failed. See cause.", (Throwable) result);
        if (!"testWorkContext".equals(result))
            throw new Exception("Incorrect substring returned by contextual work: " + result);
    }

    /**
     * Tests work context inflow.
     */
    static void testWorkContextInflow(BootstrapContext bootstrapContext) throws Exception {

        WorkManager workManager = bootstrapContext.getWorkManager();

        java.lang.ClassLoader cl = bootstrapContext.getClass().getClassLoader();
        Class<?> frameworkUtil = cl.loadClass("org.osgi.framework.FrameworkUtil");
        Method getBundle = frameworkUtil.getMethod("getBundle", Class.class);
        Object bundle = getBundle.invoke(null, bootstrapContext.getClass());
        Method getBundleContext = bundle.getClass().getMethod("getBundleContext", (Class<?>[]) null);
        Object bundleContext = getBundleContext.invoke(bundle, (Object[]) null);
        Method getServiceReferences = bundleContext.getClass().getMethod("getServiceReferences", Class.class, String.class);
        Collection<?> collectionSvcRefs = (Collection<?>) getServiceReferences.invoke(bundleContext, Collection.class,
                                                                                      "(component.name=test.jca.workcontext.CollectionContextProvider)");
        Object collectionSvcRef = collectionSvcRefs.iterator().next();
        Class<?> srref = cl.loadClass("org.osgi.framework.ServiceReference");
        Method getService = bundleContext.getClass().getMethod("getService", srref);
        @SuppressWarnings("unchecked")
        final Collection<String> collectionSvc = (Collection<String>) getService.invoke(bundleContext, collectionSvcRef);
        try {
            ClassLoader loader = collectionSvc.getClass().getClassLoader();
            @SuppressWarnings("unchecked")
            Class<? extends WorkContext> CollectionContext = (Class<? extends WorkContext>) loader.loadClass("test.jca.workcontext.CollectionContext");
            @SuppressWarnings("unchecked")
            Class<? extends WorkContext> UnsupportedContext = (Class<? extends WorkContext>) loader.loadClass("test.jca.workcontext.UnsupportedContext");

            // BootstrapContext.isContextSupported
            if (!bootstrapContext.isContextSupported(CollectionContext))
                throw new Exception("CollectionContext should be supported as a work context");
            if (bootstrapContext.isContextSupported(SecurityContext.class))
                throw new Exception("SecurityContext should not be supportd when the feature is not enabled");

            Collection<String> result;
            BVTWorkListener workListener = new BVTWorkListener();
            BVTWorkWithContext<Collection<String>> work = new BVTWorkWithContext<Collection<String>>() {
                private static final long serialVersionUID = 1L;

                @Override
                public Collection<String> call() {
                    return new LinkedList<String>(collectionSvc);
                }
            };

            // Put context onto the current thread
            collectionSvc.add("testing123");

            // doWork with empty inflow context
            work.setWorkContexts(); // empty list
            workManager.doWork(work);
            result = work.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!result.contains("testing123") || result.size() > 1)
                throw new Exception("CollectionContext should be unchanged when no work inflow context is specified. Instead: " + result);

            // doWork with inflow context
            WorkContext collectionContext = CollectionContext.getConstructor(String[].class).newInstance((Object) new String[] { "item1", "item2" });
            work.setWorkContexts(collectionContext);
            workManager.doWork(work);
            result = work.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!result.contains("item1") || !result.contains("item2") || result.size() > 2)
                throw new Exception("Inflow context (doWork) does not match CollectionContext of execution thread: " + result);
            int count = (Integer) CollectionContext.getMethod("getContextSetupsCompleted").invoke(collectionContext);
            if (count != 1)
                throw new Exception("Should have exactly one context setup notification, not " + count);

            // Verify that inflow context was removed after doWork completes
            if (!collectionSvc.contains("testing123") || collectionSvc.size() > 1)
                throw new Exception("Context of submitter thread not properly restored after doWork: " + collectionSvc);

            // scheduleWork
            collectionContext = CollectionContext.getConstructor(String[].class).newInstance((Object) new String[] { "item3" });
            work.setWorkContexts(collectionContext);
            workManager.scheduleWork(work);

            // startWork
            long startDuration = workManager.startWork(work);
            if (startDuration < 0 && startDuration != WorkManager.UNKNOWN)
                throw new Exception("Invalid start duration: " + startDuration);

            result = work.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!result.contains("item3") || result.size() > 1)
                throw new Exception("Inflow context does not match CollectionContext of execution thread: " + result);

            result = work.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!result.contains("item3") || result.size() > 1)
                throw new Exception("Inflow context does not match CollectionContext of execution thread: " + result);

            count = (Integer) CollectionContext.getMethod("getContextSetupsCompleted").invoke(collectionContext);
            if (count != 2)
                throw new Exception("Should have exactly two context setup notifications, not " + count);

            // Use an unsupported type of WorkContext
            WorkContext unsupportedContext = UnsupportedContext.newInstance();
            work.setWorkContexts(collectionContext, unsupportedContext);
            workListener.clear();
            try {
                startDuration = workManager.startWork(work, TIMEOUT, null, workListener);
                if (startDuration < WorkManager.UNKNOWN || startDuration > TIMEOUT)
                    throw new Exception("It took an unreasonably long time to start the work: " + startDuration + " ms");
            } catch (WorkCompletedException x) {
                if (!WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE.equals(x.getErrorCode()))
                    throw new Exception("Incorrect error code is " + x.getErrorCode(), x);
                if (x.getMessage().indexOf("J2CA8625") < 0)
                    throw new Exception(x);
            }

            WorkEvent event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_ACCEPTED)
                throw new Exception("Expected WORK_ACCEPTED, not: " + event.getType());

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_STARTED)
                throw new Exception("Expected WORK_STARTED, not: " + event.getType());

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_COMPLETED)
                throw new Exception("Expected WORK_COMPLETED, not: " + event.getType());
            String errorCode = event.getException().getErrorCode();
            if (!WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE.equals(errorCode))
                throw new Exception("Expected UNSUPPORTED_CONTEXT_TYPE, not: " + errorCode, event.getException());
            if (event.getException().getMessage() == null
                || event.getException().getMessage().indexOf("J2CA8625") < 0)
                throw new Exception(event.getException());

            String[] failures = (String[]) UnsupportedContext.getMethod("getContextSetupFailures").invoke(unsupportedContext);
            if (failures.length != 1)
                throw new Exception("Expected exactly one context failure, not " + Arrays.asList(failures));
            if (!WorkContextErrorCodes.UNSUPPORTED_CONTEXT_TYPE.equals(failures[0]))
                throw new Exception("Expected UNSUPPORTED_CONTEXT_TYPE, not: " + failures[0]);

            // Duplicate work inflow context
            collectionContext = CollectionContext.getConstructor(String[].class).newInstance((Object) new String[] { "item4" });
            work.setWorkContexts(collectionContext, collectionContext);
            workListener.clear();
            try {
                workManager.doWork(work, TIMEOUT, null, workListener);
                throw new Exception("Should not be able to doWork with duplicate inflow context");
            } catch (WorkCompletedException x) {
                if (!WorkContextErrorCodes.DUPLICATE_CONTEXTS.equals(x.getErrorCode()))
                    throw new Exception("Incorrect error code is " + x.getErrorCode(), x);
                if (x.getMessage().indexOf("J2CA8624") < 0)
                    throw new Exception(x);
            }

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_ACCEPTED)
                throw new Exception("Should get WORK_ACCEPTED, not: " + event.getType());

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_STARTED)
                throw new Exception("Should get WORK_STARTED, not: " + event.getType());

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_COMPLETED)
                throw new Exception("Should get WORK_COMPLETED, not: " + event.getType());
            errorCode = event.getException().getErrorCode();
            if (!WorkContextErrorCodes.DUPLICATE_CONTEXTS.equals(errorCode))
                throw new Exception("Should get DUPLICATE_CONTEXTS, not: " + errorCode, event.getException());
            if (event.getException().getMessage() == null
                || event.getException().getMessage().indexOf("J2CA8624") < 0)
                throw new Exception(event.getException());

            failures = (String[]) CollectionContext.getMethod("getContextSetupFailures").invoke(collectionContext);
            if (failures.length != 1)
                throw new Exception("Should get exactly one context failure, not " + Arrays.asList(failures));
            if (!WorkContextErrorCodes.DUPLICATE_CONTEXTS.equals(failures[0]))
                throw new Exception("Should get DUPLICATE_CONTEXTS, not: " + failures[0]);

            // Fail context setup on purpose
            collectionContext = CollectionContext.getConstructor(String[].class).newInstance((Object) new String[] { "item5" });
            HintsContext hintsContext = new HintsContext();
            hintsContext.setHint(HintsContext.NAME_HINT, "WorkThatFailsContextSetup"); // Signal the collection context to fail setup
            work.setWorkContexts(collectionContext, hintsContext);
            workListener.clear();
            workManager.scheduleWork(work, TIMEOUT, null, workListener);

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_ACCEPTED)
                throw new Exception("Expecting WORK_ACCEPTED, not: " + event.getType());

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_STARTED)
                throw new Exception("Expecting WORK_STARTED, not: " + event.getType());

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_COMPLETED)
                throw new Exception("Expecting WORK_COMPLETED, not: " + event.getType());
            errorCode = event.getException().getErrorCode();
            if (!WorkContextErrorCodes.CONTEXT_SETUP_FAILED.equals(errorCode))
                throw new Exception("Expecting CONTEXT_SETUP_FAILED, not: " + errorCode, event.getException());
            if (event.getException().getMessage().indexOf("Intentionally caused failure") < 0)
                throw new Exception(event.getException());

            failures = (String[]) CollectionContext.getMethod("getContextSetupFailures").invoke(collectionContext);
            if (failures.length != 1)
                throw new Exception("Expecting exactly one context failure, not " + Arrays.asList(failures));
            if (!WorkContextErrorCodes.CONTEXT_SETUP_FAILED.equals(failures[0]))
                throw new Exception("Expecting CONTEXT_SETUP_FAILED, not: " + failures[0]);

            // Attempt to use ExecutionContext and WorkContextProvider together - this is not allowed
            try {
                workListener.clear();
                startDuration = workManager.startWork(work, TIMEOUT, new ExecutionContext(), workListener);
                throw new Exception("Should not be able to start work when ExecutionContext is specified and the work is a WorkContextProvider.");
            } catch (WorkRejectedException x) {
            } // expected

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_REJECTED)
                throw new Exception("Event should be WORK_REJECTED, not: " + event.getType());
            if (!(event.getException() instanceof WorkRejectedException))
                throw new Exception("Unexpected exception from work rejected event.getExeception. See cause for unexpected exception", event.getException());
            if (event.getException().getMessage() == null || event.getException().getMessage().indexOf("J2CA8623") < 0)
                throw new Exception("Unexpected message from work rejected event.getException. See cause for exception", event.getException());

            // use ExecutionContext
            final BVTWork<String> moreWork = new BVTWork<String>() {
                @Override
                public String call() throws Exception {
                    return "running " + this;
                }
            };
            workListener.clear();
            workManager.scheduleWork(moreWork, TIMEOUT, new ExecutionContext(), workListener);

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_ACCEPTED)
                throw new Exception("Event should be WORK_ACCEPTED, not: " + event.getType());

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_STARTED)
                throw new Exception("Event should be WORK_STARTED, not: " + event.getType());

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_COMPLETED)
                throw new Exception("Event should be WORK_COMPLETED, not: " + event.getType());
            if (event.getException() != null)
                throw event.getException();

            // Attempt to use ExecutionContext and WorkContextProvider (which returns null for getWorkContexts) together - this is not allowed
            BVTWorkWithContext<Integer> workWithNullContext = new BVTWorkWithContext<Integer>() {
                private static final long serialVersionUID = 3087908540555353013L;

                @Override
                public Integer call() throws Exception {
                    return 1;
                }
            };
            workListener.clear();
            try {
                workManager.startWork(workWithNullContext, TIMEOUT, new ExecutionContext(), workListener);
                throw new Exception("startWork should have been rejected due to presence of both ExecutionContext and WorkContextProvider, even if getWorkContexts returns null");
            } catch (WorkRejectedException x) {
                if (x.getMessage() == null || !x.getMessage().startsWith("J2CA8623"))
                    throw new Exception("WorkRejectedException has unexpected or missing message", x);
            }

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_REJECTED)
                throw new Exception("Event should be WORK_REJECTED, not: " + event.getType());
            if (!(event.getException() instanceof WorkRejectedException) || event.getException().getMessage() == null || !event.getException().getMessage().startsWith("J2CA8623"))
                throw new Exception("Event has unexpected or missing exception", event.getException());

            // Fail HintsContext due to invalid NAME_HINT data type
            hintsContext = new HintsContext();
            hintsContext.setHint(HintsContext.NAME_HINT, 1020304l);
            work.setWorkContexts(hintsContext);
            workListener.clear();
            try {
                workManager.doWork(work);
                throw new Exception("Expecting work with invalid NAME_HINT type to fail.");
            } catch (WorkCompletedException x) {
                if (!WorkContextErrorCodes.CONTEXT_SETUP_FAILED.equals(x.getErrorCode())
                    || !(x.getCause() instanceof ClassCastException)
                    || x.getCause().getMessage() == null
                    || x.getCause().getMessage().indexOf("HintsContext.NAME_HINT") < 0)
                    throw x;
            }

            // Fail HintsContext due to invalid LONGRUNNING_HINT data type
            hintsContext = new HintsContext();
            hintsContext.setHint(HintsContext.LONGRUNNING_HINT, "true");
            work.setWorkContexts(hintsContext);
            workListener.clear();
            try {
                workManager.startWork(work, TIMEOUT, null, workListener);
            } catch (WorkCompletedException x) {
                // this can happen if the work fails quickly
            }

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_ACCEPTED)
                throw new Exception("HintsContext with invalid hint type: Expecting WORK_ACCEPTED, not: " + event.getType());

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_STARTED)
                throw new Exception("HintsContext with invalid hint type: Expecting WORK_STARTED, not: " + event.getType());

            event = workListener.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event.getType() != WorkEvent.WORK_COMPLETED)
                throw new Exception("HintsContext with invalid hint type: Expecting WORK_COMPLETED, not: " + event.getType());
            errorCode = event.getException().getErrorCode();
            if (!WorkContextErrorCodes.CONTEXT_SETUP_FAILED.equals(errorCode))
                throw new Exception("HintsContext with invalid hint type: Expecting CONTEXT_SETUP_FAILED, not: " + errorCode, event.getException());
            if (!(event.getException().getCause() instanceof ClassCastException)
                || event.getException().getCause().getMessage() == null
                || event.getException().getCause().getMessage().indexOf("HintsContext.LONGRUNNING_HINT") < 0)
                throw new Exception("HintsContext with invalid hint type: Unexpected cause", event.getException());
        } finally {
            Method ungetService = bundleContext.getClass().getMethod("ungetService", srref);
            ungetService.invoke(bundleContext, collectionSvcRef);
        }
    }
}
