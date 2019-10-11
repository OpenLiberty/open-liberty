/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.persistra.resourceadapter;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

/**
 * Test resource adapter that schedules persistent tasks upon start.
 */
@Connector
public class PSETestResourceAdapter implements ResourceAdapter {
    /**
     * Interval for polling task status (in milliseconds).
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(10);

    private static PersistentExecutor scheduler;
    private static Future<TaskStatus<?>> taskStatusFuture;

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) {}

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec) {}

    public static Future<TaskStatus<?>> getTaskStatus() {
        return taskStatusFuture;
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) {
        return null;
    }

    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException {
        System.out.println("PSETestResourceAdapter is starting");
        try {
            // There is no good way to access a persistent scheduled executor from a resource adapter thread.
            // For now, we will cheat and find it in the service registry.
            Class<?> raClass = bootstrapContext.getClass();
            Class<?> FrameworkUtil = raClass.getClassLoader().loadClass("org.osgi.framework.FrameworkUtil");
            Class<?> ServiceReference = raClass.getClassLoader().loadClass("org.osgi.framework.ServiceReference");
            Object bundle = FrameworkUtil.getMethod("getBundle", Class.class).invoke(null, raClass);
            Object bundleContext = bundle.getClass().getMethod("getBundleContext").invoke(bundle);
            Method getServiceReferences = bundleContext.getClass().getMethod("getServiceReferences", Class.class, String.class);
            Method getService = bundleContext.getClass().getMethod("getService", ServiceReference);

            Collection<?> refs = (Collection<?>) getServiceReferences.invoke(bundleContext, PersistentExecutor.class, "(jndiName=concurrent/myScheduler)");
            Object ref = refs.iterator().next();
            scheduler = (PersistentExecutor) getService.invoke(bundleContext, ref);

            refs = (Collection<?>) getServiceReferences.invoke(bundleContext, ExecutorService.class, "(id=DefaultManagedExecutorService)");
            ref = refs.iterator().next();
            ExecutorService executor = (ExecutorService) getService.invoke(bundleContext, ref);

            taskStatusFuture = executor.submit(new Callable<TaskStatus<?>>() {
                @Override
                public TaskStatus<?> call() throws Exception {
                    TaskStatus<?> taskStatus = scheduler.schedule(new RASerializableTask(), new RATrigger());

                    for (long start = System.nanoTime(); !taskStatus.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                        taskStatus = scheduler.getStatus(taskStatus.getTaskId());

                    return taskStatus;
                }
            });
        } catch (Exception x) {
            throw new ResourceAdapterInternalException(x);
        }
        System.out.println("PSETestResourceAdapter has started");
    }

    @Override
    public void stop() {
        System.out.println("PSETestResourceAdapter is stopping");
        if (taskStatusFuture != null)
            try {
                TaskStatus<?> taskStatus = taskStatusFuture.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
                taskStatus.cancel(true);
                if (!scheduler.remove(taskStatus.getTaskId()))
                    throw new RuntimeException("Failed to remove task " + taskStatus);
                taskStatusFuture = null;
            } catch (RuntimeException x) {
                x.printStackTrace(System.out);
                throw x;
            } catch (Exception x) {
                x.printStackTrace(System.out);
                throw new RuntimeException(x);
            }
        System.out.println("PSETestResourceAdapter has stopped");
    }
}
