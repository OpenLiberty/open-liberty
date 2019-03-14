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
package com.ibm.ws.concurrent.persistent.db;

import java.security.AccessController;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.persistent.internal.Utils;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.concurrent.persistent.PartitionRecord;
import com.ibm.wsspi.concurrent.persistent.TaskRecord;
import com.ibm.wsspi.concurrent.persistent.TaskStore;
import com.ibm.wsspi.persistence.DatabaseStore;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

/**
 * Task store for persistent tasks stored in a database.
 */
public class DatabaseTaskStore implements TaskStore {
    private static final TraceComponent tc = Tr.register(DatabaseTaskStore.class);
    private final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private static final Map<DatabaseStore, DatabaseTaskStore> dbTaskStores = new HashMap<DatabaseStore, DatabaseTaskStore>();
    private static final Map<DatabaseStore, Integer> refCounts = new HashMap<DatabaseStore, Integer>();

    private final DatabaseStore dbStore;

    /**
     * Indicates this instance has been destroyed. Must use the init/destroy lock when accessing this.
     */
    private boolean destroyed;

    /**
     * Lock for lazily initializing and destroying the persistence service unit.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Persistence service unit. Must use the init/destroy lock to determine if lazy initialization is needed.
     */
    private PersistenceServiceUnit persistenceServiceUnit;

    private DatabaseTaskStore(DatabaseStore dbStore) {
        this.dbStore = dbStore;
    }

    public static DatabaseTaskStore get(DatabaseStore dbStore) {
        Integer count;
        DatabaseTaskStore dbTaskStore;
        synchronized (refCounts) {
            count = refCounts.get(dbStore);
            count = count == null ? 1 : (count + 1);
            refCounts.put(dbStore, count);

            if (count == 1)
                dbTaskStores.put(dbStore, dbTaskStore = new DatabaseTaskStore(dbStore));
            else
                dbTaskStore = dbTaskStores.get(dbStore);
        }
        return dbTaskStore;
    }

    public static void unget(DatabaseStore dbStore) {
        DatabaseTaskStore removed = null;
        synchronized (refCounts) {
            Integer count = refCounts.get(dbStore);
            if (count > 1)
                refCounts.put(dbStore, count - 1);
            else {
                refCounts.remove(dbStore);
                removed = dbTaskStores.remove(dbStore);
            }
        }
        if (removed != null) {
            removed.lock.writeLock().lock();
            try {
                removed.destroyed = true;
                if (removed.persistenceServiceUnit != null)
                    removed.persistenceServiceUnit.close();
            } catch (Throwable x) {
                // auto FFDC
            } finally {
                removed.lock.writeLock().unlock();
            }
        }
    }

    /**
     * Appends conditions to a JPA query to filter based on the presence or absence of the specified state.
     * This method optimizes to avoid the MOD function if possible.
     *
     * @param sb string builder for the query
     * @param state a task state. For example, TaskState.CANCELED
     * @param inState indicates whether to include or exclude results with the specified state
     * @return map of parameters and values that must be set on the query.
     */
    @Trivial
    private final Map<String, Object> appendStateComparison(StringBuilder sb, TaskState state, boolean inState) {
        Map<String, Object> params = new HashMap<String, Object>();
        switch (state) {
            case SCHEDULED:
                sb.append("t.STATES").append(inState ? "<" : ">=").append(":s");
                params.put("s", TaskState.SUSPENDED.bit);
                break;
            case SUSPENDED:
                sb.append(inState ? "t.STATES>=:s1 AND t.STATES<:s2" : "(t.STATES<:s1 OR t.STATES>=:s2)");
                params.put("s1", TaskState.SUSPENDED.bit);
                params.put("s2", TaskState.ENDED.bit);
                break;
            case ENDED:
            case CANCELED:
                sb.append("t.STATES").append(inState ? ">=" : "<").append(":s");
                params.put("s", state.bit);
                break;
            case SUCCESSFUL:
                sb.append(inState ? "t.STATES>=:s1 AND t.STATES<:s2" : "(t.STATES<:s1 OR t.STATES>=:s2)");
                params.put("s1", TaskState.SUCCESSFUL.bit);
                params.put("s2", TaskState.FAILURE_LIMIT_REACHED.bit);
                break;
            case FAILURE_LIMIT_REACHED:
                sb.append(inState ? "t.STATES>=:s1 AND t.STATES<:s2" : "(t.STATES<:s1 OR t.STATES>=:s2)");
                params.put("s1", TaskState.FAILURE_LIMIT_REACHED.bit);
                params.put("s2", TaskState.CANCELED.bit);
                break;
            default:
                sb.append("MOD(t.STATES,:s*2)").append(inState ? ">=" : "<").append(":s");
                params.put("s", state.bit);
        }
        return params;
    }

    /**
     * Update the record for a task in the persistent store to indicate that the task is canceled.
     *
     * @param taskId unique identifier for a persistent task
     * @return true if the state of the task was updated as a result of this method, otherwise false.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    @Override
    public boolean cancel(long taskId) throws Exception {
        StringBuilder update = new StringBuilder(87).append("UPDATE Task t SET t.STATES=")
                        .append(TaskState.CANCELED.bit + TaskState.ENDED.bit)
                        .append(",t.VERSION=t.VERSION+1 WHERE t.ID=:i AND t.STATES<")
                        .append(TaskState.ENDED.bit);

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "cancel", taskId, update);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(update.toString());
            query.setParameter("i", taskId);
            boolean canceled = query.executeUpdate() > 0;

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "cancel", canceled);
            return canceled;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int cancel(String pattern, Character escape, TaskState state, boolean inState, String owner) throws Exception {
        StringBuilder update = new StringBuilder(133).append("UPDATE Task t SET t.STATES=")
                        .append(TaskState.CANCELED.bit + TaskState.ENDED.bit)
                        .append(" WHERE t.STATES<")
                        .append(TaskState.ENDED.bit); // cannot cancel already-ended tasks
        if (owner != null)
            update.append(" AND t.OWNR=:o");
        if (pattern != null) {
            update.append(" AND t.INAME LIKE :p");
            if (escape != null)
                update.append(" ESCAPE :e");
        }
        Map<String, Object> stateParams = null;
        if (!TaskState.ANY.equals(state))
            stateParams = appendStateComparison(update.append(" AND "), state, inState);
        else if (!inState)
            return 0; // empty result if someone asks to cancel tasks not in any state

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "cancel", pattern, escape, state + ":" + inState, owner, update);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(update.toString());
            if (owner != null)
                query.setParameter("o", owner);
            if (pattern != null) {
                query.setParameter("p", pattern);
                if (escape != null)
                    query.setParameter("e", escape);
            }
            if (stateParams != null)
                for (Map.Entry<String, Object> param : stateParams.entrySet())
                    query.setParameter(param.getKey(), param.getValue());

            int count = query.executeUpdate();

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "cancel", count);
            return count;
        } finally {
            em.close();
        }
    }

    /**
     * Create an entry in the persistent store for a new task.
     *
     * @param taskRecord information about the persistent task
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    @Override
    public void create(TaskRecord taskRecord) throws Exception {
        Task task = new Task(taskRecord);
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            em.persist(task);
            em.flush();
            taskRecord.setId(task.ID);
        } finally {
            em.close();
        }
    }

    /**
     * Create a property entry in the persistent store.
     *
     * @param name unique name for the property.
     * @param value value of the property.
     * @return true if the property was created. False if a property with the same name already exists.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    @FFDCIgnore({ EntityExistsException.class, PersistenceException.class, Exception.class })
    @Override
    public boolean createProperty(String name, String value) throws Exception {
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        Property property = new Property(name, value);
        try {
            em.persist(property);
            em.flush();
        } catch (EntityExistsException x) {
            return false;
        } catch (PersistenceException x) {
            // Some JPA providers may throw PersistenceException for some scenarios where it can be
            // determined the row really does exist; handle those here
            if (x.getCause() instanceof SQLIntegrityConstraintViolationException) {
                return false;
            }
            try {
                em.detach(property); // ensure em.find won't return object from cache
                if (em.find(Property.class, name) != null) {
                    return false;
                }
            } catch (Exception ex) {
                // Oddly, JPA has not defined exceptions for em.find; throws subclass of RuntimeException
                // Do not FFDC; just adds extra clutter (original will be FFDC'd below)
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "createProperty", ex);
            }
            FFDCFilter.processException(x, getClass().getName(), "309", this);
            throw x;
        } finally {
            em.close();
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public List<PartitionRecord> find(PartitionRecord expected) throws Exception {
        StringBuilder select = new StringBuilder(172).append("SELECT p.EXECUTOR,p.HOSTNAME,p.ID,p.LSERVER,p.USERDIR FROM Partition p");
        if (expected != null) {
            select.append(" WHERE");
            if (expected.hasExecutor())
                select.append(" p.EXECUTOR=:x AND");
            if (expected.hasHostName())
                select.append(" p.HOSTNAME=:h AND");
            if (expected.hasId())
                select.append(" p.ID=:i AND");
            if (expected.hasLibertyServer())
                select.append(" p.LSERVER=:l AND");
            if (expected.hasUserDir())
                select.append(" p.USERDIR=:u AND");
            int length = select.length();
            select.delete(length - (select.charAt(length - 1) == 'E' ? 6 : 3), length);
        }
        select.append(" ORDER BY p.ID ASC");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "find", expected, select);

        List<PartitionRecord> records = new ArrayList<PartitionRecord>();
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            TypedQuery<Object[]> query = em.createQuery(select.toString(), Object[].class);
            if (expected != null) {
                if (expected.hasExecutor())
                    query.setParameter("x", expected.getExecutor());
                if (expected.hasHostName())
                    query.setParameter("h", expected.getHostName());
                if (expected.hasId())
                    query.setParameter("i", expected.getId());
                if (expected.hasLibertyServer())
                    query.setParameter("l", expected.getLibertyServer());
                if (expected.hasUserDir())
                    query.setParameter("u", expected.getUserDir());
            }

            List<Object[]> results = query.getResultList();
            for (Object[] result : results) {
                PartitionRecord record = new PartitionRecord(true);
                record.setExecutor((String) result[0]);
                record.setHostName((String) result[1]);
                record.setId((Long) result[2]);
                record.setLibertyServer((String) result[3]);
                record.setUserDir((String) result[4]);
                records.add(record);
            }
        } finally {
            em.close();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "find", records.size() < 20 ? records : records.size());
        return records;
    }

    /** {@inheritDoc} */
    @Override
    public TaskRecord find(long taskId, long partitionId, long maxNextExecTime, boolean forUpdate) throws Exception {
        StringBuilder find = new StringBuilder(237)
                        .append("SELECT t.LOADER,t.OWNR,t.MBITS,t.INAME,t.NEXTEXEC,t.ORIGSUBMT,t.PREVSCHED,t.PREVSTART,t.PREVSTOP,t.RESLT,t.RFAILS,t.STATES,t.TASKB,t.TASKINFO,t.TRIG,t.VERSION FROM Task t WHERE t.ID=:i AND t.PARTN=:p AND t.STATES<")
                        .append(TaskState.SUSPENDED.bit)
                        .append(" AND t.NEXTEXEC<=:m");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "find", taskId, partitionId, Utils.appendDate(new StringBuilder(30), maxNextExecTime), forUpdate, find);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            TypedQuery<Object[]> query = em.createQuery(find.toString(), Object[].class);
            if (forUpdate)
                query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            query.setParameter("i", taskId);
            query.setParameter("p", partitionId);
            query.setParameter("m", maxNextExecTime);
            List<Object[]> resultList = query.getResultList();
            if (resultList.isEmpty()) {
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "find", null);
                return null;
            }
            Object[] result = resultList.get(0);

            TaskRecord taskRecord = new TaskRecord(false);
            taskRecord.setId(taskId);
            taskRecord.setIdentifierOfClassLoader((String) result[0]);
            taskRecord.setIdentifierOfOwner((String) result[1]);
            taskRecord.setIdentifierOfPartition(partitionId);
            taskRecord.setMiscBinaryFlags((Short) result[2]);
            taskRecord.setName((String) result[3]);
            taskRecord.setNextExecutionTime((Long) result[4]);
            taskRecord.setOriginalSubmitTime((Long) result[5]);
            taskRecord.setPreviousScheduledStartTime((Long) result[6]);
            taskRecord.setPreviousStartTime((Long) result[7]);
            taskRecord.setPreviousStopTime((Long) result[8]);
            taskRecord.setResult((byte[]) result[9]);
            taskRecord.setConsecutiveFailureCount((Short) result[10]);
            taskRecord.setState((Short) result[11]);
            taskRecord.setTask((byte[]) result[12]);
            taskRecord.setTaskInformation((byte[]) result[13]);
            taskRecord.setTrigger((byte[]) result[14]);
            taskRecord.setVersion((Integer) result[15]);

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "find", taskRecord);
            return taskRecord;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public TaskRecord findById(long taskId, String owner, boolean includeTrigger) throws Exception {
        StringBuilder find = new StringBuilder(116).append("SELECT t.LOADER,t.MBITS,t.INAME,t.NEXTEXEC,t.RESLT,t.STATES,t.VERSION");
        if (includeTrigger)
            find.append(",t.TRIG");
        find.append(" FROM Task t WHERE t.ID=:i");
        if (owner != null)
            find.append(" AND t.OWNR=:o");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "findById", taskId, owner, find);

        List<Object[]> resultList;
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            TypedQuery<Object[]> query = em.createQuery(find.toString(), Object[].class);
            query.setParameter("i", taskId);
            if (owner != null)
                query.setParameter("o", owner);
            resultList = query.getResultList();
        } finally {
            em.close();
        }

        TaskRecord taskRecord;
        if (resultList.isEmpty())
            taskRecord = null;
        else {
            Object[] result = resultList.get(0);

            taskRecord = new TaskRecord(false);
            taskRecord.setId(taskId);
            taskRecord.setIdentifierOfClassLoader((String) result[0]);
            taskRecord.setMiscBinaryFlags((Short) result[1]);
            taskRecord.setName((String) result[2]);
            taskRecord.setNextExecutionTime((Long) result[3]);
            taskRecord.setResult((byte[]) result[4]);
            taskRecord.setState((Short) result[5]);
            taskRecord.setVersion((Integer) result[6]);
            if (includeTrigger)
                taskRecord.setTrigger((byte[]) result[7]);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "findById", taskRecord);
        return taskRecord;
    }

    /** {@inheritDoc} */
    @Override
    public long findOrCreate(PartitionRecord record) throws Exception {
        String find = "SELECT p.ID FROM Partition p WHERE p.EXECUTOR=:x AND p.HOSTNAME=:h AND p.LSERVER=:l AND p.USERDIR=:u";

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "findOrCreate", record, find);

        Long result;
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            // First look for an existing entry
            TypedQuery<Long> query = em.createQuery(find, Long.class);
            query.setParameter("x", record.getExecutor());
            query.setParameter("h", record.getHostName());
            query.setParameter("l", record.getLibertyServer());
            query.setParameter("u", record.getUserDir());
            List<Long> results = query.getResultList();
            if (results.size() > 0)
                result = results.get(0);
            else {
                // If not found, create a new entry
                Partition partition = new Partition(record);
                em.persist(partition);
                em.flush();
                result = partition.ID;
            }
        } finally {
            em.close();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "findOrCreate", result);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public List<Long> findTaskIds(String pattern, Character escape, TaskState state, boolean inState,
                                  Long minId, Integer maxResults, String owner, Long partition) throws Exception {
        int i = 0;
        StringBuilder find = new StringBuilder(152).append("SELECT t.ID FROM Task t");
        if (minId != null)
            find.append(++i == 1 ? " WHERE" : " AND").append(" t.ID>=:m");
        if (owner != null)
            find.append(++i == 1 ? " WHERE" : " AND").append(" t.OWNR=:o");
        if (partition != null)
            find.append(++i == 1 ? " WHERE" : " AND").append(" t.PARTN=:p");
        if (pattern != null) {
            find.append(++i == 1 ? " WHERE" : " AND").append(" t.INAME LIKE :n");
            if (escape != null)
                find.append(" ESCAPE :e");
        }
        Map<String, Object> stateParams = null;
        if (!TaskState.ANY.equals(state))
            stateParams = appendStateComparison(find.append(++i == 1 ? " WHERE " : " AND "), state, inState);
        else if (!inState)
            return Collections.emptyList(); // empty result if someone asks for tasks not in any state
        find.append(" ORDER BY t.ID ASC");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "findTaskIds", pattern, escape, state + ":" + inState, minId, maxResults, owner, partition, find);

        List<Long> resultList;
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery(find.toString(), Long.class);
            if (maxResults != null)
                query.setMaxResults(maxResults);
            if (minId != null)
                query.setParameter("m", minId);
            if (owner != null)
                query.setParameter("o", owner);
            if (partition != null)
                query.setParameter("p", partition);
            if (pattern != null) {
                query.setParameter("n", pattern);
                if (escape != null)
                    query.setParameter("e", escape);
            }
            if (stateParams != null)
                for (Map.Entry<String, Object> param : stateParams.entrySet())
                    query.setParameter(param.getKey(), param.getValue());

            resultList = query.getResultList();
        } finally {
            em.close();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "findTaskIds", resultList.size() <= 100 ? resultList : resultList.size());
        return resultList;
    }

    /** {@inheritDoc} */
    @Override
    public List<TaskStatus<?>> findTaskStatus(String pattern, Character escape, TaskState state, boolean inState,
                                              Long minId, Integer maxResults, String owner, boolean includeTrigger,
                                              PersistentExecutor executor) throws Exception {
        StringBuilder find = new StringBuilder(187)
                        .append("SELECT t.ID,t.LOADER,t.MBITS,t.INAME,t.NEXTEXEC,t.RESLT,t.STATES");
        if (includeTrigger)
            find.append(",t.TRIG");
        find.append(" FROM Task t");
        int i = 0;
        if (minId != null)
            find.append(++i == 1 ? " WHERE" : " AND").append(" t.ID>=:m");
        if (owner != null)
            find.append(++i == 1 ? " WHERE" : " AND").append(" t.OWNR=:o");
        if (pattern != null) {
            find.append(++i == 1 ? " WHERE" : " AND").append(" t.INAME LIKE :p");
            if (escape != null)
                find.append(" ESCAPE :e");
        }
        Map<String, Object> stateParams = null;
        if (!TaskState.ANY.equals(state))
            stateParams = appendStateComparison(find.append(++i == 1 ? " WHERE " : " AND "), state, inState);
        else if (!inState)
            return Collections.emptyList(); // empty result if someone asks for tasks not in any state
        find.append(" ORDER BY t.ID ASC");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "findTaskStatus", pattern, escape, state + ":" + inState, minId, maxResults, owner, executor, find);

        List<Object[]> results;
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            TypedQuery<Object[]> query = em.createQuery(find.toString(), Object[].class);
            if (maxResults != null)
                query.setMaxResults(maxResults);
            if (minId != null)
                query.setParameter("m", minId);
            if (owner != null)
                query.setParameter("o", owner);
            if (pattern != null) {
                query.setParameter("p", pattern);
                if (escape != null)
                    query.setParameter("e", escape);
            }
            if (stateParams != null)
                for (Map.Entry<String, Object> param : stateParams.entrySet())
                    query.setParameter(param.getKey(), param.getValue());

            results = query.getResultList();
        } finally {
            em.close();
        }

        List<TaskStatus<?>> statusList = new ArrayList<TaskStatus<?>>(results.size());
        for (Object[] result : results) {
            TaskRecord record = new TaskRecord(false);
            record.setId((Long) result[0]);
            record.setIdentifierOfClassLoader((String) result[1]);
            record.setMiscBinaryFlags((Short) result[2]);
            record.setName((String) result[3]);
            record.setNextExecutionTime((Long) result[4]);
            record.setResult((byte[]) result[5]);
            record.setState((Short) result[6]);
            if (includeTrigger)
                record.setTrigger((byte[]) result[7]);
            statusList.add(record.toTaskStatus(executor));
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "findTaskStatus", statusList.size() < 20 ? statusList : statusList.size());
        return statusList;
    }

    /** {@inheritDoc} */
    @Override
    public List<Object[]> findUpcomingTasks(long partition, long maxNextExecTime, Integer maxResults) throws Exception {
        StringBuilder find = new StringBuilder(129)
                        .append("SELECT t.ID,t.MBITS,t.NEXTEXEC,t.TXTIMEOUT FROM Task t WHERE t.PARTN=:p AND t.STATES<")
                        .append(TaskState.SUSPENDED.bit)
                        .append(" AND t.NEXTEXEC<=:m ORDER BY t.NEXTEXEC");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "findUpcomingTasks", partition, Utils.appendDate(new StringBuilder(30), maxNextExecTime), maxResults, find);

        List<Object[]> resultList;
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            TypedQuery<Object[]> query = em.createQuery(find.toString(), Object[].class);
            query.setParameter("p", partition);
            query.setParameter("m", maxNextExecTime);
            if (maxResults != null)
                query.setMaxResults(maxResults);
            List<Object[]> results = query.getResultList();
            resultList = results;
        } finally {
            em.close();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "findUpcomingTasks", resultList.size());
        return resultList;
    }

    /** {@inheritDoc} */
    @Override
    public TaskRecord getNextExecutionTime(long taskId, String owner) throws Exception {
        StringBuilder find = new StringBuilder(74)
                        .append("SELECT t.MBITS,t.NEXTEXEC,t.STATES FROM Task t WHERE t.ID=:i");
        if (owner != null)
            find.append(" AND t.OWNR=:o");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getNextExecutionTime", taskId, owner, find);

        List<Object[]> resultList;
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            TypedQuery<Object[]> query = em.createQuery(find.toString(), Object[].class);
            query.setParameter("i", taskId);
            if (owner != null)
                query.setParameter("o", owner);
            resultList = query.getResultList();
        } finally {
            em.close();
        }

        TaskRecord taskRecord;
        if (resultList.isEmpty())
            taskRecord = null;
        else {
            Object[] result = resultList.get(0);
            taskRecord = new TaskRecord(false);
            taskRecord.setMiscBinaryFlags((Short) result[0]);
            taskRecord.setNextExecutionTime((Long) result[1]);
            taskRecord.setState((Short) result[2]);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getNextExecutionTime", taskRecord);
        return taskRecord;
    }

    /**
     * Returns the persistence service unit, lazily initializing if necessary.
     *
     * @return the persistence service unit.
     * @throws Exceptin if an error occurs.
     * @throws IllegalStateException if this instance has been destroyed.
     */
    public final PersistenceServiceUnit getPersistenceServiceUnit() throws Exception {
        lock.readLock().lock();
        try {
            if (destroyed)
                throw new IllegalStateException();
            if (persistenceServiceUnit == null) {
                // Switch to write lock for lazy initialization
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (destroyed)
                        throw new IllegalStateException();
                    if (persistenceServiceUnit == null)
                        persistenceServiceUnit = dbStore.createPersistenceServiceUnit(priv.getClassLoader(Task.class),
                                                                                      Partition.class.getName(),
                                                                                      Property.class.getName(),
                                                                                      Task.class.getName());
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            return persistenceServiceUnit;
        } finally {
            lock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties(String pattern, Character escape) throws Exception {
        StringBuilder find = new StringBuilder(62)
                        .append("SELECT p.ID,p.VAL FROM Property p WHERE p.ID LIKE :p");
        if (escape != null)
            find.append(" ESCAPE :e");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getProperties", pattern, escape, find);

        Map<String, String> map = new HashMap<String, String>();
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            TypedQuery<Object[]> query = em.createQuery(find.toString(), Object[].class);
            if (pattern != null) {
                query.setParameter("p", pattern);
                if (escape != null)
                    query.setParameter("e", escape);
            }
            List<Object[]> results = query.getResultList();
            for (Iterator<Object[]> it = results.iterator(); it.hasNext();) {
                Object[] row = it.next();
                map.put((String) row[0], (String) row[1]);
            }
        } finally {
            em.close();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getProperties", map.size() < 20 ? map : map.size());
        return map;
    }

    /** {@inheritDoc} */
    @Override
    public String getProperty(String name) throws Exception {
        String find = "SELECT p.VAL FROM Property p WHERE p.ID=:i";

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getProperty", name, find);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            TypedQuery<String> query = em.createQuery(find, String.class);
            query.setParameter("i", name);
            List<String> results = query.getResultList();
            String value = results.isEmpty() ? null : results.get(0);

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "getProperty", value);
            return value;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public TaskRecord getTrigger(long taskId) throws Exception {
        String find = "SELECT t.OWNR,t.STATES,t.TRIG FROM Task t WHERE t.ID=:i";

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getTrigger", taskId, find);

        List<Object[]> resultList;
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            TypedQuery<Object[]> query = em.createQuery(find.toString(), Object[].class);
            query.setParameter("i", taskId);
            resultList = query.getResultList();
        } finally {
            em.close();
        }

        TaskRecord taskRecord;
        if (resultList.isEmpty())
            taskRecord = null;
        else {
            Object[] result = resultList.get(0);
            taskRecord = new TaskRecord(false);
            taskRecord.setIdentifierOfOwner((String) result[0]);
            taskRecord.setState((Short) result[1]);
            taskRecord.setTrigger((byte[]) result[2]);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getTrigger", taskRecord);
        return taskRecord;
    }

    /** {@inheritDoc} */
    @Override
    public short incrementFailureCount(long taskId) throws Exception {
        String update = "UPDATE Task t SET t.RFAILS=t.RFAILS+1 WHERE t.ID=:i AND t.RFAILS<" + Short.MAX_VALUE;
        String find = "SELECT t.RFAILS FROM Task t WHERE t.ID=:i";
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "incrementFailureCount", taskId, update, find);

        short consecutiveFailureCount;
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query updateQuery = em.createQuery(update.toString());
            updateQuery.setParameter("i", taskId);
            updateQuery.executeUpdate();
            TypedQuery<Short> findQuery = em.createQuery(find, Short.class);
            findQuery.setParameter("i", taskId);
            List<Short> results = findQuery.getResultList();
            consecutiveFailureCount = results.size() == 1 ? results.get(0) : -1;
        } finally {
            em.close();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "incrementFailureCount", consecutiveFailureCount);
        return consecutiveFailureCount;
    }

    /** {@inheritDoc} */
    @Override
    public int persist(PartitionRecord updates, PartitionRecord expected) throws Exception {
        StringBuilder update = new StringBuilder(160)
                        .append("UPDATE Partition SET ");
        if (updates.hasExecutor())
            update.append("EXECUTOR=:x2,");
        if (updates.hasHostName())
            update.append("HOSTNAME=:h2,");
        if (updates.hasId())
            update.append("ID=:i2,");
        if (updates.hasLibertyServer())
            update.append("LSERVER=:l2,");
        if (updates.hasUserDir())
            update.append("USERDIR=:u2,");
        update.setCharAt(update.length() - 1, ' ');

        update.append("WHERE");
        if (expected.hasExecutor())
            update.append(" EXECUTOR=:x1 AND");
        if (expected.hasHostName())
            update.append(" HOSTNAME=:h1 AND");
        if (expected.hasId())
            update.append(" ID=:i1 AND");
        if (expected.hasLibertyServer())
            update.append(" LSERVER=:l1 AND");
        if (expected.hasUserDir())
            update.append(" USERDIR=:u1 AND");
        int length = update.length();
        update.delete(length - (update.charAt(length - 1) == 'E' ? 6 : 4), length);

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "persist", updates, expected, update);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(update.toString());

            if (updates.hasExecutor())
                query.setParameter("x2", updates.getExecutor());
            if (updates.hasHostName())
                query.setParameter("h2", updates.getHostName());
            if (updates.hasId())
                query.setParameter("i2", updates.getId());
            if (updates.hasLibertyServer())
                query.setParameter("l2", updates.getLibertyServer());
            if (updates.hasUserDir())
                query.setParameter("u2", updates.getUserDir());

            if (expected.hasExecutor())
                query.setParameter("x1", expected.getExecutor());
            if (expected.hasHostName())
                query.setParameter("h1", expected.getHostName());
            if (expected.hasId())
                query.setParameter("i1", expected.getId());
            if (expected.hasLibertyServer())
                query.setParameter("l1", expected.getLibertyServer());
            if (expected.hasUserDir())
                query.setParameter("u1", expected.getUserDir());

            int count = query.executeUpdate();

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "persist", count);
            return count;
        } finally {
            em.close();
        }
    }

    /**
     * Persist updates to a task record in the persistent store.
     *
     * @param updates updates to make to the task. Only the specified fields are persisted. Version must be omitted, as it always increments by 1.
     * @param expected criteria that must be matched for optimistic update to succeed. Must include Id.
     * @return true if persistent task store was updated, otherwise false.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    @Override
    public boolean persist(TaskRecord updates, TaskRecord expected) throws Exception {
        StringBuilder update = new StringBuilder(220)
                        .append("UPDATE Task t SET ");
        if (updates != null) {
            if (updates.hasIdentifierOfClassLoader())
                update.append("t.LOADER=:c2,");
            if (updates.hasIdentifierOfOwner())
                update.append("t.OWNR=:o2,");
            if (updates.hasIdentifierOfPartition())
                update.append("t.PARTN=:p2,");
            if (updates.hasMiscBinaryFlags())
                update.append("t.MBITS=:m2,");
            if (updates.hasName())
                update.append("t.INAME=:n2,");
            if (updates.hasNextExecutionTime())
                update.append("t.NEXTEXEC=:ne2,");
            if (updates.hasOriginalSubmitTime())
                update.append("t.ORIGSUBMT=:os2,");
            if (updates.hasPreviousScheduledStartTime())
                update.append("t.PREVSCHED=:sc2,");
            if (updates.hasPreviousStartTime())
                update.append("t.PREVSTART=:sa2,");
            if (updates.hasPreviousStopTime())
                update.append("t.PREVSTOP=:so2,");
            if (updates.hasResult())
                update.append("t.RESLT=:r2,");
            if (updates.hasConsecutiveFailureCount())
                update.append("t.RFAILS=:f2,");
            if (updates.hasState())
                update.append("t.STATES=:s2,");
            if (updates.hasTask())
                update.append("t.TASKB=:t2,");
            if (updates.hasTaskInformation())
                update.append("t.TASKINFO=:ti2,");
            if (updates.hasTrigger())
                update.append("t.TRIG=:tr2,");
        }

        update.append("t.VERSION=t.VERSION+1 WHERE t.ID=:i");

        if (expected.hasIdentifierOfClassLoader())
            update.append(" AND t.LOADER=:c1");
        if (expected.hasIdentifierOfOwner())
            update.append(" AND t.OWNR=:o1");
        if (expected.hasIdentifierOfPartition())
            update.append(" AND t.PARTN=:p1");
        if (expected.hasMiscBinaryFlags())
            update.append(" AND t.MBITS=:m1");
        if (expected.hasName())
            update.append(" AND t.INAME=:n1");
        if (expected.hasNextExecutionTime())
            update.append(" AND t.NEXTEXEC=:ne1");
        if (expected.hasOriginalSubmitTime())
            update.append(" AND t.ORIGSUBMT=:os1");
        if (expected.hasPreviousScheduledStartTime())
            update.append(" AND t.PREVSCHED=:sc1");
        if (expected.hasPreviousStartTime())
            update.append(" AND t.PREVSTART=:sa1");
        if (expected.hasPreviousStopTime())
            update.append(" AND t.PREVSTOP=:so1");
        if (expected.hasResult())
            update.append(" AND t.RESLT=:r1");
        if (expected.hasConsecutiveFailureCount())
            update.append(" AND t.RFAILS=:f1");
        if (expected.hasState())
            update.append(" AND t.STATES=:s1");
        if (expected.hasTask())
            update.append(" AND t.TASKB=:t1");
        if (expected.hasTaskInformation())
            update.append(" AND t.TASKINFO=:ti1");
        if (expected.hasTrigger())
            update.append(" AND t.TRIG=:tr1");
        if (expected.hasVersion())
            update.append(" AND t.VERSION=:v1");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "persist", updates, expected, update);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(update.toString());

            if (updates != null) {
                if (updates.hasIdentifierOfClassLoader())
                    query.setParameter("c2", updates.getIdentifierOfClassLoader());
                if (updates.hasIdentifierOfOwner())
                    query.setParameter("o2", updates.getIdentifierOfOwner());
                if (updates.hasIdentifierOfPartition())
                    query.setParameter("p2", updates.getIdentifierOfPartition());
                if (updates.hasMiscBinaryFlags())
                    query.setParameter("m2", updates.getMiscBinaryFlags());
                if (updates.hasName())
                    query.setParameter("n2", updates.getName());
                if (updates.hasNextExecutionTime())
                    query.setParameter("ne2", updates.getNextExecutionTime());
                if (updates.hasOriginalSubmitTime())
                    query.setParameter("os2", updates.getOriginalSubmitTime());
                if (updates.hasPreviousScheduledStartTime())
                    query.setParameter("sc2", updates.getPreviousScheduledStartTime());
                if (updates.hasPreviousStartTime())
                    query.setParameter("sa2", updates.getPreviousStartTime());
                if (updates.hasPreviousStopTime())
                    query.setParameter("so2", updates.getPreviousStopTime());
                if (updates.hasResult())
                    query.setParameter("r2", updates.getResult());
                if (updates.hasConsecutiveFailureCount())
                    query.setParameter("f2", updates.getConsecutiveFailureCount());
                if (updates.hasState())
                    query.setParameter("s2", updates.getState());
                if (updates.hasTask())
                    query.setParameter("t2", updates.getTask());
                if (updates.hasTaskInformation())
                    query.setParameter("ti2", updates.getTaskInformation());
                if (updates.hasTrigger())
                    query.setParameter("tr2", updates.getTrigger());
            }

            query.setParameter("i", expected.getId());

            if (expected.hasIdentifierOfClassLoader())
                query.setParameter("c1", expected.getIdentifierOfClassLoader());
            if (expected.hasIdentifierOfOwner())
                query.setParameter("o1", expected.getIdentifierOfOwner());
            if (expected.hasIdentifierOfPartition())
                query.setParameter("p1", expected.getIdentifierOfPartition());
            if (expected.hasMiscBinaryFlags())
                query.setParameter("m1", expected.getMiscBinaryFlags());
            if (expected.hasName())
                query.setParameter("n1", expected.getName());
            if (expected.hasNextExecutionTime())
                query.setParameter("ne1", expected.getNextExecutionTime());
            if (expected.hasOriginalSubmitTime())
                query.setParameter("os1", expected.getOriginalSubmitTime());
            if (expected.hasPreviousScheduledStartTime())
                query.setParameter("sc1", expected.getPreviousScheduledStartTime());
            if (expected.hasPreviousStartTime())
                query.setParameter("sa1", expected.getPreviousStartTime());
            if (expected.hasPreviousStopTime())
                query.setParameter("so1", expected.getPreviousStopTime());
            if (expected.hasResult())
                query.setParameter("r1", expected.getResult());
            if (expected.hasConsecutiveFailureCount())
                query.setParameter("f1", expected.getConsecutiveFailureCount());
            if (expected.hasState())
                query.setParameter("s1", expected.getState());
            if (expected.hasTask())
                query.setParameter("t1", expected.getTask());
            if (expected.hasTaskInformation())
                query.setParameter("ti1", expected.getTaskInformation());
            if (expected.hasTrigger())
                query.setParameter("tr1", expected.getTrigger());
            if (expected.hasVersion())
                query.setParameter("v1", expected.getVersion());

            boolean updated = query.executeUpdate() > 0;
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "persist", updated);
            return updated;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(long taskId, String owner, boolean removeIfEnded) throws Exception {
        StringBuilder delete = new StringBuilder(66)
                        .append("DELETE FROM Task t WHERE t.ID=:i");
        if (!removeIfEnded)
            delete.append(" AND t.STATES<").append(TaskState.ENDED.bit);
        if (owner != null)
            delete.append(" AND t.OWNR=:o");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "remove", taskId, owner, removeIfEnded, delete);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(delete.toString());
            query.setParameter("i", taskId);
            if (owner != null)
                query.setParameter("o", owner);

            boolean removed = query.executeUpdate() > 0;

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "remove", removed);
            return removed;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int remove(PartitionRecord criteria) throws Exception {
        StringBuilder delete = new StringBuilder(111)
                        .append("DELETE FROM Partition p WHERE");
        if (criteria != null) {
            if (criteria.hasExecutor())
                delete.append(" p.EXECUTOR=:x AND");
            if (criteria.hasHostName())
                delete.append(" p.HOSTNAME=:h AND");
            if (criteria.hasId())
                delete.append(" p.ID=:i AND");
            if (criteria.hasLibertyServer())
                delete.append(" p.LSERVER=:l AND");
            if (criteria.hasUserDir())
                delete.append(" p.USERDIR=:u AND");
        }
        int length = delete.length();
        delete.delete(length - (delete.charAt(length - 1) == 'E' ? 6 : 4), length);

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "remove", criteria, delete);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(delete.toString());

            if (criteria != null) {
                if (criteria.hasExecutor())
                    query.setParameter("x", criteria.getExecutor());
                if (criteria.hasHostName())
                    query.setParameter("h", criteria.getHostName());
                if (criteria.hasId())
                    query.setParameter("i", criteria.getId());
                if (criteria.hasLibertyServer())
                    query.setParameter("l", criteria.getLibertyServer());
                if (criteria.hasUserDir())
                    query.setParameter("u", criteria.getUserDir());
            }

            int count = query.executeUpdate();

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "remove", count);
            return count;
        } finally {
            em.close();
        }
    }

    /**
     * Remove all tasks that match the specified name pattern and the presence or absence
     * (as determined by the inState attribute) of the specified state.
     * For example, to remove all canceled tasks that have a name that starts with "PAYROLL_TASK_",
     * taskStore.remove("PAYROLL\\_TASK\\_%", '\\', TaskState.CANCELED, true, "app1");
     *
     * @param pattern task name pattern similar to the LIKE clause in SQL (% matches any characters, _ matches one character)
     * @param escape escape character that indicates when matching characters like % and _ should be interpreted literally.
     * @param state a task state. For example, TaskState.UNATTEMPTED.
     * @param inState indicates whether to remove tasks with or without the specified state
     * @param owner name of owner to match as the task submitter. Null to ignore.
     * @return count of tasks removed.
     * @throws Exception if an error occurs when attempting to update the persistent task store.
     */
    @Override
    public int remove(String pattern, Character escape, TaskState state, boolean inState, String owner) throws Exception {
        StringBuilder delete = new StringBuilder(100)
                        .append("DELETE FROM Task t");
        int i = 0;
        if (owner != null)
            delete.append(++i == 1 ? " WHERE" : " AND").append(" t.OWNR=:o");
        if (pattern != null) {
            delete.append(++i == 1 ? " WHERE" : " AND").append(" t.INAME LIKE :p");
            if (escape != null)
                delete.append(" ESCAPE :e");
        }
        Map<String, Object> stateParams = null;
        if (!TaskState.ANY.equals(state))
            stateParams = appendStateComparison(delete.append(++i == 1 ? " WHERE " : " AND "), state, inState);
        else if (!inState)
            return 0; // empty result if someone asks to remove tasks not in any state

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "remove", pattern, escape, state + ":" + inState, owner, delete);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(delete.toString());
            if (owner != null)
                query.setParameter("o", owner);
            if (pattern != null) {
                query.setParameter("p", pattern);
                if (escape != null)
                    query.setParameter("e", escape);
            }
            if (stateParams != null)
                for (Map.Entry<String, Object> param : stateParams.entrySet())
                    query.setParameter(param.getKey(), param.getValue());

            int count = query.executeUpdate();

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "remove", count);
            return count;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int removeProperties(String pattern, Character escape) throws Exception {
        StringBuilder delete = new StringBuilder(58)
                        .append("DELETE FROM Property WHERE ID LIKE :pattern");
        if (escape != null)
            delete.append(" ESCAPE :escape");

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "removeProperties", pattern, escape, delete);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(delete.toString());
            query.setParameter("pattern", pattern);
            if (escape != null)
                query.setParameter("escape", escape);
            int count = query.executeUpdate();

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "removeProperties", count);
            return count;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeProperty(String name) throws Exception {
        String update = "DELETE FROM Property WHERE ID=:i";

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "removeProperty", name, update);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(update);
            query.setParameter("i", name);
            boolean removed = query.executeUpdate() > 0;

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "removeProperty", removed);
            return removed;
        } finally {
            em.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean setProperty(String name, String value) throws Exception {
        String update = "UPDATE Property SET VAL=:v WHERE ID=:i";

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "setProperty", name, value, update);

        boolean exists;
        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(update);
            query.setParameter("v", value);
            query.setParameter("i", name);
            exists = query.executeUpdate() > 0;
        } finally {
            em.close();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "setProperty", exists);
        return exists;
    }

    /** {@inheritDoc} */
    @Override
    public int transfer(Long maxTaskId, long oldPartitionId, long newPartitionId) throws Exception {
        StringBuilder update = new StringBuilder(69)
                        .append("UPDATE Task SET PARTN=:p2 WHERE ");
        if (maxTaskId != null && maxTaskId != Long.MAX_VALUE)
            update.append("ID<=:i AND ");
        update.append("PARTN=:p1 AND STATES<").append(TaskState.ENDED.bit);

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "transfer", "taskId>=" + maxTaskId, "partition:" + oldPartitionId + "-->" + newPartitionId, update);

        EntityManager em = getPersistenceServiceUnit().createEntityManager();
        try {
            Query query = em.createQuery(update.toString());
            query.setParameter("p2", newPartitionId);
            if (maxTaskId != null && maxTaskId != Long.MAX_VALUE)
                query.setParameter("i", maxTaskId);
            query.setParameter("p1", oldPartitionId);
            int count = query.executeUpdate();

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "transfer", count);
            return count;
        } finally {
            em.close();
        }
    }
}
