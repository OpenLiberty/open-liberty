/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.osgi.pmi.internal;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ejbcontainer.EJBType;

import io.openliberty.ejbcontainer.jakarta.test.osgi.pmi.CountStatistic;
import io.openliberty.ejbcontainer.jakarta.test.osgi.pmi.EJBStats;
import io.openliberty.ejbcontainer.jakarta.test.osgi.pmi.RangeStatistic;
import io.openliberty.ejbcontainer.jakarta.test.osgi.pmi.TimeStatistic;

// SERV1/ws/code/runtime/src/com/ibm/ws/pmi/server/modules/BeanModule.java
public class TestEJBPMICollaborator implements EJBPMICollaborator, EJBStats {
    private static final TraceComponent tc = Tr.register(TestEJBPMICollaborator.class);

    final transient EJBComponentMetaData cmd;
    private final boolean stateless;
    private final boolean messageDriven;

    private final Map<String, Object> statistics = new LinkedHashMap<String, Object>();
    // SERV1/ws/code/pmi/src/com/ibm/websphere/pmi/xml/beanModule.xml
    // SERV1/ws/code/pmi/src/com/ibm/ws/pmi/properties/PMIText.nlsprops
    private final transient CountStatistic creates = newCountStatistic("CreateCount"); // 1 - beanModule.creates
    private final transient CountStatistic removes = newCountStatistic("RemoveCount"); // 2 - beanModule.removes
    private final transient CountStatistic activations; // 3 - beanModule.activates - entity || stateful
    private final transient CountStatistic passivations; // 4 - beanModule.passivates - entity
    private final transient CountStatistic instantiates; // 5 - beanModule.instantiates - entity || stateful || messageDriven
    private final transient CountStatistic destroys; // 6 - beanModule.destroys - entity || stateful || messageDriven
    private final transient CountStatistic loads; // 7 - beanModule.loads - entity
    private final transient CountStatistic stores; // 8 - beanModule.stores - entity
    private final transient RangeStatistic actives; // 9 - beanModule.readyCount - entity (or stateful methodReadyCount)
    private final transient RangeStatistic lives = newRangeStatistic("LiveCount"); // 10 - beanModule.concurrentLives
    private final transient CountStatistic methodCalls = newCountStatistic("MethodCallCount"); // 11 - beanModule.totalMethodCalls
    private final transient TimeStatistic methodRt = newTimeStatistic("MethodResponseTime"); // 12 - beanModule.avgMethodRt
    private final transient TimeStatistic createRt = newTimeStatistic("CreateTime"); // 14 - beanModule.avgCreateTime
    private final transient TimeStatistic removeRt = newTimeStatistic("RemoveTime"); // 15 - beanModule.avgRemoveTime
    private final transient RangeStatistic methodLoad = newRangeStatistic("ActiveMethodCount"); // 18 - beanModule.activeMethods
    private final transient CountStatistic getsFromPool; // 19 - beanModule.getsFromPool - entity || stateless || messageDriven
    private final transient CountStatistic getsFound; // 20 - beanModule.getsFound - entity || stateless || messageDriven
    private final transient CountStatistic returnsToPool; // 21 - beanModule.returnsToPool - entity || stateless || messageDriven
    private final transient CountStatistic returnsDiscarded; // 22 - beanModule.returnsDiscarded - entity || stateless || messageDriven
    private final transient CountStatistic drainsFromPool; // 23 - beanModule.drainsFromPool - entity || stateless || messageDriven
    private final transient AverageStatistic avgDrainSize; // 24 - beanModule.avgDrainSize - entity || stateless || messageDriven
    private final transient RangeStatistic poolSize; // 25 - beanModule.poolSize - entity || messageDriven (or stateless methodReadyCount)
    private final transient CountStatistic messageCount; // 26 - beanModule.messageCount - messageDriven
    private final transient CountStatistic messageBackoutCount; // 27 - beanModule.messageBackoutCount - messageDriven
    private final transient TimeStatistic averageServerSessionWaitTime; // 28 - beanModule.avgSrvSessionWaitTime - messageDriven
    private final transient RangeStatistic serverSessionUsage; // 29 - beanModule.serverSessionUsage - messageDriven
    private final transient TimeStatistic activationRt; // 30 - beanModule.activationTime - entity || stateful
    private final transient TimeStatistic passivationRt; // 31 - beanModule.passivationTime - entity || stateful
    private final transient TimeStatistic loadRt; // 32 - beanModule.loadTime - entity
    private final transient TimeStatistic storeRt; // 33 - beanModule.storeTime - entity
    private final transient RangeStatistic passivatedSFSB; // 34 - beanModule.passivationCount - stateful
    // 35 - beanModule.methodReadyCount - stateful (actives) || stateless (poolSize)
    private final transient TimeStatistic readLockTime; // 36 - beanModule.readLockTime - singleton
    private final transient TimeStatistic writeLockTime; // 37 - beanModule.writeLockTime - singleton
    private final transient CountStatistic lockCancelCount; // 38 - beanModule.lockCancelCount - singleton
    private final transient TimeStatistic asyncWaitTime; // 39 - beanModule.asyncWaitTime - stateless || stateful || singleton
    private final transient RangeStatistic asyncQSize; // 40 - beanModule.asyncQSize - stateless || stateful || singleton
    private final transient CountStatistic asyncCancelCount; // 41 - beanModule.asyncCancelCount - stateless || stateful || singleton
    private final transient CountStatistic asyncFNFFailCount; // 42 - beanModule.asyncFNFFailCount - stateless || stateful || singleton
    // ??? Should be a range?
    private final transient CountStatistic asyncFutureObjectCount; // 43 - beanModule.asyncFutureObjectCount - stateless || stateful || singleton
    private final transient CountStatistic discards; // 44 - beanModule.discards - entity || stateless || stateful || messageDriven

    // 50 (SUBMODULE) - beanModule.methods
    static class MethodStats {
        final CountStatistic perMethodCalls = new CountStatistic(); // 51 - beanModule.methods.methodCalls
        final TimeStatistic perMethodRt = new TimeStatistic(); // 52 - beanModule.methods.methodRt - all
        final RangeStatistic perMethodLoad = new RangeStatistic(); // 53 - beanModule.methods.methodLoad - all

        public Object getStat(String name) {
            if ("MethodLevelCallCount".equals(name)) {
                return perMethodCalls;
            }
            if ("MethodLevelResponseTime".equals(name)) {
                return perMethodRt;
            }
            if ("MethodLevelConcurrentInvocations".equals(name)) {
                return perMethodLoad;
            }
            return null;
        }
    }

    private void addStatistic(String name, Object stat) {
        Object oldStat = statistics.put(name, stat);
        Assert.assertNull(oldStat);
    }

    private RangeStatistic newRangeStatistic(String name) {
        RangeStatistic stat = new RangeStatistic();
        addStatistic(name, stat);
        return stat;
    }

    private CountStatistic newCountStatistic(String name) {
        CountStatistic stat = new CountStatistic();
        addStatistic(name, stat);
        return stat;
    }

    private AverageStatistic newAverageStatistic(String name) {
        // Per SERV1/ws/code/pmi.j2ee/src/com/ibm/ws/pmi/j2ee/PMIJ2EEStatsHelper.java:
        //   J2EE doesn't have AverageStatistic
        //   Since TimeStatistic is derived from AverageStatistic
        //   wrap AverageStatistic with TimeStatistic
        AverageStatistic stat = new TimeStatistic();
        addStatistic(name, stat);
        return stat;
    }

    private TimeStatistic newTimeStatistic(String name) {
        TimeStatistic stat = new TimeStatistic();
        addStatistic(name, stat);
        return stat;
    }

    public TestEJBPMICollaborator(EJBComponentMetaData cmd) {
        this.cmd = cmd;

        EJBType type = cmd.getEJBType();
        boolean entity = type.isEntity();
        stateless = type == EJBType.STATELESS_SESSION;
        boolean stateful = type == EJBType.STATEFUL_SESSION;
        boolean singleton = type == EJBType.SINGLETON_SESSION;
        messageDriven = type == EJBType.MESSAGE_DRIVEN;

        activations = entity || stateful ? newCountStatistic("ActivateCount") : null;
        passivations = entity ? newCountStatistic("PassivateCount") : null;
        instantiates = entity || stateful || messageDriven ? newCountStatistic("InstantiateCount") : null;
        destroys = entity || stateful || messageDriven ? newCountStatistic("FreedCount") : null;
        loads = entity ? newCountStatistic("LoadCount") : null;
        stores = entity ? newCountStatistic("StoreCount") : null;
        actives = entity ? newRangeStatistic("ReadyCount") : stateful ? newRangeStatistic("MethodReadyCount") : null;
        getsFromPool = entity || stateless || messageDriven ? newCountStatistic("RetrieveFromPoolCount") : null;
        getsFound = entity || stateless || messageDriven ? newCountStatistic("RetrieveFromPoolSuccessCount") : null;
        returnsToPool = entity || stateless || messageDriven ? newCountStatistic("ReturnsToPoolCount") : null;
        returnsDiscarded = entity || stateless || messageDriven ? newCountStatistic("ReturnsDiscardCount") : null;
        drainsFromPool = entity || stateless || messageDriven ? newCountStatistic("DrainsFromPoolCount") : null;
        avgDrainSize = entity || stateless || messageDriven ? newAverageStatistic("DrainSize") : null;
        poolSize = entity || messageDriven ? newRangeStatistic("PooledCount") : stateless ? newRangeStatistic("MethodReadyCount") : null;
        messageCount = messageDriven ? newCountStatistic("MessageCount") : null;
        messageBackoutCount = messageDriven ? newCountStatistic("MessageBackoutCount") : null;
        averageServerSessionWaitTime = messageDriven ? newTimeStatistic("WaitTime") : null;
        serverSessionUsage = messageDriven ? newRangeStatistic("ServerSessionPoolUsage") : null;
        activationRt = entity || stateful ? newTimeStatistic("ActivationTime") : null;
        passivationRt = entity || stateful ? newTimeStatistic("PassivationTime") : null;
        loadRt = entity ? newTimeStatistic("LoadTime") : null;
        storeRt = entity ? newTimeStatistic("StoreTime") : null;
        passivatedSFSB = stateful ? newRangeStatistic("PassiveCount") : null;
        readLockTime = singleton ? newTimeStatistic("ReadLockTime") : null;
        writeLockTime = singleton ? newTimeStatistic("WriteLockTime") : null;
        lockCancelCount = singleton ? newCountStatistic("LockCancelCount") : null;
        asyncWaitTime = stateless || stateful || singleton ? newTimeStatistic("AsyncWaitTime") : null;
        asyncQSize = stateless || stateful || singleton ? newRangeStatistic("AsyncQSize") : null;
        asyncCancelCount = stateless || stateful || singleton ? newCountStatistic("AsyncCancelCount") : null;
        asyncFNFFailCount = stateless || stateful || singleton ? newCountStatistic("AsyncFNFFailCount") : null;
        asyncFutureObjectCount = stateless || stateful || singleton ? newCountStatistic("AsyncFutureObjectCount") : null;
        discards = entity || stateless || stateful || messageDriven ? newCountStatistic("Discards") : null;
    }

    @Override
    public String[] getStatisticNames() {
        Set<String> keys = statistics.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    @Override
    public Object getStatistic(String name) {
        return statistics.get(name);
    }

    @Trivial
    private void increment(AbstractIncrementableStatistic stat) {
        if (stat != null) {
            stat.increment();
        }
    }

    @Trivial
    private void decrement(AbstractIncrementableStatistic stat) {
        if (stat != null) {
            stat.decrement();
        }
    }

    @Trivial
    private void set(RangeStatistic stat, int newValue) {
        if (stat != null) {
            stat.set(newValue);
        }
    }

    @Trivial
    private void add(AverageStatistic stat, long value) {
        if (stat != null) {
            stat.add(value);
        }
    }

    @Trivial
    private void addDuration(AverageStatistic stat, long duration) {
        if (stat != null) {
            stat.add(TimeUnit.NANOSECONDS.toMillis(duration));
        }
    }

    @Trivial
    private long timeBegin() {
        return System.nanoTime();
    }

    private long timeDuration(long startTime) {
        return System.nanoTime() - startTime;
    }

    private MethodStats getMethodStats(EJBMethodMetaData mmd) {
        // TODO
        return null;
    }

    @Override
    public void beanInstantiated() {
        increment(lives);
        if (stateless || messageDriven) {
            increment(creates);
        }
        increment(instantiates);
    }

    @Override
    public void beanDestroyed() {
        decrement(lives);
        increment(destroys);
    }

    @Override
    public void beanCreated() {
        increment(actives);
        increment(creates);
    }

    @Override
    public void beanRemoved() {
        decrement(actives);
        increment(removes);
    }

    @Override
    public void beanDiscarded() {
        decrement(actives);
    }

    @Override
    public long methodPreInvoke(Object key, EJBMethodMetaData mi) {
        increment(methodLoad);
        increment(methodCalls);

        MethodStats methodStats = getMethodStats(mi);
        if (methodStats != null) {
            increment(methodStats.perMethodCalls);
            increment(methodStats.perMethodLoad);
        }

        return timeBegin();
    }

    @Override
    public void methodPostInvoke(Object key, EJBMethodMetaData mi, long startTime) {
        long duration = timeDuration(startTime);

        addDuration(methodRt, duration);
        decrement(methodLoad);

        MethodStats methodStats = getMethodStats(mi);
        if (methodStats != null) {
            decrement(methodStats.perMethodLoad);
            addDuration(methodStats.perMethodRt, duration);
        }
    }

    @Override
    public void objectRetrieve(int size, boolean objectInPool) {
        increment(getsFromPool);
        if (objectInPool) {
            increment(getsFound);
            set(poolSize, size);
        }
    }

    @Override
    public void objectReturn(int size, boolean objectDiscarded) {
        if (objectDiscarded) {
            increment(returnsDiscarded);
        } else {
            set(poolSize, size);
        }
        increment(returnsToPool);
    }

    @Override
    public void poolCreated(int size) {
        set(poolSize, size);
    }

    @Override
    public void poolDrained(int size, int numObjectsDiscarded) {
        if (numObjectsDiscarded > 0) {
            set(poolSize, size);
        }
        increment(drainsFromPool);
        add(avgDrainSize, numObjectsDiscarded);
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long activationTime() {
        return timeBegin();
    }

    @Override
    public void activationTime(long startTime) {
        long duration = timeDuration(startTime);

        increment(actives);
        increment(activations);
        decrement(passivatedSFSB);
        addDuration(activationRt, duration);
    }

    @Override
    public long passivationTime() {
        return timeBegin();
    }

    @Override
    public void passivationTime(long startTime) {
        long duration = timeDuration(startTime);

        decrement(actives);
        increment(passivations);
        increment(passivatedSFSB);
        addDuration(passivationRt, duration);
    }

    @Override
    public long loadTime() {
        return timeBegin();
    }

    @Override
    public void loadTime(long startTime) {
        long duration = timeDuration(startTime);

        increment(loads);
        addDuration(loadRt, duration);
    }

    @Override
    public long storeTime() {
        return timeBegin();
    }

    @Override
    public void storeTime(long startTime) {
        long duration = timeDuration(startTime);

        increment(stores);
        addDuration(storeRt, duration);
    }

    @Override
    public void messageDelivered() {
        increment(messageCount);
    }

    @Override
    public void messageBackedOut() {
        increment(messageBackoutCount);
    }

    @Override
    public long waitingForServerSession() {
        return timeBegin();
    }

    @Override
    public void gotServerSession(long startTime) {
        long duration = timeDuration(startTime);

        addDuration(averageServerSessionWaitTime, duration);
    }

    private void updateServerSessionUsage(int newInUseCount, int poolSize) {
        set(serverSessionUsage, newInUseCount * 100 / poolSize);
    }

    @Override
    public void serverSessionRetrieve(int newInUseCount, int poolSize) {
        updateServerSessionUsage(newInUseCount, poolSize);
    }

    @Override
    public void serverSessionReturn(int newInUseCount, int poolSize) {
        updateServerSessionUsage(newInUseCount, poolSize);
    }

    @Trivial
    private static String getTimerCounterName(int strCounterId) {
        switch (strCounterId) {
            case CREATE_RT:
                return "CREATE_RT";
            case REMOVE_RT:
                return "REMOVE_RT";
            case READ_LOCK_TIME:
                return "READ_LOCK_TIME";
            case WRITE_LOCK_TIME:
                return "WRITE_LOCK_TIME";
            case ASYNC_WAIT_TIME:
                return "ASYNC_WAIT_TIME";
            default:
                return Integer.toString(strCounterId);
        }
    }

    @Override
    public long initialTime(int strCounterId) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "initialTime", getTimerCounterName(strCounterId));

        long begin = timeBegin();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "initialTime", begin);
        return begin;
    }

    @Override
    public long finalTime(int strCounterId, long startTime) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "finalTime", getTimerCounterName(strCounterId), startTime);

        long duration = timeDuration(startTime);

        switch (strCounterId) {
            case CREATE_RT:
                addDuration(createRt, duration);
                break;
            case REMOVE_RT:
                addDuration(removeRt, duration);
                break;
            case READ_LOCK_TIME:
                addDuration(readLockTime, duration);
                break;
            case WRITE_LOCK_TIME:
                addDuration(writeLockTime, duration);
                break;
            case ASYNC_WAIT_TIME:
                addDuration(asyncWaitTime, duration);
                break;
            default:
                throw new IllegalArgumentException(Integer.toString(strCounterId));
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "finalTime", duration);
        return duration;
    }

    @Override
    public void countCancelledLocks() {
        increment(lockCancelCount);
    }

    @Override
    public void asyncQueSizeIncrement() {
        increment(asyncQSize);
    }

    @Override
    public void asyncQueSizeDecrement() {
        decrement(asyncQSize);
    }

    @Override
    public void asyncMethodCallCanceled() {
        increment(asyncCancelCount);
    }

    @Override
    public void asyncFNFFailed() {
        increment(asyncFNFFailCount);
    }

    @Override
    public void discardCount() {
        increment(discards);
    }

    @Override
    public void asyncFutureObjectIncrement() {
        increment(asyncFutureObjectCount);
    }

    @Override
    public void asyncFutureObjectDecrement() {
        decrement(asyncFutureObjectCount);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException();
    }
}
