/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.persistent.fat.simctrl;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * We are simulating a controller within a single server (where there can be multiple persistent executors
 * all pointing at the same database) because it's convenient to let declarative services do all of the
 * coordination for us. That's all implementation detail for the test. The important piece here is
 * exercising the operations in persistent executor that are usable via the PersistentExecutorMBean.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class SimulatedController {
    // How often (in milliseconds) to look for persistent executors to which tasks can be reassigned.
    private static final long
        EXEC_DISABLED_INSTANCES_POLL_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10),
        RETRY_INTERVAL = TimeUnit.SECONDS.toMillis(1);

    // Map of execution-enabled executors to their partition id
    private final ConcurrentHashMap<PersistentExecutor, Long> executors = new ConcurrentHashMap<PersistentExecutor, Long>();

    // Map of execution-disabled executors to their partition id
    private final ConcurrentHashMap<PersistentExecutor, Long> executorsWithExecDisabled = new ConcurrentHashMap<PersistentExecutor, Long>();

    // Futures that allow us to cancel the finder tasks during server shutdown
    private final ConcurrentLinkedQueue<ScheduledFuture<?>> finderTaskFutures = new ConcurrentLinkedQueue<ScheduledFuture<?>>();

    // Current index in the executors list for basic round robin approach to transferring tasks
    private final AtomicInteger rrIndex = new AtomicInteger();

    // Default managed scheduled executor
    private volatile ScheduledExecutorService scheduledExecutor;

    @Activate
    protected void activate(ComponentContext context) {
        System.out.println("Controller activated");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        // Reduce the possibility that FinderTasks might attempt to run during server shutdown.
        scheduledExecutor = null;
        for (ScheduledFuture<?> future = finderTaskFutures.poll(); future != null; future = finderTaskFutures.poll())
            if (future != null)
                future.cancel(true);
        System.out.println("Controller deactivated");
    }

    // Returns an entry for an active partition, if any.
    // This uses an inefficient way to cycle through active instances, but this is only test code.
    private Entry<PersistentExecutor, Long> getActivePartitionEntry() {
        Entry<PersistentExecutor, Long> entry = null;
        Iterator<Entry<PersistentExecutor, Long>> entries = executors.entrySet().iterator();
        if (entries.hasNext()) {
            int index = rrIndex.getAndIncrement();
            entry = entries.next();
            for (int i = 0; i < index && entries.hasNext(); i++)
                entry = entries.next();
            if (!entries.hasNext())
                rrIndex.set(0);
        }
        return entry;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setPersistentExecutor(PersistentExecutor executor, Map<String, Object> props) throws Exception {
        String name = (String) props.get("config.displayId");
        String id = (String) props.get("id");

        Method getPartitionId = executor.getClass().getDeclaredMethod("getPartitionId");
        getPartitionId.setAccessible(true);
        getPartitionId.invoke(executor);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=PersistentExecutorMBean,*,name=" + name + "*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        ObjectInstance bean = s.iterator().next();
        Object obj = mbs.invoke(bean.getObjectName(), "findPartitionInfo", new Object[] { null, null, null, id },
                                new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
        String[][] records = (String[][]) obj;
        long partition = -1;
        for (String[] record : records) {
            if (record[4].equals(id))
                partition = Long.valueOf(record[0]);
        }

        if ((Boolean) props.get("enableTaskExecution")) {
            System.out.println("Controller notified of instance than can run tasks [" + partition + "]: " + executor);
            executors.put(executor, partition);
            System.out.println("Execution-enabled instances are: " + executors);
        } else {
            System.out.println("Controller notified of instance that cannot run tasks [" + partition + "]: " + executor);
            executorsWithExecDisabled.put(executor, partition);
            System.out.println("Execution-disabled instances are: " + executorsWithExecDisabled);

            if (scheduledExecutor != null) {
                FinderTask finder = new FinderTask(partition, EXEC_DISABLED_INSTANCES_POLL_INTERVAL_MS);
                finderTaskFutures.add(scheduledExecutor.schedule(finder, EXEC_DISABLED_INSTANCES_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS));
            }
        }
    }

    @Reference(target = "(id=DefaultManagedScheduledExecutorService)")
    protected void setScheduledExecutor(ScheduledExecutorService executor) {
        scheduledExecutor = executor;

        for (Long partition : executorsWithExecDisabled.values()) {
            FinderTask finder = new FinderTask(partition, EXEC_DISABLED_INSTANCES_POLL_INTERVAL_MS);
            finderTaskFutures.add(scheduledExecutor.schedule(finder, EXEC_DISABLED_INSTANCES_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS));
        }
    }

    protected void unsetPersistentExecutor(PersistentExecutor executor) {
        Long partition = executors.remove(executor);
        if (partition != null) {
            System.out.println("Controller notified of instance going down [" + partition + "]: " + executor);
            System.out.println("Active instances that can run tasks are: " + executors);

            ScheduledExecutorService schedExecutor = scheduledExecutor;
            if (schedExecutor == null || FrameworkState.isStopping())
                System.out.println("In shutdown, not scheduling FinderTask");
            else {
                FinderTask finder = new FinderTask(partition, -1);
                ScheduledFuture<Void> future = schedExecutor.schedule(finder, 1, TimeUnit.SECONDS);
                finder.future.set(future);
                finderTaskFutures.add(future);
            }
        } else {
            partition = executorsWithExecDisabled.remove(executor);
            System.out.println("Controller notified of instance going down [" + partition + "]: " + executor);
            System.out.println("Active instances that cannot run tasks are: " + executors);
        }
    }

    protected void unsetScheduledExecutor(ScheduledExecutorService executor) {}

    /**
     * Query for tasks that should be transferred to a different executor instance
     */
    private class FinderTask implements Callable<Void> {
        private final AtomicReference<ScheduledFuture<Void>> future = new AtomicReference<ScheduledFuture<Void>>();
        private final long interval; // -1 to run once only
        private final long partition;

        private FinderTask(long partition, long interval) {
            this.partition = partition;
            this.interval = interval;
        }

        @Override
        public Void call() throws Exception {
            System.out.println("Controller is looking for non-ended tasks in partition " + partition);
            boolean successful = false;
            try {
                Iterator<Entry<PersistentExecutor, Long>> entries = executors.entrySet().iterator();
                if (entries.hasNext()) {
                    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                    ObjectName obn = new ObjectName("WebSphere:type=PersistentExecutorMBean,*");
                    Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
                    ObjectInstance bean = s.iterator().next();

                    Long minId = null;
                    int maxResults = 3;
                    List<Long> taskIds;
                    boolean transferred;
                    do {
                        Object obj2 = mbs.invoke(bean.getObjectName(), "findTaskIds", new Object[] { partition, "ENDED", false, minId, maxResults },
                                                 new String[] { "long", "java.lang.String", "boolean", "java.lang.Long", "java.lang.Integer" });
                        Long[] tasks = (Long[]) obj2;
                        taskIds = new ArrayList<Long>(Arrays.asList(tasks));
                        transferred = true; // assume we will successfully transfer any tasks we find
                        System.out.println("Controller found tasks " + taskIds);
                        int numTasks = taskIds.size();
                        if (numTasks > 0) {
                            Long maxTaskId = numTasks == maxResults ? taskIds.get(maxResults - 1) : null;

                            Entry<PersistentExecutor, Long> activeEntry = getActivePartitionEntry();
                            if (activeEntry != null) {
                                long newPartitionId = activeEntry.getValue();

                                Object obj = mbs.invoke(bean.getObjectName(), "findPartitionInfo", new Object[] { null, null, null, null },
                                                        new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
                                String[][] records = (String[][]) obj;
                                String execId = null;
                                for (String[] record : records) {
                                    if (Long.valueOf(record[0]).equals(newPartitionId))
                                        execId = record[4];
                                }
                                ObjectName newObn = new ObjectName("WebSphere:type=PersistentExecutorMBean,*,id=" + execId + "*");
                                s = mbs.queryMBeans(newObn, null);
                                ObjectInstance newBean = s.iterator().next();
                                int transfers = (Integer) mbs.invoke(newBean.getObjectName(), "transfer", new Object[] { maxTaskId, partition },
                                                                     new String[] { "java.lang.Long", "long" });
                                if (transfers > 0)
                                    transferred = true;

                                System.out.println("Controller transferred tasks"
                                                   + (maxTaskId == null ? "" : (" <= " + maxTaskId))
                                                   + " to partition " + newPartitionId + "? " + transferred);
                            }
                        }
                    } while (taskIds.size() == maxResults || !transferred);

                    successful = true;
                    System.out.println("Controller is done looking for tasks");
                }
            } finally {
                if (interval > 0 || !successful) {
                    if (!successful)
                        System.out.println("Controller was unable to transfer tasks at this time.");

                    ScheduledExecutorService schedExecutor = scheduledExecutor;
                    if (schedExecutor == null || FrameworkState.isStopping())
                        System.out.println("In shutdown, not rescheduling FinderTask");
                    else {
                        ScheduledFuture<Void> previous = future.get();
                        ScheduledFuture<Void> f = schedExecutor.schedule(this, successful ? interval : RETRY_INTERVAL, TimeUnit.MILLISECONDS);
                        future.set(f);
                        finderTaskFutures.add(f);
                        finderTaskFutures.remove(previous);
                    }
                }
            }
            return null;
        }
    }
}
